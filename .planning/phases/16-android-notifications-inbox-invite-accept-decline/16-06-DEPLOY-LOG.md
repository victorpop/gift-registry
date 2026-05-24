# Phase 16 Plan 16-06 — Deploy Log

**Deploy session:** 2026-05-24 (continuation of plan 16-06 Task 2)
**Operator:** Claude (gsd-executor) executing approved deploy from user session
**Target project:** `gift-registry-ro`
**Functions region:** `europe-west3`

---

## Step 1 — Composite index deploy

**Command:**
```bash
firebase deploy --only firestore:indexes --project gift-registry-ro
```

**Result:** SUCCESS — deployed at 2026-05-24T18:15Z (approx)

**Log file:** `/tmp/16-06-index-deploy.log`

**Output:**
```
=== Deploying to 'gift-registry-ro'...
i  deploying firestore
i  firestore: reading indexes from firestore.indexes.json...
i  cloud.firestore: checking firestore.rules for compilation errors...
✔  cloud.firestore: rules file firestore.rules compiled successfully
i  firestore: deploying indexes...
✔  firestore: deployed indexes in firestore.indexes.json successfully for (default) database
✔  Deploy complete!
```

**Index added:** `notifications` composite — `(type ASC, payload.registryId ASC)` — used by the inbox cleanup query in `acceptInvite` / `declineInvite` post-transaction.

**Index state verification (Pitfall 7):**
- `firebase firestore:indexes --project gift-registry-ro` lists the new composite index without a `state` field.
- CLI behavior: `state` is omitted when an index is READY (CREATING/BUILDING indexes carry the field).
- Empty collection in production → index builds immediately; no polling required this run.

**Decision:** Index confirmed READY → proceeded directly to function deploy.

---

## Step 2 — Cloud Functions build

**Command:**
```bash
cd functions && npm run build
```

**Result:** SUCCESS — `tsc` clean exit, no errors.

---

## Step 3 — Cloud Functions deploy

**Command:**
```bash
firebase deploy --only functions:acceptInvite,functions:declineInvite,functions:inviteToRegistry --project gift-registry-ro
```

**Result:** SUCCESS — all three functions deployed.

**Log file:** `/tmp/16-06-functions-deploy.log`

**Outcomes:**

| Function | Operation | Region | Runtime | Type | Result |
| -------- | --------- | ------ | ------- | ---- | ------ |
| `acceptInvite` | CREATE | europe-west3 | nodejs22 | callable (2nd gen) | ✔ Successful create |
| `declineInvite` | CREATE | europe-west3 | nodejs22 | callable (2nd gen) | ✔ Successful create |
| `inviteToRegistry` | UPDATE | europe-west3 | nodejs22 | callable (2nd gen) | ✔ Successful update |

**Package size:** 284.9 KB
**APIs auto-enabled during deploy:** cloudfunctions, cloudbuild, artifactregistry, cloudtasks, run, eventarc, pubsub, storage, firebaseextensions

---

## Step 4 — Live function verification

**Command:**
```bash
firebase functions:list --project gift-registry-ro
```

(Note: planned `gcloud functions list` failed with `gcloud auth` not active — fell back to `firebase functions:list` which provides the same verification surface. Not a blocker — see "Warnings" below.)

**Log file:** `/tmp/16-06-functions-list.log`

**Result — three target functions live in europe-west3:**

```
│ acceptInvite       │ v2 │ callable │ europe-west3 │ 256 │ nodejs22 │
│ declineInvite      │ v2 │ callable │ europe-west3 │ 256 │ nodejs22 │
│ inviteToRegistry   │ v2 │ callable │ europe-west3 │ 256 │ nodejs22 │
```

---

## Step 5 — Rules deploy SKIPPED (intentional)

Per Plan 16-01 Pattern 8 verification, `firestore.rules` requires NO edit for Phase 16 — the existing `isInvited()` rule works correctly with the strict accept-gate model (invitedUsers checked at read time; pendingInvitedUsers never grants read access). Rules tests cover the contract. No deploy issued.

---

## Warnings (non-blocking)

1. **firebase-functions outdated notice** — Firebase CLI warned: "package.json indicates an outdated version of firebase-functions. Please upgrade using npm install --save firebase-functions@latest in your functions directory." This does NOT block deploy (functions deployed successfully). Logged as a future maintenance todo, not a Phase 16 blocker.

2. **gcloud auth not active** — `gcloud functions list` returned auth error. Verified via `firebase functions:list` instead (same data, different CLI). Acceptance criterion ("functions list contains acceptInvite AND declineInvite") satisfied via the substitute command.

---

## Deploy summary

- **Index:** READY (notifications composite type+payload.registryId)
- **Functions:** 3 deployed to europe-west3 (2 NEW, 1 UPDATED)
- **Rules:** unchanged (intentional, Pattern 8)
- **Production status:** Backend ready for on-device UAT (Task 3)

---

*Generated 2026-05-24 by gsd-executor continuation session.*
