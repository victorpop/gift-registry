---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 08
artifact: UAT-RESULTS
created: 2026-05-29
device: <fill in: model + Android version + locale>
build: 54d5236
---

# Phase 17 Plan 08 — On-Device Re-UAT Results (Search v2 + UAT-6 Driver)

## Setup

```bash
cd /Users/victorpop/ai-projects/gift-registry
# APK already built (BUILD SUCCESSFUL, 54d5236, -Puse_emulator=false)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`-Puse_emulator=false` is mandatory for physical devices (per MEMORY.md — debug builds default to 10.0.2.2 AVD-only).

Sign in as a **registered (non-anonymous) user** (Discover requires registered auth per D-12).

## Backend state going into UAT

- **discoverSearch** (europe-west3): **Search v2 LIVE** — Gemini JSON-mode intent extraction (no grounding tools) → up to 3 Serper.dev /shopping queries (gl=ro, hl=ro, location=Romania, num=10) → 43-store hostname allowlist post-filter → de-dupe → cache write → return `{ products, cached_at }`.
- **SERPER_API_KEY** bound to discoverSearch runtime SA (Secret Manager, verified in Plan 17-07).
- **GEMINI_API_KEY** bound to discoverSearch runtime SA (Secret Manager, set in Plan 17-06).
- **discoverPopular / community triggers** (europe-west3): **UNCHANGED** — 17-07 did not touch the community-popular path.
- **Android app:** price guard `if (product.price > 0.0)` confirmed present at `DiscoverProductCard.kt:112` (from Plan 17-05 commit 96448bc). CSE-zero-price case handled — no "0,00 RON" will be shown.
- **Root cause of UAT-6 hallucination:** Gemini `google_search` grounding was replacing product IDs from the grounding chunks with Gemini-generated titles. Now Gemini only extracts intent; real products come from Serper's /shopping results (real pages, real titles). Structurally eliminated.

---

## UAT Scenario Checklist

Mark each scenario PASS / FAIL / DEFER and add notes.

---

### RE-UAT-6 DRIVER — "cadou copil 2 ani" (THE CRITICAL ONE — original UAT-6 hallucination case)

**THIS IS THE GATE SCENARIO FOR PHASE 17.**

- **Action:** Type **"cadou copil 2 ani"** in the search bar, submit. Wait for results (3-8s first time, < 500ms if cached). Tap 2-3 result cards one by one.
- **Expected:** "FROM THE WEB" section appears with real product cards. For EACH tapped card, the title shown in the app MUST match the actual product on the linked retailer page. For example: a toy card with title "Set jucarii Montessori" should open a Montessori toy listing on the retailer — NOT an Esprit T-shirt, NOT a phone cover, NOT a random unrelated product. This disproves the original grounding hallucination.
- **Pass condition:** Titles in-app match the linked pages for all 2-3 tapped cards.
- **Fail condition:** Any card title does NOT match the opened retailer page (the hallucination persists).
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Cards tapped and title-match verdict:**
  1. Card title in-app: __________ / Retailer page title: __________ / [ ] Match / [ ] Mismatch
  2. Card title in-app: __________ / Retailer page title: __________ / [ ] Match / [ ] Mismatch
  3. Card title in-app: __________ / Retailer page title: __________ / [ ] Match / [ ] Mismatch
- **Notes:**
- **If FAIL:** Run `adb logcat | grep -i discover` and paste output.

---

### RE-UAT-07 — "Gift for coffee lover" (English query, CSE in-allowlist results)

- **Action:** Search **"Gift for coffee lover"** in English.
- **Expected:** "FROM THE WEB" section populates with coffee-related product cards (coffee machines, accessories, coffee beans) from supported stores (emag.ro, altex.ro, etc.).
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

### RE-UAT-08 — "Wedding gift for friends" (occasion keyword routing)

- **Action:** Search **"Wedding gift for friends"**.
- **Expected:** Relevant results (wedding/home/registry-appropriate stores). Some results from wedding-specific or home-décor Romanian retailers.
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

### RE-UAT-11 — Locale parity (Romanian locale end-to-end)

- **Action:** Switch device locale to Romanian (Settings → General → Language). Open the app, navigate to Discover, search **"cadou Craciun bunica"**.
- **Expected:**
  - Bottom nav label: **DESCOPERĂ** (not DISCOVER)
  - Section headers: **DE PE WEB** / **DIN COMUNITATE** (not FROM THE WEB / FROM THE COMMUNITY)
  - Search bar placeholder: **"Caută orice produs..."** (not "Search for any product...")
  - Results are Romanian-language product titles (Serper gl=ro forces Romanian results regardless of UI locale)
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

### RE-UAT-PRICE — Price guard (no "0,00 RON" when CSE returns no price)

- **Action:** Browse any search results page that has populated cards.
- **Expected:** Cards where Serper returned no price show **NO price line** — the price is simply absent. No card should display "0,00 RON" or "0 RON". Cards with a real Serper price (e.g. "299,99 RON") show it correctly formatted.
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

### RE-UAT-IMAGES — Product images load

- **Action:** Browse any search results page.
- **Expected:** Product images load over https (Google encrypted-tbn CDN, served by Serper), OR the placeholder drawable renders. No broken/error glyph for most cards (an occasional missing image on a card that has no valid URL is acceptable, but the majority should show a real product image).
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

### RE-UAT-TAP — Card tap opens raw retailer URL

- **Action:** Tap any result card.
- **Expected:** Device's default browser opens to the raw retailer URL (no affiliate transform, no google.com/shopping redirect wrapper — or if google.com/shopping appears, the browser follows through to the retailer).
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

### RE-UAT-REGRESSION — Community-popular section still loads

- **Action:** Navigate to Discover without searching. Observe the "FROM THE COMMUNITY" section.
- **Expected:** The community-popular cards still render (or the empty-popular message if no popularItems). This section was NOT touched by Plan 17-07 — confirming no regression.
- **Result:** [ ] PASS / [ ] FAIL / [ ] DEFER
- **Notes:**

---

## Final Tally

- **PASS:**   /8
- **FAIL:**   /8
- **DEFER:**  /8

Critical (must PASS for Phase 17 gate):
- [ ] RE-UAT-6 DRIVER PASS — hallucination disproved (title-in-app matches linked page)
- [ ] RE-UAT-PRICE PASS — no "0,00 RON" displayed
- [ ] RE-UAT-REGRESSION PASS — community-popular not broken

Non-critical (DEFER acceptable with rationale):
- [ ] RE-UAT-07
- [ ] RE-UAT-08
- [ ] RE-UAT-11
- [ ] RE-UAT-IMAGES
- [ ] RE-UAT-TAP

## Defects discovered

1. (none yet — fill in if any FAIL)

## Sign-off

- **UAT walked through by:** <name>
- **Date:** <yyyy-mm-dd>
- **Outcome:** [ ] uat pass / [ ] uat partial: <gap list> / [ ] uat fail: <reason>

---

*Resume signal: Type "uat pass" if all critical scenarios PASS, "uat partial: <gaps>" if some non-critical fail/defer, or "uat fail: <reason>" if the UAT-6 hallucination persists or search returns no real products.*
