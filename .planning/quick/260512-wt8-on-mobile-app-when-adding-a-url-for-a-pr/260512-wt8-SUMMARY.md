---
phase: quick-260512-wt8
plan: 01
subsystem: ui
tags: [android, compose, stateflow, debounce, og-metadata, accessibility, i18n]

# Dependency graph
requires:
  - phase: phase-07-affiliate-links
    provides: FetchOgMetadataUseCase + ItemRepository OG fetch path
  - phase: phase-04-add-item
    provides: AddItemViewModel / AddItemScreen paste-URL mode
provides:
  - Auto-fetch of OG metadata when a valid http(s) URL is typed or pasted into AddItemScreen (700ms debounce)
  - lastFetchedUrl de-dup contract so re-emissions of the same URL do not re-fetch
  - collectLatest-based cancellation of in-flight fetches when the URL changes mid-flight
  - Icons.Outlined.CloudDownload trailing icon (replaces misleading Refresh glyph) with localized contentDescription
  - 7-test pin file (AddItemViewModelAutoFetchTest) locking the auto-fetch contract
affects: [phase-04-add-item, phase-07-affiliate-links, future-onboarding-or-paste-flows]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Debounced auto-action StateFlow pipeline: source.drop(1).debounce(N).distinctUntilChanged().collectLatest { validatedAction(it) }"
    - "Manual-action bypass: same underlying function (onFetchMetadata) reused for both auto-fetch and IconButton retry; de-dup only applied inside the auto-flow collector"
    - "Init-block deep-link pattern: seed lastFetchedUrl BEFORE subscribing the auto-flow so existing one-shot fetches do not double-fire"

key-files:
  created:
    - app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "Pure debounce path — no clipboard-paste short-circuit (Compose OutlinedTextField cannot distinguish paste from type without a custom InputTransformation; the ~700ms cost is acceptable)"
  - "Icons.Outlined.CloudDownload chosen over Refresh because the action is 'fetch remote data', not 'reload the same thing' (semantic icon match)"
  - "Manual icon-tap path bypasses lastFetchedUrl de-dup — retry-after-error must always work even when the URL is unchanged"
  - "lastFetchedUrl is updated on BOTH success and failure inside onFetchMetadata, so a failed attempt does not auto-retry on the next identical re-emission"
  - "init block seeds lastFetchedUrl=initialUrl.trim() BEFORE subscribing the new flow to prevent double-firing on Store-Browser deep-link"

patterns-established:
  - "Debounced auto-action flow: collectLatest gives free cancellation + sequential per-emission semantics"
  - "Mockk Log.class static-stub in @Before / unmockk in @After for ViewModel unit tests that hit android.util.Log on JVM"

requirements-completed: [QUICK-WT8-01, QUICK-WT8-02]

# Metrics
duration: ~13min
completed: 2026-05-12
---

# Quick-260512-wt8: Paste-URL Auto-Fetch Summary

**AddItemScreen now auto-fetches OG metadata ~700ms after a valid http(s) URL is typed or pasted, with collectLatest cancellation, lastFetchedUrl de-dup, and a CloudDownload trailing icon for the manual-retry affordance.**

## Performance

- **Duration:** ~13 min (first commit 2026-05-12T20:44:10Z → human-verify approved 2026-05-12T20:56:21Z; on-device UAT excluded)
- **Started:** 2026-05-12T20:44:10Z
- **Completed:** 2026-05-12T20:56:21Z
- **Tasks:** 2/2 (1 auto + 1 human-verify checkpoint)
- **Files modified:** 5 (1 created, 4 modified)

## Accomplishments
- **Auto-fetch pipeline** wired into `AddItemViewModel.init {}` — `url.drop(1).debounce(700).distinctUntilChanged().collectLatest { ... }` with an http/https-only validity gate and a `lastFetchedUrl` de-dup tracker.
- **Manual-retry preserved** — the trailing IconButton still calls `onFetchMetadata()` directly and ignores the de-dup tracker, so a user can always force a retry after an error.
- **Icon swap** — `Icons.Default.Refresh` (misleading rotational glyph) replaced with `Icons.Outlined.CloudDownload` (semantic match for "fetch remote data"). New string `item_url_fetch_button_cd` added in EN ("Fetch product details") and RO ("Preia detaliile produsului") so the icon now has an accessible, localized contentDescription where it previously had `null`.
- **No regression in the Store-Browser deep-link path** — the init block seeds `lastFetchedUrl = initialUrl.trim()` BEFORE subscribing the auto-flow, so the existing one-shot fetch from a deep-link still fires exactly once.
- **7-test contract pin** in `AddItemViewModelAutoFetchTest.kt` locks: validity gate, non-http scheme rejection, debounce-to-single-fire, rapid-edit-final-value-only, same-URL de-dup, initialUrl single-fire, manual retry bypass.
- **11-scenario on-device UAT approved** by the user (icon visual, paste auto-fetch, typing debounce, garbage input, non-http schemes, de-dup, manual retry, mid-flight cancellation, Romanian content-description, deep-link single-fire, failure-path retry).

## Task Commits

Each task was committed atomically:

1. **Task 1 (RED): test scaffold + EN/RO strings** — `f80a2f6` (test)
2. **Task 1 (GREEN): auto-fetch pipeline + icon swap + Log.class test stub** — `ea7c843` (feat)
3. **Task 2: human-verify checkpoint** — no code commit; approval recorded inline in PLAN.md (`status="complete"`, `resolution="approved"`)

