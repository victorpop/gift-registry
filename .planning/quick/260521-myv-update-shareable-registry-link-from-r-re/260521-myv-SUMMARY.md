---
phase: quick/260521-myv-update-shareable-registry-link-from-r-re
plan: 01
subsystem: ui
tags: [android, kotlin, share, deep-link, web-fallback, url]

# Dependency graph
requires:
  - phase: 11-registry-detail-create-add-item-redesign
    provides: "ShareBanner + shareUrlOf composable (introduced the Android share pill)"
  - phase: 05-web-fallback
    provides: "/registry/:id canonical route in web/src/App.tsx"
  - phase: 06-notifications-email-flows (260420-nh8)
    provides: "functions/src/config/publicUrls.ts::buildRegistryUrl returning /registry/{id}"
provides:
  - "Android share/copy flow now emits the canonical https://gift-registry-ro.web.app/registry/{registryId} URL"
  - "Removes the only remaining /r/{id} producer in production code; web router no longer 404s on Android-shared links"
affects: ["future share flows", "deep-link work", "phase 14 guest UAT (links shared during UAT-2/3/4 will now load RegistryPage instead of NotFoundPage)"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Canonical registry URL helper (shareUrlOf) is single source of truth for Android share/copy URL — display string derived at runtime, no second hardcoded string in ShareBanner.kt"

key-files:
  created: []
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt"
    - "app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt"
    - "app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt"

key-decisions:
  - "No web/Firebase Hosting redirect rule added — web app never had a /r/:id route, so /r/ links shared from Android were always broken; we fix the source rather than maintain a redirect for links that never worked"
  - "No web/functions/firebase.json changes — those code paths already emit /registry/{id} (verified by grep + existing unit tests in publicUrls.test.ts / emailTemplates.test.ts)"
  - "Renamed test placesRegistryIdAfterRSegment → placesRegistryIdAfterRegistrySegment for clarity now that the prefix is /registry/ (not /r/)"

patterns-established:
  - "Production-code URL contracts pinned by repo-wide grep + a single shareUrlOf builder — future share URL changes flip one constant and a small set of tests"

requirements-completed: [QUICK-260521-myv]

# Metrics
duration: 2min
completed: 2026-05-21
---

# Quick Task 260521-myv: Update shareable registry link from /r/ to /registry/

**Android shareUrlOf now returns https://gift-registry-ro.web.app/registry/{id} so the ShareBanner pill, clipboard copy, and ACTION_SEND share intent all match the web fallback router (web/src/App.tsx) and Cloud Functions buildRegistryUrl.**

## Performance

- **Duration:** 2min (1m 43s)
- **Started:** 2026-05-21T13:36:26Z
- **Completed:** 2026-05-21T13:38:42Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- `shareUrlOf("abc123")` now returns `https://gift-registry-ro.web.app/registry/abc123` (was `/r/abc123`)
- ShareBanner pill on RegistryDetailScreen now displays `gift-registry-ro.web.app/registry/{id}` and copies/shares the same URL
- 5/5 tests in `ShareUrlTest` pass (`./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.ShareUrlTest"`)
- `./gradlew :app:assembleDebug` succeeds
- Repo-wide grep confirms zero `web.app/r/` matches in `app/src/main`, `app/src/test`, `web/src`, `functions/src` (production code is /r/-free)

## Task Commits

Each task was committed atomically:

1. **Task 1: Flip ShareUrl.kt + ShareUrlTest.kt from /r/ to /registry/** — `db41de8` (fix)
2. **Task 2: Update ShareBanner.kt display-format comment + final repo-wide /r/ grep sweep** — `045d2fa` (chore)

_Note: Task 1 was a coupled test+production flip in a single atomic commit (RED→GREEN) since the test file is the authoritative spec for the URL format and both files form one semantic unit._

## Files Created/Modified

- `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt` — `shareUrlOf` return string flipped from `/r/$registryId` to `/registry/$registryId`; KDoc line about "web fallback router's /r/:id" updated to `/registry/:id`.
- `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` — KDoc example, KDoc CONTEXT.md reference, `returnsCorrectHostAndPath` assertion, `placesRegistryIdAfterRSegment` (renamed to `placesRegistryIdAfterRegistrySegment`) assertion, and `doesNotUrlEncodeRegistryId` inline comment all updated to `/registry/`.
- `app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt` — comment-only change on line 53; display URL is computed at runtime via `shareUrlOf().removePrefix("https://")`, so no logic change.

## Decisions Made

1. **No `firebase.json` redirect for `/r/:id` → `/registry/:id`.** The web app never registered a `/r/:id` route (`web/src/App.tsx` only defines `/registry/:id` and `/registry/:id/item/:itemId`). Any Android-shared `/r/{id}` link historically fell through to the `*` wildcard and rendered `NotFoundPage`. Adding a redirect would maintain a code path for links that never worked. The pure-Android fix is sufficient.
2. **No changes to `web/`, `functions/`, `firebase.json`, `AndroidManifest.xml`, or `hosting/`.** All canonical-side code already emits `/registry/{id}` (verified by repo grep + existing unit tests in `functions/src/__tests__/publicUrls.test.ts` and `emailTemplates.test.ts`).
3. **Renamed test `placesRegistryIdAfterRSegment` → `placesRegistryIdAfterRegistrySegment`.** Old name referenced the `/r/` segment which no longer exists; the new name is self-documenting for future readers.
4. **TDD atomic flip** rather than separate RED and GREEN commits. The test file is the spec for `shareUrlOf`'s return value; splitting the flip into two commits would leave a known-red commit in `main` for no benefit (no test scaffolding was already present; this is a value flip).

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Verification Proof

```
$ ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.ShareUrlTest"
BUILD SUCCESSFUL in 12s
  (all 5 tests pass)

$ grep -rn '"https://gift-registry-ro.web.app/r/\|web.app/r/' \
    app/src/main app/src/test web/src functions/src \
    --include='*.kt' --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.xml'
  (zero matches — exit 1)

$ grep -rn '"https://gift-registry-ro.web.app/registry/' \
    app/src/main app/src/test functions/src --include='*.kt' --include='*.ts'
  app/src/main/.../ShareUrl.kt:16
  app/src/test/.../ShareUrlTest.kt:10
  app/src/test/.../ShareUrlTest.kt:21
  functions/src/__tests__/publicUrls.test.ts:23
  functions/src/__tests__/emailTemplates.test.ts:64
  functions/src/__tests__/emailTemplates.test.ts:106

$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 9s
```

Note: `.planning/phases/11-*` and the older `260507-veb` quick task still contain `/r/` references — these are historical archived records and were intentionally left untouched per plan instructions.

## User Setup Required

None — pure Android source change, no env vars, no dashboard config, no deployment required. The change ships in the next Android release; existing installed debug builds will continue to emit the old URL until rebuilt/reinstalled.

## Recommended Manual Smoke (optional)

Not a gating step, but recommended after merge / next debug install:

1. Launch debug APK, sign in as a registry owner.
2. Open RegistryDetailScreen for any owned registry.
3. Tap the share-banner pill.
4. Confirm the system share-sheet preview and the clipboard now read `https://gift-registry-ro.web.app/registry/{id}` (not `/r/{id}`).
5. Paste the copied URL into Chrome — it should load `RegistryPage` (not `NotFoundPage`).

## Next Phase Readiness

- Unblocks any link-sharing path of Phase 14 guest UAT (UAT-2 / UAT-3 / UAT-4): links generated from the Android app during UAT will now resolve to the canonical `RegistryPage` rather than falling through to `NotFoundPage`.
- No follow-up tasks created; this closes the regression entirely.

## Self-Check: PASSED

- File exists: `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt` — FOUND
- File exists: `app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt` — FOUND
- File exists: `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` — FOUND
- Commit `db41de8` (Task 1) — to verify below
- Commit `045d2fa` (Task 2) — to verify below

---
*Quick task: 260521-myv-update-shareable-registry-link-from-r-re*
*Completed: 2026-05-21*
