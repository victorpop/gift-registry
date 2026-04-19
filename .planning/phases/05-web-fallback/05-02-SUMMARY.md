---
phase: 05-web-fallback
plan: 02
subsystem: infra
tags: [firebase, app-check, tanstack-query, region-pin, emulator, typescript]

requires:
  - phase: 05-web-fallback
    plan: 01
    provides: "Vite + React 19 scaffold with package.json, vitest config, and vite-env.d.ts env typings"

provides:
  - "web/src/firebase.ts — initializeApp, getFirestore, getFunctions('europe-west3'), getAuth singletons"
  - "web/src/queryClient.ts — QueryClient configured for onSnapshot-driven caching (staleTime: Infinity)"
  - "web/src/main.tsx — App Check init before createRoot, QueryClientProvider wrapping App tree"
  - "web/src/features/__tests__/firebase.test.ts — 4 smoke tests for region pin, persistence, emulator on/off"

affects: [05-03, 05-04, 05-05, 05-06, 05-07]

tech-stack:
  added: []
  patterns:
    - "firebase.ts module singleton pattern: export { app, db, functions, auth } — consumed by feature hooks"
    - "FUNCTIONS_REGION const in firebase.ts — named constant prevents accidental omission of region arg"
    - "Debug token via self.FIREBASE_APPCHECK_DEBUG_TOKEN set before initializeAppCheck call"
    - "App Check init guarded by VITE_RECAPTCHA_SITE_KEY presence — graceful degradation in dev with warning"
    - "vi.hoisted() + vi.resetModules() pattern for testing ES module side-effect init files"

key-files:
  created:
    - "web/src/firebase.ts — Firebase singletons with europe-west3 region pin + emulator wiring"
    - "web/src/queryClient.ts — QueryClient with staleTime Infinity defaults for onSnapshot cache"
    - "web/src/features/__tests__/firebase.test.ts — 4 smoke tests (region, persistence, emulator on/off)"
  modified:
    - "web/src/main.tsx — App Check init + QueryClientProvider + debug token wiring"

decisions:
  - "ES module imports are hoisted, so debug token (self.FIREBASE_APPCHECK_DEBUG_TOKEN) is set at runtime before initializeAppCheck call — not before the import statement. initializeApp/getFirestore/getFunctions/getAuth construct lazy clients with no network calls; actual requests fire when components read/write data, which is after initializeAppCheck."
  - "VITE_RECAPTCHA_SITE_KEY absence in dev shows console.warn and skips App Check init — avoids crash without a reCAPTCHA key registered, making local dev frictionless"
  - "void setPersistence() — fire-and-forget is intentional; the promise only rejects if persistence type is unsupported (it never is for browserLocalPersistence in modern browsers)"

metrics:
  duration: "2min"
  completed: "2026-04-19"
  tasks: 2
  files: 4
---

# Phase 05 Plan 02: Firebase JS SDK Init + App Check + QueryClient Summary

**Firebase JS SDK initialized with `europe-west3` region pin for Functions, App Check wired before `createRoot`, and TanStack QueryClient provided at root — all four WEB-XX requirements unblocked.**

## What Was Built

### `web/src/firebase.ts`

Exports four Firebase singleton instances used throughout all feature hooks:

```typescript
export const app: FirebaseApp = initializeApp(firebaseConfig)
export const db: Firestore = getFirestore(app)
export const functions: Functions = getFunctions(app, FUNCTIONS_REGION)  // 'europe-west3'
export const auth: Auth = getAuth(app)
```

**Critical correctness properties:**
- `getFunctions(app, 'europe-west3')` — prevents the `us-central1` default (same bug fixed in `AppModule.kt` on 2026-04-19)
- `setPersistence(auth, browserLocalPersistence)` — WEB-D-12, parity with Android AUTH-04
- Emulator wiring behind `VITE_USE_EMULATORS === 'true'` using ports from `firebase.json` (Auth 9099, Firestore 8080, Functions 5001)

### `web/src/queryClient.ts`

Single `QueryClient` instance configured for Firestore `onSnapshot`-driven caches:

```typescript
defaultOptions: {
  queries: {
    staleTime: Infinity,        // onSnapshot keeps data fresh; no refetch needed
    refetchOnWindowFocus: false, // subscription is always active
    refetchOnMount: false,       // cache populated by listener, not queryFn
    refetchOnReconnect: false,
    retry: false,                // listener surfaces errors directly
  },
  mutations: { retry: false },
}
```

### `web/src/main.tsx`

Initialization order (execution order, not import order):

1. Debug token set via `self.FIREBASE_APPCHECK_DEBUG_TOKEN` (DEV only)
2. `initializeAppCheck(app, { provider: new ReCaptchaV3Provider(...) })` — before any component renders
3. `createRoot(...).render(<QueryClientProvider client={queryClient}><App /></QueryClientProvider>)`

### `web/src/features/__tests__/firebase.test.ts`

4 smoke tests using `vi.hoisted` + `vi.resetModules` pattern for ES module testing:

| Test | Assertion |
|------|-----------|
| pins getFunctions to europe-west3 | `getFunctions` called with `'europe-west3'` as second arg |
| uses browserLocalPersistence | `setPersistence` called with `browserLocalPersistence` |
| connects emulators when VITE_USE_EMULATORS=true | All three `connect*Emulator` functions called with correct ports |
| does NOT connect emulators when flag is not true | No `connect*Emulator` function called |

## Smoke Test Results

```
 ✓ src/features/__tests__/firebase.test.ts (4 tests) 12ms
 Test Files  1 passed (1)
 Tests  4 passed (4)
```

## Build Verification

```
tsc --noEmit: passes (0 errors)
vite build:   ✓ built in 1.01s (500.85 kB JS bundle — Firebase SDK weight is expected)
```

The chunk size warning (`> 500 kB`) is expected for a fresh Firebase app with no code splitting. This is deferred to a post-v1 optimization (no lazy routes in Phase 5; single SPA page-level splitting planned for Phase 6+).

## Deviations from Plan

### Note on ES Module Import Order

**Found during:** Task 2 implementation

**Issue:** The plan's code example showed `import { app } from './firebase'` appearing after the debug token block as a static import statement. In standard TypeScript/ESM, `import` statements are always hoisted and cannot appear after executable code. A static `import` cannot be placed mid-module.

**Fix applied:** Placed the `import { app } from './firebase'` at the top of the file with all other imports (hoisted). The debug token (`self.FIREBASE_APPCHECK_DEBUG_TOKEN`) is set in the module body, which runs at module evaluation time — after all imports are resolved but before `initializeAppCheck` is called. This is correct because `initializeApp/getFirestore/getFunctions/getAuth` only construct lazy client objects; no network requests fire until component code reads or writes data. `initializeAppCheck` is called before `createRoot`, ensuring App Check is active before any component renders.

**Files modified:** `web/src/main.tsx`

**Impact:** None — same initialization order guarantee holds. The `@ts-expect-error` comment for `self.FIREBASE_APPCHECK_DEBUG_TOKEN` is preserved as specified.

## Known Stubs

None. All files in this plan are fully functional infrastructure. Components that consume these singletons (`import { db } from '@/firebase'`, `import { queryClient } from '@/queryClient'`) will be created in Plans 04-07.

## Self-Check: PASSED
