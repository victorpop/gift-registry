package com.giftregistry.fcm

import com.giftregistry.domain.usecase.RegisterFcmTokenUseCase
import com.giftregistry.ui.notifications.NotificationBus
import com.giftregistry.ui.notifications.PurchasePush
import com.google.firebase.messaging.RemoteMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GiftRegistryMessagingServiceTest {

    @Test
    fun `onNewToken invokes RegisterFcmTokenUseCase with the token`() = runTest {
        val registerUseCase = mockk<RegisterFcmTokenUseCase>()
        coEvery { registerUseCase.invoke("abc") } returns Result.success(Unit)
        val bus = NotificationBus()

        val handler = MessagingHandler(registerUseCase, bus)
        handler.onNewToken("abc")
        advanceUntilIdle()

        coVerify(exactly = 1) { registerUseCase.invoke("abc") }
    }

    @Test
    fun `onMessageReceived with type=purchase emits PurchasePush to bus`() = runTest {
        val registerUseCase = mockk<RegisterFcmTokenUseCase>(relaxed = true)
        val bus = NotificationBus()
        val received = mutableListOf<PurchasePush>()
        val collectJob = TestScope(StandardTestDispatcher()).launchCollector(bus, received)

        val handler = MessagingHandler(registerUseCase, bus)
        val remoteMessage = mockk<RemoteMessage>()
        every { remoteMessage.data } returns mapOf(
            "type" to "purchase",
            "registryId" to "reg1",
            "registryName" to "Ana's Registry",
        )
        every { remoteMessage.notification } returns null

        handler.onMessageReceived(remoteMessage)
        advanceUntilIdle()

        assertEquals(1, received.size)
        assertEquals("reg1", received[0].registryId)
        assertEquals("Ana's Registry", received[0].registryName)

        collectJob.cancel()
    }

    @Test
    fun `onMessageReceived with non-purchase type is ignored`() = runTest {
        val registerUseCase = mockk<RegisterFcmTokenUseCase>(relaxed = true)
        val bus = NotificationBus()
        val received = mutableListOf<PurchasePush>()
        val collectJob = TestScope(StandardTestDispatcher()).launchCollector(bus, received)

        val handler = MessagingHandler(registerUseCase, bus)
        val remoteMessage = mockk<RemoteMessage>()
        every { remoteMessage.data } returns mapOf("type" to "other", "registryId" to "reg1")
        every { remoteMessage.notification } returns null

        handler.onMessageReceived(remoteMessage)
        advanceUntilIdle()

        assertEquals(0, received.size)
        collectJob.cancel()
    }

    @Test
    fun `onMessageReceived without registryId is ignored`() = runTest {
        val registerUseCase = mockk<RegisterFcmTokenUseCase>(relaxed = true)
        val bus = NotificationBus()
        val received = mutableListOf<PurchasePush>()
        val collectJob = TestScope(StandardTestDispatcher()).launchCollector(bus, received)

        val handler = MessagingHandler(registerUseCase, bus)
        val remoteMessage = mockk<RemoteMessage>()
        every { remoteMessage.data } returns mapOf("type" to "purchase")
        every { remoteMessage.notification } returns null

        handler.onMessageReceived(remoteMessage)
        advanceUntilIdle()

        assertEquals(0, received.size)
        collectJob.cancel()
    }
}

private fun TestScope.launchCollector(bus: NotificationBus, sink: MutableList<PurchasePush>) =
    launch { bus.events.collect { sink += it } }
