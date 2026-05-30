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

## registry-tiles-counts-show-zero — Hardcoded zero literals in RegistryCard; no aggregate count fields exist on registry documents
- **Date:** 2026-05-30
- **Error patterns:** counts show zero, items zero, reserved zero, given zero, registry tile, stats, statsLine, home screen, RegistryCard, hardcoded, deferred, Phase 10
- **Root cause:** `statsLine()` in `RegistryCard.kt` passed literal `0` to all three string resources — an intentional Phase 10 deferral noted in a comment. The `Registry` domain model and Firestore registry documents have no aggregate count fields; items live in a subcollection (`registries/{id}/items`). Nothing in `RegistryListViewModel` loaded items for the list screen so counts were never computed.
- **Fix:** Added `RegistryCounts` data class. `RegistryListViewModel` combines a per-registry `ObserveItemsUseCase` flow for every registry, computing items/reserved/given from `ItemStatus` client-side using `kotlinx.coroutines.flow.combine`. Threaded `RegistryCounts` through `RegistryListUiState.Success`, `RegistryListScreen`, and `RegistryCard`.
- **Files changed:** app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt, app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt, app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
---

## build-flag-env-mismatch — installDebug silently switches Firebase backend when -Puse_emulator flag is not matched to recent task context
- **Date:** 2026-05-30
- **Error patterns:** no registries, empty list, registries missing, works for one user not another, installDebug, use_emulator, emulator, production, gradle flag, environment mismatch, seed data, different data
- **Root cause:** This project's debug builds default to `use_emulator=true` (local Firebase emulator). Prior tasks in the same session had built with `-Puse_emulator=false` (production Firebase). Running plain `./gradlew :app:installDebug` silently reverted to the emulator backend, which has different seed data. The test account (maria.alexa.pop@gmail.com) owns 0 registries on the emulator, so the list appeared empty — a false verification failure that looked like a broken feature.
- **Fix (process):** Before running `installDebug` to verify a fix, check the most recent `.planning/quick/*/SUMMARY.md` or commit messages for the gradle flag used in prior tasks and match it. Always pass `-Puse_emulator=false` when the intent is to verify against production Firebase. When the environment switches silently, treat an empty list as a suspected backend mismatch, not a code regression.
- **Files changed:** (process issue — no code changed)
---

## 260513-url-fetch-fails-ikea-android — Firebase Auth emits cached user without token validation; stale token blocks all Firebase calls
- **Date:** 2026-05-13
- **Error patterns:** INVALID_REFRESH_TOKEN, fetchOgMetadata, ExecutionException, FirebaseException, UNAUTHENTICATED, couldn't reach, ogFetchFailed, callable, stale token, emulator reset, cold-launch, anonymous auth, refresh token, getIdToken
- **Root cause:** Firebase Auth SDK on Android restores the cached user from on-disk storage synchronously on app cold-launch, without contacting the auth server to validate the refresh token. After an emulator reset (or any server-side session invalidation), the app enters Authenticated state with a dead token. Every subsequent Firebase call (Functions callable, Firestore streams) internally calls getIdToken(), which hits the auth server and gets INVALID_REFRESH_TOKEN. The callable throws, runCatching catches it, and ogFetchFailed=true shows "couldn't reach that page." The user is permanently stuck in a broken Authenticated state with no recovery path until manual sign-out or uninstall. This pattern applies in production whenever anonymous auth tokens expire.
- **Fix:** Add a getIdToken(forceRefresh=true) health-check in FirebaseAuthDataSource on the first non-null AuthStateEvent.Initial event. On any exception, call auth.signOut() before emitting to the flow — this drives AuthViewModel to Unauthenticated and the nav gate back to AuthScreen. Secondary UX fixes retained: realistic browser UA and 10s timeout in fetchOgMetadata.ts; ogFetchEmpty soft-failure state and item_og_no_data_inline string for the distinct case where the function succeeds but returns no metadata.
- **Files changed:** app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt, functions/src/registry/fetchOgMetadata.ts, app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt, app/src/main/res/values/strings.xml, app/src/main/res/values-ro/strings.xml, app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt
---

