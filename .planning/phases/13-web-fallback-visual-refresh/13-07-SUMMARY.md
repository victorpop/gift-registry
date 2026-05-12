---
phase: 13-web-fallback-visual-refresh
plan: 07
subsystem: ui
tags: [react, tailwind, giftmaison, vitest, i18next, radix-dialog, radix-toast, 404, re-reserve, guest-identity, toast, regression-sweep]

# Dependency graph
requires:
  - phase: 13-web-fallback-visual-refresh-00
    provides: Design tokens (gm.*/--gm-* CSS custom properties), font-display/body/mono families, rounded-gm-card/modal, shadow-gm-modal, animate-gm-pulse keyframe
  - phase: 13-web-fallback-visual-refresh-01
    provides: Btn / Field / MonoCaption / Pill / PulseDot / Wordmark atoms
  - phase: 13-web-fallback-visual-refresh-02
    provides: TopNav / Footer / StickyMobileBar chrome
  - phase: 13-web-fallback-visual-refresh-03
    provides: All web_* i18n keys (auth / hero / reserve / pill / nav / footer)
  - phase: 13-web-fallback-visual-refresh-04
    provides: RegistryPage + ItemCard + FilterChips + ProgressStrip restyle
  - phase: 13-web-fallback-visual-refresh-05
    provides: StickyReserveBanner + ReserveDetailSection + HowTimerWorks
  - phase: 13-web-fallback-visual-refresh-06
    provides: AuthScreen + AuthModal + GuestSkipCard + EditorialPhoto restyle

provides:
  - ReReservePage restyled — bg-gm-paper + accent pulsing dot (animate-gm-pulse) + gm-inkSoft body; all useEffect/hasFiredRef/mutation logic preserved byte-identical
  - NotFoundPage restyled — TopNav + Footer chrome + Display L heading + Body L body + ghost Btn "Back" CTA (new — Phase 5 had no CTA)
  - GuestIdentityModal restyled — bg-gm-paper rounded-gm-modal shell + 3 Field atoms + primary Btn; full API preserved (open/onOpenChange/onSubmit/useGuestIdentity/form.reset on open)
  - ToastProvider restyled — bg-gm-paper/text-gm-ink/border-gm-line card with variant-coloured left border (success=accent, error=warn, neutral=line); rounded-gm-card; all Phase 5 legacy tokens replaced
  - Test re-baseline: 111 tests green (up from 92 in Phase 5); ItemCard + RegistryPage tests rewritten against Phase 13 Pill/PulseDot/MonoCaption atom contract; Playwright e2e no spec files (fixtures only) — documented
  - Phase 13 closes — every giver-facing surface has Phase 13 GiftMaison styling

affects: [14-web-fallback-live-deploy, future-occasion-cascade]

# Tech tracking
tech-stack:
  added: []  # No new dependencies; all libraries from prior plans
  patterns:
    - "data-status attribute testing pattern: ItemCard test asserts data-status='available|reserved|purchased' on the article wrapper + uppercase i18n copy ('AVAILABLE' / 'RESERVED' / 'PURCHASED') instead of legacy Phase 5 CSS class references (bg-surface-variant / bg-primary / bg-surface-on)"
    - "GuestIdentityModal direct barrel import acceptable — the file itself has no jsdom test in plan 13-07 scope that would crash; existing GuestIdentityModal.test.tsx was already firebase-mock-free (only probes form behaviour, not atom rendering)"
    - "Playwright e2e no-op pattern: web/e2e/ has fixtures.ts only, no spec files; npm run e2e exits 0 via empty test set — future specs can be added in Phase 14"

key-files:
  created: []  # No new files created; all work is restyle + test re-baseline
  modified:
    - web/src/pages/ReReservePage.tsx (token-pass restyle — JSX return only; logic preserved)
    - web/src/pages/NotFoundPage.tsx (full visual rewrite — same copy contract; adds TopNav/Footer + Back CTA)
    - web/src/features/auth/GuestIdentityModal.tsx (full visual rewrite — API preserved; Field atoms replace inline labels)
    - web/src/components/ToastProvider.tsx (token-pass restyle — API + behaviour unchanged; all Phase 5 tokens replaced)
    - web/src/features/registry/__tests__/ItemCard.test.tsx (4 tests rewritten against Phase 13 contract)
    - web/src/features/registry/__tests__/RegistryPage.test.tsx (1 test updated for Phase 13 split-node occasion+date)
    - web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx (re-baselined from Phase 5 class assertions)

