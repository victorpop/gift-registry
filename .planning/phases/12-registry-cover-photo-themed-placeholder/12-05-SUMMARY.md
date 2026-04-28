---
phase: 12-registry-cover-photo-themed-placeholder
plan: 05
subsystem: testing
tags: [stylepreview, uat, validation, traceability, storage-rules, cover-photo, kotlin, compose]

# Dependency graph
requires:
  - phase: 12-04 (this phase)
    provides: "End-to-end wired Create + Detail cover-photo surfaces (CoverPhotoPickerInline above OccasionTileGrid; ModalBottomSheet host; owner-only tap on RegistryDetailHero); 10 cover_photo_* strings × 2 locales; CreateRegistryViewModel.onSave upload-BEFORE-write contract"
  - phase: 12-03 (this phase)
    provides: "HeroImageOrPlaceholder + CoverPhotoPickerInline + CoverPhotoPickerSheet + PresetThumbnail composables — visually previewable in Studio"
  - phase: 12-02 (this phase)
    provides: "storage.rules + firebase.json wiring (authored 2026-04-28, commit e979e45) — deploy is the human checkpoint this plan owns"
  - phase: 12-01 (this phase)
    provides: "Wave 0 RED scaffolding — 8 test files locking the executable contract; this plan signs off the trace"
  - phase: 11-registry-detail-create-add-item-redesign
    provides: "StyleGuidePreview file precedent (Phase 11 added 7 sections; Phase 12 adds 4)"

provides:
  - "4 new StyleGuidePreview @Preview sections covering all 4 Phase 12 visual contracts (HeroImageOrPlaceholder hero+card variants, CoverPhotoPickerInline 3 states, CoverPhotoPickerSheet body inline-rendered, RegistryCard placeholder Primary+Secondary)"
  - "Visible-bug regression check now reviewable in Android Studio Preview pane (the user-reported `imageUrl == null` empty-card bug is reproducible AND fixed in the same Preview)"
  - "Phase 12 VALIDATION.md fully populated and signed off (status=approved, nyquist_compliant=true, wave_0_complete=true)"
  - "Per-Task Verification Map with 15 rows spanning Plans 12-01..12-05 (mechanical V&V trace from Decision IDs to passing tests + UAT)"
  - "Manual-Only Verifications table updated with 6 PASSED + 1 DEFERRED (storage rules live-bucket deny test)"
  - "Follow-up todo for deferred `firebase deploy --only storage` (deploy command + cross-service permissions prompt + acceptance checklist)"

affects:
  - "v1.1 GA: Phase 12 is GA-ready except for the storage rules deploy (gates production cover-photo upload traffic until the follow-up todo lands)"
  - "/gsd:verify-work 12: full Wave 0 + Wave 1 + Wave 2 + Wave 3 trace is in place; verifier can mechanically confirm decision-ID coverage from VALIDATION.md"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "StyleGuidePreview Wave 3 pattern (Phase 11 → Phase 12): each new shared composable shipped in Wave 1 gets a corresponding @Preview block in Wave 3 so the visual contract is reviewable in Studio without booting an emulator"
    - "@Preview-friendly bottom-sheet rendering: ModalBottomSheet does NOT render in @Preview, so the sheet body is reproduced inline as a Column with the same children (LazyVerticalGrid + PresetThumbnail + Pick-from-gallery pill + Remove button) so the visual contract is still reviewable"
    - "Visible-bug regression check via @Preview: render the buggy state (imageUrl=null on RegistryCardPrimary + Secondary) directly so any regression of the placeholder-rendering fix becomes visually obvious in the Preview pane"
    - "Deferred-deploy todo pattern: when a human-action checkpoint is partially deferred (visual UAT approved + on-device UAT approved + deploy skipped), capture the deploy step in a structured todo file rather than blocking the plan — keeps the SUMMARY honest about what shipped vs what's pending"

key-files:
  created:
    - ".planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md — follow-up for the deferred `firebase deploy --only storage` step"
    - ".planning/phases/12-registry-cover-photo-themed-placeholder/12-05-SUMMARY.md — this summary"
  modified:
    - "app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt — appended 4 Phase 12 @Preview sections (count went from 23 to 27)"
    - ".planning/phases/12-registry-cover-photo-themed-placeholder/12-VALIDATION.md — status=approved + nyquist_compliant=true + wave_0_complete=true; Per-Task Verification Map populated; Manual-Only Verifications statuses; Sign-Off checklist ticked; Approval line dated 2026-04-28"

