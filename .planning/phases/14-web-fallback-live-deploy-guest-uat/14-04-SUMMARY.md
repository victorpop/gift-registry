---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 04
subsystem: infra
tags: [app-check, recaptcha-v3, cloud-tasks, oauth, signinwithredirect, firebase-hosting, uat, production]

# Dependency graph
requires:
  - phase: 14-web-fallback-live-deploy-guest-uat
    provides: Plans 14-01/02/03 — Firebase Web app registered, hosting deployed, Functions deployed (with tsconfig cleanup + env handling), Firestore + Storage rules deployed to gift-registry-ro
  - phase: 13-web-fallback-visual-refresh
    provides: GiftMaison-restyled React bundle (3 redesigned screens + sticky reservation banner + shared chrome) — the bundle this plan deploys + UATs against production
  - phase: 05-web-fallback
    provides: WEB-01..04 code-complete + the 7 manual-only UAT items in 05-VALIDATION.md "Manual-Only Verifications" table — the closure spec for this plan
provides:
  - WEB-01/02/03/04 closed against PRODUCTION (was code-complete in Phase 5 but pending real-browser UAT since 2026-04-19)
  - App Check wired on the web side via reCAPTCHA v3 (web/.env.local + web/src/firebase.ts initializeAppCheck call); web bundle redeployed; monitor mode active
  - Google OAuth on web migrated from signInWithPopup to signInWithRedirect with authDomain aligned to web.app serving origin — works cross-browser (Chrome + Safari)
  - Reservation auto-release pipeline fully working in production (was silently broken since Phase 5/6 deploy due to 3 sequential bugs in the Cloud Tasks integration — all caught + fixed during this plan)
  - 14-04-UAT-RESULTS.md ledger documenting all evidence for the 7 UAT items + the 5 silent prod bugs + the 2 deferral decisions

affects:
  - Phase 14 verification (next orchestrator step) — verifier reads this SUMMARY + UAT-RESULTS to confirm phase closure
  - Future Android App Check work (deferred todo 2026-05-22-wire-android-app-check-and-flip-enforcement.md) — blocks the App Check enforcement flip until Android wiring exists
  - Any future Cloud Tasks integration — three lessons captured in retrospective (queue resource resolution, OIDC tokens, region-qualified lookups; promote-enqueue-failures pattern)
  - v1.1 PROJECT.md milestone caveat "Giver-facing web fallback added to v1.1 scope on 2026-04-30 (Phases 13 + 14)" — Phase 14 closure resolves the "live deploy + guest UAT pending" half

# Tech tracking
tech-stack:
  added:
    - firebase/app-check (sub-entry of firebase v12.12.0 — already installed; no new npm dep)
    - reCAPTCHA v3 (site key registered in Firebase Console + GCP reCAPTCHA admin)
  patterns:
    - "signInWithRedirect over signInWithPopup for cross-browser OAuth on web (popup-to-opener postMessage channel restricted by modern browser policy)"
    - "authDomain MUST equal serving origin for Firebase Auth redirect-event state to persist across the OAuth round-trip (cross-origin storage isolation)"
    - "Cloud Tasks via Firebase Admin SDK getFunctions().taskQueue() — region-qualified resource path required (locations/<region>/functions/<name>), NOT bare queue name"
    - "Plan-frontmatter must_haves can be annotated DEFERRED:<reason> + todo-pointer rather than deleted when work is pushed to a follow-up — preserves intent record"

key-files:
  created:
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-SUMMARY.md (this file)
    - .planning/todos/pending/2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile.md
    - .planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md
    - .planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md
  modified:
    - web/.env.local (VITE_RECAPTCHA_SITE_KEY filled in + VITE_FIREBASE_AUTH_DOMAIN aligned from firebaseapp.com → web.app; .env.local is git-ignored so changes ship via rebuilt bundle, not source commit)
    - web/src/firebase.ts (initializeAppCheck with ReCaptchaV3Provider added; App Check init deduped from main.tsx)
    - web/src/main.tsx (slimmed — removed second initializeAppCheck call; getRedirectResult wired at module-load for OAuth return leg)
    - web/src/features/auth/authProviders.ts (signInWithPopup → signInWithRedirect)
    - web/src/features/auth/AuthScreen.tsx (useAuth.isReady gate to kill post-redirect flash)
    - web/src/components/giftmaison/TopNav.tsx (useAuth.isReady gate on auth-area slot only)
    - functions/src/reservation/createReservation.ts (queue-name + OIDC + region — three sequential prod-bug fixes)

