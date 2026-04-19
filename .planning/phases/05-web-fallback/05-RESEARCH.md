# Phase 5: Web Fallback - Research

**Researched:** 2026-04-19
**Domain:** React SPA + Firebase JS SDK + Vite + i18next
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **WEB-D-01:** Vite + React 19 + TypeScript. Static build output deploys to `hosting/public/` via Firebase Hosting. Firebase Hosting is already configured with SPA rewrites.
- **WEB-D-02:** React Router v7 for SPA routing. Handles `/registry/:id` and the `/reservation/:id/re-reserve` email deep link stubbed in Phase 4.
- **WEB-D-03:** Tailwind CSS + Radix UI Primitives (`@radix-ui/react-dialog`, `@radix-ui/react-toast`, `@radix-ui/react-alert-dialog`, `@radix-ui/react-label`, `@radix-ui/react-visually-hidden`). Lucide React for icons. Inter via Google Fonts.
- **WEB-D-04:** TanStack Query for server state management on top of Firebase JS SDK calls. No Redux.
- **WEB-D-05:** `/registry/:id` renders the registry immediately for anonymous visitors. No login wall.
- **WEB-D-06:** Guest identity (first name, last name, email) collected via modal on Reserve click. Values persisted to `localStorage.guestIdentity`.
- **WEB-D-07:** On successful reservation, open retailer `affiliateUrl` in a new tab (`target=_blank rel=noopener`). Registry tab stays open.
- **WEB-D-08:** Countdown display: inline badge on reserved item cards + sticky banner when current giver has an active reservation.
- **WEB-D-09:** Login providers: Email/password + Google OAuth. `signInWithPopup` for Google. "Continue as guest" is NOT a Firebase Auth call.
- **WEB-D-10:** Private registry gating enforced by Firestore security rules. Client does not do access logic — runs query, gets doc or permission-denied, maps to 404.
- **WEB-D-11:** Re-reserve deep link `/reservation/:id/re-reserve` calls `resolveReservation` callable, then navigates to `/registry/:id?autoReserveItemId=:itemId`. No new backend.
- **WEB-D-12:** Firebase Auth `browserLocalPersistence` (default). Session survives tab close.
- **WEB-D-13:** Registry-not-found / rules-denied → single generic "Registry not available" 404 page. Client does NOT distinguish deleted from never-existed from permission-denied.
- **WEB-D-14:** Private registry viewer-not-invited → same 404 page as WEB-D-13. Does not leak that the registry exists.
- **WEB-D-15:** Localization via i18next using seeded `web/i18n/en.json` + `ro.json`. Browser locale auto-detected; manual override persisted to localStorage.
- **WEB-D-16:** Loading UX: skeleton screens on initial load. Inline spinner on Reserve button during callable. No full-page spinners.
- **WEB-D-17:** Firebase JS SDK Functions instance MUST be pinned to `europe-west3` region. DO NOT use default `us-central1`.
- **WEB-D-18:** Firebase App Check enabled with reCAPTCHA v3 provider. `VITE_RECAPTCHA_SITE_KEY` env var for prod. Debug token pattern for local dev.
- **WEB-D-19:** Affiliate URL from item doc. Web client never runs AffiliateUrlTransformer. Redirect uses whatever is on the doc.

### Claude's Discretion

- Exact Tailwind color tokens, component shape language, typography scale
- Component library choice within "headless" category (Radix UI vs Headless UI vs ark-ui) — UI-SPEC chose Radix UI
- File/folder layout under the web app root (`src/pages`, `src/features`, `src/components`)
- Form validation library (zod + react-hook-form is reasonable default)
- Whether the sticky reservation banner persists in localStorage or only in React state
- Exact skeleton visual rhythm

### Deferred Ideas (OUT OF SCOPE)

- PWA / offline mode
- Web push notifications
- Visual design refinement beyond functional-first UI
- Analytics / funnel tracking on web
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| WEB-01 | Gift givers can view a registry via web browser without installing the app | Vite+React+Firebase JS SDK onSnapshot pattern enables real-time read; Firestore rules already allow unauthenticated reads on public registries |
| WEB-02 | Gift givers can reserve items from the web fallback | `httpsCallable(functions, 'createReservation')` call from web client; same Cloud Function as Android; error mapping for ITEM_UNAVAILABLE |
| WEB-03 | Gift givers can log in, create an account, or continue as guest on web | Firebase Auth JS SDK: `signInWithEmailAndPassword`, `createUserWithEmailAndPassword`, `signInWithPopup(GoogleAuthProvider)`; guest path collects identity in GuestIdentityModal without Firebase Auth |
| WEB-04 | Web fallback redirects to retailer on reservation (same as Android flow) | `createReservation` returns `affiliateUrl`; open in new tab via `window.open(affiliateUrl, '_blank', 'noopener')` |
</phase_requirements>

---

## Summary

Phase 5 builds a Vite + React 19 + TypeScript SPA that lives under `web/` as source and outputs to `hosting/public/` for Firebase Hosting. The stack is entirely settled by decisions WEB-D-01 through WEB-D-19 with very little left to research as options — the work is confirming exact API patterns and identifying pitfalls.

The highest-risk implementation areas are: (1) the TanStack Query + Firestore `onSnapshot` bridging pattern, which requires explicit attention to subscription lifecycle and stale time settings; (2) Firebase App Check integration in a Vite SPA, which requires setting `self.FIREBASE_APPCHECK_DEBUG_TOKEN` before `initializeAppCheck` for local dev; (3) pinning Functions to `europe-west3`, which the JS SDK does NOT do by default (same bug fixed in Android today); and (4) the Firestore `permission-denied` vs `not-found` error code handling that maps both to the same 404 page.

