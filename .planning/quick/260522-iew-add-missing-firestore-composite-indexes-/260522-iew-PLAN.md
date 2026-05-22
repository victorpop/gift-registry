---
phase: quick/260522-iew-add-missing-firestore-composite-indexes-
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - firestore.indexes.json
autonomous: false
requirements:
  - QUICK-260522-iew-01
must_haves:
  truths:
    - "Guest path `hydrateActiveReservation` query no longer throws FAILED_PRECONDITION in europe-west3 / gift-registry-ro"
    - "Signed-in path `hydrateActiveReservation` query no longer throws FAILED_PRECONDITION"
    - "Existing `items` (status+addedAt) and `reservations` (status+expiresAt) indexes remain intact in firestore.indexes.json AND in production"
    - "The two pre-existing indexes in production are NOT deleted by the deploy (firestore deploy is declarative — a missing entry would delete; we preserve both)"
  artifacts:
    - path: "firestore.indexes.json"
      provides: "Composite index definitions for `reservations` collection covering both query shapes plus the pre-existing items + reservations expiry index"
      contains: "registryId, giverEmail, giverId, status, createdAt"
  key_links:
    - from: "firestore.indexes.json"
      to: "functions/src/reservation/hydrateActiveReservation.ts:60-67 (signed-in path)"
      via: "composite index: registryId ASC, giverId ASC, status ASC, createdAt DESC"
      pattern: "where.*registryId.*where.*giverId.*where.*status.*orderBy.*createdAt"
    - from: "firestore.indexes.json"
      to: "functions/src/reservation/hydrateActiveReservation.ts:75-83 (guest path)"
      via: "composite index: registryId ASC, giverEmail ASC, giverId ASC, status ASC, createdAt DESC"
      pattern: "where.*registryId.*where.*giverEmail.*where.*giverId.*where.*status.*orderBy.*createdAt"
---

<objective>
Add the two missing Firestore composite indexes that `hydrateActiveReservation` requires, and deploy
them to production so the guest-path FAILED_PRECONDITION error stops firing on every refresh in
europe-west3 / gift-registry-ro.

Purpose: The function is in production, the queries are correct (intentional defence-in-depth — guests
must scope by `giverId == null` so a guest cannot impersonate a signed-in giver by email), but the
required composite indexes were never declared. Cloud Run logs show the guest-path query erroring on
every web/app refresh (error group `CPjI86GD2qez9wE`, last seen 2026-05-22T09:28:24Z).

Output: Updated `firestore.indexes.json` with two new entries, deployed to gift-registry-ro, both
indexes confirmed building or READY in Firestore.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md
@firestore.indexes.json
@functions/src/reservation/hydrateActiveReservation.ts

<interfaces>
<!-- The two query shapes that need indexes. Extracted from functions/src/reservation/hydrateActiveReservation.ts -->
<!-- Composite-index rule: all equality fields first (any order among themselves is fine for query matching, -->
<!-- but the index definition must list them before the orderBy field), then the orderBy field last. -->

Signed-in path (lines 60-67):
```ts
db.collection("reservations")
  .where("registryId", "==", registryId)
  .where("giverId", "==", uid)
  .where("status", "==", "active")
  .orderBy("createdAt", "desc")
  .limit(1)
```
Required index: registryId ASC, giverId ASC, status ASC, createdAt DESC

Guest path (lines 75-83):
```ts
db.collection("reservations")
  .where("registryId", "==", registryId)
  .where("giverEmail", "==", giverEmail)
  .where("giverId", "==", null)
  .where("status", "==", "active")
  .orderBy("createdAt", "desc")
  .limit(1)
```
Required index: registryId ASC, giverEmail ASC, giverId ASC, status ASC, createdAt DESC

Existing indexes (PRESERVE VERBATIM — declarative deploy will delete entries we omit):
- items: status ASC, addedAt DESC
- reservations: status ASC, expiresAt ASC  (used by the reservation expiry Cloud Task — DO NOT TOUCH)
</interfaces>

