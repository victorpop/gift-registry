---
date: 2026-05-22
category: uat
phase_origin: 14-web-fallback-live-deploy-guest-uat
plan_origin: 14-04
priority: low
deferred_from: Plan 14-04 Task 8
surfaced_during: Plan 14-04 close-out — Tasks 8/9 deferred to unblock phase closure
---

# UAT Pass 2 — Recruited real giver for web-fallback items 2 + 3

## Why this exists

Plan 14-04 closed with **Pass 1 only** on UAT items 2 (retailer redirect) and
3 (guest localStorage across browser restart). Pass 1 means: the user (= me,
the app owner) ran every item in his own fresh Chrome + Safari incognito
profiles on his own machine. Both items returned **PASS** in both browsers —
see `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`
rows 2 and 3 under "Pass 1 — Solo Incognito".

Pass 2 (D-08 layered defense in `14-CONTEXT.md`) was always meant to catch
"works on my machine" bias by handing items 2 + 3 to a real giver friend on
their own device + their own mailbox + their own browser version. This was
explicitly deferred at the Plan 14-04 close-out because scheduling a real
giver requires coordination time the user does not want to block Phase 14
closure on.

**Risk assessment:** LOW.

- Pass 1 already proved both items work end-to-end against the deployed
  bundle in both Chromium-family and WebKit browsers.
- The two items exercise generic web-platform behaviour (`window.open`
  cross-tab semantics + `localStorage` persistence across the application
  lifecycle) — neither depends on device-specific code paths in our React
  bundle.
- The most likely Pass-2-only failure mode is iOS Safari `window.open`
  being blocked by tracking-prevention or popup-blocker settings the user
  himself doesn't have configured aggressively. That class of failure would
  surface as the retailer opening in the SAME tab instead of a NEW tab —
  cosmetically degraded but the reservation still gets created.

**This is NOT a blocker for closing Phase 14.** It is a follow-up validation
that hardens the layered-UAT promise made by D-08.

## What's needed to close this todo

### 1. Pick a recruited tester

Real giver demographic. Should NOT be the app owner. Ideally:

- A friend who has a modern phone (any iPhone running iOS 17+ OR any
  Android with Chrome / Samsung Internet).
- A friend who has an email account they'll actually check.
- A friend willing to spend 5-10 minutes on a quick mobile test.

### 2. Hand them a clean PUBLIC test registry

- Use the Android app (prod APK) to create a fresh public registry with at
  least 2 available items — or reuse an existing one that has at least 2
  items still `status=available`.
- Share the URL `https://gift-registry-ro.web.app/registry/<id>` via your
  normal sharing channel (SMS / WhatsApp / etc — like a real giver would
  actually receive it).

### 3. Tester script (copy-paste-able)

You can send this verbatim. It maps 1:1 to UAT items 2 and 3:

> Hi! Doing a quick test of my gift-list app — should take 5 minutes.
>
> **Part 1 (retailer redirect):** Open this link on your phone:
> `https://gift-registry-ro.web.app/registry/<id>`
> Tap "Rezervă / Reserve" on one of the items. Fill in your first name,
> last name, and an email you can check. Then tell me three things:
> (a) Did a new tab/window open showing the retailer site (e.g. emag.ro)?
> (b) Is the original gift-list page still open in another tab?
> (c) Does the gift-list page now show a countdown timer at the top?
>
> **Part 2 (guest memory across browser restart):** Now close your
> entire browser app (swipe it away from the recents list — not just
> the tab). Re-open your browser and paste this same registry URL again.
> Tap "Reserve" on a DIFFERENT item. Did the form auto-fill your name and
> email this time, or did it ask you again?
>
> Thanks!

### 4. Collect their answers

Open `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`,
find the "Pass 2 — Recruited Real Giver" section, and fill in the table
rows for items 2 and 3:

```markdown
| # | Item | Device | Result | Evidence | Logged At |
|---|------|--------|--------|----------|-----------|
| 2 | Retailer redirect opens new tab + keeps registry tab alive | <iPhone 15, Safari 17.5 / etc> | **PASS** | Tester: "Yes, emag opened in a new tab. Yes, the original list is still open with a timer." | 2026-MM-DD |
| 3 | Guest localStorage persists across browser restart | <same device> | **PASS** | Tester: "It pre-filled my name and email automatically the second time." | 2026-MM-DD |
```

### 5. If a Pass-2-only failure surfaces

That's exactly what Pass 2 exists to catch. Log it as a separate
`/gsd:debug` or `/gsd:quick` follow-up with:

- Their exact device (model + OS version + browser version — ask them
  to screenshot Settings if needed).
- The exact answer they gave.
- Whether the divergence from Pass 1 indicates a real bug (e.g. retailer
  opens in same tab on iOS Safari → real browser-platform bug) or a
  device-config artefact (e.g. their browser has aggressive popup
  blocking → user-setting, not our bug).

Bugs go to a new `.planning/todos/pending/` file with a `category: bug` tag.
Device-config artefacts get a one-line note in the existing UAT-RESULTS row
and the todo can still be closed.

## How to close

Once Pass 2 results are logged into `14-04-UAT-RESULTS.md`, move this todo
from `.planning/todos/pending/` to `.planning/todos/completed/`:

```bash
git mv \
  /Users/victorpop/ai-projects/gift-registry/.planning/todos/pending/2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md \
  /Users/victorpop/ai-projects/gift-registry/.planning/todos/completed/
git commit -m "chore(todos): close UAT Pass 2 recruited-giver follow-up"
```

## Related

- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md`
  D-08 (layered-UAT decision: Pass 1 solo + Pass 2 recruited).
- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`
  — Pass 1 results table for items 2 and 3 (the baseline to compare
  Pass 2 against).
- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-SUMMARY.md`
  — Plan 14-04 close-out documenting why Pass 2 was deferred.
