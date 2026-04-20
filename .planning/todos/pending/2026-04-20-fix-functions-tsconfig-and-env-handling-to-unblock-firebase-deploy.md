---
created: 2026-04-20T15:30:58.900Z
title: Fix functions tsconfig and env handling to unblock firebase deploy
area: tooling
files:
  - functions/tsconfig.json
  - functions/package.json
  - functions/.env
  - functions/.env.example
  - .gitignore
  - firestore.rules
---

## Problem

Two pre-existing tooling issues in `functions/` surfaced during quick task `260420-nh8` (deploy hosting + fix email URL) and again during `260420-ozb` (notifications inbox). They are independent of those tasks but block clean redeploys.

### 1. `functions/tsconfig.json` produces the wrong `lib/` layout

Current tsconfig:
```json
{
  "include": ["src", "scripts"],
  "compilerOptions": { "outDir": "lib", ... }
}
```

With no `rootDir` declared, `tsc` picks the common ancestor of the `include` paths (the `functions/` root) as the effective root. As a result, `npm run build` writes output to `lib/src/*.js` and `lib/scripts/*.js`, but `functions/package.json` declares `"main": "lib/index.js"`. Firebase deploy fails with:

```
Error: functions/lib/index.js does not exist, can't deploy Cloud Functions
```

The committed `functions/lib/` directory on main is stale output from before `scripts` was added to `include` (git blame: commit `5c60f27`), which masks the issue — a fresh `rm -rf lib && npm run build` reproduces the broken layout immediately.

**Workaround currently in use:** invoke `tsc` directly with `--rootDir src src/index.ts`. This is not committed anywhere — every future deployer must know the trick.

### 2. `defineString` params require explicit values in non-interactive deploys

`functions/src/config/publicUrls.ts` (introduced by quick task `260420-nh8`) uses:
```ts
const PUBLIC_WEB_BASE_URL = defineString("PUBLIC_WEB_BASE_URL", {
  default: "https://gift-registry-ro.web.app",
});
```

Despite the `default`, `firebase deploy --non-interactive` refuses to deploy:
```
Error: In non-interactive mode but have no value for the following environment variables: PUBLIC_WEB_BASE_URL
```

**Workaround currently in use:** a local `functions/.env` with `PUBLIC_WEB_BASE_URL=https://gift-registry-ro.web.app`. The file is gitignored (global `.env` rule), so it exists only on the deployer's machine. Next deployer hits the same error.

### Why this matters now (not just theoretical cleanup)

Quick task `260420-ozb` (persistent in-app notifications inbox) shipped:
- A new `writeNotification` Admin-SDK helper wired into 5 event sources in Cloud Functions
- New `firestore.rules` block for `users/{uid}/notifications`

Both need to ship to production for the inbox to actually surface notifications. The inbox UI is live in-app but will show empty until the functions deploy succeeds. So this tooling fix is not abstract — it's gating the user-visible payoff of `260420-ozb`.

## Solution

TBD — but the pieces are clear:

### Part A — tsconfig layout

Pick one:
1. **Add `"rootDir": "src"` and remove `"scripts"` from `include`.** Cleanest; matches what tsc expects for a library with one source tree. Move `scripts/seedStores.ts` compilation to a separate invocation (either its own `tsconfig.scripts.json` with `extends` + `rootDir: "scripts"` + `outDir: "lib/scripts"`, or a dedicated `ts-node` run for the seed script since it's only ever called manually via `npm run seed:stores`).
2. **Add `"rootDir": "."`** and accept the `lib/src/*` layout, then update `package.json` `"main"` to `lib/src/index.js`. Less invasive to the build script, but propagates the wrong structure deeper.
3. **Keep `include`, add `"rootDir": "src"`, drop `scripts` from `include`, and have `npm run seed:stores` use `ts-node` directly** instead of pre-compiling. Recommended — `seedStores.ts` is a one-shot maintenance script, not part of the deployed functions payload.

Clean up the committed-but-stale `functions/lib/` on main once the build is correct.

### Part B — defineString env handling

Pick one:
1. **Commit `functions/.env` with the default value.** Since the value is the public hosting domain (not a secret), committing it removes the non-interactive friction entirely. Add a comment in the file explaining its purpose.
2. **Commit `functions/.env.example` and document the copy step.** More conventional, but every deployer still has to copy-and-rename.
3. **Drop `defineString` in favor of `process.env.PUBLIC_WEB_BASE_URL ?? DEFAULT`.** Loses Firebase's param-management story, but the current usage in `publicUrls.ts` already falls back to a hardcoded default, so the param is only providing the non-interactive friction, not real value.

Recommendation: option 1 (commit `.env` with the default public URL). The URL isn't sensitive, and "firebase deploy just works from a fresh clone" is worth more than conceptual purity.

### Part C — deploy the pending notifications + rules changes

After A and B are resolved, run:
```
firebase deploy --only functions,firestore:rules --project gift-registry-ro
```

This deploys:
- All 5 event sources with the new `writeNotification` helper
- The new `users/{uid}/notifications` rules block

Verify end-to-end with a two-account live test (Maria invites Victor → Victor's bell badges to "1" within seconds).

## Related

- `/Users/victorpop/ai-projects/gift-registry/.planning/quick/260420-nh8-fix-email-invite-url-and-deploy-firebase/260420-nh8-SUMMARY.md` — "Notable Issues Encountered" section documents both workarounds
- `/Users/victorpop/ai-projects/gift-registry/.planning/quick/260420-ozb-add-persistent-in-app-notifications-inbo/` — the notifications work that needs the deploy