key-decisions:
  - "D-09 amendment 2026-05-21: 60s seed-script approach abandoned (Cloud Tasks API ADC setup failed on dev machine and left a phantom prod reservation); UAT-6 verified via natural 30-min production timer through the prod-pointed Android APK instead — same deployed pipeline, just at real production timing. Seed script reverted in commit 1c970c4."
  - "Plan 14-04 close-out 2026-05-22: Task 8 (recruited-giver Pass 2) deferred — Pass 1 PASS in both browsers is sufficient to close the plan; coordinating a real giver session would block Phase 14 closure on scheduling overhead. Risk LOW."
  - "Plan 14-04 close-out 2026-05-22: Task 9 (App Check enforcement flip) deferred — Android app has no App Check provider wired (grep returns zero matches). Flipping enforcement would 403-reject every Android request in production. Wiring Android Play Integrity + Debug providers is a substantial follow-up tracked as its own pending todo."
  - "Google OAuth on web switched from signInWithPopup to signInWithRedirect: popup-to-opener postMessage channel restricted by modern browser policy (Safari ITP, Chrome COOP). Redirect is the only flow that works on every browser the giver might use; desktop UX trade-off (full-page navigation instead of popup) accepted in exchange for cross-browser reliability."
  - "VITE_FIREBASE_AUTH_DOMAIN changed from default gift-registry-ro.firebaseapp.com → gift-registry-ro.web.app to align with serving origin: signInWithRedirect saves redirect-event state in Web Storage keyed by origin, so authDomain MUST equal the serving origin for getRedirectResult to find the state on the return leg."

patterns-established:
  - "Layered UAT design (D-08): Pass 1 (user solo in incognito) catches obvious bugs in a controlled environment; Pass 2 (recruited real giver on own device) catches 'works on my machine' bias. Pass 1 alone surfaced 5 silent prod bugs in this plan — Pass 2 is the safety net beyond that."
  - "When natural production timing is short enough (UAT-6: 30 min), use it instead of building a compressed-time helper. The natural path tests the EXACT pipeline production users experience and surfaces real bugs (like the Cloud Tasks trilogy) that a shortcut path would still have hit but might have hidden behind tooling complexity."
  - "App Check production posture progression: monitor mode FIRST across all platforms (D-04) → wait for verified % >95% across 24h → flip enforcement per-service (Storage → Functions → Firestore) with smoke-test between each. Per-service rollback via Console > Unenforce (D-03) is immediate. Never flip-enforce until every platform sending traffic to that service is App-Check-wired."
  - "Prod-bug enqueue-failure pattern lesson: try/catch around enqueue calls (originally added for emulator portability) silently swallowed all 3 Cloud Tasks bugs in production, converting hard failures into 'reservation committed but no task' invisible-to-user states. Consider promoting enqueue failures in prod (when FUNCTIONS_EMULATOR is unset) to a hard error OR a Firestore-logged failure record so they surface via alerting."

requirements-completed: [WEB-01, WEB-02, WEB-03, WEB-04]

# Metrics
duration: ~2 days (2026-05-21 — 2026-05-22, multi-session with checkpoints)
completed: 2026-05-22
---

# Phase 14 Plan 04: Layered UAT + App Check enforcement Summary

**Closed WEB-01..04 against production with all 7 manual-UAT items PASS in real Chrome + Safari; web App Check wired (reCAPTCHA v3, monitor mode); 5 silent production bugs in the Cloud Tasks reservation pipeline + the OAuth flow caught and fixed mid-UAT; recruited-giver Pass 2 + App Check enforcement flip deferred to follow-up todos.**

## Performance

