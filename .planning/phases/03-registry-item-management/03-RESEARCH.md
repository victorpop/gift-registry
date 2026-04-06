# Phase 03: Registry + Item Management - Research

**Researched:** 2026-04-06
**Domain:** Firestore CRUD (registries/items), Cloud Functions callable (OG metadata), client-side affiliate URL transformation, Navigation3 screen scaffolding
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Firestore Data Access Layer**
- D-01: Items are a Firestore subcollection (`registries/{id}/items/{itemId}`) — matches existing security rules and Phase 1 schema
- D-02: Real-time snapshot listeners for item status updates — Firestore listeners flow through Repository -> ViewModel -> Compose via StateFlow (same pattern as auth state)
- D-03: `firebase-firestore` dependency must be added to Android (not yet in build.gradle.kts)
- D-04: New `FirestoreDataSource` mirroring the existing `FirebaseAuthDataSource` pattern — injected via Hilt

**Affiliate URL Transformation**
- D-05: Client-side utility (`AffiliateUrlTransformer`) injects affiliate tags at item-add time — no Cloud Function call needed since affiliate tag patterns are not secret
- D-06: Both `originalUrl` and `affiliateUrl` stored on item document per Phase 1 schema
- D-07: Unknown merchant URLs pass through without affiliate tag (logged for review per AFF-04)
- D-08: EMAG items receive affiliate tags automatically on add (AFF-02)

**OG Metadata Extraction**
- D-09: Cloud Function callable fetches URL and parses Open Graph metadata (title, image, price) — more reliable than client-side HTML fetching from Android
- D-10: Owner sees auto-filled fields after paste, can edit before saving (ITEM-02 + ITEM-05)
- D-11: Fallback to manual entry if OG extraction fails or returns incomplete data

**Navigation and Screen Structure**
- D-12: Replace `HomeKey` placeholder with registry list screen — this becomes the authenticated home
- D-13: New navigation keys following existing `@Serializable` pattern: registry list, create registry, registry detail, add item, edit item
- D-14: Keys needing parameters (registry ID, item ID) use `data class` instead of `data object`

**Registry Invite Flow**
- D-15: `invitedUsers` map on registry document (already in Phase 1 schema) for O(1) membership check in security rules
- D-16: Invite sends email to all invitees; in-app notification only for users with existing accounts (REG-06 vs REG-07)
- D-17: Email sending via Cloud Function trigger on invite action — actual email delivery implementation may be minimal/stub in Phase 3, full email flows in Phase 6

### Claude's Discretion

- Exact screen layouts and Compose component structure
- Error handling UX patterns (loading, empty states, error states)
- Form validation approach for registry creation/editing
- Internal code organization within domain/data/UI layers
- Whether invite notification is a Firestore trigger or callable function

### Deferred Ideas (OUT OF SCOPE)

- EMAG catalog browsing and search (ITEM-03, ITEM-04) — Phase 7
- Full email delivery implementation for invites — Phase 6 (Phase 3 stubs the trigger)
- Purchase notifications for owners (NOTF-01, NOTF-02) — Phase 6
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REG-01 | Owner can create a registry with name, occasion type, event date/time, location, description | Firestore `registries/` create pattern; domain model + use case |
| REG-02 | Owner can edit all registry details | Firestore update; same data layer as create |
| REG-03 | Owner can delete a registry | Firestore delete; security rules `isOwner` already enforces this |
| REG-04 | Owner can set registry visibility (public / private) | `visibility` field in Firestore schema; security rules already enforce read gating |
| REG-05 | Owner can invite specific users to a private registry via email | `invitedUsers` map + `invites/` subcollection; callable Cloud Function stubs email |
| REG-06 | Invited users with accounts receive in-app notification and email | Firebase Auth user lookup in Cloud Function; in-app notification stub |
| REG-07 | Invited users without accounts receive email only | Cloud Function checks Auth; email-only path for non-users |
| REG-08 | Invited non-users see login/signup/guest options upon accessing the link | Deep link handling; existing auth screen already presents these options |
| REG-09 | Owner can opt in/out of purchase notifications | `notificationsEnabled` field on `users/{uid}` document; DataStore-backed preference |
| REG-10 | Owner can have multiple active registries simultaneously | No special handling; registry list query by `ownerId` naturally supports multiple |
| ITEM-01 | Owner can add an item by pasting any URL | URL input field; calls OG metadata callable then `AffiliateUrlTransformer`; writes item document |
| ITEM-02 | URL import auto-fills title, image, price via Open Graph metadata | Cloud Function callable `fetchOgMetadata`; returns `title`, `imageUrl`, `price` or nulls |
| ITEM-05 | Owner can manually edit item details | Edit item screen; Firestore update on `registries/{id}/items/{itemId}` |
| ITEM-06 | Owner can remove an item from a registry | Firestore delete on item document; security rules `isOwner` enforces |
| ITEM-07 | Items display real-time status (available, reserved, purchased) | Firestore snapshot listener on items subcollection; StateFlow in ViewModel |
| AFF-01 | URL transformer identifies merchant domain and appends correct affiliate tag | `AffiliateUrlTransformer` utility; regex/startsWith on domain; extensible merchant map |
| AFF-02 | EMAG items receive affiliate tags automatically on add | EMAG domain match in `AffiliateUrlTransformer`; 2Performant affiliate URL pattern |
| AFF-03 | Affiliate tag injection is invisible to users | Stored in `affiliateUrl` field; UI shows `title` and `originalUrl` only |
| AFF-04 | Unknown merchant URLs pass through without breaking; logged for review | `AffiliateUrlTransformer` returns original URL for unknown merchants; `affiliateUrl == originalUrl`; log event |
</phase_requirements>

