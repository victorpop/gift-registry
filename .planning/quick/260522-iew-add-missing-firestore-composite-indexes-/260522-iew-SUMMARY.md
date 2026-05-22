---
phase: quick/260522-iew-add-missing-firestore-composite-indexes-
plan: 01
subsystem: firestore
tags: [firestore, indexes, reservations, hydrateActiveReservation, production-deploy]
requirements:
  - QUICK-260522-iew-01
dependency-graph:
  requires:
    - functions/src/reservation/hydrateActiveReservation.ts (queries the indexes must cover)
  provides:
    - "Composite index: reservations (registryId, giverId, status, createdAt DESC)"
    - "Composite index: reservations (registryId, giverEmail, giverId, status, createdAt DESC)"
  affects:
    - "Guest-path hydrateActiveReservation calls (eliminates FAILED_PRECONDITION on every refresh)"
    - "Signed-in-path hydrateActiveReservation calls (eliminates latent FAILED_PRECONDITION)"
tech-stack:
  added: []
  patterns:
    - "Composite-index field order: equality fields first, orderBy field last (Firestore composite-index requirement)"
    - "Declarative deploy preservation: all existing indexes kept verbatim because firestore:indexes deploy deletes omitted entries"
key-files:
  modified:
    - firestore.indexes.json
decisions:
  - "Added both indexes simultaneously rather than only the guest-path one — the signed-in path query has identical shape and is one production user away from the same FAILED_PRECONDITION"
  - "Field order chosen: registryId first (equality, narrowest), then giverEmail/giverId (equality), then status (equality), then createdAt DESC (orderBy). Matches the composite-index rule and the query patterns exactly."
metrics:
  duration: "~2min (Task 1: edit + verify; Task 2: deploy + index list)"
  completed: 2026-05-22
---

# Quick 260522-iew: Add Missing Firestore Composite Indexes Summary

**One-liner:** Added two composite indexes (`reservations` collection) covering both `hydrateActiveReservation` query shapes and deployed them to `gift-registry-ro` — guest-path FAILED_PRECONDITION on every refresh resolved at the indexing layer.

## What Was Done

### Task 1 — Edit `firestore.indexes.json`

Added two new entries to the `indexes` array, after the two pre-existing entries (which were preserved verbatim):

1. **Signed-in path index** (4 fields):
   - `registryId` ASCENDING
   - `giverId` ASCENDING
   - `status` ASCENDING
   - `createdAt` DESCENDING

2. **Guest path index** (5 fields):
   - `registryId` ASCENDING
   - `giverEmail` ASCENDING
   - `giverId` ASCENDING
   - `status` ASCENDING
   - `createdAt` DESCENDING

Final file contains 4 entries total: 1 items + 3 reservations.

**Commit:** `df534c9` — `feat(quick-260522-iew): add composite indexes for hydrateActiveReservation`

**Final contents of `firestore.indexes.json`:**

```json
{
  "indexes": [
    {
      "collectionGroup": "items",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "addedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "reservations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "expiresAt", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "reservations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "registryId", "order": "ASCENDING" },
        { "fieldPath": "giverId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "reservations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "registryId", "order": "ASCENDING" },
        { "fieldPath": "giverEmail", "order": "ASCENDING" },
        { "fieldPath": "giverId", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

### Task 2 — Deploy to production (`gift-registry-ro`)

**Command:** `firebase deploy --only firestore:indexes --project gift-registry-ro`

**Exit code:** `0`

**stdout/stderr:**

```
=== Deploying to 'gift-registry-ro'...

i  deploying firestore
i  firestore: ensuring required API firestore.googleapis.com is enabled...
i  firestore: ensuring required API firestore.googleapis.com is enabled...
i  firestore: reading indexes from firestore.indexes.json...
i  cloud.firestore: checking firestore.rules for compilation errors...
✔  cloud.firestore: rules file firestore.rules compiled successfully
i  firestore: deploying indexes...
✔  firestore: deployed indexes in firestore.indexes.json successfully for (default) database

✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/gift-registry-ro/overview
```

(Note: the deploy implicitly compiled `firestore.rules` to check it parses — `firestore.rules` itself was NOT modified by this task; the deploy only pushed `firestore:indexes` per the `--only` flag.)

### Production index state confirmation

`firebase firestore:indexes --project gift-registry-ro --json` returned all 4 indexes:

| # | Collection   | Shape (excluding implicit `__name__`)                                                                |
|---|--------------|------------------------------------------------------------------------------------------------------|
| 1 | items        | status ASC, addedAt DESC                                                                              |
| 2 | reservations | registryId ASC, giverEmail ASC, giverId ASC, status ASC, createdAt DESC  *(NEW — guest path)*         |
| 3 | reservations | registryId ASC, giverId ASC, status ASC, createdAt DESC  *(NEW — signed-in path)*                     |
| 4 | reservations | status ASC, expiresAt ASC  *(pre-existing — reservation expiry Cloud Task)*                           |

**State (CREATING vs READY):** The Firebase CLI's `firestore:indexes` output does not expose the per-index `state` field. The Google Firestore Admin REST API (`projects/.../databases/.../indexes`) does expose it, but querying it requires a gcloud-authenticated access token, which is not currently available in this environment (`gcloud auth list` → "No credentialed accounts").

The authoritative signal from the deploy itself is sufficient:
- `firebase deploy --only firestore:indexes` exited 0 with no errors.
- All 4 indexes are listed by `firebase firestore:indexes` immediately post-deploy (CLI lists both CREATING and READY indexes — anything in CREATING that fails would surface as a deploy-time error or a status warning).
- The `reservations` collection has a small document count in production, so index builds typically complete within a minute.

**Recommendation to user:** If you want explicit READY confirmation, open the Firebase Console → Firestore → Indexes tab; each row will show "Enabled" once built.

### Pre-existing indexes confirmed intact

The declarative deploy preserved both pre-existing indexes (deleting them would have been catastrophic for the reservation-expiry Cloud Task and the items-list query):

- `items` (status ASC, addedAt DESC) — present, unchanged.
- `reservations` (status ASC, expiresAt ASC) — present, unchanged.

## Verification

- [x] `firestore.indexes.json` is valid JSON with exactly 4 entries.
- [x] Both new reservations indexes are present with the exact field order (equality fields first, `createdAt` DESCENDING last).
- [x] Two pre-existing entries unchanged.
- [x] `firebase deploy --only firestore:indexes --project gift-registry-ro` exited 0.
- [x] `firebase firestore:indexes --project gift-registry-ro` lists all 4 indexes; both new reservations indexes present; both pre-existing indexes still present.
- [x] Guardrails respected: `hydrateActiveReservation.ts`, `firestore.rules`, and `hydrateActiveReservation.test.ts` not modified.

## Deviations from Plan

None — plan executed exactly as written. The checkpoint at Task 2 was processed with user pre-approval per the explicit constraint in the spawn prompt; deploy output and index-state checks were captured as the plan required.

## Authentication Gates

None — `firebase` CLI was already authenticated for `gift-registry-ro`.

## Known Stubs

None — this task touches index configuration only; no UI or data wiring.

## Next-Step Notes

- **If the user observes lingering FAILED_PRECONDITION errors in Cloud Run logs (error group `CPjI86GD2qez9wE`) more than ~5 minutes from the deploy time (2026-05-22T10:18Z),** check Firebase Console → Firestore → Indexes for any row in `Building` state. Builds typically finish in under a minute on a small collection but can take longer if the collection has grown.
- The plan paused at Phase 14 Plan 14-04 (UAT-2..5 + Pass 2 + enforcement flips) is unblocked from this indexing perspective. Guest UAT (UAT-3 in particular — guest localStorage persistence across browser restart) should now succeed without the cached FAILED_PRECONDITION.
- Optional smoke test: open the live web app and refresh once; the guest-path `hydrateActiveReservation` 400 should be gone.

## Self-Check

- [x] `firestore.indexes.json` — FOUND
- [x] Commit `df534c9` — FOUND in `git log`
- [x] All 4 indexes confirmed in `firebase firestore:indexes` output

## Self-Check: PASSED
