---
phase: 2
slug: android-core-auth
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-05
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Kotlin Coroutines Test + Hilt Testing |
| **Config file** | `app/build.gradle.kts` (test dependencies) |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "*.auth.*"` |
| **Full suite command** | `./gradlew testDebugUnitTest` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest --tests "*.auth.*"`
- **After every plan wave:** Run `./gradlew testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | 01 | 1 | AUTH-01 | unit | `./gradlew testDebugUnitTest --tests "*.SignUpViewModelTest"` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | AUTH-02 | unit | `./gradlew testDebugUnitTest --tests "*.LoginViewModelTest"` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | AUTH-03 | unit | `./gradlew testDebugUnitTest --tests "*.GoogleSignInTest"` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | AUTH-04 | unit | `./gradlew testDebugUnitTest --tests "*.SessionPersistenceTest"` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | AUTH-05 | unit | `./gradlew testDebugUnitTest --tests "*.GuestAccessTest"` | ❌ W0 | ⬜ pending |
| TBD | 01 | 1 | AUTH-06 | unit | `./gradlew testDebugUnitTest --tests "*.GuestConversionTest"` | ❌ W0 | ⬜ pending |
| TBD | 02 | 1 | I18N-01 | unit | `./gradlew testDebugUnitTest --tests "*.LocaleTest"` | ❌ W0 | ⬜ pending |
| TBD | 02 | 1 | I18N-03 | unit | `./gradlew testDebugUnitTest --tests "*.LocaleOverrideTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Test framework setup in `app/build.gradle.kts` — JUnit 5, Coroutines Test, Hilt Testing, Turbine for Flow testing
- [ ] `app/src/test/java/.../auth/` — test directory structure
- [ ] Fake `AuthRepository` implementation for ViewModel unit tests

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Google OAuth popup flow | AUTH-03 | Requires real Google Play Services / Credential Manager UI | 1. Launch app on emulator with Google account 2. Tap "Sign in with Google" 3. Verify credential picker appears 4. Select account 5. Verify redirect back to app with logged-in state |
| Language auto-detection from device locale | I18N-03 | Requires device locale change in emulator settings | 1. Set emulator locale to Romanian 2. Launch app 3. Verify UI displays Romanian strings 4. Change to English 5. Verify UI switches |
| Guest access without account | AUTH-05 | Requires manual flow through UI | 1. Launch app 2. Tap "Continue as Guest" 3. Verify guest can browse without providing info upfront |

*If none: "All phase behaviors have automated verification."*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
