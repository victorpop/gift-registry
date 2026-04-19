# Phase 7: Romanian Store Browser — Research

**Researched:** 2026-04-19
**Domain:** Android WebView in Jetpack Compose, Firestore config reads, DataStore preferences, Navigation3 back stack, Material3 FAB menus
**Confidence:** HIGH (primary findings verified against live codebase; framework API claims verified against official docs and training knowledge where Context7 is unavailable for Android-specific APIs)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**D-01:** 8 curated Romanian retailers (eMAG, Altex, Flanco, Libris, Cărturești, IKEA RO, Dedeman, Elefant)
**D-02:** Store list in Firestore `config/stores` doc — `{ stores: [{ id, name, homepageUrl, displayOrder, logoAsset }] }` — editable without Play Store release
**D-03:** Store logos bundled in `app/src/main/res/drawable-nodpi/`, Firestore `logoAsset` resolved via `context.resources.getIdentifier(...)`, fallback to `store_generic.webp`
**D-04:** `displayOrder: int` ascending sort; seeded with 10, 20, 30...
**D-05:** JavaScript enabled in WebView
**D-06:** Cookies persist via `CookieManager` default
**D-07:** Current URL captured via `WebViewClient.onPageFinished` → ViewModel `StateFlow<String>`
**D-08:** Non-http/https schemes blocked via `shouldOverrideUrlLoading`, snackbar shown
**D-09:** "Add to list" tap opens existing Phase 3 add-item screen pre-filled with WebView URL
**D-10:** Registry picker shown when user has ≥2 registries; hidden when 1; remembers last selection via `LastRegistryPreferencesDataStore`
**D-11:** Two entry points — Home FAB menu + Registry Detail "Add item" third option
**D-12:** Bottom sheet overlays WebView; back dismisses sheet first, then exits browser
**D-13:** `AffiliateUrlTransformer` unchanged — no new merchant rules in this phase
**D-14:** No affiliate rewriting during browsing — transform only at add-time
**D-15:** WebView load failure shows error state; "Add to list" disabled
**D-16:** Network lost mid-browse — WebView native stale page; Add button stays enabled
**D-17:** Store list load failure shows error state with retry; NO hardcoded fallback list
**D-18:** New strings under `stores_*` prefix in `strings.xml` + `values-ro/strings.xml`
**D-19:** Unit tests for `StoreRepositoryImpl`, `StoreListViewModel`, `StoreBrowserViewModel`, `LastRegistryPreferencesDataStore`
**D-20:** No Compose/UI tests — follow Phase 2 pattern (ViewModel unit tests + manual emulator)
**D-21:** Firestore rules: `config/stores` world-readable, owner-writable via admin only
**D-22:** Seed script `functions/scripts/seedStores.ts` — idempotent `set`, reads `functions/data/stores.seed.json`

### Claude's Discretion

No explicit discretion areas beyond those already resolved by D-01..D-22.

### Deferred Ideas (OUT OF SCOPE)

- Admin UI for managing stores in-app
- Per-store featured products / curated picks
- Usage analytics / most-tapped stores
- Smart store suggestions based on registry occasion type
- Additional affiliate transformers for Altex/Flanco/Libris/Cărturești/IKEA/Dedeman/Elefant (MERCH-01, v2)
- Desktop/web variant of store browser
- Deep product search within a store via API
- Saving browsing history within a store
- Login-wall detection / warnings
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| STORE-01 | Owner can open a "Browse stores" entry point and see a curated list of popular Romanian retailers (logo + name) | Firestore one-shot read from `config/stores`; `LazyVerticalGrid`; `getIdentifier` for logo resolution |
| STORE-02 | Tapping a store opens the retailer's homepage in an in-app WebView | `AndroidView(::WebView)` inside Compose `Scaffold`; `WebViewClient` callbacks; `webSettings` configuration |
| STORE-03 | Persistent bottom "Add to list" button opens existing add-item screen pre-filled with current WebView URL; affiliate tag applied via Phase 3 `AffiliateUrlTransformer` | Navigation3 back stack push; `AddItemKey` extended with optional `initialUrl`; `hiltViewModelWithNavArgs` pattern already established |
| STORE-04 | If the store page fails to load, the WebView shows an error state and the "Add to list" button is disabled; retry available, nav stack intact | `WebViewClient.onReceivedError` → ViewModel state update; error overlay via `Box` + `Modifier.alpha(0f)` on WebView |
</phase_requirements>

---

## Summary

Phase 7 introduces a curated Romanian store browser. The feature is anchored on three new Android screens (Store List, Store Browser) plus modifications to two existing screens (Home FAB menu, Add Item). There are no new Cloud Function deployments — the seed script is a one-off admin utility, not a deployed function.

The technical challenge is not WebView itself (Android `WebView` is stable and well-understood) but the correct integration pattern: wrapping `WebView` inside `AndroidView` in Compose while keeping URL state in a Hilt ViewModel via `StateFlow`, and threading that URL into the existing `AddItemScreen`/`AddItemViewModel` flow. The existing `hiltViewModelWithNavArgs` pattern (already in `HiltViewModelNavArgs.kt`) provides the bridge.

