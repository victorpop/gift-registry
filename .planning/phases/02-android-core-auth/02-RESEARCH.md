# Phase 2: Android Core + Auth - Research

**Researched:** 2026-04-05
**Domain:** Android Kotlin / Jetpack Compose / Firebase Authentication / Navigation3 / Hilt DI / Android Localization
**Confidence:** HIGH (core stack), MEDIUM (Navigation3 auth-gating patterns — library is new and community guidance is emerging)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **ARCH-01:** Domain-Data-UI 3-layer clean architecture — domain layer holds use cases for auth flows, data layer wraps Firebase SDK, UI layer has Compose screens + ViewModels
- **ARCH-02:** Single NavHost with auth graph + main graph — auth screens gated by auth state, auto-redirect when logged in
- **ARCH-03:** Hilt DI modules organized by layer: DataModule, DomainModule, AppModule
- **ARCH-04:** ViewModel unit tests with fake repositories — verify auth state transitions without Firebase dependency
- **AUTH-UX-01:** Single screen with tabs for Login / Sign Up — reduces navigation steps
- **AUTH-UX-02:** Google OAuth button placed below email form with "OR" divider — standard social login pattern, email remains primary
- **AUTH-UX-03:** "Continue as Guest" link below auth options — visible but secondary; guest provides name/email on reservation action, not at entry
- **AUTH-UX-04:** Guest-to-account conversion triggered via bottom sheet prompt after reservation action — non-blocking, contextual (per AUTH-06)
- **I18N-UX-01:** Device locale auto-detected → app language, fallback to English — standard Android behavior
- **I18N-UX-02:** Manual language override in Settings screen with language picker, persisted via DataStore
- **I18N-UX-03:** Language switch recreates activity to apply immediately — standard Android locale change pattern
- **I18N-UX-04:** String keys follow Phase 1 convention: feature-namespaced (`auth_login_title`, `auth_signup_button`, etc.)

### Claude's Discretion

Specific Compose component structure, exact screen layouts, error handling UX, loading state patterns, and internal code organization within each layer are at Claude's discretion. Follow Material3 design guidelines and Jetpack Compose best practices.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | User can sign up with email and password | Firebase Auth `createUserWithEmailAndPassword` — standard Kotlin coroutine await pattern |
| AUTH-02 | User can log in with email and password | Firebase Auth `signInWithEmailAndPassword` |
| AUTH-03 | User can log in with Google OAuth | Credential Manager API + Firebase `signInWithCredential(GoogleAuthProvider.getCredential(...))` |
| AUTH-04 | User session persists across app restarts | Firebase Auth persists session by default in local storage; `currentUser` survives process death |
| AUTH-05 | Guest can access registry by providing first name, last name, email | Anonymous Firebase Auth + Firestore guest profile — no UI login required at entry; guest data collected at reservation action |
| AUTH-06 | Guest offered account creation after completing a reservation | Anonymous account linking via `currentUser.linkWithCredential(credential)` triggered from bottom sheet |
| I18N-01 | App UI supports Romanian and English | `res/values/strings.xml` (en) + `res/values-ro/strings.xml` (ro) — already partially established in Phase 1 |
| I18N-03 | Language auto-detected from device locale with manual override | `AppCompatDelegate.setApplicationLocales()` with DataStore persistence — backward-compatible to API 21 |
</phase_requirements>

---

## Summary

Phase 2 creates the Android app from scratch — there is currently no Kotlin code, no `build.gradle.kts`, and no `AndroidManifest.xml` in the project. The `app/` directory contains only `src/main/res/values/strings.xml` and `src/main/res/values-ro/strings.xml` established in Phase 1. This phase must scaffold the full Android project structure before implementing any auth features.

The core technical risk is **Navigation3's auth-gating pattern**. Navigation3 (released stable November 2025) is significantly different from Navigation Compose 2.x — the developer owns the back stack as a `SnapshotStateList<T>`, and there is no built-in conditional graph concept. Auth gating requires observing Firebase Auth state as a `StateFlow` and imperatively manipulating the back stack rather than declaring route guards. The pattern is well-understood conceptually but community examples are still sparse.

A second important finding is **Hilt version drift**. CLAUDE.md specifies Hilt 2.51.x with KAPT, but the current stable version is **2.59.2** and it requires KSP (not KAPT) and Android Gradle Plugin 9.0+ as a minimum. Since the project targets AGP 9.x, using Hilt 2.59.2 with KSP is the correct choice. KAPT is not wrong but KSP is 2x faster and the current recommended approach for all new projects.

