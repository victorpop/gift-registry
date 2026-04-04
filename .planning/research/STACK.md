# Stack Research

**Domain:** Android gift registry app with Firebase backend and web fallback
**Researched:** 2026-04-04
**Confidence:** HIGH (core Android/Firebase stack) | MEDIUM (EMAG integration, web fallback approach)

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.3.x (stable) | Primary language | Official Google-recommended language for Android since 2019; Compose is Kotlin-only; coroutines and Flow are native Kotlin features essential for Firebase reactive patterns |
| Jetpack Compose BOM | 2026.03.00 | UI framework | Google's current standard for Android UI; replaces XML layouts; mandatory for modern navigation patterns; significantly less boilerplate than View system |
| Android Gradle Plugin | 9.x | Build toolchain | AGP 9.0+ has Kotlin support built in — no separate kotlin-android plugin needed; required for current stable Compose and Hilt versions |
| Firebase Android BoM | 34.11.0 | Backend SDK version management | BoM ensures all Firebase libraries use compatible versions; v34+ dropped KTX sub-modules (Kotlin APIs merged into main modules) — using main modules is now the correct pattern |
| Cloud Firestore | via BoM 34.11.0 (26.1.2) | Primary database | Real-time listeners for reservation state; atomic transactions for the 30-minute reservation race condition; no self-hosted infrastructure; Firebase-native |
| Firebase Authentication | via BoM | Auth (email/password, guest, Google) | Handles registered users and email-based flows; Firebase Auth supports anonymous auth for guest path then credential linking for account conversion |
| Firebase Cloud Functions (2nd gen) | firebase-functions 4.x, Node.js 22 | Server-side logic | 2nd gen is the current generation; handles: reservation expiry via Cloud Tasks, affiliate URL transformation, email triggers, Firestore event triggers |
| Firebase Cloud Messaging | via BoM | Push notifications | In-app and background notifications for registry invites and purchase alerts; Firebase-native, no third-party dependency |

### Android App Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hilt | 2.51.x | Dependency injection | Google-recommended DI for Android; integrates with ViewModel, Navigation, WorkManager; far less manual wiring than Dagger alone; use everywhere |
| Navigation3 (androidx.navigation3) | 1.0.1 | In-app navigation | New stable Compose-first navigation library (Nov 2025); better back stack control and adaptive layout support than Nav2; use for all new projects |
| Kotlin Coroutines | 1.9.x | Async operations | All Firebase SDK calls return Kotlin-awaitable types; use for all async work, including Firestore listeners and Cloud Functions calls |
| Kotlin Flow | bundled with coroutines | Reactive streams | Firestore `snapshotFlow` pattern for real-time reservation state; use StateFlow in ViewModels |
| Coil | 3.4.0 | Image loading | Kotlin-first, Compose-native image loading; handles product thumbnails from EMAG; AsyncImagePainter works directly in composables |
| Retrofit | 3.0.0 | HTTP client for EMAG | EMAG product catalog is accessed via REST (see EMAG note below); Retrofit 3 is Kotlin-first and requires no separate converter setup for JSON |
| Kotlin Serialization | 2.x | JSON parsing | Native Kotlin JSON; pairs with Retrofit 3 and Firestore custom object mapping; no reflection overhead like Gson |
| DataStore (Preferences) | via AndroidX | Local preferences | Store user locale preference, guest session token; Firebase is the source of truth for all shared state — DataStore only for local-only settings |
| Material3 | via Compose BOM | Design system | Google's current design language; Compose BOM includes M3; use M3 components (not M2) throughout |
| Firebase App Check | via BoM | API abuse prevention | Prevents unauthorized clients from accessing Firestore/Functions; critical for a public-facing reservation API |

