---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 04
type: execute
wave: 3
depends_on: [14-01, 14-02, 14-03]
files_modified:
  - web/.env.local
  - web/src/firebase.ts
  - functions/scripts/seedNearExpiryReservation.ts
autonomous: false
requirements: [WEB-01, WEB-02, WEB-03, WEB-04]
must_haves:
  truths:
    - "All 7 manual UAT items in .planning/phases/05-web-fallback/05-VALIDATION.md 'Manual-Only Verifications' table pass against the production deploy"
    - "Solo Pass 1 (user in incognito, both Chrome + Safari + Romanian-locale profile) completes all 7 items with documented pass evidence"
    - "Pass 2 with a recruited real giver friend on their own device passes items 2 (retailer redirect) and 3 (guest localStorage across browser restart) without 'works on my machine' bias"
    - "Email re-reserve UAT item 6 verified end-to-end via the production 30-min expiry timer (D-09 amendment 2026-05-21 — seed-script approach abandoned, see 14-CONTEXT.md). User reserves an item via the prod-pointed Android app, waits the natural 30 min for the deployed releaseReservation Cloud Task to fire, receives the expiry email, clicks the re-reserve CTA, and confirms a NEW reservation is created end-to-end. Same deployed pipeline (Cloud Task → release → expiry email → re-reserve link → createReservation), just exercised at production timing instead of a 60s shortcut."
    - "App Check has been flipped from monitor-only to ENFORCED for Firestore, Functions, AND Storage in Firebase Console — AFTER the monitor-mode smoke-test window shows clean appcheck:exchange traffic (D-04)"
    - "reCAPTCHA v3 site key has been registered in Firebase Console and added to web/.env.local as VITE_RECAPTCHA_SITE_KEY; web/src/firebase.ts now calls initializeAppCheck() with the ReCaptchaV3Provider; web bundle rebuilt + redeployed with App Check provider active"
    - "DevTools Network panel filtered to appcheck.googleapis.com shows appcheck:exchange returning 200 BEFORE the first Firestore/Functions call in real Chrome on prod (UAT item 1 closure evidence per D-05)"
  artifacts:
    - path: ".planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md"
      provides: "Pass/fail record for all 7 UAT items + layered-UAT Pass 1 + Pass 2 evidence"
      contains: "Manual UAT items 1-7"
    - path: "functions/scripts/seedNearExpiryReservation.ts"
      provides: "One-shot script that writes a reservation doc AND enqueues a Cloud Task with 60s delay targeting the deployed releaseReservation onTaskDispatched handler (D-09)"
      contains: "expiresAt"
    - path: "web/src/firebase.ts"
      provides: "App Check init via ReCaptchaV3Provider, gated on VITE_USE_EMULATORS !== 'true' AND VITE_RECAPTCHA_SITE_KEY present"
      contains: "initializeAppCheck"
  key_links:
    - from: "Web client App Check provider"
      to: "appcheck.googleapis.com appcheck:exchange endpoint"
      via: "reCAPTCHA v3 site key wired in web/.env.local + initializeAppCheck() call added to web/src/firebase.ts"
      pattern: "appcheck:exchange"
    - from: "functions/scripts/seedNearExpiryReservation.ts"
      to: "deployed releaseReservation onTaskDispatched handler"
      via: "Cloud Tasks queue scheduled 60s in the future (mirrors createReservation's enqueue pattern, just with a shorter delay)"
      pattern: "createTask"
---

<objective>
Close all 7 manual UAT items defined in `.planning/phases/05-web-fallback/05-VALIDATION.md` "Manual-Only Verifications" table by running a layered UAT (D-08: solo Pass 1 in incognito + recruited-giver Pass 2) against the now-live production deploy from Plans 14-01/14-02/14-03. Operationalize App Check by registering reCAPTCHA v3, smoke-testing in monitor mode, then flipping enforcement on for Firestore + Functions + Storage (D-04). Verify the email re-reserve flow using a 60-second-delay Cloud Task that fires the deployed releaseReservation handler (D-09).

Purpose: This plan is the official closure of WEB-01/02/03/04 against PRODUCTION (the requirements were code-complete in Phase 5 but the "pending real-browser UAT" caveat in PROJECT.md has been open since 2026-04-19). Closes Phase 14 and the v1.1 web fallback story.

Output: A `14-04-UAT-RESULTS.md` ledger with pass/fail evidence per UAT item, an enforced App Check posture, deferred-bug list (if any UAT items surface real bugs they get logged here and split out as quick-tasks), and a Phase-14 completion summary. Plan SUMMARY captures App Check rollback procedure (D-03) in case enforcement flip breaks prod.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md
@.planning/phases/05-web-fallback/05-VALIDATION.md
@.planning/phases/05-web-fallback/05-CONTEXT.md
@web/.env.local
@web/src/firebase.ts
@functions/src/reservation/createReservation.ts
@functions/src/reservation/releaseReservation.ts

<interfaces>
<!-- The 7 UAT items mapped to their requirement IDs (from 05-VALIDATION.md table rows 90-97). -->
<!-- Each task in this plan covers one or more items in this exact order. -->

| Item # | Behavior | Requirement | Task |
|--------|----------|-------------|------|
| 1 | reCAPTCHA v3 App Check token acquisition in production | WEB-02, WEB-03 | Task 4 (after Tasks 1-3 register site key + smoke monitor mode) |
| 2 | Retailer redirect opens new tab + keeps registry tab alive (Chrome + Safari) | WEB-04 | Task 5 (Pass 1) + Task 9 (Pass 2 recruited-giver) |
| 3 | Guest localStorage persists across browser restart | WEB-03 | Task 5 (Pass 1) + Task 9 (Pass 2 recruited-giver) |
| 4 | Romanian browser-locale autodetection on cold load (WEB-D-15) | I18N-03 | Task 5 (Pass 1 — Romanian-system-language profile) |
| 5 | SPA deep-link to PRIVATE registry rules-deny (unauthenticated → 404) | WEB-04 | Task 5 (Pass 1) |
| 6 | Email deep-link re-reserve end-to-end (60s-delay Cloud Task — D-09) | WEB-02, RES-08 | Task 6a (seed) + Task 6b (await email + click + verify) |
| 7 | Google OAuth popup flow on deployed build | WEB-03, AUTH-03 | Task 7 (after Task 2 adds OAuth origin) |

<!-- LOCKED — App Check wiring state (Branch decision resolved during revision):

The current `web/src/firebase.ts` (lines 1-34 read during revision) does NOT call
`initializeAppCheck()` and does NOT import from `firebase/app-check`. Phase 5 Plan 02
did NOT wire App Check init — the env var `VITE_RECAPTCHA_SITE_KEY` exists in .env.example
but no code consumes it.

Therefore Task 3 MUST add the App Check init code to web/src/firebase.ts. `files_modified`
correctly lists this file unconditionally.

The `firebase` npm package (v12.12.0 per web/package.json:25) already exposes the
`firebase/app-check` sub-entry — NO `npm install` is required. The import works out
of the box. -->

<!-- LOCKED — D-09 seed-script path (Branch decision resolved during revision):

releaseReservation.ts (read during revision) confirms the release trigger pattern:
  - `export const releaseReservation = onTaskDispatched<ReleasePayload>({region, ...}, async (req) => {...})`
  - There is NO periodic scan, NO Firestore on-update trigger; release is ONLY driven
    by a Cloud Task scheduled at createReservation time.
  - There IS a `releaseReservationCallable` (manual owner release) but D-09 explicitly
    wants to validate the Cloud Tasks → release → expiry-email pipeline end-to-end,
    NOT bypass it via the callable.

