---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/.env.local
  - hosting/public/
autonomous: false
requirements: [WEB-01]
user_setup:
  - service: firebase
    why: "Register a Web app in the gift-registry-ro Firebase project and obtain its SDK config (one-shot manual op — D-02 forbids Claude from running apps:create directly)"
    dashboard_config:
      - task: "Run firebase apps:create WEB and paste sdkconfig JSON back (registration is non-idempotent — re-running creates duplicate web apps in the project)"
        location: "Local terminal (auth'd via firebase login as victorpop)"

must_haves:
  truths:
    - "A single Firebase Web app exists in the gift-registry-ro project (verifiable via firebase apps:list)"
    - "web/.env.local contains all six VITE_FIREBASE_* values matching the registered Web app"
    - "https://gift-registry-ro.web.app/ returns HTML containing <div id=\"root\"> AND a non-empty React-rendered subtree (no blank page)"
    - "A deep-link path (e.g. /registry/nonexistent) returns the SPA shell (200) rather than 404 — confirms SPA rewrite still intact post-deploy"
    - "hosting/public/index.html is served with Cache-Control: no-cache (or equivalent revalidation hint); hashed assets in /assets/*.js are served with long-cache headers"
  artifacts:
    - path: "web/.env.local"
      provides: "Real Firebase Web SDK config baked into the Vite build"
      contains: "VITE_FIREBASE_API_KEY="
    - path: "hosting/public/index.html"
      provides: "SPA entry served at /"
      contains: "id=\"root\""
    - path: "hosting/public/assets/"
      provides: "Hashed JS/CSS bundles with real firebase config strings embedded"
  key_links:
    - from: "web/src/firebase.ts"
      to: "import.meta.env.VITE_FIREBASE_*"
      via: "Vite env var inlining at build time"
      pattern: "VITE_FIREBASE_(API_KEY|PROJECT_ID|APP_ID)"
    - from: "https://gift-registry-ro.web.app/registry/anything"
      to: "/index.html"
      via: "firebase.json hosting.rewrites[**]"
      pattern: "/index.html"
---

<objective>
Register the Firebase Web app in `gift-registry-ro`, populate `web/.env.local` with the real SDK config, rebuild the Vite bundle, and deploy hosting-only so `https://gift-registry-ro.web.app/` renders the redesigned Phase 13 bundle instead of a blank page.

Purpose: Closes the immediate root cause of the blank page (folded todo `2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md`). This is Wave 1 and gates Plans 14-02, 14-03, and 14-04 — without a working hosted bundle, functions/rules deploys and UAT have nothing to validate against.

Output: A live, functional web fallback at `https://gift-registry-ro.web.app/` with real Firebase config wired and SPA routing verified. Plan SUMMARY captures the previous-known-good commit SHA + the `firebase hosting:rollback` runbook (D-03).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/ROADMAP.md
@.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md
@.planning/todos/pending/2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md
@.planning/phases/05-web-fallback/05-CONTEXT.md
@.planning/phases/05-web-fallback/05-VALIDATION.md
@web/.env.example
@web/src/firebase.ts
@web/vite.config.ts
@firebase.json
@.firebaserc

<interfaces>
<!-- Key contracts the executor needs. Already in codebase. -->

From web/src/firebase.ts:1-22:
```typescript
const FUNCTIONS_REGION = 'europe-west3'  // WEB-D-17 — DO NOT MODIFY

const firebaseConfig = {
  apiKey:            import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain:        import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId:         import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket:     import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId:             import.meta.env.VITE_FIREBASE_APP_ID,
}
export const app: FirebaseApp = initializeApp(firebaseConfig)  // throws on empty config
```

From web/vite.config.ts:15:
```typescript
build: {
  outDir: path.resolve(__dirname, '../hosting/public'),  // Vite outputs directly here
  emptyOutDir: true,  // wipes hosting/public/ on each build
}
```

