---
phase: quick-260530-nx5
plan: 01
subsystem: android-ui
tags: [discover, navigation, add-item, prefill, unit-test, tdd]
dependency_graph:
  requires: [quick-260428-iny (fromAddSheet picker), quick-260530-ncw (grid layout)]
  provides: [DiscoverCard → AddItem prefill flow]
  affects: [AppNavKeys, AddItemViewModel, DiscoverProductCard, DiscoverScreen, AppNavigation]
tech_stack:
  added: []
  patterns: [TDD red/green, hoisted nav callback, SavedStateHandle prefill, hasPrefill() gate]
key_files:
  created:
    - app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelPrefillTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
decisions:
  - "Omit fabShadow + paper ring on small button: they look heavy at 20dp; card provides visual separation"
  - "32dp hit target (not full M3 48dp floor): compromise to avoid crowding retailer row at 2-col widths"
  - "prefillRetailerName + prefillCurrency accepted but not yet wired to Item model: future retailer-chip work"
  - "hasPrefill() checks title|url|imageUrl|price only (not retailerName/currency): those 4 are the visible form fields"
  - "TDD: RED committed first (a2fa179), GREEN as separate commit (237e2ad)"
metrics:
  duration: ~5 min
  completed_date: "2026-05-30"
  tasks_completed: 2
  tasks_total: 3
  files_changed: 8
---

# Phase quick-260530-nx5 Plan 01: Discover Add-to-Registry Button + AddItem Prefill Summary

**One-liner:** Terracotta + button on every Discover product card that opens AddItemScreen with Serper-supplied product data pre-filled and skips the OG-metadata Cloud Function fetch.

## What Was Built

### AddItemKey Extension (AppNavKeys.kt)

Added 6 optional nullable fields to the existing `@Serializable data class AddItemKey`:

```kotlin
val prefillTitle: String? = null
val prefillUrl: String? = null
val prefillImageUrl: String? = null
val prefillPrice: String? = null
val prefillRetailerName: String? = null
val prefillCurrency: String? = null
```

All existing call sites compile unchanged (all fields default to null).

### AddItemViewModel hasPrefill() Branch (AddItemViewModel.kt)

Reads the 6 new fields from `SavedStateHandle` as private vals. A `hasPrefill()` helper returns `true` when any of title, url, imageUrl, or price is non-blank.

In `init {}`, the prefill branch runs **before** the `initialUrl` block:

```
if (hasPrefill()) {
    // hydrate url, title, imageUrl, price directly
    // prime lastFetchedUrl → auto-fetch debounce pipeline DOES NOT FIRE
    // NO onFetchMetadata() call
} else if (initialUrl.isNotBlank()) {
    // existing Store-Browser path (unchanged)
}
```

The critical design: priming `lastFetchedUrl = prefillUrl.trim()` in the prefill branch ensures the 700ms debounce collector sees `trimmed == lastFetchedUrl` on its first emission and returns early — so the Cloud Function is never called even with a valid URL in the field.

### AddItemScreen Parameter Threading (AddItemScreen.kt)

Extended the `@Composable fun AddItemScreen(...)` signature with the 6 prefill params (all `String? = null`, placed before the lambda params). Each is forwarded into `hiltViewModelWithNavArgs` with `?:""` coercion, matching the existing pattern.

### Small Button Composable (DiscoverProductCard.kt)

New private `DiscoverAddToRegistryButton` composable:

- **32dp outer Box** — click target with semantics (`Role.Button`, `contentDescription = stringResource(R.string.discover_card_add_to_registry)`)
- **20dp inner Box** — `colors.accent` CircleShape background (same terracotta token as GiftMaisonFab)
- **14dp Icon** — `Icons.Default.Add`, tinted `colors.accentInk` (same token as FAB plus icon)
- **No fabShadow, no paper ring** — rationale: the 4dp border + drop shadow look heavy at 20dp; the card surface already provides visual separation

The button sits inside a `Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = CenterVertically)` in the retailer-name row. The retailer `Text` gets `Modifier.weight(1f)` so the button is pushed right regardless of label length. When `retailerName` is blank, a `Spacer(Modifier.weight(1f))` holds the position so the button always renders right-aligned.

**Gesture isolation:** Compose routes the tap to the innermost `Modifier.clickable` consumer. The `DiscoverAddToRegistryButton`'s `Modifier.clickable(onClick = onClick)` captures the gesture before it propagates to the parent `Card.onClick` — so the outer browser-intent tap keeps working.

### DiscoverScreen Callback Hoisting (DiscoverScreen.kt)

```kotlin
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    onAddToRegistry: (DiscoverProduct) -> Unit = {},  // new param, default no-op
)
```

Both `DiscoverProductCard` call sites (search/loaded and community/loaded) now pass `onAddToRegistry = onAddToRegistry`.

### AppNavigation Wiring (AppNavigation.kt)

`entry<DiscoverKey>` now provides the `onAddToRegistry` lambda:

```kotlin
entry<DiscoverKey> {
    DiscoverScreen(
        onAddToRegistry = { product ->
            backStack.add(AddItemKey(
                registryId = null,
                fromAddSheet = true,
                prefillTitle = product.title,
                prefillUrl = product.retailerUrl,
                prefillImageUrl = product.imageUrl,
                prefillPrice = if (product.price > 0.0) product.price.toString() else null,
                prefillRetailerName = product.retailerName,
                prefillCurrency = product.currency,
            ))
        },
    )
}
```

`entry<AddItemKey>` forwards all 6 prefill fields from `key.*` into `AddItemScreen(...)`.

### Strings Added

| File | Key | Value |
|------|-----|-------|
| `values/strings.xml` | `discover_card_add_to_registry` | Add to a registry |
| `values-ro/strings.xml` | `discover_card_add_to_registry` | Adaugă la o listă (entity-encoded) |

### Unit Test Pinning the No-OG-Fetch Contract (AddItemViewModelPrefillTest.kt)

3 test cases using the same pattern as `AddItemViewModelAutoFetchTest`:

1. **`prefill values hydrate form state and do not trigger OG fetch`** — asserts title/url/imageUrl/price values AND `coVerify(exactly = 0) { fetch.invoke(any()) }`
2. **`no prefill values leaves form blank and does not trigger OG fetch`** — baseline fromAddSheet sanity
3. **`prefill takes precedence — even with prefillUrl set, fetchOgMetadata is NOT called`** — the critical case; advances 2000ms past debounce

## Commits

| Hash | Type | Description |
|------|------|-------------|
| `a2fa179` | test (RED) | Add failing test for AddItemViewModel prefill path |
| `237e2ad` | feat (GREEN) | Add prefill path to AddItemKey/AddItemViewModel (skips OG fetch) |
| `4fa4b61` | feat | Small add-to-registry button on Discover cards (+strings, +nav wiring) |

## Outstanding: Task 3 — On-device Visual + Functional Verification

**Status:** AWAITING HUMAN VERIFY

The checkpoint (Task 3) requires on-device verification on emulator-5554. This cannot be automated.

**APK:** `/Users/victorpop/ai-projects/gift-registry/app/build/outputs/apk/debug/app-debug.apk`

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — all 6 prefill fields are fully wired from nav key → DiscoverProduct → AddItemKey → AddItemScreen → AddItemViewModel → form state. `prefillRetailerName` and `prefillCurrency` are stored in SavedStateHandle but not yet surfaced in the Item model UI (the plan documents this as intentional deferral).
