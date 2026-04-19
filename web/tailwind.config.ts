import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary:     { DEFAULT: '#6750A4', on: '#FFFFFF' },
        surface:     { DEFAULT: '#FFFBFE', variant: '#E7E0EC', on: '#1C1B1F', onVariant: '#49454F' },
        destructive: { DEFAULT: '#B3261E', on: '#FFFFFF' },
        outline:     { DEFAULT: '#CAC4D0' },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
} satisfies Config