From firebase.json (hosting block):
```json
"hosting": {
  "public": "hosting/public",
  "rewrites": [{ "source": "**", "destination": "/index.html" }]
}
```

From .firebaserc:
```json
{ "projects": { "default": "gift-registry-ro" } }
```
</interfaces>
</context>

<tasks>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 1: Register Firebase Web app and capture SDK config (manual one-shot — D-02)</name>
  <files>(no repo file changes — user runs CLI commands and pastes output)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-02 manual-op rationale)
    - .planning/todos/pending/2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md (canonical CLI sequence)
    - .firebaserc (confirms default project alias is gift-registry-ro)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Nothing yet — this checkpoint precedes any automation. The Firebase Web app registration is a one-shot, non-idempotent operation: re-running `firebase apps:create` creates a duplicate Web app entry in the project. Per CONTEXT.md D-02, the USER must run these commands so they own the registration moment.</what-built>
  <how-to-verify>
    Run these commands LOCALLY (in this order) and paste the output back into the executor chat:

    1. Confirm no existing Web app:
    ```
    firebase apps:list --project gift-registry-ro
    ```
    Expected: a list with the ANDROID app only; NO row of type `WEB`.

    If a WEB app already exists, STOP and skip to step 3 (use that app's ID).

    2. Create the Web app:
    ```
    firebase apps:create WEB "Gift Registry Web" --project gift-registry-ro
    ```
    Expected output: a JSON-ish block with `appId: 1:NNN:web:XXXX`. Copy this `appId`.

    3. Fetch the SDK config (the full config JSON the executor needs):
    ```
    firebase apps:sdkconfig WEB --project gift-registry-ro
    ```
    (If multiple Web apps exist, append the specific `appId` from step 2.)

    Expected output:
    ```js
    firebase.initializeApp({
      "apiKey": "AIza...",
      "authDomain": "gift-registry-ro.firebaseapp.com",
      "projectId": "gift-registry-ro",
      "storageBucket": "gift-registry-ro.firebasestorage.app",   // or .appspot.com
      "messagingSenderId": "NNNNNNNNNNNN",
      "appId": "1:NNN:web:XXXXXXXX"
    });
    ```

    PASTE the FULL output of step 3 back into the chat. The executor will extract the 6 values into `web/.env.local` in Task 2.
  </how-to-verify>
  <acceptance_criteria>
    - `firebase apps:list --project gift-registry-ro` output shows exactly ONE row of type `WEB` post-registration
    - User pastes back a JSON-like block containing all 6 keys: `apiKey`, `authDomain`, `projectId`, `storageBucket`, `messagingSenderId`, `appId`
    - `projectId` value equals exactly `gift-registry-ro` (any other value means wrong project — STOP)
    - `authDomain` value equals exactly `gift-registry-ro.firebaseapp.com`
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the full `firebase apps:sdkconfig WEB --project gift-registry-ro` output back into the chat, then type "config pasted".</resume-signal>
</task>

<task type="auto">
  <name>Task 2: Write web/.env.local with real Firebase config</name>
  <files>web/.env.local</files>
  <read_first>
    - web/.env.example (canonical key list and ordering — match exactly so the file is grep-able against the template)
    - web/src/firebase.ts (confirms which VITE_FIREBASE_* keys are actually consumed at runtime)
    - .gitignore (confirms `.env.local` line 36 — file must NOT be committed)
  </read_first>
  <action>
    Create `/Users/victorpop/ai-projects/gift-registry/web/.env.local` (this path is gitignored — DO NOT commit). Use the 6 values pasted back in Task 1.

    File content template (fill in EXACT values from Task 1 paste-back):
    ```
    # Firebase config — production gift-registry-ro Web app
    # Generated from `firebase apps:sdkconfig WEB --project gift-registry-ro` on {today}
    # API key is public (Firebase web keys are not secrets — App Check enforces request validity)
    VITE_FIREBASE_API_KEY=<paste apiKey value here, no quotes>
    VITE_FIREBASE_AUTH_DOMAIN=gift-registry-ro.firebaseapp.com
    VITE_FIREBASE_PROJECT_ID=gift-registry-ro
    VITE_FIREBASE_STORAGE_BUCKET=<paste storageBucket value here, no quotes>
    VITE_FIREBASE_MESSAGING_SENDER_ID=<paste messagingSenderId value here, no quotes>
    VITE_FIREBASE_APP_ID=<paste appId value here, no quotes>

    # App Check + reCAPTCHA — left empty intentionally. Plan 14-04 wires reCAPTCHA v3 site key
    # after monitor-mode verification (CONTEXT.md D-04). Until then, App Check is monitor-only
    # in console and the web client runs without an App Check provider on prod.
    VITE_RECAPTCHA_SITE_KEY=
    VITE_APP_CHECK_DEBUG_TOKEN=

    # Production deploy — DO NOT enable emulators
    VITE_USE_EMULATORS=false
    ```

    Notes:
    - Per `web/src/firebase.ts:30`, `VITE_USE_EMULATORS=false` (or unset) keeps the client pointed at prod
    - Per CONTEXT.md D-04, App Check stays monitor-only on first deploy — `VITE_RECAPTCHA_SITE_KEY` remains empty; Plan 14-04 sets this AFTER user registers a reCAPTCHA v3 site key in Firebase console
    - Per WEB-D-17 (referenced in CONTEXT.md canonical_refs), the `europe-west3` region pin lives in `web/src/firebase.ts:7` as a hardcoded constant — NOT an env var. Do not add a region env var.

    After writing the file, run:
    ```bash
    test -f /Users/victorpop/ai-projects/gift-registry/web/.env.local && grep -c "^VITE_FIREBASE_" /Users/victorpop/ai-projects/gift-registry/web/.env.local
    ```
    Expected stdout: `6` (six VITE_FIREBASE_* lines present).

    Confirm gitignore protection:
    ```bash
    git check-ignore /Users/victorpop/ai-projects/gift-registry/web/.env.local
    ```
    Expected stdout: `web/.env.local` (means git WILL ignore it).
  </action>
  <verify>
    <automated>test -f /Users/victorpop/ai-projects/gift-registry/web/.env.local && grep -E "^VITE_FIREBASE_PROJECT_ID=gift-registry-ro$" /Users/victorpop/ai-projects/gift-registry/web/.env.local && grep -E "^VITE_FIREBASE_AUTH_DOMAIN=gift-registry-ro\.firebaseapp\.com$" /Users/victorpop/ai-projects/gift-registry/web/.env.local && grep -E "^VITE_USE_EMULATORS=false$" /Users/victorpop/ai-projects/gift-registry/web/.env.local && git check-ignore /Users/victorpop/ai-projects/gift-registry/web/.env.local</automated>
  </verify>
  <acceptance_criteria>
    - `/Users/victorpop/ai-projects/gift-registry/web/.env.local` exists
    - File contains line `VITE_FIREBASE_PROJECT_ID=gift-registry-ro` (exact match)
    - File contains line `VITE_FIREBASE_AUTH_DOMAIN=gift-registry-ro.firebaseapp.com` (exact match)
    - File contains line `VITE_FIREBASE_API_KEY=AIza...` (any non-empty value starting with `AIza`)
    - File contains line `VITE_FIREBASE_APP_ID=1:` (any non-empty value starting with `1:`)
    - File contains line `VITE_USE_EMULATORS=false`
    - File contains line `VITE_RECAPTCHA_SITE_KEY=` (intentionally empty per D-04)
    - `grep -c "^VITE_FIREBASE_" web/.env.local` returns `6`
    - `git check-ignore web/.env.local` exits 0 with the path on stdout (gitignored)
  </acceptance_criteria>
  <done>web/.env.local is on disk with all 6 real Firebase config values, App Check site key intentionally empty per D-04, and file is gitignored.</done>
</task>

<task type="auto">
  <name>Task 3: Rebuild Vite bundle with real config baked in</name>
  <files>hosting/public/index.html, hosting/public/assets/*</files>
  <read_first>
    - web/vite.config.ts (confirm outDir is `../hosting/public` and emptyOutDir is true — build wipes the deploy root)
    - web/package.json (confirm `npm run build` script invokes Vite build)
    - .gitignore (lines 21-25 — confirm `hosting/public/assets/`, `index.html`, `vite.svg`, `.well-known/` are gitignored so the rebuild doesn't dirty the working tree with generated files)
  </read_first>
  <action>
    Rebuild the web bundle. The Vite build reads `web/.env.local` at build time and inlines the values into the JS bundles (Vite's `import.meta.env.*` is a compile-time substitution, NOT runtime).

    Run from repo root:
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/web && npm run build
    ```

    Expected: build completes in ~5-15s, writes to `/Users/victorpop/ai-projects/gift-registry/hosting/public/` (emptied first). Last lines of output should include `✓ built in` and a list of emitted assets (`index.html`, `assets/index-XXXXXX.js`, `assets/index-XXXXXX.css`).

    Verify the real apiKey actually got inlined (the original blank-page bug was empty strings in the bundle):
    ```bash
    grep -l 'apiKey:"AIza' /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js
    ```
    Expected: at least one file path on stdout. If empty, the env var didn't get baked in (most likely cause: `.env.local` written to wrong directory).

    Also verify projectId is in the bundle:
    ```bash
    grep -l 'projectId:"gift-registry-ro"' /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js
    ```
    Expected: at least one file path on stdout.

    DO NOT commit the rebuilt `hosting/public/` — it's gitignored.
  </action>
  <verify>
    <automated>test -f /Users/victorpop/ai-projects/gift-registry/hosting/public/index.html && grep -q 'id="root"' /Users/victorpop/ai-projects/gift-registry/hosting/public/index.html && ls /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js | head -1 && grep -l 'projectId:"gift-registry-ro"' /Users/victorpop/ai-projects/gift-registry/hosting/public/assets/*.js</automated>
  </verify>
  <acceptance_criteria>
    - `hosting/public/index.html` exists and contains `id="root"`
    - `hosting/public/assets/` contains at least one hashed `index-*.js` file
    - At least one bundled JS file contains the literal substring `projectId:"gift-registry-ro"` (confirms env var was inlined at build time, not left as empty string — this is the original blank-page bug's regression test)
    - At least one bundled JS file contains a substring matching `apiKey:"AIza` (real API key inlined)
    - Build command exited 0
  </acceptance_criteria>
  <done>Local bundle in hosting/public/ has the real Firebase config baked in; ready to deploy.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 4: Capture previous-known-good hosting release SHA (D-03 rollback prep)</name>
  <files>(no repo file changes — recording into the SUMMARY at plan close)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-03 rollback strategy)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Nothing — this is a pre-deploy snapshot. CONTEXT.md D-03 mandates capturing the previous-known-good hosting release identifier BEFORE the new deploy so `firebase hosting:rollback` has a known target if the deploy breaks production.</what-built>
  <how-to-verify>
    Run locally and paste output back into the chat:

    1. List current hosting releases (most recent first):
    ```
    firebase hosting:channel:list --project gift-registry-ro
    ```
    (This shows the live channel; the live release is the rollback target if the next deploy breaks anything.)

    2. Capture the current git HEAD SHA (the previous-known-good code-level reference):
    ```
    git -C /Users/victorpop/ai-projects/gift-registry rev-parse HEAD
    ```
    Expected: a 40-char SHA. As of context-gathering, HEAD was `75a121b...`.

    Paste BOTH outputs into the chat. The executor will record them in the plan SUMMARY's "Rollback runbook" section per D-03.
  </how-to-verify>
  <acceptance_criteria>
    - User pastes a 40-character git SHA (matches `[0-9a-f]{40}`) representing the previous-known-good code state
    - User pastes hosting channel info showing the current live release exists in the gift-registry-ro project
    - Both values stored verbatim in the plan SUMMARY under a "## Rollback runbook" section with the command `firebase hosting:rollback --project gift-registry-ro` documented
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the git SHA and channel list output, then type "rollback target captured".</resume-signal>
</task>

<task type="auto">
  <name>Task 5: Deploy hosting-only to production</name>
  <files>(deploys hosting/public/ to https://gift-registry-ro.web.app — no repo file changes)</files>
  <read_first>
    - firebase.json (confirms `hosting.public = "hosting/public"` and SPA rewrite `** → /index.html`)
    - .firebaserc (confirms default project alias resolves to gift-registry-ro — deploy will fail loudly if the alias is wrong)
  </read_first>
  <action>
    Deploy ONLY the hosting target (NOT functions, NOT rules — those are Plans 14-02 and 14-03):

    ```bash
    cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only hosting --project gift-registry-ro
    ```

    Expected output ends with:
    ```
    ✔  Deploy complete!

    Hosting URL: https://gift-registry-ro.web.app
    ```

    If the deploy fails (auth error, project mismatch, network), STOP. Do not retry blindly — surface the error to the user; they may need to run `firebase login` or fix `.firebaserc`.

    Successful deploy exit code: 0.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only hosting --project gift-registry-ro 2>&1 | tee /tmp/14-01-deploy.log && grep -q "Deploy complete" /tmp/14-01-deploy.log</automated>
  </verify>
  <acceptance_criteria>
    - `firebase deploy --only hosting --project gift-registry-ro` exits 0
    - Deploy log contains the literal string `Deploy complete!`
    - Deploy log contains the literal URL `https://gift-registry-ro.web.app`
    - Deploy log does NOT contain `Error:` or `failed to deploy`
  </acceptance_criteria>
  <done>Bundle deployed to https://gift-registry-ro.web.app; ready for the SPA + cache-headers verification step.</done>
</task>

<task type="auto">
  <name>Task 6: Verify SPA rewrite + cache headers + non-blank render (D-12)</name>
  <files>(curl-based verification — no repo file changes)</files>
  <read_first>
    - firebase.json (the SPA rewrite block being verified: `hosting.rewrites[**] → /index.html`)
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-12 — explicit post-deploy verification required)
  </read_first>
  <action>
    Per CONTEXT.md D-12, EXPLICITLY verify SPA rewrite and cache headers post-deploy (silent failure here would break private-registry rules-deny UAT #5 later).

    1. Verify root URL renders the SPA shell (no blank page):
    ```bash
    curl -sS https://gift-registry-ro.web.app/ | grep -E '(id="root"|projectId)'
    ```
    Expected: at least one matching line — the served HTML contains the React mount point.

    2. Verify cache headers on index.html (should be no-cache so future deploys are immediately visible):
    ```bash
    curl -sI https://gift-registry-ro.web.app/ | grep -iE '^(cache-control|content-type):'
    ```
    Expected:
    - `Content-Type: text/html; charset=utf-8`
    - `Cache-Control: max-age=3600` OR `Cache-Control: no-cache` OR similar revalidation directive (Firebase Hosting default for HTML is `max-age=3600`, NOT no-cache — that is acceptable per D-12 "sensible" wording; a long TTL like `max-age=31536000` on index.html would be a failure)

    3. Verify hashed assets get long-cache (immutable) headers:
    ```bash
    ASSET=$(curl -sS https://gift-registry-ro.web.app/ | grep -oE '/assets/index-[A-Za-z0-9_-]+\.js' | head -1)
    echo "Sampling asset: $ASSET"
    curl -sI "https://gift-registry-ro.web.app${ASSET}" | grep -iE '^cache-control:'
    ```
    Expected: `Cache-Control: max-age=31536000` (Firebase Hosting default for `/assets/*` hashed files).

    4. Verify SPA deep-link rewrite (D-12 explicit acceptance — the rewrite is what makes /registry/:id work without a backend route):
    ```bash
    curl -sS https://gift-registry-ro.web.app/registry/nonexistent-id-for-rewrite-test | grep -E 'id="root"'
    curl -s -o /dev/null -w "%{http_code}" https://gift-registry-ro.web.app/registry/nonexistent-id-for-rewrite-test
    ```
    Expected: first command outputs the React mount line (rewrite returned index.html). Second command prints `200`.

    If ANY of these fail, the deploy is broken — document in the SUMMARY and surface to user before proceeding to Plan 14-02/14-03.
  </action>
  <verify>
    <automated>curl -sS https://gift-registry-ro.web.app/ | grep -q 'id="root"' && curl -sS https://gift-registry-ro.web.app/ | grep -q 'projectId:"gift-registry-ro"' && curl -s -o /dev/null -w "%{http_code}" https://gift-registry-ro.web.app/registry/nonexistent-spa-test | grep -q "^200$" && curl -sS https://gift-registry-ro.web.app/registry/nonexistent-spa-test | grep -q 'id="root"'</automated>
  </verify>
  <acceptance_criteria>
    - `curl -sS https://gift-registry-ro.web.app/` returns HTML containing `id="root"`
    - The same response body contains the literal string `projectId:"gift-registry-ro"` (proves the real config is in the deployed bundle, not the previous empty-string bundle)
    - `curl -sI https://gift-registry-ro.web.app/` returns `Content-Type: text/html; charset=utf-8`
    - `curl -sI https://gift-registry-ro.web.app/` returns a `Cache-Control` header with a value other than `max-age=31536000` (HTML must not be cached for a year)
    - `curl -sI https://gift-registry-ro.web.app/assets/index-*.js` returns `Cache-Control: max-age=31536000` (hashed assets ARE cached forever)
    - `curl -s -o /dev/null -w "%{http_code}" https://gift-registry-ro.web.app/registry/nonexistent-spa-test` returns `200` (SPA rewrite intact)
    - The deep-link path body contains `id="root"` (proves rewrite returned index.html, not a 404 page)
  </acceptance_criteria>
  <done>Hosting is live, deep-links rewrite to SPA, cache headers are sane; no blank page. Plan 14-04 can now UAT against this deploy.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 7: Visual smoke-test in a real browser</name>
  <files>(manual visual verification — no repo file changes)</files>
  <read_first>
    - .planning/phases/13-web-fallback-visual-refresh/13-CONTEXT.md (what the redesigned bundle should look like — Housewarming palette, wordmark, Instrument Serif italic)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Bundle deployed at https://gift-registry-ro.web.app. Curl verification (Task 6) confirmed the HTML shell is non-empty and rewrites work, but only a real browser can confirm React mounts, fonts load, and the redesigned chrome renders.</what-built>
  <how-to-verify>
    1. Open https://gift-registry-ro.web.app/ in a fresh Chrome tab (NOT incognito — incognito is for UAT items in Plan 14-04).

    2. Confirm visually:
    - Page is NOT blank (the original bug)
    - GiftMaison wordmark appears in top nav (Instrument Serif italic with terracotta period — per Phase 13 D-13)
    - EN / RO language switch is visible in top nav
    - Body text uses Inter; headings may use Instrument Serif

    3. Open DevTools → Console. Confirm:
    - NO `FirebaseError: ...` red errors
    - NO `Failed to load resource: 401` for googleapis.com (App Check is monitor-only per D-04, so requests should succeed)
    - NO `Uncaught Error: initializeApp` errors

    4. Open DevTools → Network. Reload the page. Confirm:
    - `index.html` returns 200
    - `assets/index-*.js` returns 200
    - At least one request to `firestore.googleapis.com` or `firebase.googleapis.com` returns 200 (proves real config is talking to real Firebase)

    5. Try a known registry deep-link (substitute a real registry ID you own):
    - `https://gift-registry-ro.web.app/registry/<your-registry-id>`
    - Expected: registry detail page renders with the GiftMaison styling (hero image, item grid)

    If anything visual is broken OR console shows errors, STOP and report back — do NOT mark this plan complete.
  </how-to-verify>
  <acceptance_criteria>
    - User confirms https://gift-registry-ro.web.app/ renders a non-blank page in real Chrome
    - User confirms the GiftMaison wordmark is visible in top nav (Phase 13 D-13 rendered)
    - User confirms DevTools Console shows zero FirebaseError, zero 401 from googleapis, zero `initializeApp` errors
    - User confirms a real registry deep-link `/registry/<id>` renders the redesigned registry detail page (not the blank page, not a 404)
    - User pastes back any unexpected console warnings/errors for the executor to triage
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Type "visual smoke passed" or describe the issue (blank page / console error / wrong styling).</resume-signal>
</task>

</tasks>

<verification>
Combined Plan 14-01 verification checklist (all must pass before this plan closes):

1. `web/.env.local` exists with 6 VITE_FIREBASE_* values matching the registered Web app (Task 2)
2. `hosting/public/assets/*.js` contains `projectId:"gift-registry-ro"` and `apiKey:"AIza...` (Task 3 — proves config baked in)
3. `firebase deploy --only hosting` exited 0 (Task 5)
4. `curl https://gift-registry-ro.web.app/` returns HTML with `id="root"` AND `projectId:"gift-registry-ro"` (Task 6)
5. `curl -I https://gift-registry-ro.web.app/assets/index-*.js` returns `Cache-Control: max-age=31536000` (Task 6 — D-12 cache header)
6. `curl https://gift-registry-ro.web.app/registry/anything` returns 200 with `id="root"` (Task 6 — D-12 SPA rewrite)
7. User-confirmed visual smoke in real Chrome: non-blank, wordmark visible, no FirebaseError in console (Task 7)
</verification>

<success_criteria>
Plan 14-01 is complete when:
- [ ] Single Firebase Web app exists in gift-registry-ro (verified via `firebase apps:list`)
- [ ] web/.env.local has all 6 real config values, is gitignored
- [ ] Rebuilt bundle contains real `projectId:"gift-registry-ro"` (not empty string — original bug fix)
- [ ] `firebase deploy --only hosting` succeeded
- [ ] Curl checks pass: root returns HTML with id="root", SPA rewrite intact (deep-link returns 200 + shell), cache headers sane (HTML short-cache, /assets/* long-cache)
- [ ] User-verified in real Chrome: non-blank page, wordmark renders, no console errors
- [ ] SUMMARY.md captures: previous-known-good git SHA + rollback command `firebase hosting:rollback --project gift-registry-ro` (D-03)
- [ ] Wave 2 (Plans 14-02 + 14-03) is unblocked
</success_criteria>

<output>
After completion, create `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-01-SUMMARY.md` including:

1. What was deployed (web app ID, env vars set — values masked, deploy log highlights)
2. Verification evidence (curl outputs from Task 6, console smoke result from Task 7)
3. **Rollback runbook (D-03 — REQUIRED):**
   - Previous-known-good git SHA: `<value captured in Task 4>`
   - Rollback command: `firebase hosting:rollback --project gift-registry-ro`
   - Alternative code-rollback: `git checkout <SHA> && cd web && npm run build && firebase deploy --only hosting --project gift-registry-ro`
4. Known follow-ups (App Check site key still empty — wired in Plan 14-04 per D-04)
</output>
