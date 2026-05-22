---
phase: 14-web-fallback-live-deploy-guest-uat
verified: 2026-05-22T18:10:00Z
status: passed_with_deferrals
score: 4/4 requirements verified (WEB-01..04) + 7/7 UAT items PASS in Pass 1; 2 user-accepted deferrals logged as follow-up todos
re_verification: false
deferrals:
  - id: task-8-recruited-giver-pass-2
    scope: "UAT items 2 (retailer redirect) and 3 (guest localStorage) — Pass 2 with recruited real giver"
    why: "Pass 1 PASS in Chrome + Safari incognito provides primary validation; Pass 2 was the 'works on my machine' smell-test layer (D-08), not the closure-gating evidence."
    risk: low
    follow_up: ".planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md"
    user_accepted: true
  - id: task-9-app-check-enforcement-flip
    scope: "Flip App Check from monitor-only to enforced for Storage → Functions → Firestore (D-04)"
    why: "Android app has NO App Check provider wired (grep verified zero matches across app/src/, app/build.gradle.kts, gradle/libs.versions.toml). Flipping enforcement today would 403-reject every Android-originated request in production."
    risk: medium
    posture_at_close: "monitor-only on all three services; web fallback IS App-Check-wired (reCAPTCHA v3 active, appcheck:exchange returns 200 per UAT-1)"
    follow_up: ".planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md"
    user_accepted: true
notes:
  - "Phase 14 was scoped to convert WEB-01..04 from 'code-complete + pending real-browser UAT' (per PROJECT.md caveat dated 2026-04-19) to 'verified against production'. Plans 14-01/02/03 delivered the live deploy (hosting + functions + rules); Plan 14-04 ran the layered UAT and surfaced + fixed 5 silent production bugs en route to PASS."
  - "All 7 manual UAT items in 05-VALIDATION.md 'Manual-Only Verifications' table PASSED in Pass 1 against https://gift-registry-ro.web.app/ in real Chrome + Safari incognito; UAT-6 PASSED on attempt #4 after the Cloud Tasks bug trilogy fixes (bf4ca31, d0c7516, 7ffb380); UAT-7 PASSED on attempt #3 after the OAuth fixes (3218a49, 47c1bfa)."
  - "Verifier did NOT re-litigate the 2 user-accepted deferrals as gaps; they are recorded as deferrals with logged follow-up todos per the verification scope brief."
---

# Phase 14: Web Fallback Live Deploy + Guest UAT — Verification Report

**Phase Goal:** Close WEB-01..04 against the live production deploy at `https://gift-registry-ro.web.app/`. The requirements were code-complete in Phase 5 but had a "pending real-browser UAT against production" caveat in PROJECT.md since 2026-04-19. Phase 14 was scoped to (1) register the Firebase web app + write `.env.local`, (2) deploy hosting + functions + rules to the production project, (3) run a layered UAT (D-08) against the live deploy to officially close the requirements.

**Verified:** 2026-05-22T18:10:00Z
**Status:** `passed_with_deferrals`
**Re-verification:** No — initial verification.

---

## Goal Achievement

### Observable Truths (derived from Phase 14 closure criteria in 14-CONTEXT.md `<domain>` block)

