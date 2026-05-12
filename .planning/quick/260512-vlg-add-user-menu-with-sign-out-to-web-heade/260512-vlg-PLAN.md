---
phase: quick-260512-vlg
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/package.json
  - web/i18n/en.json
  - web/i18n/ro.json
  - web/src/components/giftmaison/UserMenu.tsx
  - web/src/components/giftmaison/index.ts
  - web/src/components/giftmaison/TopNav.tsx
  - web/src/components/giftmaison/__tests__/UserMenu.test.tsx
autonomous: false
requirements:
  - QUICK-VLG-01

must_haves:
  truths:
    - "Signed-in users see a clickable user avatar button in the TopNav (not a static div)"
    - "Clicking (or Enter/Space on) the avatar opens a dropdown menu containing a Sign out action"
    - "Pressing Escape, clicking outside, or focusing away closes the menu"
    - "Selecting Sign out calls Firebase Auth signOut(), the menu closes, and the TopNav re-renders the 'Sign in' CTA (user → null via onAuthStateChanged)"
    - "All visible strings (button aria-label, menu items, error toast) resolve via i18next in both en and ro"
  artifacts:
    - path: "web/src/components/giftmaison/UserMenu.tsx"
      provides: "Accessible dropdown menu attached to the user avatar trigger"
      min_lines: 40
    - path: "web/src/components/giftmaison/TopNav.tsx"
      provides: "TopNav that mounts <UserMenu /> when authenticated instead of a static avatar div"
      contains: "UserMenu"
    - path: "web/i18n/en.json"
      provides: "auth.user_menu_label, auth.sign_out keys"
      contains: "user_menu_label"
    - path: "web/i18n/ro.json"
      provides: "auth.user_menu_label, auth.sign_out keys (Romanian)"
      contains: "user_menu_label"
    - path: "web/src/components/giftmaison/__tests__/UserMenu.test.tsx"
      provides: "Vitest coverage: opens on click, closes on Esc, calls signOut on selecting the action"
      min_lines: 40
  key_links:
    - from: "web/src/components/giftmaison/UserMenu.tsx"
      to: "web/src/features/auth/authProviders.ts"
      via: "import { signOut } from '../../features/auth/authProviders'"
      pattern: "from.*authProviders"
    - from: "web/src/components/giftmaison/TopNav.tsx"
      to: "web/src/components/giftmaison/UserMenu.tsx"
      via: "renders <UserMenu user={user} /> in the authenticated branch"
      pattern: "<UserMenu"
    - from: "web/src/components/giftmaison/UserMenu.tsx"
      to: "@radix-ui/react-dropdown-menu"
      via: "DropdownMenu.Root + Trigger + Content + Item primitives"
      pattern: "from '@radix-ui/react-dropdown-menu'"
---

<objective>
Add a user menu dropdown to the web TopNav so that authenticated users can sign out. Today the TopNav renders the user avatar as a static `<div>` with no click handler (web/src/components/giftmaison/TopNav.tsx lines 44–50), leaving signed-in users with no way to disconnect. This plan turns that avatar into a Radix `DropdownMenu.Trigger` that opens a menu containing a single "Sign out" item, calling the existing `signOut()` from `features/auth/authProviders.ts`.

Purpose: Close the obvious UX gap — a logged-in user must always be able to log out. Without this, the only way to switch accounts is to clear browser state.

Output:
- New `UserMenu` atom in the giftmaison kit using `@radix-ui/react-dropdown-menu` (keyboard-accessible by default — Esc, click-outside, Tab focus return all handled by Radix).
- TopNav wires the new menu when `user` is non-null.
- Two new i18n keys (`auth.user_menu_label`, `auth.sign_out`) in en + ro.
- Vitest coverage for the open / close / sign-out path.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@web/src/components/giftmaison/TopNav.tsx
@web/src/features/auth/authProviders.ts
@web/src/features/auth/useAuth.ts
@web/src/components/ToastProvider.tsx
@web/src/components/giftmaison/Btn.tsx
@web/i18n/en.json
@web/i18n/ro.json
@web/package.json

<interfaces>
<!-- Contracts the executor needs — already established in the codebase. -->