---

## Summary

Phase 3 introduces the core data management features: registry CRUD, item CRUD, real-time status display, affiliate URL transformation, OG metadata extraction, and a private registry invite stub. All locked decisions align cleanly with existing Phase 1/2 patterns — the main work is extending the established 3-layer clean architecture (domain interface → data implementation → Hilt binding) into registry and item domains.

The most technically novel piece is the Cloud Function callable for OG metadata extraction (D-09). The Android client must handle the async call with a loading state while auto-filling form fields, with a graceful fallback to manual entry. The affiliate URL transformer (D-05) is client-side pure Kotlin — simpler than a Cloud Function but must be designed as an extensible merchant map so adding merchants later does not require structural changes.

The invite flow (D-16/D-17) is intentionally minimal in Phase 3: the Cloud Function callable writes to `invitedUsers` map and stubs email delivery. Full email and in-app notification delivery is Phase 6.

**Primary recommendation:** Follow the `FirebaseAuthDataSource` → `AuthRepositoryImpl` → use case → ViewModel → Compose pattern exactly. New `FirestoreDataSource`, `RegistryRepository`, `ItemRepository` all mirror the auth layer structure established in Phase 2.

---

## Standard Stack

### Core (all already in project or locked by decision)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| firebase-firestore | via BoM 34.11.0 | Registry and item persistence | Must add to build.gradle.kts (D-03); main module, not KTX |
| firebase-functions (callable SDK) | via BoM 34.11.0 | OG metadata callable from Android | 2nd gen callable pattern; matches existing Functions scaffold |
| kotlinx-coroutines-core | 1.9.x | Async Firestore operations | Already in project via coroutines-android |
| Kotlin Flow / StateFlow | bundled | Real-time item status from Firestore listener | Established pattern from Phase 2 auth state |
| Hilt | 2.59.2 | DI for FirestoreDataSource and new repositories | KSP annotation processing; already in project |
| Navigation3 1.0.1 | already in project | New registry/item navigation keys | data class keys for parameterized routes (D-14) |
| Material3 | via Compose BOM | Registry and item form UIs | Already in project; use M3 throughout |
| Kotlin Serialization | 2.x | Firestore DTO mapping; nav key serialization | Already in project; @Serializable on nav keys |

### Cloud Functions (backend additions)

| Technology | Version | Purpose |
|------------|---------|---------|
| firebase-functions/v2 | already in functions/src/index.ts | OG metadata callable; invite stub |
| node-html-parser or cheerio | latest | HTML parsing for Open Graph tags in callable |
| firebase-admin | already initialized | Firestore writes from Functions; Auth user lookup for invites |

### Dependencies to Add

**Android `app/build.gradle.kts`:**
```kotlin
// Firebase Firestore (not yet present)
implementation("com.google.firebase:firebase-firestore")

// Firebase Functions callable (for OG metadata)
implementation("com.google.firebase:firebase-functions")
```

**`gradle/libs.versions.toml`:**
```toml
[libraries]
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }
firebase-functions = { group = "com.google.firebase", name = "firebase-functions" }
```

**Cloud Functions `functions/package.json`:**
```bash
npm install node-html-parser
# OR
npm install cheerio
```

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| node-html-parser in Cloud Function | jsdom | node-html-parser is lighter, no DOM environment needed; jsdom adds ~3MB and is overkill for OG tag extraction |
| Client-side `AffiliateUrlTransformer` | Cloud Function | Decision locked as D-05; client-side is acceptable since affiliate tag patterns are not secret |
| Firestore snapshot listener | one-time get + polling | Real-time listeners are Firestore's core strength; one-time get does not satisfy ITEM-07 real-time requirement |

---

## Architecture Patterns

### Recommended Project Structure (new additions for Phase 3)

