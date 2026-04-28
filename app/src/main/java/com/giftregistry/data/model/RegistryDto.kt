package com.giftregistry.data.model

data class RegistryDto(
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
    // Phase 12 — Pitfall 1 fix (Plan 02 Task 1). RegistryDto previously dropped
    // imageUrl on Firestore reads because the field was missing from the data
    // class, even though Registry.imageUrl exists in the domain model and
    // RegistryRepositoryImpl.toMap/toUpdateMap (also Phase 12) write the value.
    val imageUrl: String? = null,
    // Typed as Map<String, Any?> rather than Map<String, Boolean> so a legacy
    // document with a nested-map value (from an earlier bug in inviteToRegistry
    // where dotted email keys were interpreted as nested paths) deserializes
    // instead of crashing the whole snapshot listener. The domain mapper in
    // RegistryRepositoryImpl coerces each entry back to a Boolean.
    val invitedUsers: Map<String, Any?> = emptyMap(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