**Primary recommendation:** Scaffold the full Android project first (build files, manifest, Application class, MainActivity), then implement auth in clean architecture layers (data → domain → UI), then add navigation gating, then wire localization. Do not attempt feature work before the scaffold is complete.

---

## Project Constraints (from CLAUDE.md)

| Directive | What It Means for This Phase |
|-----------|------------------------------|
| Kotlin only (no Java) | All source files `.kt`; no `.java` files |
| Firebase BoM 34.11.0 — no KTX modules | Import `com.google.firebase:firebase-auth` not `firebase-auth-ktx` |
| Navigation3 1.0.1 — not Navigation Compose 2.x | Use `androidx.navigation3:navigation3-runtime` and `navigation3-ui` |
| Hilt for DI everywhere | All ViewModels, repositories, use cases injected via Hilt |
| Material3 only (not M2) | `MaterialTheme` from `androidx.compose.material3` |
| No LiveData | Use `StateFlow` + `collectAsStateWithLifecycle()` |
| No Room / SQLite | DataStore for local-only preferences; Firebase for all shared state |
| No hardcoded strings | Every UI string in `strings.xml` / `strings-ro.xml` with feature-namespaced key |
| AGP 9.x | Kotlin plugin is built into AGP 9 — no separate `kotlin-android` plugin needed |
| strings.xml key convention | `auth_`, `common_`, `app_`, `registry_`, `reservation_` prefixes |

---

## Standard Stack

### Core (this phase)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.3.x | Primary language | AGP 9 built-in; Compose is Kotlin-only |
| Android Gradle Plugin | 9.0.0 | Build toolchain | Hilt 2.59 requires AGP 9 minimum |
| Kotlin Symbol Processing (KSP) | 2.3.4 | Annotation processing | Required by Hilt 2.59.2; 2x faster than KAPT |
| Jetpack Compose BOM | 2026.03.00 | UI framework version management | Pins all Compose versions consistently |
| Material3 | via Compose BOM | Design system | Google current standard; `MaterialTheme` from M3 |
| Firebase BoM | 34.11.0 | Firebase version management | No KTX sub-modules — Kotlin APIs in main modules |
| Firebase Authentication | via BoM | Auth (email/password, Google, anonymous) | Session persistence, credential linking built-in |
| Hilt | 2.59.2 | Dependency injection | Requires AGP 9 + KSP; `@HiltAndroidApp`, `@HiltViewModel` |
| Navigation3 runtime | 1.0.1 | Back stack state management | Compose-first; developer owns `SnapshotStateList` |
| Navigation3 UI | 1.0.1 | `NavDisplay` composable | Renders back stack entries |
| AndroidX Credentials | 1.5.0 | Google Sign-In (Credential Manager) | Replaces deprecated `GoogleSignInClient`; API 23+ |
| credentials-play-services-auth | 1.5.0 | Credential Manager on pre-API 34 | Required for backward compat with Google services |
| Google Identity (googleid) | 1.1.1 | `GetGoogleIdOption` / `GetSignInWithGoogleOption` | Part of Credential Manager Google Sign-In flow |
| DataStore Preferences | 1.1.x (via AndroidX) | Local-only preferences | Locale override, guest session — NOT for shared state |
| AppCompat | 1.7.x | Activity base class + locale APIs | `AppCompatActivity` required for `AppCompatDelegate.setApplicationLocales()` |
| Kotlin Coroutines | 1.9.x | Async operations | Firebase SDK calls are `suspend` / coroutine-compatible |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Hilt Navigation Compose (androidx.hilt) | 1.2.0 | `hiltViewModel()` in composables | Needed if scoping ViewModels to NavEntry — verify compatibility with Nav3 |
| Kotlin Serialization | 2.x | JSON (future phases) | Don't add yet — not needed for auth-only phase |
| Coil | 3.4.0 | Image loading (future phases) | Add in Phase 3 when product images appear |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| KSP for Hilt | KAPT | KAPT still works but 2x slower; KSP is the modern path for Hilt 2.59+ |
| Credential Manager | Legacy `GoogleSignInClient` | `GoogleSignInClient` is deprecated and will be removed from Play Services in 2025+ |
| `AppCompatDelegate.setApplicationLocales()` | Custom locale wrapper with `createConfigurationContext` | Per-app language API handles backward compat automatically down to API 21; custom wrappers have more edge cases |

### Installation