### Web Fallback Stack

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| React | 19.x | Web UI framework | Firebase JS SDK is first-class React; Firebase Hosting integrates natively; best ecosystem for rapid web implementation; not sharing code with Android so no cross-platform overhead |
| Firebase JS SDK | 11.x | Same Firebase backend | Shares Firestore, Auth, and Functions with Android app; no separate backend needed for web fallback |
| Firebase Hosting | n/a | Static web hosting | Zero-configuration CDN for React SPA; paired with existing Firebase project; free tier sufficient for giver-only access volume |
| i18next | 24.x | Web localization | Industry standard for React i18n; handles Romanian/English switching; same structure as Android strings.xml pattern but for web |

### Cloud Functions (Backend) Stack

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Node.js | 22 (LTS) | Functions runtime | Current Firebase-recommended stable runtime; Node.js 18 deprecated, 20 and 22 fully supported, 24 in preview |
| TypeScript | 5.x | Functions language | Type safety for shared data models (Firestore document shapes, reservation state machine); prevents schema drift between Android and web |
| Cloud Tasks | via Admin SDK | Delayed reservation expiry | Schedule a task 30 minutes after reservation creation to release it; more precise than polling a scheduled cron; tasks can be cancelled if giver completes purchase |
| Nodemailer / SendGrid | current | Transactional email | Expiry emails to givers when reservation lapses; SendGrid preferred for deliverability in Romanian market |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Android Studio Meerkat (2024.3+) | IDE | Required for Compose preview and AGP 9.x support |
| Firebase Emulator Suite | Local dev/testing | Emulates Firestore, Auth, Functions, Hosting locally — eliminates dev costs and enables offline development |
| Firebase CLI | Deploy and emulator control | Required for Functions deployment and Hosting; use version 13+ for 2nd gen Functions support |
| ktlint | Kotlin linting | Enforce code style; integrate into CI |
| Gradle Version Catalogs (libs.versions.toml) | Dependency management | Single source of truth for all library versions; standard in AGP 8+ projects |

---

## Installation

```kotlin
// build.gradle.kts (app module)

// Compose BOM
val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
implementation(composeBom)
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Firebase BOM — no KTX modules, use main modules directly (BoM 34+)
val firebaseBom = platform("com.google.firebase:firebase-bom:34.11.0")
implementation(firebaseBom)
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")
implementation("com.google.firebase:firebase-messaging")
implementation("com.google.firebase:firebase-appcheck-playintegrity")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")
kapt("com.google.dagger:hilt-android-compiler:2.51.1")

// Navigation 3
implementation("androidx.navigation3:navigation3-ui:1.0.1")

// Coil
implementation("io.coil-kt.coil3:coil-compose:3.4.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

// Retrofit + Kotlin Serialization
implementation("com.squareup.retrofit2:retrofit:3.0.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.x")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.x")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.x")
```

```bash
# Firebase CLI (for Cloud Functions and Hosting)
npm install -g firebase-tools@latest

# Cloud Functions init (TypeScript, Node 22)
firebase init functions  # select TypeScript, Node 22
```

---

## Alternatives Considered

