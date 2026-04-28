package com.giftregistry.ui.registry.create

import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.data.storage.CoverImageProcessor
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.User
import com.giftregistry.domain.registry.RegistryRepository
import com.giftregistry.domain.storage.StorageRepository
import com.giftregistry.domain.usecase.CreateRegistryUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.UpdateRegistryUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

/**
 * quick-260428-s3b — RED → GREEN tests for the Event Time wiring on
 * [CreateRegistryViewModel].
 *
 * Pins:
 * - QUICK-S3B-02: `setEventTime(hour, minute)` is a no-op when `eventDateMs` is
 *   null (time without a date anchor is meaningless); when a date IS set, it
 *   mutates only the hour/minute portion of the same `Long` and flips
 *   `eventTimeSet=true`.
 * - QUICK-S3B-03: edit-mode hydration sets `eventTimeSet=true` IFF the loaded
 *   timestamp's HOUR_OF_DAY or MINUTE is non-zero (Calendar-decoded — NOT raw
 *   `% 86_400_000L`, which is timezone-incorrect for non-UTC zones).
 *
 * Mirrors the 7-arg constructor + SavedStateHandle setup from
 * [CreateRegistryViewModelCoverTest]. Timestamps are built via Calendar so
 * the assertions are timezone-stable on any developer machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRegistryViewModelEventTimeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        authRepository: AuthRepository = mockk(relaxed = true) {
            every { currentUser } returns User(uid = "uid-1", email = null, displayName = null, isAnonymous = false)
        },
        createRegistryUseCase: CreateRegistryUseCase = mockk(relaxed = true),
        updateRegistryUseCase: UpdateRegistryUseCase = mockk(relaxed = true),
        observeRegistryUseCase: ObserveRegistryUseCase = mockk(relaxed = true),
        registryRepository: RegistryRepository = mockk(relaxed = true) {
            every { newRegistryId() } returns "minted-id-1"
        },
        storageRepository: StorageRepository = mockk(relaxed = true),
        coverImageProcessor: CoverImageProcessor = mockk(relaxed = true),
        registryId: String? = null,
    ): CreateRegistryViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (registryId != null) set("registryId", registryId)
        }
        return CreateRegistryViewModel(
            authRepository = authRepository,
            createRegistryUseCase = createRegistryUseCase,
            updateRegistryUseCase = updateRegistryUseCase,
            observeRegistryUseCase = observeRegistryUseCase,
            registryRepository = registryRepository,
            storageRepository = storageRepository,
            coverImageProcessor = coverImageProcessor,
            savedStateHandle = savedStateHandle,
        )
    }

    /**
     * QUICK-S3B-02 — setEventTime is gated on eventDateMs being non-null. A
     * time-of-day without a date anchor is nonsense; the VM must reject it
     * silently AND keep eventTimeSet=false so the time field stays empty.
     */
    @Test
    fun setEventTime_doesNothing_whenEventDateMsIsNull() = runTest {
        val vm = buildViewModel()
        // Pre-condition: fresh VM, no date picked yet.
        assertNull("Pre-condition: eventDateMs starts null", vm.eventDateMs.value)
        assertEquals(
            "Pre-condition: eventTimeSet starts false",
            false,
            vm.eventTimeSet.value,
        )

        vm.setEventTime(14, 30)

        // CONTRACT: gated — both states unchanged.
        assertNull("eventDateMs must stay null when no date anchor", vm.eventDateMs.value)
        assertEquals(
            "eventTimeSet must stay false when setEventTime is called without a date",
            false,
            vm.eventTimeSet.value,
        )
    }

    /**
     * QUICK-S3B-02 — when eventDateMs IS set, setEventTime must mutate ONLY the
     * hour/minute portion of the same Long (preserve year/month/day) and flip
     * eventTimeSet=true. Decoded via Calendar to stay timezone-stable.
     */
    @Test
    fun setEventTime_mutatesHourAndMinute_preservingDate_whenEventDateMsSet() = runTest {
        val vm = buildViewModel()
        // 2026-06-15 00:00:00 local — built via Calendar so the test is
        // timezone-independent (raw UTC ms would shift on non-UTC machines).
        val anchor = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        vm.eventDateMs.value = anchor

        vm.setEventTime(14, 30)

        // Decode the resulting Long via Calendar and assert each field.
        val resultCal = Calendar.getInstance().apply { timeInMillis = vm.eventDateMs.value!! }
        assertEquals("year preserved", 2026, resultCal.get(Calendar.YEAR))
        assertEquals("month preserved (June=5)", Calendar.JUNE, resultCal.get(Calendar.MONTH))
        assertEquals("day preserved", 15, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals("hour set to 14", 14, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("minute set to 30", 30, resultCal.get(Calendar.MINUTE))
        assertEquals("seconds zeroed", 0, resultCal.get(Calendar.SECOND))
        assertEquals("millis zeroed", 0, resultCal.get(Calendar.MILLISECOND))

        assertEquals(
            "eventTimeSet must flip true after a successful setEventTime",
            true,
            vm.eventTimeSet.value,
        )
    }

    /**
     * QUICK-S3B-03 — edit-mode hydration: a persisted Registry whose eventDateMs
     * encodes a non-midnight time-of-day must land in the VM with both
     * eventDateMs hydrated AND eventTimeSet=true so the UI's time field renders
     * the formatted time on first paint.
     */
    @Test
    fun editMode_hydration_setsEventTimeSet_whenLoadedTimestampHasNonZeroHourMinute() = runTest {
        val storedMs = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val storedRegistry = Registry(
            id = "reg-with-time",
            ownerId = "uid-1",
            title = "Wedding bash",
            occasion = "Wedding",
            eventDateMs = storedMs,
        )
        val observeRegistryUseCase = mockk<ObserveRegistryUseCase>(relaxed = true)
        coEvery { observeRegistryUseCase.invoke("reg-with-time") } returns flowOf(storedRegistry)

        val vm = buildViewModel(
            observeRegistryUseCase = observeRegistryUseCase,
            registryId = "reg-with-time",
        )
        advanceUntilIdle() // drain init { viewModelScope.launch { ... } }

        assertEquals(
            "edit-mode: eventDateMs hydrates verbatim from Firestore",
            storedMs,
            vm.eventDateMs.value,
        )
        assertEquals(
            "QUICK-S3B-03: non-midnight loaded timestamp must flip eventTimeSet=true",
            true,
            vm.eventTimeSet.value,
        )
    }

    /**
     * QUICK-S3B-03 — counter-case: a persisted Registry whose eventDateMs is
     * exactly midnight (00:00) must hydrate with eventTimeSet=false so the
     * time field stays empty (we cannot distinguish "user picked midnight" from
     * "user picked only a date" — and the bug report says display empty in
     * the latter case, which dominates real-world data).
     */
    @Test
    fun editMode_hydration_leavesEventTimeSetFalse_whenLoadedTimestampIsMidnight() = runTest {
        val storedMs = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val storedRegistry = Registry(
            id = "reg-midnight",
            ownerId = "uid-1",
            title = "Date-only registry",
            occasion = "Wedding",
            eventDateMs = storedMs,
        )
        val observeRegistryUseCase = mockk<ObserveRegistryUseCase>(relaxed = true)
        coEvery { observeRegistryUseCase.invoke("reg-midnight") } returns flowOf(storedRegistry)

        val vm = buildViewModel(
            observeRegistryUseCase = observeRegistryUseCase,
            registryId = "reg-midnight",
        )
        advanceUntilIdle()

        assertEquals(
            "edit-mode: eventDateMs hydrates verbatim from Firestore",
            storedMs,
            vm.eventDateMs.value,
        )
        assertEquals(
            "QUICK-S3B-03: midnight loaded timestamp must leave eventTimeSet=false",
            false,
            vm.eventTimeSet.value,
        )
    }

    /**
     * QUICK-S3B-03 — counter-case: registry whose eventDateMs is null (no date
     * picked at all) must hydrate with eventTimeSet=false. No Calendar decode
     * is attempted on a null timestamp.
     */
    @Test
    fun editMode_hydration_leavesEventTimeSetFalse_whenEventDateMsIsNull() = runTest {
        val storedRegistry = Registry(
            id = "reg-no-date",
            ownerId = "uid-1",
            title = "No date registry",
            occasion = "Wedding",
            eventDateMs = null,
        )
        val observeRegistryUseCase = mockk<ObserveRegistryUseCase>(relaxed = true)
        coEvery { observeRegistryUseCase.invoke("reg-no-date") } returns flowOf(storedRegistry)

        val vm = buildViewModel(
            observeRegistryUseCase = observeRegistryUseCase,
            registryId = "reg-no-date",
        )
        advanceUntilIdle()

        assertNull("edit-mode: null eventDateMs hydrates as null", vm.eventDateMs.value)
        assertEquals(
            "QUICK-S3B-03: null eventDateMs must leave eventTimeSet=false",
            false,
            vm.eventTimeSet.value,
        )
    }
}