```
app/src/main/java/com/giftregistry/
├── data/
│   ├── auth/                    # existing — no changes
│   ├── registry/
│   │   ├── FirestoreDataSource.kt       # mirrors FirebaseAuthDataSource
│   │   ├── RegistryRepositoryImpl.kt    # mirrors AuthRepositoryImpl
│   │   └── ItemRepositoryImpl.kt
│   └── model/
│       ├── RegistryDto.kt               # Firestore DTO (@Serializable or plain data class)
│       └── ItemDto.kt
│
├── domain/
│   ├── auth/                    # existing — no changes
│   ├── registry/
│   │   └── RegistryRepository.kt        # interface
│   ├── item/
│   │   └── ItemRepository.kt            # interface
│   ├── model/
│   │   ├── Registry.kt
│   │   └── Item.kt
│   └── usecase/
│       ├── CreateRegistryUseCase.kt
│       ├── UpdateRegistryUseCase.kt
│       ├── DeleteRegistryUseCase.kt
│       ├── ObserveRegistriesUseCase.kt
│       ├── ObserveItemsUseCase.kt
│       ├── AddItemUseCase.kt
│       ├── UpdateItemUseCase.kt
│       ├── DeleteItemUseCase.kt
│       ├── FetchOgMetadataUseCase.kt
│       └── InviteToRegistryUseCase.kt
│
├── di/
│   ├── AppModule.kt             # add FirebaseFirestore + FirebaseFunctions providers
│   └── DataModule.kt            # add RegistryRepository + ItemRepository bindings
│
├── util/
│   └── AffiliateUrlTransformer.kt       # pure Kotlin merchant domain map
│
└── ui/
    ├── registry/
    │   ├── list/
    │   │   ├── RegistryListScreen.kt
    │   │   └── RegistryListViewModel.kt
    │   ├── create/
    │   │   ├── CreateRegistryScreen.kt
    │   │   └── CreateRegistryViewModel.kt
    │   └── detail/
    │       ├── RegistryDetailScreen.kt
    │       └── RegistryDetailViewModel.kt
    ├── item/
    │   ├── add/
    │   │   ├── AddItemScreen.kt
    │   │   └── AddItemViewModel.kt
    │   └── edit/
    │       ├── EditItemScreen.kt
    │       └── EditItemViewModel.kt
    └── navigation/
        ├── AppNavKeys.kt        # add new keys (data class for parameterized)
        └── AppNavigation.kt     # replace HomeKey placeholder; add new entries

functions/src/
├── index.ts                     # existing healthCheck; export new functions
└── registry/
    ├── fetchOgMetadata.ts       # HTTPS callable
    └── inviteToRegistry.ts      # HTTPS callable (stub — email delivery Phase 6)
```

### Pattern 1: FirestoreDataSource (mirroring FirebaseAuthDataSource)

**What:** Single Firestore data source class, `@Singleton`, injected via Hilt. Exposes `Flow<List<T>>` via `callbackFlow` + `addSnapshotListener` + `awaitClose`. Exposes suspend functions for create/update/delete via `.await()`.

**When to use:** All Firestore reads and writes in Phase 3.

**Example:**
```kotlin
// app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun observeRegistries(ownerId: String): Flow<List<RegistryDto>> = callbackFlow {
        val listener = firestore.collection("registries")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toObject(RegistryDto::class.java) } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createRegistry(data: Map<String, Any>): String {
        val ref = firestore.collection("registries").document()
        ref.set(data).await()
        return ref.id
    }
}
```

**Source:** Mirrors `FirebaseAuthDataSource.authStateFlow` pattern established in Phase 2.

### Pattern 2: Navigation3 Parameterized Keys (data class)

**What:** Navigation keys that carry parameters use `data class` with `@Serializable`. Object keys (no params) remain `data object`.

**When to use:** RegistryDetailKey(registryId), AddItemKey(registryId), EditItemKey(registryId, itemId).

**Example:**
```kotlin
// AppNavKeys.kt additions
@Serializable data object AuthKey           // existing
@Serializable data object HomeKey           // existing (becomes RegistryListKey effectively)
@Serializable data object SettingsKey       // existing
@Serializable data object CreateRegistryKey
@Serializable data class RegistryDetailKey(val registryId: String)
@Serializable data class AddItemKey(val registryId: String)
@Serializable data class EditItemKey(val registryId: String, val itemId: String)
```

**AppNavigation.kt entryProvider additions:**
```kotlin
entry<CreateRegistryKey> { CreateRegistryScreen(onBack = { backStack.removeLast() }) }
entry<RegistryDetailKey> { key ->
    RegistryDetailScreen(registryId = key.registryId, onBack = { backStack.removeLast() })
}
entry<AddItemKey> { key ->
    AddItemScreen(registryId = key.registryId, onBack = { backStack.removeLast() })
}
entry<EditItemKey> { key ->
    EditItemScreen(registryId = key.registryId, itemId = key.itemId, onBack = { backStack.removeLast() })
}
```

### Pattern 3: OG Metadata Cloud Function Callable

**What:** 2nd gen HTTPS callable function. Android calls it with a URL string; Function fetches the page, parses Open Graph tags, returns structured metadata. Android shows result in form fields; user can edit before saving.

**When to use:** ITEM-01/ITEM-02 — any time owner pastes a URL.

**Cloud Function (TypeScript):**
```typescript
// functions/src/registry/fetchOgMetadata.ts
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { parse } from "node-html-parser";

export const fetchOgMetadata = onCall(async (request) => {
  const url: string = request.data.url;
  if (!url) throw new HttpsError("invalid-argument", "url is required");

  try {
    const response = await fetch(url, {
      headers: { "User-Agent": "Mozilla/5.0 (compatible; GiftRegistryBot/1.0)" },
      signal: AbortSignal.timeout(5000),
    });
    const html = await response.text();
    const root = parse(html);

    const og = (property: string) =>
      root.querySelector(`meta[property="og:${property}"]`)?.getAttribute("content") ?? null;

    return {
      title: og("title") ?? root.querySelector("title")?.text ?? null,
      imageUrl: og("image"),
      price: og("price:amount") ?? og("product:price:amount") ?? null,
      siteName: og("site_name"),
    };
  } catch (e) {
    // Return nulls on failure — client falls back to manual entry (D-11)
    return { title: null, imageUrl: null, price: null, siteName: null };
  }
});
```

