---
phase: quick/260510-oja
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/data/model/ItemDto.kt
  - app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt
  - app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
  - app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt
autonomous: false
requirements:
  - QUICK-260510-oja

must_haves:
  truths:
    - "Android FirestoreDataSource.observeItems() does NOT crash when an item document carries server-written reservation fields (reservedBy: String, reservedAt: Timestamp, expiresAt: Timestamp)"
    - "ItemDto round-trips a Firestore item document containing reservedBy/reservedAt/expiresAt without 'No setter/field' Logcat warnings"
    - "An item with NO reservation fields (status=available, reservedBy=null, reservedAt=null, expiresAt=null) still deserializes — most items in any registry are in this state"
    - "ItemDto.expiresAt (Timestamp?) maps to Item.expiresAt (Long ms?) at the ItemRepositoryImpl boundary, preserving the contract consumed by StatusChip / ReservedChip"
    - "An optimistic local snapshot where reservedAt is null (FieldValue.serverTimestamp() not yet resolved) does not crash the mapper"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/data/model/ItemDto.kt"
      provides: "DTO matching the Cloud Function canonical schema — reservedBy: String?, reservedAt: Timestamp?, expiresAt: Timestamp?"
      contains: "Timestamp"
    - path: "app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt"
      provides: "ItemDto.toDomain converts Timestamp expiresAt to Long ms for the domain Item type"
      contains: "expiresAt?.toDate()?.time"
    - path: "app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt"
      provides: "observeItems no longer needs the manual getTimestamp() workaround on line 111 — toObject handles it natively now"
      contains: "doc.toObject(ItemDto::class.java)"
    - path: "app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt"
      provides: "Type-shape regression tests pinning ItemDto field types and the toDomain Timestamp→Long conversion"
      min_lines: 40
  key_links:
    - from: "app/src/main/java/com/giftregistry/data/model/ItemDto.kt"
      to: "functions/src/reservation/createReservation.ts (lines 62-67, canonical writer)"
      via: "Firestore item document field shape: reservedBy:string, reservedAt:Timestamp, expiresAt:Timestamp"
      pattern: "(reservedBy|reservedAt|expiresAt).*Timestamp"
    - from: "app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt (toDomain)"
      to: "app/src/main/java/com/giftregistry/domain/model/Item.kt (expiresAt: Long?)"
      via: "Timestamp.toDate().time conversion at the data→domain boundary"
      pattern: "expiresAt\\?\\.toDate\\(\\)\\?\\.time"
    - from: "app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt (line 112 — observeItems toObject call)"
      to: "app/src/main/java/com/giftregistry/data/model/ItemDto.kt"
      via: "Firestore CustomClassMapper auto-deserialization (no manual getTimestamp() needed)"
      pattern: "doc\\.toObject\\(ItemDto::class\\.java\\)"
---

<objective>
Fix the Android `ItemDto` schema mismatch causing a hard `RuntimeException: Could not deserialize object. Failed to convert a value of type com.google.firebase.Timestamp to long (found in field 'expiresAt')` whenever the registry detail screen observes an item that has been reserved.

Purpose: The Cloud Function `createReservation` (functions/src/reservation/createReservation.ts:62-67) is the source of truth — it writes `reservedBy: string`, `reservedAt: Timestamp` (via `FieldValue.serverTimestamp()`), and `expiresAt: Timestamp` (via `Timestamp.fromMillis(...)`). Android's `ItemDto` is missing the first two fields entirely (Logcat: `No setter/field for reservedBy found on class ItemDto`) and types `expiresAt` as `Long?`, causing Firestore's `CustomClassMapper` to throw on `doc.toObject(ItemDto::class.java)` at `FirestoreDataSource.observeItems` line 112. This kills the registry detail screen for any owner who has ever had a giver reserve one of their items — the core "reservations work" promise from PROJECT.md.