| Recommended | Alternative | Why Not |
|-------------|-------------|---------|
| Kotlin | Java | Java is explicitly ruled out in project scope; Compose is Kotlin-only |
| Jetpack Compose | XML Views | Compose is the current Google standard; XML has no future investment; new Navigation3 is Compose-only |
| Navigation3 | Navigation Compose 2.x | Nav3 is the new stable Compose-first library as of Nov 2025; new projects should adopt it; Nav2 is legacy |
| Hilt | Koin | Hilt is Google-recommended and has official Jetpack integrations (ViewModel, Navigation3); Koin is fine but less ecosystem support |
| Coil 3 | Glide, Picasso | Coil is Kotlin-first and has native Compose composables; Glide/Picasso are Java-based and require bridging |
| Retrofit 3 | Ktor Client | Retrofit 3 is now Kotlin-first and more familiar to the Android ecosystem; Ktor is better for KMP projects — this is Android-only |
| React (web fallback) | Kotlin Multiplatform / Compose Web | KMP/Compose Web is not stable for production web targets as of 2026; React with Firebase JS SDK is battle-tested and shares the same backend with zero extra work |
| Cloud Tasks | Scheduled cron functions | Cron runs on a fixed interval (e.g., every minute) and must scan all reservations; Cloud Tasks fires exactly at the 30-minute mark per reservation — more efficient and precise |
| Firebase Cloud Functions | Self-hosted backend (Express/NestJS) | Firebase Functions eliminates server management; aligns with the "Firebase only" constraint in PROJECT.md |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Firebase KTX modules (`firebase-auth-ktx`, etc.) | Removed from BoM v34.0.0 (July 2025). Projects depending on KTX modules cannot upgrade past BoM v33 | Use main modules directly (`firebase-auth`, `firebase-firestore`) — Kotlin APIs are now in the main modules |
| Firebase Realtime Database | Weaker querying, no atomic transactions, harder security rules; Firestore is the current Firebase database product | Cloud Firestore |
| Navigation Compose 2.x for new projects | Legacy API; Navigation3 (1.0.1 stable) is the recommended replacement with better Compose integration | `androidx.navigation3:navigation3-ui` |
| Gson | Reflection-based, no Kotlin null safety, being deprecated in Android ecosystem | `kotlinx-serialization-json` |
| LiveData | Replaced by StateFlow/SharedFlow in Kotlin coroutines ecosystem; Compose does not work well with LiveData observers | Kotlin StateFlow + `collectAsStateWithLifecycle()` |
| Cloud Functions 1st gen | Deprecated; 1st gen has stricter concurrency limits and is being sunset | Cloud Functions 2nd gen |
| Room (SQLite) | Explicitly out of scope in PROJECT.md: "Multiple persistence layers — Firebase only, no SQLite" | Cloud Firestore for all persistence |
| XML layouts / Fragment navigation | Requires mixing legacy APIs with Compose; no path forward to Navigation3 | Jetpack Compose + Navigation3 throughout |

---

## EMAG Integration — Critical Note

**The EMAG Marketplace API is a seller/merchant API, not a public product catalog search API.**

The documented API (v4.5.1) is for marketplace sellers to manage their listings and process orders. It requires seller credentials (Basic Auth with marketplace account). This is not appropriate for a consumer gift registry browsing experience.

**Actual integration strategy:**

1. **Affiliate links via 2Performant**: EMAG's affiliate program was managed by ProfitShare (acquired by 2Performant in May 2025). Register as an affiliate on 2Performant to get an affiliate ID. Deep links to EMAG products are constructed as `https://event.2performant.com/events/click?ad_type=product_store&unique=<YOUR_ID>&aff_code=<CODE>&campaign_unique=<CAMPAIGN>` — the URL transformer Cloud Function appends this tag to any recognized EMAG URL.

2. **Product catalog browsing**: Since there is no public EMAG search API, product catalog browsing must be implemented by one of:
   - **Web scraping** (legally grey, fragile, EMAG ToS may prohibit)
   - **WebView embedding** of emag.ro search within the app (simpler but poor UX)
   - **Registry owners add items via URL paste** (the primary flow already in scope)
   - Deeper investigation needed before committing to a catalog-browsing feature — this warrants a dedicated research spike before Phase implementation

**Confidence:** LOW on "EMAG public catalog API" — no evidence this exists. MEDIUM on 2Performant affiliate link approach — confirmed via research but API details need direct 2Performant account verification.

---

## Reservation System — Architecture Note

The 30-minute reservation timer is the most technically complex piece. The recommended approach:

1. **Client writes reservation to Firestore** using a transaction that checks `item.status == "available"` before setting `item.status = "reserved"` with `reservedBy`, `reservedAt`, and `expiresAt` fields.
2. **Cloud Function (Firestore trigger)** fires on reservation creation, enqueues a Cloud Tasks task scheduled for `now + 30 minutes`.
3. **Cloud Tasks executes the task** after 30 minutes: checks if the reservation is still active (not purchased), releases the item back to "available", sends expiry email.
4. **Client cancels task** via a Cloud Function call if the item is marked "purchased" — preventing false expiry emails.

