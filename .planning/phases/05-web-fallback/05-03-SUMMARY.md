---
phase: 05-web-fallback
plan: 03
subsystem: ui
tags: [i18next, react-i18next, i18next-browser-languagedetector, react-router, localization, routing]

requires:
  - phase: 05-web-fallback
    plan: 02
    provides: "web/src/main.tsx with App Check init + QueryClientProvider; web/src/firebase.ts singletons"

provides:
  - "web/src/i18n/en.json — full EN translation resource with all UI-SPEC keys (37 keys)"
  - "web/src/i18n/ro.json — full RO translation resource with all UI-SPEC keys (37 keys)"
  - "web/src/i18n/index.ts — i18next init with LanguageDetector + react-i18next binding"
  - "web/src/components/LanguageSwitcher.tsx — toggle en/ro, persists to localStorage 'lang'"
  - "web/src/App.tsx — createBrowserRouter with 4 routes"
  - "web/src/pages/AppRootPage.tsx — root '/' stub page"
  - "web/src/pages/RegistryPage.tsx — '/registry/:id' stub (Plan 04 extends)"
  - "web/src/pages/ReReservePage.tsx — '/reservation/:id/re-reserve' stub (Plan 07 extends)"
  - "web/src/pages/NotFoundPage.tsx — generic 404 per WEB-D-13/WEB-D-14"
  - "web/src/main.tsx — import './i18n' added before import App (synchronous i18n init)"

affects: [05-04, 05-05, 05-06, 05-07]

tech-stack:
  added:
    - "i18next ^26.0 — already in package.json from Plan 01"
    - "react-i18next ^17.0 — already in package.json from Plan 01"
    - "i18next-browser-languagedetector ^8.0 — already in package.json from Plan 01"
    - "react-router ^7.14 — already in package.json from Plan 01 (used for first time here)"
  patterns:
    - "i18n initialized as side-effect import before createRoot: import './i18n' in main.tsx before import App"
    - "dot-notation key access: t('reservation.reserve_item') — keySeparator '.' (default), NOT keySeparator: false"
    - "localStorage 'lang' key for manual language override: lookupLocalStorage: 'lang'"
    - "React Router v7 import surface: import { createBrowserRouter, RouterProvider, useParams, useNavigate, useSearchParams } from 'react-router' (NOT 'react-router-dom')"
    - "createMemoryRouter for testing: createMemoryRouter([...routes], { initialEntries: [path] })"

key-files:
  created:
    - "web/src/i18n/en.json — full EN translation (37 keys across 5 namespaces)"
    - "web/src/i18n/ro.json — full RO translation (37 keys across 5 namespaces)"
    - "web/src/i18n/index.ts — i18next singleton with LanguageDetector + initReactI18next"
    - "web/src/components/LanguageSwitcher.tsx — en/ro toggle button"
    - "web/src/pages/AppRootPage.tsx — root page stub"
    - "web/src/pages/RegistryPage.tsx — registry detail stub"
    - "web/src/pages/ReReservePage.tsx — re-reserve deep link stub"
    - "web/src/pages/NotFoundPage.tsx — generic 404 page"
    - "web/src/__tests__/i18n.test.ts — 5 i18n tests"
    - "web/src/__tests__/App.test.tsx — 4 route smoke tests"
  modified:
    - "web/src/App.tsx — replaced scaffold stub with createBrowserRouter (4 routes)"
    - "web/src/main.tsx — added import './i18n' before import App"
    - "web/i18n/en.json — synced to match web/src/i18n/en.json (legacy seed)"
    - "web/i18n/ro.json — synced to match web/src/i18n/ro.json (legacy seed)"

key-decisions:
  - "i18n import order in main.tsx: import './i18n' placed at line 6, import App at line 7 — guarantees i18next is initialized synchronously before any React component mounts"
  - "Legacy seed sync: web/i18n/*.json kept byte-identical to web/src/i18n/*.json via cp — prevents drift for any tool that references the legacy seed path (firebase.json, tooling)"
  - "React Router v7 import: from 'react-router' (not 'react-router-dom') — v7 merged dom into main package; react-router-dom is v6 legacy"
  - "RegistryPage stub keeps 'Registry {id}' as literal interpolation (not i18n key) — intentional, id is dynamic data not a translatable string; all other user-visible text uses t()"
  - "Plan 04 can safely use useParams<{ id: string }>(), useNavigate(), useSearchParams() from 'react-router' — router is configured in createBrowserRouter data mode"

patterns-established:
  - "Page stub pattern: minimal component using t() for all visible strings, with comment explaining which plan will replace the body"
  - "Router smoke test pattern: createMemoryRouter + RouterProvider + renderAt(path) helper — reusable across all page tests"

requirements-completed: [WEB-01]

duration: 2min
completed: 2026-04-19
---

# Phase 05 Plan 03: i18next + React Router v7 + Page Stubs Summary

