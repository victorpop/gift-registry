---
phase: 01-firebase-foundation
plan: 03
subsystem: infra
tags: [i18n, android, strings, firebase-hosting, assetlinks, localization, romanian]

# Dependency graph
requires:
  - phase: 01-firebase-foundation/01-01
    provides: firebase.json with hosting config pointing to hosting/public/

provides:
  - Android strings.xml (EN + RO) with 18 keys each using feature-namespaced convention
  - Web i18n JSON files (en.json, ro.json) with matching nested key structure (i18next)
  - assetlinks.json placeholder at hosting/public/.well-known/ for Android App Links
  - hosting/public/index.html placeholder for Firebase Hosting
  - Feature-namespaced key convention: app_, common_, auth_, registry_, reservation_

affects: [02-auth-android, 03-registry-android, 04-reservation-flow, 05-web-fallback]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Android i18n: res/values/strings.xml (EN default) + res/values-ro/strings.xml (RO)"
    - "Web i18n: i18next nested JSON format with top-level namespace keys"
    - "Feature-namespaced key convention: feature_screen_element snake_case"
    - "i18next interpolation: {{variable}} syntax (not Android %1$d format)"

key-files:
  created:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
    - web/i18n/en.json
    - web/i18n/ro.json
    - hosting/public/.well-known/assetlinks.json
    - hosting/public/index.html
  modified: []

key-decisions:
  - "Feature-namespaced key convention established: app_, common_, auth_, registry_, reservation_ prefixes prevent key collision across features"
  - "assetlinks.json uses PLACEHOLDER values for package_name and sha256_cert_fingerprints — will be updated in Phase 2 (Android scaffold) and Phase 5 (App Links)"
  - "i18next {{variable}} interpolation for web vs Android %1$d format — different syntax per platform"

patterns-established:
  - "Pattern: All new UI strings added to BOTH strings.xml files (EN + RO) simultaneously — no orphaned keys"
  - "Pattern: Web i18n keys mirror Android key structure using dot-notation nesting (auth.sign_in_title vs auth_sign_in_title)"

requirements-completed: [I18N-02]

# Metrics
duration: 3min
completed: 2026-04-04
---

# Phase 01 Plan 03: i18n String Resources and Hosting Assets Summary

**Android strings.xml (EN+RO) and web i18n JSON (en.json+ro.json) with feature-namespaced keys, plus assetlinks.json and hosting placeholder**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-04T17:25:10Z
- **Completed:** 2026-04-04T17:28:45Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Android string resources created for English (default) and Romanian with 18 matching keys each using feature-namespaced convention
- Web i18n JSON files created in i18next nested format with 5 top-level namespaces (app, common, auth, registry, reservation) matching between EN and RO
- assetlinks.json placeholder created at correct hosting path for future Android App Links verification in Phase 5
- hosting/public/index.html placeholder ensures Firebase Hosting does not 404

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Android string resource files (I18N-02)** - `1e1c642` (feat)
2. **Task 2: Create web i18n files and hosting assets** - `90ca3b5` (feat)

**Plan metadata:** (pending)

## Files Created/Modified
- `app/src/main/res/values/strings.xml` - English Android string resources (18 keys, feature-namespaced)
- `app/src/main/res/values-ro/strings.xml` - Romanian Android string resources (18 matching keys)
- `web/i18n/en.json` - English web i18n in i18next nested JSON format (5 namespaces)
- `web/i18n/ro.json` - Romanian web i18n with identical key structure
- `hosting/public/.well-known/assetlinks.json` - Android App Links verification placeholder
- `hosting/public/index.html` - Firebase Hosting placeholder page

## Decisions Made
- Feature-namespaced key convention (`app_`, `common_`, `auth_`, `registry_`, `reservation_`) chosen to prevent key collision across features as the app grows — all future phases add keys under these prefixes
- assetlinks.json uses PLACEHOLDER values; will be updated in Phase 2 (package name from Android scaffold) and Phase 5 (SHA-256 fingerprint from signing config)
- i18next interpolation syntax `{{variable}}` used for web (vs Android `%1$d`) — documented as expected platform difference

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## Known Stubs
- `hosting/public/.well-known/assetlinks.json` - `package_name` is `ro.PLACEHOLDER.giftregistry` and `sha256_cert_fingerprints` is `PLACEHOLDER:SHA256:FINGERPRINT:GOES:HERE`. These are intentional placeholders; Phase 2 will supply the actual package name and Phase 5 will supply the certificate fingerprint.

## Next Phase Readiness
- I18N-02 satisfied: all UI labels stored in separate resource files
- Feature-namespaced key convention established for all future phases (2, 3, 4, 5) to follow
- All subsequent Android screens add keys to both strings.xml files simultaneously
- Web fallback (Phase 5) will wire i18n JSON to i18next library
- assetlinks.json will be finalized in Phase 5 (App Links configuration)

## Self-Check: PASSED

All files verified present:
- app/src/main/res/values/strings.xml: FOUND
- app/src/main/res/values-ro/strings.xml: FOUND
- web/i18n/en.json: FOUND
- web/i18n/ro.json: FOUND
- hosting/public/.well-known/assetlinks.json: FOUND
- hosting/public/index.html: FOUND
- .planning/phases/01-firebase-foundation/01-03-SUMMARY.md: FOUND

All commits verified:
- 1e1c642 (Task 1 - Android strings): FOUND
- 90ca3b5 (Task 2 - web i18n + hosting): FOUND

---
*Phase: 01-firebase-foundation*
*Completed: 2026-04-04*
