---
quick_id: 260516-lbf
type: quick
mode: tdd
autonomous: true
files_modified:
  - web/src/lib/firestore-mapping.ts
  - web/src/lib/__tests__/firestore-mapping.test.ts
requirements:
  - LBF-01  # Web tiles render shop/price/currency for Android-written items
must_haves:
  truths:
    - "Web ItemCard for an Android-written item shows merchantDomain (e.g. 'emag.ro') above the title"
    - "Web ItemCard for an Android-written item shows price + currency (e.g. '459,00 RON') below the title"
    - "Existing legacy/future docs that ship explicit numeric `price` + structured `currency` + explicit `merchantDomain` continue to work unchanged (preferred over derived values)"
    - "All 171 existing web tests remain green; ~9 new tests added"
  artifacts:
    - path: "web/src/lib/firestore-mapping.ts"
      provides: "Exported helpers `parsePriceString` and `deriveMerchantDomain`; updated `mapItemSnapshot` consuming Android string-price schema"
      contains: "export function parsePriceString"
    - path: "web/src/lib/firestore-mapping.ts"
      provides: "Exported `deriveMerchantDomain` helper"
      contains: "export function deriveMerchantDomain"
    - path: "web/src/lib/__tests__/firestore-mapping.test.ts"
      provides: "Unit + integration tests for parsePriceString / deriveMerchantDomain / mapItemSnapshot Android-schema path"
      contains: "describe('parsePriceString'"
  key_links:
    - from: "web/src/features/registry/ItemCard.tsx"
      to: "web/src/lib/firestore-mapping.ts (Item.price, Item.currency, Item.merchantDomain)"
      via: "useItemsQuery → mapItemSnapshot"
      pattern: "item\\.(price|currency|merchantDomain)"
    - from: "web/src/lib/firestore-mapping.ts mapItemSnapshot"
      to: "Android ItemDto schema (price: String?, no currency, no merchantDomain — derived from originalUrl)"
      via: "parsePriceString + deriveMerchantDomain"
      pattern: "parsePriceString|deriveMerchantDomain"
---

<objective>
Fix the web mapper so product tiles populated by the Android app display shop name, price and currency. The Android `ItemDto` writes `price` as a free-form string (e.g. "459,00 RON") and does NOT write `currency` or `merchantDomain` at all — the web mapper currently expects a numeric `price`, an explicit `currency`, and an explicit `merchantDomain`, so all three render as null. Adapt `mapItemSnapshot` to (a) accept both numeric and string `price` shapes, (b) parse currency out of the string when present, (c) derive `merchantDomain` from `originalUrl` when not provided explicitly. No Android, backend, UI or i18n changes.

Purpose: Close the schema-mismatch gap surfaced after k37 verification (same class of bug as quick-260510-o7w for RegistryDto; that fix established the precedent — web mapper adapts to Android-canonical schema, never the other way around).

Output: Updated `firestore-mapping.ts` with two new exported helpers + JSDoc explaining the schema mismatch; new unit + integration tests appended to `firestore-mapping.test.ts`.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@./CLAUDE.md
@.planning/STATE.md

<!-- THE file to patch (mapItemSnapshot lines 71-88) -->
@web/src/lib/firestore-mapping.ts

<!-- Test file already exists with mapRegistrySnapshot suite — append new describes -->
@web/src/lib/__tests__/firestore-mapping.test.ts

<!-- De facto schema source: Android writes price as String?, no currency, no merchantDomain -->
@app/src/main/java/com/giftregistry/data/model/ItemDto.kt
@app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt

<!-- Reference implementation for parsePriceString — mirror lines 71-108 client-side -->
@functions/src/registry/fetchOgMetadata.ts

<!-- Consumers — no change needed, will auto-light-up once mapper returns populated fields -->
@web/src/features/registry/ItemCard.tsx
@web/src/features/registry/useItemsQuery.ts
@web/src/features/registry/__tests__/useItemsQuery.test.ts

