---
phase: quick/260510-oja
plan: 01
subsystem: data
tags: [android, firestore, kotlin, dto, custom-class-mapper, timestamp, reservation, tdd]

requires:
  - phase: 04-reservation
    provides: "Cloud Function createReservation canonical writer (functions/src/reservation/createReservation.ts) — source of truth for the Firestore item document reservation field shape"
provides:
  - Android `ItemDto` aligned with Cloud Function canonical schema (`reservedBy: String?`, `reservedAt: Timestamp?`, `expiresAt: Timestamp?`)
  - `ItemRepositoryImpl.toDomain` boundary conversion `Timestamp.toDate().time` → `Long?` ms preserving `Item.expiresAt: Long?` UI contract
  - `FirestoreDataSource.observeItems` cleaned up — manual `getTimestamp("expiresAt")` workaround removed (now dead code)
  - `ItemDtoSchemaTest` JVM regression suite (6 tests) pinning DTO field types and the mapper conversion
affects: [reservation, registry-detail, owner-flow, future-reserved-by-display]

tech-stack:
  added: []
  patterns:
    - "DTO ↔ domain boundary conversion: Firestore-native types in DTO, primitive-typed domain models, conversion in repository toDomain mapper"
    - "JVM unit-test pin for DTO type-shape via constructor named-args (compile-time gate, not runtime)"
    - "MockK + StateFlow + first() pattern for exercising private extension mappers without changing visibility (mirrors RegistryRepositoryImplObserveTest)"

key-files:
  created:
    - "app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt — 6-test schema regression pin (117 lines)"
  modified:
    - "app/src/main/java/com/giftregistry/data/model/ItemDto.kt — added reservedBy/reservedAt, retyped expiresAt → Timestamp?"
    - "app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt — toDomain converts Timestamp expiresAt → Long ms"
    - "app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt — removed dead getTimestamp workaround, body matches RegistryDto pattern"

key-decisions:
  - "Align Android DTO to Cloud Function canonical writer (not the reverse) — function is deployed, working, and source of truth per project constraints"
  - "Domain Item.kt left untouched (expiresAt: Long?) — keeps StatusChip/ReservedChip/RegistryItemRow/StyleGuidePreview consumers unchanged; smallest safe surface for a crash-fix"
  - "reservedBy/reservedAt intentionally NOT propagated to domain Item — DTO carries them for forward-compatibility; future 'reserved by {giver}' UI can promote them"
  - "Test 5/6 mapper exercise via MockK + StateFlow + first() rather than reflection or visibility relaxation — follows RegistryRepositoryImplObserveTest pattern already in repo"

patterns-established:
  - "DTO field types match Firestore wire format; conversions to domain primitives happen in repository toDomain extension"
  - "When CustomClassMapper crash points at FirestoreDataSource.toObject, fix at the DTO type rather than trying to compensate after with manual getTimestamp/getString"

requirements-completed:
  - QUICK-260510-oja

duration: ~25min
completed: 2026-05-07
---

# Quick Task 260510-oja: Android ItemDto Schema Mismatch Fix Summary

**Aligned Android `ItemDto` with the Cloud Function canonical schema (added `reservedBy: String?` + `reservedAt: Timestamp?`, retyped `expiresAt: Long?` → `Timestamp?`) so `FirestoreDataSource.observeItems` no longer crashes with `RuntimeException: Could not deserialize object` when an item carries server-written reservation fields.**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-05-07
- **Tasks:** 3 (2 auto + 1 human-verify checkpoint)
- **Files modified:** 4 (1 created, 3 modified)
- **Test counts:** 330/330 JVM tests pass (was 324; 6 new schema tests added)

## Accomplishments