Therefore the seed script MUST:
  1. Write the reservation doc directly via Admin SDK (mirrors what createReservation does)
     with expiresAt = now() + 60s
  2. Enqueue a Cloud Task targeting the deployed releaseReservation onTaskDispatched
     URL, scheduled 60s in the future, with payload { reservationId }
  3. The deployed releaseReservation handler then fires, runs releaseReservationCore,
     sends the expiry email, and writes the giver notification — exact same pipeline
     that production uses, just compressed from 30min → 60s.

This is the LOCKED path. Do NOT use releaseReservationCallable (bypasses Cloud Tasks).
Do NOT rely on a periodic scan (none exists). Do NOT just override expiresAt on an
existing reservation (the originally-enqueued Cloud Task fires at the original 30min mark). -->
</interfaces>
</context>

<tasks>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 1: Register reCAPTCHA v3 site key in Firebase Console (manual one-shot per D-02 + D-04)</name>
  <files>(no repo file changes yet — user does Console work, executor wires the resulting key in Task 3)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-04 monitor-mode-first rationale, D-02 manual-op rationale)
    - .planning/phases/05-web-fallback/05-CONTEXT.md (WEB-D-18 — App Check uses reCAPTCHA v3 in production)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Nothing yet — Console-only manual setup. Per D-02 + D-04, App Check site key registration is a one-shot console operation and starts in monitor-only mode (NOT enforced) so a misconfigured key doesn't lock prod out.</what-built>
  <how-to-verify>
    Do these in order:

    1. Open https://console.firebase.google.com/project/gift-registry-ro/appcheck

    2. If the Web app from Plan 14-01 is not listed:
    - Click "Get started" or "Register" next to the Web app row
    - If prompted, choose "reCAPTCHA v3" as the provider

    3. Generate / register a reCAPTCHA v3 site key:
    - In the "reCAPTCHA v3" panel, click "Register" or "Save"
    - Firebase will either generate a key for you OR ask you to paste one created at https://www.google.com/recaptcha/admin (if it asks: create a v3 key, type=v3, domains=`gift-registry-ro.web.app` + `gift-registry-ro.firebaseapp.com`)
    - Copy the SITE KEY (NOT the secret key). It looks like `6Lc...` followed by ~40 chars.

    4. **CRITICAL — leave enforcement OFF for now (D-04):**
    - In Firebase Console > App Check > APIs tab, for EACH of: Cloud Firestore, Cloud Functions, Cloud Storage
    - Confirm the enforcement state shows **"Unenforced (monitor mode)"** or "Not enforced"
    - DO NOT click "Enforce" on any of the three — that happens in Task 10 after monitor-mode smoke passes

    5. (Optional) Set a token TTL: default 1 hour is fine; no change needed.

    Paste back into the chat:
    - The reCAPTCHA v3 site key (`6Lc...` value) — Task 3 needs this for web/.env.local
    - Confirmation that all three (Firestore, Functions, Storage) are in MONITOR mode (NOT enforced)
  </how-to-verify>
  <acceptance_criteria>
    - User pastes a reCAPTCHA v3 site key matching the pattern `6L[A-Za-z0-9_-]{38,}` (Google's v3 site key format)
    - User confirms App Check enforcement state for Cloud Firestore is "Unenforced / Monitor mode"
    - User confirms App Check enforcement state for Cloud Functions is "Unenforced / Monitor mode"
    - User confirms App Check enforcement state for Cloud Storage is "Unenforced / Monitor mode"
    - User has NOT enforced any service (verified by re-checking the APIs tab) — this is non-negotiable per D-04
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the site key + monitor-mode confirmation, then type "appcheck site key registered, monitor mode confirmed".</resume-signal>
</task>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 2: Add gift-registry-ro.web.app to Google OAuth Authorized origins (D-06)</name>
  <files>(no repo file changes — user does Google Cloud Console work)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-06 OAuth origin addition + incognito verification)
    - .planning/phases/05-web-fallback/05-CONTEXT.md (WEB-D-09 — Google OAuth via signInWithPopup)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Nothing — Google Cloud Console (NOT Firebase Console) manual config. Without this step, "Continue with Google" on prod (UAT item 7) will fail with `redirect_uri_mismatch` or `Error 400: invalid_request`.</what-built>
  <how-to-verify>
    1. Open https://console.cloud.google.com/apis/credentials?project=gift-registry-ro

    2. Find the OAuth 2.0 Client ID that backs Firebase Auth Google sign-in:
    - It will be named something like "Web client (auto created by Google Service)" or "Gift Registry Web"
    - If multiple exist, the one used by Firebase Auth typically has `https://gift-registry-ro.firebaseapp.com/__/auth/handler` already in the redirect URIs list

    3. Click the client ID to edit. Verify or ADD to:

    **Authorized JavaScript origins:**
    - `https://gift-registry-ro.web.app` (ADD if missing)
    - `https://gift-registry-ro.firebaseapp.com` (verify — usually present)

    **Authorized redirect URIs:**
    - `https://gift-registry-ro.web.app/__/auth/handler` (ADD if missing)
    - `https://gift-registry-ro.firebaseapp.com/__/auth/handler` (verify — usually present)

    4. Click SAVE. Changes can take a few minutes to propagate.

    5. (Optional dry-run) Open https://gift-registry-ro.web.app in incognito, navigate to a sign-in surface, click "Continue with Google" — popup opens, complete with a real Google account. If you get `Error 400: redirect_uri_mismatch`, the URL above is wrong; recheck.

    Paste back into the chat:
    - Confirmation that `https://gift-registry-ro.web.app` is now in BOTH Authorized JavaScript origins AND Authorized redirect URIs (the `/__/auth/handler` suffix variant)
    - Optional: dry-run popup-completed confirmation
  </how-to-verify>
  <acceptance_criteria>
    - User confirms `https://gift-registry-ro.web.app` is in Authorized JavaScript origins
    - User confirms `https://gift-registry-ro.web.app/__/auth/handler` is in Authorized redirect URIs
    - User confirms changes were saved (not just edited and abandoned)
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste confirmation of both URL additions saved, then type "oauth origins added".</resume-signal>
</task>

