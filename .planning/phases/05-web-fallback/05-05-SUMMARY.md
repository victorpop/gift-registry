---
phase: 05-web-fallback
plan: 05
subsystem: auth
tags: [firebase-auth, react-hook-form, zod, radix-dialog, localStorage, guest-identity, oauth, tdd]

requires:
  - phase: 05-web-fallback
    plan: 02
    provides: "web/src/firebase.ts — auth singleton with browserLocalPersistence already set"
  - phase: 05-web-fallback
    plan: 03
    provides: "web/src/i18n — auth.* translation keys (en + ro)"

provides:
  - "web/src/features/auth/authProviders.ts — signInEmail, signUpEmail, signInWithGoogle, signOut"
  - "web/src/features/auth/useAuth.ts — { user, isReady } hook subscribing to onAuthStateChanged"
  - "web/src/features/auth/useGuestIdentity.ts — { identity, save, clear } localStorage hook"
  - "web/src/features/auth/AuthModal.tsx — Radix Dialog: Sign In / Create Account tabs + Google OAuth"
  - "web/src/features/auth/GuestIdentityModal.tsx — Radix Dialog: firstName/lastName/email + localStorage pre-fill"

affects: [05-06, 05-07]

tech-stack:
  added: []
  patterns:
    - "authProviders module pattern: UI never imports firebase/auth directly — UI imports from authProviders.ts only (testable via mock)"
    - "useAuth isReady flag pattern: starts false, flips true on first onAuthStateChanged emission (prevents auth-restore flash)"
    - "useGuestIdentity shape-validation on read: all three fields must be present strings or null returned"
    - "Radix Dialog pattern: Dialog.Root + Dialog.Portal + Dialog.Overlay + Dialog.Content with Dialog.Title + Dialog.Description"
    - "react-hook-form + zodResolver: form.handleSubmit(handleValid) with per-field error display via role='alert'"

key-files:
  created:
    - "web/src/features/auth/authProviders.ts — Firebase Auth call wrapper (signIn, signUp, Google OAuth, signOut)"
    - "web/src/features/auth/useAuth.ts — onAuthStateChanged subscription hook"
    - "web/src/features/auth/useGuestIdentity.ts — localStorage-persisted guest identity hook"
    - "web/src/features/auth/AuthModal.tsx — Radix Dialog auth modal"
    - "web/src/features/auth/GuestIdentityModal.tsx — Radix Dialog guest identity collection modal"
    - "web/src/features/auth/__tests__/useAuth.test.tsx — 4 tests (isReady flag behavior, user emission, unmount cleanup)"
    - "web/src/features/auth/__tests__/useGuestIdentity.test.ts — 6 tests (null on empty, pre-fill, save, malformed JSON, missing fields, clear)"
    - "web/src/features/auth/__tests__/AuthModal.test.tsx — 6 tests (tab toggle, Google button, guest link, email submit, signup submit)"
    - "web/src/features/auth/__tests__/GuestIdentityModal.test.tsx — 5 tests (empty fields, pre-fill, validation errors, email format, valid submit)"

decisions:
  - "authProviders.ts catches auth/popup-closed-by-user and auth/cancelled-popup-request silently returning null — caller no-ops instead of showing an error toast"
  - "AuthModal Dialog.Description added as sr-only element — Radix warns when no description is present; both modals now have accessible descriptions"
  - "useGuestIdentity reads identity as lazy useState initializer — avoids a re-render on mount from reading localStorage synchronously"
  - "GuestIdentityModal form.reset() in useEffect on open change — ensures pre-fill reflects any identity update between modal opens"

metrics:
  duration: "3min"
  completed: "2026-04-19"
  tasks: 2
  files: 9
---

# Phase 05 Plan 05: Auth Layer + Guest Identity Summary

**useAuth hook (isReady flag + onAuthStateChanged), useGuestIdentity hook (localStorage 'guestIdentity' with shape { firstName, lastName, email }), AuthModal and GuestIdentityModal built on Radix Dialog with react-hook-form + zod — 21 new tests, all 56 suite tests pass.**

## What Was Built

### Guest Identity Storage Contract

localStorage key: `guestIdentity`
Shape: `{ firstName: string, lastName: string, email: string }`

This mirrors Android's `GuestPreferencesDataStore` (DataStore key `guest_prefs`) exactly. The shape is validated on every read — missing fields or malformed JSON yield `null` (safe default).

### `web/src/features/auth/authProviders.ts`

Thin wrapper around Firebase Auth SDK. The UI layer never imports `firebase/auth` directly:

```typescript
signInEmail(email, password) → Promise<User>
signUpEmail(email, password) → Promise<User>
signInWithGoogle() → Promise<User | null>   // null = popup closed (not an error)
signOut() → Promise<void>
```

`signInWithGoogle` catches `auth/popup-closed-by-user` and `auth/cancelled-popup-request` and returns `null` — no error toast shown to user for dismissing the popup.

### `web/src/features/auth/useAuth.ts`

```typescript
const { user, isReady } = useAuth()
```

- `isReady === false` until the first `onAuthStateChanged` callback fires
- `isReady === true` after Firebase resolves the persisted session (user may still be null)
- Unsubscribes on component unmount
- `browserLocalPersistence` is set in `firebase.ts` (Plan 02) — session survives tab close (WEB-D-12)

### `web/src/features/auth/useGuestIdentity.ts`

```typescript
const { identity, save, clear } = useGuestIdentity()
// identity: GuestIdentity | null
// save({ firstName, lastName, email }) — writes localStorage + updates state
// clear() — removes from localStorage + sets null
```

Cross-tab sync via `storage` event listener.

### `web/src/features/auth/AuthModal.tsx`

Radix `Dialog.Root` with:
- Sign In / Create Account tab toggle (toggle buttons, no Radix Tabs)
- Google OAuth button (inline SVG logo) → `signInWithGoogle()`
- Email/password form with zod (min 6 chars password)
- "Continue as guest" link → calls `onContinueAsGuest` prop + closes modal
- All strings via `t()` — no hardcoded text

### `web/src/features/auth/GuestIdentityModal.tsx`

Radix `Dialog.Root` with:
- Three fields: First Name, Last Name, Email — all required; email format validated
- Pre-fills from `localStorage.guestIdentity` on modal open
- On valid submit: `save(values)` + `onSubmit(values)` + closes modal
- `Dialog.Description` present (accessible, screen-reader visible)

## Plan 06 Integration Points

Plan 06 (reservation flow) can:

```typescript
// 1. Check auth state to decide whether to show GuestIdentityModal
const { user, isReady } = useAuth()

// 2. Show GuestIdentityModal when user is not authenticated on Reserve click
<GuestIdentityModal
  open={guestModalOpen}
  onOpenChange={setGuestModalOpen}
  onSubmit={(identity) => {
    // identity: { firstName, lastName, email }
    // proceed with createReservation callable using identity.email as giverId
  }}
/>

// 3. Show AuthModal from nav Sign In link
<AuthModal
  open={authModalOpen}
  onOpenChange={setAuthModalOpen}
  onContinueAsGuest={() => setGuestModalOpen(true)}
/>

// 4. Read stored identity for reservation payload
const { identity } = useGuestIdentity()
// identity?.email is the giverId for guest reservations
```

## Test Coverage by Requirement

| Requirement | Tests | Coverage |
|-------------|-------|----------|
| WEB-03: sign in email | 1 (AuthModal.test) + useAuth.test | signInEmail called with correct args |
| WEB-03: create account | 1 (AuthModal.test) | signUpEmail called with correct args |
| WEB-03: Google OAuth | 1 (AuthModal.test) | signInWithGoogle invoked on button click |
| WEB-03: continue as guest | 1 (AuthModal.test) | onContinueAsGuest callback called |
| WEB-03: guest identity collection | 5 (GuestIdentityModal.test) | empty, pre-fill, validation x2, valid submit |
| AUTH-04 parity: session persistence | 0 new (Plan 02 covers setPersistence) | browserLocalPersistence set in firebase.ts |
| isReady flag | 2 (useAuth.test) | false before first emission, true after |
| localStorage round-trip | 3 (useGuestIdentity.test) | null on empty, pre-fill, save() |
| Malformed JSON tolerance | 2 (useGuestIdentity.test) | malformed JSON + missing fields → null |

**Total new tests: 21 (10 hooks + 11 modals)**
**Total suite: 56 tests, all passing**

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing a11y] Added Dialog.Description to AuthModal**
- **Found during:** Task 2 implementation
- **Issue:** Radix Dialog warns when `Dialog.Content` has no `Dialog.Description` and no `aria-describedby={undefined}`. AuthModal had no description (GuestIdentityModal did).
- **Fix:** Added `<Dialog.Description className="sr-only">` with the mode-aware title text — visually hidden but present for screen readers.
- **Files modified:** `web/src/features/auth/AuthModal.tsx`
- **Commit:** 8d4f40e

## Known Stubs

None. All auth flows are fully implemented. Plan 06 wires the Reserve button to open GuestIdentityModal when user is not authenticated.

## Self-Check: PASSED