- **Crash fix:** Registry detail screen no longer crashes for any owner who has had a giver reserve one of their items. The `CustomClassMapper: No setter/field for reservedBy/reservedAt` warnings and the `Failed to convert a value of type com.google.firebase.Timestamp to long (found in field 'expiresAt')` runtime exception are gone — confirmed by user on rebuilt APK against the previously-crashing registry.
- **Schema alignment:** `ItemDto` now matches what `functions/src/reservation/createReservation.ts:62-67` writes — `reservedBy: string`, `reservedAt: Timestamp` (FieldValue.serverTimestamp()), `expiresAt: Timestamp` (Timestamp.fromMillis()).
- **Boundary discipline:** `ItemRepositoryImpl.toDomain` does the `Timestamp` → `Long` ms conversion at the data→domain seam so the existing `Item.expiresAt: Long?` UI contract (StatusChip, ReservedChip, RegistryItemRow, StyleGuidePreview) is preserved verbatim.
- **Dead-code removal:** `FirestoreDataSource.observeItems` no longer carries the manual `doc.getTimestamp("expiresAt")?.toDate()?.time` workaround that crash analysis confirmed was never reached (`toObject` throws before `.copy(...)` runs). Body now matches the `RegistryDto` pattern used elsewhere in the same file.
- **Regression guard:** New JVM `ItemDtoSchemaTest` (6 tests) pins the DTO field types at compile time and the `Timestamp` → `Long` mapper conversion at runtime, so a future regression (someone re-typing `expiresAt: Long?` "to simplify") fails fast in `:app:testDebugUnitTest`.

## Task Commits

TDD cycle followed (RED → GREEN → human-verify):

1. **Task 1: RED — pin ItemDto schema + mapper conversion (6 failing tests)** — `c28bffb` (test)
2. **Task 2: GREEN — align ItemDto with Cloud Function reservation schema** — `1ed563b` (fix)
3. **Task 3: Human-verify checkpoint** — no commit (device walkthrough)

**Plan metadata:** committed alongside this SUMMARY.

## Files Created/Modified

- `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt` (created, 117 lines) — 6 tests: 3 DTO type-shape pins (`reservedBy: String?`, `reservedAt: Timestamp?`, `expiresAt: Timestamp?`), 1 default-null pin (all reservation fields default `null`), 2 mapper-conversion pins (non-null Timestamp → epoch-ms `Long`, null → null).
- `app/src/main/java/com/giftregistry/data/model/ItemDto.kt` (modified) — added `import com.google.firebase.Timestamp`; added `val reservedBy: String? = null`, `val reservedAt: Timestamp? = null` fields; retyped `val expiresAt: Long? = null` → `val expiresAt: Timestamp? = null`. KDoc cites the canonical writer (`createReservation.ts`) so future readers know which side owns the schema.
- `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt` (modified) — one-line change in private `ItemDto.toDomain(registryId)`: `expiresAt = expiresAt` → `expiresAt = expiresAt?.toDate()?.time` with inline comment explaining the boundary conversion.
- `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` (modified) — `observeItems` `mapNotNull` body simplified from `getTimestamp("expiresAt")?.toDate()?.time` + `doc.toObject(...)?.copy(id, expiresAt)` to `doc.toObject(ItemDto::class.java)?.copy(id = doc.id)` (matches RegistryDto pattern on lines 32, 60, 71).

## Automated Gates

| Gate | Command | Result |
| ---- | ------- | ------ |
| Schema regression suite | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest"` | 6/6 GREEN |
| Full Android JVM suite | `./gradlew :app:testDebugUnitTest` | 330/330 GREEN |
| Debug APK build | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| Canonical fields present | `grep -nE "(reservedBy\|reservedAt\|expiresAt: Timestamp)" .../ItemDto.kt` | 3 fields at lines 31-33 |
| Boundary conversion present | `grep -nE "expiresAt\?\.toDate\(\)\?\.time" .../ItemRepositoryImpl.kt` | match at line 73 |
| Dead workaround removed | `grep -n "doc.getTimestamp" .../FirestoreDataSource.kt` | (no matches) |

## RED → GREEN Cycle

- **RED (Task 1, commit c28bffb):** `ItemDtoSchemaTest.kt` written first; ran `./gradlew :app:testDebugUnitTest --tests *ItemDtoSchemaTest*` and observed Kotlin compile errors (`unresolved reference: reservedBy`, `unresolved reference: reservedAt`, `type mismatch: inferred type is Timestamp but Long? was expected`). Compile-failure RED gate confirmed — proves the test exercises the actual bug.
- **GREEN (Task 2, commit 1ed563b):** Three source edits in order (`ItemDto.kt` schema → `ItemRepositoryImpl.kt` mapper → `FirestoreDataSource.kt` cleanup). Re-ran test suite: 6/6 schema tests pass, 330/330 full JVM suite pass, `:app:assembleDebug` green.

## Human Verification

**Status:** approved.

