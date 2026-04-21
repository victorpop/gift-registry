package com.giftregistry.ui.registry.list

import com.giftregistry.domain.model.Registry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCR-07: Drafts heuristic — `title.isBlank() || itemCount == 0`.
 * CONTEXT.md locks this as a client-only heuristic until a real
 * `Registry.status: 'draft'` field ships (deferred to v1.2).
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships Registry.isDraft(itemCount: Int).
 */
class DraftHeuristicTest {
    @Test fun blankTitle_isDraft() =
        assertTrue(Registry(title = "").isDraft(itemCount = 3))

    @Test fun whitespaceTitle_isDraft() =
        assertTrue(Registry(title = "   ").isDraft(itemCount = 3))

    @Test fun emptyItems_isDraft() =
        assertTrue(Registry(title = "Ana's list").isDraft(itemCount = 0))

    @Test fun bothPresent_isNotDraft() =
        assertFalse(Registry(title = "Ana's list").isDraft(itemCount = 5))

    @Test fun blankTitleAndEmptyItems_isDraft() =
        assertTrue(Registry(title = "").isDraft(itemCount = 0))
}
