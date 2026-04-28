---
phase: 12
slug: registry-cover-photo-themed-placeholder
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-27
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

> To be filled by gsd-planner from PLAN.md task list. Every task needs either an automated test command OR an explicit Wave 0 dependency.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD     | TBD  | 0    | D-XX        | unit      | TBD               | ❌ W0       | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

> Failing-test stubs (RED) created in Wave 0 — Wave 1 / Wave 2 implementations flip them GREEN. Pattern proven in Phases 8–11. Filenames below match Plan 12-01's authoritative `<files>` list (8 RED test files total).

- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/PresetCatalogTest.kt` — `preset:Wedding:3` round-trip resolves to a valid `R.drawable.preset_*` ID; unknown sentinel returns null (D-02, D-05)
- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/ResolveImageModelTest.kt` — null/URL/preset-sentinel routing for Coil 3 model parameter (D-05, D-16)
- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoSelectionTest.kt` — sealed-interface equality, default state is `None`, `Gallery(uri)` carries the Uri (D-11 supports)
- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerEnabledTest.kt` — picker disabled when `occasion == null` / blank / whitespace (D-12)
- [ ] `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelCoverTest.kt` — `onSave()` uploads BEFORE writing the registry document (D-07 + Pitfall 2 strict ordering via `coVerifyOrder { uploadCover; createRegistryUseCase }`); gallery upload failure surfaces in `error` flow WITHOUT emitting `savedRegistryId`; preset selection skips upload and encodes as `preset:Occasion:index`; occasion change clears preset selection (D-11)
- [ ] `app/src/test/java/com/giftregistry/data/registry/RegistryRepositoryImplCoverTest.kt` — RED tests for Pitfall 1 (RegistryDto + toMap/toUpdateMap roundtrip imageUrl) plus `newRegistryId_returnsNonBlankString` (D-07 enabler — Plan 12-02 Task 1 plumbs the helper for upload-then-write)
- [ ] `app/src/test/java/com/giftregistry/data/storage/StorageRepositoryImplTest.kt` — resize → upload → download URL roundtrip with mocked Firebase Storage; happy path + failure path (D-04, D-05, D-07)
- [ ] `app/src/test/java/com/giftregistry/data/storage/CoverImageProcessorTest.kt` — decodeAndCompress size invariant (D-06); `@Ignored` on Wave 0 if Robolectric is absent from the toolchain (Plan 12-02 decides; instrumented variant deferred to Plan 05 follow-up)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Bottom-sheet visual matches GiftMaison tokens | D-09, D-10 | Pixel-level UI; preview in StyleGuidePreview + on-device | Open StyleGuidePreview "CoverPhotoPickerSheet (Wedding)" section; verify 14 dp tile radius, accent border on selected, mono-caps "Pick from gallery" pill |
| Hero gradient placeholder pixel parity | D-14, D-16 | Pre-existing visual contract from Phase 11 must not regress | Open RegistryDetailScreen for a registry with `imageUrl == null`; compare to Phase 11 baseline screenshot |
| Card placeholder on home grid | D-15 | Visible bug fix — must look correct in both card variants | Open Home with one Primary + one Secondary card both with `imageUrl == null`; verify gradient fills 16:9 image area only, not the full card |
| Photo Picker → Storage upload roundtrip | D-04, D-05, D-07 | Requires real Photo Picker UI + Storage emulator | Run app against Emulator Suite; create registry, pick from gallery, verify file lands at `/users/{uid}/registries/{registryId}/cover.jpg` and `Registry.imageUrl` reflects the download URL |
| Owner-only tap-to-change on Detail hero | D-13 | UI gate; security boundary is in storage.rules | Open registry as owner — tap should open sheet; open same registry as guest — no tap affordance |
| Storage rules deny non-owner write | D-08 | Cross-service rule (storage → firestore) — Firebase emulator required | Run `firebase emulators:exec --only storage,firestore "./scripts/storage-rules-smoke.sh"` (script TBD in plan); non-owner write must reject |
| Romanian locale strings | I18N-02 | Localization | Switch device to Romanian; verify all `cover_photo_*` keys translate (no fallback to English) |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