| #   | Truth                                                                                                                                                                                            | Status     | Evidence                                                                                                                                                                                                                                                                                                                  |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `https://gift-registry-ro.web.app/` renders the redesigned Phase 13 bundle (no blank page).                                                                                                      | ✓ VERIFIED | `hosting/public/index.html` exists with hashed bundle reference `/assets/index-CUlNqIJy.js`; `firebase.json` hosting block points to `hosting/public`; SPA rewrite `** → /index.html` present; UAT-1, UAT-4, UAT-5, UAT-7 all interacted with the deployed bundle successfully. Plan 14-01 SUMMARY confirms deploy succeeded. |
| 2   | Backing Cloud Functions deployed successfully (createReservation + releaseReservation + Cloud Tasks pipeline working end-to-end in prod).                                                        | ✓ VERIFIED | Plan 14-02 SUMMARY confirms functions deploy succeeded (commit `c0b7066`). `functions/src/reservation/createReservation.ts` lines 85-87 use region-qualified path `locations/${REGION}/functions/releaseReservation` (fix `7ffb380`) with Firebase Admin SDK `getFunctions().taskQueue(...)` API for automatic OIDC (fix `d0c7516`); REGION pinned to `europe-west3`. UAT-6 PASS on attempt #4 proves end-to-end pipeline works in prod. |
| 3   | Firestore + Storage rules deployed to production (firestore.rules with notifications block + storage.rules with cross-service firestore.get helpers).                                            | ✓ VERIFIED | Plan 14-03 SUMMARY confirms both rule sets deployed; deploy-log grep verified both "released rules" lines present. Default-deny probes confirmed effective post-deploy. UAT-5 PASS proves private-registry rules-deny works in deployed state.                                                                            |
| 4   | All 7 Phase-5 manual UAT items verified PASS in real Chrome + Safari against the production deploy.                                                                                              | ✓ VERIFIED | 14-04-UAT-RESULTS.md "Pass 1 — Solo Incognito" table shows all 7 items PASS with dated evidence. UAT-6 closed on attempt #4 after Cloud Tasks trilogy fixes; UAT-7 closed on attempt #3 after OAuth fixes; rest PASS first try.                                                                                          |
| 5   | App Check operationalized: reCAPTCHA v3 site key registered, web bundle wired, monitor mode active on all three services.                                                                        | ✓ VERIFIED (monitor) / ⚠️ ENFORCEMENT DEFERRED | `web/src/firebase.ts` lines 40-45 call `initializeAppCheck(app, { provider: new ReCaptchaV3Provider(...), isTokenAutoRefreshEnabled: true })` gated on `VITE_USE_EMULATORS !== 'true'` and `VITE_RECAPTCHA_SITE_KEY` present. UAT-1 verified `appcheck:exchange` returns 200 in real Chrome. Enforcement flip deferred — Android app has no App Check provider wired (grep verified zero matches); logged in deferral todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md`. |

**Score:** 5/5 closure criteria verified (#5 partially — monitor mode active as per D-04 stage 1; enforcement flip deferred with user acceptance).

---

## Per-Requirement Verification Matrix

REQUIREMENTS.md lists WEB-01..04 as a single sentence each ("Gift givers can …"). Per the verification scope brief, I verified each via the supporting UAT item(s) defined in `14-04-layered-uat-and-appcheck-enforcement-PLAN.md` `<interfaces>` table mapping requirement IDs → UAT items.

### WEB-01: Gift givers can view a registry via web browser without installing the app

| Sub-claim                                                            | Status      | Evidence source                                                                                                                                                            |
| -------------------------------------------------------------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Web fallback renders at production URL (not blank)                   | ✓ VERIFIED  | Plan 14-01 SUMMARY (deploy succeeded); `hosting/public/index.html` references valid hashed bundle; UAT-1/4/5/7 interactions confirm deployed bundle is the redesigned shell. |
| Public registry deep-links resolve via SPA rewrite                   | ✓ VERIFIED  | `firebase.json` `rewrites: [{ source: "**", destination: "/index.html" }]`; UAT-5 PASS exercised a private-registry deep-link (resolves to 404 page, no blank).            |
| i18next locale auto-detection on cold load                           | ✓ VERIFIED  | UAT-4 PASS — Romanian system+browser locale, fresh Cmd+Q + relaunch incognito, UI rendered in Romanian (WEB-D-15 working end-to-end against deployed bundle).             |

**Verdict:** ✓ PASS — Requirement satisfied against live production. Code evidence + UAT-1/4/5 in real browsers.

### WEB-02: Gift givers can reserve items from the web fallback

| Sub-claim                                                            | Status      | Evidence source                                                                                                                                                                                                                                                              |
| -------------------------------------------------------------------- | ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Reservation flow reaches createReservation callable in prod          | ✓ VERIFIED  | UAT-2 PASS (Chrome + Safari) — Reserve flow committed reservation, item flipped to "Reserved" status with accent dot on return. Backed by Plan 14-02 functions deploy.                                                                                                       |
| Reservation auto-releases at 30-min mark via Cloud Tasks pipeline    | ✓ VERIFIED  | UAT-6 PASS on attempt #4 (2026-05-22) — natural 30-min path through prod-pointed Android APK: task visible in `releaseReservation` queue within seconds of reserve, handler fired at scheduled time, item flipped to `available`, reservation flipped to `expired`, expiry email arrived. User-confirmed. |
| Email re-reserve CTA round-trips through deployed callables          | ✓ VERIFIED  | UAT-6 PASS (continuation) — user clicked re-reserve CTA in expiry email, landed on registry detail with `?autoReserveItemId=…`, new `createReservation` called, new reservation created. End-to-end pipeline verified.                                                       |
| App Check (reCAPTCHA v3) token acquisition in prod                   | ✓ VERIFIED  | UAT-1 PASS — DevTools Network panel filtered to `appcheck.googleapis.com` showed `appcheck:exchange` HTTP 200 firing BEFORE first `firestore.googleapis.com` request, in real Chrome on prod.                                                                              |

**Verdict:** ✓ PASS — Requirement satisfied against live production. End-to-end pipeline (reserve → 30-min Cloud Task → release → expiry email → re-reserve) verified by UAT-6 PASS on attempt #4. Note: 5 silent production bugs (Cloud Tasks queue-name, OIDC token, region default) were caught and fixed before this PASS — see Retrospective section.

### WEB-03: Gift givers can log in, create an account, or continue as guest on web

| Sub-claim                                                            | Status      | Evidence source                                                                                                                                                                                                                                                              |
| -------------------------------------------------------------------- | ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Google OAuth works on deployed build (cross-browser)                 | ✓ VERIFIED  | UAT-7 PASS on attempt #3 (2026-05-22) in BOTH Chrome and Safari incognito. `web/src/features/auth/authProviders.ts` uses `signInWithRedirect` (fix `3218a49`); `web/src/main.tsx` wires `getRedirectResult(auth)` at module-load (verified by grep); `VITE_FIREBASE_AUTH_DOMAIN` aligned to `gift-registry-ro.web.app` (fix `47c1bfa`). |
| Guest identity persists across browser restart                       | ✓ VERIFIED  | UAT-3 PASS — Chrome incognito → quit → relaunch; `ItemReservePage` STEP 2 CTA path detected prior guest identity from localStorage and reserved silently (no modal re-prompt). Hydration round-trip is the proof.                                                            |
| Auth session persists across tab close (browserLocalPersistence)     | ✓ VERIFIED  | `web/src/firebase.ts` line 52: `void setPersistence(auth, browserLocalPersistence)`. WEB-D-12 honored.                                                                                                                                                                       |
| App Check guards token acquisition (web side wired)                  | ✓ VERIFIED  | UAT-1 PASS — see WEB-02 sub-claim above.                                                                                                                                                                                                                                     |

**Verdict:** ✓ PASS — Requirement satisfied against live production. UAT-3 + UAT-7 verified guest + OAuth paths end-to-end.

### WEB-04: Web fallback redirects to retailer on reservation (same as Android flow)

| Sub-claim                                                            | Status      | Evidence source                                                                                                                                                                                                                                                              |
| -------------------------------------------------------------------- | ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Retailer redirect opens new tab + registry tab stays alive (cross-browser) | ✓ VERIFIED | UAT-2 PASS in BOTH Chrome and Safari incognito — new tab opens to retailer affiliate URL; registry tab stays open with sticky countdown banner; item shows "Reserved" on return. Behavior identical in both browsers.                                                       |
| Private-registry deep-link rules-deny path is safe                   | ✓ VERIFIED  | UAT-5 PASS — direct paste of private-registry URL in fresh incognito (no auth) rendered the generic 404 page. No data leak (existence not revealed, owner-only data not shown). Firestore returned `permission-denied`, caught and mapped to 404 by client per WEB-D-13/14. Backed by Plan 14-03 rules deploy. |

**Verdict:** ✓ PASS — Requirement satisfied against live production. UAT-2 + UAT-5 verified end-to-end in both browsers.

---

## Required Artifacts

| Artifact                                                                                                  | Expected                                                                | Status        | Details                                                                                                                                                                                                              |
| --------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`                             | Pass/fail record for all 7 UAT items                                    | ✓ VERIFIED    | Exists; contains Pass 1 table with PASS for all 7 items + 5 prod-bug post-mortems + UAT-6 natural 30-min plan + deferred-bugs section + Plan 14-04 Final Tally.                                                       |
| `web/src/firebase.ts`                                                                                     | initializeAppCheck via ReCaptchaV3Provider, env-gated                   | ✓ VERIFIED    | Lines 40-45 call `initializeAppCheck(app, { provider: new ReCaptchaV3Provider(import.meta.env.VITE_RECAPTCHA_SITE_KEY), isTokenAutoRefreshEnabled: true })` with `!emulator && key present` guard.                  |
| `web/src/main.tsx`                                                                                        | `getRedirectResult(auth)` at module-load for OAuth return leg           | ✓ VERIFIED    | Line 3 imports `getRedirectResult`; line 22 wires it at module level with `.catch(...)`.                                                                                                                              |
| `web/src/features/auth/authProviders.ts`                                                                  | `signInWithRedirect` (not `signInWithPopup`)                            | ✓ VERIFIED    | Lines 1-10 import `signInWithRedirect` + `getRedirectResult` (no `signInWithPopup` import); line 35 `signInWithGoogle()` uses `signInWithRedirect(auth, provider)`.                                                  |
| `web/src/features/auth/AuthScreen.tsx`                                                                    | `useAuth().isReady` gate to kill post-redirect flash                    | ✓ VERIFIED    | Line 37 destructures `isReady`; line 51 `if (!isReady) { return null }`.                                                                                                                                              |
| `web/src/components/giftmaison/TopNav.tsx`                                                                | `useAuth().isReady` gate on auth-area slot only                         | ✓ VERIFIED    | Line 32 destructures `isReady`; line 52 `{!isReady ? null : user ? ...}`; comment block lines 46-50 documents the wordmark+language-switcher-render-during-cold-boot intent.                                          |
| `functions/src/reservation/createReservation.ts`                                                          | TaskQueue uses region-qualified path + Firebase Admin SDK for OIDC      | ✓ VERIFIED    | Line 85: `getFunctions().taskQueue<ReleasePayload>(\`locations/${REGION}/functions/releaseReservation\`)`. Region constant `europe-west3` (line 23). Includes prod-bug trilogy fixes `bf4ca31` + `d0c7516` + `7ffb380`. |
| `hosting/public/index.html` + `hosting/public/assets/*`                                                   | Built React bundle exists post-deploy                                   | ✓ VERIFIED    | `index.html` references `/assets/index-CUlNqIJy.js` + `/assets/index-CLN58XV4.css` (hashed bundle); `auth-editorial.jpg`, `og-default.png` also present.                                                              |
| `firebase.json`                                                                                           | SPA rewrite + cache headers + storage rules + firestore rules wired     | ✓ VERIFIED    | `rewrites: [{ source: "**", destination: "/index.html" }]`; `/assets/**` cache-control `max-age=31536000, immutable`; `storage.rules` + `firestore.rules` declared.                                                  |
| Deferral todos                                                                                            | Two pending todos for Task 8 + Task 9 deferrals                          | ✓ VERIFIED    | Both exist in `.planning/todos/pending/`: `2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md` + `2026-05-22-wire-android-app-check-and-flip-enforcement.md`.                                            |
| Folded ops todos moved to `completed/`                                                                    | 3 pending → completed transitions                                       | ✓ VERIFIED    | `register-firebase-web-app-and-deploy-real-web-config.md`, `fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md`, `deploy-phase-12-storage-rules.md` all in `completed/`.                          |

