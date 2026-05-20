# Phase 14: Web Fallback Live Deploy + Guest UAT - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-20
**Phase:** 14-web-fallback-live-deploy-guest-uat
**Areas discussed:** Deploy sequence & gating, App Check production posture, UAT scope & gating, Bundle scope & tooling fixes

---

## Deploy sequence & gating

### Q1: Deploy strategy — staged or big-bang?

| Option | Description | Selected |
|--------|-------------|----------|
| Staged | 4 plans: hosting / functions / rules / UAT. Each independently smoke-tested + rollback-able | ✓ |
| Big-bang | One plan, single mega-deploy. Faster but ambiguous half-deploy risk | |
| Hybrid | Hosting standalone, then combined functions+rules+storage | |

**User's choice:** Staged (Recommended)
**Notes:** None — recommendation accepted.

### Q2: How should I surface the manual Firebase-console steps?

| Option | Description | Selected |
|--------|-------------|----------|
| Per-checkpoint pause | Generate commands at the moment needed, user runs, pastes back | ✓ |
| Doc-then-batch upfront | CHECKLIST.md of all manual ops, user runs all upfront | |
| You decide as we go | Default to per-checkpoint but pre-document the obvious ones | |

**User's choice:** Per-checkpoint pause (Recommended)
**Notes:** None.

### Q3: Rollback strategy?

| Option | Description | Selected |
|--------|-------------|----------|
| Hosting rollback + functions revert script | `firebase hosting:rollback` + documented prev-known-good SHA | ✓ |
| Git revert + redeploy | `git revert HEAD` + full rebuild + redeploy | |
| Forward-fix only | No pre-planned rollback; debug + fix forward | |

**User's choice:** Hosting rollback + functions revert script (Recommended)
**Notes:** Each plan's SUMMARY.md captures the previous-known-good commit SHA.

---

## App Check production posture

### Q1: App Check enforcement mode on first production deploy?

| Option | Description | Selected |
|--------|-------------|----------|
| Monitor mode first, enforce after smoke-test | reCAPTCHA registered, App Check monitor-only on first deploy, flip after verification | ✓ |
| Enforce immediately | Register + enforce in one motion; misconfig = immediate prod outage | |
| Defer App Check to v1.2 cleanup | Ship with App Check disabled; contradicts Phase 5 D-18 | |

**User's choice:** Monitor mode first, enforce after smoke-test (Recommended)
**Notes:** None.

### Q2: How do we verify the appcheck token exchange works (UAT item #1)?

| Option | Description | Selected |
|--------|-------------|----------|
| DevTools Network inspection | Filter `appcheck.googleapis.com`, confirm 200 before Firestore/Functions calls | ✓ |
| DevTools + Firebase console App Check metrics | Above PLUS server-side metrics dashboard confirmation 5+ min later | |
| Smoke functional test only | Skip network inspection; if Reserve/Auth work, App Check works implicitly | |

**User's choice:** DevTools Network inspection (Recommended)
**Notes:** Matches the exact Phase 5 validation row instructions.

### Q3: Google OAuth + production origin?

| Option | Description | Selected |
|--------|-------------|----------|
| Add prod origin to OAuth client + test in incognito | Cloud Console → OAuth 2.0 client → add JS origin + redirect URI; verify in clean session | ✓ |
| Use Firebase-managed OAuth | Trust default authDomain config; only intervene if popup fails | |

**User's choice:** Add prod origin to OAuth client + test in incognito (Recommended)
**Notes:** None.

---

## UAT scope & gating

### Q1: What's the closure bar — all 7 manual UAT items or a subset?

| Option | Description | Selected |
|--------|-------------|----------|
| All 7 must pass | App Check, retailer redirect (Chrome+Safari), guest localStorage, RO autodetect, private-deny, email re-reserve, OAuth | ✓ |
| Critical 5 pass, defer 2 | Defer Romanian autodetect + email re-reserve to follow-up | |
| You decide as we hit each one | Discover-as-we-go per-item pass/defer | |

**User's choice:** All 7 must pass (Recommended)
**Notes:** Closes the milestone honestly.

