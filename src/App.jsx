
import { useEffect, useState } from 'react'
import { supabase } from './lib/supabase'

export default function App() {
  const [searching, setSearching] = useState(false)

  async function handleFindMatch() {
    setSearching(true)

    const tg = window.Telegram.WebApp
    const telegramId = tg?.initDataUnsafe?.user?.id?.toString()

    if (!telegramId) {
      alert('Не удалось получить Telegram ID')
      return
    }

    await supabase.from('queue').delete().eq('telegram_id', telegramId)
    await supabase.from('queue').insert({ telegram_id: telegramId })

    const interval = setInterval(async () => {
      const { data: game } = await supabase
        .from('games')
        .select('*')
        .or(`white_id.eq.${telegramId},black_id.eq.${telegramId}`)
        .order('created_at', { ascending: false })
        .limit(1)

      if (game && game.length > 0) {
        clearInterval(interval)
        const gameId = game[0].id
        window.location.href = `/game/${gameId}`
      }
    }, 2000)
  }

  useEffect(() => {
    if (window.Telegram?.WebApp) {
      window.Telegram.WebApp.ready()
      window.Telegram.WebApp.expand()
    }
  }, [])

  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-white text-gray-900 p-6">
      <h1 className="text-2xl font-bold">♟️ Онлайн шахматы</h1>
      <button onClick={handleFindMatch} className="bg-blue-500 text-white px-4 py-2 rounded-xl w-full max-w-xs">
        🔍 Поиск соперника
      </button>
      <button className="bg-green-500 text-white px-4 py-2 rounded-xl w-full max-w-xs">
        👥 Друзья
      </button>
    </div>
  )
}
