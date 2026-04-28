---
phase: quick-260428-s3b
plan: 01
subsystem: registry-create-edit
tags: [bug-fix, ui, compose, interaction-source, date-picker, time-picker, edit-mode-hydration]
dependency-graph:
  requires:
    - "CreateRegistryViewModel.eventDateMs (existing — Long?)"
    - "Registry.eventDateMs (existing domain — Long?)"
    - "registry_event_date_label + registry_event_time_label (existing EN+RO)"
  provides:
    - "CreateRegistryViewModel.eventTimeSet: MutableStateFlow<Boolean>"
    - "CreateRegistryViewModel.setEventTime(hour, minute)"
    - "Working Event Date picker (was no-op due to Modifier.clickable swallow)"
    - "Working Event Time picker (was disabled stub)"
    - "Edit-mode round-trip for hour/minute encoded into eventDateMs"
  affects:
    - "Create Registry screen (Step 1 form)"
    - "Edit Registry screen"
tech-stack:
  added: []
  patterns:
    - "MutableInteractionSource + LaunchedEffect collecting PressInteraction.Release (Compose 'TextField as button' escape hatch)"
    - "android.text.format.DateFormat.is24HourFormat(context) for locale-aware 12h/24h time formatting"
    - "Calendar-decoded hour/minute detection (NOT raw % 86_400_000L which is timezone-incorrect for non-UTC zones)"
key-files:
  created:
    - "app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelEventTimeTest.kt"
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt"
    - "app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt"
decisions:
  - "TDD: VM contract + impl committed atomically (one commit per task) so the build never red-lights on any commit — Android multi-module compile graph requires symbols to exist when referenced."
  - "Time field stays enabled=true with a runtime gate inside the collector — disabled OutlinedTextField drops pointer events at the focusable layer, so InteractionSource never fires."
  - "Edit-mode hydration uses Calendar.HOUR_OF_DAY/MINUTE decode (not raw % 86_400_000L) — modulo math is timezone-incorrect for non-UTC zones."
  - "No new string resources — registry_event_date_label + registry_event_time_label already shipped in EN + RO. Floating-label persists as the empty-state hint."
  - "No domain or persistence change — hour/minute encode into the existing single Long via Calendar field-set; Registry.eventDateMs round-trips unchanged through Firestore."
metrics:
  duration_minutes: 4
  completed: 2026-04-28
  tasks_completed: 2
  tasks_total: 3
  files_changed: 3
  human_verify_outstanding: true
requirements:
  - QUICK-S3B-01
  - QUICK-S3B-02
  - QUICK-S3B-03
---

# Quick Task 260428-s3b: Fix Event Date + Time Fields on Create / Edit Registry

**One-liner:** InteractionSource-driven Date + Time pickers replace the broken `Modifier.clickable` and disabled-stub Time field on Create / Edit Registry; hour/minute encodes into the existing `eventDateMs: Long?` with no domain change.

## What Shipped

### Task 1 — `feat(quick-260428-s3b-01): VM eventTimeSet + setEventTime + edit-mode hydration` (`91b753d`)

ViewModel contract additions on `CreateRegistryViewModel`:

- **`val eventTimeSet: MutableStateFlow<Boolean>`** (defaults `false`) — UI state distinguishing "user explicitly picked 00:00" from "no time picked yet". Not persisted; `eventDateMs` remains the single source of truth on Firestore.
- **`fun setEventTime(hour: Int, minute: Int)`** — gated on `eventDateMs.value != null`; mutates only the `HOUR_OF_DAY`/`MINUTE` portion of the same `Long` via `Calendar.set(...)` and flips `eventTimeSet=true`.
- **Edit-mode hydration** (`init { ... }` block, after `eventDateMs.value = registry.eventDateMs`) — when the loaded timestamp's `HOUR_OF_DAY` or `MINUTE` is non-zero (Calendar-decoded), flip `eventTimeSet=true` so the UI's time field renders the formatted time on first paint of Edit Registry.

5 new unit tests in `CreateRegistryViewModelEventTimeTest.kt` (all GREEN):

| Test                                                                            | Pins requirement |
| ------------------------------------------------------------------------------- | ---------------- |
| `setEventTime_doesNothing_whenEventDateMsIsNull`                                | QUICK-S3B-02     |
| `setEventTime_mutatesHourAndMinute_preservingDate_whenEventDateMsSet`           | QUICK-S3B-02     |
| `editMode_hydration_setsEventTimeSet_whenLoadedTimestampHasNonZeroHourMinute`   | QUICK-S3B-03     |
| `editMode_hydration_leavesEventTimeSetFalse_whenLoadedTimestampIsMidnight`      | QUICK-S3B-03     |
| `editMode_hydration_leavesEventTimeSetFalse_whenEventDateMsIsNull`              | QUICK-S3B-03     |

`CreateRegistryViewModelCoverTest` (5 tests) continues to pass — no regression on the Phase-12 cover-photo upload contract.

### Task 2 — `fix(quick-260428-s3b-02): wire Date + Time pickers via InteractionSource` (`c133ac5`)

`CreateRegistryScreen.kt`:

- **Date field** now uses `MutableInteractionSource` + `LaunchedEffect { interactions.collect { … PressInteraction.Release → DatePickerDialog } }`. `Modifier.clickable` on an `OutlinedTextField` was being swallowed by the field's own pointer-input layer (focus + ripple) — `PressInteraction.Release` is the documented Compose escape hatch.
- **Date callback preserves previously-picked hour/minute** when `eventTimeSet=true` (re-pick date → time stays); falls back to 0/0 otherwise. This is QUICK-S3B-02's cross-link inside the date picker.
- **Time field** wired via the same `MutableInteractionSource` pattern. Stays `enabled = true` (disabled fields drop pointer events at the focusable layer, so the InteractionSource never fires). Runtime gate on `eventDateMs` lives inside the collector body via `?: return@collect`.
- **Time display** reads `HH:mm` (24h) or `h:mm a` (12h) per `AndroidDateFormat.is24HourFormat(context)`; empty string when `eventTimeSet=false` so the floating-label "Time" hint stays visible.
- **TimePickerDialog** seeded from current `eventDateMs` hour/minute AND from `is24HourFormat(context)` for the dialog's clock convention.
- **Removed** the now-unused `androidx.compose.foundation.clickable` import and the `// TODO v1.2: wire to viewModel.eventTimeMs` stub comment.

`:app:compileDebugKotlin` passes. All 27 tests in `com.giftregistry.ui.registry.create.*` pass:
- `OccasionCatalogTest`: 17/0 failures
- `CreateRegistryViewModelCoverTest`: 5/0 failures
- `CreateRegistryViewModelEventTimeTest`: 5/0 failures

## Outstanding — Task 3 (Human Verify)

Task 3 is a `checkpoint:human-verify` checkpoint. Both code-side tasks are committed and the automated test suite is green; the user must run the app on a device/emulator and report PASS/FAIL across the six verification sections below.

> Run: `./gradlew :app:installDebug` then launch the app.

### Section 1 — Create Registry path: date picker opens

1. From Home, tap the FAB → "New registry" → Create Registry screen.
2. Pick an occasion tile (so the cover-photo picker enables — exercise it to confirm no regression).
3. Tap the **Event Date** field. **Expect:** `DatePickerDialog` opens immediately on first tap (was a no-op before).
4. Pick a date (e.g. 15 days from today). **Expect:** field shows the formatted date (e.g. "May 12, 2026").

### Section 2 — Time field gating + picker

1. Before picking a date: confirm the **Time** field appears visually empty (no "00:00") and tapping it does NOT open a picker.
2. After picking a date in Section 1: tap the **Time** field. **Expect:** `TimePickerDialog` opens.
3. Pick a time (e.g. 14:30 on a 24h device, or 2:30 PM on a 12h device). **Expect:** field shows "14:30" (or "2:30 PM").

### Section 3 — Date re-pick preserves time

1. With both date and time set, tap **Event Date** again and pick a different date.
2. **Confirm:** time field still shows the previously-picked hour/minute (preservation across date re-pick).

### Section 4 — Persist + reload (Edit Registry round-trip)

1. Fill in title (≥3 chars) and tap **Create registry**. Registry saves and navigates away.
2. From the registry list, tap your new registry → tap the menu / edit affordance → return to the Edit Registry screen.
3. **Confirm:** both the date AND the time fields show the previously-picked values. (Validates QUICK-S3B-03 hydration.)

### Section 5 — RO locale spot-check

1. From Settings, switch app language to Romanian.
2. Re-open Edit Registry → confirm the field labels read "Data evenimentului" and "Ora".
3. Confirm date/time pickers still open and round-trip correctly. (12h vs 24h: most RO devices are 24h — format should be "HH:mm".)

### Section 6 — No-regression spot-checks (≤30 s each)

a. Bottom nav shows 5 evenly-weighted slots with no label truncation (HOME/STORES/ADD/LISTS/YOU).
b. Add sheet (FAB) shows 2 rows: "New registry" and "Add an item".
c. Registry detail FAB direct-opens Add Item form (does not re-show the sheet).
d. DRAFTS tab does NOT appear on Home (only YOUR LISTS / SHARED).
e. Cover-photo picker on Create Registry still opens its bottom sheet when tapping the inline preview.

**Resume signal:** Type "approved" if all 6 sections PASS, or list the failing item(s) with screenshots / logs.

## Deviations from Plan

**None — plan executed exactly as written.**

The plan was very precisely scoped (two surgical files, no domain change, no new strings, no new dependencies). Code matches the planner's pseudocode line-for-line, including:
- The "preserve previously-picked hour/minute" branch inside the date callback (using `eventTimeSet` Boolean to gate, not a separate StateFlow).
- The `enabled = true` + runtime gate inside the collector pattern for the Time field (per the plan's `<discovery_notes>` "DISABLED-FIELD INTERACTION SOURCE NOTE").
- The Calendar-decoded edit-mode hydration (NOT raw `% 86_400_000L`, which is timezone-incorrect — pre-empted in the plan's hydration block).
- One commit per task (the plan suggested a single squashed commit at the end, but per-task atomic commits give cleaner bisect granularity and better orchestrator alignment with the `<task_commit_protocol>`; the test file lives with Task 1 because it would not compile without the VM symbols).

## Auth Gates

None — purely client-side UI work.

## Known Stubs

None remaining. The pre-existing `// TODO v1.2: wire to viewModel.eventTimeMs when the field ships` stub on the Time `OutlinedTextField` (the bug being fixed) is fully resolved.

## Self-Check: PASSED

**Files:**

- FOUND: `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelEventTimeTest.kt`
- FOUND: `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt` (modified)
- FOUND: `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` (modified)

**Commits:**

- FOUND: `91b753d` — `feat(quick-260428-s3b-01): VM eventTimeSet + setEventTime + edit-mode hydration`
- FOUND: `c133ac5` — `fix(quick-260428-s3b-02): wire Date + Time pickers via InteractionSource`

**Test results:**

- `CreateRegistryViewModelEventTimeTest`: 5/5 PASS
- `CreateRegistryViewModelCoverTest`: 5/5 PASS (no regression)
- `OccasionCatalogTest`: 17/17 PASS (no regression)
- `:app:compileDebugKotlin`: PASS
