/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#2C3E50",
        secondary: "#34495E",
        accent: "#3498DB",
      },
    },
  },
  plugins: [],
} 