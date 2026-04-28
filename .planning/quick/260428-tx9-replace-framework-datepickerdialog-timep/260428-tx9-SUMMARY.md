---
phase: quick-260428-tx9
plan: 01
subsystem: registry-create
tags: [ui, theme, material3, compose, picker, palette, giftmaison]
requires:
  - quick-260428-s3b (Event Date + Time pickers — InteractionSource trigger pattern, eventTimeSet StateFlow, setEventTime VM API — all preserved verbatim)
  - Phase 08 GiftMaison design foundation (LightColorScheme.primary = gm.accent terracotta in Theme.kt:36)
provides:
  - M3 Compose DatePickerDialog + DatePicker(state) on Create/Edit Registry
  - M3 Compose AlertDialog { TimePicker(state) } on Create/Edit Registry
  - GiftMaison terracotta palette inheritance on both pickers (header, selection, AM/PM toggle)
  - UTC↔local-Calendar conversion guard against Bucharest UTC+2/+3 off-by-one
affects:
  - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt — only file modified
tech-stack:
  added:
    - androidx.compose.material3.DatePicker
    - androidx.compose.material3.DatePickerDialog (M3 Compose, NOT android.app)
    - androidx.compose.material3.rememberDatePickerState
    - androidx.compose.material3.TimePicker
    - androidx.compose.material3.rememberTimePickerState
    - androidx.compose.material3.AlertDialog
    - androidx.compose.material3.ExperimentalMaterial3Api (opt-in)
  removed:
    - android.app.DatePickerDialog (framework — bypassed Compose theme)
    - android.app.TimePickerDialog (framework — bypassed Compose theme)
  patterns:
    - "M3 picker default colors() inherit MaterialTheme.colorScheme.primary — no `colors=` override needed when LightColorScheme.primary maps the brand accent"
    - "rememberDatePickerState UTC-midnight ↔ local-Calendar Long: decode UTC to (year, month, day), re-encode in local TZ preserving (prevHour, prevMin)"
    - "M3 has NO TimePickerDialog composable; AlertDialog { TimePicker(state) } is the documented standard"
    - "Stringresource(android.R.string.ok / .cancel) for system-localized OK/Cancel — zero new strings.xml keys"
    - "@OptIn(ExperimentalMaterial3Api::class) at @Composable function level (not file-level) — narrows the opt-in surface"
key-files:
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt
decisions:
  - "Inherit from LightColorScheme via M3 defaults instead of explicit DatePickerDefaults.colors()/TimePickerDefaults.colors() override — fewer call-site mistakes when the palette evolves; one less place to drift from Theme.kt"
  - "android.R.string.ok / cancel over new common_ok / common_cancel strings — system labels are translated by the Android platform on RO devices; avoids polluting strings.xml"
  - "UTC↔local conversion code lives at the call site (DatePickerDialog confirmButton), not in a helper — single use, hides nothing, makes the off-by-one risk visible to the next reader"
  - "@OptIn at @Composable level not file level — DatePicker/TimePicker remain experimental in BOM 2026.03 surface; localizing the opt-in flags it for future BOM-bump audits"
  - "Show-state flags declared BEFORE the LaunchedEffect collectors — Kotlin scope rules require forward declaration even in linear @Composable bodies; build error 'Unresolved reference showDatePicker' caught this in the first compile pass"
metrics:
  duration: 2min
  completed: 2026-04-28
  task_count: 1 (auto) + 1 (checkpoint:human-verify, deferred to user)
  file_count: 1 (impl)
  commit_count: 1 (impl) + 1 (docs to follow)
requirements:
  - QUICK-TX9-01  # Date picker honours GiftMaison terracotta palette — pending human-verify
  - QUICK-TX9-02  # Time picker honours GiftMaison terracotta palette — pending human-verify
  - QUICK-TX9-03  # Hour/minute preservation on re-pick still works — preserved verbatim
  - QUICK-TX9-04  # 24h/12h device-locale awareness — preserved verbatim (is24Hour passed to rememberTimePickerState)
  - QUICK-TX9-05  # Edit Registry round-trip — preserved verbatim (no VM/persistence changes)
---

# quick-260428-tx9 Plan 01: Replace framework Date/Time pickers with M3 Compose Summary

## One-liner

Swap `android.app.DatePickerDialog` + `android.app.TimePickerDialog` for `androidx.compose.material3.DatePickerDialog` + `DatePicker(state)` and `AlertDialog { TimePicker(state) }` on Create/Edit Registry — pickers now inherit GiftMaison terracotta via `MaterialTheme.colorScheme.primary` instead of bleeding the system Material green/teal.

