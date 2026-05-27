/**
 * Phase 17 D-13/D-14/D-45/D-48: unit tests for `checkAndIncrementRateLimit`.
 *
 * Verifies:
 *   - Under-limit calls succeed (first call for a new uid; 5 prior timestamps).
 *   - At-limit (20 prior timestamps within last hour) throws HttpsError
 *     ("resource-exhausted") and does NOT append to the stored list.
 *   - Timestamps older than 1 hour are filtered out before counting.
 *   - HttpsError code is "resource-exhausted" specifically.
 *
 * RED-phase: this file is committed before the implementation; tests MUST fail
 * until `functions/src/discover/rateLimit.ts` is created.
 *
 * Mock strategy (mirrors createReservation.test.ts pattern): in-memory fake
 * Firestore exposing only `collection().doc()` + `runTransaction((tx) => ...)`
 * with `tx.get()` + `tx.set()`. Pure synchronous state — no admin SDK import.
 */
import { checkAndIncrementRateLimit } from "../../discover/rateLimit";
import { HttpsError } from "firebase-functions/v2/https";

interface StoredDoc {
  timestamps?: number[];
  lastWriteAt?: unknown;
}

function makeFakeDb(initial?: { timestamps?: number[] }) {
  let stored: StoredDoc | null = initial ? { ...initial } : null;
  const ref = { _path: "discoverRateLimits/u1" };
  const db = {
    collection: (_: string) => ({ doc: (__: string) => ref }),
    runTransaction: async (cb: (tx: unknown) => Promise<void>) => {
      const tx = {
        get: async (_: unknown) => ({
          exists: stored !== null,
          data: () => stored ?? undefined,
        }),
        set: (_: unknown, data: StoredDoc) => {
          stored = data;
        },
      };
      await cb(tx);
    },
  };
  return { db: db as never, getStored: () => stored };
}

describe("checkAndIncrementRateLimit", () => {
  it("allows the first call for a new uid", async () => {
    const { db, getStored } = makeFakeDb();
    await expect(checkAndIncrementRateLimit(db, "u1")).resolves.toBeUndefined();
    expect(getStored()?.timestamps).toHaveLength(1);
  });

  it("allows a call when 5 prior timestamps are within the window", async () => {
    const now = Date.now();
    const { db, getStored } = makeFakeDb({
      timestamps: [now - 1000, now - 2000, now - 3000, now - 4000, now - 5000],
    });
    await expect(checkAndIncrementRateLimit(db, "u1")).resolves.toBeUndefined();
    expect(getStored()?.timestamps).toHaveLength(6);
  });

  it("rejects the call at exactly 20 prior timestamps", async () => {
    const now = Date.now();
    const ts = Array.from({ length: 20 }, (_, i) => now - i * 1000);
    const { db, getStored } = makeFakeDb({ timestamps: ts });
    await expect(checkAndIncrementRateLimit(db, "u1")).rejects.toBeInstanceOf(HttpsError);
    // Stored array unchanged (transaction did not write).
    expect(getStored()?.timestamps).toHaveLength(20);
  });

  it("filters out timestamps older than 1 hour before counting", async () => {
    const now = Date.now();
    // 20 stale entries (older than 1 hour) — should all be filtered out.
    const staleTs = Array.from({ length: 20 }, (_, i) => now - 3700000 - i * 1000);
    const { db, getStored } = makeFakeDb({ timestamps: staleTs });
    await expect(checkAndIncrementRateLimit(db, "u1")).resolves.toBeUndefined();
    // After cleanup, only the new timestamp remains.
    expect(getStored()?.timestamps).toHaveLength(1);
  });

  it("throws HttpsError with code 'resource-exhausted'", async () => {
    const now = Date.now();
    const ts = Array.from({ length: 20 }, (_, i) => now - i * 1000);
    const { db } = makeFakeDb({ timestamps: ts });
    await expect(checkAndIncrementRateLimit(db, "u1")).rejects.toMatchObject({
      code: "resource-exhausted",
    });
  });
});
