---
phase: quick-260419-ubj
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/domain/preferences/OnboardingPreferencesRepository.kt
  - app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt
  - app/src/main/java/com/giftregistry/di/DataModule.kt
  - app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt
  - app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
autonomous: true
requirements:
  - ONBOARD-01
  - ONBOARD-02
  - ONBOARD-03

must_haves:
  truths:
    - "On first unauthenticated launch, the onboarding carousel is the entry point (not AuthScreen directly)"
    - "User can horizontally swipe through 3 onboarding slides, each with the exact copy specified"
    - "The 4th pager page is the existing AuthScreen; swiping past slide 3 reveals it seamlessly"
    - "Tapping Skip on slides 1-3 jumps directly to the AuthScreen (page 4)"
    - "Page indicator dots are visible on slides 1-3 and hidden on page 4 (auth)"
    - "Once onboarding is marked seen (Skip tapped, swiped into auth, or user authenticated), relaunching the app skips the carousel and lands on AuthScreen"
    - "Authenticated users never see the carousel, even on fresh launch (session restore path)"
    - "Signing out does NOT re-trigger the carousel — onboarding_seen persists"
    - "All slide copy renders in Romanian when device locale is Romanian"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/domain/preferences/OnboardingPreferencesRepository.kt"
      provides: "Domain interface for onboarding_seen flag"
      exports: ["OnboardingPreferencesRepository"]
    - path: "app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt"
      provides: "DataStore Preferences implementation, name='onboarding_prefs'"
      contains: "name = \"onboarding_prefs\""
    - path: "app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt"
      provides: "HorizontalPager carousel (pageCount=4, page 4 = AuthScreen)"
      min_lines: 80
    - path: "app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt"
      provides: "Hilt ViewModel exposing onboardingSeen StateFlow + markSeen() action"
    - path: "app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt"
      provides: "New OnboardingKey nav key"
      contains: "OnboardingKey"
    - path: "app/src/main/res/values/strings.xml"
      provides: "English slide copy"
      contains: "onboarding_slide_1_title"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "Romanian slide copy"
      contains: "onboarding_slide_1_title"
  key_links:
    - from: "AppNavigation.kt LaunchedEffect(authUiState)"
      to: "OnboardingKey vs AuthKey decision"
      via: "OnboardingViewModel.onboardingSeen flag read before setting Unauthenticated backStack"
      pattern: "OnboardingKey|onboardingSeen"
    - from: "OnboardingScreen.kt HorizontalPager page 4"
      to: "AuthScreen() composable"
      via: "direct composable call inside HorizontalPager page slot"
      pattern: "AuthScreen\\("
    - from: "OnboardingScreen Skip button + pager-settled-on-page-4"
      to: "OnboardingViewModel.markSeen()"
      via: "onComplete callback writes onboarding_seen=true to DataStore"
      pattern: "markSeen\\(\\)|onboarding_seen"
    - from: "DataModule.kt"
      to: "OnboardingPreferencesDataStore Hilt binding"
      via: "@Binds @Singleton OnboardingPreferencesRepository"
      pattern: "bindOnboardingPreferences"
---

<objective>
Add a 3-slide onboarding carousel that appears once, before the Auth screen, on first launch.
The 4th HorizontalPager page IS the existing AuthScreen — swiping past slide 3 transitions
seamlessly into login/sign-up. A "Skip" button on slides 1-3 jumps directly to the auth page.
The carousel never reappears once any of these happen: user swipes/taps through to auth,
user successfully authenticates, or user signs out and back in.

Purpose: First-time users get a quick orientation to what the app does before being asked to
sign in. Returning users go straight to auth. Authenticated users are unaffected.

Output: OnboardingScreen (HorizontalPager 4 pages), OnboardingViewModel, OnboardingPreferencesDataStore/Repository,
Hilt binding, new OnboardingKey nav entry, new strings in EN + RO.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@CLAUDE.md
@.planning/STATE.md
@.planning/PROJECT.md

