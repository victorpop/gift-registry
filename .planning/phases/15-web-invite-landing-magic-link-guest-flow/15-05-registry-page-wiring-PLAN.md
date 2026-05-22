---
phase: 15-web-invite-landing-magic-link-guest-flow
plan: 05
type: execute
wave: 3
depends_on: [15-03, 15-04]
files_modified:
  - web/src/pages/RegistryPage.tsx
  - web/src/__tests__/RegistryPage.invite.test.tsx
autonomous: false
requirements: []
context_decisions:
  - "Invite signal — how the page knows came from an email invite (RegistryPage reads ?invite=1 query param)"
  - "Modal — dismissibility (URL cleanup on dismissal — strip ?invite=1 so page refresh doesn't re-trigger)"
  - "Anti-flash (gate modal-open state on useAuth.isReady per Phase 14 fix)"
  - "Post-auth navigation (clean URL, no ?invite=1, navigate via React Router)"
gap_closure: false

must_haves:
  truths:
    - "RegistryPage mounts InviteLandingModal when ?invite=1 is present AND useAuth.isReady is true AND user is null"
    - "RegistryPage does NOT show InviteLandingModal when user is signed in (even if ?invite=1 is present) — modal is for unauthenticated invitees only"
    - "RegistryPage does NOT show InviteLandingModal during the brief auth-hydration window (isReady=false) — prevents flash for already-signed-in users"
    - "Dismissing the modal (Not now / overlay / Esc) strips ?invite=1 from the URL so a page refresh does not re-trigger the modal"
    - "Successful create-account flow (onAccountCreated callback) strips ?invite=1 and lets the user proceed on the registry — no extra navigation needed since they're already on /registry/:id"
    - "Magic-link guest path's check-email confirmation state is reachable without navigating away from RegistryPage"
    - "An end-to-end Vitest test confirms the modal mount/dismiss/URL-strip cycle"
  artifacts:
    - path: "web/src/pages/RegistryPage.tsx"
      provides: "Mounts InviteLandingModal when ?invite=1 + unauthenticated, with anti-flash gate and URL cleanup"
      contains: "InviteLandingModal"
    - path: "web/src/__tests__/RegistryPage.invite.test.tsx"
      provides: "Vitest coverage for invite-landing detection, modal mount, dismissal URL cleanup, signed-in suppression"
      min_lines: 100
  key_links:
    - from: "web/src/pages/RegistryPage.tsx"
      to: "web/src/features/auth/InviteLandingModal.tsx"
      via: "import InviteLandingModal from '../features/auth/InviteLandingModal'"
      pattern: "InviteLandingModal"
    - from: "web/src/pages/RegistryPage.tsx"
      to: "searchParams.get('invite')"
      via: "useSearchParams hook (already imported)"
      pattern: "searchParams.get\\('invite'\\)"
    - from: "web/src/pages/RegistryPage.tsx"
      to: "useAuth().isReady gate"
      via: "isReady boolean conditional"
      pattern: "isReady"
---

<objective>
Wire the new `InviteLandingModal` (built in Plan 15-03) into `RegistryPage.tsx` so that:

1. When a user lands on `/registry/{id}?invite=1` (from clicking the invite email's CTA — see Plan 15-02) AND they are not yet authenticated, the InviteLandingModal opens.
2. The modal does NOT open if the user is already signed in (skip the invite gate — they already have an account).
3. The modal does NOT open during the brief auth-hydration window (`useAuth.isReady === false`) to prevent flash for already-signed-in users whose session is still loading — per the Phase 14 [[feedback_live_deploy_pacing]] fix pattern, gate the conditional on `isReady`.
4. Dismissing the modal (any path: "Not now", Esc, overlay-click, or successful `onAccountCreated`) strips the `?invite=1` query param so a page refresh doesn't re-trigger the modal in the same session.
5. The continueUrlPath passed to the modal is `/registry/${id}` — the path the magic-link callback (Plan 15-04's EmailLinkCallbackPage) should navigate the user back to after sign-in.

This is the integration plan that lets the whole Phase 15 flow work end-to-end:
- User opens invite email → CTA URL is `https://gift-registry-ro.web.app/registry/{id}?invite=1` (Plan 15-02)
- Page loads, RegistryPage detects `?invite=1` + unauthenticated + isReady → mounts InviteLandingModal (THIS PLAN)
- User picks "Create an account" → fills form → signUpEmail → `linkInviteOnSignup` blocking function swaps email→UID server-side → modal calls onAccountCreated → RegistryPage strips `?invite=1` → user sees registry as authenticated member (THIS PLAN + Plan 15-04 backend)
- OR user picks "Continue as guest" → modal sends magic-link → check-email state → user clicks link in email → /auth/email-link?next=%2Fregistry%2F{id} → completeInviteSignIn → navigate to /registry/{id} (Plan 15-04)

Includes a `checkpoint:human-verify` task at the end because the end-to-end invite-email-click flow requires a real Firebase project (or careful emulator setup with email-link sign-in enabled) and human eyes to confirm the modal renders correctly and the dismissal cycle leaves a clean URL.

Purpose: Wave 3, depends on Plan 15-03 (consumes InviteLandingModal component + InviteLandingModalProps) and Plan 15-04 (consumes the magic-link callback route so the secondary CTA's full loop works; Cloud Function need not be deployed for this plan's unit tests to pass, but the human-verify checkpoint needs it deployed in the emulator at minimum).

Output: RegistryPage.tsx with invite-landing wiring + a focused Vitest suite covering the gating logic + a human-verify checkpoint for the end-to-end flow.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/ROADMAP.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-03-invite-landing-modal-PLAN.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-03-invite-landing-modal-SUMMARY.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-04-magic-link-callback-and-cloud-function-PLAN.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-04-magic-link-callback-and-cloud-function-SUMMARY.md
@CLAUDE.md

<!-- Canonical references — executor MUST read these first -->
@web/src/pages/RegistryPage.tsx
@web/src/features/auth/InviteLandingModal.tsx
@web/src/features/auth/useAuth.ts
@web/src/features/reservation/__tests__/RegistryPage.autoReserve.test.tsx
@web/src/__tests__/RegistryPage.confirmPurchase.test.tsx

<interfaces>
<!-- From web/src/features/auth/InviteLandingModal.tsx (built in Plan 15-03): -->
```typescript
export interface InviteLandingModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  continueUrlPath: string  // pass `/registry/${id}` here
  onAccountCreated: (user: User) => void
}
export default function InviteLandingModal(props: InviteLandingModalProps): JSX.Element;
```

<!-- From web/src/features/auth/useAuth.ts (UNCHANGED — preserved): -->
```typescript
export function useAuth(): { user: User | null, isReady: boolean };
// isReady === false during cold-start until first onAuthStateChanged emission;
// gate ALL invite-modal logic on isReady=true to prevent flash for already-signed-in users.
```

<!-- From web/src/pages/RegistryPage.tsx (CURRENT — already imports useSearchParams, useAuth, useState; only ADD the new modal logic, don't rebuild existing logic): -->
Existing relevant code patterns:
- Line 32: `const [searchParams, setSearchParams] = useSearchParams()`
- Line 33: `const { user, isReady: authReady } = useAuth()`
- Lines 110-184: existing autoReserve effect — gates on `authReady`, demonstrates the URL-cleanup pattern: `const next = new URLSearchParams(searchParams); next.delete('autoReserveItemId'); setSearchParams(next, { replace: true })`
- Lines 322-338: existing modal mounts (AuthModal + GuestIdentityModal) at the bottom of the return — InviteLandingModal mounts here too
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Wire InviteLandingModal into RegistryPage with anti-flash gate and URL cleanup</name>
  <files>web/src/pages/RegistryPage.tsx, web/src/__tests__/RegistryPage.invite.test.tsx</files>
  <read_first>
    - web/src/pages/RegistryPage.tsx (READ FIRST — 341 lines; you are ADDING invite-landing logic; do NOT modify the auto-reserve effect, the j5j override logic, or any existing rendering)
    - web/src/features/auth/InviteLandingModal.tsx (READ — confirm the props shape from Plan 15-03 exactly)
    - web/src/__tests__/RegistryPage.confirmPurchase.test.tsx (READ — canonical Vitest pattern for RegistryPage tests, including useParams mock, useSearchParams mock, useAuth mock, Firestore listener mocks)
    - web/src/features/reservation/__tests__/RegistryPage.autoReserve.test.tsx (READ — canonical example of testing a "?query=val + authReady gate" pattern; the invite test mirrors this structure exactly)
    - .planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md (READ — sections "Invite signal", "Modal — dismissibility", "Anti-flash" in `<specifics>`)
  </read_first>
  <behavior>
    Five tests in `web/src/__tests__/RegistryPage.invite.test.tsx`:

    1. **does NOT open modal when ?invite=1 is absent** — render at `/registry/reg-1` (no query), authReady=true, user=null → InviteLandingModal NOT in DOM
    2. **does NOT open modal when user is signed in** — render at `/registry/reg-1?invite=1`, authReady=true, user={ uid: 'u1' } → modal NOT in DOM
    3. **does NOT open modal during auth hydration** — render at `/registry/reg-1?invite=1`, authReady=false, user=null → modal NOT in DOM (anti-flash)
    4. **opens modal when ?invite=1 + authReady=true + user=null** — modal appears
    5. **dismissing modal strips ?invite=1 from URL** — open the modal, click "Not now", confirm `setSearchParams` was called with a URLSearchParams that does NOT contain `invite`

    Then implement the wiring in RegistryPage.tsx to make tests pass.
  </behavior>
  <action>
    Step 1 — RED: Create `web/src/__tests__/RegistryPage.invite.test.tsx`. Mirror the structure of `RegistryPage.confirmPurchase.test.tsx` (read it first to confirm the mock topology):

    ```typescript
    import { beforeEach, describe, expect, it, vi } from 'vitest'
    import { render, screen, waitFor } from '@testing-library/react'
    import userEvent from '@testing-library/user-event'
    import { MemoryRouter, Routes, Route } from 'react-router'
    import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
    import '../i18n'

    // Track setSearchParams calls
    const setSearchParamsMock = vi.fn()

    // Auth state — overridable per test via the mockState ref
    const authMockState: { user: { uid: string; email: string } | null; isReady: boolean } = {
      user: null,
      isReady: true,
    }

    vi.mock('../features/auth/useAuth', () => ({
      useAuth: () => ({ user: authMockState.user, isReady: authMockState.isReady }),
    }))

    vi.mock('../features/auth/useGuestIdentity', () => ({
      useGuestIdentity: () => ({ identity: null, save: vi.fn(), clear: vi.fn() }),
    }))

    // React-router setSearchParams mock so we can assert URL cleanup
    vi.mock('react-router', async () => {
      const actual = await vi.importActual<typeof import('react-router')>('react-router')
      return {
        ...actual,
        useSearchParams: () => [new URLSearchParams(window.location.search), setSearchParamsMock],
      }
    })

    // Stub all the Firestore queries the page uses so they return loading/empty
    vi.mock('../features/registry/useRegistryQuery', () => ({
      useRegistryQuery: () => ({ data: { id: 'reg-1', ownerId: 'someone-else', title: 'Test Registry', visibility: 'public', invitedUsers: {} } }),
    }))
    vi.mock('../features/registry/useItemsQuery', () => ({
      useItemsQuery: () => ({ data: [] }),
    }))
    vi.mock('../features/reservation/useActiveReservation', () => ({
      useActiveReservation: () => ({ active: null, set: vi.fn() }),
    }))
    vi.mock('../features/reservation/useActiveReservationHydration', () => ({
      useActiveReservationHydration: () => undefined,
    }))
    vi.mock('../features/reservation/useCreateReservation', () => ({
      useCreateReservation: () => ({ mutate: vi.fn(), isPending: false }),
    }))
    vi.mock('../firebase', () => ({
      auth: { _kind: 'fakeAuth' },
      app: { _kind: 'fakeApp' },
      db: { _kind: 'fakeDb' },
      functions: { _kind: 'fakeFunctions' },
    }))

    // Mock the auth providers (used by InviteLandingModal when imported)
    vi.mock('../features/auth/authProviders', () => ({
      signInEmail: vi.fn(),
      signUpEmail: vi.fn().mockResolvedValue({ uid: 'new-u' }),
      signInWithGoogle: vi.fn(),
      signOut: vi.fn(),
      getRedirectResult: vi.fn(),
      sendInviteSignInLink: vi.fn().mockResolvedValue(undefined),
      completeInviteSignIn: vi.fn(),
      isSignInWithEmailLink: vi.fn(),
    }))

    import RegistryPage from '../pages/RegistryPage'

    function renderAt(searchString: string) {
      // Set window.location.search so the useSearchParams mock reads it
      // Note: jsdom allows mutating window.location.search via history.replaceState
      window.history.replaceState({}, '', `/registry/reg-1${searchString}`)
      const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
      return render(
        <QueryClientProvider client={qc}>
          <MemoryRouter initialEntries={[`/registry/reg-1${searchString}`]}>
            <Routes>
              <Route path="/registry/:id" element={<RegistryPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      )
    }

    describe('RegistryPage — invite landing modal gating', () => {
      beforeEach(() => {
        setSearchParamsMock.mockReset()
        authMockState.user = null
        authMockState.isReady = true
      })

      it('does NOT open InviteLandingModal when ?invite=1 is absent', () => {
        authMockState.user = null
        authMockState.isReady = true
        renderAt('')
        // The invite-landing modal title should NOT be present
        expect(screen.queryByText(/You're invited|Ești invitat/i)).toBeNull()
      })

      it('does NOT open InviteLandingModal when user is already signed in', () => {
        authMockState.user = { uid: 'u1', email: 'a@b.com' }
        authMockState.isReady = true
        renderAt('?invite=1')
        expect(screen.queryByText(/You're invited|Ești invitat/i)).toBeNull()
      })

      it('does NOT open InviteLandingModal during auth hydration (anti-flash)', () => {
        authMockState.user = null
        authMockState.isReady = false  // hydrating
        renderAt('?invite=1')
        expect(screen.queryByText(/You're invited|Ești invitat/i)).toBeNull()
      })

      it('opens InviteLandingModal when ?invite=1 + authReady + unauthenticated', async () => {
        authMockState.user = null
        authMockState.isReady = true
        renderAt('?invite=1')
        // Modal title should be present
        expect(await screen.findByText(/You're invited|Ești invitat/i)).toBeInTheDocument()
      })

      it('dismissing modal strips ?invite=1 from URL', async () => {
        const user = userEvent.setup()
        authMockState.user = null
        authMockState.isReady = true
        renderAt('?invite=1')
        await screen.findByText(/You're invited|Ești invitat/i)
        // Click "Not now"
        await user.click(screen.getByRole('button', { name: /not now|mai târziu/i }))
        await waitFor(() => {
          expect(setSearchParamsMock).toHaveBeenCalled()
        })
        // The URLSearchParams passed must NOT contain `invite`
        const lastCallArgs = setSearchParamsMock.mock.calls[setSearchParamsMock.mock.calls.length - 1]
        const params = lastCallArgs[0] as URLSearchParams
        expect(params.get('invite')).toBeNull()
      })
    })
    ```

    Run `cd web && npx vitest run RegistryPage.invite` — first test will pass by accident (no modal logic yet means no modal renders), but tests 4 and 5 will FAIL (modal doesn't exist yet on the page).

    Step 2 — GREEN: Edit `web/src/pages/RegistryPage.tsx`:

    1. Add the import at the top (near line 14):
    ```typescript
    import InviteLandingModal from '../features/auth/InviteLandingModal'
    ```

    2. After the existing `const [filter, setFilter] = useState<ItemFilter>('all')` line (~line 83), add a derived flag for invite-landing visibility:
    ```typescript
    // Phase 15: invite-landing modal — surfaces when an email-invited user
    // opens the registry via the invite email's `?invite=1` URL.
    // Gates:
    //   - searchParam invite === '1' (came from email per Plan 15-02 URL builder)
    //   - authReady (avoid flash for already-signed-in users mid-hydration)
    //   - user === null (don't gate signed-in users — they already have an account)
    const inviteParam = searchParams.get('invite')
    const showInviteLanding = inviteParam === '1' && authReady && !user

    // Strip ?invite=1 from the URL. Called on dismiss AND after successful
    // account creation so a page refresh doesn't re-trigger the modal.
    const stripInviteParam = useCallback(() => {
      const next = new URLSearchParams(searchParams)
      next.delete('invite')
      setSearchParams(next, { replace: true })
    }, [searchParams, setSearchParams])
    ```

    NOTE: `useCallback` is already imported at line 1 — verify before adding.

    3. At the bottom of the JSX return, AFTER the existing `<GuestIdentityModal>` block (~line 338), ADD the InviteLandingModal mount:
    ```typescript
          <InviteLandingModal
            open={showInviteLanding}
            onOpenChange={(open) => {
              // Modal closing for any reason (Not now / Esc / overlay / post-success)
              // → strip ?invite=1 so refresh doesn't re-trigger.
              if (!open) {
                stripInviteParam()
              }
            }}
            continueUrlPath={`/registry/${id ?? ''}`}
            onAccountCreated={() => {
              // signUpEmail succeeded. The blocking Cloud Function
              // linkInviteOnSignup (Plan 15-04) has already swapped
              // invitedUsers[email:{email}] → invitedUsers[{newUid}] server-side
              // before this callback fires. Strip ?invite=1; the user is now
              // on /registry/{id} and the useAuth state will flip on its own
              // when onAuthStateChanged fires from the signup.
              stripInviteParam()
            }}
          />
    ```

    4. Run `cd web && npx vitest run RegistryPage.invite` → all 5 tests pass.

    5. Run `cd web && npx vitest run 2>&1 | grep -E "Tests.*failed"` → no other tests regress.

    6. Run `cd web && npx tsc --noEmit` → clean.

    Commit: `feat(15-05): wire InviteLandingModal into RegistryPage (anti-flash + URL cleanup)`
  </action>
  <verify>
    <automated>cd web && npx vitest run RegistryPage.invite 2>&1 | tail -15</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c "import InviteLandingModal from '../features/auth/InviteLandingModal'" web/src/pages/RegistryPage.tsx` returns 1
    - `grep -c "const inviteParam = searchParams.get('invite')" web/src/pages/RegistryPage.tsx` returns 1
    - `grep -c "showInviteLanding = inviteParam === '1' && authReady && !user" web/src/pages/RegistryPage.tsx` returns 1
    - `grep -c "stripInviteParam" web/src/pages/RegistryPage.tsx` returns at least 3 (declaration + 2 call sites: onOpenChange + onAccountCreated)
    - `grep -c "<InviteLandingModal" web/src/pages/RegistryPage.tsx` returns 1
    - `grep -c "open={showInviteLanding}" web/src/pages/RegistryPage.tsx` returns 1
    - `grep -c "continueUrlPath={\`/registry/" web/src/pages/RegistryPage.tsx` returns 1
    - `grep -c "next.delete('invite')" web/src/pages/RegistryPage.tsx` returns 1
    - File `web/src/__tests__/RegistryPage.invite.test.tsx` exists with 5 it() blocks: `grep -c "it(" web/src/__tests__/RegistryPage.invite.test.tsx` returns 5
    - `cd web && npx vitest run RegistryPage.invite 2>&1 | grep "5 passed"` matches
    - `cd web && npx vitest run 2>&1 | grep -E "Tests.*failed" | grep -v "0 failed" | wc -l` returns 0 (no regressions across the whole web test suite)
    - `cd web && npx tsc --noEmit 2>&1 | grep "RegistryPage" | wc -l` returns 0
    - The existing auto-reserve effect (lines 110-184 of RegistryPage.tsx) is UNTOUCHED — `git diff -U0 web/src/pages/RegistryPage.tsx` shows only the import line, the inviteParam/showInviteLanding/stripInviteParam additions, and the InviteLandingModal mount (no edits to the auto-reserve useEffect or the j5j override useMemo)
  </acceptance_criteria>
  <done>RegistryPage mounts InviteLandingModal under the 3-gate condition (invite=1 + authReady + !user), strips ?invite=1 on any modal close, passes /registry/{id} as continueUrlPath; 5 new tests pass; no existing RegistryPage tests regress.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Human verifies the end-to-end invite-landing flow against the Firebase emulator</name>
  <read_first>
    - .planning/phases/15-web-invite-landing-magic-link-guest-flow/15-04-magic-link-callback-and-cloud-function-PLAN.md (READ — review the user_setup dashboard tasks; the emulator handles most of these automatically but you should know what production needs)
    - web/src/firebase.ts (READ — confirms emulator wiring at port 9099 for Auth, 8080 for Firestore, 5001 for Functions)
  </read_first>
  <files>web/src/pages/RegistryPage.tsx (READ-ONLY here — already modified in Task 1; this checkpoint verifies the end-to-end behaviour, no new file modifications), web/src/features/auth/InviteLandingModal.tsx (READ-ONLY), web/src/pages/EmailLinkCallbackPage.tsx (READ-ONLY), functions/src/auth/linkInviteOnSignup.ts (READ-ONLY)</files>
  <action>This task does NOT modify code. It runs a human-driven verification of the end-to-end Phase 15 flow against the Firebase emulator. The executor agent: (1) starts the Firebase emulator suite with import/export of emulator-data, (2) starts the Vite dev server with VITE_USE_EMULATORS=true, (3) follows the 16 numbered steps in <how-to-verify> below, (4) hands control to the human to execute the verification, (5) waits for the human to type "approved" or describe failures. The executor does NOT proceed past this checkpoint without the human signal.</action>
  <verify><automated>echo "Manual verification required — see how-to-verify block; no automated check substitutes for human UAT against the emulator."</automated></verify>
  <what-built>
    The full Phase 15 invite-landing → magic-link → registry-access flow, end-to-end:

    1. Backend invite send: `inviteToRegistry` callable writes invitedUsers["email:X"] = true AND sends an invite email whose CTA URL contains `?invite=1`
    2. Web detection: `/registry/{id}?invite=1` mounts `InviteLandingModal` (gated on authReady + !user)
    3. Create-account path: form → signUpEmail → `linkInviteOnSignup` blocking function swaps email→UID → modal closes → URL cleaned → user sees registry as authenticated invited member
    4. Magic-link path: secondary CTA → sendInviteSignInLink → modal shows "Check your email" → user clicks email link → `/auth/email-link?next=...` → completeInviteSignIn → swap fires → navigate to /registry/{id}
    5. Dismiss path: "Not now" → modal closes → ?invite=1 stripped → page refresh doesn't re-trigger
  </what-built>
  <how-to-verify>
    PRE-FLIGHT:

    1. Start emulators with persistent data:
       ```bash
       cd /Users/victorpop/ai-projects/gift-registry
       firebase emulators:start --import=./emulator-data --export-on-exit=./emulator-data
       ```
       Expected: Auth on :9099, Firestore on :8080, Functions on :5001, Hosting on :5002.

    2. In a second terminal:
       ```bash
       cd /Users/victorpop/ai-projects/gift-registry/web
       VITE_USE_EMULATORS=true npm run dev
       ```
       (or build + serve via Hosting emulator — either works)

    3. Check that the Auth emulator's UI (http://localhost:4000/auth) supports email-link sign-in. If the emulator silently rejects sendSignInLinkToEmail, enable "Email/Password → Email link" in the emulator UI under the Authentication tab.

    VERIFY UI MODAL (path 1 — create account):

    4. Open http://localhost:5173/registry/{some-private-registry-id}?invite=1 (or whatever the dev server port is) in an Incognito window.
       Expected: InviteLandingModal opens with the title "You're invited", body text, 4 fields (first name, last name, email, password), primary CTA "Create an account", secondary text-button "Continue as guest", small "Not now" dismiss link.

    5. Click "Not now". Expected: modal closes, URL becomes /registry/{id} (no ?invite=1), refresh does NOT re-open the modal.

    6. Refresh with `?invite=1` re-appended. Modal opens again.

    7. Fill out the form: First name = "Test", Last name = "User", Email = "newuser+invite@example.com", Password = "testpass123". Click "Create an account".
       Expected: signUpEmail completes, the blocking function fires server-side (check Functions emulator logs for "[linkInviteOnSignup]" entries), modal closes, URL is clean, user is now signed in (top nav shows their email), they can see the registry contents.

    8. Check Firestore emulator UI (http://localhost:4000/firestore): the registry's `invitedUsers` map should now contain `{newUid: true}` and NOT contain `email:newuser+invite@example.com`.

    VERIFY UI MODAL (path 2 — magic-link guest):

    9. Sign out (top nav). Open http://localhost:5173/registry/{another-private-registry-id}?invite=1 in Incognito.

    10. Fill email = "magicuser@example.com" (or whatever the registry was invited to — use a registry whose invitedUsers has `email:magicuser@example.com`). Click "Continue as guest".
        Expected: modal transitions to "Check your email" state, shows the email address, primary button is now "Mai târziu" / "Not now".

    11. Check the Auth emulator UI → "Sign-in links" tab. Find the magic link for this email, copy it.

    12. Paste it into the address bar (or open in the same window). Expected: lands at /auth/email-link?next=%2Fregistry%2F{id}, briefly shows the loading state, then navigates to /registry/{id} with the user signed in.

    13. Check Firestore: invitedUsers should now contain `{magic-user-uid: true}` and not `email:magicuser@example.com`.

    VERIFY UI MODAL (path 3 — already-signed-in user):

    14. Stay signed in. Open /registry/{id}?invite=1 in the SAME window (not incognito).
        Expected: modal does NOT open (user is already authenticated). Registry renders normally.

    VERIFY UI MODAL (path 4 — anti-flash):

    15. Sign in, then immediately open /registry/{id}?invite=1 in a NEW tab so auth state is hydrating from localStorage.
        Expected: modal does NOT briefly flash before being dismissed by the auth check. The render should be clean: either no modal, or the modal appears only if auth hydration confirms no signed-in user.

    VERIFY UI POLISH:

    16. Visual diff against `SaveYourSpotModal` — confirm both use the same Tailwind classes (bg-gm-paper, rounded-gm-modal, p-6, shadow-gm-modal), same Dialog.Overlay (bg-gm-ink/40 backdrop-blur-[2px]), same Title font (font-display 24px), same MonoCaption tone for subtitle/labels.

    EXPECTED OUTCOMES PER STEP:

    | Step | Expected |
    |------|----------|
    | 4 | Modal renders correctly, all 4 fields present, 3 buttons present |
    | 5 | URL stripped, refresh doesn't re-open |
    | 7 | Account created, blocking fn ran, user signed in, registry visible |
    | 8 | Firestore: invitedUsers swapped (UID present, email:* absent) |
    | 10 | Modal transitions to check-email state |
    | 12 | Magic link works, navigate completes, user signed in |
    | 13 | Firestore: invitedUsers swapped for magic-link user too |
    | 14 | Modal does NOT open for signed-in users |
    | 15 | No flash during hydration |
    | 16 | Visual parity with SaveYourSpotModal |
  </how-to-verify>
  <resume-signal>Type "approved" if all 16 steps pass. Otherwise describe which step failed and what you observed.</resume-signal>
  <done>Human confirms all 16 verification steps PASS in the Firebase emulator. Any FAILURE means filing a gap (probably an InviteLandingModal copy issue, a missing i18n key, an authDomain mismatch — see [[reference_appcheck_cached_failure]] for the cached-failure recovery pattern if App Check blocks the magic-link call) and re-running this plan or opening a `/gsd:debug` session.</done>
</task>

</tasks>

<verification>
- `cd web && npx vitest run 2>&1 | grep "passed" | tail -3` shows all RegistryPage tests + new RegistryPage.invite tests pass
- `cd web && npx tsc --noEmit` exits 0
- `grep -c "InviteLandingModal" web/src/pages/RegistryPage.tsx` returns at least 2 (import + mount)
- Human verification checkpoint approved (Task 2)
- The phase goal achieves: "Email recipients who open a registry invite link see a dedicated landing modal that lets them either create an account or continue as a magic-link guest; both paths land them on the shared registry, with the Cloud Function ensuring their UID enters registries.invitedUsers" — verified by steps 4, 7, 10, 12, 13 of the human UAT
</verification>

<success_criteria>
This plan is complete when:
1. RegistryPage.tsx mounts InviteLandingModal under the 3-condition gate (invite=1 + authReady + !user) with anti-flash + URL cleanup
2. 5 new Vitest tests pass covering: absent param, signed-in suppression, hydration suppression, mount on positive condition, URL cleanup on dismiss
3. No existing RegistryPage tests regress
4. TypeScript compiles clean
5. Human approves the 16-step UAT against the Firebase emulator
</success_criteria>

<output>
After completion, create `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-05-registry-page-wiring-SUMMARY.md` summarizing:
- Lines added to RegistryPage.tsx (import + 3 declarations + 1 JSX mount = ~25 lines)
- Test file path and count (5)
- Confirmation of the 3-gate condition (invite=1 + authReady + !user)
- Confirmation that stripInviteParam fires on BOTH dismiss AND successful onAccountCreated
- Result of the 16-step human UAT (PASS or which steps failed)
- Note on any deferred work surfaced during UAT (e.g. registry-name interpolation in the modal title, custom email-link template) — those go to `.planning/todos/pending/`
</output>
