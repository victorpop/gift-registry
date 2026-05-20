---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 01
subsystem: infra
tags: [firebase, hosting, vite, react, web, deployment, env-config]

# Dependency graph
requires:
  - phase: 13-web-fallback-visual-refresh
    provides: Redesigned Phase 13 React/Vite bundle ready to deploy
provides:
  - Firebase Web app registered in gift-registry-ro project (appId 1:85980314822:web:bfff3402857e6ca9feabc7)
  - web/.env.local with real VITE_FIREBASE_* config (gitignored)
  - Rebuilt Vite bundle with real Firebase config baked in (hosting/public/, gitignored)
  - Live deployment at https://gift-registry-ro.web.app replacing the blank-page bundle
  - firebase.json updated with long-cache headers for /assets/** (Cache-Control: max-age=31536000)
  - SPA rewrite verified: all deep-links return 200 + shell
affects: [14-02-functions-tsconfig-cleanup-and-deploy, 14-03-firestore-and-storage-rules-deploy, 14-04-layered-uat-and-appcheck-enforcement]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Firebase Web app registration as one-shot manual op (D-02): user runs firebase apps:create WEB to avoid duplicate creation"
    - "Vite env var inlining: VITE_FIREBASE_* values in web/.env.local are compile-time substitutions, NOT runtime reads"
    - "Firebase Hosting deploy scope isolation: firebase deploy --only hosting keeps functions/rules on previous version"
    - "Long-cache for hashed assets: /assets/** gets max-age=31536000; index.html gets max-age=3600 (Firebase default)"

key-files:
  created:
    - web/.env.local (gitignored — real Firebase Web SDK config, 6 VITE_FIREBASE_* keys)
    - hosting/public/index.html (gitignored — rebuilt Vite SPA entry with real config baked in)
    - hosting/public/assets/* (gitignored — hashed JS/CSS bundles with projectId:"gift-registry-ro" inlined)
  modified:
    - firebase.json (added headers block for long-cache on /assets/**)

key-decisions:
  - "Rollback strategy is fix-forward: firebase hosting:rollback target is the 2026-04-20 empty-config blank-page bundle — rolling back worsens the UX. Fix-forward (redeploy corrected bundle) is the only useful recovery path."
  - "App Check stays monitor-only on first deploy: VITE_RECAPTCHA_SITE_KEY left empty per D-04; Plan 14-04 wires reCAPTCHA v3 site key after monitor-mode verification"
  - "Firestore guest-read PERMISSION_DENIED on registry docs is a deferred concern: local firestore.rules (with defensive visibility.get default) not yet deployed; deferred to Plan 14-03"

patterns-established:
  - "hosting-only deploy pattern: firebase deploy --only hosting gates Wave 1; functions/rules deploy in separate waves to isolate blast radius"
  - "Bundle config verification: grep hosting/public/assets/*.js for projectId:\"gift-registry-ro\" confirms env var inlining succeeded"

requirements-completed: [WEB-01]

# Metrics
duration: ~90min (multi-session including human checkpoints)
completed: 2026-05-20
---

# Phase 14 Plan 01: Register Web App and Hosting Deploy Summary

**Firebase Web app registered in gift-registry-ro, real SDK config wired into Vite bundle, and redesigned Phase 13 shell deployed live at https://gift-registry-ro.web.app — replacing the blank page caused by missing VITE_FIREBASE_* env vars**

## Performance

- **Duration:** ~90 min (multi-session with human checkpoints)
- **Started:** 2026-05-20
- **Completed:** 2026-05-20
- **Tasks:** 7/7
- **Files modified:** 2 (firebase.json committed; web/.env.local + hosting/public/* gitignored)

## Accomplishments

- Registered Firebase Web app in gift-registry-ro project; appId `1:85980314822:web:bfff3402857e6ca9feabc7`
- Populated `web/.env.local` with all 6 real `VITE_FIREBASE_*` values; confirmed gitignored and NOT committed
- Rebuilt Vite bundle; verified `projectId:"gift-registry-ro"` and `apiKey:"AIzaSyAemd-gFr9K6Z1COp4CvauDNmy5Wmi2z6w"` baked into `hosting/public/assets/*.js`
- Fixed `firebase.json` missing long-cache headers for `/assets/**` (deviation auto-fix before first deploy attempt)
- Deployed hosting-only to production; `https://gift-registry-ro.web.app` live
- Curl-verified: root returns 200 with `id="root"` + `projectId:"gift-registry-ro"`, SPA deep-links return 200, cache headers correct
- User visual smoke confirmed: React mounted, GiftMaison branding rendered, EN/RO switcher present, sign-in CTA present, footer present, no FirebaseError in console

## Task Commits

Tasks executed across multiple sessions:

1. **Task 1: Register Firebase Web app (manual D-02)** - `(manual checkpoint — no commit)` — User registered Web app via `firebase apps:create WEB`; appId `1:85980314822:web:bfff3402857e6ca9feabc7` confirmed
2. **Task 2: Write web/.env.local with real config** - `(no commit — gitignored file)` — File written with 6 VITE_FIREBASE_* values + VITE_USE_EMULATORS=false + empty VITE_RECAPTCHA_SITE_KEY
3. **Task 3: Rebuild Vite bundle** - `3d597b1` (fix) — Pre-build TS2493 test error auto-fixed; bundle rebuilt with real config; `projectId:"gift-registry-ro"` confirmed in assets
4. **Task 4: Capture rollback context (D-03)** - `(manual checkpoint — no commit)` — Previous HEAD `3d597b1` recorded; rollback strategy documented (fix-forward)
5. **Task 5: Deploy hosting-only to production** - `9e3ddd3` (fix) — `firebase.json` long-cache headers added for `/assets/**`; deployed successfully; `https://gift-registry-ro.web.app` live
6. **Task 6: Verify SPA rewrite + cache headers + non-blank render** - `(no commit — verification only)` — All curl checks passed: 200/200/200, id="root" present, projectId in bundle, cache headers correct
7. **Task 7: Visual smoke-test in real browser** - `(manual checkpoint — user-approved)` — React mounted, GiftMaison branding rendered, SPA deep-link routed correctly, Firebase initialized cleanly

**Plan metadata:** (this commit)

## Files Created/Modified

- `firebase.json` — Added `headers` block: `index.html` inherits Firebase default (`max-age=3600`); `/assets/**` gets `Cache-Control: max-age=31536000` (immutable hashed bundles)
- `web/.env.local` — **Gitignored, NOT committed** — 6 VITE_FIREBASE_* keys with real gift-registry-ro Web app config; VITE_RECAPTCHA_SITE_KEY intentionally empty (D-04); VITE_USE_EMULATORS=false
- `hosting/public/index.html` — **Gitignored, NOT committed** — Rebuilt Vite SPA entry point with `id="root"`
- `hosting/public/assets/*.js` — **Gitignored, NOT committed** — Hashed JS bundles with `projectId:"gift-registry-ro"` and `apiKey:"AIzaSyAemd-gFr9K6Z1COp4CvauDNmy5Wmi2z6w"` inlined at build time

## Decisions Made

- **Fix-forward rollback strategy:** The last known hosting release (pre-14-01) served the old empty-config bundle that caused the blank page. `firebase hosting:rollback` would restore that broken state. The only useful recovery path is always fix-forward: fix the config and redeploy.
- **App Check stays monitor-only:** `VITE_RECAPTCHA_SITE_KEY` left empty per CONTEXT.md D-04. Plan 14-04 wires reCAPTCHA v3 site key after Firebase console registration and monitor-mode verification.
- **PERMISSION_DENIED on guest registry reads deferred:** Firestore security rules in production are stale relative to local `firestore.rules`. The deployed rules lack the defensive `visibility.get('visibility', 'public')` default. This is a Plan 14-03 deliverable; Plan 14-01's Wave 1 goal ("render the redesigned Phase 13 bundle instead of a blank page") is fully met.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added long-cache headers for /assets/** in firebase.json**
- **Found during:** Task 5 (Deploy hosting-only to production)
- **Issue:** `firebase.json` had no explicit `headers` block. Firebase Hosting defaults to `max-age=3600` for all files including hashed JS/CSS bundles. Hashed assets are content-addressed (filename changes on every build) and should be cached immutably (`max-age=31536000`). Without this, browsers re-fetch the entire bundle on every page load.
- **Fix:** Added `headers` array to `firebase.json` with `{ "source": "/assets/**", "headers": [{ "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }] }`. `index.html` continues to use Firebase's default `max-age=3600`.
- **Files modified:** `firebase.json`
- **Verification:** `curl -sI https://gift-registry-ro.web.app/assets/index-*.js | grep -i cache-control` returned `cache-control: public, max-age=31536000, immutable`
- **Committed in:** `9e3ddd3` (Task 5 commit)

**2. [Rule 3 - Blocking] Fixed TS2493 tuple-index TypeScript error in RegistryPage.test.tsx**
- **Found during:** Task 3 (Rebuild Vite bundle) — `npm run build` failed on TypeScript type-check
- **Issue:** `RegistryPage.test.tsx` had a tuple-index access error (TS2493) that blocked the Vite production build (`vite build` runs `tsc --noEmit` as part of the build step)
- **Fix:** Corrected the tuple-index access in the test file
- **Files modified:** `web/src/pages/RegistryPage.test.tsx` (or test harness file)
- **Verification:** `npm run build` completed successfully; bundle emitted to `hosting/public/`
- **Committed in:** `3d597b1` (pre-build fix commit)

---

**Total deviations:** 2 auto-fixed (1 missing critical, 1 blocking)
**Impact on plan:** Both auto-fixes necessary for correctness and ability to complete the plan. No scope creep.

## Issues Encountered

- TypeScript build error (TS2493) blocked `npm run build` during Task 3; auto-fixed per Rule 3.
- `firebase.json` missing long-cache headers for hashed assets; auto-fixed per Rule 2 before first deploy.

## Rollback Runbook (D-03)

**Previous-known-good git SHA:** `3d597b1` (HEAD at time of first deploy)

**Important:** The previous live hosting release (before Plan 14-01 deploy) served the **empty-config bundle** — the one that caused the blank page. Rolling back via `firebase hosting:rollback` would restore the broken blank-page experience.

**Rollback command (restores prior release — NOT recommended):**
```bash
firebase hosting:rollback --project gift-registry-ro
```

**Recommended recovery (fix-forward):**
If the current deploy is broken, fix the issue and redeploy:
```bash
cd /Users/victorpop/ai-projects/gift-registry/web && npm run build
cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only hosting --project gift-registry-ro
```

**Code-level rollback (emergency):**
```bash
git checkout 3d597b1
cd /Users/victorpop/ai-projects/gift-registry/web && npm run build
cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only hosting --project gift-registry-ro
```

## Deferred Verification: Firestore Guest-Read PERMISSION_DENIED

**Discovered during:** Task 7 visual smoke test on `https://gift-registry-ro.web.app/registry/heTs42jyX1XPDtBSJbR3`

**Finding:** The app correctly rendered the "Registry not available" state, but a direct Firestore REST probe confirmed `PERMISSION_DENIED` (403) for unauthenticated guest reads of that registry document:
```
curl https://firestore.googleapis.com/v1/projects/gift-registry-ro/databases/(default)/documents/registries/<id>
→ 403 PERMISSION_DENIED
```

**Root cause:** The production Firestore rules are stale relative to `firestore.rules` in the repo. The current local rules contain the defensive `visibility.get('visibility', 'public')` default that handles legacy registry docs missing the `visibility` field. Those rules have NOT been deployed yet.

**Why this is NOT a 14-01 regression:**
- Wave 1's goal is "render the redesigned Phase 13 bundle instead of a blank page" — fully met
- Plan 14-01 does NOT include Firestore rules deploy (that is Plan 14-03, Wave 2)
- Plan 14-01 does NOT include guest registry UAT (that is Plan 14-04, Wave 3)
- The web app rendered correctly (no blank page, no JS crash, SPA routing works)

**Required follow-up after Plan 14-03:**
After the current `firestore.rules` deploy (Plan 14-03), verify guest reads work:
```bash
curl "https://firestore.googleapis.com/v1/projects/gift-registry-ro/databases/(default)/documents/registries/<a-known-public-registry-id>"
```
Expected: 200 with registry document body (or 404 NOT_FOUND if the registry truly doesn't exist — but NOT 403).

The full guest-access UAT (including unauthenticated read flows) belongs to Plan 14-04.

## Known Stubs

- `VITE_RECAPTCHA_SITE_KEY=` intentionally empty in `web/.env.local` — App Check stays monitor-only. Plan 14-04 wires the reCAPTCHA v3 site key after Firebase console registration and monitor-mode verification (CONTEXT.md D-04).

## Next Phase Readiness

**Wave 2 (Plan 14-02) unblocked:** Firebase Hosting has a live bundle at `https://gift-registry-ro.web.app`. Functions tsconfig cleanup and deploy can proceed independently.

**Wave 2 (Plan 14-03) unblocked:** Firestore and Storage rules deploy is now unblocked; guest-read PERMISSION_DENIED will be resolved when those rules land in production.

**Wave 3 (Plan 14-04) requires Waves 1+2:** Full guest UAT and App Check enforcement flip require both a live bundle AND deployed rules/functions. Plans 14-02 and 14-03 must complete first.

---
*Phase: 14-web-fallback-live-deploy-guest-uat*
*Completed: 2026-05-20*
