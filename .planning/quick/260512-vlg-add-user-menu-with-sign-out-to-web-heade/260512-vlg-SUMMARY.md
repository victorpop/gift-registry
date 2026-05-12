---
phase: quick-260512-vlg
plan: "01"
subsystem: web-frontend
tags: [user-menu, sign-out, dropdown, radix-ui, i18n, accessibility]
dependency_graph:
  requires: [web/src/features/auth/authProviders.ts, web/src/features/auth/useAuth.ts, web/src/components/ToastProvider.tsx]
  provides: [web/src/components/giftmaison/UserMenu.tsx]
  affects: [web/src/components/giftmaison/TopNav.tsx]
tech_stack:
  added: ["@radix-ui/react-dropdown-menu@2.1.x"]
  patterns: ["Radix DropdownMenu with Portal for accessible dropdown", "i18next interpolation for user display name", "Reactive sign-out via onAuthStateChanged (no imperative navigation)"]
key_files:
  created:
    - web/src/components/giftmaison/UserMenu.tsx
    - web/src/components/giftmaison/__tests__/UserMenu.test.tsx
  modified:
    - web/src/components/giftmaison/TopNav.tsx
    - web/src/components/giftmaison/index.ts
    - web/i18n/en.json
    - web/i18n/ro.json
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/package.json
decisions:
  - "Used @radix-ui/react-dropdown-menu@^2.1.0 (Radix family already in use) rather than custom dropdown"
  - "No useNavigate on sign-out — useAuth's onAuthStateChanged subscriber reactively sets user=null, TopNav re-renders Sign in CTA automatically"
metrics:
  duration: ~8 min
  completed_date: "2026-05-12"
  tasks_completed: 3
  files_changed: 9
human_verify: cleared
---

# Quick 260512-vlg: Add User Menu with Sign-Out to Web Header

**One-liner:** Radix DropdownMenu avatar trigger in TopNav giving authenticated users a keyboard-accessible "Sign out" action backed by Firebase Auth, with EN + RO i18n.

## What Was Built

The TopNav's static avatar `<div>` (which had no click handler) was replaced with a `<UserMenu>` atom using `@radix-ui/react-dropdown-menu`. Authenticated users now see their initials circle as a clickable trigger that opens a positioned dropdown containing a "Sign out" item.

**Files changed:**

| File | Role |
|------|------|
| `web/src/components/giftmaison/UserMenu.tsx` | New atom — Radix DropdownMenu trigger + portal + sign-out item |
| `web/src/components/giftmaison/TopNav.tsx` | Replace static avatar div with `<UserMenu user={user} initials={...} />` |
| `web/src/components/giftmaison/index.ts` | Export UserMenu and UserMenuProps from giftmaison barrel |
| `web/src/components/giftmaison/__tests__/UserMenu.test.tsx` | 4 Vitest specs: aria-label, open, signOut call, error toast |
| `web/i18n/en.json` + `web/src/i18n/en.json` | Add `auth.user_menu_label`, `auth.sign_out` (EN) |
| `web/i18n/ro.json` + `web/src/i18n/ro.json` | Add `auth.user_menu_label`, `auth.sign_out` (RO) |
| `web/package.json` | Add `@radix-ui/react-dropdown-menu@^2.1.0` dependency |

## Radix Version Installed

`@radix-ui/react-dropdown-menu@2.1.x` — installed to match existing Radix family (`@radix-ui/react-dialog@^1.1.15`, `@radix-ui/react-toast@^1.2.0`). Radix versions packages independently so DropdownMenu 2.x is correct alongside Dialog 1.x.

## Sign-Out Navigation Approach

`useNavigate` was deliberately NOT called after sign-out. `useAuth` subscribes to Firebase Auth's `onAuthStateChanged` — when `signOut()` resolves, the listener fires, `user → null`, and TopNav automatically re-renders the "Sign in" CTA. This matches the existing reactive pattern in the codebase and is intentional: a giver mid-flow on `/registry/:id` who accidentally hits sign-out should remain on the registry page (accessible to anonymous users) rather than being routed away.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added i18n keys to web/src/i18n/ (not just web/i18n/)**
- **Found during:** Task 2 test execution
- **Issue:** The plan specified adding keys to `web/i18n/en.json` and `web/i18n/ro.json`, but the i18n module (`web/src/i18n/index.ts`) loads from `web/src/i18n/en.json` / `web/src/i18n/ro.json`. Tests showed raw keys (`auth.user_menu_label`) being rendered instead of translations.
- **Fix:** Added the two new keys to both `web/src/i18n/en.json` and `web/src/i18n/ro.json` in addition to the `web/i18n/` copies.
- **Files modified:** `web/src/i18n/en.json`, `web/src/i18n/ro.json`
- **Commit:** 1b54ed4

## Task Commits

| Task | Commit | Description |
|------|--------|-------------|
| Task 1 | `359ba1f` | feat: UserMenu + Radix + i18n keys + TopNav wire-up |
| Task 2 | `1b54ed4` | test: 4 Vitest specs for UserMenu |

## UAT Notes

Human verification cleared by user (2026-05-12). All 8 verification steps confirmed: avatar opens dropdown, keyboard + click-outside dismissal work, Sign out flips TopNav to the "Sign in" CTA reactively, EN/RO toggle swaps strings live, 115/115 web tests green.

## Self-Check

Task 1 files created/modified:
- [x] `web/src/components/giftmaison/UserMenu.tsx` — created
- [x] `web/src/components/giftmaison/TopNav.tsx` — modified
- [x] `web/src/components/giftmaison/index.ts` — modified
- [x] `web/i18n/en.json` + `web/i18n/ro.json` — modified
- [x] `web/src/i18n/en.json` + `web/src/i18n/ro.json` — modified (auto-fix)
- [x] `web/package.json` — modified (new dependency)

Task 2 files:
- [x] `web/src/components/giftmaison/__tests__/UserMenu.test.tsx` — created, 4 specs, all pass

Commits verified: `359ba1f`, `1b54ed4`

TypeScript: exits 0
Test suite: 115/115 pass across 23 files