<guardrails>
- Do NOT modify `functions/src/reservation/hydrateActiveReservation.ts` — the `giverId == null` clause on the guest path is intentional defence-in-depth.
- Do NOT modify `firestore.rules` — this is purely an indexing fix.
- Do NOT modify `functions/src/reservation/hydrateActiveReservation.test.ts` — emulator tests don't exercise indexes.
- Do NOT change, reorder, or remove the two existing index entries (items / reservations expiry). `firebase deploy --only firestore:indexes` is declarative; omitting an entry deletes the index in production.
- Field order within each new index entry MUST be: equality fields first, orderBy field last (Firestore composite-index requirement). Use the exact orders specified in the interfaces block above.
</guardrails>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add two composite index entries to firestore.indexes.json</name>
  <files>firestore.indexes.json</files>
  <action>
    Edit `firestore.indexes.json` to add two new entries to the `indexes` array, AFTER the existing two entries (preserve the existing `items` and `reservations` expiry entries verbatim — do not reorder, rename, or alter their field order).

    Add these two new entries to the `reservations` collection group:

    1. Signed-in path index — fields in this exact order:
       - `registryId` ASCENDING
       - `giverId` ASCENDING
       - `status` ASCENDING
       - `createdAt` DESCENDING

    2. Guest path index — fields in this exact order:
       - `registryId` ASCENDING
       - `giverEmail` ASCENDING
       - `giverId` ASCENDING
       - `status` ASCENDING
       - `createdAt` DESCENDING

    Both entries use `"collectionGroup": "reservations"` and `"queryScope": "COLLECTION"`, matching the style of the existing reservations expiry entry.

    Keep `fieldOverrides: []` unchanged.

    After saving, the file should have 4 entries in `indexes`: items (status+addedAt), reservations (status+expiresAt, EXISTING — do not touch), reservations (signed-in NEW), reservations (guest NEW).
  </action>
  <verify>
    <automated>node -e "const j=JSON.parse(require('fs').readFileSync('firestore.indexes.json','utf8')); if(j.indexes.length!==4) throw new Error('expected 4 indexes, got '+j.indexes.length); const r=j.indexes.filter(i=>i.collectionGroup==='reservations'); if(r.length!==3) throw new Error('expected 3 reservations indexes, got '+r.length); const signedIn=r.find(i=>i.fields.length===4 && i.fields[0].fieldPath==='registryId' && i.fields[1].fieldPath==='giverId' && i.fields[2].fieldPath==='status' && i.fields[3].fieldPath==='createdAt' && i.fields[3].order==='DESCENDING'); if(!signedIn) throw new Error('signed-in index missing or wrong field order'); const guest=r.find(i=>i.fields.length===5 && i.fields[0].fieldPath==='registryId' && i.fields[1].fieldPath==='giverEmail' && i.fields[2].fieldPath==='giverId' && i.fields[3].fieldPath==='status' && i.fields[4].fieldPath==='createdAt' && i.fields[4].order==='DESCENDING'); if(!guest) throw new Error('guest index missing or wrong field order'); const expiry=r.find(i=>i.fields.length===2 && i.fields[0].fieldPath==='status' && i.fields[1].fieldPath==='expiresAt'); if(!expiry) throw new Error('expiry index was removed or altered'); console.log('OK: 4 indexes, both new reservations indexes present, expiry index intact');"</automated>
  </verify>
  <done>
    `firestore.indexes.json` is valid JSON, contains 4 indexes total, both new reservations indexes are present with the exact field order specified, and the two pre-existing entries (items, reservations-expiry) are unchanged.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Deploy indexes to production and confirm build state</name>
  <what-built>
    Two new composite indexes declared in `firestore.indexes.json` for the `reservations` collection, covering both query shapes inside `hydrateActiveReservation`.
  </what-built>
  <how-to-verify>
    Claude will run the following automated steps BEFORE handing back to you. The checkpoint exists because this is a production deploy of shared infrastructure — you should review the deploy output before declaring the task done.

    Claude's automated steps:

    1. Run the deploy and capture full output:
       ```
       firebase deploy --only firestore:indexes --project gift-registry-ro
       ```
       Report stdout, stderr, and the exit code. Successful deploys exit 0 and typically print "indexes are being created" or "indexes already exist".

    2. List current indexes (machine-readable form) and report state of the two new ones:
       ```
       firebase firestore:indexes --project gift-registry-ro
       ```
       Look for the two new reservations indexes; each will show state `CREATING` (still building) or `READY` (done).

    3. If either new index is still `CREATING`, poll up to 3 times with 60-second sleeps between polls. After the third poll, stop and report the final state per index — do NOT block on it forever. Index builds on an empty / small collection are usually under a minute; on populated collections they can take several minutes. Either outcome is acceptable for this checkpoint as long as the deploy itself succeeded (exit 0, no errors).

    4. As a smoke test (only if both indexes are `READY`), suggest the user open the live web app and refresh once — the guest-path `hydrateActiveReservation` 400/FAILED_PRECONDITION should be gone. This is optional and the user can defer it; the index `READY` state is the authoritative signal.

    Your job at the checkpoint:
    - Confirm Claude reported `firebase deploy` exited 0 with no errors / warnings about the deploy itself.
    - Confirm Claude reported both new indexes exist in `firebase firestore:indexes` output (state `CREATING` or `READY` both acceptable).
    - Confirm Claude reported the two pre-existing indexes (items status+addedAt, reservations status+expiresAt) are still present in `firebase firestore:indexes` output — proving the declarative deploy did not delete them.
  </how-to-verify>
  <resume-signal>Type "approved" once you've reviewed Claude's deploy report, or describe what looks wrong.</resume-signal>
</task>

</tasks>

<verification>
- `firestore.indexes.json` parses as valid JSON.
- File contains exactly 4 index entries: 1 items + 3 reservations (expiry-existing, signed-in-new, guest-new).
- Field order within each new entry matches the composite-index rule (equality fields then orderBy DESCENDING last).
- `firebase deploy --only firestore:indexes --project gift-registry-ro` exits 0.
- `firebase firestore:indexes --project gift-registry-ro` lists all 4 indexes; the two new reservations indexes are in `CREATING` or `READY` state; the two pre-existing indexes (items+addedAt, reservations+expiresAt) are still present and unchanged.
</verification>

<success_criteria>
- Both new composite indexes exist in `firestore.indexes.json` with the exact field order specified.
- Production deploy succeeded (exit 0, no deploy errors).
- Production index list confirms presence of both new indexes plus the two pre-existing ones.
- (Eventual / not blocking) Guest-path `hydrateActiveReservation` calls no longer return FAILED_PRECONDITION once Firestore finishes building the indexes.
</success_criteria>

<output>
After completion, create `.planning/quick/260522-iew-add-missing-firestore-composite-indexes-/260522-iew-SUMMARY.md` documenting:
- The final contents of `firestore.indexes.json` (4 entries)
- The `firebase deploy` stdout/stderr and exit code
- The state of each of the two new indexes at the time of the final poll (CREATING or READY)
- Confirmation that the two pre-existing indexes are still in production
- Any next-step note if indexes were still `CREATING` at handoff (user should monitor in Firebase Console)
</output>
