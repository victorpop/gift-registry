---
phase: quick-260605-g6x
plan: 01
subsystem: android-ui
tags: [registry-detail, date-formatting, localization]
requires:
  - "Registry.eventDateMs encoding date+time as one timestamp (eventTimeSet convention)"
provides:
  - "Details date pill that conditionally appends localized event time after the date"
affects:
  - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailsSection.kt"
tech-stack:
  added: []
  patterns:
    - "DateFormat.is24HourFormat(context) for locale-aware 12/24h time formatting (mirrors CreateRegistryScreen)"
    - "non-midnight timestamp heuristic = a time was set (eventTimeSet convention from quick-260428-s3b)"
key-files:
  created: []
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailsSection.kt"
decisions:
  - "Reuse the existing eventTimeSet heuristic (non-midnight ⇒ time set) rather than add a persisted flag — no model/ViewModel changes"
  - "Format date/time at runtime via SimpleDateFormat; no strings.xml keys added"
metrics:
  duration: 3min
  completed: 2026-06-05
---

# Quick 260605-g6x: Add Event Time to Registry Details Date Pill Summary

Registry Details date pill now appends the locale-aware event time ("MMM d · HH:mm" / "MMM d · h:mm a") when the stored timestamp is non-midnight, and stays date-only ("MMM d") when it is midnight or null.

## What Changed

`RegistryDetailsSection.kt` — the `formatEventDate` helper was widened to `(Long, Boolean)`:
- Decodes the timestamp via `Calendar`; `hasTime = HOUR_OF_DAY != 0 || MINUTE != 0`.
- Date-only timestamps (midnight) return `"MMM d"` unchanged.
- Non-midnight timestamps append `" · "` plus the time, using `HH:mm` (24h) or `h:mm a` (12h).
- The composable derives `is24Hour` from `LocalContext.current` + `DateFormat.is24HourFormat(context)` and passes it into the helper, mirroring the create-registry screen convention.

The empty-block early-return, DETAILS header, description block, and location card were left byte-identical.

## Verification

- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
- `git diff --name-only` lists exactly one file: `RegistryDetailsSection.kt`.
- No new keys in `values/strings.xml` or `values-ro/strings.xml`.
- Code-trace of the three cases:
  - `eventDateMs == null` → `hasDate` false → pill not rendered.
  - midnight (00:00) → `hasTime` false → returns `"MMM d"`.
  - non-midnight → `hasTime` true → returns `"MMM d · HH:mm"` (24h) or `"MMM d · h:mm a"` (12h).

## Deviations from Plan

None — plan executed exactly as written.

## Notes

- The build required copying the gitignored `local.properties` and `app/google-services.json` from the main checkout into the worktree to satisfy Gradle (SDK location + Firebase config). These remain gitignored and were NOT committed.
- Pre-existing `Unnecessary non-null assertion (!!)` warnings on lines 98/107/131 are unrelated to this change (description/location/early-return null assertions) and were left untouched per scope boundary.

## Commits

- `7268d1d`: feat(quick-260605-g6x): append locale-aware event time to Details date pill

## Self-Check: PASSED
