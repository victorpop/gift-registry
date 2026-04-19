---
phase: 06-notifications-email-flows
plan: "05"
subsystem: web-giver-ui
tags: [react, firebase-functions, httpsCallable, tailwind, i18n, vitest, notf-01, notf-02]
dependency_graph:
  requires:
    - phase: 06-00
      provides: Phase 6 web i18n keys (reservation.confirm_purchase_*)
    - phase: 06-02
      provides: confirmPurchase callable (function name, error codes)
    - phase: 05
      provides: ToastProvider, useActiveReservation, ReservationBanner, RegistryPage
  provides:
    - web/src/features/reservation/useConfirmPurchase.ts (hook wrapping confirmPurchase callable)
    - web/src/features/reservation/ConfirmPurchaseBanner.tsx (UI-SPEC Contract 1 web component)
    - web/src/pages/RegistryPage.tsx (updated — renders ConfirmPurchaseBanner when active)
  affects:
    - web RegistryPage rendering (new banner renders below ReservationBanner)
    - Pre-existing RegistryPage tests (added ConfirmPurchaseBanner mock to prevent transitive firebase init)
tech_stack:
  added: []
  patterns:
    - useConfirmPurchase: useState + useCallback state machine; httpsCallable inside callback (not module scope)
    - ConfirmPurchaseBanner: useEffect refs for idempotent toast on success/error; no dismiss button
    - RegistryPage: conditional render via useActiveReservation().active (existing hook, new destructure)
key_files:
  created:
    - web/src/features/reservation/useConfirmPurchase.ts
    - web/src/features/reservation/ConfirmPurchaseBanner.tsx
    - web/src/features/reservation/__tests__/useConfirmPurchase.test.ts
    - web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx
    - web/src/__tests__/RegistryPage.confirmPurchase.test.tsx
  modified:
    - web/src/pages/RegistryPage.tsx (import + active destructure + conditional render)
    - web/src/features/reservation/__tests__/RegistryPage.autoReserve.test.tsx (ConfirmPurchaseBanner mock)
    - web/src/__tests__/App.test.tsx (ConfirmPurchaseBanner mock)
    - web/src/features/registry/__tests__/RegistryPage.test.tsx (ConfirmPurchaseBanner mock)
decisions:
  - "useConfirmPurchase uses plain useState/useCallback (not TanStack useMutation) to match plan spec; httpsCallable created inside confirm() callback so test mocks bind at call time"
  - "RegistryPage destructures active from useActiveReservation to gate ConfirmPurchaseBanner render — minimal change, no new state"
  - "Pre-existing RegistryPage tests needed ConfirmPurchaseBanner mocks (Rule 1 auto-fix) to prevent transitive firebase.ts auth init failure in jsdom"
metrics:
  duration: ~4min
  completed_date: "2026-04-19T19:19:46Z"
  tasks_completed: 3
  files_modified: 9
---

# Phase 6 Plan 05: Web Giver Confirm-Purchase Banner Summary

`useConfirmPurchase` hook, `ConfirmPurchaseBanner` component (UI-SPEC Contract 1 web), and RegistryPage wiring — web givers can now explicitly confirm purchase via a sticky banner that invokes the `confirmPurchase` Firebase callable.

## What Was Built

### Task 1: useConfirmPurchase Hook

`web/src/features/reservation/useConfirmPurchase.ts`

**Return shape:** `{ confirm: (reservationId: string) => Promise<void>, status: "idle" | "pending" | "success" | "error", error: string | null }`

**Error-code mapping:**
- `functions/failed-precondition` with message `"RESERVATION_EXPIRED"` → `error === "RESERVATION_EXPIRED"` (surfaced unchanged so UI can show localized error key)
- Any other error → `error === err.message` or `"unknown"`

**Key pattern:** `httpsCallable(functions, "confirmPurchase")` is created INSIDE the `confirm` callback (not at module scope) — identical discipline to `useResolveReservation`. This ensures vitest mocks of `firebase/functions` bind correctly at call time.

**Tests:** 7 specs covering idle state, success path, RESERVATION_EXPIRED, generic error, payload passthrough, callable name, retry-after-error reset.

### Task 2: ConfirmPurchaseBanner Component

`web/src/features/reservation/ConfirmPurchaseBanner.tsx`