The second non-trivial area is `getIdentifier` for drawable resolution with R8/ProGuard. Bundled drawables used by name string at runtime require a `keep` rule or direct `@DrawableRes` mapping to survive minification.

**Primary recommendation:** Follow the `callbackFlow + awaitClose` pattern already established in `FirestoreDataSource` for `StoreRepositoryImpl`. Use `AndroidView(::WebView)` with all WebView callbacks forwarding to `StoreBrowserViewModel`. Use `hiltViewModelWithNavArgs` for the extended `AddItemKey(registryId, initialUrl, initialRegistryId)`. Mirror `GuestPreferencesDataStore` verbatim for `LastRegistryPreferencesDataStore`.

---

## Standard Stack

### Core (all already in `libs.versions.toml` — no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android WebView | framework | In-app web browser | Framework-provided, no dependency needed; `INTERNET` permission already in `AndroidManifest.xml` |
| Jetpack Compose `AndroidView` | via Compose BOM 2026.03.00 | Bridge View-based `WebView` into Compose | Official Compose interop mechanism for View-based components not yet in Compose |
| Navigation3 | 1.0.1 (already in project) | New nav keys `StoreListKey`, `StoreBrowserKey` | Already established in `AppNavigation.kt` |
| Hilt | 2.59.2 (already in project) | DI for new `StoresModule` | Already established; `DataModule` pattern to follow |
| DataStore Preferences | 1.2.1 (already in project) | `LastRegistryPreferencesDataStore` | Already used for `GuestPreferencesDataStore`, `LanguagePreferencesDataStore`, `OnboardingPreferencesDataStore` |
| Firebase Firestore | via BoM 34.11.0 (already in project) | One-shot read of `config/stores` | Already used in `FirestoreDataSource` |
| Material3 | via Compose BOM 2026.03.00 (already in project) | `LazyVerticalGrid`, `SmallFloatingActionButton`, `AnimatedVisibility`, `ExposedDropdownMenuBox` | All M3 standard components |

**No new Gradle dependencies required for Phase 7.** `LazyVerticalGrid` is in `androidx.compose.foundation` (already declared via `compose-bom`). `AnimatedVisibility`, `AnimatedContent` are in `androidx.compose.animation` (transitively included). `ExposedDropdownMenuBox` is in `material3` (already declared).

**Verification:** All libraries above are confirmed present in `app/build.gradle.kts` and `libs.versions.toml`.

### New Seed Script Dependencies (functions/scripts/ only — not deployed)

| Package | Purpose |
|---------|---------|
| `firebase-admin` | Already in `functions/package.json` — used to write to Firestore from script |
| `ts-node` | Already used for Firebase Functions TypeScript — run seed script locally |

---

## Architecture Patterns

### Recommended Project Structure (new files only)

```
app/src/main/java/com/giftregistry/
├── domain/
│   ├── store/
│   │   └── StoreRepository.kt                   # interface: suspend fun getStores(): Result<List<Store>>
│   ├── model/
│   │   └── Store.kt                             # data class: id, name, homepageUrl, displayOrder, logoAsset
│   └── preferences/
│       └── LastRegistryPreferencesRepository.kt # interface: suspend get/set lastRegistryId
├── data/
│   ├── store/
│   │   ├── StoreRepositoryImpl.kt               # Firestore one-shot read
│   │   └── StoreDto.kt                          # Firestore mapping
│   └── preferences/
│       └── LastRegistryPreferencesDataStore.kt  # mirrors GuestPreferencesDataStore pattern
├── di/
│   └── StoresModule.kt                          # @Binds StoreRepository, LastRegistryPreferencesRepository
└── ui/
    └── store/
        ├── list/
        │   ├── StoreListScreen.kt
        │   ├── StoreListViewModel.kt
        │   └── StoreListUiState.kt              # sealed: Loading / Success(stores) / Error(msg)
        └── browser/
            ├── StoreBrowserScreen.kt
            └── StoreBrowserViewModel.kt

app/src/main/res/
├── drawable-nodpi/
│   ├── store_emag.webp
│   ├── store_altex.webp
│   ├── store_flanco.webp
│   ├── store_libris.webp
│   ├── store_carturesti.webp
│   ├── store_ikea.webp
│   ├── store_dedeman.webp
│   ├── store_elefant.webp
│   └── store_generic.webp                      # fallback
├── values/strings.xml                           # +stores_* keys
└── values-ro/strings.xml                        # +stores_* keys (Romanian)

functions/
├── data/
│   └── stores.seed.json
└── scripts/
    └── seedStores.ts
```

### Pattern 1: WebView in AndroidView (Compose Interop)

**What:** Wrap Android `WebView` in `AndroidView` composable. All WebView callbacks update ViewModel state via `StateFlow`. WebView reference kept as `remember { }` for retry calls.

**When to use:** Any time a framework `View` has no Compose equivalent — `WebView` is the canonical case.

**Critical detail:** The `WebView` instance must be created inside the `factory` lambda of `AndroidView` and NOT re-created on recomposition. Pass updates via the `update` lambda.

