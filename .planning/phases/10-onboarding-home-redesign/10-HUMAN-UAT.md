---
status: partial
phase: 10-onboarding-home-redesign
source: [10-05-PLAN.md Task 2, 10-VERIFICATION.md]
started: 2026-04-21T15:45:00.000Z
updated: 2026-04-21T15:45:00.000Z
---

## Current Test

[awaiting human testing — deferred by user 2026-04-21 to unblock Phase 11]

## Tests

### 1. Sign-up mode default
expected: Auth screen opens in SIGN-UP mode; First/Last/Email/Password/Confirm fields visible; CTA reads "Sign up"; no "Forgot password?" link
result: pending

### 2. Wordmark top bar
expected: `giftmaison.` renders top-left in Instrument Serif italic with terracotta accent period
result: pending

### 3. Italic-accent headline
expected: "Start your" on line 1 (italic, inkSoft); "first registry." line 2 — "first registry" in ink, trailing period in accent
result: pending

### 4. Subline copy
expected: "A warm, shareable list — set up in under a minute." (bodyM, inkSoft)
result: pending

### 5. Google banner visual + OAuth flow
expected: terracotta pill with shadow; 20×20 white G circle; "Continue with Google" accent-ink label; italic serif "→" arrow at 0.9 alpha; 3 top-right concentric rings (opacity 0.08/0.12/0.18); tap triggers Google sign-in
result: pending

### 6. Divider label
expected: "or sign up with email" in sign-up mode, "or sign in with email" in sign-in mode (bodyS, inkFaint, flanked by 1 dp line dividers)
result: pending

### 7. Pulsing caret on email field
expected: focusing empty email field shows 2×20 dp accent caret pulsing at ≈1.1 s cadence (opacity only, no scale); disappears while typing
result: pending

### 8. Password field eye toggle + helper
expected: eye toggle right; helper "At least 8 characters with one number." (bodyXS, inkFaint) in sign-up mode
result: pending

### 9. Inline warn banner on validation error
expected: tapping Sign up with blank first/last name shows warn @ 0.15 alpha bg banner with inkSoft text "First and last name are required."
result: pending

### 10. Sign-up ↔ Sign-in mode toggle
expected: tap "Already have an account? Log in" → fields collapse (no first/last/confirm); CTA → "Log in"; divider → "or sign in with email"
result: pending

### 11. Sign-in → Sign-up toggle
expected: tap "New here? Sign up" → mode returns to sign-up; fields expand
result: pending

### 12. AUTH-05 guest path preserved
expected: "Continue as guest" tertiary TextButton below footer pill (bodyXS, inkFaint); tap → anonymous Firebase sign-in → navigates to Home
result: pending

### 13. Home top bar: wordmark + avatar
expected: `giftmaison.` left; 30 dp olive-coloured circle right with user initials in paper-coloured bodyMEmphasis; tap avatar → Settings
result: pending

### 14. Home headline + stats caption
expected: "Your registries" displayXL ink; "N active · M items total" monoCaps inkFaint with correct active count
result: pending

### 15. SegmentedTabs
expected: paperDeep pill track + paper selected pill; 3 tabs "ACTIVE / DRAFTS / PAST"; 200 ms colour tween on selection; selected = ink label, unselected = inkFaint
result: pending

### 16. Primary card + secondary cards
expected: exactly ONE primary card (ink bg, paper content, image dimmed to 70 % brightness); others are paperDeep + 1 dp line + full-brightness image; each card shows 16:9 hero + occasion pill top-left + date bottom-right + displayS title + "N items • M reserved • K given" stats line
result: pending

### 17. Empty state per tab
expected: tab with no matching registries shows centred bodyM inkFaint single-line empty text ("No active / drafts / past registries yet"); no illustration, no CTA; Phase 9 bottom nav + FAB remain visible
result: pending

## Locale verification (RO)

### 18. RO copy
expected: SCR-06 — "Începe-ți / primul registru." headline; "Continuă cu Google"; "Înregistrează-te"/"Autentifică-te" CTAs; "Continuă ca oaspete" tertiary. SCR-07 — "Registrele tale"; tabs "ACTIVE / SCHIȚE / TRECUTE"; empty-state RO copy; avatar content desc "Deschide setări"
result: pending

## Regression guards

### 19. Registry card tap
expected: navigates to RegistryDetail (pre-v1.1 layout, Phase 10 does not re-skin Detail)
result: pending

### 20. Long-press card actions
expected: DropdownMenu with Edit + Delete; Edit → edit screen; Delete → confirmation dialog → deletion
result: pending

### 21. Sign out regression
expected: Sign out returns to AuthScreen with sign-up default mode
result: pending

## Summary

total: 21
passed: 0
issues: 0
pending: 21
skipped: 0
blocked: 0

## Gaps

_(none triaged — user deferred UAT to unblock Phase 11. Will appear in `/gsd:progress` and `/gsd:audit-uat` until resolved.)_
