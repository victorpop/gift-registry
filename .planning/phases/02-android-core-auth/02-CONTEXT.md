# Phase 2: Android Core + Auth - Context

**Gathered:** 2026-04-05
**Status:** Ready for planning

<domain>
## Phase Boundary

Android app scaffold with clean architecture, Hilt DI, Navigation3, and Firebase Authentication (email/password, Google OAuth, guest access). Multilingual support (Romanian/English) with auto-detection and manual override. No registry or item features — only the app skeleton and auth flows.

</domain>

<decisions>
## Implementation Decisions

### App Architecture & Navigation
- **ARCH-01:** Domain-Data-UI 3-layer clean architecture — domain layer holds use cases for auth flows, data layer wraps Firebase SDK, UI layer has Compose screens + ViewModels
- **ARCH-02:** Single NavHost with auth graph + main graph — auth screens gated by auth state, auto-redirect when logged in
- **ARCH-03:** Hilt DI modules organized by layer: DataModule, DomainModule, AppModule
- **ARCH-04:** ViewModel unit tests with fake repositories — verify auth state transitions without Firebase dependency

### Authentication UX Flow
- **AUTH-UX-01:** Single screen with tabs for Login / Sign Up — reduces navigation steps
- **AUTH-UX-02:** Google OAuth button placed below email form with "OR" divider — standard social login pattern, email remains primary
- **AUTH-UX-03:** "Continue as Guest" link below auth options — visible but secondary; guest provides name/email on reservation action, not at entry
- **AUTH-UX-04:** Guest-to-account conversion triggered via bottom sheet prompt after reservation action — non-blocking, contextual (per AUTH-06)

### Localization UX
- **I18N-UX-01:** Device locale auto-detected → app language, fallback to English — standard Android behavior
- **I18N-UX-02:** Manual language override in Settings screen with language picker, persisted via DataStore
- **I18N-UX-03:** Language switch recreates activity to apply immediately — standard Android locale change pattern
- **I18N-UX-04:** String keys follow Phase 1 convention: feature-namespaced (`auth_login_title`, `auth_signup_button`, etc.)

### Claude's Discretion
Specific Compose component structure, exact screen layouts, error handling UX, loading state patterns, and internal code organization within each layer are at Claude's discretion. Follow Material3 design guidelines and Jetpack Compose best practices.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/res/values/strings.xml` — English string resources with feature-namespaced key convention established in Phase 1
- `app/src/main/res/values-ro/strings.xml` — Romanian string resources
- `firebase.json` — Firebase project config with emulator ports (Auth=9099, Firestore=8080, Functions=5001, Hosting=5000)
- `firestore.rules` — Security rules covering users, registries, items collections
- `functions/` — Cloud Functions scaffold (TypeScript, Node 22)

### Established Patterns
- Feature-namespaced string key convention: `app_`, `common_`, `auth_`, `registry_`, `reservation_` prefixes
- Firebase Emulator Suite with singleProjectMode=true
- firebase-functions/v2 import pattern for 2nd gen Cloud Functions

### Integration Points
- Firebase Auth emulator on port 9099 for local development
- Firestore users collection for user profile storage (schema from Phase 1)
- `assetlinks.json` placeholder needs package name from this phase
- Navigation3 graph will be extended by Phase 3 (registry screens) and Phase 5 (web deep links)

</code_context>

<specifics>
## Specific Ideas

No specific requirements — user accepted all recommended approaches. Standard Android patterns apply throughout.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 02-android-core-auth*
*Context gathered: 2026-04-05*
