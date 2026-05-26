---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 06
subsystem: cross-cutting
tags: [android, firebase-app-check, cloud-functions, firestore-indexes, fcm, uat, on-device-verification, deploy]

# Dependency graph
requires:
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 02
    provides: acceptInvite + declineInvite + modified inviteToRegistry callables deployed by this plan
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 01
    provides: firestore.indexes.json composite index (users/*/notifications type asc, payload.registryId asc) deployed by this plan
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 03
    provides: NotificationRepositoryImpl wiring + LinkRegistryNotificationUseCase that UAT exercises end-to-end
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 04
    provides: InviteResponseSheet + InviteResponseViewModel state machine that Section A/B UAT validates
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 05
    provides: NotificationsScreen GiftMaison re-skin + 8 EN+RO string pairs + locked RO invite_sheet copy that Section D UAT validates
  - phase: 14-web-fallback-live-deploy-guest-uat
    provides: Phase 14 App Check infrastructure on the web; this plan closes the parked Android todo (2026-05-22-wire-android-app-check-and-flip-enforcement.md)
provides:
  - Android FirebaseAppCheck provider wired into GiftRegistryApp.onCreate (PlayIntegrity for release, Debug for emulator) — closes Phase 14 todo
  - Composite index `users/*/notifications` (type asc, payload.registryId asc) deployed to gift-registry-ro
  - 3 Cloud Functions deployed to europe-west3: acceptInvite, declineInvite, modified inviteToRegistry (writes pendingInvitedUsers)
  - 20/20 on-device UAT items PASS across 4 sections (accept, decline, edge cases, visual + locale) on emulator (Account A owner) + Huawei physical device (Account B invitee), both pointed at production backend
  - 4 UAT-surfaced deviation fixes: notifications bell entry point in HomeTopBar; FCM token registration on auth state change; PESTE→ACUM clock-skew clamp; JetBrains Mono TTF bundling
  - D-14 coupling stanza appended to 15-CONTEXT.md so Phase 15 resume team sees the linkInviteOnSignup→pendingInvitedUsers contract
affects:
  - Phase 16 production readiness: callables enforce App Check; Android client supplies App Check tokens; inbox UX works end-to-end
  - Future phases that add notifications: FCM token registration on auth state change is now the canonical pattern (NOT onNewToken-only)
  - Phase 15 (web invite-landing) resume: 15-CONTEXT.md stanza documents the pendingInvitedUsers coupling

# Tech tracking
tech-stack:
  added:
    - "firebase-appcheck-playintegrity (release variant) — already in libs.versions.toml from Phase 14, newly imported by GiftRegistryApp"
    - "firebase-appcheck-debug (debug variant) — already in libs.versions.toml from Phase 14, newly imported by GiftRegistryApp"
    - "JetBrains Mono v2.304 TTFs (Medium + SemiBold) — bundled as res/font/jetbrains_mono_{medium,semibold}.ttf; OFL license at assets/licenses/OFL_jetbrains_mono.txt"
  patterns:
    - "App Check init at Application.onCreate BEFORE any Firebase product is touched: FirebaseApp.initializeApp(this) → FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...) → only then Firestore / Functions / Auth get used. Order matters: providers installed after a Firebase operation has fired result in App Check tokens missing from in-flight requests."
    - "Debug provider for emulator builds, PlayIntegrity for release builds — debug provider auto-registers a UUID per device that must be pasted into Firebase Console → App Check → Debug tokens. AVD wipe regenerates the UUID; same for `pm clear` on physical devices. Document the workflow somewhere in onboarding docs (filed as Task #12)."
    - "FCM token registration on auth state change: FirebaseMessagingService.onNewToken fires only on install + token rotation, NOT on subsequent sign-ins. Post-install sign-ins leave users/{uid}/fcmTokens empty. Fix: subscribe to FirebaseAuth.addAuthStateListener in Application.onCreate and call FirebaseMessaging.getInstance().token + write to Firestore on every non-null user transition. This is the canonical pattern for any future phase adding notifications."
    - "DateUtils.getRelativeTimeSpanString clock-skew clamp: minOf(createdAtMs, now) on the FIRST argument prevents Romanian future-tense rendering ('peste 0 minute') when Firestore serverTimestamp() is microseconds ahead of System.currentTimeMillis(). EN-CLDR uses '0 minutes ago' symmetrically so the bug only manifests in locales with distinct past/future prepositions."
    - "GoogleFont async-load → bundled-TTF cutover: when a GoogleFont-loaded family shows inconsistent per-glyph rendering across sibling Composables on the same screen (some chars hit the downloaded font, others fall back to system sans-serif), the fix is the 260427-lnq precedent — bundle the TTFs at fixed weights in res/font/ and drop the GoogleFont entries. Already applied to InstrumentSerifFamily; now also to JetBrainsMonoFamily."
    - "Mid-UAT deviation handling: small UAT-surfaced bug fixes (Tasks 4, 5, 7, 8) committed atomically with deviation-marked commit messages, scope kept tight (each ~30-100 LoC). Alternative was gap-closure phase 16.1 but the bugs were small + necessary for the inbox to work at all + don't warrant the planning overhead."

key-files:
  created:
    - "app/src/main/res/font/jetbrains_mono_medium.ttf — JetBrains Mono v2.304 Medium TTF (~270KB), OFL-licensed"
    - "app/src/main/res/font/jetbrains_mono_semibold.ttf — JetBrains Mono v2.304 SemiBold TTF (~277KB), OFL-licensed"
    - "app/src/main/assets/licenses/OFL_jetbrains_mono.txt — SIL Open Font License v1.1 for JetBrains Mono"
    - ".planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-UAT-RESULTS.md — per-scenario PASS/FAIL log with device + backend setup"
    - ".planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-DEPLOY-LOG.md — deployment commands and verification output for Task 2"
  modified:
    - "app/src/main/java/com/giftregistry/GiftRegistryApp.kt — Task 1: App Check provider init in onCreate (debug or PlayIntegrity by build variant); Task 5: addAuthStateListener registers FCM token on every auth state change to users/{uid}/fcmTokens"
    - "app/src/main/java/com/giftregistry/ui/home/HomeTopBar.kt — Task 4: restored notifications bell + unread badge entry point removed during Phase 10 GiftMaison reskin"
    - "app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt — Task 7: clamp createdAtMs to now before DateUtils.getRelativeTimeSpanString to prevent Romanian future-tense rendering"
    - "app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt — Task 8: JetBrainsMonoFamily switched from GoogleFont async to bundled TTFs (matches InstrumentSerifFamily pattern from 260427-lnq)"
    - "app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt — Task 8: KDoc updated to note that Instrument Serif + JetBrains Mono are bundled; only Inter still loads via Google Fonts"
    - "firestore.indexes.json — Task 2: composite index for users/*/notifications already added by Plan 16-01 Wave 0; deployed by this plan"
    - "functions/src/index.ts (deployed only, no edit) — Task 2: acceptInvite / declineInvite / inviteToRegistry exports built by Plan 16-02"
    - ".planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md — Task 1: D-14 coupling stanza appended documenting linkInviteOnSignup → pendingInvitedUsers contract for Phase 15 resume team"
    - ".planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md → .planning/todos/completed/ — Task 1: closed by App Check wiring"

key-decisions:
  - "Tasks 4 + 5 + 7 + 8 added inline as Plan 16-06 deviations rather than routing through /gsd:plan-phase --gaps: all four were small (~30-100 LoC each), well-scoped to a single root cause, and necessary for Phase 16's inbox UX to be production-correct. Committed atomically with deviation-marked commit messages. Pattern reusable for future UAT-surfaced polish that emerges mid-execution."
  - "UAT runs against PRODUCTION backend, not emulators: both emulator (Account A owner) and Huawei physical (Account B invitee) built with -Puse_emulator=false. Production parity outweighs the emulator's lower friction because the deployed callables are the same code path as production and Cloud Tasks behavior (push delivery, callable enforcement) only matches reality with real backend. The -Puse_emulator=false flag is the workaround for the commit 8523217 default that broke physical-device Firebase calls (documented as task #12 follow-up to add to CLAUDE.md)."
  - "A3 foreground push rendered via onMessageReceived (not system tray) is Android-standard, not a Phase 16 bug. Marked PASS once backgrounded push verified arriving. In-app foreground banner is v1.1 polish (filed as task #15)."
  - "A5 InviteResponseSheet visual polish flagged but PASS — minor sub-pixel alignment in registry hero gradient stop boundaries that does not affect the accept-gate contract. Deferred to v1.1 visual review (filed as task #16)."
  - "16-06-UAT-RESULTS.md is the authoritative per-item status source. STATE.md and ROADMAP carry plan-level completion only; per-scenario UAT detail lives in the results doc."

patterns-established:
  - "App Check init order at Application.onCreate: FirebaseApp.initializeApp → installAppCheckProviderFactory(debug or PlayIntegrity) → THEN any Firebase product reads. Reusable for any future v1.0+ phase that ships new App Check-enforced callables."
  - "FCM token registration on auth state change (addAuthStateListener in Application.onCreate). Reusable for any future phase that depends on FCM push delivery for signed-in users."
  - "DateUtils clock-skew clamp via minOf(createdAtMs, System.currentTimeMillis()): apply at EVERY DateUtils.getRelativeTimeSpanString call site that consumes a Firestore-serverTimestamp Long. Future audit candidate: grep for getRelativeTimeSpanString to find untreated call sites."
  - "GoogleFont → bundled-TTF cutover (third application of the 260427-lnq pattern after Instrument Serif and JetBrains Mono): any future Phase that adds a fourth font family should ship it bundled from day one rather than going through async-GoogleFont → bundled-TTF cutover."
  - "Mid-UAT deviation fix workflow: when UAT surfaces a small bug, commit it as a Plan deviation task (Tasks N+1, N+2...) with explicit deviation note in the commit message. Update UAT-RESULTS.md to flag the original PASS-with-flag; once fix is verified, plain PASS. Avoid the planning overhead of /gsd:plan-phase --gaps for sub-100-LoC fixes."

requirements-completed:
  - D-01
  - D-03
  - D-05
  - D-07
  - D-09
  - D-14
  - D-20
  - D-23

requirements-deferred: []
---

# Plan 16-06 — Deploy + UAT

## Summary

Phase 16 (Android Notifications Inbox + Invite Accept/Decline) shipped to production with full on-device UAT signoff. 20/20 UAT items PASS across 4 sections, plus 4 UAT-surfaced bug fixes committed as deviation tasks.

## Tasks (chronological)

### Task 1 — Wire Android App Check + close Phase 14 follow-up + D-14 stanza (`f7a5a32`)

- Imported `firebase-appcheck` + `firebase-appcheck-playintegrity` (release) / `firebase-appcheck-debug` (debug) in `GiftRegistryApp.kt`
- `FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...)` called in `onCreate` BEFORE any Firestore/Functions/Auth use
- Build variant gate: debug → DebugAppCheckProviderFactory; release → PlayIntegrityAppCheckProviderFactory
- Closed `.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md` → moved to `completed/`
- Appended Phase 16 update stanza to `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md` documenting D-14 coupling (linkInviteOnSignup MUST target pendingInvitedUsers when Phase 15 resumes)

### Task 2 — Deploy composite index + 3 callables to gift-registry-ro (`7ff9b5e`)

- `firebase deploy --only firestore:indexes,functions --project gift-registry-ro` (europe-west3)
- Composite index `users/*/notifications` (type asc, payload.registryId asc) deployed and verified READY
- 3 Cloud Functions deployed: `acceptInvite`, `declineInvite`, modified `inviteToRegistry` (writes `pendingInvitedUsers`)
- Verification: `firebase functions:list --project gift-registry-ro` shows all 3 functions in europe-west3 with `enforceAppCheck: true`
- See `16-06-DEPLOY-LOG.md` for full output

### Task 4 — Restore notifications bell entry point in HomeTopBar (UAT-surfaced gap fix) (`8e901fc`)

- Pre-existing gap from Phase 10 GiftMaison reskin: comment in `HomeTopBar` said "bell placement deferred to Phase 11" but was never re-added
- Surfaced during A4 UAT (invitee couldn't reach the inbox to verify the card)
- Fix: added bell icon + unread badge to HomeTopBar trailing slot; navigates to NotificationsKey on tap
- Reuses existing `NotificationsViewModel.unreadCount` StateFlow

### Task 5 — Register FCM token on auth state change (UAT-surfaced gap fix) (`cf79b51`)

- Pre-existing Phase 6 gap: `MessagingHandler.onNewToken` fires only on install + token rotation, NOT on subsequent sign-ins; post-install sign-ins left `users/{uid}/fcmTokens` empty
- Surfaced during A3 UAT (push not delivered after fresh sign-in on Huawei)
- Fix: `FirebaseAuth.addAuthStateListener` in `GiftRegistryApp.onCreate` calls `FirebaseMessaging.getInstance().token` + writes to Firestore on every non-null user transition
- Memory entry saved: `feedback_fcm_token_signin.md`

### Task 3 — On-device UAT (20 scenarios) (`092adcb` partial handoff + this commit final)

- Devices: Pixel 36.1 AVD (Account A — owner) + Huawei physical (Account B — invitee, uid `dioPXtGkGcNw7xSX4DYsll5xiIn1`); both `-Puse_emulator=false` → production backend
- Section A (accept flow, 10 items): **10/10 PASS** — A5 originally flagged for minor visual polish (filed as task #16)
- Section B (decline flow, 6 items): **6/6 PASS** — required manual `invitedUsers[B-uid]` clear in Firestore Console before B1 to set up fresh pending state
- Section C (legacy + edge cases, 2 items): **2/2 PASS** — C1 used manually crafted legacy doc (no `pendingEntryKey`); C2 restored `invitedUsers[B-uid]` then re-invited to verify D-16 short-circuit
- Section D (visual + locale, 2 items): **2/2 PASS** — D1 initially flagged for timestamp size inconsistency (fixed in Task 8); D2 initially flagged for "PESTE 0 MINUTE" Romanian future-tense (fixed in Task 7)
- See `16-06-UAT-RESULTS.md` for per-scenario detail

### Task 7 — Clamp notification timestamp to now (UAT-surfaced polish fix) (`c59fc6f`)

- D2 UAT-surfaced: Romanian timestamps showed "PESTE 0 MINUTE" (future tense) instead of "ACUM 0 MINUTE" (past tense) for just-arrived invites
- Root cause: Firestore `serverTimestamp()` returns Long microseconds ahead of device `System.currentTimeMillis()`; Romanian CLDR uses distinct prepositions for past (acum) vs future (peste); EN-CLDR is symmetric so the bug only manifests in RO
- Fix in `NotificationsScreen.kt:215-228`: `val createdAt = minOf(notification.createdAtMs, now)` clamps clock-skew "future" to "now"
- Inline comment documents the WHY (non-obvious invariant — a reader could naively revert)
- Device-verified by user on emulator

### Task 8 — Bundle JetBrains Mono TTFs to fix MonoCaps font fallback (UAT-surfaced polish fix) (`8d43425`)

- D1 UAT-surfaced: NotificationsScreen timestamps rendered at inconsistent visual sizes across sibling cards on the same screen
- Root cause: `JetBrainsMonoFamily` loaded asynchronously via Google Fonts (GMS provider); different cards observed different cache states, with some glyphs falling back to system sans-serif at slightly different metrics
- Fix: same precedent as 260427-lnq applied to `InstrumentSerifFamily`
  - Downloaded JetBrains Mono v2.304 Medium + SemiBold TTFs from official repo
  - Bundled in `app/src/main/res/font/jetbrains_mono_{medium,semibold}.ttf`
  - OFL license bundled at `app/src/main/assets/licenses/OFL_jetbrains_mono.txt`
  - `JetBrainsMonoFamily` switched from `Font(googleFont=...)` to `Font(resId=...)`
- Device-verified by user

### Task 3-post — Close plan, advance state

- `16-06-UAT-RESULTS.md` updated: status=complete, 20/20 PASS, gaps section cleared
- `16-06-SUMMARY.md` written (this file)
- `HANDOFF.json` + `.continue-here.md` deleted (one-shot artifacts)
- STATE.md + ROADMAP advanced
- Plan 16-06 marked complete; phase verification gate next

## Deferred items (filed as follow-up todos)

- **Task #10** — File Huawei sign-in follow-up todo: pre-Phase-16 sign-in must have used `-Puse_emulator=false` explicitly; document the trap for future phases
- **Task #12** — Document `-Puse_emulator=false` flag in CLAUDE.md (build flag for physical devices when not pointing at AVD-loopback Firebase emulators)
- **Task #15** — Foreground invite push polish (v1.1): in-app banner when invite arrives while app is on-screen, instead of routing to `onMessageReceived` silently
- **Task #16** — A5 InviteResponseSheet visual polish (v1.1): minor sub-pixel alignment in registry hero gradient stop boundaries

## Verification

- 20/20 UAT items PASS on real backend + 2 devices (Pixel AVD + Huawei physical)
- App Check enforced: callables reject any device without a registered debug token (verified by temporarily revoking — see DEPLOY-LOG)
- All Plan 16-06 must_haves satisfied
- Tasks 7 + 8 device-verified by user (PESTE→ACUM Romanian fix; consistent JetBrains Mono rendering across all timestamps)
