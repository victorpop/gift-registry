---
phase: 08-giftmaison-design-foundation
plan: 01
subsystem: ui-theme
tags: [compose, google-fonts, theming, tdd, wave-0, des-01, des-02, des-03, des-04, des-05]

# Dependency graph
requires:
  - phase: 02-android-core-auth
    provides: MaterialTheme baseline + GiftRegistryTheme composable wrapper at ui/theme/Theme.kt
provides:
  - androidx.compose.ui:ui-text-google-fonts dependency resolved via Compose BOM on debugRuntimeClasspath
  - R.array.com_google_android_gms_fonts_certs resource (Google Fonts provider cert hashes, dev+prod)
  - 5 RED theme unit test files under app/src/test/java/com/giftregistry/ui/theme/ encoding the handoff contract
  - Failing-symbol references for Wave 1-2 to implement (housewarmingColors, giftMaisonFontProvider, InstrumentSerifFamily, InterFamily, JetBrainsMonoFamily, giftMaisonTypography, giftMaisonShapes, giftMaisonSpacing, GiftMaisonShadows, wordmarkAnnotatedString)
affects: [08-02-colors, 08-03-fonts-typography, 08-04-shapes-spacing-shadows, 08-05-wordmark-theme-rewire, phase-9-nav-chrome, phase-10-owner-screens]

# Tech tracking
tech-stack:
  added:
    - "androidx.compose.ui:ui-text-google-fonts (BOM-pinned at 1.10.5 via compose-bom 2026.03.00)"
  patterns:
    - "TDD RED-first for a foundation phase: test files reference not-yet-existing symbols to force Waves 1-2 to implement against a handoff contract"
    - "Google Fonts provider cert hashes (dev + prod) live in res/values-v23/font_certs.xml, copy-paste identical to the Jetchat sample"
    - "Pure-Kotlin factory + @Immutable data class token pattern — testable without a Compose test runtime (unit tests assert on AnnotatedString shape, Color hex, TextStyle fields, Shape/Dp equality)"

key-files:
  created:
    - app/src/main/res/values-v23/font_certs.xml
    - app/src/test/java/com/giftregistry/ui/theme/ColorsTest.kt
    - app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt
    - app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt
    - app/src/test/java/com/giftregistry/ui/theme/ShapesAndDimensTest.kt
    - app/src/test/java/com/giftregistry/ui/theme/WordmarkTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts

key-decisions:
  - "Placed the compose-ui-text-google-fonts alias alphabetically after coil-network-okhttp (c-o-i before c-o-m) as specified in the plan — follows existing version-catalog ordering convention"
  - "TypographyTest.kt includes an explicit Pitfall 2 guard (everyRole_disablesFontPadding_pitfall2) that asserts platformStyle == PlatformTextStyle(includeFontPadding = false) on all 10 type roles — prevents em lineHeight inflation from default font padding"
  - "WordmarkTest asserts against a pure-Kotlin factory wordmarkAnnotatedString(ink: Color, accent: Color): AnnotatedString so Wave 2 must factor the @Composable wordmark over this testable helper"

patterns-established:
  - "Wave 0 RED pattern: every phase requirement gets an automated failing test before any implementation exists. Waves 1-2 must flip each test from RED to GREEN, proving the implementation satisfies the handoff contract"
  - "Plan ships both dependency wiring AND the RED tests in one wave so downstream waves have a pre-flight sampling harness the moment they begin"

requirements-completed: []
# NOTE: Plan 08-01's frontmatter lists requirements: [DES-01..05], but this Wave 0 plan
# ONLY lays the prerequisites + RED stubs — it does NOT yet satisfy any DES-* requirement.
# Waves 1-2 (08-02, 08-03, 08-04, 08-05) ship the production code that turns the RED tests
# GREEN; those plans will mark the individual DES-* IDs complete as they land. Per
# deviation Rule 1 (plan-authoring imprecision), requirements mark-complete was NOT
# called for this plan — DES-01..05 remain "Pending" in REQUIREMENTS.md traceability.

# Metrics
duration: 5 min
completed: 2026-04-20
---

# Phase 8 Plan 1: Wave 0 — Google Fonts prerequisite + 5 RED theme tests Summary

**BOM-pinned androidx.compose.ui:ui-text-google-fonts wired in, font_certs.xml (dev+prod provider hashes) added under values-v23, and 5 unit tests authored against the GiftMaison handoff contract that compile-fail today on exactly 10 missing symbols — a deterministic RED→GREEN harness for Waves 1-2.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-20T16:33:00Z
- **Completed:** 2026-04-20T16:38:48Z
- **Tasks:** 2
- **Files modified:** 8 (2 edited + 6 created)

## Accomplishments

