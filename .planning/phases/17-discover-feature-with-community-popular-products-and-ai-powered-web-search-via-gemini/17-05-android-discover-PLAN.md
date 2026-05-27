---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 05
type: execute
wave: 3
depends_on:
  - "17-01"
  - "17-03"
files_modified:
  - app/src/main/java/com/giftregistry/domain/discover/DiscoverProduct.kt
  - app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt
  - app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt
  - app/src/main/java/com/giftregistry/di/DiscoverModule.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
  - app/src/main/res/drawable/discover_card_placeholder.xml
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
  - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
  - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
  - app/src/test/java/com/giftregistry/ui/discover/DiscoverViewModelTest.kt
  - app/src/test/java/com/giftregistry/data/discover/DiscoverRepositoryImplTest.kt
autonomous: true
requirements:
  - D-01
  - D-02
  - D-32
  - D-33
  - D-34
  - D-35
  - D-36
  - D-37
  - D-38
  - D-39
  - D-40
  - D-41
  - D-49
  - D-50
  - D-51

must_haves:
  truths:
    - "Bottom nav slot 2 shows the Search icon + DISCOVER/DESCOPERĂ label and pushes DiscoverKey on tap"
    - "DiscoverScreen renders a search OutlinedTextField + two LazyColumn sections (FROM THE WEB shown only after a search; FROM THE COMMUNITY always)"
    - "Tapping a DiscoverProductCard launches Intent.ACTION_VIEW with raw retailer URL (no affiliate transform); ActivityNotFoundException → Snackbar"
    - "DiscoverViewModel emits idle → loading → loaded transitions for both popular and search; loadPopular() called from init {}"
    - "DiscoverRepositoryImpl wraps FirebaseFunctions.getHttpsCallable for discoverPopular + discoverSearch, returns Result<List<DiscoverProduct>>"
    - "StyleGuidePreview has a DiscoverPreview section with idle / loading / loaded / empty / error sub-previews"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt"
      provides: "Compose entry — Scaffold + search bar + two sections"
      contains: "DiscoverScreen"
      min_lines: 80
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt"
      provides: "Product card composable (AsyncImage 16:9 + title + desc + price)"
      contains: "DiscoverProductCard"
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt"
      provides: "Hilt ViewModel with popular + search StateFlows + retry()"
      contains: "@HiltViewModel"
    - path: "app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt"
      provides: "interface DiscoverRepository { suspend fun getPopular(); suspend fun search(query) }"
      contains: "interface DiscoverRepository"
    - path: "app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt"
      provides: "FirebaseFunctions getHttpsCallable wrapper with runCatching"
      contains: "getHttpsCallable"
    - path: "app/src/main/java/com/giftregistry/di/DiscoverModule.kt"
      provides: "Hilt module binding DiscoverRepository to DiscoverRepositoryImpl"
      contains: "@Binds"
    - path: "app/src/main/res/drawable/discover_card_placeholder.xml"
      provides: "Vector drawable — gradient + product-box glyph placeholder"
      contains: "vector"
    - path: "app/src/main/res/values/strings.xml"
      provides: "10 new discover_* keys + nav_discover_tab"
      contains: "nav_discover_tab"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "10 new discover_* keys + nav_discover_tab (Romanian)"
      contains: "DESCOPERĂ"
  key_links:
    - from: "app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt"
      to: "AppNavigation onDiscover callback → backStack.add(DiscoverKey)"
      via: "Slot 2: NavSlotId.DISCOVER + Icons.Outlined.Search + R.string.nav_discover_tab + onDiscover lambda"
      pattern: "NavSlotId.DISCOVER\\|onDiscover\\|nav_discover_tab"
    - from: "app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt"
      to: "FirebaseFunctions europe-west3 region"
      via: "FirebaseFunctions.getInstance(\"europe-west3\").getHttpsCallable(\"discoverPopular\")"
      pattern: "getInstance.*europe-west3"
    - from: "app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt"
      to: "Intent.ACTION_VIEW launch + Snackbar fallback"
      via: "Card(onClick = { try startActivity(Intent.ACTION_VIEW Uri.parse(retailerUrl)) catch ActivityNotFoundException showSnackbar })"
      pattern: "Intent\\.ACTION_VIEW\\|ActivityNotFoundException"
---

<objective>
Ship the full Android Discover feature: domain interface, data implementation, Hilt module, ViewModel (with state machines for both popular and search), Compose UI (DiscoverScreen + DiscoverProductCard + shimmer skeletons + placeholder drawable + en/ro strings), nav-graph rewire (slot 2 of bottom nav, new DiscoverKey, entry block, NavVisibility), StyleGuidePreview integration, and unit tests for ViewModel + Repository.

Purpose: Per CONTEXT.md D-01, D-02, D-32 through D-41, D-49 through D-51, plus the entirety of 17-UI-SPEC.md. This is the visible payoff of Phase 17 — the user-facing surface that consumes the backend (plans 17-03, 17-04).

Output: 11 new Kotlin source files, 1 new vector drawable, edits to strings.xml + values-ro/strings.xml (10+ keys each), edits to 4 nav/chrome files, append to StyleGuidePreview.kt, 2 unit-test files.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-01-stores-decommission-PLAN.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-03-callables-PLAN.md
@CLAUDE.md
</context>

<interfaces>
<!-- Callables exposed by plan 17-03 — consumed verbatim. -->
<!-- Response shapes per CONTEXT.md D-20 (popular) and D-31 (search). -->

discoverPopular Callable response (from functions/src/discover/getPopular.ts):
```typescript
{
  products: [
    { id: string, title: string, description: "", image_url: string,
      price: number, currency: "RON", retailer_url: string }
  ]
}
```

discoverSearch Callable request + response (from functions/src/discover/search.ts):
```typescript
// Request
{ query: string }   // 1–200 chars, non-empty after trim
// Response (D-31)
{ products: DiscoverProduct[], cached_at: string /* ISO 8601 */ }
// DiscoverProduct shape:
{ title: string, description: string, image_url: string, price: number,
  currency: string, retailer_url: string, retailer_name: string }
```

