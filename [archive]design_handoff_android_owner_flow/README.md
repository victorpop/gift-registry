# Handoff — GiftMaison (Android owner flow)

> **GiftMaison** is a gift-registry app. This handoff covers the **owner-facing Android flow**: the screens an event owner (Ana, Mihai, a parent-to-be) sees when creating and managing their registry. The giver-facing web flow is out of scope for this package.

---

## About the design files

The files in `reference/` are **design references, not production code**. They are HTML/React prototypes built for visual communication — the layout, spacing, typography, colour tokens, and interactions are the contract; the code itself is not meant to be copy-pasted.

Your job: **recreate these screens in the target Android codebase** (Jetpack Compose preferred; Material 3 as baseline) using the app's existing architecture, navigation, and patterns. If the project is greenfield, choose Jetpack Compose + a single-activity NavGraph + Kotlin; nothing here requires a legacy XML view system.

## Fidelity

**High-fidelity.** Colours (oklch), type scale, spacing, border radii, shadows, and copy are all final unless explicitly flagged. Implement pixel-accurate.

One deviation to plan for: the reference uses `oklch()` for colour tokens (more perceptually-even across the four occasion themes). On Android, **convert each token to sRGB hex** in your colour resources — the occasion-switching logic doesn't need oklch at runtime. Converted values are in the [Design tokens](#design-tokens) table.

---

## Contents of this handoff

```
design_handoff_android_owner_flow/
├── README.md                                    (this file)
└── reference/
    ├── GiftMaison - gift registry mocks.html        (the full prototype — open in a browser)
    ├── android-frame.jsx                        (the device-bezel wrapper used in the mocks)
    └── theme.jsx                                (all colour tokens + type scale + sample data)
```

Open the HTML in any modern browser. The Android artboards are the bottom row (06–10). Use **Tweaks** in the toolbar to swap the occasion theme (housewarming / wedding / baby / birthday) and see how tokens cascade.

---

## Screens

There are **5 Android screens** plus a **bottom-sheet modal**. Numbering matches the labels in the prototype.

| # | Screen | Route suggestion | Bottom nav visible? |
|---|---|---|---|
| 06 | Onboarding + sign up | `onboarding/auth` | No (full-bleed) |
| 07 | Home — all registries | `home` | Yes (**Home** active) |
| 08 | Registry detail — items list | `registry/{id}` | Yes (**Lists** active) |
| 09 | Create registry | `registry/new` | No (modal flow) |
| 10 | Add item · paste URL | `registry/{id}/add` | No (modal flow) |
| — | Add action sheet (FAB tap) | bottom sheet over `home` | — |

### Device canvas

All screens are designed for a **340 × 720 logical** viewport (inside a device bezel). At real-device densities this maps comfortably to 360 dp width — use that as your Compose preview width. The status bar above and gesture handle below are OS chrome, not part of the screen.

---

### 06 · Onboarding + sign up

**Purpose:** first-run. New user creates an account to start a registry. Also used as the sign-in entry point (single screen, "Log in" pill at the bottom switches mode).

**Layout, top to bottom:**
1. Top bar — `giftmaison.` wordmark left, "Need help?" text link right
2. Headline: `Start your` / `first registry.` (second line italic, accent colour)
3. Subline: "A warm, shareable list — set up in under a minute."
4. **Google banner** — large terracotta-accent CTA (see detail below)
5. Divider with "or sign up with email" caption
6. First name / Last name row (2-col grid)
7. Email field (focused — accent border, blinking caret)
8. Password field (dotted mask, eye-slash toggle at right, helper: "At least 8 characters with one number.")
9. **Sign up** — primary full-width pill, ink background
10. Terms line — "By continuing you agree to our Terms of Service and Privacy."
11. Footer pill — "Already have an account? **Log in**" (ghost pill, switches to sign-in mode)

**Google banner spec (important — user iterated on this):**
- Rounded 16 radius, padding `14 16`, background = accent, shadow `0 10 24 {accent}55`
- Left: **20×20** white circle containing the 12×12 Google G glyph
- Centre: "Continue with Google" label (600 weight, 15 px, letter-spacing −0.2), vertically centred in a 40-height flex row
- Right: italic display-serif "→" arrow, opacity 0.9
- Two decorative concentric rings absolute-positioned in the top-right (subtle `{accentInk}25` and `{accentInk}18` strokes — purely ornamental, clip with `overflow: hidden`)

