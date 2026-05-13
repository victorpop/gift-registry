/**
 * Tests for hydrateActiveReservation callable (quick-260512-x5d Task 1).
 * Covers: signed-in and guest query paths, defence-in-depth, empty results, error cases.
 *
 * Mock harness mirrors confirmPurchase.test.ts extended with chainable query support.
 */

// Mutable store — overrideable per test
let mockStore: Record<string, Record<string, Record<string, unknown>>> = {};

// Query filter state — chain accumulates constraints, .get() applies them
type WhereConstraint = { field: string; op: string; value: unknown };

function resetStore() {
  mockStore = {
    reservations: {
      res1: {
        status: "active",
        registryId: "reg1",
        itemId: "it1",
        giverId: "u1",
        giverEmail: "user1@example.com",
        affiliateUrl: "https://emag.ro/item1",
        expiresAt: { toMillis: () => 99999999000 },
        createdAt: { seconds: 1000 },
        giverName: "User One",
        cloudTaskName: "task/xyz",
      },
      resGuest: {
        status: "active",
        registryId: "reg1",
        itemId: "it2",
        giverId: null,
        giverEmail: "guest@example.com",
        affiliateUrl: "https://emag.ro/item2",
        expiresAt: { toMillis: () => 88888888000 },
        createdAt: { seconds: 900 },
        giverName: "Guest User",
        cloudTaskName: "task/abc",
      },
    },
    "registries/reg1/items": {
      it1: { title: "Coffee Machine", merchantDomain: "emag.ro", status: "reserved" },
      it2: { title: "Blender", merchantDomain: "emag.ro", status: "reserved" },
    },
    registries: {
      reg1: { title: "Wedding Registry", ownerId: "owner1" },
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
      return {
        exists: data !== undefined,
        id: docId,
        data: () => (data ? { ...data } : undefined),
      };
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

  /**
   * Chainable query builder: accumulates where/orderBy/limit constraints.
   * .get() filters the in-memory store and returns matching docs sorted/limited.
   */
  const makeQueryRef = (
    collPath: string,
    constraints: WhereConstraint[],
    limitN: number | null,
    _orderByField: string | null
  ): unknown => ({
    where: (field: string, op: string, value: unknown) =>
      makeQueryRef(collPath, [...constraints, { field, op, value }], limitN, _orderByField),
    orderBy: (field: string) =>
      makeQueryRef(collPath, constraints, limitN, field),
    limit: (n: number) =>
      makeQueryRef(collPath, constraints, n, _orderByField),
    get: async () => {
      const col = mockStore[collPath] || {};
      let docs = Object.entries(col).map(([id, data]) => ({
        id,
        exists: true,
        data: () => ({ ...data }),
        ref: makeDocRef(collPath, id),
      }));

      // Apply where constraints
      for (const { field, op, value } of constraints) {
        docs = docs.filter((doc) => {
          const docData = doc.data() as Record<string, unknown>;
          if (op === "==") {
            if (value === null) return docData[field] === null || docData[field] === undefined;
            return docData[field] === value;
          }
          return true;
        });
      }

      // Apply orderBy (descending createdAt by id for simplicity)
      if (_orderByField) {
        docs.sort((a, b) => {
          const av = (a.data() as Record<string, unknown>)[_orderByField];
          const bv = (b.data() as Record<string, unknown>)[_orderByField];
          const as = typeof av === "object" && av !== null ? (av as { seconds: number }).seconds : 0;
          const bs = typeof bv === "object" && bv !== null ? (bv as { seconds: number }).seconds : 0;
          return bs - as; // desc
        });
      }

      // Apply limit
      if (limitN !== null) docs = docs.slice(0, limitN);

      return { docs, empty: docs.length === 0, size: docs.length };
    },
  });

  const makeCollRef = (collPath: string): unknown => ({
    doc: (id: string) => makeDocRef(collPath, id),
    where: (field: string, op: string, value: unknown) =>
      makeQueryRef(collPath, [{ field, op, value }], null, null),
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
import { hydrateActiveReservation } from "../reservation/hydrateActiveReservation";

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
});

describe("hydrateActiveReservation callable", () => {
  it("Test 10 (happy — signed-in): returns active reservation with item details", async () => {
    const result = await hydrateActiveReservation.run(
      makeRequest({ registryId: "reg1" }, { uid: "u1" })
    );

    expect(result).toMatchObject({
      active: {
        reservationId: "res1",
        itemId: "it1",
        itemName: "Coffee Machine",
        affiliateUrl: "https://emag.ro/item1",
        merchantDomain: "emag.ro",
        expiresAtMs: 99999999000,
      },
    });
  });

  it("Test 11 (happy — guest): returns active reservation for guest by email", async () => {
    const result = await hydrateActiveReservation.run(
      makeRequest({ registryId: "reg1", giverEmail: "guest@example.com" }, null)
    );

    expect(result).toMatchObject({
      active: {
        reservationId: "resGuest",
        itemId: "it2",
        itemName: "Blender",
        affiliateUrl: "https://emag.ro/item2",
        merchantDomain: "emag.ro",
        expiresAtMs: 88888888000,
      },
    });
  });

  it("Test 12 (signed-in ignores payload.giverEmail): uses auth.uid only, not giverEmail from payload", async () => {
    // If signed-in attacker passes guest@example.com, should only match res1 (uid=u1), not resGuest
    const result = await hydrateActiveReservation.run(
      makeRequest({ registryId: "reg1", giverEmail: "guest@example.com" }, { uid: "u1" })
    );

    // Should return res1 (signed-in user u1's reservation), ignoring giverEmail param
    expect(result).toMatchObject({
      active: {
        reservationId: "res1",
        itemId: "it1",
      },
    });
  });

  it("Test 13 (empty reservation.affiliateUrl, item has originalUrl): returns active with affiliateUrl = item.originalUrl", async () => {
    mockStore.reservations.res1 = { ...mockStore.reservations.res1, affiliateUrl: "" };
    mockStore["registries/reg1/items"].it1 = {
      ...mockStore["registries/reg1/items"].it1,
      originalUrl: "https://ikea.com/p/lack",
    };

    const result = await hydrateActiveReservation.run(
      makeRequest({ registryId: "reg1" }, { uid: "u1" })
    );

    expect(result).toMatchObject({
      active: {
        reservationId: "res1",
        itemId: "it1",
        itemName: "Coffee Machine",
        affiliateUrl: "https://ikea.com/p/lack",
        merchantDomain: "emag.ro",
        expiresAtMs: 99999999000,
      },
    });
  });

  it("Test 13b (empty reservation.affiliateUrl AND empty item.originalUrl): returns active with affiliateUrl = ''", async () => {
    mockStore.reservations.res1 = { ...mockStore.reservations.res1, affiliateUrl: "" };
    expect(mockStore["registries/reg1/items"].it1.originalUrl).toBeUndefined();

    const result = await hydrateActiveReservation.run(
      makeRequest({ registryId: "reg1" }, { uid: "u1" })
    );

    expect(result.active).not.toBeNull();
    expect(result.active).toMatchObject({
      reservationId: "res1",
      itemId: "it1",
      itemName: "Coffee Machine",
      affiliateUrl: "",
      merchantDomain: "emag.ro",
    });
  });

  it("Test 14 (no registryId): throws invalid-argument MISSING_REGISTRY_ID", async () => {
    await expect(
      hydrateActiveReservation.run(makeRequest({}, { uid: "u1" }))
    ).rejects.toMatchObject({
      code: "invalid-argument",
      message: "MISSING_REGISTRY_ID",
    });
  });

  it("Test 15 (guest with no giverEmail): throws invalid-argument MISSING_GIVER_EMAIL", async () => {
    await expect(
      hydrateActiveReservation.run(makeRequest({ registryId: "reg1" }, null))
    ).rejects.toMatchObject({
      code: "invalid-argument",
      message: "MISSING_GIVER_EMAIL",
    });
  });

  it("Test 16 (no active reservation for user): returns {active: null}", async () => {
    // User with no active reservations
    const result = await hydrateActiveReservation.run(
      makeRequest({ registryId: "reg1" }, { uid: "u_no_reservation" })
    );

    expect(result).toEqual({ active: null });
  });
});