<!-- Existing patterns to mirror exactly -->
@app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt
@app/src/main/java/com/giftregistry/data/preferences/GuestPreferencesDataStore.kt
@app/src/main/java/com/giftregistry/domain/preferences/LanguagePreferencesRepository.kt
@app/src/main/java/com/giftregistry/di/DataModule.kt

<!-- Navigation touchpoints -->
@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
@app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt
@app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt

<!-- Theme tokens (use these, do not hardcode colors/typography) -->
@app/src/main/java/com/giftregistry/ui/theme/Color.kt
@app/src/main/java/com/giftregistry/ui/theme/Type.kt

<!-- Localization targets -->
@app/src/main/res/values/strings.xml
@app/src/main/res/values-ro/strings.xml

<interfaces>
<!-- Key contracts the executor will rely on. No codebase exploration required. -->

From LanguagePreferencesRepository.kt (pattern to mirror for Onboarding):
```kotlin
interface LanguagePreferencesRepository {
    fun observeLanguageTag(): Flow<String?>
    suspend fun setLanguageTag(tag: String)
    suspend fun getLanguageTag(): String?
}
```

From LanguagePreferencesDataStore.kt (implementation pattern):
```kotlin
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class LanguagePreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : LanguagePreferencesRepository {
    private val languageTagKey = stringPreferencesKey("language_tag")
    override fun observeLanguageTag(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[languageTagKey] }
    override suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { prefs -> prefs[languageTagKey] = tag }
    }
    override suspend fun getLanguageTag(): String? =
        context.dataStore.data.first()[languageTagKey]
}
```

NOTE from GuestPreferencesDataStore.kt (CRITICAL — duplicate DataStore names crash at runtime):
The new DataStore MUST use name = "onboarding_prefs" and the Context extension property
MUST be named uniquely (e.g., `onboardingDataStore`). Do NOT reuse "user_prefs" or "guest_prefs".

From AuthUiState.kt:
```kotlin
sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(val uid: String, val isAnonymous: Boolean) : AuthUiState
}
```

From AppNavKeys.kt (add OnboardingKey alongside these):
```kotlin
@Serializable data object AuthKey
@Serializable data object HomeKey
// ... other keys
```

From AppNavigation.kt (the branching decision lives in the LaunchedEffect(authUiState) block;
Unauthenticated currently sets backStack to [AuthKey] — this is what needs to conditionally
use [OnboardingKey] instead when onboardingSeen == false).

From AuthScreen.kt: `@Composable fun AuthScreen(viewModel: AuthViewModel = hiltViewModel())`
— no parameters needed; can be called directly as page 4 of the pager.

Compose HorizontalPager (Foundation, already on classpath via Compose BOM 2026.03.00):
```kotlin
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

val pagerState = rememberPagerState(pageCount = { 4 })
HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
    when (page) {
        0, 1, 2 -> OnboardingSlide(...)
        3 -> AuthScreen()
    }
}
```
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: DataStore + domain repo + Hilt binding + nav key + strings</name>
  <files>
    app/src/main/java/com/giftregistry/domain/preferences/OnboardingPreferencesRepository.kt,
    app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt,
    app/src/main/java/com/giftregistry/di/DataModule.kt,
    app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt,
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml
  </files>
  <action>
All scaffolding needed for Task 2. Do everything below as a single coherent change.

1. **Domain interface** — create `OnboardingPreferencesRepository.kt` in `com.giftregistry.domain.preferences`:
   ```kotlin
   package com.giftregistry.domain.preferences

   import kotlinx.coroutines.flow.Flow

   interface OnboardingPreferencesRepository {
       fun observeOnboardingSeen(): Flow<Boolean>
       suspend fun isOnboardingSeen(): Boolean
       suspend fun setOnboardingSeen()
   }
   ```
   Pure Kotlin — NO Android/Firebase imports (mirrors LanguagePreferencesRepository convention per Phase 02 decision).

