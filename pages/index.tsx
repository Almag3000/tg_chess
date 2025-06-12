import { useState } from 'react';
import { motion } from 'framer-motion';
import Game from '../components/Game';

export default function Home() {
  const [level, setLevel] = useState(1);
  const [isGameOver, setIsGameOver] = useState(false);

  const handleLevelComplete = () => {
    setLevel(prev => prev + 1);
  };

  const handleGameOver = () => {
    setIsGameOver(true);
  };

  const restartGame = () => {
    setLevel(1);
    setIsGameOver(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary to-secondary">
      <div className="container mx-auto px-4 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-8"
        >
          <h1 className="text-4xl font-bold text-white mb-2">Match-3</h1>
          <p className="text-lg text-white/80">Соберите три в ряд!</p>
        </motion.div>

        {isGameOver ? (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            className="text-center"
          >
            <h2 className="text-3xl font-bold text-white mb-4">Игра окончена!</h2>
            <p className="text-xl text-white mb-6">Вы достигли уровня {level}</p>
            <button
              onClick={restartGame}
              className="bg-white text-primary px-6 py-3 rounded-lg font-semibold hover:bg-opacity-90 transition-all"
            >
              Начать заново
            </button>
          </motion.div>
        ) : (
          <Game level={level} onLevelComplete={handleLevelComplete} />
        )}
      </div>
    </div>
  );
}
