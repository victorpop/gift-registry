---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 03
type: execute
wave: 2
depends_on:
  - 16-01-wave-0-red-tests-and-index
files_modified:
  - app/src/main/java/com/giftregistry/domain/model/Notification.kt
  - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt
  - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt
autonomous: true
requirements:
  - D-25
  - D-26
  - D-27
user_setup: []

must_haves:
  truths:
    - "NotificationType enum contains 3 new values: INVITE_ACCEPTED_SELF, INVITE_ACCEPTED, INVITE_DECLINED"
    - "NotificationType.fromWire maps invite_accepted_self / invite_accepted / invite_declined to the corresponding enum values"
    - "NotificationType.fromWire returns UNKNOWN for any unrecognized wire string (forward-compat preserved)"
    - "NotificationRepository interface declares acceptInvite(registryId): Result<Unit> and declineInvite(registryId): Result<Unit>"
    - "NotificationRepositoryImpl constructor accepts FirebaseFunctions in addition to FirebaseFirestore"
    - "NotificationRepositoryImpl.acceptInvite calls functions.getHttpsCallable(\"acceptInvite\") with mapOf(\"registryId\" to registryId) and wraps in runCatching"
    - "NotificationRepositoryImpl.declineInvite calls functions.getHttpsCallable(\"declineInvite\") with the same payload shape"
    - "Existing FirebaseFunctions Hilt provider (AppModule.kt line 36) is reused — no new DI module needed"
    - "All Plan 16-01 Wave 0 Android tests for these symbols flip RED → GREEN"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/domain/model/Notification.kt"
      provides: "Extended NotificationType enum with 3 new values + extended fromWire"
      contains: "INVITE_ACCEPTED_SELF"
    - path: "app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt"
      provides: "Extended interface with acceptInvite + declineInvite suspend methods"
      contains: "suspend fun acceptInvite"
    - path: "app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt"
      provides: "Implementation: FirebaseFunctions dep + 2 new httpsCallable wrappers"
      contains: "getHttpsCallable(\"acceptInvite\")"
  key_links:
    - from: "NotificationRepositoryImpl"
      to: "Cloud Function acceptInvite"
      via: "FirebaseFunctions.getHttpsCallable httpsCallable"
      pattern: "getHttpsCallable\\(\"acceptInvite\"\\)"
    - from: "NotificationRepositoryImpl"
      to: "Cloud Function declineInvite"
      via: "FirebaseFunctions.getHttpsCallable httpsCallable"
      pattern: "getHttpsCallable\\(\"declineInvite\"\\)"
    - from: "NotificationType.fromWire"
      to: "Cloud Function wire strings (invite_accepted_self / invite_accepted / invite_declined)"
      via: "String-to-enum mapping at DTO → domain conversion"
      pattern: "\"invite_accepted_self\" -> INVITE_ACCEPTED_SELF"
---

<objective>
Android domain + data layer extensions to consume the new Phase 16 backend callables. Three surgical edits: extend NotificationType enum (D-25), extend NotificationRepository interface + impl (D-27), preserve all existing behavior. Flips the Plan 16-01 RED tests for NotificationTypeFromWireTest + NotificationRepositoryImplAcceptDeclineTest GREEN.

Purpose: Give the UI layer (Plan 16-04) clean contracts to call against without thinking about Firebase.
Output: 3 modified Kotlin files.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md
@app/src/main/java/com/giftregistry/domain/model/Notification.kt
@app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt
@app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt
@app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt
@app/src/main/java/com/giftregistry/di/AppModule.kt
@app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt
@app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt

<interfaces>
<!-- AppModule.kt already provides FirebaseFunctions singleton (lines 35-43): -->
```kotlin
@Provides @Singleton
fun provideFirebaseFunctions(): FirebaseFunctions =
    FirebaseFunctions.getInstance("europe-west3").also { fns ->
        // (existing emulator wiring)
    }
```
No new @Provides binding needed — Hilt will inject it into NotificationRepositoryImpl's constructor automatically.