2. **DataStore implementation** — create `OnboardingPreferencesDataStore.kt` in `com.giftregistry.data.preferences`.
   Mirror `LanguagePreferencesDataStore.kt` line-by-line, with these exact substitutions:
   - Context extension property name: `onboardingDataStore` (NOT `dataStore` — would collide with `LanguagePreferencesDataStore`'s property; NOT `guestDataStore` — belongs to guest prefs).
   - DataStore name: `"onboarding_prefs"` (per Phase 04 decision: unique DataStore names are MANDATORY — duplicate names throw `IllegalStateException` at runtime).
   - Preference key: `booleanPreferencesKey("onboarding_seen")` — import `androidx.datastore.preferences.core.booleanPreferencesKey`.
   - `observeOnboardingSeen()` returns `Flow<Boolean>`; map absent key → `false`: `prefs[onboardingSeenKey] ?: false`.
   - `isOnboardingSeen()` suspend: `context.onboardingDataStore.data.first()[onboardingSeenKey] ?: false`.
   - `setOnboardingSeen()` suspend: `context.onboardingDataStore.edit { it[onboardingSeenKey] = true }`. No `setOnboardingSeen(false)` — onboarding is write-once-true, never reset.
   - Class annotated `@Singleton`, constructor `@Inject`, `@ApplicationContext private val context: Context`, implements `OnboardingPreferencesRepository`.

3. **Hilt binding** — edit `app/src/main/java/com/giftregistry/di/DataModule.kt`:
   - Add import: `import com.giftregistry.data.preferences.OnboardingPreferencesDataStore`
   - Add import: `import com.giftregistry.domain.preferences.OnboardingPreferencesRepository`
   - Add new `@Binds @Singleton` abstract function after `bindGuestPreferences`:
     ```kotlin
     @Binds
     @Singleton
     abstract fun bindOnboardingPreferences(impl: OnboardingPreferencesDataStore): OnboardingPreferencesRepository
     ```
   Do not touch other bindings.

4. **Nav key** — edit `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt`. Add one line:
   ```kotlin
   @Serializable data object OnboardingKey
   ```
   Place it directly under `@Serializable data object AuthKey`. Do not reorder existing keys.

5. **Strings (English)** — edit `app/src/main/res/values/strings.xml`. Insert a new section BEFORE the `<!-- Registry management (Phase 3) -->` comment (to group onboarding with auth):
   ```xml
       <!-- Onboarding carousel -->
       <string name="onboarding_slide_1_title">Create your list</string>
       <string name="onboarding_slide_1_body">Wishlist, personal shopping list, event gift registry — any list you want!</string>
       <string name="onboarding_slide_2_title">Add items from anywhere</string>
       <string name="onboarding_slide_2_body">Browse online, add a link, or add manually</string>
       <string name="onboarding_slide_3_title">The perfect gift, every time</string>
       <string name="onboarding_slide_3_body">Get the gift you want. Give the gift they want.</string>
       <string name="onboarding_skip">Skip</string>
       <string name="onboarding_next">Next</string>
       <string name="onboarding_get_started">Get Started</string>
   ```
   The exact body copy above is LITERAL — do not paraphrase. Use real em dash `—` (U+2014), not `--`.

6. **Strings (Romanian)** — edit `app/src/main/res/values-ro/strings.xml`. Insert the same section BEFORE the `<!-- Registry management (Phase 3) -->` comment, with idiomatic (not literal) RO translations:
   ```xml
       <!-- Onboarding carousel -->
       <string name="onboarding_slide_1_title">Creeaz&#259;-&#539;i lista</string>
       <string name="onboarding_slide_1_body">List&#259; de dorin&#539;e, list&#259; personal&#259; de cump&#259;r&#259;turi, registru pentru evenimente — orice list&#259; dore&#537;ti!</string>
       <string name="onboarding_slide_2_title">Adaug&#259; produse de oriunde</string>
       <string name="onboarding_slide_2_body">Caut&#259; online, adaug&#259; un link sau introdu manual</string>
       <string name="onboarding_slide_3_title">Cadoul perfect, de fiecare dat&#259;</string>
       <string name="onboarding_slide_3_body">Prime&#537;ti cadoul pe care &#238;l dore&#537;ti. Oferi cadoul pe care &#238;l doresc.</string>
       <string name="onboarding_skip">Omite</string>
       <string name="onboarding_next">Continu&#259;</string>
       <string name="onboarding_get_started">&#206;ncepe</string>
   ```
   Use XML numeric entities (`&#259;` for `ă`, `&#537;` for `ș`, `&#539;` for `ț`, `&#206;` for `Î`, `&#238;` for `î`) — matches the existing pattern in values-ro/strings.xml. Do NOT use raw UTF-8 diacritics.

Why do these together: they are the mechanical scaffolding that Task 2 consumes. Splitting would force Task 2 to redeclare types or create cross-task compilation gaps. All 6 files compile independently — no runtime coupling until Task 2 wires them.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin</automated>
    Additional checks:
    - `grep -r "onboarding_seen" app/src/main/java/com/giftregistry/data/preferences/` returns the new DataStore.
    - `grep "OnboardingKey" app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` returns the new key.
    - `grep "bindOnboardingPreferences" app/src/main/java/com/giftregistry/di/DataModule.kt` returns the new Hilt binding.
    - `grep "onboarding_slide_1_title" app/src/main/res/values/strings.xml app/src/main/res/values-ro/strings.xml` returns both files.
  </verify>
  <done>
    - OnboardingPreferencesRepository interface exists in domain layer with 3 methods (observe/is/set).
    - OnboardingPreferencesDataStore is a @Singleton @Inject class implementing the repo, uses DataStore name "onboarding_prefs" with unique Context extension property `onboardingDataStore`.
    - DataModule.kt has a new `bindOnboardingPreferences` @Binds @Singleton function.
    - AppNavKeys.kt declares `@Serializable data object OnboardingKey`.
    - Both strings.xml files contain all 9 new onboarding_* keys.
    - `./gradlew :app:compileDebugKotlin` succeeds (Hilt generates DI graph without errors).
  </done>
</task>

<task type="auto">
  <name>Task 2: OnboardingScreen + ViewModel + wire into AppNavigation</name>
  <files>
    app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt,
    app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt,
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  </files>
  <action>
Build the carousel UI and integrate it into the navigation graph. Three files; they compile together.

1. **OnboardingViewModel** — create `app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt`:
   ```kotlin
   package com.giftregistry.ui.onboarding

   import androidx.lifecycle.ViewModel
   import androidx.lifecycle.viewModelScope
   import com.giftregistry.domain.preferences.OnboardingPreferencesRepository
   import dagger.hilt.android.lifecycle.HiltViewModel
   import kotlinx.coroutines.flow.SharingStarted
   import kotlinx.coroutines.flow.StateFlow
   import kotlinx.coroutines.flow.stateIn
   import kotlinx.coroutines.launch
   import javax.inject.Inject

   sealed interface OnboardingSeenState {
       data object Loading : OnboardingSeenState
       data object NotSeen : OnboardingSeenState
       data object Seen : OnboardingSeenState
   }

   @HiltViewModel
   class OnboardingViewModel @Inject constructor(
       private val repo: OnboardingPreferencesRepository
   ) : ViewModel() {

       val state: StateFlow<OnboardingSeenState> = repo.observeOnboardingSeen()
           .let { flow ->
               kotlinx.coroutines.flow.map(flow) { seen ->
                   if (seen) OnboardingSeenState.Seen else OnboardingSeenState.NotSeen
               } as kotlinx.coroutines.flow.Flow<OnboardingSeenState>
           }
           .stateIn(
               scope = viewModelScope,
               started = SharingStarted.Eagerly,  // matches Phase 02 decision for SettingsViewModel
               initialValue = OnboardingSeenState.Loading
           )

       fun markSeen() {
           viewModelScope.launch { repo.setOnboardingSeen() }
       }
   }
   ```
   Cleaner equivalent (use this — `.let { ... }` above is just to be explicit about the Flow transform; prefer direct `.map`):
   ```kotlin
   import kotlinx.coroutines.flow.map

   val state: StateFlow<OnboardingSeenState> = repo.observeOnboardingSeen()
       .map { seen -> if (seen) OnboardingSeenState.Seen else OnboardingSeenState.NotSeen }
       .stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingSeenState.Loading)
   ```
   `SharingStarted.Eagerly` is intentional (per Phase 02 `SettingsViewModel.currentLocale` decision): we need the flag resolved before the first auth-routing frame to avoid a carousel-flash on resume.

2. **OnboardingScreen** — create `app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt`.

   Composable signature:
   ```kotlin
   @Composable
   fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel())
   ```

   Structure (pseudo-outline, implement cleanly):
   ```kotlin
   val pagerState = rememberPagerState(pageCount = { 4 })
   val scope = rememberCoroutineScope()

   // Mark seen when pager settles on page 4 (auth) OR when Skip is tapped.
   LaunchedEffect(pagerState.settledPage) {
       if (pagerState.settledPage == 3) viewModel.markSeen()
   }

   Box(modifier = Modifier.fillMaxSize()) {
       HorizontalPager(
           state = pagerState,
           modifier = Modifier.fillMaxSize()
       ) { page ->
           when (page) {
               0 -> OnboardingSlide(
                   titleRes = R.string.onboarding_slide_1_title,
                   bodyRes = R.string.onboarding_slide_1_body,
                   illustrationSlot = { /* Icon: Icons.Default.FormatListBulleted, or CardGiftcard — keep minimal, no new asset */ }
               )
               1 -> OnboardingSlide(
                   titleRes = R.string.onboarding_slide_2_title,
                   bodyRes = R.string.onboarding_slide_2_body,
                   illustrationSlot = { /* Icon: Icons.Default.Link */ }
               )
               2 -> OnboardingSlide(
                   titleRes = R.string.onboarding_slide_3_title,
                   bodyRes = R.string.onboarding_slide_3_body,
                   illustrationSlot = { /* Icon: Icons.Default.CardGiftcard */ }
               )
               3 -> AuthScreen()   // Reuses existing composable — its own Scaffold/Snackbar intact
           }
       }

       // Overlay: Skip button + page indicator dots, visible only on pages 0..2
       if (pagerState.currentPage < 3) {
           // Top-right Skip button
           TextButton(
               onClick = {
                   viewModel.markSeen()
                   scope.launch { pagerState.animateScrollToPage(3) }
               },
               modifier = Modifier
                   .align(Alignment.TopEnd)
                   .padding(top = 16.dp, end = 16.dp)
                   .defaultMinSize(minHeight = 44.dp)  // touch target
           ) {
               Text(stringResource(R.string.onboarding_skip))
           }

           // Bottom-center page indicator dots (3 dots, one per onboarding slide; page 4 not represented)
           Row(
               modifier = Modifier
                   .align(Alignment.BottomCenter)
                   .padding(bottom = 48.dp),
               horizontalArrangement = Arrangement.spacedBy(8.dp)
           ) {
               repeat(3) { index ->
                   val selected = pagerState.currentPage == index
                   Box(
                       modifier = Modifier
                           .size(if (selected) 10.dp else 8.dp)
                           .background(
                               color = if (selected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                               shape = CircleShape
                           )
                   )
               }
           }
       }
   }
   ```

   Private `OnboardingSlide` composable — single screen layout:
   ```kotlin
   @Composable
   private fun OnboardingSlide(
       @StringRes titleRes: Int,
       @StringRes bodyRes: Int,
       illustrationSlot: @Composable () -> Unit,
   ) {
       Column(
           modifier = Modifier
               .fillMaxSize()
               .background(MaterialTheme.colorScheme.background)
               .padding(horizontal = 32.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.Center
       ) {
           Box(
               modifier = Modifier.size(120.dp),
               contentAlignment = Alignment.Center
           ) { illustrationSlot() }   // Render a Material icon via Icon(..., modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
           Spacer(Modifier.height(48.dp))
           Text(
               text = stringResource(titleRes),
               style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
               textAlign = TextAlign.Center,
               modifier = Modifier.semantics { heading() }
           )
           Spacer(Modifier.height(16.dp))
           Text(
               text = stringResource(bodyRes),
               style = MaterialTheme.typography.bodyLarge,
               color = MaterialTheme.colorScheme.onSurfaceVariant,
               textAlign = TextAlign.Center
           )
           Spacer(Modifier.height(64.dp))  // Space so text doesn't collide with dot indicator
       }
   }
   ```

   Imports you will need:
   ```kotlin
   import androidx.annotation.StringRes
   import androidx.compose.foundation.background
   import androidx.compose.foundation.layout.*
   import androidx.compose.foundation.pager.HorizontalPager
   import androidx.compose.foundation.pager.rememberPagerState
   import androidx.compose.foundation.shape.CircleShape
   import androidx.compose.material.icons.Icons
   import androidx.compose.material.icons.filled.CardGiftcard
   import androidx.compose.material.icons.filled.FormatListBulleted
   import androidx.compose.material.icons.filled.Link
   import androidx.compose.material3.*
   import androidx.compose.runtime.*
   import androidx.compose.ui.Alignment
   import androidx.compose.ui.Modifier
   import androidx.compose.ui.res.stringResource
   import androidx.compose.ui.semantics.heading
   import androidx.compose.ui.semantics.semantics
   import androidx.compose.ui.text.font.FontWeight
   import androidx.compose.ui.text.style.TextAlign
   import androidx.compose.ui.unit.dp
   import androidx.hilt.navigation.compose.hiltViewModel
   import com.giftregistry.R
   import com.giftregistry.ui.auth.AuthScreen
   import kotlinx.coroutines.launch
   ```

   **Design notes (honor existing theme):**
   - No new colors. Use `MaterialTheme.colorScheme.background` for slide bg, `primary` for selected dot + illustration tint, `onSurfaceVariant` for body text and inactive dots.
   - No new typography. Use `headlineLarge` (bold) for titles — matches AuthScreen's app title treatment.
   - Illustrations are Material icons from `androidx.compose.material.icons.filled` — NO new image assets. Slide 1 → `Icons.Default.FormatListBulleted`, Slide 2 → `Icons.Default.Link`, Slide 3 → `Icons.Default.CardGiftcard`.
   - Page indicator renders ONLY on pages 0-2 (`pagerState.currentPage < 3`); on page 4 (auth), the existing AuthScreen owns the entire viewport with no overlay.
   - Skip TextButton sits top-right on the overlay layer (Box alignment), NOT inside any individual slide — it's a pager-level control.

   **AuthScreen as page 4 — verification during impl:** AuthScreen has its own `Scaffold` with `SnackbarHost`. Compose supports nested Scaffolds inside a pager page slot (pager page is a Box with pager-sized constraints). If during manual smoke-test the Snackbar, Google Credential Manager popup, or vertical scroll behaves incorrectly when rendered as pager page 4, fall back: replace `AuthScreen()` at page 3 with a slim "Ready to start?" page containing a single "Continue" button that calls `viewModel.markSeen()` and navigates to `AuthKey` via a `onNavigateToAuth: () -> Unit` param plumbed from `OnboardingScreen(onNavigateToAuth: ...)` through to AppNavigation. Document this fallback in your SUMMARY.md if triggered.

3. **Integrate into AppNavigation** — edit `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`.

   a. Add imports:
      ```kotlin
      import com.giftregistry.ui.onboarding.OnboardingScreen
      import com.giftregistry.ui.onboarding.OnboardingSeenState
      import com.giftregistry.ui.onboarding.OnboardingViewModel
      ```

   b. Near the top of `AppNavigation`, alongside `authViewModel`, add:
      ```kotlin
      val onboardingViewModel: OnboardingViewModel = hiltViewModel()
      val onboardingSeenState by onboardingViewModel.state.collectAsStateWithLifecycle()
      ```

   c. Change the Loading early-return so it waits for BOTH auth AND onboarding state to resolve:
      ```kotlin
      if (authUiState is AuthUiState.Loading || onboardingSeenState is OnboardingSeenState.Loading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
          }
          return
      }
      ```

   d. In the `LaunchedEffect(authUiState)` block, change the `Unauthenticated` branch to route via onboarding when not-seen:
      ```kotlin
      is AuthUiState.Unauthenticated -> {
          val entryKey: Any = if (onboardingSeenState is OnboardingSeenState.NotSeen) {
              OnboardingKey
          } else {
              AuthKey
          }
          val current = backStack.lastOrNull()
          // Reset only if we're not already on the correct unauthenticated entry
          if (current !is OnboardingKey && current !is AuthKey
              || (entryKey is OnboardingKey && current is AuthKey)
              || (entryKey == AuthKey && current is OnboardingKey)
          ) {
              backStack.clear()
              backStack.add(entryKey)
          }
      }
      ```
      Then extend the `LaunchedEffect` key list so it re-evaluates when onboarding flips seen:
      ```kotlin
      LaunchedEffect(authUiState, onboardingSeenState) { ... }
      ```
      Note: the `Authenticated` branch is UNCHANGED — authenticated users bypass onboarding entirely. The `Loading` branch is UNCHANGED.

   e. Add the new `entry<OnboardingKey>` to the `entryProvider { ... }` block, directly after `entry<AuthKey> { AuthScreen() }`:
      ```kotlin
      entry<OnboardingKey> { OnboardingScreen() }
      ```

   **Sign-out behavior (already correct — verify, do not change):** When user signs out, `AuthUiState` flips to `Unauthenticated`. Because `onboarding_seen` persists in DataStore, `onboardingSeenState` is `Seen`, so the routing lands on `AuthKey` — NOT `OnboardingKey`. This satisfies the "logout does not re-show onboarding" requirement without any extra code.

   **What we deliberately do NOT do:**
   - Do NOT call `viewModel.markSeen()` from `AuthScreen` on successful login. Swipe-to-auth already triggers `markSeen()` via `settledPage == 3`. If user downloads app, swipes to auth, closes app, reopens — onboarding already marked seen. Covering "user authenticated on first launch without swiping" is unreachable: authentication requires reaching AuthScreen, which requires settling on page 4.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug</automated>
    Manual smoke test (single run on emulator or device):
    1. Uninstall app OR clear app data (`adb shell pm clear com.giftregistry`).
    2. Launch app → onboarding carousel is visible, slide 1 copy matches spec exactly.
    3. Swipe left twice → slides 2, 3 render with correct copy; dot indicator moves.
    4. Swipe left again → AuthScreen renders as page 4; Skip button + dots disappear.
    5. Close app (not uninstall), relaunch → AuthScreen is the entry point (no carousel).
    6. Clear app data again → launch → onboarding visible → tap Skip on slide 1 → jumps to AuthScreen → close/relaunch → AuthScreen directly.
    7. Sign in with any method → lands on HomeKey → sign out via Settings → lands on AuthScreen (NOT onboarding).
    8. Change device locale to Romanian → clear app data → launch → slide titles/bodies render in Romanian with correct diacritics.
  </verify>
  <done>
    - `./gradlew :app:assembleDebug` succeeds.
    - On first launch (fresh install or cleared data) with no auth session, OnboardingScreen is the root, showing slide 1.
    - HorizontalPager has exactly 4 pages: slides 1-3 render `OnboardingSlide` composables with the spec copy; page 4 renders `AuthScreen()`.
    - Skip button and dot indicators are visible on pages 0-2, hidden on page 3.
    - Tapping Skip or swiping onto page 4 triggers `viewModel.markSeen()`, persisting `onboarding_seen = true` to DataStore `onboarding_prefs`.
    - Subsequent unauthenticated launches bypass OnboardingScreen and render AuthScreen directly.
    - Authenticated users land on HomeKey without seeing OnboardingScreen.
    - Signing out routes to AuthScreen, NOT OnboardingScreen (DataStore flag persists).
    - Romanian locale renders all slide copy in Romanian.
  </done>
</task>

</tasks>

<verification>
Phase-level verification:

1. **Compile gate:** `./gradlew :app:assembleDebug` succeeds with no errors.
2. **Must-have truths** — manually walk through all 9 truths listed in frontmatter `must_haves.truths`. Each must be observable on device.
3. **Must-have artifacts** — every file in `must_haves.artifacts` exists with the specified `contains` or `exports`.
4. **Must-have key_links** — run these greps, each must return at least one match:
   ```bash
   grep -n "OnboardingKey\|onboardingSeen" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
   grep -n "AuthScreen(" app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt
   grep -n "markSeen()\|onboarding_seen" app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt
   grep -n "bindOnboardingPreferences" app/src/main/java/com/giftregistry/di/DataModule.kt
   ```
5. **No hardcoded strings:** `grep -nE "\"Create your list\"|\"Add items from anywhere\"|\"The perfect gift\"" app/src/main/java/com/giftregistry/ui/onboarding/` returns NOTHING (all copy goes through `stringResource`).
6. **DataStore name uniqueness:** `grep -rn "preferencesDataStore(name" app/src/main/java/com/giftregistry/data/preferences/` shows three distinct names: `user_prefs`, `guest_prefs`, `onboarding_prefs`.
</verification>

<success_criteria>
- Fresh install + unauthenticated → onboarding carousel is the entry point, shows slide 1 with exact spec copy.
- 4-page HorizontalPager: pages 0-2 = onboarding slides, page 3 = existing AuthScreen (reused, unchanged).
- Skip button (top-right, pages 0-2 only) jumps to AuthScreen AND marks onboarding seen.
- Dot indicator (3 dots, bottom-center) reflects current page on slides 1-3, hidden on auth page.
- Onboarding appears exactly once: after Skip OR swipe-to-auth OR successful auth, it never reappears on subsequent launches.
- Sign-out returns user to AuthScreen, NEVER the carousel.
- Already-authenticated users (session restore) land on HomeKey, never seeing the carousel.
- EN (default) and RO (values-ro) strings both render correctly per device locale.
- No hardcoded string literals for any UI-visible text.
- Follows existing patterns: DataStore mirrors LanguagePreferencesDataStore, Hilt binding via DataModule, new Navigation3 key, Material3 theme tokens only, no new assets/colors/typography.
</success_criteria>

<output>
After completion, create `.planning/quick/260419-ubj-onboarding-carousel-3-slides-before-auth/260419-ubj-SUMMARY.md` documenting:
- Whether AuthScreen was reused directly as page 4 (option a) or the fallback "Continue" button approach (option b) was needed, and why.
- Any Compose/pager quirks encountered (e.g., nested scroll, Scaffold-in-pager behavior).
- Confirmation that all 8 manual smoke-test steps passed.
</output>