key-decisions:
  - "ReReservePage restyle: preserve every line of useEffect / hasFiredRef / mutation logic byte-identical; only the visible JSX return changes (bg-gm-paper + animate-gm-pulse dot + gm-inkSoft body text)"
  - "NotFoundPage gains Back CTA (navigate('/')) — Phase 5 had no CTA; ghost Btn 'Back' is a usability improvement that does not alter the page's security model (no distinction between 404 / private / deleted — intentional enumeration safety from Phase 5 D-13/D-14)"
  - "GuestIdentityModal imports atoms from the barrel ('../../components/giftmaison') — acceptable because the jsdom test file (GuestIdentityModal.test.tsx) was already firebase-free from Phase 5 (it probes form submission / validation, not atom rendering). The test did NOT need the firebase mock pattern used in AuthModal.test.tsx"
  - "ToastProvider variant-coloured left border: success→border-l-gm-accent (terracotta), error→border-l-gm-warn (warm amber), neutral→border-l-gm-line (faint divider). This is a UX improvement over Phase 5's bg-primary/bg-destructive full-card colouring — keeps the card legible while still communicating severity"
  - "Test re-baseline strategy: behaviour assertions updated to match new contract (data-status attribute + uppercase i18n copy) rather than CSS class assertions — more resilient to future styling changes while still exercising the actual contract"
  - "Playwright e2e exits 0 with no spec files — this is expected; web/e2e/fixtures.ts is a data scaffold for future Phase 14 spec authoring. Documented in SUMMARY to avoid future confusion"
  - "Test count grew from 92 to 111 due to quick-task additions (260510-o7w, 260510-oja, 260510-noi) — all 111 pass; the 92 → 111 delta is additive correctness"

patterns-established:
  - "Pattern 1: data-status attribute contract — ItemCard sets data-status on the article wrapper; tests assert this attribute + uppercase i18n copy (not CSS class assertions) to survive future styling refactors"
  - "Pattern 2: ToastProvider left-border severity pattern — bg-gm-paper card with variant-specific border-l-4 left accent (gm-accent / gm-warn / gm-line) rather than full-card colouring; preserves legibility in both success and error states on the paper bg"

requirements-completed: [D-03]

# Metrics
duration: 10min
completed: 2026-05-12
---

# Phase 13 Plan 07: Final Polish + Regression Sweep Summary

**Wave 5 close: GuestIdentityModal, ToastProvider, ReReservePage, NotFoundPage all restyled with GiftMaison tokens; 111 Vitest tests green including re-baselined ItemCard + RegistryPage contract assertions.**

## Performance

- **Duration:** ~10 min (Tasks 1-3 committed; Task 4 = human-UAT approved 2026-05-12)
- **Started:** 2026-05-07T23:30:00Z (per commit c640a21 timestamp)
- **Completed:** 2026-05-12
- **Tasks:** 4 of 4 complete (Tasks 1-3 auto; Task 4 human-UAT approved by user)
- **Files modified:** 7 (4 production files restyled, 3 test files re-baselined)

## Accomplishments

- All 4 remaining surfaces restyled: ReReservePage (paper bg + accent pulsing dot), NotFoundPage (TopNav/Footer + Display L + Back CTA), GuestIdentityModal (paper modal shell + Field atoms), ToastProvider (paper card + left-border severity)
- 111 Vitest tests green — 5 deferred-from-Plan-13-04 failures (ItemCard x4 + RegistryPage x1) resolved with data-status attribute + uppercase i18n copy assertions replacing obsolete Phase 5 CSS class checks
- Build clean (1.05 MB JS / 289 KB gzipped); typecheck exits 0; no Phase 5 legacy tokens (`bg-surface`, `surface-on*`, `border-outline`) anywhere in production `web/src/`
- Phase 13 visual refresh complete pending human UAT sign-off

## Task Commits

1. **Task 1: Restyle ReReservePage + NotFoundPage** - `c640a21` (feat)
2. **Task 2: Restyle GuestIdentityModal + ToastProvider** - `dd8a6c1` (feat)
3. **Task 3: Full Vitest regression sweep + re-baseline** - `8bf7abf` (test)
4. **Task 4: Human UAT checkpoint** — APPROVED (user sign-off 2026-05-12; all 11 verification sections passed)

**Plan metadata commit:** 1ed40bd (initial doc commit) + close commit (this continuation)

## Files Created/Modified

### Modified

