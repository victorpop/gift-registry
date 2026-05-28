---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 08
type: execute
wave: 7
depends_on: ["07"]
files_modified:
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
  - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-08-UAT-RESULTS.md
autonomous: false
requirements:
  # V2-SPEC locked decisions verified on-device by this plan:
  - V2-D3     # flat product list UI (DiscoverScreen unchanged, backend swapped)
  - V2-RULE   # product titles match the linked retailer page (no hallucination) — the UAT-6 driver re-validation
  - V2-ARCH   # end-to-end: Gemini intent → CSE → flat cards on device
  # Unchanged D-* the search path still must honor (re-validated on device, NOT re-claimed as new delivery):
  - D-13      # rate limit 20/hr surfaces as error after cap
  - D-31      # response contract → DiscoverRepositoryImpl maps products[] unchanged
  - D-32      # raw retailer URL opens on tap (no affiliate transform)
  - D-37      # price guard: card omits price when price == 0.0 (CSE returns no price)
  - D-41      # en + ro locale parity for all discover_* strings
must_haves:
  truths:
    - "Searching 'cadou copil 2 ani' (the original UAT-6 failing query) returns products whose titles match the actual linked retailer pages — the hallucination is gone"
    - "When CSE returns no price (price == 0), the card does NOT render '0,00 RON'"
    - "Product images load (CSE pagemap images via https) or fall back to the placeholder drawable"
    - "Tapping a card opens the raw retailer URL in the browser"
    - "Romanian locale shows DESCOPERĂ / DE PE WEB / DIN COMUNITATE and the search placeholder in Romanian"
    - "The community-popular section still loads (regression check — that path was untouched by 17-07)"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt"
      provides: "Product card with the price>0 guard confirmed present"
      contains: "if (product.price > 0.0)"
    - path: ".planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-08-UAT-RESULTS.md"
      provides: "Recorded on-device re-UAT outcomes for the search-v2 scenarios"
      contains: "UAT-6"
  key_links:
    - from: "app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt"
      to: "discoverSearch Callable"
      via: "getHttpsCallable('discoverSearch').call(mapOf('query' to query)) → mapResponseToProducts (UNCHANGED contract)"
      pattern: "discoverSearch"
    - from: "app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt"
      to: "browser"
      via: "Intent(ACTION_VIEW, raw retailerUrl) on card click"
      pattern: "ACTION_VIEW"
---

<objective>
Confirm the Android Discover card guards against the CSE "no price" case (price == 0 must not render "0,00 RON"), then run the on-device re-UAT that re-validates the UAT-6 hallucination driver plus the new search-v2 scenarios against the deployed backend from Plan 17-07.

