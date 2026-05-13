---
status: resolved
trigger: "url-fetch-fails-ikea-android"
created: 2026-05-13T00:00:00Z
updated: 2026-05-13T12:00:00Z
---

## Current Focus

hypothesis: CONFIRMED AND RESOLVED. Root cause: stale Firebase Auth refresh token in
  on-disk SDK cache, no recovery path. Fix: getIdToken(forceRefresh=true) health-check on
  first non-null Initial event in FirebaseAuthDataSource; sign out on any token failure.
  Secondary UX fix: ogFetchEmpty state machine and improved UA/timeout in fetchOgMetadata.ts.

test: Human-verified by user: cold-launch with stale refresh token now routes to AuthScreen,
  and after re-authentication the IKEA URL fetch succeeds and pre-fills the form.

expecting: n/a — resolved

next_action: NONE — session resolved

## Symptoms

expected: Pasting a product URL (IKEA) into "Add item by URL" fetches metadata (title, image, price) and pre-fills the form
actual: The fetch fails and shows "We couldn't read that page — fill in the details below"
errors: User-facing message: "We couldn't read that page — fill in the details below". No backend/logcat errors quoted yet.
reproduction:
  1. Run Android app pointed at local Firebase emulator
  2. Open add-item flow and choose "Add by URL"
  3. Paste an IKEA product URL (e.g. ikea.com or ikea.ro)
  4. Submit / let form attempt to fetch metadata
  5. Observe fallback message
started: Unknown — unverified if this ever worked for non-EMAG URLs

## Eliminated

- hypothesis: Prior fix (UA change, timeout increase, ogFetchEmpty state) addressed root cause
  evidence: User logcat shows INVALID_REFRESH_TOKEN cascading through ALL Firebase requests —
            Firestore watch streams AND the callable. The emulator IS reachable; the auth
            emulator rejects the stale refresh token. The callable never executes because the
            auth token cannot be obtained. UA/timeout changes in fetchOgMetadata.ts are irrelevant
            to this failure mode.
  timestamp: 2026-05-13T11:00:00Z

- hypothesis: Emulator unreachable / emulator not running
  evidence: Logcat shows Firestore watch streams also failing with INVALID_REFRESH_TOKEN —
            Firestore IS connected (watch streams are active). The failure is at token refresh,
            not at TCP connectivity. Emulator is reachable on the correct ports.
  timestamp: 2026-05-13T11:00:00Z

- hypothesis: IKEA blocks the fetch with 403 causing function to throw
  evidence: Function's try/catch returns empty (all-null) on any HTTP error — never throws.
            Curl test shows IKEA returns 200 with valid OG data for the function's User-Agent.
  timestamp: 2026-05-13T10:15:00Z

- hypothesis: Function timeout (5s) causes fetch to fail and function to throw
  evidence: Curl test shows function completes in ~1.2s for IKEA URL. Timeout not hit.
            Even if timeout fires, it's caught and returns empty (not throws).
  timestamp: 2026-05-13T10:15:00Z

- hypothesis: Network security config blocks cleartext HTTP to emulator
  evidence: src/debug/res/xml/network_security_config.xml has cleartextTrafficPermitted=true
  timestamp: 2026-05-13T10:15:00Z

- hypothesis: App Check blocks the callable
  evidence: No App Check configuration found in GiftRegistryApp or anywhere in the app.
            Emulator log shows "verifications: app: MISSING" — callable still passes.
  timestamp: 2026-05-13T10:15:00Z

- hypothesis: Region mismatch causes NOT_FOUND error
  evidence: AppModule.kt correctly uses FirebaseFunctions.getInstance("europe-west3").
            Function is registered at europe-west3 in emulator. This was fixed in
            commit 985bf78 (resolved 12 UAT bugs).
  timestamp: 2026-05-13T10:15:00Z

- hypothesis: result.data cast (Map<String, Any?>) fails
  evidence: Curl test shows emulator returns {"result": {...all-null...}} which Android SDK
            deserializes as Map with null values. No ClassCastException expected.
  timestamp: 2026-05-13T10:15:00Z

## Evidence

- timestamp: 2026-05-13T11:00:00Z
  checked: User logcat from fresh IKEA URL reproduction
  found: Primary error — "fetchOgMetadata failed: ExecutionException: 1 out of 2 underlying
         tasks failed. Caused by: FirebaseException: INVALID_REFRESH_TOKEN". Also cascading
         through Firestore: "FirestoreCallCredentials: Failed to get auth token:
         FirebaseException: INVALID_REFRESH_TOKEN". Firestore watch stream: UNAUTHENTICATED.
  implication: NOT an emulator connectivity or IKEA/UA issue. The Firebase Auth SDK's cached
               refresh token was issued by a prior emulator instance that no longer exists.
               getIdToken() calls the auth emulator which rejects the token. ALL Firebase
               API calls fail — both the callable and Firestore streams.

