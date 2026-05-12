package com.giftregistry.domain.auth

import com.giftregistry.domain.model.User

sealed interface AuthStateEvent {
    /**
     * The synchronous emission Firebase's AuthStateListener produces immediately on
     * attach. On cold start, `user` may be null even when a cached user exists — Firebase
     * has not yet finished restoring from disk. Consumers MUST keep showing a loading
     * state when this event carries a null user; the next Changed event will settle it.
     */
    data class Initial(val user: User?) : AuthStateEvent

    /**
     * Any auth-state emission after the synchronous attach: restoration completed,
     * sign-in, sign-out, etc. Consumers should treat this as the authoritative state.
     */
    data class Changed(val user: User?) : AuthStateEvent
}
