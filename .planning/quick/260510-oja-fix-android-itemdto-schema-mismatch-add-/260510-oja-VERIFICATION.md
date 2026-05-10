---
phase: quick/260510-oja
verified: 2026-05-07T00:00:00Z
status: passed
score: 5/5 must-haves verified
---

# Quick Task 260510-oja: Android ItemDto Schema Mismatch Fix — Verification Report

**Task Goal:** Fix Android `ItemDto` schema mismatch — add `reservedBy` + `reservedAt` fields, change `expiresAt` from `Long` to `Timestamp` to match the Cloud Function canonical schema (causes registry-detail crash when an item is reserved).

**Verified:** 2026-05-07
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #   | Truth                                                                                                              | Status     | Evidence                                                                                                                                                |
| --- | ------------------------------------------------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `FirestoreDataSource.observeItems()` does NOT crash when an item carries `reservedBy: String, reservedAt: Timestamp, expiresAt: Timestamp` | ✓ VERIFIED | Human-verified on device: rebuilt APK installed, navigated to previously-crashing registry with reserved item, no crash, no `CustomClassMapper` warnings in Logcat. |
| 2   | `ItemDto` round-trips a Firestore item document containing `reservedBy/reservedAt/expiresAt` without `No setter/field` warnings                 | ✓ VERIFIED | `ItemDto.kt` lines 31-33 declare all three fields with matching types; user confirmed Logcat is clean of `No setter/field` warnings.                    |
| 3   | An item with NO reservation fields (status=available, all reservation fields null) still deserializes                                            | ✓ VERIFIED | `ItemDto` defaults are all `= null`; Test 4 in `ItemDtoSchemaTest.kt` pins this; Test 6 verifies mapper handles null `expiresAt`. 6/6 tests pass.        |
| 4   | `ItemDto.expiresAt (Timestamp?)` maps to `Item.expiresAt (Long ms?)` at the `ItemRepositoryImpl` boundary                                        | ✓ VERIFIED | `ItemRepositoryImpl.kt:73` — `expiresAt = expiresAt?.toDate()?.time`. Test 5 verifies the conversion. Domain `Item.kt:15` retains `expiresAt: Long?`. |
| 5   | An optimistic local snapshot where `reservedAt` is null (FieldValue.serverTimestamp() not yet resolved) does not crash the mapper                | ✓ VERIFIED | `reservedAt: Timestamp? = null` on `ItemDto`; mapper does not read `reservedAt` (intentionally — not propagated to domain). Null-safety via `?.toDate()`.|

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact                                                                       | Expected                                                                                                       | Status     | Details                                                                                                                                                |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `app/src/main/java/com/giftregistry/data/model/ItemDto.kt`                     | DTO matching CF canonical schema — `reservedBy: String?`, `reservedAt: Timestamp?`, `expiresAt: Timestamp?`    | ✓ VERIFIED | Lines 31-33 declare all three fields; `import com.google.firebase.Timestamp` on line 3; KDoc cites canonical writer. Used by `FirestoreDataSource.observeItems` and `ItemRepositoryImpl.toDomain`. |
| `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt`       | `ItemDto.toDomain` converts Timestamp `expiresAt` to Long ms                                                   | ✓ VERIFIED | Line 73: `expiresAt = expiresAt?.toDate()?.time`. Boundary comment at lines 71-72.                                                                     |
| `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt`      | `observeItems` no longer needs manual `getTimestamp()` workaround                                              | ✓ VERIFIED | `grep "doc.getTimestamp"` returns nothing. Body is now `doc.toObject(ItemDto::class.java)?.copy(id = doc.id)` (line 114), matching `RegistryDto` pattern. |
| `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt`        | Type-shape regression tests pinning DTO field types and Timestamp→Long mapper conversion (min 40 lines)        | ✓ VERIFIED | 117 lines, 6 tests covering type pins (Tests 1-3), default-null pin (Test 4), mapper conversion non-null + null (Tests 5-6). All 6 pass per user gates. |

### Key Link Verification

| From                                                              | To                                                                       | Via                                                                                                  | Status     | Details                                                                                                                            |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- | ---------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `ItemDto.kt`                                                      | `functions/src/reservation/createReservation.ts` (canonical writer)      | Firestore item document field shape: `reservedBy:string, reservedAt:Timestamp, expiresAt:Timestamp` | ✓ WIRED    | Pattern `(reservedBy\|reservedAt\|expiresAt).*Timestamp` matches lines 9-11 (KDoc) and 31-33 (fields).                              |
| `ItemRepositoryImpl.kt` (`toDomain`)                              | `Item.kt` (`expiresAt: Long?`)                                           | `Timestamp.toDate().time` conversion at data→domain boundary                                         | ✓ WIRED    | Pattern `expiresAt\?\.toDate\(\)\?\.time` matches line 73; `Item.kt:15` retains `Long?` (unchanged, confirmed by `git log Item.kt`). |
| `FirestoreDataSource.kt` (`observeItems` line 114)                | `ItemDto.kt`                                                             | Firestore `CustomClassMapper` auto-deserialization (no manual `getTimestamp()`)                      | ✓ WIRED    | Pattern `doc\.toObject\(ItemDto::class\.java\)` matches line 114; no `getTimestamp` call remains in file.                            |

### Data-Flow Trace (Level 4)

