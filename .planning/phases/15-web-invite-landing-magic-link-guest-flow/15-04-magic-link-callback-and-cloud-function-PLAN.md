---
phase: 15-web-invite-landing-magic-link-guest-flow
plan: 04
type: execute
wave: 2
depends_on: [15-01]
files_modified:
  - web/src/pages/EmailLinkCallbackPage.tsx
  - web/src/pages/__tests__/EmailLinkCallbackPage.test.tsx
  - web/src/App.tsx
  - functions/src/auth/linkInviteOnSignup.ts
  - functions/src/__tests__/linkInviteOnSignup.test.ts
  - functions/src/index.ts
autonomous: true
requirements: []
context_decisions:
  - "Magic-link callback route (new route /auth/email-link)"
  - "Email→UID swap on signup (2nd-gen blocking function beforeUserCreated)"
  - "Post-auth navigation (clean URL, no ?invite=1)"
  - "App Check considerations (Firebase Auth methods not enforced; blocking functions have separate gating)"
gap_closure: false
user_setup:
  - service: firebase-auth
    why: "2nd-gen Identity blocking functions (beforeUserCreated) require Identity Platform enabled on the Firebase project"
    dashboard_config:
      - task: "Enable Identity Platform (one-time upgrade from legacy Firebase Auth)"
        location: "Firebase Console → Authentication → Settings → User actions → Upgrade to Identity Platform"
      - task: "Add /auth/email-link to authorized domains (for actionCodeSettings.url) — gift-registry-ro.web.app and localhost are already authorized per Phase 14; no new host added, so this may be a no-op"
        location: "Firebase Console → Authentication → Settings → Authorized domains"
      - task: "Enable Email/Password sign-in provider with email-link (passwordless) sub-option"
        location: "Firebase Console → Authentication → Sign-in method → Email/Password → toggle 'Email link (passwordless sign-in)' to enabled"

must_haves:
  truths:
    - "Magic-link callback route /auth/email-link exists in App.tsx and renders EmailLinkCallbackPage"
    - "EmailLinkCallbackPage calls completeInviteSignIn on mount and, on success, navigates to the path in the ?next= query param (or / if missing)"
    - "EmailLinkCallbackPage renders an error state when the magic link is expired, the email is missing, or the link is invalid"
    - "Cloud Function linkInviteOnSignup exists as a 2nd-gen beforeUserCreated blocking function, registered in functions/src/index.ts"
    - "linkInviteOnSignup scans registries where invitedUsers['email:{email}'] === true and atomically swaps that entry for {newUid}: true (per registry) inside a transaction"
    - "linkInviteOnSignup is idempotent — running it for an existing user with their UID already in invitedUsers is a no-op"
    - "Email mismatch (signup email differs from any pending invite-email) results in no swap (orphan email:* entries remain untouched)"
  artifacts:
    - path: "web/src/pages/EmailLinkCallbackPage.tsx"
      provides: "Magic-link callback page — completes sign-in and navigates to ?next= path"
      min_lines: 80
      exports: ["default"]
    - path: "web/src/App.tsx"
      provides: "Route /auth/email-link → EmailLinkCallbackPage"
      contains: "/auth/email-link"
    - path: "functions/src/auth/linkInviteOnSignup.ts"
      provides: "beforeUserCreated blocking function that swaps email:{email} → {uid} in registries.invitedUsers"
      exports: ["linkInviteOnSignup"]
      min_lines: 60
    - path: "functions/src/index.ts"
      provides: "Export of linkInviteOnSignup alongside other functions"
      contains: "linkInviteOnSignup"
    - path: "functions/src/__tests__/linkInviteOnSignup.test.ts"
      provides: "Test coverage for swap, idempotent re-run, email mismatch"
      min_lines: 80
  key_links:
    - from: "web/src/App.tsx"
      to: "web/src/pages/EmailLinkCallbackPage.tsx"
      via: "<Route path=\"/auth/email-link\" element={<EmailLinkCallbackPage />} />"
      pattern: "/auth/email-link"
    - from: "web/src/pages/EmailLinkCallbackPage.tsx"
      to: "web/src/features/auth/authProviders.ts"
      via: "import { completeInviteSignIn, isSignInWithEmailLink } from '../features/auth/authProviders'"
      pattern: "completeInviteSignIn"
    - from: "functions/src/index.ts"
      to: "functions/src/auth/linkInviteOnSignup.ts"
      via: "export { linkInviteOnSignup } from './auth/linkInviteOnSignup'"
      pattern: "linkInviteOnSignup"
    - from: "functions/src/auth/linkInviteOnSignup.ts"
      to: "firebase-functions/v2/identity"
      via: "import { beforeUserCreated } from 'firebase-functions/v2/identity'"
      pattern: "from \"firebase-functions/v2/identity\""
---

<objective>
Build the two server/client artifacts that close the magic-link loop:

1. **Web: `/auth/email-link` callback route** — A new page (`EmailLinkCallbackPage.tsx`) that runs on the URL Firebase sends in the magic-link email. On mount it:
   - Verifies the current URL is a sign-in link via `isSignInWithEmailLink`
   - Calls `completeInviteSignIn(window.location.href)` to authenticate the user
   - Reads `?next={path}` from the URL search params
   - Navigates to `?next` (default `/`) using `react-router`'s `navigate(..., { replace: true })` to keep the back button clean
   - On error renders a brief error state with i18n copy (`invite_landing.error_link_expired` / `error_generic`)

