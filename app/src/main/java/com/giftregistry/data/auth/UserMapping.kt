package com.giftregistry.data.auth

import com.giftregistry.domain.model.User
import com.google.firebase.auth.FirebaseUser

internal fun FirebaseUser.toDomain(): User = User(
    uid = uid,
    email = email,
    displayName = displayName,
    isAnonymous = isAnonymous,
)