```bash
# Verify current package versions against npm registry (run as sanity check)
# For Android: use libs.versions.toml + Gradle sync
# Key versions to declare in libs.versions.toml:
#
# hilt = "2.59.2"
# ksp = "2.3.4"            # must match Kotlin version (2.3.x)
# kotlin = "2.3.20"        # confirm latest 2.3.x patch
# compose-bom = "2026.03.00"
# firebase-bom = "34.11.0"
# navigation3 = "1.0.1"
# credentials = "1.5.0"
# googleid = "1.1.1"
# appcompat = "1.7.0"
# datastore = "1.1.1"
# hilt-navigation-compose = "1.2.0"
```

**Version verification note:** The `nowinandroid` reference project uses Hilt 2.59, KSP 2.3.4, AGP 9.0.0, and Navigation3 1.0.0. This phase targets Navigation3 1.0.1 (latest stable as of February 2026) — confirmed via official AndroidX releases page.

---

## Architecture Patterns

### Recommended Project Structure

```
app/
├── build.gradle.kts
├── google-services.json          # Firebase project config
└── src/main/
    ├── AndroidManifest.xml
    ├── kotlin/com/example/giftregistry/
    │   ├── GiftRegistryApp.kt        # @HiltAndroidApp Application class
    │   ├── MainActivity.kt           # @AndroidEntryPoint, AppCompatActivity, single activity
    │   ├── di/
    │   │   ├── AppModule.kt          # Context, DataStore bindings
    │   │   ├── DataModule.kt         # Repository implementations
    │   │   └── DomainModule.kt       # Use case bindings (if needed)
    │   ├── data/
    │   │   ├── auth/
    │   │   │   ├── FirebaseAuthDataSource.kt
    │   │   │   └── AuthRepositoryImpl.kt
    │   │   └── preferences/
    │   │       └── LocaleDataStore.kt
    │   ├── domain/
    │   │   ├── auth/
    │   │   │   ├── AuthRepository.kt       # interface
    │   │   │   ├── SignUpUseCase.kt
    │   │   │   ├── SignInEmailUseCase.kt
    │   │   │   ├── SignInGoogleUseCase.kt
    │   │   │   ├── SignInAnonymousUseCase.kt
    │   │   │   ├── LinkAccountUseCase.kt
    │   │   │   └── ObserveAuthStateUseCase.kt
    │   │   └── model/
    │   │       └── User.kt             # pure Kotlin model (no Firebase imports)
    │   └── ui/
    │       ├── navigation/
    │       │   ├── AppDestinations.kt   # sealed class/objects for route keys
    │       │   └── AppNavigation.kt     # NavDisplay + auth gating logic
    │       ├── auth/
    │       │   ├── AuthScreen.kt        # tabbed login/signup composable
    │       │   ├── AuthViewModel.kt     # @HiltViewModel
    │       │   └── AuthUiState.kt       # sealed class
    │       ├── settings/
    │       │   └── SettingsScreen.kt    # language picker
    │       └── theme/
    │           ├── Theme.kt
    │           ├── Color.kt
    │           └── Type.kt
    ├── res/
    │   ├── values/strings.xml          # English (already exists, Phase 2 expands)
    │   └── values-ro/strings.xml       # Romanian (already exists, Phase 2 expands)
    └── AndroidManifest.xml
```

### Pattern 1: Navigation3 Auth Gating

**What:** Auth state is observed as a `StateFlow<Boolean>` in the root composable. The back stack is manipulated imperatively when auth state changes — not via route guards.

**When to use:** Any time navigation must be conditional on runtime state (auth, onboarding, feature flags).

```kotlin
// Source: https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html
// + https://github.com/android/nav3-recipes

// AppDestinations.kt
sealed class AppDestination {
    data object Auth : AppDestination()
    data object Home : AppDestination()
    data object Settings : AppDestination()
}

// AppNavigation.kt
@Composable
fun AppNavigation(authViewModel: AuthViewModel = hiltViewModel()) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    
    // Developer owns the back stack
    val backStack = rememberNavBackStack(
        initialKey = if (isLoggedIn) AppDestination.Home else AppDestination.Auth
    )

    // React to auth state changes imperatively
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && backStack.contains(AppDestination.Auth)) {
            backStack.clear()
            backStack.add(AppDestination.Home)
        } else if (!isLoggedIn) {
            backStack.clear()
            backStack.add(AppDestination.Auth)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLast() },
        entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<AppDestination.Auth> { AuthScreen() }
            entry<AppDestination.Home> { HomeScreen() }
            entry<AppDestination.Settings> { SettingsScreen() }
        }
    )
}
```

**Key insight:** `rememberViewModelStoreNavEntryDecorator()` is required to scope ViewModels to their NavEntry so they survive recomposition but are cleared on pop.