Firestore's optimistic concurrency handles concurrent reservation attempts atomically; only one writer wins per transaction.

---

## Localization

| Platform | Approach | Notes |
|----------|----------|-------|
| Android | `res/values/strings.xml` (default English) + `res/values-ro/strings.xml` (Romanian) | Standard Android resource system; `values-ro` for Romanian; never hardcode strings in Kotlin/Compose |
| Web fallback | i18next with JSON translation files (`en.json`, `ro.json`) | Standard React i18n; locale detected from browser or explicit user switch |
| Cloud Functions | No UI strings; error messages in English | Functions return structured error codes; clients translate them |

Language selection: default to device locale; fallback to English if locale is unsupported.

---

## Version Compatibility

| Component | Compatible With | Notes |
|-----------|-----------------|-------|
| Compose BOM 2026.03.00 | Kotlin 2.x, AGP 8.7+ / 9.x | BOM pins all Compose library versions; do not override individual Compose versions |
| Firebase BoM 34.11.0 | AGP 7+, minSdk 21 | No KTX modules; requires migration if upgrading from BoM <34 |
| Navigation3 1.0.1 | Compose BOM 2025.x+ | Cannot mix with Navigation Compose 2.x in same nav graph |
| Hilt 2.51.x | AGP 7+, Kotlin 1.9+/2.x | Use `kapt` for annotation processing in Kotlin; `ksp` support coming but kapt is stable |
| Kotlin 2.3.x | AGP 9.x (built-in support) | AGP 9 removes need for separate kotlin-android plugin |
| Retrofit 3.0.0 | OkHttp 4.12, Java 8+, minSdk 21 | Binary compatible with Retrofit 2; upgrade is non-breaking for most apps |
| Coil 3.4.0 | Compose BOM 2025+, OkHttp 4+ | Coil 3 is Kotlin Multiplatform compatible but used here Android-only |

---

## Sources

- [Firebase Android SDK Release Notes](https://firebase.google.com/support/release-notes/android) — confirmed BoM 34.11.0 (March 2026), KTX module removal in v34.0.0 (HIGH confidence)
- [Firebase KTX Migration Guide](https://firebase.google.com/docs/android/kotlin-migration) — confirmed main module pattern for Kotlin (HIGH confidence)
- [Jetpack Navigation 3 stable announcement](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html) — Navigation3 1.0.0 stable Nov 2025 (HIGH confidence)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — confirmed 2026.03.00 (HIGH confidence)
- [Firebase Firestore Transactions](https://firebase.google.com/docs/firestore/manage-data/transactions) — atomic reservation pattern (HIGH confidence)
- [Firebase Cloud Tasks](https://firebase.google.com/docs/functions/task-functions) — delayed expiry approach (HIGH confidence)
- [AGP 9.0 built-in Kotlin](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — Kotlin plugin no longer separate (HIGH confidence)
- [2Performant acquires ProfitShare (May 2025)](https://2performant.com/blog/2performant-signs-final-contract-for-the-acquisition-of-profitshare-romania-and-bulgaria/) — EMAG affiliate network context (MEDIUM confidence)
- [EMAG Marketplace API docs](https://marketplace.emag.ro/infocenter/emag-academy/how-to-add-a-product/product-import-through-api-or-feeds/api-documentation/?lang=en) — confirmed seller-only API, not public catalog (MEDIUM confidence)
- [Coil changelog](https://coil-kt.github.io/coil/changelog/) — version 3.4.0 current (HIGH confidence)
- [Retrofit releases](https://github.com/square/retrofit/releases) — 3.0.0 released May 2025 (HIGH confidence)
- [Hilt docs](https://developer.android.com/training/dependency-injection/hilt-android) — 2.51.x current (MEDIUM confidence — version from community sources, not official release page)

---

*Stack research for: Gift Registry Android app (Kotlin/Compose/Firebase)*
*Researched: 2026-04-04*
