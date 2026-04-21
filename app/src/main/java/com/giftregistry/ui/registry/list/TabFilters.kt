package com.giftregistry.ui.registry.list

import com.giftregistry.domain.model.Registry
import java.util.Calendar

/**
 * SCR-07: Client-side tab filter helpers for the Home screen Active / Drafts /
 * Past segmented tabs. Pure Kotlin (no Android / no Compose) so every predicate
 * is unit-testable via TabFilterPredicateTest / DraftHeuristicTest /
 * IsPrimarySelectionTest.
 *
 * CONTEXT.md locked decisions:
 *   Active = eventDateMs == null || eventDateMs >= startOfTodayMs (inclusive boundary)
 *   Past   = eventDateMs != null && eventDateMs < startOfTodayMs   (strict)
 *   Draft  = title.isBlank() || itemCount == 0                     (heuristic)
 *   Primary = registries.maxByOrNull { it.updatedAt }?.id          (stable first on tie)
 *
 * Calendar (not java.time.LocalDate) is used because project minSdk is 23 and
 * LocalDate.atStartOfDay() requires API 26 (10-RESEARCH.md Pitfall 7).
 */

/**
 * Returns the epoch-ms timestamp of midnight in the device's default time zone
 * for the day containing [now]. Defaults to `System.currentTimeMillis()` so
 * Compose call sites can call `startOfTodayMs()` with no args.
 */
fun startOfTodayMs(now: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/** Active tab predicate — undated registries are always considered Active. */
fun Registry.isActive(todayMs: Long): Boolean =
    eventDateMs == null || eventDateMs >= todayMs

/** Past tab predicate — strict `<` comparison. Null eventDateMs is never past. */
fun Registry.isPast(todayMs: Long): Boolean =
    eventDateMs != null && eventDateMs < todayMs

/**
 * Draft heuristic — `title.isBlank() || itemCount == 0`. CONTEXT.md approved
 * this as a client-only derivation until a real `Registry.status: 'draft'`
 * field ships (deferred to v1.2). Pass `itemCount = 0` in Plan 04 until per-
 * registry stats aggregation lands (CONTEXT.md deferred).
 */
fun Registry.isDraft(itemCount: Int): Boolean =
    title.isBlank() || itemCount == 0

/**
 * Primary card selection rule — most-recently-updated registry ID, or null on
 * empty list. Mirrors AppNavigation.kt Phase 9 `primaryRegistryId` resolver so
 * both paths stay in sync. Stable: on tie, `maxByOrNull` returns the FIRST
 * matching element per Kotlin stdlib contract — pinned by unit test.
 */
fun primaryRegistryIdOf(registries: List<Registry>): String? =
    registries.maxByOrNull { it.updatedAt }?.id
