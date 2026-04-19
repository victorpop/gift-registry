---
phase: 5
slug: web-fallback
status: planned
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-19
updated: 2026-04-19
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution. Web-side only — Android tests are covered in prior phases. Cloud Functions are unchanged in this phase; functions tests already exist in prior phases.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 2.x + React Testing Library 16.x + jsdom 25.x |
| **Config file** | `web/vitest.config.ts` (Plan 01 scaffolds) |
| **Test setup** | `web/src/test/setup.ts` (Plan 01 scaffolds — imports jest-dom, stubs matchMedia/ResizeObserver, clears localStorage) |
| **Quick run command** | `cd web && npm run test:run` |
| **Focused run** | `cd web && npm run test:run -- <name-fragment>` |
| **Typecheck** | `cd web && npm run typecheck` |
| **Build (deploys to hosting/public)** | `cd web && npm run build` |
| **Estimated runtime (unit/component suite)** | ~15 seconds |

Supplementary: end-to-end validation via Firebase Emulator Suite + Playwright at `web/e2e/`. Playwright config scaffolded in Plan 01; e2e specs are out of scope for this phase's unit validation (manual/CI run: `cd web && npm run e2e`).

---

## Sampling Rate

- **After every task commit:** Run `cd web && npm run test:run` (full unit/component suite — ~15s)
- **After every plan wave:** Same command (no incremental-only mode; suite is small enough to run fully)
- **Before `/gsd:verify-work`:** `npm run typecheck && npm run test:run && npm run build` all green
- **Max feedback latency:** ~15 seconds (unit/component)

---

## Per-Task Verification Map

> IDs follow the `{phase}-{plan}-{task}` convention. Every row has an automated command or a Wave 0 scaffold dependency. No task is verify-free.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-1 | 01 (scaffold: deps + tsconfig + index) | 0 | n/a (infra) | build | `cd web && npm run typecheck` | W0 | pending |
| 5-01-2 | 01 (scaffold: vite + vitest + tailwind + playwright configs) | 0 | n/a (infra) | build | `cd web && npm run typecheck && npm run test:run && npm run build` | Produced by 5-01-1 | pending |
| 5-02-1 | 02 (firebase init) | 1 | WEB-01/02/03/04 (precondition) | unit | `cd web && npm run test:run -- firebase` | Produced by 5-02-1 | pending |
| 5-02-2 | 02 (main.tsx App Check + QueryClientProvider) | 1 | WEB-01/02/03/04 (precondition) | build+grep | `cd web && npm run typecheck && npm run test:run && npm run build` | Produced by 5-02-2 | pending |
| 5-03-1 | 03 (i18n setup + resource merge) | 1 | WEB-01 | unit | `cd web && npm run test:run -- i18n` | Produced by 5-03-1 | pending |
| 5-03-2 | 03 (router + page stubs) | 1 | WEB-01 | component | `cd web && npm run test:run -- App` | Produced by 5-03-2 | pending |
| 5-04-1 | 04 (firestore mapping + query hooks) | 2 | WEB-01, WEB-04 | unit | `cd web && npm run test:run -- useRegistryQuery useItemsQuery` | Produced by 5-04-1 | pending |
| 5-04-2 | 04 (RegistryHeader + ItemCard + Skeleton + Grid) | 2 | WEB-01 | component | `cd web && npm run test:run -- ItemCard` | Produced by 5-04-2 | pending |
| 5-04-3 | 04 (RegistryPage wiring + skeleton + 404 mapping) | 2 | WEB-01, WEB-04 | component | `cd web && npm run test:run -- RegistryPage` | Produced by 5-04-3 | pending |
| 5-05-1 | 05 (authProviders + useAuth + useGuestIdentity) | 2 | WEB-03 | unit | `cd web && npm run test:run -- useAuth useGuestIdentity` | Produced by 5-05-1 | pending |
| 5-05-2 | 05 (AuthModal + GuestIdentityModal) | 2 | WEB-03 | component | `cd web && npm run test:run -- AuthModal GuestIdentityModal` | Produced by 5-05-2 | pending |
| 5-06-1 | 06 (error-mapping + useCreateReservation + useCountdown + useActiveReservation) | 3 | WEB-02, WEB-04 | unit | `cd web && npm run test:run -- useCreateReservation useCountdown error-mapping` | Produced by 5-06-1 | pending |
| 5-06-2 | 06 (ToastProvider + ReserveButton + ReservationBanner + RegistryPage wiring) | 3 | WEB-02, WEB-04 | component | `cd web && npm run test:run -- ReserveButton` | Produced by 5-06-2 | pending |
| 5-07-1 | 07 (useResolveReservation + ReReservePage) | 3 | WEB-02 | unit+component | `cd web && npm run test:run -- useResolveReservation ReReservePage` | Produced by 5-07-1 | pending |
| 5-07-2 | 07 (autoReserveItemId handler in RegistryPage) | 3 | WEB-02 | component | `cd web && npm run test:run -- RegistryPage.autoReserve` | Produced by 5-07-2 | pending |

