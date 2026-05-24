/**
 * Wave 0 RED tests (Plan 16-01) — D-21, D-24 contract for the new
 * `acceptInvite` callable to be implemented in Plan 16-02.
 *
 * Mirrors the mock-firestore harness shape of confirmPurchase.test.ts:
 * - In-memory `mockStore` simulates the few collection paths the function
 *   touches: `registries`, `users/{uid}/notifications`, `notifications_failures`.
 * - `runTransaction` is a synchronous shim — Plan 16-02 uses tx.update with a
 *   FieldPath to delete pending + promote to invited atomically.
 * - admin.auth().getUser() returns a stub displayName for the actor lookup.
 *
 * This file fails to compile today — `../registry/acceptInvite` does not
 * exist. That IS the RED state, flipped GREEN by Plan 16-02.
 *
 * Coverage:
 *   1. Happy path: promotes pending → invited, deletes invite inbox doc,
 *      writes invite_accepted_self (invitee) + invite_accepted (owner).
 *   2. Idempotency: uid already in invitedUsers → no-op success.
 *   3. No pending entry: throws failed-precondition NO_PENDING_INVITE.
 *   4. Unauthenticated: throws unauthenticated MUST_BE_SIGNED_IN.
 *   5. Missing registryId: throws invalid-argument MISSING_REGISTRY_ID.
 *   6. Registry not found: throws not-found REGISTRY_NOT_FOUND.
 */

// Firestore in-memory store — overrideable per test
let mockStore: Record<string, Record<string, Record<string, unknown>>> = {};

function resetStore() {
  mockStore = {
    registries: {
      "reg-happy": {
        ownerId: "owner-1",
        title: "Baby Shower",
        invitedUsers: {},
        pendingInvitedUsers: { "invitee-1": true },
      },
      "reg-already": {
        ownerId: "owner-1",
        title: "Wedding",
        invitedUsers: { "invitee-1": true },
        pendingInvitedUsers: {},
      },
      "reg-no-pending": {
        ownerId: "owner-1",
        title: "Birthday",
        invitedUsers: {},
        pendingInvitedUsers: {},
      },
    },
    "users/invitee-1/notifications": {
      "inv-doc-1": {
        type: "invite",
        payload: { registryId: "reg-happy" },
      },
    },
    "users/owner-1/notifications": {},
    notifications_failures: {},
  };
}

jest.mock("firebase-admin", () => {
  type DocRef = {
    id: string;
    path: string;
    get: () => Promise<{ exists: boolean; id: string; data: () => Record<string, unknown> | undefined }>;
    set: (data: Record<string, unknown>) => Promise<void>;
    update: (dataOrFieldPath: unknown, value?: unknown) => Promise<void>;
    delete: () => Promise<void>;
    collection: (sub: string) => unknown;
    ref?: DocRef;
  };

  const makeDocRef = (collPath: string, docId: string): DocRef => {
    const ref: DocRef = {
      id: docId,
      path: `${collPath}/${docId}`,
      get: async () => {
        const col = mockStore[collPath];
        const data = col ? col[docId] : undefined;
        return { exists: data !== undefined, id: docId, data: () => (data ? { ...data } : undefined) };
      },
      set: async (data: Record<string, unknown>) => {
        if (!mockStore[collPath]) mockStore[collPath] = {};
        mockStore[collPath][docId] = { ...data };
      },
      update: async (dataOrFieldPath: unknown, value?: unknown) => {
        if (!mockStore[collPath]) mockStore[collPath] = {};
        const existing = (mockStore[collPath][docId] as Record<string, unknown>) || {};
        // FieldPath path — { segments: [field, key] }
        if (
          dataOrFieldPath &&
          typeof dataOrFieldPath === "object" &&
          "segments" in (dataOrFieldPath as object)
        ) {
          const fp = dataOrFieldPath as { segments: string[] };
          const field = fp.segments[0];
          const key = fp.segments[1];
          const map = ((existing[field] as Record<string, unknown>) || {}) as Record<string, unknown>;
          if (value === "__DELETE__") {
            delete map[key];
          } else {
            map[key] = value;
          }
          mockStore[collPath][docId] = { ...existing, [field]: map };
        } else {
          mockStore[collPath][docId] = { ...existing, ...(dataOrFieldPath as Record<string, unknown>) };
        }
      },
      delete: async () => {
        if (mockStore[collPath]) delete mockStore[collPath][docId];
      },
      collection: (sub: string) => makeCollRef(`${collPath}/${docId}/${sub}`),
    };
    return ref;
  };

  const makeCollRef = (collPath: string): unknown => {
    const coll = {
      doc: (id: string) => makeDocRef(collPath, id),
      add: async (data: Record<string, unknown>) => {
        const id = `auto_${Date.now()}_${Math.random().toString(36).slice(2)}`;
        if (!mockStore[collPath]) mockStore[collPath] = {};
        mockStore[collPath][id] = { ...data };
        return makeDocRef(collPath, id);
      },
      where: (_field: string, _op: string, _value: unknown) => coll, // chainable no-op for inbox cleanup query
      get: async () => {
        const col = mockStore[collPath] || {};
        const docs = Object.entries(col).map(([id, data]) => {
          const ref = makeDocRef(collPath, id);
          return { id, exists: true, data: () => ({ ...data }), ref };
        });
        return { docs, empty: docs.length === 0, size: docs.length };
      },
    };
    return coll;
  };

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
        update: (ref: { update: (a: unknown, b?: unknown) => void }, a: unknown, b?: unknown) =>
          ref.update(a, b),
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
        if (uid === "invitee-1") return { uid, displayName: "Mara", email: "mara@x.com" };
        if (uid === "owner-1") return { uid, displayName: "Ana", email: "ana@x.com" };
        throw new Error(`User not found: ${uid}`);
      },
    }),
  };
});