**Android call:**
```kotlin
// FetchOgMetadataUseCase.kt
class FetchOgMetadataUseCase @Inject constructor(
    private val functions: FirebaseFunctions
) {
    suspend operator fun invoke(url: String): Result<OgMetadata> = runCatching {
        val result = functions
            .getHttpsCallable("fetchOgMetadata")
            .call(mapOf("url" to url))
            .await()
        val data = result.data as Map<*, *>
        OgMetadata(
            title = data["title"] as? String,
            imageUrl = data["imageUrl"] as? String,
            price = data["price"] as? String
        )
    }
}
```

### Pattern 4: AffiliateUrlTransformer (client-side)

**What:** Pure Kotlin object/class with a single `transform(url: String): TransformResult` function. Uses a merchant map of domain patterns to affiliate URL builders. Returns both `originalUrl` and `affiliateUrl` (same string when no match).

**When to use:** On every `AddItemUseCase` invocation before writing to Firestore.

**Example:**
```kotlin
// util/AffiliateUrlTransformer.kt
data class TransformResult(
    val originalUrl: String,
    val affiliateUrl: String,
    val merchantName: String?,
    val wasTransformed: Boolean
)

object AffiliateUrlTransformer {
    private val merchantRules: Map<String, (String) -> String> = mapOf(
        "emag.ro" to { url -> buildEmagAffiliateUrl(url) },
        // Future merchants added here without structural changes
    )

    fun transform(url: String): TransformResult {
        val domain = extractDomain(url) ?: return noMatch(url)
        val builder = merchantRules.entries.firstOrNull { domain.endsWith(it.key) }?.value
            ?: return noMatch(url)
        return TransformResult(
            originalUrl = url,
            affiliateUrl = builder(url),
            merchantName = merchantRules.keys.firstOrNull { domain.endsWith(it) },
            wasTransformed = true
        )
    }

    private fun noMatch(url: String) = TransformResult(url, url, null, false)
    // wasTransformed=false signals to callers to log AFF-04 event
}
```

**EMAG 2Performant affiliate URL pattern (MEDIUM confidence — verify affiliate ID with account):**
```kotlin
private fun buildEmagAffiliateUrl(productUrl: String): String {
    // 2Performant tracking URL wraps the product URL
    val encoded = java.net.URLEncoder.encode(productUrl, "UTF-8")
    return "https://event.2performant.com/events/click" +
           "?ad_type=product_store" +
           "&unique=${AFFILIATE_UNIQUE_ID}" +
           "&aff_code=${AFFILIATE_CODE}" +
           "&campaign_unique=${EMAG_CAMPAIGN_ID}" +
           "&redirect_to=$encoded"
}
```

Note: Exact 2Performant parameter names and values must be verified against the affiliate dashboard after account registration.

### Pattern 5: Hilt DI additions

**AppModule.kt — add Firebase instances:**
```kotlin
@Provides @Singleton
fun provideFirebaseFirestore(): FirebaseFirestore =
    FirebaseFirestore.getInstance().also { db ->
        if (BuildConfig.DEBUG) db.useEmulator("10.0.2.2", 8080)
    }

@Provides @Singleton
fun provideFirebaseFunctions(): FirebaseFunctions =
    FirebaseFunctions.getInstance().also { fns ->
        if (BuildConfig.DEBUG) fns.useEmulator("10.0.2.2", 5001)
    }
```

**DataModule.kt — add new bindings:**
```kotlin
@Binds @Singleton
abstract fun bindRegistryRepository(impl: RegistryRepositoryImpl): RegistryRepository

@Binds @Singleton
abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository
```

### Anti-Patterns to Avoid

- **Calling Firestore from ViewModel directly:** Domain layer only — ViewModels call use cases, not repositories.
- **Firebase imports in domain layer:** Domain model classes (`Registry.kt`, `Item.kt`) must be pure Kotlin — no `DocumentSnapshot` or `@DocumentId` annotations there; those belong in DTOs.
- **Hardcoded UI strings:** All `registry_*`, `item_*` prefixed string keys in both `values/strings.xml` and `values-ro/strings.xml` before any screen is implemented.
- **Checking auth.uid in ViewModel:** Security rules enforce ownership — ViewModel passes `currentUser.uid` to use case; the use case delegates to the data layer which the rules protect. Do not re-implement ownership checks in UI.
- **Writing `affiliateUrl` that equals `originalUrl` without a log event:** When `AffiliateUrlTransformer.wasTransformed == false`, the AddItemUseCase must fire a log event (AFF-04). Omitting this makes revenue gaps invisible.
- **Using `set()` with `SetOptions.merge()` for registry create:** Use `set()` without merge to ensure the entire schema is written atomically on creation. Use `update()` for partial edits.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Firestore real-time updates to Compose | Manual polling or WorkManager | `callbackFlow` + `addSnapshotListener` + `awaitClose` | Firestore handles reconnects, offline cache, and diff delivery; polling is unreliable and expensive |
| HTML/OG metadata parsing | Custom HTTP fetch from Android | Cloud Function callable (`fetchOgMetadata`) | Android cannot reliably fetch arbitrary URLs (CORS, cookies, CAPTCHA); server-side fetch is more reliable |
| Affiliate URL injection at purchase time | Transform in reservation Cloud Function | Transform at item-add time, store `affiliateUrl` | Avoids blocking the reservation transaction on URL transformation; pre-computed URL is O(1) at purchase time |
| Custom form validation framework | Validator class hierarchy | Inline validation in ViewModel with `errorMessage` on form state | Existing `AuthViewModel` pattern works well; no extra library needed |
| Image URL downloading in Firestore | Storing binary in Firestore document | Store `imageUrl` string from OG metadata; display with Coil | Firestore documents are limited to 1MB; storing images as URLs is the standard pattern |

