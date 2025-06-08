import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { Game } from "js-chess-engine";

const Chessboard = dynamic(() => import("chessboardjsx"), { ssr: false });

function mapDifficulty(level: number): number {
  return Math.min(4, Math.floor((level - 1) / 14));
}

export default function Home() {
  const [menu, setMenu] = useState(true);
  const [color, setColor] = useState<"white" | "black">("white");
  const [level, setLevel] = useState(1);
  const [game, setGame] = useState<Game | null>(null);
  const [fen, setFen] = useState("");
  const [selected, setSelected] = useState<string | null>(null);
  const [legal, setLegal] = useState<string[]>([]);
  const [squareStyles, setSquareStyles] = useState<Record<string, any>>({});
  const [lastMove, setLastMove] = useState<string[]>([]);
  const [message, setMessage] = useState("");
  const [paused, setPaused] = useState(false);
  const [moves, setMoves] = useState<string[]>([]);

  const highlightSquares = (
    from: string | null,
    moves: string[] = [],
    lm: string[] = lastMove,
  ) => {
    const styles: Record<string, any> = {};
    if (lm.length === 2) {
      const [f, t] = lm;
      styles[f.toLowerCase()] = { backgroundColor: "rgba(255,215,0,0.5)" };
      styles[t.toLowerCase()] = { backgroundColor: "rgba(255,215,0,0.5)" };
    }
    if (from) {
      styles[from.toLowerCase()] = { boxShadow: "inset 0 0 0 3px rgba(50,150,255,0.8)" };
      moves.forEach((sq) => {
        styles[sq.toLowerCase()] = {
          background:
            "radial-gradient(circle, rgba(50,150,255,0.7) 20%, rgba(0,0,0,0) 22%)",
        };
      });
    }
    setSquareStyles(styles);
  };

  const startGame = () => {
    const g = new Game();
    setGame(g);
    setMenu(false);
    setPaused(false);
    setMoves([]);
    const f = g.exportFEN();
    setFen(f);
    if (color === "black") {
      const ai = g.aiMove(mapDifficulty(level)) as Record<string, string>;
      const [from, to] = Object.entries(ai)[0];
      setLastMove([from, to]);
      setMoves((m) => [...m, `${from}-${to}`]);
      setFen(g.exportFEN());
      highlightSquares(null, [], [from, to]);
    }
  };

  const onSquareClick = (square: string) => {
    if (!game || paused) return;
    square = square.toUpperCase();
    if (selected) {
      if (legal.includes(square)) {
        try {
          game.move(selected, square);
          setLastMove([selected, square]);
          setMoves((m) => [...m, `${selected}-${square}`]);
          highlightSquares(null, [], [selected, square]);
          afterPlayerMove();
        } catch {}
      }
      setSelected(null);
      setLegal([]);
      highlightSquares(null, []);
    } else {
      const state = game.exportJson();
      if (
        state.turn === (color === "white" ? "white" : "black") &&
        state.moves[square]
      ) {
        setSelected(square);
        setLegal(state.moves[square]);
        highlightSquares(square, state.moves[square]);
      }
    }
  };

  const onMouseOverSquare = (square: string) => {
    if (!game || selected || paused) return;
    square = square.toUpperCase();
    const state = game.exportJson();
    if (
      state.turn === (color === "white" ? "white" : "black") &&
      state.moves[square]
    ) {
      highlightSquares(square, state.moves[square]);
    }
  };

  const onMouseOutSquare = () => {
    if (!selected && !paused) {
      highlightSquares(null, []);
    }
  };

  const afterPlayerMove = () => {
    if (!game) return;
    setFen(game.exportFEN());
    const state = game.exportJson();
    if (state.checkMate) {
      setMessage("Мат! Вы победили");
      return;
    }
    const ai = game.aiMove(mapDifficulty(level)) as Record<string, string>;
    const [from, to] = Object.entries(ai)[0];
    setLastMove([from, to]);
    setMoves((m) => [...m, `${from}-${to}`]);
    setFen(game.exportFEN());
    highlightSquares(null, [], [from, to]);
    const st = game.exportJson();
    if (st.checkMate) {
      setMessage("Мат! Компьютер победил");
    }
  };

  const returnToMenu = () => {
    setMenu(true);
    setGame(null);
    setMessage("");
    setSelected(null);
    setLegal([]);
    setPaused(false);
    setFen("");
    setSquareStyles({});
    setLastMove([]);
    setMoves([]);
  };

  const boardWidth = 320;

  if (menu) {
    return (
      <main className="p-4 menu-screen">
        <h2 className="text-xl mb-2">Шахматы с компьютером</h2>
        <div className="mb-4">
          <label className="block mb-1">Цвет фигур</label>
          <div className="color-buttons">
            <button
              className={`color-btn ${color === "white" ? "selected" : ""}`}
              onClick={() => setColor("white")}
            >
              Белые
            </button>
            <button
              className={`color-btn ${color === "black" ? "selected" : ""}`}
              onClick={() => setColor("black")}
            >
              Чёрные
            </button>
          </div>
        </div>
        <div className="mb-4">
          <label className="block mb-1">Уровень сложности: {level}</label>
          <div className="level-picker">
            <button onClick={() => setLevel(Math.max(1, level - 1))}>-</button>
            <input
              type="number"
              min="1"
              max="69"
              value={level}
              onChange={(e) => setLevel(parseInt(e.target.value))}
            />
            <button onClick={() => setLevel(Math.min(69, level + 1))}>+</button>
          </div>
        </div>
        <button className="btn full-width" onClick={startGame}>
          Начать игру
        </button>
      </main>
    );
  }

  return (
    <main className="p-4">
      <h2 className="text-xl mb-2">Игра</h2>
      <button className="btn full-width mb-2" onClick={() => setPaused(true)}>
        Пауза
      </button>
      <div
        style={{ position: "relative", width: boardWidth, margin: "0 auto" }}
      >
        <Chessboard
          position={fen}
          width={boardWidth}
          onSquareClick={onSquareClick}
          onMouseOverSquare={onMouseOverSquare}
          onMouseOutSquare={onMouseOutSquare}
          squareStyles={squareStyles}
          orientation={color}
          boardStyle={{ border: "2px solid #444" }}
          lightSquareStyle={{ backgroundColor: "#f0d9b5" }}
          darkSquareStyle={{ backgroundColor: "#b58863" }}
          draggable={false}
        />
        {message && <div className="status-overlay show">{message}</div>}
        {paused && (
          <div className="pause-overlay">
            <button className="btn" onClick={() => setPaused(false)}>
              Продолжить
            </button>
            <button className="btn" onClick={returnToMenu}>
              В меню
            </button>
          </div>
        )}
      </div>
      <div className="move-log">
        {moves.map((m, i) => (
          <div key={i}>{i + 1}. {m}</div>
        ))}
      </div>
    </main>
  );
}