- **Duration:** ~2 days across multiple sessions (started 2026-05-21, closed 2026-05-22)
- **Tasks completed:** 7/10 PASS + 2 deferred + 1 close-out
- **Files modified (this plan):** 7 source files + 3 planning artifacts + 5 todos created/moved
- **Commits this plan:** 20+ (including 5 prod-bug fixes, 4 hosting redeploys, 1 functions redeploy, and the docs/close-out trail)

## Objective recap

Close all 7 manual UAT items defined in `.planning/phases/05-web-fallback/05-VALIDATION.md`
"Manual-Only Verifications" table by running a layered UAT (D-08: solo Pass 1
in incognito + recruited-giver Pass 2) against the now-live production deploy
from Plans 14-01/02/03. Operationalize App Check by registering reCAPTCHA v3,
smoke-testing in monitor mode, then flipping enforcement on for Firestore +
Functions + Storage (D-04). Verify the email re-reserve flow end-to-end through
the deployed `releaseReservation` handler.

Plan 14-04 is the official closure of WEB-01/02/03/04 against PRODUCTION
(requirements were code-complete in Phase 5 but the "pending real-browser UAT"
caveat in `PROJECT.md` has been open since 2026-04-19).

## Delivered

- **App Check on web wired** — `web/.env.local` populated with the reCAPTCHA v3
  site key registered in Firebase Console; `web/src/firebase.ts` calls
  `initializeAppCheck(app, { provider: new ReCaptchaV3Provider(...), isTokenAutoRefreshEnabled: true })`
  with emulator + missing-key guards. Web bundle rebuilt + redeployed
  (commit `78fed8d`). App Check init deduped from `main.tsx` into `firebase.ts`
  (commit `47c1bfa`).
- **UAT items 1-7 PASS** — all 7 items in the closure spec passed Pass 1 (solo,
  user in fresh incognito) in both Chrome and Safari against the production
  deploy at `https://gift-registry-ro.web.app/`. See
  [`14-04-UAT-RESULTS.md`](./14-04-UAT-RESULTS.md) "Pass 1 — Solo Incognito"
  table for evidence per item.
- **5 silent production bugs caught + fixed + deployed mid-UAT** (full
  retrospective below):
  1. Cloud Tasks queue name mismatch — `bf4ca31` (UAT-6)
  2. Cloud Tasks dispatch missing OIDC token — `d0c7516` (UAT-6 retest)
  3. Cloud Tasks region default mismatch — `7ffb380` (UAT-6 third attempt)
  4. `signInWithPopup` cross-origin opener channel — `3218a49` (UAT-7)
  5. `authDomain` origin mismatch with serving origin — `47c1bfa` (UAT-7 retest)
- **Post-PASS polish:** cold-boot flash on `/sign-in` during redirect return
  gated on `useAuth.isReady` — commit `1ac9f39`. Cosmetic only; prod-bug tally
  stays at 5.
- **Three folded todos closed** — moved from `pending/` to `completed/`:
  `2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md`,
  `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md`,
  `2026-04-28-deploy-phase-12-storage-rules.md`.
- **One deferred bug surfaced + logged** — invite-email CTA stripped by Gmail
  mobile (sender-authentication issue: SPF/DKIM/DMARC), tracked in
  `.planning/todos/pending/2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile.md`.
  Not a blocker for Phase 14 (affects email deliverability — REG-06/07 +
  NOTF-02/03 — not WEB-01..04).

## Deferred

- **Task 8 — Recruited-giver Pass 2 of UAT items 2 + 3.** Pass 1 (user in
  incognito, Chrome + Safari) PASSED both items end-to-end. Pass 2 (real
  giver friend on own device) deferred to unblock Phase 14 closure.
  Follow-up: [`.planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md`](../../todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md).
  Risk LOW — Pass 2 was the "works on my machine" smell-test, not the primary
  validation.

- **Task 9 — App Check enforcement flip (Storage → Functions → Firestore).**
  Android app has NO App Check provider wired (grep returns zero matches for
  `installAppCheckProviderFactory` / `firebase-appcheck` / `Firebase.appCheck`
  across `app/src/`, `app/build.gradle.kts`, `gradle/libs.versions.toml`).
  Flipping enforcement today would 403-reject every Android-originated request
  in production. Posture at close: monitor-only for all three services.
  Follow-up: [`.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md`](../../todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md)
  contains the full step-by-step close-out plan (wire Android first, then flip).
  Risk MEDIUM — backend currently accepts unprotected traffic in production
  (same posture as before the plan started on the web side; no regression),
  but D-04's intent of flip-to-enforce is unfulfilled.