2. **Backend: `linkInviteOnSignup` 2nd-gen blocking Cloud Function** — A `beforeUserCreated` handler from `firebase-functions/v2/identity` that runs server-side **before user creation completes**. It:
   - Reads the new user's email from the blocking event payload
   - Queries `registries` for documents where `invitedUsers['email:{email}'] === true` (composite query via `where(\`invitedUsers.email:${email}\`, '==', true)` — using the dotted-field-path query API)
   - For each match, in a single Firestore transaction: delete the `invitedUsers.email:{email}` field via `FieldValue.delete()` and set `invitedUsers.{newUid}` to `true`
   - Returns void on success (the user creation proceeds)
   - On internal error: logs and returns void (do NOT throw — throwing aborts user creation, which is too aggressive for a non-critical swap)

**Why 2nd-gen blocking function (not 1st-gen auth trigger):**
- Per CLAUDE.md: project standard is Cloud Functions 2nd gen.
- Blocking functions run **synchronously** before user creation finishes, so by the time the client receives the new User from `signUpEmail` or `signInWithEmailLink`, the swap has already happened. No async race, no client-side retry-with-backoff needed.
- 1st-gen auth triggers (`functions.auth.user().onCreate`) run async, requiring the client to retry on PERMISSION_DENIED during its first registry read.

