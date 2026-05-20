---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 02
type: execute
wave: 2
depends_on: [14-01]
files_modified:
  - functions/tsconfig.json
  - functions/package.json
  - functions/.env
  - functions/.env.example
  - .gitignore
autonomous: false
requirements: [WEB-02]
must_haves:
  truths:
    - "From a fresh clone, `cd functions && rm -rf lib && npm run build` produces `functions/lib/index.js` (not `functions/lib/src/index.js`) and `firebase deploy --only functions` succeeds without --rootDir overrides or missing-env errors"
    - "`functions/.env` is committed with `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app` so the defineString param resolves in non-interactive deploys"
    - "All 2nd-gen Cloud Functions in `functions/src/` are deployed to region `europe-west3` in the gift-registry-ro project (WEB-D-17)"
    - "The notifications-inbox writeNotification helper (from quick-260420-ozb commit 04d007d) ships in this deploy — it currently exists only locally"
    - "`git ls-files functions/lib/ | wc -l` outputs 0 — confirms the CONTEXT.md D-10 'committed-but-stale lib/' claim was already obsolete; no `git rm` step required"
  artifacts:
    - path: "functions/tsconfig.json"
      provides: "Build config with rootDir:src and scripts/ removed from include"
      contains: "\"rootDir\": \"src\""
    - path: "functions/package.json"
      provides: "main entry pointing at lib/index.js (not lib/src/index.js) and seed:stores using ts-node"
      contains: "\"main\": \"lib/index.js\""
    - path: "functions/.env"
      provides: "Default PUBLIC_WEB_BASE_URL for non-interactive deploy (committed per D-10)"
      contains: "PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app"
  key_links:
    - from: "functions/package.json main"
      to: "functions/lib/index.js"
      via: "Firebase deploy entrypoint discovery"
      pattern: "\"main\":\\s*\"lib/index\\.js\""
    - from: "functions/src/config/publicUrls.ts defineString"
      to: "functions/.env PUBLIC_WEB_BASE_URL"
      via: "Firebase Functions params resolver at deploy time"
      pattern: "PUBLIC_WEB_BASE_URL"
---

<objective>
Apply the full tsconfig + env cleanup from CONTEXT.md D-10 (option-1 + option-3 combo from the folded todo), commit `functions/.env` with the public-default `PUBLIC_WEB_BASE_URL`, then deploy all Cloud Functions to the gift-registry-ro project. After this plan, `firebase deploy --only functions` "just works" from a fresh clone with zero workarounds.

Purpose: Closes folded todo `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md`. Ships the persistent-notifications-inbox functions (from quick task 260420-ozb commit `04d007d`) and any other 2nd-gen function changes pending since the last deploy. Runs in parallel with Plan 14-03 (rules deploy) per CONTEXT.md D-01 — both target the same project but `--only` scopes are disjoint (functions vs firestore,storage).

Output: Clean tsconfig + committed `functions/.env` + successful production functions deploy. Plan SUMMARY captures previous-known-good functions release timestamp + rollback runbook (D-03).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md
@.planning/todos/pending/2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md
@functions/tsconfig.json
@functions/package.json
@functions/src/config/publicUrls.ts
@firebase.json
@.gitignore

<interfaces>
<!-- Current state of files being modified (read before editing) -->

Current functions/tsconfig.json:
```json
{
  "compilerOptions": {
    "module": "commonjs",
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "outDir": "lib",
    "sourceMap": true,
    "strict": true,
    "target": "es2022",
    "esModuleInterop": true,
    "skipLibCheck": true
  },
  "compileOnSave": true,
  "include": ["src", "scripts"]
}
```
Issue: no `rootDir` → tsc picks `functions/` as common ancestor of `src` + `scripts` includes → output goes to `lib/src/*.js` and `lib/scripts/*.js`.