**Key insight:** Firestore's real-time listener + `callbackFlow` pattern already handles the hardest part of ITEM-07 (real-time status updates). The pattern is already proven in Phase 2's auth state observation. The same technique applies identically to item collections.

---

## Common Pitfalls

### Pitfall 1: Missing `invitedUsers` field on registry create causes security rule failure

**What goes wrong:** Owner creates a registry without setting `invitedUsers: {}`. Any subsequent query by an authenticated user triggers `registryData.invitedUsers[request.auth.uid]` in the security rule, which throws because the field is absent (not just false). All reads fail with permission-denied.

**Why it happens:** The `invitedUsers` field is only needed for private registries, so developers omit it for public ones. But the security rule's `isInvited()` function accesses the map unconditionally.

**How to avoid:** Always include `invitedUsers: {}` (empty map) in the registry document on creation, even for public registries. The Firestore schema requires this field to be present.

**Warning signs:** Security rule tests passing for owner reads but failing for public unauthenticated reads — the rule falls through to `isInvited()` before reaching `isPublicRegistry()`.

### Pitfall 2: OG metadata callable cold starts during item add

**What goes wrong:** The first `fetchOgMetadata` call after the Functions instance idles takes 2-4 seconds. The owner pasted a URL and is waiting. If the UI has no loading state, the paste field appears unresponsive.

**Why it happens:** Cloud Functions 2nd gen spins down after ~5 minutes of inactivity. The OG callable is called on user action (not on a schedule), so it cold-starts frequently in development and for low-traffic instances.

**How to avoid:** Show a loading indicator immediately when the owner finishes pasting the URL (before the callable returns). The AddItem form should disable the Save button and show a spinner in the auto-fill area. Set minimum instances to 1 for the `fetchOgMetadata` function in production to eliminate cold starts.

**Warning signs:** The add-item UX feels sluggish; no loading state while OG metadata is being fetched.

### Pitfall 3: Firestore items listener leaks when navigating away from RegistryDetailScreen

**What goes wrong:** The `callbackFlow` listener on `registries/{id}/items` continues running after the user navigates back from RegistryDetailScreen. Multiple navigations stack up multiple active listeners on the same collection. Firestore read counts and memory usage grow unbounded.

**Why it happens:** The listener is started in `init {}` of the ViewModel via `viewModelScope.launch`. If the ViewModel outlives the screen (shared Activity ViewModel scope), the listener is never cancelled.

**How to avoid:** Use `@HiltViewModel` (screen-scoped ViewModel, not Activity-scoped). Navigation3 + `rememberSaveableStateHolderNavEntryDecorator` scopes the ViewModel to the nav entry — when the entry is popped, the ViewModel is cleared and the `viewModelScope` is cancelled, which also cancels the `callbackFlow`. Verify this is the pattern by checking `AuthViewModel` — it uses `hiltViewModel()` at the screen entry level, not at `AppNavigation` level.

**Warning signs:** Using `hiltViewModel()` inside the `AppNavigation()` composable at the top level (Activity scope) rather than inside the `entry<RegistryDetailKey>` block.

### Pitfall 4: Firestore get() inside items security rule causes cross-document read charges

**What goes wrong:** The items subcollection rule performs `get(/databases/$(database)/documents/registries/$(registryId))` on every item read to check registry ownership. For a real-time listener on 20 items, each document change re-evaluates the rule — 20 cross-document reads per update cycle.

**Why it happens:** Firestore security rules with `get()` calls perform actual reads counted against the billing quota. The existing rule in `firestore.rules` already uses this pattern, so it's a known cost — not a bug.

**How to avoid:** This is acceptable for Phase 3 (low traffic). Document it so future phases do not add additional `get()` calls in rules without consideration. For high-traffic optimization (Phase 7+), denormalize `ownerId` onto each item document so rules can check it without a parent read.

**Warning signs:** Unexpectedly high Firestore read counts in Firebase Console during development — each item listener re-evaluation counts as one document read for the parent registry lookup.

### Pitfall 5: `AffiliateUrlTransformer` hardcodes affiliate IDs in APK

**What goes wrong:** The 2Performant `AFFILIATE_UNIQUE_ID` and `AFFILIATE_CODE` are constants in `AffiliateUrlTransformer.kt`. They are compiled into the APK. Anyone who decompiles the APK can extract them and use them to redirect affiliate commissions.

**Why it happens:** D-05 says affiliate tag patterns are "not secret" — but affiliate account credentials (the specific IDs) are sensitive. The *pattern* of URL transformation is not secret; the *account credentials* embedded in the URL are.

