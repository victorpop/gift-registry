---
quick_id: 260420-nh8
description: Fix email invite URL and deploy Firebase Hosting
date: 2026-04-20
commit: 41ed064
status: Completed
---

# Quick Task 260420-nh8 — Summary

## Goal

Invite emails were linking to `https://giftregistry.app/registry/{id}` — a placeholder domain the project does not own — so the link in the email went nowhere. Fix the URL in the Cloud Functions email flow and deploy both Functions (so new emails carry the corrected link) and Hosting (so the link resolves to the actual React web app).

## What Shipped

### Code Changes — commit `41ed064`

- **`functions/src/config/publicUrls.ts` (new)** — Single source of truth for the public web base URL.
  - `defineString("PUBLIC_WEB_BASE_URL", { default: "https://gift-registry-ro.web.app" })` with a `publicWebBaseUrl()` helper that strips trailing slashes and falls back to the default when the param is unset (tests, emulator).
  - `buildRegistryUrl(registryId)` → `${base}/registry/{id}`
  - `buildReReserveUrl(reservationId)` → `${base}/reservation/{id}/re-reserve`
- **`functions/src/__tests__/publicUrls.test.ts` (new)** — Unit tests for helpers + env override.
- **Replaced hardcoded `https://giftregistry.app` in 3 call sites** (the planner caught 2 the original scouting missed):
  - `functions/src/registry/inviteToRegistry.ts` — invite email
  - `functions/src/notifications/onPurchaseNotification.ts` — purchase notification email
  - `functions/src/reservation/releaseReservation.ts` — expiry/re-reserve email
- **`functions/src/email/__tests__/emailTemplates.test.ts`** — fixture updates to use the new base URL.

### Production Deploy — project `gift-registry-ro`

Deployed with:
```
firebase deploy --only hosting,functions:inviteToRegistry,functions:onPurchaseNotification,functions:releaseReservation --project gift-registry-ro
```

- **Hosting:** `https://gift-registry-ro.web.app` now serves the React web app (Vite build, 5 files).
- **Functions (europe-west3):** `inviteToRegistry`, `onPurchaseNotification`, `releaseReservation` all updated successfully. New emails sent by any of these functions now contain working `gift-registry-ro.web.app` links.

### Post-deploy verification

| Check | Result |
|-------|--------|
| `curl https://gift-registry-ro.web.app/` | HTTP 200 |
| `curl https://gift-registry-ro.web.app/registry/anything` | HTTP 200 (SPA fallback) |
| Firebase deploy output | `✔ Deploy complete!` — all 3 functions + hosting release finalized |

## Notable Issues Encountered (pre-existing, worked around)

These are **not** regressions from this task — they were latent issues surfaced by running a functions deploy for the first time this session. They should be properly addressed in a follow-up.

### 1. `functions/tsconfig.json` produces the wrong `lib/` layout

`tsconfig.json` has `include: ["src", "scripts"]` with no `rootDir`, which makes `tsc` use the common ancestor (the functions root) as the effective rootDir. Result: `npm run build` writes to `lib/src/*.js` and `lib/scripts/*.js`, but `package.json` declares `"main": "lib/index.js"`. Firebase then refuses to deploy:

> `Error: functions/lib/index.js does not exist, can't deploy Cloud Functions`

**Worked around** by invoking `tsc` directly with `--rootDir src src/index.ts` so the output lands at `lib/index.js`.

**Follow-up needed:** either (a) split `scripts/` into its own tsconfig, (b) drop `scripts` from `include` and run `seedStores` separately, or (c) add `"rootDir": "src"` + move `scripts/` inside `src/` or exclude it.

### 2. `defineString` param requires a value in non-interactive deploys

Firebase CLI in `--non-interactive` mode refuses to deploy a function that uses `defineString("PUBLIC_WEB_BASE_URL", ...)` without an explicit value, **even though the param has a default set**. This tripped the deploy mid-flow:

> `Error: In non-interactive mode but have no value for the following environment variables: PUBLIC_WEB_BASE_URL`

**Worked around** by creating `functions/.env` with `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app` (matches the hardcoded default — no secret, just satisfies the CLI).

**Note:** `functions/.env` is gitignored (global `.env` rule). It stays on the deployer's machine. Future deploys either need to recreate this file locally, deploy interactively (accept the default at the prompt), or we commit a `functions/.env.example` + document the copy step.

### 3. Stray `functions/scripts/seedStores.js` / `.map`

The ad-hoc `tsc` invocation produced compiled output for `scripts/seedStores.ts` outside `lib/`. Cleaned up in the worktree. Follow-up tsconfig fix would prevent this permanently.

## What Did NOT Change

- No behavior change in the React web app itself — it was already built into `hosting/public/` earlier this session; this deploy merely published it.
- No database or Firestore rule changes.
- No UI changes.
- The pre-existing, broken `functions/tsconfig.json` `include` layout is unchanged — documented above as a follow-up.

## Verification Against Original Issues

The user's pre-clear report called out three separate issues; this task addressed **only #1**. The other two are still open:

| # | Issue | Status |
|---|-------|--------|
| 1 | Web link in email goes nowhere | ✅ **Resolved** — URL fixed, functions redeployed, hosting live |
| 2 | Victor doesn't see Maria's registry (shared-with-me query missing) | ⏳ Not in scope for this task |
| 3 | No in-app invite notification (FCM / in-app inbox) | ⏳ Not in scope for this task |

## Files Touched

| Path | Change |
|------|--------|
| `functions/src/config/publicUrls.ts` | added |
| `functions/src/__tests__/publicUrls.test.ts` | added |
| `functions/src/registry/inviteToRegistry.ts` | modified |
| `functions/src/notifications/onPurchaseNotification.ts` | modified |
| `functions/src/reservation/releaseReservation.ts` | modified |
| `functions/src/email/__tests__/emailTemplates.test.ts` | modified |
| `functions/.env` (local, gitignored) | created (deploy-time convenience) |
| `hosting/public/**` | rebuilt (gitignored build output) |

## Deploy Artifacts

- Deployed Hosting URL: https://gift-registry-ro.web.app
- Firebase account: pop.v.victor@gmail.com
- Project: gift-registry-ro
- Functions region: europe-west3
- Commit on main: `41ed064`
