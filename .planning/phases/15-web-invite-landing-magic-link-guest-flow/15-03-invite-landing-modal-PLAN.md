---
phase: 15-web-invite-landing-magic-link-guest-flow
plan: 03
type: execute
wave: 2
depends_on: [15-01]
files_modified:
  - web/src/features/auth/InviteLandingModal.tsx
  - web/src/features/auth/__tests__/InviteLandingModal.test.tsx
autonomous: true
requirements: []
context_decisions:
  - "Modal — visual & interaction (Radix Dialog, gm-paper, font-display, MonoCaption, Btn, Field — mirrors SaveYourSpotModal)"
  - "Modal — dismissibility (Dismissible: Not now / overlay-click / Esc closes; URL cleanup happens in caller per Plan 15-05)"
  - "Claude's Discretion — inline password+name fields in modal vs. routing to AuthScreen — DECISION: inline (single-modal variant)"
  - "Guest path — magic-link instead of pure anonymous"
gap_closure: false

must_haves:
  truths:
    - "InviteLandingModal renders a Radix Dialog with bg-gm-paper, font-display title, MonoCaption subtitle, Btn primary CTA — matching SaveYourSpotModal style conventions"
    - "Modal has two states: initial (choice screen with first/last/email/password fields + primary 'Create an account' and secondary 'Continue as guest') and check-email (post-magic-link-send confirmation showing the email address)"
    - "Primary CTA 'Create an account' validates all fields and calls signUpEmail from authProviders; on success calls props.onAccountCreated(user) and closes the modal"
    - "Secondary CTA 'Continue as guest' validates email field only and calls sendInviteSignInLink from authProviders, then transitions the modal to the check-email state"
    - "Modal is dismissible — clicking the dismiss button, overlay, or Esc fires props.onOpenChange(false)"
    - "Modal uses i18n keys from the invite_landing namespace (no hardcoded strings)"
  artifacts:
    - path: "web/src/features/auth/InviteLandingModal.tsx"
      provides: "Dismissible invite-landing modal with two states (initial + check-email)"
      min_lines: 150
      exports: ["default", "InviteLandingModalProps"]
    - path: "web/src/features/auth/__tests__/InviteLandingModal.test.tsx"
      provides: "Vitest coverage of both modal states + primary/secondary CTAs + dismissal"
      min_lines: 80
  key_links:
    - from: "web/src/features/auth/InviteLandingModal.tsx"
      to: "web/src/features/auth/authProviders.ts"
      via: "import { signUpEmail, sendInviteSignInLink } from './authProviders'"
      pattern: "from './authProviders'"
    - from: "web/src/features/auth/InviteLandingModal.tsx"
      to: "web/src/i18n/en.json:invite_landing.*"
      via: "useTranslation hook → t('invite_landing.title') etc"
      pattern: "t\\('invite_landing"
    - from: "web/src/features/auth/InviteLandingModal.tsx"
      to: "web/src/components/giftmaison"
      via: "import { Btn, Field, MonoCaption } from '../../components/giftmaison'"
      pattern: "from '../../components/giftmaison'"
---

<objective>
Create the new dismissible invite-landing modal that mirrors `SaveYourSpotModal.tsx`'s visual conventions (Radix Dialog, gm-paper bg, font-display title, MonoCaption subtitle, Btn primary CTA, Field input atoms) but is a separate file with its own props and two-state behavior:

**State 1 — Initial choice screen:** Title ("You're invited"), body explaining the two paths, first/last/email/password fields, primary CTA "Create an account →" (validates all fields, calls `signUpEmail`), secondary text-button "Continue as guest" (validates email only, calls `sendInviteSignInLink`), and a small dismiss link/Btn ("Not now").

**State 2 — Check-email confirmation:** Title ("Check your email"), body ("We sent a sign-in link to {email}…"), single dismiss/close action. Reached after secondary CTA succeeds.