**Empty-state note:** the field values in the prototype ("Ana" / "Popescu" / "ana.popescu@gmail.com") are placeholder copy, shown in `inkFaint`. Implement as standard hint text.

---

### 07 · Home — all registries

**Purpose:** logged-in landing. Lists the registries the user owns. Switches between Active / Drafts / Past.

**Layout:**
1. Top bar — `giftmaison.` wordmark left, avatar (30 px circle, initials on `second` green) right
2. `Your registries` display-serif headline (32 px)
3. Caption — "3 active · 34 items total" in mono caps
4. Segmented control pill — Active / Drafts / Past (first selected, white pill on `paperDeep` track)
5. Scrolling vertical list of **registry cards**, `padding-bottom: 100` to clear the floating FAB

**Registry card:**
- 16 radius, overflow-hidden
- Top: 16:9 hero image
  - Tiny occasion pill top-left ("HOUSEWARMING" etc. — mono 9.5, white pill on image)
  - Date bottom-right (mono, paper colour)
- Body: 12/14 padding
  - Display-serif title (18 px, letter-spacing −0.3, line-height 1.1)
  - Stats row: `{n} items · {n} reserved · {n} given`, mono caps, bullet separators (2×2 dots)

**Primary card variant:** the user's "current" registry renders with `background: ink`, `color: paper`, no border, image darkened to 70% brightness. All other cards use `paperDeep` bg, `line` border, full-brightness image. **Only one card is primary at a time** (the most-recently-active registry).

---

### 08 · Registry detail — items list

**Purpose:** owner views a single registry, its items, and which are reserved/given.

**Layout, top to bottom:**
1. **180 px hero image** with parallax behaviour on scroll (optional in v1 — a pinned toolbar that fades to opaque as the list scrolls is the MVP)
   - Gradient overlay: `ink44` top → transparent 40% → `inkAA` bottom
   - Top row over image: back button (paper-ish circle, 32 px), share ↗ button, overflow ⋯ button
   - Bottom-left of image: tiny occasion/date pill with `backdrop-filter: blur(8px)`, display-serif title (24 px), "housewarming" in italic
2. **Stats strip** — horizontal row of 4 stats (items / reserved / given / views), each number in display serif 22 px, label in mono caps 8.5. Divider lines between them (`1px solid {line}`)
3. **Share banner** — 10-radius pill in `accentSoft`, 26 px accent square with ↗ glyph, URL in mono accent weight 600, helper "Tap to copy or share" below. Whole row is a tap target.
4. **Filter chips** — horizontal scroll: All 12 · Open 9 · Reserved 2 · Completed 1. Active chip is ink-filled with paper text; inactive are outlined with `line` border and show the count in a fainter tone.
5. **Full-width item rows** (NOT a 2-col grid — the user specifically asked for list rows). Each row:
   - 58×58 10-radius thumbnail, left
   - Title (body 13, 500, letter-spacing −0.1, single line with ellipsis) + price row ("189 RON · emag.ro" — price in body, store in mono)
   - Optional sub-line: "Reserved by Ioana M." (accent) or "Given by Maria" (second)
   - Right column: status chip stacked above a 26×26 ⋯ circle button (`line` border)
   - `borderBottom: 1px solid {line}` except last row
6. Bottom padding 90 px to clear nav.

**Status chips:**
- **Reserved** — pill `bg: accent, color: accentInk, mono 8.5 caps 600`, pulsing 4×4 dot on left, "23m" countdown. The dot animation is `pulse` keyframe (opacity 1 ↔ 0.5, scale 1 ↔ 0.85, 1.4s ease-in-out infinite).
- **Given** — pill `bg: secondSoft, color: second`, "✓ given"
- **Open** — outlined pill, `bg: transparent, border: line, color: inkFaint`, "open"

**Purchased-item treatment:** row opacity 0.55, image `filter: grayscale(1)` with a `{ink}40` tint overlay + a centred ✓ mark, title strikethrough. This is a deliberate trust pattern — givers who later view the public page also see given items (still visible, not hidden) so nobody duplicates.