<interfaces>
<!-- What the mapper consumes (Android-written Firestore document, the canonical shape): -->
<!-- From app/src/main/java/com/giftregistry/data/model/ItemDto.kt + ItemRepositoryImpl.kt toMap(): -->
```
{
  title: string,
  originalUrl: string,         // e.g. "https://www.emag.ro/...."
  affiliateUrl: string,
  imageUrl: string | null,
  price: string | null,        // free-form, e.g. "459,00 RON", "€19.99", "299"
  notes: string | null,
  status: string,              // "available" | "reserved" | "purchased"
  createdAt: number,
  updatedAt: number,
  reservedBy: string | null,
  reservedAt: Timestamp | null,
  expiresAt: Timestamp | null,
  // NOTE: no `currency` field
  // NOTE: no `merchantDomain` field
}
```

<!-- What the web Item type expects (web/src/lib/firestore-mapping.ts:50-64): -->
```typescript
export interface Item {
  id: string
  title: string
  imageUrl: string | null
  price: number | null            // <-- numeric, must be parsed from string
  currency: string | null         // <-- must be parsed from price string or fall through to d.currency
  notes: string | null
  status: ItemStatus
  reservedBy: string | null
  reservedAt: Date | null
  expiresAt: Date | null
  affiliateUrl: string
  originalUrl: string
  merchantDomain: string | null   // <-- must be derived from originalUrl when absent
}
```

<!-- Server-side helpers to mirror (functions/src/registry/fetchOgMetadata.ts:26-108): -->
```typescript
const CURRENCY_ALIASES: Record<string,string> = { "€":"EUR","$":"USD","£":"GBP","lei":"RON","ron":"RON","eur":"EUR","usd":"USD" /* minimal subset */ }
function normalizeCurrency(raw): string | null   // ISO 4217 passthrough, else alias map
function extractAmount(raw): string | null       // /\d[\d.,]*/ first match
function parsePriceString(raw): { amount, currency } | null  // server signature returns string amount
```

<!-- Client-side helpers being added (NEW exports): -->
```typescript
export function parsePriceString(raw: string): { amount: number | null; currency: string | null }
export function deriveMerchantDomain(d: { merchantDomain?: unknown; originalUrl?: unknown }): string | null
```

<!-- Existing private helper convention in firestore-mapping.ts:66-69 (coerceStatus is NOT exported). -->
<!-- The new helpers MUST be exported so the test file can target them directly. -->