key-decisions:
  - "CoverPhotoPickerSheet inline-body @Preview (vs trying to render ModalBottomSheet itself): ModalBottomSheet renders as a stub in Studio's @Preview pane — reproducing the body content directly keeps the visual contract reviewable without an emulator boot"
  - "Storage rules deploy deferred via structured todo (not via `partial — ...` resume signal escalation): the user explicitly said `approved — storage deploy skipped`, so all 12 UAT checks are PASSED and only the deploy step is deferred. The deferred-deploy todo carries the deploy command, cross-service permissions prompt, verification steps, and target timing (before any production cover-photo upload traffic)"
  - "VALIDATION.md `status: approved` (not `complete`) per the original VALIDATION.md schema seen in PLAN's read_first directive — `approved` reflects user sign-off, `complete` is reserved for downstream verification work"
  - "Per-Task Verification Map row count chosen as 15 (1+3+3+3+3+1+1 across Plans 12-01..12-05): one row per task in Plans 12-02..12-05 plus the 3 Plan 12-01 tasks. This exceeds the plan's `at least 14 rows` floor by one"

patterns-established:
  - "StyleGuidePreview placeholder regression check: render the formerly-buggy state directly in @Preview so a future regression of the Phase 12 placeholder fix would be visible without on-device testing"
  - "Deferred-deploy todo: when a human checkpoint splits into auto-approved UAT + manually-deferred deploy, capture the deferred step as a structured todo file with deploy command + verification — keeps the plan/phase complete from a process standpoint while leaving production traffic blocked until the follow-up lands"

requirements-completed:
  - D-08
  - D-09
  - D-10
  - D-13
  - D-14
  - D-15
  - D-16

# Metrics
duration: 12min
completed: 2026-04-28
---

# Phase 12 Plan 05: StyleGuidePreview + UAT Sign-off + Storage Rules Deploy Deferral Summary

**4 new StyleGuidePreview @Preview sections shipped + on-device UAT approved (12-check checklist) + visual UAT approved (Studio Preview) + 12-VALIDATION.md fully populated and signed off; `firebase deploy --only storage` deferred to a structured follow-up todo per the user's `approved — storage deploy skipped` resume signal.**

## Performance

- **Duration:** ~12 min (Task 1 + checkpoint wait + Task 3 + summary)
- **Started:** 2026-04-28T15:08:00Z (Task 1 commit `33a447b` at 15:10:05)
- **Completed:** 2026-04-28T15:20:00Z (this summary)
- **Tasks:** 3
- **Files modified:** 2 (StyleGuidePreview.kt, 12-VALIDATION.md)
- **Files created:** 2 (deferred-deploy todo, this SUMMARY)

## Accomplishments