Existing Hilt / Compose patterns (extracted from codebase):
```kotlin
// di/AppModule.kt + di/StoresModule.kt (deleted by 17-01) — current style:
@Module
@InstallIn(SingletonComponent::class)
abstract class XModule {
  @Binds
  abstract fun bindXRepository(impl: XRepositoryImpl): XRepository
}

// ui pattern for Hilt ViewModel:
@HiltViewModel
class XViewModel @Inject constructor(private val repo: XRepository) : ViewModel()

// ui/common/chrome/GiftMaisonBottomNav.kt current slot-2 (will rename):
private enum class NavSlotId { HOME, STORES, FAB, LISTS, YOU }
fun GiftMaisonBottomNav(currentKey: Any?, onHome: () -> Unit, onStores: () -> Unit, onFab: () -> Unit, onLists: () -> Unit, onYou: () -> Unit, ...)
```

Existing AsyncImage + placeholder pattern (precedent: app/src/main/java/com/giftregistry/ui/registry/detail/HeroImageOrPlaceholder.kt from Phase 12):
```kotlin
AsyncImage(
  model = ImageRequest.Builder(...).data(imageUrl).build(),
  placeholder = painterResource(R.drawable.X_placeholder),
  error = painterResource(R.drawable.X_placeholder),
  contentScale = ContentScale.Crop,
)
```
</interfaces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Domain + Data + DI layer (DiscoverProduct, DiscoverRepository, Impl, DiscoverModule) + Repository unit tests</name>

  <behavior>
    - DiscoverProduct domain model: data class with id, title, description, imageUrl, price (Double), currency, retailerUrl
    - DiscoverRepository interface: suspend fun getPopular(): Result<List<DiscoverProduct>>; suspend fun search(query: String): Result<List<DiscoverProduct>>
    - DiscoverRepositoryImpl.getPopular(): calls FirebaseFunctions.getInstance("europe-west3").getHttpsCallable("discoverPopular").call().await(), maps HashMap → List<DiscoverProduct> via the "products" key, wraps in runCatching → Result
    - DiscoverRepositoryImpl.search(query): same pattern, passes `{ "query" to query }` payload, calls "discoverSearch"
    - Empty `products` array → Result.success(emptyList()) (NOT Result.failure)
    - Callable failure (e.g., FirebaseFunctionsException) → Result.failure with the exception
    - Hilt DiscoverModule binds DiscoverRepository → DiscoverRepositoryImpl with @Binds + @Singleton
  </behavior>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-39, D-40, D-49)
    - app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt (existing Firebase Functions + Hilt + runCatching pattern — mirror this style)
    - app/src/main/java/com/giftregistry/di/AppModule.kt (existing Hilt @Binds pattern)
    - app/src/test/java/com/giftregistry/data/reservation/ReservationRepositoryImplTest.kt (existing repository test pattern with mocked FirebaseFunctions)
    - CLAUDE.md (KSP not KAPT lock; Firebase BoM 34.x main modules — `firebase-functions` not `firebase-functions-ktx`)
  </read_first>

  <files>
    app/src/main/java/com/giftregistry/domain/discover/DiscoverProduct.kt,
    app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt,
    app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt,
    app/src/main/java/com/giftregistry/di/DiscoverModule.kt,
    app/src/test/java/com/giftregistry/data/discover/DiscoverRepositoryImplTest.kt
  </files>

  <action>
    **1. `app/src/main/java/com/giftregistry/domain/discover/DiscoverProduct.kt`**:
    ```kotlin
    package com.giftregistry.domain.discover

    /**
     * Phase 17 D-39 domain model — flat data class matching the Callable
     * response shape (popular: id from doc, search: server-generated UUID
     * inserted at mapping time to satisfy LazyColumn keys).
     *
     * `price` is a Double because both Callables return numeric price (D-20
     * and D-31). Display formatting via NumberFormat is applied at the UI
     * layer (D-37).
     */
    data class DiscoverProduct(
        val id: String,
        val title: String,
        val description: String,
        val imageUrl: String,
        val price: Double,
        val currency: String,
        val retailerUrl: String,
    )
    ```

    **2. `app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt`** — D-39 verbatim:
    ```kotlin
    package com.giftregistry.domain.discover

    interface DiscoverRepository {
        suspend fun getPopular(): Result<List<DiscoverProduct>>
        suspend fun search(query: String): Result<List<DiscoverProduct>>
    }
    ```

    **3. `app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt`** — D-40 verbatim:
    ```kotlin
    package com.giftregistry.data.discover

    import com.giftregistry.domain.discover.DiscoverProduct
    import com.giftregistry.domain.discover.DiscoverRepository
    import com.google.firebase.functions.FirebaseFunctions
    import kotlinx.coroutines.tasks.await
    import java.util.UUID
    import javax.inject.Inject
    import javax.inject.Singleton

    @Singleton
    class DiscoverRepositoryImpl @Inject constructor(
        private val functions: FirebaseFunctions,
    ) : DiscoverRepository {

        override suspend fun getPopular(): Result<List<DiscoverProduct>> = runCatching {
            val result = functions
                .getHttpsCallable("discoverPopular")
                .call()
                .await()
            mapResponseToProducts(result.getData(), generateMissingIds = false)
        }

        override suspend fun search(query: String): Result<List<DiscoverProduct>> = runCatching {
            val result = functions
                .getHttpsCallable("discoverSearch")
                .call(mapOf("query" to query))
                .await()
            // Search responses don't include doc IDs — generate stable UUIDs at the
            // mapping site so LazyColumn keys are unique and recomposition is stable.
            mapResponseToProducts(result.getData(), generateMissingIds = true)
        }

        private fun mapResponseToProducts(data: Any?, generateMissingIds: Boolean): List<DiscoverProduct> {
            @Suppress("UNCHECKED_CAST")
            val map = data as? Map<String, Any?> ?: return emptyList()
            val products = map["products"] as? List<Map<String, Any?>> ?: return emptyList()
            return products.map { item ->
                DiscoverProduct(
                    id = (item["id"] as? String) ?: if (generateMissingIds) UUID.randomUUID().toString() else "",
                    title = (item["title"] as? String).orEmpty(),
                    description = (item["description"] as? String).orEmpty(),
                    imageUrl = (item["image_url"] as? String).orEmpty(),
                    price = when (val p = item["price"]) {
                        is Number -> p.toDouble()
                        is String -> p.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    },
                    currency = (item["currency"] as? String) ?: "RON",
                    retailerUrl = (item["retailer_url"] as? String).orEmpty(),
                )
            }
        }
    }
    ```

    **4. `app/src/main/java/com/giftregistry/di/DiscoverModule.kt`**:
    ```kotlin
    package com.giftregistry.di

    import com.giftregistry.data.discover.DiscoverRepositoryImpl
    import com.giftregistry.domain.discover.DiscoverRepository
    import com.google.firebase.functions.FirebaseFunctions
    import dagger.Binds
    import dagger.Module
    import dagger.Provides
    import dagger.hilt.InstallIn
    import dagger.hilt.components.SingletonComponent
    import javax.inject.Singleton

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class DiscoverModule {
        @Binds
        @Singleton
        abstract fun bindDiscoverRepository(impl: DiscoverRepositoryImpl): DiscoverRepository

        companion object {
            @Provides
            @Singleton
            fun provideFirebaseFunctions(): FirebaseFunctions =
                FirebaseFunctions.getInstance("europe-west3")
        }
    }
    ```
    Note: If `FirebaseFunctions` is already provided elsewhere in the codebase (check for an existing `@Provides fun provideFirebaseFunctions`), DO NOT redefine it — remove the `companion object` block and let the existing provider satisfy `DiscoverRepositoryImpl`'s constructor. Run `grep -rn "provideFirebaseFunctions\|FirebaseFunctions.getInstance" app/src/main/java/com/giftregistry/di/ app/src/main/java/com/giftregistry/data/` to confirm.

    **5. `app/src/test/java/com/giftregistry/data/discover/DiscoverRepositoryImplTest.kt`** — mock FirebaseFunctions, assert mapping behavior:

    Cases (D-49 verbatim):
    - Valid response with `products: [{id, title, description, image_url, price, currency, retailer_url}]` → Result.success with mapped DiscoverProduct list
    - Empty `products: []` → Result.success(emptyList()) — NOT failure
    - Callable failure (e.g., HttpsError simulated as FirebaseFunctionsException) → Result.failure(<that exception>)
    - Price as numeric (Long/Double from JSON map) → mapped to Double
    - Search Callable: `call(mapOf("query" to "espresso"))` invoked with correct payload
    - Missing `id` field on search response → UUID generated; on popular response → empty string

    Use the existing reservation repository test as a template; if mocking FirebaseFunctions is too heavy, use a fake `FirebaseFunctions` test double constructed via mockk/Mockito.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      grep -q "data class DiscoverProduct" app/src/main/java/com/giftregistry/domain/discover/DiscoverProduct.kt
      grep -q "interface DiscoverRepository" app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt
      grep -q "suspend fun getPopular" app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt
      grep -q "suspend fun search" app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt
      grep -q "getHttpsCallable(\"discoverPopular\")" app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt
      grep -q "getHttpsCallable(\"discoverSearch\")" app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt
      grep -q "FirebaseFunctions.getInstance(\"europe-west3\")\\|FirebaseFunctions\\b" app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt
      grep -q "runCatching" app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt
      grep -q "@Binds" app/src/main/java/com/giftregistry/di/DiscoverModule.kt
      grep -q "DiscoverRepository" app/src/main/java/com/giftregistry/di/DiscoverModule.kt
      ./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin -q
      ./gradlew app:testDebugUnitTest --tests com.giftregistry.data.discover.DiscoverRepositoryImplTest -q
      echo OK
      '
    </automated>
  </verify>

  <done>
    DiscoverProduct, DiscoverRepository, DiscoverRepositoryImpl, DiscoverModule exist with documented contracts. DiscoverRepositoryImplTest covers ≥4 cases from D-49 and all pass. App compiles. FirebaseFunctions provided via Hilt at `europe-west3` (either by this module or via existing provider — verified).
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: DiscoverViewModel + DiscoverUiState + ViewModel unit tests + Discover string resources (en + ro)</name>

  <behavior>
    - DiscoverUiState defines two sealed states: PopularState (Loading | Loaded(List<DiscoverProduct>) | Empty | Error(String)) and SearchState (Idle | Loading | Loaded(List<DiscoverProduct>) | Empty | Error(String))
    - DiscoverViewModel exposes popular: StateFlow<PopularState>, search: StateFlow<SearchState>, searchQuery: StateFlow<String>
    - init { loadPopular() } — fires on creation
    - loadPopular() emits Loading, then either Loaded (non-empty), Empty (empty list success), or Error (failure with message resource id or text)
    - search(query) — trims input; empty trimmed → reset to Idle; non-empty → Loading → Loaded/Empty/Error
    - onQueryChange(query) updates searchQuery; does NOT auto-fire search (Search IME action does)
    - retryPopular() / retrySearch(currentQuery) re-invoke loadPopular/search
  </behavior>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-38, D-41, D-49)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md ("Interaction States Summary" section — full state machine)
    - app/src/test/java/com/giftregistry/MainDispatcherRule.kt (existing test rule for coroutines)
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt (existing Hilt ViewModel + StateFlow pattern to mirror)
    - app/src/test/java/com/giftregistry/ui/registry/list/RegistryListViewModelTest.kt (existing ViewModel test pattern — Turbine + Truth + MainDispatcherRule)
  </read_first>

  <files>
    app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt,
    app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt,
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml,
    app/src/test/java/com/giftregistry/ui/discover/DiscoverViewModelTest.kt
  </files>

  <action>
    **1. `app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt`**:
    ```kotlin
    package com.giftregistry.ui.discover

    import com.giftregistry.domain.discover.DiscoverProduct

    sealed interface PopularState {
        data object Loading : PopularState
        data class Loaded(val products: List<DiscoverProduct>) : PopularState
        data object Empty : PopularState
        data class Error(val message: String) : PopularState
    }

    sealed interface SearchState {
        data object Idle : SearchState
        data object Loading : SearchState
        data class Loaded(val products: List<DiscoverProduct>) : SearchState
        data object Empty : SearchState
        data class Error(val message: String) : SearchState
    }
    ```

    **2. `app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt`**:
    ```kotlin
    package com.giftregistry.ui.discover

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.giftregistry.domain.discover.DiscoverRepository
    import dagger.hilt.android.lifecycle.HiltViewModel
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.launch
    import javax.inject.Inject

    @HiltViewModel
    class DiscoverViewModel @Inject constructor(
        private val repository: DiscoverRepository,
    ) : ViewModel() {

        private val _popular = MutableStateFlow<PopularState>(PopularState.Loading)
        val popular: StateFlow<PopularState> = _popular.asStateFlow()

        private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
        val search: StateFlow<SearchState> = _search.asStateFlow()

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        init {
            loadPopular()
        }

        fun loadPopular() {
            _popular.value = PopularState.Loading
            viewModelScope.launch {
                repository.getPopular().fold(
                    onSuccess = { products ->
                        _popular.value = if (products.isEmpty()) PopularState.Empty
                                         else PopularState.Loaded(products)
                    },
                    onFailure = { err -> _popular.value = PopularState.Error(err.message ?: "Unknown error") },
                )
            }
        }

        fun onQueryChange(query: String) {
            _searchQuery.value = query
        }

        fun search(query: String) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                _search.value = SearchState.Idle
                return
            }
            _search.value = SearchState.Loading
            viewModelScope.launch {
                repository.search(trimmed).fold(
                    onSuccess = { products ->
                        _search.value = if (products.isEmpty()) SearchState.Empty
                                       else SearchState.Loaded(products)
                    },
                    onFailure = { err -> _search.value = SearchState.Error(err.message ?: "Unknown error") },
                )
            }
        }

        fun retrySearch() {
            search(_searchQuery.value)
        }
    }
    ```

    **3. `app/src/main/res/values/strings.xml`** — append 10 new keys (D-41 verbatim copy). Insert these in alphabetical order or grouped under a `<!-- Phase 17 Discover -->` comment block:
    ```xml
        <!-- Phase 17: Discover screen and bottom-nav slot 2 -->
        <string name="nav_discover_tab">DISCOVER</string>
        <string name="discover_search_placeholder">Search for any product...</string>
        <string name="discover_section_web">FROM THE WEB</string>
        <string name="discover_section_community">FROM THE COMMUNITY</string>
        <string name="discover_empty_search">No matches found. Try a different search.</string>
        <string name="discover_empty_popular">Popular items will appear here once people add gifts.</string>
        <string name="discover_error_load">Could not load. Try again.</string>
        <string name="discover_error_search">Search failed. Try again.</string>
        <string name="discover_retry">Retry</string>
        <string name="discover_no_browser_toast">Could not open browser</string>
    ```

    **4. `app/src/main/res/values-ro/strings.xml`** — append the Romanian counterparts (D-41 verbatim). Diacritics must be present as Unicode chars or `\u` escapes per file convention; mirror whichever convention is used by adjacent keys in values-ro/strings.xml (the existing file uses `\u` escapes):
    ```xml
        <!-- Phase 17: Discover screen and bottom-nav slot 2 -->
        <string name="nav_discover_tab">DESCOPERĂ</string>
        <string name="discover_search_placeholder">Caută orice produs...</string>
        <string name="discover_section_web">DE PE WEB</string>
        <string name="discover_section_community">DIN COMUNITATE</string>
        <string name="discover_empty_search">Niciun rezultat. Încearcă o altă căutare.</string>
        <string name="discover_empty_popular">Articolele populare vor apărea aici pe măsură ce oamenii adaugă cadouri.</string>
        <string name="discover_error_load">Nu s-a putut încărca. Încearcă din nou.</string>
        <string name="discover_error_search">Căutarea a eşuat. Încearcă din nou.</string>
        <string name="discover_retry">Reîncearcă</string>
        <string name="discover_no_browser_toast">Nu s-a putut deschide browserul</string>
    ```
    LocalizationParityTest (existing) will assert key parity between en and ro — both files must have these exact 10 keys.

    **5. `app/src/test/java/com/giftregistry/ui/discover/DiscoverViewModelTest.kt`** — D-49 verbatim cases:
    - `init` triggers loadPopular → Loading then Loaded on success
    - `loadPopular` empty-list → Empty
    - `loadPopular` failure → Error
    - `search("")` → SearchState.Idle (no Callable invocation)
    - `search("espresso")` → Loading → Loaded
    - `search` failure → Error
    - `search` empty result → Empty
    - `retrySearch()` re-invokes the last query

    Use Turbine for StateFlow assertions, Truth/JUnit, MainDispatcherRule for `viewModelScope.launch`. Mock DiscoverRepository as a fake test double exposing controllable `Result` returns.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      grep -q "sealed interface PopularState" app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt
      grep -q "sealed interface SearchState" app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt
      grep -q "@HiltViewModel" app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
      grep -q "loadPopular()" app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
      grep -q "init {" app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
      # Strings — all 10 keys present in en + ro
      for k in nav_discover_tab discover_search_placeholder discover_section_web discover_section_community discover_empty_search discover_empty_popular discover_error_load discover_error_search discover_retry discover_no_browser_toast; do
        grep -q "name=\"$k\"" app/src/main/res/values/strings.xml
        grep -q "name=\"$k\"" app/src/main/res/values-ro/strings.xml
      done
      grep -q "DESCOPER" app/src/main/res/values-ro/strings.xml
      grep -q "DISCOVER" app/src/main/res/values/strings.xml
      ./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin -q
      ./gradlew app:testDebugUnitTest --tests com.giftregistry.ui.discover.DiscoverViewModelTest --tests com.giftregistry.LocalizationParityTest -q
      echo OK
      '
    </automated>
  </verify>

  <done>
    DiscoverUiState defines PopularState + SearchState sealed interfaces. DiscoverViewModel exposes popular/search/searchQuery StateFlows and loadPopular/search/onQueryChange/retrySearch methods. All 8 ViewModel test cases pass. 10 Discover string keys present in BOTH en + ro strings.xml. LocalizationParityTest passes (en/ro keys match). App compiles.
  </done>
