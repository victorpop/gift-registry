# Phase 12: Registry Cover Photo & Themed Placeholder - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-27
**Phase:** 12-registry-cover-photo-themed-placeholder
**Mode:** discuss
**Areas discussed:** Photo source & storage backend, Card placeholder visual, Predefined occasion catalog (user-introduced), Picker UI & placement, Edit-flow scope, Image constraints, Order gating

---

## Gray Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| Photo source & storage backend | Where the photo comes from + where it goes (Storage vs URL-paste) | ✓ |
| Card placeholder visual | Mirror Phase 11 hero pattern or do something distinct | ✓ |
| Picker placement & edit-flow scope | Where the cover-photo control lives in Create + whether Edit + tap-on-detail | (folded into follow-up) |
| Image constraints & upload UX | Crop / compression / progress UI | (folded into follow-up) |

**User free-text addition:** "I want to have a few predefined photos for each occasion, but also let user pick a photo from their gallery."

---

## Photo Source & Storage Backend

### Q1: Where should the predefined occasion photos live?

| Option | Description | Selected |
|--------|-------------|----------|
| Bundled in app drawables | Photos ship inside the APK as drawable-xxhdpi resources. No network, instant render, no Storage cost. Tradeoff: catalog updates require Play Store release. | ✓ |
| Firebase Storage public bucket + Firestore catalog doc | Photos in Storage; Firestore doc maps occasion → URLs. Catalog can change without Play release; adds first-launch network dependency and Storage cost. | |
| Hybrid — bundled fallback set + Firestore override | Always have a bundled set (works offline, day-one), but check Firestore for an override list. More code paths to maintain. | |

**User's choice:** Bundled in app drawables.

---

### Q2: Where does a gallery-picked photo get stored after the user selects it?

| Option | Description | Selected |
|--------|-------------|----------|
| Firebase Storage | Upload to /users/{uid}/registries/{registryId}/cover.jpg, store the public download URL on Registry.imageUrl. Adds firebase-storage to the BoM bundle, needs Storage rules + App Check. Works cross-device and for guest web viewers. | ✓ |
| Local content URI on the device | Save the file:// or content:// URI on Registry.imageUrl. No upload, no Storage cost. Tradeoff: image disappears on device wipe and is invisible to web fallback / other devices — breaks the share link for guests. | |

**User's choice:** Firebase Storage.

---

### Q3: How many predefined photos per occasion?

| Option | Description | Selected |
|--------|-------------|----------|
| 3 per occasion | Lean — 18 images total. Fastest to curate and lowest APK / Storage footprint. | |
| 6 per occasion | 36 images total. Fits a 2x3 grid in the picker. More visual variety. | ✓ (via free-text) |

**User's choice (free text):** "user will select only one photo per occasion as a cover photo, so we don't need to have a scroll. Library should have 6 options for each occasion."

**Notes:** Each registry has exactly one cover photo — no rotation. Picker shows 6 thumbnails (3×2 grid) so the user picks one. This clarification fed into D-03 in CONTEXT.md.

---

### Q4: When no photo is picked at all (skipped), what should the registry card and detail hero show?

| Option | Description | Selected |
|--------|-------------|----------|
| Themed placeholder (gradient + occasion glyph) | Mirror the Phase 11 hero pattern on cards too — accentSoft→accent gradient + Instrument Serif italic glyph. Cards and hero look consistent. | ✓ |
| Auto-assign occasion preset #1 on save | Every registry always has an imageUrl. Never see the gradient placeholder in production. | |

**User's choice:** Themed placeholder (gradient + occasion glyph).

---

## Picker UI & Edit Scope

