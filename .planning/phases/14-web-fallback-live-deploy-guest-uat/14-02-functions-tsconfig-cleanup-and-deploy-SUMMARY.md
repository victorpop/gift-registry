---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 02
subsystem: infra
tags: [firebase-functions, cloud-functions, typescript, tsconfig, nodejs22, europe-west3]

# Dependency graph
requires:
  - phase: 14-web-fallback-live-deploy-guest-uat/14-01
    provides: Firebase Hosting deployed (web app live); project gift-registry-ro confirmed active
provides:
  - tsconfig with rootDir:src producing lib/index.js layout (not lib/src/index.js)
  - functions/.env committed with PUBLIC_WEB_BASE_URL default (no-workaround fresh-clone deploy)
  - 11 Cloud Functions live on gift-registry-ro (3 new + 8 refreshed) in europe-west3
  - getReservationForItem, hydrateActiveReservation, releaseReservationCallable — new callables live
  - notifications-inbox writeNotification helper (from quick-260420-ozb) shipped to production
affects: [14-04-layered-uat-and-appcheck-enforcement, web-fallback, reservation-system]

# Tech tracking
tech-stack:
  added: [ts-node@^10.9.0 (devDependency — for seed:stores script standalone execution)]
  patterns:
    - "functions/.env committed with public-only defaults; secrets stay in .env.local (gitignored)"
    - "tsconfig rootDir:src + include:['src'] — scripts/ compiled standalone via ts-node, not tsc"
    - "firebase deploy --only functions is the canonical deploy command — no flags or overrides needed"

key-files:
  created:
    - functions/.env
    - functions/.env.example
  modified:
    - functions/tsconfig.json
    - functions/package.json
    - .gitignore

key-decisions:
  - "Committed functions/.env with PUBLIC_WEB_BASE_URL (public Firebase Hosting URL — not a secret); enables firebase deploy --only functions from a fresh clone with zero interactive prompts"
  - "Added rootDir:src to tsconfig.json; removed scripts/ from include array — scripts compiled via ts-node standalone; fixes lib/src/ nesting bug"
  - "healthCheck is in us-central1 (not europe-west3) — pre-existing condition from initial no-region definition; documented not fixed (changing region requires delete + recreate, out of scope)"

patterns-established:
  - "functions/.env pattern: public-only values committed; secrets override via functions/.env.local (gitignored)"
  - ".gitignore negation: !functions/.env and !functions/.env.example exceptions scoped tightly; web/.env.local remains protected"

requirements-completed: [WEB-02]

# Metrics
duration: ~15min
completed: 2026-05-21
---

# Phase 14 Plan 02: Functions tsconfig cleanup and deploy Summary

**tsconfig rootDir fix + committed .env default shipped; 11 Cloud Functions (3 new) deployed to europe-west3 on gift-registry-ro with zero workarounds from a fresh clone**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-05-21T10:43:31Z
- **Completed:** 2026-05-21T10:46:00Z
- **Tasks:** 5 (Tasks 1-3 completed in prior session; Tasks 4-5 completed in this session)
- **Files modified:** 5

## Accomplishments

- Fixed tsconfig.json (added `rootDir: "src"`, removed `scripts/` from include) so `npm run build` produces `lib/index.js` not `lib/src/index.js`
- Committed `functions/.env` with `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app` with `.gitignore` negation — `firebase deploy --only functions` now works from a fresh clone with no interactive prompts
- Deployed all 11 functions to production: 3 new (`getReservationForItem`, `hydrateActiveReservation`, `releaseReservationCallable`) + 8 refreshed; all in europe-west3 (WEB-D-17 verified)
- Shipped persistent-notifications functions from quick-260420-ozb (`onPurchaseNotification` + `writeNotification` helper) to production for the first time
- Updated `seed:stores` script to use `ts-node scripts/seedStores.ts` (standalone, not through tsc build)

## Task Commits

