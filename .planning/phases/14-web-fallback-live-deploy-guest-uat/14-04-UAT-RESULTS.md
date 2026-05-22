---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 04
artifact: uat-results
deploy_target: https://gift-registry-ro.web.app
hosting_deploy_commit: 78fed8d
appcheck_posture_at_uat: monitor-only (enforcement flip happens in Task 10)
created: 2026-05-21
status: in-progress (Pass 1 items 1-5 PASS; item 6 fourth attempt pending after three prod-bug fixes #1 queue-name, #2 OIDC, #3 region default; item 7 pending; Pass 2 pending; enforcement flip pending)
---

# Phase 14 Plan 04 — UAT Results

Production live-deploy verification ledger. Source-of-truth for the 7 manual UAT
items defined in `.planning/phases/05-web-fallback/05-VALIDATION.md`
"Manual-Only Verifications" table.

Layered per D-08:
- **Pass 1 — Solo Incognito:** User runs every item in fresh Chrome+Safari
  incognito profiles against the production deploy.
- **Pass 2 — Recruited Real Giver:** Real giver friend on their own device
  validates items 2 (retailer redirect) + 3 (guest localStorage) — catches
  "works on my machine" bias before Phase 14 closes.

---

## Pass 1 — Solo Incognito

| # | Item | Browser(s) | Result | Evidence | Logged At |
|---|------|------------|--------|----------|-----------|
| 1 | App Check `appcheck:exchange` returns 200 in real Chrome (D-05) | Chrome (regular tab, DevTools open) | **PASS** | DevTools Network panel filtered to `appcheck.googleapis.com` showed `appcheck:exchange` HTTP 200 firing BEFORE first `firestore.googleapis.com` request. Verified after fix to App Check secret-key mismatch + IndexedDB clear (see memory `reference_appcheck_cached_failure.md`). | 2026-05-21 |
| 2 | Retailer redirect opens new tab + keeps registry tab alive | Chrome + Safari (both incognito) | **PASS (both)** | Reserve flow opens new tab to retailer affiliate URL; registry tab stays open with sticky countdown banner; item shows "Reserved" with accent dot on return. Behavior identical in Chrome and Safari. | 2026-05-21 |
| 3 | Guest localStorage persists across browser restart | Chrome (incognito → quit → relaunch) | **PASS** | Tested via `ItemReservePage` "STEP 2 OF 2" CTA. After Cmd+Q + relaunch, the page detected prior guest identity from localStorage and reserved silently (no modal re-prompt). This is by design per `web/src/pages/ItemReservePage.tsx:273-296` — the `if (identity)` branch fires `reserveMutation.mutate` directly when identity exists, bypassing the guest-identity modal. The localStorage hydration round-trip is the proof; modal-skipping is the observable evidence. | 2026-05-21 |
| 4 | Romanian browser-locale autodetection on cold load (WEB-D-15) | Chrome (system+browser language switched to Romanian, full Cmd+Q + relaunch, fresh incognito) | **PASS** | UI rendered in Romanian on cold incognito load after switching system/Chrome to Romanian — i18next browser-detection working end-to-end against the deployed bundle. | 2026-05-21 |
| 5 | SPA deep-link to PRIVATE registry, unauthenticated → 404 | Chrome (fresh incognito, no auth session) | **PASS** | Direct paste of private-registry URL rendered the generic 404 page. No data leak (existence not revealed, owner-only data not shown). Firestore returned `permission-denied`, caught and mapped to 404 by the client per WEB-D-13/14. | 2026-05-21 |
| 6 | Email deep-link re-reserve end-to-end (natural 30-min path — D-09 amendment 2026-05-21) | Android prod-pointed APK + web fallback (incognito) + user's own mailbox | **IN PROGRESS (fourth attempt after region-qualifier fix)** | First attempt (17:40:44 UTC+3 reserve → 18:10:44 expected expiry) revealed prod bug #1: wrong Cloud Tasks queue name. Fix `bf4ca31` deployed `2026-05-21T15:25:26Z`. Second attempt revealed prod bug #2: tasks now enqueued correctly but dispatched without OIDC token → Cloud Run rejected with HTTP 403, task retried 3× then dropped. Fix `d0c7516` (refactor to Firebase Admin TaskQueue API) deployed `2026-05-22T09:31:10Z`. Third attempt (reservation `Jf38gRSE5gUBVzUgzVyp`, reserved 2026-05-22 12:50:39 UTC+3) revealed prod bug #3: Admin SDK `taskQueue("name")` form defaulted to `us-central1` lookup, returning `Queue does not exist` — our function + queue are in `europe-west3`. Fix `7ffb380` (region-qualified resource path) deployed `2026-05-22T10:24:16Z`. Three stuck reservations to clean manually. User to restart UAT-6 fourth attempt with a different item on the fixed deployment. | — |
| 7 | Google OAuth popup flow on deployed build | TBD | **PENDING** | Awaiting Task 7 verification. | — |

---

## UAT-6 plan — Natural 30-min path (D-09 amendment 2026-05-21)

The original D-09 plan was to seed a near-expiry reservation via a
60-second-delay Cloud Task targeting the deployed `releaseReservation`
onTaskDispatched handler, compressing the 30-min production wait to ~1 min.
That approach was abandoned during execution (see 14-CONTEXT.md "D-09
amendment" for the post-mortem). The replacement plan exercises the exact
same end-state pipeline — reservation → Cloud Task release → expiry email
→ re-reserve link click → new reservation — using the real 30-min production
timer instead of a compressed-time helper script.

**Pre-requisite check:** Confirm the prod-pointed APK
(`com.giftregistry`, built with `-Puse_emulator=false`) is still installed
on phone `WCR0219729000994`. If it was uninstalled:

```bash
cd /Users/victorpop/ai-projects/gift-registry
./gradlew :app:assembleDebug -Puse_emulator=false
~/Library/Android/sdk/platform-tools/adb -s WCR0219729000994 \
  install -r app/build/outputs/apk/debug/app-debug.apk
```

**Execution steps:**

1. Open the prod-pointed Android app, sign in.
2. Navigate to a PUBLIC test registry where the user owns an item with
   `status=available` and a valid `affiliateUrl`. Use a DIFFERENT item
   than the ones consumed by Pass 1 UAT-2 / UAT-3 so the slate is clean.
3. Tap **Reserve**. Record:
   - Timestamp (so the 30-min mark is known)
   - Reservation ID (visible in app, or via Firestore Console
     `reservations` collection filtered by the test giverEmail)
4. Wait the natural 30-min for the production Cloud Task to fire the
   deployed `releaseReservation` handler → `releaseReservationCore`
   transaction → expiry email + giver notification.
5. When the expiry email arrives in the user's own mailbox
   (the giverEmail used on the reservation):
   - Confirm the subject line (template:
     `functions/src/email/templates/expiry.ts`)
   - Confirm the CTA link in the email body is clickable.
     User-to-self mail typically does NOT trigger Gmail's
     link-strip heuristic, but if it DOES, that's a SECOND
     occurrence of the deferred bug
     `2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile.md`
     — log it as an additional data point under that todo, do NOT
     block UAT-6 closure on it.
   - Click the re-reserve CTA.
6. The web page should land on the registry detail page with
   `?autoReserveItemId=<itemId>` in the URL and auto-fire a new
   `createReservation` call (per WEB-D-11).
7. Verify in the Android app (refresh the registry) AND/OR
   Firebase Console > Firestore:
   - A NEW reservation exists with `status=active` and a fresh
     30-min `expiresAt` countdown.
   - The ORIGINAL reservation has `status=expired`.
   - The item's `status` was briefly `available` (between
     release and re-reserve) and is now back to `reserved`.

**Paste back into the chat:**

- Original reservation ID
- Expiry email subject line
- New reservation ID
- Then type `uat-6 passed` (or describe what broke if any step failed)

**Why this is equivalent to the abandoned seed approach:**
The deployed pipeline is the SAME code in both cases —
`onTaskDispatched releaseReservation` → `releaseReservationCore` →
`sendEmail` via Trigger Email extension → SendGrid delivery → user click
→ `resolveReservation` callable → `createReservation` callable. The only
difference is whether the Cloud Task was enqueued with a 60-second or
30-minute delay. The 30-min delay is what production users will actually
experience, so this UAT is arguably MORE faithful to the production
behaviour than the seed-script shortcut would have been.

---

## Production bugs fixed during Plan 14-04

### 2026-05-21 — `createReservation` Cloud Tasks queue name mismatch (auto-release silently broken since Phase 5/6 deploy)

- **Surfaced during:** UAT-6 first attempt (natural 30-min path).
- **Symptom:** User reserved an item via the prod-pointed Android app on
  phone `WCR0219729000994` at `2026-05-21 17:40:44 UTC+3`. At
  `18:10:44 UTC+3` (the recorded `expiresAt`) the reservation did NOT
  auto-release:
  - Firestore item still `status="reserved"`, with intact `reservedAt`,
    `reservedBy`, `expiresAt` fields.
  - No expiry email arrived in the giver's mailbox.
  - The Android app's sticky countdown banner read
    "0 MIN LEFT — AUTO-RELEASES IF NOT PURCHASED" with no state transition.
- **Root cause:** `functions/src/reservation/createReservation.ts:24`
  declared `const QUEUE_NAME = "release-reservation"` (hyphenated).
  Firebase 2nd-gen `onTaskDispatched` auto-creates a Cloud Tasks queue
  whose name matches the function export — `releaseReservation`
  (camelCase). The hyphenated queue does not exist in this project, so
  every `tasksClient.createTask({ parent: queuePath, ... })` call has
  been throwing `NOT_FOUND` since the original Phase 5/6 functions
  deploy. The error is caught by the `try/catch` on line 104, logs
  `[createReservation] Cloud Tasks enqueue failed (emulator?):` as a
  warning, and proceeds — committing the reservation with
  `cloudTaskName=""` and no scheduled release. The emulator-only
  `setTimeout` fallback (lines 115-128) gated by `FUNCTIONS_EMULATOR=true`
  is the only thing that has ever made the release fire in dev, which
  is why no Phase 5/6 emulator test caught it.
- **Confirmation:** User verified via
  https://console.cloud.google.com/cloudtasks?project=gift-registry-ro
  that only a `releaseReservation` queue (camelCase) exists — no
  `release-reservation` queue ever existed.
- **Why tests didn't catch it:** Unit tests for `createReservation`
  stub `CloudTasksClient` and don't assert against a real queue path.
  Tests for `releaseReservation` invoke the handler directly, never
  through the Cloud Tasks → onTaskDispatched dispatch path.
- **Fix:** Changed `QUEUE_NAME` from `"release-reservation"` to
  `"releaseReservation"` (one-line edit, commit `bf4ca31`). Built
  cleanly with `npm run build` before deploy.
- **Deploy:** `firebase deploy --only functions:createReservation
  --project gift-registry-ro`, completed `2026-05-21T15:25:26Z`
  (18:25 UTC+3). Deploy log at `/tmp/14-04-fix-deploy.log`. No other
  function touched; no rules / hosting / firebase.json change.
- **Stuck reservation cleanup:** The reservation from the first UAT
  attempt has `cloudTaskName=""` and no Cloud Task scheduled, so it
  will never auto-release on its own. User to clean it manually via
  Firestore Console (see checkpoint instructions).
- **Retest plan:** User reserves a DIFFERENT item via the prod-pointed
  Android app (so the just-cleaned item stays untouched as evidence),
  records timestamp + reservation ID, waits the natural 30 minutes,
  confirms (a) expiry email arrives, (b) re-reserve CTA fires
  `createReservation` again, (c) new reservation has populated
  `cloudTaskName` field. Optional in-flight check: Functions logs at
  https://console.firebase.google.com/project/gift-registry-ro/functions/logs?functionFilter=createReservation
  should NOT show the `Cloud Tasks enqueue failed` warning for the
  new reservation.
- **Scope:** Affected ALL production reservations since the original
  Phase 5/6 deploy, but only manifested as a UAT-blocking issue once
  end-to-end production verification (Phase 14) actually waited the
  natural 30 minutes. Confirms the value of UAT-6's natural-30-min
  path over the abandoned compressed-time seed-script approach.

### 2026-05-22 — `createReservation` Cloud Tasks dispatch missing OIDC token (HTTP 403, second silent prod bug)

- **Surfaced during:** UAT-6 retest after the queue-name fix `bf4ca31`.
  User reserved a fresh item, confirmed via Firestore Console that
  `cloudTaskName` was populated on the new reservation doc (proving the
  task was now reaching the `releaseReservation` queue), waited the
  natural 30 minutes — and the auto-release STILL did not fire.
- **Symptom:** Item (`/registry/x8K9QtRAjzQYNNkoiLAx/item/kgOb1Aei8N5opWisW6jS`
  — the VINDKAST veioză) remained `status="reserved"` past `expiresAt`
  with intact `reservedAt`, `reservedBy`, `expiresAt` fields. No expiry
  email. No state transition.
- **Smoking-gun log evidence from Cloud Tasks dispatch logs in prod:**
  ```
  "textPayload": "The request was not authenticated. ... Empty Authorization header value."
  "status": 403
  "userAgent": "Google-Cloud-Tasks"
  "requestUrl": "https://europe-west3-gift-registry-ro.cloudfunctions.net/releaseReservation"
  ```
  Two log entries 20s apart matching the `releaseReservation`
  `retryConfig: { maxAttempts: 3, minBackoffSeconds: 10 }`. The task
  was retried 3 times, each rejected by Cloud Run with 403, then
  dropped from the queue.
- **Root cause:** `createReservation.ts:84-132` used raw
  `@google-cloud/tasks` `CloudTasksClient.createTask()` constructing an
  `httpRequest` without an `oidcToken`. Firebase 2nd-gen
  `onTaskDispatched` functions deploy on Cloud Run revisions configured
  to require authenticated invocation; an unauthenticated POST is
  rejected with HTTP 403 "Empty Authorization header value". The task
  WAS enqueued correctly (so `cloudTaskName` populated, queue showed
  the task) but every dispatch attempt was denied at the Cloud Run
  ingress, and after the retry budget exhausted the task silently
  dropped.
- **Why bug #1 hid bug #2:** Until `bf4ca31`, tasks never even reached
  the queue (NOT_FOUND on `createTask`), so the dispatch-auth failure
  was untestable. Fixing the queue name surfaced the auth failure
  on the very next UAT attempt.
- **Fix:** Refactored the enqueue block in `createReservation.ts` to
  use the documented Firebase pattern:
  `getFunctions().taskQueue<ReleasePayload>("releaseReservation").enqueue(payload, { scheduleTime })`.
  The Firebase Admin SDK handles OIDC token generation, audience
  selection, and runtime service account binding automatically — these
  are the things the raw `@google-cloud/tasks` SDK leaves to the
  caller. `ReleasePayload` exported from `releaseReservation.ts` so the
  generic typechecks. `@google-cloud/tasks` dependency kept in
  `package.json` because `releaseReservationCallable.deleteTask`
  still uses it. Commit `d0c7516`. Built cleanly with
  `npm run build` before deploy.
- **Schema regression (minor, intentional):** The new TaskQueue API
  returns `Promise<void>`, so the reservation doc no longer carries
  `cloudTaskName`. Two implications:
  1. `releaseReservationCallable`'s `deleteTask` optimization (which
     uses `cloudTaskName` to cancel the scheduled task on manual
     release) is now a no-op — the `if (cloudTaskName)` guard skips it.
  2. A late-firing Cloud Task that arrives after a manual release is
     a harmless wasted invocation because `releaseReservationCore`
     is idempotent (no-ops when `status !== "active"`).
  Net effect: slightly more Cloud Run invocations on the manual-release
  edge case, zero correctness impact.