---

### 09 · Create registry

**Purpose:** step 1 of 2 when starting a new registry — pick occasion, name, date, place, visibility.

**Layout:**
1. App bar — back ← button, "Step 1 of 2" mono caption centered, "Skip" ghost link right
2. Display-serif headline: "What's the **occasion?**" (last word italic, accent)
3. Subline: "We'll set the tone, suggested categories, and email theme to match."
4. **2×3 grid of occasion tiles** — 14 radius, 14/16 padding, 6 gap column, 10 gap row
   - Each tile: italic display-serif glyph at top (`⌂ ♡ ◐ ✦ ❅ +`, 22 px, accent colour), then label below (body 13, 500)
   - Selected tile: `bg: accent, color: accentInk, border: accent` (1.5 px)
   - Unselected: `bg: paperDeep, color: ink, border: line` (1.5 px)
5. Form fields, 14 gap:
   - Registry name (full width)
   - Date / Time (2-col grid, 10 gap)
   - Place (full width)
6. Visibility card — `paperDeep` bg, `line` border, 12 radius, 12 padding. Mono caps label "VISIBILITY". Two radio rows — Public / Private. Selected radio is 18 px circle with 2 px accent border containing an 8 px accent dot.
7. Bottom CTA bar — `1px line` border-top, `paper` bg, 12/20 padding. Primary pill: "Continue · add items →", full-width.

---

### 10 · Add item · paste URL

**Purpose:** owner pastes a product URL from a retailer; app fetches Open Graph metadata, applies affiliate tag invisibly, previews the item.

**Layout:**
1. App bar — × close button, "Add item" centred title, (reserved space on right)
2. **Segmented tabs** — 3 options inside a pill track: Paste URL (selected) / Browse stores / Manual
3. **URL field** — 12 radius, `paperDeep` bg, 1.5 px accent border:
   - Mono caps line: "⌕ Fetching from emag.ro" with a pulsing 4 px accent dot
   - The URL itself in mono 11.5, `inkSoft`, with the `/pd/DSGB42BBM/` suffix at 0.5 opacity (de-emphasises the query string / product code)
   - Divider (1 px line), then a row: "✓ Affiliate tag applied invisibly" in mono `ok`-green left, ghost "clear" button right
4. **Preview card** — 14 radius, `paperDeep`, `line` border:
   - 80×80 8-radius thumbnail left
   - Title (body 14, 500), price (body 13 weight 500 + "RON" in mono tiny), source line "emag.ro · via Open Graph" (mono caps)
5. Form fields, 12 gap:
   - Title — pre-filled, with small green "✓ auto-filled" tag next to the label
   - Note for gift givers (optional) — placeholder: "e.g., in silver, please — matches the kitchen"
6. Info pill — `secondSoft` background, 10 radius, ℹ glyph + caption: "We recognized emag.ro and will add our affiliate tag. No impact on your guests."
7. Bottom CTA bar — two buttons: ghost "Add another" (flex 1) + primary "Save to registry ✓" (flex 1.5)

**Affiliate-tag copy is a non-negotiable trust signal.** Givers must never see it. Owners must always be reassured it exists and they benefit (or whatever the business side decides). Don't hide the fact; don't broadcast it.

---

### Bottom sheet — Add action sheet (FAB tap)

Triggered from the centre **+** FAB on Home. Dismisses on scrim tap / swipe down.

- Scrim: `{ink}55` over the blurred home screen (`filter: blur(1px)`, opacity 0.3)
- Sheet: 22 radius top corners, 20/18 padding, `-10 40 shadow` above
- Drag handle: 36×4 pill, `line` colour, centred, 14 gap below
- Title: display-serif "What are you adding?" (22 px)
- 4 action rows, 10 gap:
  - New registry (accent-filled icon square, `accentSoft` row bg) — primary action
  - Item from URL (paper icon square with accent glyph, `paperDeep` row bg)
  - Browse stores (same treatment)
  - Add manually (same treatment)
- Each row: 36×36 10-radius icon square with italic display-serif glyph, heading body 13.5 weight 500, subtitle body 11.5 in `inkSoft`, chevron `›` at right

