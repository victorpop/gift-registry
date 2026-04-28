// theme.jsx — GiftMaison brand tokens + occasion palettes

const THEMES = {
  housewarming: {
    name: 'Housewarming',
    headline: 'Welcome home',
    subline: 'A place, now filled',
    paper: 'oklch(0.972 0.012 75)',      // warm cream
    paperDeep: 'oklch(0.94 0.018 72)',   // warmer card
    ink: 'oklch(0.22 0.015 50)',         // soft near-black
    inkSoft: 'oklch(0.42 0.02 55)',
    inkFaint: 'oklch(0.62 0.025 60)',
    line: 'oklch(0.88 0.015 70)',
    accent: 'oklch(0.58 0.15 38)',       // terracotta
    accentInk: 'oklch(0.98 0.01 75)',
    accentSoft: 'oklch(0.92 0.04 42)',
    second: 'oklch(0.48 0.07 145)',      // deep olive
    secondSoft: 'oklch(0.9 0.03 135)',
    ok: 'oklch(0.58 0.11 150)',
    warn: 'oklch(0.68 0.14 65)',
  },
  wedding: {
    name: 'Wedding',
    headline: 'Two, together',
    subline: 'Small tokens for a big day',
    paper: 'oklch(0.975 0.008 20)',
    paperDeep: 'oklch(0.95 0.013 15)',
    ink: 'oklch(0.22 0.015 340)',
    inkSoft: 'oklch(0.42 0.02 340)',
    inkFaint: 'oklch(0.62 0.025 340)',
    line: 'oklch(0.88 0.015 20)',
    accent: 'oklch(0.56 0.13 355)',      // rose
    accentInk: 'oklch(0.98 0.01 20)',
    accentSoft: 'oklch(0.92 0.04 355)',
    second: 'oklch(0.42 0.04 280)',
    secondSoft: 'oklch(0.9 0.02 280)',
    ok: 'oklch(0.58 0.11 150)',
    warn: 'oklch(0.68 0.14 65)',
  },
  baby: {
    name: 'Baby shower',
    headline: 'Tiny, loved',
    subline: 'Hand-picked by family',
    paper: 'oklch(0.975 0.01 225)',
    paperDeep: 'oklch(0.95 0.018 220)',
    ink: 'oklch(0.22 0.02 240)',
    inkSoft: 'oklch(0.42 0.025 235)',
    inkFaint: 'oklch(0.62 0.03 230)',
    line: 'oklch(0.88 0.02 225)',
    accent: 'oklch(0.6 0.1 230)',        // soft dusty blue
    accentInk: 'oklch(0.98 0.01 225)',
    accentSoft: 'oklch(0.92 0.04 225)',
    second: 'oklch(0.52 0.09 55)',       // warm mustard counter
    secondSoft: 'oklch(0.92 0.04 65)',
    ok: 'oklch(0.58 0.11 150)',
    warn: 'oklch(0.68 0.14 65)',
  },
  birthday: {
    name: 'Birthday',
    headline: 'Another lap',
    subline: 'Here\u2019s to the year ahead',
    paper: 'oklch(0.975 0.012 90)',
    paperDeep: 'oklch(0.95 0.02 85)',
    ink: 'oklch(0.22 0.015 60)',
    inkSoft: 'oklch(0.42 0.025 65)',
    inkFaint: 'oklch(0.62 0.03 70)',
    line: 'oklch(0.88 0.02 80)',
    accent: 'oklch(0.64 0.16 75)',       // marigold
    accentInk: 'oklch(0.2 0.02 70)',
    accentSoft: 'oklch(0.92 0.05 80)',
    second: 'oklch(0.42 0.12 350)',      // plum counter
    secondSoft: 'oklch(0.9 0.03 350)',
    ok: 'oklch(0.58 0.11 150)',
    warn: 'oklch(0.62 0.17 30)',
  },
};

const TYPE = {
  display: '"Instrument Serif", "Cormorant Garamond", Georgia, serif',
  body: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  mono: '"JetBrains Mono", ui-monospace, "SF Mono", Menlo, monospace',
};

// Sample photography (Unsplash — housewarming objects)
const PHOTOS = {
  bowl: 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600&q=70',
  lamp: 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&q=70',
  plant: 'https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=600&q=70',
  linen: 'https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=600&q=70',
  coffee: 'https://images.unsplash.com/photo-1516224498413-84ecf3a1e7fd?w=600&q=70',
  knives: 'https://images.unsplash.com/photo-1593618998160-e34014e67546?w=600&q=70',
  board: 'https://images.unsplash.com/photo-1541542684-4a9c1a9a3196?w=600&q=70',
  candle: 'https://images.unsplash.com/photo-1603006905003-be475563bc59?w=600&q=70',
  vase: 'https://images.unsplash.com/photo-1578500351865-d6c3706f46bc?w=600&q=70',
  throw: 'https://images.unsplash.com/photo-1540638349517-3abd5afc9847?w=600&q=70',
  espresso: 'https://images.unsplash.com/photo-1494314671902-399b18174975?w=600&q=70',
  mugs: 'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=600&q=70',
  couple: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&q=70',
  apt: 'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?w=800&q=70',
};

// Sample registry items
const SAMPLE_ITEMS = [
  { id: 'i1', title: 'Stoneware mixing bowl set', retailer: 'emag.ro', price: 189, currency: 'RON', image: PHOTOS.bowl, status: 'available' },
  { id: 'i2', title: 'Sculptural table lamp, ceramic base', retailer: 'dedeman.ro', price: 420, currency: 'RON', image: PHOTOS.lamp, status: 'reserved', reservedBy: 'Ioana M.', minutesLeft: 23 },
  { id: 'i3', title: 'Fiddle-leaf fig, medium', retailer: 'emag.ro', price: 145, currency: 'RON', image: PHOTOS.plant, status: 'purchased', purchasedBy: 'Andrei' },
  { id: 'i4', title: 'Linen duvet cover, dune', retailer: 'zara.home', price: 349, currency: 'RON', image: PHOTOS.linen, status: 'available' },
  { id: 'i5', title: 'Moka pot, 6-cup', retailer: 'emag.ro', price: 119, currency: 'RON', image: PHOTOS.coffee, status: 'available' },
  { id: 'i6', title: 'Santoku knife, hand-forged', retailer: 'emag.ro', price: 280, currency: 'RON', image: PHOTOS.knives, status: 'available' },
  { id: 'i7', title: 'Walnut cutting board', retailer: 'dedeman.ro', price: 165, currency: 'RON', image: PHOTOS.board, status: 'available' },
  { id: 'i8', title: 'Beeswax pillar candle set', retailer: 'emag.ro', price: 85, currency: 'RON', image: PHOTOS.candle, status: 'purchased', purchasedBy: 'Maria' },
  { id: 'i9', title: 'Hand-thrown flower vase', retailer: 'etsy.com', price: 240, currency: 'RON', image: PHOTOS.vase, status: 'available' },
  { id: 'i10', title: 'Wool throw blanket, ochre', retailer: 'zara.home', price: 399, currency: 'RON', image: PHOTOS.throw, status: 'available' },
  { id: 'i11', title: 'Espresso machine, semi-auto', retailer: 'emag.ro', price: 1849, currency: 'RON', image: PHOTOS.espresso, status: 'available' },
  { id: 'i12', title: 'Stoneware mug set of four', retailer: 'emag.ro', price: 160, currency: 'RON', image: PHOTOS.mugs, status: 'available' },
];

Object.assign(window, { THEMES, TYPE, PHOTOS, SAMPLE_ITEMS });