- timestamp: 2026-05-13T11:00:00Z
  checked: GiftRegistryApp.onCreate — app-level startup
  found: Only restores locale preference. No auth health-check, no sign-out-on-bad-token.
  implication: No startup recovery for stale auth sessions.

- timestamp: 2026-05-13T11:00:00Z
  checked: AppModule.provideFirebaseAuth — Hilt singleton
  found: Creates FirebaseAuth.getInstance(), wires useEmulator if USE_FIREBASE_EMULATOR=true.
         No token validation, no error handling.
  implication: Auth SDK is configured but not health-checked.

- timestamp: 2026-05-13T11:00:00Z
  checked: FirebaseAuthDataSource — authStateFlow and all methods
  found: authStateFlow uses AuthStateListener. Firebase restores the cached user from disk
         synchronously on listener attach (AuthStateEvent.Initial with user != null) WITHOUT
         validating the refresh token. signInAnonymously(), signInWithEmail() etc. are only
         called by user action (continueAsGuest(), signIn()). No token health-check anywhere.
  implication: The cached user object from a reset emulator session appears valid locally —
               user.isAnonymous is true, user.uid exists — but any Firebase server call
               (token refresh, Firestore, Functions) fails with INVALID_REFRESH_TOKEN.

- timestamp: 2026-05-13T11:00:00Z
  checked: AuthViewModel.init
  found: AuthStateEvent.Initial with user != null → emits AuthUiState.Authenticated immediately.
         App navigates to HomeKey, user can reach AddItemScreen. No subsequent check validates
         the token is actually usable.
  implication: The navigation gate (Authenticated → HomeKey) passes with a dead token.
               User appears signed in but every Firebase operation fails.

- timestamp: 2026-05-13T11:00:00Z
  checked: AppNavigation — auth flow
  found: authUiState == Authenticated → backStack cleared → HomeKey pushed. No path from
         Authenticated back to AuthKey unless signOut() is called. The app has no mechanism
         to detect INVALID_REFRESH_TOKEN and sign out automatically.
  implication: Once the stale-token state is entered, the user is permanently "authenticated"
               with a dead token until they manually sign out or uninstall the app.

- timestamp: 2026-05-13T11:00:00Z
  checked: ItemRepositoryImpl.fetchOgMetadata — the failing call site
  found: functions.getHttpsCallable("fetchOgMetadata").call(mapOf("url" to url)).await()
         wrapped in runCatching. The Functions SDK internally calls getIdToken before
         attaching the auth token to the callable request. getIdToken hits the auth emulator,
         which rejects the stale token → FirebaseException(INVALID_REFRESH_TOKEN). runCatching
         catches it → Result.failure → onFailure in AddItemViewModel → ogFetchFailed=true.
  implication: The callable failure is an auth failure, not a network or function logic failure.
               The URL is never even sent to the emulator's Functions service.

- timestamp: 2026-05-13T11:00:00Z
  checked: firebase.json emulator config — auth emulator port
  found: auth emulator on port 9099. AppModule wires auth.useEmulator(host, 9099). Ports match.
  implication: Auth emulator wiring is correct. The problem is stale token, not wrong port.

- timestamp: 2026-05-13T10:05:00Z
  checked: strings.xml for "couldn't read" error string
  found: R.string.item_og_fetch_failed_inline — only shown in AddItemScreen when ogFetchFailed=true
  implication: User DEFINITELY sees ogFetchFailed=true. onFailure() was called. Firebase callable threw.

- timestamp: 2026-05-13T10:06:00Z
  checked: Firebase emulator debug log
  found: Emulator running since 07:01 today. fetchOgMetadata registered and initialized.
         Functions invoked exactly 3 times — all from investigator's curl tests at 07:09-07:10.
         NO Android app invocations visible in log for entire session.
  implication: Android app either hasn't called fetchOgMetadata in this session, or the call
               fails before reaching the emulator (SDK-level exception).

- timestamp: 2026-05-13T10:08:00Z
  checked: Curl test: POST to http://127.0.0.1:5001/gift-registry-ro/europe-west3/fetchOgMetadata
  found: Returns {"result":{"title":"KALLAX Etajeră...","price":"229 RON",...}} in ~1.2s.
         Function correctly fetches IKEA.com and extracts OG metadata.
  implication: Function works correctly. Not the source of the bug.

- timestamp: 2026-05-13T10:09:00Z
  checked: Emulator log for "Outgoing network have been stubbed"
  found: Firebase emulator stubs http/https/net modules — but only for Firebase-internal calls.
         External HTTP calls (to ikea.com) are NOT blocked. Proven by curl test success.
  implication: Network stubbing is NOT blocking the external fetch.