The entire backend (Firestore schema, Cloud Functions, security rules) is shared verbatim with the Android client. No server-side changes are required for Phase 5.

**Primary recommendation:** Scaffold the `web/` app with `npm create vite@latest . -- --template react-ts`, configure `build.outDir: '../../hosting/public'` in `vite.config.ts`, initialize Firebase before React renders (in `main.tsx`), and set up the onSnapshot-into-TanStack-Query bridge in a shared `useRegistryQuery` hook.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vite | 6.x (latest: ~6.3) | Build tool + dev server | Locked WEB-D-01; Node 22 compatible; `create vite --template react-ts` scaffold |
| React | 19.2.5 | UI framework | Locked WEB-D-01; Firebase JS SDK is React-agnostic; Hooks-first |
| TypeScript | 5.x | Language | Locked WEB-D-01; shared type safety with Functions |
| React Router | 7.14.1 | SPA routing | Locked WEB-D-02; `createBrowserRouter` data mode for route map |
| TanStack Query | 5.99.2 | Server state | Locked WEB-D-04; wraps onSnapshot subscriptions |
| Firebase JS SDK | 12.12.0 | Auth + Firestore + Functions + App Check | Locked by project; matches backend |
| i18next | 26.0.6 | i18n core | Locked WEB-D-15; note: `npm view i18next version` returns 26.x, not 24.x — CLAUDE.md recommendation is slightly stale |
| react-i18next | 17.0.4 | React bindings for i18next | Companion to i18next |
| i18next-browser-languagedetector | ~8.x | Browser locale detection | Detects from navigator; caches to localStorage |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| @radix-ui/react-dialog | 1.1.15 | GuestIdentityModal, AuthModal | Required by UI-SPEC |
| @radix-ui/react-toast | ~1.2.x | Reservation feedback toasts | Required by UI-SPEC |
| @radix-ui/react-alert-dialog | ~1.1.x | (available; not in primary UI-SPEC flow) | If destructive confirm needed |
| @radix-ui/react-label | ~2.1.x | Form labels with a11y | Required by UI-SPEC |
| @radix-ui/react-visually-hidden | ~1.2.x | Screen-reader-only text | Required by UI-SPEC |
| lucide-react | current | Icons (status badges, spinner) | Required by UI-SPEC |
| tailwindcss | 4.x | Utility CSS | Required by UI-SPEC; note: v4 changes config format (see Pitfalls) |
| @vitejs/plugin-react | current | JSX transform for Vite | Required for React with Vite |
| react-hook-form | 7.x | Form state management | Claude's discretion; 3 forms in this phase |
| zod | 3.x | Schema validation | Claude's discretion; pairs with react-hook-form |
| @hookform/resolvers | 3.x | Zod resolver for react-hook-form | Bridge package |

### Version Verification Note

Versions above for `i18next` (26.x) and `tailwindcss` (4.x) were verified against the npm registry during research (2026-04-19). CLAUDE.md references i18next 24.x — the actual current version is 26.x. Tailwind CSS 4.x changed the config format significantly from v3 (see Pitfalls section).

### Installation

```bash
# From project root: scaffold web app
mkdir -p web && cd web
npm create vite@latest . -- --template react-ts

# Core dependencies
npm install firebase react-router @tanstack/react-query
npm install i18next react-i18next i18next-browser-languagedetector

# UI
npm install tailwindcss @vitejs/plugin-react
npm install @radix-ui/react-dialog @radix-ui/react-toast @radix-ui/react-alert-dialog
npm install @radix-ui/react-label @radix-ui/react-visually-hidden
npm install lucide-react

# Forms (Claude's discretion)
npm install react-hook-form zod @hookform/resolvers

# Dev/test
npm install -D vitest @testing-library/react @testing-library/user-event jsdom
npm install -D @testing-library/jest-dom
```

---

## Architecture Patterns

### Recommended Project Structure

```
web/                          # Source root (separate from hosting/public/)
├── index.html                # Vite entry HTML (NOT in src/)
├── vite.config.ts            # build.outDir: '../../hosting/public'
├── tailwind.config.ts        # extend.colors with UI-SPEC tokens
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── vitest.config.ts
├── .env.local                # VITE_RECAPTCHA_SITE_KEY (git-ignored)
├── .env.development.local    # VITE_APP_CHECK_DEBUG_TOKEN (git-ignored)
├── src/
│   ├── main.tsx              # Firebase init + App Check + QueryClient + RouterProvider
│   ├── App.tsx               # Route definitions only
│   ├── firebase.ts           # initializeApp, getFirestore, getFunctions('europe-west3'), getAuth
│   ├── i18n.ts               # i18next init, language detector, load from /locales/
│   ├── features/
│   │   ├── registry/
│   │   │   ├── RegistryPage.tsx        # /registry/:id route component
│   │   │   ├── useRegistryQuery.ts     # onSnapshot -> TanStack Query bridge
│   │   │   ├── useItemsQuery.ts        # items subcollection onSnapshot bridge
│   │   │   ├── RegistryHeader.tsx
│   │   │   ├── ItemCard.tsx
│   │   │   ├── SkeletonCard.tsx
│   │   │   └── ReservationBanner.tsx
│   │   ├── reservation/
│   │   │   ├── ReReservePage.tsx       # /reservation/:id/re-reserve
│   │   │   ├── useCreateReservation.ts # useMutation wrapping httpsCallable
│   │   │   ├── useResolveReservation.ts
│   │   │   ├── ReserveButton.tsx
│   │   │   └── GuestIdentityModal.tsx
│   │   └── auth/
│   │       ├── AuthModal.tsx
│   │       ├── useAuthState.ts         # onAuthStateChanged -> React state
│   │       └── useGuestIdentity.ts     # localStorage persistence
│   ├── components/
│   │   ├── NotFoundPage.tsx
│   │   ├── AppRootPage.tsx             # / redirect + "Get the app" page
│   │   ├── ToastProvider.tsx           # Radix Toast.Provider wrapper
│   │   └── Nav.tsx
│   └── locales/                        # Copied/merged from web/i18n/ seeds
│       ├── en.json
│       └── ro.json
└── public/                   # Vite static assets (favicon, etc.) — NOT hosting/public/
```

