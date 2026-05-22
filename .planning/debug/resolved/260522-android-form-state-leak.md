---
status: resolved
trigger: "On the Android app, the Add Registry form and the Add Item form both display data from the previously-submitted registry/item instead of empty fields when the user opens them for a second time."
created: 2026-05-22T00:00:00Z
updated: 2026-05-22T04:00:00Z
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: CONFIRMED. After clean reinstall there is no cached Firebase user. FirebaseAuthDataSource emits AuthStateEvent.Initial(null) synchronously on first listener attach. AuthViewModel handles Initial(null) as AuthUiState.Loading (was designed to prevent BUG-AUTH-FLASH-260512). But with a fresh install, no subsequent Changed event ever fires — Firebase only calls the listener again when state actually changes. The app is trapped in Loading forever. The withTimeout fix is irrelevant: it only runs when user != null on first callback. Fresh install => user == null => withTimeout never executes.
test: Fix AuthViewModel to treat Initial(null) as Unauthenticated. Verify Initial(null) path is safe: in the current FirebaseAuthDataSource, Initial(null) is ONLY emitted when user == null on first callback (no cached session). Users with cached sessions always get Initial(non-null) after the async getIdToken validation — or Changed(null) if signout occurs. BUG-AUTH-FLASH-260512 is NOT reintroduced because the flash was caused by the old code emitting Initial(null) before it had checked for a cached user — the new FirebaseAuthDataSource never emits Initial(null) for a cached-user scenario.
expecting: After fix, fresh install (no cached user) reaches AuthScreen immediately. Existing user (cached session) still gets Authenticated via Initial(non-null) after token validation.
next_action: Apply fix to AuthViewModel line 49 — change AuthUiState.Loading to AuthUiState.Unauthenticated in the Initial(null) branch.

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: When the user taps "Add Registry" (or "Add Item") to create a new entry, the form fields should be empty / at default values.
actual: The form fields are pre-populated with the values the user entered last time they created a registry (or item). The stale data appears immediately on form open — before any user interaction.
errors: None. No crash. Pure state leak.
reproduction:
  1. Open the Android app.
  2. Open the Add Registry form, fill in all fields, tap Save/Create successfully so the registry is created.
  3. Navigate back to wherever the "Add Registry" button lives, tap it again to add a second registry.
  4. Observe: the form fields contain the values from step 2 (previous registry), not empty defaults.
  5. Same exact pattern reproduces with the Add Item form.
started: Likely always existed since screens were built — not a recent regression.

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: Shared SavedStateHandle / DataStore / repository holding form draft
  evidence: No draft store exists. StateFlows live entirely inside ViewModels.
  timestamp: 2026-05-22T00:01:00Z

- hypothesis: ViewModel is @Singleton scoped
  evidence: Both VMs are @HiltViewModel (not @Singleton). The scope problem is via the ViewModelStore key, not DI scope.
  timestamp: 2026-05-22T00:01:00Z

