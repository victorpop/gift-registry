---
status: partial
phase: 07-romanian-store-browser
source: [07-03-PLAN.md Task 3]
started: 2026-04-20T05:26:35Z
updated: 2026-04-20T05:26:35Z
---

## Current Test

[awaiting human testing]

## Setup

Before running the tests:

1. Ensure `config/stores` is seeded in Firestore (emulator or prod):
   ```
   cd functions && npm run seed:stores
   ```
   Or use the Firebase Console to verify 8 store documents exist in `config/stores`.

2. Install the debug APK on a physical device or emulator:
   ```
   ./gradlew :app:installDebug
   ```

3. Sign in as a registry owner with at least one existing registry.

## Tests

### A. STORE-01 — Store List + Home FAB menu

expected: Home screen Extended FAB ("New") expands to show two items: "Browse stores" (shopping bag icon) and "Create registry" (edit icon). Tapping "Browse stores" opens the Store List screen showing a 2-column grid of 8 stores with logos and names. Scrolling works. Tapping back arrow returns to Home.

setup: Open the app, log in as a registry owner. Tap the Extended FAB on the Home screen.

result: [pending]

### B. STORE-02 — WebView opens store homepage

expected: Tapping a store card in the Store List opens the StoreBrowserScreen. The store's homepage loads inside the WebView with JS-rendered content visible (search bar, category tiles, product cards). TopAppBar shows the store name as title and a Close (X) icon. Bottom bar shows a filled primary "Add to list" button (56dp tall, full-width) that remains visible and tappable while scrolling.

setup: From Registry Detail of a registry you own: tap the Add FAB → choose "Browse stores". Tap "eMAG". Wait for the eMAG homepage to load.

result: [pending]

### C. STORE-03 — Add-to-list funnel + affiliate round-trip

expected: Tapping a product page URL in the WebView captures the URL (onPageFinished fires). Tapping "Add to list" opens AddItemScreen with the URL field pre-filled and OG fetch auto-triggered (title, image URL, price populate within 1-3 seconds). Saving the item creates the item in Registry Detail. The Firestore item document has `affiliateUrl` populated with an eMAG affiliate tag (placeholder ID from Phase 3). For a non-eMAG store (e.g. Libris), `affiliateUrl` equals `originalUrl` (pass-through per AFF-04) and Logcat shows "Unknown merchant URL" warning.

setup: From Test B's state: navigate to a product detail page in the eMAG WebView. Tap "Add to list". Tap Save. Check Registry Detail and Firestore Console.

result: [pending]

### D. STORE-04 — WebView error state + retry

expected: With airplane mode enabled, tapping a store shows an error overlay: WifiOff icon + "Page didn't load" heading + body text + filled "Try again" button + outlined "Back" button. The "Add to list" button is disabled (greyed out). Disabling airplane mode and tapping "Try again" loads the store homepage; "Add to list" becomes enabled.

setup: Enable airplane mode on the device. Tap a store from the Store List. Observe error overlay. Disable airplane mode, tap "Try again".

result: [pending]

### E. External scheme blocking (D-08)

expected: Tapping a `tel:` or `mailto:` link in the WebView shows a Toast ("This link opens in an external app") and the WebView stays on the current page — no external app opens.

setup: Navigate to a store contact page that has phone or email links (eMAG footer, Libris contact page, etc.). Tap the link.

result: [pending]

### F. Cookie persistence (D-06)

expected: Cookies (login state, accepted cookie banners) survive across WebView sessions. After accepting a cookie banner or logging in to a store, backing out to Store List and reopening the same store shows no re-prompt.

setup: Open a store WebView. Accept the cookie banner or log into the store. Back out to Store List. Re-open the same store.

result: [pending]

### G. Romanian locale UAT (D-18)

expected: With device language set to Romanian, all Phase 7 strings display in Romanian: FAB label "Nou", menu "Răsfoiește magazine", CTA "Adaugă la listă", error heading "Pagina nu s-a încărcat", retry "Încearcă din nou", close "Închide".

setup: Change device language to Romanian (Settings → System → Language). Repeat Tests A-D briefly.

result: [pending]

### H. FAB menu dismiss (Component Contract 1 scrim)

expected: Tapping outside the expanded FAB menu (on the background area) collapses the menu without navigating. Tapping the FAB again when the menu is open (FAB shows Close icon) also collapses the menu.

setup: On Home, tap the FAB to expand the menu. Tap a background area outside the menu. Re-expand the menu. Tap the FAB again.

result: [pending]

## Summary

total: 8
passed: 0
issues: 0
pending: 8
skipped: 0
blocked: 0

## Gaps

_(none recorded yet — fill in only if a test fails)_
