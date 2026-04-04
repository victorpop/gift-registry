# Phase 2: Android Core + Auth - Research

**Researched:** 2026-04-04
**Domain:** Android (Kotlin, Jetpack Compose, Hilt DI, Navigation3, Firebase Auth)
**Confidence:** HIGH

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
| AUTH-01 | User can sign up with email and password | Firebase Auth `createUserWithEmailAndPassword()` — fully supported |
| AUTH-02 | User can log in with email and password | Firebase Auth `signInWithEmailAndPassword()` — fully supported |
| AUTH-03 | User can log in with Google OAuth | Credential Manager + `GoogleIdTokenCredential` → Firebase `signInWithCredential()` |
| AUTH-04 | User session persists across app restarts | Firebase Auth persists session to disk by default — no extra code required |
| AUTH-05 | Guest can access registry and reserve items by providing first name, last name, and email | Guest data model in domain layer; no Firebase anonymous auth required for this phase — guest state stored locally, used in Phase 4 reservation flow |
| AUTH-06 | Guest is offered account creation after completing a reservation | `ModalBottomSheet` component — exposed from this phase, triggered by Phase 4 |
| I18N-01 | App UI supports Romanian and English languages | `res/values/strings.xml` + `res/values-ro/strings.xml` already established in Phase 1 |
| I18N-03 | Language auto-detected from device/browser locale with manual override | `AppCompatDelegate.setApplicationLocales()` with DataStore persistence — works back to API 24 via AppCompat (API 21-23 edge case documented) |

</phase_requirements>

---

## Summary

This phase builds the complete Android application from scratch: Gradle project scaffold, 3-layer clean architecture, Hilt dependency injection, Navigation3 navigation, Firebase Authentication (email/password + Google OAuth + guest path), and multilingual support (Romanian/English with DataStore-persisted manual override).

The Android project directory currently contains only `res/values/strings.xml` and `res/values-ro/strings.xml` from Phase 1 — no `build.gradle.kts`, no Kotlin sources, no manifest. This phase starts the Android project from zero and wires all foundational systems.