### Pattern 2: Hilt 2.59.2 with KSP — Scaffold Setup

**What:** Full project scaffold with AGP 9 and KSP replacing KAPT.

```kotlin
// gradle/libs.versions.toml
[versions]
hilt = "2.59.2"
ksp = "2.3.4"
kotlin = "2.3.20"
agp = "9.0.0"
composeBom = "2026.03.00"
firebaseBom = "34.11.0"
navigation3 = "1.0.1"
credentials = "1.5.0"
googleid = "1.1.1"
appcompat = "1.7.0"
datastore = "1.1.1"
hiltNavigationCompose = "1.2.0"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }
credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
credentials-play-services = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
googleid = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

```kotlin
// app/build.gradle.kts (key sections)
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.giftregistry"  // set actual package name here
    compileSdk = 36
    defaultConfig {
        minSdk = 24  // covers 98%+ of active devices as of 2026
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Firebase BOM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation3
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    // Google Sign-In via Credential Manager
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // AppCompat (for locale APIs)
    implementation(libs.appcompat)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")  // collectAsStateWithLifecycle

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.2.0")  // StateFlow testing
}
```

### Pattern 3: Firebase Auth — Email/Password + Google + Anonymous

```kotlin
// Source: https://firebase.google.com/docs/auth/android/password-auth
// Source: https://firebase.google.com/docs/auth/android/anonymous-auth
// Source: https://firebase.google.com/docs/auth/android/account-linking

// AuthRepositoryImpl.kt (data layer — wraps Firebase SDK)
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Unit
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            Unit
        }

    override suspend fun signInAnonymously(): Result<Unit> =
        runCatching {
            firebaseAuth.signInAnonymously().await()
            Unit
        }

    // Guest-to-account conversion (AUTH-06)
    override suspend fun linkEmailToAnonymous(email: String, password: String): Result<Unit> =
        runCatching {
            val credential = EmailAuthProvider.getCredential(email, password)
            firebaseAuth.currentUser!!.linkWithCredential(credential).await()
            Unit
        }

    override fun signOut() = firebaseAuth.signOut()
}
```

```kotlin
// Google Sign-In via Credential Manager (AUTH-03)
// Source: https://developers.google.com/identity/android-credential-manager
// In AuthViewModel or a dedicated GoogleSignInHelper

suspend fun getGoogleIdToken(context: Context, serverClientId: String): String? {
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)  // show all Google accounts
        .setServerClientId(serverClientId)      // from google-services.json
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    return try {
        val result = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        googleIdTokenCredential.idToken
    } catch (e: GetCredentialException) {
        null  // user cancelled or no accounts available
    }
}
```

### Pattern 4: Per-App Language with DataStore

```kotlin
// Source: https://developer.android.com/guide/topics/resources/app-languages

// LocaleDataStore.kt (data layer)
class LocaleDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.createDataStore("user_prefs")
    private val LOCALE_KEY = stringPreferencesKey("locale_override")

    val localeFlow: Flow<String?> = dataStore.data.map { it[LOCALE_KEY] }

    suspend fun setLocale(languageTag: String?) {
        dataStore.edit { prefs ->
            if (languageTag == null) prefs.remove(LOCALE_KEY)
            else prefs[LOCALE_KEY] = languageTag
        }
    }
}

// MainActivity.kt — apply locale before setContent
class MainActivity : AppCompatActivity() {
    @Inject lateinit var localeDataStore: LocaleDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        // Restore persisted locale BEFORE super.onCreate for correct initial render
        lifecycleScope.launch {
            localeDataStore.localeFlow.firstOrNull()?.let { tag ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GiftRegistryTheme { AppNavigation() } }
    }
}

// SettingsScreen language switch
fun applyLocale(languageTag: String) {
    // AppCompatDelegate handles activity recreation automatically
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    // Also persist to DataStore for next launch
}
```

**AndroidManifest.xml locale config:**
```xml
<application
    android:localeConfig="@xml/locales_config">
    <!-- Required for per-app language support -->
```

```xml
<!-- res/xml/locales_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en"/>
    <locale android:name="ro"/>
