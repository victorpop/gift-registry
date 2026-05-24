package com.giftregistry.domain.model

import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Wave 0 RED test (Plan 16-01) — locks in the wire-string contract for the
 * three new invite-lifecycle notification types added by Phase 16 (D-25).
 *
 * Production code in [NotificationType] does NOT yet declare
 * INVITE_ACCEPTED_SELF / INVITE_ACCEPTED / INVITE_DECLINED — this file
 * will fail to compile until Plan 16-03 extends the enum. That compile
 * failure IS the RED state.
 *
 * Regression cases at the bottom ensure the existing five mappings remain
 * untouched when the enum is extended.
 */
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

    // ───── Regression — existing mappings must remain intact ─────

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