**Critical distinction:** `web/public/` is Vite's static asset folder (copied to output). `hosting/public/` is the Firebase Hosting deploy root and the Vite build output. These are different directories.

### Pattern 1: Vite Config for Firebase Hosting Output

```typescript
// web/vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: path.resolve(__dirname, '../../hosting/public'),
    emptyOutDir: true,
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
})
```

**Why `emptyOutDir: true` explicitly:** Vite only auto-clears outDir when it's inside the project root. Since `hosting/public/` is outside `web/`, you must set this explicitly or old build artifacts accumulate.

### Pattern 2: Firebase Initialization (main.tsx order matters)

```typescript
// web/src/main.tsx
import { initializeApp } from 'firebase/app'
import { initializeAppCheck, ReCaptchaV3Provider } from 'firebase/app-check'

// App Check debug token MUST be set before initializeAppCheck
if (import.meta.env.DEV) {
  // Setting to true auto-generates a debug token; print to console
  // Register that token in Firebase console > App Check > Debug tokens
  (self as any).FIREBASE_APPCHECK_DEBUG_TOKEN = import.meta.env.VITE_APP_CHECK_DEBUG_TOKEN || true
}

const app = initializeApp(firebaseConfig)

initializeAppCheck(app, {
  provider: new ReCaptchaV3Provider(import.meta.env.VITE_RECAPTCHA_SITE_KEY),
  isTokenAutoRefreshEnabled: true,
})

// Then render React
ReactDOM.createRoot(document.getElementById('root')!).render(...)
```

**Order constraint:** App Check initialization must happen before any Firestore/Functions/Auth calls. Initializing it after the first Firebase call may cause `Unchecked status code: 403` errors.

### Pattern 3: Firebase Module Init with Region Pinning

```typescript
// web/src/firebase.ts
import { initializeApp } from 'firebase/app'
import { getFirestore } from 'firebase/firestore'
import { getFunctions } from 'firebase/functions'
import { getAuth } from 'firebase/auth'

const firebaseConfig = { /* from .env or hardcoded public config */ }
export const app = initializeApp(firebaseConfig)
export const db = getFirestore(app)
// CRITICAL: pin region — JS SDK defaults to us-central1 just like Android
export const functions = getFunctions(app, 'europe-west3')
export const auth = getAuth(app)
```

### Pattern 4: TanStack Query + Firestore onSnapshot Bridge

The canonical pattern wraps `onSnapshot` in a Promise that resolves on first emission and uses `queryClient.setQueryData` for subsequent real-time updates:

```typescript
// web/src/features/registry/useRegistryQuery.ts
import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { doc, onSnapshot, FirestoreError } from 'firebase/firestore'
import { db } from '@/firebase'

export function useRegistryQuery(registryId: string) {
  const queryClient = useQueryClient()
  const queryKey = ['registry', registryId]

  useEffect(() => {
    const docRef = doc(db, 'registries', registryId)
    const unsub = onSnapshot(
      docRef,
      (snap) => {
        if (!snap.exists()) {
          queryClient.setQueryData(queryKey, null)
        } else {
          queryClient.setQueryData(queryKey, { id: snap.id, ...snap.data() })
        }
      },
      (err: FirestoreError) => {
        // permission-denied and not-found both map to 404 (WEB-D-13)
        queryClient.setQueryData(queryKey, null)
      }
    )
    return unsub // cleanup unsubscribes on unmount
  }, [registryId, queryClient])

  return useQuery({
    queryKey,
    queryFn: () => Promise.resolve(queryClient.getQueryData(queryKey) ?? null),
    staleTime: Infinity,      // onSnapshot keeps data fresh; prevent refetch
    refetchOnWindowFocus: false,
    refetchOnMount: false,
  })
}
```

**Key settings:**
- `staleTime: Infinity` — data is always fresh because onSnapshot pushes updates
- `refetchOnWindowFocus: false` — subscription is already active; refetch would create a new read
- `refetchOnMount: false` — same reason
- Error callback handles both `permission-denied` and `not-found` by setting data to `null`; the component checks for `null` and renders NotFoundPage

### Pattern 5: Calling Cloud Functions from Web

```typescript
// web/src/features/reservation/useCreateReservation.ts
import { httpsCallable } from 'firebase/functions'
import { useMutation } from '@tanstack/react-query'
import { functions } from '@/firebase'

interface CreateReservationRequest {
  registryId: string
  itemId: string
  giverName: string
  giverEmail: string
  giverId: string | null
}

interface CreateReservationResponse {
  reservationId: string
  affiliateUrl: string
  expiresAtMs: number
}

const createReservationFn = httpsCallable<CreateReservationRequest, CreateReservationResponse>(
  functions, 'createReservation'
)

export function useCreateReservation() {
  return useMutation({
    mutationFn: (req: CreateReservationRequest) =>
      createReservationFn(req).then(r => r.data),
    onSuccess: (data) => {
      // Open retailer tab (WEB-D-07)
      if (data.affiliateUrl) {
        window.open(data.affiliateUrl, '_blank', 'noopener,noreferrer')
      }
    },
  })
}
```