**How to avoid:** The affiliate URL builder function is in the client, but the affiliate IDs should be stored as build-time constants from a non-committed config file (e.g., `local.properties` or a `BuildConfig` field sourced from a Gradle property). This is not the same as a runtime secret, but it prevents trivial APK extraction of the IDs. Alternatively, accept the risk for Phase 3 (affiliate ID is low-sensitivity) and document as a known limitation.

**Warning signs:** `AFFILIATE_UNIQUE_ID = "your-id-here"` as a Kotlin `const val` in a source file committed to git.

### Pitfall 6: Security rules not tested for invite flow (REG-05 to REG-08)

**What goes wrong:** The invite callable adds a UID to `invitedUsers` map. If the rule test suite is not extended for the invite path (invited UID can read private registry; non-invited UID cannot), a regression in firestore.rules can silently break private registry access.

**Why it happens:** Existing 12 tests in `firestore.rules.test.ts` cover basic private/public scenarios. The invite flow (updating `invitedUsers` via Cloud Function, then verifying read access) is not yet tested.

**How to avoid:** Extend `firestore.rules.test.ts` in Phase 3 with:
- Owner can update `invitedUsers` map on private registry
- Newly invited UID can read private registry after update
- Removed UID cannot read private registry after removal

---

## Code Examples

Verified patterns from existing project code and established Firebase conventions:

### Firestore DTO Mapping

```kotlin
// data/model/RegistryDto.kt
// Use plain data class with @JvmField or no-arg constructor for Firestore deserialization
data class RegistryDto(
    val id: String = "",            // populated after fetch via documentId
    val ownerId: String = "",
    val title: String = "",
    val occasion: String = "",
    val visibility: String = "public",
    val eventDateMs: Long? = null,
    val eventLocation: String? = null,
    val description: String? = null,
    val locale: String = "en",
    val notificationsEnabled: Boolean = true,
    val invitedUsers: Map<String, Boolean> = emptyMap()
) {
    // Required by Firestore deserializer (no-arg constructor satisfied by default values)
}
```

### Firestore snapshot listener with callbackFlow

```kotlin
// Source: mirrors FirebaseAuthDataSource.authStateFlow (Phase 2)
fun observeItems(registryId: String): Flow<List<ItemDto>> = callbackFlow {
    val listener = firestore
        .collection("registries").document(registryId)
        .collection("items")
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ItemDto::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items)
        }
    awaitClose { listener.remove() }
}
```

### runCatching in RepositoryImpl

```kotlin
// Source: mirrors AuthRepositoryImpl pattern (Phase 2)
override suspend fun createRegistry(registry: Registry): Result<String> =
    runCatching { dataSource.createRegistry(registry.toDto().toMap()) }

override suspend fun deleteItem(registryId: String, itemId: String): Result<Unit> =
    runCatching { dataSource.deleteItem(registryId, itemId) }
```

### Navigation3 entry with parameterized key

```kotlin
// Source: extends AppNavigation.kt entryProvider block
entry<RegistryDetailKey> { key ->
    RegistryDetailScreen(
        registryId = key.registryId,
        onNavigateToAddItem = { backStack.add(AddItemKey(key.registryId)) },
        onBack = { backStack.removeLast() }
    )
}
```

### Cloud Function 2nd gen callable registration

```typescript
// functions/src/index.ts
import { fetchOgMetadata } from "./registry/fetchOgMetadata";
import { inviteToRegistry } from "./registry/inviteToRegistry";

export { healthCheck, fetchOgMetadata, inviteToRegistry };
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Firebase KTX modules (`firebase-firestore-ktx`) | Main module (`firebase-firestore`) with built-in Kotlin APIs | BoM 34.0.0 (July 2025) | Import `com.google.firebase:firebase-firestore` only; KTX module removed |
| `DocumentSnapshot.getString()` manual mapping | `toObject(MyClass::class.java)` with Kotlin data class defaults | Firestore SDK ~20.x | Less boilerplate; requires no-arg constructor (default values satisfy this) |
| Firebase Dynamic Links for sharing | Android App Links + Firebase Hosting static fallback | Dynamic Links deprecated Aug 2025 | Shareable registry links use `https://` URLs; handled in Phase 5 |
| Cloud Functions 1st gen | Cloud Functions 2nd gen (`firebase-functions/v2`) | 2023, mandatory by 2025 | Already using v2 import in `functions/src/index.ts` |

**Deprecated/outdated:**
- `firebase-functions/v1` import: replaced by `firebase-functions/v2` — already correct in project.
- `DocumentSnapshot.data` direct Map access: use `toObject()` with typed DTOs — less error-prone.
- KAPT annotation processing: replaced by KSP 2.3.6 in Phase 2 — already correct in project.

---

## Open Questions

1. **2Performant affiliate URL exact parameters**
   - What we know: 2Performant (which acquired ProfitShare) handles EMAG affiliate links; URL structure involves `event.2performant.com/events/click` with `ad_type`, `unique`, `aff_code`, `campaign_unique` parameters.
   - What's unclear: The exact parameter names and values require a registered affiliate account. The `AFFILIATE_UNIQUE_ID`, `AFFILIATE_CODE`, and `EMAG_CAMPAIGN_ID` cannot be confirmed without account access.
   - Recommendation: Implement `AffiliateUrlTransformer` with placeholder constants that are configurable via `BuildConfig` fields. The structure is correct; the values are account-specific. Phase 3 can run against the emulator with placeholder values.

