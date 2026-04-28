---
phase: 12
slug: registry-cover-photo-themed-placeholder
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-27
completed: 2026-04-28
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + mockk + turbine + kotlinx-coroutines-test (Android unit tests, JVM) |
| **Config file** | `app/build.gradle.kts` (existing test source set) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests '*Phase12*'` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~30s for filtered Phase 12 subset; ~90s for full app unit suite |

---

## Sampling Rate

- **After every task commit:** Run quick filtered command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green + on-device UAT pass
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

> Populated from Plans 12-01..12-05 SUMMARY.md task lists. Every task has either an automated test command or an explicit Wave 0 dependency.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-01-T1 | 12-01 | 0 | D-02, D-05, D-11, D-12, D-14, D-16 | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.registry.cover.*Test'` | ✅ | ✅ |
| 12-01-T2 | 12-01 | 0 | D-04, D-05, D-06, D-07 (Pitfall 1+2) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.data.storage.*Test' --tests 'com.giftregistry.data.registry.RegistryRepositoryImplCoverTest' --tests 'com.giftregistry.ui.registry.create.CreateRegistryViewModelCoverTest'` | ✅ | ✅ (8 GREEN, 12 RED→GREEN by Plans 02-04, 1 @Ignored) |
| 12-01-T3 | 12-01 | 0 | tooling (firebase-storage 22.0.1 + Storage Emulator port 9199) | manual | `./gradlew :app:dependencies | grep firebase-storage` + `cat firebase.json | jq '.emulators.storage.port'` | ✅ | ✅ |
| 12-02-T1 | 12-02 | 1 | Pitfall 1 (RegistryDto.imageUrl + toMap/toUpdateMap) + D-07 (newRegistryId) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.data.registry.RegistryRepositoryImplCoverTest'` | ✅ | ✅ (5/5 GREEN) |
| 12-02-T2 | 12-02 | 1 | D-02, D-05 (PresetCatalog real lookup + ResolveImageModel 3-branch + 36 placeholder JPEGs) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.registry.cover.PresetCatalogTest' --tests 'com.giftregistry.ui.registry.cover.ResolveImageModelTest'` | ✅ | ✅ (13/13 GREEN) |
| 12-02-T3 | 12-02 | 1 | D-04, D-06, D-07 (Storage end-to-end: DataSource + Repo + Processor + Hilt) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.data.storage.StorageDataSourceTest' --tests 'com.giftregistry.data.storage.StorageRepositoryImplTest'` | ✅ | ✅ (6/6 GREEN; CoverImageProcessorTest 🟦 @Ignored) |
| 12-02-T4 | 12-02 | 1 | D-08 (storage.rules cross-service, firebase.json wiring) | manual | `node -e 'JSON.parse(require("fs").readFileSync("firebase.json","utf8"))'` + Firebase Console Rules Simulator | ✅ | 🟦 deployed-deferred (file authored + wired; deploy in follow-up todo) |
| 12-03-T1 | 12-03 | 1 | D-12, D-14, D-16 (HeroImageOrPlaceholder shared composable + isCoverPickerEnabled real predicate) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.registry.cover.CoverPhotoPickerEnabledTest'` | ✅ | ✅ (4/4 GREEN) |
| 12-03-T2 | 12-03 | 1 | D-15 (RegistryCard primary + secondary consume HeroImageOrPlaceholder; visible bug fix) | manual | StyleGuidePreview "RegistryCard placeholder — Primary (Wedding) + Secondary (Baby)" + on-device | ✅ | 🟦 (UAT PASSED) |
| 12-03-T3 | 12-03 | 1 | D-09, D-10, D-11 (CoverPhotoPickerInline + CoverPhotoPickerSheet + PresetThumbnail) | manual | StyleGuidePreview "CoverPhotoPickerInline — 3 states" + "CoverPhotoPickerSheet (Wedding)" + on-device | ✅ | 🟦 (UAT PASSED) |
| 12-04-T1 | 12-04 | 2 | D-07 + Pitfall 2 (CreateRegistryViewModel upload-BEFORE-write + D-11 occasion clears preset) | unit | `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.registry.create.CreateRegistryViewModelCoverTest'` | ✅ | ✅ (5/5 GREEN — coVerifyOrder pinned) |
| 12-04-T2 | 12-04 | 2 | D-09, D-12 (CoverPhotoPickerInline above OccasionTileGrid; sheet host; 10 EN+RO strings) | manual | `grep -c '<string name="cover_photo_'` on values + values-ro strings.xml (= 10 each) + on-device RO locale verification | ✅ | ✅ + 🟦 (counts GREEN; on-device UAT PASSED) |
| 12-04-T3 | 12-04 | 2 | D-13 (RegistryDetailScreen owner-only tap target on hero) | manual | On-device — owner sees ripple, guest does not | ✅ | 🟦 (UAT PASSED) |
| 12-05-T1 | 12-05 | 3 | All Phase 12 composables visually reviewable (D-09/D-10/D-13/D-14/D-15/D-16) | manual | `grep -c '@Preview' StyleGuidePreview.kt` ≥ 27 (was 23 before this plan) + Android Studio Preview pane | ✅ | ✅ + 🟦 (count = 27; UAT PASSED) |
| 12-05-T2 | 12-05 | 3 | All manual UAT (12-check checklist) + storage.rules deploy | manual | Visual UAT (Studio Preview) + on-device UAT (Emulator) + `firebase deploy --only storage` | partial | 🟦 (visual + on-device UAT PASSED; storage deploy DEFERRED — see todo) |
| 12-05-T3 | 12-05 | 3 | Phase 12 traceability (per-task map + Wave 0 + Manual-Only + Sign-Off) | docs | placeholder-token grep returns 0 in this file + `grep -c 'nyquist_compliant: true\|wave_0_complete: true\|status: approved'` returns 3 | ✅ | ✅ |

