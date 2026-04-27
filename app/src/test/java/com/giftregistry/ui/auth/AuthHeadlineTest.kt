package com.giftregistry.ui.auth

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCR-06: Auth headline AnnotatedString construction.
 *
 * Contract (updated per design Image #7 and user constraint):
 *   Line 1 "Start your"        → SpanStyle(color = ink)      ← solid black, not inkSoft
 *   \n
 *   Line 2 "first registry."   → SpanStyle(color = accent)   ← entire line incl. period
 *
 * Same pattern as wordmarkAnnotatedString in GiftMaisonWordmark.kt — factory
 * is pure Kotlin so it can be unit-tested without Compose UI test runtime.
 */
class AuthHeadlineTest {

    private val ink = Color(0xFF2A2420)
    private val inkSoft = Color(0xFF6A5E52)
    private val accent = Color(0xFFC8623A)

    private fun build() = authHeadlineAnnotatedString(
        prefix = "Start your",
        accent = "first registry",
        ink = ink,
        accentColor = accent,
        inkSoft = inkSoft,
    )

    @Test fun text_containsBothLines() {
        val result = build()
        assertTrue(
            "Expected headline to contain 'Start your' and 'first registry.' but got: ${result.text}",
            result.text.contains("Start your") && result.text.contains("first registry.")
        )
    }

    @Test fun text_hasNewlineBetweenLines() {
        assertTrue(
            "Expected 'Start your\\nfirst registry.' but got: ${build().text}",
            build().text.contains("Start your\nfirst registry.")
        )
    }

    @Test fun period_isAccentColored() {
        val result = build()
        val periodStart = result.text.length - 1  // last char
        val span = result.spanStyles.firstOrNull { span ->
            periodStart in span.start until span.end
        }
        assertEquals(
            "Trailing period must be accent-coloured",
            accent, span?.item?.color
        )
    }

    @Test fun firstRegistryLine_isAccentColored() {
        val result = build()
        val startIdx = result.text.indexOf("first registry.")
        val span = result.spanStyles.firstOrNull { span ->
            startIdx in span.start until span.end
        }
        assertEquals(
            "'first registry.' run must be entirely accent-coloured (period included)",
            accent, span?.item?.color
        )
    }

    @Test fun startYourLine_isInkColored() {
        val result = build()
        val startIdx = result.text.indexOf("Start your")
        val span = result.spanStyles.firstOrNull { span ->
            startIdx in span.start until span.end
        }
        assertEquals(
            "'Start your' must be solid ink-coloured (per design Image #7, not inkSoft)",
            ink, span?.item?.color
        )
    }

    @Test fun periodIsLastCharacter() =
        assertEquals('.', build().text.last())
}