2. **`fetchOgMetadata` function fetch() availability in Node 22**
   - What we know: Node.js 22 has native `fetch()` (stable as of Node 21). Cloud Functions 2nd gen on Node 22 should have it available.
   - What's unclear: Whether Firebase's Node 22 runtime includes the `--experimental-fetch` flag or has it enabled by default.
   - Recommendation: Use native `fetch()` in the Cloud Function; if it fails in emulator testing, fall back to `node-fetch` package. Node 22 has it built-in as stable.

3. **OG metadata extraction reliability for common Romanian retailers**
   - What we know: Open Graph tags are a web standard; most major retailers implement them for social sharing.
   - What's unclear: EMAG's OG tag structure (specifically whether `og:price:amount` or a custom meta tag is used for price). This can only be verified by fetching an actual EMAG product page.
   - Recommendation: Test the `fetchOgMetadata` callable against 2-3 real EMAG product URLs during Wave 0 setup. The `price` field is best-effort — the fallback to manual entry (D-11) handles missing price gracefully.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Cloud Functions emulator | Yes | v22.14.0 | — |
| Firebase CLI | Functions deploy + emulator | Yes | 15.13.0 | — |
| Firebase Emulator Suite | Firestore + Functions local dev | Yes (configured in firebase.json) | via CLI 15.13.0 | — |
| Android SDK / AGP 8.13.0 | Android build | Assumed present (Phase 2 completed) | 8.13.0 | — |
| `firebase-firestore` BoM module | D-03: Firestore on Android | Not yet added to build.gradle.kts | via BoM 34.11.0 | — |
| `firebase-functions` BoM module | OG metadata callable from Android | Not yet added to build.gradle.kts | via BoM 34.11.0 | — |
| `node-html-parser` npm package | `fetchOgMetadata` Cloud Function | Not yet installed in functions/ | latest | `cheerio` (heavier) |

**Missing dependencies with no fallback:**
- `firebase-firestore` Android dependency — must be added before any Firestore code compiles (Wave 0 build task)
- `firebase-functions` Android dependency — must be added for the OG metadata callable (Wave 0 build task)

**Missing dependencies with fallback:**
- `node-html-parser` — if unavailable, `cheerio` is a viable alternative; both parse HTML for OG tags

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 + MockK 1.13.17 + Turbine 1.2.0 + kotlinx-coroutines-test 1.9.0 |
| Config file | app/build.gradle.kts (testImplementation already present) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest && cd tests/rules && npm test` |
| Firestore rules tests | `cd tests/rules && npm test` (Jest, requires Firestore emulator on :8080) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REG-01 | CreateRegistryUseCase returns registry ID on success | unit | `./gradlew :app:testDebugUnitTest --tests "*.CreateRegistryUseCaseTest"` | Wave 0 |
| REG-02 | UpdateRegistryUseCase updates fields | unit | `./gradlew :app:testDebugUnitTest --tests "*.UpdateRegistryUseCaseTest"` | Wave 0 |
| REG-03 | DeleteRegistryUseCase succeeds for owner | unit | `./gradlew :app:testDebugUnitTest --tests "*.DeleteRegistryUseCaseTest"` | Wave 0 |
| REG-04 | Registry visibility field written correctly | unit | `./gradlew :app:testDebugUnitTest --tests "*.RegistryRepositoryTest"` | Wave 0 |
| REG-05/06/07 | InviteToRegistryUseCase writes invitedUsers map | unit | `./gradlew :app:testDebugUnitTest --tests "*.InviteToRegistryUseCaseTest"` | Wave 0 |
| REG-08 | Deep link to private registry shows auth options | manual | Manual: navigate to private registry link while logged out | N/A |
| REG-09 | notificationsEnabled updated in users document | unit | `./gradlew :app:testDebugUnitTest --tests "*.NotificationPreferenceTest"` | Wave 0 |
| REG-10 | observeRegistries returns all owner registries | unit | `./gradlew :app:testDebugUnitTest --tests "*.ObserveRegistriesUseCaseTest"` | Wave 0 |
| ITEM-01 | AddItemUseCase writes item document with URLs | unit | `./gradlew :app:testDebugUnitTest --tests "*.AddItemUseCaseTest"` | Wave 0 |
| ITEM-02 | FetchOgMetadataUseCase returns OG fields | unit (mock Functions) | `./gradlew :app:testDebugUnitTest --tests "*.FetchOgMetadataUseCaseTest"` | Wave 0 |
| ITEM-05 | UpdateItemUseCase updates item fields | unit | `./gradlew :app:testDebugUnitTest --tests "*.UpdateItemUseCaseTest"` | Wave 0 |
| ITEM-06 | DeleteItemUseCase removes item document | unit | `./gradlew :app:testDebugUnitTest --tests "*.DeleteItemUseCaseTest"` | Wave 0 |
| ITEM-07 | Item status changes propagate via Flow | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests "*.ObserveItemsUseCaseTest"` | Wave 0 |
| AFF-01 | AffiliateUrlTransformer transforms EMAG URL | unit | `./gradlew :app:testDebugUnitTest --tests "*.AffiliateUrlTransformerTest"` | Wave 0 |
| AFF-02 | EMAG URLs receive affiliate tag | unit | included in AffiliateUrlTransformerTest | Wave 0 |
| AFF-03 | affiliateUrl stored; originalUrl also stored | unit | included in AddItemUseCaseTest | Wave 0 |
| AFF-04 | Unknown merchant URL passes through; logged | unit | `./gradlew :app:testDebugUnitTest --tests "*.AffiliateUrlTransformerTest#unknownMerchantPassesThrough"` | Wave 0 |
| Firestore rules - invite path | invitedUsers update grants read access | integration (Jest) | `cd tests/rules && npm test` | Wave 0 (new tests) |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest && cd tests/rules && npm test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/data/registry/FakeRegistryRepository.kt` — fake for unit tests
- [ ] `app/src/test/java/com/giftregistry/data/item/FakeItemRepository.kt` — fake for unit tests
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/CreateRegistryUseCaseTest.kt`
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/ObserveItemsUseCaseTest.kt` — uses Turbine
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCaseTest.kt` — mock `FirebaseFunctions`
- [ ] `app/src/test/java/com/giftregistry/util/AffiliateUrlTransformerTest.kt` — pure Kotlin, no mocking needed
- [ ] `tests/rules/firestore.rules.test.ts` — add 3 new describe blocks for invite flow
- [ ] Framework install: already present (`junit:junit:4.13.2`, `mockk`, `turbine`, `coroutines-test` in build.gradle.kts)