</locale-config>
```

### Anti-Patterns to Avoid

- **Firebase KTX imports:** Never `import com.google.firebase.auth.ktx.*` — KTX modules removed in BoM 34.0.0. Use `com.google.firebase.auth.FirebaseAuth` directly.
- **LiveData in ViewModels:** Never `MutableLiveData` — use `MutableStateFlow` and expose as `StateFlow`.
- **`collectAsState()` instead of `collectAsStateWithLifecycle()`:** The lifecycle-aware variant stops collection when the composable is not visible (lifecycle < STARTED), preventing unnecessary work and memory leaks.
- **Hardcoding strings:** Every user-visible string needs a key in `strings.xml` and `strings-ro/strings.xml`. The Phase 1 convention is `auth_`, `common_`, etc.
- **Navigation Compose 2.x imports:** Do not add `androidx.navigation:navigation-compose` — it conflicts with Navigation3 and is explicitly ruled out in CLAUDE.md.
- **KAPT with AGP 9:** Don't use `apply plugin: 'kotlin-kapt'`. AGP 9 + Hilt 2.59 requires KSP.
- **`signInWithPopup` / web-style Google auth:** Android uses Credential Manager, not web-style redirects.
- **`Locale.setDefault()`:** Deprecated pattern that doesn't integrate with Android's per-app language system. Use `AppCompatDelegate.setApplicationLocales()` exclusively.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Session persistence | Custom token storage | Firebase Auth (built-in) | Firebase Auth persists tokens in EncryptedSharedPreferences automatically |
| Google Sign-In UI | Custom OAuth WebView | Credential Manager API | Account picker UI, biometric auth, passkey support are built-in |
| Anonymous-to-account linking | Manual UID copy | `currentUser.linkWithCredential()` | Firebase handles UID merge and data continuity atomically |
| Back stack state | Custom navigation manager | Navigation3 `SnapshotStateList` | Compose state-native; integrates with `rememberSaveable` and process death |
| Locale persistence | Custom SharedPreferences wrapper | DataStore + `AppCompatDelegate` | DataStore is coroutine-native; `AppCompatDelegate` handles RTL and config change edge cases |
| DI wiring | Manual Singleton factory | Hilt `@Singleton` + `@HiltViewModel` | Hilt handles scoped lifecycle, ViewModel factory creation, test replacement |
| Auth state observation | Polling `currentUser` | `FirebaseAuth.AuthStateListener` wrapped in `callbackFlow` | Push-based; immediate auth change notification; no polling |

**Key insight:** Firebase Auth handles the hardest parts of authentication — token refresh, offline persistence, account linking state machine — out of the box. The data layer should be a thin wrapper, not reimplementing these mechanics.

---

## Common Pitfalls

### Pitfall 1: KSP Version Must Match Kotlin Version

**What goes wrong:** Build fails with `KSP can only be used on Kotlin 2.x.x. Your current Kotlin version is...` or `NoSuchMethodError`.

**Why it happens:** KSP version has a tight coupling to the Kotlin compiler version. `ksp = "2.3.4"` works with `kotlin = "2.3.x"` but not `kotlin = "2.4.x"`.

**How to avoid:** The KSP version format is `{kotlin-version}-{ksp-patch}`. Confirm the KSP version matches the Kotlin version from the [KSP releases page](https://github.com/google/ksp/releases) before locking versions.

**Warning signs:** Gradle sync errors mentioning "symbol not found" or "incompatible KSP version."

### Pitfall 2: Navigation3 NavEntry Requires `rememberViewModelStoreNavEntryDecorator`

**What goes wrong:** ViewModels are recreated on every recomposition instead of surviving navigation, causing auth state to reset or UI state to be lost.

**Why it happens:** Navigation3 does not manage ViewModel lifecycle by default — it's opt-in via a decorator.

**How to avoid:** Always add `rememberViewModelStoreNavEntryDecorator()` to `NavDisplay`'s `entryDecorators` list.

**Warning signs:** `hiltViewModel()` returns a fresh instance every time the screen is entered from the back stack.

### Pitfall 3: Google Sign-In Credential Manager Requires Context (Not ApplicationContext)

**What goes wrong:** `GetCredentialException: TYPE_NO_CREDENTIAL` or the bottom sheet doesn't appear.

**Why it happens:** `CredentialManager.getCredential()` needs an `Activity` context to show the system bottom sheet. Passing `ApplicationContext` silently fails.

**How to avoid:** Pass the `Activity` context from the composable via `LocalContext.current` (which resolves to the activity in a single-activity setup). In the ViewModel, use a `CredentialRequest` helper that accepts the context as a parameter.

**Warning signs:** No bottom sheet appears; `GetCredentialException` with `TYPE_USER_CANCELED` or empty credential types.

### Pitfall 4: Firebase Auth State Listener Race on Process Restart

**What goes wrong:** App flashes the auth screen for 200-400ms before redirecting to Home, even when user is logged in.

**Why it happens:** Firebase Auth restores the session asynchronously. The first emission from `AuthStateListener` is null (not yet restored), causing NavDisplay to show the auth screen, then a second emission arrives with the restored user.

**How to avoid:** Show a loading/splash composable until the first non-null auth state emission (or a timeout of ~1 second). Don't treat `null` as "definitely logged out" until Firebase has completed its initialization check.

**Warning signs:** Brief flash of login screen on app launch for authenticated users.

### Pitfall 5: AppCompatDelegate.setApplicationLocales() Must Happen Before super.onCreate() Completes

**What goes wrong:** Language change applies on the NEXT launch, not immediately, when using autoStoreLocales.

**Why it happens:** Without `android:autoStoreLocales="true"` in the manifest, you must apply the stored locale before `super.onCreate()` creates views. The DataStore read is async, which conflicts with the synchronous startup expectation.

**How to avoid:** Use `android:autoStoreLocales="true"` in the manifest (API 33+) and also manage DataStore persistence manually for API 21-32 backward compat. Alternatively, use a blocking runBlocking for the initial DataStore read only at startup (acceptable — this is a single read at process start).

**Warning signs:** Language change from Settings takes effect only after the app is killed and relaunched.

### Pitfall 6: Anonymous Auth and Guest Account Are NOT the Same Thing

**What goes wrong:** Guest user's Firestore data is lost after account creation, or security rules reject operations from the anonymous UID after linking.

**Why it happens:** Confusion between "anonymous sign-in" (Firebase Auth creates a real but ephemeral UID) and "guest mode" (user provides name/email without creating a Firebase account). This project's AUTH-05/AUTH-06 uses Firebase anonymous auth as the guest mechanism, which means the UID is valid for Firestore writes — but the Firestore security rules must allow anonymous users to write reservations.

**How to avoid:** Confirm Firestore rules explicitly permit `request.auth != null && request.auth.token.firebase.sign_in_provider == 'anonymous'` for guest-allowed operations. When linking (AUTH-06), the UID stays the same — no data migration needed.

**Warning signs:** Firestore writes rejected after anonymous sign-in; data disappears after account linking.

---

## Code Examples

### Application Class + Hilt

```kotlin
// Source: https://developer.android.com/training/dependency-injection/hilt-android
@HiltAndroidApp
class GiftRegistryApp : Application()
```

### Hilt ViewModel

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val signInEmail: SignInEmailUseCase,
    private val signUpEmail: SignUpUseCase,
    private val signInGoogle: SignInGoogleUseCase,
    private val signInAnonymous: SignInAnonymousUseCase
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = observeAuthState()
        .map { user -> user != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            signInEmail(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Unknown error") }
        }
    }
}
```

