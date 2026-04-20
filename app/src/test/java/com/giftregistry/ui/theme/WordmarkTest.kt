package com.giftregistry.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DES-05: Wordmark builds a single AnnotatedString with two SpanStyles —
 * "giftmaison" in ink, "." in accent. Factored as a pure Kotlin helper so it
 * is unit-testable without a Compose test runtime.
 */
class WordmarkTest {
    private val ink = Color(0xFF2A2420)
    private val accent = Color(0xFFC8623A)

    @Test fun annotatedString_containsFullLiteral() {
        val annotated = wordmarkAnnotatedString(ink, accent)
        assertEquals("giftmaison.", annotated.text)
    }

    @Test fun annotatedString_hasTwoSpanStyles() {
        val annotated = wordmarkAnnotatedString(ink, accent)
        assertEquals(2, annotated.spanStyles.size)
    }

    @Test fun firstSpan_coversWordmarkBodyInInk() {
        val annotated = wordmarkAnnotatedString(ink, accent)
        val first = annotated.spanStyles[0]
        assertEquals(0, first.start)
        assertEquals("giftmaison".length, first.end)
        assertEquals(ink, first.item.color)
    }

    @Test fun secondSpan_coversPeriodInAccent() {
        val annotated = wordmarkAnnotatedString(ink, accent)
        val second = annotated.spanStyles[1]
        assertEquals("giftmaison".length, second.start)
        assertEquals("giftmaison.".length, second.end)
        assertEquals(accent, second.item.color)
    }

    @Test fun factory_isDeterministicForSameColors() {
        val a = wordmarkAnnotatedString(ink, accent)
        val b = wordmarkAnnotatedString(ink, accent)
        assertTrue(a == b)
    }
}
