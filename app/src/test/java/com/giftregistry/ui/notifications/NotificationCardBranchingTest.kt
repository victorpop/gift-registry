package com.giftregistry.ui.notifications

import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.model.NotificationType
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Wave 0 RED test (Plan 16-01) — D-11 contract.
 *
 * Pure-Kotlin predicate `shouldOpenInviteSheet(notification)` decides whether
 * tapping a notification card opens the accept/decline bottom sheet or falls
 * back to the legacy navigate-to-registry behaviour.
 *
 * Branching rules:
 *  - type == INVITE AND payload["pendingEntryKey"] != null → open sheet
 *  - type == INVITE AND payload["pendingEntryKey"] == null → legacy fallback (D-11)
 *  - any other type → never opens sheet
 *
 * Production helper `shouldOpenInviteSheet` does NOT exist yet — this file
 * will fail to compile until Plan 16-04 ships the predicate. The compile
 * failure IS the RED state.
 */
class NotificationCardBranchingTest {

    private fun notif(type: NotificationType, pendingEntryKey: String? = null): Notification = Notification(
        id = "n1",
        type = type,
        titleKey = "k",
        bodyKey = "k",
        titleFallback = "t",
        bodyFallback = "b",
        payload = if (pendingEntryKey != null) mapOf("pendingEntryKey" to pendingEntryKey) else emptyMap(),
        createdAtMs = 0L,
        readAtMs = null,
    )

    @Test fun `INVITE with uid pendingEntryKey opens sheet`() {
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

    @Test fun `INVITE_ACCEPTED does not open sheet`() {
        assertFalse(shouldOpenInviteSheet(notif(NotificationType.INVITE_ACCEPTED, "uid-1")))
    }

    @Test fun `INVITE_DECLINED does not open sheet`() {
        assertFalse(shouldOpenInviteSheet(notif(NotificationType.INVITE_DECLINED, "uid-1")))
    }

    @Test fun `RESERVATION_CREATED does not open sheet`() {
        assertFalse(shouldOpenInviteSheet(notif(NotificationType.RESERVATION_CREATED)))
    }

    @Test fun `ITEM_PURCHASED does not open sheet`() {
        assertFalse(shouldOpenInviteSheet(notif(NotificationType.ITEM_PURCHASED)))
    }

    @Test fun `UNKNOWN does not open sheet`() {
        assertFalse(shouldOpenInviteSheet(notif(NotificationType.UNKNOWN)))
    }
}