Output:
- `ItemDto` adds `reservedBy: String? = null` and `reservedAt: Timestamp? = null`; changes `expiresAt: Long?` → `expiresAt: Timestamp?` to match the canonical schema.
- `ItemRepositoryImpl.toDomain` converts the new `Timestamp` `expiresAt` to `Long` ms at the data→domain boundary, preserving the existing `Item.expiresAt: Long?` contract that `StatusChip` / `ReservedChip` already consume.
- `FirestoreDataSource.observeItems` removes the now-dead manual `getTimestamp("expiresAt")` workaround on line 111 — `toObject` handles the Timestamp natively now that the DTO field type matches.
- New unit-test file pins the type contract and the mapper conversion so a future regression (e.g. someone re-typing `expiresAt: Long?` to "save mapping logic") fails fast at JVM test time.

Out of scope (do NOT touch):
- Cloud Function code (canonical, deployed, working — Android must align to it, not the reverse).
- Web mapper (separate concern; recently fixed in 260510-o7w; web `mapItemSnapshot` already handles `Timestamp` correctly).
- Reservation business logic (`ReservationRepositoryImpl`, callable invocations) — only the deserialize boundary is broken.
- Domain model `Item` shape — keep `expiresAt: Long?`. Do NOT add `reservedBy` / `reservedAt` to the domain `Item`; no UI consumes them yet, and ballooning scope risks introducing a second bug. The DTO-level fields silently ride along for forward-compatibility.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@app/src/main/java/com/giftregistry/data/model/ItemDto.kt
@app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt
@app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
@app/src/main/java/com/giftregistry/domain/model/Item.kt
@functions/src/reservation/createReservation.ts
@functions/src/reservation/releaseReservation.ts

<diagnosis>
Already completed by orchestrator — DO NOT re-investigate.

**Crash signature (real Logcat):**
```
W [CustomClassMapper]: No setter/field for reservedBy found on class com.giftregistry.data.model.ItemDto
W [CustomClassMapper]: No setter/field for reservedAt found on class com.giftregistry.data.model.ItemDto
E AndroidRuntime: java.lang.RuntimeException: Could not deserialize object.
    Failed to convert a value of type com.google.firebase.Timestamp to long (found in field 'expiresAt')
    at com.giftregistry.data.registry.FirestoreDataSource$observeItems$1$1.onEvent(FirestoreDataSource.kt:112)
```

**Schema delta (Android ItemDto vs Cloud Function canonical writer):**

| Field | Cloud Function writes | Android ItemDto today | Required change |
|---|---|---|---|
| `reservedBy` | `string` (giverEmail) | _missing_ | ADD `reservedBy: String? = null` |
| `reservedAt` | `Timestamp` (FieldValue.serverTimestamp()) | _missing_ | ADD `reservedAt: Timestamp? = null` |
| `expiresAt` | `Timestamp` (Timestamp.fromMillis(...)) | `Long?` | CHANGE to `Timestamp? = null` |

**Why the existing line-111 workaround in FirestoreDataSource doesn't help:**

```kotlin
// Current (broken):
val expiresAtMs = doc.getTimestamp("expiresAt")?.toDate()?.time
doc.toObject(ItemDto::class.java)?.copy(  // ← CRASHES HERE before .copy ever runs
    id = doc.id,
    expiresAt = expiresAtMs,
)
```

The `getTimestamp()` call on line 111 succeeds, but `doc.toObject(ItemDto::class.java)` on line 112 invokes Firestore's `CustomClassMapper`, which iterates document fields and tries to coerce `expiresAt` (Timestamp) into the DTO's declared `Long?`. That throws BEFORE `.copy(...)` is reached — the workaround is dead code on the crash path.

**The fix:** Match the DTO field type to what Firestore actually delivers (`Timestamp?`). `toObject` then succeeds. The line-111 workaround becomes redundant and is removed (Task 2). The Timestamp→Long conversion moves to the data→domain mapper in `ItemRepositoryImpl.toDomain` (Task 2).

**Edge cases the implementation MUST handle:**