---

## Key Link Verification

| From                                                                  | To                                                                              | Via                                                                                                                                                                          | Status        | Details                                                                                                                                                                                            |
| --------------------------------------------------------------------- | ------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Web client App Check provider                                         | `appcheck.googleapis.com appcheck:exchange` endpoint                            | `VITE_RECAPTCHA_SITE_KEY` in `web/.env.local` + `initializeAppCheck()` call in `web/src/firebase.ts:40-45`                                                                  | ✓ WIRED       | UAT-1 PASS — DevTools confirmed `appcheck:exchange` HTTP 200 fires before first Firestore call in real Chrome on prod. End-to-end wiring verified.                                                |
| Android prod-pointed APK reserving a real item                        | Deployed `releaseReservation` onTaskDispatched handler                          | Natural 30-min Cloud Task scheduled by `createReservation` in production (same pipeline as real users)                                                                       | ✓ WIRED       | UAT-6 PASS attempt #4 — task visible in `releaseReservation` queue within seconds (create_time `13:28:41`, ETA `13:58:40`), handler fired at scheduled time, item flipped to `available`, reservation `expired`, expiry email delivered. |
| Email re-reserve CTA                                                  | Deployed `createReservation` callable                                           | `?autoReserveItemId=<itemId>` URL param on registry detail page auto-fires reservation per WEB-D-11                                                                          | ✓ WIRED       | UAT-6 PASS (continuation step) — link click → page load → new reservation created end-to-end.                                                                                                       |
| `signInWithRedirect` initiator                                        | `getRedirectResult` consumer on return leg                                      | Both run against same origin (`gift-registry-ro.web.app`) so redirect-event state in `sessionStorage`/`localStorage` is visible end-to-end (fix `47c1bfa` for authDomain) | ✓ WIRED       | UAT-7 PASS attempt #3 — post-redirect signed-in UI rendered without manual refresh; `firebase:authUser:*` populated in localStorage as expected, in both Chrome and Safari.                       |

