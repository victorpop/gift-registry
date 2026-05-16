---
quick_id: 260516-lbf
type: quick
mode: tdd
status: complete
requirements:
  - LBF-01
files_modified:
  - web/src/lib/firestore-mapping.ts
  - web/src/lib/__tests__/firestore-mapping.test.ts
commits:
  - 88aba0c: test(quick-260516-lbf-01) — RED, 18 failing tests
  - d5cda99: fix(quick-260516-lbf-02) — GREEN, helpers + mapper rewrite
test-delta:
  before: 148 tests (worktree baseline, env-gated suites excluded)
  after: 167 tests (19 new, all green)
duration: 5m 47s
completed: 2026-05-16
---

# Quick 260516-lbf: Fix web product tiles missing shop/price/currency — Summary

## One-liner

Web `mapItemSnapshot` now adapts to the Android-canonical Firestore item
schema (string `price`, no `currency`, no `merchantDomain`) via two new
exported helpers `parsePriceString` + `deriveMerchantDomain`; ItemCard tiles
auto-light-up with shop name, price and currency for Android-written items.

## What was broken

The Android app — the canonical writer per
`app/src/main/java/com/giftregistry/data/model/ItemDto.kt` — writes:
- `price` as a free-form `String?` (e.g. `"459,00 RON"`, `"€19.99"`, `"299"`),
- NO `currency` field,
- NO `merchantDomain` field.

The web `mapItemSnapshot` previously expected a numeric `price`, an explicit
`currency`, and an explicit `merchantDomain`. Result on the web ItemCard:
- `priceText` was empty (no price line below the title).
- `retailerText` was empty (no shop name above the title).

Same class of bug as `quick-260510-o7w` for `RegistryDto` (where the web
mapper read `name`/`occasionType`/`eventDate` while Android writes
`title`/`occasion`/`eventDateMs`) — the established fix philosophy is that
the **web mapper adapts to the Android-canonical schema, never the other way
around**, because the Android app is the source of truth for what lives in
Firestore.

## What was fixed

Two new exported helpers in `web/src/lib/firestore-mapping.ts` (mirroring
`functions/src/registry/fetchOgMetadata.ts` server-side parser):

- **`parsePriceString(raw: string)`** → `{ amount: number | null; currency: string | null }`.
  Decimal-separator heuristic: when both `.` and `,` are present the
  right-most wins; when only `,` is present it is decimal unless the
  comma-tail is exactly 3 digits (then thousands, so `"1,234"` → `1234`).
  Currency tokens are normalized via a small `CURRENCY_ALIASES` map
  (`€`→`EUR`, `lei`/`ron`→`RON`, …) plus a 3-letter ISO passthrough.

- **`deriveMerchantDomain(d)`** → `string | null`.
  Priority: explicit non-empty `d.merchantDomain` wins, else `new URL(d.originalUrl).hostname.replace(/^www\./, '')`, else `null`.

`mapItemSnapshot` is updated to:
1. Accept numeric `d.price` (legacy/future) OR string `d.price` (Android).
2. Parse currency from the price string when present.
3. Prefer explicit `d.currency` when provided, else fall back to the parsed currency.
4. Derive `merchantDomain` via the new helper.

JSDoc above `mapItemSnapshot` documents the schema mismatch and the
`quick-260510-o7w` precedent.

**Explicit structured fields still win** — a future writer that normalizes
price server-side does not need a code change in the mapper to take effect.

## Test delta

| Suite                             | Before | After | New |
| --------------------------------- | -----: | ----: | --: |
| `firestore-mapping.test.ts`       |      4 |    23 |  19 |
| Full web suite (env-gated)        |    148 |   167 |  19 |

New tests appended (in order):
- `parsePriceString` — 8 cases (RO locale, US locale, symbol prefix, bare numeric, empty, alias currency, non-numeric, 3-digit comma tail).
- `deriveMerchantDomain` — 6 cases (explicit override wins, `www.` strip, plain host, missing url, malformed url, empty-string override falls through).
- `mapItemSnapshot — Android schema` — 5 cases (string price + derived host, bare numeric string, null price, legacy structured fields win unchanged, numeric price without currency).