- timestamp: 2026-05-13T10:10:00Z
  checked: fetchOgMetadata function error handling
  found: ALL errors are caught and return empty (all-null success). Function NEVER throws.
         If IKEA returns 403 → response.ok=false → returns empty.
         If fetch() throws (timeout, DNS, etc.) → catch block → returns empty.
  implication: The Firebase callable itself always succeeds (HTTP 200 from function).
               Android onFailure() can only be triggered by SDK-level exception (not function logic).

- timestamp: 2026-05-13T10:12:00Z
  checked: AppModule.kt — FirebaseFunctions DI configuration
  found: FirebaseFunctions.getInstance("europe-west3") with useEmulator(FIREBASE_EMULATOR_HOST, 5001)
         gated on USE_FIREBASE_EMULATOR. Emulator host defaults to "10.0.2.2" (AVD alias).
  implication: If user is on physical device AND built with default emulatorHost (10.0.2.2),
               the Functions callable would fail to connect and throw IOException.

- timestamp: 2026-05-13T10:13:00Z
  checked: quick task 260510-sai (configurable emulatorHost)
  found: Task was completed and verified on physical device. Adds -PemulatorHost=<LAN_IP> support.
         But requires explicit opt-in — default still "10.0.2.2" which fails on physical device.
  implication: Physical device testing requires passing -PemulatorHost. If user forgot, Functions
               callable fails. BUT this would also break Auth/Firestore, not just Functions.

## Resolution

root_cause: Stale Firebase Auth refresh token in the Android SDK's on-disk cache, with no
  recovery path in the app. After the Firebase emulator is reset (auth state cleared), the
  app cold-starts and the Firebase Auth SDK restores the previously cached anonymous user
  from local storage WITHOUT contacting the auth server. AuthViewModel sees user != null →
  emits Authenticated → navigation proceeds to HomeKey. When fetchOgMetadata is called,
  the Functions SDK internally calls getIdToken() to attach an auth token, which hits the
  auth emulator. The emulator rejects the stale refresh token with INVALID_REFRESH_TOKEN.
  The callable throws ExecutionException(FirebaseException(INVALID_REFRESH_TOKEN)).
  runCatching in ItemRepositoryImpl catches it → Result.failure → ogFetchFailed=true →
  "We couldn't read that page." The same error cascades to all other Firebase operations
  (Firestore watch streams also fail with UNAUTHENTICATED).

  This is fundamentally an emulator-reset workflow problem, but will also occur in production
  if a user's anonymous auth token expires or if Firebase invalidates a session for any reason.

  Note: the prior session's UA change, timeout increase, and ogFetchEmpty state machine are
  valid UX hygiene fixes for a distinct soft-failure case (function succeeds but returns no
  metadata). Those changes are retained as-is and are NOT reverted.

fix: |
  PRIMARY FIX — FirebaseAuthDataSource.kt:
    Added a getIdToken(forceRefresh=true) health-check on the first non-null AuthStateEvent.Initial
    event. If the call fails with any exception (INVALID_REFRESH_TOKEN, USER_NOT_FOUND,
    TOKEN_EXPIRED, etc.), auth.signOut() is called before emitting to authStateFlow. This
    collapses the "appears authenticated but all Firebase calls fail" window to zero — the
    sign-out forces an AuthStateEvent.Changed(null) which drives AuthViewModel to Unauthenticated
    and the nav gate back to AuthScreen. Safe in production: only fires on genuinely invalid sessions.

  SECONDARY UX FIXES (retained from prior session):
    - fetchOgMetadata.ts: realistic browser User-Agent, 10s timeout, emulator diagnostics logging
    - AddItemViewModel.kt: ogFetchEmpty state flow (soft failure when function returns all-null)
    - AddItemScreen.kt: third branch — "No details found for that URL — fill in below."
    - strings.xml / strings-ro.xml: item_og_no_data_inline; item_og_fetch_failed_inline updated
    - AddItemViewModelAutoFetchTest.kt: 4 new tests for ogFetchEmpty state machine

verification: |
  - Human-verified: cold-launch with stale refresh token now routes to AuthScreen; after
    re-authentication the IKEA URL fetch succeeds and pre-fills the form.
  - All 29 Android unit tests pass (./gradlew :app:testDebugUnitTest, BUILD SUCCESSFUL)
  - All 78 Cloud Functions tests pass (npm test in functions/)
  - Curl test confirms IKEA URL returns valid OG data through updated function
  - TypeScript compilation successful (npm run build)

files_changed:
  - app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt
  - functions/src/registry/fetchOgMetadata.ts
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
  - app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelAutoFetchTest.kt
