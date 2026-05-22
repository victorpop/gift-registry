---
phase: 15-web-invite-landing-magic-link-guest-flow
plan: 02
type: execute
wave: 1
depends_on: []
files_modified:
  - functions/src/config/publicUrls.ts
  - functions/src/email/templates/invite.ts
  - functions/src/registry/inviteToRegistry.ts
  - functions/src/__tests__/publicUrls.test.ts
autonomous: true
requirements: []
context_decisions:
  - "Invite signal — how the page knows came from an email invite (?invite=1 query param)"
  - "URL shape (gift-registry-ro.web.app/registry/{id}?invite=1)"
gap_closure: false

must_haves:
  truths:
    - "buildRegistryUrl accepts an optional second argument and produces a URL containing '?invite=1' when invite is requested"
    - "buildRegistryUrl signature change is backwards compatible — all existing callers continue to produce URLs without ?invite=1"
    - "The invite email template's CTA link now contains the ?invite=1 query param so recipients land on the invite-landing modal"
    - "publicUrls.test.ts covers the new invite-flavored URL output"
  artifacts:
    - path: "functions/src/config/publicUrls.ts"
      provides: "buildRegistryUrl with optional invite flag"
      contains: "invite"
    - path: "functions/src/email/templates/invite.ts"
      provides: "Invite email template that consumes the invite-flavored URL"
      contains: "?invite=1"
    - path: "functions/src/registry/inviteToRegistry.ts"
      provides: "Invite send callable that passes invite: true when building the URL"
      contains: "invite: true"
    - path: "functions/src/__tests__/publicUrls.test.ts"
      provides: "Test coverage for invite-flavored URL"
      contains: "?invite=1"
  key_links:
    - from: "functions/src/registry/inviteToRegistry.ts"
      to: "functions/src/config/publicUrls.ts"
      via: "buildRegistryUrl(registryId, { invite: true })"
      pattern: "buildRegistryUrl\\(.*invite"
    - from: "functions/src/email/templates/invite.ts"
      to: "?invite=1 query param"
      via: "registryUrl parameter (passed in from inviteToRegistry.ts)"
      pattern: "registryUrl"
---

<objective>
Extend the backend invite-send pipeline so emailed registry links carry the `?invite=1` query param. This is the signal that lets the web fallback's `RegistryPage` (Plan 15-05) distinguish "user clicked the invite email" from "user pasted/shared the link," and therefore decide whether to surface the invite-landing modal.

Three precise changes:
1. `functions/src/config/publicUrls.ts` — extend `buildRegistryUrl(registryId)` to `buildRegistryUrl(registryId, opts?: { invite?: boolean })`. When `opts.invite === true`, append `?invite=1`. Backwards compatible (current call sites in `onPurchaseNotification.ts` continue working unchanged).
2. `functions/src/registry/inviteToRegistry.ts` — at line 95 (`const registryUrl = buildRegistryUrl(registryId);`), update the call to `buildRegistryUrl(registryId, { invite: true })`. Nothing else in this file changes.
3. `functions/src/__tests__/publicUrls.test.ts` — add tests covering the new opts behavior (invite=true appends `?invite=1`, omitted opts produces the existing URL).

The invite email template (`functions/src/email/templates/invite.ts`) already takes `registryUrl` as a vars input — no template change needed; the URL it renders just happens to now contain `?invite=1`.

Purpose: Wave 1 root, parallel-safe with Plan 15-01. Plans 15-03/04/05 do not directly depend on this backend change for their own implementation, but the end-to-end UAT in Phase 15 requires the email link to carry the `?invite=1` signal so an invitee actually reaches the invite-landing modal.

Output: An invite email sent from `inviteToRegistry` now contains a CTA URL like `https://gift-registry-ro.web.app/registry/{id}?invite=1`.
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

<!-- Canonical references — executor MUST read these first -->
@functions/src/config/publicUrls.ts
@functions/src/registry/inviteToRegistry.ts
@functions/src/email/templates/invite.ts
@functions/src/__tests__/publicUrls.test.ts
@functions/src/notifications/onPurchaseNotification.ts

<interfaces>
<!-- Existing signatures the executor must preserve backwards-compatibility against -->

