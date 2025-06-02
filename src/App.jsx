import { useEffect } from 'react'

export default function App() {
  useEffect(() => {
    if (window.Telegram?.WebApp) {
      window.Telegram.WebApp.ready()
      window.Telegram.WebApp.expand()
    }
  }, [])

  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-white text-gray-900 p-6">
      <h1 className="text-2xl font-bold">♟️ Онлайн шахматы</h1>
      <button className="bg-blue-500 text-white px-4 py-2 rounded-xl w-full max-w-xs">🔍 Поиск соперника</button>
      <button className="bg-green-500 text-white px-4 py-2 rounded-xl w-full max-w-xs">👥 Друзья</button>
    </div>
  )
}