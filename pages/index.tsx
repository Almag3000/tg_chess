import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/router";
import { Leaderboard } from "@/components/Leaderboard";
import { Chess } from "chess.js";

const Chessboard = dynamic(() => import("chessboardjsx"), { ssr: false });

export default function Home() {
  const router = useRouter();
  const [user, setUser] = useState("Anonymous");

  const [fen, setFen] = useState("");
  const [solution, setSolution] = useState<string[]>([]);
  const [moveIndex, setMoveIndex] = useState(0);
  const [status, setStatus] = useState("");
  const [game, setGame] = useState<Chess | null>(null);
  const [selectedSquare, setSelectedSquare] = useState<string | null>(null);
  const [legalSquares, setLegalSquares] = useState<string[]>([]);
  const [squareStyles, setSquareStyles] = useState<Record<string, any>>({});
  const [orientation, setOrientation] = useState<"white" | "black">("white");
  const [showLeaderboard, setShowLeaderboard] = useState(false);
  const [rating, setRating] = useState<number>(1000);
  const saveRating = (val: number) => {
    setRating(val);
    if (typeof window !== 'undefined') {
      fetch('/api/rating', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user, rating: val }),
      }).catch(() => {});
    }
  };

  const loadPuzzle = async () => {
    try {
      const res = await fetch('/api/puzzle');
      if (!res.ok) throw new Error('api failed');
      const data = await res.json();
      setFen(data.fen);
      setSolution(data.solution);
      setStatus('');
      setMoveIndex(0);
      setSelectedSquare(null);
      setLegalSquares([]);
      setSquareStyles({});
      const g = new Chess(data.fen);
      setGame(g);
      setOrientation(g.turn() === 'w' ? 'white' : 'black');
    } catch (e) {
      console.error('Failed to load puzzle from API', e);
    }
  };

  useEffect(() => {
    if (!router.isReady) return;
    let name = (router.query.username as string) || "";
    if (typeof window !== "undefined") {
      if (name) {
        localStorage.setItem("username", name);
      } else {
        name = localStorage.getItem("username") || "";
      }
      if (!name) {
        name = window.prompt("Введите ваше имя") || "Anonymous";
        localStorage.setItem("username", name);
      }
      (async () => {
        try {
          const res = await fetch("/api/rating?user=" + encodeURIComponent(name));
          const data = await res.json();
          setRating(data.rating ?? 1000);
        } catch {
          setRating(1000);
        }
      })();
    }
    setUser(name || "Anonymous");
  }, [router.isReady, router.query.username]);

  useEffect(() => {
    if (user) {
      loadPuzzle();
    }
  }, [user]);

  const applyMove = (from: string, to: string) => {
    if (!game) return;
    const move = from + to;
    if (move === solution[moveIndex]) {
      (game as any).move({ from, to });
      let next = moveIndex + 1;
      let lastFrom = from;
      let lastTo = to;
      if (next < solution.length) {
        const auto = solution[next];
        (game as any).move({ from: auto.slice(0, 2), to: auto.slice(2, 4) });
        lastFrom = auto.slice(0, 2);
        lastTo = auto.slice(2, 4);
        next += 1;
      }
      setFen(game.fen());

      const highlight: Record<string, any> = {};
      highlight[lastFrom] = { backgroundColor: "rgba(173,216,230,0.6)" };
      highlight[lastTo] = { backgroundColor: "rgba(173,216,230,0.6)" };
      setSquareStyles(highlight);

      if (next === solution.length) {
        setStatus("✅ Правильно!");
        const newRating = rating + 10;
        saveRating(newRating);
        setTimeout(loadPuzzle, 1000);
      } else {
        setMoveIndex(next);
      }
    } else {
      setStatus("❌ Ошибка");
      const newRating = Math.max(0, rating - 10);
      saveRating(newRating);
    }
  };

  const onSquareClick = (square: string) => {
    if (!game) return;

    if (selectedSquare) {
      if (legalSquares.includes(square)) {
        applyMove(selectedSquare, square);
      }
      setSelectedSquare(null);
      setLegalSquares([]);
      setSquareStyles({});
    } else {
      const piece = (game as any).get(square);
      if (piece && piece.color === game!.turn()) {
        const moves = (game as any).moves({ square, verbose: true }).map((m: any) => m.to);
        setSelectedSquare(square);
        setLegalSquares(moves);
        const highlight: Record<string, any> = {};
        highlight[square] = { backgroundColor: "rgba(0,255,0,0.4)" };
        moves.forEach((sq) => {
          highlight[sq] = { backgroundColor: "rgba(255,255,0,0.4)" };
        });
        setSquareStyles(highlight);
      }
    }
  };

  const showHint = () => {
    const move = solution[moveIndex];
    setStatus(`Правильный ход: ${move}`);
    const newRating = Math.max(0, rating - 10);
    saveRating(newRating);
    const from = move.slice(0, 2);
    const to = move.slice(2, 4);
    const highlight: Record<string, any> = {};
    highlight[from] = { backgroundColor: "rgba(30,144,255,0.6)" };
    highlight[to] = { backgroundColor: "rgba(30,144,255,0.6)" };
    setSquareStyles(highlight);
  };

  useEffect(() => {
    if (!status) return;
    const t = setTimeout(() => setStatus(""), 1000);
    return () => clearTimeout(t);
  }, [status]);

  const boardWidth = 320;

  return (
    <main className="p-4">
      <h2 className="text-xl mb-2">Реши шахматную задачу</h2>
      <div style={{ position: "relative", width: boardWidth, margin: "0 auto" }}>
        <Chessboard
          position={fen}
          width={boardWidth}
          onSquareClick={onSquareClick}
          squareStyles={squareStyles}
          orientation={orientation}
          boardStyle={{ border: "2px solid #444" }}
          lightSquareStyle={{ backgroundColor: "#f0d9b5" }}
          darkSquareStyle={{ backgroundColor: "#b58863" }}
          draggable={false}
        />
        {status && (
          <div className={`status-overlay show`}>{status}</div>
        )}
      </div>
      <div className="buttons">
        <button className="btn" onClick={showHint}>
          Подсказка
        </button>
        <button
          className="btn"
          onClick={() => setShowLeaderboard(!showLeaderboard)}
        >
          Лидеры
        </button>
      </div>
      <p className="mt-2">Рейтинг: {rating}</p>
      {showLeaderboard && <Leaderboard />}
    </main>
  );
}
