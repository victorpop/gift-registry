# Phase 15: Web Invite-Landing + Magic-Link Guest Flow - Context

**Gathered:** 2026-05-22
**Status:** Ready for planning
**Source:** In-chat discussion with user (scope agreed before `/gsd:plan-phase` invocation; user selected "I trust the scope above" to skip formal `/gsd:discuss-phase`)

<domain>
## Phase Boundary

**What this phase delivers (web-only):**

When an email-invited recipient clicks the registry link in their invite email, they land on a dedicated invite-landing modal that lets them:
1. **Create an account** — existing email+password signup flow; on success they're added to `registries.invitedUsers` under their new UID (replacing the prior `email:{email}` entry), and land on the shared registry.
2. **Continue as guest** — Firebase passwordless magic-link sign-in (`sendSignInLinkToEmail`); on link-click they're authenticated as a real Firebase user (anonymous-equivalent UX, real UID under the hood), the same UID-swap runs, and they land on the shared registry.

Both paths converge on: authenticated user with their UID in `registries.invitedUsers`, redirected to `/registry/{registryId}`.

**Scope:**
- Web fallback only (`web/`)
- New invite-landing modal component (separate from existing `SaveYourSpotModal.tsx`)
- Backend invite-email URL gets `?invite=1` query param so the landing page can detect "came from email"
- New Cloud Function (auth-trigger or callable, decided in plan) to swap `email:{email}` → `{uid}` entries in `registries.invitedUsers` when an invited not-yet-registered user signs up
- New magic-link callback route on the web app
- i18n: new strings for the modal in en + ro

**Out of scope:**
- Android invite-landing UI (Android invite flow already exists via `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt`)
- Changes to `SaveYourSpotModal.tsx` (that modal is for the *public-link reservation* flow, distinct from invite landing)
- Adding invite tokens or a dedicated `invites` subcollection (the existing `registries.invitedUsers` map design is preserved — we only add the `email:` → UID swap on signup)
- Owner-side "send invite" UI on web (Phase 5 explicitly kept web giver-only; sending invites remains Android-only)

</domain>

<decisions>
## Implementation Decisions

### Modal — visual & interaction

- **New component file:** `web/src/features/auth/InviteLandingModal.tsx` — distinct file, not a refactor of `SaveYourSpotModal.tsx`.
- **Visual baseline:** Match `SaveYourSpotModal.tsx`'s style — Radix Dialog (`@radix-ui/react-dialog`), `bg-gm-paper`, `font-display` title, `MonoCaption` for subtitle/labels, `Btn` for primary CTA, `Field` for inputs. Use existing GiftMaison atoms from `web/src/components/giftmaison/`.
- **Form fields in initial modal state:** None pre-filled. Primary CTA is "Create an account" → either reveals the password+name fields inline (single-modal variant) OR routes to the existing `AuthScreen` with a "return to /registry/{id}" intent (multi-screen variant). **Planner decides** which is cleaner given the existing AuthScreen.
- **Secondary action:** "Continue as guest" → triggers Firebase `sendSignInLinkToEmail`, then transitions the modal to a "Check your email" confirmation state (in-modal, no navigation).

### Modal — dismissibility

- **Decision:** Dismissible. "Not now" / overlay-click / Esc closes the modal and leaves the user on the registry page. For *private* registries the underlying page will Firestore-deny → 404 (existing behavior); for *public* registries the user can browse anonymously.
- **Rationale:** Lowest friction; matches the pattern of the existing `SaveYourSpotModal.tsx` which uses Radix's default dismissible Dialog.
- **Consequence:** The modal must not re-appear after dismissal during the same session. After dismissal, strip `?invite=1` from the URL so a page refresh doesn't re-trigger.

### Invite signal — how the page knows "came from an email invite"

- **Decision:** Append `?invite=1` to the registry URL in invite emails.
- **Implementation:** Extend `functions/src/config/publicUrls.ts:buildRegistryUrl()` to accept an `invite?: boolean` option (or add a sibling helper `buildInviteRegistryUrl`). Update `functions/src/email/templates/invite.ts` to use the invite-flavored URL in the CTA link.
- **Detection:** `RegistryPage.tsx` reads the `invite` query param; if `=== '1'` AND user is unauthenticated → show `InviteLandingModal`. Otherwise behave as today.

