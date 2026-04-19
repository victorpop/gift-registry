package com.giftregistry.domain.fcm

/**
 * Phase 6 (D-09, D-11): FCM token lifecycle for the authenticated user.
 * Tokens are stored at `users/{uid}/fcmTokens/{tokenId}` with tokenId == token value.
 * Anonymous callers are no-ops (tokens are tied to a user account).
 */
interface FcmTokenRepository {
    /** Register the current device's FCM token for the currently signed-in user, if any. */
    suspend fun registerToken(token: String): Result<Unit>

    /** Remove a token (e.g., on sign-out) — no-op when not signed in. */
    suspend fun deleteToken(token: String): Result<Unit>
}