- **Compose Google Fonts module resolves on debug classpath.** `androidx.compose.ui:ui-text-google-fonts:1.10.5` is transitively pinned by Compose BOM 2026.03.00 — no version hard-coded in `libs.versions.toml`.
- **Google Fonts provider certificate resource compiles.** `res/values-v23/font_certs.xml` was verified via `./gradlew :app:processDebugResources` (BUILD SUCCESSFUL); `R.array.com_google_android_gms_fonts_certs` will be available to Wave 1's `GoogleFont.Provider(...)`.
- **5 RED theme test files compile-fail with exactly the expected missing symbols.**
  - ColorsTest → unresolved `housewarmingColors`
  - FontsTest → unresolved `giftMaisonFontProvider`, `InstrumentSerifFamily`, `InterFamily`, `JetBrainsMonoFamily`
  - TypographyTest → unresolved `giftMaisonTypography`, plus cascading `InstrumentSerifFamily`/`InterFamily`/`JetBrainsMonoFamily`
  - ShapesAndDimensTest → unresolved `giftMaisonShapes`, `giftMaisonSpacing`, `GiftMaisonShadows`
  - WordmarkTest → unresolved `wordmarkAnnotatedString`
- **Pitfall 2 guard wired in early.** `TypographyTest.everyRole_disablesFontPadding_pitfall2` will catch any future regression where an author forgets `PlatformTextStyle(includeFontPadding = false)` on a type role.

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire ui-text-google-fonts dependency + create font_certs.xml resource** — `d333412` (chore)
2. **Task 2: Create 5 failing theme unit test files (one per DES-* requirement)** — `9aea617` (test)

_Plan metadata commit appended separately below via `docs(08-01): complete wave-0 plan`._

## Files Created/Modified

- `gradle/libs.versions.toml` — Added `compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }` alias (no `version.ref`; BOM-pinned)
- `app/build.gradle.kts` — Added `implementation(libs.compose.ui.text.google.fonts)` alongside other Compose deps
- `app/src/main/res/values-v23/font_certs.xml` — Created. Two `<string-array>` (dev + prod) with the Google Play Services font-provider certificates, and one top-level `<array name="com_google_android_gms_fonts_certs">` that aggregates them
- `app/src/test/java/com/giftregistry/ui/theme/ColorsTest.kt` — 13 `@Test` methods asserting the 13 Housewarming sRGB hex tokens verbatim from the handoff
- `app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt` — 5 `@Test` methods asserting provider authority/package + non-null FontFamilys
- `app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt` — 11 `@Test` methods (10 type roles + Pitfall 2 guard) asserting fontSize in sp, letterSpacing in em, lineHeight in em, FontWeight, and FontFamily per role
- `app/src/test/java/com/giftregistry/ui/theme/ShapesAndDimensTest.kt` — 20 `@Test` methods (7 radii + 10 spacing tokens + 3 shadow elevations)
- `app/src/test/java/com/giftregistry/ui/theme/WordmarkTest.kt` — 5 `@Test` methods asserting the pure-Kotlin `wordmarkAnnotatedString(ink, accent): AnnotatedString` contract (text = "giftmaison.", 2 SpanStyles, deterministic)

## Decisions Made

