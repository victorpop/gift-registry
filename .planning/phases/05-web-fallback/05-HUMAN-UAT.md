---
status: partial
phase: 05-web-fallback
source: [05-VERIFICATION.md]
started: 2026-04-19
updated: 2026-04-19
---

## Current Test

[awaiting human testing]

## Tests

### 1. reCAPTCHA v3 App Check token acquisition in production
expected: After deploying to Firebase Hosting preview channel and opening in real Chrome, DevTools Network panel shows `appcheck.googleapis.com/v1/projects/*/apps/*/appcheck:exchange` succeeding before the first Firestore or Functions call
result: [pending]

### 2. Retailer redirect opens in a new tab and keeps the registry tab alive
expected: Clicking Reserve on a live registry in Chrome and Safari opens the `affiliateUrl` in a new tab AND the registry tab still shows the countdown banner afterwards (not replaced, not blank)
result: [pending]

### 3. Guest localStorage persistence across browser restart
expected: Reserve as guest, close the browser completely, re-open `/registry/:id`, click Reserve — the guest modal pre-fills first name / last name / email from localStorage
result: [pending]

### 4. Language auto-detection from browser locale on cold load
expected: Set system language to Romanian, open the site in a clean profile (no localStorage lang key) — UI renders in Romanian; switching via LanguageSwitcher persists to localStorage and survives reload
result: [pending]

### 5. SPA deep-link on cold browser with private registry rules-deny
expected: Paste `https://<host>/registry/<private-id>` in a new private window without being logged in; the generic "Registry not available" page renders (NOT a stack trace, NOT an explicit "you are not invited" message, NOT the registry content)
result: [pending]

### 6. Email deep-link re-reserve end-to-end
expected: Trigger a reservation expiry (Phase 4 stub), click the re-reserve link in the email, land on `/reservation/:id/re-reserve`, get redirected to `/registry/:id?autoReserveItemId=:itemId`, reservation auto-fires, new tab opens to retailer. **Note:** requires Phase 6 email delivery wiring before this can be exercised beyond the console-log stub.
result: [pending]

### 7. Google OAuth popup flow
expected: Click "Sign in with Google" in AuthModal, popup opens, select account, popup closes, modal closes, user is signed in. Requires live Firebase project with Google OAuth provider enabled and the web app's authorized domain configured. Blocked by Google OAuth requiring matching debug SHA-1 or production domain registration.
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0
blocked: 0

## Gaps

None automated-check — all gaps are real-browser behaviors outside jsdom scope, pre-identified in 05-VALIDATION.md.
