package com.giftregistry.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Acquires a `@HiltViewModel`-annotated [VM] and seeds its [SavedStateHandle] with [args].
 *
 * Navigation3 does not copy nav keys into `SavedStateHandle` the way Navigation Compose 2
 * did — the typed nav key is handed to the entry lambda and that's it. ViewModels that read
 * `savedStateHandle["foo"]` therefore see `null` unless we plumb the args in ourselves.
 *
 * We build `CreationExtras` with [DEFAULT_ARGS_KEY] pointing at a `Bundle` containing [args];
 * `createSavedStateHandle` (used by Hilt's generated factory) picks that bundle up when
 * constructing the VM's `SavedStateHandle`.
 *
 * The hilt-navigation-compose 1.2.0 version of `hiltViewModel()` does not accept a
 * `CreationExtras` override, so we construct Hilt's factory explicitly via
 * `androidx.hilt.navigation.HiltViewModelFactory` and go through the lower-level
 * [viewModel] overload.
 *
 * @param key optional ViewModelStore key — pass a stable value like the primary nav arg
 *   (e.g. `registryId`) so two instances of the same destination don't share a cached VM.
 */
@Composable
inline fun <reified VM : ViewModel> hiltViewModelWithNavArgs(
    key: String? = null,
    vararg args: Pair<String, Any?>,
): VM {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner provided via LocalViewModelStoreOwner"
    }
    val hiltFactory = HiltViewModelFactory(
        context = LocalContext.current,
        delegateFactory = (owner as HasDefaultViewModelProviderFactory)
            .defaultViewModelProviderFactory,
    )
    val extras = MutableCreationExtras(owner.defaultViewModelCreationExtras).apply {
        set(DEFAULT_ARGS_KEY, bundleOf(*args))
    }
    return viewModel(
        viewModelStoreOwner = owner,
        key = key,
        factory = hiltFactory,
        extras = extras,
    )
}
