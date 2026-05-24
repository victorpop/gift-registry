---
phase: 16
slug: android-notifications-inbox-invite-accept-decline
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-24
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

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | D-01..D-28 | TBD | TBD | TBD | ⬜ pending |

*Populated by planner — every plan task must have an `<automated>` verify command or a Wave 0 dependency.*

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] FCM Android test harness — `app/src/test/.../NotificationRepositoryTest.kt` with Firestore emulator binding
- [ ] Functions test scaffolding — `functions/src/__tests__/acceptInvite.test.ts`, `declineInvite.test.ts` mirroring `confirmPurchase.test.ts` shape
- [ ] Firestore rules test — `firestore.rules.test.ts` cases for `users/{uid}/notifications` access (only owner can read/update)
- [ ] Composite index for inbox cleanup query — `firestore.indexes.json` adds `(type asc, payload.registryId asc)` on `users/*/notifications` (Pitfall 7)
- [ ] Hilt verification — confirm `FirebaseModule` exposes `FirebaseFunctions` to the notifications package (Open Question 1)
- [ ] Localization parity test — assert `strings.xml` ↔ `values-ro/strings.xml` keys match (Open Question 2)
- [ ] Android App Check provider wiring — coordinate with pending todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md` so `enforceAppCheck: true` callables don't 401 from device

*If any item lands "as planned without a Wave 0 task", it must be referenced from `read_first` of the dependent task.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Foreground FCM banner + notification channel rendering | D-01, D-05 | OS-level UI + animation; Compose UI tests for system notifications are not reliable | Push test message via Firebase console; verify channel name/importance and tap-through |
| ModalBottomSheet animation polish on accept/decline | D-09 | Compose UI tests for sheet enter/exit animations are flaky | Run on physical Pixel device; verify dismissal blocked while `isLoading == true` |
| App Check enforcement in production | D-20 | Requires real Play Integrity token | Deploy to internal track, observe callable success vs. unauthenticated |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
