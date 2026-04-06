---
phase: 03-registry-item-management
verified: 2026-04-06T12:00:00Z
status: gaps_found
score: 9/10 must-haves verified
gaps:
  - truth: "Invited non-users accessing private registry link see login/signup/guest options (REG-08)"
    status: partial
    reason: "Deep link routing works and unauthenticated users are sent to AuthKey. AuthScreen renders sign in and sign up (Google + email). However, the 'Continue as Guest' option is absent from AuthScreen — the string resource auth_continue_as_guest exists but is never rendered. REG-08 explicitly requires guest as a third option alongside login and signup."
    artifacts:
      - path: "app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt"
        issue: "auth_continue_as_guest string resource exists but no TextButton or clickable renders it. No call to signInAnonymously anywhere in AuthScreen or AuthViewModel."
    missing:
      - "Add 'Continue as Guest' TextButton to AuthScreen calling AuthViewModel.signInAnonymously (or equivalent)"
      - "Wire signInAnonymously in AuthViewModel if not already implemented (check Phase 2 AuthViewModel)"
human_verification:
  - test: "Registry CRUD end-to-end flow"
    expected: "Owner can create, list, edit, and delete registries with data persisting to Firestore emulator"
    why_human: "Requires running Android emulator and Firebase emulator suite together"
  - test: "Item add with EMAG URL OG auto-fill"
    expected: "Pasting an EMAG URL fetches OG metadata and auto-fills title, image, price fields"
    why_human: "Requires live network or OG mock server; involves UI state (loading spinner, auto-fill)"
  - test: "Invite bottom sheet sends invite and updates Firestore invitedUsers map"
    expected: "Email entered in bottom sheet triggers inviteToRegistry callable, Firestore shows invitedUsers updated"
    why_human: "Requires Firebase emulator and manual inspection of Firestore data"
  - test: "Deep link routes unauthenticated user to AuthKey"
    expected: "adb shell am start with registry deep link while logged out shows AuthScreen"
    why_human: "Requires physical device or emulator ADB"
  - test: "Affiliate URL correctly set on saved items"
    expected: "EMAG items stored with affiliateUrl containing event.2performant.com; non-EMAG items store originalUrl"
    why_human: "Requires Firestore emulator inspection after item creation"
---

# Phase 3: Registry Item Management Verification Report

**Phase Goal:** Registry owners can create and manage registries, add items via any URL with automatic affiliate tag injection, and gift givers see real-time item status
**Verified:** 2026-04-06
**Status:** gaps_found
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Owner can create and manage registries (list, create, edit, delete) | ✓ VERIFIED | RegistryListScreen/RegistryListViewModel, CreateRegistryScreen/CreateRegistryViewModel, RegistryDetailScreen/RegistryDetailViewModel all exist and wire to Firestore via use cases |
| 2 | Owner can add items by pasting any URL with automatic affiliate tag injection | ✓ VERIFIED | AddItemScreen + AddItemViewModel exist; ItemRepositoryImpl.addItem calls AffiliateUrlTransformer.transform before write; EMAG URLs get 2Performant URL, unknown pass through |
| 3 | Auto-fill populates title, image, price from OG metadata | ✓ VERIFIED | AddItemViewModel.onFetchMetadata calls FetchOgMetadataUseCase; loading/error states present; auto-filled fields editable |
| 4 | Gift givers see real-time item status (Available, Reserved, Purchased) | ✓ VERIFIED | RegistryDetailViewModel uses ObserveItemsUseCase (callbackFlow-based); RegistryDetailScreen renders ItemStatusChip with all three statuses using string resources |
| 5 | Owner can invite users to private registries | ✓ VERIFIED | InviteBottomSheet + InviteViewModel wired into RegistryDetailKey entry; calls InviteToRegistryUseCase; inviteToRegistry Cloud Function writes to invitedUsers map |
| 6 | Invited non-users see login/signup/guest options when accessing registry link | ✗ PARTIAL | Deep link routing works (MainActivity extracts registryId, AppNavigation routes unauthenticated to AuthKey). AuthScreen shows login and sign up but does NOT render the "Continue as Guest" option — auth_continue_as_guest string exists but no composable renders it |
| 7 | EMAG URLs receive affiliate tags; unknown URLs pass through unchanged | ✓ VERIFIED | AffiliateUrlTransformer has emag.ro rule generating event.2performant.com URL; unknown merchants return wasTransformed=false; AFF-04 logs via Log.w |
| 8 | Firestore data layer with real-time observation wired through Hilt | ✓ VERIFIED | FirestoreDataSource uses callbackFlow + awaitClose; RegistryRepositoryImpl and ItemRepositoryImpl use runCatching; AppModule provides FirebaseFirestore + FirebaseFunctions; DataModule binds both repositories |
| 9 | Security rule tests cover invited user read access | ✓ VERIFIED | tests/rules/firestore.rules.test.ts contains 15 tests including 3 new invite flow tests: "invited user can read private registry", "non-invited user cannot read private registry", "invited user can read items in private registry" |
| 10 | All UI labels externalized in EN and RO string resources | ✓ VERIFIED | 50+ Phase 3 string keys present in both values/strings.xml and values-ro/strings.xml; grep confirms 5 spot-check keys in both files; no hardcoded English in Screen.kt files |