- hypothesis: -Puse_emulator=false is not honored by the build (flag doesn't thread through to runtime)
  evidence: |
    Fully traced end-to-end. build.gradle.kts line 38-39 reads the Gradle property and writes
    it into BuildConfig.USE_FIREBASE_EMULATOR. AppModule.kt checks that BuildConfig field at
    singleton construction time for FirebaseAuth (port 9099), FirebaseFirestore (8080), and
    FirebaseFunctions (5001). StorageModule.kt checks it for FirebaseStorage (9199). With
    -Puse_emulator=false, none of the useEmulator() calls execute. All four clients point at
    production Firebase. The flag is correctly honored.
  timestamp: 2026-05-22T02:00:00Z

- hypothesis: App Check cached failure blocks startup
  evidence: |
    No App Check code exists anywhere in app/src/**/*.kt. App Check is not integrated into
    this project. Cannot be the cause.
  timestamp: 2026-05-22T02:00:00Z

- hypothesis: runBlocking in GiftRegistryApp.onCreate() causes indefinite hang
  evidence: |
    GiftRegistryApp.kt line 21: runBlocking { languagePrefsRepo.getLanguageTag() } calls
    context.dataStore.data.first() — a disk-only DataStore read. No network dependency.
    Cannot block indefinitely. Dismissed as startup hang cause.
  timestamp: 2026-05-22T02:00:00Z

- hypothesis: withTimeout(15s) fix is the relevant fix for the post-clean-reinstall hang
  evidence: |
    withTimeout only runs when user != null on first AuthStateListener callback (FirebaseAuthDataSource
    line 32). Clean reinstall has no cached user -> user == null -> withTimeout branch never entered.
    The user did a clean reinstall before the second test. Their device has no cached Firebase session.
    The spinner is driven by Initial(null) -> AuthUiState.Loading with no subsequent Changed event.
  timestamp: 2026-05-22T03:00:00Z

- hypothesis: form-state code changes (resetForm / onResetForm) cause the startup hang
  evidence: |
    resetForm() is private, called only inside onSave() on success — never at startup.
    onResetForm() in AddItemScreen is inside LaunchedEffect gated by savedItemId != null
    — also never at startup. Both functions are synchronous StateFlow assignments with no
    async, no suspend, no network I/O. Cannot cause a loading spinner hang.
  timestamp: 2026-05-22T01:00:00Z

- hypothesis: OnboardingPreferencesDataStore causes loading hang
  evidence: DataStore is disk-only (no network). Emits almost instantly on startup. Both
    OnboardingSeenState.Seen and NotSeen unblock the spinner. Cannot hang indefinitely.
  timestamp: 2026-05-22T01:00:00Z

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-05-22T00:01:00Z
  checked: CreateRegistryScreen.kt line 103-107
  found: |
    hiltViewModelWithNavArgs(
        key = registryId ?: "new",
        "registryId" to registryId,
    )
    For CreateRegistryKey (create mode), registryId is null, so key = "new".
    "new" is a constant string — it never changes across navigations.
  implication: |
    The ViewModelStore (Activity-scoped in Nav3 when using hiltViewModel) caches the
    CreateRegistryViewModel under key="new". When the user navigates back and opens
    CreateRegistryKey a second time, hiltViewModelWithNavArgs returns the SAME existing
    VM instance with all StateFlows still holding the previously-typed values.

- timestamp: 2026-05-22T00:01:00Z
  checked: AddItemScreen.kt line 65-74
  found: |
    hiltViewModelWithNavArgs(
        key = registryId ?: "add-item-no-registry-yet",
        ...
    )
    When entering via RegistryDetail FAB with a concrete registryId (e.g. "abc123"),
    key = "abc123". On second visit to the same registry's AddItemKey, the same
    ViewModel instance is returned — title/url/price/notes still hold prior values.
    When entering via the FAB sheet (registryId=null), key = "add-item-no-registry-yet"
    — same constant string reuse problem.
  implication: Same mechanism as CreateRegistryViewModel.

- timestamp: 2026-05-22T00:01:00Z
  checked: CreateRegistryViewModel.kt — onSave() success path (line 208-210)
  found: |
    onSuccess = { newId -> savedRegistryId.value = newId }
    After successful create, only savedRegistryId is set. title, occasion, eventDateMs,
    eventLocation, description, visibility, coverPhotoSelection are NEVER cleared.
  implication: |
    All form fields retain their values indefinitely in the Activity-scoped VM.
    On next visit to CreateRegistryKey, the same instance is served and the stale
    StateFlow values are immediately reflected in the UI.

- timestamp: 2026-05-22T00:01:00Z
  checked: AddItemViewModel.kt — onSave() success path (line 293) and onResetForm() (line 338-350)
  found: |
    onSuccess = { itemId -> _savedItemId.value = itemId }
    onResetForm() exists and clears url/title/imageUrl/price/notes — but it is only
    called from AddItemScreen.kt LaunchedEffect when addAnotherMode=true (line 107).
    On the Save-and-Exit path (addAnotherMode=false, line 115), only clearSavedItemId()
    is called followed by onBack(). onResetForm() is NOT called.
  implication: |
    After Save-and-Exit, url/title/imageUrl/price/notes remain populated in the
    Activity-scoped VM. The next visit to the same AddItemKey returns the same instance,
    fields already filled.

- timestamp: 2026-05-22T00:01:00Z
  checked: CreateRegistryViewModel.kt — clearSavedRegistryId() comment (line 252-258)
  found: |
    The comment says "Activity-scoped ViewModel survives the lifetime of the Activity"
    and notes this is why clearSavedRegistryId() is needed to avoid spurious navigation.
    The developer was aware the scope survives but did not apply the same reset logic to
    the form field StateFlows.
  implication: The activity-scope persistence is intentional for navigation stability, but
    the missing form-field reset is an oversight — not a design constraint.

- timestamp: 2026-05-22T02:00:00Z
  checked: |
    app/build.gradle.kts (emulator flag plumbing), di/AppModule.kt (Firebase singletons),
    di/StorageModule.kt (FirebaseStorage singleton), GiftRegistryApp.kt (Application.onCreate),
    data/preferences/LanguagePreferencesDataStore.kt (getLanguageTag impl)
  found: |
    1. -Puse_emulator=false is fully honored. Gradle property -> BuildConfig.USE_FIREBASE_EMULATOR=false
       -> none of the useEmulator() calls in AppModule/StorageModule fire. All Firebase clients
       hit production.
    2. No App Check code exists anywhere in the Kotlin source tree. Not integrated.
    3. GiftRegistryApp.onCreate() has runBlocking { languagePrefsRepo.getLanguageTag() } but
       this is a DataStore disk read — no network, cannot hang indefinitely.
    4. The 15-second timeout is compiled into FirebaseAuthDataSource in the current APK
       (withTimeout(15_000L) around user.getIdToken(true).await()).
    5. The "stuck on loading" behavior reported by the user described the PRE-FIX install.
       The user has not yet reported whether the newly-built APK (with the fix) was installed
       and tested.
  implication: |
    All investigated alternative startup blockers are eliminated. The fix (15s timeout) is in
    place and the build correctly targets production Firebase. The only remaining verification
    gap is: did the user actually install and run the new APK?

- timestamp: 2026-05-22T03:00:00Z
  checked: |
    AuthViewModel.kt (full file), FirebaseAuthDataSource.kt (full current state with withTimeout fix),
    AppNavigation.kt (spinner gate logic), AuthStateEvent.kt, git log for both files,
    git show 845f147 (Initial/Changed introduction), git show c0ae973 (getIdToken fix)
  found: |
    1. AppNavigation.kt line 130-145: spinner shows when authUiState is AuthUiState.Loading OR
       onboardingSeenState is OnboardingSeenState.Loading. Confirmed gate is on AuthUiState.Loading.

    2. AuthViewModel.kt lines 39-51: Initial(null) => AuthUiState.Loading. This was intentionally
       introduced in commit 845f147 to fix BUG-AUTH-FLASH-260512. The comment says "wait for the
       post-restoration Changed emission."

    3. CRITICAL: Firebase AuthStateListener fires EXACTLY ONCE when there is no cached user —
       with null on first attach. No subsequent Changed event follows unless auth state actually
       changes. Initial(null) = "genuinely no session". There is NO "post-restoration Changed
       emission" coming for a fresh-install device.

    4. The withTimeout fix in FirebaseAuthDataSource only executes in the branch where
       user != null on first callback (line 32: "if (user != null)"). Fresh install has
       user == null → withTimeout is never entered → fix is irrelevant to this hang.

    5. BUG-AUTH-FLASH-260512 context: the flash was caused by the OLD code emitting Initial(null)
       when Firebase had not yet loaded the cached user from disk. The CURRENT FirebaseAuthDataSource
       (after c0ae973) fixes this differently: when user != null, it defers emitting Initial until
       after async getIdToken validation. When user == null, emitting Unauthenticated is correct
       and safe — there is no cached user to flash about.

    6. Therefore: the correct fix is AuthViewModel line 49 — change Initial(null) branch from
       AuthUiState.Loading to AuthUiState.Unauthenticated. This does NOT reintroduce
       BUG-AUTH-FLASH-260512 because the old flash race no longer exists in FirebaseAuthDataSource.
  implication: |
    Root cause is AuthViewModel treating Initial(null) as Loading. Fresh-install / clean-reinstall
    users have no cached session → Initial(null) → Loading → no Changed ever fires → infinite spinner.
    The withTimeout fix was a red herring for THIS scenario (it helps if user has a cached session
    and token refresh hangs, but that's a different scenario). Fix is a 1-line change in AuthViewModel.

- timestamp: 2026-05-22T01:00:00Z
  checked: |
    CreateRegistryViewModel.kt (full file after form-state fix), AddItemScreen.kt (full
    file after form-state fix), AppNavigation.kt (loading spinner logic), AuthViewModel.kt,
    OnboardingViewModel.kt, FirebaseAuthDataSource.kt, app/build.gradle.kts
  found: |
    1. resetForm() is private and called ONLY from onSave() on success path — zero
       startup-time execution. AddItemScreen's onResetForm() call is inside
       LaunchedEffect(savedItemId) gated by savedItemId != null — also zero startup-time.
       FORM-STATE CHANGES CANNOT CAUSE STARTUP HANG.

    2. AppNavigation shows spinner while authUiState == AuthUiState.Loading OR
       onboardingSeenState == OnboardingSeenState.Loading.

    3. OnboardingPreferencesDataStore is disk-only DataStore — cannot hang.

    4. FirebaseAuthDataSource: when device has a cached Firebase user, it launches a
       coroutine to call `user.getIdToken(true).await()`. This is a NETWORK call.
       If the call hangs (emulator unreachable, network issue), the coroutine never
       completes, authStateFlow never emits anything, and AuthViewModel stays in Loading.

    5. build.gradle.kts line 38-39: default for debug builds is USE_FIREBASE_EMULATOR=true,
       FIREBASE_EMULATOR_HOST="10.0.2.2". On a physical device 10.0.2.2 is unreachable
       (AVD-only loopback alias). Physical device testing requires -PemulatorHost=<real-ip>
       AND the emulator to be running, OR -Puse_emulator=false.

    6. STATE.md confirms previous physical device tests used -PemulatorHost=192.168.1.10
       with the emulator running. If the current build was assembled without that flag
       and without -Puse_emulator=false, and the emulator is not running, the token
       refresh hangs → app never leaves loading.
  implication: |
    The startup hang is almost certainly: debug build has USE_FIREBASE_EMULATOR=true
    baked in, device has a cached auth user, getIdToken(true) routes to unreachable
    emulator host, hangs indefinitely. This is COINCIDENTAL with the form-state fix —
    the code changes themselves are correct and not the cause.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: |
  TWO separate issues:

  1. FORM STATE LEAK (original issue):
     Both CreateRegistryViewModel (key="new") and AddItemViewModel (key=registryId) are
     Activity-scoped via hiltViewModelWithNavArgs with stable, constant keys. On second
     visit, the same VM instance is returned with all MutableStateFlows intact.
     CreateRegistryViewModel never resets any form field on successful save.
     AddItemViewModel has onResetForm() but it was only called on the "Add another" path.

  2. STARTUP HANG (regression after clean reinstall of form-state fix build):
     AuthViewModel treats AuthStateEvent.Initial(null) as AuthUiState.Loading. This was
     intentional (commit 845f147) to prevent BUG-AUTH-FLASH-260512 — a flash of AuthScreen
     before Firebase restored a cached user from disk. However, Firebase only fires
     AuthStateListener once when there is NO cached user (null on first attach). No subsequent
     Changed event follows. The app stays in Loading forever. Clean reinstall removes the
     cached Firebase session, so the device hits this path every launch.
     
     The withTimeout fix in FirebaseAuthDataSource is correct but only relevant for a different
     scenario: cached user + network unreachable. It never executes on a fresh install.

fix: |
  1. FORM STATE LEAK:
     - resetForm() added to CreateRegistryViewModel, called from onSave() on create success.
     - AddItemScreen.kt: onResetForm() called on Save-and-Exit path (was clearSavedItemId()).

  2. STARTUP HANG:
     - AuthViewModel.kt: change Initial(null) branch from AuthUiState.Loading to
       AuthUiState.Unauthenticated. BUG-AUTH-FLASH-260512 is NOT reintroduced because the
       flash race no longer exists: FirebaseAuthDataSource (post-c0ae973) now defers emitting
       Initial until after async getIdToken validation for the user != null path. Initial(null)
       only fires when there genuinely is no cached session.
     - Keep withTimeout(15_000L) fix in FirebaseAuthDataSource (correct for cached-user +
       unreachable network scenario).

verification: |
  User performed a clean install of the APK built with -Puse_emulator=false. App reached
  the login screen immediately (no infinite spinner). Full flow verified: sign-in works,
  Add Registry form is empty on second visit, Add Item form is empty on second visit.
  All four fixes confirmed working end-to-end on physical device.
files_changed:
  - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
  - app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt
  - app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt
