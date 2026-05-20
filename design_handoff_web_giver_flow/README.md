# Handoff — GiftMaison (Web giver flow)

> **GiftMaison** is a gift-registry app. This handoff covers the **giver-facing web flow**: the screens a guest sees when someone shares a registry link with them. The native owner flow (Android) is a separate package.
>
> **Audience**: guests with a public registry link. Most won't have an account, won't want one, and **most will arrive on mobile** — directly from a WhatsApp / iMessage / email link. Treat this surface as a **mobile-first web fallback**, not a desktop product.

---

## About the design files

The mocks in `reference/` are **design references, not production code**. They are HTML/React prototypes built for visual communication — the layout, spacing, type, colour, copy, and interactions are the contract; the JSX is not meant to be copy-pasted.

The mocks were **drawn at desktop widths (1280–1440 px)** to make the layout legible during review. **The implementation must be mobile-first.** See [Responsive behaviour](#responsive-behaviour) for the breakpoints, layout adaptations, and mobile-only patterns the desktop mocks don't show.

## Fidelity

**High-fidelity at the component level** — colours (oklch), type scale, spacing, radii, copy are all final unless explicitly flagged. **Layout fidelity is desktop-only;** at mobile widths you are expected to reflow per the rules in this doc, not to literally squeeze the desktop layout into 375 px.

`oklch()` colour tokens convert cleanly to sRGB hex — see the [Design tokens](#design-tokens) table.

---

## Why a web fallback exists

Givers usually receive a registry link in a chat thread and tap it on their phone. Forcing them to install a native app, sign up, or download anything destroys the conversion rate of the entire gift loop — and we don't earn the right to ask: most of them will reserve one gift in their lifetime per registry-owner-friend.

Therefore:

1. **The whole reserve flow must work without an account.** Name + (optional email for the timer reminder) is enough.
2. **No app install prompts, no smart banners, no interstitials.** The web flow is the product for these users, not a funnel into native.
3. **Account creation is post-hoc and dismissible.** We only offer it after a successful reservation, when the user has a sunk-cost reason to keep their state (the timer). See screen 04.
4. **Touch-first density.** Tap targets ≥ 44 px, no hover-only affordances, no tooltips for required content.
5. **Server-side rendering is preferred.** First paint of the registry page must be fast on a 4G phone halfway through a workday. Hydrate the interactive bits (filter, reserve modal, timer) after.

---

## Contents of this handoff

```
design_handoff_web_giver_flow/
├── README.md                                   (this file)
├── screens/                                    (PNG previews of the desktop refs)
│   ├── 01-registry-desktop.png
│   ├── 02-reserve-timer.png
│   ├── 03-auth.png
│   ├── 04-guest-convert.png
│   └── 05-expired.png
└── reference/
    ├── GiftMaison - gift registry mocks.html   (the full prototype — open in a browser)
    ├── theme.jsx                               (colour tokens + occasion themes + sample data)
    ├── web-screens.jsx                         (the 5 web screens as React components)
    └── browser-window.jsx                      (browser-frame wrapper used in the canvas)
```

Open the prototype HTML and scroll to the top section ("Web — giver experience"). Toggle **Tweaks** in the toolbar to switch the occasion theme (housewarming / wedding / baby / birthday) and confirm tokens cascade.

---

## Screens

There are **5 distinct routes** in the giver flow. Numbering matches the labels in the prototype.

| # | Screen | Route | Auth required? |
|---|---|---|---|
| 01 | Registry detail (the public list) | `/r/{slug}` | No — public link |
| 02 | Reserve + 30-min timer | `/r/{slug}/reserve/{itemId}` | No — guest OK |
| 03 | Auth — sign in / sign up / guest | `/sign-in?next={…}` | — |
| 04 | Guest → account conversion modal | overlay on `/r/{slug}` after reserving as guest | — |
| 05 | Reservation expired + re-reserve | `/r/{slug}/re-reserve?token={…}` | Token-authenticated link |

### Canonical viewport

- **Mobile-first design target: 375 × 812 (iPhone-class).** Lay everything out for this width.
- Tablet breakpoint at **640 px**: relax horizontal padding, allow 2-column item grid.
- Desktop breakpoint at **1024 px**: switch to the 3-column item grid and the side-by-side layouts shown in the desktop refs.
- The desktop ref widths in `screens/` are 1280 px (auth/guest/expired) and 1440 px (registry/reserve). Treat as upper bounds — the design does not need to scale beyond 1440.

---

### 01 · Registry detail (public list)

**Purpose:** the page a giver lands on after tapping the share link.

**Top to bottom (mobile):**
1. Sticky top nav — wordmark left, EN/RO switch + "Sign in" ghost link right (collapse to a hamburger only if you must — prefer keeping all three visible)
2. **Hero** — occasion pill + "Public link" pill + date · headline (display serif, italic accent on the emphasised phrase) · subline (body, max ~3 lines, `text-wrap: pretty`)
3. **Progress strip** — "{n} of {total} chosen" + thin progress bar + share button. **On mobile, this drops below the hero**, full width. On desktop it sits right of the hero.
4. **Filter chips** — All / Available / Reserved / Purchased. Horizontally scrollable on mobile (no overflow menu). Active chip = white pill on `paperDeep` track.
5. **Item grid** — 1 col on mobile, 2 col at 640 px, 3 col at 1024 px, gap 16/20.
6. Footer — minimal: "© giftmaison 2026 · terms · privacy · en/ro".

**Item card:**
- 14 radius, `line` border, `paper` bg
- 16:10 image (4:3 on mobile to use vertical space well)
- Status pill top-left of image: `Available` (neutral) / `Reserved` (accent + pulse dot) / `Purchased` (`ok` green)
- Body: title (15 px body 500, −0.2 letter-spacing), price + retailer row, then either:
  - "Reserve this gift →" primary pill button (Available)
  - Reserved-by banner with countdown (`accentSoft` bg, accent dot pulsing, "{N} MIN LEFT — auto-releases…" mono caps)
  - Nothing (Purchased — opacity 0.55, image grayscale, "✓ Given by {firstName}" pill centred over image)

**Purchased items stay in the list** (greyed, not hidden) so other givers don't duplicate the gift. Trust pattern.

---

### 02 · Reserve + 30-minute timer

**Purpose:** Step 2 of the reservation. The giver has tapped "Reserve this gift" on a card. We hold the item for 30 minutes while they buy it at the retailer.

**Top to bottom:**
1. **Sticky reservation banner** (full-width, `ink` bg, `paper` text)
   - Pulsing accent dot · "You reserved {itemName}" + "{MM:SS} remaining · finish your purchase at {retailer}"
   - Right side: "Release reservation" (quiet) + "Continue to {retailer} →" (accent CTA). On mobile this stacks under the message line.
2. **Page body (2 col on desktop, stacked on mobile):**
   - **Left/main:**
     - Mono caption "Your reservation · Step 2 of 2"
     - Display-serif headline: "Nice one. *Now finish the purchase* at the retailer." (italic accent on the second phrase)
     - **Reserved item card** — 160 px square thumbnail, title, price, retailer, then a nested time-to-purchase progress bar with countdown (60s render cadence; the `MM:SS` digits also update on the sticky banner)
     - **Confirm-back card** — `accentSoft` bg, "Bought it? Mark it as purchased." headline + "I completed the purchase ✓" accent button
   - **Right/sidebar:** "How the timer works" 4-step ordered list. On mobile this sits below the main column and is collapsible by default.

**Timer details:**
- 30 minutes from the moment the reserve action succeeds server-side.
- Countdown digits update every second; auto-release / state transition is server-driven (do not trust client clocks).
- 5 minutes before expiry, send an email reminder if we have an email.
- On expiry without confirmation: state reverts to `available`, send the giver a one-tap re-reserve email link → screen 05.

---

### 03 · Auth — sign in / sign up / guest

**Purpose:** an optional waypoint. Most givers will skip it. **The "Continue as guest" path must always succeed** — this screen is never blocking.

**Layout (split on desktop, stacked on mobile):**
1. **Left/main column (520 px on desktop, full bleed on mobile):**
   - Wordmark
   - Caption "You're invited to a registry"
   - Headline: "*Pick up* where you left off"
   - Subline explaining the trade-off (account = state across devices; guest = name only)
   - **Tab switcher** — Sign in / Create account (underline indicator)
   - Email + password fields, primary "Sign in" button
   - Divider "or"
   - "Continue with Google" ghost button (G glyph)
   - **Guest skip card** — dashed `line` border, "Just here to reserve a gift? Continue as guest — we'll only ask for your name." with a "Skip →" ghost button. **This is the most important affordance on the screen** — make sure it is reachable in 1 tap on mobile.
   - Footer: "© giftmaison 2026 · terms · privacy · en / ro"
2. **Right/editorial column (desktop only, hide below 1024 px):** atmospheric photo with a quote overlay. Pure marketing — drop on mobile.

**Mobile-specific:**
- Hide the editorial photo entirely.
- Pin the "Continue as guest →" affordance to the bottom of the viewport (sticky), so it's never below the fold.
- Default tab is **Sign in** for returning users (detect via cookie / localStorage), **Create account** otherwise.

---

### 04 · Guest → account conversion (modal)

**Purpose:** opportunistic upsell **only after** the giver has successfully reserved a gift as a guest. It must be **dismissible without losing the reservation** — that's the whole point. Don't break the trust we just earned.

**Layout (modal centered over the dimmed registry):**
- Backdrop: dimmed + `blur(2px)` registry behind. Modal scrim is implicit (the blur is the scrim).
- Card: 520 px, 20 radius, `paper` bg, drop shadow.
- **Top section** (gradient `accentSoft` → `paper`):
  - Pills: "✓ Reserved" + "{itemName} · {MM:SS} left"
  - Display-serif headline: "Save your spot, *{firstName}*?" (italic + accent on the name)
  - Subline: explains the reservation will stay tied to them across devices + nudge before expiry.
- **Body:**
  - 3 benefit rows: timer-tracking, email nudge, all-registries-in-one-place
  - Single field: "Set a password to finish" (we already have email from the guest flow)
  - **Two buttons of equal weight:** "Not now, thanks" (quiet) + "Create my account →" (primary). The decline button must be just as obvious as the CTA.
  - Caption: "Using {email} from your guest reservation"

**Behavioural rules:**
- Dismissing the modal does **not** dismiss the reservation. Make this crystal clear via the timer pill staying visible after dismissal.
- Do not show this modal more than once per guest reservation.
- If the guest didn't give us an email when reserving, replace the password field with email + password — but still allow dismissal.

---

### 05 · Reservation expired + re-reserve

**Purpose:** the giver tapped a one-tap email link after their reservation expired. The token in the URL authenticates them and lets them re-reserve in one click.

**Layout (2 col on desktop, stacked on mobile):**
1. **Left/main:**
   - Pill: "⌛ Reservation expired" (`secondSoft` bg, `second` fg)
   - Headline: "The timer ran out, *but the gift's still free.*" (italic last clause)
   - Subline: explains what happened, when it happened, and that the item hasn't been claimed by anyone else
   - CTAs: **"Re-reserve now →"** (accent, primary) + "Back to the full registry" (ghost)
   - Info card: "Why do we expire reservations?" — pre-empts the "this is annoying" reaction
2. **Right:** the item itself in a small product card (image grayscaled to 30%, "◯ Available again" pill)

**Edge case:** if someone else already reserved/purchased the item between expiry and re-click, swap the right card for an apology + suggestions of similar still-available items from the same registry.

---

## Responsive behaviour

The desktop refs assume ≥ 1024 px. Below that, apply these rules:

| Element | ≥ 1024 px (desktop ref) | 640–1024 px (tablet) | < 640 px (mobile) |
|---|---|---|---|
| Page padding | 40 px | 28 px | 16 px |
| Item grid | 3 col | 2 col | 1 col |
| Hero layout (screen 01) | Hero left + progress strip right | Hero on top, progress strip below in 2 col | Stacked, full width |
| Reserve page (02) main + sidebar | Side by side | Side by side, sidebar narrower | Stacked, sidebar collapsed |
| Auth split (03) | Form left + photo right | Form full width, photo hidden | Form full width, guest skip pinned bottom |
| Sticky reserve banner (02) | Single row | Single row | 2 rows: message above, buttons below, full-width buttons |
| Convert modal (04) | Centred 520 px card | Centred 520 px card | Bottom sheet — slides up from bottom, 22 radius top corners only |
| Body type (display-serif headlines) | 56 / 44 / 34 px | 48 / 38 / 30 px | 36 / 28 / 24 px |
| Tap targets | ≥ 36 px | ≥ 40 px | ≥ 44 px |

### Mobile-only patterns

- **Convert modal becomes a bottom sheet** (drag-handle at top, slides up). Match the bottom-sheet conventions documented in the Android handoff (radius 22, drag-handle 36×4 in `line`).
- **Reserve banner buttons go full-width** in 2 stacked rows under the message text.
- **Filter chips on screen 01 horizontally scroll** with momentum; no fade or overflow indicator needed.
- **Sticky CTAs**: on screens 02 and 05, the primary CTA pins to the bottom of the viewport on mobile (8–12 px from the safe-area inset, with a `paper` blurred backdrop and a 1 px `line` top-border).
- **No hover states** carry semantic meaning. Anything reachable only on hover must also be reachable on tap (e.g. the "show password" suffix is a tap toggle, not a hover reveal).

---

## Interactions & behaviour

### Reservation lifecycle

Items have 3 lifecycle states: `available → reserved → purchased`, with auto-release back to `available` if no purchase confirmation in 30 minutes.

- **available** → any visitor can reserve. Card pill = `Available` (neutral).
- **reserved** → one giver holds it for 30 min. Card pill = `◉ Reserved` (accent), card body shows banner with reserver name + countdown ("{N} MIN LEFT") for everyone _except_ the reserver themselves (who sees the sticky banner from screen 02).
  - Countdown displayed at minute granularity in card banners; second granularity in the sticky banner / convert modal.
  - On expiry without confirmation: state → `available`, email re-reserve link sent if we have an email.
- **purchased** → giver confirmed. Pill = `✓ Purchased`, card opacity 0.55, image grayscale, "✓ Given by {firstName}" overlay. Stays in the list — does not get hidden.

The owner sees more (reserver / giver names); see the Android handoff. **In this web flow, names are first-name + last initial only** ("Andrei P.") to balance privacy with the trust signal.

### Guest reservation flow (the critical path)

1. User taps "Reserve this gift →" on a card (screen 01)
2. **No auth wall.** A small inline form asks: name (required), email (optional — "we'll email you a 5-minute warning before the timer ends"). One sentence of explanation, one button. Not a full screen.
3. On submit → server creates reservation + sets a HttpOnly cookie tying the browser to it → redirect to screen 02.
4. After 30 seconds on screen 02 (or after the user dismisses the timer banner, whichever first): show screen 04 modal. Once.

### URL handling (re-reserve token)

- Re-reserve email link: `/r/{slug}/re-reserve?token={one-time-token}`
- Token is single-use, expires after 24 h, scoped to the original reserver and the original item.
- On click: verify server-side, if valid → re-create the reservation + redirect to screen 02 with a fresh 30-min timer. If invalid (used, expired, item now reserved by someone else) → screen 05's edge-case variant.

### Timer rendering

- Sticky banner countdown: `MM:SS`, updates every 1 s.
- Card banners: "{N} MIN LEFT", updates every 60 s.
- Server is authoritative — client computes `max(0, expiresAt - now)`.
- When the timer hits 0 client-side, optimistically transition the UI to the "expired" state and refetch the registry to confirm.

---

## State management

The web flow has very little durable client state. Most lives on the server, with HttpOnly cookies for guest sessions.

```ts
type Visitor =
  | { kind: 'guest'; reservationCookie: string }   // HttpOnly, server-set
  | { kind: 'auth';  userId: string };

type Registry = {
  slug: string;
  ownerName: string;
  occasion: 'housewarming' | 'wedding' | 'baby' | 'birthday' | 'christmas' | 'custom';
  date: string;        // ISO
  visibility: 'public' | 'private';
  hero: { headline: string; subline: string; image: string };
  items: RegistryItem[];
  stats: { total: number; reserved: number; purchased: number };
};

type RegistryItem = {
  id: string;
  title: string;
  image: string;
  price: { amount: number; currency: 'RON' | 'EUR' | 'USD' };
  retailer: string;        // display name
  productUrl: string;      // affiliate-tagged server-side
  status: 'available' | 'reserved' | 'purchased';
  reservation?: { byFirstNameInitial: string; expiresAt: string };
  gift?: { byFirstName: string; givenAt: string };
};
```

- **Guest reservations are tied to a cookie**, not a stored account. The cookie's the source of truth for "is this me?" until they convert.
- The cookie should also be emitted as a `reservation_token` query param on the email-reminder link so a guest can resume on a different device.
- Registry data on the public route can be **statically rendered** (with revalidation on item-status change) — most visits are read-only. Item status is the live bit; revalidate on mutation, ISR / on-demand revalidate as appropriate.

---

## Design tokens

All tokens live in `reference/theme.jsx` as `oklch()`. Convert to sRGB hex for production CSS or use `oklch()` directly (modern browsers; CSS Houdini fallback for legacy). Values below are the **Housewarming** theme (default for the demo registry); generate equivalents for Wedding / Baby / Birthday from `theme.jsx`.

### Colour tokens (Housewarming)

| Token | oklch (source) | sRGB hex | Usage |
|---|---|---|---|
| `paper` | `oklch(0.972 0.012 75)` | `#F7F2E9` | Page bg, card bg |
| `paperDeep` | `oklch(0.94 0.018 72)` | `#EDE5D5` | Subtle surfaces, segmented track |
| `ink` | `oklch(0.22 0.015 50)` | `#2A2420` | Primary text, primary buttons, sticky banner |
| `inkSoft` | `oklch(0.42 0.02 55)` | `#6A5E52` | Body text |
| `inkFaint` | `oklch(0.62 0.025 60)` | `#9C8E7F` | Tertiary text, placeholders |
| `line` | `oklch(0.88 0.015 70)` | `#DDD4C4` | Borders, dividers |
| `accent` | `oklch(0.58 0.15 38)` | `#C8623A` | Terracotta — brand, Reserved, primary CTAs |
| `accentInk` | `oklch(0.98 0.01 75)` | `#FCF8EF` | Text on accent |
| `accentSoft` | `oklch(0.92 0.04 42)` | `#F3DED0` | Accent backgrounds, modal-top gradient |
| `second` | `oklch(0.48 0.07 145)` | `#4F7050` | Olive — "expired" pill text, avatars |
| `secondSoft` | `oklch(0.9 0.03 135)` | `#D7E2CE` | Olive backgrounds |
| `ok` | `oklch(0.58 0.11 150)` | `#4F9668` | Success — Purchased pill |
| `warn` | `oklch(0.68 0.14 65)` | `#D29447` | (unused on these screens) |

### Type scale

| Role | Font | Weight | Size (desktop / mobile) | Letter-spacing | Line-height |
|---|---|---|---|---|---|
| Display XL (page hero) | Instrument Serif | 400 | 56 / 36 | −1.4 | 1.0 |
| Display L (section / modal) | Instrument Serif | 400 | 44 / 28 | −1.0 | 1.05 |
| Display M | Instrument Serif | 400 | 34 / 24 | −0.8 | 1.05 |
| Display S (card title featured) | Instrument Serif | 400 | 22 / 20 | −0.4 | 1.1 |
| Body L | Inter | 400–500 | 16 / 15 | −0.2 | 1.55 |
| Body M (default) | Inter | 400–500 | 14 / 14 | −0.1 | 1.5 |
| Body S | Inter | 400–500 | 13 / 12.5 | 0 | 1.4 |
| Mono caps (label / meta) | JetBrains Mono | 500 | 10–12 | 1.2–1.5 | 1.3 |

**Italic display serif** is reserved for emphasised phrases inside a headline (e.g. *at last*, *Now finish the purchase*) and for the wordmark's terminal period accent. Always paired with `accent`.

Pull Instrument Serif, Inter, and JetBrains Mono from Google Fonts. All three available; preconnect both `fonts.googleapis.com` and `fonts.gstatic.com`.

### Spacing / radii / shadows

- **Page padding:** 40 desktop / 28 tablet / 16 mobile (see responsive table)
- **Radii:** 8 (thumbnails), 10 (small surfaces), 12 (inputs, info pills), 14 (cards), 18 (item-detail cards), 20 (modal), 999 (pills, buttons)
- **Borders:** `1px solid {line}` everywhere; focused inputs `1.5px {accent}`
- **Shadows:** kept minimal — modal uses `0 40px 80px rgba(0,0,0,0.18)`. Sticky banner has no shadow (flat `ink` block). Cards rely on borders not shadow.

### Animations

```css
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.5; transform: scale(0.85); }
}
```

- Pulsing dot on Reserved card pill, sticky banner, and convert-modal pill.
- Width-transition on the timer progress bar in screen 02 (CSS `transition: width 1s linear`).
- Modal entrance: slide-up 240 ms `cubic-bezier(0.2, 0.7, 0.1, 1)` + fade backdrop. On mobile (bottom-sheet variant), same easing, slide from `translateY(100%)` to `0`.

No other named animations. Keep transitions subtle — this is a content-first experience, not a kinetic one.

---

## Accessibility

- **Touch targets ≥ 44 px** on mobile, 36 px desktop.
- **Colour contrast**: `ink` on `paper` is 13:1; `accent` on `paper` is 4.6:1 (passes AA for UI / 18px+ text but **not** AA for body text). Do not put body text in `accent` on `paper`. Do put body text in `inkSoft` or `ink`.
- **Keyboard**: every CTA is a real `<button>` or `<a>`; no `div onClick`. The sticky banner's "Release reservation" is a `<button>`; "Continue to {retailer}" is an `<a target="_blank" rel="noopener">`.
- **Focus rings**: 2 px `accent` outline with 2 px offset, on `:focus-visible` only.
- **Screen reader**: the timer should announce the remaining minutes (not seconds) once per minute via an `aria-live="polite"` region — don't barrage AT users with second-by-second updates.
- **Reduced motion**: disable the pulse keyframe and the modal slide under `prefers-reduced-motion: reduce` — fade-only.
- **Language**: `<html lang>` reflects the EN/RO toggle; persist via cookie.

---

## SEO / OG / link unfurling

The registry detail page (`/r/{slug}`) is **the link people share in chat**. Its OG card is the single most important marketing surface for the entire product — it determines whether a tap happens.

- **`og:title`**: "{ownerName}'s {occasion}" e.g. "Ana & Mihai's housewarming"
- **`og:description`**: short subline pulled from the registry's `subline` field, truncated to 140 chars
- **`og:image`**: 1200 × 630 generated server-side. Wordmark top-left, occasion glyph, headline (Instrument Serif italic), date, "{n} of {total} chosen" progress text. Use the same colour tokens as the page. Cache aggressively — invalidate when the registry's name / hero changes.
- **`twitter:card`**: `summary_large_image`
- **No JS required to render** the OG card content — `<meta>` tags must be in the initial HTML for crawlers.

Private-link registries should still serve a generic OG card ("A private gift registry") rather than the real headline, in case the link leaks beyond intended recipients.

---

## Out of scope for this handoff

Asked and intentionally deferred — flag to PM before implementing:

- **Search / sort within the registry** (current design assumes < 50 items per registry; if that grows, add filters)
- **Multi-currency display per visitor locale** (server displays in registry currency only for v1)
- **Comments / messages from giver to owner** (post-purchase note exists in the Android owner flow but not the giver web flow yet)
- **Group gifting / chip-in** (one giver per item only)
- **Wishlist import** from the registry to a giver's own list
- **Email templates** (transactional emails the system sends — reminders, expiry, re-reserve link)
- **Owner reply-to-thanks** flow
- **PWA / installable variant**
- **Print view of the registry**
- **Empty states**: "this registry has no items yet", "all items are reserved", "all items are given"

---

## Files

- **`reference/GiftMaison - gift registry mocks.html`** — the complete prototype. Top section is the web flow (5 desktop refs in browser frames). Use the Tweaks panel to swap occasion themes.
- **`reference/web-screens.jsx`** — source of the 5 web screens as React components. Not production code; reference for layout, copy, and exact spacing only.
- **`reference/theme.jsx`** — colour tokens (every occasion), type scale, sample data.
- **`reference/browser-window.jsx`** — the chrome wrapper used in the prototype. For visual presentation only; not part of the design.

---

## Questions for the design author

If anything here contradicts the prototype, the **prototype wins** — flag the discrepancy back. Areas where this README is opinionated and may need confirmation:

1. **The bottom-sheet conversion for the convert modal on mobile.** The desktop ref shows a centred card; the README mandates a bottom sheet on mobile. Confirm this is desired (it's the mobile-native pattern).
2. **First-name + last initial display rule** ("Andrei P.") for reserver / giver names on public surfaces. Confirm — Android handoff used "Ioana M." style.
3. **The 30-second delay before showing the convert modal** on screen 02. May need product input.
4. **"Continue as guest →" sticky pinning** on mobile — confirm this overrides the editorial photo's absence rather than just floating below the form.
5. **Whether "Sign in" and "Create account" merge into one screen or split.** Current design uses tabs on one screen; alternative is two routes with a swap link.
