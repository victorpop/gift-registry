/**
 * Wave 0 RED tests (Plan 16-01) — D-22, D-24 contract for the new
 * `declineInvite` callable to be implemented in Plan 16-02.
 *
 * Mirrors acceptInvite.test.ts harness — same mock firestore shim with
 * FieldPath-aware update. Decline differs from accept in three ways:
 *   - removes the uid from pendingInvitedUsers WITHOUT touching invitedUsers
 *   - writes ONLY the owner-side notification (invite_declined). No
 *     invitee-side "you declined" notification — by design (D-22).
 *   - still deletes the original INVITE inbox doc for the invitee.
 *
 * Idempotency contract: invoking decline on a registry where uid is not in
 * pending AND not in invited returns success without writes (matches the
 * "no-op when already done" pattern). Plan 16-02 may instead choose to
 * throw failed-precondition; tests assert one or the other — we go with
 * the explicit failed-precondition path to match acceptInvite for symmetry.
 *
 * This file fails to compile today — `../registry/declineInvite` does not
 * exist. That IS the RED state, flipped GREEN by Plan 16-02.
 */

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
  };

  const makeDocRef = (collPath: string, docId: string): DocRef => ({
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
  });

  const makeCollRef = (collPath: string): unknown => {
    const coll = {
      doc: (id: string) => makeDocRef(collPath, id),
      add: async (data: Record<string, unknown>) => {
        const id = `auto_${Date.now()}_${Math.random().toString(36).slice(2)}`;
        if (!mockStore[collPath]) mockStore[collPath] = {};
        mockStore[collPath][id] = { ...data };
        return makeDocRef(collPath, id);
      },
      where: (_f: string, _o: string, _v: unknown) => coll,
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

// Import AFTER mocks — RED until Plan 16-02 creates ../registry/declineInvite
import { declineInvite } from "../registry/declineInvite";

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

describe("declineInvite callable", () => {
  it("Test 1 (happy path): removes pending entry, leaves invitedUsers untouched, writes ONLY owner notification", async () => {
    const result = await declineInvite.run(makeRequest({ registryId: "reg-happy" }));
    expect(result).toEqual({ success: true });

    const reg = mockStore.registries["reg-happy"] as {
      invitedUsers: Record<string, boolean>;
      pendingInvitedUsers: Record<string, boolean>;
    };
    expect(reg.pendingInvitedUsers["invitee-1"]).toBeUndefined();
    // CRITICAL: invitedUsers must NOT have been touched
    expect(reg.invitedUsers["invitee-1"]).toBeUndefined();

    // Original INVITE inbox doc deleted
    const inviteeInbox = mockStore["users/invitee-1/notifications"] || {};
    expect(inviteeInbox["inv-doc-1"]).toBeUndefined();

    // No invitee-side notification — decline does NOT write a "you declined" inbox entry
    const inviteeDocs = Object.values(mockStore["users/invitee-1/notifications"] || {});
    const selfNotif = inviteeDocs.find(
      (d) => (d as { type?: string }).type === "invite_declined"
    );
    expect(selfNotif).toBeUndefined();

    // Owner-side invite_declined notification written
    const ownerDocs = Object.values(mockStore["users/owner-1/notifications"] || {});
    const ownerNotif = ownerDocs.find(
      (d) => (d as { type?: string }).type === "invite_declined"
    );
    expect(ownerNotif).toBeDefined();
  });

  it("Test 2 (idempotency): uid not in pending and not in invited — throws failed-precondition NO_PENDING_INVITE", async () => {
    await expect(
      declineInvite.run(makeRequest({ registryId: "reg-no-pending" }))
    ).rejects.toMatchObject({
      code: "failed-precondition",
      message: "NO_PENDING_INVITE",
    });
  });

  it("Test 3 (unauthenticated): throws unauthenticated MUST_BE_SIGNED_IN", async () => {
    await expect(
      declineInvite.run(makeRequest({ registryId: "reg-happy" }, null))
    ).rejects.toMatchObject({
      code: "unauthenticated",
      message: "MUST_BE_SIGNED_IN",
    });
  });

  it("Test 4 (missing registryId): throws invalid-argument MISSING_REGISTRY_ID", async () => {
    await expect(
      declineInvite.run(makeRequest({}))
    ).rejects.toMatchObject({
      code: "invalid-argument",
      message: "MISSING_REGISTRY_ID",
    });
  });

  it("Test 5 (registry not found): throws not-found REGISTRY_NOT_FOUND", async () => {
    await expect(
      declineInvite.run(makeRequest({ registryId: "does-not-exist" }))
    ).rejects.toMatchObject({
      code: "not-found",
      message: "REGISTRY_NOT_FOUND",
    });
  });
});
