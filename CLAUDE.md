<!-- GSD:project-start source:PROJECT.md -->
## Project

**Gift Registry**

A general-purpose gift registry Android application with a web fallback for gift givers. Registry owners create and manage wishlists for any occasion (birthdays, weddings, baby showers, etc.), adding items via URL import or browsing an EMAG-backed product catalog. Gift givers access registries through shareable links, reserve items with a 30-minute purchase window, and click through to buy at the retailer. Monetized through affiliate link commissions.

**Core Value:** Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.

### Constraints

- **Tech stack**: Java/Kotlin for Android, Firebase for backend — no other persistence layer
- **Retailer integration**: EMAG API for catalog; other retailers supported via URL import only
- **Reservation model**: 30-minute hard timer, no extensions — keeps inventory state simple and predictable
- **Guest access**: Must work without account creation to reduce friction for gift givers
- **Localization**: All UI labels externalized in resource files (strings.xml for Android, i18n files for web) — no hardcoded strings
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

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
## Installation
# Firebase CLI (for Cloud Functions and Hosting)
# Cloud Functions init (TypeScript, Node 22)
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
## EMAG Integration — Critical Note
## Reservation System — Architecture Note
## Localization
| Platform | Approach | Notes |
|----------|----------|-------|
| Android | `res/values/strings.xml` (default English) + `res/values-ro/strings.xml` (Romanian) | Standard Android resource system; `values-ro` for Romanian; never hardcode strings in Kotlin/Compose |
| Web fallback | i18next with JSON translation files (`en.json`, `ro.json`) | Standard React i18n; locale detected from browser or explicit user switch |
| Cloud Functions | No UI strings; error messages in English | Functions return structured error codes; clients translate them |
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
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