```kotlin
// Source: official Android Compose interop docs + established Compose pattern
@Composable
fun StoreWebView(
    homepageUrl: String,
    onUrlChanged: (String) -> Unit,
    onPageLoadFailed: () -> Unit,
    onExternalSchemeBlocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true       // D-05
                settings.domStorageEnabled = true        // required by Romanian e-commerce sites
                CookieManager.getInstance().setAcceptCookie(true)  // D-06
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        onUrlChanged(url)               // D-07
                    }
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        if (request.isForMainFrame) {
                            onPageLoadFailed()          // D-15
                        }
                    }
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val scheme = request.url.scheme ?: ""
                        if (scheme != "http" && scheme != "https") {
                            onExternalSchemeBlocked()   // D-08
                            return true
                        }
                        return false
                    }
                }
                loadUrl(homepageUrl)
            }.also { webViewRef.value = it }
        },
        modifier = modifier
    )
}
```

**PITFALL — `update` lambda:** `AndroidView`'s `update` lambda runs on every recomposition. Do NOT call `webView.loadUrl(...)` inside `update` unconditionally — it reloads the page on every recomposition. Guard with state comparison or use `LaunchedEffect(homepageUrl)` to trigger load once.

### Pattern 2: Firestore One-Shot Read (not a Flow)

**What:** `config/stores` is a static configuration document. Use `get().await()` (one-shot) rather than `addSnapshotListener` (Flow). The document changes very rarely; a live listener would be wasteful.

**When to use:** Configuration documents that change at most on deploys, not user actions.

```kotlin
// Source: established pattern in FirestoreDataSource + Firestore Kotlin docs
suspend fun getStores(): Result<List<StoreDto>> = runCatching {
    val snapshot = firestore.collection("config").document("stores").get().await()
    @Suppress("UNCHECKED_CAST")
    val rawList = snapshot.get("stores") as? List<Map<String, Any>> ?: emptyList()
    rawList.map { map ->
        StoreDto(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            homepageUrl = map["homepageUrl"] as? String ?: "",
            displayOrder = (map["displayOrder"] as? Long)?.toInt() ?: 0,
            logoAsset = map["logoAsset"] as? String ?: "store_generic",
        )
    }.sortedBy { it.displayOrder }
}
```

**Note on `toObject` with arrays:** Firestore's `toObject(T::class.java)` does not map top-level arrays-in-document fields reliably via POJO. Use manual `snapshot.get("stores")` cast. This is confirmed by the existing `FirestoreDataSource` pattern where `doc.toObject(ItemDto::class.java)` is used for documents but not for subdocuments.

### Pattern 3: getIdentifier for Bundled Drawables

**What:** Resolve a drawable name string (from Firestore) to an `@DrawableRes Int` at runtime.

**When to use:** When the drawable name is determined at runtime (from a config document), not compile-time.

```kotlin
// Source: Android Resources docs; confirmed safe pattern with R8 keep rule
fun resolveLogoResId(context: Context, logoAsset: String): Int {
    val resId = context.resources.getIdentifier(
        logoAsset,           // e.g. "store_emag"
        "drawable",
        context.packageName
    )
    return if (resId != 0) resId else R.drawable.store_generic
}
```

**CRITICAL — R8/ProGuard keep rule required.** When `minifyEnabled = true` (release builds), R8 removes unused resources by name. Drawables accessed only via `getIdentifier` (string name, not `R.drawable.*` reference) appear "unused" and ARE REMOVED. Add to `app/proguard-rules.pro`:

```
# Keep store logo drawables accessed via getIdentifier
-keep class **.R$drawable { *; }
```

Alternatively (and more precisely), annotate a companion object constant with `@Keep`:

```kotlin
// In a dedicated file that's always compiled
object StoreLogoKeepMarker {
    @Keep val store_emag = R.drawable.store_emag
    @Keep val store_altex = R.drawable.store_altex
    // ... all 8 + generic
}
```

**Research verdict:** The `proguard-rules.pro` approach is simpler for this use case. The `@Keep` companion approach is more surgical. Given the project has `buildConfig = true` and `minifyEnabled` is not yet confirmed on/off for release, add the rule proactively. The `drawable-nodpi` directory must be created — it does not currently exist in the project.

### Pattern 4: DataStore for LastRegistryPreference

**What:** Mirror `GuestPreferencesDataStore` exactly. New file, new DataStore name (`last_registry_prefs`), unique Context extension property.

**CRITICAL — duplicate DataStore name pitfall** (from STATE.md and existing code comments): The DataStore `name` parameter must be unique per app. The existing DataStores use `"guest_prefs"`, `"language_prefs"`, `"onboarding_prefs"`. Use `"last_registry_prefs"` (confirmed unique).

```kotlin
// Source: mirrors GuestPreferencesDataStore exactly
private val Context.lastRegistryDataStore: DataStore<Preferences> by preferencesDataStore(name = "last_registry_prefs")

@Singleton
class LastRegistryPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : LastRegistryPreferencesRepository {
    private val lastRegistryIdKey = stringPreferencesKey("last_registry_id")

    override fun observeLastRegistryId(): Flow<String?> =
        context.lastRegistryDataStore.data.map { it[lastRegistryIdKey] }

    override suspend fun getLastRegistryId(): String? =
        context.lastRegistryDataStore.data.first()[lastRegistryIdKey]

    override suspend fun setLastRegistryId(registryId: String) {
        context.lastRegistryDataStore.edit { it[lastRegistryIdKey] = registryId }
    }
}
```