---

## Project Constraints (from CLAUDE.md)

| Directive | Impact on Phase 3 |
|-----------|-------------------|
| Tech stack: Java/Kotlin for Android, Firebase for backend — no other persistence layer | All registry and item data goes to Firestore; no SQLite, no local DB other than DataStore for preferences |
| No hardcoded strings | All `registry_*` and `item_*` string keys must be in `strings.xml` (en) and `strings-ro.xml` (ro) before screens are built |
| Firebase BoM 34.11.0 — use main modules, NOT KTX | `firebase-firestore` and `firebase-functions` (not `-ktx` variants) |
| KSP over KAPT (Phase 2 decision) | Any new annotation-processed code uses KSP |
| AGP 9.x — no separate kotlin-android plugin | Do not add `kotlin-android` to plugins block |
| Zero Firebase imports in domain layer | `Registry.kt`, `Item.kt`, `RegistryRepository.kt`, `ItemRepository.kt` have no Firestore types |
| `callbackFlow` + `awaitClose` for Firebase listeners | Items snapshot listener follows this exact pattern |
| `runCatching` wrapping Firebase calls in data layer | All `suspend fun` in RepositoryImpl use `runCatching` |
| `SharingStarted.Eagerly` for immediate StateFlow emission | Use in ViewModels that must emit immediately on init |
| Navigation3 `@Serializable` keys pattern | `data class` for parameterized keys; `data object` for static keys |
| No `rememberViewModelStoreNavEntryDecorator` — use Activity `hiltViewModel()` | Already handled; Phase 3 follows same `hiltViewModel()` calls inside entry blocks |
| Affiliate URL transformation in client utility (D-05) | `AffiliateUrlTransformer` is the only place affiliate tag logic lives on Android |
| `invitedUsers` map (not array) for O(1) membership check | Registry document always includes `invitedUsers: {}` even on create |
| reservations collection hard-deny | Do not add any item status write from Android to `reservations/` — that is Phase 4 Cloud Functions only |

---

## Sources

### Primary (HIGH confidence)
- Phase 2 codebase (`FirebaseAuthDataSource.kt`, `AuthRepositoryImpl.kt`, `AppNavKeys.kt`) — direct pattern templates for Phase 3 data layer
- `firestore.rules` in project root — confirmed `invitedUsers` map structure and items subcollection access pattern
- `tests/rules/firestore.rules.test.ts` — 12 existing tests; confirmed Jest framework and `@firebase/rules-unit-testing` setup
- `.planning/research/ARCHITECTURE.md` — Firestore schema, affiliate URL strategy, component responsibilities
- `.planning/phases/03-registry-item-management/03-CONTEXT.md` — all locked decisions D-01 through D-17

### Secondary (MEDIUM confidence)
- Firebase Firestore Android SDK docs — `toObject()`, `addSnapshotListener`, `.await()` patterns
- Cloud Functions v2 `onCall` documentation — callable function pattern for OG metadata
- 2Performant affiliate URL structure — MEDIUM confidence; exact IDs require account verification

### Tertiary (LOW confidence)
- OG metadata tag names for EMAG price field (`og:price:amount` vs `og:product:price:amount`) — needs empirical verification against live EMAG product pages
- Node 22 native `fetch()` availability in Firebase Functions runtime — believed available but not verified against current Firebase Functions Node 22 runtime documentation

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries are already in project or are standard Firebase modules with known patterns
- Architecture: HIGH — all patterns mirror Phase 2 code already in the repository
- Cloud Function OG metadata: MEDIUM — pattern is standard but exact HTML parser choice and OG field names for EMAG need empirical verification
- Affiliate URL: MEDIUM — 2Performant URL structure confirmed but account-specific IDs are placeholders
- Pitfalls: HIGH — based on existing project security rules, established Firebase anti-patterns, and Phase 1/2 decisions

**Research date:** 2026-04-06
**Valid until:** 2026-05-06 (Firebase SDK versions stable; 2Performant affiliate URL structure may change with account setup)
