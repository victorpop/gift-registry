package com.giftregistry.ui.item.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.auth.AuthStateEvent
import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.usecase.AddItemUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveRegistriesUseCase
import com.giftregistry.ui.registry.list.isActive
import com.giftregistry.ui.registry.list.startOfTodayMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AddItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    observeRegistries: ObserveRegistriesUseCase,
    private val addItem: AddItemUseCase,
    private val fetchOgMetadata: FetchOgMetadataUseCase
) : ViewModel() {

    // quick-260428-iny: registryId is now nullable on the nav key. When the user
    // entered AddItemScreen via the FAB sheet (`fromAddSheet=true`) they have not
    // yet picked a registry — the picker rendered as the first form field gates
    // Save until they choose one. For all other paths (CreateRegistry → AddItem
    // chain, Store Browser deep link, Registry Detail FAB) a concrete registryId
    // arrives in savedStateHandle and seeds `_selectedRegistryId` immediately so
    // the picker stays hidden and behaviour matches pre-trim AddItemScreen.
    private val initialRegistryIdFromKey: String =
        savedStateHandle["registryId"] ?: ""

    /** True when AddItemScreen was reached via the trimmed Add-action sheet. */
    val fromAddSheet: Boolean = savedStateHandle["fromAddSheet"] ?: false

    private val _selectedRegistryId = MutableStateFlow<String?>(
        if (initialRegistryIdFromKey.isBlank()) null else initialRegistryIdFromKey
    )
    val selectedRegistryId: StateFlow<String?> = _selectedRegistryId.asStateFlow()

    /** Picker mutator — UI calls this when the user picks a registry from the dropdown. */
    fun setRegistry(id: String) {
        _selectedRegistryId.value = id
    }

    // Phase 7: Plan 03 — pre-fill support from Store Browser
    val initialUrl: String = savedStateHandle["initialUrl"] ?: ""
    val initialRegistryId: String = savedStateHandle["initialRegistryId"] ?: ""

    // quick-260530-nx5: optional pre-fill from a trusted upstream source.
    // When any of these is non-blank, init { } hydrates form state directly
    // and SKIPS the OG-metadata Cloud Function — the upstream (Serper) data
    // is the source of truth; re-fetching could overwrite it with worse OG tags.
    private val prefillTitle: String = savedStateHandle["prefillTitle"] ?: ""
    private val prefillUrl: String = savedStateHandle["prefillUrl"] ?: ""
    private val prefillImageUrl: String = savedStateHandle["prefillImageUrl"] ?: ""
    private val prefillPrice: String = savedStateHandle["prefillPrice"] ?: ""
    @Suppress("unused") // accepted for future retailer-chip rendering
    private val prefillRetailerName: String = savedStateHandle["prefillRetailerName"] ?: ""
    @Suppress("unused") // accepted for future currency display
    private val prefillCurrency: String = savedStateHandle["prefillCurrency"] ?: ""

    private fun hasPrefill(): Boolean =
        prefillTitle.isNotBlank() ||
            prefillUrl.isNotBlank() ||
            prefillImageUrl.isNotBlank() ||
            prefillPrice.isNotBlank()

    /**
     * The signed-in user's ACTIVE registries — drives the picker dropdown when
     * `fromAddSheet=true`. Mirrors RegistryListViewModel's flatMapLatest pattern.
     * Empty list flips the picker into its zero-registry empty-state branch
     * (renders an inline "Create a registry first" affordance).
     *
     * quick-260507-uce: filter through `Registry.isActive(todayMs)` BEFORE the
     * `.catch { ... }.stateIn(...)` so error fallback still emits an empty list.
     * The same predicate powers the Lists screen Active tab — single source of
     * truth in `com.giftregistry.ui.registry.list.TabFilters` (NEVER redefine).
     * Past registries (eventDateMs < startOfTodayMs) must not appear here.
     *
     * `todayMs` is captured INSIDE the `map { }` block (per emission), not once
     * outside it, so a screen kept open past midnight re-evaluates "active"
     * naturally on the next Firestore emission. This mirrors how
     * `RegistryListScreen` recomputes via `remember(registries, ...)`.
     */
    val registriesForPicker: StateFlow<List<Registry>> =
        authRepository.authState
            .filter { event -> event !is AuthStateEvent.Initial || event.user != null }
            .map { event ->
                when (event) {
                    is AuthStateEvent.Initial -> event.user
                    is AuthStateEvent.Changed -> event.user
                }
            }
            .flatMapLatest { user ->
                if (user == null) flowOf(emptyList())
                else observeRegistries(user.uid)
            }
            .map { registries ->
                val todayMs = startOfTodayMs()
                registries.filter { it.isActive(todayMs) }
            }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Form fields
    val url = MutableStateFlow("")
    val title = MutableStateFlow("")
    val imageUrl = MutableStateFlow("")
    val price = MutableStateFlow("")
    val notes = MutableStateFlow("")

    private val _isFetchingOg = MutableStateFlow(false)
    val isFetchingOg: StateFlow<Boolean> = _isFetchingOg.asStateFlow()

    private val _ogFetchFailed = MutableStateFlow(false)
    val ogFetchFailed: StateFlow<Boolean> = _ogFetchFailed.asStateFlow()

    /**
     * True when the Cloud Function call succeeded but returned no usable metadata
     * (e.g. site uses JS rendering, Cloudflare blocked the fetch, or there are no
     * OG tags). Distinct from [ogFetchFailed] which is set when the callable itself
     * throws (emulator unreachable, network error, SDK-level failure).
     *
     * UI shows a softer "No details found" hint rather than the hard "Couldn't reach
     * that page" error so the user understands the URL was reached but no data came back.
     */
    private val _ogFetchEmpty = MutableStateFlow(false)
    val ogFetchEmpty: StateFlow<Boolean> = _ogFetchEmpty.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _savedItemId = MutableStateFlow<String?>(null)
    val savedItemId: StateFlow<String?> = _savedItemId.asStateFlow()

    /**
     * quick-260512-wt8: tracks the most recently fetched URL so the auto-fetch
     * flow can de-dup — typing then deleting then retyping the same URL must
     * NOT re-fetch.
     *
     * The manual icon-button path (`onFetchMetadata()` invoked from the
     * IconButton onClick) deliberately does NOT consult this field — manual
     * retry is allowed even when the URL is unchanged (rationale: retry after
     * a transient error).
     *
     * Updated by `onFetchMetadata()` on BOTH success and failure (failure marks
     * the URL as "we tried it, don't auto-retry"; user can still manual-retry).
     */
    private var lastFetchedUrl: String = ""

    /**
     * quick-260512-wt8: pure helper — used by the auto-fetch flow's filter and
     * unit-testable without Compose / SavedStateHandle. Accepts http/https only,
     * with a non-blank host.
     */
    private fun isValidProductUrl(s: String): Boolean {
        val trimmed = s.trim()
        if (trimmed.isBlank()) return false
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }

    init {
        if (hasPrefill()) {
            // quick-260530-nx5: pre-fill from Discover (Serper-supplied data).
            // Hydrate form fields directly — the OG-metadata Cloud Function is
            // NOT called, both because we already have trustworthy product data
            // and because re-fetching could clobber it with worse OG tags.
            if (prefillUrl.isNotBlank()) {
                url.value = prefillUrl
                // Prime the auto-fetch dedup gate so the debounced flow below
                // does NOT fire on first emission of this URL.
                lastFetchedUrl = prefillUrl.trim()
            }
            if (prefillTitle.isNotBlank()) title.value = prefillTitle
            if (prefillImageUrl.isNotBlank()) imageUrl.value = prefillImageUrl
            if (prefillPrice.isNotBlank()) price.value = prefillPrice
            // prefillRetailerName + prefillCurrency are accepted for future use
            // (e.g. surfacing the retailer chip on the preview card) but the
            // current Item model has no retailerName field — wire when needed.
        } else if (initialUrl.isNotBlank()) {
            url.value = initialUrl
            // quick-260512-wt8: mark as "already requested" so the auto-fetch
            // flow below does not double-fire when it observes the same value
            // on first emission past the drop(1) gate.
            lastFetchedUrl = initialUrl.trim()
            // Fire OG fetch automatically — user can still edit before saving. The
            // existing affiliate transform (ItemRepositoryImpl) runs on save; no
            // changes needed to the affiliate pipeline for Phase 7.
            onFetchMetadata()
        }

        // quick-260512-wt8: Auto-fetch on URL change.
        //
        //   - drop(1)               skip the initial "" emission from MutableStateFlow
        //   - debounce(700)         wait until typing/pasting settles
        //   - distinctUntilChanged  suppress redundant identical re-emissions
        //   - collectLatest         cancel any in-flight fetch when a new url arrives
        //
        // Validity gate (http/https + non-blank host) and de-dup against
        // lastFetchedUrl run INSIDE the collector, AFTER debounce, so the
        // pipeline cancels stale work before it commits writes to the form
        // fields.
        viewModelScope.launch {
            url
                .drop(1)
                .debounce(700)
                .distinctUntilChanged()
                .collectLatest { current ->
                    val trimmed = current.trim()
                    if (!isValidProductUrl(trimmed)) return@collectLatest
                    if (trimmed == lastFetchedUrl) return@collectLatest
                    onFetchMetadata()
                }
        }
    }

    fun onUrlChanged(newUrl: String) {
        url.value = newUrl
    }

    // Called when user finishes entering/pasting URL — fetches OG metadata
    fun onFetchMetadata() {
        val currentUrl = url.value.trim()
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _isFetchingOg.value = true
            _ogFetchFailed.value = false
            _ogFetchEmpty.value = false

            fetchOgMetadata(currentUrl)
                .onSuccess { og ->
                    val hasData = !og.title.isNullOrBlank() ||
                        !og.imageUrl.isNullOrBlank() ||
                        !og.price.isNullOrBlank()
                    Log.d(
                        "AddItemVM",
                        "fetchOgMetadata OK url=$currentUrl hasData=$hasData " +
                            "title=${og.title} image=${og.imageUrl} " +
                            "price=${og.price} priceAmount=${og.priceAmount} " +
                            "priceCurrency=${og.priceCurrency}"
                    )
                    if (hasData) {
                        // Auto-fill form fields — user can edit before saving
                        if (!og.title.isNullOrBlank()) title.value = og.title
                        if (!og.imageUrl.isNullOrBlank()) imageUrl.value = og.imageUrl
                        if (!og.price.isNullOrBlank()) price.value = og.price
                    } else {
                        // Function succeeded but found no OG data — site is JS-rendered
                        // or blocked the scrape. Show a soft hint to fill in manually.
                        _ogFetchEmpty.value = true
                    }
                }
                .onFailure { e ->
                    // Callable itself threw — emulator unreachable, network error, etc.
                    Log.e("AddItemVM", "fetchOgMetadata callable failed for url=$currentUrl", e)
                    _ogFetchFailed.value = true
                }

            _isFetchingOg.value = false
            // quick-260512-wt8: mark this URL as "we tried it" so the auto-fetch
            // flow's de-dup check suppresses an immediate re-fire on the same
            // value. The manual retry path (this same function called from the
            // trailing IconButton) bypasses the de-dup because the auto-fetch
            // flow only enforces it inside its collector — direct invocations
            // always run unconditionally.
            lastFetchedUrl = currentUrl
        }
    }

    fun onSave() {
        if (title.value.isBlank()) {
            _error.value = "Item name is required"
            return
        }

        // quick-260428-iny: defensive guard — UI also disables the Save CTAs when
        // selectedRegistryId is null + fromAddSheet=true. If somehow reached here
        // without a registry chosen, surface a clear error instead of NPE-ing.
        val targetRegistryId = _selectedRegistryId.value
        if (targetRegistryId.isNullOrBlank()) {
            _error.value = "Please choose a registry first"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            val item = Item(
                originalUrl = url.value.trim(),
                title = title.value.trim(),
                imageUrl = imageUrl.value.trim().ifBlank { null },
                price = price.value.trim().ifBlank { null },
                notes = notes.value.trim().ifBlank { null }
            )

            addItem(targetRegistryId, item)
                .onSuccess { itemId -> _savedItemId.value = itemId }
                .onFailure { e -> _error.value = e.message ?: "Failed to save item" }

            _isSaving.value = false
        }
    }

    fun clearError() { _error.value = null }

    /**
     * Called by AddItemScreen's LaunchedEffect immediately after consuming `savedItemId`
     * on the "Save and Exit" path. Resets the field to null so that if the same
     * AddItemKey is re-entered (Activity-scoped ViewModel survives the Activity
     * lifetime), the stale non-null value does not trigger an immediate spurious
     * navigation on the next composition. Mirrors CreateRegistryViewModel.clearSavedRegistryId().
     */
    fun clearSavedItemId() { _savedItemId.value = null }

    // --- Phase 11 Plan 05: derived StateFlows for SCR-10 UI ---

    /** True once OG metadata populated any field and the fetch didn't fail. */
    val ogFetchSucceeded: StateFlow<Boolean> = combine(
        title, imageUrl, price, ogFetchFailed, isFetchingOg,
    ) { t, img, p, failed, fetching ->
        !fetching && !failed && (t.isNotBlank() || img.isNotBlank() || p.isNotBlank())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** True when url host matches a Phase 3 merchantRules entry. */
    val isAffiliateDomain: StateFlow<Boolean> = url.map { u ->
        com.giftregistry.util.AffiliateUrlTransformer.isAffiliateDomain(u)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** SCR-10: "Clear" action on the affiliate confirmation row — resets url and OG-derived fields. */
    fun onClearUrl() {
        url.value = ""
        title.value = ""
        imageUrl.value = ""
        price.value = ""
        _ogFetchFailed.value = false
        _ogFetchEmpty.value = false
        // quick-260512-wt8: reset dedup gate so the next paste/type fetches.
        lastFetchedUrl = ""
    }

    /** SCR-10: "Add another" CTA — save via onSave() then caller calls this to reset all fields. */
    fun onResetForm() {
        url.value = ""
        title.value = ""
        imageUrl.value = ""
        price.value = ""
        notes.value = ""
        _ogFetchFailed.value = false
        _ogFetchEmpty.value = false
        _savedItemId.value = null
        _error.value = null
        // quick-260512-wt8: reset dedup gate so the next paste/type fetches.
        lastFetchedUrl = ""
    }
}
