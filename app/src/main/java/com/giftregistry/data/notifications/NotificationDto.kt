package com.giftregistry.data.notifications

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class NotificationDto(
    val id: String = "",
    val type: String = "",
    val titleKey: String = "",
    val bodyKey: String = "",
    val title: String = "",
    val body: String = "",
    val payload: Map<String, Any?> = emptyMap(),
    @ServerTimestamp val createdAt: Date? = null,
    val readAt: Date? = null,
)