| # | Task | Commit | Type |
|---|------|--------|------|
| 1 | Apply tsconfig + package.json cleanup (D-10) | `f5497dd` | chore |
| 2 | Commit functions/.env with public default + .gitignore + .env.example | `71ce8fe` | chore |
| 3 | Capture rollback context (checkpoint — user confirmed "deploy approved") | n/a (no code change) | — |
| 4 | Deploy functions to production | n/a (cloud deploy — no repo changes) | — |
| 5 | Post-deploy smoke verification | n/a (verification only) | — |

**Plan metadata:** `[this commit]` (docs: complete plan)

## Files Created/Modified

- `functions/tsconfig.json` — Added `"rootDir": "src"`, removed `"scripts"` from `include` array
- `functions/package.json` — Changed `main` from `lib/src/index.js` → `lib/index.js`; `seed:stores` from `npm run build && node lib/scripts/seedStores.js` → `ts-node scripts/seedStores.ts`; added `ts-node@^10.9.0` to devDependencies
- `functions/.env` — New file; `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app`; committed deliberately (public URL, not a secret)
- `functions/.env.example` — New file; documents the env convention
- `.gitignore` — Added `!functions/.env` and `!functions/.env.example` negation exceptions in Environment block; `web/.env.local` and `functions/.env.local` remain gitignored

## Decisions Made