**Score:** 9/10 truths verified (REG-08 partial)

---

## Required Artifacts

### Plan 00 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/test/java/com/giftregistry/data/registry/FakeRegistryRepository.kt` | Fake RegistryRepository | ✓ VERIFIED | `class FakeRegistryRepository : RegistryRepository` confirmed |
| `app/src/test/java/com/giftregistry/data/item/FakeItemRepository.kt` | Fake ItemRepository | ✓ VERIFIED | `class FakeItemRepository : ItemRepository` confirmed |
| `app/src/test/java/com/giftregistry/domain/usecase/CreateRegistryUseCaseTest.kt` | Test stub | ✓ VERIFIED | File exists |
| `app/src/test/java/com/giftregistry/domain/usecase/ObserveItemsUseCaseTest.kt` | Test stub with Turbine | ✓ VERIFIED | File exists |

### Plan 01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/giftregistry/domain/model/Registry.kt` | Registry domain model | ✓ VERIFIED | `data class Registry` with `invitedUsers: Map<String, Boolean>` confirmed |
| `app/src/main/java/com/giftregistry/domain/model/Item.kt` | Item domain model | ✓ VERIFIED | `data class Item` with `affiliateUrl` field confirmed |
| `app/src/main/java/com/giftregistry/domain/model/ItemStatus.kt` | ItemStatus enum | ✓ VERIFIED | AVAILABLE/RESERVED/PURCHASED enum with fromString factory |
| `app/src/main/java/com/giftregistry/domain/registry/RegistryRepository.kt` | Registry repository interface | ✓ VERIFIED | `interface RegistryRepository` with full CRUD + observe |
| `app/src/main/java/com/giftregistry/domain/item/ItemRepository.kt` | Item repository interface | ✓ VERIFIED | `interface ItemRepository` with fetchOgMetadata |
| `app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt` | Affiliate URL transformer | ✓ VERIFIED | `object AffiliateUrlTransformer` with emag.ro rule and event.2performant.com URL |
| `app/src/test/java/com/giftregistry/util/AffiliateUrlTransformerTest.kt` | Transformer tests | ✓ VERIFIED | File exists with 5+ tests |
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` | Phase 3 nav keys | ✓ VERIFIED | CreateRegistryKey, RegistryDetailKey, AddItemKey, EditItemKey, EditRegistryKey all present |

### Plan 02 Artifacts (Cloud Functions)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `functions/src/registry/fetchOgMetadata.ts` | OG metadata callable | ✓ VERIFIED | `export const fetchOgMetadata = onCall({ region: "europe-west3" })` with parse(html) |
| `functions/src/registry/inviteToRegistry.ts` | Invite callable | ✓ VERIFIED | `export const inviteToRegistry = onCall({ region: "europe-west3" })` with invitedUsers write |
| `functions/src/index.ts` | Function exports | ✓ VERIFIED | Exports fetchOgMetadata and inviteToRegistry |

### Plan 03 Artifacts (Data Layer)

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` | Firestore data access | ✓ VERIFIED | `class FirestoreDataSource` with callbackFlow + awaitClose for registries and items |
| `app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt` | Registry impl | ✓ VERIFIED | `class RegistryRepositoryImpl` wrapping FirestoreDataSource with runCatching |
| `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt` | Item impl | ✓ VERIFIED | `class ItemRepositoryImpl` with AffiliateUrlTransformer applied in addItem |
| `app/src/main/java/com/giftregistry/di/AppModule.kt` | Hilt DI providers | ✓ VERIFIED | provideFirebaseFirestore and provideFirebaseFunctions present |

