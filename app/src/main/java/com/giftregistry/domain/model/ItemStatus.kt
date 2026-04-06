package com.giftregistry.domain.model

enum class ItemStatus {
    AVAILABLE,
    RESERVED,
    PURCHASED;

    companion object {
        fun fromString(value: String): ItemStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AVAILABLE
    }
}