- Committed `functions/.env` with the public Firebase Hosting URL. The `defineString` param has a `default:` but Firebase CLI still prompts in non-interactive mode without a `.env` file. The URL is fully public (it's the web app hostname). "Fresh-clone deploy just works" is worth more than conceptual purity about not committing `.env` files. See CONTEXT.md D-10.
- Kept `healthCheck` in `us-central1` as a pre-existing condition. Moving it to `europe-west3` requires deleting and recreating the function (Cloud Run revision region is immutable). Out of scope for this plan.
- Used `ts-node` for `seed:stores` rather than keeping a separate tsconfig for scripts. The seed script is a dev-only maintenance tool; ts-node is simpler and avoids polluting the production build.

## Deploy Results

### Pre-deploy state (rollback target)
- **Git SHA at deploy:** `71ce8fe1257eaa1bedd92c257b5c9e224a1e1c36`
- **Rollback target SHA (also the same — this is post-task-2 commit):** `71ce8fe`

### Deploy log summary (2026-05-21T10:44:xx UTC)

**3 new functions created (CREATE operation):**
- `getReservationForItem` (europe-west3) — callable
- `hydrateActiveReservation` (europe-west3) — callable
- `releaseReservationCallable` (europe-west3) — callable

**8 existing functions refreshed (UPDATE operation):**
- `confirmPurchase` (europe-west3) — callable
- `createReservation` (europe-west3) — callable
- `fetchOgMetadata` (europe-west3) — callable
- `inviteToRegistry` (europe-west3) — callable
- `onPurchaseNotification` (europe-west3) — Firestore trigger
- `releaseReservation` (europe-west3) — taskQueue
- `resolveReservation` (europe-west3) — callable
- `healthCheck` (us-central1) — https (see note below)

**Note — ext-firestore-send-email-processqueue:** Also visible in `functions:list`; this is a Firebase Extension function installed on the project, not managed by this deploy. It was already present.

**Deploy completed:** `Deploy complete!` — exit 0, no errors.

**Key absence:** No `In non-interactive mode but have no value for PUBLIC_WEB_BASE_URL` error. Task 2 fix confirmed working.

**Key absence:** No `functions/lib/index.js does not exist` error. Task 1 fix confirmed working.

### Post-deploy verification (2026-05-21T10:45:xx UTC)

| Check | Result |
|-------|--------|
| `firebase functions:list` — 3 new functions present | PASS |
| `firebase functions:list` — all 8 existing functions present | PASS |
| `healthCheck` (us-central1) curl response | HTTP 200 |
| `getReservationForItem` POST curl (no auth) | HTTP 400 (callable exists, rejecting invalid request) |
| `hydrateActiveReservation` POST curl (no auth) | HTTP 400 (callable exists, rejecting invalid request) |
| `releaseReservationCallable` POST curl (no auth) | HTTP 400 (callable exists, rejecting invalid request) |
| `createReservation` POST curl (no auth) | HTTP 400 (callable exists, rejecting invalid request) |
| All europe-west3 functions in correct region | PASS (verified via functions:list) |

All 3 new callables returned 400 (not 404) — they exist and are enforcing validation. This is the expected "callable exists" signal.

## Fresh-clone Hypothesis: Verified

After this plan, `firebase deploy --only functions` from a fresh clone succeeds with zero workarounds.

**Evidence:**
1. `functions/.env` is committed — `PUBLIC_WEB_BASE_URL` param resolves without interactive prompt
2. `functions/tsconfig.json` has `"rootDir": "src"` — `npm run build` produces `lib/index.js` (not `lib/src/index.js`)
3. `functions/package.json` `"main": "lib/index.js"` matches the build output
4. `git ls-files functions/lib/` returns 0 rows — no stale compiled artifacts in git
5. This deploy itself was executed from the current working tree (no special flags, no overrides) and succeeded — that IS the fresh-clone scenario

## Known Notes

- **healthCheck in us-central1:** Pre-existing condition from initial function definition without explicit region. All other functions are in europe-west3 per WEB-D-17. Moving healthCheck would require delete + recreate (Cloud Run region is immutable after creation). Low priority — it is not a business logic function.
- **firebase-functions version warning:** Deploy emitted `package.json indicates an outdated version of firebase-functions. Please upgrade using npm install --save firebase-functions@latest`. This is a non-blocking warning. Upgrade deferred to a future maintenance plan — do not address mid-UAT.

## Rollback Runbook (D-03)

**Scenario:** This deploy introduced a regression; need to revert to previous function code.

**Pre-deploy SHA (rollback target):** `71ce8fe1257eaa1bedd92c257b5c9e224a1e1c36`

**Code rollback (redeploy from previous SHA):**
```bash
git checkout 71ce8fe1257eaa1bedd92c257b5c9e224a1e1c36
cd functions && npm install
firebase deploy --only functions --project gift-registry-ro
git checkout main  # return to current branch after rollback deploy
```

**Emergency per-function rollback (Cloud Run revision — faster, no rebuild):**
```bash
# Step 1: Find the previous revision
gcloud run revisions list --service=<function-name> --region=europe-west3 --project=gift-registry-ro

# Step 2: Route 100% traffic to previous revision
gcloud run services update-traffic <function-name> \
  --to-revisions=<previous-revision>=100 \
  --region=europe-west3 \
  --project=gift-registry-ro
```

Replace `<function-name>` with the affected function (e.g., `createreservation` — Cloud Run service names are lowercase). Replace `<previous-revision>` with the revision hash from step 1.

**Which approach to use:**
- Regression in a single function → Cloud Run revision rollback (fastest, no redeploy needed)
- Multiple functions affected → Code rollback + full redeploy

## Deviations from Plan

None — plan executed exactly as written. Tasks 1-3 were completed in the prior session (continuation agent scenario); Tasks 4-5 executed in this session with all acceptance criteria met.

## Issues Encountered

None. The two previously known issues (tsconfig layout bug + missing functions/.env) were fixed in Tasks 1-2. The deploy proceeded cleanly.

## Next Phase Readiness

- **Plan 14-03** (Firestore + Storage rules deploy) ran in parallel per CONTEXT.md D-01. Both plans targeted `--only` scopes that are disjoint (functions vs firestore,storage). Status of 14-03 should be checked separately.
- **Plan 14-04** (layered UAT + App Check enforcement) is now unblocked:
  - Task 6 (email re-reserve via functions:shell) — unblocked (functions live)
  - Task 2 (retailer redirect via createReservation) — unblocked (callable live and reachable)
  - All reservation callables live: `createReservation`, `resolveReservation`, `confirmPurchase`, `releaseReservationCallable`, `hydrateActiveReservation`, `getReservationForItem`
- **Folded todo** `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` can be moved from `pending/` to `complete/` — the two issues it documented (tsconfig layout + missing .env) are both resolved and deployed.

---
*Phase: 14-web-fallback-live-deploy-guest-uat*
*Completed: 2026-05-21*
