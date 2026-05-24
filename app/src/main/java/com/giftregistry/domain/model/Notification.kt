package com.giftregistry.domain.model

// Zero Firebase imports — pure Kotlin domain model.

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

data class Notification(
    val id: String,
    val type: NotificationType,
    val titleKey: String,
    val bodyKey: String,
    val titleFallback: String,
    val bodyFallback: String,
    val payload: Map<String, String?>, // flattened string-only view; all server values coerced to String? client-side
    val createdAtMs: Long,
    val readAtMs: Long?,
)