## Files touched (exactly two, as required by the plan)

- `web/src/lib/firestore-mapping.ts` — +154 lines (CURRENCY_ALIASES, normalizeCurrency, parsePriceString, deriveMerchantDomain, JSDoc, updated mapItemSnapshot body).
- `web/src/lib/__tests__/firestore-mapping.test.ts` — +186 lines (extended imports, `makeItemSnap` fixture, three new `describe` blocks).

`git diff --stat HEAD~2..HEAD` confirms scope: 2 files, +335 / -5.

## Verification

- `cd web && npx tsc --noEmit` — exits 0 (clean).
- `cd web && npm test -- --run` — 27/27 test files pass, 167/167 tests pass.
- `cd web && npm test -- --run src/lib/__tests__/firestore-mapping.test.ts` — 23/23 pass (4 mapRegistry + 8 parsePrice + 6 deriveMerchant + 5 mapItem-Android).
- ItemCard / useItemsQuery unchanged — UI auto-lights-up because the mapper now returns populated fields.
- No changes outside the two listed files (no `app/`, `functions/`, `web/src/i18n/`, no UI files).
- D-06 unaffected (no reserver/giver identity changes).

## Deviations from plan

### [Rule 3 — Blocking issue] Created `web/.env.local` in worktree to unblock test infrastructure

- **Found during:** Task 2 verify step (full `npm test -- --run`).
- **Issue:** Worktree-fresh checkout has no `web/.env.local`, so 6 test files (`App.test.tsx`, `ItemReservePage.test.tsx`, `ProgressStrip.test.tsx`, `RegistryPage.test.tsx`, `RegistryPage.autoReserve.test.tsx`, `StickyReserveBanner.test.tsx`) crashed at module load with `FirebaseError: Firebase: Error (auth/invalid-api-key)` — `src/firebase.ts:22 getAuth(app)` runs at import time and requires the `VITE_FIREBASE_*` env vars. This blocked the plan's "full web suite must end green" verification.
- **Why pre-existing, not caused by my fix:** verified by stashing the mapper change and re-running — same 6 files failed identically at the same module-init line.
- **Fix:** copied the emulator-only fake-creds `.env.local` from the main checkout (`VITE_FIREBASE_API_KEY=fake-api-key-for-emulator`, `VITE_USE_EMULATORS=true`, project id `gift-registry-ro`). The file is `.gitignore`d (does not appear in `git status`), so it does not pollute commits.
- **Files modified:** `web/.env.local` (untracked, gitignored — not committed).
- **Result:** all 27 test files pass; 167/167 tests green.

### Test-count target adjustment

The plan estimated ~180 tests total (171 prior + ~9 new). Actual: 148
prior (worktree baseline once env-gated suites are collectable) + 19 new =
167. The 19-new figure is slightly higher than the plan's ~9 because I
counted each `it()` case (8 parsePrice + 6 deriveMerchant + 5 mapItem)
instead of the plan's rough "~9" estimate. No tests skipped, no
duplication. The 23-tests-in-the-mapper-file figure (4 prior + 19 new)
matches the per-describe breakdown one-for-one with the plan's case enumeration.

## Cross-reference

- **`quick-260510-o7w`** — same class of bug for `RegistryDto`
  (`name`/`occasionType`/`eventDate` web fields vs. Android's
  `title`/`occasion`/`eventDateMs`). Set the precedent that the web mapper
  is the adaptation layer, not the Android schema. This fix extends the same
  pattern to `ItemDto`.
- **`quick-260513-k37`** — verification of web ItemCard surfaced the
  remaining gap (shop/price/currency missing on Android-written items) and
  triggered this fix.

## Self-Check: PASSED

Files verified:
- FOUND: web/src/lib/firestore-mapping.ts (modified, 154 insertions)
- FOUND: web/src/lib/__tests__/firestore-mapping.test.ts (modified, 186 insertions)
- FOUND: .planning/quick/260516-lbf-fix-web-product-tiles-missing-shop-price/260516-lbf-SUMMARY.md

Commits verified:
- FOUND: 88aba0c (test(quick-260516-lbf-01))
- FOUND: d5cda99 (fix(quick-260516-lbf-02))