jest.mock("firebase-admin/firestore", () => ({
  FieldValue: {
    serverTimestamp: () => new Date(),
    delete: () => "__DELETE__",
  },
  FieldPath: class FakeFieldPath {
    segments: string[];
    constructor(...args: string[]) {
      this.segments = args;
    }
  },
}));

// Import AFTER mocks — RED until Plan 16-02 creates ../registry/acceptInvite
import { acceptInvite } from "../registry/acceptInvite";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeRequest(data: unknown, uid: string | null = "invitee-1"): any {
  return {
    data,
    auth: uid ? { uid, token: {} } : null,
    rawRequest: {} as never,
    acceptsStreaming: false,
  };
}

beforeEach(() => {
  resetStore();
});

describe("acceptInvite callable", () => {
  it("Test 1 (happy path): promotes pending→invited, deletes invite inbox doc, writes self + owner notifications", async () => {
    const result = await acceptInvite.run(makeRequest({ registryId: "reg-happy" }));
    expect(result).toEqual({ success: true });

    const reg = mockStore.registries["reg-happy"] as {
      invitedUsers: Record<string, boolean>;
      pendingInvitedUsers: Record<string, boolean>;
    };
    expect(reg.invitedUsers["invitee-1"]).toBe(true);
    expect(reg.pendingInvitedUsers["invitee-1"]).toBeUndefined();

    // Original INVITE inbox doc deleted
    const inviteeInbox = mockStore["users/invitee-1/notifications"] || {};
    expect(inviteeInbox["inv-doc-1"]).toBeUndefined();

    // invite_accepted_self notification written to invitee
    const inviteeDocs = Object.values(mockStore["users/invitee-1/notifications"] || {});
    const selfNotif = inviteeDocs.find(
      (d) => (d as { type?: string }).type === "invite_accepted_self"
    );
    expect(selfNotif).toBeDefined();

    // invite_accepted notification written to owner
    const ownerDocs = Object.values(mockStore["users/owner-1/notifications"] || {});
    const ownerNotif = ownerDocs.find(
      (d) => (d as { type?: string }).type === "invite_accepted"
    );
    expect(ownerNotif).toBeDefined();
  });

  it("Test 2 (idempotency): uid already in invitedUsers — returns success without writes", async () => {
    const beforeInvitedUsers = { ...(mockStore.registries["reg-already"] as { invitedUsers: Record<string, boolean> }).invitedUsers };
    const beforeOwnerInbox = { ...(mockStore["users/owner-1/notifications"] || {}) };

    const result = await acceptInvite.run(makeRequest({ registryId: "reg-already" }));
    expect(result).toEqual({ success: true });

    // No new notifications written to owner
    expect(mockStore["users/owner-1/notifications"]).toEqual(beforeOwnerInbox);
    // Invited map unchanged
    expect((mockStore.registries["reg-already"] as { invitedUsers: Record<string, boolean> }).invitedUsers).toEqual(beforeInvitedUsers);
  });

  it("Test 3 (no pending entry): throws failed-precondition NO_PENDING_INVITE", async () => {
    await expect(
      acceptInvite.run(makeRequest({ registryId: "reg-no-pending" }))
    ).rejects.toMatchObject({
      code: "failed-precondition",
      message: "NO_PENDING_INVITE",
    });
  });

  it("Test 4 (unauthenticated): throws unauthenticated MUST_BE_SIGNED_IN", async () => {
    await expect(
      acceptInvite.run(makeRequest({ registryId: "reg-happy" }, null))
    ).rejects.toMatchObject({
      code: "unauthenticated",
      message: "MUST_BE_SIGNED_IN",
    });
  });

  it("Test 5 (missing registryId): throws invalid-argument MISSING_REGISTRY_ID", async () => {
    await expect(
      acceptInvite.run(makeRequest({}))
    ).rejects.toMatchObject({
      code: "invalid-argument",
      message: "MISSING_REGISTRY_ID",
    });
  });

  it("Test 6 (registry not found): throws not-found REGISTRY_NOT_FOUND", async () => {
    await expect(
      acceptInvite.run(makeRequest({ registryId: "does-not-exist" }))
    ).rejects.toMatchObject({
      code: "not-found",
      message: "REGISTRY_NOT_FOUND",
    });
  });
});