## add-first-item-navigates-to-home-not-registry — Navigation3: AddItem pushed without RegistryDetailKey on stack causes post-create save to land on Home
- **Date:** 2026-05-30
- **Error patterns:** navigates to home, wrong screen after save, Home screen, registry detail, first item, post-create, CTA, add items, back stack, AddItemKey, RegistryDetailKey, CreateRegistryKey, onSaved, AppNavigation
- **Root cause:** `AppNavigation.kt` `entry<CreateRegistryKey>.onSaved` popped `CreateRegistryKey` then pushed `AddItemKey` without first inserting `RegistryDetailKey(registryId)`. Back stack after the post-create CTA was `[HomeKey, AddItemKey]`. `AddItemScreen.onBack()` always pops exactly one entry on save, so it landed on `HomeKey`. The working path (RegistryDetail → Add Item) was unaffected because `RegistryDetailKey` was already beneath `AddItemKey` on that stack.
- **Fix:** Inserted `backStack.add(RegistryDetailKey(registryId))` between the `removeAt` and the `AddItemKey` push in `entry<CreateRegistryKey>.onSaved`. Stack becomes `[HomeKey, RegistryDetailKey, AddItemKey]`; pop after save lands correctly on `RegistryDetailKey`.
- **Files changed:** app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
---

## navigation3-stack-shaping — General pattern: leaf screen pops a fixed count; call site must seed the return destination before pushing the leaf
- **Date:** 2026-05-30
- **Error patterns:** wrong destination after save, back navigates to wrong screen, lands on home, save and return, onBack, pop, backStack, Navigation3, AddItemKey, leaf screen, entry path, single path works other does not
- **Root cause:** In this codebase, leaf screens (e.g. AddItemScreen) call a single `onBack()` / `backStack.removeAt(backStack.lastIndex)` on completion. They are path-agnostic. When a call site that opens a leaf screen does not place the intended return destination on the stack before the leaf key, the pop lands one level higher than expected. The bug manifests only for that specific entry path; other entry paths that naturally have the return destination on the stack appear correct.
- **Fix (pattern):** Each `backStack.add(<LeafKey>)` call site owns the responsibility of ensuring the intended return destination is already on the stack. Do not fix this from inside the leaf screen. Detection heuristic: if a save-and-return flow lands on the wrong screen for only one entry path, grep all `backStack.add(<LeafKey>)` sites and verify the parent entry is present beneath the leaf in every case.
- **Files changed:** (pattern entry — no single file; applies to AppNavigation.kt call sites generally)
---

## create-registry-default-other-blocks-cover-picker — ViewModel "" initial value disagrees with canonical "Custom" form; cover picker gated on raw value
- **Date:** 2026-05-30
- **Error patterns:** Other tile highlighted, cover picker disabled, pick an occasion, cover photo picker, OccasionTileGrid, isCoverPickerEnabled, MutableStateFlow, isNullOrBlank, first render, first frame, create registry, occasion empty string
- **Root cause:** `CreateRegistryViewModel` initialized `occasion = MutableStateFlow("")`. `OccasionTileGrid` canonicalizes `""` to `"Custom"` via `OccasionCatalog.storageKeyFor` and highlights the Other tile, but `isCoverPickerEnabled("")` short-circuits on `isNullOrBlank()` and returns false. The visual default and the gating predicate operated on two different representations of "nothing chosen."
- **Fix:** Changed `MutableStateFlow("")` to `MutableStateFlow("Custom")` on line 44 and `occasion.value = ""` to `occasion.value = "Custom"` in `resetForm()` on line 275 in `CreateRegistryViewModel.kt`. Both views now agree on the same canonical initial value.
- **Files changed:** app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt
---

## visual-default-predicate-mismatch — General pattern: UI visual fallback for empty state must agree with downstream gating predicates
- **Date:** 2026-05-30
- **Error patterns:** element highlighted but disabled, UI highlights but feature gated, selected but not enabled, visual selection disagrees with state, default state, sentinel value, isNullOrBlank, canonical, fallback rendering, first frame, cover picker, occasion
- **Root cause:** When a UI component uses a fallback rendering for an empty/null state (e.g. "highlight Other when nothing is selected"), any downstream predicates that gate behavior on that same state operate on the raw StateFlow value — not the canonicalized form. The visual layer translates the sentinel; the predicate does not. Result: the element appears selected/active but the feature it guards is disabled.
- **Fix (pattern):** Make the canonical value the actual initial state — never let the "default" be a sentinel that only the visual layer translates. Change the StateFlow initializer and any reset paths to use the canonical value directly. Detection heuristic: when a UI element is highlighted but downstream gating disagrees, grep for the StateFlow's initial value and check whether it equals the value the highlight code resolves to. If not, they will always disagree on first render.
- **Files changed:** (pattern entry — no single file)
---