### Collecting State in Compose

```kotlin
// Source: https://developer.android.com/reference/kotlin/androidx/lifecycle/compose/package-summary
@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ... render based on uiState
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `GoogleSignInClient` (`play-services-auth`) | Credential Manager (`androidx.credentials`) | Deprecated 2024, removal ongoing 2025 | Must use Credential Manager for new projects; old API will be removed |
| Firebase KTX modules (`firebase-auth-ktx`) | Main Firebase modules with built-in Kotlin APIs | BoM 34.0.0 (July 2025) | Cannot upgrade past BoM 33 if using KTX imports |
| Navigation Compose 2.x | Navigation3 (`androidx.navigation3`) | Stable November 2025 | New projects use Nav3; Nav2 is legacy |
| KAPT annotation processing | KSP (Kotlin Symbol Processing) | Hilt 2.59 requires KSP + AGP 9 | KSP is 2x faster; KAPT still works but is the legacy path |
| `LiveData` in ViewModel | `StateFlow` + `collectAsStateWithLifecycle` | Standard since Kotlin coroutines matured | LiveData has no Compose integration; StateFlow is the modern contract |
| Manual locale with `createConfigurationContext` | `AppCompatDelegate.setApplicationLocales()` | API 33 (backported via AppCompat) | Per-app language system-aware; handles RTL, edge cases |

**Deprecated/outdated in this phase:**
- `firebase-auth-ktx` import: replaced by main module
- `kapt` Gradle configuration: replaced by `ksp` for Hilt 2.59+
- `hiltNavGraphViewModel()`: Use `hiltViewModel()` with `rememberViewModelStoreNavEntryDecorator()` in Nav3
- `Locale.setDefault()`: Use `AppCompatDelegate.setApplicationLocales()`

---

## Open Questions

1. **`hilt-navigation-compose` compatibility with Navigation3**
   - What we know: `hilt-navigation-compose:1.2.0` provides `hiltViewModel()` scoped to Navigation 2.x `NavBackStackEntry`. Navigation3 uses `rememberViewModelStoreNavEntryDecorator()` instead.
   - What's unclear: Whether `hilt-navigation-compose` is needed at all with Nav3, or whether standard `hiltViewModel()` from Hilt's Compose integration works with Nav3's entry decorator.
   - Recommendation: Try standard `hiltViewModel()` first (it should work in the Nav3 entry context). If ViewModel scoping breaks, add `hilt-navigation-compose` and investigate. The `nav3-recipes` GitHub repo is the reference for this.

2. **`assetlinks.json` package name placeholder**
   - What we know: Phase 1 established `assetlinks.json` with PLACEHOLDER values. STATE.md confirms this needs the package name from Phase 2.
   - What's unclear: The exact package name to use (the project doesn't have one yet).
   - Recommendation: Decide package name as part of scaffold setup (e.g., `com.example.giftregistry` for dev, a production name TBD). Update `hosting/assetlinks.json` as part of this phase.

3. **Firebase Emulator connection from Android emulator**
   - What we know: Firebase emulator runs on ports Auth=9099, Firestore=8080. Android emulator uses `10.0.2.2` as the host machine address.
   - What's unclear: Whether an additional `network_security_config.xml` is needed to allow cleartext to the emulator host.
   - Recommendation: Add `network_security_config.xml` allowing cleartext to `10.0.2.2` in debug builds, and connect to emulator in `GiftRegistryApp` when `BuildConfig.DEBUG` is true.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Android Studio | IDE for Kotlin/Compose development | ✓ | AI-252.x (Meerkat Feature Drop / Otter — supports AGP 9.x and Compose) | — |
| Android SDK platforms | Compilation | ✓ | android-34, android-36 installed | — |
| Android SDK build tools | Compilation | ✓ | 34.0.0, 35.0.0, 36.1.0 | — |
| Android SDK platform-tools (adb) | Debugging/emulation | ✓ | Present in `~/Library/Android/sdk/platform-tools/` | — |
| System image for emulator | Running Android emulator | ✓ | android-36.1 available | Physical device |
| Java (JDK) | Gradle build | ✓ | OpenJDK 25 (2025-09-16) | — |
| Node.js | Firebase Emulator Suite | ✓ | 22.14.0 | — |
| Firebase CLI | Emulator startup, Functions deploy | ✓ | 15.13.0 | — |
| Google Play Services | Credential Manager (Google Sign-In) | ✗ on emulator by default | — | Use test emulator image with Play Store, or mock in tests |
| `google-services.json` | Firebase Android SDK connection | ✗ (not in repo) | — | Must be downloaded from Firebase Console and placed in `app/` |

**Missing dependencies with no fallback:**
- `google-services.json`: Must be obtained from Firebase Console and placed at `app/google-services.json`. The Firebase CLI `firebase apps:sdkconfig android` can download it.

**Missing dependencies with fallback:**
- Google Play Services on emulator: Use an AVD with "Google Play" system image (android-36.1 image available; need to verify Play variant) for Credential Manager testing. Alternatively, mock `CredentialManager` in unit tests.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Kotlin Coroutines Test + Turbine |
| Config file | Standard Android test runner (no separate config file needed — AGP handles it) |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` |

