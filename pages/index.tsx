import { useState, useEffect, useRef } from 'react';
import { Chessboard } from 'chessboardjsx';
import { Chess } from 'chess.js';
import { motion } from 'framer-motion';
import { RefreshIcon } from '@heroicons/react/outline';
import ChessEngine from '../components/ChessEngine';

export default function Home() {
  const [game, setGame] = useState(new Chess());
  const [difficulty, setDifficulty] = useState(10);
  const [isThinking, setIsThinking] = useState(false);
  const [status, setStatus] = useState('Инициализация...');
  const [isEngineReady, setIsEngineReady] = useState(false);
  const engineRef = useRef<ChessEngine | null>(null);

  useEffect(() => {
    const initEngine = async () => {
      engineRef.current = new ChessEngine();
      await engineRef.current.init();
      engineRef.current.setDifficulty(difficulty);
      setIsEngineReady(true);
      setStatus('Ваш ход');
    };

    initEngine();

    return () => {
      if (engineRef.current) {
        engineRef.current.quit();
      }
    };
  }, []);

  useEffect(() => {
    if (engineRef.current && isEngineReady) {
      engineRef.current.setDifficulty(difficulty);
    }
  }, [difficulty]);

  const makeMove = async (move: { from: string; to: string; promotion?: string }) => {
    const gameCopy = new Chess(game.fen());
    try {
      gameCopy.move(move);
      setGame(gameCopy);
      setStatus('Ход компьютера...');
      setIsThinking(true);

      if (engineRef.current && isEngineReady) {
        const bestMove = await engineRef.current.getBestMove(gameCopy.fen());
        const newGame = new Chess(gameCopy.fen());
        newGame.move(bestMove);
        setGame(newGame);
        setStatus('Ваш ход');
        setIsThinking(false);
      }
    } catch (error) {
      console.error('Недопустимый ход:', error);
      setStatus('Недопустимый ход');
      setIsThinking(false);
    }
  };

  const resetGame = () => {
    setGame(new Chess());
    setStatus('Ваш ход');
  };

  const onDrop = ({ sourceSquare, targetSquare }: { sourceSquare: string; targetSquare: string }) => {
    const move = {
      from: sourceSquare,
      to: targetSquare,
      promotion: 'q',
    };
    makeMove(move);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary to-secondary text-white">
      <div className="container mx-auto px-4 py-8">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-8"
        >
          <h1 className="text-4xl font-bold mb-2">Шахматы</h1>
          <p className="text-lg opacity-80">Играйте против компьютера</p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="md:col-span-2">
            <div className="card">
              <Chessboard
                position={game.fen()}
                onPieceDrop={onDrop}
                boardWidth={600}
                arePiecesDraggable={!isThinking}
                customBoardStyle={{
                  borderRadius: '4px',
                  boxShadow: '0 2px 10px rgba(0, 0, 0, 0.5)'
                }}
                customDarkSquareStyle={{ backgroundColor: '#4a5568' }}
                customLightSquareStyle={{ backgroundColor: '#edf2f7' }}
              />
            </div>
          </div>

          <div className="space-y-6">
            <div className="card">
              <h2 className="text-xl font-semibold mb-4">Настройки</h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm mb-2">Сложность</label>
                  <input
                    type="range"
                    min="1"
                    max="20"
                    value={difficulty}
                    onChange={(e) => setDifficulty(parseInt(e.target.value))}
                    className="w-full"
                    disabled={!isEngineReady}
                  />
                  <div className="text-center mt-2">
                    {difficulty === 1 && 'Начинающий'}
                    {difficulty > 1 && difficulty < 10 && 'Любитель'}
                    {difficulty >= 10 && difficulty < 15 && 'Продвинутый'}
                    {difficulty >= 15 && 'Мастер'}
                  </div>
                </div>
                <button
                  onClick={resetGame}
                  className="btn btn-primary w-full flex items-center justify-center space-x-2"
                  disabled={!isEngineReady}
                >
                  <RefreshIcon className="w-5 h-5" />
                  <span>Новая игра</span>
                </button>
              </div>
            </div>

            <div className="card">
              <h2 className="text-xl font-semibold mb-4">Статус игры</h2>
              <div className="text-center text-lg">
                {!isEngineReady ? (
                  <div className="flex items-center justify-center space-x-2">
                    <div className="loading-spinner" />
                    <span>Загрузка движка...</span>
                  </div>
                ) : isThinking ? (
                  <div className="flex items-center justify-center space-x-2">
                    <div className="loading-spinner" />
                    <span>Компьютер думает...</span>
                  </div>
                ) : (
                  status
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