**Error handling:** `httpsCallable` throws `FirebaseFunctionsError` with `code` matching `HttpsError` codes. Map `failed-precondition` (ITEM_UNAVAILABLE) to the conflict toast; map others to `common.error_generic`.

### Pattern 6: React Router v7 Data Mode Route Map

Use **Data Mode** (`createBrowserRouter`) not Framework Mode (which requires a build plugin and SSR config). Data mode is the right choice for a Vite-scaffolded SPA that doesn't need SSR.

```typescript
// web/src/App.tsx
import { createBrowserRouter, RouterProvider, redirect } from 'react-router'

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppRootPage />,  // "Get the app" static page (UI-SPEC)
  },
  {
    path: '/registry/:id',
    element: <RegistryPage />,
    errorElement: <NotFoundPage />,
  },
  {
    path: '/reservation/:id/re-reserve',
    element: <ReReservePage />,
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])

export default function App() {
  return <RouterProvider router={router} />
}
```

Auth is a modal, not a route. `/auth` route is not needed. The AuthModal is triggered from the Nav "Sign in" link and rendered at the RegistryPage level.

### Pattern 7: i18next Setup

```typescript
// web/src/i18n.ts
import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import en from './locales/en.json'
import ro from './locales/ro.json'

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { en: { translation: en }, ro: { translation: ro } },
    fallbackLng: 'en',
    supportedLngs: ['en', 'ro'],
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'lang',  // key for manual override persistence
    },
    interpolation: { escapeValue: false },
  })

export default i18n
```

**Import in main.tsx before rendering** (`import './i18n'`) — i18next must be initialized synchronously before any component renders.

**Key naming:** The existing `web/i18n/en.json` seed uses a flat namespace structure (`{ "app": { "name": ... } }`). The i18next key syntax to access it would be `t('app.name')`. The UI-SPEC keys use underscore convention (`app_name`). The planner must standardize key format when merging seeds — recommendation: keep nested JSON (matches existing seeds) and use `t('reservation.reserve_item')` dot notation.

### Pattern 8: Guest Identity Persistence (localStorage)

```typescript
// web/src/features/auth/useGuestIdentity.ts
const GUEST_IDENTITY_KEY = 'guestIdentity'

export interface GuestIdentity {
  firstName: string
  lastName: string
  email: string
}

export function useGuestIdentity() {
  const [identity, setIdentity] = useState<GuestIdentity | null>(() => {
    try {
      const raw = localStorage.getItem(GUEST_IDENTITY_KEY)
      return raw ? JSON.parse(raw) : null
    } catch { return null }
  })

  const save = (g: GuestIdentity) => {
    localStorage.setItem(GUEST_IDENTITY_KEY, JSON.stringify(g))
    setIdentity(g)
  }

  return { identity, save }
}
```

**Shape mirrors Android:** Android's `GuestPreferencesDataStore` stores `firstName`, `lastName`, `email` — same field names.

### Pattern 9: Firestore Error Code Distinction

The Firebase JS SDK `FirestoreError` exposes a `code` property of type `FirestoreErrorCode` (a string union). Both `permission-denied` and `not-found` result in the onSnapshot error callback firing. Per WEB-D-13/14, both map to `null` data and the same 404 page — **no distinction needed at the client layer**.

```typescript
// In onSnapshot error callback:
(err: FirestoreError) => {
  // err.code is 'permission-denied' | 'not-found' | ...
  // Both cases: render NotFoundPage (WEB-D-13 + WEB-D-14)
  queryClient.setQueryData(queryKey, null)
}
```

This is intentional: it prevents private registry enumeration. The Firestore rules return `permission-denied` for private registries the user cannot access; deleted or bad IDs return `not-found`. The client intentionally does not distinguish.

### Anti-Patterns to Avoid

- **Calling `getFunctions(app)` without region:** Defaults to `us-central1`. All `createReservation` and `resolveReservation` calls will fail with CORS or region mismatch errors. Always `getFunctions(app, 'europe-west3')`.
- **Initializing App Check after first Firebase call:** Causes 403 errors on the first request. `initializeAppCheck` must run before any Firestore/Functions/Auth call in `main.tsx`.
- **Using React Router Framework Mode for this SPA:** Framework Mode requires the `@react-router/dev` Vite plugin and generates its own entry file — it conflicts with Vite's standard `index.html` entry. Use Data Mode (`createBrowserRouter`).
- **Setting `queryFn` to start the onSnapshot subscription:** Causes double subscriptions when React re-renders. Keep the subscription in `useEffect` and `queryFn` as a passive cache reader.
- **Reading `invitedUsers` map on the client:** The map may have malformed nested-map values (known Phase 3/4 bug). The client never reads `invitedUsers` directly — rules enforce access, client just reads the registry doc or gets denied.
- **Hardcoding strings in JSX:** All visible strings must use `t('key')` — I18N-02 requirement. No English-language string literals in JSX.
- **Writing to `reservations/` collection from web client:** Firestore rules deny all client writes to `reservations/`. All reservation operations go through callables.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Real-time Firestore subscriptions | Custom event emitter / polling | `onSnapshot` from Firebase JS SDK | Built-in reconnect, offline buffering, server-push |
| Query cache invalidation | Manual useState + useEffect chains | TanStack Query `queryClient.setQueryData` | Cache lifecycle, deduplication, devtools |
| Form validation | Custom validation functions | zod schema + react-hook-form | Type inference, error message consistency, accessibility-ready |
| Accessible modal (focus trap, Escape key, aria-modal) | Custom dialog component | `@radix-ui/react-dialog` | WCAG 2.1 AA compliant ARIA, focus management, keyboard nav — notoriously hard to get right |
| Toast notification stack | Custom CSS animations + z-index stacking | `@radix-ui/react-toast` | Proper `aria-live` region, accessibility, dismiss on timer |
| Browser language detection | `navigator.language` parsing | `i18next-browser-languagedetector` | Handles locale subtags, BCP 47 normalization, fallback chains |
| Auth persistence | sessionStorage + custom headers | Firebase Auth `browserLocalPersistence` (default) | Token refresh, secure storage, consistent with Android |
| Google OAuth popup | Google Identity Services direct | Firebase Auth `signInWithPopup(new GoogleAuthProvider())` | Firebase handles token exchange and user record creation |