## inbox-invite-modal-missing-fields-and-rough-rise — Description missing from invite sheet; sheet requires manual drag to expand
- **Date:** 2026-05-30
- **Error patterns:** description missing, event date missing, invite sheet, ModalBottomSheet, InviteResponseSheet, rough rise, manual drag, half-expanded, buildEnrichedInvitePayload, notification payload, skipPartiallyExpanded, inbox, invite, modal
- **Root cause:** Two independent bugs. (1) Double-sided omission: `buildEnrichedInvitePayload` in `inviteNotificationHelpers.ts` never wrote `description` into the notification payload, AND `InviteResponseSheet.kt` never read or rendered it. Both sides of the contract were missing simultaneously. (2) `rememberModalBottomSheetState()` called without `skipPartiallyExpanded = true`; Material3 default is `false`, so the sheet landed in half-expanded state on first frame requiring user drag to reveal content.
- **Fix:** (1) Added `description` field to `buildEnrichedInvitePayload` return value; added render block in `InviteResponseSheet` between registryName and eventDateLabel. (2) Added `skipPartiallyExpanded = true` to `rememberModalBottomSheetState` call, preserving the existing `confirmValueChange` guard.
- **Files changed:** functions/src/registry/inviteNotificationHelpers.ts, app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt
---

## material3-bottom-sheet-half-expanded-default — General pattern: ModalBottomSheet defaults to half-expanded; requires explicit opt-out
- **Date:** 2026-05-30
- **Error patterns:** sheet rises but requires drag, half expanded, scroll to see content, ModalBottomSheet, rememberModalBottomSheetState, skipPartiallyExpanded, animate, expand, bottom sheet, rough rise, content hidden
- **Root cause:** `rememberModalBottomSheetState()` in Material3 defaults to `skipPartiallyExpanded = false`. When sheet content is taller than the partial-expand threshold, the sheet animates to the half-expanded stop and requires user drag to reveal the rest. This is correct default behavior for sheets where partial content is intentional; it is a bug for sheets meant to open fully in one animation.
- **Fix (pattern):** Pass `skipPartiallyExpanded = true` explicitly whenever the intent is for the sheet to rise to full content height in one step. Symptom heuristic: "sheet rises but user has to scroll/drag to see content" — check this argument before investigating layout/scroll modifiers, padding, or content height calculations.
- **Files changed:** (pattern entry — no single file)
---

## denormalized-notification-payload-double-sided-contract — General pattern: notification payload additions must be written by the Function AND read by the client
- **Date:** 2026-05-30
- **Error patterns:** field missing in UI, notification payload, inbox, invite, Cloud Function, composable, buildEnrichedInvitePayload, description, missing field, one side fixed, both sides
- **Root cause:** Denormalized notification payloads (written at event time, read by inbox UI) are a double-sided contract: the Cloud Function writer owns the payload schema; the client composable owns the rendering. Adding a field only on the client silently succeeds at compile time but the field is always null at runtime. Adding it only on the server writes the data but nothing renders. When investigating "field missing in client UI" for notification/inbox flows, both the writer (Cloud Function helper) and the reader (composable) must be checked for the field name.
- **Fix (pattern):** When adding any field visible in the inbox/notification UI, grep both the Cloud Function payload builder AND the composable reader for that field name before declaring the fix complete. If either side is missing, the change is incomplete.
- **Files changed:** (pattern entry — no single file)
---

## notification-payload-schema-no-backfill — General pattern: schema additions to denormalized notification payloads do not retroactively populate existing documents
- **Date:** 2026-05-30
- **Error patterns:** old notification still blank, existing invite missing field, backfill, retroactive, notification payload, inbox, denormalized, schema addition, new field, existing documents
- **Root cause:** Notification documents are written once at event time and never updated. Adding a field to the Cloud Function payload builder has no effect on documents already written to Firestore. Existing inbox entries will not show the new field until a fresh write event creates a new notification document.
- **Fix (pattern):** When verifying a notification payload fix, always use a NEW event (new invite, new notification trigger) to confirm the field appears — do not test against pre-existing inbox entries. When handing off to the user for verification, explicitly tell them that existing inbox entries will still be blank and that is expected; only entries created after the deploy will carry the new field.
- **Files changed:** (pattern entry — no single file)
---