**Critical user setup (cannot be automated by Claude):**
- Identity Platform MUST be enabled in Firebase Console (legacy Auth doesn't support blocking functions) — see `user_setup` frontmatter.
- Email-link (passwordless) sign-in MUST be enabled in the Auth provider config.

Purpose: Wave 2, depends on Plan 15-01 (consumes `completeInviteSignIn` + `isSignInWithEmailLink` helpers; reads `invite_landing.error_*` i18n keys). Independent of Plan 15-03 (modal) — both Wave 2 plans can run in parallel because they touch different files. Plan 15-05 (RegistryPage wiring) consumes nothing this plan exports directly, but the end-to-end flow requires this plan's artifacts to exist.

Output: A working magic-link callback page + a deployable Cloud Function that handles the email→UID swap.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/ROADMAP.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-01-i18n-and-auth-providers-PLAN.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-01-i18n-and-auth-providers-SUMMARY.md
@CLAUDE.md

<!-- Canonical references — executor MUST read these first -->
@web/src/App.tsx
@web/src/pages/ReReservePage.tsx
@web/src/features/auth/authProviders.ts
@web/src/features/auth/useAuth.ts
@web/src/components/giftmaison/index.ts
@functions/src/registry/inviteToRegistry.ts
@functions/src/index.ts
@functions/src/__tests__/inviteToRegistry.test.ts
@firestore.rules

<interfaces>
<!-- From web/src/features/auth/authProviders.ts (AFTER Plan 15-01): -->
```typescript
export async function completeInviteSignIn(href: string, fallbackEmail?: string): Promise<User>;
export { isSignInWithEmailLink };  // re-export of firebase/auth function
// Throws Error('not-a-sign-in-link') | Error('missing-email-for-sign-in-link') | underlying firebase errors
```

<!-- From firebase-functions/v2/identity (firebase-functions ^7.2.3 — confirmed at planning time): -->
```typescript
import { beforeUserCreated, AuthBlockingEvent, BlockingOptions } from "firebase-functions/v2/identity";

// AuthBlockingEvent has:
//   - data.email: string | undefined  (the new user's email)
//   - data.uid: string                  (the new user's UID — populated even before creation completes)
//   - data.providerId: string           ('password' for email/password and email-link signups)

export declare function beforeUserCreated(
  handler: (event: AuthBlockingEvent) => MaybeAsync<BeforeCreateResponse | void>
): BlockingFunction;
```

<!-- From firebase-admin/firestore: -->
```typescript
import { FieldPath, FieldValue, getFirestore } from "firebase-admin/firestore";

// FieldPath usage (mirrors functions/src/registry/inviteToRegistry.ts line 92):
//   await registryRef.update(new FieldPath("invitedUsers", inviteKey), true);
// FieldValue.delete() for removing a map entry:
//   await registryRef.update(new FieldPath("invitedUsers", oldKey), FieldValue.delete());
```

<!-- From firestore.rules (DO NOT MODIFY — line 30-36): -->
```javascript
function isInvited(registryData) {
  return isSignedIn() &&
         registryData.get('visibility', 'public') == 'private' &&
         registryData.get('invitedUsers', {})[request.auth.uid] == true;
}
```
The Cloud Function's job is to ensure `invitedUsers[newUid] == true` BEFORE the user reads the registry — so this rule passes.

<!-- From functions/src/registry/inviteToRegistry.ts line 86 (CONVENTION TO MIRROR): -->
```typescript
const inviteKey = invitedUid ?? `email:${email}`;
// So when the user has no account, the key written is `email:bob@example.com` (literal `email:` prefix)
```

<!-- From web/src/App.tsx (CURRENT — 20 lines): -->
```typescript
const router = createBrowserRouter([
  { path: '/',                                          element: <AppRootPage /> },
  { path: '/registry/:id',                              element: <RegistryPage />,      errorElement: <NotFoundPage /> },
  { path: '/registry/:id/item/:itemId',                 element: <ItemReservePage /> },
  { path: '/reservation/:id/re-reserve',                element: <ReReservePage /> },
  { path: '/sign-in',                                   element: <AuthScreen /> },
  { path: '*',                                          element: <NotFoundPage /> },
])
// ADD: { path: '/auth/email-link', element: <EmailLinkCallbackPage /> }
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Create EmailLinkCallbackPage with tests, wire route in App.tsx</name>
  <files>web/src/pages/EmailLinkCallbackPage.tsx, web/src/pages/__tests__/EmailLinkCallbackPage.test.tsx, web/src/App.tsx</files>
  <read_first>
    - web/src/App.tsx (READ FIRST — 20 lines; you are adding ONE route)
    - web/src/pages/ReReservePage.tsx (READ — closest structural analogue: a callback-style page that runs logic on mount, then navigates; mirror its TopNav/Footer + loading-state pattern + useEffect-with-cleanup pattern)
    - web/src/features/auth/authProviders.ts (READ — confirm completeInviteSignIn / isSignInWithEmailLink signatures from Plan 15-01)
    - web/src/features/auth/useAuth.ts (READ — anti-flash pattern: gate behaviour on `isReady` to avoid acting before Firebase Auth resolves persisted session, per Phase 14 fix [[feedback_live_deploy_pacing]])
    - web/src/i18n/en.json (READ — confirm invite_landing.error_link_expired / error_generic / error_email_mismatch keys exist from Plan 15-01)
  </read_first>
  <behavior>
    Six tests in `EmailLinkCallbackPage.test.tsx`:

    1. **calls completeInviteSignIn on mount with the current href** — useEffect fires, mock receives `window.location.href`
    2. **navigates to ?next= path on success** — after completeInviteSignIn resolves, `navigate('/registry/reg-123', { replace: true })` is called
    3. **navigates to / when ?next= is missing** — same as above but with no next param → navigates to '/'
    4. **renders loading state while pending** — before the promise resolves, screen shows a loading indicator (or the dialog hidden, just a "Signing you in…" caption)
    5. **renders error state when completeInviteSignIn throws** — promise rejects → screen shows the error_generic copy
    6. **renders error_link_expired when the link is invalid** — if isSignInWithEmailLink returns false → screen shows error_link_expired copy

    Then create the component to make tests pass + wire the route in App.tsx.
  </behavior>
  <action>
    Step 1 — RED: Create `web/src/pages/__tests__/EmailLinkCallbackPage.test.tsx`:

    ```typescript
    import { beforeEach, describe, expect, it, vi } from 'vitest'
    import { render, screen, waitFor } from '@testing-library/react'
    import { MemoryRouter, Routes, Route } from 'react-router'
    import '../../i18n'

    const navigateMock = vi.fn()
    vi.mock('react-router', async () => {
      const actual = await vi.importActual<typeof import('react-router')>('react-router')
      return { ...actual, useNavigate: () => navigateMock }
    })

    const providerMocks = vi.hoisted(() => ({
      completeInviteSignIn: vi.fn(),
      isSignInWithEmailLink: vi.fn(),
      signInEmail: vi.fn(),
      signUpEmail: vi.fn(),
      sendInviteSignInLink: vi.fn(),
      signInWithGoogle: vi.fn(),
      signOut: vi.fn(),
      getRedirectResult: vi.fn(),
    }))
    vi.mock('../../features/auth/authProviders', () => providerMocks)

    vi.mock('../../firebase', () => ({
      auth: { _kind: 'fakeAuth' },
      app: { _kind: 'fakeApp' },
      db: { _kind: 'fakeDb' },
      functions: { _kind: 'fakeFunctions' },
    }))

    import EmailLinkCallbackPage from '../EmailLinkCallbackPage'

    function renderAt(path: string) {
      return render(
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/auth/email-link" element={<EmailLinkCallbackPage />} />
          </Routes>
        </MemoryRouter>
      )
    }

    describe('EmailLinkCallbackPage', () => {
      beforeEach(() => {
        navigateMock.mockReset()
        providerMocks.completeInviteSignIn.mockReset()
        providerMocks.isSignInWithEmailLink.mockReset().mockReturnValue(true)
      })

      it('calls completeInviteSignIn on mount', async () => {
        providerMocks.completeInviteSignIn.mockResolvedValue({ uid: 'u1', email: 'a@b.com' })
        renderAt('/auth/email-link?next=%2Fregistry%2Freg-123')
        await waitFor(() => {
          expect(providerMocks.completeInviteSignIn).toHaveBeenCalledTimes(1)
        })
      })

      it('navigates to ?next= path on success', async () => {
        providerMocks.completeInviteSignIn.mockResolvedValue({ uid: 'u1', email: 'a@b.com' })
        renderAt('/auth/email-link?next=%2Fregistry%2Freg-123')
        await waitFor(() => {
          expect(navigateMock).toHaveBeenCalledWith('/registry/reg-123', { replace: true })
        })
      })

      it('navigates to / when ?next= is missing', async () => {
        providerMocks.completeInviteSignIn.mockResolvedValue({ uid: 'u1', email: 'a@b.com' })
        renderAt('/auth/email-link')
        await waitFor(() => {
          expect(navigateMock).toHaveBeenCalledWith('/', { replace: true })
        })
      })

      it('renders loading state initially', () => {
        // Never resolve — page stays in pending state
        providerMocks.completeInviteSignIn.mockReturnValue(new Promise(() => {}))
        renderAt('/auth/email-link')
        // Some loading indicator — match "Signing you in" or a status role
        // We assert there's NO error visible yet
        expect(screen.queryByRole('alert')).toBeNull()
      })

      it('renders error_generic when completeInviteSignIn throws', async () => {
        providerMocks.completeInviteSignIn.mockRejectedValue(new Error('bang'))
        renderAt('/auth/email-link')
        await waitFor(() => {
          expect(screen.getByRole('alert')).toBeInTheDocument()
        })
      })

      it('renders error_link_expired when isSignInWithEmailLink returns false', async () => {
        providerMocks.isSignInWithEmailLink.mockReturnValue(false)
        providerMocks.completeInviteSignIn.mockRejectedValue(new Error('not-a-sign-in-link'))
        renderAt('/auth/email-link')
        await waitFor(() => {
          expect(screen.getByRole('alert')).toBeInTheDocument()
        })
        // Expired link copy should be visible — search across both locales
        expect(
          screen.getByText(/this sign-in link has expired|acest link a expirat|something went wrong|ceva nu a mers/i)
        ).toBeInTheDocument()
      })
    })
    ```

    Run `cd web && npx vitest run EmailLinkCallbackPage` — all 6 fail with "Cannot find module ../EmailLinkCallbackPage" (RED).

    Step 2 — GREEN: Create `web/src/pages/EmailLinkCallbackPage.tsx`:

    ```typescript
    import { useEffect, useState } from 'react'
    import { useNavigate, useSearchParams } from 'react-router'
    import { useTranslation } from 'react-i18next'
    import { TopNav, Footer, MonoCaption } from '../components/giftmaison'
    import { completeInviteSignIn, isSignInWithEmailLink } from '../features/auth/authProviders'
    import { auth } from '../firebase'

    type Status = 'pending' | 'success' | 'error'

    /**
     * Phase 15 magic-link callback. Runs at /auth/email-link?next={path}
     * after the user clicks the sign-in link in their invite email.
     *
     * Flow:
     *   1. On mount, check isSignInWithEmailLink(auth, href). If false → error_link_expired.
     *   2. Call completeInviteSignIn(href). The blocking Cloud Function
     *      linkInviteOnSignup runs synchronously before user creation completes,
     *      so by the time we receive the User, registries.invitedUsers already
     *      contains the new UID — no client-side retry needed.
     *   3. Read ?next= and navigate({ replace: true }) — replace so the back
     *      button doesn't return the user to the consumed magic link.
     *   4. On error: render an error message (link expired or generic).
     */
    export default function EmailLinkCallbackPage() {
      const { t } = useTranslation()
      const navigate = useNavigate()
      const [searchParams] = useSearchParams()
      const [status, setStatus] = useState<Status>('pending')
      const [errorKey, setErrorKey] = useState<string>('invite_landing.error_generic')

      useEffect(() => {
        let cancelled = false
        const href = typeof window !== 'undefined' ? window.location.href : ''

        // Guard: if the URL isn't actually a Firebase sign-in link, short-circuit.
        if (!isSignInWithEmailLink(auth, href)) {
          if (!cancelled) {
            setErrorKey('invite_landing.error_link_expired')
            setStatus('error')
          }
          return () => { cancelled = true }
        }

        completeInviteSignIn(href)
          .then(() => {
            if (cancelled) return
            const nextPath = searchParams.get('next') || '/'
            // replace: true keeps the back button clean (no return to consumed link)
            navigate(nextPath, { replace: true })
            setStatus('success')
          })
          .catch((err) => {
            if (cancelled) return
            const msg = err instanceof Error ? err.message : ''
            if (msg === 'not-a-sign-in-link') {
              setErrorKey('invite_landing.error_link_expired')
            } else if (msg.includes('expired') || msg.includes('invalid')) {
              setErrorKey('invite_landing.error_link_expired')
            } else {
              setErrorKey('invite_landing.error_generic')
            }
            setStatus('error')
          })

        return () => { cancelled = true }
      // searchParams is stable per route; including it triggers no extra runs.
      // eslint-disable-next-line react-hooks/exhaustive-deps
      }, [])

      return (
        <div className="min-h-screen flex flex-col bg-gm-paper">
          <TopNav />
          <main className="flex-1 flex items-center justify-center px-4">
            <div className="max-w-[400px] w-full text-center flex flex-col gap-4">
              {status === 'pending' && (
                <>
                  <h1 className="font-display text-[28px] text-gm-ink leading-[1.05] tracking-[-0.5px]">
                    {t('common.loading')}
                  </h1>
                  <MonoCaption size="sm" tone="faint">
                    {t('common.loading')}
                  </MonoCaption>
                </>
              )}
              {status === 'error' && (
                <>
                  <h1 className="font-display text-[28px] text-gm-ink leading-[1.05] tracking-[-0.5px]">
                    {t('invite_landing.title')}
                  </h1>
                  <p role="alert" className="font-body text-[14.5px] text-gm-warn leading-[1.55]">
                    {t(errorKey)}
                  </p>
                </>
              )}
            </div>
          </main>
          <Footer />
        </div>
      )
    }
    ```

    Step 3 — Wire the route in `web/src/App.tsx`. Add the import and route:

    ```typescript
    import EmailLinkCallbackPage from './pages/EmailLinkCallbackPage'

    // Inside createBrowserRouter array, ADD this entry BEFORE the catchall `*`:
    { path: '/auth/email-link', element: <EmailLinkCallbackPage /> },
    ```

    Resulting router:
    ```typescript
    const router = createBrowserRouter([
      { path: '/',                                          element: <AppRootPage /> },
      { path: '/registry/:id',                              element: <RegistryPage />,      errorElement: <NotFoundPage /> },
      { path: '/registry/:id/item/:itemId',                 element: <ItemReservePage /> },
      { path: '/reservation/:id/re-reserve',                element: <ReReservePage /> },
      { path: '/sign-in',                                   element: <AuthScreen /> },
      { path: '/auth/email-link',                           element: <EmailLinkCallbackPage /> },
      { path: '*',                                          element: <NotFoundPage /> },
    ])
    ```

    Step 4 — Run `cd web && npx vitest run EmailLinkCallbackPage` and `cd web && npx vitest run App.test` — all tests pass.

    Commit: `feat(15-04): add /auth/email-link callback page + route`
  </action>
  <verify>
    <automated>cd web && npx vitest run EmailLinkCallbackPage 2>&1 | tail -15</automated>
  </verify>
  <acceptance_criteria>
    - File `web/src/pages/EmailLinkCallbackPage.tsx` exists with ≥80 lines
    - `grep -c "export default function EmailLinkCallbackPage" web/src/pages/EmailLinkCallbackPage.tsx` returns 1
    - `grep -c "completeInviteSignIn" web/src/pages/EmailLinkCallbackPage.tsx` returns at least 2 (import + call)
    - `grep -c "isSignInWithEmailLink" web/src/pages/EmailLinkCallbackPage.tsx` returns at least 2 (import + guard call)
    - `grep -c "searchParams.get('next')" web/src/pages/EmailLinkCallbackPage.tsx` returns 1
    - `grep -c "{ replace: true }" web/src/pages/EmailLinkCallbackPage.tsx` returns 1 (so back button stays clean)
    - `grep -c "t('invite_landing" web/src/pages/EmailLinkCallbackPage.tsx` returns at least 2
    - `grep -c "role=\"alert\"" web/src/pages/EmailLinkCallbackPage.tsx` returns 1 (error rendering)
    - File `web/src/pages/__tests__/EmailLinkCallbackPage.test.tsx` exists with 6 it() blocks: `grep -c "it(" web/src/pages/__tests__/EmailLinkCallbackPage.test.tsx` returns 6
    - `grep -c "path: '/auth/email-link'" web/src/App.tsx` returns 1
    - `grep -c "import EmailLinkCallbackPage from './pages/EmailLinkCallbackPage'" web/src/App.tsx` returns 1
    - `cd web && npx vitest run EmailLinkCallbackPage 2>&1 | grep "6 passed"` matches
    - `cd web && npx tsc --noEmit 2>&1 | grep -E "(EmailLinkCallbackPage|App\.tsx)" | wc -l` returns 0
  </acceptance_criteria>
  <done>EmailLinkCallbackPage exists, wired into App.tsx at /auth/email-link, calls completeInviteSignIn on mount, navigates to ?next= path with replace: true, handles errors with i18n copy, all 6 tests pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create linkInviteOnSignup blocking Cloud Function with tests, register export</name>
  <files>functions/src/auth/linkInviteOnSignup.ts, functions/src/__tests__/linkInviteOnSignup.test.ts, functions/src/index.ts</files>
  <read_first>
    - functions/src/registry/inviteToRegistry.ts (READ FIRST — canonical reference for FieldPath usage on invitedUsers, especially lines 79-92 explaining why `email:` keys with dots in the email body need FieldPath; this function does the OPPOSITE direction — removes the email:* key and adds the uid key)
    - functions/src/__tests__/inviteToRegistry.test.ts (READ — canonical reference for the Jest mock setup pattern for firebase-admin: fakeDb with collection/doc/update/runTransaction; also shows the FieldPath mock at line 154; mirror this exact mocking pattern in your new test file)
    - functions/src/index.ts (READ — 30 lines; you will ADD one line: `export { linkInviteOnSignup } from './auth/linkInviteOnSignup';`)
    - .planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md (READ — sections "Email→UID swap on signup" and "Edge cases")
    - firestore.rules (READ — confirms `isInvited()` predicate at line 30-36 keys on `invitedUsers[request.auth.uid]`; this function's job is to make sure that key exists before the user's first read)
  </read_first>
  <behavior>
    Four tests (Jest, matching the inviteToRegistry.test.ts pattern):

    1. **Test A — swap on signup:** Pre-seed `registries/reg1` with `invitedUsers: { 'email:bob@example.com': true }`. Invoke `linkInviteOnSignup.run({ data: { email: 'bob@example.com', uid: 'new-uid' } })`. Assert: `invitedUsers['email:bob@example.com']` is gone AND `invitedUsers['new-uid'] === true`.

    2. **Test B — idempotent on existing UID:** Pre-seed `registries/reg1` with `invitedUsers: { 'existing-uid': true }`. Invoke the function for user with email `existing@example.com` and uid `existing-uid`. Assert: `invitedUsers` is unchanged (one key, value true, key is `existing-uid`) — no swap because no `email:existing@example.com` key existed.

    3. **Test C — email mismatch is a no-op:** Pre-seed `registries/reg1` with `invitedUsers: { 'email:alice@example.com': true }`. Invoke for user with email `bob@example.com` uid `bob-uid`. Assert: `invitedUsers` still contains exactly `'email:alice@example.com': true` (no swap, no orphan removal — alice's invite is preserved).

    4. **Test D — multiple registries swapped in parallel:** Pre-seed TWO registries (`reg1`, `reg2`) both with `invitedUsers: { 'email:carol@example.com': true }`. Invoke for `carol@example.com` / `carol-uid`. Assert: BOTH registries now have `invitedUsers['carol-uid'] === true` and neither has `'email:carol@example.com'`.

    Then create the Cloud Function so all 4 tests pass + register it in index.ts.
  </behavior>
  <action>
    Step 1 — RED: Create `functions/src/__tests__/linkInviteOnSignup.test.ts`. Mirror the mock-setup pattern from `inviteToRegistry.test.ts` exactly. Key differences:

    - No FCM mock needed (this function doesn't send notifications)
    - Need a `collection.where(...).get()` mock that filters by `invitedUsers.email:{x} == true`
    - The fakeDb must also support iterating over a collection's docs and applying the where filter

    Use these exact contents:

    ```typescript
    /**
     * Tests for linkInviteOnSignup (Phase 15) — 2nd-gen beforeUserCreated blocking
     * function that swaps `invitedUsers["email:{email}"]` → `invitedUsers[{newUid}]`
     * when an email-invited user creates their account.
     */

    // Firestore store shared between tests
    let store: Record<string, Record<string, Record<string, unknown>>>;

    function resetStore() {
      store = { registries: {} };
    }

    // Capture queries so tests can introspect them
    const lastQueryField: { value: string | null } = { value: null };

    jest.mock("firebase-admin", () => {
      const makeDocRef = (collPath: string, docId: string): unknown => ({
        id: docId,
        path: `${collPath}/${docId}`,
        get: async () => {
          const col = store[collPath];
          const data = col ? col[docId] : undefined;
          return { exists: data !== undefined, id: docId, data: () => (data ? { ...data } : undefined) };
        },
        update: async (...args: unknown[]) => {
          if (!store[collPath]) store[collPath] = {};
          // We expect calls of shape:
          //   update(new FieldPath("invitedUsers", oldKey), FieldValue.delete(), new FieldPath("invitedUsers", newKey), true)
          // Iterate args in pairs of (FieldPath, value).
          const existing = (store[collPath][docId] as Record<string, unknown>) || {};
          const invitedUsers = { ...(existing.invitedUsers as Record<string, unknown> || {}) };
          for (let i = 0; i < args.length; i += 2) {
            const fp = args[i] as { segments?: string[] } | unknown;
            const val = args[i + 1];
            const segs = (fp as { segments?: string[] }).segments;
            if (segs && segs[0] === "invitedUsers" && segs.length >= 2) {
              const k = segs[1];
              if (val === "__DELETE__") {
                delete invitedUsers[k];
              } else {
                invitedUsers[k] = val;
              }
            }
          }
          store[collPath][docId] = { ...existing, invitedUsers };
        },
      });

      const makeCollRef = (collPath: string): unknown => {
        const ref: Record<string, unknown> = {
          doc: (id: string) => makeDocRef(collPath, id),
          // where(field, op, value) returns a Query — we filter the docs by the field.
          where: (field: string, _op: string, value: unknown) => {
            lastQueryField.value = field;
            return {
              get: async () => {
                const col = store[collPath] || {};
                const docs = Object.entries(col)
                  .filter(([, data]) => {
                    // Resolve nested dotted field path: "invitedUsers.email:foo@bar.com"
                    const parts = field.split(/\.(.+)/); // splits at first dot only
                    if (parts.length < 2) return false;
                    const [top, rest] = parts;
                    const root = (data as Record<string, unknown>)[top] as Record<string, unknown> | undefined;
                    if (!root) return false;
                    return root[rest] === value;
                  })
                  .map(([id, data]) => ({
                    id,
                    exists: true,
                    data: () => ({ ...data }),
                    ref: makeDocRef(collPath, id),
                  }));
                return { docs, empty: docs.length === 0, size: docs.length };
              },
            };
          },
          get: async () => {
            const col = store[collPath] || {};
            const docs = Object.entries(col).map(([id, data]) => ({
              id,
              exists: true,
              data: () => ({ ...data }),
              ref: makeDocRef(collPath, id),
            }));
            return { docs, empty: docs.length === 0, size: docs.length };
          },
        };
        return ref;
      };

      const fakeDb: Record<string, unknown> = {
        collection: (path: string) => makeCollRef(path),
        runTransaction: async (fn: (tx: unknown) => Promise<unknown>) => {
          const tx = {
            get: async (ref: { get: () => unknown }) => ref.get(),
            update: (ref: { update: (...args: unknown[]) => void }, ...args: unknown[]) =>
              ref.update(...args),
          };
          return fn(tx);
        },
      };

      return {
        __esModule: true,
        initializeApp: jest.fn(),
        firestore: () => fakeDb,
      };
    });

    jest.mock("firebase-admin/firestore", () => ({
      FieldValue: {
        delete: () => "__DELETE__",
        serverTimestamp: () => new Date(),
      },
      FieldPath: class FakeFieldPath {
        segments: string[];
        constructor(...args: string[]) {
          this.segments = args;
        }
      },
      getFirestore: () => {
        // Re-export the firestore() from admin mock so callers using either API work.
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        return require("firebase-admin").firestore();
      },
    }));

    // Import AFTER mocks
    import { linkInviteOnSignup } from "../auth/linkInviteOnSignup";

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    type AnyBlockingEvent = any;

    function makeEvent(email: string, uid: string): AnyBlockingEvent {
      return {
        data: {
          uid,
          email,
          providerId: "password",
        },
        eventId: "test-evt",
        eventType: "providers/cloud.auth/eventTypes/user.beforeCreate",
        resource: "",
        timestamp: new Date().toISOString(),
      };
    }

    beforeEach(() => {
      resetStore();
      lastQueryField.value = null;
    });

    describe("linkInviteOnSignup", () => {
      it("Test A — swaps email:{email} → {uid} on signup", async () => {
        store.registries["reg1"] = {
          ownerId: "owner-x",
          visibility: "private",
          invitedUsers: { "email:bob@example.com": true },
        };
        await linkInviteOnSignup.run(makeEvent("bob@example.com", "new-uid"));
        const invited = store.registries["reg1"].invitedUsers as Record<string, unknown>;
        expect(invited["email:bob@example.com"]).toBeUndefined();
        expect(invited["new-uid"]).toBe(true);
      });

      it("Test B — idempotent when user UID is already in invitedUsers", async () => {
        store.registries["reg1"] = {
          ownerId: "owner-x",
          visibility: "private",
          invitedUsers: { "existing-uid": true },
        };
        await linkInviteOnSignup.run(makeEvent("existing@example.com", "existing-uid"));
        const invited = store.registries["reg1"].invitedUsers as Record<string, unknown>;
        // No email:* key existed → no swap
        expect(Object.keys(invited)).toEqual(["existing-uid"]);
        expect(invited["existing-uid"]).toBe(true);
      });

      it("Test C — email mismatch is a no-op (orphan email:* key preserved)", async () => {
        store.registries["reg1"] = {
          ownerId: "owner-x",
          visibility: "private",
          invitedUsers: { "email:alice@example.com": true },
        };
        await linkInviteOnSignup.run(makeEvent("bob@example.com", "bob-uid"));
        const invited = store.registries["reg1"].invitedUsers as Record<string, unknown>;
        expect(invited["email:alice@example.com"]).toBe(true); // alice's invite preserved
        expect(invited["bob-uid"]).toBeUndefined(); // bob NOT added
      });

      it("Test D — swaps across MULTIPLE registries in one signup", async () => {
        store.registries["reg1"] = {
          ownerId: "owner-x",
          visibility: "private",
          invitedUsers: { "email:carol@example.com": true },
        };
        store.registries["reg2"] = {
          ownerId: "owner-y",
          visibility: "private",
          invitedUsers: { "email:carol@example.com": true, "other-uid": true },
        };
        await linkInviteOnSignup.run(makeEvent("carol@example.com", "carol-uid"));
        const r1 = store.registries["reg1"].invitedUsers as Record<string, unknown>;
        const r2 = store.registries["reg2"].invitedUsers as Record<string, unknown>;
        expect(r1["email:carol@example.com"]).toBeUndefined();
        expect(r1["carol-uid"]).toBe(true);
        expect(r2["email:carol@example.com"]).toBeUndefined();
        expect(r2["carol-uid"]).toBe(true);
        expect(r2["other-uid"]).toBe(true); // unrelated key preserved
      });
    });
    ```

    Run `cd functions && npm test -- linkInviteOnSignup` — all 4 tests FAIL (file doesn't exist).

    Step 2 — GREEN: Create `functions/src/auth/linkInviteOnSignup.ts`:

    ```typescript
    import { beforeUserCreated } from "firebase-functions/v2/identity";
    import * as admin from "firebase-admin";
    import { FieldPath, FieldValue } from "firebase-admin/firestore";

    /**
     * Phase 15: 2nd-gen blocking function that runs synchronously BEFORE a new
     * user is created in Firebase Auth (via email/password OR email-link signup).
     *
     * Purpose: when a registry owner invited someone by email (via
     * inviteToRegistry, which writes `invitedUsers["email:{email}"] = true`),
     * and that invitee later signs up, this function swaps the email-keyed
     * entry for their newly-assigned UID. After the swap, the security rule
     * `isInvited()` (firestore.rules line 30-36) passes for the new user on
     * their FIRST registry read — no async race, no client retry needed.
     *
     * Algorithm:
     *   1. Query `registries` where `invitedUsers.email:{email} == true`.
     *   2. For each matching registry, run a transaction that:
     *        - reads the doc (so concurrent owner-updates can't clobber)
     *        - calls update() with FieldPath("invitedUsers", "email:{email}")
     *          = FieldValue.delete() AND FieldPath("invitedUsers", "{uid}") = true
     *   3. Return void — letting user creation proceed.
     *
     * Failure semantics: ANY thrown error from inside a blocking function
     * ABORTS user creation. That's too aggressive for a non-critical swap, so
     * we catch + log all errors and return void. Worst case the user gets
     * created but their UID isn't in invitedUsers yet — they'll see a 404 on
     * the private registry until manually re-invited. Tradeoff is documented
     * here intentionally.
     *
     * Edge cases:
     *   - Email mismatch (Test C): no docs match, no-op.
     *   - User already in invitedUsers via prior invite (Test B): no email:*
     *     match exists, no-op.
     *   - Multiple registries (Test D): swap each in its own transaction.
     */
    export const linkInviteOnSignup = beforeUserCreated(
      { region: "europe-west3" },
      async (event) => {
        const email = event.data?.email;
        const uid = event.data?.uid;
        if (!email || !uid) {
          console.warn("[linkInviteOnSignup] missing email or uid; skipping swap");
          return;
        }

        const db = admin.firestore();
        const emailKey = `email:${email}`;
        // Note: Firestore dotted-field-path query. `email:` keys never contain '.'
        // before the colon, so the field path is unambiguous: "invitedUsers.email:bob@example.com"
        // Map-value queries via dotted path are supported in Firestore (composite
        // index NOT required for == on a single map field).
        const fieldPath = `invitedUsers.${emailKey}`;

        try {
          const snapshot = await db
            .collection("registries")
            .where(fieldPath, "==", true)
            .get();

          if (snapshot.empty) {
            return; // no pending invites for this email
          }

          // Sequential transactions per registry to keep each atomic.
          // The expected number of pending invites per email is tiny (usually 1-3).
          for (const docSnap of snapshot.docs) {
            try {
              await db.runTransaction(async (tx) => {
                const ref = docSnap.ref;
                // Combined update: delete old key + set new key in ONE Firestore
                // operation so there's no intermediate state where the user has
                // neither key set (which would lock them out mid-transaction).
                await ref.update(
                  new FieldPath("invitedUsers", emailKey),
                  FieldValue.delete(),
                  new FieldPath("invitedUsers", uid),
                  true,
                );
              });
            } catch (txErr) {
              // Per-registry failure: log and continue with the next. Don't
              // bubble up — see "Failure semantics" docstring above.
              console.error(
                `[linkInviteOnSignup] swap failed for registry ${docSnap.id} (email=${email}, uid=${uid}):`,
                txErr,
              );
            }
          }
        } catch (err) {
          console.error(
            `[linkInviteOnSignup] outer error (email=${email}, uid=${uid}):`,
            err,
          );
          // Do NOT throw — user creation must succeed even if our swap fails.
        }
      },
    );
    ```

    Step 3 — Register in `functions/src/index.ts`. Insert ONE new line after the existing exports (suggested location: after line 22, after `export { onPurchaseNotification }`):

    ```typescript
    export { linkInviteOnSignup } from "./auth/linkInviteOnSignup";
    ```

    Step 4 — `mkdir -p functions/src/auth` if the directory doesn't exist (it doesn't per planning — `ls functions/src/auth/` returned ENOENT). The Cloud Function source file lives at `functions/src/auth/linkInviteOnSignup.ts`.

    Step 5 — Run `cd functions && npm test -- linkInviteOnSignup` → all 4 tests pass.

    Step 6 — Run `cd functions && npx tsc --noEmit` → clean.

    Step 7 — Verify the function is deployable (dry-run): `cd functions && npm run build && firebase deploy --only functions:linkInviteOnSignup --dry-run --project gift-registry-ro` (skip if not currently authenticated to firebase CLI; the build step alone is enough proof — it's `tsc` underneath).

    Commit: `feat(15-04): add linkInviteOnSignup 2nd-gen blocking function for email→UID swap`
  </action>
  <verify>
    <automated>cd functions && npm test -- linkInviteOnSignup 2>&1 | tail -15</automated>
  </verify>
  <acceptance_criteria>
    - Directory `functions/src/auth/` exists
    - File `functions/src/auth/linkInviteOnSignup.ts` exists with ≥60 lines
    - `grep -c "import { beforeUserCreated } from \"firebase-functions/v2/identity\"" functions/src/auth/linkInviteOnSignup.ts` returns 1
    - `grep -c "export const linkInviteOnSignup = beforeUserCreated" functions/src/auth/linkInviteOnSignup.ts` returns 1
    - `grep -c "region: \"europe-west3\"" functions/src/auth/linkInviteOnSignup.ts` returns 1 (matches project's region pin from CLAUDE.md / Phase 5)
    - `grep -c "\\\`email:\\\${email}\\\`" functions/src/auth/linkInviteOnSignup.ts` returns 1 (the email:* key format matches inviteToRegistry.ts convention)
    - `grep -c "FieldValue.delete()" functions/src/auth/linkInviteOnSignup.ts` returns 1
    - `grep -c "FieldPath(\"invitedUsers\"" functions/src/auth/linkInviteOnSignup.ts` returns at least 2 (delete + set)
    - `grep -c "runTransaction" functions/src/auth/linkInviteOnSignup.ts` returns 1
    - `grep -c "catch" functions/src/auth/linkInviteOnSignup.ts` returns at least 2 (per-registry + outer — non-throwing)
    - File `functions/src/__tests__/linkInviteOnSignup.test.ts` exists with 4 it() blocks: `grep -c "it(" functions/src/__tests__/linkInviteOnSignup.test.ts` returns 4
    - `grep -c "export { linkInviteOnSignup } from \"./auth/linkInviteOnSignup\"" functions/src/index.ts` returns 1
    - `cd functions && npm test -- linkInviteOnSignup 2>&1 | grep -E "Tests:.*4 passed"` matches
    - `cd functions && npx tsc --noEmit 2>&1 | grep -E "linkInviteOnSignup" | wc -l` returns 0
    - `cd functions && npm run build 2>&1 | grep -E "error TS" | wc -l` returns 0 (full build succeeds — file compiles to lib/)
  </acceptance_criteria>
  <done>linkInviteOnSignup beforeUserCreated function exists in functions/src/auth/, registered in functions/src/index.ts, builds clean, and all 4 unit tests pass (swap, idempotent, mismatch, multi-registry).</done>
</task>

</tasks>

<verification>
- `cd web && npx vitest run 2>&1 | grep -E "Tests.*passed"` — no regressions (existing tests + 6 new EmailLinkCallbackPage tests all pass)
- `cd functions && npm test 2>&1 | grep -E "Tests:" | tail -3` — no regressions (existing tests + 4 new linkInviteOnSignup tests all pass)
- `cd web && npx tsc --noEmit` exits 0
- `cd functions && npx tsc --noEmit` exits 0
- `cd functions && npm run build` exits 0 (the function builds to lib/auth/linkInviteOnSignup.js)
- `grep -c "/auth/email-link" web/src/App.tsx` returns 1 (route wired)
- `grep -c "linkInviteOnSignup" functions/src/index.ts` returns 1 (function exported)
- Manual deploy preflight (optional, only if authenticated): `cd functions && firebase deploy --only functions:linkInviteOnSignup --dry-run --project gift-registry-ro` exits 0
</verification>

<success_criteria>
This plan is complete when:
1. `EmailLinkCallbackPage.tsx` exists, wired at /auth/email-link, calls completeInviteSignIn on mount, navigates to ?next= with replace:true, handles errors — all 6 Vitest tests pass
2. `linkInviteOnSignup.ts` exists as a 2nd-gen beforeUserCreated blocking function, registered in index.ts, swaps email:{email} → {uid} atomically per registry, idempotent, mismatch-safe, multi-registry-safe — all 4 Jest tests pass
3. Both web and functions builds are clean (tsc passes)
4. No existing tests regress
5. User aware of the dashboard requirements (Identity Platform + email-link sign-in enabled in Firebase Console)
</success_criteria>

<output>
After completion, create `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-04-magic-link-callback-and-cloud-function-SUMMARY.md` summarizing:
- Route added to App.tsx (/auth/email-link)
- EmailLinkCallbackPage file path + test count (6)
- Cloud Function file path + test count (4) + region pin (europe-west3)
- Confirmation that the function uses `beforeUserCreated` from `firebase-functions/v2/identity`
- The 3 user_setup tasks from frontmatter (Identity Platform enable, authorized domains check, email-link sign-in enable) — execute-plan will surface these to the user
- The non-throwing failure semantics (swap failure → log + continue, user creation never blocked by us)
</output>