<!-- Verbatim pattern from ReservationRepositoryImpl.kt lines 59-65 (the shape for new methods): -->
```kotlin
override suspend fun confirmPurchase(reservationId: String): Result<Unit> = runCatching {
    functions
        .getHttpsCallable("confirmPurchase")
        .call(mapOf("reservationId" to reservationId))
        .await()
    Unit
}
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Extend NotificationType enum + NotificationRepository interface + NotificationRepositoryImpl</name>
  <read_first>
    - app/src/main/java/com/giftregistry/domain/model/Notification.kt (current enum)
    - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt (current interface — 10 lines)
    - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt (current impl)
    - app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt (template for httpsCallable wrappers — lines 59-65)
    - app/src/main/java/com/giftregistry/di/AppModule.kt (verify FirebaseFunctions provider exists — lines 35-43)
    - app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt (RED test from Plan 16-01)
    - app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt (RED test from Plan 16-01)
  </read_first>
  <behavior>
    NotificationType extension (D-25):
    - Add 3 new enum values: INVITE_ACCEPTED_SELF, INVITE_ACCEPTED, INVITE_DECLINED. Position them BEFORE UNKNOWN so UNKNOWN remains the last sentinel.
    - Extend fromWire's when block with the 3 new wire-string → enum mappings.
    - Preserve all existing enum values and mappings verbatim.

    NotificationRepository interface (D-27):
    - Add `suspend fun acceptInvite(registryId: String): Result<Unit>`.
    - Add `suspend fun declineInvite(registryId: String): Result<Unit>`.
    - Preserve observe, observeUnreadCount, markRead verbatim.

    NotificationRepositoryImpl (D-27):
    - Add FirebaseFunctions to the constructor: `@Inject constructor(private val firestore: FirebaseFirestore, private val functions: FirebaseFunctions)`.
    - Add `acceptInvite` and `declineInvite` overrides using the verbatim shape from ReservationRepositoryImpl.confirmPurchase.
    - Use `kotlinx.coroutines.tasks.await` (already imported).
    - Preserve all existing methods and DTO mapper verbatim.
  </behavior>
  <action>
    Edit 3 files exactly as specified.

    1. EDIT app/src/main/java/com/giftregistry/domain/model/Notification.kt:
    Replace the enum block (lines 5-23) with:
    ```kotlin
    enum class NotificationType {
        INVITE,
        RESERVATION_CREATED,
        ITEM_PURCHASED,
        RESERVATION_EXPIRED,
        RE_RESERVE_WINDOW,
        INVITE_ACCEPTED_SELF, // D-25 — invitee-side "You joined" confirmation
        INVITE_ACCEPTED,       // D-25 — owner-side "{actor} accepted your invite"
        INVITE_DECLINED,       // D-25 — owner-side "{actor} declined your invite"
        UNKNOWN; // forward-compat: any future server type reads as UNKNOWN

        companion object {
            fun fromWire(raw: String?): NotificationType = when (raw) {
                "invite" -> INVITE
                "reservation_created" -> RESERVATION_CREATED
                "item_purchased" -> ITEM_PURCHASED
                "reservation_expired" -> RESERVATION_EXPIRED
                "re_reserve_window" -> RE_RESERVE_WINDOW
                "invite_accepted_self" -> INVITE_ACCEPTED_SELF
                "invite_accepted" -> INVITE_ACCEPTED
                "invite_declined" -> INVITE_DECLINED
                else -> UNKNOWN
            }
        }
    }
    ```
    Preserve the `data class Notification(...)` block (lines 25-35) and the package + comment header (lines 1-3) verbatim.

    2. EDIT app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt:
    Replace the interface block (lines 6-10) with:
    ```kotlin
    interface NotificationRepository {
        fun observe(uid: String, limit: Int = 50): Flow<List<Notification>>
        fun observeUnreadCount(uid: String): Flow<Int>
        suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit>

        /**
         * D-27 — Promote the current user from pendingInvitedUsers into invitedUsers
         * on [registryId]. Server-side transaction guarantees atomicity; this client
         * wrapper just calls the `acceptInvite` callable and surfaces success/failure
         * via Result<Unit>.
         *
         * Returns Result.failure if: NOT signed in, missing registryId, pending entry
         * not found, registry not found, network error, App Check token missing.
         */
        suspend fun acceptInvite(registryId: String): Result<Unit>

        /**
         * D-27 — Remove the current user from pendingInvitedUsers on [registryId].
         * Same failure modes as acceptInvite. Decline is symmetric with accept on
         * the wire; the difference is server-side (no membership promote, no JOINED
         * notification — just owner inbox entry).
         */
        suspend fun declineInvite(registryId: String): Result<Unit>
    }
    ```
    Preserve the package + imports verbatim.

    3. EDIT app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt:
    Add `import com.google.firebase.functions.FirebaseFunctions` to the imports (alphabetically near the existing `com.google.firebase.firestore.*` imports).

    Replace the constructor (lines 17-19) with:
    ```kotlin
    @Singleton
    class NotificationRepositoryImpl @Inject constructor(
        private val firestore: FirebaseFirestore,
        private val functions: FirebaseFunctions, // D-27 — required for accept/declineInvite callables
    ) : NotificationRepository {
    ```

    AFTER the existing markRead override (after line 82, before the `// ----- DTO → domain mapper -----` comment at line 84), insert:
    ```kotlin

        /**
         * D-27 — accept the current user's pending invite for [registryId].
         *
         * Verbatim shape of ReservationRepositoryImpl.confirmPurchase — onCall callable
         * via httpsCallable, runCatching wraps to Result<Unit>. Server-side enforces
         * App Check + auth; client only needs to be signed in and have App Check wired.
         */
        override suspend fun acceptInvite(registryId: String): Result<Unit> = runCatching {
            functions
                .getHttpsCallable("acceptInvite")
                .call(mapOf("registryId" to registryId))
                .await()
            Unit
        }

        /**
         * D-27 — decline the current user's pending invite for [registryId].
         * Symmetric to acceptInvite on the wire; server-side handles the divergence.
         */
        override suspend fun declineInvite(registryId: String): Result<Unit> = runCatching {
            functions
                .getHttpsCallable("declineInvite")
                .call(mapOf("registryId" to registryId))
                .await()
            Unit
        }

    ```
    Preserve all other methods, the DTO mapper, and the `toDomain` extension verbatim.

    Hilt note: AppModule.kt already provides `FirebaseFunctions` as a @Singleton @Provides binding (verified). Adding it as a constructor param on the @Inject-annotated impl is enough — no new module entry needed.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest" --tests "com.giftregistry.data.notifications.NotificationRepositoryImplAcceptDeclineTest" 2>&1 | tail -40</automated>
  </verify>
  <acceptance_criteria>
    - app/src/main/java/com/giftregistry/domain/model/Notification.kt contains string "INVITE_ACCEPTED_SELF,"
    - Notification.kt contains string "INVITE_ACCEPTED,"
    - Notification.kt contains string "INVITE_DECLINED,"
    - Notification.kt contains string "\"invite_accepted_self\" -> INVITE_ACCEPTED_SELF"
    - Notification.kt contains string "\"invite_accepted\" -> INVITE_ACCEPTED"
    - Notification.kt contains string "\"invite_declined\" -> INVITE_DECLINED"
    - Notification.kt contains string "UNKNOWN;" (UNKNOWN remains last)
    - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt contains string "suspend fun acceptInvite(registryId: String): Result<Unit>"
    - NotificationRepository.kt contains string "suspend fun declineInvite(registryId: String): Result<Unit>"
    - NotificationRepository.kt contains string "fun observe(" (existing method preserved)
    - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt contains string "import com.google.firebase.functions.FirebaseFunctions"
    - NotificationRepositoryImpl.kt contains string "private val functions: FirebaseFunctions"
    - NotificationRepositoryImpl.kt contains string "getHttpsCallable(\"acceptInvite\")"
    - NotificationRepositoryImpl.kt contains string "getHttpsCallable(\"declineInvite\")"
    - NotificationRepositoryImpl.kt contains string "mapOf(\"registryId\" to registryId)"
    - NotificationRepositoryImpl.kt contains string "override fun observe(" (existing method preserved)
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest" exits 0 (9 tests pass)
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.notifications.NotificationRepositoryImplAcceptDeclineTest" exits 0 (3 tests pass)
    - ./gradlew :app:compileDebugKotlin exits 0 (no compilation errors)
  </acceptance_criteria>
  <done>Domain + data layer extended; Wave 0 RED tests for these symbols flip GREEN; no Hilt module changes; no other tests broken.</done>
</task>

</tasks>

<verification>
- ./gradlew :app:testDebugUnitTest passes the 2 target test classes (12 tests total).
- ./gradlew :app:compileDebugKotlin exits 0 (entire app compiles — no callers broken).
- ./gradlew :app:testDebugUnitTest (full unit suite) exits 0 — verify no regressions in other notification or DTO tests.
- Hilt graph still resolves: ./gradlew :app:hiltAggregateDepsDebug exits 0 (if available) OR :app:compileDebugKotlin succeeds (DI errors surface at compile time).
</verification>

<success_criteria>
- 3 files modified, 3 grep-verifiable contracts in place.
- 2 RED test classes flip GREEN.
- No new files created (no new Hilt module — reuses existing AppModule.kt provider).
- Plan 16-04 can now construct NotificationRepository instances that expose acceptInvite/declineInvite for the UI layer.
</success_criteria>

<output>
After completion, create `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-03-SUMMARY.md` listing 3 file edits, the Hilt-reuse decision, and test results.
</output>
