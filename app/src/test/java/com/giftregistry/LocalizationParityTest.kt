package com.giftregistry

import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File

/**
 * Wave 0 test (Plan 16-01) — D-28 contract.
 *
 * Asserts that every `<string name="...">` key in res/values/strings.xml
 * has a matching key in res/values-ro/strings.xml (and vice versa).
 *
 * This test should PASS today (Phase 16 has not added any new strings yet);
 * it serves as a guard so future plans (16-04, 16-05) cannot ship a string
 * resource without a Romanian translation.
 *
 * Unit tests run with working directory = the `app/` module root, so the
 * paths below are relative to that.
 */
class LocalizationParityTest {

    private val keyRegex = Regex("""<string\s+name="([^"]+)"""")

    private fun keys(path: String): Set<String> {
        val file = File(path)
        if (!file.exists()) error("Missing strings file at working dir ${File(".").absolutePath}: $path")
        return keyRegex.findAll(file.readText())
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun `EN and RO strings xml have matching key sets`() {
        val en = keys("src/main/res/values/strings.xml")
        val ro = keys("src/main/res/values-ro/strings.xml")
        val missingFromRo = en - ro
        val missingFromEn = ro - en
        assertEquals(
            "Keys missing from RO (values-ro/strings.xml): $missingFromRo; " +
                "keys missing from EN (values/strings.xml): $missingFromEn",
            emptySet<String>(),
            missingFromRo + missingFromEn,
        )
    }
}
