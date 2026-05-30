---
status: resolved
trigger: "create-registry-default-other-blocks-cover-picker"
created: 2026-05-30T00:00:00Z
updated: 2026-05-30T12:00:00Z
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: CONFIRMED — Shape (A). ViewModel initializes occasion to "" (empty string). OccasionCatalog.storageKeyFor("") returns "Custom", so OccasionTileGrid renders "Custom/Other" tile as selected. isCoverPickerEnabled("") returns false because "".isNullOrBlank() == true. The two pieces never agreed on what "default" means.
test: change ViewModel initial value from "" to "Custom" so isCoverPickerEnabled("Custom") == true on first frame
expecting: first-render "Other" tile highlighted AND cover picker enabled simultaneously
next_action: apply single-line fix in CreateRegistryViewModel.kt, update resetForm() to match, compile to verify

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: When the Create Registry screen opens, "Other" should be selected AND the cover photo picker should be enabled — the two views should agree on the selected occasion from the very first frame.

actual: On first open, the "Other" tile is highlighted but the cover photo picker shows "Pick an occasion to see suggested covers" and is disabled. Tapping any other occasion → the new tile lights up + picker activates. Tapping "Other" again → the cover picker now activates and works correctly. Subsequent screen opens reproduce the same first-render bug.

errors: No exceptions / no logcat errors — pure UI state.

reproduction:
  1. From Home, tap to start creating a new registry.
  2. Observe the Create Registry screen on first frame: "Other" tile shows highlighted, cover photo picker shows "Pick an occasion to see suggested covers" and is non-tappable.
  3. Tap any other tile (Birthday) → it lights up, cover picker becomes enabled.
  4. Tap "Other" again → "Other" lights up AND cover picker now stays enabled.

started: Always — not a regression.

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: ViewModel default is some non-empty sentinel like Occasion.OTHER but isCoverPickerEnabled excludes "Custom"
  evidence: isCoverPickerEnabled returns !occasion.isNullOrBlank(). "Custom" is non-blank so it would return true if VM set it. VM is actually "".
  timestamp: 2026-05-30T00:01:00Z

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-05-30T00:01:00Z
  checked: CreateRegistryViewModel.kt line 44
  found: val occasion = MutableStateFlow("") — empty string initial value
  implication: isCoverPickerEnabled("") == false because "".isNullOrBlank() == true

- timestamp: 2026-05-30T00:01:00Z
  checked: OccasionCatalog.storageKeyFor (line 54-62)
  found: storageKeyFor("") returns "Custom" (empty string hits the `normalised.isEmpty()` branch)
  implication: OccasionTileGrid sees canonicalSelected == "Custom" == last entry storageKey → last tile isSelected=true (visual highlight on "Custom/Other")

- timestamp: 2026-05-30T00:01:00Z
  checked: OccasionTileGrid.kt lines 63-65
  found: isSelected = selectedOccasion.equals(entry.storageKey, ignoreCase = true) || canonicalSelected == entry.storageKey. For occasion="" and entry="Custom": first clause false, second clause ("Custom"=="Custom") true → tile renders selected.
  implication: Visual selection fires via the canonical fallback, not a real match.

- timestamp: 2026-05-30T00:01:00Z
  checked: CoverPhotoPickerEnabled.kt line 17-18
  found: isCoverPickerEnabled returns !occasion.isNullOrBlank(). Called from CoverPhotoPickerInline with the raw occasion string (not the canonicalized one).
  implication: isCoverPickerEnabled("") == false → cover picker disabled on first render, contradicting the visual tile highlight.

- timestamp: 2026-05-30T00:01:00Z
  checked: CreateRegistryViewModel.resetForm() line 275
  found: occasion.value = "" — resetForm also resets to empty string, so the bug reproduces on every new registry creation.
  implication: Fix must be applied in both the StateFlow initializer AND resetForm().

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: CreateRegistryViewModel initializes `occasion = MutableStateFlow("")`. OccasionCatalog.storageKeyFor("") canonicalizes empty string to "Custom", causing OccasionTileGrid to render the "Custom/Other" tile as visually selected. However isCoverPickerEnabled("") checks the raw string (not the canonical form) and returns false because "".isNullOrBlank() == true. The two systems diverge: grid uses canonical fallback; cover picker uses raw value.
fix: Change MutableStateFlow("") to MutableStateFlow("Custom") in CreateRegistryViewModel.kt (line 44). Change occasion.value = "" to occasion.value = "Custom" in resetForm() (line 275). This makes the ViewModel's actual value match what the grid displays visually, so isCoverPickerEnabled("Custom") == true on first frame.
verification: Confirmed on physical device by user. "Other" tile highlighted on first frame AND cover picker immediately enabled. Second-registry creation path (via resetForm()) also clean.
files_changed: [app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt]