*Status: ⬜ pending · ✅ green (automated) · ❌ red · ⚠️ flaky · 🟦 manual / UAT-driven*

---

## Wave 0 Requirements

> Failing-test stubs (RED) created in Wave 0 — Wave 1 / Wave 2 implementations flipped them GREEN. Pattern proven in Phases 8–11. Filenames below match Plan 12-01's authoritative `<files>` list (8 RED test files total).

- [x] `app/src/test/java/com/giftregistry/ui/registry/cover/PresetCatalogTest.kt` — `preset:Wedding:3` round-trip resolves to a valid `R.drawable.preset_*` ID; unknown sentinel returns null (D-02, D-05) — **GREEN** (Plan 12-02)
- [x] `app/src/test/java/com/giftregistry/ui/registry/cover/ResolveImageModelTest.kt` — null/URL/preset-sentinel routing for Coil 3 model parameter (D-05, D-16) — **GREEN** (Plan 12-02)
- [x] `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoSelectionTest.kt` — sealed-interface equality, default state is `None`, `Gallery(uri)` carries the Uri (D-11 supports) — **GREEN** (Plan 12-01 final-shape)
- [x] `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerEnabledTest.kt` — picker disabled when `occasion == null` / blank / whitespace (D-12) — **GREEN** (Plan 12-03)
- [x] `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelCoverTest.kt` — `onSave()` uploads BEFORE writing the registry document (D-07 + Pitfall 2 strict ordering via `coVerifyOrder { uploadCover; createRegistryUseCase }`); gallery upload failure surfaces in `error` flow WITHOUT emitting `savedRegistryId`; preset selection skips upload and encodes as `preset:Occasion:index`; occasion change clears preset selection (D-11) — **GREEN** (Plan 12-04)
- [x] `app/src/test/java/com/giftregistry/data/registry/RegistryRepositoryImplCoverTest.kt` — RED tests for Pitfall 1 (RegistryDto + toMap/toUpdateMap roundtrip imageUrl) plus `newRegistryId_returnsNonBlankString` (D-07 enabler — Plan 12-02 Task 1 plumbs the helper for upload-then-write) — **GREEN** (Plan 12-02)
- [x] `app/src/test/java/com/giftregistry/data/storage/StorageRepositoryImplTest.kt` — resize → upload → download URL roundtrip with mocked Firebase Storage; happy path + failure path (D-04, D-05, D-07) — **GREEN** (Plan 12-02; D-05 path schema moved to `StorageDataSourceTest`)
- [x] `app/src/test/java/com/giftregistry/data/storage/CoverImageProcessorTest.kt` — decodeAndCompress size invariant (D-06); `@Ignored` on Wave 0 because Robolectric is not on `testRuntimeClasspath`; instrumented variant deferred — **@Ignored** (interface contract locked; runtime D-06 invariant deferred)

