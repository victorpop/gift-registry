# Phase 4: Reservation System - Context

**Gathered:** 2026-04-11 (assumptions mode, autonomous)
**Status:** Ready for planning

<domain>
## Phase Boundary

Gift givers can reliably reserve an available item via a server-authoritative 30-minute timer with race-condition prevention and automatic release on expiry. Covers RES-01 through RES-09. Email delivery is stubbed (Phase 6 wires the full SendGrid integration). Web fallback reservation is deferred to Phase 5.

</domain>

<decisions>
## Implementation Decisions

### Reservation Write Mechanism (Race Condition Prevention)
- **D-01:** Introduce a `createReservation` callable Cloud Function (firebase-functions v2, `europe-west3`, Node 22) that runs a Firestore `runTransaction` on `registries/{registryId}/items/{itemId}`, aborting with `HttpsError("failed-precondition", "ITEM_UNAVAILABLE")` if status is not `available`
- **D-02:** Within the same transaction: update item status to `reserved` + set `reservedBy`/`reservedAt`/`expiresAt` fields, AND create a new doc in top-level `reservations/{reservationId}` collection
- **D-03:** Clients never write reservations directly — the existing `firestore.rules` hard-deny on the reservations collection is preserved
- **D-04:** Reservation document schema: `{ itemId, registryId, giverId, giverName, giverEmail, status, createdAt, expiresAt, cloudTaskName, affiliateUrl }`

### Timer / Expiry (Cloud Tasks)
- **D-05:** `createReservation` enqueues a Cloud Task via `@google-cloud/tasks` targeting a `releaseReservation` task-queue function scheduled at `now + 30 minutes`
- **D-06:** Store the returned `cloudTaskName` on the reservation document for cancellation (owner marks purchased in later phases)
- **D-07:** `releaseReservation` re-runs a transaction that reverts item status to `available` and clears reservation fields only if reservation is still `active` and `now > expiresAt` (guards against double-release)
- **D-08:** No client-side timer authority — Android displays a countdown UI but server-side expiry is the single source of truth
- **D-09:** Use `firebase-functions` v2 `onTaskDispatched` helper for the queue handler (no new SDK unless needed)

### Expiration Email + Re-Reserve Flow
- **D-10:** Stub the expiration email in Phase 4 (console.log + `[STUB] Email would be sent to...`) — mirrors the Phase 3 `inviteToRegistry` precedent. Full SendGrid/Trigger Email wiring deferred to Phase 6 per PROJECT.md phase allocation
- **D-11:** Re-reserve flow calls the same `createReservation` callable with the same transactional check — no shortcut authorization. Rejection returns a localized "no longer available" error
- **D-12:** Email deep link format: `https://giftregistry.app/reservation/{reservationId}/re-reserve` — routes via existing Android App Link handling

### UI State, Real-Time Updates & Guest Identity
- **D-13:** Add `ReservationRepository` + `ReserveItemUseCase` in domain/data layers following the existing `ItemRepository`/`RegistryRepository` pattern
- **D-14:** `RegistryDetailScreen` is the host for the "Reserve" button on item cards — visible only when `status == AVAILABLE`. The existing `FirestoreDataSource.observeItems` callbackFlow propagates status changes in real time (no new listener infrastructure)
- **D-15:** On successful reservation, launch Intent to `item.affiliateUrl` (RES-04) AFTER the callable returns success — never before
- **D-16:** Guest giver identity persisted via new `GuestPreferencesDataStore` mirroring `LanguagePreferencesDataStore` — name + email stored so process death doesn't orphan reservations
- **D-17:** Guest identity sent in the callable request payload and stored on the reservation doc (for email address lookup at expiry time)
- **D-18:** On the reserved item card, display a countdown showing remaining minutes until expiry (display-only, re-computed from `expiresAt` field on each recomposition)

### Security Rules Tests
- **D-19:** Add tests to `tests/rules/firestore.rules.test.ts` confirming:
  - Client cannot write to `reservations/` directly (existing hard-deny preserved)
  - Items collection allows `status` field read by anyone (for RES-02/RES-06 real-time visibility)
  - Items collection prevents client-side direct write to `status` field (only Cloud Function Admin SDK can update)

