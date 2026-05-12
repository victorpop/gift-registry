/**
 * Tests for releaseReservationCallable (quick-260512-x5d Task 1).
 * Covers: ownership enforcement, Cloud Task cancellation, status guards, error cases.
 *
 * Mock harness mirrors confirmPurchase.test.ts and releaseReservation.test.ts.
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
        giverId: "u1",
        giverEmail: "user1@example.com",
        expiresAt: { seconds: 99999999, nanoseconds: 0 }, // future
        giverName: "User One",
      },
      resGuest: {
        status: "active",
        registryId: "reg1",
        itemId: "it2",
        cloudTaskName: "task/abc",
        giverId: null,
        giverEmail: "guest@example.com",
        expiresAt: { seconds: 99999999, nanoseconds: 0 },
        giverName: "Guest User",
      },
    },
    "registries/reg1/items": {
      it1: { title: "Coffee Machine", status: "reserved" },
      it2: { title: "Blender", status: "reserved" },
    },
    registries: {
      reg1: { title: "Wedding Registry", ownerId: "owner1" },
    },
    mail: {},
    "users/owner1/notifications": {},
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
  Timestamp: {
    now: () => ({ seconds: 9999, nanoseconds: 0 }),
  },
}));

// Import AFTER mocks
import { releaseReservationCallable } from "../reservation/releaseReservation";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeRequest(data: Record<string, unknown>, auth: { uid: string } | null = null): any {
  return {
    data,
    auth: auth ? { uid: auth.uid, token: {} } : null,
    rawRequest: {} as never,
    acceptsStreaming: false,
  };
}

beforeEach(() => {
  resetStore();
  deleteTaskMock.mockReset();
  deleteTaskMock.mockResolvedValue([{}]);
});

describe("releaseReservationCallable", () => {
  it("Test 1 (happy — signed-in owner): releases reservation, deletes Cloud Task, returns {success:true}", async () => {
    const result = await releaseReservationCallable.run(
      makeRequest({ reservationId: "res1" }, { uid: "u1" })
    );

    expect(result).toEqual({ success: true });
    expect(mockStore.reservations.res1.status).toBe("expired");
    expect(mockStore["registries/reg1/items"].it1.status).toBe("available");
    expect(deleteTaskMock).toHaveBeenCalledTimes(1);
    expect(deleteTaskMock).toHaveBeenCalledWith({ name: "task/xyz" });
  });

  it("Test 2 (happy — guest owner): guest release with matching email + null giverId succeeds", async () => {
    const result = await releaseReservationCallable.run(
      makeRequest({ reservationId: "resGuest", giverEmail: "guest@example.com" }, null)
    );

    expect(result).toEqual({ success: true });
    expect(mockStore.reservations.resGuest.status).toBe("expired");
    expect(deleteTaskMock).toHaveBeenCalledWith({ name: "task/abc" });
  });

  it("Test 3 (signed-in wrong uid): throws permission-denied RELEASE_NOT_OWNER, reservation stays active", async () => {
    await expect(
      releaseReservationCallable.run(
        makeRequest({ reservationId: "res1" }, { uid: "u_wrong" })
      )
    ).rejects.toMatchObject({
      code: "permission-denied",
      message: "RELEASE_NOT_OWNER",
    });

    expect(mockStore.reservations.res1.status).toBe("active");
    expect(deleteTaskMock).not.toHaveBeenCalled();
  });

  it("Test 4 (guest tries signed-in reservation): giverEmail matches but giverId != null → permission-denied", async () => {
    await expect(
      releaseReservationCallable.run(
        makeRequest({ reservationId: "res1", giverEmail: "user1@example.com" }, null)
      )
    ).rejects.toMatchObject({
      code: "permission-denied",
      message: "RELEASE_NOT_OWNER",
    });

    expect(mockStore.reservations.res1.status).toBe("active");
    expect(deleteTaskMock).not.toHaveBeenCalled();
  });

  it("Test 5 (already expired): throws failed-precondition RESERVATION_NOT_ACTIVE", async () => {
    mockStore.reservations.res1 = { ...mockStore.reservations.res1, status: "expired" };

    await expect(
      releaseReservationCallable.run(
        makeRequest({ reservationId: "res1" }, { uid: "u1" })
      )
    ).rejects.toMatchObject({
      code: "failed-precondition",
      message: "RESERVATION_NOT_ACTIVE",
    });
  });

  it("Test 6 (not found): throws not-found RESERVATION_NOT_FOUND", async () => {
    await expect(
      releaseReservationCallable.run(
        makeRequest({ reservationId: "missing" }, { uid: "u1" })
      )
    ).rejects.toMatchObject({
      code: "not-found",
      message: "RESERVATION_NOT_FOUND",
    });
  });

  it("Test 7 (missing reservationId): throws invalid-argument MISSING_RESERVATION_ID", async () => {
    await expect(
      releaseReservationCallable.run(makeRequest({}, { uid: "u1" }))
    ).rejects.toMatchObject({
      code: "invalid-argument",
      message: "MISSING_RESERVATION_ID",
    });
  });

  it("Test 8 (Cloud Task NOT_FOUND, code=5): swallowed, still returns {success:true}", async () => {
    deleteTaskMock.mockRejectedValue(
      Object.assign(new Error("NOT_FOUND"), { code: 5 })
    );

    const result = await releaseReservationCallable.run(
      makeRequest({ reservationId: "res1" }, { uid: "u1" })
    );
    expect(result).toEqual({ success: true });
    expect(mockStore.reservations.res1.status).toBe("expired");
  });

  it("Test 9 (manual release skips not-yet-expired guard): future expiresAt is ignored, status flips to expired", async () => {
    // res1 has expiresAt.seconds = 99999999 (far future); default Timestamp.now().seconds = 9999
    // Without skipNotYetExpiredGuard this would be a no-op in releaseReservationCore
    const result = await releaseReservationCallable.run(
      makeRequest({ reservationId: "res1" }, { uid: "u1" })
    );
    expect(result).toEqual({ success: true });
    // The manual release MUST bypass the not-yet-expired guard
    expect(mockStore.reservations.res1.status).toBe("expired");
  });
});