---

## Shared chrome

### Bottom nav

Present on screens 07 and 08. Hidden on 06, 09, 10 (full-bleed flows).

- Background `paper`, 1 px `line` border-top, 4/4/6 padding
- 5 slots: **Home · Stores · [+FAB] · Lists · You**
- Tabs are column stacks: 22×22 stroked icon (1.6 stroke, linecap round) inside a 999-radius pill that fills with `accentSoft` when selected; label below in mono 9.5 caps
- Selected state: icon stroke = accent, label = accent 600; unselected: `inkFaint`, 500
- **Centre FAB**: 54 px circle, `bg: accent, color: accentInk`, floating at `top: -22` relative to its slot so it lifts ~half its height above the bar. Shadow `0 8 20 {accent}55` + a 4 px paper ring (`0 0 0 4 paper`) to separate it from the bar
- The FAB's slot also shows a tiny mono "ADD" caption at the bar's baseline

### Status bar (top)

Standard Android status bar, 34 px tall. For Compose, use `WindowCompat` to draw behind it and inset your content. The reference shows 9:41 time left, a camera-cutout pill top-centre (22 px circle), and signal/battery icons right.

### System gesture handle (bottom)

A 100×4 pill, 40% opacity ink. Standard Android 10+ handle; no design work needed beyond respecting the gesture-inset.

---

## Interactions & behaviour

### Reservation timer (cross-cutting)

Items have 3 lifecycle states: `open → reserved → given` (with auto-release back to `open` if no confirmation in 30 minutes).

- **open** → any giver can reserve. Chip = outlined "open".
- **reserved** → one giver holds it for 30 min. Chip = filled accent + pulsing dot + "Nm" countdown.
  - Countdown updates once per minute (not once per second — reduces re-render and matches the web "23 MIN LEFT" cadence).
  - On timer expiry with no confirmation: state reverts to `open`, giver receives a re-reserve email link.
- **given** → giver confirmed purchase. Chip = `secondSoft` "✓ given". Item becomes strikethrough + grayscale but stays in the list.

On the **owner** screens documented here, the timer is display-only — owners can't manually reserve/release on behalf of givers.

### Tab switching (Home)

Active / Drafts / Past is a cheap local filter on the registries list — no network round-trip needed if the app has all three loaded. If paginated, Active is the only eagerly-loaded bucket.

### URL paste (Add item)

On paste:
1. Show "⌕ Fetching from {domain}" immediately with the pulsing dot
2. Hit backend endpoint `/og-scrape?url={…}` (or equivalent) — returns `{title, image, price, currency, retailer}`
3. Populate preview card + form fields
4. If the domain matches an affiliate partner (maintained list: emag.ro, dedeman.ro, zara.home, etc.), silently append the tracking parameter server-side and show the "✓ Affiliate tag applied invisibly" line
5. On scrape failure: collapse the preview card, show a ghost error "We couldn't read that page — fill in the details below" and let the user manually complete the fields

### Create registry — step 1 → 2

"Continue · add items →" dismisses this screen and pushes the Add-item screen (screen 10) onto the stack with a fresh registry id. Skip from the top bar accepts the typed fields as-is and goes straight to Home, creating a Draft registry.

### Navigation patterns

- 06 is the root route when unauthenticated; after sign-up it replaces itself with 07
- 07 → 08 via tapping a registry card
- 08 → 10 via bottom-sheet → "Item from URL" (FAB is the only entry point)
- 09 is reached via bottom-sheet → "New registry" from Home
- 08 back button pops to 07; × on 09/10 pops to whatever was underneath (usually 07)

---

## State management

Minimum viable state shape:

```kotlin
data class User(val id: String, val firstName: String, val lastName: String, val email: String, val avatarSeed: String)

enum class Occasion { Housewarming, Wedding, Baby, Birthday, Christmas, Custom }
enum class Visibility { Public, Private }

data class Registry(
    val id: String,
    val ownerId: String,
    val name: String,
    val occasion: Occasion,
    val date: LocalDate,
    val time: LocalTime?,
    val place: String?,
    val visibility: Visibility,
    val heroImage: String?,
    val isPrimary: Boolean,           // UI hint — surfaces this card as the dark "current" card on Home
    val stats: RegistryStats,
)

data class RegistryStats(val items: Int, val reserved: Int, val given: Int, val views: Int)

enum class ItemStatus { Open, Reserved, Given }

data class RegistryItem(
    val id: String,
    val registryId: String,
    val title: String,
    val image: String,
    val price: Money,                 // amount + currency
    val retailer: String,             // display name, e.g. "emag.ro"
    val productUrl: String,           // affiliate-tagged, server-side
    val status: ItemStatus,
    val reservation: Reservation?,    // null unless status == Reserved
    val gift: Gift?,                  // null unless status == Given
    val ownerNote: String?,
)

data class Reservation(val byName: String, val reservedAt: Instant, val expiresAt: Instant)
data class Gift(val byName: String, val givenAt: Instant)
```

- The reservation timer is **authoritative on the server** — the client computes `minutesLeft = max(0, expiresAt - now)` and re-renders every 60s. Don't rely on client-scheduled timers for state transitions.
- `isPrimary` on Home can be a simple `registries.firstOrNull()?.id`-style UI decision; not a persisted flag unless product wants pinning later.
- Affiliate-tag application is entirely backend — the client never sees the untagged URL.

---

## Design tokens

All tokens live in `reference/theme.jsx` as `oklch()`. Convert to sRGB hex for Android `colors.xml`. Values below are the **Housewarming** theme (default). Generate equivalents for Wedding / Baby / Birthday from `theme.jsx`.

### Colour tokens (Housewarming)

| Token | oklch (source) | sRGB hex (approx) | Usage |
|---|---|---|---|
| `paper` | `oklch(0.972 0.012 75)` | `#F7F2E9` | Page bg, elevated surfaces, pills-on-dark |
| `paperDeep` | `oklch(0.94 0.018 72)` | `#EDE5D5` | Cards, segmented-track bg, field bg |
| `ink` | `oklch(0.22 0.015 50)` | `#2A2420` | Primary text, primary buttons |
| `inkSoft` | `oklch(0.42 0.02 55)` | `#6A5E52` | Body text, secondary labels |
| `inkFaint` | `oklch(0.62 0.025 60)` | `#9C8E7F` | Placeholders, tertiary text, inactive icons |
| `line` | `oklch(0.88 0.015 70)` | `#DDD4C4` | Borders, dividers |
| `accent` | `oklch(0.58 0.15 38)` | `#C8623A` | Terracotta — brand accent, selected states, Reserved |
| `accentInk` | `oklch(0.98 0.01 75)` | `#FCF8EF` | Text on accent surfaces |
| `accentSoft` | `oklch(0.92 0.04 42)` | `#F3DED0` | Accent backgrounds, selected-tab pill |
| `second` | `oklch(0.48 0.07 145)` | `#4F7050` | Olive — Given state, avatar bg |
| `secondSoft` | `oklch(0.9 0.03 135)` | `#D7E2CE` | Olive backgrounds, info pills |
| `ok` | `oklch(0.58 0.11 150)` | `#4F9668` | Success text (affiliate tag line) |
| `warn` | `oklch(0.68 0.14 65)` | `#D29447` | Warnings (not used on Android flows yet) |

Hex values are sRGB approximations of the oklch sources. If your colour pipeline supports wide-gamut, prefer converting oklch directly — the oklch values are authoritative.

### Other themes

`reference/theme.jsx` defines **Wedding** (rose), **Baby** (dusty blue + mustard), **Birthday** (marigold + plum). Same token names, different values. The owner-facing screens use the registry's occasion theme for accent/accentSoft/accentInk/second/secondSoft; the greyscale tokens (`paper` through `line`) shift only subtly per theme (they pick up a hint of the accent's hue).

### Type scale