### Claude's Discretion
- Exact Compose layout for reservation countdown + button states
- Error UX (snackbar vs dialog) for conflict/network errors
- Cloud Task queue configuration (min/max instances, dispatch deadline) — planner selects based on researcher findings
- Whether to expose a "my active reservations" screen in Phase 4 or defer to future polish

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Firebase backend & security
- `firestore.rules` — Hard-deny on reservations collection, item status access rules
- `tests/rules/firestore.rules.test.ts` — Existing 15+ tests to extend, not break
- `.planning/research/ARCHITECTURE.md` lines 170-196 — createReservation/releaseReservation flow with field schema
- `.planning/research/ARCHITECTURE.md` lines 385-405 — Reservation document schema including `cloudTaskName`
- `.planning/research/PITFALLS.md` Pitfall 1 — Reservation race condition transaction guard pattern
- `.planning/research/PITFALLS.md` Pitfall 2 — Client-authoritative expiry prohibited
- `.planning/research/PITFALLS.md` Pitfall 3 — Re-reserve must go through same transaction
- `.planning/research/PITFALLS.md` Pitfall 7 — Guest identity persistence

### Existing Cloud Functions patterns
- `functions/src/index.ts` — Function exports, region config
- `functions/src/registry/inviteToRegistry.ts` — onCall + region + HttpsError + stub email precedent
- `functions/src/registry/fetchOgMetadata.ts` — onCall response pattern

### Existing data layer patterns (Phase 3)
- `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` — callbackFlow + awaitClose real-time listener pattern
- `app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt` — runCatching + Firebase Functions callable integration
- `app/src/main/java/com/giftregistry/data/model/ItemDto.kt` — DTO pattern with default values

### Existing domain models
- `app/src/main/java/com/giftregistry/domain/model/Item.kt` — Has `affiliateUrl`, `originalUrl` fields
- `app/src/main/java/com/giftregistry/domain/model/ItemStatus.kt` — AVAILABLE/RESERVED/PURCHASED enum already defined
- `app/src/main/java/com/giftregistry/domain/model/GuestUser.kt` — Guest identity data class

### Existing UI hosts
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — Natural host for Reserve button on item cards
- `app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt` — DataStore pattern to mirror for `GuestPreferencesDataStore`

### Project docs
- `.planning/PROJECT.md` — Core value: "reservation-to-purchase flow must be seamless and trustworthy"
- `.planning/REQUIREMENTS.md` — RES-01 through RES-09 definitions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FirestoreDataSource.observeItems` — Already provides real-time item status listener (RES-02/RES-06 require no new infrastructure, just UI binding)
- `ItemStatus` enum — AVAILABLE/RESERVED/PURCHASED states already defined
- `GuestUser` data class — Guest identity model exists
- `LanguagePreferencesDataStore` — Pattern to mirror for `GuestPreferencesDataStore`
- `InviteViewModel` — Callable function invocation pattern from Phase 3
- `inviteToRegistry.ts` — Stub-email + onCall + europe-west3 precedent
- `AffiliateUrlTransformer` — Phase 3 utility; Phase 4 uses its output but doesn't modify it

### Established Patterns
- Firebase Functions v2 `onCall` with `europe-west3` region
- `admin.firestore()` + `runTransaction` for atomic writes
- `HttpsError` with machine-readable codes for client-facing errors
- Kotlin `callbackFlow { awaitClose { } }` for Firebase listener bridging
- `runCatching` wrapping Firebase SDK calls in data layer
- DataStore-backed preferences for local-only state
- 3-layer clean architecture (domain interfaces → data implementations → Hilt bindings)

### Integration Points
- `RegistryDetailScreen` — Add Reserve button + countdown per item card
- `AppNavigation.kt` — May need new route for re-reserve deep link landing (TBD by planner)
- `firestore.rules` — Preserve hard-deny on reservations; add clarifying comment
- `functions/src/index.ts` — Export new `createReservation` and `releaseReservation` functions
- `functions/package.json` — Add `@google-cloud/tasks` dependency if not using v2 helper alone

</code_context>

<specifics>
## Specific Ideas

- Stub email delivery for Phase 4 matching Phase 3 invite flow — unblocks full SendGrid wiring in Phase 6
- Guest persistence via DataStore (not Firestore) — local-only per established pattern

</specifics>

<deferred>
## Deferred Ideas

- Full SendGrid email delivery — Phase 6
- "My active reservations" dashboard screen for givers — future polish, not in v1 scope
- Owner notification on reservation (separate from purchase notification) — Phase 6
- Web fallback reservation flow — Phase 5

</deferred>

---

*Phase: 04-reservation-system*
*Context gathered: 2026-04-11 via assumptions mode --auto*
