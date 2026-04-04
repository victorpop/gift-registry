# Architecture Research

**Domain:** Gift Registry — Android app with Firebase backend and web fallback
**Researched:** 2026-04-04
**Confidence:** HIGH (Firebase patterns well-documented; EMAG-specific integration is MEDIUM)

## Standard Architecture

### System Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                                   │
│                                                                        │
│  ┌──────────────────────────┐    ┌──────────────────────────────┐     │
│  │   Android App (Kotlin)   │    │   Web App (gift givers only) │     │
│  │                          │    │                              │     │
│  │  Presentation (Compose)  │    │  HTML + JS (static hosting)  │     │
│  │  ViewModel + StateFlow   │    │  No framework required       │     │
│  │  Repository interfaces   │    │  Firebase JS SDK             │     │
│  └────────────┬─────────────┘    └──────────────┬───────────────┘     │
│               │                                  │                     │
└───────────────┼──────────────────────────────────┼─────────────────────┘
                │  Firebase SDK (Android + JS)      │
                ▼                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        FIREBASE BACKEND                               │
│                                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                │
│  │ Firebase     │  │  Firestore   │  │  Cloud       │                │
│  │ Auth         │  │  (primary    │  │  Functions   │                │
│  │              │  │   datastore) │  │              │                │
│  │ Email/pass   │  │              │  │  Reservation │                │
│  │ Guest tokens │  │  registries/ │  │  expiry task │                │
│  └──────────────┘  │  items/      │  │  Affiliate   │                │
│                    │  reservations│  │  URL inject  │                │
│  ┌──────────────┐  │  users/      │  │  Email notif │                │
│  │ Firebase     │  │  invites/    │  └──────┬───────┘                │
│  │ Hosting      │  └──────────────┘         │                        │
│  │              │                            │                        │
│  │ Web fallback │  ┌──────────────┐  ┌───────▼──────┐                │
│  │ static files │  │  Firebase    │  │  Cloud Tasks │                │
│  └──────────────┘  │  Storage     │  │  (scheduled  │                │
│                    │  (item images│  │   expiry)    │                │
│                    │   optional)  │  └──────────────┘                │
│                    └──────────────┘                                   │
└──────────────────────────────────────────────────────────────────────┘
                                │
┌───────────────────────────────┼──────────────────────────────────────┐
│                   EXTERNAL SERVICES                                   │
│                               │                                       │
│  ┌──────────────────┐  ┌──────▼───────────┐                          │
│  │  EMAG Public API │  │  Email Provider  │                          │
│  │                  │  │  (Firebase Email │                          │
│  │  Product catalog │  │   or SendGrid)   │                          │
│  │  search/browse   │  │                  │                          │
│  └──────────────────┘  └──────────────────┘                          │
└──────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| Android Presentation | Screen UI, user input, navigation | Jetpack Compose + Navigation Component |
| ViewModel | UI state management, business logic coordination | AndroidX ViewModel + StateFlow/LiveData |
| Repository | Data access abstraction, source-of-truth coordination | Interface in domain layer, impl in data layer |
| Firebase Auth | User identity (registered users + guest tokens) | Email/password; anonymous auth for guests |
| Firestore | Primary datastore — registries, items, reservations | Collections: registries, items, reservations, users, invites |
| Cloud Functions | Server-side logic that must not run on client | Reservation expiry, affiliate URL injection, invite emails |
| Cloud Tasks | Delayed task scheduling (30-min reservation expiry trigger) | Enqueued on reservation creation, released on expiry |
| Firebase Hosting | Static web fallback for gift givers | HTML/JS served at a domain; Firebase JS SDK |
| Android App Links | Deep links from shareable URLs into app (if installed) | `assetlinks.json` verification; HTTP URLs fall through to web |
| EMAG API | Product catalog search and item metadata | REST calls from Android; not called from Cloud Functions |
| URL Transformer | Affiliate tag injection into retailer product URLs | Cloud Function or util library with merchant-specific rules |

## Recommended Project Structure

### Android App

