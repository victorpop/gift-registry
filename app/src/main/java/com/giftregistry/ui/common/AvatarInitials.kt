package com.giftregistry.ui.common

/**
 * SCR-06 + SCR-07: Avatar initials derivation for the Home top bar.
 *
 * Fallback chain (pure Kotlin — no Android, unit-tested):
 *   1) displayName is non-null AND non-blank → trim, split on whitespace (regex),
 *      take first 2 tokens, map each to first char uppercased, join.
 *   2) else email first char uppercased (if email is non-null AND non-blank)
 *   3) else "?"
 *
 * Examples:
 *   toAvatarInitials("Ana Popescu", null)          → "AP"
 *   toAvatarInitials("Ana Maria Popescu", null)    → "AM"   (take 2)
 *   toAvatarInitials("Ana", null)                  → "A"
 *   toAvatarInitials("ana popescu", null)          → "AP"   (uppercased)
 *   toAvatarInitials(null, "ana@example.com")      → "A"
 *   toAvatarInitials("  ", "bob@example.com")      → "B"    (blank → fallback)
 *   toAvatarInitials(null, null)                   → "?"
 *   toAvatarInitials("  Ana  Popescu ", null)      → "AP"   (trimmed + split)
 */
fun toAvatarInitials(displayName: String?, email: String?): String {
    if (!displayName.isNullOrBlank()) {
        val initials = displayName.trim()
            .split("\\s+".toRegex())
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
        if (initials.isNotEmpty()) return initials
    }
    val firstEmailChar = email?.firstOrNull()?.uppercaseChar()
    return firstEmailChar?.toString() ?: "?"
}
