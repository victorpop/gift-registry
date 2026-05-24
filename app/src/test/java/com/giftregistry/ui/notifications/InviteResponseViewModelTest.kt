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

/**
 * Wave 0 RED test (Plan 16-01) — D-07 state machine contract for the
 * accept/decline bottom sheet ViewModel.
 *
 * Expected state machine:
 *   Idle ──accept()──▶ Submitting ──success──▶ AcceptedSuccess
 *                                  └─failure─▶ Error(action=Accept)
 *   Idle ──decline()─▶ Submitting ──success──▶ DeclinedSuccess
 *                                  └─failure─▶ Error(action=Decline)
 *   Error ──retry()──▶ Submitting ──...─▶ AcceptedSuccess | Error
 *
 * `InviteResponseViewModel` does NOT yet exist — this file will fail to
 * compile until Plan 16-04 ships the ViewModel. That compile failure IS
 * the RED state.
 */
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
    fun `accept failure transitions Idle to Submitting to Error with Accept action`() = runTest {
        val repo = mockk<NotificationRepository>()
        coEvery { repo.acceptInvite("reg-1") } returns Result.failure(RuntimeException("network"))
        val vm = InviteResponseViewModel(repo)

        vm.state.test {
            awaitItem() // Idle
            vm.accept("reg-1")
            awaitItem() // Submitting
            val err = awaitItem()
            assertTrue(err is InviteResponseViewModel.State.Error)
            assertEquals(
                InviteResponseViewModel.Action.Accept,
                (err as InviteResponseViewModel.State.Error).action,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decline success transitions Idle to Submitting to DeclinedSuccess`() = runTest {
        val repo = mockk<NotificationRepository>()
        coEvery { repo.declineInvite("reg-1") } returns Result.success(Unit)
        val vm = InviteResponseViewModel(repo)

        vm.state.test {
            awaitItem() // Idle
            vm.decline("reg-1")
            assertTrue(awaitItem() is InviteResponseViewModel.State.Submitting)
            assertTrue(awaitItem() is InviteResponseViewModel.State.DeclinedSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decline failure transitions to Error with Decline action`() = runTest {
        val repo = mockk<NotificationRepository>()
        coEvery { repo.declineInvite("reg-1") } returns Result.failure(RuntimeException("boom"))
        val vm = InviteResponseViewModel(repo)

        vm.state.test {
            awaitItem() // Idle
            vm.decline("reg-1")
            awaitItem() // Submitting
            val err = awaitItem()
            assertTrue(err is InviteResponseViewModel.State.Error)
            assertEquals(
                InviteResponseViewModel.Action.Decline,
                (err as InviteResponseViewModel.State.Error).action,
            )
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
