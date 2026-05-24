---
phase: 16
slug: android-notifications-inbox-invite-accept-decline
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-05-24
updated: 2026-05-24
---

# Phase 16 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + MockK + Turbine (Android), Jest (Functions), @firebase/rules-unit-testing (Rules) |
| **Config file** | `app/build.gradle.kts` (Android), `functions/jest.config.js`, `firestore.rules.test.ts` (rules) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*Notification*"` |
| **Full suite command** | `./gradlew test && (cd functions && npm test) && npm test --prefix functions -- firestore.rules` |
| **Estimated runtime** | ~90 seconds |

---

## Sampling Rate

- **After every task commit:** Run scoped quick test (`./gradlew :app:testDebugUnitTest --tests "*<TouchedSymbol>*"` or `npm test -- <fileGlob>`)
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

> Populated 2026-05-24 from the `<verify><automated>` commands in each plan task. Every task has an automated verification command or an explicit Wave 0 dependency. Total: 14 tasks across 6 plans.

| Task ID | Plan | Wave | Requirements | Test Type | Automated Command | File Exists | Status |
|---------|------|------|--------------|-----------|-------------------|-------------|--------|
| 16-01-T1 | 16-01-wave-0-red-tests-and-index | 1 | D-07, D-11, D-25, D-27 (Wave 0 RED tests) | Android unit (RED scaffold) | `./gradlew :app:compileDebugUnitTestKotlin` + grep for "INVITE_ACCEPTED_SELF\|InviteResponseViewModel\|NotificationRepositoryImpl(firestore, functions)" in compile log + `ls` on 5 test files | yes (5 Android test files) | ⬜ pending |
| 16-01-T2 | 16-01-wave-0-red-tests-and-index | 1 | D-18, D-19, D-21, D-22, D-23, D-24, D-28 (rules tests + index + functions RED) | Functions tsc + rules tests + JSON validation | `cd functions && npx tsc --noEmit` (expects RED on acceptInvite/declineInvite modules) + `cd tests/rules && npm test` (PASS — Pattern 8) + python3 JSON validation of firestore.indexes.json + assert notifications composite index present | yes (2 new Jest test files + extended rules test + composite index) | ⬜ pending |
| 16-02-T1 | 16-02-backend-callables-and-invite-pending | 2 | D-20, D-21, D-22, D-24 (acceptInvite + declineInvite + helpers) | Jest (Functions, transactional) | `cd functions && npm test -- acceptInvite declineInvite` | yes (acceptInvite.ts, declineInvite.ts, inviteNotificationHelpers.ts) | ⬜ pending |
| 16-02-T2 | 16-02-backend-callables-and-invite-pending | 2 | D-10, D-13, D-15, D-16, D-23 (inviteToRegistry — pendingInvitedUsers + enriched payload + D-16 already-member branch; D-15 negative-coverage no blacklist) | Jest (Functions) + grep negative-coverage | `cd functions && npm test -- inviteToRegistry` AND `! grep -rE "blacklist\|declinedUsers\|declinedSet" functions/src/registry/*.ts` (D-15 verify) | yes (modified inviteToRegistry.ts + index.ts) | ⬜ pending |
| 16-03-T1 | 16-03-android-domain-data-layer | 2 | D-25, D-26, D-27 (NotificationType + Repository + Impl extensions; D-26 payload Map shape unchanged) | Android unit (JUnit + MockK) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest" --tests "com.giftregistry.data.notifications.NotificationRepositoryImplAcceptDeclineTest"` (flips RED → GREEN; 9+3 tests) | yes (3 modified Kotlin files) | ⬜ pending |
| 16-04-T1 | 16-04-invite-response-sheet-and-viewmodel | 3 | D-07 (InviteResponseViewModel state machine) | Android unit (Turbine + coroutines-test) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.InviteResponseViewModelTest"` (4 tests) | yes (InviteResponseViewModel.kt) | ⬜ pending |
| 16-04-T2 | 16-04-invite-response-sheet-and-viewmodel | 3 | D-01, D-03, D-05, D-07, D-11 (InviteResponseSheet + DeclineConfirmDialog + shouldOpenInviteSheet predicate + 11 stub strings × 2 locales) | Android compile + unit | `./gradlew :app:compileDebugKotlin` AND `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.NotificationCardBranchingTest" --tests "com.giftregistry.LocalizationParityTest"` (6+1 tests) | yes (InviteResponseSheet.kt + 11 stub strings × 2 locales) | ⬜ pending |
| 16-04-T3 | 16-04-invite-response-sheet-and-viewmodel | 3 | D-11 (NotificationsScreen tap-branching + sheet host + AppNavigation onAcceptSuccess wiring) | Android compile + unit (full notifications.* suite) | `./gradlew :app:compileDebugKotlin` AND `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*"` | yes (NotificationsViewModel.kt + NotificationsScreen.kt + AppNavigation.kt) | ⬜ pending |
| 16-05-T1 | 16-05-inbox-reskin-and-strings | 4 | D-28 (locked EN + RO copy: 9 new keys + 11 stub replacements = 20 keys × 2 locales) | Android unit (LocalizationParityTest) + grep | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest"` AND `grep -c` on new keys in both locales | yes (values/strings.xml + values-ro/strings.xml) | ⬜ pending |
| 16-05-T2 | 16-05-inbox-reskin-and-strings | 4 | D-02, D-08, D-09, D-25 (NotificationsScreen re-skin + localizedTitle/Body for 3 new types; D-02/D-08 negative-coverage no Badge/BadgedBox/pendingCount) | Android compile + unit + grep negative-coverage | `./gradlew :app:compileDebugKotlin` AND `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*" --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest" --tests "com.giftregistry.LocalizationParityTest"` AND `! grep -E "Badge\(\|BadgedBox\|pendingCount" app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` (D-02/D-08 verify) | yes (NotificationsScreen.kt rewritten in place) | ⬜ pending |
| 16-05-T3 | 16-05-inbox-reskin-and-strings | 4 | D-09 (StyleGuidePreview offline visual reference) | Android compile | `./gradlew :app:compileDebugKotlin` (preview functions compile clean; preview rendering verified visually in Android Studio @Preview pane) | yes (StyleGuidePreview.kt appended) | ⬜ pending |
| 16-06-T1 | 16-06-deploy-and-uat | 5 | D-14, D-20 (Android App Check provider wiring + D-14 coupling stanza appended to 15-CONTEXT.md) | Gradle assemble + grep | `./gradlew :app:assembleDebug` AND `grep -q "FirebaseAppCheck" GiftRegistryApp.kt` AND `ls .planning/todos/completed/2026-05-22-wire-android-app-check-and-flip-enforcement.md` AND `grep -q "Phase 16 update (appended 2026-05-24)" 15-CONTEXT.md` AND `grep -q "linkInviteOnSignup MUST target" 15-CONTEXT.md` | yes (GiftRegistryApp.kt + 15-CONTEXT.md addendum + todo moved to completed/) | ⬜ pending |
| 16-06-T2 | 16-06-deploy-and-uat | 5 | D-20, D-23 (deploy composite index + 3 Cloud Functions to gift-registry-ro / europe-west3) | Deploy log grep | `grep -E "successfully deployed\|Index already built\|Index built"` on /tmp/16-06-indexes-deploy.log AND `grep -E "acceptInvite\|declineInvite\|inviteToRegistry"` on /tmp/16-06-functions-deploy.log AND `grep -E "acceptInvite\|declineInvite"` on /tmp/16-06-functions-list.log | yes (16-06-DEPLOY-LOG.md captured) | ⬜ pending |
| 16-06-T3 | 16-06-deploy-and-uat | 5 | D-01, D-03, D-05, D-07, D-09 (on-device UAT — 18 scenarios end-to-end) | checkpoint:human-verify (UAT log file existence check) | `test -f 16-06-UAT-RESULTS.md && grep -cE "^[0-9]+\\."` on the file, asserting ≥ 18 numbered scenarios recorded with PASS/FAIL/FLAG | yes (16-06-UAT-RESULTS.md authored by human operator) | ⬜ pending (checkpoint) |

*Status legend: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Sampling continuity check:** 14/14 tasks have automated verify commands. No 3-consecutive-task gap exists. The only checkpoint is 16-06-T3 (UAT) which is explicitly a `checkpoint:human-verify` task per the orchestrator contract; its automated verify confirms the UAT log artifact exists with at least 18 numbered scenarios. All other 13 tasks run scoped Gradle/Jest commands under 90s each.

---

## Wave 0 Requirements

- [x] FCM Android test harness — `app/src/test/.../NotificationRepositoryImplAcceptDeclineTest.kt` with Firestore-mock + httpsCallable mock (Plan 16-01 Task 1)
- [x] Functions test scaffolding — `functions/src/__tests__/acceptInvite.test.ts`, `declineInvite.test.ts` mirroring `confirmPurchase.test.ts` shape (Plan 16-01 Task 2)
- [x] Firestore rules test — `firestore.rules.test.ts` cases for `pendingInvitedUsers` read scope (D-18) + invitedUsers post-promote (D-19) (Plan 16-01 Task 2)
- [x] Composite index for inbox cleanup query — `firestore.indexes.json` adds `(type asc, payload.registryId asc)` on `users/*/notifications` (Pitfall 7) (Plan 16-01 Task 2)
- [x] Hilt verification — confirmed `AppModule.kt` already exposes `FirebaseFunctions` singleton (Open Question 1 resolved during plan-phase; Plan 16-03 reuses without new module)
- [x] Localization parity test — asserts `strings.xml` ↔ `values-ro/strings.xml` keys match (Plan 16-01 Task 1)
- [ ] Android App Check provider wiring — coordinated via Plan 16-06 Task 1 (closes folded todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md`)

*If any item lands "as planned without a Wave 0 task", it must be referenced from `read_first` of the dependent task — verified during plan authoring.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Foreground FCM banner + notification channel rendering | D-01, D-05 | OS-level UI + animation; Compose UI tests for system notifications are not reliable | Push test message via Firebase console; verify channel name/importance and tap-through (Plan 16-06 Task 3 UAT scenario A3) |
| ModalBottomSheet animation polish on accept/decline | D-09 | Compose UI tests for sheet enter/exit animations are flaky | Run on physical Pixel device; verify dismissal blocked while `isLoading == true` (Plan 16-06 Task 3 UAT scenario A6) |
| InviteResponseSheet @Preview render (StyleGuidePreview) | D-09 | ModalBottomSheet doesn't render statically in @Preview without a complex test harness | Plan 16-05 Task 3 documents this deferral; covered by Plan 16-06 Task 3 UAT scenarios A4–A7 |
| App Check enforcement in production | D-20 | Requires real Play Integrity token | Deploy to internal track, observe callable success vs. unauthenticated (Plan 16-06 Task 2 deploy + Task 3 UAT scenario A6/A7) |
| End-to-end accept/decline + locale parity + legacy fallback + re-invite of already-member | D-01..D-28 (all) | Multi-device + multi-account flow; locale switch is a system setting | Plan 16-06 Task 3: 18 UAT scenarios (A1–A10 accept; B1–B6 decline; C1–C2 legacy + re-invite; D1–D2 visual + locale) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (14/14 tasks mapped above)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (6/7 items shipped via Plan 16-01; App Check item shipped via Plan 16-06 T1)
- [x] No watch-mode flags
- [x] Feedback latency < 90s (Gradle scoped test ~45s; Jest scoped test ~10s; tsc ~5s)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready (populated from plan tasks 2026-05-24)