- `web/src/pages/ReReservePage.tsx` — Token-pass restyle; JSX return replaced with paper bg + accent pulsing dot + gm-inkSoft body. All useEffect / hasFiredRef / mutation logic preserved byte-identical.
- `web/src/pages/NotFoundPage.tsx` — Full visual rewrite; TopNav + Footer chrome added, Display L heading (font-display 28/36/44 px), Body L body (gm-inkSoft), ghost Btn "Back" CTA (new vs Phase 5). Same i18n keys (registry.not_found_title / registry.not_found_body / common.back).
- `web/src/features/auth/GuestIdentityModal.tsx` — Full visual rewrite preserving Phase 5 API (open / onOpenChange / onSubmit / useGuestIdentity / form.reset on open / same zod schema). 3 inline `<label>` blocks replaced with 3 Field atoms. Paper modal shell: rounded-gm-modal + shadow-gm-modal + bg-gm-ink/40 backdrop-blur. Primary Btn submit full-width.
- `web/src/components/ToastProvider.tsx` — Token-pass restyle. All Phase 5 tokens removed (bg-surface, text-surface-on, bg-primary, text-primary-on, bg-destructive, border-outline, rounded-md). Replaced with bg-gm-paper / text-gm-ink / border-gm-line / rounded-gm-card. Variant-coloured left border: success=border-l-gm-accent, error=border-l-gm-warn, neutral=border-l-gm-line.
- `web/src/features/registry/__tests__/ItemCard.test.tsx` — 4 tests rewritten: 'renders title, price' now uses data-testid="price"; status pill tests assert data-status attribute + uppercase i18n copy instead of legacy CSS class names.
- `web/src/features/registry/__tests__/RegistryPage.test.tsx` — 1 test updated; Phase 13 splits occasion + date across two DOM nodes (Pill 'WEDDING' + MonoCaption), so the legacy combined regex fails; test now asserts each independently.
- `web/src/features/reservation/__tests__/ConfirmPurchaseBanner.test.tsx` — Re-baselined from Phase 5 class assertions to GiftMaison Btn atom assertions (included in Task 1 commit as part of the regression sweep at that stage).

## Decisions Made

See frontmatter `key-decisions` array — 7 decisions covering: ReReservePage byte-identical logic preservation, NotFoundPage Back CTA UX rationale, GuestIdentityModal barrel-import decision, ToastProvider left-border severity pattern, test assertion strategy, Playwright no-op documentation, test count growth from quick tasks.

## Deviations from Plan

None — plan executed exactly as written. All 4 file restyles match the plan's code snippets verbatim. The 5 pre-existing test failures (deferred from Plan 13-04, logged in `deferred-items.md` by Plan 13-06) were the exact failures Task 3 was designed to address. The Playwright e2e suite having no spec files was noted in the test(13-07) commit and is documented as expected.

## Issues Encountered

- **Playwright e2e no spec files:** `npm --prefix web run e2e` exits 0 via an empty test set (only `web/e2e/fixtures.ts` exists). This is correct — the fixtures are a scaffold for Phase 14 spec authoring. Documented to prevent future confusion. Plan 13-07 considers this acceptable per the plan's stated scope ("if any e2e fails on a CSS selector... update the selector" — no specs to fail).
- **Test count 92 → 111:** The 19 new tests were added in quick tasks (260510-o7w: 4 tests, 260510-oja: 3 tests, 260510-noi: 1 test; plus existing tests from prior quick tasks). All 111 pass. The plan's "92 existing tests" reference reflects the Phase 13 start count; the current suite is larger due to in-flight quick-task commits on main.

## User Setup Required

None — no external service configuration required. Human-UAT checkpoint (Task 4) is the only pending step.

## Known Stubs

None — no UI stubs created in this plan. All 4 restyled components consume real data from their existing hooks (useResolveReservation, useTranslation, useGuestIdentity, showToast API).

## Next Phase Readiness

- **Phase 14 (web deploy + production UAT):** Phase 13 visual refresh is complete. All giver-facing surfaces have Phase 13 GiftMaison styling. Build is clean. No Phase 5 tokens remain in production source.
- **UAT result:** Human approved 2026-05-12 — all 11 checklist sections passed. No blocking issues. No follow-up visual todos raised. Phase 13 is shippable.

## Self-Check: PASSED

- [x] `web/src/pages/ReReservePage.tsx` restyled (commit c640a21 — grep confirms bg-gm-paper + animate-gm-pulse + hasFiredRef present)
- [x] `web/src/pages/NotFoundPage.tsx` restyled (commit c640a21 — grep confirms TopNav + Footer + font-display + registry.not_found_title)
- [x] `web/src/features/auth/GuestIdentityModal.tsx` restyled (commit dd8a6c1 — grep confirms import Btn/Field from giftmaison barrel + bg-gm-paper rounded-gm-modal + shadow-gm-modal + useGuestIdentity)
- [x] `web/src/components/ToastProvider.tsx` restyled (commit dd8a6c1 — grep confirms bg-gm-paper + border-gm-line + rounded-gm-card; no Phase 5 tokens)
- [x] All 3 task commits exist on main: c640a21, dd8a6c1, 8bf7abf
- [x] `npm --prefix web run test:run` → 111 passed (22 test files)
- [x] `npm --prefix web run typecheck` → exits 0
- [x] `npm --prefix web run build` → exits 0 (1.05 MB JS / 289 KB gzipped)
- [x] `grep -rE "bg-surface|surface-on|surface-onVariant|surface-variant|border-outline" web/src/` → 0 matches in production components

---
*Phase: 13-web-fallback-visual-refresh*
*Plan: 07*
*Completed: 2026-05-12 (UAT approved)*
