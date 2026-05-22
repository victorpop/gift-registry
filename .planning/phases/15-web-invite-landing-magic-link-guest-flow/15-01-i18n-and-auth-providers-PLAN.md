---
phase: 15-web-invite-landing-magic-link-guest-flow
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/src/i18n/en.json
  - web/src/i18n/ro.json
  - web/src/features/auth/authProviders.ts
autonomous: true
requirements: []
context_decisions:
  - "Localization (i18n keys for invite_landing namespace)"
  - "Guest path — magic-link instead of pure anonymous"
  - "Modal — visual & interaction (key copy aligns with SaveYourSpotModal pattern)"
gap_closure: false

must_haves:
  truths:
    - "web/src/i18n/en.json contains the invite_landing namespace with at least 10 keys"
    - "web/src/i18n/ro.json contains the invite_landing namespace with the same key set as en.json"
    - "authProviders.ts exports sendInviteSignInLink(email, continueUrl) and completeInviteSignIn(href) helpers"
    - "Existing authProviders exports (signInEmail, signUpEmail, signInWithGoogle, signOut, getRedirectResult) are not removed or renamed"
  artifacts:
    - path: "web/src/i18n/en.json"
      provides: "invite_landing.* English copy"
      contains: "\"invite_landing\""
    - path: "web/src/i18n/ro.json"
      provides: "invite_landing.* Romanian copy"
      contains: "\"invite_landing\""
    - path: "web/src/features/auth/authProviders.ts"
      provides: "Magic-link send + complete wrappers, alongside existing email/google providers"
      exports: ["sendInviteSignInLink", "completeInviteSignIn", "signInEmail", "signUpEmail", "signInWithGoogle", "signOut", "getRedirectResult"]
  key_links:
    - from: "web/src/features/auth/authProviders.ts"
      to: "firebase/auth"
      via: "import { sendSignInLinkToEmail, signInWithEmailLink, isSignInWithEmailLink } from 'firebase/auth'"
      pattern: "from 'firebase/auth'"
---

<objective>
Lay the foundation that the rest of Phase 15 (modal, callback page, Cloud Function, RegistryPage wiring) consumes:
1. Add the `invite_landing.*` i18n namespace to both `en.json` and `ro.json` so the new modal + callback page can render localized strings (per `CLAUDE.md`: no hardcoded strings).
2. Extend `web/src/features/auth/authProviders.ts` with two pure-additive wrappers — `sendInviteSignInLink` and `completeInviteSignIn` — around Firebase JS SDK 11's `sendSignInLinkToEmail` / `signInWithEmailLink`. No existing exports change.

Purpose: This plan is a Wave 1 root (no dependencies) so it can run in parallel with Plan 15-02 (backend URL builder + email template). Plans 15-03 (modal), 15-04 (callback page + Cloud Function), and 15-05 (RegistryPage wiring) all import either i18n keys or the new authProviders helpers — so this MUST land first.

Output: i18n keys + 2 new auth helper exports — no UI, no rendered components, no backend.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/ROADMAP.md
@.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md
@CLAUDE.md

<!-- Canonical references — executor MUST read these first to mirror conventions -->
@web/src/features/auth/authProviders.ts
@web/src/features/auth/SaveYourSpotModal.tsx
@web/src/i18n/en.json
@web/src/i18n/ro.json

<interfaces>
<!-- Key types and signatures the executor needs. Use these directly. -->

From web/src/features/auth/authProviders.ts (EXISTING — DO NOT MODIFY):
```typescript
export async function signInEmail(email: string, password: string): Promise<User>;
export async function signUpEmail(email: string, password: string): Promise<User>;
export async function signInWithGoogle(): Promise<void>;
export { getRedirectResult };
export async function signOut(): Promise<void>;
```

From web/src/firebase.ts:
```typescript
export const auth: Auth;  // initialized firebase Auth instance to pass to all helpers
```