1. **Optimistic local snapshot — `reservedAt` is null pre-server-roundtrip.** Firestore's client SDK returns `null` for a `FieldValue.serverTimestamp()` that hasn't been resolved by the server yet. `reservedAt: Timestamp? = null` already handles this; mapper must not throw on null.
2. **Most items have all three reservation fields null** (status=available, no active reservation). The DTO's `null` defaults cover this; mapper must not throw on null.
3. **Domain consumers of `Item.expiresAt`:** verified by grep — `StatusChip`/`ReservedChip` (`ui/common/status/StatusChip.kt`), `RegistryItemRow` (passes `item.expiresAt` into StatusChip), `StyleGuidePreview`. All read `Long?` epoch-ms. The mapper conversion `expiresAt?.toDate()?.time` preserves this contract exactly. (`ReservationRepositoryImpl` reads a separate `expiresAtMs: Number` from the callable response payload — unrelated, untouched.)
4. **`ItemDto.expiresAt` is currently consumed in only one place:** `ItemRepositoryImpl.toDomain` line 71 (`expiresAt = expiresAt`). This is the only call site that needs updating.

**Test feasibility note (informs Task 1's scope):**

A true end-to-end Firestore deserializer test would require either Robolectric (not configured in this project — build.gradle.kts has no Robolectric dep, only `junit:junit:4.13.2`, `coroutines.test`, `mockk`, `turbine`) or the Firebase Emulator with an instrumented test (out of scope for a JVM unit test). Mocking `DocumentSnapshot.toObject` is also unhelpful: `toObject` is the SDK code we're trying to keep happy — mocking it just asserts our mock returns what we tell it to.

The pragmatic JVM-testable contract is:
- (a) `ItemDto` accepts `Timestamp` for `expiresAt` and `reservedAt`, and `String` for `reservedBy`, at compile + construction time. (Type-shape pin.)
- (b) `ItemRepositoryImpl.toDomain` converts a non-null `Timestamp` `expiresAt` to the correct epoch-ms `Long`, and a null `Timestamp` to a null `Long`. (Mapper pin.)

The actual `CustomClassMapper` deserializer happiness is verified by the Task 3 human-verify checkpoint — running the app against the live emulator with a reserved item, the exact crash path the Logcat showed. The crash itself is the regression test until/unless Robolectric is added later.
</diagnosis>

<interfaces>
<!-- Cloud Function source-of-truth writer (functions/src/reservation/createReservation.ts) -->
```typescript
const expiresAt = Timestamp.fromMillis(expiresAtMs)  // line 38
tx.update(itemRef, {                                   // lines 62-67
  status: "reserved",
  reservedBy: giverEmail,                              // string
  reservedAt: FieldValue.serverTimestamp(),            // Timestamp
  expiresAt,                                           // Timestamp
})
```

<!-- Cloud Function paired writer (functions/src/reservation/releaseReservation.ts) -->
```typescript
tx.update(itemRef, {                                   // lines 78-83
  status: "available",
  reservedBy: FieldValue.delete(),
  reservedAt: FieldValue.delete(),
  expiresAt: FieldValue.delete(),
})
```

<!-- Android domain target (app/src/main/java/com/giftregistry/domain/model/Item.kt — UNCHANGED) -->
```kotlin
data class Item(
    val id: String = "",
    val registryId: String = "",
    val title: String = "",
    val originalUrl: String = "",
    val affiliateUrl: String = "",
    val imageUrl: String? = null,
    val price: String? = null,
    val notes: String? = null,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val expiresAt: Long? = null,                       // Long ms — UI-facing, do NOT change
)
```

<!-- Test scaffolding available (from app/build.gradle.kts) -->
- junit:junit:4.13.2
- libs.coroutines.test
- libs.mockk
- libs.turbine
- com.google.firebase:firebase-firestore (provides com.google.firebase.Timestamp at test classpath)

<!-- Existing data-layer test pattern (RegistryRepositoryImplObserveTest.kt, StoreRepositoryImplTest.kt) -->
- JVM unit tests under app/src/test/java
- MockK for repository collaborators
- com.google.firebase.Timestamp is plain Java/Kotlin and instantiable in JVM tests via Timestamp(seconds, nanos) or Timestamp.fromMillis(ms)
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: RED — pin ItemDto field types and the toDomain Timestamp→Long mapper conversion</name>
  <files>app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt</files>
  <behavior>
    The new test file must contain at least these cases:

    - Test 1 (DTO type-shape pin — `reservedBy`): Construct `ItemDto(reservedBy = "giver@example.com")` using a named argument. Assert `dto.reservedBy == "giver@example.com"`. Compilation will fail today (field doesn't exist) — test source won't compile until Task 2 adds the field. RED here = compile-time failure or assertion failure if a stub field of the wrong type is used.

    - Test 2 (DTO type-shape pin — `reservedAt`): Construct `ItemDto(reservedAt = Timestamp(1_700_000_000L, 0))`. Assert `dto.reservedAt?.seconds == 1_700_000_000L`. RED via compile failure today.

    - Test 3 (DTO type-shape pin — `expiresAt: Timestamp?`): Construct `ItemDto(expiresAt = Timestamp.fromMillis(1_700_000_500_000L))`. Assert `dto.expiresAt?.toDate()?.time == 1_700_000_500_000L`. RED today because `expiresAt` is currently typed `Long?` — passing `Timestamp` won't compile.

    - Test 4 (DTO defaults — guards Edge case 2): Construct `ItemDto()` with no reservation fields. Assert `dto.reservedBy == null && dto.reservedAt == null && dto.expiresAt == null`. After Task 2, this guards "most items have null reservation fields" without crashing.

    - Test 5 (Mapper conversion — non-null Timestamp): Build an `ItemDto` with `expiresAt = Timestamp.fromMillis(1_700_000_500_000L)`, run it through `ItemRepositoryImpl`'s `toDomain` mapper (this is currently `private` — see Action for how to exercise it), assert the resulting `Item.expiresAt == 1_700_000_500_000L`.

    - Test 6 (Mapper conversion — null Timestamp guards Edge case 1, 2): `ItemDto()` with no `expiresAt` → `Item.expiresAt == null` after `toDomain`. Guards optimistic-local-snapshot null and not-reserved null cases.

    The mapper is currently a private extension function inside `ItemRepositoryImpl`. To exercise it from a unit test without changing visibility:
    - Construct a real `ItemRepositoryImpl(dataSource = mockk(relaxed = true), functions = mockk(relaxed = true))`.
    - Stub `dataSource.observeItems(registryId)` to return a `MutableStateFlow(listOf(itemDto))`.
    - Collect the first emission of `repo.observeItems(registryId)`, assert the single resulting `Item`'s `expiresAt` matches expectation.
    - Use `kotlinx.coroutines.test.runTest` and `kotlinx.coroutines.flow.first()`.

    This pattern is already established in `RegistryRepositoryImplObserveTest.kt` — follow the same MockK + StateFlow shape.

    Run `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest"` and CONFIRM the test source FAILS TO COMPILE today (because `ItemDto` lacks `reservedBy`/`reservedAt` and types `expiresAt: Long?`). This compile failure IS the RED gate.
  </behavior>
  <action>
    Create `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt` following the package and import style of `app/src/test/java/com/giftregistry/data/registry/RegistryRepositoryImplObserveTest.kt`.

    Implementation notes:
    - Package: `com.giftregistry.data.registry`.
    - Imports needed: `com.giftregistry.data.model.ItemDto`, `com.giftregistry.domain.model.Item`, `com.google.firebase.Timestamp`, `com.google.firebase.firestore.FirebaseFirestore`, `com.google.firebase.functions.FirebaseFunctions`, `io.mockk.every`, `io.mockk.mockk`, `kotlinx.coroutines.flow.MutableStateFlow`, `kotlinx.coroutines.flow.first`, `kotlinx.coroutines.test.runTest`, `org.junit.Assert.*`, `org.junit.Test`.
    - For Tests 5 & 6, set up `FirestoreDataSource` as `mockk(relaxed = true)` and stub `every { dataSource.observeItems("reg1") } returns MutableStateFlow(listOf(itemDto))`. Construct `ItemRepositoryImpl(dataSource, mockk(relaxed = true))`. Collect first emission via `repo.observeItems("reg1").first()`.
    - Use `Timestamp.fromMillis(...)` (preferred) or `Timestamp(seconds, nanos)` for fixture timestamps; `com.google.firebase.Timestamp` is at the test classpath via the `firebase-firestore` main module dependency.
    - DO NOT mock `Timestamp` — use real instances. It's a plain value class.
    - DO NOT attempt to mock `DocumentSnapshot.toObject` — out of scope per diagnosis.

    Then run:
    ```
    ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest" 2>&1 | tee /tmp/oja-red.log
    ```

    Expected RED outcome: KOTLIN COMPILE ERROR mentioning `unresolved reference: reservedBy`, `unresolved reference: reservedAt`, and/or `type mismatch: inferred type is Timestamp but Long? was expected` (for `expiresAt`). The compile failure is the RED gate — no runtime test execution happens at this stage. This proves the test exercises the actual schema bug.

    Why: Without a compile-time RED gate, a future regression (someone re-typing a field back to its broken type "to simplify") would silently succeed. Pinning the types in a test file gives us a JVM-fast, no-emulator-needed regression guard for the parts we CAN unit-test. The deserializer-roundtrip portion is verified at the Task 3 device checkpoint.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest" 2>&1 | tee /tmp/oja-red.log; grep -E "(unresolved reference: reservedBy|unresolved reference: reservedAt|type mismatch.*Timestamp.*Long|Compilation error|FAILED)" /tmp/oja-red.log || (echo "RED gate did not fail at compile time as expected" && exit 1)</automated>
  </verify>
  <done>
    - Test file `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt` exists with 6 tests covering: 3 type-shape pins, 1 default-null pin, 2 mapper-conversion pins.
    - `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest"` FAILS at Kotlin compile with errors mentioning `reservedBy`/`reservedAt`/`expiresAt`-Timestamp-mismatch (RED confirmed).
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: GREEN — update ItemDto, ItemRepositoryImpl mapper, and FirestoreDataSource so the schema matches the Cloud Function canonical writer</name>
  <files>app/src/main/java/com/giftregistry/data/model/ItemDto.kt, app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt, app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt</files>
  <behavior>
    After this change:

    1. `ItemDto` has 3 schema changes:
       - ADD `val reservedBy: String? = null`
       - ADD `val reservedAt: Timestamp? = null` (with `import com.google.firebase.Timestamp`)
       - CHANGE `val expiresAt: Long? = null` → `val expiresAt: Timestamp? = null`

    2. `ItemRepositoryImpl.toDomain` converts the new `Timestamp` `expiresAt` to `Long?` epoch-ms at the data→domain boundary:
       - `expiresAt = expiresAt?.toDate()?.time` (preserves `Item.expiresAt: Long?` contract for `StatusChip` / `ReservedChip`).
       - `reservedBy` / `reservedAt` are NOT propagated to the domain `Item` (out of scope — no UI consumes them yet).

    3. `FirestoreDataSource.observeItems` removes the now-dead manual workaround:
       - Delete line 111 (`val expiresAtMs = doc.getTimestamp("expiresAt")?.toDate()?.time`).
       - Delete the `expiresAt = expiresAtMs` argument from the `.copy(...)` call.
       - Resulting body: `doc.toObject(ItemDto::class.java)?.copy(id = doc.id)`.
       - This matches the pattern already used for registries on lines 32, 60, 71.

    4. All 6 tests from Task 1 must compile and pass (GREEN).

    5. The full Android JVM test suite must remain green (no regressions). Verified via `./gradlew :app:testDebugUnitTest`.

    6. The Android app must still compile cleanly: `./gradlew :app:assembleDebug`.
  </behavior>
  <action>
    Make THREE source changes in this exact order:

    **Step A — `app/src/main/java/com/giftregistry/data/model/ItemDto.kt`:**

    Replace the entire file with:

    ```kotlin
    package com.giftregistry.data.model

    import com.google.firebase.Timestamp

    /**
     * Firestore item document DTO. Schema is owned by the Cloud Function
     * `createReservation` (functions/src/reservation/createReservation.ts) — the
     * canonical writer. This DTO MUST match what the function writes:
     *   reservedBy: string (giverEmail)
     *   reservedAt: Timestamp (FieldValue.serverTimestamp())
     *   expiresAt:  Timestamp (Timestamp.fromMillis(...))
     *
     * `reservedAt` is null on the optimistic local snapshot before the server
     * resolves serverTimestamp(); the Timestamp? type already handles that.
     * All three reservation fields are null when status == "available".
     *
     * Conversion to the domain `Item.expiresAt: Long?` happens in
     * `ItemRepositoryImpl.toDomain`. Keep DTO types Firestore-native here.
     */
    data class ItemDto(
        val id: String = "",
        val title: String = "",
        val originalUrl: String = "",
        val affiliateUrl: String = "",
        val imageUrl: String? = null,
        val price: String? = null,
        val notes: String? = null,
        val status: String = "available",
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val reservedBy: String? = null,
        val reservedAt: Timestamp? = null,
        val expiresAt: Timestamp? = null,
    )
    ```

    **Step B — `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt`:**

    In the private `ItemDto.toDomain(registryId: String)` extension at the bottom of the file, change exactly one line. Currently:

    ```kotlin
    private fun ItemDto.toDomain(registryId: String) = Item(
        id = id, registryId = registryId, title = title,
        originalUrl = originalUrl, affiliateUrl = affiliateUrl,
        imageUrl = imageUrl, price = price, notes = notes,
        status = ItemStatus.fromString(status),
        createdAt = createdAt, updatedAt = updatedAt,
        expiresAt = expiresAt,
    )
    ```

    Change to:

    ```kotlin
    private fun ItemDto.toDomain(registryId: String) = Item(
        id = id, registryId = registryId, title = title,
        originalUrl = originalUrl, affiliateUrl = affiliateUrl,
        imageUrl = imageUrl, price = price, notes = notes,
        status = ItemStatus.fromString(status),
        createdAt = createdAt, updatedAt = updatedAt,
        // DTO carries Firestore Timestamp (matches Cloud Function writer);
        // domain Item.expiresAt is Long ms for StatusChip / ReservedChip consumers.
        expiresAt = expiresAt?.toDate()?.time,
    )
    ```

    Do NOT add `reservedBy` / `reservedAt` to the domain `Item` here — out of scope (no UI consumes them yet, and `Item.kt` is not in `files_modified`).

    **Step C — `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt`:**

    In `observeItems` (lines 105-120), the current body inside `mapNotNull` is:

    ```kotlin
    val items = snapshot?.documents?.mapNotNull { doc ->
        val expiresAtMs = doc.getTimestamp("expiresAt")?.toDate()?.time
        doc.toObject(ItemDto::class.java)?.copy(
            id = doc.id,
            expiresAt = expiresAtMs,
        )
    } ?: emptyList()
    ```

    Replace with:

    ```kotlin
    val items = snapshot?.documents?.mapNotNull { doc ->
        // ItemDto now matches the Cloud Function canonical schema
        // (reservedBy/reservedAt/expiresAt are Firestore-native types),
        // so toObject deserializes natively — no manual getTimestamp() needed.
        doc.toObject(ItemDto::class.java)?.copy(id = doc.id)
    } ?: emptyList()
    ```

    This matches the pattern already used for `RegistryDto` on lines 32, 60, 71.

    **Step D — verify:**

    Run, in order:

    1. `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest"` — Task 1's 6 tests must compile AND pass (GREEN).
    2. `./gradlew :app:testDebugUnitTest` — full Android JVM suite passes (no regressions).
    3. `./gradlew :app:assembleDebug` — debug APK builds cleanly (catches any wider compile issues, e.g. unexpected DTO consumer we missed in grep).

    Why these exact changes: the diagnosis identified ONLY these three files as needing modification (DTO type, mapper conversion, removal of dead workaround). `Item.kt` is intentionally untouched — keeping the domain stable means UI code (`StatusChip`, `RegistryItemRow`, `StyleGuidePreview`) needs zero changes, which is the smallest safe surface for a crash-fix.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest" 2>&1 | tail -30 && ./gradlew :app:testDebugUnitTest 2>&1 | tail -20 && ./gradlew :app:assembleDebug 2>&1 | tail -10</automated>
  </verify>
  <done>
    - `ItemDto.kt` adds `reservedBy: String?` and `reservedAt: Timestamp?`, changes `expiresAt: Long?` → `expiresAt: Timestamp?`, imports `com.google.firebase.Timestamp`, has the explanatory KDoc tying the schema to `createReservation.ts`.
    - `ItemRepositoryImpl.toDomain` converts `expiresAt?.toDate()?.time` and has the inline comment explaining the boundary conversion.
    - `FirestoreDataSource.observeItems` no longer calls `doc.getTimestamp("expiresAt")` and no longer overrides `expiresAt` in the `.copy(...)` call.
    - All 6 tests in `ItemDtoSchemaTest` pass.
    - `./gradlew :app:testDebugUnitTest` exits 0.
    - `./gradlew :app:assembleDebug` exits 0 (clean build).
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Human-verify — registry detail screen no longer crashes when an item has been reserved</name>
  <files>(no source files modified; this is a checkpoint that re-runs the user's original repro on a device/AVD)</files>
  <what-built>
    Android `ItemDto` schema now matches the Cloud Function canonical writer. The `RuntimeException: Could not deserialize object. Failed to convert a value of type com.google.firebase.Timestamp to long (found in field 'expiresAt')` crash at `FirestoreDataSource.observeItems(FirestoreDataSource.kt:112)` should be resolved. The `No setter/field for reservedBy/reservedAt` Logcat warnings should also be gone.
  </what-built>
  <action>
    Pause and ask the user to reproduce the original crash path end-to-end against the live Firebase emulator. Claude has nothing further to automate at this step — the Logcat warnings cannot be reliably observed without a real `CustomClassMapper` invocation, which requires the Firebase Android SDK on a real Android runtime. Present the verification steps below and wait for the resume signal.
  </action>
  <how-to-verify>
    Reproduce the original crash path end-to-end against the live Firebase emulator:

    1. Start the Firebase emulator suite (`firebase emulators:start` from project root, or whatever the project's standard local-dev incantation is — emulator must be reachable on the default emulator host since `BuildConfig.USE_FIREBASE_EMULATOR` defaults to `true` for debug builds).
    2. Install and launch the freshly built debug APK on a device or AVD: `./gradlew :app:installDebug` then launch from the launcher.
    3. Sign in as an owner (or use an existing dev account).
    4. Open or create a registry that has at least one item. If no items exist, add one (URL-import flow is fine).
    5. Reserve that item. Easiest path: open the same registry on the web fallback in a browser as a guest giver, fill the giver form, submit the reservation. (Alternative: invoke `createReservation` directly via the emulator UI / `firebase functions:shell`.) Confirm the web shows the reservation success state.
    6. Switch back to the Android app on the same registry detail screen.

    EXPECTED:
    - The registry detail screen renders. No crash. No ANR.
    - The reserved item shows the reserved-state chip with a countdown (StatusChip / ReservedChip) — this is existing UI from prior phases; it just needs the deserialize step to stop crashing.
    - Logcat is clean of `CustomClassMapper` warnings about `reservedBy` / `reservedAt`. Filter Logcat with: `adb logcat | grep -E "CustomClassMapper|FirestoreDataSource|AndroidRuntime"`.
    - Other items (status=available, no reservation) still render normally.

    REGRESSION CHECKS:
    - Owner edit flow on an item still works (no DTO consumer broken by the type change).
    - Backgrounding/resuming the app on the registry detail screen does not produce new crashes.

    If ANY of the above fails, capture the new Logcat output and resume with details — likely an additional consumer of `ItemDto.expiresAt` we didn't catch, or an unrelated regression introduced by the build.
  </how-to-verify>
  <verify>(human-verified — see how-to-verify steps; resume signal below)</verify>
  <resume-signal>Type "approved" if registry detail renders without crash and Logcat is clean. Otherwise paste the failing Logcat tail.</resume-signal>
  <done>User has typed "approved" (or equivalent) confirming: (1) registry detail screen renders with at least one reserved item, (2) no `RuntimeException: Could not deserialize object` in Logcat, (3) no `CustomClassMapper: No setter/field for reservedBy|reservedAt` warnings on the registry detail flow, (4) other (non-reserved) items still render normally.</done>
</task>

</tasks>

<verification>
After Task 2 (automated):

1. `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.registry.ItemDtoSchemaTest"` — 6 tests pass.
2. `./gradlew :app:testDebugUnitTest` — full JVM suite green.
3. `./gradlew :app:assembleDebug` — clean build.
4. `grep -nE "(reservedBy|reservedAt|expiresAt: Timestamp)" app/src/main/java/com/giftregistry/data/model/ItemDto.kt` — confirms canonical fields present.
5. `grep -nE "expiresAt\\?\\.toDate\\(\\)\\?\\.time" app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt` — confirms boundary conversion present.
6. `grep -n "doc.getTimestamp" app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` — should return NOTHING (dead workaround removed).

After Task 3 (human-verified):

7. Owner registry detail screen renders without `RuntimeException` when at least one item is reserved.
8. Logcat has no `No setter/field for reservedBy` or `No setter/field for reservedAt` warnings on registry detail.
</verification>

<success_criteria>
- Android `ItemDto` adds `reservedBy: String?` and `reservedAt: Timestamp?`, and changes `expiresAt: Long?` → `expiresAt: Timestamp?`.
- `ItemRepositoryImpl.toDomain` converts `Timestamp` `expiresAt` → `Long` ms at the data→domain boundary; domain `Item.expiresAt: Long?` is unchanged.
- `FirestoreDataSource.observeItems` no longer carries the dead `getTimestamp("expiresAt")` workaround on the crash path.
- New JVM unit-test file `app/src/test/java/com/giftregistry/data/registry/ItemDtoSchemaTest.kt` pins all three DTO type changes and the mapper conversion (6 tests).
- TDD cycle followed: tests written first, observed RED at compile time, then implementation made them GREEN.
- Full Android JVM test suite stays green (no regressions in pre-existing tests).
- `./gradlew :app:assembleDebug` builds cleanly.
- Human-verified: registry detail screen with a reserved item renders, no `Could not deserialize` crash, no `CustomClassMapper` warnings for `reservedBy` / `reservedAt`.
- Cloud Function code, web mapper, reservation business logic, and domain `Item.kt` are unmodified.
</success_criteria>

<output>
After completion, create `.planning/quick/260510-oja-fix-android-itemdto-schema-mismatch-add-/260510-oja-SUMMARY.md` with:
- The 4 files changed (ItemDto.kt, ItemRepositoryImpl.kt, FirestoreDataSource.kt, new ItemDtoSchemaTest.kt) and the exact line-level edits.
- Confirmation of RED (compile-failure) → GREEN (6 tests pass) cycle.
- Test counts before/after for `:app:testDebugUnitTest`.
- Confirmation that the human-verify checkpoint passed (paste the resume-signal user text or note "approved").
- Note that `reservedBy` / `reservedAt` are intentionally NOT propagated to the domain `Item` (no UI consumer yet) — flagged for any future phase that wants to display "reserved by {giver}" in the owner UI.
- Note that the latent `createdAt`/`updatedAt` schema question (Android writes Long ms, Cloud Function writes via timestamps elsewhere) was NOT touched — out of scope for this crash fix.
</output>