User confirmed on device after rebuilding/installing the debug APK:
- Navigated to the previously-crashing registry containing a reserved item.
- No `RuntimeException: Could not deserialize object` crash.
- No `CustomClassMapper: No setter/field for reservedBy|reservedAt` Logcat warnings on the registry-detail flow.
- Other (non-reserved) items still render normally; reserved item shows StatusChip / ReservedChip with countdown.

VERIFICATION.md (status: passed, score 5/5 must-haves) written by gsd-verifier records the same evidence and adds Level 4 data-flow trace (Firestore snapshot → `ItemDto` → `Item.expiresAt: Long ms` → StatusChip/ReservedChip).

## Decisions Made

- **Domain `Item.kt` left untouched.** `expiresAt: Long?` stays on the domain model — `StatusChip`, `ReservedChip`, `RegistryItemRow`, and `StyleGuidePreview` already consume `Long?`. Changing the domain would force UI/test cascade work outside the crash-fix scope. Conversion happens in the DTO → domain mapper, which is the correct architectural seam.
- **`reservedBy` / `reservedAt` not propagated to domain.** No UI consumer exists yet; the DTO carries them for forward-compatibility. Flagged here for any future phase that wants to display "reserved by {giver}" in the owner UI — promote to `Item` then.
- **Test feasibility scope.** Robolectric / Firebase Emulator instrumented tests are NOT configured in this project. JVM-testable contract is (a) DTO field shapes accept Firestore-native types at construction time, (b) mapper converts Timestamp → Long ms correctly. The actual `CustomClassMapper` deserializer happiness is verified by the Task 3 device checkpoint — the crash itself is the live regression test until/unless Robolectric is added later.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] Substituted `Timestamp(Date(ms))` constructor in test fixtures**

- **Found during:** Task 1 (RED — `ItemDtoSchemaTest.kt` compile pass)
- **Issue:** The plan suggested `Timestamp.fromMillis(1_700_000_500_000L)` for fixture timestamps. On the project's pinned `com.google.firebase:firebase-firestore` version, `Timestamp.fromMillis` is package-private / not exposed on the JVM test classpath, causing a compile error in the test file.
- **Fix:** Used the publicly-accessible constructor `Timestamp(Date(1_700_000_500_000L))` (with `import java.util.Date`) which produces an equivalent `Timestamp` value for fixture purposes. The DTO accepts `Timestamp?` regardless of which constructor produced the value — the type contract is what's pinned, not the construction path.
- **Files modified:** `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt`
- **Verification:** All 6 tests compile and pass; Test 5 still proves `expiresAt?.toDate()?.time` returns the original `1_700_000_500_000L` epoch-ms value.
- **Committed in:** `c28bffb` (Task 1 RED commit) and verified GREEN in `1ed563b`.

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Compile-blocker resolved with semantically-equivalent fixture construction; no scope creep, no contract change. Plan's intent (pin `Timestamp?` field type at construction time) preserved verbatim.

## Issues Encountered

None beyond the deviation above. Cloud Function code, web mapper (`mapItemSnapshot` from quick-260510-o7w), reservation business logic (`ReservationRepositoryImpl`), and domain `Item.kt` all left untouched as scoped.

## User Setup Required

None — no external service configuration required.

## Known Stubs

None. All four touched files are wired end-to-end (DTO → repository → domain → UI), with the user-confirmed device walkthrough verifying live data flow against the Firebase emulator.

## Next Phase Readiness

- **Reservation flow restored** for owners on the Android app — unblocks any phase that exercises owner registry detail with active reservations.
- **Pre-existing latent issue noted (out of scope):** `ItemRepositoryImpl.kt:80` writes `createdAt` / `updatedAt` via `System.currentTimeMillis()` (client clock). The Cloud Function may write these via `FieldValue.serverTimestamp()` elsewhere. This was not touched — it doesn't affect the reservation-deserialize crash. Flag for a future schema-consistency phase.
- **Future opportunity:** If the owner UI ever wants to show "reserved by {giverEmail}" or a fresh-reservation badge based on `reservedAt`, those fields are already on the DTO — just promote them to the domain `Item` and add the mapper line.

## Self-Check: PASSED

Verified after writing this SUMMARY:
- `app/src/main/java/com/giftregistry/data/model/ItemDto.kt` — exists.
- `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt` — exists.
- `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` — exists.
- `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt` — exists.
- Commit `c28bffb` (RED, Task 1) — present in `git log`.
- Commit `1ed563b` (GREEN, Task 2) — present in `git log`.

---

*Phase: quick/260510-oja*
*Completed: 2026-05-07*