```
app/
├── src/main/
│   ├── java/ro/yourapp/giftregistry/
│   │   ├── data/
│   │   │   ├── repository/          # Repository implementations
│   │   │   │   ├── RegistryRepositoryImpl.kt
│   │   │   │   ├── ItemRepositoryImpl.kt
│   │   │   │   └── ReservationRepositoryImpl.kt
│   │   │   ├── remote/
│   │   │   │   ├── firestore/       # Firestore data sources
│   │   │   │   └── emag/            # EMAG API service + DTOs
│   │   │   └── model/               # Data layer models (Firestore DTOs)
│   │   │
│   │   ├── domain/
│   │   │   ├── model/               # Domain models (pure Kotlin, no Android deps)
│   │   │   ├── repository/          # Repository interfaces
│   │   │   └── usecase/             # Use cases (one class per action)
│   │   │       ├── CreateRegistryUseCase.kt
│   │   │       ├── ReserveItemUseCase.kt
│   │   │       └── AddItemFromUrlUseCase.kt
│   │   │
│   │   ├── presentation/
│   │   │   ├── registry/            # Registry creation/management screens
│   │   │   ├── catalog/             # EMAG catalog browse screen
│   │   │   ├── giver/               # Gift giver view (registry + reservation)
│   │   │   ├── auth/                # Login, register, guest flow
│   │   │   └── settings/            # Language, notifications
│   │   │
│   │   ├── di/                      # Hilt dependency injection modules
│   │   └── util/
│   │       ├── AffiliateUrlTransformer.kt
│   │       └── LocaleHelper.kt
│   │
│   └── res/
│       ├── values/strings.xml       # English strings
│       └── values-ro/strings.xml    # Romanian strings
```

### Firebase Cloud Functions

```
functions/
├── src/
│   ├── reservation/
│   │   ├── createReservation.ts     # Atomic reserve with Cloud Task enqueue
│   │   ├── releaseReservation.ts    # Cloud Task target — auto-release on expiry
│   │   └── cancelReservation.ts    # Manual cancel (giver action)
│   ├── url/
│   │   └── transformAffiliateUrl.ts # Merchant detection + tag injection
│   ├── invite/
│   │   └── sendInviteEmail.ts       # Invite emails for private registries
│   └── notification/
│       └── sendExpiryEmail.ts       # Notify giver when reservation lapses
├── package.json
└── tsconfig.json
```

### Web Fallback (Firebase Hosting)

```
web/
├── public/
│   ├── index.html                   # Entry point (no SPA framework needed)
│   ├── registry.html                # Registry view for gift givers
│   ├── reserve.html                 # Reservation confirmation page
│   ├── js/
│   │   ├── firebase-init.js
│   │   ├── registry.js
│   │   └── reservation.js
│   └── css/
│       └── main.css
├── i18n/
│   ├── en.json                      # English strings
│   └── ro.json                      # Romanian strings
└── firebase.json                    # Hosting + rewrite rules
```

### Structure Rationale

- **domain/:** Pure Kotlin, zero Android/Firebase imports — independently testable, not coupled to Firestore
- **data/repository/:** Firestore-specific implementations hidden behind interfaces; swappable if backend changes
- **data/remote/emag/:** EMAG API isolated here; nowhere else in the app talks to EMAG directly
- **util/AffiliateUrlTransformer.kt:** URL mutation logic isolated — merchant rules are a change hotspot
- **functions/reservation/:** Server-side reservation logic separated from URL and email concerns — each file has one job
- **web/:** Intentionally thin — no React/Vue framework. Gift givers just need to view, reserve, and click out.

## Architectural Patterns

### Pattern 1: Atomic Reservation with Cloud Tasks Expiry

**What:** Reservation creation is a single Firestore transaction that simultaneously sets item status to `reserved` and enqueues a Cloud Task to fire at `now + 30 minutes`. The task's job is to check whether the reservation is still active and release the item if so.

**When to use:** Any time a resource must be held temporarily with guaranteed release, regardless of client connectivity.

