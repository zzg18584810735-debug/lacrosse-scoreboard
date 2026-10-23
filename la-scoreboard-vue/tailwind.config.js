/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx,vue}",
  ],
  theme: {
    extend: {
      colors: {
        'home-primary': '#ef4444', // Red-500
        'home-secondary': '#b91c1c', // Red-700
        'away-primary': '#3b82f6', // Blue-500
        'away-secondary': '#1d4ed8', // Blue-700
      }
    },
  },
  plugins: [],
}
