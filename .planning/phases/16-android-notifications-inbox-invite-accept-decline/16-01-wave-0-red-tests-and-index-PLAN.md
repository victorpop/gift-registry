---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt
  - app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt
  - app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt
  - app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt
  - app/src/test/java/com/giftregistry/LocalizationParityTest.kt
  - functions/src/__tests__/acceptInvite.test.ts
  - functions/src/__tests__/declineInvite.test.ts
  - tests/rules/firestore.rules.test.ts
  - firestore.indexes.json
autonomous: true
requirements:
  - D-07
  - D-11
  - D-18
  - D-19
  - D-21
  - D-22
  - D-24
  - D-25
  - D-27
  - D-28
user_setup: []

must_haves:
  truths:
    - "RED test files exist for NotificationType.fromWire mapping the 3 new wire strings"
    - "RED test file exists for NotificationCard branching on pendingEntryKey presence"
    - "RED test file exists for InviteResponseViewModel state machine (Idle → Submitting → Error/Success)"
    - "RED test file exists for NotificationRepositoryImpl.acceptInvite/declineInvite calling correct callables"
    - "RED test file exists for acceptInvite Cloud Function transaction + idempotency"
    - "RED test file exists for declineInvite Cloud Function transaction + idempotency"
    - "New test cases added to firestore.rules.test.ts for pendingInvitedUsers read scope (D-18) + isInvited unchanged after promote (D-19)"
    - "firestore.indexes.json contains composite index (type asc, payload.registryId asc) on users/*/notifications"
    - "LocalizationParityTest enforces strings.xml ↔ values-ro/strings.xml key parity"
    - "All RED tests fail when executed (compile or assertion fail expected; this is the RED state)"
  artifacts:
    - path: "app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt"
      provides: "Pure-Kotlin test for fromWire mapping of invite_accepted_self / invite_accepted / invite_declined + UNKNOWN fallback"
      contains: "fromWire"
    - path: "app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt"
      provides: "Test for sheet-vs-navigate branching predicate"
      contains: "pendingEntryKey"
    - path: "app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt"
      provides: "State machine test via Turbine"
      contains: "Submitting"
    - path: "app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt"
      provides: "Repository wrapper test for acceptInvite/declineInvite httpsCallable name"
      contains: "acceptInvite"
    - path: "functions/src/__tests__/acceptInvite.test.ts"
      provides: "Jest test for accept transaction (promote + write 3 notifications + idempotency)"
      contains: "acceptInvite"
    - path: "functions/src/__tests__/declineInvite.test.ts"
      provides: "Jest test for decline transaction (remove pending + write owner notification + idempotency)"
      contains: "declineInvite"
    - path: "tests/rules/firestore.rules.test.ts"
      provides: "Extended rules tests for D-18 + D-19 (added describe blocks)"
      contains: "pendingInvitedUsers"
    - path: "firestore.indexes.json"
      provides: "Composite index for inbox cleanup query (Pitfall 7)"
      contains: "payload.registryId"
    - path: "app/src/test/java/com/giftregistry/LocalizationParityTest.kt"
      provides: "Asserts EN and RO strings.xml have matching key sets"
      contains: "values-ro"
  key_links:
    - from: "Wave 0 RED tests"
      to: "Wave 2/3/4 GREEN implementations"
      via: "Test file paths referenced in dependent plan read_first"
      pattern: "Test files exist and fail; later plans flip them GREEN"
---

<objective>
Wave 0 RED-test scaffolding for Phase 16 per the Nyquist validation strategy (16-VALIDATION.md). Creates all failing test files BEFORE any production code is written, adds the composite Firestore index required by the inbox cleanup query (Pitfall 7), and establishes a localization parity test. Every subsequent plan ships GREEN code that flips one or more of these RED tests.

Purpose: Lock in expected behavior contracts before implementation; surface contradictions in the spec early; satisfy Nyquist Dimension 8 (verify_phase_goal coverage).
Output: 8 new test files + extended rules test file + firestore.indexes.json — all RED, all committed.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-VALIDATION.md
@app/src/main/java/com/giftregistry/domain/model/Notification.kt
@app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt
@functions/src/__tests__/confirmPurchase.test.ts
@functions/src/__tests__/inviteToRegistry.test.ts
@tests/rules/firestore.rules.test.ts
@firestore.indexes.json

