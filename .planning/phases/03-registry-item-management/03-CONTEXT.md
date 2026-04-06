# Phase 3: Registry + Item Management - Context

**Gathered:** 2026-04-06 (assumptions mode)
**Status:** Ready for planning

<domain>
## Phase Boundary

Registry owners can create and manage registries for any occasion, add items via any URL with automatic affiliate tag injection, and gift givers see real-time item status. This phase covers REG-01 through REG-10, ITEM-01/02/05/06/07, and AFF-01 through AFF-04. EMAG catalog browsing (ITEM-03, ITEM-04) is deferred to Phase 7. Reservation logic (RES-*) is Phase 4. Notification delivery (NOTF-*) is Phase 6.

</domain>

<decisions>
## Implementation Decisions

### Firestore Data Access Layer
- **D-01:** Items are a Firestore subcollection (`registries/{id}/items/{itemId}`) — matches existing security rules and Phase 1 schema
- **D-02:** Real-time snapshot listeners for item status updates — Firestore listeners flow through Repository -> ViewModel -> Compose via StateFlow (same pattern as auth state)
- **D-03:** `firebase-firestore` dependency must be added to Android (not yet in build.gradle.kts)
- **D-04:** New `FirestoreDataSource` mirroring the existing `FirebaseAuthDataSource` pattern — injected via Hilt

### Affiliate URL Transformation
- **D-05:** Client-side utility (`AffiliateUrlTransformer`) injects affiliate tags at item-add time — no Cloud Function call needed since affiliate tag patterns are not secret
- **D-06:** Both `originalUrl` and `affiliateUrl` stored on item document per Phase 1 schema
- **D-07:** Unknown merchant URLs pass through without affiliate tag (logged for review per AFF-04)
- **D-08:** EMAG items receive affiliate tags automatically on add (AFF-02)

### OG Metadata Extraction
- **D-09:** Cloud Function callable fetches URL and parses Open Graph metadata (title, image, price) — more reliable than client-side HTML fetching from Android
- **D-10:** Owner sees auto-filled fields after paste, can edit before saving (ITEM-02 + ITEM-05)
- **D-11:** Fallback to manual entry if OG extraction fails or returns incomplete data

### Navigation and Screen Structure
- **D-12:** Replace `HomeKey` placeholder with registry list screen — this becomes the authenticated home
- **D-13:** New navigation keys following existing `@Serializable` pattern: registry list, create registry, registry detail, add item, edit item
- **D-14:** Keys needing parameters (registry ID, item ID) use `data class` instead of `data object`

### Registry Invite Flow (REG-05 through REG-08)
- **D-15:** `invitedUsers` map on registry document (already in Phase 1 schema) for O(1) membership check in security rules
- **D-16:** Invite sends email to all invitees; in-app notification only for users with existing accounts (REG-06 vs REG-07)
- **D-17:** Email sending via Cloud Function trigger on invite action — actual email delivery implementation may be minimal/stub in Phase 3, full email flows in Phase 6

### Claude's Discretion
- Exact screen layouts and Compose component structure
- Error handling UX patterns (loading, empty states, error states)
- Form validation approach for registry creation/editing
- Internal code organization within domain/data/UI layers
- Whether invite notification is a Firestore trigger or callable function

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Firestore schema and security rules
- `firestore.rules` — Security rules for registries, items subcollection, reservations; defines access patterns
- `tests/rules/firestore.rules.test.ts` — 12 existing rule tests that must not break
- `.planning/research/ARCHITECTURE.md` — Firestore schema with field definitions for registries and items collections

### Existing app architecture
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — Navigation3 setup, HomeKey placeholder to replace
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — Serializable nav key pattern
- `app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt` — Firebase data source pattern to mirror for Firestore
- `app/src/main/java/com/giftregistry/data/auth/AuthRepositoryImpl.kt` — Repository implementation pattern
- `app/src/main/java/com/giftregistry/di/` — Existing Hilt DI module structure

### Cloud Functions
- `functions/src/index.ts` — Existing scaffold with firebase-functions/v2, ready for OG metadata callable

### Build configuration
- `app/build.gradle.kts` — Dependencies list (firebase-firestore needs adding)
- `gradle/libs.versions.toml` — Version catalog

### Research findings
- `.planning/research/ARCHITECTURE.md` — Schema patterns, affiliate URL transformation strategy, Cloud Functions responsibilities
- `.planning/research/PITFALLS.md` — Hot document limits, security rules as sole access boundary
- `.planning/research/STACK.md` — Recommended libraries and versions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FirebaseAuthDataSource` — Pattern template for new `FirestoreDataSource`
- `AuthRepositoryImpl` — Repository pattern with `runCatching` for Firebase calls
- `AuthViewModel` — ViewModel + StateFlow + `collectAsStateWithLifecycle()` pattern
- `AppNavigation.kt` — Navigation3 entryProvider pattern to extend
- `strings.xml` / `strings-ro.xml` — Feature-namespaced string key convention (`registry_`, `item_` prefixes)
- `GiftRegistryTheme` — Material3 theme with established color scheme

### Established Patterns
- 3-layer clean architecture: domain interfaces -> data implementations -> DI bindings
- `callbackFlow` with `awaitClose` for Firebase listener bridging
- `runCatching` wrapping Firebase SDK calls in data layer
- KSP for annotation processing (not KAPT)
- No Hilt Gradle plugin — explicit `Hilt_*` base class pattern for `@AndroidEntryPoint`
- Navigation3 `@Serializable` keys with `entryProvider` and `NavDisplay`
- `SharingStarted.Eagerly` for immediate StateFlow emission in ViewModels

### Integration Points
- `HomeKey` entry in AppNavigation.kt (line 79-103) — replace placeholder with registry list
- Firestore emulator on port 8080 for local development
- Cloud Functions emulator on port 5001 for OG metadata callable
- Auth state from `AuthViewModel` — registry screens only shown when `Authenticated`

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches.

</specifics>

<deferred>
## Deferred Ideas

- EMAG catalog browsing and search (ITEM-03, ITEM-04) — Phase 7
- Full email delivery implementation for invites — Phase 6 (Phase 3 stubs the trigger)
- Purchase notifications for owners (NOTF-01, NOTF-02) — Phase 6

</deferred>

---

*Phase: 03-registry-item-management*
*Context gathered: 2026-04-06*