### Q5: What should the picker UI look like in the Create / Edit registry flow?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline section in the form | A 16:9 preview block at the top of the form (above occasion tiles). Tapping it opens a bottom sheet with the 6 presets + 'Pick from gallery' button. Sheet updates whenever occasion changes. | ✓ |
| Dedicated step | Make 'Pick a cover' a visible step between occasion tiles and the name field. Larger 6-tile grid + gallery button rendered inline (no sheet). | |
| Optional FAB-style icon on the preview | Show only the placeholder by default; a small camera/pencil button overlay opens the same sheet. Lower visual weight — user might miss the feature. | |

**User's choice:** Inline section in the form.

---

### Q6: Which screens get the cover-photo picker? (multiSelect)

| Option | Description | Selected |
|--------|-------------|----------|
| Create registry | Picker lives in CreateRegistryScreen for new registries. | ✓ |
| Edit registry | Same picker also reachable when editing an existing registry. CreateRegistryScreen handles edit mode, so this is a flag check. | ✓ |
| Tap-to-change on Registry Detail hero | Owner taps the 180px hero on RegistryDetailScreen — opens the same picker sheet. | ✓ |

**User's choice:** All three (owner-only on Detail per security rules already in place).

---

### Q7: What constraints / processing should we apply to gallery-picked photos before upload?

| Option | Description | Selected |
|--------|-------------|----------|
| Compress + downscale to 1280x720 (16:9), no crop UI | Auto-resize to 1280x720 max, JPEG q=85. Hero (180dp) and cards (16:9) both use ContentScale.Crop, so the image fits without an explicit crop screen. Predictable upload size (~150-300KB). | ✓ |
| Show a 16:9 crop UI before upload | User adjusts crop to match exactly what shows on the card hero. More control, but adds an extra screen / library (e.g. uCrop). | |
| Upload as-is | No processing. Smallest code, but uploads can be 5-10MB+ and waste Storage egress. | |

**User's choice:** Compress + downscale to 1280×720 (16:9), no crop UI.

---

### Q8: When the user is choosing a preset, what happens if they haven't picked an occasion yet?

| Option | Description | Selected |
|--------|-------------|----------|
| Force occasion selection first | Cover-photo block shows the gradient placeholder with text 'Pick an occasion to see suggested covers'. The 'Pick a cover' tap is disabled until an occasion is chosen. Gallery picker still available. | ✓ |
| Show all occasions' presets in picker | Picker sheet shows all 36 presets grouped by occasion header. User can pick any preset, which also auto-selects that occasion. | |

**User's choice:** Force occasion selection first.

**Note:** Per CONTEXT.md D-12, gallery picking is also gated until occasion exists — the upload path needs the registryId/occasion context to determine the Storage path and the gallery flow happens during onSave() anyway, so the gating extends consistently.

---

## Wrap-up

### Q9: Ready to capture context, or want to dig further?

| Option | Description | Selected |
|--------|-------------|----------|
| I'm ready for context | Write CONTEXT.md with the decisions captured. Anything I haven't asked about will be marked Claude's Discretion or deferred. | ✓ |
| Explore more gray areas | Surface 2-3 more decisions worth input — e.g. preset identifier encoding, web-fallback rendering, Remove action UX. | |

**User's choice:** I'm ready for context.

---

## Claude's Discretion

Items not asked because they're implementation details the planner / researcher can resolve:

- Preset selection encoding on `Registry.imageUrl` (sentinel scheme vs `android.resource://` URI vs Firebase Storage URLs for presets too).
- Web fallback rendering for preset-backed registries.
- "Remove cover photo" action UX placement (sheet menu item vs long-press).
- Exact 6-image curation per occasion (asset acquisition is a planning task).
- `BitmapFactory` / Coil 3 transformation choice for image resize.
- Final Firebase Storage rules wording (baseline given in D-08).

## Deferred Ideas

- Per-occasion theme cascade (THEME-01/02/03) — still v1.2.
- Camera capture — user said no.
- URL paste source — user said no.
- Image cropping UI — auto-resize covers the need.
- Per-item image upload — separate scope.
- Multi-image carousel / cover photo rotation.