### Guest path — magic-link instead of pure anonymous

- **Decision:** "Continue as guest" uses Firebase passwordless email-link sign-in (`sendSignInLinkToEmail` + `signInWithEmailLink`) — NOT `signInAnonymously`.
- **Rationale:** Pure anonymous UIDs cannot be added to `registries.invitedUsers` (the invite is keyed by `email:{email}`, not by anonymous UID). Magic-link gives a real UID tied to the invitee's email, which lets the existing `invitedUsers` membership check work without modifying Firestore rules. UX is still "no password required" so the framing remains guest-like.
- **continueUrl decision:** **Planner chooses** between (a) passing target registry path via Firebase `actionCodeSettings.url` (recommended — survives email client redirects, no localStorage required), or (b) storing target path in `localStorage` and resolving on callback. Recommend (a) for reliability; (b) is fallback if Firebase's continueUrl whitelist becomes painful.

### Magic-link callback route

- **New route in `web/src/App.tsx`:** `/auth/email-link` (or similar — planner can finalize the name).
- **Behavior:** Calls `signInWithEmailLink(auth, email, window.location.href)`, awaits, then navigates to the target registry path (from continueUrl or localStorage). On error (expired link, email mismatch) → show error state + offer to re-request.

### Email→UID swap on signup

- **Problem:** `inviteToRegistry` (existing) writes `invitedUsers["email:foo@bar.com"] = true` for not-yet-registered invitees. When that invitee later signs up (either via password or magic-link), their new UID is NOT yet in `invitedUsers`, so Firestore rules will deny their registry read.
- **Decision:** Add a Cloud Function that, on new user creation, scans `registries` where `invitedUsers["email:{newUser.email}"] == true`, atomically swaps that key for `{newUid}: true`, and removes the email-prefixed entry. One Firestore transaction per registry.
- **Trigger mechanism — planner decides:**
  - **(a) Auth trigger** — `functions.auth.user().onCreate` (1st gen) OR Firebase Identity Platform blocking function (2nd gen `beforeUserCreated`/`beforeUserSignedIn`). 2nd gen blocking functions are preferred per project's stated 2nd-gen Functions standard.
  - **(b) Callable** — `swapInvitedEmailToUid` callable invoked from the client immediately after `signUpEmail` / `signInWithEmailLink`. Simpler but requires the client to remember to call it.
  - **Recommend (a) auth/blocking trigger** for robustness — fires server-side regardless of which client path created the user.
- **Edge cases:**
  - Email mismatch (user signs up with `bob@bar.com` but was invited as `alice@bar.com`) → swap nothing; the orphan `email:alice@bar.com` entry remains until alice herself signs up or owner removes it.
  - User already exists at invite-send time → `inviteToRegistry` already stores their UID directly (existing behavior); no swap needed.
  - Re-invite after signup → `inviteToRegistry` looks up UID first (existing behavior); writes UID directly.
  - Magic-link sign-in for an existing user → `signInWithEmailLink` just authenticates the existing account; if their UID is already in `invitedUsers` (because they accepted a prior invite), no swap needed. Trigger should be safe to no-op on existing-user re-auth.

### Post-auth navigation