From web/src/features/auth/authProviders.ts:
```typescript
export async function signOut(): Promise<void>
```
Calls `firebase/auth` `signOut(auth)` under the hood. Returns void; throws on network errors. After it resolves, `onAuthStateChanged` fires inside `useAuth`, which sets `user → null` reactively — no manual navigation needed.

From web/src/features/auth/useAuth.ts:
```typescript
export function useAuth(): { user: User | null, isReady: boolean }
```
TopNav already subscribes via this hook. Once `signOut()` resolves, `user` becomes `null` on the next tick and TopNav re-renders the "Sign in" branch automatically.

From web/src/components/ToastProvider.tsx:
```typescript
export function useToast(): { showToast: (title: string, variant?: 'success' | 'error' | 'neutral') => void }
```
Use `showToast(t('common.error_generic'), 'error')` on signOut failure. App is wrapped by ToastProvider at the root so `useToast()` is always available inside TopNav/UserMenu.

From web/src/components/giftmaison/TopNav.tsx (current avatar block — lines 44–50):
```tsx
{user ? (
  <div
    aria-label={user.displayName ?? user.email ?? 'Account'}
    className="w-8 h-8 rounded-full bg-gm-second text-gm-paper flex items-center justify-center font-body text-[12px] font-medium"
  >
    {initials || 'A'}
  </div>
) : onSignInClick ? ( /* Sign in button */ ) : ( /* Sign in link */ )}
```
The avatar is a static div. Replace this `<div>` with `<UserMenu user={user} initials={initials || 'A'} />`. Keep the existing initials derivation logic intact (lines 33–37) — pass the computed `initials` string into UserMenu so the trigger keeps the same visual.

Radix DropdownMenu primitives needed (after install):
```typescript
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
// DropdownMenu.Root, Trigger, Portal, Content, Item, Separator (optional)
// Built-in: Esc closes, click-outside closes, focus returns to trigger on close,
// arrow-key navigation between items, Enter/Space activates Item, role="menu"/"menuitem" ARIA.
```