**Visual contract (UI-SPEC Contract 1 web):**
- `role="status"` + `aria-live="polite"` root div
- `bg-surface-variant` background (#E7E0EC), `min-h-[64px]`, `px-4 py-3`
- Shopping-cart SVG (20px, `text-primary` — #6750A4)
- Heading: `text-sm font-bold` using `t("reservation.confirm_purchase_heading")`
- CTA: `bg-primary text-primary-on rounded-md px-4 py-2 text-sm font-bold min-h-[48px] focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-60 disabled:cursor-not-allowed`
- `font-bold` (700) and `font-normal` (400) only — `font-semibold` explicitly absent

**Toast call sites:**
- Success: `showToast(t("reservation.confirm_purchase_success"), "success")` — fires exactly once via `successToastedRef`
- Error: `showToast(t("reservation.confirm_purchase_error"), "error")` — fires per distinct error message via `errorToastedForRef`
- No dismiss button — banner visibility controlled by parent via `useActiveReservation`

**Tests:** 5 specs covering DOM structure, click→callable, success toast, error toast, disabled+aria-busy during pending.

### Task 3: RegistryPage Wiring

`web/src/pages/RegistryPage.tsx`

**Render condition:** `active` from `useActiveReservation()` (existing hook). Added `active` to the destructure that previously only extracted `set`.

```tsx
<ReservationBanner />
{active && (
  <ConfirmPurchaseBanner reservationId={active.reservationId} />
)}
```

**Unmount mechanism:** `active` is set to `null` when `clear()` is called (which `ReservationBanner` calls when countdown hits 0, or when Firestore snapshot updates status away from "active"). The component unmounts naturally — no internal dismiss logic needed.

**Integration tests:** 3 specs in `RegistryPage.confirmPurchase.test.tsx`:
1. Banner text visible when `active` has a reservation
2. Banner absent when `active === null`
3. Banner absent when reservation cleared (null from hook)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Three pre-existing RegistryPage tests broke after ConfirmPurchaseBanner import**
- **Found during:** Task 3 full suite run
- **Issue:** `RegistryPage` now imports `ConfirmPurchaseBanner` → `useConfirmPurchase` → `firebase.ts`. The three existing RegistryPage test files (`App.test.tsx`, `RegistryPage.test.tsx`, `RegistryPage.autoReserve.test.tsx`) mock `ReservationBanner` but not `ConfirmPurchaseBanner`, causing transitive firebase.ts auth initialization to fail with `auth/invalid-api-key` in jsdom.
- **Fix:** Added `vi.mock('../features/reservation/ConfirmPurchaseBanner', () => ({ ConfirmPurchaseBanner: () => null }))` to each affected test file.
- **Files modified:** `web/src/__tests__/App.test.tsx`, `web/src/features/registry/__tests__/RegistryPage.test.tsx`, `web/src/features/reservation/__tests__/RegistryPage.autoReserve.test.tsx`
- **Commits:** included in Task 3 commit `4bcc348`

**2. [Rule 1 - Bug] ConfirmPurchaseBanner tests needed i18n initialization**
- **Found during:** Task 2 GREEN phase
- **Issue:** Test showed `reservation.confirm_purchase_success` (raw key) instead of translated string; i18next not initialized in test environment.
- **Fix:** Added `import "../../../i18n"` to `ConfirmPurchaseBanner.test.tsx` (mirrors existing test pattern in `ReserveButton.test.tsx`, `RegistryPage.autoReserve.test.tsx`).
- **Files modified:** `web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx`

**3. [Rule 1 - Bug] active variable not in scope in RegistryPage**
- **Found during:** Task 3 integration test (first run)
- **Issue:** `RegistryPage` only destructured `set` from `useActiveReservation()` — adding `{active && ...}` in JSX referenced undefined `active`.
- **Fix:** Changed destructure to `const { active, set: setActive } = useActiveReservation()`.
- **Files modified:** `web/src/pages/RegistryPage.tsx`

## Known Stubs

None — the banner is fully wired to the `confirmPurchase` callable. The callable transitions `reservation.status → "purchased"` atomically (Plan 06-02). No placeholder data paths.

## Manual UAT Items (Carried Forward from VALIDATION.md)

Email-client rendering of received purchase/expiry emails — out of automated scope. These require:
1. A live SendGrid SMTP configuration (production only)
2. Receiving actual email in Gmail / Apple Mail / Outlook and visually inspecting the template layout, subject line, and CTA button click-through

The email templates themselves are fully implemented (Plan 06-01); rendering correctness requires human evaluation.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 7ff9f56 | feat(06-05): implement useConfirmPurchase hook + 7 unit tests |
| 2 | b6b0ae3 | feat(06-05): add ConfirmPurchaseBanner component + 5 tests |
| 3 | 4bcc348 | feat(06-05): wire ConfirmPurchaseBanner into RegistryPage + integration tests |

## Self-Check: PASSED

- FOUND: web/src/features/reservation/useConfirmPurchase.ts
- FOUND: web/src/features/reservation/ConfirmPurchaseBanner.tsx
- FOUND: web/src/__tests__/RegistryPage.confirmPurchase.test.tsx
- FOUND: commits 7ff9f56, b6b0ae3, 4bcc348