- **Deploy:** `firebase deploy --only functions:createReservation
  --project gift-registry-ro`, completed `2026-05-22T09:31:10Z`. Deploy
  log at `/tmp/14-04-oidc-deploy.log`. No other function touched; no
  rules / hosting / firebase.json change.
- **Stuck reservation cleanup:** The reservation from the bug-#2 UAT
  attempt (item `kgOb1Aei8N5opWisW6jS` on registry
  `x8K9QtRAjzQYNNkoiLAx`) has no Cloud Task scheduled (it was dropped
  after the 3 retries) and will not auto-release. Plus the bug-#1
  stuck reservation from the first attempt is still on file. User to
  clean both manually via Firestore Console (see checkpoint).
- **Retest plan (third UAT-6 attempt):** User reserves a DIFFERENT
  item via prod-pointed Android app on phone `WCR0219729000994`. Notes
  timestamp + new reservation ID. EXPECTED: the new reservation doc
  will NOT have a `cloudTaskName` field (or it will be absent / empty
  — that's the new normal post-fix). The positive signal lives
  elsewhere:
  - GCP Console > Cloud Tasks > `releaseReservation` queue:
    a task is visible (until it fires + auto-purges).
  - At expiry: Functions logs for `releaseReservation` show a 200
    invocation with `[releaseReservation] ...` log lines from
    `releaseReservationCore` (NOT another 403).
  - Item flips `status=available`; reservation flips `status=expired`;
    expiry email arrives.
- **Operational ask:** User to enable logging on the
  `releaseReservation` Cloud Tasks queue (GCP Console > Cloud Tasks
  > queue ⋯ menu > Enable logging or log sampling ratio 1.0) before
  the retest. Without it we were flying blind on the first two bugs.
- **Why tests didn't catch this either:** Unit tests for
  `createReservation` stub `CloudTasksClient` entirely; the integration
  surface between Cloud Tasks dispatch and Cloud Run authentication
  is only exercised by an actual deployed function receiving an actual
  dispatched task. Emulator does not reproduce Cloud Run's auth
  ingress. Confirms that production UAT remains irreplaceable for
  exactly this class of bug.

### 2026-05-22 — `createReservation` taskQueue lookup defaulted to `us-central1` (third silent prod bug)

- **Surfaced during:** UAT-6 third attempt after the OIDC fix `d0c7516`
  deployed at `2026-05-22T09:31:10Z`. User reserved a fresh item
  (reservation `Jf38gRSE5gUBVzUgzVyp`, created `2026-05-22 12:50:39
  UTC+3`) on prod-pointed Android app, phone `WCR0219729000994`.
- **Symptom:** The Cloud Tasks queue
  https://console.cloud.google.com/cloudtasks/queue/europe-west3/releaseReservation/tasks?project=gift-registry-ro
  showed "Tasks in queue: 0" / "No rows to display" — the enqueue
  never even landed in the queue. The reservation doc committed
  normally, but no task was scheduled. End-user behaviour identical to
  bugs #1 and #2: item stuck `status="reserved"` past `expiresAt`, no
  expiry email, app banner reads "0 MIN LEFT" forever.
- **Smoking-gun log evidence from Functions logs for `createReservation`
  at `2026-05-22T09:50:39.480Z`:**
  ```
  [createReservation] taskQueue.enqueue failed (emulator?):
    FirebaseFunctionsError: Queue does not exist. If you just created
    the queue, wait at least a minute for the queue to initialize.
    errorInfo: { code: 'functions/not-found', message: 'Queue does not exist...' }
  ```
  Caught by the same `try/catch` that was originally added for the
  emulator fallback path — which is why the reservation still committed
  cleanly while the queue stayed empty.
- **Root cause:** `getFunctions().taskQueue("releaseReservation")`
  with a bare queue-name string (no fully-qualified resource path)
  causes the Firebase Admin SDK to default the lookup to `us-central1`.
  Our function + auto-created queue live in `europe-west3` (see
  `REGION = "europe-west3"` constant at the top of
  `createReservation.ts` and the function's `{ region: REGION }`
  option). The SDK was probing
  `projects/gift-registry-ro/locations/us-central1/queues/releaseReservation`
  — right project, wrong region — and getting NOT_FOUND. The Firebase
  Admin docs only mention region defaulting in passing; the bare-name
  form is the example most code samples show, which is how it slipped
  through the refactor.
- **Why bug #2 hid bug #3:** Until `d0c7516`, the raw
  `@google-cloud/tasks` SDK with an explicit `parent` path bypassed
  the Admin SDK's region-defaulting behaviour entirely — we were
  constructing `projects/.../locations/europe-west3/queues/...`
  ourselves. Switching to `getFunctions().taskQueue()` to get
  automatic OIDC token handling also handed region resolution to the
  Admin SDK, which defaulted incorrectly. Each fix surfaced the next
  layer's bug.
- **Fix:** One-line change in `createReservation.ts:85` —
  `getFunctions().taskQueue<ReleasePayload>("releaseReservation")`
  → `getFunctions().taskQueue<ReleasePayload>(\`locations/${REGION}/functions/releaseReservation\`)`.
  The `REGION` constant was already in scope. Commit `7ffb380`. Built
  cleanly with `npm run build` before deploy.
- **Deploy:** `firebase deploy --only functions:createReservation
  --project gift-registry-ro`, completed `2026-05-22T10:24:16Z`.
  Deploy log at `/tmp/14-04-region-deploy.log`. No other function
  touched; no rules / hosting / firebase.json change.
- **Stuck reservation cleanup:** Reservation `Jf38gRSE5gUBVzUgzVyp`
  has no Cloud Task scheduled and will not auto-release. Plus the two
  prior stuck reservations from bug-#1 and bug-#2 attempts. User to
  clean all three manually via Firestore Console (see checkpoint).
- **Retest plan (fourth UAT-6 attempt):** User reserves a DIFFERENT
  item via prod-pointed Android app on phone `WCR0219729000994`.
  Notes timestamp + new reservation ID. Critical early signal added
  this round: check the Cloud Tasks queue console WITHIN 30 SECONDS
  of reserving — if a task is visible (with ETA ~30 min in the
  future), the region-fix worked and we can wait the 30-min path. If
  the task is NOT visible, do NOT wait — stop and grab the new error.
- **Trilogy retrospective (for Plan 14-04 SUMMARY):** Three
  consecutive production-only bugs (#1 queue-name mismatch, #2
  missing OIDC token, #3 region default) all manifested with
  identical end-user symptoms — Cloud Task never fires, reservation
  stays "reserved" forever, no email — but each had a distinct root
  cause in a different layer of the Cloud Tasks integration:
  (1) queue resource resolution, (2) dispatch-time authentication,
  (3) region-qualified resource resolution. Each fix surfaced the
  next layer's bug because the prior bug was masking it. Lessons
  for the SUMMARY:
  - The same `try/catch` around enqueue (added in good faith for
    emulator portability) silently swallowed all three bugs in prod,
    converting a hard failure into "reservation committed but no
    task". Consider promoting enqueue failures in prod (when
    `FUNCTIONS_EMULATOR` is unset) to a hard error OR a
    Firestore-logged failure record so they surface via alerting
    instead of hiding in `console.warn`.
  - Every bug was caught only by the natural 30-min UAT path
    (D-09 amendment 2026-05-21). A compressed-time seed-script
    approach would have hit the same NOT_FOUND / 403 / wrong-region
    errors — but at least UAT-6 made them visible.
  - Unit-level stubs for `CloudTasksClient` /
    `getFunctions().taskQueue()` cannot catch this class of bug.
    Future Cloud Tasks integrations should include a one-shot
    deployed smoke-test (reserve → 60s task → assert release) before
    the natural-flow UAT.
- **Why tests didn't catch this either:** Same as bug #2 —
  `getFunctions().taskQueue()` is stubbed in unit tests; region
  resolution only happens against live Google APIs. Emulator does not
  exercise the GCP region-resolution path at all.

---

## Pass 2 — Recruited Real Giver

| # | Item | Device | Result | Evidence | Logged At |
|---|------|--------|--------|----------|-----------|
| 2 | Retailer redirect opens new tab + keeps registry tab alive | TBD | **PENDING** | Awaiting Task 8 (recruited-giver session). | — |
| 3 | Guest localStorage persists across browser restart | TBD | **PENDING** | Awaiting Task 8 (recruited-giver session). | — |

---

## App Check Enforcement Flip

| Service | Posture During UAT | Target Posture | Flipped At |
|---------|--------------------|----------------|------------|
| Cloud Firestore | Monitor-only (D-04) | Enforced | TBD (Task 10) |
| Cloud Functions | Monitor-only (D-04) | Enforced | TBD (Task 10) |
| Cloud Storage | Monitor-only (D-04) | Enforced | TBD (Task 10) |

Rollback procedure (per D-03): Firebase Console > App Check > APIs tab > toggle
the affected service back to "Unenforced". Effective within ~1 minute. No code
change required.

---

## Deferred bugs

Real bugs surfaced during UAT that are NOT blockers for Phase 14 closure.
Each gets a dated pending-todo file in `.planning/todos/pending/` and is
logged here for traceability.

### 2026-05-21 — Invite-email CTA button stripped/non-clickable in Gmail mobile app

- **Surfaced during:** Pass 1 setup for UAT-5 (a real-world invitee was being
  invited to a private test registry so the rules-deny path could be exercised
  with their account; the invitee never reached the registry because the CTA
  in the invitation email was non-clickable in Gmail mobile).
- **Symptom:** Invitee's Gmail mobile app received the registry-invite email
  (initially in spam, then marked non-spam and moved to inbox). The "View
  registry" CTA button remained non-clickable from inbox.
- **Diagnostic state:** The Firestore `mail/` doc has the correct
  `<a href="https://gift-registry-ro.web.app/registry/S3JR2ntPqz6ruhXBFhXf">`
  in `message.html`. Template rendering verified end-to-end through
  `functions/src/email/templates/invite.ts` → `_shell.ts` → `sendEmail()`.
  Therefore the bug is NOT in our template, URL builder, nor
  `PUBLIC_WEB_BASE_URL` resolution.
- **Root cause hypothesis:** SMTP sender authentication missing (SPF / DKIM /
  DMARC not configured for the From: domain behind the Firebase Trigger Email
  extension). Gmail's link-protection / link-stripping heuristics keep CTA
  buttons disabled on emails from un-authenticated senders, even after the
  user marks the message as non-spam.
- **Scope notes:** NOT a blocker for Phase 14 closure (WEB-01..04 are about
  web-fallback access; this is about email deliverability — REG-06/07 +
  NOTF-02/03). Affects ALL transactional emails sent by the app: invite,
  expiry (UAT-6 may surface the same issue on the invitee's device), purchase
  confirmation. The expiry-email re-reserve link in UAT-6 is exercised by the
  current user (sender) so it's expected to work end-to-end for the UAT;
  the deliverability bug becomes visible only when the email is received by
  a stricter mailbox.
- **Logged:** `.planning/todos/pending/2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile.md`
- **Commit:** `2dd7f8e`
- **Follow-up path:** Quick-task once SMTP-extension config is inspected
  (`firebase ext:list` → identify provider → either fix DNS records or migrate
  to SendGrid/Postmark/Resend with verified sender domain).

---

## Test setup notes

- **Hosting URL under test:** `https://gift-registry-ro.web.app/`
- **Hosting bundle commit:** `78fed8d` ("feat(14-04): wire App Check reCAPTCHA v3 into firebase.ts, redeploy hosting")
- **Functions deployed commit:** `c0b7066` ("docs(14-02): complete plan — functions deployed with tsconfig + env cleanup")
- **Rules deployed:** firestore.rules + storage.rules per Plan 14-03 SUMMARY
- **App Check during Pass 1 items 1-5:** monitor-only (enforcement deliberately
  off per D-04 — the smoke-test window happens during Pass 1 UAT itself).

---

*Authored: 2026-05-21 by execute-phase Plan 14-04 (continuation)*
