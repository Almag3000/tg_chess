import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { useRouter } from "next/router";
import puzzles from "@/data/puzzles.json";
import { Leaderboard } from "@/components/Leaderboard";

const Chessboard = dynamic(() => import("chessboardjsx"), { ssr: false });

export default function Home() {
  const router = useRouter();
  const user = (router.query.username as string) || "Anonymous";

  const [fen, setFen] = useState("");
  const [solution, setSolution] = useState<string[]>([]);
  const [moveIndex, setMoveIndex] = useState(0);
  const [status, setStatus] = useState("");
  const [rating, setRating] = useState<number>(() => {
    if (typeof window !== "undefined") {
      const saved = localStorage.getItem("rating_" + user);
      return saved ? parseInt(saved) : 1000;
    }
    return 1000;
  });

  useEffect(() => {
    const puzzle = puzzles[Math.floor(Math.random() * puzzles.length)];
    setFen(puzzle.fen);
    setSolution(puzzle.solution);
    setStatus("");
    setMoveIndex(0);
  }, []);

  const onDrop = ({ sourceSquare, targetSquare }: any) => {
    const move = sourceSquare + targetSquare;
    if (move === solution[moveIndex]) {
      if (moveIndex + 1 === solution.length) {
        setStatus("✅ Правильно!");
        const newRating = rating + 10;
        setRating(newRating);
        localStorage.setItem("rating_" + user, newRating.toString());
      } else {
        setMoveIndex(moveIndex + 1);
      }
    } else {
      setStatus("❌ Ошибка");
      const newRating = Math.max(0, rating - 10);
      setRating(newRating);
      localStorage.setItem("rating_" + user, newRating.toString());
    }
  };

  return (
    <main className="p-4">
      <h2 className="text-xl mb-2">Реши шахматную задачу</h2>
      <Chessboard position={fen} onDrop={onDrop} width={350} />
      <p className="mt-2">{status}</p>
      <p className="mt-2">Рейтинг: {rating}</p>
      <Leaderboard />
    </main>
  );
}
