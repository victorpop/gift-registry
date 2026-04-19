---
phase: 05-web-fallback
plan: 06
subsystem: reservation-web
tags: [tanstack-query, httpsCallable, radix-toast, countdown, guest-flow, retailer-redirect, tdd]

requires:
  - phase: 05-web-fallback
    plan: 04
    provides: "ItemCard.reserveSlot + ItemGrid.renderReserve extension points; RegistryPage stub"
  - phase: 05-web-fallback
    plan: 05
    provides: "useAuth, useGuestIdentity, GuestIdentityModal, AuthModal"
  - phase: 04-reservation-system
    plan: 01
    provides: "createReservation Cloud Function (europe-west3) — callable contract"

provides:
  - "web/src/lib/error-mapping.ts — HttpsError code → i18n key mapper"
  - "web/src/features/reservation/useCreateReservation.ts — TanStack mutation wrapping httpsCallable(functions, 'createReservation')"
  - "web/src/features/reservation/useCountdown.ts — setInterval countdown hook from expiresAtMs"
  - "web/src/features/reservation/useActiveReservation.ts — React context for current giver's active reservation"
  - "web/src/features/reservation/ReserveButton.tsx — auth gate CTA (authenticated + guest modal paths)"
  - "web/src/features/reservation/ReservationBanner.tsx — sticky banner with countdown + retailer link"
  - "web/src/components/ToastProvider.tsx — Radix Toast provider + useToast imperative hook"
  - "web/src/pages/RegistryPage.tsx — wired with renderReserve + ReservationBanner + AuthModal sign-in"
  - "web/src/main.tsx — ActiveReservationProvider + ToastProvider wrapping App"

affects: [05-07]

tech-stack:
  added: []
  patterns:
    - "httpsCallable at module level (not inside hook) — callable fn is a singleton; mock must not reset httpsCallableMock in beforeEach"
    - "useMutation onSuccess opens window.open(affiliateUrl, '_blank', 'noopener,noreferrer') before calling options.onSuccess"
    - "useCountdown: Math.max(0, expiresAtMs - Date.now()) prevents NaN/negative diff; expired=true when totalSeconds===0"
    - "useActiveReservation: React state only (not localStorage) — tab-scoped UX is acceptable for 30-min window per CONTEXT.md discretion"
    - "ReserveButton: user?.uid -> immediate callable; user===null -> setGuestModalOpen(true) -> onSubmit -> callable"
    - "ReservationBanner auto-dismiss: useEffect on countdown.expired -> clear() -> component returns null"
    - "Test mocking pattern: RegistryPage.test.tsx + App.test.tsx need vi.mock for useAuth + all reservation components to avoid firebase/auth initialization"

key-files:
  created:
    - "web/src/lib/error-mapping.ts — mapHttpsErrorToI18nKey: failed-precondition/ITEM_UNAVAILABLE→reservation.conflict; else→common.error_generic"
    - "web/src/features/reservation/useCreateReservation.ts — useMutation + httpsCallable + window.open on success"
    - "web/src/features/reservation/useCountdown.ts — setInterval 1s tick, handles past timestamps (expired=true)"
    - "web/src/features/reservation/useActiveReservation.ts — Context+Provider with set/clear API"
    - "web/src/features/reservation/ReserveButton.tsx — auth gate, GuestIdentityModal integration, aria-busy spinner"
    - "web/src/features/reservation/ReservationBanner.tsx — sticky banner, auto-dismiss on expired, retailer anchor"
    - "web/src/components/ToastProvider.tsx — Radix Toast.Provider, ToastContext, useToast()"
    - "web/src/features/reservation/__tests__/useCreateReservation.test.ts — 6 tests"
    - "web/src/features/reservation/__tests__/useCountdown.test.ts — 5 tests"
    - "web/src/features/reservation/__tests__/error-mapping.test.ts — 6 tests"
    - "web/src/features/reservation/__tests__/ReserveButton.test.tsx — 4 tests"
  modified:
    - "web/src/pages/RegistryPage.tsx — wired renderReserve, ReservationBanner, AuthModal, Sign in nav button"
    - "web/src/main.tsx — added ActiveReservationProvider + ToastProvider"
    - "web/src/features/registry/__tests__/RegistryPage.test.tsx — added mocks for useAuth + reservation layer"
    - "web/src/__tests__/App.test.tsx — added mocks for useAuth + reservation layer"

