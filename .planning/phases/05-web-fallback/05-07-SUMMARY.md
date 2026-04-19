---
phase: 05-web-fallback
plan: 07
subsystem: reservation-web
tags: [tanstack-query, httpsCallable, deep-link, auto-reserve, re-reserve, tdd, react-router]

requires:
  - phase: 05-web-fallback
    plan: 06
    provides: "useCreateReservation, ReserveButton, ReservationBanner, ToastProvider, useActiveReservation"
  - phase: 04-reservation-system
    plan: 06
    provides: "resolveReservation Cloud Function (europe-west3) — callable contract"

provides:
  - "web/src/features/reservation/useResolveReservation.ts — TanStack mutation wrapping httpsCallable(functions, 'resolveReservation')"
  - "web/src/pages/ReReservePage.tsx — full impl: waits for auth, calls callable, navigates on success/error"
  - "web/src/pages/RegistryPage.tsx — reads autoReserveItemId query param and auto-triggers reservation when item is available"

affects: []

tech-stack:
  added: []
  patterns:
    - "ReReservePage: hasFiredRef + mutation state guard prevents double-fire in React 18 StrictMode"
    - "ReReservePage waits for useAuth.isReady before calling resolveReservation (Pitfall 7 cold-start race)"
    - "useResolveReservation: httpsCallable called inside hook (not at module level) — callable is only needed on one page, no singleton benefit"
    - "RegistryPage autoReserveFiredRef pattern: ref gates the useEffect to fire once per mount, safe across re-renders"
    - "autoReserveItemId param cleared from URL with { replace: true } immediately after action — prevents re-trigger on back navigation"
    - "4 anonymous scenarios handled: authenticated, guest with stored identity, guest without identity (GuestIdentityModal), unavailable/missing item (conflict toast)"

key-files:
  created:
    - "web/src/features/reservation/useResolveReservation.ts — useMutation wrapping resolveReservation callable"
    - "web/src/features/reservation/__tests__/useResolveReservation.test.ts — 4 tests: callable name, payload, success shape, error surface"
    - "web/src/features/reservation/__tests__/ReReservePage.test.tsx — 5 tests: loading text, auth gate, param forwarding, success nav, error nav"
    - "web/src/features/reservation/__tests__/RegistryPage.autoReserve.test.tsx — 6 tests: all 4 scenarios + idempotency"
  modified:
    - "web/src/pages/ReReservePage.tsx — replaced Plan 03 stub with full implementation"
    - "web/src/pages/RegistryPage.tsx — added autoReserveItemId useEffect + guest submit handler"
    - "web/src/features/registry/__tests__/RegistryPage.test.tsx — added mocks for useGuestIdentity + useCreateReservation"
    - "web/src/__tests__/App.test.tsx — added mocks for useGuestIdentity, useCreateReservation, useResolveReservation"

key-decisions:
  - "useResolveReservation calls httpsCallable inside the hook (not at module level) — resolveReservation is only needed on ReReservePage, no singleton benefit; test mocks apply at call time"
  - "ReReservePage uses hasFiredRef + mutation state check for StrictMode safety — avoids double-call to resolveReservation in React 18 dev mode"
  - "RegistryPage keeps autoReserve logic in RegistryPage (not ReserveButton) — programmatic trigger on mount has no user click event; RegistryPage already owns the searchParams"
  - "autoReserveFiredRef.current set to true before GuestIdentityModal open — prevents any possible re-render between setState calls from triggering the effect twice"
  - "Conflict toast uses reservation.conflict key (same as race-condition toast from Plan 06) — consistent UX for 'item no longer available' regardless of how user arrived"

metrics:
  duration: "7min"
  completed: "2026-04-19"
  tasks: 2
  files: 8
---

# Phase 05 Plan 07: Re-Reserve Email Deep Link Flow Summary

**Complete email deep-link re-reserve path wired: /reservation/:id/re-reserve calls resolveReservation callable, navigates to /registry/:registryId?autoReserveItemId=:itemId, RegistryPage auto-fires createReservation — 15 new tests, 92 total suite tests passing, Phase 5 WEB-01 through WEB-04 all complete.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-04-19T15:34:34Z
- **Completed:** 2026-04-19T15:41:xx Z
- **Tasks:** 2
- **Files:** 8 created/modified

## What Was Built

### Full Re-Reserve Flow Diagram

```
Email link: /reservation/:id/re-reserve
  └─ ReReservePage
       ├─ Wait: useAuth.isReady === true  (Pitfall 7 guard)
       ├─ Fire: resolveReservation({ reservationId: id })  [europe-west3 callable]
       ├─ onSuccess → navigate('/registry/:registryId?autoReserveItemId=:itemId', { replace: true })
       └─ onError   → navigate('/', { replace: true })

/registry/:registryId?autoReserveItemId=:itemId
  └─ RegistryPage.useEffect
       ├─ Gate: authReady && itemsQ.data loaded
       ├─ Find item by id in items list
       │
       ├─ Case: item not found || status !== 'available'
       │    └─ showToast('reservation.conflict', 'error') + clear param
       │
       ├─ Case: user authenticated
       │    └─ clear param + autoReserveMutation.mutate({ registryId, itemId, giverName, giverEmail, giverId })
       │
       ├─ Case: anonymous + stored guest identity
       │    └─ clear param + autoReserveMutation.mutate({ ..., giverId: null })
       │
       └─ Case: anonymous, no stored identity
            └─ open GuestIdentityModal → onSubmit → clear param + mutate

autoReserveMutation (useCreateReservation)
  ├─ onSuccess: window.open(affiliateUrl, '_blank') + setActive(...) + showToast('reservation.success')
  └─ onError:   mapHttpsErrorToI18nKey + showToast(key, 'error')
```

### Anonymous User Scenarios

| Scenario | Auth | Identity | Action |
|----------|------|----------|--------|
| Authenticated | `user !== null` | — | Fire mutation immediately with uid + email |
| Returning guest | `user === null` | `identity !== null` in localStorage | Fire mutation with stored name + email |
| New guest | `user === null` | `identity === null` | Open GuestIdentityModal; submit fires mutation |
| Item unavailable | any | any | Conflict toast + clear param; no mutation |

### Auto-Reserve Idempotency via useRef Gate

```typescript
const autoReserveFiredRef = useRef(false)

useEffect(() => {
  if (autoReserveFiredRef.current) return  // fired on previous render → skip
  if (!autoReserveItemId) return
  if (!authReady || !itemsQ.data) return   // wait for readiness

  // ... determine scenario ...

  autoReserveFiredRef.current = true       // set BEFORE any async work
  // ... fire mutation or open modal ...
}, [autoReserveItemId, authReady, itemsQ.data, user, identity, ...])
```

The ref persists across React 18 StrictMode's double-mount invocation. The effect body runs twice in dev but the second invocation hits `if (autoReserveFiredRef.current) return` and exits immediately.

### Phase 5 End-to-End Coverage

| Requirement | Plans | Status |
|-------------|-------|--------|
| WEB-01: registry read + item list | Plans 03, 04 | Complete |
| WEB-02: reserve from web (all paths) | Plan 06 (manual) + Plan 07 (re-reserve email) | Complete |
| WEB-03: auth (email, Google, guest) | Plan 05 | Complete |
| WEB-04: retailer redirect (new tab) | Plan 06 (useCreateReservation.onSuccess) | Complete |

**Phase 5 is complete end-to-end.**

## Test Coverage

| Test File | Tests | What's Covered |
|-----------|-------|----------------|
| useResolveReservation.test.ts | 4 | callable name, payload forwarding, success shape, error surface |
| ReReservePage.test.tsx | 5 | loading text, auth gate (Pitfall 7), param forwarding, success nav, error nav |
| RegistryPage.autoReserve.test.tsx | 6 | authenticated auto-fire, unavailable item, missing item, guest with identity, guest without identity (modal), idempotency |

**New tests: 15 | Previous total: 77 | New total: 92**

## Task Commits

1. `b92f456` — feat(05-07): useResolveReservation hook + ReReservePage implementation
2. `9a67e24` — feat(05-07): autoReserveItemId param handler on RegistryPage + full suite green

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] RegistryPage.test.tsx + App.test.tsx broke when RegistryPage imported useGuestIdentity + useCreateReservation**
- **Found during:** Task 2 full suite run
- **Issue:** Plan 07 adds `useGuestIdentity` and `useCreateReservation` imports to RegistryPage. These modules call firebase/functions and firebase/auth at module level respectively, triggering `FirebaseError: auth/invalid-api-key` in jsdom (no real Firebase config in tests).
- **Fix:** Added `vi.mock('../../auth/useGuestIdentity', ...)` and `vi.mock('../../reservation/useCreateReservation', ...)` to RegistryPage.test.tsx. Added the same plus `vi.mock('../features/reservation/useResolveReservation', ...)` (ReReservePage is tested in App.test.tsx router) to App.test.tsx.
- **Files modified:** `RegistryPage.test.tsx`, `App.test.tsx`
- **Commit:** 9a67e24

**2. [Rule 2 - Missing critical functionality] useResolveReservation calls httpsCallable inside hook (deviation from plan's module-level pattern)**
- **Found during:** Task 1 RED phase analysis
- **Reason:** The plan code placed httpsCallable at module level (same as useCreateReservation). However, useCreateReservation uses module-level because it's a singleton used across multiple components. useResolveReservation is only used in ReReservePage and benefits from calling httpsCallable inside the hook so test mocks of `httpsCallableMock.mockReturnValue(callableMock)` in `beforeEach` apply at call time. This also avoids the "do NOT reset httpsCallableMock in beforeEach" constraint from Plan 06.
- **Fix:** Called httpsCallable inside the hook body.
- **Files modified:** `useResolveReservation.ts`
- **Commit:** b92f456

## Known Stubs

None. The re-reserve email deep link flow is fully implemented end-to-end.

## Self-Check: PASSED
