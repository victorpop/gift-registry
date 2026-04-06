---
phase: 3
slug: registry-item-management
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-06
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + MockK 1.13.17 + Turbine 1.2.0 + kotlinx-coroutines-test 1.9.0 |
| **Config file** | app/build.gradle.kts (testImplementation already present) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest && cd tests/rules && npm test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.*"`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest && cd tests/rules && npm test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | REG-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.CreateRegistryUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-01-02 | 01 | 1 | REG-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.UpdateRegistryUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-01-03 | 01 | 1 | REG-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*.DeleteRegistryUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-01-04 | 01 | 1 | REG-04 | unit | `./gradlew :app:testDebugUnitTest --tests "*.RegistryRepositoryTest"` | Wave 0 | ⬜ pending |
| 03-01-05 | 01 | 1 | REG-10 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ObserveRegistriesUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-02-01 | 02 | 1 | ITEM-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.AddItemUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-02-02 | 02 | 1 | ITEM-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.FetchOgMetadataUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-02-03 | 02 | 1 | ITEM-05 | unit | `./gradlew :app:testDebugUnitTest --tests "*.UpdateItemUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-02-04 | 02 | 1 | ITEM-06 | unit | `./gradlew :app:testDebugUnitTest --tests "*.DeleteItemUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-02-05 | 02 | 1 | ITEM-07 | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests "*.ObserveItemsUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-02-06 | 02 | 1 | AFF-01,02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.AffiliateUrlTransformerTest"` | Wave 0 | ⬜ pending |
| 03-02-07 | 02 | 1 | AFF-03 | unit | included in AddItemUseCaseTest | Wave 0 | ⬜ pending |
| 03-02-08 | 02 | 1 | AFF-04 | unit | `./gradlew :app:testDebugUnitTest --tests "*.AffiliateUrlTransformerTest#unknownMerchantPassesThrough"` | Wave 0 | ⬜ pending |
| 03-03-01 | 03 | 2 | REG-05,06,07 | unit | `./gradlew :app:testDebugUnitTest --tests "*.InviteToRegistryUseCaseTest"` | Wave 0 | ⬜ pending |
| 03-03-02 | 03 | 2 | Firestore rules | integration | `cd tests/rules && npm test` | Wave 0 | ⬜ pending |
| 03-03-03 | 03 | 2 | REG-09 | unit | `./gradlew :app:testDebugUnitTest --tests "*.NotificationPreferenceTest"` | Wave 0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/giftregistry/data/registry/FakeRegistryRepository.kt` — fake for unit tests
- [ ] `app/src/test/java/com/giftregistry/data/item/FakeItemRepository.kt` — fake for unit tests
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/CreateRegistryUseCaseTest.kt`
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/ObserveItemsUseCaseTest.kt` — uses Turbine
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCaseTest.kt` — mock FirebaseFunctions
- [ ] `app/src/test/java/com/giftregistry/util/AffiliateUrlTransformerTest.kt` — pure Kotlin, no mocking needed
- [ ] `tests/rules/firestore.rules.test.ts` — add 3 new describe blocks for invite flow

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Deep link to private registry shows auth options | REG-08 | Requires Android deep link intent + full navigation stack | Navigate to private registry link while logged out; verify login/signup/guest options shown |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