### Pattern 5: Navigation3 — New Keys and Entry Points

**What:** Add `StoreListKey` and `StoreBrowserKey` to `AppNavKeys.kt`. Register entries in `AppNavigation.kt`. Extend `AddItemKey` with optional `initialUrl` and `initialRegistryId`.

**CRITICAL — `@Serializable` default values in Navigation3:** From STATE.md (Phase 4 decision): "Avoids Navigation3 `@Serializable` default-value complications." The `StoreBrowserKey` and the extended `AddItemKey` use nullable params, not default values, to avoid this issue.

```kotlin
// AppNavKeys.kt additions
@Serializable data object StoreListKey
@Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)

// Extended AddItemKey — REPLACES existing
@Serializable data class AddItemKey(
    val registryId: String,
    val initialUrl: String? = null,           // null when entered from Registry Detail normally
    val initialRegistryId: String? = null,    // null when entered from Registry Detail (pre-selected)
)
```

**IMPORTANT:** Adding `initialUrl` and `initialRegistryId` with defaults to `AddItemKey` is safe for Navigation3 serialization ONLY if they are nullable with `= null`. The STATE.md warning about default-value complications applies to non-null defaults. Nullable defaults are part of Kotlin serialization standard.

### Pattern 6: AddItemScreen — initialUrl Pre-fill

**What:** The `AddItemScreen` must accept an optional `initialUrl` and trigger `onFetchMetadata()` automatically when it's non-null. The `AddItemViewModel` receives the value via `SavedStateHandle` using the existing `hiltViewModelWithNavArgs` mechanism.

```kotlin
// AddItemScreen.kt signature change
@Composable
fun AddItemScreen(
    registryId: String,
    initialUrl: String? = null,
    initialRegistryId: String? = null,
    onBack: () -> Unit,
    viewModel: AddItemViewModel = hiltViewModelWithNavArgs(
        key = registryId,
        "registryId" to registryId,
        "initialUrl" to (initialUrl ?: ""),
        "initialRegistryId" to (initialRegistryId ?: ""),
    )
)

// AddItemViewModel.kt — reads from SavedStateHandle
val initialUrl: String = savedStateHandle["initialUrl"] ?: ""

init {
    if (initialUrl.isNotBlank()) {
        url.value = initialUrl
        onFetchMetadata()  // auto-trigger OG fetch
    }
}
```

### Pattern 7: FAB Menu (Home Screen Refactor)

**What:** Refactor `RegistryListScreen`'s single `ExtendedFloatingActionButton` into an expandable menu pattern using `AnimatedVisibility` + `SmallFloatingActionButton` items. No third-party library — M3 only.