<interfaces>
<!-- Current NotificationType enum (to be extended in Plan 16-03): -->
```kotlin
// app/src/main/java/com/giftregistry/domain/model/Notification.kt
enum class NotificationType {
    INVITE, RESERVATION_CREATED, ITEM_PURCHASED,
    RESERVATION_EXPIRED, RE_RESERVE_WINDOW, UNKNOWN;
    companion object {
        fun fromWire(raw: String?): NotificationType = when (raw) {
            "invite" -> INVITE
            "reservation_created" -> RESERVATION_CREATED
            "item_purchased" -> ITEM_PURCHASED
            "reservation_expired" -> RESERVATION_EXPIRED
            "re_reserve_window" -> RE_RESERVE_WINDOW
            else -> UNKNOWN
        }
    }
}
data class Notification(
    val id: String, val type: NotificationType,
    val titleKey: String, val bodyKey: String,
    val titleFallback: String, val bodyFallback: String,
    val payload: Map<String, String?>, val createdAtMs: Long, val readAtMs: Long?,
)
```

<!-- Repository interface (current — to be extended): -->
```kotlin
// app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt
interface NotificationRepository {
    fun observe(uid: String, limit: Int = 50): Flow<List<Notification>>
    fun observeUnreadCount(uid: String): Flow<Int>
    suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit>
}
```

<!-- Existing Jest test harness pattern (confirmPurchase.test.ts shape, to mirror): -->
The functions/src/__tests__/ directory uses firebase-functions-test + jest. acceptInvite.test.ts and declineInvite.test.ts must follow the SAME setup/teardown shape as confirmPurchase.test.ts.