key-decisions:
  - "httpsCallable called at module level (not in useMutation fn) — callable is a lazy client; calling once at init is idiomatic Firebase JS SDK pattern"
  - "window.open in useMutation.onSuccess (not in options.onSuccess) — guarantees retailer tab opens even if caller's onSuccess throws"
  - "useActiveReservation uses React state only (not localStorage) — 30-min window is tab-scoped; localStorage adds cross-tab complexity for minimal benefit"
  - "Auto-dismiss banner via useEffect on countdown.expired — avoids any stale banner after reservation expires without a page reload"
  - "RegistryPage.test.tsx + App.test.tsx: mock useAuth + all reservation layer via vi.mock (not firebase/auth) — prevents firebase/auth initialization error in jsdom"
  - "httpsCallableMock not reset in beforeEach — httpsCallable is called at module load time; resetting clears the call record needed for 'registers callable with name' test"

metrics:
  duration: "7min"
  completed: "2026-04-19"
  tasks: 2
  files: 13
---

# Phase 05 Plan 06: Reservation Flow Web Summary

**End-to-end reservation flow wired: ReserveButton handles auth gate (authenticated/guest), calls createReservation callable against europe-west3, opens retailer in new tab, shows Radix Toast feedback, and updates sticky banner with 30-min countdown — 21 new tests, all 77 suite tests passing.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-04-19T15:24:55Z
- **Completed:** 2026-04-19T15:31:45Z
- **Tasks:** 2
- **Files:** 13 created/modified

## What Was Built

### End-to-End Reservation Flow

```
User clicks ReserveButton
  ├─ user !== null → doReserve(displayName, email, uid)
  └─ user === null → setGuestModalOpen(true)
                       └─ GuestIdentityModal.onSubmit
                            └─ doReserve(firstName+lastName, email, null)

doReserve → mutation.mutate({registryId, itemId, giverName, giverEmail, giverId})
              └─ httpsCallable(functions, 'createReservation') [europe-west3]
                   ├─ onSuccess: window.open(affiliateUrl, '_blank', 'noopener,noreferrer')  // WEB-D-07
                   │            setActive({...})  // feeds ReservationBanner
                   │            showToast(t('reservation.success'), 'success')
                   └─ onError:  mapHttpsErrorToI18nKey(err.code, err.message)
                                showToast(t(key), 'error')
```

### Error-to-Toast Mapping

| HttpsError code | message | i18n key | Toast variant |
|-----------------|---------|----------|---------------|
| `failed-precondition` | any | `reservation.conflict` | error |
| any | `ITEM_UNAVAILABLE` | `reservation.conflict` | error |
| `not-found` | — | `common.error_generic` | error |
| `internal` | — | `common.error_generic` | error |
| any other | — | `common.error_generic` | error |

### Countdown Pattern

```typescript
// useCountdown: pure display-only, no enforcement
function compute(expiresAtMs, nowMs): Countdown {
  const diff = Math.max(0, expiresAtMs - nowMs)  // clamp to 0 prevents NaN
  const totalSeconds = Math.floor(diff / 1000)
  return { minutes: floor(totalSeconds/60), seconds: totalSeconds%60, totalSeconds, expired: totalSeconds===0 }
}
// setInterval(1000) ticks; clearInterval on cleanup
```

### Plan 07 Integration Point

Plan 07 can auto-trigger the reserve flow from `?autoReserveItemId=X` query param:

```tsx
// In RegistryPage (or a useEffect in it), check the query param:
const [searchParams] = useSearchParams()
const autoReserveItemId = searchParams.get('autoReserveItemId')

// Find the item from itemsQ.data, then programmatically trigger ReserveButton:
// Option A: Add a `ref` to ReserveButton and expose a `.trigger()` method
// Option B: Store pendingAutoReserveItemId in state; ReserveButton checks it on mount
// Option C: Move the mutate call up to RegistryPage and pass it down via renderReserve

// Recommended: ReserveButton accepts optional `autoTrigger?: boolean` prop;
// when true + mutation not pending, calls handleClick() in a useEffect(() => {...}, [autoTrigger])
```

## Test Coverage by Requirement

| Requirement | File | Tests | Coverage |
|-------------|------|-------|----------|
| WEB-02: reserve from web (auth path) | ReserveButton.test.tsx | 2 | authenticated mutate, pending state |
| WEB-02: reserve from web (guest path) | ReserveButton.test.tsx | 2 | guest modal open, post-submit mutate |
| WEB-04: retailer redirect | useCreateReservation.test.ts | 2 | window.open called + not called for empty URL |
| RES-09: conflict toast | error-mapping.test.ts | 2 | failed-precondition + ITEM_UNAVAILABLE |
| Countdown | useCountdown.test.ts | 5 | future, tick, past, null, undefined |
| Callable payload | useCreateReservation.test.ts | 2 | full payload shape + onSuccess/onError callbacks |

**Total new tests: 21 (6 + 5 + 6 + 4)**
**Total suite: 77 tests, all passing**

## Task Commits

1. `3e1ba38` — feat(05-06): reservation hooks — useCreateReservation, useCountdown, useActiveReservation, error-mapping
2. `222d47b` — feat(05-06): ToastProvider + ReserveButton + ReservationBanner + RegistryPage reservation wiring

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] httpsCallableMock reset cleared module-level callable registration**
- **Found during:** Task 1 GREEN phase (first test failing)
- **Issue:** The plan's test called `httpsCallableMock.mockReset()` in `beforeEach`, clearing the call record from module initialization time. Since `httpsCallable(functions, 'createReservation')` is called at module level (not inside the hook), the "registers callable" assertion found 0 calls after the reset.
- **Fix:** Removed `httpsCallableMock.mockReset()` from `beforeEach`. The test now checks cumulative calls (including the module-init call) and documents why.
- **Files modified:** `useCreateReservation.test.ts`
- **Commit:** 3e1ba38

**2. [Rule 1 - Bug] TypeScript error on windowOpenSpy type annotation**
- **Found during:** Task 2 build phase
- **Issue:** `ReturnType<typeof vi.spyOn>` without type parameters produced a narrow overloaded type incompatible with the spy's assignment.
- **Fix:** Changed to `ReturnType<typeof vi.spyOn<any, any>>` with eslint-disable comment.
- **Files modified:** `useCreateReservation.test.ts`
- **Commit:** 222d47b

**3. [Rule 1 - Bug] RegistryPage.test.tsx + App.test.tsx broke when RegistryPage imported useAuth + AuthModal**
- **Found during:** Task 2 full suite run
- **Issue:** Plan 06 adds `useAuth`, `AuthModal`, `GuestIdentityModal`, `ReserveButton`, `ReservationBanner` imports to RegistryPage. Existing tests lacked mocks for these, causing `firebase/auth` initialization crash (auth/invalid-api-key in jsdom).
- **Fix:** Added `vi.mock('../../auth/useAuth', ...)`, `vi.mock('../../reservation/ReserveButton', ...)`, `vi.mock('../../reservation/ReservationBanner', ...)`, `vi.mock('../../auth/AuthModal', ...)`, `vi.mock('../../auth/GuestIdentityModal', ...)` to both RegistryPage.test.tsx and App.test.tsx.
- **Files modified:** `RegistryPage.test.tsx`, `App.test.tsx`
- **Commit:** 222d47b

## Known Stubs

None. All reservation flow components are fully implemented. Plan 07 can wire `?autoReserveItemId=X` using the approach documented in the "Plan 07 Integration Point" section above.

## Self-Check: PASSED

All created files verified present on disk. Both task commits (3e1ba38, 222d47b) verified in git log.
