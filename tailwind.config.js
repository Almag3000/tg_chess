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
        board: {
          light: "#edf2f7",
          dark: "#4a5568"
        }
      },
      animation: {
        'thinking': 'thinking 1s ease-in-out infinite',
      },
      keyframes: {
        thinking: {
          '0%, 100%': { transform: 'scale(1)' },
          '50%': { transform: 'scale(1.1)' },
        }
      }
    },
  },
  plugins: [],
} 