import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#fff8eb',
          100: '#fce9c3',
          500: '#c88a2d',
          700: '#8a5a17',
          900: '#3f2a12',
        },
      },
    },
  },
  plugins: [],
} satisfies Config;