## Files created or modified

### Web (React / Vite)

- `web/.env.local` — `VITE_RECAPTCHA_SITE_KEY` filled in;
  `VITE_FIREBASE_AUTH_DOMAIN` aligned `firebaseapp.com` → `web.app`. The file
  is git-ignored so changes ship via the rebuilt bundle, not a source commit.
- `web/src/firebase.ts` — `initializeAppCheck(app, { provider: new ReCaptchaV3Provider(...) })`
  call added (Task 3); App Check init consolidated here (deduped from `main.tsx`).
- `web/src/main.tsx` — slimmed: second `initializeAppCheck` call removed;
  `getRedirectResult(auth)` wired at module-load time for the OAuth return leg.
- `web/src/features/auth/authProviders.ts` — `signInWithPopup` → `signInWithRedirect`
  refactor.
- `web/src/features/auth/AuthScreen.tsx` — `useAuth.isReady` gate to kill
  post-redirect flash of the sign-in form during the return leg.
- `web/src/components/giftmaison/TopNav.tsx` — `useAuth.isReady` gate on the
  auth-area slot ONLY (wordmark + language switcher render normally during
  cold-boot).

### Cloud Functions

- `functions/src/reservation/createReservation.ts` — three sequential
  prod-bug fixes (queue name, OIDC token, region qualifier). Each fix
  surfaced the next layer's bug. Final state: uses Firebase Admin SDK
  `getFunctions().taskQueue<ReleasePayload>(\`locations/${REGION}/functions/releaseReservation\`).enqueue(payload, { scheduleTime })`
  — region-qualified path + automatic OIDC token + automatic queue resolution.

### Planning artifacts

- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`
  — full UAT ledger with Pass 1 evidence per item, the 5 prod-bug post-mortems,
  UAT-6 natural-30-min plan, deferral sections, and the Plan 14-04 Final Tally.
- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-SUMMARY.md`
  — this file.
- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-layered-uat-and-appcheck-enforcement-PLAN.md`
  — `must_haves.truths` annotated with `DEFERRED:` markers + todo pointers
  for items 8 + 9; abandoned `seedNearExpiryReservation.ts` artifact entry
  commented out with rationale; `files_modified` updated to reflect actual
  shipped surface.
- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md`
  — already updated mid-plan with the D-09 amendment (2026-05-21).

### Todos

- Created (this plan):
  - `.planning/todos/pending/2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile.md`
  - `.planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md`
  - `.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md`
- Moved `pending/` → `completed/` (closed by this plan + predecessors):
  - `2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md` (closed by Plan 14-01)
  - `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` (closed by Plan 14-02)
  - `2026-04-28-deploy-phase-12-storage-rules.md` (closed by Plan 14-03)

## Commits

Chronological (oldest → newest), filtered to Plan 14-04 work:

