---
phase: quick-260605-h1d
plan: 01
subsystem: firestore-security-rules
tags: [firestore-rules, invites, pending-invitees, security]
requires:
  - "pendingInvitedUsers map on registry docs (Phase 16 D-23)"
  - "invite-response sheet live registry fetch (quick 260605-ge6)"
provides:
  - "isPendingInvited() rule helper granting registry-DOC read to pending invitees"
  - "D-18 rules unit tests encoding the pending-read-doc / no-items policy"
affects:
  - "Registry document read access for signed-in pending invitees on private registries"
tech-stack:
  added: []
  patterns:
    - "Defensive registryData.get(key, default) helper mirroring isInvited()"
    - "Scope a permission grant to one read rule via an OR clause, not by editing canReadRegistry()"
key-files:
  created: []
  modified:
    - firestore.rules
    - tests/rules/firestore.rules.test.ts
decisions:
  - "Loosen Firestore rules (over a payload / Cloud-Functions change) so the invite-response sheet can fetch the live registry doc for pending invitees."
  - "Grant the new permission via a dedicated isPendingInvited() helper ORed onto the registry-DOC read rule ONLY — items subcollection read stays on canReadRegistry() so pending invitees still cannot read items."
metrics:
  duration: ~6min
  completed: 2026-06-05
---

# Quick 260605-h1d: Allow pending invitees to read the registry doc — Summary

Pending registry invitees (signed-in, private registry, `pendingInvitedUsers[uid]==true`)
can now read the registry DOCUMENT — enabling the invite-response bottom sheet's live
fetch of description/location/date — while the items subcollection stays locked to
post-accept invitees, owners, and public registries.

## What changed

- **`firestore.rules`**: Added a new `isPendingInvited(registryData)` helper next to
  `isInvited()` that checks `pendingInvitedUsers[request.auth.uid] == true` on a private
  registry. ORed it onto the registry-DOC read rule only:
  `allow read: if canReadRegistry(resource.data) || isPendingInvited(resource.data);`
  `canReadRegistry()` and the items read rule (line 56) are byte-identical to before —
  the diff is exactly the new 13-line function plus the single OR on the doc-read rule.
- **`tests/rules/firestore.rules.test.ts`**: Rewrote the D-18 describe block to encode the
  new policy.

## Code-trace of all four rule cases (registry-DOC read)

1. **Pending invitee** (private, `pendingInvitedUsers[uid]==true`, not in `invitedUsers`):
   - DOC read → `canReadRegistry` false (isOwner=false, isPublic=false, isInvited=false)
     but `isPendingInvited` true → **SUCCEEDS**. ✓
   - Items read → `canReadRegistry` only (isPendingInvited NOT applied to items) → all
     three sub-checks false → **FAILS**. ✓
2. **True outsider** (in neither `invitedUsers` nor `pendingInvitedUsers`):
   - DOC read → `canReadRegistry` false AND `isPendingInvited` false → **FAILS**. ✓
3. **Owner** (`ownerId == uid`):
   - DOC read → `canReadRegistry` → `isOwner` true → **SUCCEEDS** (unchanged). ✓
4. **Post-accept invitee** (`invitedUsers[uid]==true`):
   - DOC read → `canReadRegistry` → `isInvited` true → **SUCCEEDS** (unchanged). ✓

## Tests updated (D-18 block)

- Header comment rewritten to state the new loosened policy (removed the old
  "no rule edit needed / Pattern 8" wording).
- (b) First test reframed into a **true-outsider denial**: seed keeps
  `pendingInvitedUsers: { "stranger-uid": true }` but auth is `outsider-uid` (in neither
  map); still `assertFails`. Renamed "true outsider (in neither map) cannot read registry doc".
- (c) Pending-only test **flipped `assertFails` → `assertSucceeds`** and renamed
  "pending-only invitee CAN read the registry doc" (same seed: private, `invitedUsers: {}`,
  `pendingInvitedUsers: { "invitee-uid": true }`, auth `invitee-uid`).
- (d) Owner test and the D-19 post-accept test left unchanged (both `assertSucceeds`).
- (e) **New scope-guard test** "pending-only invitee CANNOT read items (scope guard)":
  seeds a private registry with `pendingInvitedUsers: { "pending-uid": true }` +
  `invitedUsers: {}`, seeds an item via `testEnv.withSecurityRulesDisabled` (mirroring the
  inline pattern at lines 265-277), auths as `pending-uid`, and `assertFails` on the item
  `getDoc`. This locks in that the items path was NOT opened.

## Verification performed in-worktree

- `grep` confirms `isPendingInvited` appears only at the helper definition and the
  registry-DOC read OR clause — NOT under the items read rule.
- `git diff` confirms `firestore.rules` changed only by the new function + the single OR;
  `canReadRegistry()` body and the items read rule are untouched.
- `grep -q "pending-only invitee CANNOT read items"` passes (new scope-guard test landed).

## Did `tsc` run in the worktree?

**No.** The worktree has no `tests/rules/node_modules` installed, so
`npx tsc --noEmit -p tests/rules/tsconfig.json` could not resolve a TypeScript compiler
(`npx` aborted: "missing packages and no YES option"). Per the plan's fallback, the grep
check for the new test was used and passed. The test edits follow the exact existing
patterns in the same file (seed shape, `withSecurityRulesDisabled` item seeding,
`assertFails`/`assertSucceeds`), so no type drift is expected.

## NOT run here — deferred to the orchestrator (post-merge, MAIN checkout)

- `npm run test:emulator` (the Firestore rules emulator suite) — the worktree has no
  node_modules / emulator available. Must be run from the MAIN checkout after merge.
- `firebase deploy --only firestore:rules` to production — must be run by the orchestrator
  after merge.

## Deviations from Plan

None — plan executed exactly as written. (Note: files were edited in the agent worktree
copies at `.claude/worktrees/agent-a5ad10876197ed562/` rather than the shared-checkout
paths, because the agent is isolated to the worktree; the changes are otherwise identical.)

## Self-Check: PASSED
