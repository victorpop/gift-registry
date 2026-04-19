/**
 * Tests for onPurchaseNotification trigger (Plan 06-02, Task 2).
 * Covers: D-04 transition guard, idempotency, opt-in, opt-out, FCM fanout (D-10),
 * stale token cleanup (D-11), locale selection (D-14), failure logging (D-15).
 */

// FCM mock
const sendEachForMulticastMock = jest.fn();
jest.mock("firebase-admin/messaging", () => ({
  getMessaging: () => ({ sendEachForMulticast: sendEachForMulticastMock }),
}));

// sendEmail + purchaseTemplate mocks (avoid Firestore writes from email path being entangled)
const sendEmailMock = jest.fn();
const purchaseTemplateMock = jest.fn();
jest.mock("../email/send", () => ({
  sendEmail: sendEmailMock,
}));
jest.mock("../email/templates/purchase", () => ({
  purchaseTemplate: purchaseTemplateMock,
}));

// Shared mutable store
let store: Record<string, Record<string, Record<string, unknown>>>;
const mailStore: unknown[] = [];
const notificationsFailuresStore: unknown[] = [];

function resetStore() {
  store = {
    registries: {
      reg1: {
        ownerId: "owner1",
        title: "Wedding Registry",
        notifyOnPurchase: true,
      },
    },
    "registries/reg1/items": {
      it1: { status: "reserved", title: "Coffee Machine" },
    },
    users: {
      owner1: { email: "owner@x.com", preferredLocale: "en" },
    },
    "users/owner1/fcmTokens": {
      "tok-1": { token: "tok-1", platform: "android" },
      "tok-2": { token: "tok-2", platform: "android" },
    },
    reservations: {
      "res-1": {
        itemId: "it1",
        registryId: "reg1",
        status: "purchased",
        giverName: "Ana Popescu",
        createdAt: { seconds: 1000, nanoseconds: 0 },
      },
    },
    mail: {},
    notifications_failures: {},
  };
  mailStore.length = 0;
  notificationsFailuresStore.length = 0;
}

jest.mock("firebase-admin", () => {
  const makeDocRef = (collPath: string, docId: string): unknown => {
    const docRefObj: Record<string, unknown> = {
      id: docId,
      path: `${collPath}/${docId}`,
      get: async () => {
        const col = store[collPath];
        const data = col ? col[docId] : undefined;
        return {
          exists: data !== undefined,
          id: docId,
          data: () => (data ? { ...data } : undefined),
        };
      },
      set: async (data: Record<string, unknown>) => {
        if (!store[collPath]) store[collPath] = {};
        store[collPath][docId] = { ...data };
      },
      update: async (data: Record<string, unknown>) => {
        if (!store[collPath]) store[collPath] = {};
        store[collPath][docId] = {
          ...(store[collPath][docId] || {}),
          ...data,
        };
      },
      delete: async () => {
        if (store[collPath]) delete store[collPath][docId];
      },
      collection: (sub: string) => makeCollRef(`${collPath}/${docId}/${sub}`),
    };
    return docRefObj;
  };

  function makeWhereQuery(collPath: string, field: string, op: string, val: unknown) {
    return {
      where: (f: string, o: string, v: unknown) => makeWhereQuery(collPath, f, o, v),
      orderBy: () => ({
        limit: () => ({
          get: async () => {
            const col = store[collPath] || {};
            const docs = Object.entries(col)
              .filter(([, data]) => {
                const d = data as Record<string, unknown>;
                if (op === "==") return d[field] === val;
                return true;
              })
              .map(([id, data]) => ({
                id,
                exists: true,
                data: () => ({ ...data }),
              }));
            return { docs, empty: docs.length === 0, size: docs.length };
          },
        }),
      }),
      limit: () => ({
        get: async () => {
          const col = store[collPath] || {};
          const docs = Object.entries(col)
            .filter(([, data]) => {
              const d = data as Record<string, unknown>;
              if (op === "==") return d[field] === val;
              return true;
            })
            .map(([id, data]) => ({
              id,
              exists: true,
              data: () => ({ ...data }),
            }));
          return { docs, empty: docs.length === 0, size: docs.length };
        },
      }),
    };
  }

  const makeCollRef = (collPath: string): unknown => ({
    doc: (id: string) => makeDocRef(collPath, id),
    add: async (data: Record<string, unknown>) => {
      const id = `auto_${Date.now()}_${Math.random().toString(36).slice(2)}`;
      if (!store[collPath]) store[collPath] = {};
      store[collPath][id] = { ...data };
      if (collPath === "mail") mailStore.push({ ...data });
      if (collPath === "notifications_failures") notificationsFailuresStore.push({ ...data });
      return makeDocRef(collPath, id);
    },
    get: async () => {
      const col = store[collPath] || {};
      const docs = Object.entries(col).map(([id, data]) => ({
        id,
        exists: true,
        data: () => ({ ...data }),
        ref: makeDocRef(collPath, id),
      }));
      return { docs, empty: docs.length === 0, size: docs.length };
    },
    where: (field: string, op: string, val: unknown) =>
      makeWhereQuery(collPath, field, op, val),
    batch: () => {
      const ops: Array<() => void> = [];
      return {
        delete: (ref: { delete: () => void }) => {
          ops.push(() => ref.delete());
        },
        commit: async () => {
          ops.forEach((op) => op());
        },
      };
    },
  });

  const fakeDb = {
    collection: (path: string) => makeCollRef(path),
    doc: (path: string) => {
      const parts = path.split("/");
      const docId = parts[parts.length - 1];
      const collPath = parts.slice(0, parts.length - 1).join("/");
      return makeDocRef(collPath, docId);
    },
    runTransaction: async (fn: (tx: unknown) => Promise<unknown>) => {
      const tx = {
        get: async (ref: { get: () => unknown }) => ref.get(),
        update: (ref: { update: (d: unknown) => void }, data: unknown) =>
          ref.update(data as Record<string, unknown>),
        set: (ref: { set: (d: unknown) => void }, data: unknown) =>
          ref.set(data as Record<string, unknown>),
      };
      return fn(tx);
    },
    batch: () => {
      const ops: Array<() => void> = [];
      return {
        delete: (ref: { delete: () => void }) => {
          ops.push(() => ref.delete());
        },
        commit: async () => {
          ops.forEach((op) => op());
        },
      };
    },
  };

  return {
    __esModule: true,
    initializeApp: jest.fn(),
    firestore: () => fakeDb,
    auth: () => ({
      getUser: async (uid: string) => {
        if (uid === "owner1") return { uid: "owner1", email: "owner@x.com" };
        throw Object.assign(new Error("auth/user-not-found"), { code: "auth/user-not-found" });
      },
    }),
  };
});

