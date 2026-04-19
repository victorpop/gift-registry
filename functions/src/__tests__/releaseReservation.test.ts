/**
 * Tests for releaseReservation — stub replacement with email delivery.
 * Task 2, Plan 06-01.
 */

const mailStore: Record<string, unknown>[] = [];
let shouldMailThrow = false;

// Mutable store so we can vary data per test
let mockStore: Record<string, Record<string, Record<string, unknown>>> = {};

function resetStore() {
  mockStore = {
    reservations: {
      res1: {
        status: "active",
        expiresAt: { seconds: 1 }, // expiresAt in the past (seconds=1 << Timestamp.now()=9999)
        registryId: "reg1",
        itemId: "it1",
        giverEmail: "giver@x.com",
        cloudTaskName: "",
      },
    },
    "registries/reg1/items": {
      it1: { title: "Coffee Machine", status: "reserved" },
    },
    registries: {
      reg1: { title: "Wedding Registry", ownerId: "owner1" },
    },
    mail: {},
  };
}

jest.mock("firebase-admin", () => {
  const makeDocRef = (collPath: string, docId: string): unknown => ({
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
    update: async (data: Record<string, unknown>) => {
      if (!mockStore[collPath]) mockStore[collPath] = {};
      mockStore[collPath][docId] = { ...(mockStore[collPath][docId] || {}), ...data };
    },
    delete: async () => {
      if (mockStore[collPath]) delete mockStore[collPath][docId];
    },
    collection: (sub: string) => makeCollRef(`${collPath}/${docId}/${sub}`),
  });

  const makeCollRef = (collPath: string): unknown => ({
    doc: (id: string) => makeDocRef(collPath, id),
    add: async (data: Record<string, unknown>) => {
      if (collPath === "mail" && shouldMailThrow) {
        throw new Error("mail/write-failed");
      }
      const id = `auto_${Date.now()}`;
      if (!mockStore[collPath]) mockStore[collPath] = {};
      mockStore[collPath][id] = { ...data };
      if (collPath === "mail") mailStore.push({ ...data });
      return makeDocRef(collPath, id);
    },
    get: async () => {
      const col = mockStore[collPath] || {};
      const docs = Object.entries(col).map(([id, data]) => ({
        id,
        exists: true,
        data: () => ({ ...data }),
        ref: makeDocRef(collPath, id),
      }));
      return { docs, empty: docs.length === 0, size: docs.length };
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
  };

  return {
    __esModule: true,
    initializeApp: jest.fn(),
    firestore: () => fakeDb,
  };
});

jest.mock("firebase-admin/firestore", () => ({
  FieldValue: {
    delete: () => "__DELETE__",
    serverTimestamp: () => new Date(),
  },
  Timestamp: {
    now: () => ({ seconds: 9999, nanoseconds: 0 }),
  },
}));

// Import AFTER mocks
import { releaseReservation } from "../reservation/releaseReservation";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeRequest(reservationId: string): any {
  return { data: { reservationId } };
}

beforeEach(() => {
  resetStore();
  mailStore.length = 0;
  shouldMailThrow = false;
});

describe("releaseReservation (with email)", () => {
  it("Test A: writes mail doc with correct subject when reservation expires", async () => {
    await releaseReservation.run(makeRequest("res1"));

    expect(mailStore).toHaveLength(1);
    const doc = mailStore[0] as { to: string; message: { subject: string; html: string } };
    expect(doc.to).toBe("giver@x.com");
    expect(doc.message.subject).toMatch(/^Your reservation for/);
    expect(doc.message.subject).toContain("Coffee Machine");
  });

  it("Test B: when reservation is already expired, no mail doc written", async () => {
    // Override reservation to already be expired
    mockStore.reservations.res1 = {
      ...mockStore.reservations.res1,
      status: "expired",
    };

    await releaseReservation.run(makeRequest("res1"));

    expect(mailStore).toHaveLength(0);
  });

  it("Test C: when itemName is missing, fallback 'your gift' appears in email subject", async () => {
    // Remove item title
    mockStore["registries/reg1/items"].it1 = { status: "reserved" };

    await releaseReservation.run(makeRequest("res1"));

    expect(mailStore).toHaveLength(1);
    const doc = mailStore[0] as { message: { subject: string } };
    expect(doc.message.subject).toContain("your gift");
  });

  it("Test D: if sendEmail throws, handler does NOT throw — logs and returns", async () => {
    shouldMailThrow = true;

    // Should not throw even though mail write fails
    await expect(releaseReservation.run(makeRequest("res1"))).resolves.toBeUndefined();
  });
});
