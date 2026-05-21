# Phase 14: Web Fallback Live Deploy + Guest UAT - Context

**Gathered:** 2026-05-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Get the redesigned web bundle (Phase 13 output) live at `https://gift-registry-ro.web.app` against the production `gift-registry-ro` Firebase project, deploy backing Cloud Functions + Firestore rules + Storage rules, and close the 7 outstanding human-UAT items defined in `.planning/phases/05-web-fallback/05-VALIDATION.md` "Manual-Only Verifications" section.

**Scope anchor:** Ops + verification, not features. No new web behaviour, no new screens, no new requirements. Phase 14 is the last bridge between "shipped in code" and "shipped to real users" for the v1.1 web fallback story.

**Closure criteria:**
1. `https://gift-registry-ro.web.app/` renders the redesigned Phase 13 bundle (no blank page) — closes the immediate `register-firebase-web-app-and-deploy-real-web-config` todo
2. Backing Cloud Functions deploy succeeds without manual workarounds — closes the `fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy` todo
3. Firestore + Storage rules deployed to production (the Phase-12 storage.rules todo closes here too)
4. All 7 Phase-5 manual UAT items verified green in real Chrome + Safari against the production deploy
5. App Check enforced (after a brief monitor-only smoke-test window)

**Out of scope (carried from Phase 13 deferrals):**
- Screens 04 (Guest convert modal) + 05 (Expired re-reserve rich page) — net-new screens, not part of deploy
- SSR migration, slug routing, cookie sessions, OG image Cloud Function — architectural
- 4-occasion theme cascade — stays deferred to v1.2 per REQUIREMENTS.md THEME-01..03
- Reserver/giver name display — requires server-side projection

</domain>

<decisions>
## Implementation Decisions

### Deploy sequence & gating

- **D-01: Staged deploy across 4 plans.** Plan 14-01 = register Firebase Web app + `.env.local` + hosting-only deploy (fixes the blank page). Plan 14-02 = functions tsconfig full cleanup + functions deploy. Plan 14-03 = `firestore.rules` + `storage.rules` deploy. Plan 14-04 = layered UAT. Each plan independently smoke-tested and rollback-able. Rejected big-bang because a single-shot full deploy that half-succeeds leaves prod in an ambiguous state.
- **D-02: Per-checkpoint pause for manual Firebase-console ops.** I generate the exact `firebase apps:create WEB` / `firebase apps:sdkconfig web` commands and Firebase-console clicks (reCAPTCHA v3 site key registration, OAuth origin add) at the moment they're needed; user runs them and pastes the SDK config / site key back; execution resumes. Rejected doc-then-batch upfront because per-checkpoint verification prevents downstream errors.
- **D-03: Rollback strategy = `firebase hosting:rollback` + documented previous-known-good SHA.** Each plan's SUMMARY.md captures the previous-known-good commit SHA so a single command reverts the functions/rules deploy if needed. Hosting rollback is one-command via Firebase CLI built-in.

### App Check production posture

- **D-04: Monitor mode first, enforce after smoke-test.** reCAPTCHA v3 site key registered in Firebase console (manual) but App Check left in monitor-only mode for Firestore + Functions on first deploy. After confirming the appcheck exchange works in real Chrome, flip enforcement on for all three (Firestore, Functions, Storage). Avoids the all-or-nothing prod-outage risk of enforcing immediately.
- **D-05: Verify App Check via DevTools Network inspection.** Per Phase 5 validation row: Chrome DevTools → Network → filter `appcheck.googleapis.com` → confirm `appcheck:exchange` returns 200 BEFORE the first Firestore/Functions call. This is UAT item #1 closure evidence.
- **D-06: Google OAuth — add prod origin to Cloud Console + test in incognito.** Add `https://gift-registry-ro.web.app` to Authorized JavaScript origins + redirect URIs on the OAuth 2.0 client backing Firebase Auth. Verify by opening prod in an incognito window, clicking "Continue with Google", completing the popup with a real Google account. UAT item #7 closure evidence.

### UAT scope & gating

- **D-07: All 7 Phase-5 manual UAT items must pass before Phase 14 closes.** Items: App Check exchange (1), retailer redirect in Chrome+Safari (2), guest localStorage across browser restart (3), Romanian browser autodetect (4), private-registry rules-deny on cold deep-link (5), email re-reserve end-to-end (6), Google OAuth popup (7). Source of truth: `.planning/phases/05-web-fallback/05-VALIDATION.md` "Manual-Only Verifications" table. No deferrals — closes the milestone honestly.
- **D-08: Layered UAT — solo in incognito first, then with a recruited real giver.** Pass 1: user runs all 7 items themselves in fresh Chrome+Safari profiles, including a Romanian-system-language profile for item #4. Pass 2: user recruits a real giver friend to run the guest-reserve flow (items 2, 3 specifically) on their own device — catches "works on my machine" bias before declaring done.
- **D-09: Email-deep-link UAT (#6) — seed a near-expiry reservation via `firebase functions:shell`.** Connect functions shell to prod, run a one-shot script that creates a reservation with `expiresAt = now() + 60s` so Cloud Tasks fires release + expiry email in ~1 minute. Click the link from the inbox, verify re-reserve works. Faster than waiting 30 min and tests the exact production code path.

### Bundle scope & tooling fixes

- **D-10: Full functions tsconfig cleanup in Plan 14-02.** Apply the todo's option-1+3 combo: add `"rootDir": "src"` to `functions/tsconfig.json`, drop `"scripts"` from `include`, switch `npm run seed:stores` to use `ts-node` directly (no pre-compile), delete the committed-but-stale `functions/lib/` directory, commit `functions/.env` with the public default `PUBLIC_WEB_BASE_URL`. Future deploys "just work" from a fresh clone.
- **D-11: Fold both pending ops todos + the Phase-12 storage.rules todo into Phase 14 plans.** Plan 14-02 owns the functions tsconfig fix + functions deploy. Plan 14-03 owns `firestore.rules` + `storage.rules` deploy (closing the Phase-12 deferred-deploy todo). Keeps the production push atomic and the planning narrative cohesive. No standalone quick-tasks before Phase 14.
- **D-12: Verify SPA rewrite + cache headers explicitly post-deploy.** Plan 14-01 includes: confirm `firebase.json` SPA rewrite (`/** → /index.html`) is intact post-deploy by visiting a deep-link path; confirm cache headers are sensible (immutable for hashed JS/CSS, no-cache for `index.html`) — prevents stale-cache issues on subsequent deploys. Failure here would silently break private-registry rules-deny UAT (#5).

### Folded Todos

- **`2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md`** → Plan 14-01. Root cause of the blank-page bug at `gift-registry-ro.web.app`. Solution path in the todo is canonical: `firebase apps:create WEB` → `firebase apps:sdkconfig web` → write `web/.env.local` → rebuild → deploy hosting.
- **`2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md`** → Plan 14-02. Two issues: tsconfig produces wrong `lib/` layout (workaround: `tsc --rootDir src src/index.ts`); `defineString PUBLIC_WEB_BASE_URL` blocks non-interactive deploy (workaround: local-only `functions/.env`). Per D-10, full cleanup not minimum-fix.
- **`2026-04-28-deploy-phase-12-storage-rules.md`** → Plan 14-03. `storage.rules` authored in Phase 12 Plan 02 (commit `e979e45`) but never deployed — user deferred at UAT time. Phase 14's rules-deploy plan picks this up by including `--only storage` in the firebase deploy.

### Claude's Discretion

- Exact bash invocation of the near-expiry seed script (D-09) — TypeScript shell snippet vs adhoc node REPL
- Whether to add a release version stamp to the bundle for layered-UAT traceability (option-3 in the hosting question was not selected; can still be added if planner sees value)
- Format of the rollback runbook in each plan's SUMMARY — inline section vs separate file
- Whether Plan 14-04 (UAT) is one plan covering all 7 items or split into sub-plans per UAT item
- Specific Firebase-console screenshot capture / verification artefacts the user wants kept

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 14 closure criteria (the 7 manual UAT items)

- `.planning/phases/05-web-fallback/05-VALIDATION.md` — "Manual-Only Verifications" table (rows 90-97). The exact 7 items, the requirement each maps to, and the test instructions. **This is the closure spec for Phase 14.**

### Folded todos (concrete deploy instructions)

- `.planning/todos/pending/2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md` — Plan 14-01 source-of-truth. Contains exact CLI commands, the `web/.env.local` keys required, and the verification checklist.
- `.planning/todos/pending/2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` — Plan 14-02 source-of-truth. Contains the tsconfig options analysis, the `defineString` env friction explanation, and the recommended solution path.
- `.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md` — Plan 14-03 source-of-truth. Context on why the storage rules deploy was deferred during Phase 12 UAT and what it gates.

### Phase 5 locked decisions still in effect

- `.planning/phases/05-web-fallback/05-CONTEXT.md` §`<decisions>`
  - **WEB-D-17:** functions region pinned to `europe-west3` — every deploy must respect this
  - **WEB-D-18:** App Check uses reCAPTCHA v3 in production — Phase 14 operationalizes this
  - **WEB-D-15:** i18next locale auto-detection from browser — UAT item #4 closure depends on this working

### Phase 13 shipped state (what we're deploying)

- `.planning/phases/13-web-fallback-visual-refresh/13-CONTEXT.md` — full decision log for what's in the bundle (Housewarming palette only, 3 redesigned screens, sticky reservation banner, no SSR / no slug routing)

### Project-level state

- `.planning/PROJECT.md` — Current Milestone section. Note: "Giver-facing web fallback was added to v1.1 scope on 2026-04-30 (Phases 13 + 14)" — Phase 14 closure flips this from "live deploy + guest UAT pending" to validated.
- `.planning/REQUIREMENTS.md` — WEB-01..04 (already marked Complete in code; Phase 14 closes the parenthetical "pending real-browser UAT" caveat in PROJECT.md)

### Critical source files (planner / executor will touch these)

- `web/.env.example` — template for `web/.env.local` (NOT committed; created per Plan 14-01)
- `web/src/firebase.ts:10-17` — reads the `VITE_FIREBASE_*` env vars; will throw on `initializeApp({})` if env is empty
- `web/vite.config.ts:15` — outputs to `hosting/public/` (Phase 5 D-locked, do not change)
- `functions/tsconfig.json` — Plan 14-02 target
- `functions/package.json` — `"main": "lib/index.js"` declaration; tsconfig fix must keep this consistent
- `functions/src/config/publicUrls.ts` — `defineString` usage that gates non-interactive deploy
- `firebase.json` — hosting + functions + rules config; SPA rewrite lives here
- `firestore.rules` — production rules (notifications block from `260420-ozb` ships in this deploy)
- `storage.rules` — Phase 12 deferred-deploy target

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `web/src/firebase.ts` — Firebase init with `browserLocalPersistence` + `FUNCTIONS_REGION = 'europe-west3'` constant. **Do not modify the region pin** (WEB-D-17 locked).
- `web/.env.example` — documents every `VITE_FIREBASE_*` key needed. Plan 14-01 just needs to copy this to `.env.local` and fill in real values from `firebase apps:sdkconfig web`.
- `firebase.json` — hosting + functions + Firestore rules already wired. Adding `--only storage` to the deploy command picks up `storage.rules` because it's referenced in `firebase.json` per Phase 12 Plan 02 commit `e979e45`.
- `functions/lib/` — currently committed-but-stale per the tsconfig todo. Plan 14-02 deletes it as part of the cleanup.
- `firebase functions:shell` — built-in REPL connected to prod functions; Plan 14-04's email UAT (D-09) uses this to seed a 60s-expiry reservation.

### Established Patterns

- All Cloud Functions are 2nd-gen `firebase-functions/v2` (Phase 1 locked)
- `firebase deploy --only <target>` for staged deploys — already used by `260420-nh8` and others
- Local-only `functions/.env` for `defineString` defaults — current workaround that Plan 14-02 codifies by committing the file (per D-10)
- `web/public/` is the Vite static-asset source (NOT `hosting/public/` — that's the build output that Vite empties on each build per Phase 5 D-locked)

### Integration Points

- **Firebase console (manual):** Web app registration, reCAPTCHA v3 site key registration, App Check enforcement toggle, App Check metrics dashboard — all per-checkpoint pauses (D-02).
- **Google Cloud Console (manual):** OAuth 2.0 client → Authorized JavaScript origins + redirect URIs (D-06).
- **Firebase CLI:** Every actual deploy step. Version 13+ required for 2nd-gen Functions support (already in CLAUDE.md).
- **No client code changes expected.** Phase 14 is configuration + deploy + verification. If a UAT item exposes a real bug, it gets split out as a quick-task and Plan 14-04 logs a deferred item.

</code_context>

<specifics>
## Specific Ideas

- **Layered UAT (D-08) is the user's chosen safety net.** Solo pass catches obvious bugs in a controlled environment; recruited-giver pass catches "works on my machine" bias. Planner should write Plan 14-04 so the recruited-giver step is a clean handoff (clear instructions, no functions:shell dependency for the giver's items).
- **Per-checkpoint pause (D-02) is non-negotiable for shared-state changes.** Firebase Web app registration is a one-shot — if Claude accidentally fires it twice via `firebase apps:create`, the project ends up with two web apps. User wants to be the one running every `firebase apps:*` command and pasting back the output.
- **Monitor-mode App Check first (D-04) is the user-implicit reason for the staged deploy.** If enforcement-first broke prod, rollback is messy because App Check state lives in Firebase console, not in code. Monitor mode is the "safe default that we tighten later" posture.
- **The `defineString` env todo's option-1 recommendation (commit `functions/.env`) is canonical (D-10).** The URL isn't sensitive ("the URL isn't sensitive, and 'firebase deploy just works from a fresh clone' is worth more than conceptual purity" per the todo).

</specifics>

<deferred>
## Deferred Ideas

**Carried from Phase 13 (still deferred):**
- Screens 04 (Guest convert modal) + 05 (Expired re-reserve rich page) — net-new screens
- Reserver/giver name display (`Andrei P.`) — requires server-side projection
- SSR migration, slug routing (`/r/{slug}`), cookie sessions, OG image function
- 4-occasion theme cascade (THEME-01..03 → v1.2)
- 5-minute pre-expiry email reminder

**Surfaced during Phase 14 discussion but explicitly deferred:**
- Release version stamp / build marker in the bundle — discussed under hosting question; not selected. Can be picked up as a quick-task post-Phase-14 if traceability gaps surface during layered UAT.
- Monitoring / alerting (Firebase Performance, Crashlytics web SDK, Sentry) — not in scope for v1.1; ops decision for v1.2 milestone.
- Post-deploy smoke-test automation — Phase 14's UAT is manual by design (the 7 items are explicitly "Manual-Only Verifications"). Automating any of them is a v1.2 concern.

### Reviewed Todos (not folded)

- `2026-04-20-group-registries-by-ownership-and-clarify-invitee-permissions.md` — **Out of scope.** Android-only UI work (file paths all `app/src/main/java/.../Kt`). Matched on keyword overlap but unrelated to web deploy. Stays in the pending queue.
- `2026-04-27-curate-real-preset-jpegs-for-phase-12.md` — Out of scope (Phase 12 cleanup, not web).

</deferred>

---

## D-09 amendment (2026-05-21)

60s seed-script approach was abandoned during execution. Cloud Tasks API
requires gcloud Application Default Credentials, which weren't set up on the
dev machine. After a failed seed attempt left a phantom reservation in prod
that had to be manually cleaned via Firestore Console, the user opted to
verify UAT-6 via the natural 30-min path through the prod-pointed Android
app — same end-state, no extra tooling.

The replacement plan (see 14-04-UAT-RESULTS.md "UAT-6 plan — Natural 30-min
path") exercises the exact same deployed pipeline (Cloud Task → release →
expiry email → re-reserve link → new reservation) just with the natural
30-min delay instead of a compressed 60s delay. Seed script
`functions/scripts/seedNearExpiryReservation.ts` was reverted in commit
`1c970c4`. The `must_haves.truths` entry in
`14-04-layered-uat-and-appcheck-enforcement-PLAN.md` that referenced the
60s Cloud Task was rewritten to point at the natural-path verification.

---

*Phase: 14-web-fallback-live-deploy-guest-uat*
*Context gathered: 2026-05-20*
*D-09 amended: 2026-05-21*