**Key insight:** The Radix UI Dialog primitive alone saves 2-3 days of accessibility work. Focus management, ARIA roles, scroll locking, and keyboard nav are non-trivial and fail audits when hand-rolled.

---

## Common Pitfalls

### Pitfall 1: Functions Region Default — `us-central1`

**What goes wrong:** `getFunctions(app)` without a region argument defaults to `us-central1`. All callable invocations fail with network errors or CORS errors since the deployed functions are in `europe-west3`.

**Why it happens:** This is the same bug fixed in `AppModule.kt` on 2026-04-19. The JS SDK has identical behavior.

**How to avoid:** Always `getFunctions(app, 'europe-west3')` in `firebase.ts`. Never call `getFunctions(app)` anywhere in the codebase.

**Warning signs:** `httpsCallable` calls return `FunctionsError` with code `internal` and CORS headers missing in browser devtools.

### Pitfall 2: App Check Initialization Order

**What goes wrong:** If `initializeAppCheck` is called after the first Firestore or Functions call, all requests before initialization will fail with 403 App Check token validation failures.

**Why it happens:** Firebase SDKs attach App Check tokens to outgoing requests. If the token provider isn't initialized, no token is attached and Firebase backend rejects with 403.

**How to avoid:** In `main.tsx`, initialize Firebase app, then `initializeAppCheck`, then render `<React.StrictMode><App /></React.StrictMode>`. Import `i18n.ts` before rendering as well.

**Warning signs:** 403 errors on Firestore reads on first page load; errors disappear after refresh (because App Check may have cached a token from a prior session).

### Pitfall 3: Tailwind v4 Config Format Breaking Change

**What goes wrong:** Tailwind CSS 4.x (current) uses a CSS-first configuration approach. The `tailwind.config.ts` file format from v3 is no longer the primary mechanism. Copying v3 config patterns from documentation/tutorials will produce a config that is silently ignored.

**Why it happens:** Tailwind v4 moved to `@import "tailwindcss"` in CSS files and uses CSS custom properties for configuration rather than a JS config file.

**How to avoid:** If using Tailwind v4: configure tokens via CSS custom properties in `src/index.css`. The UI-SPEC color tokens would be defined as CSS variables. Alternatively: pin Tailwind to v3 (`tailwindcss@^3`) which still uses `tailwind.config.ts`. The planner must pick one — recommend v3 for this phase to avoid v4 migration friction, since all documentation and online examples still reference v3 config syntax.

**Warning signs:** Custom colors from `extend.colors` not appearing in generated CSS; Tailwind Intellisense not showing custom classes.

### Pitfall 4: Vite `emptyOutDir` and `hosting/public/` Contents

**What goes wrong:** Vite's `build.emptyOutDir` defaults to `true` only when `outDir` is inside the project root. Since `hosting/public/` is outside `web/` (the project root), Vite defaults to `false` — old files accumulate and are never cleaned.

**Why it happens:** Safety guard in Vite to prevent accidentally deleting files outside the project. Since hosting/public is outside web/, the guard kicks in.

**How to avoid:** Explicitly set `emptyOutDir: true` in `vite.config.ts`. Also ensure `hosting/public/.gitkeep` or `hosting/public/` is git-tracked with a placeholder so the directory exists before first build.

**Warning signs:** Old `index.html` placeholder still served from Firebase Hosting after deploy; stale JS chunk files accumulate.

### Pitfall 5: i18next Key Format Mismatch

**What goes wrong:** The existing `web/i18n/en.json` seeds use nested JSON (`{ "reservation": { "timer_label": "..." } }`). The UI-SPEC adds new keys using the same nested format (`reservation.reserve_item`). If i18next is configured with a non-default `keySeparator` or the keys are flattened, `t('reservation.reserve_item')` returns the key instead of the translation.

**Why it happens:** i18next key resolution depends on `keySeparator` setting (default is `"."`). Mismatches between seed format and expected key format cause silent fallthrough.

**How to avoid:** Keep nested JSON format in `en.json` and `ro.json`. Use dot-notation keys in components (`t('reservation.reserve_item')`). Do NOT set `keySeparator: false`. Verify during Wave 0 by rendering one translated key in isolation before building full screens.

### Pitfall 6: TanStack Query Stale Time + onSnapshot Double Subscription