Current functions/package.json (relevant fields):
```json
{
  "main": "lib/src/index.js",      // current workaround for the wrong layout
  "scripts": {
    "build": "tsc",
    "seed:stores": "npm run build && node lib/scripts/seedStores.js"
  },
  "engines": { "node": "22" }
}
```

Current functions/src/config/publicUrls.ts:
```typescript
const DEFAULT_BASE_URL = "https://gift-registry-ro.web.app";
const PUBLIC_WEB_BASE_URL = defineString("PUBLIC_WEB_BASE_URL", { default: DEFAULT_BASE_URL });
```
Issue: despite `default`, `firebase deploy --non-interactive` still requires `PUBLIC_WEB_BASE_URL` in `functions/.env` (which is currently gitignored and missing from main).

Current .gitignore (line 11-13 + 36):
```
# Functions
functions/lib/
functions/node_modules/
...
# Environment
.env
.env.local
```
Issue: the `.env` rule (line 36) ignores ALL `.env` files including `functions/.env`. D-10 requires committing `functions/.env`, so we must add a negation exception.

Current functions/src/ entry points:
```
functions/src/
  __tests__/
  config/
  email/
  index.ts            <- top-level entry (export * from ...)
  notifications/
  registry/
  reservation/
```

Current functions/scripts/:
```
functions/scripts/
  seedStores.ts       <- one-shot maintenance script (NOT part of deployed bundle)
```
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Apply tsconfig + package.json cleanup (D-10 option-1 + option-3)</name>
  <files>functions/tsconfig.json, functions/package.json</files>
  <read_first>
    - functions/tsconfig.json (current state — confirm it matches the snapshot in interfaces)
    - functions/package.json (current "main" field and "seed:stores" script)
    - functions/src/index.ts (confirm it's the actual entry — top-level export aggregator)
    - functions/scripts/seedStores.ts (confirm it's the only file in scripts/; ts-node will need to compile it standalone)
    - .planning/todos/pending/2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md (canonical Part A spec)
  </read_first>
  <action>
    PER CONTEXT.md D-10 (option-1 + option-3 combo from the todo):

    **Step A — Rewrite `/Users/victorpop/ai-projects/gift-registry/functions/tsconfig.json`** to:
    ```json
    {
      "compilerOptions": {
        "module": "commonjs",
        "noImplicitReturns": true,
        "noUnusedLocals": true,
        "rootDir": "src",
        "outDir": "lib",
        "sourceMap": true,
        "strict": true,
        "target": "es2022",
        "esModuleInterop": true,
        "skipLibCheck": true
      },
      "compileOnSave": true,
      "include": ["src"]
    }
    ```
    Changes vs current: added `"rootDir": "src"`, removed `"scripts"` from `include`. Effect: `tsc` writes output to `lib/index.js` (not `lib/src/index.js`); scripts/ is no longer compiled by `npm run build`.

    **Step B — Edit `/Users/victorpop/ai-projects/gift-registry/functions/package.json`**:
    1. Change `"main": "lib/src/index.js"` → `"main": "lib/index.js"`
    2. Change `"seed:stores": "npm run build && node lib/scripts/seedStores.js"` → `"seed:stores": "ts-node scripts/seedStores.ts"`
    3. Add `"ts-node": "^10.9.0"` to `devDependencies` (so the seed script can run standalone without going through `tsc`)

    Leave all other fields untouched (engines.node="22", firebase-functions, firebase-admin, etc.).

    **Step C — Install ts-node** so the seed:stores script keeps working:
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/functions && npm install --save-dev ts-node@^10.9.0
    ```

    **Step D — Verify the build produces the correct layout**:
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/functions && rm -rf lib && npm run build && ls -la lib/
    ```
    Expected (after this change):
    - `lib/index.js` exists (NOT `lib/src/index.js`)
    - `lib/scripts/` does NOT exist (scripts/ no longer in include)
    - `lib/config/`, `lib/email/`, `lib/notifications/`, `lib/registry/`, `lib/reservation/` exist as flat directories under `lib/`

    If the layout is still wrong (e.g., `lib/src/` reappears), STOP — the rootDir change didn't take effect.

    **NOTE on CONTEXT.md D-10's "delete the committed-but-stale `functions/lib/`" instruction:**
    Per `git ls-files functions/lib/` checked during planning, `functions/lib/` is NOT tracked (it's gitignored via `.gitignore:13`). The "delete" instruction is satisfied by ABSENCE — there is nothing committed to `git rm`. The stale lib/ on the developer's machine is purely local and gets wiped by `rm -rf lib && npm run build` above. The acceptance criterion below locks this discovery as a verifiable assertion (`git ls-files functions/lib/ | wc -l` outputs `0`) so any future regression where lib/ accidentally gets committed will be caught.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/functions && grep -q '"rootDir": "src"' tsconfig.json && node -e 'const c=require("./tsconfig.json"); if (c.include.includes("scripts")) { console.error("tsconfig.json include still contains scripts/"); process.exit(1) }' && grep -q '"main": "lib/index.js"' package.json && grep -q '"seed:stores": "ts-node scripts/seedStores.ts"' package.json && rm -rf lib && npm run build && test -f lib/index.js && ! test -d lib/src && ! test -d lib/scripts && [ "$(cd /Users/victorpop/ai-projects/gift-registry && git ls-files functions/lib/ | wc -l | tr -d ' ')" = "0" ]</automated>
  </verify>
  <acceptance_criteria>
    - `functions/tsconfig.json` contains `"rootDir": "src"` (exact substring)
    - `functions/tsconfig.json` `include` array equals `["src"]` (NO `"scripts"` entry — verified structurally via `node -e 'require("./tsconfig.json").include.includes("scripts")'` returning false)
    - `functions/package.json` contains `"main": "lib/index.js"` (exact substring — NOT `lib/src/index.js`)
    - `functions/package.json` contains `"seed:stores": "ts-node scripts/seedStores.ts"` (exact substring)
    - `functions/package.json` devDependencies includes `"ts-node"` with semver value
    - After `rm -rf lib && npm run build`: `functions/lib/index.js` exists
    - After `rm -rf lib && npm run build`: `functions/lib/src/` directory does NOT exist
    - After `rm -rf lib && npm run build`: `functions/lib/scripts/` directory does NOT exist
    - Exit code of `npm run build` is 0 (no TypeScript errors introduced by the rootDir change)
    - `git ls-files functions/lib/ | wc -l` outputs `0` — confirms the CONTEXT.md D-10 "delete the committed-but-stale lib/" instruction is satisfied by absence; no `git rm` step needed
  </acceptance_criteria>
  <done>Build produces the canonical `lib/index.js` layout; seed:stores still works via ts-node (verified separately if desired); no committed-lib drift possible because lib/ is gitignored and the absence assertion is now part of the verify gate.</done>
</task>

<task type="auto">
  <name>Task 2: Commit functions/.env with public default (D-10) and update .gitignore + .env.example</name>
  <files>functions/.env, functions/.env.example, .gitignore</files>
  <read_first>
    - .gitignore (lines 35-37 — confirms `.env` line currently catches `functions/.env`; need a negation exception)
    - functions/src/config/publicUrls.ts (confirms `PUBLIC_WEB_BASE_URL` is the ONLY defineString param in use — no other env vars need committing)
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-10 + the "URL isn't sensitive" rationale in specifics)
  </read_first>
  <action>
    PER CONTEXT.md D-10 ("commit functions/.env with the public default; URL isn't sensitive; 'firebase deploy just works from a fresh clone' is worth more than conceptual purity"):

    **Step A — Create `/Users/victorpop/ai-projects/gift-registry/functions/.env`** with content:
    ```
    # Functions environment — defaults for defineString params used at deploy time.
    # Committed deliberately (see Phase 14 Plan 02 / 14-CONTEXT.md D-10).
    # The URL below is the public Firebase Hosting domain — not a secret.
    # Override locally with functions/.env.local (gitignored) or via `firebase functions:config:set`.
    PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app
    ```

    **Step B — Update `/Users/victorpop/ai-projects/gift-registry/.gitignore`** to add a negation that lets `functions/.env` through while keeping `functions/.env.local` (and all other `.env`s) ignored.

    Modify the `# Environment` block (currently lines 35-37):
    ```
    # Environment
    .env
    .env.local

    # Exception: functions/.env is committed with the public PUBLIC_WEB_BASE_URL default
    # (see Phase 14 Plan 02 / 14-CONTEXT.md D-10). Other .env files remain gitignored.
    !functions/.env
    ```

    Verify the exception works:
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry && git check-ignore functions/.env || echo "NOT IGNORED (correct)"
    git check-ignore functions/.env.local && echo "STILL IGNORED (correct)" || echo "WARNING: functions/.env.local would be tracked"
    git check-ignore web/.env.local && echo "STILL IGNORED (correct)" || echo "WARNING: web/.env.local would be tracked"
    ```
    Expected: `functions/.env` returns "NOT IGNORED"; `functions/.env.local` and `web/.env.local` still return "STILL IGNORED".

    **Step C — Update `/Users/victorpop/ai-projects/gift-registry/functions/.env.example`** to document the same default (so the convention is discoverable):
    ```
    # Functions environment — committed defaults. See functions/.env for actual values.
    # Override locally with functions/.env.local (gitignored).
    PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app
    ```

    Note: `.env.example` is also caught by the `.env` gitignore rule. The same negation could be applied for it, but it's optional — many repos accept .env.example being untracked since it's documentation. For consistency, ALSO add `!functions/.env.example` to the gitignore exception block.

    Updated gitignore block:
    ```
    # Environment
    .env
    .env.local

    # Exception: functions/.env + .env.example are committed (Phase 14 Plan 02 / D-10).
    # The PUBLIC_WEB_BASE_URL value is the public Firebase Hosting domain — not a secret.
    !functions/.env
    !functions/.env.example
    ```
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && test -f functions/.env && grep -q "^PUBLIC_WEB_BASE_URL=https://gift-registry-ro\.web\.app$" functions/.env && test -f functions/.env.example && grep -q "^PUBLIC_WEB_BASE_URL=" functions/.env.example && grep -q "^!functions/\.env$" .gitignore && ! git check-ignore functions/.env && git check-ignore web/.env.local</automated>
  </verify>
  <acceptance_criteria>
    - `functions/.env` exists and contains line `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app` (exact)
    - `functions/.env.example` exists and contains line `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app` (exact)
    - `.gitignore` contains line `!functions/.env` (the negation exception)
    - `.gitignore` contains line `!functions/.env.example`
    - `git check-ignore functions/.env` exits with status 1 (NOT ignored — will be tracked when added)
    - `git check-ignore functions/.env.local` exits 0 (still ignored — local overrides safe)
    - `git check-ignore web/.env.local` exits 0 (still ignored — Plan 14-01's secrets safe)
  </acceptance_criteria>
  <done>functions/.env is committable with the public-default URL; gitignore negation is scoped so only this one file slips through; future local `.env.local` overrides remain ignored.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Capture previous-known-good functions deploy timestamp (D-03 rollback prep)</name>
  <files>(no repo file changes — recording into SUMMARY)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-03 rollback strategy)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Nothing — pre-deploy rollback snapshot. Unlike hosting (which has a one-command `firebase hosting:rollback`), Cloud Functions rollback requires either redeploying a previous git SHA or using Cloud Run revision rollback (2nd-gen functions are Cloud Run under the hood). D-03 mandates capturing the rollback target before the new deploy.</what-built>
  <how-to-verify>
    Run locally and paste the output back into the chat:

    1. Capture current git HEAD SHA (this is the rollback code target):
    ```
    git -C /Users/victorpop/ai-projects/gift-registry rev-parse HEAD
    ```

    2. List currently-deployed function names + versions (so SUMMARY documents what was running before):
    ```
    firebase functions:list --project gift-registry-ro
    ```
    Expected: a table of function names (e.g. `createReservation`, `releaseReservation`, `fetchOgMetadata`, etc.) with region `europe-west3` and current version stamps.

    3. (Optional but recommended) List Cloud Run revisions for one canary function to confirm 2nd-gen rollback path is available:
    ```
    gcloud run revisions list --service=createreservation --region=europe-west3 --project=gift-registry-ro --limit=5 2>/dev/null || echo "gcloud not available; rollback via git checkout + redeploy"
    ```

    PASTE outputs back into the chat. The executor records them in the SUMMARY's rollback section.
  </how-to-verify>
  <acceptance_criteria>
    - User pastes a 40-char git SHA
    - User pastes `firebase functions:list` output showing functions exist in `europe-west3` (WEB-D-17 region pin verified intact)
    - SUMMARY's Rollback runbook section will document: (a) git SHA, (b) command `git checkout <SHA> && cd functions && npm install && firebase deploy --only functions --project gift-registry-ro`, (c) emergency Cloud Run revision rollback via `gcloud run services update-traffic <function-name> --to-revisions <previous-revision>=100 --region europe-west3 --project gift-registry-ro`
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the git SHA and functions:list output, then type "rollback target captured".</resume-signal>
</task>

<task type="auto">
  <name>Task 4: Deploy functions to production</name>
  <files>(deploys functions/lib/* to Cloud Functions in europe-west3 — no repo file changes)</files>
  <read_first>
    - functions/package.json (confirm main=lib/index.js after Task 1 edits)
    - functions/.env (confirm PUBLIC_WEB_BASE_URL line is present after Task 2)
    - firebase.json (confirm functions block points at `source: functions` — no codebase rename)
    - .firebaserc (confirm default project is gift-registry-ro)
  </read_first>
  <action>
    Deploy ONLY the functions target (NOT hosting — Plan 14-01 already shipped that; NOT firestore/storage — that's Plan 14-03 running in parallel):

    ```bash
    cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only functions --project gift-registry-ro
    ```

    Expected output (interleaved):
    ```
    === Deploying to 'gift-registry-ro'...
    i  deploying functions
    i  functions: ensuring required API ... is enabled... (already enabled, skipped on subsequent deploys)
    i  functions: preparing codebase default for deployment
    i  functions: creating Node.js 22 (2nd Gen) function ... (or "updating Node.js 22 (2nd Gen) function ...")
    ...
    ✔  functions[xxxxx(europe-west3)]: Successful create/update operation.
    ...
    ✔  Deploy complete!
    ```

    KEY ACCEPTANCE: NO occurrence of:
    - `Error: In non-interactive mode but have no value for the following environment variables: PUBLIC_WEB_BASE_URL` (Task 2 prevents this)
    - `Error: functions/lib/index.js does not exist` (Task 1 prevents this)
    - Any function deploying to a region OTHER than `europe-west3` (WEB-D-17 violation)

    If the deploy fails, capture full output and STOP. Do NOT retry blindly — check the specific error against the two known issues that Tasks 1+2 fix.

    The deploy ships everything currently in `functions/src/`, including:
    - The 5 event sources wired with `writeNotification` (from quick-260420-ozb commit `04d007d`)
    - The notifications subcollection writes (matching the `firestore.rules` block also from that commit — which deploys in Plan 14-03)
    - All Phase 4 reservation functions (createReservation, releaseReservation), Phase 3 fetchOgMetadata, etc.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only functions --project gift-registry-ro 2>&1 | tee /tmp/14-02-deploy.log && grep -q "Deploy complete" /tmp/14-02-deploy.log && ! grep -qiE "Error:|In non-interactive mode but have no value" /tmp/14-02-deploy.log && grep -q "europe-west3" /tmp/14-02-deploy.log</automated>
  </verify>
  <acceptance_criteria>
    - `firebase deploy --only functions --project gift-registry-ro` exits 0
    - Deploy log contains literal string `Deploy complete!`
    - Deploy log contains at least one `(europe-west3)` substring (region pin verified — WEB-D-17)
    - Deploy log does NOT contain `In non-interactive mode but have no value` (defineString fix works)
    - Deploy log does NOT contain `functions/lib/index.js does not exist` (tsconfig fix works)
    - Deploy log does NOT contain any `Error:` lines (other than informational lines about already-enabled APIs which are info-level not errors)
    - Post-deploy, `firebase functions:list --project gift-registry-ro` shows all functions in `europe-west3` (no rogue us-central1 functions)
  </acceptance_criteria>
  <done>All functions deployed to europe-west3 on gift-registry-ro; no env/tsconfig workarounds needed; persistent notifications functions (from 260420-ozb) are live.</done>
</task>

<task type="auto">
  <name>Task 5: Post-deploy smoke — verify a callable returns a sane response</name>
  <files>(no repo file changes — verification only)</files>
  <read_first>
    - functions/src/index.ts (confirm which callables are exported)
    - functions/src/reservation/createReservation.ts (most critical callable — gates Plan 14-04's UAT)
  </read_first>
  <action>
    Smoke-test that the deployed functions are actually reachable via Firebase JS SDK conventions. We're not testing business logic here (that's Plan 14-04 UAT) — just confirming the HTTPS endpoints respond on the europe-west3 region.

    1. Confirm function endpoint URLs exist for the europe-west3 region. For 2nd-gen functions, callable endpoints follow the pattern:
    `https://<function-name>-<hash>-ew.a.run.app` (Cloud Run URL)
    OR
    `https://europe-west3-gift-registry-ro.cloudfunctions.net/<functionName>` (Functions URL)

    Run:
    ```bash
    firebase functions:list --project gift-registry-ro 2>&1 | grep -E "(createReservation|resolveReservation|fetchOgMetadata|writeNotification|europe-west3)"
    ```
    Expected: multiple lines matching, each showing region `europe-west3` (NOT `us-central1`).

    2. Smoke an HTTPS callable without auth (we expect 401/403 NOT 404):
    ```bash
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "https://europe-west3-gift-registry-ro.cloudfunctions.net/createReservation" -H "Content-Type: application/json" -d '{"data":{}}'
    ```
    Expected status: `401` or `403` or `400` (auth/validation rejection) — NOT `404` (which would mean the function isn't deployed) and NOT `500` (server error during cold start).

    If the curl returns 404: the function isn't deployed under that name/region — STOP and check the deploy log.

    If the curl returns 500 with a Functions framework stack trace: cold-start crash, likely a build artifact problem — STOP and investigate.

    3. Run Firebase Functions log tail for the last 5 minutes to confirm no startup errors:
    ```bash
    firebase functions:log --project gift-registry-ro --limit 20 2>&1 | head -50
    ```
    Expected: no `Error:` lines, no `Cannot find module 'lib/src/index.js'` lines, no unhandled rejection messages.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && firebase functions:list --project gift-registry-ro 2>&1 | grep -q "europe-west3" && STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "https://europe-west3-gift-registry-ro.cloudfunctions.net/createReservation" -H "Content-Type: application/json" -d '{"data":{}}') && [ "$STATUS" != "404" ] && [ "$STATUS" != "000" ] && echo "Callable reachable with HTTP $STATUS"</automated>
  </verify>
  <acceptance_criteria>
    - `firebase functions:list --project gift-registry-ro` output contains at least one line with `europe-west3` (region pin lives)
    - `curl https://europe-west3-gift-registry-ro.cloudfunctions.net/createReservation` returns an HTTP status code that is NOT 404 and NOT 000 (i.e. function endpoint exists and responds; 401/403/400 is acceptable, those are auth/validation rejections)
    - `firebase functions:log --project gift-registry-ro --limit 20` does NOT contain `Cannot find module` errors
    - `firebase functions:log --project gift-registry-ro --limit 20` does NOT contain `MODULE_NOT_FOUND` errors
  </acceptance_criteria>
  <done>Functions are deployed, reachable on europe-west3, and not crashing on cold start; ready for Plan 14-04 UAT to exercise real business logic.</done>
</task>

</tasks>

<verification>
Combined Plan 14-02 verification (all must pass):

1. `functions/tsconfig.json` has `"rootDir": "src"` and `include: ["src"]` (no scripts) — Task 1
2. `functions/package.json` has `"main": "lib/index.js"` (not lib/src/index.js) — Task 1
3. `rm -rf functions/lib && cd functions && npm run build` produces `functions/lib/index.js` (NOT `functions/lib/src/index.js`) — Task 1
4. `git ls-files functions/lib/ | wc -l` outputs `0` — confirms D-10 "stale lib/" claim was already obsolete — Task 1
5. `functions/.env` exists with `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app` AND is NOT gitignored — Task 2
6. `web/.env.local` is STILL gitignored (Plan 14-01 secrets protection unaffected) — Task 2
7. `firebase deploy --only functions --project gift-registry-ro` exits 0 with `Deploy complete!` and at least one `europe-west3` mention — Task 4
8. Deploy log contains zero `In non-interactive mode but have no value` errors and zero `functions/lib/index.js does not exist` errors — Task 4
9. Curl to `https://europe-west3-gift-registry-ro.cloudfunctions.net/createReservation` returns a non-404 status — Task 5
</verification>

<success_criteria>
Plan 14-02 is complete when:
- [ ] tsconfig produces `lib/index.js` (not `lib/src/index.js`) from a fresh build
- [ ] `git ls-files functions/lib/` returns no rows (lib/ stays out of git)
- [ ] package.json `main` and `seed:stores` are updated, ts-node installed
- [ ] `functions/.env` committed with public default; .gitignore negation in place; web/.env.local still safe
- [ ] Functions deploy succeeded with zero workarounds (no `--rootDir` override, no missing-env errors)
- [ ] All deployed functions are in `europe-west3` (WEB-D-17 verified)
- [ ] Post-deploy smoke: createReservation callable is reachable (non-404)
- [ ] SUMMARY.md captures: previous git SHA, rollback command (`git checkout <SHA> && cd functions && npm install && firebase deploy --only functions --project gift-registry-ro`), emergency Cloud Run revision rollback command
- [ ] Plan 14-04 UAT items 6 (email re-reserve via functions:shell) and 2 (retailer redirect via createReservation) are unblocked
</success_criteria>

<output>
After completion, create `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-02-SUMMARY.md` including:

1. What was changed (tsconfig diff, package.json diff, .env committed, .gitignore negation)
2. Build verification (`ls functions/lib/` output before & after — proves the layout fix)
3. Deploy log highlights (function names deployed, region, version stamps)
4. **Rollback runbook (D-03 — REQUIRED):**
   - Previous-known-good git SHA: `<from Task 3>`
   - Code rollback: `git checkout <SHA> && cd functions && npm install && firebase deploy --only functions --project gift-registry-ro`
   - Emergency per-function rollback (Cloud Run): `gcloud run services update-traffic <function-name> --to-revisions=<previous-rev>=100 --region=europe-west3 --project=gift-registry-ro`
5. Confirmation that the persistent-notifications functions from quick-260420-ozb (`writeNotification` helper + 5 event sources) are now live in prod
6. Note: folded todo `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` can be moved from `pending/` to `complete/` after this plan ships
</output>
</output>
