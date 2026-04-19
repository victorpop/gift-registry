/**
 * Tests for confirmPurchase callable (Plan 06-02, Task 1).
 * Covers: D-01/D-02 transaction, Cloud Task cancellation, expired reservation,
 * not-found, missing arg, NOT_FOUND swallow, other error swallow, no task name.
 */

const deleteTaskMock = jest.fn();

jest.mock("@google-cloud/tasks", () => ({
  CloudTasksClient: jest.fn().mockImplementation(() => ({
    deleteTask: deleteTaskMock,
    queuePath: () => "",
  })),
}));

// Mutable store — overrideable per test
let mockStore: Record<string, Record<string, Record<string, unknown>>> = {};

function resetStore() {
  mockStore = {
    reservations: {
      res1: {
        status: "active",
        registryId: "reg1",
        itemId: "it1",
        cloudTaskName: "task/xyz",
      },
    },
    "registries/reg1/items": {
      it1: { status: "reserved" },
    },
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
      const id = `auto_${Date.now()}`;
      if (!mockStore[collPath]) mockStore[collPath] = {};
      mockStore[collPath][id] = { ...data };
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
    serverTimestamp: () => new Date(),
    delete: () => "__DELETE__",
  },
}));

// Import AFTER mocks
import { confirmPurchase } from "../reservation/confirmPurchase";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeRequest(reservationId: string): any {
  return {
    data: { reservationId },
    auth: null,
    rawRequest: {} as never,
    acceptsStreaming: false,
  };
}

beforeEach(() => {
  resetStore();
  deleteTaskMock.mockReset();
  deleteTaskMock.mockResolvedValue([{}]);
});

describe("confirmPurchase callable", () => {
  it("Test 1 (happy path): returns {success:true}, updates reservation+item to purchased, cancels Cloud Task", async () => {
    const result = await confirmPurchase.run(makeRequest("res1"));

    expect(result).toEqual({ success: true });

    // Reservation should be marked purchased
    expect(mockStore.reservations.res1.status).toBe("purchased");
    // Item should be marked purchased
    expect(mockStore["registries/reg1/items"].it1.status).toBe("purchased");

    // Cloud Task deleted exactly once with the stored name
    expect(deleteTaskMock).toHaveBeenCalledTimes(1);
    expect(deleteTaskMock).toHaveBeenCalledWith({ name: "task/xyz" });
  });

  it("Test 2 (already expired): throws HttpsError failed-precondition RESERVATION_EXPIRED", async () => {
    mockStore.reservations.res1 = {
      ...mockStore.reservations.res1,
      status: "expired",
    };

    await expect(confirmPurchase.run(makeRequest("res1"))).rejects.toMatchObject({
      code: "failed-precondition",
      message: "RESERVATION_EXPIRED",
    });
  });

  it("Test 3 (not found): throws HttpsError not-found RESERVATION_NOT_FOUND", async () => {
    await expect(confirmPurchase.run(makeRequest("missing"))).rejects.toMatchObject({
      code: "not-found",
      message: "RESERVATION_NOT_FOUND",
    });
  });

  it("Test 4 (missing reservationId): throws HttpsError invalid-argument MISSING_RESERVATION_ID", async () => {
    await expect(confirmPurchase.run(makeRequest(""))).rejects.toMatchObject({
      code: "invalid-argument",
      message: "MISSING_RESERVATION_ID",
    });
  });

  it("Test 5 (Cloud Task NOT_FOUND): swallows code=5 error, still returns {success:true}", async () => {
    deleteTaskMock.mockRejectedValue(
      Object.assign(new Error("NOT_FOUND"), { code: 5 })
    );

    const result = await confirmPurchase.run(makeRequest("res1"));
    expect(result).toEqual({ success: true });
    // Reservation still marked purchased
    expect(mockStore.reservations.res1.status).toBe("purchased");
  });

  it("Test 6 (Cloud Task other error): swallows arbitrary error, still returns {success:true}", async () => {
    deleteTaskMock.mockRejectedValue(new Error("NETWORK_ERROR"));

    const result = await confirmPurchase.run(makeRequest("res1"));
    expect(result).toEqual({ success: true });
    // Reservation still marked purchased
    expect(mockStore.reservations.res1.status).toBe("purchased");
  });

  it("Test 7 (no cloudTaskName stored): deleteTask NOT called, handler succeeds", async () => {
    mockStore.reservations.res1 = {
      ...mockStore.reservations.res1,
      cloudTaskName: "",
    };

    const result = await confirmPurchase.run(makeRequest("res1"));
    expect(result).toEqual({ success: true });
    expect(deleteTaskMock).not.toHaveBeenCalled();
  });
});
