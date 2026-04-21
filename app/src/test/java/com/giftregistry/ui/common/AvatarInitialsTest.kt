package com.giftregistry.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SCR-06 + SCR-07: Avatar initials derivation with fallback chain.
 *
 *   1) displayName.split(\\s+).take(2).map { it.first().uppercaseChar() }.joinToString("")
 *   2) else email?.firstOrNull()?.uppercaseChar()?.toString()
 *   3) else "?"
 *
 * Used on Home top bar (30 dp avatar) and in any future profile surface.
 * Kept as a top-level pure fn so it is unit-testable without Compose runtime.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships AvatarInitials.kt with
 * top-level `toAvatarInitials(displayName: String?, email: String?): String`
 * in com.giftregistry.ui.common.
 */
class AvatarInitialsTest {
    @Test fun twoWordDisplayName_returnsTwoLetters() =
        assertEquals("AP", toAvatarInitials(displayName = "Ana Popescu", email = null))

    @Test fun threeWordDisplayName_returnsFirstTwo() =
        assertEquals("AM", toAvatarInitials(displayName = "Ana Maria Popescu", email = null))

    @Test fun singleWordDisplayName_returnsSingleLetter() =
        assertEquals("A", toAvatarInitials(displayName = "Ana", email = null))

    @Test fun lowercaseDisplayName_returnsUppercase() =
        assertEquals("AP", toAvatarInitials(displayName = "ana popescu", email = null))

    @Test fun nullDisplayName_fallsBackToEmail() =
        assertEquals("A", toAvatarInitials(displayName = null, email = "ana@example.com"))

    @Test fun blankDisplayName_fallsBackToEmail() =
        assertEquals("B", toAvatarInitials(displayName = "   ", email = "bob@example.com"))

    @Test fun nullDisplayNameAndNullEmail_returnsQuestionMark() =
        assertEquals("?", toAvatarInitials(displayName = null, email = null))

    @Test fun nullDisplayNameAndBlankEmail_returnsQuestionMark() =
        assertEquals("?", toAvatarInitials(displayName = null, email = ""))

    @Test fun extraWhitespace_isTrimmed() =
        assertEquals("AP", toAvatarInitials(displayName = "  Ana   Popescu  ", email = null))
}
