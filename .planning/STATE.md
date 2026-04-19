---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 06-00-PLAN.md
last_updated: "2026-04-19T18:58:47.526Z"
last_activity: 2026-04-19
progress:
  total_phases: 7
  completed_phases: 5
  total_plans: 32
  completed_plans: 27
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-04)

**Core value:** Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.
**Current focus:** Phase 06 — notifications-email-flows

## Current Position

Phase: 06 (notifications-email-flows) — EXECUTING
Plan: 2 of 6
Status: Ready to execute
Last activity: 2026-04-19

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

## Accumulated Context

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

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 7 (EMAG Catalog): EMAG has no confirmed public product catalog API. A research spike is required before Phase 7 implementation planning begins. Integration strategy (2Performant affiliate links, WebView embed, URL paste as primary) is unresolved.
- Phase 4 (Reservation): Cloud Tasks cancellation API (`cloudTaskName` pattern) should be verified against current Firebase documentation before Phase 4 planning.

## Session Continuity

Last session: 2026-04-19T18:58:47.523Z
Stopped at: Completed 06-00-PLAN.md
Resume file: None
