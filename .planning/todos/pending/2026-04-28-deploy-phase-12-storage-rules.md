---
date: 2026-04-28
category: deployment
phase_origin: 12-registry-cover-photo-themed-placeholder
plan_origin: 12-05
priority: high
status: deferred-from-12-05
---

# Deploy Phase 12 storage.rules to gift-registry-ro

## Context

Phase 12 Plan 05 reached the human-verify checkpoint covering visual UAT,
on-device UAT, and the live `firebase deploy --only storage` step. The user
approved UAT but **deferred the storage rules deploy** — `approved — storage
deploy skipped`.

`storage.rules` was authored and wired into `firebase.json` in Plan 12-02
(commit `e979e45`). The rules file ships locally; it has NOT been deployed
to the live Firebase project yet, so production registry traffic that uses
the cover-photo upload path will currently hit the default
`allow read, write: if false` Storage rule (deny-by-default) until this
deploy lands.

## Files in scope

- `storage.rules` (already authored — owner-only writes via cross-service
  `firestore.get()` lookup against the registry document; mirrors
  `firestore.rules` `canReadRegistry` model with legacy-doc defaults via
  `data.get('visibility', 'public')`; default-deny on `match /{allPaths=**}`)
- `firebase.json` (already wires `storage.rules` at the top level peer to
  `firestore.rules`)

Both files are committed in `e979e45`. **No code changes needed for this
todo — only the deploy step.**

## Deploy command

From the repo root:

```bash
firebase deploy --only storage --project gift-registry-ro
```

(Substitute the project alias if different — check `.firebaserc`.)

## First-time prompt — accept the cross-service grant

On the **first** deploy of `storage.rules` for this project, Firebase CLI
will ask for permission to enable cross-service rules (Storage Rules
calling `firestore.get(...)` to look up registry ownership / visibility).
**Accept the prompt (`Y`).** Subsequent deploys will run silently.

## Verification

1. Deploy completes without error.
2. **Firebase Console -> Storage -> Rules tab** shows the deployed rules
   content with the `firestore.get(...)` cross-service helpers
   (`isOwnerOfRegistry`, `isPublicOrInvited`).
3. Optional smoke test via the Console **Rules Simulator**:
   - Path: `/users/{otherUid}/registries/{someRegistryId}/cover.jpg`
   - Auth: a non-owner uid
   - Operation: `create`
   - Expected: **DENY**.
4. Optional smoke test for read on a private registry as a non-invited
   user — expected **DENY** (mirrors `canReadRegistry` model).

## Suggested timing

Land **before any production registry traffic uses the cover-photo
upload path**. Until this deploy is live:

- Preset selections still work end-to-end (no Storage write involved).
- Gallery uploads will be blocked by the default deny rule on the live
  bucket. Local emulator runs are unaffected (emulator uses the local
  `storage.rules` file directly).

## References

- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-02-SUMMARY.md`
  (storage.rules authoring decision + cross-service rules pattern)
- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-05-PLAN.md`
  (Task 2 STEP A — the original deploy instructions, including the
  cross-service permissions prompt)
- `storage.rules`
- `firebase.json`
- Commit `e979e45` (Plan 12-02 Task 4 — storage.rules + firebase.json wiring)

## Acceptance

- [ ] `firebase deploy --only storage --project gift-registry-ro` runs successfully
- [ ] Cross-service permissions prompt accepted (one-time)
- [ ] Firebase Console Storage -> Rules tab shows the deployed `firestore.get(...)` rules
- [ ] Rules Simulator: non-owner write to a stranger's registry path returns DENY
- [ ] Optional: real-bucket smoke test via the app (gallery upload by owner succeeds; non-owner attempt fails)