| Commit | Type | What |
|--------|------|------|
| (earlier) | docs | Plan 14-04 authored + revised |
| `78fed8d` | feat(14-04) | Wire App Check reCAPTCHA v3 into firebase.ts, redeploy hosting (Task 3) |
| (between) | docs(14-04) | UAT-1 PASS recorded; UAT-2/3/4/5 PASS recorded |
| `bf4ca31` | fix(14-04) | Cloud Tasks queue name mismatch — `release-reservation` (hyphenated) → `releaseReservation` (camelCase); deployed `functions:createReservation`; UAT-6 bug #1 of trilogy |
| `c9e6d97` | docs(14-04) | Record OIDC bug fix in UAT results |
| `d0c7516` | fix(14-04) | Cloud Tasks dispatch missing OIDC token — switched raw `@google-cloud/tasks` CloudTasksClient to Firebase Admin SDK `getFunctions().taskQueue(...).enqueue(...)` for automatic OIDC token + service account binding; deployed; UAT-6 bug #2 of trilogy |
| `7ffb380` | fix(14-04) | Cloud Tasks region default mismatch — region-qualify taskQueue lookup to `locations/europe-west3/functions/releaseReservation`; deployed; UAT-6 bug #3 of trilogy |
| `73bc8e7` | docs(14-04) | Record region-qualifier bug fix in UAT results |
| `b00c45a` | docs(14-04) | Close UAT-6 PASS — natural 30-min re-reserve loop verified end-to-end |
| `3218a49` | fix(14-04) | Switch Google auth from `signInWithPopup` to `signInWithRedirect`; deployed hosting; UAT-7 bug #4 |
| `5d966ab` | docs(14-04) | Record signInWithRedirect fix in UAT results |
| `47c1bfa` | fix(14-04) | Align authDomain with web.app origin + dedupe App Check init; deployed hosting; UAT-7 bug #5 |
| `6af0905` | docs(14-04) | Record authDomain alignment fix in UAT results |
| `1ac9f39` | fix(14-04) | Gate AuthScreen + TopNav auth area on useAuth.isReady to kill post-redirect flash; deployed hosting; post-PASS polish |
| `73f6015` | docs(14-04) | Close UAT-7 PASS + record post-redirect flash polish |
| `ca93bb3` | docs(14-04) | Close UAT-7, defer Task 8 + Task 9, log follow-up todos (this turn) |
| `c2d8c3e` | docs(14-04) | Annotate plan must_haves with deferral pointers (this turn) |
| `189ae6f` | chore(14-04) | Move 3 folded todos pending → completed (this turn) |
| (next)     | docs(14-04) | Write 14-04-SUMMARY.md (this commit) |
| (next)     | docs(state) | Plan 14-04 closed; Phase 14 ready for verification |

Note: hosting + functions deploys are operational side-effects of the commits
above — each `fix(14-04)` above includes a `firebase deploy --only ...`
recorded in the corresponding `/tmp/14-04-*.log` deploy log. Per-deploy logs
referenced in the UAT-RESULTS prod-bug post-mortems.

## Key decisions made during execution

1. **D-09 amendment 2026-05-21 — natural 30-min path replaces 60s seed script.**
   The original D-09 plan was to seed a near-expiry reservation via a
   60-second-delay Cloud Task targeting the deployed `releaseReservation`
   handler. Abandoned during execution: Cloud Tasks API requires gcloud
   Application Default Credentials which weren't set up on the dev machine,
   and a failed seed attempt left a phantom reservation in prod that had to
   be manually cleaned. Replaced with the natural 30-min production timer
   through the prod-pointed Android APK — same deployed pipeline, just at
   real production timing. Seed script reverted in commit `1c970c4`.
   Documented in `14-CONTEXT.md` "D-09 amendment" section and the
   `14-04-UAT-RESULTS.md` "UAT-6 plan" section. This decision arguably made
   the plan MORE faithful — the 30-min path is what production users
   actually experience, and it surfaced the Cloud Tasks bug trilogy that
   the seed shortcut might have hidden behind tooling complexity.

2. **Plan 14-04 close-out 2026-05-22 — Task 8 (recruited-giver Pass 2) deferred.**
   Pass 1 PASS in both Chrome + Safari is sufficient evidence to close the
   plan against the WEB-01..04 closure spec; coordinating a real giver
   session would block Phase 14 closure on scheduling overhead. Pass 2 was
   layered defense (D-08), not primary validation. Tracked in
   `.planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md`.

3. **Plan 14-04 close-out 2026-05-22 — Task 9 (App Check enforcement flip)
   deferred.** Android app has no App Check provider wired (grep-verified).
   Flipping enforcement today would 403-reject every Android request in
   production. Wiring Android Play Integrity + Debug providers + registering
   debug tokens + waiting ≥24h for monitor metrics is a substantial follow-up
   that warrants its own pending todo (and possibly its own Phase 14.1 entry
   if the scope grows). Tracked in
   `.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md`.

## Rollback runbook (D-03 — preserved for posterity even though enforcement was deferred)

