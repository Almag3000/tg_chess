import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/router";
import puzzles from "@/data/puzzles.json";
import { Leaderboard } from "@/components/Leaderboard";
import { Chess } from "chess.js";

const Chessboard = dynamic(() => import("chessboardjsx"), { ssr: false });

export default function Home() {
  const router = useRouter();
  const user = (router.query.username as string) || "Anonymous";

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
  const [rating, setRating] = useState<number>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("rating_" + user);
      return saved ? parseInt(saved) : 1000;
    }
    return 1000;
  });

  const loadPuzzle = () => {
    const puzzle = puzzles[Math.floor(Math.random() * puzzles.length)];
    setFen(puzzle.fen);
    setSolution(puzzle.solution);
    setStatus("");
    setMoveIndex(0);
    setSelectedSquare(null);
    setLegalSquares([]);
    setSquareStyles({});
    const g = new Chess(puzzle.fen);
    setGame(g);
    setOrientation(puzzle.fen.split(" ")[1] === "w" ? "white" : "black");
  };

  useEffect(() => {
    loadPuzzle();
  }, []);

  const applyMove = (from: string, to: string) => {
    if (!game) return;
    const move = from + to;
    if (move === solution[moveIndex]) {
      (game as any).move({ from, to });
      let next = moveIndex + 1;
      if (next === solution.length) {
        setStatus("✅ Правильно!");
        const newRating = rating + 10;
        setRating(newRating);
        localStorage.setItem("rating_" + user, newRating.toString());
        setTimeout(loadPuzzle, 1000);
        return;
      }

      const auto = solution[next];
      (game as any).move({ from: auto.slice(0, 2), to: auto.slice(2, 4) });
      next += 1;
      setFen(game.fen());

      if (next === solution.length) {
        setStatus("✅ Правильно!");
        const newRating = rating + 10;
        setRating(newRating);
        localStorage.setItem("rating_" + user, newRating.toString());
        setTimeout(loadPuzzle, 1000);
      } else {
        setMoveIndex(next);
      }
    } else {
      setStatus("❌ Ошибка");
      const newRating = Math.max(0, rating - 10);
      setRating(newRating);
      localStorage.setItem("rating_" + user, newRating.toString());
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
    setStatus(`Правильный ход: ${solution[moveIndex]}`);
    const newRating = Math.max(0, rating - 10);
    setRating(newRating);
    localStorage.setItem("rating_" + user, newRating.toString());
  };

  return (
    <main className="p-4">
      <h2 className="text-xl mb-2">Реши шахматную задачу</h2>
      <Chessboard
        position={fen}
        width={350}
        onSquareClick={onSquareClick}
        squareStyles={squareStyles}
        orientation={orientation}
        draggable={false}
      />
      <div className="mt-2">
        <button className="mr-2 px-2 py-1 bg-gray-200" onClick={showHint}>
          Подсказка
        </button>
        <button
          className="px-2 py-1 bg-gray-200"
          onClick={() => setShowLeaderboard(!showLeaderboard)}
        >
          Лидеры
        </button>
      </div>
      <p className="mt-2">{status}</p>
      <p className="mt-2">Рейтинг: {rating}</p>
      {showLeaderboard && <Leaderboard />}
    </main>
  );
}