**What goes wrong:** If `queryFn` inside `useQuery` also starts an `onSnapshot` listener, and `staleTime` is not set to `Infinity`, React Query may trigger a refetch (creating a second `onSnapshot`) when the component re-mounts or the window regains focus.

**Why it happens:** TanStack Query's default `staleTime: 0` means data is considered stale immediately. On re-mount, it calls `queryFn` again, starting another subscription.

**How to avoid:** Keep the onSnapshot subscription in `useEffect` (separate from the queryFn). Set `staleTime: Infinity`, `refetchOnWindowFocus: false`, `refetchOnMount: false`. The queryFn is a passive reader (`() => queryClient.getQueryData(key)`).

### Pitfall 7: Re-Reserve Page Auth State Race

**What goes wrong:** On `/reservation/:id/re-reserve`, the page calls `resolveReservation` callable. If App Check isn't initialized yet or the auth state hasn't settled, the callable may fire without proper tokens and fail.

**Why it happens:** The re-reserve URL is opened from an email link, meaning it's a cold start. Firebase Auth restores persisted sessions asynchronously. App Check token acquisition is also async.

**How to avoid:** Show a loading spinner until both `onAuthStateChanged` has emitted at least once (indicating session restoration is complete). Only then invoke `resolveReservation`. This is a cold-start deep link so a brief loading state is acceptable (UI-SPEC shows "Checking your reservation…" text).

### Pitfall 8: SPA Rewrite Already Exists — Don't Duplicate

**What goes wrong:** Developer adds a new `"rewrites"` entry in `firebase.json` for the web app, conflicting with the existing `** → /index.html` catch-all.

**Why it happens:** The `firebase.json` already has the correct SPA rewrite. No changes to `firebase.json` are needed for Phase 5.

**How to avoid:** `firebase.json` is already correct: `"public": "hosting/public"` + `"rewrites": [{ "source": "**", "destination": "/index.html" }]`. The only `firebase.json` change that might be needed is updating `"ignore"` if `web/node_modules/**` needs to be excluded from hosting (it already is via `**/node_modules/**`).

---

## Code Examples

### Firebase App Check + Debug Token (Vite Pattern)

```typescript
// web/src/main.tsx
import { initializeApp } from 'firebase/app'
import { initializeAppCheck, ReCaptchaV3Provider } from 'firebase/app-check'
import './i18n' // initialize before render

// Set debug token BEFORE initializeAppCheck
if (import.meta.env.DEV) {
  // VITE_APP_CHECK_DEBUG_TOKEN from .env.development.local
  // If undefined, setting to `true` auto-generates a UUID token (print to console)
  ;(self as any).FIREBASE_APPCHECK_DEBUG_TOKEN =
    import.meta.env.VITE_APP_CHECK_DEBUG_TOKEN ?? true
}

const app = initializeApp({
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
})

initializeAppCheck(app, {
  provider: new ReCaptchaV3Provider(import.meta.env.VITE_RECAPTCHA_SITE_KEY),
  isTokenAutoRefreshEnabled: true,
})

import ReactDOM from 'react-dom/client'
import App from './App'

ReactDOM.createRoot(document.getElementById('root')!).render(<App />)
```

### Tailwind v3 Config (Recommended — pin to v3 for stability)

```typescript
// web/tailwind.config.ts (v3 format)
import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary:     { DEFAULT: '#6750A4', on: '#FFFFFF' },
        surface:     { DEFAULT: '#FFFBFE', variant: '#E7E0EC', on: '#1C1B1F', onVariant: '#49454F' },
        destructive: { DEFAULT: '#B3261E', on: '#FFFFFF' },
        outline:     { DEFAULT: '#CAC4D0' },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
} satisfies Config
```

### React Router v7 Data Mode (No Plugin Needed)

```typescript
// web/src/App.tsx
import { createBrowserRouter, RouterProvider } from 'react-router'
import RegistryPage from '@/features/registry/RegistryPage'
import ReReservePage from '@/features/reservation/ReReservePage'
import NotFoundPage from '@/components/NotFoundPage'
import AppRootPage from '@/components/AppRootPage'

const router = createBrowserRouter([
  { path: '/',                              element: <AppRootPage /> },
  { path: '/registry/:id',                  element: <RegistryPage /> },
  { path: '/reservation/:id/re-reserve',    element: <ReReservePage /> },
  { path: '*',                              element: <NotFoundPage /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
```

### Items Subcollection onSnapshot Bridge

```typescript
// web/src/features/registry/useItemsQuery.ts
import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { collection, onSnapshot, query, FirestoreError } from 'firebase/firestore'
import { db } from '@/firebase'

export function useItemsQuery(registryId: string) {
  const queryClient = useQueryClient()
  const queryKey = ['registry', registryId, 'items']

  useEffect(() => {
    const col = collection(db, 'registries', registryId, 'items')
    const q = query(col)  // no orderBy needed for Phase 5; items render in insertion order
    const unsub = onSnapshot(
      q,
      (snap) => {
        const items = snap.docs.map(d => ({ id: d.id, ...d.data() }))
        queryClient.setQueryData(queryKey, items)
      },
      (_err: FirestoreError) => {
        queryClient.setQueryData(queryKey, [])
      }
    )
    return unsub
  }, [registryId, queryClient])

  return useQuery({
    queryKey,
    queryFn: () => Promise.resolve(queryClient.getQueryData(queryKey) ?? []),
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnMount: false,
  })
}
```

### resolveReservation Callable + Navigation