Visual tokens (already defined in tailwind.config.ts):
- `bg-gm-paper` / `text-gm-ink` / `border-gm-line` — menu content surface (mirrors ToastProvider card styling)
- `bg-gm-second` / `text-gm-paper` — avatar trigger (unchanged from current)
- `hover:bg-gm-paperDeep` / `focus:bg-gm-paperDeep` — menu item hover/focus state
- `text-gm-accent` — destructive variant ink for sign-out item (optional, keeps it distinct)
- `rounded-gm-card` (if present, else `rounded-lg`) — content card radius
- `focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent` — trigger focus ring (match Btn.tsx)
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add Radix DropdownMenu, build UserMenu, wire into TopNav, add i18n keys</name>
  <files>
    web/package.json,
    web/i18n/en.json,
    web/i18n/ro.json,
    web/src/components/giftmaison/UserMenu.tsx,
    web/src/components/giftmaison/index.ts,
    web/src/components/giftmaison/TopNav.tsx
  </files>
  <behavior>
    - UserMenu renders a circular avatar button (gm-second bg, gm-paper fg, initials inside) — same dimensions/colors as today's static div (w-8 h-8 rounded-full text-[12px] font-medium).
    - The button uses `aria-label={t('auth.user_menu_label', { name })}` where `name = user.displayName ?? user.email ?? ''` — so screen readers announce "Account menu, Jane Doe" rather than just "A".
    - Clicking the button opens a Radix DropdownMenu.Content positioned below-right of the trigger (sideOffset=8, align="end"), rendered in a Portal.
    - Content shows a single DropdownMenu.Item labelled `t('auth.sign_out')`.
    - Selecting the item:
        1. closes the menu (Radix default),
        2. calls `await signOut()` from `features/auth/authProviders`,
        3. on rejection: `showToast(t('common.error_generic'), 'error')`.
      The component does NOT call `useNavigate` — `useAuth` reactively flips `user → null` and TopNav re-renders the "Sign in" CTA. (This matches how `AuthScreen.tsx` lets `onAuthStateChanged` drive UI rather than imperative routing for the signed-in branch — line 47–49 shows the inverse: bouncing already-signed-in users _away_ from /sign-in is fine, but for sign-out, the visible page may be /registry/:id or /, which the user is allowed to keep viewing in anonymous mode, so we deliberately do NOT navigate away.)
    - Keyboard: Enter/Space on trigger opens menu (Radix default); Esc closes; click-outside closes; focus returns to trigger on close (Radix default). All free from Radix — no custom focus management needed.
    - i18n keys (added to en.json and ro.json under the existing `auth` group):
        - `auth.user_menu_label` — en: "Account menu for {{name}}", ro: "Meniu cont pentru {{name}}"
        - `auth.sign_out` — en: "Sign out", ro: "Deconectează-te"
  </behavior>
  <action>
    1. Install Radix DropdownMenu (matching the version range of the other Radix packages — 1.x): from the `web/` directory run `npm install @radix-ui/react-dropdown-menu@^2.1.0`. Verify package.json now lists it under dependencies. (Radix DropdownMenu is currently at 2.x and is the correct sibling of `react-dialog@^1.1.15`/`react-toast@^1.2.0` already installed — Radix versions DropdownMenu independently; do NOT pin it to 1.x.)

    2. Add the two new i18n keys to BOTH `web/i18n/en.json` and `web/i18n/ro.json`, inside the existing `"auth": { … }` block (keep the trailing `"sign_in_link"` entry, append after it). Use the exact strings listed in <behavior>. Double-check JSON commas — append correctly, don't break the file.

    3. Create `web/src/components/giftmaison/UserMenu.tsx`:
       ```tsx
       import { useTranslation } from 'react-i18next'
       import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
       import type { User } from 'firebase/auth'
       import { signOut } from '../../features/auth/authProviders'
       import { useToast } from '../ToastProvider'

       export interface UserMenuProps {
         user: User
         initials: string
       }

       export function UserMenu({ user, initials }: UserMenuProps) {
         const { t } = useTranslation()
         const { showToast } = useToast()
         const displayName = user.displayName ?? user.email ?? ''

         const handleSignOut = async () => {
           try {
             await signOut()
           } catch {
             showToast(t('common.error_generic'), 'error')
           }
         }

         return (
           <DropdownMenu.Root>
             <DropdownMenu.Trigger
               aria-label={t('auth.user_menu_label', { name: displayName })}
               className="w-8 h-8 rounded-full bg-gm-second text-gm-paper flex items-center justify-center font-body text-[12px] font-medium cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
             >
               {initials}
             </DropdownMenu.Trigger>
             <DropdownMenu.Portal>
               <DropdownMenu.Content
                 align="end"
                 sideOffset={8}
                 className="min-w-[160px] bg-gm-paper border border-gm-line rounded-lg shadow-lg p-1 z-50"
               >
                 <DropdownMenu.Item
                   onSelect={handleSignOut}
                   className="font-body text-[13.5px] text-gm-ink px-3 py-2 rounded-md cursor-pointer outline-none data-[highlighted]:bg-gm-paperDeep data-[highlighted]:text-gm-ink"
                 >
                   {t('auth.sign_out')}
                 </DropdownMenu.Item>
               </DropdownMenu.Content>
             </DropdownMenu.Portal>
           </DropdownMenu.Root>
         )
       }

       export default UserMenu
       ```

    4. Export from the giftmaison barrel — add to `web/src/components/giftmaison/index.ts`:
       ```ts
       export { UserMenu, type UserMenuProps } from './UserMenu'
       ```

    5. Edit `web/src/components/giftmaison/TopNav.tsx`:
       - Add `import { UserMenu } from './UserMenu'` near the existing imports.
       - Replace the `<div aria-label=… className="w-8 h-8 rounded-full …">{initials || 'A'}</div>` block (currently lines 45–50) with:
         ```tsx
         <UserMenu user={user} initials={initials || 'A'} />
         ```
       - Leave the initials derivation, the `LanguageSwitcher`, and the `onSignInClick` branches untouched.

    6. Why NOT a custom dropdown / portal-less popover: We deliberately picked Radix DropdownMenu instead of hand-rolling a `useState`+`onClick`+`useEffect(click-outside)` dropdown because:
       - The project already depends on 5 other `@radix-ui/react-*` packages and ToastProvider uses Radix patterns (`@radix-ui/react-toast`) — adding DropdownMenu is on-pattern, not a new dependency family.
       - Radix gives Esc / click-outside / focus-return / arrow-key nav / `role="menu"` ARIA out of the box, all of which are constraints from this plan's <constraints>.
       - A custom dropdown would need ~80 lines (state, ref, useEffect listeners, key handling) vs. ~40 lines using Radix — and the custom version would lack arrow-key nav.

    7. Why we do NOT add `useNavigate` after sign-out: `useAuth` is a Firebase `onAuthStateChanged` subscriber — when `signOut()` resolves, the listener fires, `user → null`, TopNav re-renders with the "Sign in" CTA. The page itself (e.g., `/registry/:id` or `/`) remains accessible to anonymous users by design (web fallback is giver-facing). Navigating elsewhere would be a regression — a giver mid-flow who accidentally hits sign-out should NOT be punted off the registry page.
  </action>
  <verify>
    <automated>cd web && npx tsc --noEmit && npm run test:run -- src/__tests__/i18n.test.ts</automated>
  </verify>
  <done>
    - `web/package.json` lists `@radix-ui/react-dropdown-menu` under dependencies.
    - `web/i18n/en.json` contains `auth.user_menu_label` ("Account menu for {{name}}") and `auth.sign_out` ("Sign out"); `web/i18n/ro.json` contains the Romanian equivalents.
    - `web/src/components/giftmaison/UserMenu.tsx` exists, ~40+ lines, default-exports UserMenu, uses DropdownMenu primitives.
    - `web/src/components/giftmaison/index.ts` re-exports `UserMenu` and `UserMenuProps`.
    - `web/src/components/giftmaison/TopNav.tsx` imports `UserMenu` and renders `<UserMenu user={user} initials={initials || 'A'} />` instead of the static `<div>` avatar block.
    - `cd web && npx tsc --noEmit` exits 0.
    - i18n vitest still passes (sanity check — no JSON regression).
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Vitest coverage for UserMenu (open, sign-out success, sign-out failure)</name>
  <files>web/src/components/giftmaison/__tests__/UserMenu.test.tsx</files>
  <behavior>
    - Test 1: Trigger renders with the localized aria-label and the initials inside it. Menu items are NOT in the DOM before the trigger is clicked (Radix mounts Content via Portal lazily on open).
    - Test 2: Clicking the trigger opens the menu — a `menuitem` with text "Sign out" becomes visible.
    - Test 3: Clicking the Sign out menuitem calls the mocked `signOut()` exactly once.
    - Test 4: When `signOut()` rejects, `showToast` is called with the localized `common.error_generic` text and variant `'error'`.
  </behavior>
  <action>
    Create `web/src/components/giftmaison/__tests__/UserMenu.test.tsx`. Follow the existing test pattern from `web/src/features/auth/__tests__/AuthModal.test.tsx` for mocking `firebase` + `authProviders`.

    ```tsx
    import { beforeEach, describe, expect, it, vi } from 'vitest'
    import { render, screen, waitFor } from '@testing-library/react'
    import userEvent from '@testing-library/user-event'
    import '../../../../i18n'
    import type { User } from 'firebase/auth'

    const providerMocks = vi.hoisted(() => ({
      signOut: vi.fn(),
    }))
    vi.mock('../../../features/auth/authProviders', () => providerMocks)

    // Stub the firebase module so jsdom doesn't choke on a real config
    // (matches the pattern in AuthModal.test.tsx).
    vi.mock('../../../firebase', () => ({
      auth: { _kind: 'fakeAuth' },
      app: { _kind: 'fakeApp' },
      db: { _kind: 'fakeDb' },
      functions: { _kind: 'fakeFunctions' },
    }))

    const toastMocks = vi.hoisted(() => ({ showToast: vi.fn() }))
    vi.mock('../../ToastProvider', () => ({
      useToast: () => toastMocks,
      ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    }))

    import { UserMenu } from '../UserMenu'

    const fakeUser = {
      uid: 'u1',
      email: 'jane@example.com',
      displayName: 'Jane Doe',
    } as unknown as User

    describe('UserMenu', () => {
      beforeEach(() => {
        providerMocks.signOut.mockReset()
        providerMocks.signOut.mockResolvedValue(undefined)
        toastMocks.showToast.mockReset()
      })

      it('renders avatar trigger with localized aria-label and initials', () => {
        render(<UserMenu user={fakeUser} initials="JD" />)
        const trigger = screen.getByRole('button', { name: /account menu for jane doe/i })
        expect(trigger).toHaveTextContent('JD')
        // Menu content is portal-mounted lazily — Sign out item should not be in DOM yet.
        expect(screen.queryByRole('menuitem', { name: /sign out/i })).not.toBeInTheDocument()
      })

      it('opens menu on trigger click and reveals Sign out item', async () => {
        const user = userEvent.setup()
        render(<UserMenu user={fakeUser} initials="JD" />)
        await user.click(screen.getByRole('button', { name: /account menu for jane doe/i }))
        expect(await screen.findByRole('menuitem', { name: /sign out/i })).toBeInTheDocument()
      })

      it('calls signOut() when Sign out menuitem is selected', async () => {
        const user = userEvent.setup()
        render(<UserMenu user={fakeUser} initials="JD" />)
        await user.click(screen.getByRole('button', { name: /account menu for jane doe/i }))
        await user.click(await screen.findByRole('menuitem', { name: /sign out/i }))
        await waitFor(() => expect(providerMocks.signOut).toHaveBeenCalledTimes(1))
        expect(toastMocks.showToast).not.toHaveBeenCalled()
      })

      it('shows an error toast when signOut() rejects', async () => {
        providerMocks.signOut.mockRejectedValueOnce(new Error('network'))
        const user = userEvent.setup()
        render(<UserMenu user={fakeUser} initials="JD" />)
        await user.click(screen.getByRole('button', { name: /account menu for jane doe/i }))
        await user.click(await screen.findByRole('menuitem', { name: /sign out/i }))
        await waitFor(() =>
          expect(toastMocks.showToast).toHaveBeenCalledWith(
            expect.stringMatching(/something went wrong/i),
            'error',
          ),
        )
      })
    })
    ```

    Notes for the executor:
    - The `vi.hoisted` pattern is the same one used in `AuthModal.test.tsx` (line 6) — it ensures the mock object exists at hoist time so `vi.mock` can return it.
    - Path depth: from `web/src/components/giftmaison/__tests__/` the i18n module is at `../../../../i18n` (four `..` — verify with the file you created), `features/auth/authProviders` is at `../../../features/auth/authProviders`, and `firebase` is at `../../../firebase`. If a path resolves wrong, look at how AuthModal.test.tsx imports (it lives one level shallower, so its paths have one fewer `..`).
    - If `userEvent.click` on the menuitem fails because Radix uses `pointer` events under jsdom, fall back to `await user.keyboard('{Enter}')` after focusing the item via Tab — but try the click path first; Radix DropdownMenu has documented jsdom-compatible userEvent support.
  </action>
  <verify>
    <automated>cd web && npm run test:run -- src/components/giftmaison/__tests__/UserMenu.test.tsx</automated>
  </verify>
  <done>
    - Test file exists, 4 specs, all pass under `npm run test:run`.
    - `cd web && npx tsc --noEmit` still exits 0 (no type regressions from the new file).
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Human verification — open menu and sign out in the running web app</name>
  <files>web/src/components/giftmaison/UserMenu.tsx, web/src/components/giftmaison/TopNav.tsx</files>
  <action>
    Pause for human verification. After Tasks 1 and 2 are merged and tests are green, hand the running web app to the user. What was built (recap for the user):
    - User avatar in the web TopNav is now an interactive Radix DropdownMenu trigger.
    - Clicking it opens a dropdown with "Sign out".
    - Selecting Sign out calls Firebase Auth `signOut()`, the menu closes, and the TopNav re-renders the "Sign in" CTA (because `useAuth` reactively reports `user → null`).
    - i18n keys exist in en + ro and the EN/RO language toggle in the TopNav swaps the menu strings live.

    Verification steps for the user:
    1. Start the dev stack from `web/`:
       ```
       cd web && npm run dev
       ```
       (and ensure Firebase emulators are running if you normally use them — `firebase emulators:start` from the repo root. Auth emulator is required for sign-in/out.)
    2. Open the Vite-reported URL (typically http://localhost:5173), then navigate to `/sign-in` and sign in with any test account (email/password or Google — the Google popup works against the Auth emulator).
    3. After sign-in, you're bounced back to `/`. Confirm the olive circle avatar in the top-right shows your initials.
    4. Click the avatar:
       - Expected: a small dropdown card appears below-right of the avatar with one item, "Sign out".
       - Expected: Tab/Shift-Tab cycles focus into and out of the menu; arrow-down highlights the Sign out item.
    5. Press Escape:
       - Expected: menu closes, focus returns to the avatar trigger.
    6. Open the menu again, click outside the card:
       - Expected: menu closes (click-outside dismissal).
    7. Open the menu and click "Sign out":
       - Expected: menu closes, the avatar disappears, and the "Sign in" ghost button replaces it in the TopNav within ~1 s.
       - Expected: no error toast appears.
    8. Toggle EN/RO via the LanguageSwitcher in the TopNav while signed in:
       - Expected (EN): menu item reads "Sign out"; avatar aria-label reads "Account menu for {your name}".
       - Expected (RO): menu item reads "Deconectează-te"; avatar aria-label reads "Meniu cont pentru {your name}" (inspect via DevTools accessibility tab).
    9. Sign in again, then optionally simulate a sign-out failure to verify the error toast:
       - In DevTools, throttle network to Offline, click the avatar, click Sign out.
       - Expected: the error toast ("Something went wrong. Please try again." / "Ceva a mers prost. Încearcă din nou.") appears bottom-center, and the user remains signed in. (Note: Firebase Auth's signOut is largely local; if it does not reject under offline mode, you can skip this step — the unit test covers the rejection path.)
    10. Run the full test suite once more from `web/`:
        ```
        npm run test:run
        ```
        — confirm green.

    Resume signal: the user types "approved" if all checks pass; otherwise they describe the issue (which step, observed vs. expected) so it can be patched.
  </action>
  <verify>
    Human confirms steps 1–10 above pass. No automated command — this is the explicit human checkpoint.
    <automated>cd web && npm run test:run</automated>
  </verify>
  <done>
    - User has typed "approved" (or equivalent) after running the verification steps.
    - Full vitest suite still green.
    - No regressions reported in EN/RO localization parity or in keyboard a11y.
  </done>
</task>

</tasks>

<verification>
After Tasks 1 and 2 complete (before checkpoint):

- `cd web && npx tsc --noEmit` exits 0
- `cd web && npm run test:run` exits 0 (existing suite remains green and the new UserMenu suite passes)
- `web/package.json` diff shows exactly one added dependency: `@radix-ui/react-dropdown-menu`
- `web/i18n/en.json` and `web/i18n/ro.json` are still valid JSON (no trailing-comma corruption) and both contain the two new `auth.*` keys

After Task 3 (human verification) returns "approved":

- Manual sign-out flow works end-to-end against the Auth emulator
- Localization parity confirmed in both EN and RO
- Keyboard a11y (Esc, Tab, click-outside, focus return) confirmed by a human
</verification>

<success_criteria>
- A signed-in user can open the TopNav user menu by clicking their avatar (mouse OR keyboard) and choose "Sign out".
- Selecting Sign out calls `signOut()` from `features/auth/authProviders.ts`, which calls Firebase Auth `signOut(auth)`. The `useAuth` listener reactively reports `user → null` and TopNav re-renders the "Sign in" CTA — without any imperative routing.
- The menu is keyboard accessible: Esc closes, click-outside closes, Tab/Shift-Tab focus management works, focus returns to the trigger on close.
- Every visible string (trigger aria-label, menu item, error toast) is sourced from i18next; both `en.json` and `ro.json` carry the new keys.
- No new UI library introduced beyond `@radix-ui/react-dropdown-menu`, which is in the same Radix family as the 5 packages already in use.
- Automated tests cover open / sign-out success / sign-out failure paths.
</success_criteria>

<output>
After completion, create `.planning/quick/260512-vlg-add-user-menu-with-sign-out-to-web-heade/260512-vlg-SUMMARY.md` capturing:
- Files changed (with brief role of each)
- The Radix version installed
- Confirmation that no `useNavigate` was added on the sign-out path and why (reactive `useAuth` → null already drives TopNav re-render)
- Any UAT notes returned by the human verifier
</output>
