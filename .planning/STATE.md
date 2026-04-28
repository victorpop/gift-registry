---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: "Milestone: GiftMaison visual refresh"
status: verifying
stopped_at: Completed 12-05-PLAN.md (Phase 12 Wave 3 — UAT approved; storage rules deploy deferred)
last_updated: "2026-04-28T19:27:00.000Z"
last_activity: 2026-04-28
progress:
  total_phases: 12
  completed_phases: 12
  total_plans: 61
  completed_plans: 61
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-20)

**Core value:** Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.
**Current focus:** Phase 12 — registry-cover-photo-themed-placeholder

## Current Position

Phase: 12 (registry-cover-photo-themed-placeholder) — EXECUTING
Plan: 5 of 5
Status: Phase complete — ready for verification
Last activity: 2026-04-28

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: 0
- Average duration: —
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**

- Last 5 plans: —
- Trend: —

*Updated after each plan completion*
| Phase 01-firebase-foundation P01 | 15 | 2 tasks | 9 files |
| Phase 01-firebase-foundation P03 | 3 | 2 tasks | 6 files |
| Phase 01-firebase-foundation P02 | 8min | 2 tasks | 6 files |
| Phase 02-android-core-auth P01 | 3min | 2 tasks | 17 files |
| Phase 02-android-core-auth P02 | 6min | 2 tasks | 15 files |
| Phase 02-android-core-auth P03 | 24min | 2 tasks | 11 files |
| Phase 02-android-core-auth P04 | 4min | 2 tasks | 6 files |
| Phase 03-registry-item-management P02 | 5min | 2 tasks | 4 files |
| Phase 03-registry-item-management P01 | 4min | 2 tasks | 13 files |
| Phase 03-registry-item-management P03 | 12 | 2 tasks | 18 files |
| Phase 03-registry-item-management P05 | 8min | 2 tasks | 16 files |
| Phase 04-reservation-system P01 | 2min | 2 tasks | 9 files |
| Phase 04-reservation-system P02 | 1min | 2 tasks | 3 files |
| Phase 04-reservation-system P03 | 2min | 2 tasks | 9 files |
| Phase 04-reservation-system P04 | 3min | 3 tasks | 9 files |
| Phase 04-reservation-system P05 | 8min | 2 tasks | 7 files |
| Phase 04-reservation-system P06 | 4min | 3 tasks | 9 files |
| Phase 05-web-fallback P01 | 8min | 2 tasks | 21 files |
| Phase 05-web-fallback P02 | 2min | 2 tasks | 4 files |
| Phase 05-web-fallback P03 | 2min | 2 tasks | 14 files |
| Phase 05-web-fallback P04 | 10min | 3 tasks | 13 files |
| Phase 05-web-fallback P05 | 3min | 2 tasks | 9 files |
| Phase 05-web-fallback P06 | 7min | 2 tasks | 13 files |
| Phase 05-web-fallback P07 | 7min | 2 tasks | 8 files |
| Phase 06-notifications-email-flows P00 | 7min | 3 tasks | 11 files |
| Phase 06-notifications-email-flows P03 | 4min | 3 tasks | 14 files |
| Phase 06-notifications-email-flows P01 | 7min | 2 tasks | 14 files |
| Phase 06-notifications-email-flows P02 | 10min | 2 tasks | 5 files |
| Phase 06 P05 | 4min | 3 tasks | 9 files |
| Phase 06-notifications-email-flows P04 | 12min | 3 tasks | 15 files |
| Phase 07-romanian-store-browser P00 | 7min | 3 tasks | 13 files |
| Phase 07-romanian-store-browser P01 | 8min | 2 tasks | 12 files |
| Phase 07-romanian-store-browser P02 | 3min | 2 tasks | 9 files |
| Phase 07-romanian-store-browser P03 | 12min | 3 tasks | 8 files |
| Phase 08-giftmaison-design-foundation P01 | 5min | 2 tasks | 8 files |
| Phase 09-shared-chrome-status-ui P01 | 2min | 2 tasks | 5 files |
| Phase 09-shared-chrome-status-ui P02 | 3min | 2 tasks | 6 files |
| Phase 09-shared-chrome-status-ui P03 | 4min | 2 tasks | 5 files |
| Phase 10-onboarding-home-redesign P01 | 2 | 2 tasks | 6 files |
| Phase 10-onboarding-home-redesign P02 | 45min | 2 tasks | 8 files |
| Phase 10 P03 | 4min | 2 tasks | 6 files |
| Phase 10-onboarding-home-redesign P04 | 3min | 2 tasks | 8 files |
| Phase 11-registry-detail-create-add-item-redesign P01 | 8min | 2 tasks | 7 files |
| Phase 11-registry-detail-create-add-item-redesign P02 | 4min | 2 tasks | 8 files |
| Phase 11 P03 | 5min | 2 tasks | 8 files |
| Phase 11 P04 | 4min | 2 tasks | 6 files |
| Phase 11 P05 | 5min | 2 tasks | 11 files |
| Phase 12-registry-cover-photo-themed-placeholder P01 | 8min | 3 tasks | 19 files |
| Phase 12-registry-cover-photo-themed-placeholder P03 | 10min | 3 tasks | 7 files |
| Phase 12-registry-cover-photo-themed-placeholder P02 | 9min | 4 tasks | 52 files |
| Phase 12-registry-cover-photo-themed-placeholder P04 | 11min | 3 tasks | 12 files |
| Phase 12-registry-cover-photo-themed-placeholder P05 | 12min | 3 tasks | 4 files |

## Accumulated Context

### Roadmap Evolution

