---
phase: 4
slug: reservation-system
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-11
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (rules)** | Jest 29 + `@firebase/rules-unit-testing` 5.0 (TypeScript) |
| **Framework (Android)** | JUnit 4 + MockK 1.13.17 + Turbine 1.2.0 + kotlinx-coroutines-test 1.9.0 |
| **Framework (functions)** | Jest + direct HTTP POST to emulator endpoints |
| **Quick run (rules)** | `cd tests/rules && npm test -- --forceExit` |
| **Full suite** | `firebase emulators:exec --only firestore,auth,functions "cd tests/rules && npm test && ./gradlew :app:testDebugUnitTest"` |
| **Android quick** | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.*"` |
| **Estimated runtime** | ~45 seconds |

---

## Sampling Rate

- **After every task commit:** `./gradlew :app:testDebugUnitTest` (Android) OR `cd tests/rules && npm test` (rules) OR `cd functions && npm run build` (Cloud Functions TS compile)
- **After every wave merge:** Full suite
- **Before `/gsd:verify-work`:** Full suite must be green + emulator smoke test of createReservation → releaseReservation flow
- **Max feedback latency:** 45 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | RES-09 | security rule | `cd tests/rules && npm test -- -t "reservations write deny"` | Wave 0 | ⬜ pending |
| 04-01-02 | 01 | 1 | RES-02 | security rule | `cd tests/rules && npm test -- -t "item status read"` | Wave 0 | ⬜ pending |
| 04-02-01 | 02 | 1 | RES-01, RES-09 | integration | `firebase emulators:exec --only firestore,functions "curl -X POST .../createReservation"` | Wave 0 | ⬜ pending |
| 04-02-02 | 02 | 1 | RES-03, RES-05 | integration | `firebase emulators:exec "curl -X POST .../releaseReservation"` | Wave 0 | ⬜ pending |
| 04-02-03 | 02 | 1 | RES-07 (stub) | unit | `cd functions && npm test -- -t "expiry email stub logs"` | Wave 0 | ⬜ pending |
| 04-03-01 | 03 | 2 | D-13 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ReserveItemUseCaseTest"` | Wave 0 | ⬜ pending |
| 04-03-02 | 03 | 2 | D-16 | unit | `./gradlew :app:testDebugUnitTest --tests "*.GuestPreferencesDataStoreTest"` | Wave 0 | ⬜ pending |
| 04-03-03 | 03 | 2 | RES-02, RES-06 | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests "*.ObserveReservationsTest"` | Wave 0 | ⬜ pending |
| 04-04-01 | 04 | 3 | RES-01, RES-04 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ReserveItemViewModelTest"` | Wave 0 | ⬜ pending |
| 04-04-02 | 04 | 3 | RES-08 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ReReserveFlowTest"` | Wave 0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `functions/package.json` — Add `@google-cloud/tasks` dependency
- [ ] `functions/src/reservation/createReservation.ts` — New file stub with failing test
- [ ] `functions/src/reservation/releaseReservation.ts` — New file stub with failing test
- [ ] `tests/rules/firestore.rules.test.ts` — Add tests for RES-02/RES-06 item status read + RES-09 reservation write deny
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt` — Failing stub
- [ ] `app/src/test/java/com/giftregistry/data/preferences/GuestPreferencesDataStoreTest.kt` — Failing stub

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Guest identity persists across process death | D-16 | Requires Android instrumented test or manual device testing | Enable "Don't keep activities" in Developer Options, reserve an item, reopen app, verify reservation still visible |
| Countdown timer display on reserved item card | D-18 | Visual verification | Reserve an item, observe countdown decreasing in real time on detail screen |
| Affiliate URL intent launches retailer after successful reservation | RES-04 | Requires physical device with browser | Reserve item, verify browser opens to retailer URL |
| Re-reserve deep link from email | RES-08 | Email stubbed in Phase 4 — manual URL test | Paste re-reserve URL into browser, verify auth + reservation flow |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 45s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