- **App Check enforcement rollback (per-service, immediate):** Firebase Console
  > App Check > APIs tab > <Service> > "Unenforce". Takes effect within ~1
  minute. No code change required. Additionally: tell affected web users to
  clear site data — Firebase JS SDK aggressively caches 400-class App Check
  failures and will NOT auto-retry after enforcement is flipped off (see
  the user's `reference_appcheck_cached_failure` memory). Android users are
  not affected by this caching bug (different SDK).
- **reCAPTCHA site key removal (last resort):** Firebase Console > App Check
  > Apps > <Web app> > Unregister.
- **Code-level rollback for Plan 14-04 hosting redeploys:**
  `git checkout <previous SHA> -- web/ && cd web && npm run build && firebase deploy --only hosting --project gift-registry-ro`.
  Previous known-good SHAs before this plan's hosting work: `c0b7066` (web
  bundle from Plan 14-02 closure). Per-deploy known-good points: `78fed8d`
  (App Check wired, pre-OAuth fixes), `3218a49` (signInWithRedirect,
  pre-authDomain fix), `47c1bfa` (authDomain aligned, pre-flash polish),
  `1ac9f39` (current production).
- **Code-level rollback for Plan 14-04 functions redeploys:**
  `git checkout <previous SHA> -- functions/src/reservation/createReservation.ts && cd functions && npm run build && firebase deploy --only functions:createReservation --project gift-registry-ro`.
  Per-fix known-good points: pre-`bf4ca31` (broken queue name — DO NOT
  ROLLBACK TO), pre-`d0c7516` (broken OIDC — DO NOT ROLLBACK TO),
  pre-`7ffb380` (broken region — DO NOT ROLLBACK TO). The trilogy is
  one-way: each fix surfaced the next bug; rolling back any one of the
  three re-breaks the auto-release pipeline. If a rollback is needed for
  some new reason, roll back to `c0b7066` (pre-Plan-14-04 Functions
  state — broken auto-release will return, same broken-from-Phase-5/6
  behavior, accept as known limitation).

## Retrospective — the "5 silent prod bugs" story

Plan 14-04's layered-UAT approach paid for itself many times over. Five
sequential production bugs were caught + fixed mid-plan, each with
identical-looking user-facing symptoms but living in completely different
parts of the stack. None of them would have been caught by the existing
unit-test + emulator-test coverage. Each was visible ONLY when production
UAT exercised the real cross-vendor handshake against the real deployed
origin.

### Cloud Tasks reservation pipeline trilogy (bugs 1-3)

All three bugs manifested with the same end-user symptom: reservation stuck
`status="reserved"` past `expiresAt`, no expiry email, app banner reads
"0 MIN LEFT" forever. But they lived in three different layers of the
Cloud Tasks integration:

1. **Queue resource resolution** (`bf4ca31`) — `QUEUE_NAME = "release-reservation"`
   (hyphenated) didn't match the auto-created `releaseReservation` queue
   (camelCase). NOT_FOUND silently caught by a try/catch that was originally
   added in good faith for emulator portability. Affected ALL production
   reservations since the Phase 5/6 deploy. Only manifested as a
   UAT-blocking issue once end-to-end production verification (Phase 14)
   actually waited the natural 30 minutes.

2. **Dispatch-time authentication** (`d0c7516`) — fixing the queue name
   surfaced this latent bug on the very next UAT attempt. The raw
   `@google-cloud/tasks` SDK constructed `httpRequest` without an
   `oidcToken`; Firebase 2nd-gen `onTaskDispatched` functions deploy on
   Cloud Run revisions requiring authenticated invocation; unauthenticated
   POST → HTTP 403 "Empty Authorization header value". The task WAS
   enqueued correctly (so `cloudTaskName` populated, queue showed the
   task) but every dispatch attempt was denied at Cloud Run ingress, and
   after the retry budget exhausted the task silently dropped. Refactor
   to Firebase Admin SDK `getFunctions().taskQueue(...).enqueue(...)`
   which handles OIDC automatically.