<!-- Existing rules test harness pattern (firestore.rules.test.ts): -->
Uses @firebase/rules-unit-testing v3+ with assertFails / assertSucceeds helpers. Existing testEnv.authenticatedContext('uid').firestore() pattern is reused.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Create Android RED unit tests (4 test files) + LocalizationParityTest</name>
  <read_first>
    - app/src/main/java/com/giftregistry/domain/model/Notification.kt (current enum to be extended)
    - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt (current impl to be extended)
    - app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt (template for httpsCallable wrappers)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md (Wave 0 gaps section, lines 666-676)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md (Interaction & State Contracts section for state machine spec)
  </read_first>
  <behavior>
    Test 1 — NotificationTypeFromWireTest: assert fromWire("invite_accepted_self") == INVITE_ACCEPTED_SELF, fromWire("invite_accepted") == INVITE_ACCEPTED, fromWire("invite_declined") == INVITE_DECLINED, fromWire("unknown_future_type") == UNKNOWN, fromWire(null) == UNKNOWN. Also reassert existing mappings still work (regression guard).
    Test 2 — NotificationCardBranchingTest: assert pure-Kotlin predicate `shouldOpenInviteSheet(notification)` returns TRUE when notification.type == INVITE AND notification.payload["pendingEntryKey"] != null, returns FALSE when type != INVITE, returns FALSE when type == INVITE but pendingEntryKey is null (legacy invite — D-11 fallback).
    Test 3 — InviteResponseViewModelTest (Turbine): assert state machine starts in Idle; after accept() emits Submitting then AcceptedSuccess (on repo success); after accept() with repo failure emits Submitting then Error(action=Accept, message=...); from Error state, retry() returns to Submitting; decline() flow identical structure.
    Test 4 — NotificationRepositoryImplAcceptDeclineTest: using MockK fake FirebaseFunctions, assert acceptInvite(registryId) calls functions.getHttpsCallable("acceptInvite").call(mapOf("registryId" to registryId)); declineInvite calls "declineInvite" with same payload shape; both wrap in runCatching and return Result<Unit>.
    Test 5 — LocalizationParityTest: enumerate <string name=...> keys from res/values/strings.xml and res/values-ro/strings.xml; assert key sets are equal. Reads files via test resources or parses XML directly.
  </behavior>
  <action>
    Create exactly these 5 test files. All MUST fail compilation OR assertions in RED state (production code does not yet implement INVITE_ACCEPTED_SELF etc.).

    File 1 — app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt:
    ```kotlin
    package com.giftregistry.domain.model

    import org.junit.Test
    import org.junit.Assert.assertEquals

    class NotificationTypeFromWireTest {
        @Test fun `fromWire maps invite_accepted_self to INVITE_ACCEPTED_SELF`() {
            assertEquals(NotificationType.INVITE_ACCEPTED_SELF, NotificationType.fromWire("invite_accepted_self"))
        }
        @Test fun `fromWire maps invite_accepted to INVITE_ACCEPTED`() {
            assertEquals(NotificationType.INVITE_ACCEPTED, NotificationType.fromWire("invite_accepted"))
        }
        @Test fun `fromWire maps invite_declined to INVITE_DECLINED`() {
            assertEquals(NotificationType.INVITE_DECLINED, NotificationType.fromWire("invite_declined"))
        }
        @Test fun `fromWire maps unknown future type to UNKNOWN`() {
            assertEquals(NotificationType.UNKNOWN, NotificationType.fromWire("some_future_v2_type"))
        }
        @Test fun `fromWire maps null to UNKNOWN`() {
            assertEquals(NotificationType.UNKNOWN, NotificationType.fromWire(null))
        }
        // Regression — existing mappings untouched
        @Test fun `fromWire preserves invite mapping`() {
            assertEquals(NotificationType.INVITE, NotificationType.fromWire("invite"))
        }
        @Test fun `fromWire preserves reservation_created mapping`() {
            assertEquals(NotificationType.RESERVATION_CREATED, NotificationType.fromWire("reservation_created"))
        }
        @Test fun `fromWire preserves item_purchased mapping`() {
            assertEquals(NotificationType.ITEM_PURCHASED, NotificationType.fromWire("item_purchased"))
        }
        @Test fun `fromWire preserves reservation_expired mapping`() {
            assertEquals(NotificationType.RESERVATION_EXPIRED, NotificationType.fromWire("reservation_expired"))
        }
        @Test fun `fromWire preserves re_reserve_window mapping`() {
            assertEquals(NotificationType.RE_RESERVE_WINDOW, NotificationType.fromWire("re_reserve_window"))
        }
    }
    ```

    File 2 — app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt:
    ```kotlin
    package com.giftregistry.ui.notifications

    import com.giftregistry.domain.model.Notification
    import com.giftregistry.domain.model.NotificationType
    import org.junit.Test
    import org.junit.Assert.assertTrue
    import org.junit.Assert.assertFalse

    class NotificationCardBranchingTest {
        private fun notif(type: NotificationType, pendingEntryKey: String? = null): Notification = Notification(
            id = "n1", type = type,
            titleKey = "k", bodyKey = "k",
            titleFallback = "t", bodyFallback = "b",
            payload = if (pendingEntryKey != null) mapOf("pendingEntryKey" to pendingEntryKey) else emptyMap(),
            createdAtMs = 0L, readAtMs = null,
        )

        @Test fun `INVITE with pendingEntryKey opens sheet`() {
            assertTrue(shouldOpenInviteSheet(notif(NotificationType.INVITE, "uid-1")))
        }
        @Test fun `INVITE with email pendingEntryKey opens sheet`() {
            assertTrue(shouldOpenInviteSheet(notif(NotificationType.INVITE, "email:jane.doe@example.com")))
        }
        @Test fun `INVITE without pendingEntryKey (legacy) does not open sheet`() {
            assertFalse(shouldOpenInviteSheet(notif(NotificationType.INVITE, null)))
        }
        @Test fun `INVITE_ACCEPTED_SELF does not open sheet`() {
            assertFalse(shouldOpenInviteSheet(notif(NotificationType.INVITE_ACCEPTED_SELF, "uid-1")))
        }
        @Test fun `RESERVATION_CREATED does not open sheet`() {
            assertFalse(shouldOpenInviteSheet(notif(NotificationType.RESERVATION_CREATED)))
        }
        @Test fun `UNKNOWN does not open sheet`() {
            assertFalse(shouldOpenInviteSheet(notif(NotificationType.UNKNOWN)))
        }
    }
    ```

    File 3 — app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt:
    ```kotlin
    package com.giftregistry.ui.notifications

    import app.cash.turbine.test
    import com.giftregistry.domain.notifications.NotificationRepository
    import io.mockk.coEvery
    import io.mockk.mockk
    import kotlinx.coroutines.ExperimentalCoroutinesApi
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.test.UnconfinedTestDispatcher
    import kotlinx.coroutines.test.resetMain
    import kotlinx.coroutines.test.runTest
    import kotlinx.coroutines.test.setMain
    import org.junit.After
    import org.junit.Before
    import org.junit.Test
    import org.junit.Assert.assertTrue
    import org.junit.Assert.assertEquals

    @OptIn(ExperimentalCoroutinesApi::class)
    class InviteResponseViewModelTest {
        private val dispatcher = UnconfinedTestDispatcher()
        @Before fun setup() { Dispatchers.setMain(dispatcher) }
        @After fun tear() { Dispatchers.resetMain() }

        @Test
        fun `accept success transitions Idle to Submitting to AcceptedSuccess`() = runTest {
            val repo = mockk<NotificationRepository>()
            coEvery { repo.acceptInvite("reg-1") } returns Result.success(Unit)
            val vm = InviteResponseViewModel(repo)
            vm.state.test {
                assertTrue(awaitItem() is InviteResponseViewModel.State.Idle)
                vm.accept("reg-1")
                assertTrue(awaitItem() is InviteResponseViewModel.State.Submitting)
                assertTrue(awaitItem() is InviteResponseViewModel.State.AcceptedSuccess)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `accept failure transitions Idle to Submitting to Error`() = runTest {
            val repo = mockk<NotificationRepository>()
            coEvery { repo.acceptInvite("reg-1") } returns Result.failure(RuntimeException("network"))
            val vm = InviteResponseViewModel(repo)
            vm.state.test {
                awaitItem() // Idle
                vm.accept("reg-1")
                awaitItem() // Submitting
                val err = awaitItem()
                assertTrue(err is InviteResponseViewModel.State.Error)
                assertEquals(InviteResponseViewModel.Action.Accept, (err as InviteResponseViewModel.State.Error).action)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `decline success transitions Idle to Submitting to DeclinedSuccess`() = runTest {
            val repo = mockk<NotificationRepository>()
            coEvery { repo.declineInvite("reg-1") } returns Result.success(Unit)
            val vm = InviteResponseViewModel(repo)
            vm.state.test {
                awaitItem()
                vm.decline("reg-1")
                assertTrue(awaitItem() is InviteResponseViewModel.State.Submitting)
                assertTrue(awaitItem() is InviteResponseViewModel.State.DeclinedSuccess)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `retry from Error returns to Submitting with same action`() = runTest {
            val repo = mockk<NotificationRepository>()
            coEvery { repo.acceptInvite("reg-1") } returnsMany listOf(
                Result.failure(RuntimeException("first")),
                Result.success(Unit),
            )
            val vm = InviteResponseViewModel(repo)
            vm.state.test {
                awaitItem() // Idle
                vm.accept("reg-1")
                awaitItem() // Submitting
                awaitItem() // Error
                vm.retry()
                assertTrue(awaitItem() is InviteResponseViewModel.State.Submitting)
                assertTrue(awaitItem() is InviteResponseViewModel.State.AcceptedSuccess)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
    ```

    File 4 — app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt:
    ```kotlin
    package com.giftregistry.data.notifications

    import com.google.android.gms.tasks.Tasks
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.functions.FirebaseFunctions
    import com.google.firebase.functions.HttpsCallableReference
    import com.google.firebase.functions.HttpsCallableResult
    import io.mockk.every
    import io.mockk.mockk
    import io.mockk.slot
    import io.mockk.verify
    import kotlinx.coroutines.test.runTest
    import org.junit.Test
    import org.junit.Assert.assertTrue
    import org.junit.Assert.assertEquals

    class NotificationRepositoryImplAcceptDeclineTest {

        private fun setupRepo(): Pair<NotificationRepositoryImpl, FirebaseFunctions> {
            val firestore = mockk<FirebaseFirestore>(relaxed = true)
            val functions = mockk<FirebaseFunctions>()
            val callable = mockk<HttpsCallableReference>()
            val result = mockk<HttpsCallableResult>()
            every { functions.getHttpsCallable(any()) } returns callable
            every { callable.call(any()) } returns Tasks.forResult(result)
            return NotificationRepositoryImpl(firestore, functions) to functions
        }

        @Test
        fun `acceptInvite calls acceptInvite callable with registryId`() = runTest {
            val (repo, functions) = setupRepo()
            val nameSlot = slot<String>()
            val argSlot = slot<Map<String, Any>>()
            every { functions.getHttpsCallable(capture(nameSlot)) } answers {
                val callable = mockk<HttpsCallableReference>()
                every { callable.call(capture(argSlot)) } returns Tasks.forResult(mockk<HttpsCallableResult>())
                callable
            }

            val result = repo.acceptInvite("reg-123")

            assertTrue(result.isSuccess)
            assertEquals("acceptInvite", nameSlot.captured)
            assertEquals(mapOf("registryId" to "reg-123"), argSlot.captured)
        }

        @Test
        fun `declineInvite calls declineInvite callable with registryId`() = runTest {
            val (repo, functions) = setupRepo()
            val nameSlot = slot<String>()
            val argSlot = slot<Map<String, Any>>()
            every { functions.getHttpsCallable(capture(nameSlot)) } answers {
                val callable = mockk<HttpsCallableReference>()
                every { callable.call(capture(argSlot)) } returns Tasks.forResult(mockk<HttpsCallableResult>())
                callable
            }

            val result = repo.declineInvite("reg-456")

            assertTrue(result.isSuccess)
            assertEquals("declineInvite", nameSlot.captured)
            assertEquals(mapOf("registryId" to "reg-456"), argSlot.captured)
        }

        @Test
        fun `acceptInvite wraps thrown exception in Result failure`() = runTest {
            val firestore = mockk<FirebaseFirestore>(relaxed = true)
            val functions = mockk<FirebaseFunctions>()
            val callable = mockk<HttpsCallableReference>()
            every { functions.getHttpsCallable("acceptInvite") } returns callable
            every { callable.call(any()) } returns Tasks.forException(RuntimeException("network"))

            val repo = NotificationRepositoryImpl(firestore, functions)
            val result = repo.acceptInvite("reg-x")

            assertTrue(result.isFailure)
        }
    }
    ```

    File 5 — app/src/test/java/com/giftregistry/LocalizationParityTest.kt:
    ```kotlin
    package com.giftregistry

    import org.junit.Test
    import org.junit.Assert.assertEquals
    import java.io.File

    /**
     * Asserts that every string key in values/strings.xml has a matching key in values-ro/strings.xml.
     * Run as standard unit test; reads files from project paths via working-dir relative.
     */
    class LocalizationParityTest {
        @Test
        fun `EN and RO strings xml have matching key sets`() {
            val keyRegex = Regex("""<string\s+name="([^"]+)"""")
            fun keys(path: String): Set<String> {
                val file = File(path)
                if (!file.exists()) error("Missing strings file: $path")
                return keyRegex.findAll(file.readText())
                    .map { it.groupValues[1] }
                    .toSet()
            }
            val en = keys("src/main/res/values/strings.xml")
            val ro = keys("src/main/res/values-ro/strings.xml")
            val missingFromRo = en - ro
            val missingFromEn = ro - en
            assertEquals(
                "Keys missing from RO: $missingFromRo; keys missing from EN: $missingFromEn",
                emptySet<String>(),
                missingFromRo + missingFromEn,
            )
        }
    }
    ```

    Notes:
    - Tests 1-4 will FAIL to compile until plans 16-03 and 16-04 add the production code (this is expected RED state).
    - Test 5 should PASS today (Phase 16 has not added any new strings yet); it will guard future plans.
    - Use existing libs from libs.versions.toml: junit, mockk, turbine, kotlinx-coroutines-test — all already wired.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugUnitTestKotlin 2>&1 | tee /tmp/16-01-compile.log; grep -q "NotificationType.INVITE_ACCEPTED_SELF\|InviteResponseViewModel\|NotificationRepositoryImpl(firestore, functions)" /tmp/16-01-compile.log && echo "RED: compile errors expected on production code"; ls app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt app/src/test/java/com/giftregistry/LocalizationParityTest.kt</automated>
  </verify>
  <acceptance_criteria>
    - File app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt exists
    - NotificationTypeFromWireTest.kt contains string "INVITE_ACCEPTED_SELF"
    - NotificationTypeFromWireTest.kt contains string "invite_accepted_self"
    - NotificationTypeFromWireTest.kt contains string "invite_declined"
    - File app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt exists
    - NotificationCardBranchingTest.kt contains string "shouldOpenInviteSheet"
    - NotificationCardBranchingTest.kt contains string "pendingEntryKey"
    - File app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt exists
    - InviteResponseViewModelTest.kt contains string "Turbine" or "import app.cash.turbine"
    - InviteResponseViewModelTest.kt contains string "InviteResponseViewModel.State.Submitting"
    - InviteResponseViewModelTest.kt contains string "AcceptedSuccess"
    - InviteResponseViewModelTest.kt contains string "DeclinedSuccess"
    - File app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt exists
    - NotificationRepositoryImplAcceptDeclineTest.kt contains string "getHttpsCallable(\"acceptInvite\")"
    - NotificationRepositoryImplAcceptDeclineTest.kt contains string "getHttpsCallable(\"declineInvite\")"
    - File app/src/test/java/com/giftregistry/LocalizationParityTest.kt exists
    - LocalizationParityTest.kt contains string "values-ro/strings.xml"
    - Compile of :app:compileDebugUnitTestKotlin produces unresolved-reference errors for INVITE_ACCEPTED_SELF or InviteResponseViewModel (RED state — these symbols don't exist yet)
  </acceptance_criteria>
  <done>5 Android RED test files exist on disk; compile fails with expected unresolved references for symbols to be implemented in plans 16-03/16-04.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create Functions RED tests + extend Firestore rules tests + add composite index</name>
  <read_first>
    - functions/src/__tests__/confirmPurchase.test.ts (template for transaction test harness)
    - functions/src/__tests__/inviteToRegistry.test.ts (existing tests — note current assertions on invitedUsers writes)
    - tests/rules/firestore.rules.test.ts (existing rules tests — examine structure for adding new describe blocks)
    - firestore.indexes.json (current index list)
    - firestore.rules (current isInvited rule definition)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md (Pattern 4 + Pitfall 7 for index reasoning; Example E for the rules test shape)
  </read_first>
  <behavior>
    Test acceptInvite.test.ts:
    - Test 1 (happy path): seed registry with pendingInvitedUsers={uid:true}; invoke acceptInvite; assert pendingInvitedUsers[uid] deleted AND invitedUsers[uid]==true; assert INVITE notification doc deleted from users/{uid}/notifications; assert two new notification docs written (invite_accepted_self to invitee, invite_accepted to owner).
    - Test 2 (idempotency): seed registry where uid is ALREADY in invitedUsers and NOT in pending; invoke acceptInvite; assert returns success without writes.
    - Test 3 (no pending entry): seed registry without uid in pending or invited; invoke; assert HttpsError code = failed-precondition with message NO_PENDING_INVITE.
    - Test 4 (unauthenticated): invoke without auth; assert HttpsError code = unauthenticated.
    - Test 5 (missing registryId): invoke with auth but no registryId; assert HttpsError code = invalid-argument.
    - Test 6 (registry not found): invoke with valid uid but registryId that doesn't exist; assert HttpsError code = not-found.

    Test declineInvite.test.ts:
    - Test 1 (happy path): seed registry with pendingInvitedUsers={uid:true}; invoke declineInvite; assert pendingInvitedUsers[uid] deleted AND invitedUsers unchanged; assert INVITE notification doc deleted; assert ONE owner notification written (invite_declined). No JOINED notification.
    - Test 2 (idempotency): seed registry without uid anywhere; invoke; assert HttpsError failed-precondition (or no-op success per discretion — match the chosen contract in Plan 16-02).
    - Tests 3-6: same as acceptInvite for auth + arg validation.

    Extended inviteToRegistry.test.ts:
    - Update existing assertions: writes go to pendingInvitedUsers (NOT invitedUsers).
    - New: notification payload contains pendingEntryKey, occasion, coverUrl (or null), eventDateMs (or null).
    - New: when invitee is already in invitedUsers, pendingInvitedUsers is NOT touched, but FCM push and inbox notification ARE still written (D-16).

    Extended firestore.rules.test.ts:
    - D-18 describe block per RESEARCH.md Example E (4 tests).
    - D-19 test: invitee promoted to invitedUsers CAN read.
    - Existing tests untouched.

    firestore.indexes.json:
    - Add composite index on users/*/notifications with (type asc, payload.registryId asc) — Pitfall 7.
  </behavior>
  <action>
    Create exactly these files.

    File 1 — functions/src/__tests__/acceptInvite.test.ts:
    Mirror the shape and harness setup of functions/src/__tests__/confirmPurchase.test.ts. Use the same fixtures directory and emulator binding. Tests must reference an `acceptInvite` exported function (which doesn't exist yet — Plan 16-02 creates it). Write 6 tests covering happy path + idempotency + 4 error cases as described above. Use `import { acceptInvite } from "../registry/acceptInvite"`. The test file will fail to compile until Plan 16-02 creates the function.

    File 2 — functions/src/__tests__/declineInvite.test.ts:
    Mirror confirmPurchase.test.ts harness. Import from `../registry/declineInvite`. 6 tests per the behavior block. Will fail to compile until Plan 16-02 creates it.

    File 3 — MODIFY functions/src/__tests__/inviteToRegistry.test.ts:
    Read the file first. Find existing assertions on `invitedUsers` writes. Update them to assert writes go to `pendingInvitedUsers` instead. Add new assertions:
    - After a fresh invite to a new email, the written notification payload contains: { pendingEntryKey: "uid-or-email:xxx", occasion: <registry.occasion>, coverUrl: <registry.imageUrl OR null>, eventDateMs: <registry.eventAt.toMillis() OR null> }
    - When invitee uid is ALREADY in invitedUsers, NO write to pendingInvitedUsers occurs, but writeNotification and sendInvitePush ARE called.
    Keep existing tests passing structure; only the assertion targets change. Will be RED until Plan 16-02 ships the modified inviteToRegistry.ts.

    File 4 — MODIFY tests/rules/firestore.rules.test.ts:
    Read the file first. Append a new describe block at the end:
    ```typescript
    describe("pendingInvitedUsers read scope (D-18)", () => {
      it("non-owner cannot read a registry doc with pendingInvitedUsers populated", async () => { /* see Example E */ });
      it("invitee with ONLY pending entry (no invitedUsers entry) cannot read registry", async () => { /* see Example E */ });
      it("owner CAN read registry with pendingInvitedUsers populated", async () => { /* see Example E */ });
      it("D-19: invitee promoted to invitedUsers (post-accept) CAN read", async () => { /* see Example E */ });
    });
    ```
    Use the verbatim test bodies from .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md Example E (lines 891-936). Reuse the existing `testEnv` / `adminDb` / `assertFails` / `assertSucceeds` helpers — do NOT duplicate setup boilerplate. The 4 new tests should PASS today because the existing rules already enforce owner-only reads of registry docs (per Pattern 8: no rule edit needed; the test verifies the existing rule covers the new field).

    File 5 — MODIFY firestore.indexes.json:
    Add a new entry to the `indexes` array (alongside existing items/reservations indexes):
    ```json
    {
      "collectionGroup": "notifications",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "type", "order": "ASCENDING" },
        { "fieldPath": "payload.registryId", "order": "ASCENDING" }
      ]
    }
    ```
    The complete file must remain valid JSON. Preserve all existing indexes verbatim. This is required for the acceptInvite/declineInvite inbox cleanup query (Pitfall 7).
  </action>
  <verify>
    <automated>cd functions && npx tsc --noEmit 2>&1 | grep -E "acceptInvite|declineInvite" | head -5 && echo "RED: acceptInvite/declineInvite test files reference missing modules (expected)"; (cd tests/rules && npm test 2>&1 | tail -20); cat firestore.indexes.json | python3 -c "import sys, json; d=json.load(sys.stdin); assert any(i.get('collectionGroup')=='notifications' for i in d['indexes']), 'notifications index missing'; print('OK: notifications composite index present')"</automated>
  </verify>
  <acceptance_criteria>
    - File functions/src/__tests__/acceptInvite.test.ts exists
    - acceptInvite.test.ts contains string "import { acceptInvite }"
    - acceptInvite.test.ts contains string "failed-precondition"
    - acceptInvite.test.ts contains string "invite_accepted_self"
    - acceptInvite.test.ts contains string "invite_accepted"
    - acceptInvite.test.ts contains string "pendingInvitedUsers"
    - acceptInvite.test.ts contains string "invitedUsers"
    - File functions/src/__tests__/declineInvite.test.ts exists
    - declineInvite.test.ts contains string "import { declineInvite }"
    - declineInvite.test.ts contains string "invite_declined"
    - declineInvite.test.ts contains string "pendingInvitedUsers"
    - File functions/src/__tests__/inviteToRegistry.test.ts contains string "pendingInvitedUsers"
    - inviteToRegistry.test.ts contains string "pendingEntryKey"
    - inviteToRegistry.test.ts contains string "occasion"
    - inviteToRegistry.test.ts contains string "coverUrl"
    - inviteToRegistry.test.ts contains string "eventDateMs"
    - File tests/rules/firestore.rules.test.ts contains string "pendingInvitedUsers read scope (D-18)"
    - firestore.rules.test.ts contains string "D-19"
    - firestore.rules.test.ts contains string "reg-pending-only"
    - File firestore.indexes.json contains string "notifications"
    - firestore.indexes.json contains string "payload.registryId"
    - firestore.indexes.json is valid JSON (python3 -c "import json; json.load(open('firestore.indexes.json'))" exits 0)
    - cd tests/rules && npm test exits 0 (rules tests PASS — Pattern 8 confirms no rule change needed)
    - cd functions && npx tsc --noEmit reports module-not-found errors for ../registry/acceptInvite AND ../registry/declineInvite (RED state — modules created in Plan 16-02)
  </acceptance_criteria>
  <done>Backend RED test scaffolding exists, rules tests extended and passing (Pattern 8 verification), composite index added to firestore.indexes.json. acceptInvite/declineInvite test files reference modules that don't yet exist — this is the expected RED state to be flipped GREEN by Plan 16-02.</done>
</task>

</tasks>

<verification>
- All 5 Android test files exist on disk under app/src/test/java/com/giftregistry/.
- All 2 new Cloud Functions test files exist under functions/src/__tests__/.
- inviteToRegistry.test.ts has been modified to expect pendingInvitedUsers writes.
- firestore.rules.test.ts has new D-18/D-19 describe block.
- firestore.indexes.json contains the notifications composite index.
- Rules tests (cd tests/rules && npm test) PASS — existing rules already cover the new field per Pattern 8.
- Android compile of test sources FAILS with unresolved references (RED state expected).
- Functions tsc FAILS to find acceptInvite/declineInvite modules (RED state expected).
</verification>

<success_criteria>
- 8 new test files + 1 modified test file + 1 modified index file = 10 file changes total.
- All grep-verifiable string assertions pass.
- Rules tests green; Android + Functions compile RED (expected; flipped by later plans).
- Git commit groups these files with a clear "Wave 0 RED + index" message.
</success_criteria>

<output>
After completion, create `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-01-SUMMARY.md` listing each file created/modified and confirming the RED-state expectation for Android + Functions compile failures.
</output>
