---
phase: 5
slug: web-fallback
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-19
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution. Web-side only — Android tests are covered in prior phases. Cloud Functions are unchanged in this phase; functions tests already exist in prior phases.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest 2.x + React Testing Library 16.x + jsdom 25.x |
| **Config file** | `web/vitest.config.ts` (Wave 0 installs) |
| **Quick run command** | `cd web && npm test -- --run` |
| **Full suite command** | `cd web && npm test -- --run --coverage` |
| **Estimated runtime** | ~15 seconds for unit/component; ~60 seconds for full suite w/ coverage |

Supplementary: end-to-end validation via Firebase Emulator Suite exercising the giver → reserve → redirect flow lives under `web/e2e/` (Playwright 1.x), run via `cd web && npm run e2e`.

---

## Sampling Rate

- **After every task commit:** Run `cd web && npm test -- --run --changed`
- **After every plan wave:** Run `cd web && npm test -- --run`
- **Before `/gsd:verify-work`:** Full suite + e2e both green
- **Max feedback latency:** 15 seconds (unit/component)

---

## Per-Task Verification Map

> IDs follow the `{phase}-{plan}-{task}` convention. Filled in by the planner; status updated by the executor.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 5-01-* | 01 (scaffold) | 1 | n/a (infra) | build | `cd web && npm run build` | ❌ W0 | ⬜ pending |
| 5-02-* | 02 (firebase init) | 1 | WEB-01/02/03/04 (pre-req) | unit | `cd web && npm test -- firebase` | ❌ W0 | ⬜ pending |
| 5-03-* | 03 (i18n + router) | 1 | WEB-01 | unit+component | `cd web && npm test -- i18n router` | ❌ W0 | ⬜ pending |
| 5-04-* | 04 (registry view) | 2 | WEB-01 | component | `cd web && npm test -- RegistryPage ItemCard` | ❌ W0 | ⬜ pending |
| 5-05-* | 05 (auth + guest) | 2 | WEB-03 | component | `cd web && npm test -- AuthModal GuestIdentity` | ❌ W0 | ⬜ pending |
| 5-06-* | 06 (reservation) | 3 | WEB-02, WEB-04 | component+e2e | `cd web && npm test -- ReserveButton ReservationBanner && npm run e2e -- reserve` | ❌ W0 | ⬜ pending |
| 5-07-* | 07 (re-reserve link) | 3 | WEB-02 | component+e2e | `cd web && npm test -- ReReservePage && npm run e2e -- re-reserve` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

Planner may re-partition plans — the table must be updated to match the final plan breakdown before execution.

---

## Wave 0 Requirements

- [ ] `web/package.json` — Vitest 2.x, React Testing Library 16.x, jsdom 25.x, Playwright 1.x as devDependencies
- [ ] `web/vitest.config.ts` — jsdom env, setupFiles pointing to `web/src/test/setup.ts`
- [ ] `web/src/test/setup.ts` — Firebase emulator connect, App Check debug token, i18next init in test mode
- [ ] `web/src/test/fakeFirestore.ts` — in-memory Firestore fake for component tests that don't need the full emulator
- [ ] `web/playwright.config.ts` — webServer pointing to Vite dev server + Firebase emulator; baseURL `http://localhost:5002`
- [ ] `web/e2e/fixtures.ts` — seeded registry + item fixtures (public registry, private registry, available item, reserved item)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| reCAPTCHA v3 App Check token acquisition in production | WEB-02, WEB-03 | reCAPTCHA v3 cannot run in jsdom or CI without a real site key and a real browser DOM; emulator uses debug token | Deploy to Firebase Hosting preview channel, open in real Chrome, check DevTools Network panel for `appcheck.googleapis.com/v1/projects/*/apps/*/appcheck:exchange` succeeding before first Firestore or Functions call |
| Retailer redirect opens in a new tab and keeps the registry tab alive | WEB-04 | Real browser `window.open` behavior varies by pop-up blocker settings; jsdom stub does not replicate this | Click Reserve on a live registry in Chrome and Safari, verify affiliateUrl opens in a new tab AND the registry tab still shows the countdown banner afterwards |
| Guest localStorage persistence across browser restart | WEB-03 | localStorage behavior is mockable in jsdom but real cross-session persistence requires a real browser | Reserve as guest, close the browser completely, re-open `/registry/:id`, click Reserve, confirm the modal pre-fills first name / last name / email |
| Language auto-detection from browser locale on cold load | WEB-D-15 | jsdom fixes `navigator.language` at test time; real-world detection depends on OS language and Accept-Language headers | Set system language to Romanian, open the site in a clean profile, verify Romanian strings render |
| SPA deep-link on cold browser with private registry rules-deny | WEB-04 | Firebase Auth session restore + Firestore rules eval + 404 render is a composition test that is easier to validate end-to-end visually | Paste `https://<host>/registry/<private-id>` in a new private window without being logged in; expect the generic "Registry not available" page |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags (Vitest `--run` mandatory)
- [ ] Feedback latency < 15s (unit/component)
- [ ] `nyquist_compliant: true` set in frontmatter when planner finalizes per-task rows

**Approval:** pending