**Tally:** 7 of 8 RED suites flipped to GREEN; 1 (`CoverImageProcessorTest`) remains `@Ignored` at the class level by Plan 12-01 decision.

---

## Manual-Only Verifications

| # | Behavior | Requirement | Why Manual | Test Instructions | Status (2026-04-28) |
|---|----------|-------------|------------|-------------------|---------------------|
| 1 | Bottom-sheet visual matches GiftMaison tokens | D-09, D-10 | Pixel-level UI; preview in StyleGuidePreview + on-device | Open StyleGuidePreview "CoverPhotoPickerSheet (Wedding)" section; verify 14 dp tile radius, accent border on selected, mono-caps "Pick from gallery" pill | ✅ PASSED — 12-05-T1 visual UAT (Studio Preview) + 12-05-T2 on-device UAT |
| 2 | Hero gradient placeholder pixel parity | D-14, D-16 | Pre-existing visual contract from Phase 11 must not regress | Open RegistryDetailScreen for a registry with `imageUrl == null`; compare to Phase 11 baseline screenshot | ✅ PASSED — 12-05-T2 on-device UAT (40 sp glyph preserved on 180 dp hero) |
| 3 | Card placeholder on home grid | D-15 | Visible bug fix — must look correct in both card variants | Open Home with one Primary + one Secondary card both with `imageUrl == null`; verify gradient fills 16:9 image area only, not the full card | ✅ PASSED — 12-05-T1 visual UAT + 12-05-T2 on-device UAT (the user-reported visible bug from the Home screenshot is RESOLVED) |
| 4 | Photo Picker → Storage upload roundtrip | D-04, D-05, D-07 | Requires real Photo Picker UI + Storage emulator | Run app against Emulator Suite; create registry, pick from gallery, verify file lands at `/users/{uid}/registries/{registryId}/cover.jpg` and `Registry.imageUrl` reflects the download URL | ✅ PASSED — 12-05-T2 on-device UAT against Storage Emulator (port 9199) |
| 5 | Owner-only tap-to-change on Detail hero | D-13 | UI gate; security boundary is in storage.rules | Open registry as owner — tap should open sheet; open same registry as guest — no tap affordance | ✅ PASSED — 12-05-T2 on-device UAT (owner sees ripple, guest does not; `clickable(enabled = onCoverTap != null)` no-op verified) |
| 6 | Storage rules deny non-owner write | D-08 | Cross-service rule (storage → firestore) — deployed environment OR Firebase emulator required | Firebase Console Storage Rules Simulator with auth.uid set to non-owner, request type "create": expect DENY. Or run via deployed bucket once `firebase deploy --only storage` lands. | 🟦 DEFERRED — `firebase deploy --only storage` skipped at user's request (`approved — storage deploy skipped`). Follow-up todo: `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md`. Local `storage.rules` content is correct (cross-service `firestore.get(...)` mirroring `canReadRegistry`); the live bucket is currently default-deny until the deploy lands. |
| 7 | Romanian locale strings | I18N-02 | Localization | Switch device to Romanian; verify all `cover_photo_*` keys translate (no fallback to English) | ✅ PASSED — 12-05-T2 on-device UAT in Română locale (10 keys × 2 locales × 1 visible pass) |

**Tally:** 6 of 7 PASSED; 1 DEFERRED (storage rules live-bucket deny test, gated on the deploy follow-up todo).

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (see Per-Task Verification Map above — every task has automated coverage OR an explicit manual UAT entry tied to a Decision ID)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (interleaved unit-test coverage across all 5 plans confirms)
- [x] Wave 0 covers all MISSING references (8 RED files shipped in Plan 12-01; 7 flipped GREEN, 1 @Ignored with documented reason)
- [x] No watch-mode flags (`./gradlew testDebugUnitTest` is a one-shot run; no `-t` / `--continuous`)
- [x] Feedback latency < 90s (Phase 12 filtered subset ~30s; full app suite ~90s)
- [x] `nyquist_compliant: true` set in frontmatter
- [x] On-device UAT (12-check checklist from Plan 12-05 Task 2) PASSED via user resume-signal `approved — storage deploy skipped`
- [ ] `firebase deploy --only storage --project gift-registry-ro` — **DEFERRED** to `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` (user-approved deferral; production traffic blocked until the todo lands)

**Approval:** approved 2026-04-28 (storage rules deploy deferred — see todo) via `/gsd:execute-phase` Wave 3 human checkpoint.