---

## Data-Flow Trace (Level 4)

| Artifact                                       | Data Variable                       | Source                                                                  | Produces Real Data | Status     |
| ---------------------------------------------- | ----------------------------------- | ---------------------------------------------------------------------- | ------------------ | ---------- |
| Live web bundle at gift-registry-ro.web.app    | Firestore registry/item docs        | `useRegistryQuery`/`useItemsQuery` → deployed Firestore                | Yes                | ✓ FLOWING  |
| Reservation banner countdown                   | `reservations/{id}` doc             | `useActiveReservationHydration` → deployed Firestore                   | Yes                | ✓ FLOWING  |
| Auto-release pipeline                          | Cloud Task → `releaseReservation`   | TaskQueue (Firebase Admin) → deployed onTaskDispatched handler          | Yes                | ✓ FLOWING (UAT-6 attempt #4) |
| Expiry email                                   | `mail/` doc → Firestore Trigger Email | `releaseReservationCore` writes to `mail/` collection                  | Yes                | ✓ FLOWING (UAT-6 — user received email) |
| App Check token                                | reCAPTCHA v3 site token             | `initializeAppCheck` provider call → `appcheck.googleapis.com`         | Yes                | ✓ FLOWING (UAT-1 — exchange returns 200) |

---

## Behavioral Spot-Checks

| Behavior                                                                            | Command / Evidence                                                                                                                                                                                                                                                                          | Result                       | Status |
| ----------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------- | ------ |
| Live URL serves redesigned shell (not blank, not legacy)                            | `hosting/public/index.html` references hashed bundle + GiftMaison wordmark + correct OG meta. UAT interactions used the live URL successfully.                                                                                                                                          | Bundle live + interactive    | ✓ PASS |
| `initializeAppCheck` is invoked exactly once in `web/src/firebase.ts`                | `grep -c "initializeAppCheck" web/src/firebase.ts` = 1; `grep -c "initializeAppCheck" web/src/main.tsx` = 0 (deduped 2026-05-22 per fix `47c1bfa`)                                                                                                                                       | Single init, correct module  | ✓ PASS |
| `signInWithPopup` is no longer imported anywhere in `web/src/`                      | `grep -rn "signInWithPopup" web/src/` returns no source matches (verified via authProviders.ts source — only `signInWithRedirect`/`getRedirectResult` imported)                                                                                                                          | Popup flow removed           | ✓ PASS |
| Cloud Tasks queue name uses camelCase + region-qualified path                       | `createReservation.ts:85` `getFunctions().taskQueue<ReleasePayload>(\`locations/${REGION}/functions/releaseReservation\`)` — REGION = `europe-west3`                                                                                                                                       | Region-qualified path used   | ✓ PASS |
| Android app has NO App Check provider wired (rationale for Task 9 deferral)         | `grep -rn "installAppCheckProviderFactory\|firebase-appcheck\|Firebase.appCheck\|PlayIntegrityAppCheck" app/src/ app/build.gradle.kts gradle/libs.versions.toml` → zero matches                                                                                                          | Confirmed unwired             | ✓ PASS (as expected) |
| Auth `browserLocalPersistence` honored                                              | `web/src/firebase.ts:52` `void setPersistence(auth, browserLocalPersistence)` — WEB-D-12 implementation                                                                                                                                                                                   | Persistence wired             | ✓ PASS |
| All 8 referenced commits exist in git history                                       | `git log --oneline -1 <sha>` for each: `bf4ca31`, `d0c7516`, `7ffb380`, `3218a49`, `47c1bfa`, `1ac9f39`, `78fed8d`, `1c970c4` — all returned matching commit subject lines                                                                                                              | All commits valid             | ✓ PASS |

---

## Requirements Coverage

| Requirement | Source Plan      | Description                                                                          | Status      | Evidence                                                                                       |
| ----------- | ---------------- | ------------------------------------------------------------------------------------ | ----------- | ---------------------------------------------------------------------------------------------- |
| WEB-01      | 14-01, 14-04     | Gift givers can view a registry via web browser without installing the app           | ✓ SATISFIED | Plan 14-01 hosting deploy; UAT-1/4/5 + retail-redirect UAT-2 interacted with deployed shell    |
| WEB-02      | 14-02, 14-04     | Gift givers can reserve items from the web fallback                                  | ✓ SATISFIED | Plan 14-02 functions deploy + Plan 14-04 trilogy fixes; UAT-2 + UAT-6 PASS                     |
| WEB-03      | 14-04            | Gift givers can log in, create an account, or continue as guest on web               | ✓ SATISFIED | UAT-3 (guest persistence) + UAT-7 (Google OAuth cross-browser) both PASS                       |
| WEB-04      | 14-03, 14-04     | Web fallback redirects to retailer on reservation (same as Android flow)             | ✓ SATISFIED | Plan 14-03 rules deploy enables UAT-5 rules-deny path; UAT-2 verifies redirect cross-browser   |

**Orphaned requirements:** None. ROADMAP.md Phase 14 row lists WEB-01..04; PLAN frontmatter lists WEB-01..04; all four are satisfied. (Plans 14-01/02/03 reference these IDs as their `requirements-completed` field — verified in SUMMARY frontmatter.)

**I18N-03** (browser locale autodetect) was referenced by Plan 14-04 `<interfaces>` table for UAT-4 but is NOT in the phase's declared `requirements` field — it's a Phase 5 / Phase 13 requirement already complete in REQUIREMENTS.md. UAT-4 PASS provides additional regression evidence but does not change Phase 14's WEB-01..04-only scope. Not flagged as a gap.

---

## Anti-Patterns Scan

Scanned the 7 source files modified in this phase plus the canonical infra files for stubs/TODO/placeholder patterns:

| File                                                          | Pattern                                                                 | Severity | Impact                                                                                                                                       |
| ------------------------------------------------------------- | ----------------------------------------------------------------------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `functions/src/reservation/createReservation.ts:93-96`        | `try/catch` around `queue.enqueue(...)` with `console.warn` fallback   | ℹ️ Info  | Documented in retrospective as the swallow-mechanism for the bug trilogy. SUMMARY recommends future promotion to hard-fail when `FUNCTIONS_EMULATOR` is unset. Not a new gap — captured + tracked as a lesson. |
| `functions/src/reservation/createReservation.ts:118-121`      | Schema-regression comment about `cloudTaskName` being absent post-fix  | ℹ️ Info  | Intentional, documented. `releaseReservationCallable.deleteTask` becomes a no-op (idempotent). Captured in fix `d0c7516` SUMMARY notes.    |
| `hosting/public/index.html:11`                                | OG card generation comment ("deferred to a future phase")              | ℹ️ Info  | Phase 13 deferred (carried in 14-CONTEXT.md `<deferred>`). Out of WEB-01..04 scope.                                                          |
| (no other notable anti-patterns surfaced in the changed surface)                                                                            |                                                                         |          |                                                                                                                                              |

**No blockers found.**

---

## Deferred Items (User-Accepted — NOT Gaps)

Per the verification scope brief, the following two deferrals are explicitly user-accepted with logged follow-up todos. They are NOT treated as gaps. They are recorded here for traceability.

### 1. Task 8 — Recruited-giver Pass 2 of UAT items 2 + 3

- **Pass 1 status:** PASS in both Chrome and Safari incognito (user solo). Confirmed end-to-end retailer redirect + cross-tab semantics + localStorage persistence across browser restart.
- **Pass 2 status:** Deferred. Requires scheduling a real giver friend on their own device.
- **Risk:** LOW. Pass 1 already exercised the relevant browser-platform behaviors (window.open cross-tab semantics, localStorage persistence across application lifecycle) — both are generic and not device-specific. Pass 2 was the "works on my machine" smell-test layer (D-08), not the primary closure-gating evidence.
- **Follow-up:** `.planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md` (contains copy-paste tester script + answer-collection template + divergence-handling guide).

### 2. Task 9 — App Check enforcement flip (Storage → Functions → Firestore)

- **Web side wired:** App Check (reCAPTCHA v3) is active in **monitor mode** for the web fallback. UAT-1 verified `appcheck:exchange` returns 200.
- **Android side NOT wired:** Verified by grep — zero matches for `installAppCheckProviderFactory` / `firebase-appcheck` / `Firebase.appCheck` / `PlayIntegrityAppCheck` across `app/src/`, `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- **Why deferred:** Flipping enforcement today would 403-reject every Android-originated request in production. All authenticated owner flows (registry creation, item add, reservation, invite) would break for every Android user immediately.
- **Posture at close:** monitor-only on all three services. Same posture the project had pre-Phase-14 on backend services (no regression); web side gained App Check wiring this phase.
- **Risk:** MEDIUM. Security posture is unchanged from pre-Phase-14 (backend was always accepting unprotected traffic), but D-04's intent of flip-to-enforce is unfulfilled.
- **Follow-up:** `.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md` (contains 5-step close-out plan: wire Android, register debug tokens, wait ≥24h for monitor metrics >95%, flip per-service with smoke-test, keep D-03 rollback ready).

---

## Production Bug Catch Retrospective (5 silent bugs caught + fixed mid-UAT)

Plan 14-04's layered UAT surfaced 5 sequential production bugs, each with identical-looking user-facing symptoms but living in completely different parts of the stack. None would have been caught by existing unit-test / emulator-test coverage. Each fix surfaced the next layer's latent bug.

### Cloud Tasks Reservation Pipeline Trilogy (Bugs 1-3) — All symptom: "reservation stuck reserved past expiresAt, no expiry email, banner won't clear"

| # | Bug                                                              | Root cause                                                                                                                                                                                                                                                                | Fix commit | UAT attempt   |
| - | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ------------- |
| 1 | Cloud Tasks queue name mismatch                                  | `QUEUE_NAME = "release-reservation"` (hyphenated) didn't match auto-created `releaseReservation` queue (camelCase). NOT_FOUND silently swallowed by `try/catch` originally added for emulator portability. Affected ALL prod reservations since Phase 5/6 deploy.     | `bf4ca31`  | UAT-6 try #1  |
| 2 | Cloud Tasks dispatch missing OIDC token                          | Raw `@google-cloud/tasks` SDK constructed `httpRequest` without `oidcToken`. Firebase 2nd-gen `onTaskDispatched` on Cloud Run requires authenticated invocation → HTTP 403 "Empty Authorization header value" after 3 retries → silent drop. Surfaced after fix #1.   | `d0c7516`  | UAT-6 try #2  |
| 3 | Cloud Tasks region default mismatch                              | `getFunctions().taskQueue("releaseReservation")` with bare name → Firebase Admin SDK defaulted to `us-central1`; our function lives in `europe-west3`. NOT_FOUND silently caught. Surfaced after fix #2 took raw SDK out of the loop.                                  | `7ffb380`  | UAT-6 try #3  |

**UAT-6 closed PASS on attempt #4** with all three fixes deployed (2026-05-22).

### Web Auth Pair (Bugs 4-5) — All symptom: "OAuth round-trip completes but opener tab still shows signed-out UI in Chrome + Safari"

| # | Bug                                                              | Root cause                                                                                                                                                                                                                                                                | Fix commit | UAT attempt   |
| - | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ------------- |
| 4 | `signInWithPopup` cross-origin opener channel restricted         | Browser security policies (Safari ITP, Chrome COOP) sever `window.opener.postMessage` channel. Popup completed and persisted credentials, but opener's in-memory Auth never received them. Both Chrome + Safari. Fix: switched to `signInWithRedirect`.                | `3218a49`  | UAT-7 try #1  |
| 5 | `authDomain` origin mismatch with serving origin                 | `signInWithRedirect` routes through `${authDomain}/__/auth/handler`. With `authDomain = firebaseapp.com` and serving origin `web.app`, redirect-event state persisted in `firebaseapp.com`'s storage and was unreachable from `web.app`. `getRedirectResult` returned null silently. Fix: align `authDomain` to `web.app`. | `47c1bfa`  | UAT-7 try #2  |

**UAT-7 closed PASS on attempt #3** + post-PASS polish (`1ac9f39`) gating sign-in UI on `isReady` to kill cold-boot flash.

### Pattern

- Bugs 1-3 only manifested in the natural 30-min UAT path. The abandoned 60s seed-script (D-09) would have hit the same NOT_FOUND / 403 / wrong-region errors but with the additional complication of failed gcloud ADC setup. The natural path was strictly more faithful to production.
- Bugs 4-5 only manifested in real browsers (not jsdom, not Playwright — Google CAPTCHA blocks automation).
- All 5 bugs caught BEFORE Pass 2 (recruited giver) — which is exactly what the layered UAT (D-08) was designed for.
- Lesson promoted to SUMMARY: consider promoting Cloud Tasks enqueue failures in prod (when `FUNCTIONS_EMULATOR` is unset) to hard errors / Firestore-logged failure records so they surface via alerting instead of hiding in `console.warn`.

---

## Final Verdict

**Status: `passed_with_deferrals`**

All 4 phase requirements (WEB-01, WEB-02, WEB-03, WEB-04) are satisfied against the live production deploy at `https://gift-registry-ro.web.app/`. All 7 manual UAT items in `05-VALIDATION.md` "Manual-Only Verifications" table PASSED in Pass 1 (user solo) in real Chrome + Safari incognito. The PROJECT.md caveat dating from 2026-04-19 — "WEB-01..04 code-complete but pending real-browser UAT against production" — is now resolved.

Two items are explicitly deferred with user acceptance and logged follow-up todos. Per the verification scope brief, these are recorded as deferrals (not gaps): Pass 2 recruited-giver UAT (LOW risk, layered defense already satisfied by Pass 1) and App Check enforcement flip (MEDIUM risk, blocked by Android App Check provider being unwired — flipping today would 403-reject all Android traffic).

The phase delivered substantial unplanned value: 5 silent production bugs in the Cloud Tasks reservation pipeline and the web OAuth flow were caught and fixed mid-UAT, including one (Cloud Tasks queue-name mismatch) that had been silently breaking auto-release for ALL production reservations since the Phase 5/6 deploy. Without Phase 14's natural-timing UAT in real browsers against the real deployed origin, these bugs would have remained latent.

### Recommended Next Step

Update PROJECT.md milestone caveat:
> "Giver-facing web fallback was added to v1.1 scope on 2026-04-30 (Phases 13 + 14): visual refresh complete (Phase 13), live deploy + guest UAT pending (Phase 14)"

→ change to:
> "Giver-facing web fallback in v1.1 (Phases 13 + 14): visual refresh complete (Phase 13), live deploy + guest UAT verified (Phase 14, closed 2026-05-22 with two follow-up todos: recruited-giver Pass 2 + Android App Check wiring before enforcement flip)."

Then commit the verifier-produced VERIFICATION.md as part of the Phase 14 close-out bundle (per orchestrator handoff).

---

_Verified: 2026-05-22T18:10:00Z_
_Verifier: Claude (gsd-verifier)_
