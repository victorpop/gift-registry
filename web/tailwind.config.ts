import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Phase 13 — Housewarming wired through CSS custom props (D-07).
        // sRGB hex shown in comments; oklch source kept for v1.2 migration.
        gm: {
          paper:      'var(--gm-paper)',      // #F7F2E9 — oklch(0.972 0.012 75)
          paperDeep:  'var(--gm-paperDeep)',  // #EDE5D5 — oklch(0.94 0.018 72)
          ink:        'var(--gm-ink)',        // #2A2420 — oklch(0.22 0.015 50)
          inkSoft:    'var(--gm-inkSoft)',    // #6A5E52 — oklch(0.42 0.02 55)
          inkFaint:   'var(--gm-inkFaint)',   // #9C8E7F — oklch(0.62 0.025 60)
          line:       'var(--gm-line)',       // #DDD4C4 — oklch(0.88 0.015 70)
          accent:     'var(--gm-accent)',     // #C8623A — oklch(0.58 0.15 38)
          accentInk:  'var(--gm-accentInk)',  // #FCF8EF — oklch(0.98 0.01 75)
          accentSoft: 'var(--gm-accentSoft)', // #F3DED0 — oklch(0.92 0.04 42)
          second:     'var(--gm-second)',     // #4F7050 — oklch(0.48 0.07 145)
          secondSoft: 'var(--gm-secondSoft)', // #D7E2CE — oklch(0.9 0.03 135)
          ok:         'var(--gm-ok)',         // #4F9668 — oklch(0.58 0.11 150)
          warn:       'var(--gm-warn)',       // #D29447 — oklch(0.68 0.14 65)
        },
        // Phase 5 legacy tokens kept for compat during the cutover. Removal audit in Phase 14.
        primary:     { DEFAULT: '#6750A4', on: '#FFFFFF' },
        surface:     { DEFAULT: '#FFFBFE', variant: '#E7E0EC', on: '#1C1B1F', onVariant: '#49454F' },
        destructive: { DEFAULT: '#B3261E', on: '#FFFFFF' },
        outline:     { DEFAULT: '#CAC4D0' },
      },
      fontFamily: {
        display: ['"Instrument Serif"', '"Cormorant Garamond"', 'Georgia', 'serif'],
        body:    ['Inter', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'sans-serif'],
        mono:    ['"JetBrains Mono"', 'ui-monospace', '"SF Mono"', 'Menlo', 'monospace'],
        sans:    ['Inter', 'system-ui', 'sans-serif'], // legacy alias
      },
      spacing: {
        '7': '28px', // tablet page padding (handoff-mandated — must be explicit override)
      },
      borderRadius: {
        'gm-card':       '14px',
        'gm-card-large': '18px',
        'gm-modal':      '20px',
      },
      boxShadow: {
        'gm-modal': '0 40px 80px rgba(0, 0, 0, 0.18)',
      },
      animation: {
        'gm-pulse': 'gm-pulse 1.4s ease-in-out infinite alternate',
      },
      keyframes: {
        'gm-pulse': {
          '0%, 100%': { opacity: '1', transform: 'scale(1)' },
          '50%':      { opacity: '0.5', transform: 'scale(0.85)' },
        },
      },
    },
  },
  plugins: [],
} satisfies Config
