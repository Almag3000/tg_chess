import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/router";
import puzzles from "@/data/puzzles.json";
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

  const updateLeaderboard = async (value: number) => {
    try {
      await fetch("/api/leaderboard", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ user, rating: value }),
      });
    } catch (e) {
      console.error("Failed to update leaderboard", e);
    }
  };

  const loadPuzzle = async () => {
    const useLocal = () => {
      let history: string[] = [];
      if (typeof window !== "undefined") {
        history = JSON.parse(localStorage.getItem("puzzle_history") || "[]");
      }
      if (history.length >= puzzles.length) history = [];
      const pool = puzzles.filter((p) => !history.includes(p.fen));
      const puzzle = pool[Math.floor(Math.random() * pool.length)];
      if (typeof window !== "undefined") {
        history.push(puzzle.fen);
        localStorage.setItem("puzzle_history", JSON.stringify(history));
      }
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

    const parsePgn = (pgn: string) => {
      const body = pgn
        .split("\n")
        .filter((l) => !l.startsWith("[") && l.trim())
        .join(" ");
      return body
        .replace(/\d+\.\.\.?/g, "")
        .replace(/\d+\./g, "")
        .trim()
        .split(/\s+/)
        .filter((m) => m && m !== "*");
    };

    try {
      let history: string[] = [];
      if (typeof window !== "undefined") {
        history = JSON.parse(localStorage.getItem("puzzle_history") || "[]");
      }
      for (let attempt = 0; attempt < 5; attempt++) {
        const res = await fetch("https://api.chess.com/pub/puzzle/random");
        const data = await res.json();
        if (history.includes(data.fen)) continue;
        const g = new Chess(data.fen);
        const sanMoves = parsePgn(data.pgn);
        const seq: string[] = [];
        for (const san of sanMoves) {
          const mv = (g as any).move(san, { sloppy: true });
          if (!mv) throw new Error("Invalid puzzle from API");
          seq.push(mv.from + mv.to + (mv.promotion || ""));
        }

        if (typeof window !== "undefined") {
          history.push(data.fen);
          localStorage.setItem(
            "puzzle_history",
            JSON.stringify(history.slice(-50))
          );
        }

        setFen(g.fen());
        setSolution(seq);
        setStatus("");
        setMoveIndex(0);
        setSelectedSquare(null);
        setLegalSquares([]);
        setSquareStyles({});
        setGame(g);
        setOrientation(g.turn() === "w" ? "white" : "black");
        return;
      }
      throw new Error("Could not fetch unique puzzle");
    } catch (e) {
      console.error("Failed to load puzzle from API", e);
      useLocal();
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
      const saved = localStorage.getItem("rating_" + name);
      setRating(saved ? parseInt(saved) : 1000);
    }
    setUser(name || "Anonymous");
  }, [router.isReady, router.query.username]);

  useEffect(() => {
    if (user) {
      loadPuzzle();
    }
  }, [user]);

  useEffect(() => {
    if (user) {
      updateLeaderboard(rating);
    }
  }, [rating, user]);

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
        setRating(newRating);
        localStorage.setItem("rating_" + user, newRating.toString());
        updateLeaderboard(newRating);
        setTimeout(loadPuzzle, 1000);
      } else {
        setMoveIndex(next);
      }
    } else {
      setStatus("❌ Ошибка");
      const newRating = Math.max(0, rating - 10);
      setRating(newRating);
      localStorage.setItem("rating_" + user, newRating.toString());
      updateLeaderboard(newRating);
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
    setRating(newRating);
    localStorage.setItem("rating_" + user, newRating.toString());
    updateLeaderboard(newRating);
    const from = move.slice(0, 2);
    const to = move.slice(2, 4);
    const highlight: Record<string, any> = {};
    highlight[from] = { backgroundColor: "rgba(30,144,255,0.6)" };
    highlight[to] = { backgroundColor: "rgba(30,144,255,0.6)" };
    setSquareStyles(highlight);
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
        boardStyle={{ border: "2px solid #444" }}
        lightSquareStyle={{ backgroundColor: "#f0d9b5" }}
        darkSquareStyle={{ backgroundColor: "#b58863" }}
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