jest.mock("firebase-admin/firestore", () => ({
  FieldValue: {
    serverTimestamp: () => new Date(),
    delete: () => "__DELETE__",
  },
}));

// Import AFTER mocks
import { onPurchaseNotification } from "../notifications/onPurchaseNotification";

// Build a minimal event object mirroring the onDocumentUpdated event shape
function makeEvent(
  before: Record<string, unknown>,
  after: Record<string, unknown>,
  params: { registryId: string; itemId: string } = { registryId: "reg1", itemId: "it1" }
) {
  const afterRef = {
    get: async () => {
      const col = store[`registries/${params.registryId}/items`];
      const data = col ? col[params.itemId] : undefined;
      return { exists: data !== undefined, data: () => (data ? { ...data } : undefined) };
    },
    update: async (data: Record<string, unknown>) => {
      if (!store[`registries/${params.registryId}/items`]) {
        store[`registries/${params.registryId}/items`] = {};
      }
      store[`registries/${params.registryId}/items`][params.itemId] = {
        ...(store[`registries/${params.registryId}/items`][params.itemId] || {}),
        ...data,
      };
    },
  };

  return {
    data: {
      before: { data: () => ({ ...before }) },
      after: {
        data: () => ({ ...after }),
        ref: afterRef,
      },
    },
    params,
  };
}

beforeEach(() => {
  resetStore();
  sendEachForMulticastMock.mockReset();
  sendEachForMulticastMock.mockResolvedValue({
    responses: [{ success: true }, { success: true }],
  });
  sendEmailMock.mockReset();
  sendEmailMock.mockResolvedValue(undefined);
  purchaseTemplateMock.mockReset();
  purchaseTemplateMock.mockReturnValue({
    subject: "Someone bought a gift from your registry!",
    html: "<p>test</p>",
    text: "test",
  });
});

