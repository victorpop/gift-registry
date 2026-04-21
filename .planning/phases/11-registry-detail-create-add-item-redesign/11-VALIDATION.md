---
phase: 11
slug: registry-detail-create-add-item-redesign
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-21
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + MockK + Turbine (unit tests under `app/src/test/`) |
| **Config file** | none — JUnit 4 auto-detected by AGP |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.*" --tests "com.giftregistry.ui.registry.create.*" --tests "com.giftregistry.ui.item.add.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~60 seconds (unit suite) |

---

## Sampling Rate

- **After every task commit:** Phase 11 quick run command
- **After every plan wave:** `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full unit suite green; visual verification of 3 screens via StyleGuidePreview
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | TBD | TBD | TBD | TBD | ⬜ pending |

*Populated by gsd-planner from 11-RESEARCH.md. Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

RED stub unit test files (pure-Kotlin JUnit 4, covering SCR-08/09/10 contract):
- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/FilterChipStateTest.kt` — filter enum (All/Open/Reserved/Completed) + predicate mapping AVAILABLE→Open, RESERVED→Reserved, PURCHASED→Completed
- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/HeroToolbarAlphaTest.kt` — pinned-toolbar alpha fade calc; critical pitfall guard: `firstVisibleItemIndex >= 1 → 1f`
- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/RegistryStatsTest.kt` — `registryStatsOf(items)` deriving items/reserved/given counts; `viewCount` placeholder 0
- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` — `shareUrlOf(registryId): String` format `https://gift-registry-ro.web.app/r/{id}`
- [ ] `app/src/test/java/com/giftregistry/ui/registry/create/OccasionCatalogTest.kt` — 6 occasions (Housewarming/Wedding/Baby/Birthday/Christmas/Custom) with glyph + label + storage key; legacy alias map ("Baby shower" → Baby, "Anniversary" → Custom, etc.)
- [ ] `app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt` — AddItemMode enum (PasteUrl/BrowseStores/Manual) + default = PasteUrl; rememberSaveable contract
- [ ] `app/src/test/java/com/giftregistry/ui/item/add/AffiliateRowVisibilityTest.kt` — `shouldShowAffiliateRow(url: String?, isAffiliateDomain: Boolean, ogFetchSucceeded: Boolean): Boolean` — both conditions must be true

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Hero gradient overlay renders correctly | SCR-08 | 3-stop Brush.verticalGradient with alpha values — pixel-level check | Open RegistryDetail on device; confirm bottom of hero fades to ink @ 0.67 for text legibility |
| Parallax alpha fade on scroll | SCR-08 | Scroll behavior timing cannot be screenshot-diffed | Scroll the item list; confirm pinned toolbar fades from transparent to opaque, stays opaque once hero scrolls off |
| Filter chips horizontal scroll + active state | SCR-08 | LazyRow scroll interaction | Tap each filter chip; confirm ink-filled active state with paper text + count |
| Share banner tap opens share sheet | SCR-08 | System share sheet requires real launch | Tap share banner; confirm Android share sheet opens with registry URL |
| Occasion tile grid selected/unselected states | SCR-09 | Visual pixel check on borders + colors | Tap each tile; confirm accent bg + accent border on selected, paperDeep + line border on unselected |
| Pulsing dot on URL field at 1000 ms cadence | SCR-10 | Animation timing | Paste URL; observe "⌕ Fetching from {domain}" with pulsing dot at ≈1 s full cycle |
| Affiliate confirmation row visibility rule | SCR-10 | Conditional render on real network | Paste emag.ro URL → row shows; paste unknown URL → row hidden |
| Preview card with OG image | SCR-10 | Real image load via Coil | Paste URL; confirm 80×80 thumbnail + title/price/source line populate |
| RO locale for 35 new string keys | I18N-01 | Device locale switch | Switch to Romanian; verify Phase 11 strings render correctly |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (7 RED stub test files listed above)
- [ ] No watch-mode flags
- [ ] Feedback latency < 60 s
- [ ] `nyquist_compliant: true` set in frontmatter after Wave 0 lands

**Approval:** pending