- **RED tests reference a pure-Kotlin factory `wordmarkAnnotatedString(ink: Color, accent: Color)`, not a `@Composable`.** This commits Wave 2 to factoring the `GiftMaisonWordmark @Composable` over a testable plain-Kotlin helper — so the AnnotatedString shape can be unit-asserted without a Compose test runtime.
- **BOM-pinning (no explicit version).** The `compose-ui-text-google-fonts` alias deliberately omits `version.ref`. BOM 2026.03.00 resolves it to 1.10.5, exactly aligned with the other `androidx.compose.ui:*` modules the project already consumes.
- **Place `implementation(libs.compose.ui.text.google.fonts)` immediately after `material-icons-extended`.** Keeps the Compose dependencies clustered in `app/build.gradle.kts`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Plan verify assertion off-by-2] `font_certs.xml` grep count**
- **Found during:** Task 1 acceptance verification
- **Issue:** The plan's `<automated>` check asserts `grep -c "com_google_android_gms_fonts_certs" app/src/main/res/values-v23/font_certs.xml` returns exactly `3`. The plan-provided file content actually produces `5` matches (1 top-level `<array name>`, 2 inner `<item>@array/..._dev/prod</item>` references, 2 `<string-array name>`). The plan author miscounted the `<item>@array/...</item>` cross-references in the top-level array.
- **Fix:** Did NOT modify the file (the file content was specified verbatim in the plan and the acceptance criterion "File `app/src/main/res/values-v23/font_certs.xml` exists" is met). The functional requirement — that `R.array.com_google_android_gms_fonts_certs` resolve at compile time — was independently verified via `./gradlew :app:processDebugResources` returning BUILD SUCCESSFUL.
- **Files modified:** None (plan's verify was imprecise; file content follows the plan exactly)
- **Verification:** `./gradlew :app:processDebugResources` → BUILD SUCCESSFUL (the actual compile-time contract)
- **Committed in:** d333412 (Task 1 commit — no fix needed)

**2. [Rule 1 - Requirements prematurely listed] Did not mark DES-01..05 complete**
- **Found during:** State updates after SUMMARY creation
- **Issue:** Plan 08-01 frontmatter lists `requirements: [DES-01, DES-02, DES-03, DES-04, DES-05]`, but Wave 0 only ships RED stubs + the Google Fonts prerequisite — no production code that satisfies any DES-* requirement exists yet. Waves 1-2 will ship that code (08-02 → DES-03, 08-03 → DES-01/02, 08-04 → DES-04, 08-05 → DES-05).
- **Fix:** Did NOT call `requirements mark-complete`. Left `requirements-completed: []` in the SUMMARY frontmatter with a NOTE explaining the reasoning. The subsequent Wave 1/2 plans will mark their respective DES-* IDs complete as they land the production code. This preserves the truth of REQUIREMENTS.md traceability.
- **Files modified:** None
- **Verification:** `REQUIREMENTS.md` still lists DES-01..05 as "Pending" under Phase 8 traceability — correct state
- **Committed in:** (no code change)

---

**Total deviations:** 2 documented (2 plan-authoring imprecisions — neither required a code change; both are book-keeping corrections)
**Impact on plan:** Zero. All Task 1 and Task 2 acceptance criteria were met. The only corrections were (a) noting that the plan's `grep -c` expected-count was miscalculated and (b) declining to prematurely mark requirements complete. Both preserve the integrity of the artifact set.

## Issues Encountered

None. Both tasks completed on first pass. Gradle's compilation of the test target failed exactly as intended, with every missing symbol the plan predicted (plus cascading downstream errors that will naturally resolve once the missing types land in Waves 1-2).

## User Setup Required

None — no external services, credentials, or dashboard configuration needed. `font_certs.xml` contains PUBLIC Google-published certificate hashes, not secrets.

## Known Stubs

None. The 5 test files intentionally reference symbols that don't yet exist — that is a RED-first TDD artifact, not a UI stub.

## Next Phase Readiness

- **Ready for 08-02, 08-03, 08-04 (Wave 1, parallel).** All three plans can be executed simultaneously — they have non-overlapping `files_modified` per 08-VALIDATION.md Planner Notes, and each one now has a failing test waiting to be turned GREEN.
  - 08-02 → lands `housewarmingColors()` in `GiftMaisonColors.kt` → `ColorsTest` passes
  - 08-03 → lands `giftMaisonFontProvider`, `InstrumentSerifFamily`, `InterFamily`, `JetBrainsMonoFamily`, `giftMaisonTypography()` → `FontsTest` + `TypographyTest` pass
  - 08-04 → lands `giftMaisonShapes()`, `giftMaisonSpacing()`, `GiftMaisonShadows` → `ShapesAndDimensTest` passes
- **Ready for 08-05 (Wave 2, depends on all of Wave 1).** Will land `wordmarkAnnotatedString()` + rewire `GiftRegistryTheme` to inject Housewarming tokens into Material3 ColorScheme → `WordmarkTest` passes + full app build succeeds with terracotta re-skin.
- **No blockers.** `R.array.com_google_android_gms_fonts_certs` resolves; `ui-text-google-fonts` is on the classpath; no authentication, environment, or tooling gate encountered.

## Self-Check: PASSED

- `[ -f app/src/main/res/values-v23/font_certs.xml ]` → FOUND
- `[ -f app/src/test/java/com/giftregistry/ui/theme/ColorsTest.kt ]` → FOUND
- `[ -f app/src/test/java/com/giftregistry/ui/theme/FontsTest.kt ]` → FOUND
- `[ -f app/src/test/java/com/giftregistry/ui/theme/TypographyTest.kt ]` → FOUND
- `[ -f app/src/test/java/com/giftregistry/ui/theme/ShapesAndDimensTest.kt ]` → FOUND
- `[ -f app/src/test/java/com/giftregistry/ui/theme/WordmarkTest.kt ]` → FOUND
- `git log --oneline --all | grep d333412` → FOUND
- `git log --oneline --all | grep 9aea617` → FOUND
- `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -c ui-text-google-fonts` → 11 (≥ 1)
- `./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.theme.*'` → FAIL with Unresolved references for all 10 expected symbols

---
*Phase: 08-giftmaison-design-foundation*
*Completed: 2026-04-20*