## Problem

User reported (2026-04-28) the Event Date calendar on Create Registry showed teal/green chrome that didn't match the GiftMaison palette. Root cause: `android.app.DatePickerDialog` and `android.app.TimePickerDialog` are framework dialogs — they read the device's Android system theme via the `Context` they're constructed with, completely bypassing Compose's `MaterialTheme.colorScheme`. No amount of `colors=` overrides on the surrounding Compose code can fix it because the framework dialog is rendered in its own framework Window.

## Approach

Replace both framework dialogs with their Material3 Compose equivalents, both hoisted at sibling level to `Scaffold` and gated on `MutableState<Boolean>` flags flipped by the existing s3b `InteractionSource` collectors.

**Why this works:** `LightColorScheme` in `Theme.kt:36` maps `primary = gm.accent` (terracotta). `DatePickerDefaults.colors()` and `TimePickerDefaults.colors()` read `MaterialTheme.colorScheme.primary` for the header band, selection circle, AM/PM toggle, and confirm-button text. Inheritance is automatic — no `colors=` overrides needed.

## What was built

### Imports (CreateRegistryScreen.kt)

**Removed (framework):**
```kotlin
import android.app.DatePickerDialog
import android.app.TimePickerDialog
```

**Added (M3 Compose):**
```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
```

**Kept (still needed):** `import android.text.format.DateFormat as AndroidDateFormat` (used by both `timeFormatter` and `rememberTimePickerState(is24Hour=...)`); `import androidx.compose.ui.platform.LocalContext` (used by `AndroidDateFormat.is24HourFormat(context)`).

### Show-state flags

Declared right below the `dateInteractionSource` / `timeInteractionSource` `MutableInteractionSource`s, BEFORE the `LaunchedEffect` collectors that capture them (Kotlin forward-reference rule):

```kotlin
var showDatePicker by remember { mutableStateOf(false) }
var showTimePicker by remember { mutableStateOf(false) }
```

The `LaunchedEffect(dateInteractionSource)` / `LaunchedEffect(timeInteractionSource)` collectors no longer construct framework dialogs directly — they simply flip the corresponding flag (preserving the s3b runtime gate `viewModel.eventDateMs.value != null` for the time picker).

### M3 DatePickerDialog hoist

Placed at sibling level to `Scaffold`, immediately after the existing `pickerSheetOpen` cover-photo sheet hoist (so the order is: cover sheet, date picker, time picker).

**UTC↔local-Calendar conversion** is the most subtle part. `rememberDatePickerState.initialSelectedDateMillis` is interpreted as UTC-midnight of the displayed civil day, and `selectedDateMillis` is returned as UTC-midnight. The rest of the app stores `eventDateMs` as a local-Calendar `Long`. Symmetric conversion:

```kotlin
// Seed (local → UTC):
val seedUtcMillis = remember(eventDateMs) {
    eventDateMs?.let { localMs ->
        val cal = Calendar.getInstance().apply { timeInMillis = localMs }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        utc.timeInMillis
    }
}
// Confirm (UTC → local), preserving prior hour/minute when eventTimeSet=true:
val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
    timeInMillis = utcMs
}
val pickedYear = utcCal.get(Calendar.YEAR)
val pickedMonth = utcCal.get(Calendar.MONTH)
val pickedDay = utcCal.get(Calendar.DAY_OF_MONTH)
val prevCal = Calendar.getInstance().apply { timeInMillis = viewModel.eventDateMs.value ?: 0L }
val prevHour = if (eventTimeSet) prevCal.get(Calendar.HOUR_OF_DAY) else 0
val prevMin = if (eventTimeSet) prevCal.get(Calendar.MINUTE) else 0
val localCal = Calendar.getInstance().apply {
    set(pickedYear, pickedMonth, pickedDay, prevHour, prevMin, 0)
    set(Calendar.MILLISECOND, 0)
}
viewModel.eventDateMs.value = localCal.timeInMillis
```

**Why it matters on Bucharest UTC+2/+3:** without symmetric conversion, picking "May 15, 2026" on a UTC+2 device would seed the picker at UTC-22:00 of May 14 → display May 14 — off-by-one. The conversion strips both timestamps to civil-day Y/M/D before re-encoding.

### M3 AlertDialog-wrapped TimePicker hoist

