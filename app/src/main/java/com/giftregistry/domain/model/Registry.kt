package com.giftregistry.domain.model

data class Registry(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val occasion: String = "",
    val description: String = "",
    val isPublic: Boolean = true,
    val invitedUsers: Map<String, Boolean> = emptyMap(),
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = 0L
)