3. **Region-qualified resource resolution** (`7ffb380`) — fixing OIDC
   surfaced THIS latent bug on the very next UAT attempt. Switching to
   `getFunctions().taskQueue("releaseReservation")` to get automatic
   OIDC handed region resolution back to the Admin SDK, which defaulted
   to `us-central1`. Our function + auto-created queue live in
   `europe-west3`. The SDK was probing
   `projects/gift-registry-ro/locations/us-central1/queues/releaseReservation`
   — right project, wrong region — and getting NOT_FOUND. The try/catch
   silently swallowed it, same shape as bug #1. Fix: explicit
   region-qualified path
   `locations/${REGION}/functions/releaseReservation`.

Lessons for future Cloud Tasks integrations:

- The `try/catch` around enqueue (added in good faith for emulator
  portability) silently swallowed all three bugs in prod, converting
  hard failures into "reservation committed but no task". Consider
  promoting enqueue failures in prod (when `FUNCTIONS_EMULATOR` is
  unset) to a hard error OR a Firestore-logged failure record so they
  surface via alerting instead of hiding in `console.warn`.
- Unit-level stubs for `CloudTasksClient` / `getFunctions().taskQueue()`
  cannot catch this class of bug. Emulator does not reproduce Cloud
  Run's auth ingress or GCP region resolution at all.
- Each fix surfaced the next layer's bug because the prior bug was
  masking it. Future Cloud Tasks integrations should include a one-shot
  deployed smoke-test (reserve → 60s task → assert release) before the
  natural-flow UAT.

### Web auth pair (bugs 4-5)

Both bugs manifested in BOTH Chrome and Safari incognito and were caught
ONLY in real browsers (not jsdom / not Playwright):

4. **`signInWithPopup` cross-origin opener channel restricted by browser
   policy** (`3218a49`) — popup completed and credentials persisted to
   `browserLocalPersistence`, but the opener tab's in-memory `Auth`
   instance never received them; manual refresh worked. Root cause: modern
   browser security policies (Safari ITP, Chrome COOP) sever the
   `window.opener.postMessage` channel that `signInWithPopup` relies on.
   Fix: switch to `signInWithRedirect` + `getRedirectResult` on boot.
   Trade-off accepted: full-page redirect instead of popup, less elegant
   on desktop but the only flow that works on every browser the giver
   might use (popup auth has been intermittently broken on iOS Safari
   since 2020).

5. **`authDomain` origin mismatch with serving origin** (`47c1bfa`) —
   switching to `signInWithRedirect` was the canonical mobile-safe fix
   for bug #4, and surfaced THIS latent bug. The app was served from
   `gift-registry-ro.web.app` but `VITE_FIREBASE_AUTH_DOMAIN` was the
   default `gift-registry-ro.firebaseapp.com`. `signInWithRedirect`
   navigates to `https://${authDomain}/__/auth/handler`; the Firebase
   SDK saves redirect-event state in `sessionStorage` + `localStorage`
   keyed by origin; browsers isolate Web Storage per origin. Result:
   credentials persisted in `firebaseapp.com`'s storage, then the
   redirect back to `web.app/...` couldn't find the state marker;
   `getRedirectResult` silently returned null. Fix: align
   `VITE_FIREBASE_AUTH_DOMAIN` to `web.app`. The bug was always latent
   under `signInWithPopup` (which opens `accounts.google.com` directly
   without routing through `firebaseapp.com/__/auth/handler`) — fix #4
   exposed it.

Lessons for future web-auth integrations:

- When a release ships behind any browser-vendor / cloud-vendor policy
  boundary, production UAT in real browsers against the real deployed
  origin is irreplaceable. Unit-stub-only test coverage cannot catch
  this class of bug — neither can Playwright if Google CAPTCHA blocks
  automation.
- Each fix in the same layer (web auth) can surface a deeper layer's
  latent bug — both browser-policy changes (bug 4) and storage-origin
  isolation (bug 5) are invisible until you exercise the real
  cross-origin handshake.
- `authDomain` MUST equal the serving origin for Firebase Auth's
  redirect flow to work. The Firebase SDK config defaults to
  `<project>.firebaseapp.com` which is fine if you're hosting there, but
  every project using a custom or `.web.app` Firebase Hosting URL needs
  to override this.