M3 has no `TimePickerDialog` composable — the documented pattern is `AlertDialog { TimePicker(state) }`. Placed immediately after the date picker hoist:

```kotlin
val timePickerState = rememberTimePickerState(
    initialHour = seedCal.get(Calendar.HOUR_OF_DAY),
    initialMinute = seedCal.get(Calendar.MINUTE),
    is24Hour = AndroidDateFormat.is24HourFormat(context),
)
AlertDialog(
    onDismissRequest = { showTimePicker = false },
    confirmButton = {
        TextButton(onClick = {
            viewModel.setEventTime(timePickerState.hour, timePickerState.minute)
            showTimePicker = false
        }) { Text(stringResource(android.R.string.ok)) }
    },
    dismissButton = {
        TextButton(onClick = { showTimePicker = false }) {
            Text(stringResource(android.R.string.cancel))
        }
    },
    text = { TimePicker(state = timePickerState) },
)
```

`viewModel.setEventTime(hour, minute)` is the s3b VM API — unchanged. No new write paths added.

### s3b behaviour preserved verbatim

| s3b behaviour | tx9 status |
|---|---|
| `MutableInteractionSource` + `PressInteraction.Release` trigger pattern (escape hatch around `Modifier.clickable` no-op on `OutlinedTextField`) | preserved — collectors still fire on press release |
| Hour/minute preservation on re-pick (s3b `QUICK-S3B-02`) | preserved — moved into the new `DatePickerDialog` confirmButton's UTC→local Calendar merge |
| `eventTimeSet` `StateFlow` + `setEventTime(hour, minute)` VM API | unchanged — TimePicker confirmButton calls `viewModel.setEventTime` |
| 24h/12h device-locale awareness | preserved — `is24Hour = AndroidDateFormat.is24HourFormat(context)` passed to `rememberTimePickerState` |
| Runtime gate on `eventDateMs != null` for time picker | preserved — moved into the LaunchedEffect collector (`if (... && viewModel.eventDateMs.value != null) showTimePicker = true`) |
| Edit Registry round-trip | preserved — no VM, persistence, or VM hydration changes |
| Time field stays `enabled=true` with empty value until `eventTimeSet=true` | preserved — only the picker IMPLEMENTATION swapped, no field-level changes |

### Localization