**Plan metadata commit:** added after this SUMMARY is written.

_Note: TDD task produced two commits (RED then GREEN); no refactor pass needed._

## Files Created/Modified

- `app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt` — **created**. 7 tests pinning the auto-fetch contract: invalid url, non-http scheme, valid-https-single-fire, rapid-edits-debounce-to-final, same-url-dedup, initialUrl-single-fire, manual-retry-bypass.
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` — auto-fetch flow added in init, `isValidProductUrl` helper, `lastFetchedUrl` field (read by auto-flow, written by `onFetchMetadata` on success+failure, reset by `onClearUrl` + `onResetForm`), `@OptIn(FlowPreview::class)` for `debounce`.
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` — trailing-icon imageVector flipped to `Icons.Outlined.CloudDownload`; `contentDescription` resolves `R.string.item_url_fetch_button_cd` (was hardcoded `null`); IconButton `onClick = viewModel.onFetchMetadata()` unchanged.
- `app/src/main/res/values/strings.xml` — new key `item_url_fetch_button_cd = "Fetch product details"`.
- `app/src/main/res/values-ro/strings.xml` — new key `item_url_fetch_button_cd = "Preia detaliile produsului"`.

## Decisions Made

- **No clipboard-paste short-circuit.** Compose's `OutlinedTextField.onValueChange` cannot distinguish paste from type without a custom `InputTransformation` or composition listener. The brief explicitly allowed skipping this and accepting the ~700ms debounce delay; that trade-off was taken.
- **CloudDownload (outlined) over Refresh (filled).** "Refresh" implied "reload the same thing"; the actual semantic is "fetch metadata from a remote URL." CloudDownload is available in the existing Compose BOM 2026.03.00 so no dependency was added.
- **Manual path bypasses de-dup.** The IconButton's `onClick = { viewModel.onFetchMetadata() }` is the user's escape hatch after a fetch failure — it MUST be allowed to re-fire even when the URL hasn't changed. De-dup is therefore a property of the auto-flow collector only, not of `onFetchMetadata()` itself.
- **`lastFetchedUrl` is updated on both success and failure.** A failed fetch marks the URL as "we tried it"; without this, the auto-flow could thrash on a broken URL. The user can still manually retry.
- **Seed `lastFetchedUrl` in init BEFORE the flow subscribes.** This is what guarantees the existing Store-Browser deep-link path stays a one-shot fetch (no double-fire from the new auto-flow observing the same initial value).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Static-mocked `android.util.Log` in test setup**
- **Found during:** Task 1 (GREEN phase — running `AddItemViewModelAutoFetchTest`).
- **Issue:** `AddItemViewModel.onFetchMetadata()` calls `Log.d(...)` and `Log.e(...)` on success/failure paths. On the JVM unit-test classpath there is no `android.util.Log` implementation, so the tests threw `RuntimeException: Method d in android.util.Log not mocked.`
- **Fix:** Added `mockkStatic(Log::class)` with `every { Log.d(any(), any()) } returns 0` and `every { Log.e(any(), any(), any()) } returns 0` in `@Before`; matching `unmockkStatic(Log::class)` in `@After`. Follows the existing pattern used elsewhere in the test tree.
- **Files modified:** `app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt`
- **Verification:** All 7 tests pass; no leaked static state (verified by running full `:app:testDebugUnitTest`).
- **Committed in:** `ea7c843` (rolled into the GREEN task commit; no separate commit).

---

**Total deviations:** 1 auto-fixed (Rule 3 — blocking)
**Impact on plan:** Negligible. Pure test-infrastructure fix; production behaviour unchanged. No scope creep.

## Issues Encountered

- None. The implementation matched the plan's `<action>` block step-by-step; both gradle verify commands passed first time after the Log.class mock was in place.

## User Setup Required

None — no external service configuration, no env vars, no dashboard changes. All changes are in-app code + bundled string resources.

## Verification

- `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.add.AddItemViewModelAutoFetchTest"` → **7/7 pass**
- `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.add.*"` → no regressions (AddItemModeTest, AddItemViewModelPickerFilterTest still green)
- `./gradlew :app:compileDebugKotlin` → succeeds (CloudDownload import + debounce/collectLatest/distinctUntilChanged/drop pipeline compile)
- **On-device UAT (Task 2):** 11/11 scenarios approved by user — icon CloudDownload renders, paste auto-fetch fires ~1s, typing debounces only after idle, garbage/non-http rejected, de-dup holds, manual retry bypasses, mid-flight cancellation correct, Romanian contentDescription announces, deep-link single-fires, failure path inline message + manual retry still work.

## Next Steps Ready

- Pattern reusable for any other StateFlow-driven auto-action (e.g., search-as-you-type, address autocomplete) — see `patterns-established`.
- No follow-up plans queued from this work.

## Self-Check: PASSED

Verified before writing this section:
- `app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt` exists
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` modified (contains `lastFetchedUrl` and `isValidProductUrl`)
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` modified (contains `Icons.Outlined.CloudDownload`)
- `app/src/main/res/values/strings.xml` contains `item_url_fetch_button_cd`
- `app/src/main/res/values-ro/strings.xml` contains `item_url_fetch_button_cd`
- Commits `f80a2f6` (test) and `ea7c843` (feat) exist in `git log`

---
*Phase: quick-260512-wt8*
*Completed: 2026-05-12*
