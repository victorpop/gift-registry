---
date: 2026-04-27
category: assets
phase_origin: 12-registry-cover-photo-themed-placeholder
plan_origin: 12-02
priority: medium
---

# Curate real preset JPEGs for Phase 12 cover-photo picker

## Context

Phase 12 ships 36 placeholder JPEGs in `app/src/main/res/drawable-xxhdpi/preset_*.jpg`
(6 presets × 6 occasions). The current set is colour-block stand-ins generated at
plan-execution time — solid GiftMaison-toned 1280×720 frames with the occasion
glyph + index drawn on top.

`12-CONTEXT.md` D-02 explicitly accepts placeholder filler images for Wave 1:

> "Exact 6-image curation per occasion — 36 stock-licensed JPEG files
>  (1280×720 each, owner-flow appropriate). Plan should call out the
>  asset-acquisition task explicitly; placeholder filler images are
>  acceptable for the first execution wave if real curation slips."

## What's needed

Replace the 36 placeholders with real, stock-licensed, owner-flow-appropriate
photographs at 1280×720 (16:9), JPEG quality ~85, target ~50–250 KB each:

- 6 × Housewarming
- 6 × Wedding
- 6 × Baby
- 6 × Birthday
- 6 × Christmas
- 6 × Custom (generic celebration imagery)

File names are locked (`preset_{occasion}_{1..6}.jpg`) — replace bytes only.
Drawable resource IDs and sentinel format `preset:{occasion}:{index}` must
remain identical so existing Firestore documents keep resolving.

## Acceptance

- [ ] All 36 files are real photographs (not solid-colour placeholders)
- [ ] Each file is 1280×720, JPEG, ≤ ~250 KB
- [ ] Stock licence cleared for in-app commercial use (AGPL-incompatible
      sources rejected)
- [ ] APK builds and the picker renders the new tiles without code changes
- [ ] Aim to land before v1.1 GA

## References

- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-CONTEXT.md` (D-02)
- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-02-PLAN.md`
- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-02-SUMMARY.md`
- `app/src/main/java/com/giftregistry/ui/registry/cover/PresetCatalog.kt`
- `app/src/main/res/drawable-xxhdpi/preset_*.jpg`
