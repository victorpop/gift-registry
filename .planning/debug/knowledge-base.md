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

## 260522-android-form-state-leak — Activity-scoped VM with constant key retains form state; Initial(null) maps to Loading causing infinite spinner on fresh install
- **Date:** 2026-05-22
- **Error patterns:** form state leak, stale data, pre-populated fields, Add Registry, Add Item, second visit, loading spinner, infinite loading, fresh install, clean reinstall, AuthUiState.Loading, Initial, null, AuthStateListener, cached user, startup hang
- **Root cause:** Two separate issues. (1) Form state leak: CreateRegistryViewModel and AddItemViewModel are Activity-scoped via hiltViewModelWithNavArgs with stable constant keys ("new" / registryId). Same VM instance is returned on second screen visit with all MutableStateFlows intact. CreateRegistryViewModel never cleared form fields on successful save; AddItemViewModel's onResetForm() was only called on the "Add another" path, not Save-and-Exit. (2) Startup hang on fresh install: AuthViewModel mapped AuthStateEvent.Initial(null) to AuthUiState.Loading (intentional to prevent BUG-AUTH-FLASH-260512). Firebase only fires AuthStateListener once when there is no cached user, so no subsequent Changed event ever fires. App is permanently stuck in Loading. The withTimeout fix in FirebaseAuthDataSource is correct but only executes in the user != null branch — irrelevant for fresh install.
- **Fix:** (1) resetForm() added to CreateRegistryViewModel, called on create success. AddItemScreen.kt: Save-and-Exit path calls onResetForm() instead of clearSavedItemId(). (2) AuthViewModel: Initial(null) now maps to AuthUiState.Unauthenticated. Safe because FirebaseAuthDataSource (post-getIdToken-health-check fix) never emits Initial(null) for a cached-user scenario — it only emits Initial(null) when user is genuinely null on first callback. withTimeout(15_000L) retained in FirebaseAuthDataSource for the cached-user + unreachable-network safety net.
- **Files changed:** app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt, app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt, app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt
---

## 260513-url-fetch-fails-ikea-android — Firebase Auth emits cached user without token validation; stale token blocks all Firebase calls
- **Date:** 2026-05-13
- **Error patterns:** INVALID_REFRESH_TOKEN, fetchOgMetadata, ExecutionException, FirebaseException, UNAUTHENTICATED, couldn't reach, ogFetchFailed, callable, stale token, emulator reset, cold-launch, anonymous auth, refresh token, getIdToken
- **Root cause:** Firebase Auth SDK on Android restores the cached user from on-disk storage synchronously on app cold-launch, without contacting the auth server to validate the refresh token. After an emulator reset (or any server-side session invalidation), the app enters Authenticated state with a dead token. Every subsequent Firebase call (Functions callable, Firestore streams) internally calls getIdToken(), which hits the auth server and gets INVALID_REFRESH_TOKEN. The callable throws, runCatching catches it, and ogFetchFailed=true shows "couldn't reach that page." The user is permanently stuck in a broken Authenticated state with no recovery path until manual sign-out or uninstall. This pattern applies in production whenever anonymous auth tokens expire.
- **Fix:** Add a getIdToken(forceRefresh=true) health-check in FirebaseAuthDataSource on the first non-null AuthStateEvent.Initial event. On any exception, call auth.signOut() before emitting to the flow — this drives AuthViewModel to Unauthenticated and the nav gate back to AuthScreen. Secondary UX fixes retained: realistic browser UA and 10s timeout in fetchOgMetadata.ts; ogFetchEmpty soft-failure state and item_og_no_data_inline string for the distinct case where the function succeeds but returns no metadata.
- **Files changed:** app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt, functions/src/registry/fetchOgMetadata.ts, app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt, app/src/main/res/values/strings.xml, app/src/main/res/values-ro/strings.xml, app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt
---
