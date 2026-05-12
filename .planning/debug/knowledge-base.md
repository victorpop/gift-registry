# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## item-image-fetch-broken — og:image served as http:// blocked by Android cleartext policy
- **Date:** 2026-04-28
- **Error patterns:** image broken, placeholder, http, cleartext, Coil, og:image, imageUrl, AsyncImage, error painter, fetch, mobexpert
- **Root cause:** Cloud Function returns og:image verbatim (no scheme normalization). Retailer og:image uses http://. Android targetSdk=36 blocks cleartext HTTP by default — no usesCleartextTraffic, no network_security_config for external domains. Coil silently falls back to the error painter.
- **Fix:** Added normalizeImageUrl helper in fetchOgMetadata.ts rewriting http:// → https:// before returning imageUrl. Also applied the same normalization in ItemRepositoryImpl.fetchOgMetadata (client-side defense-in-depth).
- **Files changed:** functions/src/registry/fetchOgMetadata.ts, app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt
---

## android-nav-crashes — Kotlin 2.x removeLast() / JDK 21 SequencedCollection clash on SnapshotStateList
- **Date:** 2026-05-12
- **Error patterns:** NoSuchMethodError, removeLast, SnapshotStateList, SequencedCollection, backStack, AppNavigation, crash, navigation, back arrow, physical device, Huawei, targetSdkVersion 36
- **Root cause:** Kotlin 2.x resolves `MutableList<T>.removeLast()` to `java.util.SequencedCollection.removeLast()` (JDK 21 API). `SnapshotStateList` does not implement `SequencedCollection`; ART on devices with older runtime patch levels does not expose that interface, producing `NoSuchMethodError` at every `backStack.removeLast()` call site. Secondary bugs: Activity-scoped ViewModels (`CreateRegistryViewModel` key="new", `AddItemViewModel` key=registryId) never reset their saved-ID `StateFlow` after navigation, causing spurious immediate back-navigation on second screen entry.
- **Fix:** Replace all `backStack.removeLast()` with `backStack.removeAt(backStack.lastIndex)` — `List.removeAt(Int)` is a Java 1.2 API available on every ART version. Add `size > 1` guards on all in-screen `onBack` lambdas. Add `clearSavedRegistryId()` / `clearSavedItemId()` to the respective ViewModels and call them before navigation in the consuming `LaunchedEffect`.
- **Files changed:** app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt, app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt, app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
---
