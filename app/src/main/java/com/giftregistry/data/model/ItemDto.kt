package com.giftregistry.data.model

import com.google.firebase.Timestamp

/**
 * Firestore item document DTO. Schema is owned by the Cloud Function
 * `createReservation` (functions/src/reservation/createReservation.ts) — the
 * canonical writer. This DTO MUST match what the function writes:
 *   reservedBy: string (giverEmail)
 *   reservedAt: Timestamp (FieldValue.serverTimestamp())
 *   expiresAt:  Timestamp (Timestamp.fromMillis(...))
 *
 * `reservedAt` is null on the optimistic local snapshot before the server
 * resolves serverTimestamp(); the Timestamp? type already handles that.
 * All three reservation fields are null when status == "available".
 *
 * Conversion to the domain `Item.expiresAt: Long?` happens in
 * `ItemRepositoryImpl.toDomain`. Keep DTO types Firestore-native here.
 */
data class ItemDto(
    val id: String = "",
    val title: String = "",
    val originalUrl: String = "",
    val affiliateUrl: String = "",
    val imageUrl: String? = null,
    val price: String? = null,
    val notes: String? = null,
    val status: String = "available",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val reservedBy: String? = null,
    val reservedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
)
