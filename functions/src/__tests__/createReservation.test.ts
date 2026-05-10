/**
 * Tests for createReservation — emulator-only setTimeout fallback for reservation expiry.
 * Quick task 260510-pdp.
 *
 * These tests prove:
 *   1. When FUNCTIONS_EMULATOR=true and Cloud Tasks enqueue throws, an in-process
 *      setTimeout schedules releaseReservationCore at expiresAtMs.
 *   2. When FUNCTIONS_EMULATOR is unset (production path), the fallback is dormant
 *      even if Cloud Tasks throws.
 *   3. When Cloud Tasks enqueue succeeds, no fallback is scheduled.
 */

const mockCreateTask = jest.fn();

jest.mock("@google-cloud/tasks", () => ({
  CloudTasksClient: jest.fn().mockImplementation(() => ({
    queuePath: () => "projects/p/locations/l/queues/q",
    createTask: mockCreateTask,
  })),
}));

// Spy exposed on releaseReservationCore so we can assert it is (or isn't) invoked
// by the in-process setTimeout fallback. The releaseReservation onTaskDispatched
// wrapper is mocked as a stub since createReservation never calls it directly.
const mockReleaseReservationCore = jest.fn().mockResolvedValue(undefined);
jest.mock("../reservation/releaseReservation", () => ({
  releaseReservationCore: mockReleaseReservationCore,
  releaseReservation: { run: jest.fn() },
}));

// writeNotification is best-effort and not under test here; stub it.
jest.mock("../notifications/writeNotification", () => ({
  writeNotification: jest.fn().mockResolvedValue(undefined),
}));

// Mutable Firestore store (pattern matches releaseReservation.test.ts)
let mockStore: Record<string, Record<string, Record<string, unknown>>> = {};

function resetStore() {
  mockStore = {
    registries: {
      reg1: { ownerId: "owner1", title: "Test Registry" },
    },
    "registries/reg1/items": {
      it1: { status: "available", affiliateUrl: "https://emag.ro/x", title: "Coffee Machine" },
    },
    reservations: {},
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
    doc: (id?: string) => {
      // db.collection("reservations").doc() with no arg must auto-generate an id
      // (matches Firestore behavior used by createReservation when minting reservationRef).
      const docId = id ?? `auto_${Math.random().toString(36).slice(2, 10)}`;
      return makeDocRef(collPath, docId);
    },
    add: async (data: Record<string, unknown>) => {
      const id = `auto_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
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
    delete: () => "__DELETE__",
    serverTimestamp: () => new Date(),
  },
  Timestamp: {
    fromMillis: (ms: number) => ({ seconds: Math.floor(ms / 1000), nanoseconds: 0 }),
    now: () => ({ seconds: Math.floor(Date.now() / 1000), nanoseconds: 0 }),
  },
}));

// GCLOUD_PROJECT is read inside the handler; set it before import so the module
// initializes cleanly (CloudTasksClient is mocked anyway).
process.env.GCLOUD_PROJECT = "test-project";

// Import AFTER mocks
import { createReservation } from "../reservation/createReservation";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeReq(): any {
  return {
    data: {
      registryId: "reg1",
      itemId: "it1",
      giverName: "Test Giver",
      giverEmail: "g@x.com",
      giverId: null,
    },
    auth: null,
    rawRequest: {} as never,
    acceptsStreaming: false,
  };
}

beforeEach(() => {
  resetStore();
  mockCreateTask.mockReset();
  mockReleaseReservationCore.mockReset();
  mockReleaseReservationCore.mockResolvedValue(undefined);
  delete process.env.FUNCTIONS_EMULATOR;
});

afterEach(() => {
  jest.useRealTimers();
  delete process.env.FUNCTIONS_EMULATOR;
});

describe("createReservation — emulator-only setTimeout fallback", () => {
  it("Test 1: schedules setTimeout fallback when FUNCTIONS_EMULATOR=true and enqueue fails", async () => {
    process.env.FUNCTIONS_EMULATOR = "true";
    mockCreateTask.mockRejectedValue(new Error("no Cloud Tasks emulator"));
    jest.useFakeTimers();

    await createReservation.run(makeReq());

    // Before timer fires, releaseReservationCore must not have been called yet.
    expect(mockReleaseReservationCore).not.toHaveBeenCalled();

    // Advance past 30-min expiry; flush the setTimeout callback's promise chain.
    jest.advanceTimersByTime(31 * 60 * 1000);
    await Promise.resolve();
    await Promise.resolve();

    expect(mockReleaseReservationCore).toHaveBeenCalledTimes(1);
    expect(mockReleaseReservationCore).toHaveBeenCalledWith(
      expect.objectContaining({ reservationId: expect.any(String) })
    );
    const callArg = mockReleaseReservationCore.mock.calls[0][0];
    expect(callArg.reservationId).toBeTruthy();
  });

  it("Test 2: does NOT schedule fallback when FUNCTIONS_EMULATOR is unset (production path) even if enqueue fails", async () => {
    delete process.env.FUNCTIONS_EMULATOR;
    mockCreateTask.mockRejectedValue(new Error("real prod failure"));
    jest.useFakeTimers();

    await createReservation.run(makeReq());

    jest.advanceTimersByTime(31 * 60 * 1000);
    await Promise.resolve();
    await Promise.resolve();

    expect(mockReleaseReservationCore).not.toHaveBeenCalled();
  });

  it("Test 3: does NOT schedule fallback when enqueue succeeds (happy production path)", async () => {
    // Even with FUNCTIONS_EMULATOR=true, success path skips the fallback because
    // the catch block never fires.
    process.env.FUNCTIONS_EMULATOR = "true";
    mockCreateTask.mockResolvedValue([{ name: "projects/p/locations/l/queues/q/tasks/t1" }]);
    jest.useFakeTimers();

    await createReservation.run(makeReq());

    jest.advanceTimersByTime(31 * 60 * 1000);
    await Promise.resolve();
    await Promise.resolve();

    expect(mockReleaseReservationCore).not.toHaveBeenCalled();
  });
});