Confirm/dismiss buttons use `stringResource(android.R.string.ok)` and `stringResource(android.R.string.cancel)`. These are system-localized — RO devices render "OK" / "Anulează" automatically. Zero new keys in `strings.xml` (`common_ok` / `common_cancel` were NOT added per the planner's localization decision).

## What was NOT changed

- `CreateRegistryViewModel` — zero changes (s3b `setEventTime` API + `eventTimeSet` StateFlow + `eventDateMs` MutableStateFlow all unchanged)
- `app/src/main/res/values*/strings.xml` — zero new keys
- `app/src/main/java/com/giftregistry/ui/theme/*` — zero changes (the inheritance "just works" because `LightColorScheme.primary = gm.accent`)
- Any `viewModel.*` write path other than `viewModel.eventDateMs.value =` (date) and `viewModel.setEventTime(hour, minute)` (time) — same two paths as s3b
- The s3b `InteractionSource` trigger pattern — still the only way the pickers open (Modifier.clickable on OutlinedTextField is still a no-op, escape hatch still required)

## Verification

### Automated (run by executor)

```
./gradlew :app:compileDebugKotlin --console=plain
> BUILD SUCCESSFUL in 7s
```

No compile errors from `CreateRegistryScreen.kt`. Pre-existing warnings on unrelated files (`AddActionSheet.kt`, `AddItemScreen.kt`, `OnboardingScreen.kt`, `RegistryDetailHero.kt`, `RegistryDetailScreen.kt`, `ShareBanner.kt`, DataStore `@Inject` annotation-target warnings, deprecated `Icons.Outlined.KeyboardArrowRight` / `Icons.Filled.OpenInNew` usages) are out of scope — logged below under "Deferred Issues".

Done-criteria checklist:
- `import android.app.DatePickerDialog` — absent (verified via `Grep`)
- `import android.app.TimePickerDialog` — absent (verified via `Grep`)
- `import androidx.compose.material3.{DatePicker, DatePickerDialog, rememberDatePickerState, TimePicker, rememberTimePickerState, AlertDialog, ExperimentalMaterial3Api}` — all present (7/7)
- `@OptIn(ExperimentalMaterial3Api::class)` — on `CreateRegistryScreen` (line 96)
- `showDatePicker` and `showTimePicker` flags exist
- `dateInteractionSource` / `timeInteractionSource` collectors flip the flags (no longer construct framework dialogs)
- M3 dialog hoists are siblings to `Scaffold`, alongside the cover-photo sheet
- DatePicker confirmButton implements UTC↔local Calendar conversion AND s3b hour/minute preservation
- TimePicker AlertDialog calls `viewModel.setEventTime(hour, minute)` only
- Zero `viewModel.*` write paths added/changed
- Zero `strings.xml` changes
- Zero theme/color changes

### Task 2 — human-verify: APPROVED 2026-04-28

User confirmed the fix on-device. Pickers render terracotta; OK/Cancel/locale/round-trip behaviours all working. No regressions reported.

### Original verification protocol (kept for audit trail)

**Task 2 (`type="checkpoint:human-verify"`) was NOT executed by this agent per the constraint "do NOT block on it; document it in the SUMMARY for the user".**

Build, install, and run the debug APK. Walk through the 8 checks below. Report any failure with the screen + check letter, ideally with a screenshot for borderline colour mismatches.

```
./gradlew :app:installDebug -Puse_emulator=true
```

**A — Date picker terracotta palette (primary fix):**
1. Open the app, sign in if needed, tap the bottom-nav `+` (Add) → "New registry" → land on Create Registry (Step 1)
2. Pick any occasion tile (so the form is fully active)
3. Tap the **Event Date** field
4. Expected:
   - Calendar dialog opens
   - Header band (year/date label) is **terracotta** (≈ `#C5634D` / GiftMaison `gm.accent`), NOT teal/green
   - Selected day's circle is **terracotta**, NOT system green
   - "OK" / "CANCEL" / "Switch to text input" buttons render in terracotta text colour
5. Tap a future date → **OK**
6. Expected: Dialog closes; Event Date field shows the picked date (e.g., "May 15, 2026")
7. Tap the field again → **Cancel**
8. Expected: Dialog closes; field unchanged

**B — Time picker terracotta palette:**
1. With a date already picked, tap the **Event Time** field
2. Expected:
   - Time picker dialog opens (clock-face / dial layout on phone)
   - Selected hour/minute number sits inside a **terracotta** filled circle
   - The active AM/PM toggle (12h locale) OR the active hour/minute text input has a **terracotta** background, NOT teal/green
   - "OK" / "CANCEL" buttons render in terracotta
3. Set a time (e.g., 14:30) → **OK**
4. Expected: Dialog closes; Event Time field shows "14:30" (RO/24h device) or "2:30 PM" (12h device)
5. Tap the field again → **Cancel** → dialog closes; field unchanged

**C — 24h vs. 12h locale awareness (s3b regression guard):**
1. Settings → System → Languages & input → "Use 24-hour format" — toggle ON
2. Re-tap Event Time → Expected: dial shows 0–23, no AM/PM toggle
3. Toggle OFF → re-tap → Expected: dial shows 1–12 + AM/PM toggle

**D — Hour/minute preservation on re-pick (s3b QUICK-S3B-02 regression guard):**
1. With Date=May 15, 2026 and Time=14:30 saved
2. Tap **Event Date** → pick May 22, 2026 → OK
3. Expected: Event Date now reads "May 22, 2026" AND Event Time still reads "14:30" (NOT cleared, NOT reset to 00:00)
4. The date picker should pre-highlight May 15 when reopened (initialSelectedDateMillis seeded correctly across UTC boundary).

**E — Off-by-one civil-day guard (UTC↔local conversion):**
1. With Date=May 15, 2026 saved, tap **Event Date** to reopen
2. Expected: The picker pre-highlights **May 15** (NOT May 14 or May 16). On Bucharest UTC+2/+3 this is the most likely place a UTC bug would surface.

**F — Edit Registry round-trip:**
1. Tap **Continue · add items →** to save the registry, then add at least 1 item
2. Home → tap the registry → on Detail screen tap edit/pencil affordance → land on Edit Registry
3. Expected: Event Date and Event Time fields show the previously-saved values
4. Tap Event Date → re-pick a different date → OK → tap **Update registry** → re-open Edit
5. Expected: New date persists; time preserved

**G — RO locale spot-check:**
1. Settings → app language → Romanian
2. Re-open Create Registry → tap Event Date
3. Expected: Picker labels (month names, weekday letters, "OK", "Anulează") all in Romanian. M3 sources these from the system; this is a pure inheritance check.

**H — No regressions on prior shipped fixes:**
1. Bottom nav still 5-slot, ADD label aligned with HOME/STORES/LISTS/YOU
2. Add sheet still 2 rows (New registry + Add an item)
3. Registry Detail FAB still goes to Add Item with current registry pre-selected
4. Cover-photo picker on Create Registry still opens its own sheet (not collided with Date picker)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Forward-declaration of `showDatePicker` / `showTimePicker` flags**
- **Found during:** Task 1 first compile pass
- **Issue:** Plan §2 instructed to add the flags "Just below the existing `var skipMode by remember { mutableStateOf(false) }` line (around L184)". But Plan §3 and §4 rewrite the `LaunchedEffect(dateInteractionSource)` / `LaunchedEffect(timeInteractionSource)` collectors at L153/L165 to reference `showDatePicker = true` / `showTimePicker = true`. Kotlin forward-reference rules require the `var` declarations to come BEFORE the closures that capture them, even in a linear `@Composable` body. Compile failed with `Unresolved reference 'showDatePicker'` at L156 and `Unresolved reference 'showTimePicker'` at L168.
- **Fix:** Moved the two `var ... by remember { mutableStateOf(false) }` declarations to immediately AFTER the `dateInteractionSource` / `timeInteractionSource` `MutableInteractionSource` declarations, BEFORE the `LaunchedEffect` collectors. `skipMode`, `untitledDraftLabel`, and `pickerSheetOpen` stay where they were.
- **Files modified:** `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt`
- **Commit:** `08c66da` (folded into Task 1's atomic commit since the fix is part of getting the same file to compile)

No other deviations. Plan executed as written.

## Authentication gates

None. No external services or secrets touched.

## Deferred Issues (out of scope)

These pre-existing warnings appeared in the build output but are NOT introduced by this task and are NOT in `CreateRegistryScreen.kt`. Logged here per the scope-boundary rule:

- `GuestPreferencesDataStore.kt:24`, `LanguagePreferencesDataStore.kt:27`, `LastRegistryPreferencesDataStore.kt:24`, `OnboardingPreferencesDataStore.kt:26` — `@Inject` annotation-target warnings (KT-73255, future-proofing, not breaking)
- `AddActionSheet.kt:175`, `AddItemScreen.kt:487` — `Icons.Outlined.KeyboardArrowRight` deprecated, use `Icons.AutoMirrored.Outlined.KeyboardArrowRight`
- `OnboardingScreen.kt:64` — `Icons.Filled.FormatListBulleted` deprecated, use AutoMirrored
- `RegistryDetailHero.kt:181` — `Icons.Filled.OpenInNew` deprecated, use AutoMirrored
- `RegistryDetailScreen.kt:90`, `ShareBanner.kt:48` — `LocalClipboardManager` deprecated, use `LocalClipboard` (suspend-supporting)

None block this task. They predate this work and would each be a separate quick task.

## Cross-links

- **s3b SUMMARY** (`./260428-s3b-fix-event-date-time-fields-on-createregi/260428-s3b-SUMMARY.md`) — picker-trigger lineage: InteractionSource pattern + `eventTimeSet` StateFlow + `setEventTime` VM API. tx9 preserves all of it verbatim.
- **Phase 08 GiftMaison design foundation** — `Theme.kt:36` `LightColorScheme.primary = gm.accent`. The reason M3 picker defaults inherit terracotta with zero overrides.
- **Theme.kt** — referenced by both `MaterialTheme` and `GiftMaisonTheme.colors`; tx9 changes nothing here.

## Self-Check: PASSED

- File exists: `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` — FOUND
- Commit `08c66da` (Task 1) — FOUND in `git log --oneline -3`
- `import android.app.DatePickerDialog` / `TimePickerDialog` — ABSENT (Grep returned no matches)
- All 7 M3 imports — PRESENT (Grep returned 7 matches)
- `@OptIn(ExperimentalMaterial3Api::class)` — PRESENT (line 96)
- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (no errors from CreateRegistryScreen.kt)
- Task 1 atomically committed (1 file, 142+/42-) — VERIFIED
- No bundling with unrelated work — VERIFIED via `git log --oneline -3`: commit 08c66da is solely the picker swap; preceding commits are s3b SUMMARY and s3b impl

## Known Stubs

None. No new placeholders, hardcoded empty values, or "not available" text introduced. UI flows that previously worked continue to work; the picker chrome simply renders in a different palette.