- After either path completes auth: navigate to `/registry/{registryId}` (clean URL, no `?invite=1`).
- If the swap function ran asynchronously and Firestore returns permission-denied on the first read, surface a brief retry state (the swap is fast but not synchronous from the client's perspective). Planner should consider a short retry-with-backoff on the registry fetch if the user just signed up and was on the invite path.

### Localization

- **Files to extend:** `web/src/i18n/en.json` and `web/src/i18n/ro.json`.
- **Key namespace:** `invite_landing_*` (e.g., `invite_landing.title`, `invite_landing.body`, `invite_landing.primary_cta`, `invite_landing.secondary_cta`, `invite_landing.check_email_title`, `invite_landing.check_email_body`, `invite_landing.error_*`). Follow the existing `save_your_spot.*` namespace pattern.
- **Email template strings:** `functions/src/email/templates/invite.ts` already has en/ro — extend only if the URL change requires copy adjustments (likely no).

### App Check considerations

- New magic-link client calls (`sendSignInLinkToEmail`, `signInWithEmailLink`) and any new callable must be App-Check-protected. Firebase Auth methods are not directly App-Check-enforced (Auth has its own abuse controls), but if a new callable (`swapInvitedEmailToUid`) is added, it MUST `enforceAppCheck: true` per the existing project posture.
- Recent learning ([[reference_appcheck_cached_failure]]): cached App Check failures need IndexedDB clear + secret key fix; relevant for testing but not a code change in this phase.

### Claude's Discretion (planner decides)

- Whether the create-account path inlines password+name fields in the modal vs. routing to the existing full-screen `AuthScreen` with a return-intent. Both are acceptable; choose based on which fits the existing AuthScreen architecture more cleanly.
- Exact naming of new routes, helpers, and Cloud Function (e.g., `swapInvitedEmailToUid` vs. `linkInviteOnSignup` — planner picks).
- Whether to add a small `useInviteLanding` hook to encapsulate "read `?invite=1`, manage modal open state, handle URL cleanup" in `RegistryPage.tsx`, or inline the logic.
- Test layout: extend existing `web/src/__tests__/` patterns; Vitest for component/unit tests, Playwright (per `web/playwright.config.ts`) for the end-to-end magic-link flow if feasible against the emulator.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Web — visual & interaction baseline
- `web/src/features/auth/SaveYourSpotModal.tsx` — Style reference (Radix Dialog, gm-paper, font-display, MonoCaption, Btn). Do NOT modify; new modal mirrors its visual conventions.
- `web/src/features/reservation/ReserveButton.tsx` — Reference for how a modal is wired to a page and how post-action navigation works.
- `web/src/components/giftmaison/` — Atoms (`Btn.tsx`, `Field.tsx`, `MonoCaption.tsx`, etc.) the new modal MUST use; no ad-hoc styling.

### Web — auth & state primitives
- `web/src/features/auth/authProviders.ts` — Existing `signUpEmail`; the new `sendSignInLinkToEmail` and `signInWithEmailLink` wrappers go here.
- `web/src/features/auth/useAuth.ts` — Auth state hook; new modal/route consume `isReady` to avoid post-redirect flash ([[feedback_live_deploy_pacing]] context: post-redirect flash was fixed in Phase 14 by gating on `useAuth.isReady`).
- `web/src/features/auth/useGuestIdentity.ts` — localStorage pattern reference (if the planner chooses option (b) for continueUrl storage).
- `web/src/features/auth/AuthScreen.tsx` — Existing full-page sign-in screen; planner may route create-account flow here with a return-intent.

### Web — routing & page wiring
- `web/src/App.tsx` — Add magic-link callback route here.
- `web/src/pages/RegistryPage.tsx` — Page that detects `?invite=1` and triggers the modal.

### Backend — invite send + email + URL
- `functions/src/registry/inviteToRegistry.ts` — Existing invite-send Cloud Function. The `email:{email}` map key convention is established here.
- `functions/src/email/templates/invite.ts` — Email template that needs to use the `?invite=1` URL.
- `functions/src/config/publicUrls.ts` — `buildRegistryUrl()` to extend with an `invite` flag.

### Backend — security model
- `firestore.rules` — `isInvited()` function (lines ~30-36) requires `request.auth.uid` ∈ `invitedUsers`. Phase 15 does NOT modify these rules; the new Cloud Function ensures the UID is in the map before the user's first read.

### i18n
- `web/src/i18n/en.json` and `web/src/i18n/ro.json` — Add `invite_landing.*` namespace following the existing `save_your_spot.*` pattern.

### Project conventions
- `CLAUDE.md` (repo root) — Project constraints; React 19, Firebase JS SDK 11, i18next, no hardcoded strings.
- `.planning/STATE.md` — Project history; relevant decisions include "invitedUsers map (not array) for O(1) membership check" (Phase 01) and "inviteToRegistry uses email: prefix for non-user invite keys" (Phase 03).

</canonical_refs>

<specifics>
## Specific Ideas

- **URL shape:** `https://gift-registry-ro.web.app/registry/{registryId}?invite=1` (base URL from `PUBLIC_WEB_BASE_URL` env per existing `publicUrls.ts`).
- **Magic-link continueUrl:** Recommend `https://gift-registry-ro.web.app/auth/email-link?next=%2Fregistry%2F{registryId}` so the callback knows where to send the user.
- **Cloud Function name (suggestion, planner can override):** `linkInviteOnSignup` (auth-trigger) or `swapInvitedEmailToUid` (callable). 2nd-gen blocking function preferred.
- **i18n keys (suggestion):**
  - `invite_landing.title` — e.g. "You're invited to {registryName}" (registryName interpolation if loadable; else generic "You're invited")
  - `invite_landing.body` — explains the two choices
  - `invite_landing.primary_cta` — e.g. "Create an account →"
  - `invite_landing.secondary_cta` — e.g. "Continue as guest"
  - `invite_landing.check_email_title` — e.g. "Check your email"
  - `invite_landing.check_email_body` — e.g. "We sent a sign-in link to {email}. Click it to open the registry."
  - `invite_landing.error_link_expired` — magic-link expiry copy
  - `invite_landing.error_email_mismatch` — different email than the invited one
- **Anti-flash:** Gate the modal-open state on `useAuth.isReady` ([[feedback_live_deploy_pacing]] / Phase 14 fix pattern) to avoid showing the modal momentarily for an already-authenticated user before auth state hydrates.

</specifics>

<deferred>
## Deferred Ideas

- **Owner-side "send invite" UI on web** — out of scope (web stays giver-only per Phase 5 charter). Sending invites remains Android-only.
- **Dedicated `invites` subcollection / invite tokens** — out of scope; existing `invitedUsers` map model preserved.
- **Custom magic-link email design** — Firebase's default email-link template is used initially; custom HTML template can be added later if needed (would require Email Action Handler customization).
- **Registry name in modal title (`invite_landing.title` interpolation)** — only if the registry can be loaded before auth (it cannot, for private registries). Fall back to a generic "You're invited" title; defer the personalized-title work until/unless we add a public preview endpoint.
- **Analytics on invite-funnel conversion** — out of scope; can be added once the flow is live.

</deferred>

---

*Phase: 15-web-invite-landing-magic-link-guest-flow*
*Context gathered: 2026-05-22 from in-chat discussion (in lieu of `/gsd:discuss-phase`); user explicitly opted to skip formal discuss-phase given the scope was nailed down conversationally*

---

## Phase 16 update (appended 2026-05-24)

> **Source:** Phase 16 CONTEXT D-14 + Phase 16 plan 16-06 Task 1 (closure step).
> **Authoritative record:** `.planning/STATE.md` entry "Phase 16 added 2026-05-24" — this stanza is a convenience pointer next to Phase 15's own decisions.

**Coupling contract — `linkInviteOnSignup` MUST target `pendingInvitedUsers` (not `invitedUsers`):**

Phase 16 shipped a strict accept-gate invite model on 2026-05-24. The new behavior is:
- `inviteToRegistry` writes new invites to `registries.{id}.pendingInvitedUsers[key] = true` (was: `invitedUsers`).
- The invitee taps Accept in the Android inbox (sheet → `acceptInvite` callable) to atomically promote the key into `invitedUsers`. Only then does the existing `isInvited()` rule grant read access.
- Decline removes the pending entry without promoting.

**What this means for Phase 15 when it resumes:**
- The `linkInviteOnSignup` blocking function (Phase 15's planned Cloud Function that swaps `email:{email}` → `{newUid}` at signup time) MUST read and write to `pendingInvitedUsers`, NOT `invitedUsers`.
- Signup does NOT imply acceptance. After signup + email→UID swap, the newly-signed-up user MUST still go through the Android accept-gate flow (open inbox → tap INVITE card → Accept in sheet) to gain read access to the private registry.
- Web-only invitees (who never install Android) effectively cannot accept the invite in v1; the private registry will Firestore-deny → 404 until they install Android. This is documented as an acceptable v1 limitation per Phase 16 D-17.

**Existing Phase 15 plan implication:**
- Plan `15-04-magic-link-callback-and-cloud-function-PLAN.md` (when Phase 15 resumes) must reference `pendingInvitedUsers` wherever it currently references `invitedUsers` in the `linkInviteOnSignup` design.
- No other Phase 15 plans (15-01, 15-02, 15-03, 15-05) are affected by this coupling.

**No backfill / migration needed:** Pre-Phase-16 `invitedUsers` entries are grandfathered (Phase 16 D-12). Only NEW invites flow through `pendingInvitedUsers`.