### Q2: Who runs the UAT — you alone or with a real giver?

| Option | Description | Selected |
|--------|-------------|----------|
| You alone in incognito/clean browser sessions | Solo across Chrome+Safari profiles, RO-language profile, real Google account | |
| You + a real recruited giver | You handle ops items, recruit friend for guest-reserve items | |
| **Other (free text)** | "First will do alone in incognito / clean browser sessions. Then I'll do it with a real giver recruited for tests." | ✓ |

**User's choice:** Layered — solo in incognito first, THEN with a recruited real giver
**Notes:** Best-of-both: solo catches obvious bugs in controlled env; recruited-giver catches "works on my machine" bias. Captured as D-08.

### Q3: Email-deep-link UAT (#6) — how do we test in production?

| Option | Description | Selected |
|--------|-------------|----------|
| Seed a near-expiry reservation via Cloud Functions shell | `firebase functions:shell` against prod → create reservation with expiresAt=now+60s | ✓ |
| Wait the full 30 minutes | Reserve real item, wait 30 min, click email link | |
| Defer email UAT to a follow-up | Open a quick-task with steps, ship Phase 14 without item #6 | |

**User's choice:** Seed a near-expiry reservation via Cloud Functions shell (Recommended)
**Notes:** Faster than waiting 30 min, exact production code path.

---

## Bundle scope & tooling fixes

### Q1: Functions tsconfig fix — minimum or full cleanup?

| Option | Description | Selected |
|--------|-------------|----------|
| Full cleanup | rootDir=src, drop scripts from include, ts-node for seed, delete stale lib/, commit functions/.env | ✓ |
| Minimum-to-unblock | Just commit functions/.env, document the tsc workaround in package.json | |
| Workaround now, full cleanup later | Same as minimum but open a todo for the full cleanup | |

**User's choice:** Full cleanup (Recommended)
**Notes:** Future deploys "just work" from a fresh clone.

### Q2: Bundle the open ops todos into Phase 14, or ship as quick-tasks first?

| Option | Description | Selected |
|--------|-------------|----------|
| Fold both into Phase 14 plans | 14-02 = tsconfig+functions deploy; 14-03 = firestore+storage rules deploy | ✓ |
| Ship tsconfig + storage.rules as quick-tasks first | Atomic Phase 14 but more session-switching | |
| Bundle tsconfig only; storage.rules stays separate | Critical-path-only into Phase 14 | |

**User's choice:** Fold both into Phase 14 plans (Recommended)
**Notes:** Keeps production push atomic and planning narrative cohesive.

### Q3: Hosting / SPA-rewrite concerns to validate?

| Option | Description | Selected |
|--------|-------------|----------|
| Verify SPA rewrite + cache headers | Explicit check post-deploy that /** → /index.html and cache headers sensible | ✓ |
| Hosting is fine as-is, just deploy | Trust Phase 5's firebase.json, verify only by visiting a deep-link | |
| Add a release marker / version stamp | Build-time version stamp visible in console/footer for traceability | |

**User's choice:** Verify SPA rewrite + cache headers (Recommended)
**Notes:** Release version stamp noted as deferred — can be added as quick-task post-Phase-14 if layered UAT exposes traceability gaps.

---

## Claude's Discretion

- Exact bash invocation for the near-expiry seed script
- Whether to add a release version stamp (option-3 in hosting Q3 was not selected)
- Format of the rollback runbook in each plan's SUMMARY
- Whether Plan 14-04 (UAT) is one plan or split per UAT item
- Specific Firebase-console screenshot/verification artefacts the user wants kept

## Deferred Ideas

**Carried from Phase 13:**
- Screens 04 + 05 (net-new screens)
- Reserver/giver name display (server-side projection needed)
- SSR migration, slug routing, cookie sessions, OG image function
- 4-occasion theme cascade (THEME-01..03 → v1.2)
- 5-minute pre-expiry email reminder

**Surfaced in Phase 14 discussion:**
- Release version stamp / build marker in the bundle
- Monitoring / alerting (Firebase Performance, Crashlytics web SDK, Sentry)
- Post-deploy smoke-test automation