<task type="auto" tdd="false">
  <name>Task 3: Wire reCAPTCHA v3 site key into web/.env.local, add App Check init to firebase.ts, rebuild + redeploy hosting</name>
  <files>web/.env.local, web/src/firebase.ts</files>
  <read_first>
    - web/.env.local (current state from Plan 14-01 — VITE_RECAPTCHA_SITE_KEY should currently be empty)
    - web/src/firebase.ts (LOCKED state from revision: file does NOT yet call initializeAppCheck or import from firebase/app-check)
    - web/package.json line 25 (confirms `"firebase": "^12.12.0"` is installed — `firebase/app-check` sub-entry is available without further install)
    - .planning/phases/05-web-fallback/05-CONTEXT.md (WEB-D-18 App Check policy reference)
  </read_first>
  <action>
    **LOCKED PATH (no branching) — App Check init is NOT currently wired in web/src/firebase.ts.** During revision, the planner read web/src/firebase.ts and confirmed it imports only from `firebase/app`, `firebase/firestore`, `firebase/functions`, `firebase/auth` — there is no `firebase/app-check` import and no `initializeAppCheck()` call. This task adds the wiring. NO branching, NO grep gate — the executor's job is to add the imports + init call.

    **Step A — Update web/.env.local with the real site key:**
    Edit `/Users/victorpop/ai-projects/gift-registry/web/.env.local`:
    - Find the line `VITE_RECAPTCHA_SITE_KEY=` (currently empty after Plan 14-01)
    - Set it to `VITE_RECAPTCHA_SITE_KEY=<site-key-from-Task-1>` (the `6Lc...` value)
    - Leave `VITE_APP_CHECK_DEBUG_TOKEN=` empty (debug tokens are emulator-only; not used in prod)

    **Step B — Add App Check init to `/Users/victorpop/ai-projects/gift-registry/web/src/firebase.ts`:**

    Current file structure (read during revision):
    ```typescript
    import { initializeApp, type FirebaseApp } from 'firebase/app'
    import { getFirestore, connectFirestoreEmulator, type Firestore } from 'firebase/firestore'
    import { getFunctions, connectFunctionsEmulator, type Functions } from 'firebase/functions'
    import { getAuth, connectAuthEmulator, setPersistence, browserLocalPersistence, type Auth } from 'firebase/auth'

    const FUNCTIONS_REGION = 'europe-west3'  // line 8

    const firebaseConfig = { ... }  // lines 10-17

    export const app: FirebaseApp = initializeApp(firebaseConfig)  // line 19
    export const db: Firestore = getFirestore(app)  // line 20
    export const functions: Functions = getFunctions(app, FUNCTIONS_REGION)  // line 21
    export const auth: Auth = getAuth(app)  // line 22
    ```

    Make these two specific edits:

    **Edit 1 — add the import** (after the existing `firebase/auth` import line, before the `FUNCTIONS_REGION` constant):
    ```typescript
    import { initializeAppCheck, ReCaptchaV3Provider } from 'firebase/app-check'
    ```

    **Edit 2 — add the init call** AFTER `export const app: FirebaseApp = initializeApp(firebaseConfig)` and BEFORE `export const db: Firestore = getFirestore(app)`:
    ```typescript
    // App Check — reCAPTCHA v3 in production (WEB-D-18). Site key from .env.local.
    // Skipped in emulator mode (emulators don't enforce App Check) and when the key is
    // empty (defensive guard so a missing-env build doesn't crash at module-eval time).
    if (import.meta.env.VITE_USE_EMULATORS !== 'true' && import.meta.env.VITE_RECAPTCHA_SITE_KEY) {
      initializeAppCheck(app, {
        provider: new ReCaptchaV3Provider(import.meta.env.VITE_RECAPTCHA_SITE_KEY),
        isTokenAutoRefreshEnabled: true,
      })
    }
    ```

    No `npm install` required — `firebase/app-check` is a sub-entry of the already-installed `firebase` v12.12.0 package (verified during revision against web/package.json:25).

    **Step C — Rebuild + redeploy hosting:**
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/web && npm run build
    cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only hosting --project gift-registry-ro
    ```

    Verify the site key is now in the deployed bundle:
    ```bash
    grep -l "6Lc" /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js
    ```
    Expected: at least one match (the site key is inlined by Vite).

    Also verify the App Check provider is bundled:
    ```bash
    grep -l "ReCaptchaV3Provider\|app-check" /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js
    ```
    Expected: at least one match (proves Vite tree-shake didn't drop the App Check module).
  </action>
  <verify>
    <automated>grep -q "^VITE_RECAPTCHA_SITE_KEY=6L" /Users/victorpop/ai-projects/gift-registry/web/.env.local && grep -q "initializeAppCheck" /Users/victorpop/ai-projects/gift-registry/web/src/firebase.ts && grep -q "ReCaptchaV3Provider" /Users/victorpop/ai-projects/gift-registry/web/src/firebase.ts && grep -q "firebase/app-check" /Users/victorpop/ai-projects/gift-registry/web/src/firebase.ts && cd /Users/victorpop/ai-projects/gift-registry/web && npm run build && cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only hosting --project gift-registry-ro 2>&1 | tee /tmp/14-04-deploy.log && grep -q "Deploy complete" /tmp/14-04-deploy.log && grep -lq "6L" /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js</automated>
  </verify>
  <acceptance_criteria>
    - `web/.env.local` line `VITE_RECAPTCHA_SITE_KEY=` now has a value starting with `6L` (real reCAPTCHA v3 key)
    - `web/src/firebase.ts` contains the import `from 'firebase/app-check'` (LOCKED — must be added; not previously present)
    - `web/src/firebase.ts` contains the call `initializeAppCheck(app, {` (LOCKED — must be added)
    - `web/src/firebase.ts` contains the guard `import.meta.env.VITE_USE_EMULATORS !== 'true'` (emulator skip-gate)
    - `cd web && npm run build` exits 0 (TypeScript accepts the new imports)
    - `firebase deploy --only hosting --project gift-registry-ro` exits 0 with `Deploy complete!`
    - At least one file in `hosting/public/assets/*.js` contains the substring `6L` (site key inlined)
    - At least one file in `hosting/public/assets/*.js` contains `ReCaptchaV3Provider` OR `app-check` substring (App Check module bundled, not tree-shaken)
  </acceptance_criteria>
  <done>Site key wired, App Check init code added to firebase.ts (closing the Phase 5 Plan 02 wiring gap), bundle redeployed; App Check is active in monitor mode (Firebase Console will start logging appcheck:exchange traffic).</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 4: UAT item 1 — Verify App Check appcheck:exchange returns 200 in real Chrome (D-05)</name>
  <files>(no repo file changes — manual DevTools verification)</files>
  <read_first>
    - .planning/phases/05-web-fallback/05-VALIDATION.md (row 91 — exact test instructions for UAT item 1)
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-05 — DevTools Network verification approach)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>App Check is wired (Task 3) but in monitor-only mode (Task 1). This UAT item proves the reCAPTCHA exchange actually works — token request goes out, comes back 200 — BEFORE Firestore/Functions calls fire. This is the gate for flipping enforcement on in Task 10.</what-built>
  <how-to-verify>
    Exact steps per 05-VALIDATION.md row 91 + CONTEXT.md D-05:

    1. Open https://gift-registry-ro.web.app/ in a fresh **regular** Chrome tab (NOT incognito — App Check token exchange happens once per session; incognito complicates first-load measurement)

    2. Open DevTools (Cmd+Option+I) BEFORE the page loads (or hard-reload with Cmd+Shift+R after opening DevTools)

    3. Go to the Network panel

    4. Filter to: `appcheck.googleapis.com`

    5. Observe the request list. You should see at least ONE request to a URL matching:
    `https://content-firebaseappcheck.googleapis.com/v1/projects/gift-registry-ro/apps/.../appcheck:exchange`
    (Domain prefix may vary slightly — what matters is the `appcheck:exchange` path component.)

    6. Verify:
    - Status: **200** (NOT 401, NOT 403)
    - The request appears BEFORE the first request to `firestore.googleapis.com` or `cloudfunctions.net` (timing in Network panel — sort by time)

    7. (Optional advanced check) Click the appcheck:exchange row > Response tab > confirm response is a JSON object containing an `attestationToken` field.

    8. Also check Firebase Console > App Check > Monitor mode metrics:
    - Open https://console.firebase.google.com/project/gift-registry-ro/appcheck
    - The "Cloud Firestore" and "Cloud Functions" rows should show some traffic in the last 5 minutes with a high "verified" percentage (close to 100% if the wiring is right)

    Paste back:
    - Screenshot or copy-paste of the Network row showing `appcheck:exchange` status 200
    - Confirmation that the request fires before Firestore/Functions calls
    - (Optional) Console metrics screenshot showing monitor-mode traffic
  </how-to-verify>
  <acceptance_criteria>
    - User confirms a Network request to `appcheck.googleapis.com/.../appcheck:exchange` exists with HTTP status 200
    - User confirms this request fires BEFORE the first request to `firestore.googleapis.com` or `cloudfunctions.net`
    - UAT item 1 marked PASS in 14-04-UAT-RESULTS.md (executor creates this file after this task)
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the Network panel evidence, then type "uat-1 passed" or describe the failure.</resume-signal>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 5: UAT items 2, 3, 4, 5 — Solo Pass 1 in incognito (D-08 layer 1)</name>
  <files>(no repo file changes — manual cross-browser UAT)</files>
  <read_first>
    - .planning/phases/05-web-fallback/05-VALIDATION.md (rows 92, 93, 94, 95 — exact test instructions)
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-07 all 7 items must pass, D-08 layered UAT design)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Plans 14-01/02/03 are live; App Check is in monitor mode (Task 1) and the appcheck:exchange smoke passed (Task 4). Now you run the cross-browser visual UAT for the 4 items that DON'T require seeding/OAuth: retailer redirect (item 2), guest localStorage persistence (item 3), Romanian autodetect (item 4), private-registry rules-deny (item 5).</what-built>
  <how-to-verify>
    Have a known PUBLIC test registry ID and a known PRIVATE test registry ID ready (create via Android app if you don't have them).

    **UAT item 2 — Retailer redirect opens new tab + keeps registry tab alive (Chrome AND Safari):**
    Run for BOTH Chrome AND Safari (separate incognito windows):
    1. Open `https://gift-registry-ro.web.app/registry/<public-registry-id>` in a fresh incognito window
    2. Click "Reserve" on an available item
    3. Fill guest identity modal (any first name / last name / valid email)
    4. Confirm
    5. Verify: a NEW tab opens to the retailer's affiliateUrl
    6. Verify: the registry tab IS STILL OPEN and shows the reservation countdown banner (sticky banner per Phase 13 D-04)
    7. Verify: if you switch back to the registry tab, the item shows status "Reserved" with the pulsing accent dot

    Document: "Chrome PASS / Safari PASS" — or note the failure.

    **UAT item 3 — Guest localStorage persists across browser restart:**
    1. After item 2's reservation, close the entire browser (Cmd+Q in Chrome — NOT just the window)
    2. Re-open Chrome and navigate to `https://gift-registry-ro.web.app/registry/<same-public-registry-id>`
    3. Click "Reserve" on a different available item (the previous reservation should still be active because 30min timer hasn't elapsed)
    4. Verify: the guest identity modal pre-fills with first name / last name / email from the previous reservation (per WEB-D-06 localStorage persistence)

    Document: "PASS — modal pre-filled with prior identity" or note the failure.

    **UAT item 4 — Romanian browser locale autodetection on cold load:**
    1. Switch your macOS system language to Romanian (System Settings > General > Language & Region > add Romanian as primary)
    OR
    Use Chrome's Settings > Languages > Add Romanian and reorder it to top.
    2. Quit Chrome entirely (Cmd+Q)
    3. Re-open Chrome (so it picks up the new locale ordering)
    4. Open a fresh incognito window
    5. Navigate to `https://gift-registry-ro.web.app/registry/<public-registry-id>`
    6. Verify: the UI renders in Romanian (per WEB-D-15 i18next browser-detection). Examples to spot-check:
    - Top nav "Sign in" → "Conectează-te"
    - Reserve button → "Rezervă"
    - Status pill "Available" → "Disponibil"

    If the UI is still English: open DevTools > Console and check what `navigator.language` reports — if it's still `en-US`, the locale change didn't propagate, retry the OS-level switch.

    Document: "PASS — UI in Romanian" + paste at least one Romanian string you saw — or note failure (e.g., "navigator.language=ro but UI still in English" would be an i18next config bug worth a quick-task).

    Reset your system/Chrome language after this item.

    **UAT item 5 — SPA deep-link to PRIVATE registry, unauthenticated, returns 404 (gated by Plan 14-03 rules deploy):**
    1. Open a fresh incognito window (no auth session)
    2. Paste `https://gift-registry-ro.web.app/registry/<private-registry-id-you-own-but-arent-invited-as-anonymous>`
    3. Verify: the page shows the generic "Registry not available" 404 (per WEB-D-13/14 — does NOT leak that the registry exists, does NOT show owner-only data)
    4. Open DevTools > Console: should see a `permission-denied` error from Firestore (caught and mapped to 404 by the client)

    Document: "PASS — 404 page rendered, no data leak, permission-denied caught" — or note failure.
  </how-to-verify>
  <acceptance_criteria>
    - UAT item 2: User confirms retailer redirect opens new tab AND registry tab stays alive with countdown — in BOTH Chrome AND Safari. Two PASS records logged.
    - UAT item 3: User confirms guest identity modal pre-fills after full browser quit/relaunch. PASS logged.
    - UAT item 4: User confirms UI renders in Romanian after system/browser locale switch + cold reload. At least one Romanian string copy-pasted as evidence. PASS logged.
    - UAT item 5: User confirms unauthenticated incognito access to a private registry shows the generic 404 page (not the registry content; not a different error message that would leak existence). PASS logged.
    - All four results recorded in `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md` (executor creates this file if it doesn't exist) under "Pass 1 — Solo Incognito".
    - Any failures logged as deferred-bug entries with enough detail to spawn a `/gsd:debug` or `/gsd:quick` follow-up.
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste your results for items 2-5 (with the Chrome+Safari split for item 2), then type "uat-2-3-4-5 done".</resume-signal>
</task>

<task type="auto" tdd="false">
  <name>Task 6a: Write the 60s-Cloud-Task seed script (D-09 locked path)</name>
  <files>functions/scripts/seedNearExpiryReservation.ts</files>
  <read_first>
    - .planning/phases/05-web-fallback/05-VALIDATION.md (row 96 — UAT item 6 test instructions)
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-09 — exact approach: Cloud Tasks fires release + expiry email in ~1 minute)
    - functions/src/reservation/createReservation.ts (mirror its Cloud Tasks enqueue pattern — same queue name, same handler URL, just 60s delay instead of 30min)
    - functions/src/reservation/releaseReservation.ts (confirm the onTaskDispatched handler signature — payload shape is `{ reservationId: string }`, region `europe-west3`)
  </read_first>
  <action>
    **LOCKED PATH (D-09):** Cloud Tasks driven release. The seed script does TWO things atomically: (1) writes a reservation doc with `expiresAt = now + 60s`, (2) enqueues a Cloud Task scheduled 60s in the future, targeting the deployed `releaseReservation` onTaskDispatched URL, with payload `{ reservationId }`. This mirrors `createReservation`'s production flow exactly, just compressed in time. No fallbacks, no branching — the executor reads `createReservation.ts` to learn the Cloud Tasks queue name and handler URL pattern, then copies that pattern with a shorter delay.

    Create `/Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts`:

    ```typescript
    // Phase 14 Plan 04 — UAT item 6 helper (D-09 locked Cloud-Tasks path).
    //
    // Creates a reservation doc AND enqueues a Cloud Task scheduled 60s in the future
    // targeting the DEPLOYED releaseReservation onTaskDispatched handler. The handler
    // then runs releaseReservationCore (releases the item, sends expiry email,
    // writes notification) — exact same pipeline as production, just 60s instead of 30min.
    //
    // Usage:
    //   cd functions && npx ts-node scripts/seedNearExpiryReservation.ts \
    //     <registryId> <itemId> <giverEmail>
    //
    // Then wait ~60s, watch the inbox for the expiry email, click the re-reserve link.

    import * as admin from 'firebase-admin'
    import { CloudTasksClient } from '@google-cloud/tasks'

    const PROJECT_ID = 'gift-registry-ro'
    const REGION = 'europe-west3'
    // Mirror the queue name + handler URL that createReservation enqueues against.
    // Read functions/src/reservation/createReservation.ts to confirm these strings
    // before running — they MUST match the deployed function's expectations.
    const QUEUE_NAME = 'releaseReservation'   // 2nd-gen task queue name === function name
    const HANDLER_URL = `https://${REGION}-${PROJECT_ID}.cloudfunctions.net/releaseReservation`

    admin.initializeApp({ projectId: PROJECT_ID })
    const db = admin.firestore()
    const tasksClient = new CloudTasksClient()

    async function main() {
      const [registryId, itemId, giverEmail] = process.argv.slice(2)
      if (!registryId || !itemId || !giverEmail) {
        console.error(
          'Usage: ts-node scripts/seedNearExpiryReservation.ts <registryId> <itemId> <giverEmail>'
        )
        process.exit(1)
      }

      const now = admin.firestore.Timestamp.now()
      const expiresAt = admin.firestore.Timestamp.fromMillis(now.toMillis() + 60_000)

      // Step 1 — write the reservation doc (mirrors createReservation's Firestore write)
      const reservationRef = await db.collection('reservations').add({
        registryId,
        itemId,
        giverEmail,
        giverFirstName: 'UAT',
        giverLastName: 'Test',
        giverId: null,           // guest reservation
        status: 'active',
        createdAt: now,
        expiresAt,
      })

      // Step 2 — enqueue Cloud Task with 60s delay targeting releaseReservation handler
      const queuePath = tasksClient.queuePath(PROJECT_ID, REGION, QUEUE_NAME)
      const taskPayload = { reservationId: reservationRef.id }
      const [task] = await tasksClient.createTask({
        parent: queuePath,
        task: {
          httpRequest: {
            httpMethod: 'POST',
            url: HANDLER_URL,
            headers: { 'Content-Type': 'application/json' },
            body: Buffer.from(JSON.stringify({ data: taskPayload })).toString('base64'),
          },
          scheduleTime: { seconds: Math.floor(expiresAt.toMillis() / 1000) },
        },
      })

      // Persist the cloudTaskName on the reservation so releaseReservationCallable can
      // cancel it on manual release (same as production createReservation does).
      await reservationRef.update({ cloudTaskName: task.name })

      console.log('Created reservation:', reservationRef.id)
      console.log('Expires at:', expiresAt.toDate().toISOString())
      console.log('Cloud Task scheduled:', task.name)
      console.log('Expect the expiry email at:', giverEmail, 'in ~60s')
    }

    main().catch((e) => {
      console.error(e)
      process.exit(1)
    })
    ```

    **IMPORTANT — confirm queue name + handler URL before running:**
    The constants `QUEUE_NAME` and `HANDLER_URL` above MUST match what the deployed `createReservation` uses. Read `functions/src/reservation/createReservation.ts` and grep for `tasksClient.createTask`, `queuePath`, or `releaseReservation` URL references. If the production code uses a different queue name (e.g., `release-reservation` with a hyphen) or different URL format (e.g., Cloud Run URLs `*.a.run.app` instead of the cloudfunctions.net pattern), update the constants in this script to match.

    No Firebase deploy needed for this script — it runs locally via `ts-node` against the prod project using Admin SDK credentials (the executor / user will need to be authenticated via `gcloud auth application-default login` or `GOOGLE_APPLICATION_CREDENTIALS` for Cloud Tasks API calls to succeed).

    This task is `auto` because writing the file is mechanical. Running the script + verifying the email-click happens in Task 6b (checkpoint).
  </action>
  <verify>
    <automated>test -f /Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts && grep -q "expiresAt" /Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts && grep -q "gift-registry-ro" /Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts && grep -q "europe-west3" /Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts && grep -q "tasksClient.createTask\|createTask" /Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts && grep -q "releaseReservation" /Users/victorpop/ai-projects/gift-registry/functions/scripts/seedNearExpiryReservation.ts</automated>
  </verify>
  <acceptance_criteria>
    - File `functions/scripts/seedNearExpiryReservation.ts` exists
    - Script references project `gift-registry-ro` and region `europe-west3`
    - Script writes a reservation doc with `expiresAt = now + 60_000ms`
    - Script calls `CloudTasksClient.createTask` (LOCKED D-09 path — NOT calling `releaseReservationCallable`, NOT relying on a periodic scan)
    - Script targets the deployed `releaseReservation` handler (queue name + URL match production constants — executor cross-references against createReservation.ts before running)
    - Script writes `cloudTaskName` back to the reservation doc (mirrors production pattern so the manual-release path could cancel the task if needed)
  </acceptance_criteria>
  <done>Seed script is on disk and ready to run. Task 6b runs it and verifies the email-click loop.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 6b: UAT item 6 — Run the seed, wait for the email, click re-reserve, verify new reservation (D-09 closure)</name>
  <files>(no repo file changes — invokes the script from Task 6a + manual email verification)</files>
  <read_first>
    - .planning/phases/05-web-fallback/05-VALIDATION.md (row 96 — UAT item 6 test instructions)
    - functions/scripts/seedNearExpiryReservation.ts (created by Task 6a)
    - functions/src/email/templates/expiry.ts (find the expiry email subject + re-reserve link format so user knows what to look for in inbox)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.

    **This is a checkpoint (not auto) because a human MUST click the email link.** Per BLOCKER #1 from the checker: a passing file-exists check on the seed script does NOT prove the email-click loop works. The user must paste back evidence that the email arrived AND that clicking the re-reserve link created a new reservation.
  </action>
  <what-built>Task 6a wrote the seed script. This task runs it against prod, waits for the Cloud Task to fire (~60s), waits for the expiry email to arrive in the user's inbox (~60-120s additional for SendGrid delivery), clicks the re-reserve link, and verifies a NEW reservation is created end-to-end. Closes UAT item 6 with paste-back evidence — NOT just "the script ran".</what-built>
  <how-to-verify>
    Have ready:
    - A registry ID + an available item ID in that registry (PUBLIC visibility recommended so re-reserve doesn't hit rules-deny)
    - A real email address you control and can check immediately
    - Authenticated terminal session (`gcloud auth application-default login` if not already authed; needed for Cloud Tasks API calls)

    **Step 1 — Run the seed script:**
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/functions && npx ts-node scripts/seedNearExpiryReservation.ts <registryId> <itemId> <your-email>
    ```

    Expected stdout:
    - `Created reservation: <reservationId>` ← capture this ORIGINAL reservation ID
    - `Expires at: 2026-05-20T...` (60s in the future)
    - `Cloud Task scheduled: projects/gift-registry-ro/locations/europe-west3/queues/.../tasks/...`
    - `Expect the expiry email at: <your-email> in ~60s`

    If the script errors (missing auth, wrong queue name, etc.), STOP — fix the issue, re-run.

    **Step 2 — Wait ~60-120s for the Cloud Task to fire + email to deliver:**
    Look at Firebase Console > Functions > Logs (filter: `releaseReservation`) — within ~60s of the script completing, you should see a log entry from the `releaseReservation` handler firing with the original reservation ID. This proves the Cloud Task fired correctly.

    Then check your email inbox (and spam folder — first-time expiry emails sometimes land in spam):
    - Subject is something like "Your reservation has expired" (exact wording from `functions/src/email/templates/expiry.ts`)
    - Body contains a re-reserve link like `https://gift-registry-ro.web.app/reservation/<originalReservationId>/re-reserve`

    **Step 3 — Verify Firestore release transaction ran:**
    Open Firebase Console > Firestore > `reservations/<originalReservationId>`:
    - `status` field should now be `expired` (was `active` before the Cloud Task fired)
    Also check `registries/<registryId>/items/<itemId>`:
    - `status` field should be `available` (was `reserved` during the 60s window)

    **Step 4 — Click the re-reserve link from the email:**
    Click the link in the email body. It opens the web fallback at `https://gift-registry-ro.web.app/reservation/<originalReservationId>/re-reserve`, which per WEB-D-11 calls `resolveReservation` to get registryId+itemId then redirects to `/registry/<id>?autoReserveItemId=<itemId>` and auto-fires `createReservation`.

    **Step 5 — Verify a NEW reservation was created:**
    On the registry detail page that opens, the item should show as `Reserved` with a fresh 30-min countdown. In Firebase Console > Firestore > reservations collection, there should be a NEW document (different ID from the original) with `status: active` and a fresh `expiresAt` ~30min in the future.

    **Step 6 — Clean up:**
    Either let the new reservation expire naturally (30min) or manually release it via the Android app, so prod doesn't accumulate UAT cruft.

    **Paste back into the chat:**
    - The ORIGINAL reservation ID (from Step 1 stdout)
    - The exact email subject line (from Step 2 inbox)
    - Firestore screenshot or text confirmation: original reservation status === `expired` AND item status === `available` after Step 2 (proves release ran)
    - The NEW reservation ID (from Step 5 Firestore)
    - "PASS" if all of the above completed, OR a failure description if any step broke
  </how-to-verify>
  <acceptance_criteria>
    - User pastes the ORIGINAL reservation ID printed by `seedNearExpiryReservation.ts`
    - User pastes the expiry email subject line received in their inbox (proves Cloud Task fired → handler ran → sendEmail succeeded → SendGrid delivered)
    - User confirms in Firestore that the original reservation's `status` flipped from `active` to `expired` AND the item's `status` flipped from `reserved` to `available` (proves releaseReservationCore's transaction ran end-to-end)
    - User pastes the NEW reservation ID created when they clicked the re-reserve link (proves the email link → resolveReservation → createReservation auto-loop works)
    - All evidence logged in `14-04-UAT-RESULTS.md` under UAT item 6 row with the THREE IDs (original, new) + email subject
    - Test reservations cleaned up after verification (so prod doesn't accumulate UAT cruft)
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome. The user MUST paste back: (1) original reservation ID, (2) email subject line received, (3) new reservation ID created by clicking the re-reserve link. See acceptance_criteria."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met (original ID, email subject, Firestore state-flip confirmation, new ID). UAT item 6 closed with end-to-end Cloud Tasks → release → email → re-reserve evidence.</done>
  <resume-signal>Paste: (1) original reservation ID, (2) expiry email subject line, (3) Firestore state-flip confirmation, (4) new reservation ID created from email click. Then type "uat-6 passed" or describe the failure point.</resume-signal>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 7: UAT item 7 — Google OAuth popup flow on deployed build (depends on Task 2 OAuth origin add)</name>
  <files>(no repo file changes — manual OAuth UAT)</files>
  <read_first>
    - .planning/phases/05-web-fallback/05-VALIDATION.md (row 97 — UAT item 7 test instructions)
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-06 OAuth origin add)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Task 2 added `gift-registry-ro.web.app` to the OAuth client's Authorized JavaScript origins + redirect URIs. This UAT closes the loop by actually completing a Google sign-in popup from the deployed build (catches typos in the URI registration AND any propagation delay).</what-built>
  <how-to-verify>
    1. Open https://gift-registry-ro.web.app/ in a FRESH incognito window (or fresh-profile Chrome — important: no existing Firebase Auth session)

    2. Navigate to a sign-in surface — could be:
    - The top-nav "Sign in" button (per Phase 13 D-14)
    - The dedicated `/sign-in` route (per Phase 13 D-05)
    - The auth modal triggered by a guarded action

    3. Click "Continue with Google"

    4. A Google OAuth popup window should open (per WEB-D-09 — signInWithPopup)

    5. Complete the popup with a real Google account (use a personal one you own, NOT pop.v.victor@gmail.com if that's already an owner — pick a clean test account so this exercises the new-user path)

    6. Popup closes; the main tab should:
    - Either: navigate to the post-sign-in landing (registry list, home — depends on Phase 13 routing)
    - Or: stay on the same page but show the user as authenticated (avatar in top nav per D-14)

    7. Open DevTools > Application > Local Storage > `https://gift-registry-ro.web.app`:
    - Look for a `firebase:authUser:<...>` key with a non-null value — proves the auth session persisted (WEB-D-12 browserLocalPersistence)

    **Failure modes to watch for:**
    - `Error 400: redirect_uri_mismatch` → Task 2's origin add didn't save or has a typo. Re-check Cloud Console.
    - Popup blocked by browser → not a real bug; user-level browser setting. Allow popups for gift-registry-ro.web.app and retry.
    - Popup opens but main tab never updates → check console for Firebase Auth errors; could be App Check (in monitor mode, this should not block, but if it shows 401 from `identitytoolkit.googleapis.com`, monitor mode might be misconfigured).

    Document:
    - PASS — Google account email used, screenshot of avatar/signed-in state, evidence of `firebase:authUser:*` localStorage key
    - OR failure with exact error code and stack
  </how-to-verify>
  <acceptance_criteria>
    - User confirms OAuth popup opens and completes successfully on the deployed build
    - User confirms post-popup main tab shows signed-in state (avatar or similar UI cue per Phase 13 D-14)
    - User confirms `firebase:authUser:*` key exists in localStorage with non-null value (browserLocalPersistence active)
    - PASS logged for UAT item 7 in 14-04-UAT-RESULTS.md
    - No `redirect_uri_mismatch` error (proves Task 2 origin add was correct)
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste OAuth UAT result + screenshot of signed-in state OR error message, then type "uat-7 done".</resume-signal>
</task>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 8: UAT Pass 2 — Recruit a real giver friend for items 2 + 3 (D-08 layered defense)</name>
  <files>(no repo file changes — external user testing)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-08 — layered UAT design; Pass 2 catches "works on my machine" bias)
    - .planning/phases/05-web-fallback/05-VALIDATION.md (rows 92, 93 — items 2 and 3 instructions, sanitized for the recruited tester)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Pass 1 (Task 5) was you in your own environment with your own assumptions. Pass 2 hands the test to a real giver friend on their own device — typically a friend's iPhone or Android browser + their own Gmail. Catches issues that don't appear in your environment: their localStorage quota, their iOS Safari version's `window.open` semantics, their Gmail spam filter on the email link, etc.</what-built>
  <how-to-verify>
    1. **Pick a recruited tester** — someone real, not you. Ideally:
    - Has a normal modern phone (iPhone or Android — actual giver demographic)
    - Has an email account they'll check
    - Is willing to spend 5-10 min on this

    2. **Hand them a clean test registry**:
    - Create or use an existing PUBLIC registry with at least 2 available items
    - Share the URL `https://gift-registry-ro.web.app/registry/<id>` via your normal sharing channel (text message / WhatsApp / etc — like a real giver would receive it)

    3. **Ask them to do these specific things** (item 2 + item 3 instructions, friendlied up):

    **For UAT item 2 (retailer redirect):**
    > "Click Reserve on one of the items. Fill in your name and email. After you confirm, tell me: (a) Did the retailer (e.g., emag.ro) open in a new tab? (b) Is the original registry page still open in another tab showing a countdown timer at the top?"

    **For UAT item 3 (guest localStorage):**
    > "Now close your entire browser. Re-open it and paste this same registry URL again. Click Reserve on a DIFFERENT item. Tell me: did the form pre-fill your name and email automatically?"

    4. **Collect their answers** verbatim. Document in 14-04-UAT-RESULTS.md under "Pass 2 — Recruited Giver":
    - Friend's device (e.g., "iPhone 14, Safari 17.5")
    - Item 2 answer: yes/no for new tab + yes/no for original tab alive + yes/no for countdown visible
    - Item 3 answer: yes/no for pre-fill

    5. If they hit a bug your Pass 1 missed (e.g., "the retailer opened in the SAME tab and the registry tab is gone"), log it as a deferred bug with their device info — that's a real iOS Safari `window.open` gotcha that needs a quick-task fix.

    6. Thank your friend. Optionally release their test reservation via the Android app or let it expire (30 min).
  </how-to-verify>
  <acceptance_criteria>
    - User identifies a real recruited tester (not themselves) with device info captured
    - Tester completes item 2 instructions; answers logged verbatim
    - Tester completes item 3 instructions; answers logged verbatim
    - Both items get a clear PASS or FAIL in 14-04-UAT-RESULTS.md under "Pass 2 — Recruited Giver"
    - Any divergence from Pass 1 results (something worked for you but not for them — or vice versa) explicitly flagged as a "Pass 1/Pass 2 divergence" with device info — these are the bugs Pass 2 exists to find
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste your recruited-tester's answers + device info, then type "uat pass-2 done".</resume-signal>
</task>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 9: Flip App Check from monitor to ENFORCED for Firestore + Functions + Storage (D-04)</name>
  <files>(no repo file changes — Firebase Console enforcement toggles)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-04 — flip only after monitor smoke passes; this is the final closure step)
    - 14-04-UAT-RESULTS.md (executor should ensure Task 4 showed clean appcheck:exchange traffic + Console monitor metrics showed high verified % before flipping)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Tasks 1-8 verified the web client correctly acquires App Check tokens AND the prod backend (Firestore + Functions + Storage) all accept them in monitor mode. Now flip the lock — enforcing means unauthenticated/non-App-Check-token requests will start being REJECTED at the Firebase API layer. This is the final hardening step.</what-built>
  <how-to-verify>
    **PRE-FLIP SAFETY CHECK** — confirm one more time that monitor mode shows healthy traffic:

    1. Open https://console.firebase.google.com/project/gift-registry-ro/appcheck
    2. For each of: Cloud Firestore, Cloud Functions, Cloud Storage:
    - Look at the "Verified requests" % over the last 24 hours
    - If verified % is HIGH (e.g., >95%), you're safe to flip
    - If verified % is LOW (e.g., <90%), something is producing untokenized traffic — investigate BEFORE flipping (likely the Android app needs an App Check update, or there's a server-side admin call that needs allowlisting). DO NOT flip if verified % < 90% — that means flipping will break that traffic.
    3. If safe to proceed, document the verified % values before the flip.

    **FLIP for each service** (do these ONE AT A TIME, in this order — least-risky first):

    **1. Cloud Storage** (lowest blast radius — only cover-photo uploads use it):
    - Console > App Check > APIs tab > Cloud Storage > Enforce
    - Confirm the dialog. State changes to "Enforced".

    **2. Cloud Functions** (medium — every callable invocation):
    - Console > App Check > APIs tab > Cloud Functions > Enforce
    - Confirm. Wait ~30s for propagation.
    - Smoke test: in incognito, open `https://gift-registry-ro.web.app/registry/<id>` and click Reserve. Should still work (your web client is sending valid App Check tokens — that's what Task 4 verified). If reserve fails with a 401 from cloudfunctions.net, ROLLBACK by clicking "Unenforce" in the console.

    **3. Cloud Firestore** (highest blast radius — every read/write):
    - Console > App Check > APIs tab > Cloud Firestore > Enforce
    - Confirm. Wait ~30s.
    - Smoke test: refresh the registry detail page. Should render normally. If you see permission-denied errors that you didn't see in Pass 1, ROLLBACK by clicking "Unenforce".

    **POST-FLIP VERIFICATION:**
    4. Verify all three rows now show "Enforced" status
    5. Re-test the most critical user path: incognito → reserve item → retailer redirect (same as UAT item 2). Should still work end-to-end.
    6. Paste back: confirmation of all three "Enforced" states + post-flip reserve-flow smoke test PASS

    If anything broke at any step, ROLLBACK that one service to "Unenforce" and document in the SUMMARY what happened. The other services can stay enforced.
  </how-to-verify>
  <acceptance_criteria>
    - User documents pre-flip verified-request % for all three services (must be >90% before flipping)
    - User flips Storage to Enforced; no immediate breakage observed
    - User flips Functions to Enforced; reserve flow still works in incognito
    - User flips Firestore to Enforced; registry detail page still renders in incognito
    - All three services confirmed "Enforced" in Console
    - Post-flip end-to-end reserve smoke test PASSES (same flow as UAT item 2 — proves enforcement didn't break anything)
    - SUMMARY documents the rollback command for each service (Console > App Check > APIs > <Service> > Unenforce) in case of post-flip issue
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste enforcement confirmations + post-flip smoke result, then type "appcheck enforced".</resume-signal>
</task>

<task type="auto" tdd="false">
  <name>Task 10: Write 14-04-UAT-RESULTS.md ledger + close folded todos</name>
  <files>.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md</files>
  <read_first>
    - All evidence collected from Tasks 4-9 (already in chat history)
    - .planning/phases/05-web-fallback/05-VALIDATION.md (the 7-item table being closed)
  </read_first>
  <action>
    Write `/Users/victorpop/ai-projects/gift-registry/.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md` with the structure:

    ```markdown
    # Phase 14 Plan 04 — UAT Results

    **Date:** {today}
    **Tested against:** https://gift-registry-ro.web.app (post-Plans 14-01/02/03 deploy)
    **App Check state at completion:** Enforced (Firestore + Functions + Storage)

    ## Pass 1 — Solo Incognito

    | # | Item | Requirement | Result | Evidence |
    |---|------|-------------|--------|----------|
    | 1 | reCAPTCHA v3 appcheck:exchange returns 200 before first Firestore call | WEB-02, WEB-03 | PASS / FAIL | <Network panel evidence from Task 4> |
    | 2 | Retailer redirect new tab + registry tab alive (Chrome) | WEB-04 | PASS / FAIL | <Task 5> |
    | 2 | Retailer redirect new tab + registry tab alive (Safari) | WEB-04 | PASS / FAIL | <Task 5> |
    | 3 | Guest localStorage pre-fills after browser restart | WEB-03 | PASS / FAIL | <Task 5> |
    | 4 | Romanian browser-locale autodetection on cold load | I18N-03 | PASS / FAIL | <Task 5 — paste Romanian string seen> |
    | 5 | Private registry deep-link returns 404 unauthenticated | WEB-04 | PASS / FAIL | <Task 5> |
    | 6 | Email re-reserve end-to-end via 60s-delay Cloud Task | WEB-02, RES-08 | PASS / FAIL | <Task 6b — original reservation ID + email subject + new reservation ID> |
    | 7 | Google OAuth popup flow on deployed build | WEB-03, AUTH-03 | PASS / FAIL | <Task 7> |

    ## Pass 2 — Recruited Giver

    **Tester device:** <iPhone 14, Safari 17.5 / etc — from Task 8>

    | # | Item | Result | Evidence |
    |---|------|--------|----------|
    | 2 | Retailer redirect new tab + registry tab alive | PASS / FAIL | <Task 8 — verbatim tester answer> |
    | 3 | Guest localStorage pre-fills after browser restart | PASS / FAIL | <Task 8 — verbatim tester answer> |

    ## Pass 1 / Pass 2 Divergences

    <List any item where Pass 1 and Pass 2 disagree — these are the bugs Pass 2 was designed to surface. If none, write "None.">

    ## App Check Enforcement

    | Service | Pre-flip verified % | Post-flip state | Post-flip smoke |
    |---------|---------------------|-----------------|-----------------|
    | Cloud Storage | <%> | Enforced | <reserve flow OK / rolled back> |
    | Cloud Functions | <%> | Enforced | <reserve flow OK / rolled back> |
    | Cloud Firestore | <%> | Enforced | <reserve flow OK / rolled back> |

    ## Deferred Bugs

    <For each UAT FAIL, write a one-paragraph bug brief with: symptom, browser/device, reproduction steps, suspected file. These get spun out as `/gsd:debug` or `/gsd:quick` follow-up tasks AFTER Phase 14 closes — Phase 14 closure is not blocked by these (per CONTEXT.md "If a UAT item exposes a real bug, it gets split out as a quick-task and Plan 14-04 logs a deferred item").>

    ## Closure

    - WEB-01: viewable on web — CLOSED (items 1, 5 pass)
    - WEB-02: reserve from web — CLOSED (items 1, 2, 6 pass)
    - WEB-03: auth/guest on web — CLOSED (items 3, 7 pass)
    - WEB-04: retailer redirect parity — CLOSED (items 2, 5 pass)
    - Phase 14 closure criteria 1-5 (per 14-CONTEXT.md domain block) — <list each, mark MET or BLOCKED-BY-deferred-bug>

    ## Folded Todos Closed

    - [x] `.planning/todos/pending/2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md` — closed by Plan 14-01; move to `completed/`
    - [x] `.planning/todos/pending/2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` — closed by Plan 14-02; move to `completed/`
    - [x] `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` — closed by Plan 14-03; move to `completed/`
    ```

    After writing the file, MOVE the three folded todos from `pending/` to `completed/`:
    ```bash
    mkdir -p /Users/victorpop/ai-projects/gift-registry/.planning/todos/completed
    mv /Users/victorpop/ai-projects/gift-registry/.planning/todos/pending/2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md /Users/victorpop/ai-projects/gift-registry/.planning/todos/completed/ 2>/dev/null || true
    mv /Users/victorpop/ai-projects/gift-registry/.planning/todos/pending/2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md /Users/victorpop/ai-projects/gift-registry/.planning/todos/completed/ 2>/dev/null || true
    mv /Users/victorpop/ai-projects/gift-registry/.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md /Users/victorpop/ai-projects/gift-registry/.planning/todos/completed/ 2>/dev/null || true
    ```
  </action>
  <verify>
    <automated>test -f /Users/victorpop/ai-projects/gift-registry/.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md && grep -q "Pass 1 — Solo Incognito" /Users/victorpop/ai-projects/gift-registry/.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md && grep -q "Pass 2 — Recruited Giver" /Users/victorpop/ai-projects/gift-registry/.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md && grep -q "App Check Enforcement" /Users/victorpop/ai-projects/gift-registry/.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md && grep -qE "WEB-01.*CLOSED" /Users/victorpop/ai-projects/gift-registry/.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md</automated>
  </verify>
  <acceptance_criteria>
    - `14-04-UAT-RESULTS.md` exists with all 4 sections: Pass 1, Pass 2, App Check Enforcement, Closure
    - All 7 UAT items have a PASS/FAIL with evidence
    - All 4 WEB requirements (WEB-01..04) are marked CLOSED (or BLOCKED-BY-deferred-bug with specific bug references)
    - Folded todos moved from pending/ to completed/
    - Any deferred bugs are logged with enough detail to spawn follow-up `/gsd:debug` or `/gsd:quick` tasks
  </acceptance_criteria>
  <done>Phase 14 closure ledger is on disk; folded todos archived; deferred bugs (if any) catalogued for follow-up; ready for `/gsd:verify-work` to close the phase.</done>
</task>

</tasks>

<verification>
Combined Plan 14-04 verification (all must pass for Phase 14 closure):

1. reCAPTCHA v3 site key registered in Firebase Console (Task 1)
2. OAuth Authorized origins + redirect URIs updated in Cloud Console (Task 2)
3. App Check site key wired in `web/.env.local`, App Check init code added to `web/src/firebase.ts`, hosting redeployed (Task 3 — LOCKED Branch 2: code was NOT previously wired)
4. UAT item 1: `appcheck:exchange` returns 200 in real Chrome (Task 4)
5. UAT items 2, 3, 4, 5 PASS in Pass 1 (Task 5) — including Chrome AND Safari for item 2, Romanian locale for item 4, private-registry 404 for item 5
6. UAT item 6 — seed script written (Task 6a) AND email-click loop verified end-to-end with original reservation ID + email subject + new reservation ID paste-back (Task 6b checkpoint)
7. UAT item 7 PASS — Google OAuth popup completes on deployed build (Task 7)
8. UAT Pass 2 items 2 + 3 PASS with recruited tester on their own device (Task 8)
9. App Check ENFORCED on Firestore + Functions + Storage; post-flip smoke confirms no regression (Task 9)
10. `14-04-UAT-RESULTS.md` ledger written; all 4 WEB requirements marked CLOSED; folded todos archived (Task 10)
</verification>

<success_criteria>
Plan 14-04 — and therefore Phase 14 — is complete when:
- [ ] All 7 manual UAT items in 05-VALIDATION.md "Manual-Only Verifications" table marked PASS (D-07)
- [ ] Pass 2 layered UAT with recruited giver also PASS for items 2 + 3 (D-08)
- [ ] App Check enforced on Firestore + Functions + Storage (D-04 — final hardening)
- [ ] Email re-reserve UAT executed via 60s-delay Cloud Task targeting the deployed releaseReservation handler (D-09 locked path)
- [ ] UAT item 6 closure backed by paste-back evidence (NOT just "the script exists"): original reservation ID + expiry email subject + Firestore state-flip + new reservation ID
- [ ] reCAPTCHA v3 site key + OAuth origins added (D-06)
- [ ] App Check init code added to web/src/firebase.ts (LOCKED — was not previously wired in Phase 5 Plan 02)
- [ ] DevTools Network appcheck:exchange evidence captured (D-05)
- [ ] 14-04-UAT-RESULTS.md ledger documents all evidence
- [ ] All 3 folded todos moved from pending/ to completed/
- [ ] Phase 14 closure criteria 1-5 (CONTEXT.md domain block) all MET
- [ ] WEB-01, WEB-02, WEB-03, WEB-04 officially closed against PRODUCTION (no more "pending real-browser UAT" caveat in PROJECT.md)
- [ ] Any UAT failures catalogued as deferred bugs with enough detail to spawn follow-up quick-tasks
</success_criteria>

<output>
After completion, create `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-SUMMARY.md` including:

1. UAT results summary (link to 14-04-UAT-RESULTS.md ledger)
2. App Check posture: enforced for all 3 services; pre-flip verified % per service
3. **Rollback runbook (D-03 — REQUIRED — App Check is the most consequential rollback here):**
   - App Check enforcement rollback: Console > App Check > APIs > <Service> > Unenforce (per-service, immediate effect)
   - reCAPTCHA site key removal: Console > App Check > <Web app> > Unregister (last resort)
   - Code-level rollback for hosting redeploy in Task 3 (App Check init addition to firebase.ts): `git checkout <previous SHA> && cd web && npm run build && firebase deploy --only hosting --project gift-registry-ro`
4. Deferred bugs list (from UAT-RESULTS.md) — each one tagged for follow-up via `/gsd:debug` or `/gsd:quick`
5. Folded todos closed (3 of them — see Task 10)
6. Phase 14 completion declaration: WEB-01..04 closed against production; v1.1 web fallback story complete; PROJECT.md "Giver-facing web fallback added to v1.1 scope on 2026-04-30 (Phases 13 + 14)" caveat resolved
7. **Note on task-count vs checkpoint ratio:** Plan 14-04 has 11 task entries (Tasks 1-10 with 6a/6b split), 8 of which are checkpoints. This is intentional — UAT plans require many human-gated steps per the layered defense in D-08. The checker's task-count heuristic should be read as "this is a UAT closure plan, not a build plan".
</output>
</output>