**Planner decision rationale:** I chose the *inline* variant (password + name fields in the modal) over the *route-to-AuthScreen-with-return-intent* variant because:
- `AuthScreen.tsx` (read at planning time) does not currently accept a return-intent param — it uses `navigate(-1)` after success. Adding return-intent would mean modifying a screen unrelated to Phase 15's scope.
- Inline keeps the user on the registry context (no full-page navigation away and back), which matches the dismissible-modal UX direction from CONTEXT.md.
- The form is short (4 fields), well within Radix Dialog's natural size budget — same pattern as `SaveYourSpotModal`.

This plan does NOT wire the modal into `RegistryPage.tsx` — that's Plan 15-05's job. This plan delivers a standalone, fully-tested component.

Purpose: Wave 2, depends on Plan 15-01 (i18n keys + sendInviteSignInLink helper). Plans 15-04 (callback page) and 15-05 (page wiring) consume this component.

Output: A new `InviteLandingModal.tsx` component file + a Vitest test file covering both states.
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
@web/src/features/auth/SaveYourSpotModal.tsx
@web/src/features/auth/authProviders.ts
@web/src/features/auth/useGuestIdentity.ts
@web/src/components/giftmaison/index.ts
@web/src/components/giftmaison/Btn.tsx
@web/src/components/giftmaison/Field.tsx
@web/src/components/giftmaison/MonoCaption.tsx
@web/src/features/auth/__tests__/AuthModal.test.tsx
@web/src/i18n/en.json

<interfaces>
<!-- From web/src/features/auth/authProviders.ts (AFTER Plan 15-01 lands): -->
```typescript
export async function signUpEmail(email: string, password: string): Promise<User>;
export async function sendInviteSignInLink(email: string, continueUrl: string): Promise<void>;
```

<!-- From firebase/auth: -->
```typescript
import type { User } from 'firebase/auth';
```

<!-- From web/src/components/giftmaison/index.ts: -->
```typescript
export { Btn, type BtnProps, type BtnVariant, type BtnSize } from './Btn'
export { Field, type FieldProps } from './Field'
export { MonoCaption, type MonoCaptionProps } from './MonoCaption'
```

<!-- The InviteLandingModal exports this prop shape (consumed by Plan 15-05): -->
```typescript
export interface InviteLandingModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Path the magic-link callback should navigate to after sign-in.
   *  Plan 15-05 passes `/registry/${registryId}` here. */
  continueUrlPath: string
  /** Called after signUpEmail succeeds. Parent decides what to do
   *  with the new User (typically: navigate to the registry, strip ?invite=1). */
  onAccountCreated: (user: User) => void
}
```

