---
phase: quick-260530-uq3
plan: 01
subsystem: android-ui
tags: [non-owner, product-details, localization, EditItemScreen]
dependency_graph:
  requires: []
  provides: [non-owner-product-details-ui-polish]
  affects: [EditItemScreen.kt]
tech_stack:
  added: []
  patterns: [isOwner-conditional-rendering, ACTION_VIEW-intent, AutoMirrored-icon]
key_files:
  created: []
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
decisions:
  - "Reused existing inline runCatching { startActivity(Intent(ACTION_VIEW, ...)) } pattern (already at line 103-107 in the same file) — no new utility module for a single call site"
  - "IconButton placed OUTSIDE OutlinedTextField in a Row to ensure it is tappable despite the field being enabled=false"
  - "Used Icons.AutoMirrored.Outlined.OpenInNew per plan brief (LTR/RTL-aware)"
  - "Romanian î encoded as \\u00ee to match surrounding file style (e.g. common_loading, common_retry)"
metrics:
  duration: ~2min
  completed_date: "2026-05-30"
  tasks_completed: 2
  tasks_outstanding: 1
  files_modified: 3
---

# Quick Task 260530-uq3: Non-owner Product Details Screen Polish Summary

**One-liner:** Conditional title swap, image-URL field removal, URL label rename, and OpenInNew icon for the non-owner/invitee branch of EditItemScreen.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add 3 new localized strings (en + ro) | 850f6e4 | values/strings.xml, values-ro/strings.xml |
| 2 | Wire non-owner mode in EditItemScreen.kt | 1114588 | EditItemScreen.kt |
| 3 | Human verification on emulator | outstanding | — |

## What Was Built

**Task 1 — String resources (850f6e4):**

Three new keys added to both `values/strings.xml` (English) and `values-ro/strings.xml` (Romanian), inserted at the bottom of the "Item management (Phase 3)" block (after `item_og_fetch_failed`, before the Reservation comment):

| Key | English | Romanian |
|-----|---------|----------|
| `product_details_title` | Product details | Detalii produs |
| `product_url_label` | Product URL | URL produs |
| `product_url_open_in_browser_desc` | Open in browser | Deschide în browser |

Romanian `î` encoded as `î` to match surrounding file style.

**Task 2 — EditItemScreen.kt (1114588):**

Four targeted edits, all conditioned on the existing `isOwner` StateFlow:

1. **Import:** `androidx.compose.material.icons.automirrored.outlined.OpenInNew` added in alphabetical order.
2. **TopAppBar title:** `if (isOwner) R.string.item_edit_title else R.string.product_details_title` — owners still see "Edit Item" / "Editeaza articolul".
3. **Image-URL field removed** from invitee branch: the `OutlinedTextField` labelled `item_image_label` is gone; the preceding `AsyncImage` preview block is kept.
4. **URL Row in invitee branch replaced:** now a `Row` containing a re-labelled read-only field (`product_url_label`) plus a conditional `IconButton` with `Icons.AutoMirrored.Outlined.OpenInNew` that fires `Intent.ACTION_VIEW` when `url.isNotBlank()`.

**Compile result:** `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (3 pre-existing warnings in unrelated files, out of scope).

## Verification Done Criteria Passed

- [x] `grep` returns 6 hits for the 3 new keys across both string files (3 per file)
- [x] `grep -c "item_image_label" EditItemScreen.kt` → 1 (owner branch only)
- [x] `grep -n "item_add_url_label" EditItemScreen.kt` → exactly 1 hit at owner branch line 192
- [x] `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- [ ] Manual visual verification on emulator (Task 3 — outstanding)

## Task 3: Outstanding (human-verify checkpoint)

Task 3 is a `checkpoint:human-verify` — the orchestrator will handle install and verification. The verification checklist:

- [ ] Non-owner sees "Product details" (en) / "Detalii produs" (ro) as TopAppBar title
- [ ] Image URL text field is gone in non-owner mode; image preview still shown
- [ ] URL field labelled "Product URL" / "URL produs" (not "Paste product URL")
- [ ] OpenInNew icon appears next to URL field when item has a URL; absent when no URL
- [ ] Tapping the icon opens the URL in the device browser
- [ ] Owner mode unchanged: "Edit Item" title, "Paste product URL" label, image URL field present

## Deviations from Plan

None — plan executed exactly as written. The `î` encoding fix was a minor cosmetic alignment to match the surrounding file style, not a deviation from plan intent.

## Known Stubs

None — all wired to real data (isOwner StateFlow, url StateFlow from ViewModel).

## Self-Check: PASSED

Files exist:
- app/src/main/res/values/strings.xml — FOUND (3 new keys confirmed by grep)
- app/src/main/res/values-ro/strings.xml — FOUND (3 new keys confirmed by grep)
- app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt — FOUND (OpenInNew import + wiring confirmed)

Commits exist:
- 850f6e4 — feat(quick-260530-uq3): add 3 new localized strings
- 1114588 — feat(quick-260530-uq3): wire non-owner product details view