### Overall pattern

Bugs 1-3 only manifested in the natural 30-min UAT path; bugs 4-5 only
manifested in real browsers. All five bugs were caught BEFORE Pass 2
(recruited real giver), which is exactly what the layered-UAT approach
is for. The 5-bug discovery is also part of why Pass 2 was deferred at
close-out — Pass 1 had already done so much work surfacing real prod
bugs that Pass 2's marginal value (works on someone else's device) was
proportionally smaller.

## Issues encountered

- **Phantom reservation in prod from failed 60s seed script attempt
  (2026-05-21).** Cloud Tasks API ADC setup failed on the dev machine
  during the original D-09 path; the script left a partially-written
  reservation in prod that had to be manually cleaned via Firestore
  Console. Resolved by abandoning the seed path and amending D-09 to
  use the natural 30-min production timer.
- **Stuck reservations from each Cloud Tasks bug attempt.** Bugs 1-3
  each left an item in `status="reserved"` past `expiresAt` with no
  Cloud Task scheduled (or with the task dropped after 3-retry
  exhaustion). Cleaned manually via Firestore Console between attempts.
- **App Check cached failure recovery (web).** When the App Check
  secret key was first misconfigured, the Firebase JS SDK cached the
  400 response and would not retry until IndexedDB was cleared. Logged
  to user's memory at
  `~/.claude/projects/-Users-victorpop-ai-projects-gift-registry/memory/reference_appcheck_cached_failure.md`
  for future reference.

## What this enables next

- **Phase 14 verification (next orchestrator step).** Verifier reads this
  SUMMARY + UAT-RESULTS to confirm phase closure criteria 1-5 from
  `14-CONTEXT.md` domain block:
  1. `https://gift-registry-ro.web.app/` renders the redesigned Phase 13
     bundle — MET (Plans 14-01 + 14-04 redeploys).
  2. Backing Cloud Functions deploy succeeds without manual workarounds
     — MET (Plan 14-02 + Plan 14-04 trilogy redeploys).
  3. Firestore + Storage rules deployed to production — MET (Plan 14-03).
  4. All 7 Phase-5 manual UAT items verified green in real Chrome + Safari
     against the production deploy — MET (Pass 1 PASS; Pass 2 deferred
     per close-out decision with risk LOW).
  5. App Check enforced (after a brief monitor-only smoke-test window)
     — PARTIALLY MET. App Check wired and monitor mode active on the
     web side; enforcement flip deferred until Android App Check provider
     is wired (per pending todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md`).
- **Phase 14.1 candidate (or quick-tasks) — closing the deferred items.**
  Two follow-up todos in `pending/` need eventual resolution. The Android
  App Check work is non-trivial (new Gradle deps + Application subclass
  changes + Console registration + debug-token management for every dev
  device) and may warrant a dedicated Phase 14.1 entry rather than a
  quick-task. The recruited-giver Pass 2 is a coordination task with no
  code surface — best fit for a quick-task once a giver is scheduled.
- **PROJECT.md milestone caveat resolution.** "Giver-facing web fallback
  was added to v1.1 scope on 2026-04-30 (Phases 13 + 14): visual refresh
  complete (Phase 13), live deploy + guest UAT pending (Phase 14)" — the
  "live deploy + guest UAT pending" half is now satisfied (with the
  deferral caveats above). Once Phase 14 verification closes, this caveat
  can be updated to "validated".

## Self-Check: PASSED

Verified at SUMMARY-writing time (2026-05-22):

- All 14 referenced files exist on disk (8 planning artifacts + 6 source files
  modified during the plan).
- All 18 referenced commits exist in git history (5 prod-bug fixes + 4
  docs-recording commits + 4 hosting-redeploy commits + 1 D-09 revert +
  1 baseline pre-plan SHA + 3 this-turn close-out commits).
- All 3 folded todos successfully moved to `.planning/todos/completed/`.
- All 3 new follow-up todos created in `.planning/todos/pending/`.

---
*Phase: 14-web-fallback-live-deploy-guest-uat*
*Plan: 04*
*Closed: 2026-05-22 (with deferrals — see "Deferred" section)*
