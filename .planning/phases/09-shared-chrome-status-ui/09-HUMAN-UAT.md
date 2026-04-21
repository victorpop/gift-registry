---
status: partial
phase: 09-shared-chrome-status-ui
source: [09-04-PLAN.md Task 3, 09-VERIFICATION.md]
started: 2026-04-21T13:30:00.000Z
updated: 2026-04-21T13:30:00.000Z
---

## Current Test

[awaiting human testing — deferred by user 2026-04-21 to unblock Phase 10/11]

## Tests

### 1. Sign in — land on Home
expected: bottom nav visible (5 slots: Home · Stores · +FAB · Lists · You)
result: pending

### 2. Tap any registry card → RegistryDetail
expected: bottom nav still visible; Lists slot highlighted (accentSoft pill + accent icon/label)
result: pending

### 3. On RegistryDetail, tap ⋯ → Edit registry
expected: bottom nav HIDDEN
result: pending

### 4. Back → tap FAB → sheet → "New registry" → CreateRegistry
expected: bottom nav HIDDEN
result: pending

### 5. Tap Stores slot → StoreList
expected: bottom nav HIDDEN (intentional Phase 9 change — deviates from quick-task 260420-iro persistent-everywhere)
result: pending

### 6. Tap "You" slot → Settings
expected: bottom nav HIDDEN
result: pending

### 7. Back to Home
expected: bottom nav visible again
result: pending

### 8. FAB inspection (CHROME-02)
expected: 54 dp accent-filled circle, ~4 dp paper ring visible against nav background, soft accent-tinted shadow, lifted ~22 dp above bar baseline, centred between Stores and Lists, mono-caps "ADD" caption below at bar baseline
result: partial — validated via quick-260421-moi nav weighting fix; 4-dp paper ring + shadow still need on-device confirmation

### 9. Tap the FAB
expected: AddActionSheet slides up with scrim dimming content behind
result: pending

### 10. AddActionSheet layout inspection (CHROME-03)
expected: top corners rounded 22 dp; drag handle 36×4 dp pill; italic serif title "What are you adding?"; 4 rows (New registry accentSoft primary, Item from URL / Browse stores / Add manually all paperDeep); chevron right on each row
result: pending

### 11. API 31+ blur behind scrim
expected: background home content subtly blurred behind scrim
result: pending

### 12. Dismiss sheet (swipe down or tap scrim)
expected: sheet dismisses
result: pending

### 13. FAB → "New registry" row
expected: sheet dismisses, navigates to CreateRegistry form
result: partial — validated indirectly via quick-260421-moi (RegistryListScreen FAB removed; CreateRegistry flow now exclusively through this sheet path)

### 14. FAB → "Browse stores" row
expected: navigates to StoreList with isPrimary registry id pre-selected (or null if no registries — graceful no-op)
result: pending

### 15. Reserved chip on reserved item (STAT-01)
expected: filled accent pill with (a) pulsing dot left at ≈1.4 s cadence, (b) countdown like "23m" in accent-ink, (c) "RESERVED" mono-caps label; wait ≥60 s → countdown decrements
result: pending

### 16. Open chip on available item (STAT-03)
expected: transparent outlined pill with thin line border + "OPEN" inkFaint label
result: pending

### 17. Given chip on purchased item (STAT-02)
expected: secondSoft filled pill with "✓ GIVEN" in second-colour text
result: pending

### 18. Purchased row visual treatment (STAT-04)
expected: whole row at ≈55 % opacity; image grayscale with ink tint + centred ✓; title strikethrough — row STILL VISIBLE in list
result: partial — row-level alpha ships in Phase 9; element-level grayscale/checkmark/strikethrough are Phase 11 work per PurchasedRowModifier KDoc

### 19. Backstack — FAB → "New registry" → fill → submit
expected: lands on RegistryDetail for new registry (NOT a new Home above the old one)
result: pending

### 20. Back button from new RegistryDetail
expected: pops to Home; no leaked screens
result: pending

### 21. RO locale switch
expected: nav labels "ACASĂ / MAGAZINE / ADAUGĂ / LISTE / TU"; chip labels "REZERVAT / OFERIT / DISPONIBIL"; sheet title "Ce adaugi?"
result: pending

## Summary

total: 21
passed: 0
issues: 0
pending: 18
skipped: 0
blocked: 0
partial (validated indirectly via follow-up quick tasks): 3 (items 8, 13, 18)

## Gaps

_(none triaged — user deferred UAT to unblock Phase 10/11. Will appear in `/gsd:progress` and `/gsd:audit-uat` until resolved.)_