*Status: pending · green · red · flaky*

Every row has an automated command. Every command is CI-friendly (no watch flags, no interactive prompts). Sampling continuity rule is satisfied (no 3 consecutive tasks without automated verify — every single task has one).

---

## Wave 0 Requirements (scaffolded in Plan 01)

- [ ] `web/package.json` — Vitest 2.x, RTL 16.x, jsdom 25.x, Playwright 1.x as devDependencies pinned
- [ ] `web/vitest.config.ts` — jsdom env, setupFiles `./src/test/setup.ts`
- [ ] `web/src/test/setup.ts` — `@testing-library/jest-dom`, matchMedia stub, ResizeObserver stub, afterEach cleanup + localStorage clear
- [ ] `web/src/test/fakeFirestore.ts` — in-memory Firestore fake for component tests
- [ ] `web/playwright.config.ts` — webServer pointing at Vite dev server + Firebase emulator on port 5002
- [ ] `web/e2e/fixtures.ts` — seeded registry + item fixtures (public/private, available/reserved)
- [ ] `hosting/public/.gitkeep` — ensures Firebase Hosting deploy root exists pre-build

All the above live inside Plan 01 Task 1 or Task 2 — they are the foundation every subsequent plan's tests rely on.

---

## Manual-Only Verifications

These cannot be automated in Vitest + jsdom. Executed by a human after phase completion and before `/gsd:verify-work`.

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| reCAPTCHA v3 App Check token acquisition in production | WEB-02, WEB-03 | reCAPTCHA v3 cannot run in jsdom or CI without a real site key and a real browser DOM; emulator uses debug token | Deploy to Firebase Hosting preview channel, open in real Chrome, check DevTools Network panel for `appcheck.googleapis.com/v1/projects/*/apps/*/appcheck:exchange` succeeding before first Firestore or Functions call |
| Retailer redirect opens in a new tab and keeps the registry tab alive | WEB-04 | Real browser `window.open` behavior varies by pop-up blocker settings; jsdom stub does not replicate this | Click Reserve on a live registry in Chrome and Safari, verify affiliateUrl opens in a new tab AND the registry tab still shows the countdown banner afterwards |
| Guest localStorage persistence across browser restart | WEB-03 | localStorage behavior is mockable in jsdom but real cross-session persistence requires a real browser | Reserve as guest, close the browser completely, re-open `/registry/:id`, click Reserve, confirm the modal pre-fills first name / last name / email |
| Language auto-detection from browser locale on cold load | WEB-D-15 | jsdom fixes `navigator.language` at test time; real-world detection depends on OS language and Accept-Language headers | Set system language to Romanian, open the site in a clean profile, verify Romanian strings render |
| SPA deep-link on cold browser with private registry rules-deny | WEB-04 | Firebase Auth session restore + Firestore rules eval + 404 render is a composition test that is easier to validate end-to-end visually | Paste `https://<host>/registry/<private-id>` in a new private window without being logged in; expect the generic "Registry not available" page |
| Email deep-link re-reserve end-to-end | WEB-02, RES-08 | Requires a real reservation record aged to expiry + an email link — easier to verify visually with a seeded test record | After Phase 6 ships the email, manually trigger expiry, click the link, verify auto-reserve occurs |
| Google OAuth popup flow | WEB-03, AUTH-03 | `signInWithPopup` spawns a real OAuth popup window — cannot be replicated in jsdom beyond asserting the call was made | Click "Continue with Google" on a deployed build, complete the OAuth flow in a real Google account, confirm user lands back on registry signed in |

---

## Validation Sign-Off

- [x] All 15 tasks have `<automated>` verify commands (every row above has a concrete command)
- [x] Sampling continuity: every task has an automated command — no 3-in-a-row gap
- [x] Wave 0 (Plan 01) covers all test infrastructure referenced downstream
- [x] No watch-mode flags (`test:run` is the non-watch script; `test` is watch-mode, not used in CI)
- [x] Feedback latency < 15s (unit/component suite is small)
- [x] `nyquist_compliant: true` in frontmatter

**Approval:** planner — 2026-04-19