<!-- Existing test pattern: web/src/lib/__tests__/firestore-mapping.test.ts:16-21 (makeSnap helper). -->
<!-- For mapItemSnapshot tests, a QueryDocumentSnapshot fixture is needed — same shape minus the `.exists()` branch. -->
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: RED — Append failing tests for parsePriceString, deriveMerchantDomain, mapItemSnapshot Android-schema path</name>
  <files>web/src/lib/__tests__/firestore-mapping.test.ts</files>
  <behavior>
    Append THREE new `describe` blocks to the existing file. Mirror the makeSnap pattern already in use (line 16-21) — extend it to support QueryDocumentSnapshot fixtures (no `.exists()` branch needed; QueryDocumentSnapshot is non-null by contract).

    **describe('parsePriceString'):**
    - `"459,00 RON"` → `{ amount: 459, currency: 'RON' }`
    - `"€19.99"` → `{ amount: 19.99, currency: 'EUR' }`
    - `"$1,299.99"` → `{ amount: 1299.99, currency: 'USD' }`
    - `"299"` → `{ amount: 299, currency: null }`
    - `""` → `{ amount: null, currency: null }`
    - `"lei 1.299,50"` → `{ amount: 1299.5, currency: 'RON' }`  // dot=thousands, comma=decimal (RO locale)
    - `"abc"` → `{ amount: null, currency: null }`
    - `"1,234"` (3-digit comma tail) → `{ amount: 1234, currency: null }`  // thousands, not decimal

    **describe('deriveMerchantDomain'):**
    - `{ merchantDomain: 'override.com', originalUrl: 'https://emag.ro/x' }` → `'override.com'`  // explicit wins
    - `{ originalUrl: 'https://www.emag.ro/foo' }` → `'emag.ro'`  // strip leading "www."
    - `{ originalUrl: 'https://ikea.com' }` → `'ikea.com'`
    - `{}` → `null`  // missing url
    - `{ originalUrl: 'not-a-url' }` → `null`  // malformed
    - `{ merchantDomain: '', originalUrl: 'https://emag.ro/x' }` → `'emag.ro'`  // empty string is NOT a valid override; fall through to derivation

    **describe('mapItemSnapshot — Android schema'):**
    - Android-style doc (`price: '459,00 RON'`, `originalUrl: 'https://www.emag.ro/p'`, NO currency, NO merchantDomain) → Item has `price === 459`, `currency === 'RON'`, `merchantDomain === 'emag.ro'`.
    - Android-style doc with bare numeric string (`price: '299'`, `originalUrl: 'https://ikea.com/p'`) → Item has `price === 299`, `currency === null`, `merchantDomain === 'ikea.com'`.
    - Android-style doc with empty/null price (`price: null`) → Item has `price === null`, `currency === null`, `merchantDomain` derived from originalUrl.
    - Legacy/future doc with structured fields (`price: 199.99` (number), `currency: 'EUR'`, `merchantDomain: 'shop.com'`) → those win unchanged (no derivation overrides).
    - Doc with numeric price but no currency field (`price: 199.99`, no currency) → Item has `price === 199.99`, `currency === null`.

    Helpers MUST be imported by name from `'../firestore-mapping'`:
    ```typescript
    import { mapItemSnapshot, mapRegistrySnapshot, parsePriceString, deriveMerchantDomain } from '../firestore-mapping'
    ```

    QueryDocumentSnapshot fixture pattern (no exists() needed):
    ```typescript
    const makeItemSnap = (id: string, data: Record<string, unknown>) =>
      ({ id, data: () => data }) as unknown as QueryDocumentSnapshot<DocumentData>
    ```
  </behavior>
  <action>
    1. Read `web/src/lib/__tests__/firestore-mapping.test.ts` to anchor the append point (after the existing `describe('mapRegistrySnapshot', ...)` block at line 86).
    2. Add the import line for `mapItemSnapshot`, `parsePriceString`, `deriveMerchantDomain` and `QueryDocumentSnapshot` type at the top.
    3. Add the `makeItemSnap` fixture helper near the existing `makeSnap`.
    4. Append the three `describe` blocks listed in <behavior> with the exact case enumeration above.
    5. Run the tests — they MUST fail because: `parsePriceString` and `deriveMerchantDomain` are not exported yet (compile error / import undefined), and `mapItemSnapshot` returns `price: null` / `currency: null` / `merchantDomain: null` for Android docs.
    6. DO NOT touch `firestore-mapping.ts` in this task. Failing tests are the deliverable.
    7. Commit using gsd-tools:
       `node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit "test(quick-260516-lbf-01): add failing tests for web mapper Android-schema adaptation" --files web/src/lib/__tests__/firestore-mapping.test.ts`
  </action>
  <verify>
    <automated>cd web && npm test -- --run src/lib/__tests__/firestore-mapping.test.ts 2>&1 | tail -40</automated>
    Expected: TypeScript compile error (`parsePriceString`/`deriveMerchantDomain` are not exported from '../firestore-mapping'), OR if tsc tolerates the import, runtime failures in the new describe blocks. Existing `mapRegistrySnapshot` suite still passes. Total: ~9 new failing assertions.
  </verify>
  <done>
    - Test file contains three new `describe` blocks (parsePriceString, deriveMerchantDomain, mapItemSnapshot — Android schema) appended after the existing mapRegistrySnapshot describe.
    - Import line adds `mapItemSnapshot`, `parsePriceString`, `deriveMerchantDomain` from '../firestore-mapping'.
    - Test run FAILS (tests cannot pass yet — helpers don't exist).
    - Existing `mapRegistrySnapshot` tests still pass.
    - Commit landed with `test(quick-260516-lbf-01):` prefix.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: GREEN — Add parsePriceString + deriveMerchantDomain helpers, update mapItemSnapshot to consume Android schema</name>
  <files>web/src/lib/firestore-mapping.ts</files>
  <behavior>
    All new tests from Task 1 must pass. Existing 171 web tests stay green. `tsc --noEmit` clean.
  </behavior>
  <action>
    1. Open `web/src/lib/firestore-mapping.ts`.

    2. **Add exported helper `parsePriceString`** (place ABOVE the `Item` interface, after `timestampToDate`). Mirror `functions/src/registry/fetchOgMetadata.ts` lines 71-108 (extractAmount + normalizeCurrency + parsePriceString) but client-side and returning a numeric amount (not string):

    ```typescript
    const CURRENCY_ALIASES: Record<string, string> = {
      '€': 'EUR',
      '$': 'USD',
      '£': 'GBP',
      lei: 'RON',
      ron: 'RON',
      eur: 'EUR',
      usd: 'USD',
    }

    function normalizeCurrency(raw: string | null | undefined): string | null {
      if (!raw) return null
      const trimmed = raw.trim()
      if (trimmed === '') return null
      if (/^[A-Z]{3}$/.test(trimmed)) return trimmed
      return CURRENCY_ALIASES[trimmed.toLowerCase()] ?? null
    }

    /**
     * Parses a free-form price string (as written by Android ItemDto.price, e.g. "459,00 RON",
     * "€19.99", "1.299,50 lei") into a numeric amount and ISO 4217 currency.
     *
     * Decimal-separator heuristic (mirrors functions/src/registry/fetchOgMetadata.ts):
     *  - If both '.' and ',' are present, the right-most is the decimal separator.
     *  - If only ',' is present, it is the decimal separator UNLESS the comma-tail is
     *    exactly 3 digits (then it is a thousands separator: "1,234" → 1234).
     *  - If only '.' is present, it is the decimal separator (mirror behavior; "1.234" is rare).
     *
     * Returns { amount: null, currency: null } when no numeric run is found.
     */
    export function parsePriceString(raw: string): { amount: number | null; currency: string | null } {
      if (!raw) return { amount: null, currency: null }
      const match = raw.match(/\d[\d.,]*/)
      if (!match) return { amount: null, currency: null }
      const numericRun = match[0]

      // Decimal-separator normalization → produce a JS-parsable Number string.
      let normalized: string
      const hasDot = numericRun.includes('.')
      const hasComma = numericRun.includes(',')
      if (hasDot && hasComma) {
        const lastDot = numericRun.lastIndexOf('.')
        const lastComma = numericRun.lastIndexOf(',')
        if (lastComma > lastDot) {
          // Comma is decimal → strip dots, replace comma with dot.
          normalized = numericRun.replace(/\./g, '').replace(',', '.')
        } else {
          // Dot is decimal → strip commas.
          normalized = numericRun.replace(/,/g, '')
        }
      } else if (hasComma) {
        // Only comma: decimal unless 3-digit tail (then thousands).
        const lastComma = numericRun.lastIndexOf(',')
        const tail = numericRun.slice(lastComma + 1)
        if (tail.length === 3 && /^\d{3}$/.test(tail)) {
          normalized = numericRun.replace(/,/g, '')
        } else {
          normalized = numericRun.replace(',', '.')
        }
      } else if (hasDot) {
        normalized = numericRun
      } else {
        normalized = numericRun
      }

      const amount = Number(normalized)
      if (!Number.isFinite(amount)) return { amount: null, currency: null }

      // Currency extraction from the remainder (mirrors functions parsePriceString).
      const remainder = raw.replace(numericRun, '').replace(/[\s ]+/g, ' ').trim()
      let currency: string | null = null
      if (remainder) {
        currency = normalizeCurrency(remainder)
        if (!currency) {
          for (const token of remainder.split(/\s+/)) {
            currency = normalizeCurrency(token)
            if (currency) break
          }
        }
        if (!currency && remainder.length > 0) {
          currency = normalizeCurrency(remainder[0])
        }
      }

      return { amount, currency }
    }
    ```

    3. **Add exported helper `deriveMerchantDomain`** (place directly below `parsePriceString`):

    ```typescript
    /**
     * Derives the merchant domain for an Item. Priority:
     *  1. Explicit `merchantDomain` field on the doc (legacy/future writer wins; empty string skipped).
     *  2. Hostname of `originalUrl` with leading "www." stripped.
     *  3. null when neither is usable (invalid URL, missing originalUrl).
     */
    export function deriveMerchantDomain(d: { merchantDomain?: unknown; originalUrl?: unknown }): string | null {
      if (typeof d.merchantDomain === 'string' && d.merchantDomain.trim() !== '') {
        return d.merchantDomain
      }
      if (typeof d.originalUrl !== 'string' || d.originalUrl.trim() === '') return null
      try {
        return new URL(d.originalUrl).hostname.replace(/^www\./, '')
      } catch {
        return null
      }
    }
    ```

    4. **Add JSDoc above `mapItemSnapshot`** explaining the schema mismatch — same flavor as the comment already on `mapRegistrySnapshot` (lines 33):

    ```typescript
    /**
     * Maps a Firestore item document to the web Item domain.
     *
     * Schema mismatch (surfaced during k37 verification; same class as quick-260510-o7w
     * for RegistryDto): the Android app — the canonical writer per
     * app/src/main/java/com/giftregistry/data/model/ItemDto.kt — writes:
     *   - `price` as a free-form String? (e.g. "459,00 RON", "€19.99", "299").
     *   - NO `currency` field.
     *   - NO `merchantDomain` field.
     * The web Item type expects numeric `price`, ISO `currency`, and `merchantDomain`.
     * To bridge: `parsePriceString` extracts amount + currency from the string,
     * and `deriveMerchantDomain` derives the host from `originalUrl`. Explicit
     * structured fields on the doc (numeric `price`, `currency`, `merchantDomain`)
     * still take precedence — this keeps a clean path open for a future writer that
     * normalizes price server-side without breaking existing Android-written docs.
     */
    ```

    5. **Update `mapItemSnapshot` body** — replace lines 77, 78, 86 with the schema-aware logic:

    ```typescript
    export function mapItemSnapshot(snap: QueryDocumentSnapshot<DocumentData>): Item {
      const d = snap.data()

      // Price + currency: accept either a number (legacy/future structured shape)
      // or a string (Android-canonical). String shape may embed currency.
      let price: number | null = null
      let parsedCurrency: string | null = null
      if (typeof d.price === 'number' && Number.isFinite(d.price)) {
        price = d.price
      } else if (typeof d.price === 'string' && d.price.trim() !== '') {
        const parsed = parsePriceString(d.price)
        price = parsed.amount
        parsedCurrency = parsed.currency
      }
      // Explicit `currency` on the doc (legacy/future) wins; else fall back to the
      // currency parsed out of the price string.
      const currency =
        (typeof d.currency === 'string' && d.currency.trim() !== '' ? d.currency : null) ??
        parsedCurrency

      return {
        id: snap.id,
        title: (d.title as string) ?? '',
        imageUrl: (d.imageUrl as string | null) ?? null,
        price,
        currency,
        notes: (d.notes as string | null) ?? null,
        status: coerceStatus(d.status),
        reservedBy: (d.reservedBy as string | null) ?? null,
        reservedAt: timestampToDate(d.reservedAt),
        expiresAt: timestampToDate(d.expiresAt),
        affiliateUrl: (d.affiliateUrl as string) ?? '',
        originalUrl: (d.originalUrl as string) ?? '',
        merchantDomain: deriveMerchantDomain(d),
      }
    }
    ```

    6. Run typecheck: `cd web && npx tsc --noEmit`. MUST pass.
    7. Run full test suite: `cd web && npm test -- --run`. ALL tests pass; new ~9 added.
    8. Sanity-check unchanged consumers (no changes expected, but read to confirm):
       - `web/src/features/registry/ItemCard.tsx` lines 87, 130-138 — already render `merchantDomain` / `price` / `currency` conditionally.
       - `web/src/features/registry/useItemsQuery.ts` — call site `snap.docs.map(mapItemSnapshot)` is unchanged.
    9. Commit using gsd-tools:
       `node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit "fix(quick-260516-lbf-02): adapt web firestore mapper to Android string-price + URL-derived merchant schema" --files web/src/lib/firestore-mapping.ts`

    **Out of scope (do NOT touch):**
    - `app/**` (Android — canonical schema owner)
    - `functions/**` (backend — already correct)
    - `web/src/features/registry/ItemCard.tsx` and any other UI file (auto-lights-up)
    - `web/src/i18n/**` (no copy changes)
    - Any new file (helpers live in firestore-mapping.ts per CLAUDE.md "no new files")
  </action>
  <verify>
    <automated>cd web && npx tsc --noEmit && npm test -- --run 2>&1 | tail -25</automated>
    Expected: `tsc --noEmit` exits 0. `npm test -- --run` shows all test files passing with total ~180 tests (171 prior + ~9 new). No failures, no skipped.
  </verify>
  <done>
    - `web/src/lib/firestore-mapping.ts` exports `parsePriceString` and `deriveMerchantDomain`.
    - JSDoc block above `mapItemSnapshot` explains the Android schema mismatch and references the k37 verification origin + quick-260510-o7w precedent.
    - `mapItemSnapshot` accepts both numeric and string `price`, parses currency from the string when present, prefers explicit `d.currency` when provided, derives `merchantDomain` from `originalUrl` when not explicit.
    - `tsc --noEmit` clean.
    - `npm test -- --run` green; new test count ~9; total ~180 (verify exact number in output).
    - No changes outside `web/src/lib/firestore-mapping.ts` and `web/src/lib/__tests__/firestore-mapping.test.ts`.
    - Commit landed with `fix(quick-260516-lbf-02):` prefix.
  </done>
</task>

</tasks>

<verification>
After Task 2:

1. `cd web && npx tsc --noEmit` — exits 0.
2. `cd web && npm test -- --run` — all test files green. Note new total count (expected ~180, up from 171). If total deviates significantly, investigate.
3. Inspect output: confirm new describe blocks `parsePriceString`, `deriveMerchantDomain`, `mapItemSnapshot — Android schema` appear in the run summary.
4. `git diff --stat HEAD~2..HEAD` — ONLY two files touched: `web/src/lib/firestore-mapping.ts` and `web/src/lib/__tests__/firestore-mapping.test.ts`. If anything else appears (especially under `app/`, `functions/`, or any UI file), the fix has overreached — STOP and audit.
5. Two commits landed in order: `test(quick-260516-lbf-01):` then `fix(quick-260516-lbf-02):`.
</verification>

<success_criteria>
- Web ItemCard tiles for items written by the Android app now render `merchantDomain` (above title), and `price` + `currency` (below title). No UI code changed — fields populate via the updated mapper.
- 171 pre-existing web tests remain green; ~9 new tests added (parsePriceString: 8 cases, deriveMerchantDomain: 6 cases, mapItemSnapshot Android-schema integration: ~5 cases — adjust headcount to match exact added assertions).
- `parsePriceString` and `deriveMerchantDomain` are EXPORTED (mirroring the public surface needed for tests; consistent with how `truncateTitle` is exported in ItemCard.tsx). `coerceStatus` remains private — it has no consumer outside the mapper.
- JSDoc above `mapItemSnapshot` documents the Android schema mismatch, the k37 verification origin, and the precedent set by quick-260510-o7w for RegistryDto.
- Explicit structured fields (`price: number`, `currency: string`, `merchantDomain: string`) on a future/legacy doc still win — the derivation path is a fallback, not an override.
- D-06 unaffected (no reserver/giver identity changes).
- No changes outside the two web-files listed in `files_modified`.
</success_criteria>

<output>
After completion, create `.planning/quick/260516-lbf-fix-web-product-tiles-missing-shop-price/260516-lbf-SUMMARY.md` using the standard quick summary template:
- What was broken (Android writes string price + no currency + no merchantDomain; web mapper expected numeric/structured).
- What was fixed (new exported helpers `parsePriceString` + `deriveMerchantDomain`; mapItemSnapshot now consumes both shapes).
- Test delta (171 → ~180).
- Files touched (the two web files only).
- Verification (tsc clean, full test suite green, UI lights up automatically — no UI code changed).
- Cross-reference to quick-260510-o7w (same class of bug; same fix philosophy: web mapper adapts to Android-canonical schema).
</output>