- **4 StyleGuidePreview @Preview sections shipped** appended to `StyleGuidePreview.kt`. The total `@Preview` count went from 23 to 27 (delta exactly +4, matching the plan's acceptance criterion). All 4 sections render in Android Studio's Preview pane without errors.
- **Visible-bug regression check is now reviewable in @Preview** — `RegistryCard placeholder — Primary (Wedding) + Secondary (Baby)` renders both card variants with `imageUrl = null`, exposing the originally-reported bug AND its fix in the same Preview block. Future regressions of the Phase 12 placeholder fix become immediately visible without on-device testing.
- **Visual UAT (Studio Preview, 4 checks) PASSED** by user.
- **On-device UAT (Emulator, 8 functional checks 5-12) PASSED** by user — covering the visible-bug fix on Home cards, the 180 dp hero gradient + 40 sp glyph placeholder, preset selection round-trip, gallery upload to Storage Emulator at the canonical path, owner-only tap target on the Detail hero (guest sees no affordance), and Romanian locale string parity.
- **`storage.rules` live-bucket deploy DEFERRED at the user's request** via the resume signal `approved — storage deploy skipped`. Captured the deferred step in `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` with the full deploy command, the one-time cross-service permissions prompt, verification steps (Console Rules tab + Rules Simulator), and suggested timing (before any production cover-photo upload traffic).
- **12-VALIDATION.md fully populated and signed off:**
  - Frontmatter flipped: `status: draft → approved`, `nyquist_compliant: false → true`, `wave_0_complete: false → true`.
  - Per-Task Verification Map: 15 rows spanning Plans 12-01..12-05 (mechanical V&V trace from Decision IDs to passing tests + UAT).
  - Wave 0 Requirements: all 8 RED suites checked (7 GREEN by Plans 12-02/03/04; `CoverImageProcessorTest` @Ignored per Plan 12-01 Robolectric decision).
  - Manual-Only Verifications: 6 of 7 PASSED on 2026-04-28; the storage rules live-bucket deny test is the 1 DEFERRED row (linked to the follow-up todo). Local `storage.rules` content is correct (cross-service `firestore.get(...)` mirroring `canReadRegistry`); the live bucket is currently default-deny until the deploy lands.
  - Validation Sign-Off checklist: 7 of 8 ticked; the storage deploy line is marked DEFERRED (linked to the follow-up todo).
  - Approval: `approved 2026-04-28 (storage rules deploy deferred — see todo) via /gsd:execute-phase Wave 3 human checkpoint`.

## 12-Check UAT Outcome Table

| # | Check | Surface | Outcome (2026-04-28) |
|---|-------|---------|----------------------|
| 1 | StyleGuidePreview: HeroImageOrPlaceholder hero (40 sp) + card (32 sp) renders correctly | Studio @Preview | ✅ PASSED (visual UAT) |
| 2 | StyleGuidePreview: CoverPhotoPickerInline 3 states (disabled / enabled+None / enabled+Preset) | Studio @Preview | ✅ PASSED (visual UAT) |
| 3 | StyleGuidePreview: CoverPhotoPickerSheet (Wedding, 6 presets, first selected) — 3×2 grid + pill + remove | Studio @Preview | ✅ PASSED (visual UAT) |
| 4 | StyleGuidePreview: RegistryCard placeholder — Primary (Wedding) + Secondary (Baby) regression check | Studio @Preview | ✅ PASSED (visual UAT) |
| 5 | Home card placeholder: dark Primary + light Secondary both render gradient + glyph for `imageUrl == null` | On-device | ✅ PASSED |
| 6 | RegistryDetailScreen 180 dp hero placeholder: gradient + 40 sp glyph for `imageUrl == null` | On-device | ✅ PASSED |
| 7 | Cover-photo sheet opens with 6 Wedding presets when occasion = Wedding; preset selection dismisses sheet | On-device | ✅ PASSED |
| 8 | After save: Home card + Detail hero render the chosen preset; Firestore stores `imageUrl = "preset:Wedding:N"` (Pitfall 1 fix) | On-device + Emulator UI | ✅ PASSED |
| 9 | Edit registry → Pick from gallery → System Photo Picker opens; sheet dismisses; preview updates | On-device | ✅ PASSED |
| 10 | After save: file lands at `users/{uid}/registries/{registryId}/cover.jpg` in Storage Emulator UI; `Registry.imageUrl` = download URL (D-04, D-05, D-07) | On-device + Emulator UI | ✅ PASSED |
| 11 | Non-owner viewing the same registry: NO tap affordance on hero (D-13 owner-only) | On-device | ✅ PASSED |
| 12 | Romanian locale: cover_photo_label = "Fotografie de copertă"; sheet header = "ALEGE O COPERTĂ"; pill = "Alege din galerie"; remove = "Elimină coperta"; disabled hint = "Alege o ocazie pentru a vedea coperţi sugerate" — no English fallback (I18N-02) | On-device (RO locale) | ✅ PASSED |

**Tally:** 12 of 12 PASSED via the user resume signal. Plus 1 OPTIONAL check (storage rules security simulator deny test) DEFERRED with the deploy todo.

## Storage Rules Deploy Status

- **Authored:** Plan 12-02 commit `e979e45` (2026-04-28). `storage.rules` lives at the repo root; `firebase.json` wires it at the top-level peer to `firestore.rules`.
- **Local correctness:** Verified in Plan 12-02's self-check — rules use cross-service `firestore.get(...)` to mirror `firestore.rules` `canReadRegistry`, with legacy-doc defaults via `data.get('visibility', 'public')` and a default-deny `match /{allPaths=**}` floor (Pitfall 5 — bounded cross-service reads).
- **Deployed:** ❌ NOT YET. User deferred via `approved — storage deploy skipped` resume signal.
- **Follow-up:** `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` carries:
  - Deploy command (`firebase deploy --only storage --project gift-registry-ro`)
  - One-time cross-service permissions prompt instruction (accept `Y`)
  - Verification: Firebase Console → Storage → Rules tab + optional Rules Simulator (DENY for non-owner write to a stranger's registry path)
  - Suggested timing: before any production registry traffic uses the cover-photo upload path
- **Production impact until the deploy lands:** Preset selections still work end-to-end (no Storage write involved). Gallery uploads will be blocked by the default deny rule on the live bucket. Local emulator runs are unaffected.

## Romanian Locale Verification Result

12 UAT check 12 PASSED on a Romanian-locale device. All 10 `cover_photo_*` keys translate; no English fallback. The 10-key tally was already verified in Plan 12-04's self-check via `grep -c '<string name="cover_photo_'` returning 10 on both `values/strings.xml` and `values-ro/strings.xml`. Plan 12-05's contribution was the on-device confirmation that the keys actually surface on a RO device with no plumbing breakage.

## Phase 12 Final Tally

- **Wave 0 RED suites:** 8 total. **7 GREEN** (Plans 12-02/03/04 flipped each in turn). **1 @Ignored** (`CoverImageProcessorTest` — Robolectric not on the test runtime classpath, locked at the interface level by Plan 12-01).
- **Phase 12 unit tests:** all 28 contract test methods land per the Plan 12-01 → 12-04 mapping. 0 RED Phase 12 tests remain.
- **Decision IDs:** 16 total (D-01..D-16). **All 16 satisfied.** D-02 partially via placeholder JPEGs with the curation follow-up todo (logged in Plan 12-02).
- **Pitfalls:** Pitfall 1 (RegistryDto + toMap/toUpdateMap roundtrip imageUrl) FIXED in Plan 12-02. Pitfall 2 (upload-BEFORE-write strict ordering) FIXED in Plan 12-04 with `coVerifyOrder { uploadCover; createRegistry }` pinning. Pitfall 5 (cross-service rule cost) avoided via default-deny floor in `storage.rules`. Pitfall 6 (3-stop dark overlay only on real images) preserved in `RegistryDetailHero` after the `HeroImageOrPlaceholder` extraction. Pitfall 7 (`remember(imageUrl) { resolveImageModel(imageUrl) }`) honored in `HeroImageOrPlaceholder`.
- **Follow-up todos logged:**
  1. `.planning/todos/pending/2026-04-27-curate-real-preset-jpegs-for-phase-12.md` (real preset JPEG curation — 36 stock-licensed photos to replace the placeholder colour-blocks; logged in Plan 12-02)
  2. `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` (deferred `firebase deploy --only storage`; logged in this plan)
- **The visible bug from the user's screenshot is RESOLVED:** both `RegistryCardPrimary` and `RegistryCardSecondary` now render the gradient + glyph placeholder when `Registry.imageUrl == null`. Verified via on-device UAT check 5 AND visually in StyleGuidePreview's `RegistryCard placeholder — Primary (Wedding) + Secondary (Baby)` section.

## Suggested ROADMAP.md Entry Update

```markdown
### Phase 12: Registry Cover Photo & Themed Placeholder
- [x] 12-05-PLAN.md -- Wave 3: 4 StyleGuidePreview sections + on-device UAT (12-check checklist) + storage.rules deploy (human checkpoint) — UAT approved 2026-04-28; storage deploy deferred to .planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md
```

Phase 12 row in the progress table: `5/5 plans complete` once this plan's metadata commit lands.

## StyleGuidePreview Section Inventory

| Preview Name (in @Preview annotation) | Demonstrates | Composable(s) Under Preview |
|----------------------------------------|--------------|------------------------------|
| `HeroImageOrPlaceholder — hero (40 sp) + card (32 sp)` | Single source of gradient + glyph fallback at both hero (40 sp on 180 dp) and card (32 sp on 16:9) glyph sizes; vertical accentSoft → accent gradient; Instrument Serif italic glyph in paper colour | `HeroImageOrPlaceholder` |
| `CoverPhotoPickerInline — 3 states` | Disabled (occasion = null + ink scrim + caption); enabled + None (gradient placeholder); enabled + Preset (rendered preset thumbnail) — covers all D-09 + D-12 paths | `CoverPhotoPickerInline` |
| `CoverPhotoPickerSheet (Wedding, 6 presets, first selected)` | Inline-body reproduction of the ModalBottomSheet content: 3×2 LazyVerticalGrid of PresetThumbnails (first selected with 1.5 dp accent border, others with 1 dp line border), full-width ink "Pick from gallery" pill, low-emphasis "Remove cover photo" TextButton | `PresetThumbnail` (real); ModalBottomSheet body manually composed |
| `RegistryCard placeholder — Primary (Wedding) + Secondary (Baby)` | Visible-bug regression check: both card variants render the gradient + glyph placeholder when `imageUrl == null`. Primary uses 70%-brightness ColorFilter on the (absent) image branch; Secondary renders at full brightness | `RegistryCardPrimary`, `RegistryCardSecondary` |

## Task Commits

1. **Task 1: Append 4 Phase 12 StyleGuidePreview sections** — `33a447b` (feat) — 2026-04-28T15:10:05+03:00
2. **Task 2: Deploy storage.rules + on-device UAT (human-verify checkpoint)** — *no commit* — checkpoint resolved by user resume signal `approved — storage deploy skipped`; deploy step captured in deferred-deploy todo
3. **Task 3: Finalize 12-VALIDATION.md** — `390708e` (docs) — 2026-04-28 (also includes the deferred-deploy todo creation in the same commit)

**Plan metadata commit:** _(this commit, see final commit log — covers 12-05-SUMMARY.md + STATE.md + ROADMAP.md + REQUIREMENTS.md)_

## Files Created/Modified

**Created (2):**

- `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` — follow-up for the deferred `firebase deploy --only storage`
- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-05-SUMMARY.md` — this summary

**Modified (2):**

- `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` — appended 4 Phase 12 @Preview sections (count 23 → 27); committed in Task 1 (`33a447b`)
- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-VALIDATION.md` — frontmatter approved + Per-Task Verification Map populated (15 rows) + Wave 0 Requirements all checked + Manual-Only Verifications statuses (6 PASSED, 1 DEFERRED) + Sign-Off checklist + dated Approval line; committed in Task 3 (`390708e`)

## Decisions Made

See `key-decisions` in frontmatter. Highlights:

- **CoverPhotoPickerSheet inline-body @Preview** — `ModalBottomSheet` does not render in Studio's @Preview pane, so the sheet body is reproduced directly (LazyVerticalGrid + PresetThumbnail + Pick-from-gallery pill + Remove button) so the visual contract is still reviewable.
- **Storage rules deploy deferred via structured todo** — the user explicitly approved 12 of 12 UAT checks but skipped the deploy step. Captured the deferred step in `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` rather than blocking the plan; production traffic remains blocked on cover-photo upload until the todo lands.
- **VALIDATION.md `status: approved`** (not `complete`) — matches the original schema seen in the file pre-Plan-05; `approved` reflects user sign-off, `complete` is reserved for downstream verification work.
- **Per-Task Verification Map row count = 15** — 1 row per task across Plans 12-02..12-05 plus 3 rows for Plan 12-01 (which had 3 tasks). Exceeds the plan's `at least 14 rows` floor by 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] `TBD` substring leaked into VALIDATION.md from a self-referential grep command literal**

- **Found during:** Task 3 verification (`grep -c 'TBD' 12-VALIDATION.md` returned 1).
- **Issue:** The Per-Task Verification Map row for `12-05-T3` originally embedded the literal `grep -c 'TBD' 12-VALIDATION.md` as the automated command — which itself contained the substring `TBD`, defeating the acceptance criterion `grep -c 'TBD' returns 0`.
- **Fix:** Replaced the embedded grep literal with the prose phrase `placeholder-token grep returns 0 in this file` (semantically equivalent, no embedded sentinel).
- **Files modified:** `.planning/phases/12-registry-cover-photo-themed-placeholder/12-VALIDATION.md`
- **Verification:** `grep -c 'TBD'` now returns 0; `grep -cE 'nyquist_compliant: true|wave_0_complete: true|status: approved'` returns 5 (3 in frontmatter + 2 in the Sign-Off body). Acceptance preserved.
- **Committed in:** `390708e` (Task 3 commit).

---

**Total deviations:** 1 auto-fixed (Rule 3 — Blocking).
**Impact on plan:** No scope change. Pure documentation hygiene fix; the verification map prose is functionally identical to the original grep literal but no longer leaks the placeholder token.

## Issues Encountered

- **Pre-existing dirty working tree on entry:** `.planning/phases/12-registry-cover-photo-themed-placeholder/12-01-PLAN.md` and `12-02-PLAN.md` had unrelated edits in the working tree, plus 36 modified `drawable-xxhdpi/preset_*.jpg` files (likely the curation follow-up has begun separately). Out of scope for Plan 12-05; left untouched. Plan 12-05's two commits (`33a447b` and `390708e`) plus this metadata commit do NOT include any of those pre-existing changes.

## Known Stubs

None. All Plan 12-05 deliverables ship as final-shape:
- 4 StyleGuidePreview entries: real composable references (no placeholder content).
- VALIDATION.md: every row has a real Decision-ID / test command / status; no TBD / placeholder rows remain.
- Deferred-deploy todo: structured with deploy command + verification + acceptance criteria — not a stub.

## User Setup Required

**Outstanding from this plan:** the deferred storage rules deploy. See `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md`. The follow-up requires:

- One terminal command: `firebase deploy --only storage --project gift-registry-ro`
- One-time CLI prompt: accept the cross-service permissions grant (`Y`)
- One Console verification: Firebase Console → Storage → Rules tab shows the new rules content with `firestore.get(...)` cross-service helpers

No environment variables required. The existing `gift-registry-ro` Firebase project has Storage natively enabled (Plan 12-02 USER-SETUP frontmatter confirmation).

## Next Phase Readiness

- **Phase 12 verification (`/gsd:verify-work 12`)** can run mechanically against `12-VALIDATION.md` — Wave 0 + Wave 1 + Wave 2 + Wave 3 trace is in place; all 16 Decision IDs satisfied; Pitfalls 1+2+5+6+7 pinned via tests or runtime gates.
- **v1.1 GA** is gated on the storage rules deploy todo. Until that lands, gallery cover-photo uploads will be blocked on the live bucket by the default deny rule (preset selections + placeholder rendering work end-to-end with no deploy needed).
- **No code blockers for Phase 12 closure.** All composables compile; full unit suite GREEN; visual UAT + on-device UAT signed off.
- **Suggested next step:** complete `/gsd:verify-work 12`, then either pick up the storage deploy todo OR add the next phase / quick task.

## Self-Check: PASSED

- ✅ Task 1 commit `33a447b` exists in `git log` (verified: `git log --oneline | grep 33a447b` returns 1 match)
- ✅ Task 3 commit `390708e` exists in `git log` (verified: `git log --oneline | grep 390708e` returns 1 match)
- ✅ `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` `@Preview` count = 27 (was 23 pre-Plan-05; delta = +4 matching Task 1 acceptance)
- ✅ `.planning/phases/12-registry-cover-photo-themed-placeholder/12-VALIDATION.md` frontmatter contains `status: approved`, `nyquist_compliant: true`, `wave_0_complete: true` (3 keys flipped from draft / false)
- ✅ `grep -c 'TBD' 12-VALIDATION.md` returns 0 (placeholder hygiene)
- ✅ `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` exists with deploy command, cross-service permissions prompt, verification steps, and acceptance criteria
- ✅ Per-Task Verification Map has 15 rows (exceeds ≥ 14 floor)
- ✅ All 6 Wave 0 Requirements bullets are checked `[x]` (actually 8 bullets all checked — exceeds the ≥ 6 floor)
- ✅ Manual-Only Verifications has a Status column with 6 PASSED + 1 DEFERRED entries
- ✅ Approval line reads `approved 2026-04-28 (storage rules deploy deferred — see todo) via /gsd:execute-phase Wave 3 human checkpoint`

---
*Phase: 12-registry-cover-photo-themed-placeholder*
*Plan: 05*
*Completed: 2026-04-28*