describe("onPurchaseNotification trigger", () => {
  it("Test 1 (no-op when status unchanged): same-to-same transition exits without reading registry", async () => {
    const event = makeEvent(
      { status: "purchased" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
    expect(sendEmailMock).not.toHaveBeenCalled();
  });

  it("Test 2 (no-op wrong transition): available→purchased does NOT trigger notifications", async () => {
    const event = makeEvent(
      { status: "available" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
    expect(sendEmailMock).not.toHaveBeenCalled();
  });

  it("Test 3 (idempotency — after.purchaseNotificationSentAt already set): exits early, no FCM/email", async () => {
    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased", purchaseNotificationSentAt: new Date() }
    );

    await onPurchaseNotification.run(event as never);

    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
    expect(sendEmailMock).not.toHaveBeenCalled();
  });

  it("Test 4 (opt-out): notifyOnPurchase=false → no FCM, no email, no failures doc", async () => {
    store.registries.reg1 = { ...store.registries.reg1, notifyOnPurchase: false };

    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
    expect(sendEmailMock).not.toHaveBeenCalled();
    expect(notificationsFailuresStore).toHaveLength(0);
  });

  it("Test 5 (opt-in happy path EN): English owner — FCM title EN, purchaseTemplate called, email sent", async () => {
    store.users.owner1 = { email: "owner@x.com", preferredLocale: "en" };

    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    // FCM sent to 2 tokens
    expect(sendEachForMulticastMock).toHaveBeenCalledTimes(1);
    const fcmCall = sendEachForMulticastMock.mock.calls[0][0];
    expect(fcmCall.tokens).toHaveLength(2);
    expect(fcmCall.notification.title).toBe("Your gift was purchased! 🎁");
    expect(fcmCall.data.type).toBe("purchase");
    expect(fcmCall.data.registryId).toBe("reg1");
    expect(fcmCall.data.itemId).toBe("it1");

    // purchaseTemplate called with locale en and giver name from reservation
    expect(purchaseTemplateMock).toHaveBeenCalledTimes(1);
    const templateCall = purchaseTemplateMock.mock.calls[0];
    expect(templateCall[1]).toBe("en");
    expect(templateCall[0].giverFirstName).toBe("Ana");
    expect(templateCall[0].registryName).toBe("Wedding Registry");

    // sendEmail called
    expect(sendEmailMock).toHaveBeenCalledTimes(1);
    expect(sendEmailMock.mock.calls[0][0].to).toBe("owner@x.com");
  });

  it("Test 6 (opt-in happy path RO): Romanian owner — FCM title RO, purchaseTemplate called with locale ro", async () => {
    store.users.owner1 = { email: "owner@x.com", preferredLocale: "ro" };
    purchaseTemplateMock.mockReturnValue({
      subject: "Cineva a cumpărat un cadou din lista ta!",
      html: "<p>ro</p>",
      text: "ro",
    });

    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    const fcmCall = sendEachForMulticastMock.mock.calls[0][0];
    expect(fcmCall.notification.title).toBe("Cadoul tău a fost cumpărat! 🎁");

    const templateCall = purchaseTemplateMock.mock.calls[0];
    expect(templateCall[1]).toBe("ro");
  });

  it("Test 7 (stale token cleanup): one UNREGISTERED response → that token deleted from fcmTokens", async () => {
    sendEachForMulticastMock.mockResolvedValueOnce({
      responses: [
        { success: true },
        {
          success: false,
          error: { code: "messaging/registration-token-not-registered" },
        },
      ],
    });

    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    // tok-2 (index 1) should be deleted
    const fcmTokensColl = store["users/owner1/fcmTokens"] || {};
    expect(Object.keys(fcmTokensColl)).not.toContain("tok-2");
    expect(Object.keys(fcmTokensColl)).toContain("tok-1");

    // No failures logged for normal stale cleanup
    expect(notificationsFailuresStore).toHaveLength(0);
  });

  it("Test 8 (FCM send failure): sendEachForMulticast rejects → notifications_failures doc written with type=fcm_batch", async () => {
    sendEachForMulticastMock.mockRejectedValueOnce(new Error("fcm-service-down"));

    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    expect(notificationsFailuresStore).toHaveLength(1);
    const failure = notificationsFailuresStore[0] as {
      type: string;
      userId: string;
      itemId: string;
      registryId: string;
      error: string;
    };
    expect(failure.type).toBe("fcm_batch");
    expect(failure.userId).toBe("owner1");
    expect(failure.itemId).toBe("it1");
    expect(failure.registryId).toBe("reg1");
    expect(failure.error).toBe("fcm-service-down");
    // Email still sent despite FCM failure
    expect(sendEmailMock).toHaveBeenCalledTimes(1);
  });

  it("Test 9 (no tokens): no FCM call, email still sent", async () => {
    store["users/owner1/fcmTokens"] = {};

    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" }
    );

    await onPurchaseNotification.run(event as never);

    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
    expect(sendEmailMock).toHaveBeenCalledTimes(1);
    expect(notificationsFailuresStore).toHaveLength(0);
  });

  it("Test 10 (idempotency via sentinel in store): running again with purchaseNotificationSentAt in store exits early", async () => {
    // Simulate the sentinel being written to the store (as would happen after first run)
    store["registries/reg1/items"].it1 = {
      ...store["registries/reg1/items"].it1,
      purchaseNotificationSentAt: new Date(),
    };

    // The after snapshot does NOT have the sentinel (simulates the trigger firing before our write)
    // but the transaction read picks it up from the store
    const event = makeEvent(
      { status: "reserved" },
      { status: "purchased" } // no sentinel in the after snapshot (trigger fired earlier)
    );

    await onPurchaseNotification.run(event as never);

    // Transaction reads the store which has the sentinel → exits; no duplicate sends
    expect(sendEachForMulticastMock).not.toHaveBeenCalled();
    expect(sendEmailMock).not.toHaveBeenCalled();
  });
});