From functions/src/config/publicUrls.ts (CURRENT):
```typescript
export function publicWebBaseUrl(): string;
export function buildRegistryUrl(registryId: string): string;  // ← extend this
export function buildReReserveUrl(reservationId: string): string;
```

From functions/src/notifications/onPurchaseNotification.ts (line 116) — EXISTING CALLER that MUST continue working unchanged:
```typescript
const registryUrl = buildRegistryUrl(registryId);  // no opts — returns ${base}/registry/{id} (no ?invite=1)
```

From functions/src/registry/inviteToRegistry.ts (line 95) — THE CALLER TO UPDATE:
```typescript
const registryUrl = buildRegistryUrl(registryId);  // ← becomes buildRegistryUrl(registryId, { invite: true })
```

From functions/src/email/templates/invite.ts (NO CHANGE — just renders whatever URL it receives):
```typescript
export interface InviteVars {
  ownerName: string;
  registryName: string;
  registryUrl: string;  // ← will now arrive containing ?invite=1
}
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Extend buildRegistryUrl with invite opt + update tests</name>
  <files>functions/src/config/publicUrls.ts, functions/src/__tests__/publicUrls.test.ts</files>
  <read_first>
    - functions/src/config/publicUrls.ts (READ FIRST — file is 33 lines; you are adding an opts arg to one function and keeping all existing exports intact)
    - functions/src/__tests__/publicUrls.test.ts (READ FIRST — existing test pattern, 39 lines, mirror its style for the new test cases)
    - functions/src/notifications/onPurchaseNotification.ts (READ — see line 116 to confirm an existing caller passes no opts and must keep working as-is)
  </read_first>
  <behavior>
    - Test 1 (RED first): `buildRegistryUrl('abc')` (no opts) returns `'https://gift-registry-ro.web.app/registry/abc'` — exact equality (existing test; should already pass after the function signature change because the opts arg is optional)
    - Test 2 (RED first): `buildRegistryUrl('abc', { invite: true })` returns `'https://gift-registry-ro.web.app/registry/abc?invite=1'` — exact equality
    - Test 3 (RED first): `buildRegistryUrl('abc', { invite: false })` returns `'https://gift-registry-ro.web.app/registry/abc'` (no query string — opts present but invite false)
    - Test 4 (RED first): `buildRegistryUrl('abc', {})` returns `'https://gift-registry-ro.web.app/registry/abc'` (empty opts object)
    - GREEN: extend the function signature to accept opts and append `?invite=1` when `opts?.invite === true`
  </behavior>
  <action>
    Step 1 — RED. Edit `functions/src/__tests__/publicUrls.test.ts` and APPEND a new describe block after the existing `buildReReserveUrl` block (after line 38):

    ```typescript
    describe("buildRegistryUrl with invite opt", () => {
      it("returns the plain URL when no opts are provided (backwards compatible)", () => {
        expect(buildRegistryUrl("abc")).toBe("https://gift-registry-ro.web.app/registry/abc");
      });

      it("appends ?invite=1 when opts.invite is true", () => {
        expect(buildRegistryUrl("abc", { invite: true })).toBe(
          "https://gift-registry-ro.web.app/registry/abc?invite=1"
        );
      });

      it("returns the plain URL when opts.invite is false", () => {
        expect(buildRegistryUrl("abc", { invite: false })).toBe(
          "https://gift-registry-ro.web.app/registry/abc"
        );
      });

      it("returns the plain URL when opts is an empty object", () => {
        expect(buildRegistryUrl("abc", {})).toBe("https://gift-registry-ro.web.app/registry/abc");
      });
    });
    ```

    Run `cd functions && npm test -- publicUrls` to confirm the new tests FAIL (function doesn't accept opts yet — TS error counts as RED).

    Step 2 — GREEN. Edit `functions/src/config/publicUrls.ts`. Replace lines 24-27 (the `buildRegistryUrl` function) with:

    ```typescript
    /**
     * Options for buildRegistryUrl.
     */
    export interface BuildRegistryUrlOpts {
      /** When true, appends `?invite=1` so the web fallback shows the invite-landing modal. */
      invite?: boolean;
    }

    /**
     * Builds the public URL for a registry page, e.g. `${base}/registry/{id}`.
     * Pass `{ invite: true }` to append the `?invite=1` query param that signals
     * "this link came from an invite email" to the web fallback (Plan 15-05
     * RegistryPage gates the invite-landing modal on this param).
     */
    export function buildRegistryUrl(registryId: string, opts?: BuildRegistryUrlOpts): string {
      const base = `${publicWebBaseUrl()}/registry/${registryId}`;
      return opts?.invite === true ? `${base}?invite=1` : base;
    }
    ```

    Keep `publicWebBaseUrl` and `buildReReserveUrl` unchanged.

    Run `cd functions && npm test -- publicUrls` and confirm ALL tests pass (4 existing + 4 new = 8 tests in the file).

    Step 3 — verify backwards compat. Run `cd functions && npx tsc --noEmit` and confirm zero new type errors (the existing caller in `onPurchaseNotification.ts` line 116 passes no opts, which is valid against the new optional signature).
  </action>
  <verify>
    <automated>cd functions && npm test -- publicUrls 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c "export interface BuildRegistryUrlOpts" functions/src/config/publicUrls.ts` returns 1
    - `grep -c "opts?: BuildRegistryUrlOpts" functions/src/config/publicUrls.ts` returns 1
    - `grep -c "opts?.invite === true" functions/src/config/publicUrls.ts` returns 1
    - `grep -c "?invite=1" functions/src/config/publicUrls.ts` returns at least 1
    - `grep -c "describe(\"buildRegistryUrl with invite opt\"" functions/src/__tests__/publicUrls.test.ts` returns 1
    - `grep -c "appends ?invite=1" functions/src/__tests__/publicUrls.test.ts` returns 1
    - `cd functions && npx tsc --noEmit 2>&1 | grep -E "publicUrls\.ts|onPurchaseNotification\.ts" | wc -l` returns 0 (no new type errors in either file)
    - `cd functions && npx jest --testPathPattern=publicUrls 2>&1 | grep -E "Tests:.*passed" | grep -c "8 passed"` returns 1 (4 existing + 4 new tests, all passing)
  </acceptance_criteria>
  <done>buildRegistryUrl accepts an optional `{ invite?: boolean }` second argument; passing `{ invite: true }` produces `${base}/registry/{id}?invite=1`; all callers without opts continue to receive the plain URL; publicUrls.test.ts has 8 passing tests.</done>