**Note:** Existing test infrastructure in `tests/rules/` is for Firestore security rules (TypeScript/Jest). Android unit tests live in `app/src/test/` and are separate.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Sign up with email succeeds; user is non-null after creation | unit (ViewModel + fake repo) | `./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest.signUp_*"` | ❌ Wave 0 |
| AUTH-02 | Sign in with email/password succeeds | unit (ViewModel + fake repo) | `./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest.signIn_email_*"` | ❌ Wave 0 |
| AUTH-03 | Google Sign-In flow produces idToken passed to Firebase | unit (ViewModel + mock) | `./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest.signIn_google_*"` | ❌ Wave 0 |
| AUTH-04 | Auth state flow emits non-null on restored session | unit (fake repo with stored user) | `./gradlew :app:testDebugUnitTest --tests "*.AuthRepositoryTest.session_*"` | ❌ Wave 0 |
| AUTH-05 | Anonymous sign-in creates a valid Firebase UID | unit (fake repo) | `./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest.signIn_anonymous_*"` | ❌ Wave 0 |
| AUTH-06 | `linkEmailToAnonymous` is called after reservation bottom sheet confirm | unit (ViewModel + fake repo) | `./gradlew :app:testDebugUnitTest --tests "*.AuthViewModelTest.link_account_*"` | ❌ Wave 0 |
| I18N-01 | Romanian strings present for all auth keys added in Phase 2 | manual inspection + lint | `./gradlew :app:lint` (missing translation lint) | ❌ Wave 0 |
| I18N-03 | DataStore persists locale and `AppCompatDelegate` is called with stored value | unit (DataStore in-memory test) | `./gradlew :app:testDebugUnitTest --tests "*.LocaleDataStoreTest.*"` | ❌ Wave 0 |

