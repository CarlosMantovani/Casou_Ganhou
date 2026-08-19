import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ivory: '#F7F1E6',
        'ivory-deep': '#F0E8D8',
        ink: '#2B2419',
        'ink-soft': '#5B5140',
        green: '#24402E',
        'green-deep': '#152A1D',
        wine: '#7A2E33',
        gold: '#B8935A',
        'gold-soft': '#DCC79A',
        line: '#D9CBAA',
        cream: '#F7F1E6',
        terracotta: '#24402E',
        'terracotta-dark': '#152A1D',
        blush: '#F0E8D8',
        charcoal: '#2B2419',
        'warm-gray': '#5B5140',
        olive: '#24402E',
      },
      fontFamily: {
        serif: ['"Playfair Display"', 'Georgia', 'serif'],
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 18px 48px rgba(43, 36, 25, 0.08)',
        button: '0 10px 28px rgba(36, 64, 46, 0.26)',
      },
    },
  },
  plugins: [],
} satisfies Config;