From firebase/auth (v11.x — see web/package.json "firebase": "^12.12.0" → SDK exposes these):
```typescript
function sendSignInLinkToEmail(auth: Auth, email: string, actionCodeSettings: ActionCodeSettings): Promise<void>;
function signInWithEmailLink(auth: Auth, email: string, emailLink?: string): Promise<UserCredential>;
function isSignInWithEmailLink(auth: Auth, emailLink: string): boolean;

interface ActionCodeSettings {
  url: string;  // continueUrl — where the user lands after clicking the email link
  handleCodeInApp: boolean;  // MUST be true for email-link sign-in
  // iOS/android keys not relevant for web fallback
}

interface UserCredential {
  user: User;
  // operationType, providerId
}
```

Existing i18n key shape (web/src/i18n/en.json) — `save_your_spot.*` block at lines 33-55 is the canonical template to mirror.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="false">
  <name>Task 1: Add invite_landing namespace to en.json and ro.json</name>
  <files>web/src/i18n/en.json, web/src/i18n/ro.json</files>
  <read_first>
    - web/src/i18n/en.json (READ FIRST — see existing namespace ordering and the `save_your_spot` block at lines 33-55 which is the visual + key-shape template)
    - web/src/i18n/ro.json (READ FIRST — confirm `save_your_spot` block exists at line 33 with matching key set in Romanian)
    - .planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md (READ — the `<specifics>` section lists the 8 suggested keys verbatim)
  </read_first>
  <behavior>
    - en.json gains a new top-level key `"invite_landing"` with exactly these 11 sub-keys (and no others) ordered alphabetically OR following the natural flow (title → body → ctas → check-email → errors → field labels)
    - ro.json gains the same `"invite_landing"` top-level key with exactly the same 11 sub-keys (Romanian translations)
    - Both files remain valid JSON (no trailing commas, balanced braces)
    - No other namespaces are modified
  </behavior>
  <action>
    Add a new top-level `"invite_landing"` namespace to BOTH `web/src/i18n/en.json` and `web/src/i18n/ro.json`. Insert it AFTER the existing `"save_your_spot"` block (currently at line 33-55) so namespace ordering stays grouped (auth-adjacent).

    The 11 keys (with EXACT English copy — copy these strings verbatim into en.json):

    ```json
    "invite_landing": {
      "title": "You're invited",
      "body": "Someone shared a gift registry with you. Sign in to reserve gifts, or continue as a guest with a one-time email link.",
      "primary_cta": "Create an account",
      "secondary_cta": "Continue as guest",
      "dismiss_cta": "Not now",
      "first_name_label": "First name",
      "last_name_label": "Last name",
      "email_label": "Email",
      "password_label": "Password",
      "password_placeholder": "At least 8 characters",
      "check_email_title": "Check your email",
      "check_email_body": "We sent a sign-in link to {{email}}. Click it from this device to open the registry.",
      "check_email_resend": "Send another link",
      "error_invalid_email": "Enter a valid email address.",
      "error_send_failed": "Couldn't send the sign-in link. Try again.",
      "error_link_expired": "This sign-in link has expired. Request a new one.",
      "error_email_mismatch": "This link was sent to a different email address.",
      "error_generic": "Something went wrong. Try again."
    }
    ```

    NOTE: That's actually 18 keys — keep all 18. Above the 11-key minimum from must_haves so we have full coverage of the two-state UI (initial + check-email) plus all error states the callback page needs.

    Translate to Romanian for ro.json (use these EXACT translations — they match the existing `save_your_spot.*` tone in ro.json):

    ```json
    "invite_landing": {
      "title": "Ești invitat",
      "body": "Cineva ți-a împărtășit o listă de cadouri. Conectează-te ca să rezervi cadouri sau continuă ca invitat cu un link unic primit pe email.",
      "primary_cta": "Creează un cont",
      "secondary_cta": "Continuă ca invitat",
      "dismiss_cta": "Mai târziu",
      "first_name_label": "Prenume",
      "last_name_label": "Nume",
      "email_label": "Email",
      "password_label": "Parolă",
      "password_placeholder": "Cel puțin 8 caractere",
      "check_email_title": "Verifică-ți emailul",
      "check_email_body": "Ți-am trimis un link de conectare la {{email}}. Apasă-l de pe acest dispozitiv ca să deschizi lista.",
      "check_email_resend": "Trimite alt link",
      "error_invalid_email": "Introdu o adresă de email validă.",
      "error_send_failed": "Nu am putut trimite linkul. Încearcă din nou.",
      "error_link_expired": "Acest link a expirat. Cere unul nou.",
      "error_email_mismatch": "Acest link a fost trimis la altă adresă de email.",
      "error_generic": "Ceva nu a mers bine. Încearcă din nou."
    }
    ```

    Validate both files parse as JSON after editing.
  </action>
  <verify>
    <automated>node -e "const en=require('./web/src/i18n/en.json'); const ro=require('./web/src/i18n/ro.json'); const enKeys=Object.keys(en.invite_landing||{}); const roKeys=Object.keys(ro.invite_landing||{}); if(enKeys.length<11)throw new Error('en missing keys: '+enKeys.length); if(roKeys.length!==enKeys.length)throw new Error('key count mismatch en='+enKeys.length+' ro='+roKeys.length); if(JSON.stringify(enKeys.sort())!==JSON.stringify(roKeys.sort()))throw new Error('key sets differ'); console.log('OK invite_landing keys: '+enKeys.length);"</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c "\"invite_landing\"" web/src/i18n/en.json` returns at least 1
    - `grep -c "\"invite_landing\"" web/src/i18n/ro.json` returns at least 1
    - `node -e "JSON.parse(require('fs').readFileSync('web/src/i18n/en.json','utf8'))"` exits 0
    - `node -e "JSON.parse(require('fs').readFileSync('web/src/i18n/ro.json','utf8'))"` exits 0
    - `node -e "const en=require('./web/src/i18n/en.json'); console.log(Object.keys(en.invite_landing).length)"` prints a number ≥ 11
    - `node -e "const en=require('./web/src/i18n/en.json'); const ro=require('./web/src/i18n/ro.json'); console.log(JSON.stringify(Object.keys(en.invite_landing).sort())===JSON.stringify(Object.keys(ro.invite_landing).sort()))"` prints `true`
    - en.json contains the literal strings `"You're invited"`, `"Create an account"`, `"Continue as guest"`, `"Check your email"`
    - ro.json contains the literal strings `"Ești invitat"`, `"Creează un cont"`, `"Continuă ca invitat"`, `"Verifică-ți emailul"`
    - No other top-level namespaces are modified (`save_your_spot`, `auth`, `web_auth`, etc. unchanged) — confirm by reading the file diff
  </acceptance_criteria>
  <done>Both i18n files contain the `invite_landing` namespace with ≥11 keys, identical key sets across locales, JSON-valid, and no other namespace has been touched.</done>
</task>

<task type="auto" tdd="false">
  <name>Task 2: Add sendInviteSignInLink + completeInviteSignIn helpers to authProviders.ts</name>
  <files>web/src/features/auth/authProviders.ts</files>
  <read_first>
    - web/src/features/auth/authProviders.ts (READ FIRST — current file is 45 lines; you will ONLY add new exports below the existing ones; do NOT modify or remove existing exports)
    - web/src/firebase.ts (READ — confirms `auth` export shape; the new helpers use the same `auth` import the existing helpers do)
    - .planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md (READ — `<decisions>` "Guest path — magic-link instead of pure anonymous" + `<specifics>` "Magic-link continueUrl" recommend `actionCodeSettings.url` containing `?next=/registry/{registryId}`)
  </read_first>
  <behavior>
    - `sendInviteSignInLink(email: string, continueUrl: string): Promise<void>` calls Firebase `sendSignInLinkToEmail(auth, email, { url: continueUrl, handleCodeInApp: true })` and stores the email in localStorage under a known key for the callback to retrieve
    - `completeInviteSignIn(href: string): Promise<User>` checks `isSignInWithEmailLink(auth, href)`, retrieves the stored email from localStorage (or accepts it as a fallback parameter), calls `signInWithEmailLink(auth, email, href)`, clears the localStorage entry on success, returns `userCredential.user`
    - Both helpers throw the underlying Firebase error on failure (caller handles)
    - Re-export `isSignInWithEmailLink` so the callback page can guard the route before calling complete
    - Existing exports (`signInEmail`, `signUpEmail`, `signInWithGoogle`, `getRedirectResult`, `signOut`) are NOT touched — grep before/after to verify
  </behavior>
  <action>
    Open `web/src/features/auth/authProviders.ts` and APPEND (do not edit existing code) the following:

    1. Extend the import from `firebase/auth` at the top to also import: `sendSignInLinkToEmail`, `signInWithEmailLink`, `isSignInWithEmailLink`. Keep the existing imports intact.

    2. Add a module-level constant for the localStorage key:
    ```typescript
    const INVITE_SIGNIN_EMAIL_STORAGE_KEY = 'inviteSignInEmail'
    ```

    3. Append these three exports at the end of the file (after `signOut`):

    ```typescript
    /**
     * Sends a Firebase passwordless sign-in link to the given email.
     *
     * `continueUrl` is the URL the email-link button points to. Convention for
     * Phase 15: `${origin}/auth/email-link?next=${encodeURIComponent(registryPath)}`
     * — the callback page reads `?next=` to know where to navigate after auth.
     *
     * Stores the email in localStorage so completeInviteSignIn() can retrieve it
     * on the callback (Firebase requires the same email to be passed to
     * signInWithEmailLink for verification).
     *
     * authorizedDomains note: the host portion of `continueUrl` MUST be added
     * to Firebase Console → Authentication → Settings → Authorized domains.
     * gift-registry-ro.web.app is already in the list per Phase 14; localhost
     * is auto-included for emulator dev.
     */
    export async function sendInviteSignInLink(email: string, continueUrl: string): Promise<void> {
      await sendSignInLinkToEmail(auth, email, {
        url: continueUrl,
        handleCodeInApp: true,
      })
      try {
        localStorage.setItem(INVITE_SIGNIN_EMAIL_STORAGE_KEY, email)
      } catch {
        // localStorage unavailable (private mode, SSR) — caller can pass email explicitly to completeInviteSignIn
      }
    }

    /**
     * Completes a passwordless email-link sign-in on the callback page.
     *
     * `href` is window.location.href on the callback page. Returns the
     * authenticated User. Clears the stored email on success.
     *
     * Throws if href is not a valid sign-in link OR if no email was stored
     * AND no fallback email is supplied (rare — happens if user clicks the
     * link from a different browser than the one that sent it).
     */
    export async function completeInviteSignIn(href: string, fallbackEmail?: string): Promise<User> {
      if (!isSignInWithEmailLink(auth, href)) {
        throw new Error('not-a-sign-in-link')
      }
      let email: string | null = null
      try {
        email = localStorage.getItem(INVITE_SIGNIN_EMAIL_STORAGE_KEY)
      } catch {
        email = null
      }
      if (!email) email = fallbackEmail ?? null
      if (!email) {
        throw new Error('missing-email-for-sign-in-link')
      }
      const result = await signInWithEmailLink(auth, email, href)
      try {
        localStorage.removeItem(INVITE_SIGNIN_EMAIL_STORAGE_KEY)
      } catch {
        // best-effort cleanup
      }
      return result.user
    }

    /** Re-export so the callback route can guard before calling completeInviteSignIn. */
    export { isSignInWithEmailLink }

    export const INVITE_SIGNIN_EMAIL_STORAGE_KEY_EXPORT = INVITE_SIGNIN_EMAIL_STORAGE_KEY
    ```

    The last line is for the test to inspect the storage key without exporting the mutable binding (use a renamed re-export to avoid colliding with the internal const). Tests that need the key import `INVITE_SIGNIN_EMAIL_STORAGE_KEY_EXPORT`.

    Do NOT touch the existing `signInEmail`, `signUpEmail`, `signInWithGoogle`, `getRedirectResult`, or `signOut` functions. Do NOT change the existing import block beyond adding the three new named imports.
  </action>
  <verify>
    <automated>cd web && npx tsc --noEmit -p tsconfig.json 2>&1 | grep -E "authProviders|invite" || echo "TYPECHECK OK"</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c "export async function sendInviteSignInLink" web/src/features/auth/authProviders.ts` returns 1
    - `grep -c "export async function completeInviteSignIn" web/src/features/auth/authProviders.ts` returns 1
    - `grep -c "export { isSignInWithEmailLink }" web/src/features/auth/authProviders.ts` returns 1
    - `grep -c "sendSignInLinkToEmail" web/src/features/auth/authProviders.ts` returns at least 2 (one import, one call)
    - `grep -c "signInWithEmailLink" web/src/features/auth/authProviders.ts` returns at least 2 (one import, one call)
    - `grep -c "isSignInWithEmailLink" web/src/features/auth/authProviders.ts` returns at least 3 (one import, one re-export, one call inside completeInviteSignIn)
    - `grep -c "handleCodeInApp: true" web/src/features/auth/authProviders.ts` returns 1
    - `grep -c "export async function signInEmail" web/src/features/auth/authProviders.ts` returns 1 (existing — must remain)
    - `grep -c "export async function signUpEmail" web/src/features/auth/authProviders.ts` returns 1 (existing — must remain)
    - `grep -c "export async function signInWithGoogle" web/src/features/auth/authProviders.ts` returns 1 (existing — must remain)
    - `grep -c "export { getRedirectResult }" web/src/features/auth/authProviders.ts` returns 1 (existing — must remain)
    - `grep -c "export async function signOut" web/src/features/auth/authProviders.ts` returns 1 (existing — must remain)
    - `cd web && npx tsc --noEmit -p tsconfig.json 2>&1 | grep -E "authProviders\.ts" | wc -l` returns 0 (no type errors in the modified file)
  </acceptance_criteria>
  <done>authProviders.ts exports `sendInviteSignInLink`, `completeInviteSignIn`, and re-exports `isSignInWithEmailLink` alongside the unchanged 5 original exports; typechecks clean.</done>