### Plan 04/05 UI Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` | Registry list composable | ✓ VERIFIED | `fun RegistryListScreen` with hiltViewModel(), collectAsStateWithLifecycle(), string resources |
| `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt` | Registry list ViewModel | ✓ VERIFIED | `@HiltViewModel class RegistryListViewModel` with ObserveRegistriesUseCase + DeleteRegistryUseCase |
| `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` | Create/edit registry | ✓ VERIFIED | Full form with occasion dropdown, visibility radio buttons, notifications switch (REG-09) |
| `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` | Registry detail | ✓ VERIFIED | Items list with ItemStatusChip composable for all three statuses |
| `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` | Add item screen | ✓ VERIFIED | URL field, Fetch button, loading state, error fallback, editable form |
| `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` | Add item ViewModel | ✓ VERIFIED | FetchOgMetadataUseCase + AddItemUseCase; _isFetchingOg and _ogFetchFailed state flows |
| `app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt` | Edit item screen | ✓ VERIFIED | Pre-filled form with item_edit_title |
| `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` | Invite bottom sheet | ✓ VERIFIED | `fun InviteBottomSheet` with registry_invite_title and registry_invite_send_button |
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` | Navigation wired | ✓ VERIFIED | All Phase 3 entries present; InviteBottomSheet wired; deepLinkRegistryId support |
| `tests/rules/firestore.rules.test.ts` | Security rule tests | ✓ VERIFIED | 15 tests including 3 new invite flow tests |
| `app/src/main/AndroidManifest.xml` | Deep link intent filter | ✓ VERIFIED | android:host="giftregistry.app" + android:pathPrefix="/registry/" present |
| `app/src/main/java/com/giftregistry/MainActivity.kt` | Deep link extraction | ✓ VERIFIED | deepLinkRegistryId extracted from intent.data and passed to AppNavigation |
| `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` | Guest option for REG-08 | ✗ STUB | Sign in and sign up present. auth_continue_as_guest string resource exists but no composable renders it; signInAnonymously not called anywhere in AuthScreen |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| FakeRegistryRepository | RegistryRepository interface | implements | ✓ WIRED | `class FakeRegistryRepository : RegistryRepository` |
| RegistryRepositoryImpl | FirestoreDataSource | constructor injection | ✓ WIRED | `private val dataSource: FirestoreDataSource` |
| ItemRepositoryImpl | AffiliateUrlTransformer | import + addItem call | ✓ WIRED | `import com.giftregistry.util.AffiliateUrlTransformer` + called in addItem |
| DataModule | RegistryRepositoryImpl | @Binds | ✓ WIRED | `abstract fun bindRegistryRepository(impl: RegistryRepositoryImpl): RegistryRepository` |
| DataModule | ItemRepositoryImpl | @Binds | ✓ WIRED | `abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository` |
| functions/src/index.ts | fetchOgMetadata.ts | export re-export | ✓ WIRED | `export { fetchOgMetadata } from "./registry/fetchOgMetadata"` |
| functions/src/index.ts | inviteToRegistry.ts | export re-export | ✓ WIRED | `export { inviteToRegistry } from "./registry/inviteToRegistry"` |
| RegistryListViewModel | ObserveRegistriesUseCase | constructor injection | ✓ WIRED | `observeRegistries: ObserveRegistriesUseCase` in constructor |
| AppNavigation | RegistryListScreen | entry<HomeKey> | ✓ WIRED | `entry<HomeKey> { RegistryListScreen(...)` |
| AddItemViewModel | FetchOgMetadataUseCase | constructor injection | ✓ WIRED | `private val fetchOgMetadata: FetchOgMetadataUseCase` |
| AddItemViewModel | AddItemUseCase | constructor injection | ✓ WIRED | `private val addItem: AddItemUseCase` |
| InviteBottomSheet | InviteToRegistryUseCase | ViewModel injection | ✓ WIRED | InviteViewModel injects InviteToRegistryUseCase; InviteBottomSheet uses hiltViewModel() |
| AppNavigation | InviteBottomSheet | RegistryDetailKey entry | ✓ WIRED | `if (showInviteSheet) { InviteBottomSheet(...) }` in entry<RegistryDetailKey> |
| AppNavigation | deepLinkRegistryId | MainActivity pass-through | ✓ WIRED | `AppNavigation(deepLinkRegistryId = deepLinkRegistryId)` in MainActivity |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| RegistryListScreen | `uiState` (list of Registry) | ObserveRegistriesUseCase → RegistryRepositoryImpl → FirestoreDataSource.observeRegistries() → callbackFlow Firestore listener | Yes — callbackFlow wired to Firestore `registries` collection filtered by ownerId | ✓ FLOWING |
| RegistryDetailScreen | `items` (List<Item>) | ObserveItemsUseCase → ItemRepositoryImpl → FirestoreDataSource.observeItems() → callbackFlow on items subcollection | Yes — callbackFlow wired to `registries/{id}/items` subcollection | ✓ FLOWING |
| AddItemScreen | `title`, `imageUrl`, `price` | AddItemViewModel.onFetchMetadata → FetchOgMetadataUseCase → ItemRepositoryImpl.fetchOgMetadata() → FirebaseFunctions callable | Yes — calls inviteToRegistry Cloud Function which fetches real HTML; returns null fallback on failure | ✓ FLOWING |
| InviteBottomSheet | `inviteSent` / `error` | InviteViewModel.onSendInvite → InviteToRegistryUseCase → RegistryRepositoryImpl.inviteUser() → FirebaseFunctions callable | Yes — calls inviteToRegistry Cloud Function writing to Firestore invitedUsers map | ✓ FLOWING |

---

## Behavioral Spot-Checks

Step 7b SKIPPED — app requires Android emulator + Firebase emulator to run; no static behavior verifiable without build. Compilation checks were performed by Claude during phase execution (SUMMARY files document BUILD SUCCESSFUL). Human verification items cover runtime behaviors.

---

## Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| REG-01 | 00, 01, 03, 04 | Owner can create a registry with name, occasion, date, location, description | ✓ SATISFIED | CreateRegistryScreen has all fields; CreateRegistryUseCase + RegistryRepositoryImpl write to Firestore |
| REG-02 | 01, 03, 04 | Owner can edit all registry details | ✓ SATISFIED | CreateRegistryScreen handles edit mode via SavedStateHandle + UpdateRegistryUseCase |
| REG-03 | 01, 03, 04 | Owner can delete a registry | ✓ SATISFIED | Delete with confirmation dialog in RegistryListScreen; DeleteRegistryUseCase wired |
| REG-04 | 01, 03, 04 | Owner can set visibility to public or private | ✓ SATISFIED | CreateRegistryScreen has RadioButton row for public/private; stored in Firestore |
| REG-05 | 02, 03, 05 | Owner can invite users via email | ✓ SATISFIED | InviteBottomSheet + InviteViewModel → InviteToRegistryUseCase → inviteToRegistry Cloud Function |
| REG-06 | 02, 05 | Invited users with accounts receive in-app notification and email | ✓ SATISFIED (stub) | inviteToRegistry distinguishes existing vs new users via admin.auth().getUserByEmail; email delivery stubbed (Phase 6 per plan) |
| REG-07 | 02, 05 | Invited users without accounts receive email only | ✓ SATISFIED (stub) | inviteToRegistry handles non-user case with email: prefix key; email delivery stubbed (Phase 6 per plan) |
| REG-08 | 05 | Invited non-users see login/signup/guest options upon accessing the link | ✗ PARTIAL | Deep link routes to AuthKey; AuthScreen shows sign in + sign up + Google. Guest option string exists (auth_continue_as_guest) but no button renders it in AuthScreen |
| REG-09 | 03, 04 | Owner can opt in/out of purchase notifications | ✓ SATISFIED | CreateRegistryScreen has Switch for notificationsEnabled; stored in Registry model |
| REG-10 | 01, 03, 04 | Owner can have multiple active registries | ✓ SATISFIED | observeRegistries(ownerId) returns all registries for owner; LazyColumn renders all |
| ITEM-01 | 01, 03, 05 | Owner can add an item by pasting any URL | ✓ SATISFIED | AddItemScreen with URL field; AddItemUseCase → ItemRepositoryImpl.addItem |
| ITEM-02 | 02, 03, 05 | URL import auto-fills title, image, price via OG metadata | ✓ SATISFIED | AddItemViewModel.onFetchMetadata calls FetchOgMetadataUseCase → Cloud Function; auto-fills fields |
| ITEM-05 | 01, 03, 05 | Owner can manually edit item details | ✓ SATISFIED | AddItemScreen fields are editable after auto-fill; EditItemScreen with UpdateItemUseCase |
| ITEM-06 | 01, 03, 05 | Owner can remove an item | ✓ SATISFIED | Delete item with confirmation dialog in RegistryDetailScreen; DeleteItemUseCase wired |
| ITEM-07 | 00, 01, 03, 04, 05 | Items display real-time status (available, reserved, purchased) | ✓ SATISFIED | RegistryDetailViewModel uses ObserveItemsUseCase (callbackFlow); ItemStatusChip renders all three statuses |
| AFF-01 | 01, 03, 05 | URL transformer identifies merchant domain and appends correct affiliate tag | ✓ SATISFIED | AffiliateUrlTransformer.merchantRules maps emag.ro to buildEmagAffiliateUrl |
| AFF-02 | 01, 03, 05 | EMAG items receive affiliate tags automatically on add | ✓ SATISFIED | ItemRepositoryImpl.addItem calls AffiliateUrlTransformer.transform before every item write |
| AFF-03 | 01, 03, 05 | Affiliate tag injection is invisible to users | ✓ SATISFIED | Transform happens in data layer (ItemRepositoryImpl); UI does not expose affiliateUrl to owner on add |
| AFF-04 | 01, 03, 05 | Unknown merchant URLs pass through without breaking | ✓ SATISFIED | wasTransformed=false path in ItemRepositoryImpl logs via Log.w and stores originalUrl as affiliateUrl |

**Orphaned requirements check:** ITEM-03 and ITEM-04 (EMAG catalog browse/search) appear in REQUIREMENTS.md as unchecked and are NOT claimed by any Phase 3 plan — correct, these are future scope.

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt` | 16-18 | PLACEHOLDER_UNIQUE_ID, PLACEHOLDER_AFF_CODE, PLACEHOLDER_CAMPAIGN_ID hardcoded | ⚠️ Warning | All EMAG affiliate URLs generated in dev/prod will contain placeholder IDs — no real affiliate commission trackable. Known and documented in SUMMARY; requires BuildConfig wiring from local.properties before production. Does NOT block Phase 3 goal (transform logic is correct). |
| `functions/src/registry/inviteToRegistry.ts` | 71 | `[STUB] Invite email would be sent` — email delivery not implemented | ℹ️ Info | Email sending deferred to Phase 6 per plan. inviteToRegistry correctly writes invitedUsers map and distinguishes existing vs new users. Not a blocker for Phase 3 goal. |
| `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` | (no line) | auth_continue_as_guest string not rendered — no guest sign-in path | 🛑 Blocker | REG-08 requires guest option. The string resource and auth_continue_as_guest pattern were planned in Phase 2 UI spec but never implemented. Unauthenticated users accessing a registry deep link can only sign in or create an account, not continue as guest. |

---

## Human Verification Required

### 1. Registry CRUD Flow

**Test:** Start Firebase emulator suite (`firebase emulators:start`), run Android app on emulator, sign in, create a registry with all fields (title, occasion, date, location, description), toggle notifications switch, save. Verify: registry appears in list, navigate to detail, edit registry, delete with confirmation dialog.
**Expected:** Full CRUD with data persisting to Firestore emulator; multiple registries can be created (REG-10)
**Why human:** Requires running Android emulator and Firebase emulator together; visual state transitions and dialogs cannot be verified statically

### 2. Item Add with OG Auto-fill

**Test:** From registry detail, tap Add Item. Paste an EMAG URL (e.g., `https://www.emag.ro/laptop-lenovo-ideapad/pd/DXXXXXXX/`). Wait for OG fetch. Verify title/image/price auto-fill. Edit one field manually. Save item.
**Expected:** Loading spinner appears while fetching; fields auto-fill from OG metadata; fields are editable; item saved and visible in registry detail with Available status
**Why human:** Requires live network call or OG mock; visual loading state and auto-fill behavior cannot be statically verified

### 3. Invite Flow

**Test:** From registry detail overflow menu, tap Invite. Enter an email address. Tap Send. Check Firestore emulator console for `invitedUsers` map update.
**Expected:** Bottom sheet opens; success message shown after send; Firestore `registries/{id}` document shows `invitedUsers` key updated
**Why human:** Requires Firebase emulator and Firestore data inspection

### 4. Deep Link Routing (REG-08 partial)

**Test:** While logged out, run `adb shell am start -a android.intent.action.VIEW -d "https://giftregistry.app/registry/test-id"`. Observe what screen appears.
**Expected:** AuthScreen appears with sign in and sign up options. Note: guest option is currently MISSING — this is the known gap.
**Why human:** Requires ADB and Android emulator

### 5. Affiliate URL Injection

**Test:** Add an EMAG item. Inspect the saved Firestore document in the emulator. Check `affiliateUrl` field value.
**Expected:** EMAG items have `affiliateUrl` containing `event.2performant.com/events/click` with PLACEHOLDER IDs. Non-EMAG items have `affiliateUrl == originalUrl`.
**Why human:** Requires Firestore emulator document inspection

---

## Gaps Summary

**One gap blocks full goal achievement:**

**REG-08 — Guest option missing from AuthScreen.** The phase goal partially satisfies REG-08: deep link routing is complete (unauthenticated users accessing a registry link are routed to AuthKey), and sign in / sign up options are shown. However, "login/signup/guest options" per REG-08 requires all three paths. The `auth_continue_as_guest` string resource was added in Phase 2 and the UI spec mandated a TextButton for it, but neither Phase 2 nor Phase 3 rendered that button in AuthScreen. No call to `signInAnonymously()` exists anywhere in AuthScreen.kt or AuthViewModel.kt.

**Impact:** Gift givers who receive a private registry invite link but don't want to create an account cannot proceed as guests. This affects the core value of the application (seamless gift giver access).

**Known production concern (non-blocking):** AffiliateUrlTransformer uses `PLACEHOLDER_UNIQUE_ID`, `PLACEHOLDER_AFF_CODE`, and `PLACEHOLDER_CAMPAIGN_ID` hardcoded. All affiliate URLs generated are non-functional. This is documented as requiring BuildConfig wiring from local.properties and does not block Phase 3 goal verification — the transform logic is correct and complete.

---

*Verified: 2026-04-06*
*Verifier: Claude (gsd-verifier)*
