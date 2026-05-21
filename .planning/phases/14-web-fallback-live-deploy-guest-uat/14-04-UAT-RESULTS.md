---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 04
artifact: uat-results
deploy_target: https://gift-registry-ro.web.app
hosting_deploy_commit: 78fed8d
appcheck_posture_at_uat: monitor-only (enforcement flip happens in Task 10)
created: 2026-05-21
status: in-progress (Pass 1 items 1-5 PASS; item 6 in progress via natural 30-min path; item 7 pending; Pass 2 pending; enforcement flip pending)
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
| 6 | Email deep-link re-reserve end-to-end (natural 30-min path — D-09 amendment 2026-05-21) | Android prod-pointed APK + web fallback (incognito) + user's own mailbox | **IN PROGRESS** | User reserves an item via the prod-pointed Android app on phone `WCR0219729000994`, waits the full 30-min for natural expiry, then clicks the re-reserve CTA in the inbox email. Replaces the abandoned D-09 seed-script approach (see 14-CONTEXT.md "D-09 amendment"). Awaiting user kickoff + 30-min wait + email click. | — |
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