</task>

<task type="auto" tdd="false">
  <name>Task 2: Update inviteToRegistry to pass invite: true when building the URL</name>
  <files>functions/src/registry/inviteToRegistry.ts</files>
  <read_first>
    - functions/src/registry/inviteToRegistry.ts (READ FIRST — file is 164 lines; you are changing ONE line at line 95)
    - functions/src/email/templates/invite.ts (READ — confirms the template just renders whatever `registryUrl` it receives; no template change needed)
    - functions/src/__tests__/inviteToRegistry.test.ts (READ — the existing test mocks email send; the URL it asserts on is going to change to include `?invite=1`, so this test may need updating if it asserts on the URL — search for "registry" or "registryUrl" in the test)
  </read_first>
  <behavior>
    - Line 95 of `inviteToRegistry.ts` calls `buildRegistryUrl(registryId)` — change to `buildRegistryUrl(registryId, { invite: true })`
    - The email sent by `inviteToRegistry` now contains a CTA href containing `?invite=1`
    - No other behavior changes (the function still writes mail doc, still updates invitedUsers, still calls FCM/notifications for existing users)
    - Existing inviteToRegistry tests continue to pass (any test asserting on the URL must be updated to expect `?invite=1`)
  </behavior>
  <action>
    Step 1 — Open `functions/src/registry/inviteToRegistry.ts`. Locate line 95:

    ```typescript
    const registryUrl = buildRegistryUrl(registryId);
    ```

    Change it to:

    ```typescript
    // Phase 15: invite-flavored URL carries ?invite=1 so the web fallback's
    // RegistryPage detects "came from email invite" and surfaces the
    // InviteLandingModal (create-account-or-magic-link gate).
    const registryUrl = buildRegistryUrl(registryId, { invite: true });
    ```

    Step 2 — Run `cd functions && npm test -- inviteToRegistry` to see if any existing test breaks because it asserts on the URL. If a test does assert on `registryUrl` (e.g. via the mail doc the test inspects), update the expected value to include `?invite=1`.

    Search for the URL assertion: `grep -n "registry" functions/src/__tests__/inviteToRegistry.test.ts | head -20`. If a test asserts the mail's text/html contains `${base}/registry/{id}` (without query), update it to also accept `?invite=1`. Most likely the test checks for `subject.toContain("Ana")` (owner name) — that won't change. But if it asserts the URL exactly, you must update.

    Step 3 — Confirm `cd functions && npm test -- inviteToRegistry` passes after the change.

    Do NOT modify anything else in `inviteToRegistry.ts` — not the FieldPath update, not the FCM call, not the notification write. Touch ONE line only (plus the comment above it).
  </action>
  <verify>
    <automated>cd functions && npm test -- inviteToRegistry 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c "buildRegistryUrl(registryId, { invite: true })" functions/src/registry/inviteToRegistry.ts` returns 1
    - `grep -c "buildRegistryUrl(registryId)" functions/src/registry/inviteToRegistry.ts` returns 0 (the old call form is gone)
    - `grep -c "FieldPath" functions/src/registry/inviteToRegistry.ts` returns at least 2 (unchanged — confirms you didn't accidentally touch the FieldPath code)
    - `grep -c "sendInvitePush" functions/src/registry/inviteToRegistry.ts` returns at least 2 (unchanged — confirms you didn't touch the FCM call site)
    - `cd functions && npx tsc --noEmit 2>&1 | grep "inviteToRegistry\.ts" | wc -l` returns 0 (no new type errors)
    - `cd functions && npx jest --testPathPattern=inviteToRegistry 2>&1 | grep -E "Tests:.*passed" | grep -v "failed"` matches (zero failures)
    - The email template file `functions/src/email/templates/invite.ts` is UNCHANGED — `git diff functions/src/email/templates/invite.ts` returns no output (or only whitespace)
  </acceptance_criteria>
  <done>inviteToRegistry's single call to buildRegistryUrl now passes `{ invite: true }`; all existing inviteToRegistry tests pass (with any URL-assertion test updated to expect `?invite=1`); no other file is modified.</done>
</task>

</tasks>

<verification>
- `cd functions && npm test 2>&1 | grep -E "Tests:" | tail -5` shows zero failures across all functions tests (smoke + publicUrls + inviteToRegistry + reservation tests)
- `cd functions && npx tsc --noEmit` exits 0
- `grep -n "?invite=1" functions/src/config/publicUrls.ts` returns at least 1 match
- The full chain works end-to-end (manually traceable): `inviteToRegistry.ts` line 95 → `buildRegistryUrl(id, { invite: true })` → returns `${base}/registry/{id}?invite=1` → `inviteTemplate({ registryUrl })` → email `ctaUrl` contains `?invite=1`
- `git diff functions/src/email/templates/invite.ts` produces no output (template untouched — it just renders what it's given)
- `git diff functions/src/notifications/onPurchaseNotification.ts` produces no output (purchase-notification URL builder caller is untouched; receives plain URL because it passes no opts)
</verification>

<success_criteria>
This plan is complete when:
1. `buildRegistryUrl(id, { invite: true })` produces `${base}/registry/{id}?invite=1`
2. `buildRegistryUrl(id)` (no opts) continues to produce the plain URL — verified by 8 passing publicUrls.test.ts tests
3. `inviteToRegistry.ts` passes `{ invite: true }` to buildRegistryUrl; all inviteToRegistry tests pass
4. The invite-email template is untouched (it just renders what it receives)
5. The purchase-notification caller of buildRegistryUrl is untouched and continues to produce plain URLs
6. `cd functions && npm test` exits 0 with zero failures
</success_criteria>

<output>
After completion, create `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-02-backend-url-builder-and-email-SUMMARY.md` summarizing:
- New `BuildRegistryUrlOpts` interface and its single field
- The 4 new tests added to publicUrls.test.ts
- The single line changed in inviteToRegistry.ts
- Confirmation that the email template and purchase-notification caller were both untouched
- Sample URL produced by the invite email after this plan
</output>
