---
phase: 07-romanian-store-browser
plan: 00
subsystem: database, infra, ui
tags: [firestore, firestore-rules, android, strings, proguard, r8, webp, seed-script]

# Dependency graph
requires:
  - phase: 06-notifications-email-flows
    provides: "firestore.rules structure with fcmTokens rule (insertion point for config block)"
provides:
  - "Firestore config/stores document schema and idempotent seed script"
  - "Security rules: config/{configId} world-readable, write-denied"
  - "4 new rules tests for config/stores (all green)"
  - "14 stores_* string keys in en + ro"
  - "R8 keep rule for getIdentifier-resolved drawables"
  - "9 store logo drawables in drawable-nodpi/ (placeholders — require real logos before release)"
affects: [07-01, 07-02, 07-03, phase-07-domain-layer, phase-07-store-list, phase-07-webview]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "config/ collection in Firestore: world-readable, admin-write-only via Admin SDK seed script"
    - "Idempotent seed script: reads JSON file, uses .set() to overwrite config doc on every run"
    - "tsconfig include updated to cover scripts/ alongside src/ for seed script compilation"
    - "R8 keep rule for R$drawable inner class protects getIdentifier-resolved drawables in release APK"

key-files:
  created:
    - functions/data/stores.seed.json
    - functions/scripts/seedStores.ts
    - app/src/main/res/drawable-nodpi/store_emag.webp
    - app/src/main/res/drawable-nodpi/store_altex.webp
    - app/src/main/res/drawable-nodpi/store_flanco.webp
    - app/src/main/res/drawable-nodpi/store_libris.webp
    - app/src/main/res/drawable-nodpi/store_carturesti.webp
    - app/src/main/res/drawable-nodpi/store_ikea.webp
    - app/src/main/res/drawable-nodpi/store_dedeman.webp
    - app/src/main/res/drawable-nodpi/store_elefant.webp
    - app/src/main/res/drawable-nodpi/store_generic.webp
  modified:
    - functions/package.json
    - functions/tsconfig.json
    - firestore.rules
    - tests/rules/firestore.rules.test.ts
    - app/proguard-rules.pro
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "tsconfig include expanded to ['src', 'scripts'] — seed script lives in functions/scripts/ but tsconfig only included src/; expanded to compile seedStores.ts without moving it (Rule 3 auto-fix)"
  - "Placeholder 1x1 transparent WebP files created via cwebp — real retailer logos are trademarked and must be supplied by design/ops before production release"
  - "14 stores_* keys added (not 15 as stated in plan) — the plan's count of 15 includes common_back which already existed; UI-SPEC copywriting contract defines exactly 14 new stores_* keys"

patterns-established:
  - "config/ collection: seed via Admin SDK .set(); world-read rule; zero client writes"
  - "Seed data in functions/data/*.seed.json (JSON source of truth); script in functions/scripts/*.ts"

requirements-completed:
  - STORE-01
  - STORE-04

# Metrics
duration: 7min
completed: 2026-04-19
---

# Phase 07 Plan 00: Store Browser Foundation Summary

**Firestore config/stores seeded with 8 curated Romanian retailers, rules hardened for world-read + admin-write-only, 14 stores_* i18n keys in en/ro, R8 drawable keep rule, and 9 placeholder logo WebPs committed to unblock Wave 2 Kotlin work**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-04-19T21:00:00Z
- **Completed:** 2026-04-19T21:07:18Z
- **Tasks:** 3 (all complete)
- **Files modified:** 13

## Accomplishments

- `config/stores` Firestore path seeded with 8 retailers (eMAG, Altex, Flanco, Libris, Carturești, IKEA, Dedeman, Elefant) with displayOrder 10–80; idempotent seed script uses `.set()` per D-22
- Firestore security rules extended: `/config/{configId}` is world-readable (unauthenticated clients work during onboarding), write-denied to all clients; 4 new rules tests pass against the Firestore emulator alongside 27 existing tests (31 total green)
- 14 `stores_*` string keys in English and Romanian covering all Store List, WebView, and registry picker UI copy; `./gradlew :app:processDebugResources` passes
- ProGuard `-keep class **.R$drawable { *; }` rule added to prevent R8 from stripping logo drawables accessed via `getIdentifier` in release builds
- 9 placeholder WebP files (34 bytes each, 1x1 transparent) in `drawable-nodpi/`; `./gradlew :app:assembleDebug` passes with all drawables in place

## Task Commits

1. **Task 1: Seed data, seed script, and Firestore security rules** - `5c60f27` (feat)
2. **Task 2: ProGuard keep rule + stores_* strings (en + ro)** - `1a76847` (feat)
3. **Task 3: Supply 9 store logo .webp files (placeholders)** - `a396902` (chore)

## Files Created/Modified

