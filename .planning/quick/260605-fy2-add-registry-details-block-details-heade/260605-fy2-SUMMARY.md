---
phase: quick-260605-fy2
plan: "01"
subsystem: android-ui
tags: [registry-detail, compose, localization]
requires:
  - "RegistryDetailScreen LazyColumn (hero/stats items)"
  - "Registry model fields: eventDateMs, eventLocation, description"
  - "GiftMaisonTheme tokens (colors/typography/spacing/shapes)"
provides:
  - "RegistryDetailsSection composable (date pill + description + location card)"
  - "registry_details_section_title string (en + ro)"
affects:
  - "Registry detail screen layout (new Details block between hero and stats)"
tech-stack:
  added: []
  patterns:
    - "StatsStrip-style section label (monoCaps + inkFaint) reused for DETAILS header"
    - "Early-return empty gate to omit an entire block from composition"
    - "Date-only SimpleDateFormat(\"MMM d\") private helper"
key-files:
  created:
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailsSection.kt"
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
    - "app/src/main/res/values/strings.xml"
    - "app/src/main/res/values-ro/strings.xml"
decisions:
  - "Details block ungated on isOwner — shows to owners AND guests (LOCKED)"
  - "Date pill renders date only (MMM d), never time"
  - "Location string rendered as-is, wrapping naturally (no comma split / two-line layout)"
  - "Used material-icons-extended CalendarToday + Place (artifact already on classpath)"
metrics:
  duration: 1min
  completed: 2026-06-05
---

# Quick Task 260605-fy2: Registry Details Block Summary

Adds a localized "Details" block (date-only pill, description paragraph, location card) between the hero header and stats strip on the registry detail screen, visible to both owners and guests; the whole block self-hides when all three Registry fields are empty.

## What Was Built

- **Task 1** — Added `registry_details_section_title` to both `values/strings.xml` ("Details") and `values-ro/strings.xml` ("Detalii"), stored Title-case and rendered small-caps via `monoCaps` (mirrors StatsStrip labels). Commit `a278369`.
- **Task 2** — Created `RegistryDetailsSection.kt` and inserted `item(key = "details")` into the `RegistryDetailScreen` LazyColumn between "hero" and "stats". Commit `4f017aa`.

The composable computes `hasDate / hasDescription / hasLocation` up front, early-`return`s rendering nothing when all are false, otherwise renders a padded `Column` (`spacedBy(gap12)`) with the DETAILS header plus each populated sub-element:
- Date pill: `clip(pill)` + `accentSoft` background, leading `Icons.Outlined.CalendarToday` (accent tint), date-only text via private `formatEventDate` (`SimpleDateFormat("MMM d", Locale.getDefault())`).
- Description: `bodyM` / `inkSoft`, wraps naturally, no maxLines.
- Location card: `clip(radius12)` + `paperDeep` background + 1dp `line` border, top-aligned `Icons.Outlined.Place` (inkSoft), location string rendered as-is.

Section args use safe-call `registry?.field`; when `registry` is null all three are null → empty gate → nothing renders before data loads.

## Empty-handling trace (UI not runnable here)

- (a) all null/blank → early `return`, no Column/header composed.
- (b) only date → header + pill.
- (c) only description → header + paragraph.
- (d) only location → header + card.

Each `if (hasX)` guards exactly its sub-element; the `return` precedes the `Column`.

## Deviations from Plan

None — plan executed exactly as written. Icon choice resolved to `CalendarToday` (date) and `Place` (location); both ship in `material-icons-extended`, already declared in `app/build.gradle.kts`.

## Verification

- `grep` confirms `registry_details_section_title` in both locale files.
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (only JVM native-access warnings, no compile errors).
- `git diff` scope limited to the 4 files in `files_modified`; ViewModel, Registry model, hero, stats, tabs, item list untouched.

Build note: this worktree required copying gitignored `local.properties` and `app/google-services.json` from the main checkout to compile; both remain gitignored and were not committed.

## Self-Check: PASSED