| Artifact                       | Data Variable | Source                                          | Produces Real Data | Status     |
| ------------------------------ | ------------- | ----------------------------------------------- | ------------------ | ---------- |
| `FirestoreDataSource.kt`       | `items`       | Firestore snapshot listener on items subcollection | Yes — live snapshot from emulator | ✓ FLOWING  |
| `ItemRepositoryImpl.kt`        | `Item.expiresAt` (Long ms) | `dataSource.observeItems(registryId).map { ... }` → `Timestamp.toDate().time` | Yes — verified by Test 5 mapping `Timestamp(Date(1_700_000_500_000L))` → `1_700_000_500_000L` | ✓ FLOWING  |
| `RegistryItemRow` → `StatusChip` | `item.expiresAt` (Long?) | Domain `Item` from repository | Yes — domain contract unchanged; UI consumers (StatusChip:46, ReservedChip:73) still receive `Long?` | ✓ FLOWING  |

### Behavioral Spot-Checks

| Behavior                                                                       | Command                                                                                            | Result                       | Status |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------- | ---------------------------- | ------ |
| New schema regression tests compile and pass                                   | `./gradlew :app:testDebugUnitTest --tests *ItemDtoSchemaTest*`                                     | 6/6 pass (per execution log) | ✓ PASS |
| Full Android JVM test suite remains green                                      | `./gradlew :app:testDebugUnitTest`                                                                 | 330/330 pass (per execution log) | ✓ PASS |
| Debug APK builds cleanly                                                       | `./gradlew :app:assembleDebug`                                                                     | BUILD SUCCESSFUL (per execution log) | ✓ PASS |
| Crash repro path no longer crashes (registry detail with reserved item)        | Manual: install APK → open registry with reserved item → observe                                   | No crash, clean Logcat       | ✓ PASS |
| `doc.getTimestamp` workaround removed                                          | `grep -n "doc.getTimestamp" app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` | (no matches)                 | ✓ PASS |
| Canonical fields present in DTO                                                | `grep -nE "(reservedBy\|reservedAt\|expiresAt: Timestamp)" .../ItemDto.kt`                          | All three present at lines 31-33 | ✓ PASS |
| Boundary conversion present in mapper                                          | `grep -nE "expiresAt\?\.toDate\(\)\?\.time" .../ItemRepositoryImpl.kt`                              | Match at line 73             | ✓ PASS |

### Requirements Coverage

| Requirement       | Source Plan       | Description                                                                                       | Status      | Evidence                                                                                                                                                                                                                            |
| ----------------- | ----------------- | ------------------------------------------------------------------------------------------------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| QUICK-260510-oja  | `260510-oja-PLAN` | Fix Android `ItemDto` schema mismatch causing registry-detail crash when an item has been reserved | ✓ SATISFIED | All 5 truths verified, 4 artifacts present and correct, 3 key links wired, behavioral spot-checks pass on JVM + assemble + device. User-confirmed crash repro is silent on rebuilt APK. Cloud Function code, web mapper, and domain `Item.kt` untouched as scoped. |

### Anti-Patterns Found

| File                       | Line | Pattern                                  | Severity | Impact                                                                                                                                                                                                |
| -------------------------- | ---- | ---------------------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ItemRepositoryImpl.kt`    | 80   | `"createdAt" to System.currentTimeMillis()` (client-clock writes; pre-existing, out of scope) | ℹ️ Info  | Pre-existing latent issue noted in plan output as "createdAt/updatedAt schema question … NOT touched — out of scope for this crash fix". Does not affect the reservation deserialize fix. |

No blocker or warning anti-patterns found in the four files modified by this task. Mapper `expiresAt = expiresAt?.toDate()?.time` is null-safe; DTO defaults all null; no TODO/FIXME/PLACEHOLDER strings; no empty handlers; no `return null` stubs; no hardcoded empty-prop renderings.

### Scope Discipline (Out-of-Scope Files Untouched)

| File                                                              | Expected               | Actual                                                                              | Status     |
| ----------------------------------------------------------------- | ---------------------- | ----------------------------------------------------------------------------------- | ---------- |
| `app/src/main/java/com/giftregistry/domain/model/Item.kt`         | Unchanged (`Long?`)    | Last touched in commit `7392e70` (phase 04-05); not in this task's commits          | ✓ VERIFIED |
| `functions/src/reservation/createReservation.ts`                  | Unchanged              | Not in `files_modified`; canonical writer left as the source of truth               | ✓ VERIFIED |
| Web mapper (`mapItemSnapshot`)                                    | Unchanged              | Not in `files_modified`; separate concern (handled in 260510-o7w)                   | ✓ VERIFIED |
| `ReservationRepositoryImpl`, callable invocations                 | Unchanged              | Not in `files_modified`; only the deserialize boundary was broken                   | ✓ VERIFIED |

### Human Verification Required

None outstanding. The blocking checkpoint (Task 3: registry detail screen no longer crashes when an item has been reserved) was already executed and approved by the user:
- Rebuilt APK installed.
- Navigated to the previously-crashing registry containing a reserved item.
- No crash observed.
- No `CustomClassMapper` `No setter/field` warnings on Logcat.

### Gaps Summary

No gaps. All 5 must-have truths verified; all 4 artifacts pass levels 1-4 (exists, substantive, wired, data flowing); all 3 key links wired; full behavioral spot-checks pass; user-confirmed crash repro is silent on the fixed build. Domain `Item.kt`, Cloud Function code, web mapper, and reservation business logic untouched as scoped.

The two pre-existing notes from the plan output remain advisory and do not block this task:
- `reservedBy` / `reservedAt` are intentionally NOT propagated to the domain `Item` — DTO carries them for forward-compatibility; future "reserved by {giver}" UI work can promote them.
- `createdAt` / `updatedAt` schema question (Android writes Long ms via client clock, server may write Timestamps elsewhere) is pre-existing and explicitly out of scope for this crash fix.

---

_Verified: 2026-05-07_
_Verifier: Claude (gsd-verifier)_