**Manual-only tests:**
- Google Sign-In bottom sheet appearance (requires physical device or emulator with Play Services)
- Activity recreation on language switch (UI test / manual verification)
- Auth state flash prevention on app restart (manual timing check)

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest :app:lint`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/.../AuthViewModelTest.kt` — covers AUTH-01, AUTH-02, AUTH-03, AUTH-05, AUTH-06
- [ ] `app/src/test/java/.../AuthRepositoryTest.kt` — covers AUTH-04
- [ ] `app/src/test/java/.../LocaleDataStoreTest.kt` — covers I18N-03
- [ ] `app/src/test/java/.../FakeAuthRepository.kt` — shared fake for ViewModel tests (per ARCH-04 decision)
- [ ] Build infrastructure: `app/build.gradle.kts` must include test dependencies (`junit`, `kotlinx-coroutines-test`, `turbine`)

---

## Sources

### Primary (HIGH confidence)
- [AndroidX Navigation3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) — confirmed 1.0.1 stable, February 2026, two artifacts: `navigation3-runtime` and `navigation3-ui`
- [Jetpack Navigation 3 announcement (Google I/O 2025)](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html) — NavDisplay architecture, back stack ownership model, auth gating pattern
- [Dagger releases (GitHub)](https://github.com/google/dagger/releases) — Hilt 2.59.2 is current stable; AGP 9 support added in 2.59; breaking change: AGP 9 minimum
- [Dagger Hilt Gradle Setup](https://dagger.dev/hilt/gradle-setup.html) — KSP and KAPT both supported; KSP shown first
- [androidx.credentials releases](https://developer.android.com/jetpack/androidx/releases/credentials) — 1.5.0 stable March 2025; minimum API 23; two required artifacts
- [Firebase anonymous auth docs](https://firebase.google.com/docs/auth/android/anonymous-auth) — anonymous sign-in + credential linking API confirmed
- [Firebase account linking docs](https://firebase.google.com/docs/auth/android/account-linking) — `linkWithCredential()` for guest-to-account conversion
- [Per-app language preferences guide](https://developer.android.com/guide/topics/resources/app-languages) — `AppCompatDelegate.setApplicationLocales()`, backward compat API 21+, `locales_config.xml`
- [nowinandroid `libs.versions.toml`](https://github.com/android/nowinandroid/blob/main/gradle/libs.versions.toml) — reference versions: Hilt 2.59, KSP 2.3.4, AGP 9.0.0

### Secondary (MEDIUM confidence)
- [Legacy GSI migration guide](https://developer.android.com/identity/sign-in/legacy-gsi-migration) — confirms `GoogleSignInClient` deprecated, Credential Manager is the replacement
- [Android Credential Manager developer guide](https://developers.google.com/identity/android-credential-manager) — `GetGoogleIdOption`, `CredentialManager.create(context)`, unified sign-in bottom sheet
- [Navigation3 recipes GitHub repo](https://github.com/android/nav3-recipes) — patterns for nested nav, ViewModel scoping with `rememberViewModelStoreNavEntryDecorator`
- Community guides on Navigation3 auth gating (2025) — multiple blog posts confirm the `LaunchedEffect(isLoggedIn)` + imperative back stack manipulation pattern

### Tertiary (LOW confidence)
- `googleid:1.1.1` version — confirmed on Maven Repository; no official release notes page but widely cited in 2025 blog posts

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions verified via official AndroidX releases, Dagger GitHub, nowinandroid reference project
- Architecture (3-layer clean arch): HIGH — well-established Android pattern with extensive official guidance
- Navigation3 auth gating: MEDIUM — library is new (stable Nov 2025), core pattern is clear, but edge cases in complex flows are not yet fully documented
- Credential Manager Google Sign-In: HIGH — official replacement for deprecated `GoogleSignInClient`; multiple verified sources
- Localization pattern: HIGH — official Android documentation, backward compat confirmed
- Pitfalls: HIGH — most based on official documentation and known version constraints

**Research date:** 2026-04-05
**Valid until:** 2026-07-05 (90 days — stable stack, but Navigation3 guidance may mature faster)