</task>

<task type="auto">
  <name>Task 3: discover_card_placeholder.xml drawable + DiscoverProductCard + DiscoverShimmer + DiscoverScreen Compose UI</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md (full file — every section, especially DiscoverProductCard, OutlinedTextField, Shimmer brush spec, Screen Layout Contract)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-32, D-33, D-34, D-35, D-36, D-37)
    - app/src/main/java/com/giftregistry/ui/registry/detail/HeroImageOrPlaceholder.kt (precedent: Coil AsyncImage + placeholder pattern from Phase 12)
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTheme.kt (colors, typography, spacing, shapes tokens used throughout)
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonSpacing.kt (verify gap20, gap16, gap12, gap10, gap8, edge tokens exist; UI-SPEC references them by name)
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonShapes.kt (verify radius8, radius12, radius16 exist)
    - app/src/main/res/drawable/ (list existing drawables to find a similar gradient pattern, e.g. registry placeholder)
  </read_first>

  <files>
    app/src/main/res/drawable/discover_card_placeholder.xml,
    app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt,
    app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt,
    app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
  </files>

  <action>
    **1. `app/src/main/res/drawable/discover_card_placeholder.xml`** — per UI-SPEC "discover_card_placeholder Drawable" section. Vector drawable, 16:9 viewport, accentSoft→accent gradient at 40% alpha, centered gift-box glyph in paper:
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="160dp"
        android:height="90dp"
        android:viewportWidth="160"
        android:viewportHeight="90">

        <!-- Gradient background: accentSoft (#F3DED0) top → accent (#C8623A) bottom @ 40% alpha -->
        <path
            android:pathData="M0,0 L160,0 L160,90 L0,90 Z"
            android:fillAlpha="0.4">
            <aapt:attr xmlns:aapt="http://schemas.android.com/aapt" name="android:fillColor">
                <gradient
                    android:type="linear"
                    android:startX="80"
                    android:startY="0"
                    android:endX="80"
                    android:endY="90"
                    android:startColor="#F3DED0"
                    android:endColor="#C8623A" />
            </aapt:attr>
        </path>

        <!-- Centered gift-box glyph: simplified box + lid + bow loop in paper #F7F2E9 @ 70% alpha -->
        <!-- Box body -->
        <path
            android:fillColor="#F7F2E9"
            android:fillAlpha="0.7"
            android:pathData="M64,50 L96,50 L96,68 L64,68 Z" />
        <!-- Lid -->
        <path
            android:fillColor="#F7F2E9"
            android:fillAlpha="0.7"
            android:pathData="M62,46 L98,46 L98,52 L62,52 Z" />
        <!-- Bow loop -->
        <path
            android:fillColor="#F7F2E9"
            android:fillAlpha="0.7"
            android:pathData="M78,38 Q72,32 76,28 Q80,32 80,44 Q80,32 84,28 Q88,32 82,38 Z" />
    </vector>
    ```
    If `aapt:attr` for gradient inside vector is unsupported by your minSdk, fall back to a flat fill at 30% alpha of accent. Verify by attempting to render in Android Studio preview; adjust if drawable inflation fails.

    **2. `app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt`** — per UI-SPEC "Loading State — Shimmer Skeletons" verbatim brush spec:
    ```kotlin
    package com.giftregistry.ui.discover

    import androidx.compose.animation.core.FastOutSlowInEasing
    import androidx.compose.animation.core.RepeatMode
    import androidx.compose.animation.core.animateFloat
    import androidx.compose.animation.core.infiniteRepeatable
    import androidx.compose.animation.core.rememberInfiniteTransition
    import androidx.compose.animation.core.tween
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.aspectRatio
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.unit.dp
    import com.giftregistry.ui.theme.GiftMaisonTheme

    @Composable
    fun DiscoverShimmerCard(modifier: Modifier = Modifier) {
        val colors = GiftMaisonTheme.colors
        val transition = rememberInfiniteTransition(label = "discover-shimmer")
        val translate by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmer-translate",
        )
        val brush = Brush.horizontalGradient(
            colors = listOf(colors.paperDeep, colors.line, colors.paperDeep),
            startX = translate - 300f,
            endX = translate,
        )

        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.paperDeep),
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(brush),
                )
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Box(Modifier.fillMaxWidth(0.75f).height(16.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.9f).height(13.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(0.6f).height(13.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.35f).height(14.dp).background(brush, RoundedCornerShape(8.dp)))
                }
            }
        }
    }
    ```

    **3. `app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt`** — UI-SPEC verbatim spec:
    ```kotlin
    package com.giftregistry.ui.discover

    import android.content.ActivityNotFoundException
    import android.content.Intent
    import android.net.Uri
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.aspectRatio
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.SnackbarHostState
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.res.stringResource
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import coil3.compose.AsyncImage
    import coil3.request.ImageRequest
    import com.giftregistry.R
    import com.giftregistry.domain.discover.DiscoverProduct
    import com.giftregistry.ui.theme.GiftMaisonTheme
    import kotlinx.coroutines.launch
    import java.text.NumberFormat
    import java.util.Locale

    @Composable
    fun DiscoverProductCard(
        product: DiscoverProduct,
        snackbarHostState: SnackbarHostState,
        modifier: Modifier = Modifier,
    ) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val noBrowserMessage = stringResource(R.string.discover_no_browser_toast)

        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.paperDeep),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = {
                // D-32: raw retailer URL, NO affiliate transform
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.retailerUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // D-33: Snackbar fallback
                    scope.launch { snackbarHostState.showSnackbar(noBrowserMessage) }
                }
            },
        ) {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(product.imageUrl).build(),
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.discover_card_placeholder),
                    error = painterResource(R.drawable.discover_card_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = product.title,
                        style = typography.bodyL,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            style = typography.bodyM,
                            color = colors.inkFaint,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (product.price > 0.0) {
                        val formatted = NumberFormat.getCurrencyInstance(Locale("ro", "RO"))
                            .format(product.price)
                        Text(
                            text = formatted,
                            style = typography.bodyMEmphasis,
                            color = colors.ink,
                        )
                    }
                }
            }
        }
    }
    ```

    Note: The Coil import (`coil3.compose.AsyncImage`) must match the version pinned in `gradle/libs.versions.toml`. If the project uses `coil.compose.AsyncImage` (Coil 2 namespace), use that import instead. Cross-check `HeroImageOrPlaceholder.kt` for the exact import used elsewhere.

    **4. `app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt`** — the screen composable per UI-SPEC "Screen Layout Contract":
    ```kotlin
    package com.giftregistry.ui.discover

    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.PaddingValues
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardActions
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.outlined.Search
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults
    import androidx.compose.material3.HorizontalDivider
    import androidx.compose.material3.Icon
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.OutlinedTextFieldDefaults
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.SnackbarHost
    import androidx.compose.material3.SnackbarHostState
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.collectAsState
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.remember
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalFocusManager
    import androidx.compose.ui.res.stringResource
    import androidx.compose.ui.text.input.ImeAction
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import com.giftregistry.R
    import com.giftregistry.ui.theme.GiftMaisonTheme

    @Composable
    fun DiscoverScreen(
        viewModel: DiscoverViewModel = hiltViewModel(),
    ) {
        val colors = GiftMaisonTheme.colors
        val popular by viewModel.popular.collectAsStateWithLifecycle()
        val search by viewModel.search.collectAsStateWithLifecycle()
        val query by viewModel.searchQuery.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val focusManager = LocalFocusManager.current

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = colors.paper,
        ) { inner ->
            Column(modifier = Modifier.padding(inner).fillMaxWidth()) {
                Box(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text(stringResource(R.string.discover_search_placeholder), color = colors.inkFaint) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.inkFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.line,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.search(query)
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                Box(modifier = Modifier.height(16.dp))

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, bottom = 24.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // FROM THE WEB section (only when search is not idle)
                    if (search !is com.giftregistry.ui.discover.SearchState.Idle) {
                        item { SectionHeader(stringResource(R.string.discover_section_web)) }
                        when (val state = search) {
                            SearchState.Loading -> items(3) { DiscoverShimmerCard() }
                            is SearchState.Loaded -> items(state.products, key = { it.id }) {
                                DiscoverProductCard(product = it, snackbarHostState = snackbarHostState)
                            }
                            SearchState.Empty -> item {
                                EmptyStateText(stringResource(R.string.discover_empty_search))
                            }
                            is SearchState.Error -> item {
                                InlineErrorState(
                                    message = stringResource(R.string.discover_error_search),
                                    onRetry = { viewModel.retrySearch() },
                                )
                            }
                            SearchState.Idle -> Unit
                        }
                        item { HorizontalDivider(color = colors.line, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp)) }
                    }

                    // FROM THE COMMUNITY section (always)
                    item { SectionHeader(stringResource(R.string.discover_section_community)) }
                    when (val state = popular) {
                        PopularState.Loading -> items(3) { DiscoverShimmerCard() }
                        is PopularState.Loaded -> items(state.products, key = { it.id }) {
                            DiscoverProductCard(product = it, snackbarHostState = snackbarHostState)
                        }
                        PopularState.Empty -> item {
                            EmptyStateText(stringResource(R.string.discover_empty_popular))
                        }
                        is PopularState.Error -> item {
                            InlineErrorState(
                                message = stringResource(R.string.discover_error_load),
                                onRetry = { viewModel.loadPopular() },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(text: String) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        Text(
            text = text,
            style = typography.monoCaps,
            color = colors.inkFaint,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }

    @Composable
    private fun EmptyStateText(text: String) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        ) {
            Text(text, style = typography.bodyM, color = colors.inkFaint, textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun InlineErrorState(message: String, onRetry: () -> Unit) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(message, style = typography.bodyM, color = colors.inkFaint, textAlign = TextAlign.Center)
            Box(Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.accentInk),
                shape = GiftMaisonTheme.shapes.pill,
            ) {
                Text(stringResource(R.string.discover_retry), style = typography.bodyMEmphasis)
            }
        }
    }
    ```

    Note: `accentInk` token must exist on GiftMaisonColors — confirm by reading GiftMaisonColors.kt before commit. If a different "ink on accent" token name is used, swap accordingly (e.g., `colors.paper` if accent buttons use paper text per Phase 11 precedent).
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      test -f app/src/main/res/drawable/discover_card_placeholder.xml
      grep -q "vector" app/src/main/res/drawable/discover_card_placeholder.xml
      grep -q "fun DiscoverProductCard" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
      grep -q "Intent.ACTION_VIEW\\|Intent(Intent.ACTION_VIEW" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
      grep -q "ActivityNotFoundException" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
      grep -q "NumberFormat.getCurrencyInstance(Locale(\"ro\", \"RO\"))" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
      grep -q "AsyncImage" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
      grep -q "fun DiscoverShimmerCard" app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt
      grep -q "rememberInfiniteTransition" app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt
      grep -q "fun DiscoverScreen" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
      grep -q "Icons.Outlined.Search" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
      grep -q "ImeAction.Search" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
      grep -q "SnackbarHost" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
      ./gradlew app:compileDebugKotlin -q
      echo OK
      '
    </automated>
  </verify>

  <done>
    `discover_card_placeholder.xml` exists (vector drawable with gradient + glyph). DiscoverProductCard composable uses Coil AsyncImage + Romanian price formatting + Intent.ACTION_VIEW + ActivityNotFoundException → Snackbar. DiscoverShimmerCard implements the verbatim brush spec from UI-SPEC. DiscoverScreen wires Scaffold + OutlinedTextField + LazyColumn with both sections per the Screen Layout Contract. Kotlin compiles cleanly.
  </done>
</task>

<task type="auto">
  <name>Task 4: Nav rewire (DiscoverKey + AppNavigation entry + GiftMaisonBottomNav slot 2 rename) + StyleGuidePreview DiscoverPreview</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-01, D-02, D-51)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md ("StyleGuidePreview.kt — DiscoverPreview" section)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (current — StoreListKey/StoreBrowserKey already removed by plan 17-01; DiscoverKey absent)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt (current — onStores callback at line ~160 is a no-op placeholder from plan 17-01)
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt (current — NavSlotId.STORES enum, onStores callback param, Icons.Outlined.Storefront + R.string.nav_stores_tab still present from 17-01)
    - app/src/main/res/values/strings.xml (still contains nav_stores_tab — gets deleted in this task)
    - app/src/main/res/values-ro/strings.xml (still contains nav_stores_tab — gets deleted in this task)
    - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt (current — already cleaned up by plan 17-01)
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt (current — find the end-of-file to append the DiscoverPreview section)
  </read_first>

  <files>
    app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt,
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt,
    app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt,
    app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt,
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml,
    app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
  </files>

  <action>
    **Precondition (from plan 17-01):** GiftMaisonBottomNav.kt still has `Icons.Outlined.Storefront` + `R.string.nav_stores_tab` + `NavSlotId.STORES` + `onStores` callback (plan 17-01 deliberately retained the nav_stores_tab string for build-time safety). AppNavigation.kt has an `onStores = { /* placeholder */ }` no-op block from plan 17-01 Task 2. AppNavKeys.kt has no StoreListKey/StoreBrowserKey (deleted by 17-01). This task swaps slot 2 to Discover in lock-step: rename the GiftMaisonBottomNav slot, change the string ref to nav_discover_tab, then delete the now-orphaned nav_stores_tab from both strings.xml files.

    **1. `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt`** — add DiscoverKey:
    ```kotlin
    @Serializable data object DiscoverKey
    ```
    Insert after `@Serializable data object NotificationsKey` (the last current key).

    **2. `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt`** — rewire slot 2:
    - Rename `NavSlotId.STORES` to `NavSlotId.DISCOVER` (line ~38).
    - Rename callback parameter `onStores: () -> Unit` to `onDiscover: () -> Unit` (line ~55).
    - Update the NavItemSlot for slot 2 (currently uses `Icons.Outlined.Storefront`, `R.string.nav_stores_tab`, `selected == NavSlotId.STORES`, `onClick = onStores`):
      - `icon = Icons.Outlined.Search` (D-01)
      - `labelRes = R.string.nav_discover_tab`
      - `isSelected = selected == NavSlotId.DISCOVER`
      - `onClick = onDiscover`
    - Update the `selected = when (currentKey) { … }` mapping (line ~62) to add: `is DiscoverKey -> NavSlotId.DISCOVER` (this means importing DiscoverKey from `com.giftregistry.ui.navigation.DiscoverKey`).
    - Remove the import of `androidx.compose.material.icons.outlined.Storefront` and add `androidx.compose.material.icons.outlined.Search`.

    **3. `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt`** — update KDoc to include DiscoverKey in the visible-on list (already a default no-op since the predicate is hidden-whitelist; just update the docs). Change the "Visible on: every other nav key (HomeKey, RegistryDetailKey, …" line to include `DiscoverKey`.

    **4. `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`** — three edits:
    - Add import: `import com.giftregistry.ui.discover.DiscoverScreen`.
    - Find the `onStores = { … }` callback (placeholder no-op from plan 17-01 at line ~160) and replace with the renamed callback that pushes DiscoverKey:
      ```kotlin
      onDiscover = {
          // Slot 2 → Discover. Nav root — clear any duplicate from back stack.
          if (currentKey !is DiscoverKey) {
              backStack.add(DiscoverKey)
          }
      },
      ```
    - Add a new `entry<DiscoverKey> { DiscoverScreen() }` block inside the `entryProvider { … }` body, preferably near the HomeKey entry so reviewers find it.

    **4b. Delete the now-orphaned `nav_stores_tab` from both string files** (lock-step with step 2 above):
    - `app/src/main/res/values/strings.xml`: delete the line `<string name="nav_stores_tab">STORES</string>` (~line 244).
    - `app/src/main/res/values-ro/strings.xml`: delete the line `<string name="nav_stores_tab">MAGAZINE</string>` (~line 245).
    After this step, neither GiftMaisonBottomNav.kt nor any strings.xml references nav_stores_tab. LocalizationParityTest still passes (both files have the same 10 discover_* keys added in Task 2 + no nav_stores_tab in either).

    **5. `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt`** — per UI-SPEC "StyleGuidePreview.kt — DiscoverPreview" section. Append a new `@Preview` composable showing 5 states (idle, loading, loaded with 2 cards, empty search, error). Use hard-coded fake `DiscoverProduct` instances; render each state in its own LazyColumn item. Example structure:
    ```kotlin
    @Preview(showBackground = true, widthDp = 390, heightDp = 1600, name = "Discover")
    @Composable
    fun DiscoverPreview() {
        GiftMaisonTheme {
            val fakes = listOf(
                DiscoverProduct(id = "f1", title = "Aparat de cafea espresso DeLonghi", description = "15 bar, lapte spumat", imageUrl = "", price = 1299.0, currency = "RON", retailerUrl = "https://emag.ro/x"),
                DiscoverProduct(id = "f2", title = "Cană termica YETI Rambler 20oz", description = "Stainless steel, vacuum insulated", imageUrl = "", price = 199.99, currency = "RON", retailerUrl = "https://yeti.com/y"),
            )
            val snackbar = remember { SnackbarHostState() }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                item { Text("Idle state — populated community", style = MaterialTheme.typography.titleSmall) }
                item { DiscoverProductCard(product = fakes[0], snackbarHostState = snackbar) }
                item { DiscoverProductCard(product = fakes[1], snackbarHostState = snackbar) }

                item { Text("Loading state — 3 shimmer cards") }
                items(3) { DiscoverShimmerCard() }

                item { Text("Loaded (both sections)") }
                item { Text("FROM THE WEB", style = GiftMaisonTheme.typography.monoCaps) }
                item { DiscoverProductCard(product = fakes[0], snackbarHostState = snackbar) }
                item { HorizontalDivider() }
                item { Text("FROM THE COMMUNITY", style = GiftMaisonTheme.typography.monoCaps) }
                item { DiscoverProductCard(product = fakes[1], snackbarHostState = snackbar) }

                item { Text("Empty search") }
                item { Text("No matches found. Try a different search.", style = GiftMaisonTheme.typography.bodyM) }

                item { Text("Error state") }
                item { Text("Could not load. Try again.", style = GiftMaisonTheme.typography.bodyM) }
            }
        }
    }
    ```
    Match the import style + theme wrap convention of existing previews in the file (typically there is a `Surface(GiftMaisonTheme.colors.paper) { … }` wrapper — mirror).
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      # DiscoverKey added
      grep -q "DiscoverKey" app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
      # GiftMaisonBottomNav rewired
      grep -q "NavSlotId.DISCOVER\\|NavSlotId { HOME, DISCOVER" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
      grep -q "Icons.Outlined.Search" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
      grep -q "R.string.nav_discover_tab" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
      grep -q "onDiscover" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
      ! grep -q "NavSlotId.STORES\\|onStores\\|Icons.Outlined.Storefront\\|nav_stores_tab" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
      ! grep -q "nav_stores_tab" app/src/main/res/values/strings.xml
      ! grep -q "nav_stores_tab" app/src/main/res/values-ro/strings.xml
      # AppNavigation rewired
      grep -q "entry<DiscoverKey>" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      grep -q "DiscoverScreen" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      grep -q "onDiscover" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      ! grep -q "onStores" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      # NavVisibility doc updated (visible-on list)
      grep -q "DiscoverKey" app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
      # StyleGuidePreview has DiscoverPreview
      grep -q "fun DiscoverPreview" app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
      # Build + tests
      ./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin -q
      ./gradlew app:testDebugUnitTest --tests com.giftregistry.LocalizationParityTest --tests com.giftregistry.ui.common.chrome.BottomNavVisibilityTest --tests com.giftregistry.ui.discover.DiscoverViewModelTest --tests com.giftregistry.data.discover.DiscoverRepositoryImplTest -q
      echo OK
      '
    </automated>
  </verify>

  <done>
    `DiscoverKey` added to AppNavKeys. GiftMaisonBottomNav slot 2 now reads NavSlotId.DISCOVER with Icons.Outlined.Search + R.string.nav_discover_tab + onDiscover callback (no Storefront / nav_stores_tab / onStores references remain). AppNavigation has `entry<DiscoverKey> { DiscoverScreen() }` and the bottom nav callback pushes DiscoverKey. NavVisibility doc mentions DiscoverKey. StyleGuidePreview has a DiscoverPreview section showing all 5 states. App builds; all targeted unit tests pass.
  </done>
</task>

</tasks>

<verification>
1. `./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin` succeeds.
2. `./gradlew app:testDebugUnitTest --tests com.giftregistry.LocalizationParityTest --tests com.giftregistry.ui.common.chrome.BottomNavVisibilityTest --tests com.giftregistry.ui.discover.DiscoverViewModelTest --tests com.giftregistry.data.discover.DiscoverRepositoryImplTest` all pass.
3. `grep -E "DiscoverKey|DiscoverScreen|onDiscover|nav_discover_tab" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` finds every reference.
4. `grep -E "NavSlotId.STORES|onStores|Icons.Outlined.Storefront|nav_stores_tab|R.string.nav_stores_tab" app/src/main` returns zero lines.
5. Visual confirmation deferred to plan 17-06 on-device UAT.
</verification>

<success_criteria>
- 4-layer Discover stack (domain / data / di / ui) exists: DiscoverProduct, DiscoverRepository interface, DiscoverRepositoryImpl, DiscoverModule, DiscoverUiState, DiscoverViewModel, DiscoverProductCard, DiscoverShimmer, DiscoverScreen.
- discover_card_placeholder.xml vector drawable exists with gradient + glyph.
- 10 `discover_*` string keys + `nav_discover_tab` present in BOTH `values/strings.xml` and `values-ro/strings.xml`. LocalizationParityTest passes.
- Bottom nav slot 2 renamed (NavSlotId.DISCOVER + Icons.Outlined.Search + R.string.nav_discover_tab + onDiscover callback).
- AppNavKeys has DiscoverKey; AppNavigation has `entry<DiscoverKey> { DiscoverScreen() }` and the bottom-nav callback pushes DiscoverKey.
- DiscoverProductCard tap → Intent.ACTION_VIEW with raw retailer URL; ActivityNotFoundException → Snackbar.
- DiscoverViewModel state machines for popular and search both correctly transition idle → loading → loaded/empty/error.
- DiscoverRepositoryImpl maps Callable responses, handles failures with Result.failure, empty array → Result.success(emptyList()).
- StyleGuidePreview has a DiscoverPreview section.
- All targeted unit tests pass (LocalizationParityTest, BottomNavVisibilityTest, DiscoverViewModelTest, DiscoverRepositoryImplTest).
- App compiles cleanly.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-05-SUMMARY.md` documenting:
- Whether FirebaseFunctions was provided by the new DiscoverModule or an existing module (record which).
- The exact Coil import path used (coil3.compose.AsyncImage vs coil.compose.AsyncImage) — to confirm the Coil version pinned in libs.versions.toml.
- The placeholder drawable rendering approach (vector with gradient inside `aapt:attr` vs. flat fallback) — note whichever Android Studio preview accepts.
- Any deviation from the UI-SPEC (e.g., if `accentInk` token didn't exist and a substitute was used).
- A note that on-device visual verification + smoke test happens in plan 17-06.
</output>