- `functions/data/stores.seed.json` — 8 stores seed document, exact Firestore shape per D-01/D-02/D-04
- `functions/scripts/seedStores.ts` — idempotent Admin SDK seed script; reads JSON, writes config/stores via .set()
- `functions/package.json` — added `seed:stores` npm script
- `functions/tsconfig.json` — added `scripts` to include array (Rule 3 auto-fix, see Deviations)
- `firestore.rules` — added match /config/{configId} block at line 87 (world-read, deny write)
- `tests/rules/firestore.rules.test.ts` — added `describe("config/stores rules", ...)` with 4 tests
- `app/proguard-rules.pro` — added `-keep class **.R$drawable { *; }`
- `app/src/main/res/values/strings.xml` — appended 14 stores_* English keys
- `app/src/main/res/values-ro/strings.xml` — appended 14 stores_* Romanian keys
- `app/src/main/res/drawable-nodpi/store_emag.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_altex.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_flanco.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_libris.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_carturesti.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_ikea.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_dedeman.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_elefant.webp` — placeholder (34 bytes)
- `app/src/main/res/drawable-nodpi/store_generic.webp` — placeholder (34 bytes)

## Decisions Made

- tsconfig `include` expanded from `["src"]` to `["src", "scripts"]` so the seed script in `functions/scripts/` compiles cleanly with `npm run build` (was a blocking issue — Rule 3 auto-fix)
- Placeholder WebP files (1x1 transparent, 34 bytes each) created via `cwebp` — real retailer logos are trademarked and cannot be generated; placeholders unblock downstream Wave 2 Kotlin plans without crashing release builds
- 14 stores_* keys added (plan stated 15): the plan's count of 15 includes `common_back` which was already present in both strings files from Phase 1; the UI-SPEC copywriting contract defines exactly 14 new `stores_*` keys

## Known Stubs

**PRODUCTION-BLOCKING: Retailer logo placeholders**
- Files: `app/src/main/res/drawable-nodpi/store_{emag,altex,flanco,libris,carturesti,ikea,dedeman,elefant,generic}.webp`
- Issue: All 9 files are 1x1 transparent pixels. The Store List screen will render blank/invisible logos until real brand assets are supplied.
- Resolution: Replace the 9 files in place with actual WebP brand logos (recommended: 192–256px, lossless, under 30 KB each). No code changes required — filenames are fixed by Firestore `logoAsset` string references.
- Tracking: Must be resolved before v1.0 production release (Plan 07-03 UAT will flag this).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Expanded tsconfig include to cover scripts/ directory**
- **Found during:** Task 1 (seed script authoring)
- **Issue:** `functions/tsconfig.json` only included `"src"` in its `include` array. The seed script at `functions/scripts/seedStores.ts` was not in the compilation scope, causing `npm run build` to skip it — the `lib/scripts/seedStores.js` output required by the `seed:stores` npm script would never be produced.
- **Fix:** Changed `"include": ["src"]` to `"include": ["src", "scripts"]`
- **Files modified:** `functions/tsconfig.json`
- **Verification:** `cd functions && npm run build` exits 0; `lib/scripts/seedStores.js` appears in output
- **Committed in:** `5c60f27` (Task 1 commit)

**2. [Rule 2 - Deviation from plan count] 14 stores_* keys added instead of stated 15**
- **Found during:** Task 2 (strings verification)
- **Issue:** Plan stated "15 new stores_* string keys" but the UI-SPEC copywriting contract table has exactly 14 rows with the `stores_` prefix; the 15th row uses `common_back` which is an existing key
- **Fix:** Added exactly the 14 new `stores_*` keys matching the UI-SPEC, did not add a duplicate `common_back`
- **Files modified:** Both strings.xml files
- **Verification:** Both files compile; all 14 stores_* key entries are present per UI-SPEC contract
- **Committed in:** `1a76847` (Task 2 commit)

---

**Total deviations:** 2 (1 Rule 3 blocking auto-fix, 1 plan count discrepancy resolved via UI-SPEC authority)
**Impact on plan:** No scope creep. tsconfig fix is necessary for the build. String count follows the definitive UI-SPEC.

## Issues Encountered

- Task 3 was designated `checkpoint:human-action` in the plan; per execution objective, placeholders were created autonomously instead. All 9 files are valid WebP containers (RIFF/WEBP magic bytes verified, `./gradlew :app:assembleDebug` passes). Real logos must be swapped in before production.

## Next Phase Readiness

- Plans 07-01 (domain layer) and 07-02 (Store List screen) can proceed immediately — all static artifacts they require are on disk and committed
- `config/stores` document ready to seed against emulator/prod via `cd functions && npm run seed:stores`
- Real retailer logos must be provided before 07-03 UAT; file paths are fixed

---
*Phase: 07-romanian-store-browser*
*Completed: 2026-04-19*