**Trade-offs:** Cloud Tasks adds complexity and cost. The alternative (client-side timer) is unreliable — the app can be killed, backgrounded, or go offline.

**Flow:**
```
Giver taps "Reserve"
    ↓
Cloud Function: createReservation()
    ↓ (Firestore transaction)
    → items/{itemId}.status = "reserved"
    → reservations/{id} = { giverId, expiresAt: now+30m, status: "active" }
    ↓ (Cloud Tasks enqueue)
    → scheduleTime: now + 30 minutes
    → target: releaseReservation({ reservationId })
    ↓
Giver redirected to retailer URL (with affiliate tag)

--- 30 minutes later ---
Cloud Task fires: releaseReservation()
    → if reservations/{id}.status == "active":
        → items/{itemId}.status = "available"
        → reservations/{id}.status = "expired"
        → sendExpiryEmail(giver)
```

**Confidence:** HIGH — Firebase Cloud Tasks scheduleTime is supported for this exact use case. See [official docs](https://firebase.google.com/docs/functions/task-functions).

### Pattern 2: Repository + Use Case (Clean Architecture)

**What:** Data access is abstracted behind repository interfaces defined in the domain layer. ViewModels call use cases, use cases call repositories. Firebase-specific code lives only in `data/`.

**When to use:** Always on this project — the team will test ViewModels and use cases independently of Firestore.

**Trade-offs:** More files and indirection than needed for a simple CRUD app. Justified here because the reservation logic, URL transformation, and auth flows are genuinely complex enough to benefit from testable isolation.

**Example:**
```kotlin
// domain/usecase/ReserveItemUseCase.kt
class ReserveItemUseCase(
    private val reservationRepository: ReservationRepository
) {
    suspend operator fun invoke(itemId: String, giver: GiverInfo): Result<Reservation> =
        reservationRepository.reserve(itemId, giver)
}

// presentation/giver/GiverViewModel.kt
class GiverViewModel(private val reserveItem: ReserveItemUseCase) : ViewModel() {
    fun onReserve(itemId: String, giver: GiverInfo) {
        viewModelScope.launch {
            _uiState.value = reserveItem(itemId, giver).fold(
                onSuccess = { UiState.Redirecting(it.affiliateUrl) },
                onFailure = { UiState.Error(it.message) }
            )
        }
    }
}
```

### Pattern 3: Android App Links for Deep Link / Web Fallback

**What:** Shareable registry links are standard HTTPS URLs (e.g. `https://yourapp.ro/registry/abc123`). If the Android app is installed, Android App Links routes them directly into the app. If not installed, the browser opens the Firebase Hosting web fallback at the same URL.

**When to use:** This is the correct pattern since Firebase Dynamic Links was deprecated in August 2025.

**Trade-offs:** Requires `assetlinks.json` to be served correctly from your domain. Android App Links only intercepts if the app is installed AND verification passes — first-time givers always see the web fallback.

**Implementation:**
```
Domain: yourapp.ro (or registry.yourapp.ro)
  ↓
/.well-known/assetlinks.json   ← served by Firebase Hosting
  ↓
Android intent-filter in AndroidManifest.xml:
  <intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <data android:scheme="https" android:host="yourapp.ro" />
  </intent-filter>
  ↓
App not installed → browser opens → Firebase Hosting serves web fallback
App installed → system routes into app → RegistryDetailActivity/Fragment
```

**Confidence:** HIGH — Android App Links is the official replacement. Firebase Hosting can serve `assetlinks.json` from `firebase.json` hosting rewrites.

### Pattern 4: Affiliate URL Transformation

**What:** When a user adds an item via URL, the URL Transformer identifies the merchant (by domain pattern matching) and appends the correct affiliate tag. The transformed URL is stored, not the original. EMAG product URLs from the catalog API get the tag injected at add time.

**When to use:** On item creation (both via URL import and EMAG catalog). Also re-applied at reservation time when redirecting the giver, to ensure tags are always current.

**Trade-offs:** Storing the transformed URL means the affiliate tag is baked in. If the affiliate ID changes, existing items need migration. Consider storing the original URL and transforming at redirect time for more flexibility (but adds a Cloud Function call on every redirect).

**Recommended approach:** Transform at item-add time for EMAG (known merchant, stable tags). For arbitrary URLs, transform at item-add time and store both original and transformed — allows re-transformation if rules change.

## Data Flow

### Reservation Flow (Critical Path)

```
Giver opens registry link
    ↓ (HTTPS URL → App Link or web fallback)
Registry detail screen loads (Firestore real-time listener)
    ↓
Available items shown (status == "available")
    ↓
Giver taps "Reserve" → provides name/email (or logs in)
    ↓
Android SDK calls Cloud Function: createReservation(itemId, giverInfo)
    ↓ (Firestore transaction — atomic)
    → items/{itemId}.status = "reserved"
    → reservations/{id} = { giver, expiresAt, status: "active" }
    ↓ (Cloud Task enqueued at now+30m)
    ↓
Cloud Function returns: { affiliateUrl }
    ↓
Android opens affiliate URL in external browser / Chrome Custom Tab
Giver completes purchase at retailer
    ↓
--- 30 minutes later ---
Cloud Task fires releaseReservation()
    → If status still "active": release item, send expiry email
    → If status "purchased" (owner marked): leave closed
```

### Registry Owner Flow

```
Owner adds item via URL
    ↓
AffiliateUrlTransformer identifies merchant, injects tag
    ↓
Item stored in Firestore with transformed URL + original URL
    ↓
Registry real-time listener propagates to all open giver screens
```

### EMAG Catalog Flow

```
Owner searches EMAG catalog (from Android app)
    ↓
Android calls EMAG public API directly (REST)
    ↓
Product results displayed
    ↓
Owner taps "Add to registry"
    ↓
EMAG product URL transformed (affiliate tag injected)
    ↓
Item stored in Firestore
```

Note: EMAG API is called from the Android client, not from Cloud Functions. The API is a product browsing/search API — no order placement, no inventory management.

### Invite Flow (Private Registries)

```
Owner sets registry to "private"
    ↓
Owner enters invitee email addresses
    ↓
Cloud Function: sendInviteEmail()
    → If email matches existing Firebase user: in-app notification + email
    → If no existing user: email-only with registry link
    ↓
Invitee opens link → Firestore security rules verify invitee email
    ↓
Access granted
```

### State Management on Android

```
Firestore real-time listener (items collection)
    ↓ (snapshot updates)
Repository (maps Firestore DTOs → domain models)
    ↓ (Flow/StateFlow)
ViewModel (maps domain → UI state)
    ↓ (StateFlow)
Compose UI (renders from state, no direct Firebase calls)
```

## Firestore Data Model

```
registries/
  {registryId}/
    ownerId: string
    title: string
    occasion: string        # birthday, wedding, baby, etc.
    visibility: "public" | "private"
    locale: "ro" | "en"
    createdAt: timestamp

    items/                  # subcollection
      {itemId}/
        title: string
        imageUrl: string
        originalUrl: string
        affiliateUrl: string
        merchant: string    # "emag" | "other"
        status: "available" | "reserved" | "purchased"
        reservedAt: timestamp | null
        reservedBy: string | null   # giver email

    invites/                # subcollection (private registries)
      {inviteId}/
        email: string
        status: "pending" | "accepted"

reservations/
  {reservationId}/
    registryId: string
    itemId: string
    giverEmail: string
    giverName: string
    giverUserId: string | null   # null for guests
    status: "active" | "expired" | "cancelled"
    createdAt: timestamp
    expiresAt: timestamp         # createdAt + 30 min
    cloudTaskName: string        # for task cancellation if manually released

users/
  {userId}/
    email: string
    displayName: string
    locale: "ro" | "en"
    notificationsEnabled: boolean
```

**Key design decision:** Reservations are a top-level collection (not a subcollection of items) so Cloud Functions can query all active reservations for expiry monitoring without needing to know which registry/item they belong to.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| EMAG Public API | Android client makes REST calls directly | Not behind Cloud Functions — no auth needed for public catalog search. Isolate in `data/remote/emag/` |
| EMAG Affiliate (Profitshare) | URL parameter injection (`smid=` or equivalent affiliate param) | Tag injected at item-add time; merchant detected by domain matching on `emag.ro` |
| Firebase Auth | Android SDK + Cloud Functions Admin SDK | Guests use anonymous auth; email/password for registered users |
| Firebase Firestore | Android SDK (real-time listeners) + Functions Admin SDK | Security rules enforce registry access; server-side writes bypass rules |
| Cloud Tasks | Admin SDK inside Cloud Functions only | Client never calls Cloud Tasks directly — only via Cloud Function |
| Firebase Hosting | Static files; Android App Links assetlinks.json | Web fallback is not a full SPA — intentionally minimal |
| Email delivery | Firebase Extensions (Trigger Email) or SendGrid via Function | Trigger Email extension easiest for Firebase-native setup |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Android ViewModel ↔ Repository | Kotlin coroutines / Flow | ViewModel has no Firebase imports |
| Repository ↔ Firestore | Firebase Android SDK (Kotlin extensions) | Only the data layer knows Firestore exists |
| Android App ↔ Cloud Functions | Firebase Functions callable SDK | Authenticated calls; guest tokens still authenticate |
| Cloud Function ↔ Cloud Tasks | Google Cloud Tasks Admin SDK | Only reservation creation and cancellation functions enqueue tasks |
| Cloud Function ↔ Email | Firebase Trigger Email Extension or HTTP to SendGrid | Abstractions in `notification/` module |
| Android App ↔ EMAG API | Retrofit / OkHttp REST calls | Isolated in `data/remote/emag/`; not visible to domain layer |
| Web App ↔ Firestore | Firebase JS SDK (real-time listeners) | Web app is read-heavy; reservation write goes via callable Cloud Function |

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k users | Current architecture is fine. Single Firestore database, Cloud Functions on-demand. |
| 1k-100k users | First bottleneck: Firestore hot documents (popular registry items). Mitigation: Firestore transactions handle concurrent reservations correctly by default (pessimistic mode on Standard edition). Monitor Cloud Tasks queue depth if many simultaneous reservations. |
| 100k+ users | EMAG API rate limits become a concern — consider server-side caching of catalog results in Firestore. Cloud Functions cold start latency may affect reservation UX — keep functions warm or use minimum instances. |

### Scaling Priorities

1. **First bottleneck:** Concurrent reservations on high-traffic registries. Firestore transactions handle this correctly but will retry under contention — acceptable for this use case. An item can only be reserved by one person, so contention is expected and finite.
2. **Second bottleneck:** Cloud Tasks queue throughput if a registry goes viral. Cloud Tasks has generous quotas (500 tasks/second default) — not a practical concern until substantial scale.

## Anti-Patterns

### Anti-Pattern 1: Client-Side Reservation Timer

**What people do:** Start a countdown timer on the client. When it hits zero, the client writes `status = "available"` back to Firestore.

**Why it's wrong:** The client can be killed, lose connectivity, or be manipulated. The reservation never releases. Items stay "stuck" as reserved indefinitely, frustrating registry owners and other givers.

**Do this instead:** Server-side Cloud Task enqueued at reservation creation. The task runs regardless of client state. The client shows a countdown UI only for UX — the server is the authority.

### Anti-Pattern 2: Firebase Dynamic Links for Sharing

**What people do:** Use Firebase Dynamic Links for shareable registry URLs and app-not-installed fallback.

**Why it's wrong:** Firebase Dynamic Links was shut down in August 2025. Any links created will return 404/403.

**Do this instead:** Android App Links (HTTPS URLs verified via `assetlinks.json`) with Firebase Hosting serving the web fallback at the same URL. This is simpler and more reliable anyway.

### Anti-Pattern 3: Calling EMAG API from Cloud Functions

**What people do:** Route EMAG catalog searches through a Cloud Function to avoid exposing API details on the client.

**Why it's wrong:** EMAG's public catalog API requires no authentication secret. Every Cloud Function call adds ~200-500ms latency and costs a function invocation. The Android client can call the EMAG API directly and Firestore security rules protect your data.

**Do this instead:** Call the EMAG API from the Android `data/remote/emag/` layer. The base URL is not a secret. If EMAG ever requires authenticated API access, revisit.

### Anti-Pattern 4: Real-Time Listener on Full Registry for Givers

**What people do:** Open a Firestore real-time listener on the entire registry document including all items for all givers at once.

**Why it's wrong:** Every reservation by any giver triggers a document update, re-downloading the full item list to every connected giver. For large registries (50+ items), this is wasteful.

**Do this instead:** Listen on `registries/{id}/items` as a collection, filtered to `status == "available"`. Givers only need to see available items, and the update is scoped to changed documents only.

### Anti-Pattern 5: Hardcoded Strings in UI

**What people do:** Hardcode Romanian strings in XML layouts or Kotlin code because "we'll add English later."

**Why it's wrong:** Extracting strings later is tedious and error-prone. The project explicitly requires Romanian and English — build it right from the start.

**Do this instead:** All user-visible strings go in `res/values/strings.xml` (English default) and `res/values-ro/strings.xml` (Romanian). Same discipline on the web fallback: all strings in `i18n/en.json` and `i18n/ro.json`.

## Build Order Implications

The component dependencies suggest this build sequence:

1. **Firebase project setup + Firestore schema + security rules** — everything else depends on this. Define collections and access rules before writing any app code.
2. **Firebase Auth + guest flow** — reservation and registry access require identity. Build auth before any protected screens.
3. **Registry creation + item management (Android)** — core owner flow. No reservation system needed yet.
4. **URL transformer + EMAG API integration** — item add flows depend on this. Build once, used throughout.
5. **Reservation flow + Cloud Tasks expiry** — the most complex component. Build after data model is stable.
6. **Web fallback (Firebase Hosting)** — depends on the Firestore schema being stable and security rules allowing guest reads.
7. **Android App Links + deep link routing** — requires a deployed domain (Firebase Hosting live). Build last.
8. **Invite system** — private registry feature, depends on auth, registry, and email infrastructure.
9. **Multilingual polish** — strings should be externalized from day one, but final translation review is a late-phase task.

## Sources

- Firebase Firestore transactions and concurrency: [Transaction serializability and isolation](https://firebase.google.com/docs/firestore/transaction-data-contention)
- Firebase Firestore transactions: [Transactions and batched writes](https://firebase.google.com/docs/firestore/manage-data/transactions)
- Cloud Tasks for scheduled Cloud Functions: [Enqueue functions with Cloud Tasks](https://firebase.google.com/docs/functions/task-functions)
- Firestore TTL policies: [Manage data retention with TTL policies](https://firebase.google.com/docs/firestore/ttl)
- Firebase Dynamic Links deprecation: [Dynamic Links Deprecation FAQ](https://firebase.google.com/support/dynamic-links-faq)
- Android App Links: [Create deep links — Android Developers](https://developer.android.com/training/app-links/deep-linking)
- Android MVVM + Firebase clean architecture: [FirestoreCleanArchitectureApp](https://github.com/alexmamo/FirestoreCleanArchitectureApp)
- eMAG affiliate program: [eMAG.ro on Profitshare](https://profitshare.ro/en/affiliate-programs/retail/emag)
- Race conditions in Firestore: [Race Conditions in Firestore: How to Solve it?](https://medium.com/quintoandar-tech-blog/race-conditions-in-firestore-how-to-solve-it-5d6ff9e69ba7)
- Cloud Functions scheduled tasks: [How to schedule a Cloud Function to run in the future](https://medium.com/firebase-developers/how-to-schedule-a-cloud-function-to-run-in-the-future-in-order-to-build-a-firestore-document-ttl-754f9bf3214a)

---
*Architecture research for: Gift Registry — Android + Firebase + EMAG + web fallback*
*Researched: 2026-04-04*
