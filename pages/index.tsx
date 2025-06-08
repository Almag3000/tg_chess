import { useEffect, useState } from 'react';
import dynamic from 'next/dynamic';
import { Game } from 'js-chess-engine';

const Chessboard = dynamic(() => import('chessboardjsx'), { ssr: false });

function mapDifficulty(level: number): number {
  return Math.min(4, Math.floor((level - 1) / 14));
}

export default function Home() {
  const [menu, setMenu] = useState(true);
  const [color, setColor] = useState<'white' | 'black'>('white');
  const [level, setLevel] = useState(1);
  const [game, setGame] = useState<Game | null>(null);
  const [fen, setFen] = useState('');
  const [selected, setSelected] = useState<string | null>(null);
  const [legal, setLegal] = useState<string[]>([]);
  const [squareStyles, setSquareStyles] = useState<Record<string, any>>({});
  const [message, setMessage] = useState('');

  const startGame = () => {
    const g = new Game();
    setGame(g);
    setMenu(false);
    const f = g.exportFEN();
    setFen(f);
    if (color === 'black') {
      g.aiMove(mapDifficulty(level));
      setFen(g.exportFEN());
    }
  };

  const onSquareClick = (square: string) => {
    if (!game) return;
    square = square.toUpperCase();
    if (selected) {
      if (legal.includes(square)) {
        try {
          game.move(selected, square);
          afterPlayerMove();
        } catch {}
      }
      setSelected(null);
      setLegal([]);
      setSquareStyles({});
    } else {
      const state = game.exportJson();
      if (state.turn === (color === 'white' ? 'white' : 'black') && state.moves[square]) {
        setSelected(square);
        setLegal(state.moves[square]);
        const highlight: Record<string, any> = {};
        highlight[square] = { backgroundColor: 'rgba(0,255,0,0.4)' };
        state.moves[square].forEach((sq: string) => {
          highlight[sq] = { backgroundColor: 'rgba(255,255,0,0.4)' };
        });
        setSquareStyles(highlight);
      }
    }
  };

  const afterPlayerMove = () => {
    if (!game) return;
    setFen(game.exportFEN());
    const state = game.exportJson();
    if (state.checkMate) {
      setMessage('Мат! Вы победили');
      return;
    }
    game.aiMove(mapDifficulty(level));
    setFen(game.exportFEN());
    const st = game.exportJson();
    if (st.checkMate) {
      setMessage('Мат! Компьютер победил');
    }
  };

  const boardWidth = 320;

  if (menu) {
    const levels = Array.from({ length: 69 }, (_, i) => i + 1);
    return (
      <main className="p-4">
        <h2 className="text-xl mb-2">Шахматы с компьютером</h2>
        <div className="mb-4">
          <label className="block mb-1">Цвет фигур</label>
          <select value={color} onChange={(e) => setColor(e.target.value as any)} className="border p-1">
            <option value="white">Белые</option>
            <option value="black">Чёрные</option>
          </select>
        </div>
        <div className="mb-4">
          <label className="block mb-1">Уровень сложности</label>
          <select value={level} onChange={(e) => setLevel(parseInt(e.target.value))} className="border p-1">
            {levels.map((n) => (
              <option key={n} value={n}>{n}</option>
            ))}
          </select>
        </div>
        <button className="btn" onClick={startGame}>Начать игру</button>
      </main>
    );
  }

  return (
    <main className="p-4">
      <h2 className="text-xl mb-2">Игра</h2>
      <div style={{ position: 'relative', width: boardWidth, margin: '0 auto' }}>
        <Chessboard
          position={fen}
          width={boardWidth}
          onSquareClick={onSquareClick}
          squareStyles={squareStyles}
          orientation={color}
          boardStyle={{ border: '2px solid #444' }}
          lightSquareStyle={{ backgroundColor: '#f0d9b5' }}
          darkSquareStyle={{ backgroundColor: '#b58863' }}
          draggable={false}
        />
        {message && <div className="status-overlay show">{message}</div>}
      </div>
    </main>
  );
}