**i18next initialized with LanguageDetector (localStorage 'lang' key), full UI-SPEC key set in en/ro, and React Router v7 Data Mode wired with 4 routes and localized page stubs — all giver UI routes unblocked.**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-19T18:01:06Z
- **Completed:** 2026-04-19T18:03:10Z
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments

- i18next configured with 37 keys per language (5 namespaces: app, common, auth, registry, reservation), browser locale auto-detection, and localStorage 'lang' persistence
- React Router v7 Data Mode wired with 4 routes: `/`, `/registry/:id`, `/reservation/:id/re-reserve`, catch-all `*`
- Four localized page stubs created — all text via t(), none hardcoded; Plans 04 and 07 can safely replace bodies
- 13 tests pass (5 i18n key/interpolation/fallback tests + 4 router smoke tests + 4 existing firebase tests)
- Legacy seed files at web/i18n/ synced byte-identical to web/src/i18n/

## Task Commits

Each task was committed atomically:

1. **Task 1: Move + extend i18n seeds and configure i18next with LanguageDetector** - `f52ae3e` (feat)
2. **Task 2: Wire React Router v7 + create page stubs + import i18n in main.tsx** - `980082f` (feat)

**Plan metadata:** (to be added by final commit)

## Files Created/Modified

- `web/src/i18n/en.json` — Full EN translation resource (37 keys)
- `web/src/i18n/ro.json` — Full RO translation resource (37 keys)
- `web/src/i18n/index.ts` — i18next singleton with LanguageDetector, react-i18next binding
- `web/src/components/LanguageSwitcher.tsx` — Toggle en/ro, persists to localStorage 'lang'
- `web/src/App.tsx` — createBrowserRouter with 4 routes (replaces scaffold stub)
- `web/src/main.tsx` — Added `import './i18n'` before `import App` for synchronous init
- `web/src/pages/AppRootPage.tsx` — Root `/` page stub
- `web/src/pages/RegistryPage.tsx` — `/registry/:id` stub (Plan 04 extends)
- `web/src/pages/ReReservePage.tsx` — `/reservation/:id/re-reserve` stub (Plan 07 extends)
- `web/src/pages/NotFoundPage.tsx` — Generic 404 (WEB-D-13/WEB-D-14 privacy posture)
- `web/src/__tests__/i18n.test.ts` — 5 tests: key resolution, RO switch, interpolation, localStorage persist, fallback
- `web/src/__tests__/App.test.tsx` — 4 route smoke tests using createMemoryRouter
- `web/i18n/en.json` — Synced to src/i18n/en.json (legacy seed, byte-identical)
- `web/i18n/ro.json` — Synced to src/i18n/ro.json (legacy seed, byte-identical)

## Decisions Made

- **i18n import order:** `import './i18n'` at line 6, `import App from './App'` at line 7 in main.tsx — synchronous init before any component mounts
- **Legacy seed sync:** `cp src/i18n/*.json i18n/` — byte-identical copies prevent drift; cheap operation, avoids breaking any tool referencing the legacy path
- **React Router v7 import surface:** `from 'react-router'` not `from 'react-router-dom'` — v7 merged the dom subpackage; all Plan 04+ hooks (useParams, useNavigate, useSearchParams) import from `'react-router'`
- **RegistryPage stub `Registry {id}`:** Dynamic data interpolation, not a translatable string — only i18n keys used for static UI text
- **Plan 04 contract:** Can safely use `useParams<{ id: string }>()`, `useNavigate()`, `useSearchParams()` from `'react-router'`

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required for this plan.

## Next Phase Readiness

- Plan 04 (RegistryPage implementation) can immediately use `useParams<{ id: string }>()` from `'react-router'` and replace the RegistryPage stub body
- Plan 07 (ReReservePage) can use `useNavigate()` and the `resolveReservation` callable to replace the ReReservePage stub
- All i18n keys required by Plans 04-07 are already present in both en.json and ro.json
- `t('reservation.banner_text', { itemName, minutes })` interpolation verified working
- No blockers for downstream plans

## Known Stubs

Two intentional stubs that downstream plans will resolve:

1. **web/src/pages/RegistryPage.tsx** — Body shows `Registry {id}` + `t('common.loading')`. Plan 04 replaces with RegistryHeader + ItemGrid + ReservationBanner. The stub is intentional and functional (router resolves, tests pass).

2. **web/src/pages/ReReservePage.tsx** — Body shows `t('reservation.resolving')` only. Plan 07 replaces with `resolveReservation` callable + redirect logic. The stub is intentional (comment in file documents the Plan 07 replacement).

These stubs do not prevent this plan's goal (routing unblocked, i18n keys available) — they are forward stubs for Plans 04 and 07.

## Self-Check: PASSED

---
*Phase: 05-web-fallback*
*Completed: 2026-04-19*