**Key composable:** The FAB menu state (`menuExpanded`) lives in `RegistryListScreen` composable scope, not in `RegistryListViewModel` (it's purely UI state, no business logic).

```kotlin
// Source: UI-SPEC.md Component Contract 1; M3 standard components
var menuExpanded by remember { mutableStateOf(false) }

Box(contentAlignment = Alignment.BottomEnd) {
    // Scrim to dismiss menu on outside tap
    if (menuExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { menuExpanded = false }
        )
    }

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = menuExpanded,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FabMenuItem(
                    icon = Icons.Default.ShoppingBag,
                    label = stringResource(R.string.stores_browse_label),
                    onClick = { menuExpanded = false; onNavigateToBrowseStores() }
                )
                FabMenuItem(
                    icon = Icons.Default.Edit,
                    label = stringResource(R.string.stores_create_registry_label),
                    onClick = { menuExpanded = false; onNavigateToCreate() }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExtendedFloatingActionButton(
            onClick = { menuExpanded = !menuExpanded },
            icon = {
                AnimatedContent(targetState = menuExpanded) { expanded ->
                    Icon(
                        imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            },
            text = { Text(stringResource(R.string.stores_fab_label)) }
        )
    }
}
```

**Icon availability:** `Icons.Default.ShoppingBag` is in `material-icons-extended` (already declared in `app/build.gradle.kts`). `Icons.Default.Storefront` is also available in `material-icons-extended`. Either works per UI-SPEC.

### Anti-Patterns to Avoid

- **Re-creating `WebView` on recomposition:** The `factory` lambda in `AndroidView` is called once; the `update` lambda is called every recomposition. Never call `webView.loadUrl()` in `update` without guarding.
- **Calling `webView.loadUrl()` inside a `runCatching` or Flow:** WebView callbacks are on the main thread; mixing coroutine contexts causes subtle threading bugs. Keep all WebView method calls in composable scope (main thread).
- **Storing `WebView` reference in ViewModel:** WebView is a `View`, holds a `Context` reference, and MUST NOT be stored in a ViewModel (memory leak). Store only URL strings in the ViewModel; keep the `WebView` instance as a `remember` in the composable.
- **Using `ModalBottomSheet` for Add Item overlay:** The CONTEXT.md says the sheet "overlays the WebView" (D-12). The existing `AddItemScreen` is a full `Scaffold` screen, not a sheet. The implementation navigates to `AddItemKey` (pushes to back stack) rather than showing a modal bottom sheet. The UI-SPEC confirms this is a navigation push, not a sheet overlay — D-12's "overlays" language refers to the visual impression of pushing on top of the WebView, not a literal `ModalBottomSheet`.
- **`getIdentifier` without a keep rule in release builds:** Resource stripping will silently remove logos.
- **Using `addSnapshotListener` for `config/stores`:** One-shot `get().await()` is correct — this is config, not live user data.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| WebView state persistence through config changes | Custom save/restore of page state | `rememberSaveableStateHolderNavEntryDecorator` (already in NavDisplay) + `WebView.saveState()` if needed | Navigation3's `rememberSaveableStateHolderNavEntryDecorator` already preserves composable state across back stack; WebView state is separate but native `onSaveInstanceState` handles scroll position |
| Cookie management | Custom cookie jar | `CookieManager.getInstance()` | Android `CookieManager` handles persistence across sessions automatically when `setAcceptCookie(true)` |
| Drawable name → ResId mapping | Custom lookup table | `context.resources.getIdentifier()` + keep rule | Standard Android resource system; just guard R8 stripping |
| Registry picker dropdown | Custom composable | `ExposedDropdownMenuBox` (M3) | M3 component handles focus, announcement, keyboard navigation — accessibility for free |
| OG metadata fetch | New HTTP client | Existing `fetchOgMetadata` Cloud Function (Phase 3) | Already deployed; `FetchOgMetadataUseCase` already wired in `AddItemViewModel` |
| Affiliate URL transformation | New transform logic | Existing `AffiliateUrlTransformer` (Phase 3) | Already handles EMAG + pass-through; D-13 explicitly says no changes |

**Key insight:** Phase 7 is an integration phase. Almost every major component already exists. The work is plumbing (nav keys, ViewModel StateFlow wiring, Firestore read pattern) not building new primitives.

---

## Common Pitfalls

### Pitfall 1: WebView Back Navigation Confusion

**What goes wrong:** Developer implements `webView.canGoBack()` / `webView.goBack()` for device back button, causing WebView page history (not Android back stack) to be consumed on back press. User can never leave the store browser.

**Why it happens:** WebView has its own internal back/forward history separate from the Android navigation back stack. The two are easy to conflate.

**How to avoid:** Per D-12 and UI-SPEC — the decision is explicit: device back does NOT use `webView.canGoBack()`. Back always pops the Navigation3 back stack. Implement `onBack` in the `NavDisplay` callback to pop, never consult `webView.canGoBack()`.

**Warning signs:** User gets "stuck" in a store after navigating to a product page — pressing back only goes to previous WebView page, never exits.

### Pitfall 2: getIdentifier R8 Stripping (Release Builds)

**What goes wrong:** Logos display correctly in debug builds (minify disabled) but are all replaced by the generic fallback in release builds.

**Why it happens:** R8 resource shrinker identifies drawables that have no direct `R.drawable.*` reference in compiled code as unused and removes them. `getIdentifier` calls are string-based; R8 does not trace strings.

**How to avoid:** Add to `app/proguard-rules.pro`:
```
-keep class **.R$drawable { *; }
```
Or add explicit `R.drawable` references in a `@Keep`-annotated companion object.

**Warning signs:** All store logos show the generic fallback in signed release APK; debug APK works fine.

### Pitfall 3: Duplicate DataStore Name

**What goes wrong:** Runtime `IllegalStateException: There are multiple DataStores active for the same file` on app startup.

**Why it happens:** Two `preferencesDataStore(name = "X")` delegates with the same name are created in different classes — even if the Context extension property names differ.

**How to avoid:** The DataStore `name` string must be unique. Confirmed used names: `"guest_prefs"`, `"language_prefs"`, `"onboarding_prefs"`. Use `"last_registry_prefs"` for the new DataStore.

**Warning signs:** App crashes at startup with `IllegalStateException` referencing DataStore.

### Pitfall 4: WebView Memory Leak via ViewModel Reference

**What goes wrong:** `StoreBrowserViewModel` holds a reference to a `WebView` instance. Activity rotation or navigation pops and re-creates the composable but the old `WebView` is retained in the leaked ViewModel.

**Why it happens:** ViewModels outlive composables. A `WebView` holds a `Context` reference (the Activity). Storing `WebView` in a ViewModel creates an Activity context leak.

**How to avoid:** ViewModel holds ONLY `StateFlow<String>` for the current URL and a `Boolean` for error state. The `WebView` instance is stored in a `remember { mutableStateOf<WebView?>(null) }` inside the composable.

### Pitfall 5: onReceivedError Fires for Subresources

**What goes wrong:** `WebViewClient.onReceivedError` fires for failed subresource loads (images, analytics scripts, ads), not just main page failures. The error overlay shows even when the page loaded successfully but a tracking pixel failed.

**Why it happens:** `onReceivedError` is called for all resource errors, not just main frame errors.

**How to avoid:** Check `request.isForMainFrame` before triggering the error state:
```kotlin
override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
    if (request.isForMainFrame) {
        onPageLoadFailed()  // only show error overlay for main page failures
    }
}
```
This is already specified in the UI-SPEC but deserves explicit documentation as a pitfall.

### Pitfall 6: AddItemKey Serialization with Nullable Defaults

**What goes wrong:** Navigation3 fails to serialize/deserialize `AddItemKey` if default values on `@Serializable` data classes are non-null (known project issue from STATE.md Phase 4 decision).

**Why it happens:** `kotlinx.serialization` and Navigation3's back stack restoration may struggle with non-null default parameters in certain configurations.

**How to avoid:** Make `initialUrl` and `initialRegistryId` nullable (`String?`) with `= null` defaults. Nullable nulls serialize cleanly. This is the established project pattern (`ReReserveDeepLink` uses a non-optional param; the issue is specifically with non-null defaults on optional fields).

### Pitfall 7: AnimatedContent Icon Animation Requires Stable Key

**What goes wrong:** `AnimatedContent(targetState = menuExpanded)` does not animate smoothly — icon flickers or jumps.

**Why it happens:** `AnimatedContent` uses the `targetState` as the key. If the lambda re-creates the `Icon` composable every time (which it does, since it's a lambda), the animation runs correctly. This is a false alarm — Compose handles this correctly.

**Real pitfall:** NOT providing `contentAlignment` to `AnimatedContent` when the `Add` and `Close` icons have different visual sizes — alignment snapping can look like a glitch. Use `contentAlignment = Alignment.Center`.

---

## Code Examples

### Store List ViewModel

```kotlin
// Source: mirrors RegistryListViewModel pattern in project
@HiltViewModel
class StoreListViewModel @Inject constructor(
    private val getStores: GetStoresUseCase,
    private val observeRegistries: ObserveRegistriesUseCase,
    private val observeAuthState: ObserveAuthStateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreListUiState>(StoreListUiState.Loading)
    val uiState: StateFlow<StoreListUiState> = _uiState.asStateFlow()

    init { loadStores() }

    fun loadStores() {
        viewModelScope.launch {
            _uiState.value = StoreListUiState.Loading
            getStores()
                .onSuccess { stores -> _uiState.value = StoreListUiState.Success(stores) }
                .onFailure { _uiState.value = StoreListUiState.Error(it.message ?: "") }
        }
    }
}

sealed interface StoreListUiState {
    data object Loading : StoreListUiState
    data class Success(val stores: List<Store>) : StoreListUiState
    data class Error(val message: String) : StoreListUiState
}
```

### Store Browser ViewModel

```kotlin
// Source: clean architecture pattern established in Phases 2-6
@HiltViewModel
class StoreBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getStores: GetStoresUseCase,
) : ViewModel() {

    val storeId: String = savedStateHandle["storeId"] ?: ""
    val registryId: String? = savedStateHandle["registryId"]

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageLoadFailed = MutableStateFlow(false)
    val pageLoadFailed: StateFlow<Boolean> = _pageLoadFailed.asStateFlow()

    private val _storeName = MutableStateFlow("")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _homepageUrl = MutableStateFlow("")
    val homepageUrl: StateFlow<String> = _homepageUrl.asStateFlow()

    init {
        viewModelScope.launch {
            getStores().onSuccess { stores ->
                stores.find { it.id == storeId }?.let { store ->
                    _storeName.value = store.name
                    _homepageUrl.value = store.homepageUrl
                }
            }
        }
    }

    fun onUrlChanged(url: String) {
        _currentUrl.value = url
        _pageLoadFailed.value = false
    }

    fun onPageLoadFailed() {
        _pageLoadFailed.value = true
        _currentUrl.value = ""
    }

    fun onRetry() {
        _pageLoadFailed.value = false
        // Composable reads homepageUrl and reloads; webView.reload() called from composable
    }

    val addToListEnabled: Boolean get() = !_pageLoadFailed.value && _currentUrl.value.isNotBlank()
}
```

### Seed Script Shape (functions/data/stores.seed.json)

```json
{
  "stores": [
    { "id": "emag", "name": "eMAG", "homepageUrl": "https://www.emag.ro", "displayOrder": 10, "logoAsset": "store_emag" },
    { "id": "altex", "name": "Altex", "homepageUrl": "https://altex.ro", "displayOrder": 20, "logoAsset": "store_altex" },
    { "id": "flanco", "name": "Flanco", "homepageUrl": "https://www.flanco.ro", "displayOrder": 30, "logoAsset": "store_flanco" },
    { "id": "libris", "name": "Libris", "homepageUrl": "https://www.libris.ro", "displayOrder": 40, "logoAsset": "store_libris" },
    { "id": "carturesti", "name": "Cărturești", "homepageUrl": "https://carturesti.ro", "displayOrder": 50, "logoAsset": "store_carturesti" },
    { "id": "ikea", "name": "IKEA", "homepageUrl": "https://www.ikea.com/ro/ro/", "displayOrder": 60, "logoAsset": "store_ikea" },
    { "id": "dedeman", "name": "Dedeman", "homepageUrl": "https://www.dedeman.ro", "displayOrder": 70, "logoAsset": "store_dedeman" },
    { "id": "elefant", "name": "Elefant", "homepageUrl": "https://www.elefant.ro", "displayOrder": 80, "logoAsset": "store_elefant" }
  ]
}
```

### Firestore Rule Addition

```
// Source: firestore.rules — add after existing Phase 6 rules
// config collection — world-readable (STORE-01 requires read without auth guard),
// no client writes (admin-only via seed script using Admin SDK)
match /config/{configId} {
  allow read: if true;
  allow write: if false;
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `hiltViewModel()` with Nav2 auto-populated `SavedStateHandle` | `hiltViewModelWithNavArgs()` with explicit Bundle seeding | Phase 2 (project decision) | AddItemKey must pass `initialUrl`/`initialRegistryId` via `hiltViewModelWithNavArgs` args |
| `KAPT` for Hilt annotation processing | `KSP` (confirmed in `libs.versions.toml`) | Phase 2 (AGP 9 incompatibility) | No impact on Phase 7 — already migrated |
| Firebase KTX modules | Main Firebase modules (no KTX suffix) | BoM 34.0.0 (July 2025) | No impact on Phase 7 — already using main modules |

---

## Open Questions

1. **proguard-rules.pro — does it currently exist and is minifyEnabled?**
   - What we know: `app/build.gradle.kts` has `buildFeatures { compose = true; buildConfig = true }` but does not show `buildTypes` block in the read portion. The file exists (`app/proguard-rules.pro` is standard in AGP projects).
   - What's unclear: Whether `minifyEnabled = true` for release builds is configured, and whether a proguard-rules.pro already exists.
   - Recommendation: Wave 0 task should `cat app/proguard-rules.pro` and add the keep rule regardless — it is a no-op if minify is disabled.

2. **Icons.Default.Storefront availability**
   - What we know: `material-icons-extended` is declared in `app/build.gradle.kts`. `Icons.Default.ShoppingBag` is definitely in the extended set. `Icons.Default.Storefront` may also be available.
   - What's unclear: Whether `Storefront` specifically is in the extended icon set for the version pinned by Compose BOM 2026.03.00.
   - Recommendation: Use `Icons.Default.ShoppingBag` as the primary implementation (confirmed present in extended icons). Check for `Storefront` availability during implementation; use it if present.

3. **AddItemKey serialization backward compatibility**
   - What we know: Existing `AddItemKey(registryId: String)` is already serialized in the Navigation3 back stack.
   - What's unclear: Whether adding `initialUrl: String? = null` and `initialRegistryId: String? = null` to the existing `@Serializable data class` breaks existing back stack restoration (e.g., if process death occurs and the back stack is restored from a saved state that predates the new fields).
   - Recommendation: Since `initialUrl` and `initialRegistryId` are nullable with null defaults, `kotlinx.serialization` handles missing fields as null during deserialization. This is safe. Verify with a quick test during implementation.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 7 is Android code + Firestore config. No new external tools, services, or CLIs beyond the Firebase CLI already present from Phase 1. The seed script uses `firebase-admin` already in `functions/package.json`. No new runtime dependencies.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Kotlin Coroutines Test + MockK + Turbine |
| Config file | `app/build.gradle.kts` (test deps already declared) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.store.*" -x lint` |
| Full suite command | `./gradlew :app:testDebugUnitTest -x lint` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| STORE-01 | `StoreListViewModel` emits Loading → Success(8 stores) on successful Firestore read | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreListViewModelTest" -x lint` | ❌ Wave 0 |
| STORE-01 | `StoreListViewModel` emits Loading → Error on Firestore failure | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreListViewModelTest" -x lint` | ❌ Wave 0 |
| STORE-01 | `StoreRepositoryImpl` maps `config/stores` doc to sorted `List<Store>` | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreRepositoryImplTest" -x lint` | ❌ Wave 0 |
| STORE-02 | `StoreBrowserViewModel` starts with empty `currentUrl` and `pageLoadFailed = false` | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreBrowserViewModelTest" -x lint` | ❌ Wave 0 |
| STORE-02 | `StoreBrowserViewModel.onUrlChanged` updates `currentUrl` StateFlow | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreBrowserViewModelTest" -x lint` | ❌ Wave 0 |
| STORE-03 | `StoreBrowserViewModel.addToListEnabled` is true when URL non-blank and no load failure | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreBrowserViewModelTest" -x lint` | ❌ Wave 0 |
| STORE-03 | `LastRegistryPreferencesDataStore` get/set/observe round-trip | unit | `./gradlew :app:testDebugUnitTest --tests "*.LastRegistryPreferencesDataStoreTest" -x lint` | ❌ Wave 0 |
| STORE-04 | `StoreBrowserViewModel.onPageLoadFailed` sets `pageLoadFailed = true` and clears `currentUrl` | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreBrowserViewModelTest" -x lint` | ❌ Wave 0 |
| STORE-04 | `StoreBrowserViewModel.addToListEnabled` is false after `onPageLoadFailed` | unit | `./gradlew :app:testDebugUnitTest --tests "*.StoreBrowserViewModelTest" -x lint` | ❌ Wave 0 |
| D-21 | Firestore rules: `config/stores` readable by unauthenticated client | security-rules | `cd tests/rules && npm test` | ❌ Wave 0 |
| D-21 | Firestore rules: `config/stores` write denied by authenticated client | security-rules | `cd tests/rules && npm test` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.store.*" -x lint`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest -x lint && cd tests/rules && npm test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/.../ui/store/list/StoreListViewModelTest.kt` — covers STORE-01 loading/success/error
- [ ] `app/src/test/.../ui/store/browser/StoreBrowserViewModelTest.kt` — covers STORE-02/03/04 URL state transitions
- [ ] `app/src/test/.../data/store/StoreRepositoryImplTest.kt` — covers STORE-01 Firestore mapping + sort
- [ ] `app/src/test/.../data/preferences/LastRegistryPreferencesDataStoreTest.kt` — covers STORE-03 picker memory
- [ ] `tests/rules/firestore.rules.test.ts` additions — covers D-21 config read/write rules

---

## Project Constraints (from CLAUDE.md)

All of the following apply to Phase 7 code:

| Directive | Impact on Phase 7 |
|-----------|-------------------|
| Kotlin only (no Java) | All new files: `.kt`. Seed script: `.ts` (Cloud Functions toolchain) |
| Firebase BoM 34.11.0 — main modules, no KTX suffix | `firebase-firestore` (already declared), no `firebase-firestore-ktx` |
| No Firebase imports in domain layer | `Store.kt`, `StoreRepository.kt`, `LastRegistryPreferencesRepository.kt` — pure Kotlin interfaces |
| `callbackFlow + awaitClose` for Firestore listeners | Not applicable for one-shot read; use `get().await()` for `config/stores` |
| `StateFlow<UiState>` sealed types in ViewModels | `StoreListUiState`, `StoreBrowserViewModel` state — sealed interfaces |
| Feature-namespaced string keys | All new keys use `stores_` prefix |
| All UI labels in `strings.xml` + `values-ro/strings.xml` | String table provided in UI-SPEC — 15 new keys |
| `res/values-ro/strings.xml` for Romanian | Romanian variants documented in UI-SPEC |
| Navigation3 1.0.1 — `@Serializable` data class/object nav keys | `StoreListKey`, `StoreBrowserKey`, extended `AddItemKey` |
| Hilt + KSP — `@HiltViewModel`, `@Inject`, `@Module @InstallIn` | `StoreListViewModel`, `StoreBrowserViewModel`, `StoresModule` |
| Compose BOM 2026.03.00 — M3 components throughout | All new composables use `MaterialTheme.colorScheme.*`, no hardcoded colors |
| Seed color `#6750A4`, no dynamic color | Inherited — no changes needed |
| `AndroidView` for View-based interop | `WebView` wrapped via `AndroidView` in `StoreBrowserScreen` |
| No hardcoded strings in Kotlin/Compose | All copy via `stringResource(R.string.stores_*)` |
| Cloud Functions on `europe-west3` | Not applicable — no new functions in this phase |
| Firestore rules tested via `tests/rules` harness | New `config/stores` rules added to `firestore.rules.test.ts` |

---

## Sources

### Primary (HIGH confidence)

- Live codebase read — `AppNavigation.kt`, `AppNavKeys.kt`, `RegistryListScreen.kt`, `AddItemScreen.kt`, `AddItemViewModel.kt`, `FirestoreDataSource.kt`, `GuestPreferencesDataStore.kt`, `DataModule.kt`, `HiltViewModelNavArgs.kt`, `firestore.rules`, `app/build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`
- `STATE.md` — established project decisions, confirmed DataStore name uniqueness requirement, Navigation3 serialization warning
- `07-CONTEXT.md` — locked decisions D-01..D-22
- `07-UI-SPEC.md` — component contracts, copywriting table, string keys
- `REQUIREMENTS.md` — STORE-01..04 requirement text

### Secondary (MEDIUM confidence)

- Android developer documentation (training knowledge, verified against live code patterns): `AndroidView` factory/update semantics, `WebViewClient` API, `CookieManager`, `getIdentifier`, R8 resource shrinking behavior
- Material3 documentation (training knowledge): `ExposedDropdownMenuBox`, `SmallFloatingActionButton`, `AnimatedVisibility`, `LazyVerticalGrid`

### Tertiary (LOW confidence)

- `Icons.Default.Storefront` availability in `material-icons-extended` at Compose BOM 2026.03.00 — not verified; `Icons.Default.ShoppingBag` confirmed as safe fallback

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries confirmed present in `libs.versions.toml` and `app/build.gradle.kts`; no new dependencies required
- Architecture: HIGH — patterns derived directly from existing project code; no speculation
- Pitfalls: HIGH for codebase-specific pitfalls (DataStore name, ViewModel context leak, onReceivedError subresource); MEDIUM for R8 stripping (standard Android behavior, confirmed by documentation)
- WebView API: MEDIUM — training knowledge; `WebViewClient` API is stable since API 21 and well-established

**Research date:** 2026-04-19
**Valid until:** 2026-07-19 (stable — no fast-moving libraries; all pinned via BOM)