</task>

</tasks>

<verification>
- Both i18n files contain the `invite_landing` namespace with ≥11 keys (verified by `node -e ...` snippet from Task 1 verify)
- Both files parse as valid JSON
- en.json key set === ro.json key set for the invite_landing namespace
- authProviders.ts exports 3 new symbols (sendInviteSignInLink, completeInviteSignIn, isSignInWithEmailLink re-export) and retains all 5 prior exports
- `cd web && npx tsc --noEmit -p tsconfig.json` exits 0 (no new type errors)
- `cd web && npx vitest run --reporter=dot` exits 0 (no existing tests regress — this plan adds zero new test files; that's intentional since the helpers will get coverage through Plan 15-03's modal tests and Plan 15-04's callback page tests)
</verification>

<success_criteria>
This plan is complete when:
1. The `invite_landing` namespace is present in both `en.json` and `ro.json` with matching key sets
2. `authProviders.ts` has 3 new exports for magic-link send + complete + the isSignInWithEmailLink re-export
3. No existing exports or i18n namespaces are modified
4. TypeScript compilation passes
5. Existing Vitest suite passes unchanged
</success_criteria>

<output>
After completion, create `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-01-i18n-and-auth-providers-SUMMARY.md` summarizing:
- Number of i18n keys added per locale
- Three new authProvider exports + their signatures
- Confirmation that no existing exports/namespaces were modified
- The localStorage key constant (`inviteSignInEmail`) so downstream plans know how to mock it in tests
</output>