<!-- i18n keys this component uses (from Plan 15-01's invite_landing namespace): -->
```
invite_landing.title
invite_landing.body
invite_landing.primary_cta
invite_landing.secondary_cta
invite_landing.dismiss_cta
invite_landing.first_name_label
invite_landing.last_name_label
invite_landing.email_label
invite_landing.password_label
invite_landing.password_placeholder
invite_landing.check_email_title
invite_landing.check_email_body  (interpolates {{email}})
invite_landing.error_invalid_email
invite_landing.error_send_failed
invite_landing.error_generic
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Write failing tests for InviteLandingModal (RED)</name>
  <files>web/src/features/auth/__tests__/InviteLandingModal.test.tsx</files>
  <read_first>
    - web/src/features/auth/__tests__/AuthModal.test.tsx (READ FIRST — canonical Vitest pattern for modal tests in this codebase: vi.hoisted mocks for authProviders, vi.mock for firebase, vi.mock for useAuth, render + screen + userEvent imports, '../../../i18n' side-effect import)
    - web/src/features/auth/SaveYourSpotModal.tsx (READ — to see how a similar component's tests would assert on the structure)
    - .planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md (READ — sections "Modal — visual & interaction" + "Modal — dismissibility" + "Guest path — magic-link")
  </read_first>
  <behavior>
    Six tests, all initially failing (component doesn't exist yet):

    1. **renders initial state** — open=true → screen shows the title "You're invited" (or its translated string), the body text, "Create an account" primary button, "Continue as guest" secondary button, "Not now" dismiss button, and 4 fields (first name, last name, email, password)

    2. **dismisses on "Not now" click** — clicking "Not now" calls onOpenChange(false)

    3. **dismisses on Esc** — pressing Escape calls onOpenChange(false)

    4. **primary CTA: validation blocks submit on empty fields** — clicking "Create an account" with empty form does NOT call signUpEmail mock

    5. **primary CTA: success path** — fill all 4 fields with valid values, click "Create an account", signUpEmail mock resolves with { uid: 'u1' } → onAccountCreated callback fires with the user, onOpenChange(false) fires

    6. **secondary CTA: success path transitions to check-email state** — fill email field, click "Continue as guest", sendInviteSignInLink mock resolves → modal shows "Check your email" title and the email rendered in the body
  </behavior>
  <action>
    Create `web/src/features/auth/__tests__/InviteLandingModal.test.tsx`. Mirror the structure of `AuthModal.test.tsx` precisely (same mocks, same imports, same describe-block shape). Use these exact contents:

    ```typescript
    import { beforeEach, describe, expect, it, vi } from 'vitest'
    import { render, screen } from '@testing-library/react'
    import userEvent from '@testing-library/user-event'
    import '../../../i18n'

    const providerMocks = vi.hoisted(() => ({
      signUpEmail: vi.fn(),
      sendInviteSignInLink: vi.fn(),
      completeInviteSignIn: vi.fn(),
      isSignInWithEmailLink: vi.fn(),
      signInEmail: vi.fn(),
      signInWithGoogle: vi.fn(),
      signOut: vi.fn(),
      getRedirectResult: vi.fn(),
    }))
    vi.mock('../authProviders', () => providerMocks)

    vi.mock('../../../firebase', () => ({
      auth: { _kind: 'fakeAuth' },
      app: { _kind: 'fakeApp' },
      db: { _kind: 'fakeDb' },
      functions: { _kind: 'fakeFunctions' },
    }))
    vi.mock('../useAuth', () => ({
      useAuth: () => ({ user: null, isReady: true }),
    }))

    import InviteLandingModal from '../InviteLandingModal'

    function renderModal(overrides?: {
      onOpenChange?: (o: boolean) => void
      onAccountCreated?: (u: { uid: string }) => void
    }) {
      const onOpenChange = overrides?.onOpenChange ?? vi.fn()
      const onAccountCreated = overrides?.onAccountCreated ?? vi.fn()
      render(
        <InviteLandingModal
          open
          onOpenChange={onOpenChange}
          continueUrlPath="/registry/reg-123"
          onAccountCreated={onAccountCreated as never}
        />
      )
      return { onOpenChange, onAccountCreated }
    }

    describe('InviteLandingModal', () => {
      beforeEach(() => {
        providerMocks.signUpEmail.mockReset().mockResolvedValue({ uid: 'u1' })
        providerMocks.sendInviteSignInLink.mockReset().mockResolvedValue(undefined)
      })

      it('renders the initial choice state with all CTAs and fields', () => {
        renderModal()
        // Title — match by accessible name (role=dialog title)
        expect(screen.getByText(/You're invited|Ești invitat/i)).toBeInTheDocument()
        // Primary CTA
        expect(screen.getByRole('button', { name: /create an account|creează un cont/i })).toBeInTheDocument()
        // Secondary CTA
        expect(screen.getByRole('button', { name: /continue as guest|continuă ca invitat/i })).toBeInTheDocument()
        // Dismiss
        expect(screen.getByRole('button', { name: /not now|mai târziu/i })).toBeInTheDocument()
        // Four fields
        expect(screen.getByLabelText(/first name|prenume/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/last name|nume/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/password|parolă/i)).toBeInTheDocument()
      })

      it('calls onOpenChange(false) when "Not now" is clicked', async () => {
        const user = userEvent.setup()
        const { onOpenChange } = renderModal()
        await user.click(screen.getByRole('button', { name: /not now|mai târziu/i }))
        expect(onOpenChange).toHaveBeenCalledWith(false)
      })

      it('calls onOpenChange(false) when Esc is pressed', async () => {
        const user = userEvent.setup()
        const { onOpenChange } = renderModal()
        await user.keyboard('{Escape}')
        expect(onOpenChange).toHaveBeenCalledWith(false)
      })

      it('primary CTA does NOT call signUpEmail when fields are empty', async () => {
        const user = userEvent.setup()
        renderModal()
        await user.click(screen.getByRole('button', { name: /create an account|creează un cont/i }))
        expect(providerMocks.signUpEmail).not.toHaveBeenCalled()
      })

      it('primary CTA: signup success calls onAccountCreated + closes modal', async () => {
        const user = userEvent.setup()
        const { onOpenChange, onAccountCreated } = renderModal()
        await user.type(screen.getByLabelText(/first name|prenume/i), 'Alice')
        await user.type(screen.getByLabelText(/last name|nume/i), 'Doe')
        await user.type(screen.getByLabelText(/email/i), 'alice@example.com')
        await user.type(screen.getByLabelText(/password|parolă/i), 'strongpass123')
        await user.click(screen.getByRole('button', { name: /create an account|creează un cont/i }))
        // signUpEmail called with (email, password)
        expect(providerMocks.signUpEmail).toHaveBeenCalledWith('alice@example.com', 'strongpass123')
        // onAccountCreated called with the new user
        expect(onAccountCreated).toHaveBeenCalledWith(expect.objectContaining({ uid: 'u1' }))
        expect(onOpenChange).toHaveBeenCalledWith(false)
      })

      it('secondary CTA: magic-link send transitions to check-email state', async () => {
        const user = userEvent.setup()
        renderModal()
        await user.type(screen.getByLabelText(/email/i), 'bob@example.com')
        await user.click(screen.getByRole('button', { name: /continue as guest|continuă ca invitat/i }))
        // sendInviteSignInLink called with (email, continueUrl)
        // continueUrl shape: ${origin}/auth/email-link?next=%2Fregistry%2Freg-123
        expect(providerMocks.sendInviteSignInLink).toHaveBeenCalledWith(
          'bob@example.com',
          expect.stringContaining('/auth/email-link?next=%2Fregistry%2Freg-123')
        )
        // Modal transitions to check-email state — title swaps
        expect(await screen.findByText(/check your email|verifică-ți emailul/i)).toBeInTheDocument()
        // Email rendered in confirmation body
        expect(screen.getByText(/bob@example\.com/)).toBeInTheDocument()
      })
    })
    ```

    Run `cd web && npx vitest run InviteLandingModal` — all 6 tests MUST fail with "Cannot find module '../InviteLandingModal'" (file doesn't exist yet). That's the RED gate.

    Commit: `test(15-03): add failing InviteLandingModal test suite`
  </action>
  <verify>
    <automated>cd web && npx vitest run InviteLandingModal 2>&1 | tail -10 | grep -E "Cannot find module.*InviteLandingModal|0 passed" && echo "RED CONFIRMED"</automated>
  </verify>
  <acceptance_criteria>
    - File `web/src/features/auth/__tests__/InviteLandingModal.test.tsx` exists
    - `grep -c "describe('InviteLandingModal'" web/src/features/auth/__tests__/InviteLandingModal.test.tsx` returns 1
    - `grep -c "it(" web/src/features/auth/__tests__/InviteLandingModal.test.tsx` returns 6
    - `grep -c "vi.mock('../authProviders'" web/src/features/auth/__tests__/InviteLandingModal.test.tsx` returns 1
    - `grep -c "sendInviteSignInLink" web/src/features/auth/__tests__/InviteLandingModal.test.tsx` returns at least 2 (mock def + assertion)
    - `cd web && npx vitest run InviteLandingModal 2>&1` exits non-zero AND the output mentions "Cannot find module" OR "0 passed" (confirming RED)
    - The file imports `'../../../i18n'` for side-effects (matches AuthModal.test.tsx pattern)
  </acceptance_criteria>
  <done>Test file exists with 6 tests covering both modal states and all CTAs; running the suite fails because the component doesn't exist yet — this is the intended RED state.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement InviteLandingModal.tsx to make tests pass (GREEN)</name>
  <files>web/src/features/auth/InviteLandingModal.tsx</files>
  <read_first>
    - web/src/features/auth/SaveYourSpotModal.tsx (READ FIRST — file is 257 lines; this is the canonical reference for the new modal's Radix Dialog structure, Tailwind class strings, font-display title pattern, Btn placement, Field layout, error rendering — copy structure but adapt to two-state UI)
    - web/src/features/auth/authProviders.ts (READ — confirm sendInviteSignInLink and signUpEmail signatures from Plan 15-01)
    - web/src/features/auth/__tests__/InviteLandingModal.test.tsx (READ — the test file you just created in Task 1; mirror the prop shape exactly)
    - web/src/components/giftmaison/Btn.tsx (READ — `variant: 'primary' | 'ghost'`, `size: 'sm' | 'md' | 'lg'`)
    - web/src/components/giftmaison/Field.tsx (READ — confirms props: label, type, error, autoComplete, register-spreadable)
    - web/src/components/giftmaison/MonoCaption.tsx (READ — confirms props: size, tone)
    - web/src/features/auth/useGuestIdentity.ts (READ — if you want to pre-fill the name/email fields from a prior guest identity, this is the hook to use; OPTIONAL — keep it simple, skip pre-fill if it complicates the test setup)
  </read_first>
  <behavior>
    Build the modal so all 6 tests from Task 1 pass:

    - Initial state renders the title, body, 4 fields, primary CTA, secondary CTA, dismiss button
    - "Not now" + Esc + overlay click → onOpenChange(false)
    - Primary CTA validates all fields (firstName, lastName non-empty; email valid; password ≥ 8 chars) — uses react-hook-form + zod, same as SaveYourSpotModal
    - Primary CTA success → signUpEmail → onAccountCreated(user) → onOpenChange(false)
    - Secondary CTA validates only email; on success → sendInviteSignInLink(email, continueUrl) → transition to check-email state
    - check-email state renders title + body (with email interpolated) + a single dismiss/close action
    - continueUrl built as `${window.location.origin}/auth/email-link?next=${encodeURIComponent(continueUrlPath)}`
    - All copy through i18n keys (no hardcoded strings)
  </behavior>
  <action>
    Create `web/src/features/auth/InviteLandingModal.tsx`. Use SaveYourSpotModal as the structural template; the diffs are:
    - Add a `view` state: `'initial' | 'check_email'`
    - Add `continueUrlPath` and `onAccountCreated` props (no `itemName`, no `onContinueAsGuest` — those are reservation-flow specific)
    - Secondary CTA calls `sendInviteSignInLink` instead of identity-only save
    - Add a small "Not now" dismiss button (Btn variant="ghost" or a plain underlined button — match SaveYourSpotModal's "Continue as guest" link style)

    Full implementation:

    ```typescript
    import * as Dialog from '@radix-ui/react-dialog'
    import { useEffect, useState } from 'react'
    import { useForm } from 'react-hook-form'
    import { zodResolver } from '@hookform/resolvers/zod'
    import { z } from 'zod'
    import { useTranslation } from 'react-i18next'
    import type { User } from 'firebase/auth'
    import { Btn, Field, MonoCaption } from '../../components/giftmaison'
    import { signUpEmail, sendInviteSignInLink } from './authProviders'

    const fullSchema = z.object({
      firstName: z.string().min(1, 'required'),
      lastName: z.string().min(1, 'required'),
      email: z.string().min(1, 'required').email('email'),
      password: z.string().min(8, 'weak'),
    })

    const emailOnlySchema = fullSchema.pick({ email: true })

    type FormValues = z.infer<typeof fullSchema>

    type View = 'initial' | 'check_email'

    export interface InviteLandingModalProps {
      open: boolean
      onOpenChange: (open: boolean) => void
      /** Registry path the magic-link callback should navigate to after sign-in. */
      continueUrlPath: string
      /** Called after signUpEmail succeeds with the newly-created Firebase User. */
      onAccountCreated: (user: User) => void
    }

    function buildContinueUrl(path: string): string {
      const base = typeof window !== 'undefined' ? window.location.origin : ''
      return `${base}/auth/email-link?next=${encodeURIComponent(path)}`
    }

    /**
     * Phase 15 invite-landing modal — shown when an unauthenticated user opens
     * a registry via the invite email (URL carries ?invite=1). Distinct from
     * SaveYourSpotModal (which is the public-link reserve-flow gate).
     *
     * Two paths to authenticated state:
     *   - Primary: "Create an account" → inline form (first/last/email/password),
     *     calls signUpEmail. On success, parent (Plan 15-05's RegistryPage)
     *     receives the new User and decides what to do next. The 2nd-gen
     *     blocking Cloud Function `linkInviteOnSignup` (Plan 15-04) ensures
     *     the new UID is in registries.invitedUsers before the user's first
     *     Firestore read.
     *   - Secondary: "Continue as guest" → calls sendInviteSignInLink with
     *     a continueUrl pointing at /auth/email-link?next={registryPath}.
     *     Modal transitions to a "Check your email" confirmation state.
     *
     * Dismissible: "Not now" / overlay-click / Esc closes the modal. URL
     * cleanup (stripping ?invite=1) is the caller's responsibility (Plan 15-05).
     */
    export default function InviteLandingModal({
      open,
      onOpenChange,
      continueUrlPath,
      onAccountCreated,
    }: InviteLandingModalProps) {
      const { t } = useTranslation()
      const [view, setView] = useState<View>('initial')
      const [serverError, setServerError] = useState<string | null>(null)
      const [pendingMagicLink, setPendingMagicLink] = useState(false)
      const [sentToEmail, setSentToEmail] = useState<string>('')

      const form = useForm<FormValues>({
        resolver: zodResolver(fullSchema),
        defaultValues: { firstName: '', lastName: '', email: '', password: '' },
      })

      // Reset to initial state whenever the modal opens.
      useEffect(() => {
        if (open) {
          setView('initial')
          setServerError(null)
          setSentToEmail('')
          form.reset({ firstName: '', lastName: '', email: '', password: '' })
        }
      }, [open, form])

      function translateFieldError(message: string | undefined): string | undefined {
        if (!message) return undefined
        if (message === 'required') return t('invite_landing.error_invalid_email')
        if (message === 'email') return t('invite_landing.error_invalid_email')
        if (message === 'weak') return t('invite_landing.error_generic')
        return message
      }

      async function handleCreateAccount(values: FormValues) {
        setServerError(null)
        try {
          const user = await signUpEmail(values.email, values.password)
          onAccountCreated(user)
          onOpenChange(false)
        } catch {
          setServerError(t('invite_landing.error_generic'))
        }
      }

      async function handleContinueAsGuestClick() {
        setServerError(null)
        const emailValue = form.getValues('email')
        const parsed = emailOnlySchema.safeParse({ email: emailValue })
        if (!parsed.success) {
          form.setError('email', { type: 'manual', message: 'email' })
          return
        }
        setPendingMagicLink(true)
        try {
          await sendInviteSignInLink(parsed.data.email, buildContinueUrl(continueUrlPath))
          setSentToEmail(parsed.data.email)
          setView('check_email')
        } catch {
          setServerError(t('invite_landing.error_send_failed'))
        } finally {
          setPendingMagicLink(false)
        }
      }

      const errors = form.formState.errors

      return (
        <Dialog.Root open={open} onOpenChange={onOpenChange}>
          <Dialog.Portal>
            <Dialog.Overlay className="fixed inset-0 bg-gm-ink/40 z-40 backdrop-blur-[2px]" />
            <Dialog.Content
              className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-gm-paper rounded-gm-modal p-6 shadow-gm-modal max-h-[90vh] overflow-y-auto"
              aria-describedby="invite-landing-desc"
            >
              {view === 'initial' ? (
                <>
                  <Dialog.Title className="font-display text-[24px] text-gm-ink leading-[1.1] tracking-[-0.5px] font-normal">
                    {t('invite_landing.title')}
                  </Dialog.Title>

                  <Dialog.Description
                    id="invite-landing-desc"
                    className="mt-3 font-body text-[14.5px] text-gm-inkSoft leading-[1.55]"
                  >
                    {t('invite_landing.body')}
                  </Dialog.Description>

                  <form
                    onSubmit={form.handleSubmit(handleCreateAccount)}
                    className="mt-5 flex flex-col gap-4"
                    noValidate
                  >
                    <Field
                      label={t('invite_landing.first_name_label')}
                      type="text"
                      autoComplete="given-name"
                      aria-invalid={Boolean(errors.firstName)}
                      error={translateFieldError(errors.firstName ? String(errors.firstName.message) : undefined)}
                      {...form.register('firstName')}
                    />
                    <Field
                      label={t('invite_landing.last_name_label')}
                      type="text"
                      autoComplete="family-name"
                      aria-invalid={Boolean(errors.lastName)}
                      error={translateFieldError(errors.lastName ? String(errors.lastName.message) : undefined)}
                      {...form.register('lastName')}
                    />
                    <Field
                      label={t('invite_landing.email_label')}
                      type="email"
                      autoComplete="email"
                      aria-invalid={Boolean(errors.email)}
                      error={translateFieldError(errors.email ? String(errors.email.message) : undefined)}
                      {...form.register('email')}
                    />
                    <Field
                      label={t('invite_landing.password_label')}
                      type="password"
                      autoComplete="new-password"
                      placeholder={t('invite_landing.password_placeholder')}
                      aria-invalid={Boolean(errors.password)}
                      error={translateFieldError(errors.password ? String(errors.password.message) : undefined)}
                      {...form.register('password')}
                    />

                    {serverError && (
                      <span role="alert" className="font-body text-[13px] text-gm-warn">
                        {serverError}
                      </span>
                    )}

                    <div className="flex flex-col gap-3 mt-2">
                      <Btn
                        type="submit"
                        variant="primary"
                        size="lg"
                        disabled={form.formState.isSubmitting || pendingMagicLink}
                      >
                        {t('invite_landing.primary_cta')}
                      </Btn>
                      <button
                        type="button"
                        onClick={handleContinueAsGuestClick}
                        disabled={form.formState.isSubmitting || pendingMagicLink}
                        className="font-body text-[14px] text-gm-inkSoft underline hover:text-gm-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {t('invite_landing.secondary_cta')}
                      </button>
                      <button
                        type="button"
                        onClick={() => onOpenChange(false)}
                        className="font-body text-[13px] text-gm-inkFaint hover:text-gm-inkSoft focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent self-center mt-1"
                      >
                        {t('invite_landing.dismiss_cta')}
                      </button>
                    </div>
                  </form>
                </>
              ) : (
                <>
                  <Dialog.Title className="font-display text-[24px] text-gm-ink leading-[1.1] tracking-[-0.5px] font-normal">
                    {t('invite_landing.check_email_title')}
                  </Dialog.Title>

                  <Dialog.Description
                    id="invite-landing-desc"
                    className="mt-3 font-body text-[14.5px] text-gm-inkSoft leading-[1.55]"
                  >
                    {t('invite_landing.check_email_body', { email: sentToEmail })}
                  </Dialog.Description>

                  <div className="mt-6 flex flex-col gap-3">
                    <MonoCaption size="micro" tone="faint">
                      {sentToEmail}
                    </MonoCaption>
                    <Btn
                      type="button"
                      variant="primary"
                      size="lg"
                      onClick={() => onOpenChange(false)}
                    >
                      {t('invite_landing.dismiss_cta')}
                    </Btn>
                  </div>
                </>
              )}
            </Dialog.Content>
          </Dialog.Portal>
        </Dialog.Root>
      )
    }
    ```

    Run `cd web && npx vitest run InviteLandingModal` — all 6 tests should pass GREEN.

    If a test fails because the i18n test setup doesn't include the `invite_landing` namespace yet, confirm Plan 15-01 actually ran first (it should have — this plan depends_on 15-01).

    If the secondary-CTA test fails because `email` rendered without the `@example.com` string visible to the assertion, double-check that you render `sentToEmail` in the modal body (it's both interpolated into `t('invite_landing.check_email_body', { email })` and emitted via the explicit `<MonoCaption>{sentToEmail}</MonoCaption>` line).

    Commit: `feat(15-03): implement InviteLandingModal (dismissible, two-state, i18n)`
  </action>
  <verify>
    <automated>cd web && npx vitest run InviteLandingModal 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - File `web/src/features/auth/InviteLandingModal.tsx` exists with ≥150 lines
    - `grep -c "export default function InviteLandingModal" web/src/features/auth/InviteLandingModal.tsx` returns 1
    - `grep -c "export interface InviteLandingModalProps" web/src/features/auth/InviteLandingModal.tsx` returns 1
    - `grep -c "import \* as Dialog from '@radix-ui/react-dialog'" web/src/features/auth/InviteLandingModal.tsx` returns 1
    - `grep -c "from '../../components/giftmaison'" web/src/features/auth/InviteLandingModal.tsx` returns 1
    - `grep -c "signUpEmail, sendInviteSignInLink" web/src/features/auth/InviteLandingModal.tsx` returns 1 (single import)
    - `grep -c "t('invite_landing" web/src/features/auth/InviteLandingModal.tsx` returns at least 10 (one per i18n key used)
    - `grep -c "bg-gm-paper" web/src/features/auth/InviteLandingModal.tsx` returns at least 1
    - `grep -c "font-display" web/src/features/auth/InviteLandingModal.tsx` returns at least 2 (one per state title)
    - `grep -c "view === 'initial'" web/src/features/auth/InviteLandingModal.tsx` returns 1 (two-state conditional)
    - `grep -c "encodeURIComponent" web/src/features/auth/InviteLandingModal.tsx` returns 1 (continueUrl construction)
    - `grep -c "/auth/email-link?next=" web/src/features/auth/InviteLandingModal.tsx` returns 1
    - `grep -cE "\".+\":|'[A-Z][a-z].*[a-z]'" web/src/features/auth/InviteLandingModal.tsx | head -1` — no hardcoded user-facing strings (visual review)
    - `cd web && npx vitest run InviteLandingModal 2>&1 | grep -E "Tests.*passed" | grep "6 passed"` returns 1 line (all 6 tests GREEN)
    - `cd web && npx tsc --noEmit 2>&1 | grep -E "InviteLandingModal" | wc -l` returns 0
  </acceptance_criteria>
  <done>InviteLandingModal.tsx exists, mirrors SaveYourSpotModal styling (gm-paper bg, font-display title, MonoCaption, Btn, Field), implements two-state behavior (initial + check_email), uses 10+ i18n keys, builds continueUrl with /auth/email-link?next= prefix, and all 6 Vitest cases pass.</done>
</task>

</tasks>

<verification>
- `cd web && npx vitest run InviteLandingModal 2>&1 | grep "6 passed"` matches (all 6 tests GREEN)
- `cd web && npx vitest run 2>&1 | grep -E "Tests.*failed" | grep -v "0 failed"` returns no output (no other tests regress)
- `cd web && npx tsc --noEmit` exits 0
- Visual inspection of the modal: opens InviteLandingModal in Storybook OR run dev server, force-render the modal, confirm gm-paper bg, font-display title, primary Btn, secondary text-button, dismiss text-button, 4 Field inputs — pixel-similar to SaveYourSpotModal
- No hardcoded user-facing strings — every visible string comes through `t('invite_landing.*')` or a sub-key
</verification>

<success_criteria>
This plan is complete when:
1. `web/src/features/auth/InviteLandingModal.tsx` exists, ≥150 lines, exports default + InviteLandingModalProps
2. Component implements 2-state UI (initial choice + check-email confirmation)
3. Component is dismissible (Not now button + Esc + overlay click → onOpenChange(false))
4. Primary CTA validates all 4 fields and calls signUpEmail → onAccountCreated
5. Secondary CTA validates email, calls sendInviteSignInLink, transitions to check-email state
6. All 6 Vitest tests pass
7. Component uses i18n keys exclusively (no hardcoded strings)
8. TypeScript compiles clean
</success_criteria>

<output>
After completion, create `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-03-invite-landing-modal-SUMMARY.md` summarizing:
- Component file path and line count
- Test file path and test count (6)
- Confirmation that the inline create-account variant was chosen over the route-to-AuthScreen variant (with rationale)
- Confirmation that continueUrl format is `${origin}/auth/email-link?next=${encodeURIComponent(path)}` — so Plan 15-04 can mirror this on the callback page
- List of all i18n keys consumed (so Plan 15-04 / 15-05 can avoid duplication)
- Props interface (InviteLandingModalProps) signature so Plan 15-05 knows how to instantiate the modal
</output>
