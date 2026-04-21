package com.giftregistry.domain.model

data class Registry(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val occasion: String = "",
    val visibility: String = "public",
    val eventDateMs: Long? = null,
    val eventLocation: String? = null,
    val description: String? = null,
    val locale: String = "en",
    val notificationsEnabled: Boolean = true,
    val invitedUsers: Map<String, Boolean> = emptyMap(),
    val imageUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
