import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        cream: '#FAF6F1',
        terracotta: '#B85C4A',
        'terracotta-dark': '#9C4A3A',
        gold: '#C9A227',
        blush: '#F3E1DC',
        charcoal: '#2E2A27',
        'warm-gray': '#8A7F78',
        olive: '#6B8F71',
      },
      fontFamily: {
        serif: ['"Playfair Display"', 'Georgia', 'serif'],
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 18px 48px rgba(46, 42, 39, 0.08)',
        button: '0 10px 28px rgba(184, 92, 74, 0.28)',
      },
    },
  },
  plugins: [],
} satisfies Config;
