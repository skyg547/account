/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/lib/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        kbank: {
          blue: '#4262ff',
          'blue-hover': '#3452e6',
          'blue-active': '#2b44d4',
          'blue-light': '#eef2ff',
          'blue-border': '#dbe3ff',
          bg: '#f7f8fb',
          dark: '#17191e',
          body: '#2a2e36',
          subtext: '#545b69',
          muted: '#8c94a4',
          border: '#eaedf4',
          card: '#ffffff',
        },
        primary: {
          DEFAULT: '#4262ff',
          hover: '#3452e6',
          light: '#eef2ff',
          dark: '#2b44d4',
        },
        background: "#f7f8fb",
        foreground: "#17191e",
      },
      boxShadow: {
        'kbank': '0 4px 20px -2px rgba(66, 98, 255, 0.05), 0 2px 8px -1px rgba(0, 0, 0, 0.03)',
        'kbank-hover': '0 8px 30px -4px rgba(66, 98, 255, 0.1), 0 4px 12px -2px rgba(0, 0, 0, 0.04)',
      },
    },
  },
  plugins: [],
};