The most important build-time constraint: **AGP 9.1.0 requires KSP instead of KAPT for Hilt**. The `org.jetbrains.kotlin.kapt` plugin is incompatible with AGP 9's built-in Kotlin. The `com.android.legacy-kapt` replacement also does not work with Hilt's Gradle plugin (tracked in google/dagger#4756). KSP is the only viable annotation processor path for AGP 9 + Hilt. Hilt has supported KSP fully since version 2.48; current stable is 2.56.2 as of 2026-04-04.

Google Sign-In with the legacy `com.google.android.gms:play-services-auth` API is deprecated (announced April 2024) and being removed from Google Play Services in 2025. The current approach is **Credential Manager** (`androidx.credentials`) + the `googleid` library. This extracts a Google ID token which is then passed to `FirebaseAuth.signInWithCredential()`. The Credential Manager API requires an `Activity` context — not application context — for the account picker UI.

**Primary recommendation:** Bootstrap the Android Gradle project with AGP 9.1.0 + Kotlin 2.3.20 + KSP 2.3.6, implement clean architecture layers for auth with Hilt DI, wire Navigation3 with conditional navigation for auth gating, implement Firebase Auth (email + Google via Credential Manager), and implement `AppCompatDelegate.setApplicationLocales()` for language switching with DataStore persistence.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android Gradle Plugin | 9.1.0 | Build toolchain | Latest stable; built-in Kotlin; CLAUDE.md specifies 9.x |
| Kotlin | 2.3.20 | Primary language | Latest stable tooling release (March 16, 2026); CLAUDE.md specifies 2.3.x |
| KSP | 2.3.6 | Annotation processor | Replaces KAPT; mandatory with AGP 9 + Hilt; resolves AGP 9 circular dependency issues |
| Gradle wrapper | 9.3.1 | Build system | Required minimum for AGP 9.1.0 |
| JDK | 17 | Build JDK | Required minimum for AGP 9.1.0 |
| Compose BOM | 2026.03.00 | UI framework version management | Pins all Compose library versions; CLAUDE.md specifies this version |
| Firebase Android BoM | 34.11.0 | Firebase version management | KTX modules removed at v34; main modules used directly; CLAUDE.md specifies this version |
| Hilt | 2.56.2 | Dependency injection | Google-recommended DI; KSP-compatible since 2.48; latest stable verified 2026-04-04 |
| Navigation3 | 1.0.1 | Navigation | Stable Compose-first navigation; CLAUDE.md specifies this version |
| Material3 | via Compose BOM | Design system | Included in Compose BOM; CLAUDE.md specifies M3 throughout |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.credentials:credentials | 1.6.0-rc02 | Credential Manager base | Required for Google Sign-In via Credential Manager |
| androidx.credentials:credentials-play-services-auth | 1.6.0-rc02 | Play Services backend for credentials | Required alongside `credentials` for Google Sign-In on Android |
| com.google.android.libraries.identity.googleid:googleid | 1.2.0 | Google ID token extraction | Provides `GetGoogleIdOption` and `GoogleIdTokenCredential` |
| androidx.hilt:hilt-navigation-compose | 1.2.0 | `hiltViewModel()` in Compose | Required for `hiltViewModel()` calls inside `entryProvider` blocks |
| DataStore Preferences | 1.2.1 | Persistent local settings | Language preference storage; only local-only settings per CLAUDE.md |
| AppCompat | 1.7.0 | `AppCompatDelegate` for locale | Required for `setApplicationLocales()` backward compat to API 24 |
| firebase-auth | via BoM 34.11.0 | Email/password + Google auth | Main module pattern — no KTX suffix |
| material-icons-extended | via Compose BOM | Icon set | Visibility toggle icons for password fields |
| lifecycle-viewmodel-compose | via Compose BOM | `viewModel()` in composables | Included in Compose BOM |
| activity-compose | via Compose BOM | `setContent {}` + `LocalContext` | Entry point for Compose in Activity |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| KSP | KAPT | KAPT breaks with AGP 9 built-in Kotlin; KSP is mandatory |
| Credential Manager | play-services-auth legacy | Legacy API deprecated Apr 2024, removal in progress 2025 |
| `AppCompatDelegate.setApplicationLocales()` | Manual `Locale.setDefault()` + `Configuration` | Official API handles API-level differences; manual approach is fragile |
| Navigation3 1.0.1 | Navigation Compose 2.x | Nav2 is legacy; CLAUDE.md mandates Navigation3 for new projects |

**Installation — `gradle/libs.versions.toml`:**

```toml
[versions]
agp = "9.1.0"
kotlin = "2.3.20"
ksp = "2.3.6"
composeBom = "2026.03.00"
firebaseBom = "34.11.0"
hilt = "2.56.2"
hiltNavigationCompose = "1.2.0"
navigation3 = "1.0.1"
credentials = "1.6.0-rc02"
googleid = "1.2.0"
datastorePreferences = "1.2.1"
appcompat = "1.7.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
credentials-play-services-auth = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
googleid = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleid" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**Version verification performed 2026-04-04:**
- AGP 9.1.0: confirmed stable on dl.google.com/dl/android/maven2
- Kotlin 2.3.20: confirmed stable on repo1.maven.org (March 16, 2026 tooling release)
- KSP 2.3.6: confirmed latest on central.sonatype.com (February 17, 2026)
- Navigation3 1.0.1: confirmed stable on dl.google.com/dl/android/maven2 (1.1.0-rc01 also available but not stable)
- Hilt 2.56.2: confirmed on dagger.dev/hilt/gradle-setup.html
- Firebase BoM 34.11.0: confirmed per CLAUDE.md (March 2026)
- credentials 1.6.0-rc02: confirmed latest stable on dl.google.com/dl/android/maven2
- DataStore 1.2.1: confirmed stable on dl.google.com/dl/android/maven2

---

## Architecture Patterns

### Recommended Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/giftregistry/
│   │   │   ├── GiftRegistryApp.kt          # @HiltAndroidApp Application class
│   │   │   ├── MainActivity.kt             # @AndroidEntryPoint, single Activity
│   │   │   ├── data/
│   │   │   │   ├── auth/
│   │   │   │   │   └── FirebaseAuthRepository.kt  # Implements AuthRepository
│   │   │   │   └── preferences/
│   │   │   │       └── LanguagePreferencesDataStore.kt
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt             # Application-scope bindings
│   │   │   │   ├── DataModule.kt            # Repository bindings + Firebase
│   │   │   │   └── DomainModule.kt          # Use case provision (if needed)
│   │   │   ├── domain/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthRepository.kt    # Interface (no Firebase imports)
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── AuthUser.kt      # Domain model (not FirebaseUser)
│   │   │   │   │   │   ├── AuthState.kt     # Loading / Authenticated / Unauthenticated
│   │   │   │   │   │   └── GuestUser.kt     # firstName, lastName, email
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── SignInWithEmailUseCase.kt
│   │   │   │   │       ├── SignUpWithEmailUseCase.kt
│   │   │   │   │       ├── SignInWithGoogleUseCase.kt
│   │   │   │   │       └── ObserveAuthStateUseCase.kt
│   │   │   │   └── preferences/
│   │   │   │       └── LanguagePreferencesRepository.kt  # Interface
│   │   │   └── ui/
│   │   │       ├── navigation/
│   │   │       │   ├── AppNavKeys.kt        # @Serializable nav key objects
│   │   │       │   └── AppNavigation.kt     # NavDisplay + auth state conditional
│   │   │       ├── auth/
│   │   │       │   ├── AuthScreen.kt        # Tabbed sign-in / create-account
│   │   │       │   ├── AuthViewModel.kt     # @HiltViewModel
│   │   │       │   └── components/
│   │   │       │       └── GuestConversionSheet.kt
│   │   │       ├── home/
│   │   │       │   └── HomeScreen.kt        # Placeholder for Phase 3
│   │   │       └── settings/
│   │   │           ├── SettingsScreen.kt    # Language picker
│   │   │           └── SettingsViewModel.kt # @HiltViewModel
│   │   ├── res/
│   │   │   ├── values/strings.xml           # Phase 1 base + Phase 2 additions
│   │   │   └── values-ro/strings.xml        # Romanian translations
│   │   └── AndroidManifest.xml
│   └── test/
│       └── java/com/giftregistry/
│           ├── auth/
│           │   ├── FakeAuthRepository.kt    # Test double
│           │   └── AuthViewModelTest.kt
│           └── settings/
│               └── SettingsViewModelTest.kt
├── build.gradle.kts
gradle/
├── libs.versions.toml
└── wrapper/
    └── gradle-wrapper.properties            # Gradle 9.3.1
settings.gradle.kts
build.gradle.kts                             # Root (plugin declarations, apply false)
```

### Pattern 1: AGP 9 build.gradle.kts (App Module)

**What:** AGP 9 uses built-in Kotlin — no separate `org.jetbrains.kotlin.android` plugin. Compose compiler plugin replaces the old `composeOptions.kotlinCompilerExtensionVersion`. KSP replaces KAPT.
**When to use:** All new Android projects with AGP 9+.

```kotlin
// Source: https://developer.android.com/build/migrate-to-built-in-kotlin
// Source: https://dagger.dev/hilt/gradle-setup.html
plugins {
    alias(libs.plugins.android.application)
    // NO org.jetbrains.kotlin.android — AGP 9 built-in Kotlin handles this
    alias(libs.plugins.compose.compiler)   // org.jetbrains.kotlin.plugin.compose
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)  // for @Serializable nav keys
}

android {
    namespace = "com.giftregistry"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.giftregistry"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
        buildConfig = true  // needed for BuildConfig.DEBUG emulator gating
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")  // collectAsStateWithLifecycle
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.datastore.preferences)
    implementation(libs.appcompat)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation(libs.hilt.android) { artifact { type = "testing" } }
    kspTest(libs.hilt.compiler)
}
```

### Pattern 2: Navigation3 with Auth State Gating (ARCH-02)

**What:** `NavDisplay` backed by a `mutableStateListOf` back stack. Auth state from `ObserveAuthStateUseCase` drives conditional navigation — when unauthenticated, `AuthKey` is on the stack; after sign-in, it is replaced with `HomeKey`. This is the "conditional navigation" recipe from nav3-recipes.
**When to use:** App-level navigation root. Phase 3 extends this graph with registry screens.

```kotlin
// Source: https://developer.android.com/guide/navigation/navigation-3
// Source: https://github.com/android/nav3-recipes (conditional recipe)

// AppNavKeys.kt
@Serializable data object AuthKey
@Serializable data object HomeKey
@Serializable data object SettingsKey

// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply persisted locale BEFORE super.onCreate()
        val storedLocale = runBlocking { /* read DataStore */ }
        if (storedLocale != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(storedLocale))
        }
        super.onCreate(savedInstanceState)
        setContent {
            val authState by authViewModel.authState.collectAsStateWithLifecycle()
            val backStack = remember { mutableStateListOf<Any>(AuthKey) }

            LaunchedEffect(authState) {
                when (authState) {
                    is AuthState.Authenticated -> {
                        if (backStack.lastOrNull() !is HomeKey::class) {
                            backStack.clear(); backStack.add(HomeKey)
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        backStack.clear(); backStack.add(AuthKey)
                    }
                    is AuthState.Loading -> { /* wait */ }
                }
            }

            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<AuthKey> { AuthScreen() }
                    entry<HomeKey> { HomeScreen() }
                    entry<SettingsKey> {
                        val vm = hiltViewModel<SettingsViewModel>()
                        SettingsScreen(viewModel = vm, onBack = { backStack.removeLastOrNull() })
                    }
                }
            )
        }
    }
}
```

### Pattern 3: Hilt Module Organization (ARCH-03)

**What:** Three Hilt modules aligned with clean architecture layers. `DataModule` binds repository implementations to their interfaces. `AppModule` provides Firebase and DataStore singletons.

```kotlin
// Source: https://developer.android.com/training/dependency-injection/hilt-android

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun bindLanguagePrefs(impl: LanguagePreferencesDataStore): LanguagePreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().also {
        if (BuildConfig.DEBUG) {
            it.useEmulator("10.0.2.2", 9099)  // Firebase Auth emulator; see Pitfall 3
        }
    }

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferencesDataStore.get(ctx)
}
```

### Pattern 4: Google Sign-In via Credential Manager (AUTH-03)

**What:** Extract Google ID token using Credential Manager, then exchange for a Firebase credential. Replaces deprecated `play-services-auth` legacy API.
**When to use:** Every Google Sign-In invocation from the auth screen.

```kotlin
// Source: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
// Firebase integration: https://firebase.google.com/docs/auth/android/google-signin

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val appContext: Context,
) : AuthRepository {

    private val credentialManager = CredentialManager.create(appContext)

    // activityContext MUST be an Activity — not application context
    suspend fun signInWithGoogle(activityContext: Context): Result<AuthUser> {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // allow selecting new accounts
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential as? CustomCredential
                ?: return Result.failure(Exception("Unexpected credential type"))
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(Exception("Not a Google credential"))
            }
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken, null
            )
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            Result.success(authResult.user!!.toDomain())
        } catch (e: GetCredentialException) {
            Result.failure(e)
        }
    }
}
```

**CRITICAL:** `CredentialManager.getCredential()` requires an `Activity` context to anchor the account picker bottom sheet. Pass it from the composable via `LocalContext.current` at call time. Do not store Activity context in the ViewModel.

### Pattern 5: Language Switching (I18N-UX-02 / I18N-UX-03)

**What:** `AppCompatDelegate.setApplicationLocales()` sets the app locale and triggers activity recreation automatically. DataStore persists the chosen locale tag. On startup, the stored locale is read synchronously before `super.onCreate()`.

```kotlin
// Source: https://developer.android.com/guide/topics/resources/app-languages

// SettingsViewModel:
fun onLanguageSelected(tag: String) {  // tag = "en" or "ro"
    viewModelScope.launch {
        languagePrefsRepo.setLanguageTag(tag)
        // setApplicationLocales triggers Activity.recreate() automatically on API 33+
        // On API 24-32, AppCompat handles this via configuration override + recreate
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}

// MainActivity.onCreate() — BEFORE super.onCreate():
override fun onCreate(savedInstanceState: Bundle?) {
    val storedLocale = runBlocking { languagePrefsRepo.getLanguageTag() }
    if (storedLocale != null) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(storedLocale))
    }
    super.onCreate(savedInstanceState)
    // ...
}
```

**runBlocking is acceptable here:** This is the one legitimate use — reading the initial locale synchronously before the Activity inflates its resources. Do not use `runBlocking` elsewhere.

### Pattern 6: Domain Layer Contracts (ARCH-01)

The domain layer must contain zero Firebase imports. All Firebase types are mapped at the data layer boundary.

```kotlin
// domain/auth/AuthRepository.kt — no Firebase imports
interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signOut()
}

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
}

data class AuthUser(val uid: String, val email: String?, val displayName: String?)

// domain/auth/model/GuestUser.kt
data class GuestUser(val firstName: String, val lastName: String, val email: String)
```

### Anti-Patterns to Avoid

- **`import com.google.firebase.*` in `domain/` package:** Domain must be Firebase-free. Firestore/Auth types stay in `data/` only.
- **`org.jetbrains.kotlin.android` plugin in build files with AGP 9:** Redundant at minimum, causes build failure with built-in Kotlin enabled.
- **KAPT with AGP 9:** `org.jetbrains.kotlin.kapt` is incompatible; `com.android.legacy-kapt` also breaks Hilt. Use `ksp(...)` and `kspTest(...)`.
- **`CredentialManager.getCredential()` with application context:** Crashes at runtime — Activity context required.
- **`play-services-auth` `GoogleSignInClient`:** Deprecated API; use Credential Manager.
- **`setApplicationLocales()` after `super.onCreate()`:** Locale change won't take effect until next Activity start.
- **Hardcoded strings in Kotlin/Compose:** Zero tolerance per CLAUDE.md. All text via `stringResource(R.string.*)`.
- **Missing `rememberViewModelStoreNavEntryDecorator()` in NavDisplay:** Without it, ViewModels are not scoped to NavEntries and `hiltViewModel()` will not work correctly.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Session persistence across restarts | Custom token storage | Firebase Auth (persists by default) | Firebase SDK persists auth state to encrypted storage; rehydrates on restart automatically |
| Google Sign-In flow | Custom OAuth PKCE | Credential Manager + googleid library | Handles account picker UI, nonce, token validation, account selection UX |
| DI lifecycle management | Manual singleton registry | Hilt | Correct scoping (Singleton, ViewModelComponent, ActivityComponent) is non-trivial |
| Back stack process death restoration | Manual Parcelable stack | Navigation3 `@Serializable` keys + `rememberNavBackStack` | Handles process death and config changes with zero boilerplate |
| Locale persistence and application | Manual `Locale.setDefault()` + config override | `AppCompatDelegate.setApplicationLocales()` | Official API handles API-level differences, system settings integration |
| Async preferences | SharedPreferences callbacks | DataStore Preferences | Coroutine-native, type-safe, no blocking main thread reads (except the one sanctioned use in `onCreate`) |

**Key insight:** Firebase Auth and Credential Manager handle nearly all auth session complexity. The repository pattern isolates this; ViewModels and use cases never call Firebase directly.

---

## Runtime State Inventory

Step 2.5: SKIPPED — this is a greenfield Android project phase with no rename/refactor/migration. No runtime state to inventory.

---

## Common Pitfalls

### Pitfall 1: Hilt + AGP 9 Build Failure
**What goes wrong:** Build fails with "Cannot use 'org.jetbrains.kotlin.kapt' with built-in Kotlin support", or Hilt fails to apply with `com.android.legacy-kapt`.
**Why it happens:** AGP 9 enables built-in Kotlin by default, making `kotlin-kapt` incompatible. `com.android.legacy-kapt` was intended as a bridge but Hilt's Gradle plugin does not support it (google/dagger#4756).
**How to avoid:** Use KSP from day one. Apply `com.google.devtools.ksp` plugin, use `ksp(libs.hilt.compiler)` instead of `kapt(...)`.
**Warning signs:** Build error mentioning "kapt" or "annotation processor" + "built-in Kotlin".

### Pitfall 2: Google Sign-In Activity Context Crash
**What goes wrong:** `GetCredentialException` or `IllegalStateException` calling `credentialManager.getCredential()`.
**Why it happens:** Credential Manager needs an Activity context to anchor the account picker bottom sheet. Application context lacks a window.
**How to avoid:** Pass `LocalContext.current` from the composable to the use case call. Do not hold Activity context in the ViewModel field.
**Warning signs:** Crash only on Google Sign-In button tap, email/password works fine.

### Pitfall 3: Firebase Auth Emulator Not Connected
**What goes wrong:** Auth operations hit production Firebase instead of the emulator during local development.
**Why it happens:** `useEmulator()` must be called before any auth operations; `10.0.2.2` (not `localhost`) is the correct host from an Android Emulator to reach the host machine.
**How to avoid:** Call `firebaseAuth.useEmulator("10.0.2.2", 9099)` in `AppModule.provideFirebaseAuth()` when `BuildConfig.DEBUG` is true. Enable `buildConfig = true` in the `android {}` block.
**Warning signs:** Auth operations succeed but users appear in the production Firebase console; or auth fails with "network error" on emulator.

### Pitfall 4: Language Override Delayed on First App Launch
**What goes wrong:** App shows device locale on first launch, applies stored locale only after the next restart.
**Why it happens:** If the locale is set asynchronously in `onCreate`, the Activity has already inflated resources in the device locale before the stored preference is read.
**How to avoid:** Use `runBlocking` synchronously to read the stored locale before `super.onCreate()`. This is the sanctioned one-time use.
**Warning signs:** Language picker changes language only after the next app restart, not immediately.

### Pitfall 5: Navigation3 ViewModel Scoping Without entryDecorators
**What goes wrong:** ViewModel is shared across all screens, or it re-creates on every recomposition.
**Why it happens:** Without `rememberViewModelStoreNavEntryDecorator()` in `NavDisplay`'s `entryDecorators`, there is no ViewModelStore per NavEntry. `hiltViewModel()` depends on this being present.
**How to avoid:** Always include both `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()` in `NavDisplay`. Call `hiltViewModel()` inside `entryProvider { entry<Key> { ... } }` blocks.
**Warning signs:** `AuthViewModel.init` block runs multiple times; auth state resets unexpectedly during navigation.

### Pitfall 6: Firebase KTX Module Usage
**What goes wrong:** Build fails with "Could not resolve `com.google.firebase:firebase-auth-ktx`".
**Why it happens:** KTX modules were removed from Firebase BoM v34.0.0 (July 2025). Kotlin APIs are now in the main modules.
**How to avoid:** Use `firebase-auth` (not `firebase-auth-ktx`), `firebase-firestore` (not `firebase-firestore-ktx`), etc. per CLAUDE.md.
**Warning signs:** Dependency resolution failure with KTX artifact names in the error.

### Pitfall 7: `google-services.json` Missing
**What goes wrong:** Build fails with "File google-services.json is missing from module root folder".
**Why it happens:** The Android app must be registered in the Firebase console to generate `google-services.json`. The file must be in `app/google-services.json`. This was not created in Phase 1 (Firebase-only phase).
**How to avoid:** Wave 0 must include a task to register the Android app in Firebase console with the `com.giftregistry` package name and download `google-services.json` to `app/`.
**Warning signs:** Build fails immediately with google-services plugin error.

---

## Code Examples

### Minimal `settings.gradle.kts`

```kotlin
// Source: standard AGP 9 project
pluginManagement {
    repositories {
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "gift-registry"
include(":app")
```

### Firebase Auth Emulator Connection

```kotlin
// Source: https://firebase.google.com/docs/emulator-suite/connect_auth
@Provides @Singleton
fun provideFirebaseAuth(): FirebaseAuth {
    return FirebaseAuth.getInstance().also { auth ->
        if (BuildConfig.DEBUG) {
            // 10.0.2.2 = localhost alias from Android Emulator; use actual IP for physical device
            auth.useEmulator("10.0.2.2", 9099)
        }
    }
}
```

### DataStore Language Preference

```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/datastore
object PreferencesKeys {
    val LANGUAGE_TAG = stringPreferencesKey("language_tag")
}

class LanguagePreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : LanguagePreferencesRepository {
    override fun observeLanguageTag(): Flow<String?> =
        dataStore.data.map { it[PreferencesKeys.LANGUAGE_TAG] }

    override suspend fun setLanguageTag(tag: String) {
        dataStore.edit { it[PreferencesKeys.LANGUAGE_TAG] = tag }
    }

    override suspend fun getLanguageTag(): String? =
        dataStore.data.first()[PreferencesKeys.LANGUAGE_TAG]
}
```

### String Resource Expansion (Phase 2 additions to Phase 1 base)

New keys needed per UI-SPEC.md (add to both `values/strings.xml` and `values-ro/strings.xml`):

```xml
<!-- Auth — Phase 2 additions -->
<string name="auth_sign_in_button">Sign In</string>
<string name="auth_sign_up_button">Create Account</string>
<string name="auth_google_sign_in_button">Sign in with Google</string>
<string name="auth_confirm_password_label">Confirm Password</string>
<string name="auth_or_divider">OR</string>
<string name="auth_password_show">Show password</string>
<string name="auth_password_hide">Hide password</string>
<string name="auth_guest_conversion_title">Save your reservation?</string>
<string name="auth_guest_conversion_body">Create a free account to track your reservation and receive updates.</string>
<string name="auth_guest_conversion_create">Create Account</string>
<string name="auth_guest_conversion_dismiss">Maybe later</string>
<string name="auth_settings_title">Settings</string>
<string name="auth_settings_language_label">Language</string>
<string name="auth_settings_language_english">English</string>
<string name="auth_settings_language_romanian">Română</string>
<string name="settings_language_confirm">Change Language</string>
<string name="auth_sign_in_no_account_hint">New here? Switch to Create Account to get started.</string>
<string name="auth_error_invalid_credentials">Incorrect email or password. Check your details and try again.</string>
<string name="auth_error_email_exists">An account with this email already exists. Try signing in.</string>
<string name="auth_error_weak_password">Password must be at least 8 characters.</string>
<string name="auth_error_network">No connection. Check your internet and try again.</string>
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `org.jetbrains.kotlin.android` plugin | AGP 9 built-in Kotlin (no plugin needed) | AGP 9.0 (Jan 2026) | Remove plugin; KSP migration required |
| KAPT annotation processor | KSP | AGP 9 made KAPT incompatible | KSP is mandatory for Hilt with AGP 9 |
| `firebase-auth-ktx` separate module | `firebase-auth` main module (Kotlin APIs merged) | Firebase BoM v34.0.0 (July 2025) | Simpler dependencies; KTX suffix no longer valid |
| `play-services-auth` GoogleSignInClient | `androidx.credentials` + `googleid` (Credential Manager) | Deprecated Apr 2024, removal in progress 2025 | New implementation required for all Google Sign-In |
| Navigation Compose 2.x (NavController) | Navigation3 (NavDisplay + back stack list) | Navigation3 1.0.0 stable Nov 2025 | Explicit back stack state; no NavController reference threading |
| `composeOptions { kotlinCompilerExtensionVersion }` | `org.jetbrains.kotlin.plugin.compose` Gradle plugin | Compose BOM 2025+ | Compose compiler version tied to Kotlin version via plugin |

**Deprecated/outdated (do not use):**
- `firebase-auth-ktx`: Removed in BoM v34. Use `firebase-auth` directly.
- `play-services-auth` `GoogleSignInClient`: Deprecated. Use Credential Manager.
- Navigation Compose 2.x for new projects: Legacy. CLAUDE.md mandates Navigation3 1.0.1.
- KAPT with AGP 9: Incompatible. Use KSP.

---

## Open Questions

1. **`AppCompatDelegate.setApplicationLocales()` on API 21–23**
   - What we know: AppCompat supports this API with backward compat to API 24; behavior on API 21–23 may require an `attachBaseContext()` override in the Application class
   - What's unclear: Whether the absence of `attachBaseContext()` causes a silent failure or simply falls back to device locale on API 21–23 devices
   - Recommendation: Implement without `attachBaseContext()` first, test on API 23 emulator during Wave 0. If broken, add `AppCompatDelegate.setDefaultNightMode()` + config override in `Application.attachBaseContext()`. Document the API 21–23 limitation in code comments.

2. **Google Sign-In web client ID source**
   - What we know: Credential Manager requires the web OAuth client ID from Firebase console (not the Android client ID); this ID goes into `BuildConfig.GOOGLE_WEB_CLIENT_ID` or `google-services.json`
   - What's unclear: The Firebase project's OAuth client IDs were not configured in Phase 1 (Firebase-only phase); Google Sign-In provider must be enabled in Firebase Auth console
   - Recommendation: Wave 0 task must include enabling Google Sign-In in Firebase Auth console and verifying `google-services.json` contains the client ID. Add `GOOGLE_WEB_CLIENT_ID` as a `buildConfigField` derived from `google-services.json`.

3. **`google-services.json` and Android app registration**
   - What we know: Firebase Android apps require this file in `app/`; it is generated after registering the Android app in Firebase console with the package name and optionally SHA-1 fingerprint; STATE.md notes the package name is needed from Phase 2
   - What's unclear: The exact package name to use (assumed `com.giftregistry`)
   - Recommendation: Confirm package name in Wave 0. Register Android app in Firebase console. Download `google-services.json` to `app/`. This is a hard prerequisite before any Firebase SDK call can succeed on device/emulator.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java / JDK 17+ | AGP 9.1.0 build | Yes | OpenJDK 25 (exceeds minimum) | — |
| Firebase CLI | Auth emulator (`firebase emulators:start`) | Yes | 15.13.0 | — |
| Android SDK (build-tools, platform) | Compilation | Unknown | — | Install via Android Studio `sdkmanager` |
| Android Studio Meerkat (2024.3+) | IDE + Compose preview | Unknown | — | Any editor; Compose preview requires AS |
| ADB | Device/emulator control | No | — | ADB bundled in Android Studio; use AS terminal |
| Gradle wrapper | Build execution | Auto-download | 9.3.1 (auto) | Wrapper downloads on first build |
| `google-services.json` | Firebase SDK initialization | No (not yet generated) | — | Must generate from Firebase console — no fallback |

**Missing dependencies with no fallback:**
- Android SDK — not detected in system PATH. Must be installed via Android Studio or `sdkmanager` before the Android project can compile.
- `google-services.json` — must be generated from Firebase console after registering the Android app. This blocks all Firebase calls.

**Missing dependencies with fallback:**
- ADB in system PATH — Android Studio includes ADB internally; use AS terminal for ADB commands during development.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Kotlin Coroutines Test (unit); Hilt Android Testing (instrumented) |
| Config file | Declared in `app/build.gradle.kts` test dependencies; no separate config file |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:test` |

**Note:** Unit tests run without a device (FakeAuthRepository pattern per ARCH-04). Instrumented tests require a connected device/emulator and are not required for this phase's test gate.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | `SignUpWithEmailUseCase` returns `AuthUser` on success | unit | `./gradlew :app:testDebugUnitTest --tests "*.auth.SignUpUseCaseTest"` | Wave 0 |
| AUTH-02 | `SignInWithEmailUseCase` returns `AuthUser` on valid credentials | unit | `./gradlew :app:testDebugUnitTest --tests "*.auth.SignInUseCaseTest"` | Wave 0 |
| AUTH-03 | `AuthViewModel.signInWithGoogle()` emits `Authenticated` state (FakeRepo) | unit | `./gradlew :app:testDebugUnitTest --tests "*.auth.AuthViewModelTest"` | Wave 0 |
| AUTH-04 | `ObserveAuthStateUseCase` emits `Authenticated` after sign-in (FakeRepo) | unit | `./gradlew :app:testDebugUnitTest --tests "*.auth.AuthViewModelTest"` | Wave 0 |
| AUTH-05 | `GuestUser` model instantiates with firstName, lastName, email | unit | `./gradlew :app:testDebugUnitTest --tests "*.auth.GuestUserTest"` | Wave 0 |
| AUTH-06 | `GuestConversionSheet` renders + callbacks fire | manual smoke | n/a — visual composable | n/a |
| I18N-01 | All English string keys exist in Romanian strings.xml | unit (resource) | `./gradlew :app:testDebugUnitTest --tests "*.i18n.StringResourceCompletenessTest"` | Wave 0 |
| I18N-03 | `SettingsViewModel.onLanguageSelected("ro")` invokes locale setter | unit (FakePrefs) | `./gradlew :app:testDebugUnitTest --tests "*.settings.SettingsViewModelTest"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full unit test suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/.../auth/FakeAuthRepository.kt` — test double for AUTH-01 through AUTH-04
- [ ] `app/src/test/.../auth/AuthViewModelTest.kt` — covers AUTH-01, AUTH-02, AUTH-03, AUTH-04
- [ ] `app/src/test/.../auth/GuestUserTest.kt` — covers AUTH-05
- [ ] `app/src/test/.../settings/SettingsViewModelTest.kt` — covers I18N-03
- [ ] `app/src/test/.../i18n/StringResourceCompletenessTest.kt` — covers I18N-01
- [ ] JUnit 4 + coroutines-test + mockk test dependencies in `build.gradle.kts`
- [ ] `app/google-services.json` — prerequisite for Firebase SDK to initialize; register Android app in Firebase console

---

## Project Constraints (from CLAUDE.md)

| Directive | Enforcement |
|-----------|-------------|
| Java/Kotlin for Android, Firebase for backend only | No SQLite, no Ktor server, no Room |
| Firebase BoM 34.11.0 — no KTX modules | Use `firebase-auth` not `firebase-auth-ktx` |
| Kotlin 2.3.x | Use 2.3.20 (latest stable in 2.3.x line) |
| Compose BOM 2026.03.00 | Do not override individual Compose versions |
| Navigation3 1.0.1 (not Navigation Compose 2.x) | `androidx.navigation3:navigation3-ui:1.0.1` |
| Hilt (CLAUDE.md says 2.51.x) | Research found 2.56.2 is latest stable; use 2.56.2 — the CLAUDE.md value is a lower bound |
| Material3 components only (not M2) | `androidx.compose.material3` package throughout |
| All UI labels in strings.xml / values-ro/strings.xml | Zero hardcoded strings in Kotlin/Compose |
| Guest access without account creation | No forced registration for gift givers |
| DataStore only for local-only settings | Language preference is local-only — correct use |
| GSD workflow enforcement | Phase work via `/gsd:execute-phase` |

---

## Sources

### Primary (HIGH confidence)
- [developer.android.com/build/migrate-to-built-in-kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) — AGP 9 Kotlin migration, KAPT incompatibility confirmed
- [dagger.dev/hilt/gradle-setup.html](https://dagger.dev/hilt/gradle-setup.html) — Hilt 2.56.2 KSP setup confirmed
- [developer.android.com/guide/navigation/navigation-3](https://developer.android.com/guide/navigation/navigation-3) — Navigation3 API patterns
- [developer.android.com/guide/navigation/navigation-3/recipes/passingarguments](https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments) — `hiltViewModel()` + `rememberViewModelStoreNavEntryDecorator` in Navigation3
- [developer.android.com/identity/sign-in/credential-manager-siwg-implementation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation) — Credential Manager Google Sign-In dependencies and implementation
- [developer.android.com/guide/topics/resources/app-languages](https://developer.android.com/guide/topics/resources/app-languages) — `AppCompatDelegate.setApplicationLocales()` pattern
- [firebase.google.com/docs/emulator-suite/connect_auth](https://firebase.google.com/docs/emulator-suite/connect_auth) — `useEmulator("10.0.2.2", 9099)` pattern
- [developer.android.com/build/releases/agp-9-1-0-release-notes](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — AGP 9.1.0 requires Gradle 9.3.1, JDK 17
- dl.google.com/dl/android/maven2 (maven-metadata.xml endpoints) — version verification for AGP 9.1.0, Navigation3 1.0.1, DataStore 1.2.1, Credentials 1.6.0-rc02
- repo1.maven.org/maven2/org/jetbrains/kotlin — Kotlin 2.3.20 stable confirmed
- central.sonatype.com — KSP 2.3.6 latest stable confirmed

### Secondary (MEDIUM confidence)
- [github.com/google/dagger/issues/4756](https://github.com/google/dagger/issues/4756) — Hilt incompatibility with `com.android.legacy-kapt` confirmed in Dagger issue tracker
- [github.com/android/nav3-recipes](https://github.com/android/nav3-recipes) — conditional navigation pattern for auth gating verified
- [kotlinlang.org/docs/releases.html](https://kotlinlang.org/docs/releases.html) — Kotlin 2.3.20 release date March 16, 2026 confirmed

### Tertiary (LOW confidence)
- Community Medium articles on Navigation3, Hilt KSP, and Credential Manager — used for corroboration only

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all library versions verified against Maven/Google Maven registries on 2026-04-04
- Architecture: HIGH — patterns sourced from official Android developer documentation and official nav3-recipes repository
- Pitfalls: HIGH (AGP/Hilt/KSP incompatibilities confirmed in official issue trackers) / MEDIUM (locale API 21–23 edge case — flagged as open question)

**Research date:** 2026-04-04
**Valid until:** 2026-07-04 (stable libraries; re-verify KSP and Credentials versions if planning is delayed beyond that date)
