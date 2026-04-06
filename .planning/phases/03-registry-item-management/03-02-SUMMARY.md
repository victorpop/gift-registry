---
phase: 03-registry-item-management
plan: 02
subsystem: api
tags: [firebase-functions, cloud-functions, og-metadata, node-html-parser, typescript]

# Dependency graph
requires:
  - phase: 01-firebase-foundation
    provides: Cloud Functions scaffold (index.ts, healthCheck, functions/package.json with firebase-admin and firebase-functions/v2)
provides:
  - fetchOgMetadata onCall callable (europe-west3): accepts URL, returns title/imageUrl/price/siteName or null fallback
  - inviteToRegistry onCall callable (europe-west3): writes to invitedUsers map, distinguishes existing vs new users, stubs email
affects: [03-03, 03-04, 06-email-notifications, android-item-add-flow]

# Tech tracking
tech-stack:
  added: [node-html-parser ^7.1.0]
  patterns:
    - "OG callable pattern: onCall with region, input validation, null-fallback on any error"
    - "Email stub pattern: console.log([STUB]) with Phase 6 comment for deferred email delivery"
    - "invitedUsers map key convention: UID for existing users, email: prefix for non-users"

key-files:
  created:
    - functions/src/registry/fetchOgMetadata.ts
    - functions/src/registry/inviteToRegistry.ts
  modified:
    - functions/src/index.ts
    - functions/package.json

key-decisions:
  - "inviteToRegistry uses email: prefix for non-user invite keys to prevent collision with Firebase UIDs in invitedUsers map"
  - "fetchOgMetadata returns null fields (not HttpsError) on fetch failure so Android client can fall back to manual entry without error handling overhead"
  - "node-html-parser chosen over cheerio for smaller bundle size and no DOM dependency in Node.js 22 runtime"

patterns-established:
  - "Cloud Functions callables: always use region: europe-west3 matching Firebase project region"
  - "Callable error handling: HttpsError for validation/auth failures, null return for recoverable data-fetch failures"

requirements-completed: [ITEM-02, REG-05, REG-06, REG-07]

# Metrics
duration: 5min
completed: 2026-04-06
---

# Phase 03 Plan 02: Cloud Functions — OG Metadata and Registry Invite Summary

**fetchOgMetadata callable parses Open Graph tags from any URL via node-html-parser; inviteToRegistry callable writes to invitedUsers map with UID/email-prefix keys and stubs Phase 6 email delivery**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-04-06T09:36:00Z
- **Completed:** 2026-04-06T09:37:24Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- fetchOgMetadata onCall callable: fetches HTML with 5s timeout, parses OG title/image/price/siteName, returns null fields on any failure
- inviteToRegistry onCall callable: enforces auth + ownership, resolves email to UID via admin.auth(), writes invitedUsers map, stubs email
- Both callables exported from functions/src/index.ts; TypeScript build passes with zero errors

## Task Commits

Each task was committed atomically:

1. **Task 1: fetchOgMetadata Cloud Function callable** - `c0b3bdb` (feat)
2. **Task 2: inviteToRegistry Cloud Function callable (stub)** - `c3ee3a7` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified

- `functions/src/registry/fetchOgMetadata.ts` - OG metadata extraction onCall callable
- `functions/src/registry/inviteToRegistry.ts` - Registry invite onCall callable with email stub
- `functions/src/index.ts` - Updated with fetchOgMetadata and inviteToRegistry exports
- `functions/package.json` - Added node-html-parser ^7.1.0 dependency

## Decisions Made

- inviteToRegistry uses `email:` prefix for non-user invite keys to avoid collision with Firebase Auth UIDs in the `invitedUsers` map
- fetchOgMetadata returns null fields (not HttpsError) on fetch failure — Android client falls back to manual entry without needing error handling overhead
- node-html-parser chosen over cheerio for smaller footprint in Node.js 22 Cloud Functions runtime

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- npm cache permission error on initial `npm install node-html-parser` — resolved by passing `--cache /tmp/npm-cache-$(whoami)` flag.

## Known Stubs

- `functions/src/registry/inviteToRegistry.ts` line ~65: `[STUB] Invite email would be sent` — email delivery not implemented; Phase 6 will wire actual SendGrid/Nodemailer delivery for both REG-06 (existing user) and REG-07 (non-user) paths.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- fetchOgMetadata is ready to be called from Android AddItemScreen (Phase 3 Plan 4+)
- inviteToRegistry is ready to be called from Android RegistrySettingsScreen (Phase 3 Plan 5+)
- Email delivery for invites remains a stub — must be completed in Phase 6 before production release

## Self-Check: PASSED

- FOUND: functions/src/registry/fetchOgMetadata.ts
- FOUND: functions/src/registry/inviteToRegistry.ts
- FOUND: .planning/phases/03-registry-item-management/03-02-SUMMARY.md
- FOUND commit: c0b3bdb (Task 1)
- FOUND commit: c3ee3a7 (Task 2)

---
*Phase: 03-registry-item-management*
*Completed: 2026-04-06*