- Phase 12 added: Registry Cover Photo & Themed Placeholder

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Init: Firebase over SQLite — real-time sync required for shared registries
- Init: Cloud Tasks (not cron) for reservation expiry — per-reservation precision required
- Init: Affiliate URL transformation in Cloud Functions only — never in APK; allows merchant rule updates without Play Store release
- Init: EMAG catalog phase deliberately last — no confirmed public catalog API; manual URL add is the primary path
- [Phase 01-firebase-foundation]: firebase-functions/v2 import pattern (2nd gen Cloud Functions) — 1st gen deprecated
- [Phase 01-firebase-foundation]: Emulator Suite singleProjectMode=true on ports Auth=9099, Functions=5001, Firestore=8080, Hosting=5000, UI=4000
- [Phase 01-firebase-foundation]: Feature-namespaced key convention established: app_, common_, auth_, registry_, reservation_ prefixes prevent key collision across features
- [Phase 01-firebase-foundation]: assetlinks.json uses PLACEHOLDER values — will be updated in Phase 2 (package name) and Phase 5 (SHA-256 fingerprint)
- [Phase 01-firebase-foundation]: invitedUsers map (not array) for O(1) membership check in Firestore security rules
- [Phase 01-firebase-foundation]: reservations collection hard-deny (allow read, write: if false) — Admin SDK bypasses rules for Cloud Functions
- [Phase 01-firebase-foundation]: users delete disabled — account deletion requires backend business logic, not raw client delete
- [Phase 02-android-core-auth]: KSP over KAPT for Hilt — AGP 9 built-in Kotlin makes KAPT incompatible; KSP 2.3.6 is the only viable annotation processor path
- [Phase 02-android-core-auth]: AppCompatActivity (not ComponentActivity) — required for AppCompatDelegate.setApplicationLocales() locale switching (I18N-UX-03)
- [Phase 02-android-core-auth]: Static seed color #6750A4, no dynamic color — ensures consistent brand identity across all Android versions including pre-Android 12
- [Phase 02-android-core-auth]: No org.jetbrains.kotlin.android plugin — AGP 9.1.0 has Kotlin built in; adding the plugin causes duplicate plugin application error
- [Phase 02-android-core-auth]: Zero Firebase imports in domain layer — pure Kotlin interfaces only, keeping domain testable without Android/Firebase dependencies
- [Phase 02-android-core-auth]: callbackFlow with awaitClose for Firebase AuthStateListener bridging — ensures listener cleanup on Flow cancellation
- [Phase 02-android-core-auth]: runCatching wraps Firebase suspend calls in AuthRepositoryImpl — converts exceptions to Result.failure keeping FirebaseExceptions out of domain
- [Phase 02-android-core-auth]: drop(1) on ObserveAuthStateUseCase Flow prevents auth screen flash on session restore
- [Phase 02-android-core-auth]: rememberViewModelStoreNavEntryDecorator absent from Navigation3 1.0.1 stable; hiltViewModel() uses Activity ViewModelStoreOwner
- [Phase 02-android-core-auth]: SignOutUseCase added to domain layer (was missing from Plan 02 use case list)
- [Phase 02-android-core-auth]: SharingStarted.Eagerly for SettingsViewModel.currentLocale — immediate emission on ViewModel init ensures stored locale reflects before any UI subscriber attaches
- [Phase 03-registry-item-management]: inviteToRegistry uses email: prefix for non-user invite keys to prevent collision with Firebase UIDs in invitedUsers map
- [Phase 03-registry-item-management]: fetchOgMetadata returns null fields on fetch failure so Android client can fall back to manual entry without HttpsError overhead
- [Phase 03-registry-item-management]: node-html-parser chosen over cheerio for smaller bundle size in Node.js 22 Cloud Functions runtime
- [Phase 03-registry-item-management]: AffiliateUrlTransformer uses placeholder affiliate IDs that must be replaced with BuildConfig fields before production
- [Phase 03-registry-item-management]: merchantRules map pattern in AffiliateUrlTransformer enables merchant extensibility without changing core logic
- [Phase 03-registry-item-management]: FirestoreDataSource mirrors FirebaseAuthDataSource pattern: callbackFlow + awaitClose for real-time Firestore observation
- [Phase 03-registry-item-management]: AffiliateUrlTransformer applied in ItemRepositoryImpl.addItem (data layer), not in AddItemUseCase, to keep affiliate logic out of domain
- [Phase 03-registry-item-management]: Registry screens (Plan 04 work) built as part of Plan 05 — parallel executor ran before Plan 04 dependency
- [Phase 03-registry-item-management]: deepLinkRegistryId extracted in MainActivity.onCreate and passed to AppNavigation for REG-08 cold-start deep link support
- [Phase 03-registry-item-management]: InviteBottomSheet resets inviteSent via resetInviteSent() — allows sending multiple invites per bottom sheet session
- [Phase 04-reservation-system]: createReservation stub omits CloudTasksClient import — dependency pre-wired in package.json but import deferred to Plan 02 to keep stub minimal
- [Phase 04-reservation-system]: releaseReservation throws Error (not HttpsError) since onTaskDispatched handlers do not use HttpsError
- [Phase 04-reservation-system]: createReservation wraps CloudTasksClient.createTask in try/catch so emulator build works without Cloud Tasks; cloudTaskName stored as empty string in transaction, updated via post-transaction .update()
- [Phase 04-reservation-system]: Guard transaction pattern for releaseReservation: status==active check + now>expiresAt check before any writes — idempotent handler survives Cloud Tasks retry
- [Phase 04-reservation-system]: GuestPreferencesDataStore uses DataStore name='guest_prefs' — unique Context extension property prevents runtime IllegalStateException from duplicate DataStore initialization
- [Phase 04-reservation-system]: GuestPreferencesDataStoreTest uses in-memory fake (not Robolectric) — verifies repository contract without Android framework; DataStore persistence verified manually per 04-VALIDATION.md
- [Phase 04-reservation-system]: ReservationRepositoryImpl payload includes giverId as nullable — supports both anonymous guests (null) and authenticated users passing Firebase UID
- [Phase 04-reservation-system]: ReservationEvent as Channel<BUFFERED> in ViewModel -- one-shot events collected in LaunchedEffect(Unit) so Intent fires exactly once per reservation
- [Phase 04-reservation-system]: Item domain model lacks expiresAt field -- countdown deferred, showing only Reserved label; no domain model changes made in Plan 04
- [Phase 04-reservation-system]: ReReserveDeepLink NavEntry navigates to HomeKey with Phase 6 TODO -- placeholder route exists so email stub URL is well-formed (RES-08 partial)
- [Phase 04-reservation-system]: doc.getTimestamp('expiresAt') used for reading server-written Timestamps — POJO auto-mapping to Long fails silently
- [Phase 04-reservation-system]: expiresAt excluded from Item.toMap()/toUpdateMap() — client must never write this field (D-08: no client-side timer authority)
- [Phase 04-reservation-system]: item.expiresAt?.let{} pattern for countdown display — backward compat with legacy reserved items that predate the field
- [Phase 04-reservation-system]: ReservationDeepLinkBus SharedFlow(replay=1) chosen over adding autoReserveItemId to RegistryDetailKey — avoids Navigation3 @Serializable default-value complications
- [Phase 04-reservation-system]: resolveReservation Cloud Function has no auth guard — giver arrives from email link unauthenticated (guest flow); returns only opaque navigation IDs, no PII
- [Phase 05-web-fallback]: Tailwind v3 (3.4.19) pinned for web scaffold — v4 changed config format to CSS-first; all UI-SPEC examples use v3 tailwind.config.ts syntax
- [Phase 05-web-fallback]: i18next ^26.0 — npm-verified current version; CLAUDE.md reference to v24 is stale per RESEARCH.md
- [Phase 05-web-fallback]: assetlinks.json migrated to web/public/.well-known/ — emptyOutDir: true deletes hosting/public/ on each Vite build; web/public/ is Vite static asset source
- [Phase 05-web-fallback]: ES module import hoisting means firebase.ts singletons are constructed before debug token code runs, but initializeAppCheck is called in module body before createRoot — network calls only fire when components read/write, after App Check is active
- [Phase 05-web-fallback]: VITE_RECAPTCHA_SITE_KEY absence in dev skips App Check with console.warn — no crash, frictionless local dev without reCAPTCHA registration
- [Phase 05-web-fallback]: i18n import order in main.tsx: import './i18n' at line 6, import App at line 7 — guarantees i18next initialized synchronously before any React component mounts
- [Phase 05-web-fallback]: React Router v7 import surface: from 'react-router' not 'react-router-dom' — v7 merged dom into main package; all Plan 04+ hooks import from 'react-router'
- [Phase 05-web-fallback]: Legacy seed sync: web/i18n/*.json kept byte-identical to web/src/i18n/*.json — prevents drift for any tool referencing the legacy seed path
- [Phase 05-web-fallback]: data: undefined|null|Registry semantics in useRegistryQuery — undefined=loading skeleton, null=not-found OR permission-denied (WEB-D-13+D-14), Registry=success
- [Phase 05-web-fallback]: Test pattern: await waitFor(handle not null) before firing onNext — useEffect registers callbacks asynchronously; calling before registration yields null data
- [Phase 05-web-fallback]: ItemCard.reserveSlot + ItemGrid.renderReserve extension points — Plan 06 injects real ReserveButton; Plan 04 renders disabled placeholder with min-h-[48px] touch target
- [Phase 05-web-fallback]: authProviders.ts catches auth/popup-closed-by-user and auth/cancelled-popup-request silently returning null — caller no-ops instead of showing error toast
- [Phase 05-web-fallback]: AuthModal Dialog.Description added as sr-only element — Radix warns when no description present; both modals have accessible descriptions
- [Phase 05-web-fallback]: httpsCallable called at module level (not inside hook) — callable is singleton; test must not reset httpsCallableMock in beforeEach
- [Phase 05-web-fallback]: useActiveReservation uses React state only (not localStorage) — tab-scoped UX acceptable for 30-min window per CONTEXT.md discretion
- [Phase 05-web-fallback]: RegistryPage/App tests: vi.mock all reservation + auth imports to avoid firebase/auth initialization in jsdom test environment
- [Phase 05-web-fallback]: useResolveReservation calls httpsCallable inside hook (not module level) — only used on ReReservePage, no singleton benefit; test mocks apply at call time without beforeEach reset constraint
- [Phase 05-web-fallback]: autoReserveFiredRef.current set before GuestIdentityModal open — prevents double-fire between setState calls; same ref prevents StrictMode double-effect
- [Phase 06-notifications-email-flows]: firebase-functions-test requires --legacy-peer-deps for npm install due to peer dep range declaration on firebase-admin (functional at runtime with admin@13.x)
- [Phase 06-notifications-email-flows]: mail + notifications_failures collections hard-deny all client access; fcmTokens subcollection owner-only via isSignedIn() + uid match
- [Phase 06-notifications-email-flows]: relaxed=true required for MockK mocking of final Firebase classes on Java 25 — Byte Buddy inline instrumentation does not support Java 25 bytecode
- [Phase 06-notifications-email-flows]: FCM token registration silently no-ops for anonymous users (return@runCatching Unit) — tokens are tied to user accounts
- [Phase 06-notifications-email-flows]: sendEmail validates to+subject presence and throws Error; locale defaults to en for both expiry and invite emails when recipient locale unknown (D-14 fallback)
- [Phase 06-notifications-email-flows]: invitePush handles both FCM throw and batch.commit cleanup failure independently, each writing to notifications_failures — best-effort with no error propagation
- [Phase 06-notifications-email-flows]: confirmPurchase has no auth guard — guest givers (anonymous) may confirm purchase per CONTEXT.md guest access constraint
- [Phase 06-notifications-email-flows]: Cloud Task deleteTask called after transaction commits in confirmPurchase — never inside runTransaction (Pitfall 2)
- [Phase 06-notifications-email-flows]: notifyOnPurchase defaults true when absent from registry doc — opt-out is explicit, opted-in by default
- [Phase 06]: useConfirmPurchase uses plain useState/useCallback (not TanStack useMutation) — httpsCallable created inside confirm() callback so test mocks bind at call time without beforeEach reset constraint
- [Phase 06]: ConfirmPurchaseBanner requires ConfirmPurchaseBanner mock in all RegistryPage test files to prevent transitive firebase.ts init failure in jsdom
- [Phase 06-notifications-email-flows]: MessagingHandler extracted as plain class for unit-testability — GiftRegistryMessagingService delegates; @AndroidEntryPoint(FirebaseMessagingService::class) + Hilt_GiftRegistryMessagingService required by KSP
- [Phase 06-notifications-email-flows]: SnackbarMessage sealed interface (Resource/Push) in RegistryDetailViewModel unifies confirm-purchase + FCM push snackbar routing; NotificationBus re-emitted via VM to keep Compose unaware of singleton bus
- [Phase 06-notifications-email-flows]: hasActiveReservation derived from combine(items, observeGuestIdentity) — no new DataStore reservation ID key needed for banner visibility
- [Phase 07-romanian-store-browser]: tsconfig include expanded to ['src', 'scripts'] — seed script in functions/scripts/ was excluded from compilation scope; build would fail without this change
- [Phase 07-romanian-store-browser]: Placeholder 1x1 transparent WebP files (34 bytes each) committed for all 9 store logos — real retailer logos must be supplied before v1.0 production release
- [Phase 07-romanian-store-browser]: StoresModule created as separate @Module (not added to DataModule) — keeps Phase 7 bindings discoverable and avoids touching DataModule which other phases may also modify
- [Phase 07-romanian-store-browser]: Manual snapshot.get('stores') cast over toObject — Firestore POJO mapper does not reliably map top-level arrays; confirmed by Research Pattern 2
- [Phase 07-romanian-store-browser]: DataStore name 'last_registry_prefs' verified unique against existing names (guest_prefs, language_prefs, onboarding_prefs) per Research Pitfall 3
- [Phase 07-romanian-store-browser]: StoreListKey declared as data class (not data object) to carry optional preSelectedRegistryId — enables registry-aware navigation from RegistryDetail without a separate nav key
- [Phase 07-romanian-store-browser]: entry<StoreBrowserKey> intentionally deferred to Plan 03 — tapping a store card is no-op in intermediate state, Navigation3 renders no content for unregistered keys without crashing
- [Phase 07-romanian-store-browser]: WebView ref held in Composable (remember mutableStateOf) not ViewModel — prevents Activity context leak; LaunchedEffect(homepageUrl) triggers initial load to avoid recomposition reloads in update lambda
- [Phase 07-romanian-store-browser]: External scheme blocking uses Toast inside shouldOverrideUrlLoading (MVP); Snackbar follow-up requires dedicated StateFlow — documented as known deviation
- [Phase 07-romanian-store-browser]: Add-to-list button disabled when registryId is null (Home-FAB entry path) — guards AddItemKey dispatch; D-10 registry picker is the follow-up
- [Phase 08-giftmaison-design-foundation]: Wave 0 TDD RED pattern: every phase requirement ships with an automated failing test before any implementation exists; Waves 1-2 must flip tests RED→GREEN to prove the handoff contract is satisfied
- [Phase 08-giftmaison-design-foundation]: Compose Google Fonts BOM-pinning: compose-ui-text-google-fonts alias declared without version.ref so BOM 2026.03.00 resolves it to 1.10.5 alongside other androidx.compose.ui:* modules
- [Phase 08-giftmaison-design-foundation]: Wordmark is unit-tested as a pure-Kotlin factory wordmarkAnnotatedString(ink, accent): AnnotatedString so Wave 2 must factor the @Composable GiftMaisonWordmark over this testable helper
- [Phase 08-giftmaison-design-foundation]: TypographyTest includes an explicit Pitfall 2 guard (everyRole_disablesFontPadding_pitfall2) asserting PlatformTextStyle(includeFontPadding = false) on all 10 type roles — catches future em-lineHeight regressions
- [Phase 09-shared-chrome-status-ui]: Wave 0 pure-Kotlin unit tests only: Compose UI test scaffolding deferred; unit tests cover logic; visual verification via StyleGuidePreview in Plan 04
- [Phase 09-shared-chrome-status-ui]: PulsingDot constants exposed as top-level const vals for Compose-framework-free unit testing; Plan 02 contract is PULSING_DOT_DEFAULT_PERIOD_MS=1400L + alpha 1f/0.5f + scale 1f/0.85f
- [Phase 09-shared-chrome-status-ui]: Asymmetric domain→display mapping pinned by test: ItemStatus.AVAILABLE→OPEN, ItemStatus.RESERVED→RESERVED, ItemStatus.PURCHASED→GIVEN (PURCHASED→GIVEN is highest-risk API pitfall)
- [Phase 09-shared-chrome-status-ui]: NavVisibility.kt stub created in Plan 02 to unblock BottomNavVisibilityTest compilation — all unit tests share a single compile step; correct showsBottomNav() logic available for Plan 03 to build on
- [Phase 09-shared-chrome-status-ui]: showsBottomNav() kept in NavVisibility.kt (Plan 02 stub) rather than merged into GiftMaisonBottomNav.kt — no duplicate declaration, tests already passing
- [Phase 09-shared-chrome-status-ui]: Icons.Outlined.KeyboardArrowRight used instead of Icons.AutoMirrored.Filled.ChevronRight — ChevronRight not in project Material Icons extended set
- [Phase 09-shared-chrome-status-ui]: showsBottomNav() import replaces local predicate — semantic change from persistent-everywhere to HomeKey+RegistryDetailKey only (intentional Phase 9 cutover per D-03)
- [Phase 09-shared-chrome-status-ui]: SuggestionChip imports kept in RegistryDetailScreen — file uses SuggestionChip for registry.occasion label in header; only inline ItemStatusChip composable deleted
- [Phase 10-onboarding-home-redesign]: Calendar-based startOfTodayMs (not LocalDate) for minSdk 23 compat — LocalDate.atStartOfDay() requires API 26
- [Phase 10-onboarding-home-redesign]: AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true flips current false per CONTEXT.md D-02 (first-run new user creates account)
- [Phase 10-onboarding-home-redesign P02]: Canvas over layered Box for ConcentricRings — rings anchor at corner Offset(width, 0) and natural bounds-clip achieves handoff overflow:hidden
- [Phase 10-onboarding-home-redesign P02]: Provisional string refs: auth_google_sign_in_button (GoogleBanner) + auth_settings_title (AvatarButton) — both exist in strings.xml; Plans 03/04 rewire to final keys
- [Phase 10-onboarding-home-redesign P02]: FocusedFieldCaret: 1.1 s InfiniteRepeatableSpec RepeatMode.Reverse, opacity-only — distinct from PulsingDot 1.4 s opacity+scale
- [Phase 10]: AuthUiState.kt stubs from Plan 02 were correct (firstName/lastName + AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE=true) — no edit needed in Plan 03
- [Phase 10]: Scaffold/SnackbarHost dropped from AuthScreen — inline warn-banner (colors.warn 0.15 alpha) replaces snackbar per RESEARCH.md Pattern 2
- [Phase 10]: AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true exposed as top-level const so AuthFormStateTest can assert the flip without reading Compose state
- [Phase Phase 10-onboarding-home-redesign]: onNavigateToNotifications kept as no-op default in RegistryListScreen — inbox still reachable via deep link; bell placement deferred to Phase 11 per CONTEXT.md
- [Phase Phase 10-onboarding-home-redesign]: Tab index uses Int via rememberSaveable mutableIntStateOf(0), not sealed class, per RESEARCH.md Pitfall 3
- [Phase 11]: Wave 0 test package = impl package: tests in com.giftregistry.ui.registry.detail etc. so unqualified symbol references work once Plan 02 ships
- [Phase 11]: HeroToolbarAlpha Pitfall 1 guard pinned: firstVisibleItemIndex >= 1 must short-circuit to 1f to prevent toolbar flash-to-transparent when hero scrolls off-screen
- [Phase 11]: Legacy alias map pinned: Baby shower -> Baby, Anniversary -> Housewarming for backward compat with pre-Phase-11 Firestore docs
- [Phase 11]: ADD_ITEM_MODE_DEFAULT_ORDINAL exposed as top-level const val per Phase 10 rememberSaveable Int-backed state pattern; ordinal=0 maps to PasteUrl
- [Phase 11]: isAffiliateDomain inserted between extractDomain() and noMatch() in AffiliateUrlTransformer — additive, merchantRules stays private, transform() unchanged
- [Phase 11]: OccasionCatalog legacy aliases use lowercase keys so storageKeyFor() normalises via .lowercase() — handles any casing variant from legacy Firestore docs
- [Phase 11]: ShareBanner.onShared is a non-suspend lambda; caller wraps scope.launch to call showSnackbar — compile error auto-fixed
- [Phase 11]: onSkip default no-op on CreateRegistryScreen: EditRegistryKey entry backward-compat with zero changes
- [Phase 11]: Skip-save pattern: set 'Untitled draft' placeholder before onSave() to pass VM title.length 3..50; isDraft classified by itemCount==0
- [Phase 11]: addAnotherMode flag in Composable (not ViewModel) separates save+reset from save+pop — keeps ViewModel pure; flag resets after LaunchedEffect branch fires
- [Phase 11]: LaunchedEffect(selectedTab) resets selectedTabIndex before calling onNavigateToBrowseStores — ensures re-entry shows PasteUrl tab
- [Phase 12]: Wave 0 RED uses stub-bodies pattern (compile-but-fail-on-assertion) — Plans 02/03/04 flip RED→GREEN by replacing stub bodies, never editing tests
- [Phase 12]: RegistryRepository.newRegistryId() added as default-method (returns empty string) — D-07 hook without forcing edits to RegistryRepositoryImpl + FakeRegistryRepository
- [Phase 12]: D-07 + Pitfall 2 ordering pinned via coVerifyOrder { uploadCover; createRegistry } in CreateRegistryViewModelCoverTest — Plan 04 must satisfy via VM impl, NEVER edit the test
- [Phase 12]: firebase-storage 22.0.1 resolved via Firebase BoM 34.11.0 (no KTX); Storage Emulator port 9199 wired; storage.rules deferred to Plan 02
- [Phase 12]: CoverImageProcessorTest @Ignored at class level — Robolectric not on testRuntimeClasspath; Plan 02 picks fake-Bitmap or instrumented variant for D-06 size invariants
- [Phase 12-registry-cover-photo-themed-placeholder]: HeroImageOrPlaceholder accepts optional ColorFilter parameter — applied ONLY to AsyncImage branch so gradient placeholder pops at full brightness on dark-ink primary card
- [Phase 12-registry-cover-photo-themed-placeholder]: CoverPhotoPickerInline + CoverPhotoPickerSheet are string-resource-agnostic — caller (Plan 04) passes stringResource(...) values via parameters, keeping primitives reusable in @Preview
- [Phase 12-registry-cover-photo-themed-placeholder]: Pitfall 6 honored: 3-stop dark overlay stays at RegistryDetailHero call site gated on imageUrl != null; HeroImageOrPlaceholder owns ONLY the gradient+glyph fallback, never the dark overlay
- [Phase 12-registry-cover-photo-themed-placeholder]: Pitfall 1 imageUrl roundtrip fixed in RegistryDto + RegistryRepositoryImpl.toMap/toUpdateMap/toDomain — cover URL persists through Firestore create/update/observe
- [Phase 12-registry-cover-photo-themed-placeholder]: RegistryRepositoryImpl.newRegistryId() wired via firestore.collection('registries').document().id with FirebaseFirestore injected as third constructor param — D-07 enabler for Plan 12-04 upload-then-write
- [Phase 12-registry-cover-photo-themed-placeholder]: StorageDataSource split holds the D-05 path schema; StorageRepositoryImpl is a thin runCatching wrapper — keeps FirebaseExceptions out of the domain (Phase 02 D-08)
- [Phase 12-registry-cover-photo-themed-placeholder]: 36 placeholder JPEGs generated with Pillow (1280x720, ~12 KB each, GiftMaison palette base ± 12 brightness per index) — CONTEXT D-02 fallback acceptable; curation follow-up todo logged
- [Phase 12-registry-cover-photo-themed-placeholder]: storage.rules cross-service rules use firestore.get() to mirror firestore.rules canReadRegistry — same legacy-doc defaults for visibility/invitedUsers; deploy deferred to Plan 12-05 human checkpoint
- [Phase 12-registry-cover-photo-themed-placeholder]: CreateRegistryViewModel onSave pre-mints registryId via registryRepository.newRegistryId(), uploads BEFORE Firestore write, returns early on failure (zero orphan documents)
- [Phase 12-registry-cover-photo-themed-placeholder]: RegistryRepositoryImpl.createRegistry honours pre-set registry.id via new FirestoreDataSource.createRegistryWithId; legacy id='' callers fall through to auto-mint
- [Phase 12-registry-cover-photo-themed-placeholder]: D-13 owner-only tap on RegistryDetailHero implemented via nullable onCoverTap callback — guests pass null, clickable(enabled = false) is a no-op (no ripple, no pressed state)
- [Phase 12-registry-cover-photo-themed-placeholder]: CreateRegistryViewModelCoverTest contract assertions (coVerifyOrder, exactly=0, failure assertions) preserved verbatim; only Wave 0 fail() markers + simulated state replaced with VM-driven exercise
- [Phase 12-registry-cover-photo-themed-placeholder]: Cover-photo selection rehydration on Detail VM placed in second init { } block AFTER val registry — Kotlin runs init blocks in source order, before subsequent property initializers
- [quick-260428-tx9]: M3 Compose DatePickerDialog / AlertDialog{TimePicker} replace framework android.app dialogs on CreateRegistryScreen — framework dialogs read the system Android theme (default green/teal) bypassing MaterialTheme.colorScheme; M3 Compose pickers inherit colorScheme.primary (= gm.accent terracotta via LightColorScheme) with zero `colors=` overrides
- [quick-260428-tx9]: rememberDatePickerState UTC-midnight ↔ local-Calendar Long: convert symmetrically at the picker call site (decode UTC to civil Y/M/D; re-encode locally preserving prevHour/prevMin) — Bucharest UTC+2/+3 off-by-one guard
- [quick-260428-tx9]: M3 has no TimePickerDialog composable; documented standard is AlertDialog { TimePicker(state) } with confirmButton/dismissButton TextButtons calling viewModel.setEventTime(state.hour, state.minute)
- [quick-260428-tx9]: stringResource(android.R.string.ok / .cancel) over new strings.xml keys for picker buttons — system labels are translated by the platform on RO devices, no app keys needed
- [quick-260428-v0q]: `Any?.showsBottomNav()` inverted from visible-whitelist (HomeKey + RegistryDetailKey only — Plan 09-02 D-03) to hidden-whitelist (null, AuthKey, OnboardingKey, ReReserveDeepLink only) — bottom nav is now persistent on every authenticated destination; default-true makes any future post-auth nav key show the bar without an explicit add; AppNavigation.kt:128 consumer call site untouched; pinned by 14-case BottomNavVisibilityTest
- [Phase 12-registry-cover-photo-themed-placeholder]: Plan 12-05: 4 StyleGuidePreview @Preview sections shipped (HeroImageOrPlaceholder hero+card, CoverPhotoPickerInline 3 states, CoverPhotoPickerSheet inline-body, RegistryCard placeholder regression check) — Phase 12 visual contracts now reviewable in Studio without booting an emulator
- [Phase 12-registry-cover-photo-themed-placeholder]: Plan 12-05: storage rules live deploy DEFERRED via structured todo (.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md) — user resume signal was 'approved — storage deploy skipped'; production cover-photo upload traffic blocked until the deploy lands; preset selection paths unaffected
- [Phase 12-registry-cover-photo-themed-placeholder]: Plan 12-05: 12-VALIDATION.md signed off (status=approved, nyquist_compliant=true, wave_0_complete=true); Per-Task Verification Map has 15 rows; 7 of 8 Wave 0 RED suites GREEN; all 16 Decision IDs satisfied; Pitfalls 1+2+5+6+7 pinned

### Pending Todos

- [2026-04-20 general] Register Firebase Web app and deploy real web config — `https://gift-registry-ro.web.app` blank because no Web app registered + missing `web/.env.local` → `initializeApp({})` throws. See `.planning/todos/pending/2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md`
- [2026-04-20 ui] Group registries by ownership and clarify invitee permissions — after shared-with-me query shipped, add "Your lists" / "Shared with you" section headers on Home + audit Detail screen affordances (FAB, Invite, Edit, Delete) for invitee-aware hide/disable. See `.planning/todos/pending/2026-04-20-group-registries-by-ownership-and-clarify-invitee-permissions.md`
- [2026-04-20 tooling] Fix functions tsconfig and env handling to unblock firebase deploy — `functions/tsconfig.json` produces `lib/src/*.js` instead of `lib/*.js`; `defineString` balks in non-interactive deploys. Blocks production deploy of quick-260420-ozb notifications work. See `.planning/todos/pending/2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md`

### Blockers/Concerns

- ~~Phase 7 (EMAG Catalog): EMAG has no confirmed public product catalog API~~ — **Resolved 2026-04-19**: Phase 7 re-scoped to "Romanian Store Browser" (in-app WebView + persistent Add-to-list CTA). Retires ITEM-03/04 and introduces STORE-01..04.
- ~~Phase 4 (Reservation): Cloud Tasks cancellation API~~ — **Resolved**: `cloudTaskName` pattern verified and working (used in createReservation + confirmPurchase with NOT_FOUND swallowing).

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260419-ubj | Onboarding carousel (3 slides) before auth screen | 2026-04-19 | e1aeb0f | [260419-ubj-onboarding-carousel-3-slides-before-auth](./quick/260419-ubj-onboarding-carousel-3-slides-before-auth/) |
| 260420-gat | Harden firestore.rules against missing fields on legacy registry docs | 2026-04-20 | 05207a6 | [260420-gat-harden-firestore-rules-against-missing-f](./quick/260420-gat-harden-firestore-rules-against-missing-f/) |
| 260420-gv2 | Gate Firebase emulator wiring on `-Puse_emulator` Gradle property | 2026-04-20 | 9c96e55 | [260420-gv2-gate-firebase-emulator-wiring-on-puse-em](./quick/260420-gv2-gate-firebase-emulator-wiring-on-puse-em/) |
| 260420-hcw | Wire Coil image rendering — preview in AddItemScreen + thumbnails in item list | 2026-04-20 | 75d4e61 | [260420-hcw-wire-coil-image-rendering-preview-in-add](./quick/260420-hcw-wire-coil-image-rendering-preview-in-add/) |
| 260420-hgb | Collapse two language pickers into one — single picker drives both app UI locale and Firestore preferredLocale for emails | 2026-04-20 | f069c8e | [260420-hgb-remove-the-email-language-setting-from-s](./quick/260420-hgb-remove-the-email-language-setting-from-s/) |
| 260420-hua | Bottom nav (Home, Add list, Browse stores, Preferences) + direct-create FAB on home | 2026-04-20 | be2f66b | [260420-hua-bottom-nav-home-add-list-browse-stores-p](./quick/260420-hua-bottom-nav-home-add-list-browse-stores-p/) |
| 260420-iic | Fix stale-UID bug — make RegistryListViewModel reactive to authState | 2026-04-20 | 4ff3973 | [260420-iic-fix-stale-uid-bug-make-registrylistviewm](./quick/260420-iic-fix-stale-uid-bug-make-registrylistviewm/) |
| 260420-iro | Persistent bottom nav across all screens + RegistryDetail FAB direct-opens Add Item form | 2026-04-20 | 9a842f4 | [260420-iro-persistent-bottom-nav-across-all-screens](./quick/260420-iro-persistent-bottom-nav-across-all-screens/) |
| 260420-jlg | Invite bottom sheet — show success confirmation and auto-dismiss instead of silent flip | 2026-04-20 | 2f17c7e | [260420-jlg-invite-bottom-sheet-show-success-confirm](./quick/260420-jlg-invite-bottom-sheet-show-success-confirm/) |
| 260420-nh8 | Fix email invite URL + deploy hosting and 3 functions to gift-registry-ro | 2026-04-20 | 41ed064 | [260420-nh8-fix-email-invite-url-and-deploy-firebase](./quick/260420-nh8-fix-email-invite-url-and-deploy-firebase/) |
| 260420-o6w | Show shared-with-me registries — add invitedUsers query + merge/dedupe/sort in repository | 2026-04-20 | d86577d | [260420-o6w-show-shared-with-me-registries-query-reg](./quick/260420-o6w-show-shared-with-me-registries-query-reg/) |
| 260420-ozb | Persistent in-app notifications inbox — top-bar bell, 5 event sources via writeNotification helper, EN+RO strings | 2026-04-20 | cbc8b9a | [260420-ozb-add-persistent-in-app-notifications-inbo](./quick/260420-ozb-add-persistent-in-app-notifications-inbo/) |
| 260421-lwi | Fix typography letter-spacing units (em→sp) — Phase 8 Typography used `.em` for letter-spacing but handoff JSX values are pixel-equivalent; fixed all 10 TextStyle roles + 8 TypographyTest assertions | 2026-04-21 | 7743d35 | [260421-lwi-fix-typography-letter-spacing-units-em-t](./quick/260421-lwi-fix-typography-letter-spacing-units-em-t/) |
| 260421-moi | Fix bottom nav label truncation — weight all 5 slots evenly, remove hardcoded FAB slot width, add maxLines/softWrap guards; remove duplicate RegistryListScreen legacy FAB | 2026-04-21 | c1a1f65 | [260421-moi-fix-bottom-nav-label-truncation-weight-s](./quick/260421-moi-fix-bottom-nav-label-truncation-weight-s/) |
| 260421-moi | Fix bottom-nav label truncation + remove duplicate RegistryListScreen FAB — 5-slot equal-width via Modifier.weight(1f), softWrap=false label guards, legacy Scaffold FAB removed | 2026-04-21 | 6139b9c | [260421-moi-fix-bottom-nav-label-truncation-weight-s](./quick/260421-moi-fix-bottom-nav-label-truncation-weight-s/) |
| 260427-gld | Fix wordmark letterSpacing em→sp + decouple ConcentricRings from GoogleBanner height — matchParentSize overlay pattern restores Auth screen wordmark legibility and ~68 dp banner height | 2026-04-27 | 82d8125 | [260427-gld-fix-wordmark-letterspacing-em-to-sp-and-](./quick/260427-gld-fix-wordmark-letterspacing-em-to-sp-and-/) |
| 260427-gxu | Bundle Instrument Serif TTFs as GMS-first/bundled-fallback + fix Auth headline to 2-span (ink line 1, accent line 2 incl. period) — OFL licence in assets/licenses/; 6/6 AuthHeadlineTest pass | 2026-04-27 | d4fccf8 | [260427-gxu-fix-auth-headline-colors-line-2-should-b](./quick/260427-gxu-fix-auth-headline-colors-line-2-should-b/) |
| 260427-lnq | Reorder InstrumentSerifFamily — bundled fonts promoted to sole entries, GMS async entries removed; synchronous serif rendering app-wide | 2026-04-21 | abd6028 | [260427-lnq-reorder-instrumentseriffamily-to-put-bun](./quick/260427-lnq-reorder-instrumentseriffamily-to-put-bun/) |
| 260427-lwz | Fix bottom nav clipping + FAB optical alignment — Row content height 56 dp -> 72 dp so HOME/STORES/LISTS/YOU labels and ADD caption render fully; FAB 22 dp lift preserved | 2026-04-27 | b21e24e | [260427-lwz-fix-bottom-nav-clipping-and-fab-alignmen](./quick/260427-lwz-fix-bottom-nav-clipping-and-fab-alignmen/) |
| 260427-n67 | Align ADD label with other nav labels — FabSlot mirrors NavItemSlot column (wrap-content + 44 dp inner Box scaffold + 4 dp Spacer); FAB rendered at 54 dp via requiredSize, 22 dp lift preserved via offset; Task 2 human-verify outstanding | 2026-04-27 | 5fe7c87 | [260427-n67-align-add-label-with-other-nav-labels-mi](./quick/260427-n67-align-add-label-with-other-nav-labels-mi/) |
| 260427-nkn | Drop FAB lift — plus icon must sit below the bar's top border line; removed `.offset(y = -22.dp)` from FabSlot + unused `offset` import; FAB now flush within 72 dp bar (FAB top ~4 dp below gray border, no protrusion); KDocs updated to record handoff override per user feedback; Task 2 human-verify outstanding | 2026-04-27 | d4d9a4a | [260427-nkn-drop-fab-lift-plus-icon-must-sit-below-t](./quick/260427-nkn-drop-fab-lift-plus-icon-must-sit-below-t/) |
| 260428-iny | Trim Add sheet from 4 rows to 2 (New registry + Add an item); AddItemKey gains nullable registryId + fromAddSheet flag; AddItemScreen renders Material3 ExposedDropdownMenuBox registry picker as first field when fromAddSheet=true (with empty-state link to CreateRegistryKey); CreateRegistry → AddItem chain keeps picker hidden; 4 new EN+RO strings added, 6 deprecated strings removed; sheet bug-fix preservation (corners, scrim, drag-handle, no shadow) confirmed; Task 3 human-verify outstanding | 2026-04-28 | 5752adf | [260428-iny-trim-add-sheet-to-2-options-new-registry](./quick/260428-iny-trim-add-sheet-to-2-options-new-registry/) |
| 260428-s3b | Fix Event Date + Time fields on Create/Edit Registry — InteractionSource pattern replaces broken Modifier.clickable on date field (was no-op due to TextField pointer-input swallow); add TimePickerDialog wired via same pattern with runtime gate inside collector (enabled=true so InteractionSource fires); ViewModel gets eventTimeSet StateFlow + setEventTime(hour, minute) encoding into existing eventDateMs Long; edit-mode hydration flips eventTimeSet=true on non-midnight Calendar-decoded timestamps; 5 new VM unit tests pin the contract; no domain/persistence/string changes; Task 3 human-verify outstanding | 2026-04-28 | c133ac5 | [260428-s3b-fix-event-date-time-fields-on-createregi](./quick/260428-s3b-fix-event-date-time-fields-on-createregi/) |
| 260428-tx9 | Replace framework android.app.DatePickerDialog / TimePickerDialog with Material3 Compose DatePickerDialog + DatePicker(state) and AlertDialog { TimePicker(state) } on Create/Edit Registry — pickers now inherit GiftMaison terracotta via MaterialTheme.colorScheme.primary (LightColorScheme.primary = gm.accent in Theme.kt:36) instead of bleeding the Android system Material green/teal; UTC↔local-Calendar conversion in DatePickerDialog confirmButton guards against Bucharest UTC+2/+3 off-by-one civil-day; @OptIn(ExperimentalMaterial3Api::class) at @Composable level; OK/Cancel via stringResource(android.R.string.ok / .cancel) — zero new strings.xml keys; all s3b behaviour preserved verbatim (InteractionSource trigger, hour/minute preservation on re-pick, eventTimeSet StateFlow + setEventTime VM API, 24h locale awareness, edit-mode round-trip); Task 2 human-verify outstanding | 2026-04-28 | 08c66da | [260428-tx9-replace-framework-datepickerdialog-timep](./quick/260428-tx9-replace-framework-datepickerdialog-timep/) |
| 260428-v0q | Make bottom nav persistent across all post-auth destinations — invert `Any?.showsBottomNav()` from 2-key visible-whitelist (HomeKey + RegistryDetailKey only) to 4-case hidden-whitelist (null, AuthKey, OnboardingKey, ReReserveDeepLink); fixes user's reported bug where tapping YOU hid the chrome on Settings, plus extends the same contract to Notifications, Stores, and all forms (CreateRegistry, EditRegistry, AddItem, EditItem); BottomNavVisibilityTest pins all 14 nav-key cases (4 hidden, 10 visible — every key in AppNavKeys.kt); zero touches to AppNavigation.kt, AppNavKeys.kt, GiftMaisonBottomNav.kt, or string resources; Task 2 human-verify outstanding (8 device scenarios) | 2026-04-28 | a486ca5 | [260428-v0q-make-bottom-nav-persistent-across-all-po](./quick/260428-v0q-make-bottom-nav-persistent-across-all-po/) |

## Session Continuity

Last session: 2026-04-28T19:27:00.000Z
Stopped at: Completed quick-260428-v0q (predicate inversion shipped; Task 2 device-verify outstanding — 8 scripted scenarios)
Resume file: None