```typescript
// web/src/features/reservation/ReReservePage.tsx
import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router'
import { httpsCallable } from 'firebase/functions'
import { functions } from '@/firebase'
import { useTranslation } from 'react-i18next'

const resolveReservationFn = httpsCallable<
  { reservationId: string },
  { registryId: string; itemId: string; status: string }
>(functions, 'resolveReservation')

export default function ReReservePage() {
  const { id: reservationId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useTranslation()

  useEffect(() => {
    if (!reservationId) { navigate('/'); return }
    resolveReservationFn({ reservationId })
      .then(r => {
        const { registryId, itemId } = r.data
        navigate(`/registry/${registryId}?autoReserveItemId=${itemId}`, { replace: true })
      })
      .catch(() => navigate('/'))
  }, [reservationId, navigate])

  return (
    <div className="flex items-center justify-center min-h-screen">
      <span className="text-base text-surface-on">{t('reservation.resolving')}</span>
    </div>
  )
}
```

### Google OAuth signInWithPopup

```typescript
import { signInWithPopup, GoogleAuthProvider } from 'firebase/auth'
import { auth } from '@/firebase'

const provider = new GoogleAuthProvider()

async function signInWithGoogle() {
  try {
    const result = await signInWithPopup(auth, provider)
    return result.user
  } catch (err: any) {
    if (err.code === 'auth/popup-closed-by-user') return null  // user cancelled
    throw err
  }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| i18next 24.x (CLAUDE.md) | i18next 26.x (npm registry) | 2025 | API compatible; version pin should be `^26` not `^24` |
| Tailwind v3 (most tutorials) | Tailwind v4 (current) | Jan 2025 | Config format changed; v3 still supported; pin v3 for this phase |
| React Router v6 | React Router v7 | Late 2024 | `react-router-dom` merged into `react-router`; import from `react-router` not `react-router-dom` |
| Firebase KTX modules (android) | Main modules (android) | BoM 34.0.0 (July 2025) | Web JS SDK unaffected — this was Android-only migration |

**Deprecated/outdated:**
- `import { BrowserRouter } from 'react-router-dom'`: React Router v7 — import from `react-router` (not `react-router-dom`). The package `react-router-dom` still exists but `react-router` now includes everything.
- Firebase compat API (`firebase/compat/*`): Old namespace-style API. Always use modular API (`firebase/firestore`, `firebase/functions`, etc.)

---

## Open Questions

1. **Tailwind v3 vs v4 — which to pin?**
   - What we know: v4 is current; v3 config format is broadly understood; v4 config is CSS-first and less documented for custom token extension
   - What's unclear: Whether Vite + Tailwind v4 + Radix UI Primitives combination has any known integration issues
   - Recommendation: Pin `tailwindcss@^3` for Phase 5. The `tailwind.config.ts` pattern in UI-SPEC and all documentation examples use v3 syntax. Upgrade to v4 can be a post-v1 decision.

2. **Firebase emulator + App Check in local dev**
   - What we know: Debug token approach (`self.FIREBASE_APPCHECK_DEBUG_TOKEN = true`) generates a UUID that must be manually registered in Firebase console
   - What's unclear: Whether the Firebase Hosting emulator (port 5002) needs App Check enforcement disabled for local testing
   - Recommendation: For emulator-connected local dev, set `VITE_APP_CHECK_DEBUG_TOKEN` in `.env.development.local` to a pre-registered debug token. Document the one-time Firebase console registration step in Wave 0.

3. **`autoReserveItemId` query param handling**
   - What we know: ReReservePage navigates to `/registry/:id?autoReserveItemId=:itemId`; RegistryPage must read this param and auto-trigger `createReservation`
   - What's unclear: Exact interaction when the item is no longer available on arrival (auto-reserve should silently fail, not crash)
   - Recommendation: In RegistryPage, read `useSearchParams()` for `autoReserveItemId`. Attempt reservation when items data loads and item has status `available`. If status is not available, clear the param and show conflict toast without crashing.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Vite build, npm | Yes | 22.14.0 | — |
| npm | Package management | Yes | 10.9.2 | — |
| Firebase CLI | Deploy + emulators | Yes | 15.13.0 | — |
| Firebase Emulators | Local dev/test | Yes (CLI installed) | via firebase CLI 15.13 | — |
| `web/` source directory | Phase 5 source | Exists (contains only `i18n/`) | — | Scaffold new Vite app inside `web/` |
| `hosting/public/` | Build output target | Exists (placeholder index.html) | — | Already exists; placeholder will be overwritten |
| Vite | Build tool | Not installed globally | — | `npm create vite@latest` scaffolds + installs locally |
| reCAPTCHA v3 site key | App Check | Unknown (not in repo) | — | Must be obtained from Firebase console; block if missing |

**Missing dependencies with no fallback:**
- reCAPTCHA v3 site key: Must be created in Firebase console (App Check > Register app > reCAPTCHA v3) and stored in `.env.local` as `VITE_RECAPTCHA_SITE_KEY`. Without this key, App Check cannot initialize in production.

**Missing dependencies with fallback:**
- Vite (not globally installed): The scaffold command (`npm create vite@latest`) installs Vite locally into `web/node_modules/`. No global install needed.

---

## Validation Architecture

`workflow.nyquist_validation` is `true` in `.planning/config.json` — this section is required.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest + React Testing Library |
| Config file | `web/vite.config.ts` (unified; Vitest uses Vite config via `test:` key) |
| Quick run command | `cd web && npm run test` |
| Full suite command | `cd web && npm run test -- --run` |

Add to `web/package.json` scripts:
```json
{
  "test": "vitest",
  "test:run": "vitest run"
}
```

Add to `web/vite.config.ts`:
```typescript
test: {
  environment: 'jsdom',
  setupFiles: ['./src/test-setup.ts'],
  globals: true,
}
```

`web/src/test-setup.ts`:
```typescript
import '@testing-library/jest-dom'
```

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| WEB-01 | Registry page renders items from onSnapshot | component | `cd web && npm run test -- useRegistryQuery` | No — Wave 0 |
| WEB-01 | NotFoundPage shown on permission-denied | component | `cd web && npm run test -- RegistryPage` | No — Wave 0 |
| WEB-02 | createReservation callable invoked with correct payload | component | `cd web && npm run test -- useCreateReservation` | No — Wave 0 |
| WEB-02 | ITEM_UNAVAILABLE error shows conflict toast | component | `cd web && npm run test -- ReserveButton` | No — Wave 0 |
| WEB-03 | GuestIdentityModal pre-fills from localStorage | component | `cd web && npm run test -- GuestIdentityModal` | No — Wave 0 |
| WEB-03 | Guest identity saved to localStorage on submit | component | `cd web && npm run test -- useGuestIdentity` | No — Wave 0 |
| WEB-04 | window.open called with affiliateUrl on success | unit | `cd web && npm run test -- useCreateReservation` | No — Wave 0 |

**Firebase mocking strategy:** Use `vi.mock('firebase/firestore')` and `vi.mock('firebase/functions')` for unit/component tests. For integration smoke tests, use the Firebase Emulator Suite (already configured on known ports). Do NOT write Firestore emulator-dependent tests as part of the Vitest suite — those require a running emulator and are manual-only per the emulator validation document.

### Sampling Rate

- **Per task commit:** `cd web && npm run test:run -- --reporter=verbose`
- **Per wave merge:** `cd web && npm run test:run`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps (files that must be created before implementation)

- [ ] `web/src/test-setup.ts` — `@testing-library/jest-dom` import
- [ ] `web/vitest.config.ts` or `test:` block in `web/vite.config.ts`
- [ ] `web/src/features/registry/__tests__/useRegistryQuery.test.ts` — covers WEB-01
- [ ] `web/src/features/reservation/__tests__/useCreateReservation.test.ts` — covers WEB-02, WEB-04
- [ ] `web/src/features/auth/__tests__/GuestIdentityModal.test.tsx` — covers WEB-03
- [ ] Framework install: `npm install -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom` (run inside `web/`)

---

## Project Constraints (from CLAUDE.md)

| Directive | Enforcement in Phase 5 |
|-----------|----------------------|
| Firebase only — no other persistence layer | No SQLite, no server-side database, no localStorage as truth; Firebase Firestore + Auth is the only backend |
| No hardcoded strings in Kotlin/Compose | Equivalent on web: no hardcoded strings in JSX — all via `t()` |
| Localization externalized in i18n files | `web/i18n/en.json` + `ro.json` — all Phase 5 keys added there |
| Guest access must work without account creation | Guest flow collects name/email in modal; no Firebase Auth sign-in required |
| Reservation model: 30-minute hard timer, no extensions | Web uses same `createReservation` callable as Android — no client-side timer logic |
| GSD Workflow Enforcement | All edits through GSD execute-phase; no direct repo edits |

---

## Sources

### Primary (HIGH confidence)

- Firebase docs (firebase.google.com/docs/app-check/web/recaptcha-provider) — App Check reCAPTCHA v3 setup, debug token pattern
- Firebase docs (firebase.google.com/docs/app-check/web/debug-provider) — Debug token: `self.FIREBASE_APPCHECK_DEBUG_TOKEN` before `initializeAppCheck`
- Firebase docs (firebase.google.com/docs/functions/callable) — `getFunctions(app, region)`, `httpsCallable` pattern, error codes
- Firebase docs (firebase.google.com/docs/firestore/query-data/listen) — `onSnapshot`, error callback signature
- Vite docs (vite.dev/config/build-options) — `build.outDir`, `emptyOutDir` behavior
- React Router docs (reactrouter.com/start/modes) — three modes; Data Mode for Vite SPA
- npm registry — verified: react@19.2.5, react-router@7.14.1, firebase@12.12.0, @tanstack/react-query@5.99.2, i18next@26.0.6, react-i18next@17.0.4, @radix-ui/react-dialog@1.1.15
- Existing codebase — `firestore.rules`, `functions/src/reservation/createReservation.ts`, `functions/src/reservation/resolveReservation.ts`, `firebase.json`, `web/i18n/en.json`

### Secondary (MEDIUM confidence)

- GitHub TanStack/query discussion #2621 — onSnapshot + `queryClient.setQueryData` + `staleTime: Infinity` pattern, verified by TanStack docs
- i18next-browser-languagedetector GitHub — `localStorage` cache key, detection order config

### Tertiary (LOW confidence)

- WebSearch results for Tailwind v4 config changes — verified against awareness of v4 release but not against official Tailwind v4 docs directly; recommendation is to pin v3 to avoid uncertainty

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions verified against npm registry 2026-04-19
- Architecture: HIGH — based on official Firebase JS SDK docs and existing project patterns
- Pitfalls: HIGH for Firebase-specific pitfalls (verified against same-day Android bug); MEDIUM for Tailwind v4 (indirect verification)
- Testing: HIGH — Vitest + RTL is standard for Vite projects

**Research date:** 2026-04-19
**Valid until:** 2026-05-19 (stable stack; i18next minor versions may tick)