Purpose: Per 17-RESEARCH.md, Android needs ZERO critical-path changes — the `DiscoverRepositoryImpl.mapResponseToProducts()` already maps `products[]` and the `{ products, cached_at }` contract is unchanged. The only product-facing concern is the missing-price display (CSE doesn't return price), and the on-device proof that the hallucination is structurally gone.

Output: A confirmed (or, if missing, added) price guard in `DiscoverProductCard.kt`, and a completed `17-08-UAT-RESULTS.md` recording the re-UAT outcomes. This is the gate that lets Phase 17 finally be marked verified.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-SEARCH-V2-SPEC.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-VALIDATION.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-07-SUMMARY.md

<interfaces>
<!-- Android-side facts the executor needs. Extracted from the live codebase. -->

DiscoverProductCard.kt ALREADY contains the price guard (added in 17-05 commit 96448bc, line 112) — VERIFY it survives; only edit if it is somehow absent:
```kotlin
if (product.price > 0.0) {
    val formatted = NumberFormat.getCurrencyInstance(Locale("ro", "RO"))
        .format(product.price)
    Text(text = formatted, style = typography.bodyMEmphasis, color = colors.ink)
}
```

DiscoverRepositoryImpl.search() — UNCHANGED, no edit needed. It calls discoverSearch and maps the same product fields (title, description, image_url, price, currency, retailer_url, retailer_name). The 17-07 backend keeps this exact response shape, so this file requires NO change.

MEMORY.md device build constraint (load-bearing for the UAT build):
- Physical device debug builds MUST use `-Puse_emulator=false` or every Firebase call hangs (debug defaults to 10.0.2.2, AVD-only).
- FCM/Auth caveats do not apply to Discover (Discover is request/response via Callable; signed-in registered user required per D-12).
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Confirm the price>0 guard in DiscoverProductCard.kt (add only if absent)</name>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt (the card — confirm the guard at the price Text)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md ("Open Questions" #2 price=0 + "Price — DO NOT rely on pagemap")
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md ("DiscoverProductCard" → "Price: ... omit if price == 0.0")
  </read_first>
  <action>
    CSE never returns a reliable price (17-07 sets price=0 in the normalizer). The Android card must NOT render "0,00 RON" for these.

    Read `DiscoverProductCard.kt`. The price block is expected to already be guarded:
    ```kotlin
    if (product.price > 0.0) {
        val formatted = NumberFormat.getCurrencyInstance(Locale("ro", "RO")).format(product.price)
        Text(text = formatted, style = typography.bodyMEmphasis, color = colors.ink)
    }
    ```
    - If the guard `if (product.price > 0.0)` is ALREADY present wrapping the price `Text`, make NO code change (it was added in 17-05). The task is a confirmation, not an edit.
    - If (and only if) the guard is missing, wrap the price `Text` composable in `if (product.price > 0.0) { ... }` exactly as above and rebuild.

    Do NOT touch DiscoverRepositoryImpl, DiscoverViewModel, DiscoverScreen, or the DiscoverProduct model — the backend contract is unchanged, so Android needs no other edits.

    If a code change was needed, commit it:
    `git add app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt && git commit -m "fix(17-08): guard Discover card price display when CSE returns no price"`
    If no change was needed, skip the commit and note "guard already present (96448bc)" in the SUMMARY.
  </action>
  <verify>
    <automated>grep -n "if (product.price > 0.0)" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt` contains `if (product.price > 0.0)` wrapping the price Text
    - The price `Text` (NumberFormat ... "ro","RO") is INSIDE that guard block
    - No other Android discover files were modified
  </acceptance_criteria>
  <done>The card omits the price entirely when price == 0.0 (confirmed present from 17-05, or added if it was missing).</done>
</task>

<task type="auto">
  <name>Task 2: Build + install the debug APK on the physical device</name>
  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md ("Setup" section — the exact build/install commands and the -Puse_emulator=false rationale)
    - /Users/victorpop/.claude/projects/-Users-victorpop-ai-projects-gift-registry/memory/MEMORY.md (reference_android_emulator_flag — debug builds default to 10.0.2.2; physical devices need -Puse_emulator=false or all Firebase calls hang)
  </read_first>
  <action>
    Build the debug APK for a PHYSICAL device and install it. `-Puse_emulator=false` is MANDATORY (per MEMORY.md — without it, debug builds point Firebase at 10.0.2.2 and every Callable hangs).
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry
    ./gradlew app:assembleDebug -Puse_emulator=false
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
    Record the build commit for the UAT header: `git rev-parse --short HEAD`.
    Confirm a device is attached first: `adb devices` (should list one physical device, not an emulator).
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && ./gradlew app:assembleDebug -Puse_emulator=false 2>&1 | tail -8 && ls -la app/build/outputs/apk/debug/app-debug.apk</automated>
  </verify>
  <acceptance_criteria>
    - `./gradlew app:assembleDebug -Puse_emulator=false` reports BUILD SUCCESSFUL
    - `app/build/outputs/apk/debug/app-debug.apk` exists
    - `adb install -r` reports Success
    - The build commit hash is recorded for the UAT-RESULTS header
  </acceptance_criteria>
  <done>A fresh debug APK built with -Puse_emulator=false is installed on the physical device, ready for re-UAT against the deployed search-v2 backend.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: On-device re-UAT — re-validate the UAT-6 driver + search-v2 scenarios</name>
  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-VALIDATION.md ("Manual-Only Verifications" — the exact re-UAT scenarios + why each is manual)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md (the prior UAT scaffold — mirror its scenario/PASS-FAIL structure)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-SEARCH-V2-SPEC.md ("Why we are pivoting" — the concrete UAT-6 hallucination example to disprove)
  </read_first>
  <what-built>
    Plan 17-07 deployed the re-architected `discoverSearch`: Gemini extracts intent + up-to-3 Romanian search queries (no grounding), Google CSE returns real products from the 43-store engine, results are normalized + de-duped and returned in the unchanged `{ products, cached_at }` shape. Task 1 confirmed the card hides price when CSE returns none. Task 2 installed the build. This checkpoint proves, on a real device against the live backend, that the UAT-6 hallucination is structurally gone.

    Claude has FIRST created `17-08-UAT-RESULTS.md` (see action below) with the scenario checklist pre-filled — the human fills in PASS/FAIL/notes during this checkpoint.
  </what-built>
  <how-to-verify>
    Sign in as a registered (non-anonymous) user, open Discover (bottom nav slot 2), and walk these scenarios. Record each PASS/FAIL/DEFER + notes in 17-08-UAT-RESULTS.md.
    1. UAT-6 DRIVER (the critical one): search "cadou copil 2 ani". Tap 2-3 result cards. For each, the title shown in-app MUST match the actual product on the opened retailer page (e.g., a toy card opens a toy, NOT an Esprit T-shirt). This disproves the original hallucination. PASS only if titles match the linked pages.
    2. UAT-07: search "Gift for coffee lover" → relevant coffee-related product cards from supported stores.
    3. UAT-08: search "Wedding gift for friends" → relevant results (wedding/home/registry-appropriate stores).
    4. UAT-11/locale: switch device to Romanian, search "cadou Craciun bunica" → Romanian-language results; nav reads DESCOPERĂ, headers DE PE WEB / DIN COMUNITATE, placeholder "Caută orice produs...".
    5. Price guard: confirm cards with no price show NO "0,00 RON" line (the price line is simply absent).
    6. Images: product images load over https, or the placeholder drawable renders (no broken/error glyph for most cards).
    7. Card tap: opens the raw retailer URL in the browser.
    8. Regression: the "FROM THE COMMUNITY" section still loads its popular cards (17-07 did not touch that path).
    If anything fails, capture `adb logcat | grep -i discover` output into the notes.
  </how-to-verify>
  <action>
    Before pausing for the human, Claude CREATES `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-08-UAT-RESULTS.md` using the 17-06-UAT-RESULTS.md structure as the template: frontmatter (phase, plan: 08, artifact: UAT-RESULTS, device: <fill in>, build: <commit from Task 2>), a "Backend state going into UAT" note (search-v2 deployed, CSE engine + secrets live), and a scenario checklist with one entry per how-to-verify item above (UAT-6 driver, UAT-07, UAT-08, UAT-11 locale, price guard, images, card tap, community regression), each with `[ ] PASS / [ ] FAIL / [ ] DEFER` + Notes, plus a Final Tally + Sign-off block. Then pause for the human to perform the steps and dictate results.
  </action>
  <acceptance_criteria>
    - `17-08-UAT-RESULTS.md` exists with the scenario checklist and the Task 2 build commit in its header
    - The UAT-6 driver scenario ("cadou copil 2 ani") is recorded with an explicit PASS/FAIL on title-matches-linked-page
    - Every scenario from how-to-verify has a recorded outcome (PASS/FAIL/DEFER) + notes
    - A Sign-off outcome is recorded (uat pass / uat partial / uat fail)
  </acceptance_criteria>
  <resume-signal>Type "uat pass" if all critical scenarios (esp. the UAT-6 driver) PASS, "uat partial: <gaps>" if some non-critical scenarios fail/defer, or "uat fail: <reason>" if the hallucination persists or search returns no real products.</resume-signal>
</task>

</tasks>

<verification>
- `grep -n "if (product.price > 0.0)" app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt` confirms the price guard.
- The debug APK built with `-Puse_emulator=false` installed cleanly on the physical device.
- `17-08-UAT-RESULTS.md` is filled in with the re-UAT outcomes, including an explicit verdict on the UAT-6 hallucination driver.
- No Android files other than DiscoverProductCard.kt (and only if the guard was missing) were modified — the backend-contract-unchanged invariant from 17-07 holds.
</verification>

<success_criteria>
- The card never displays "0,00 RON" when CSE returns no price.
- On a real device against the deployed backend, searching the original UAT-6 query returns products whose titles match the linked retailer pages — the hallucination is structurally eliminated.
- Romanian locale parity holds for all discover_* strings.
- Community-popular section still loads (no regression from the search re-scope).
- 17-08-UAT-RESULTS.md records the verdict that gates Phase 17 verification.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-08-SUMMARY.md` documenting: whether the price guard needed a change or was already present, the build commit UAT-ed, the re-UAT verdict (esp. the UAT-6 driver result), and any defects to fold into follow-up todos. Note explicitly whether Phase 17 can now be marked verified.
</output>