| Role | Font | Weight | Size | Letter-spacing | Line-height |
|---|---|---|---|---|---|
| Display, XL (h1) | Instrument Serif | 400 | 32 | −0.8 | 1.0 |
| Display, L (hero) | Instrument Serif | 400 | 24 | −0.4 | 1.05 |
| Display, M | Instrument Serif | 400 | 22 | −0.4 | 1.1 |
| Display, S (card title) | Instrument Serif | 400 | 18 | −0.3 | 1.1 |
| Body L | Inter | 500 | 15 | −0.2 | 1.35 |
| Body M (default) | Inter | 400–500 | 13.5 | −0.1 | 1.45 |
| Body S | Inter | 400–500 | 12.5 | 0 | 1.4 |
| Body XS | Inter | 400 | 11.5 | 0 | 1.35 |
| Mono caps (label) | JetBrains Mono | 500–600 | 9.5–11 | 0.6–1.5 | 1.3 |

**Italic display serif** is used for expressive accents — the second line of a headline, a glyph in an occasion tile, the "↗" on the Google banner. Always paired with `accent` colour.

Android: pull Instrument Serif, Inter, and JetBrains Mono from Google Fonts. All three are available on Google Fonts.

### Spacing / radii / shadows

- **Edge padding:** `16–20` on screens. Full-bleed heroes bleed to 0.
- **Radii:** `8` (thumbnails), `10` (small cards, chips), `12` (inputs), `14` (tiles), `16` (cards), `22` (bottom sheet), `40` (device bezel), `999` (pills)
- **Borders:** almost always `1px solid {line}` — except tiles (1.5 px), focused inputs (1.5 px accent)
- **Shadows:** deliberately minimal. The FAB uses `0 8 20 {accent}55`. The Google banner uses `0 10 24 {accent}55`. Bottom sheet uses `0 −10 40 rgba(0,0,0,0.15)`. Cards otherwise rely on `line` borders, not shadow.

---

## Animations

Single keyframe used throughout:

```css
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(0.85); }
}
```

Applied to:
- The 4 px dot next to reserved chips (1.4 s infinite)
- The 4 px dot on the "Fetching from…" URL field (1.0 s infinite)
- The email field's blinking caret on the onboarding screen (1.1 s infinite — opacity only, no scale)

On Compose: use `InfiniteTransition` / `animateFloat` with `tween(700, easing = FastOutSlowInEasing)` ping-ponging between 1f and 0.5f alpha, plus a matching scale transform.

No screen-transition animations are specified — use your navigation library's default slide/fade.

---

## Assets

All imagery in the reference is from Unsplash (placeholder). The production app will use owner-uploaded hero images + retailer-scraped product images.

Icons are inline SVG paths (see `RegistryDetailScreen`'s `StatusChip`, and the four bottom-nav icons in `BottomNav`). Re-implement as Compose `Icon` with `ImageVector` paths — the paths are in the source and are simple enough to retrace. Alternatively use Material Symbols: `home`, `storefront`, `list`, `person`, and the ✓/↗/⋯/×/← glyphs map to standard Material icons.

The **GiftMaison wordmark** is text: `Instrument Serif italic`, with a single accent-coloured period. No logo file needed.

---

## Files

Reference files in `reference/`:

- **`GiftMaison - gift registry mocks.html`** — the complete prototype. Scroll to the "Android" section for the 5 artboards covered here. Use Tweaks (top-right toggle) to swap occasion themes.
- **`theme.jsx`** — all colour tokens (every occasion), the type scale, and the sample data set used across mocks.
- **`android-frame.jsx`** — the device bezel component. For reference only; not needed in production.

Source lives inline in the HTML; the standalone `android-screens.jsx` in the project root is an earlier draft and **should not be used** — prefer the HTML.

---

## Out of scope for this handoff

Asked and intentionally deferred — flag to PM before implementing:

- Notifications inbox / push handling
- Settings / profile screen
- Dark mode
- Empty states (brand-new user, empty registry, no search results)
- Browse-stores WebView (item-picking UI embedded in retailers' pages)
- Email templates (transactional emails the app triggers)
- Offline behaviour and sync

---

## Questions for the design author

If anything here contradicts what you see in the prototype, the **prototype wins** — flag the discrepancy back and we'll update this doc. Particular areas where this README is opinionated and may need confirmation:

1. The "60s countdown re-render cadence" (design shows minutes, so polling in minutes is sufficient — but confirm)
2. The `isPrimary` card treatment on Home (design shows exactly one dark card; confirm the selection rule is "most recent" vs. pinning)
3. Whether "Stores" and "You" nav tabs have designs yet (currently only Home and Lists are built out)
