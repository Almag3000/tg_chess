import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';

type TileType = 'red' | 'blue' | 'green' | 'yellow' | 'purple';
type Position = { row: number; col: number };

interface GameProps {
  level: number;
  onLevelComplete: () => void;
}

export default function Game({ level, onLevelComplete }: GameProps) {
  const [board, setBoard] = useState<TileType[][]>([]);
  const [selectedTile, setSelectedTile] = useState<Position | null>(null);
  const [score, setScore] = useState(0);
  const [movesLeft, setMovesLeft] = useState(20);
  const [targetScore, setTargetScore] = useState(1000);

  const tileTypes: TileType[] = ['red', 'blue', 'green', 'yellow', 'purple'];
  const boardSize = 8;

  useEffect(() => {
    initializeBoard();
    setTargetScore(1000 + (level - 1) * 500);
    setMovesLeft(20);
    setScore(0);
  }, [level]);

  const initializeBoard = () => {
    const newBoard: TileType[][] = [];
    for (let i = 0; i < boardSize; i++) {
      const row: TileType[] = [];
      for (let j = 0; j < boardSize; j++) {
        row.push(tileTypes[Math.floor(Math.random() * tileTypes.length)]);
      }
      newBoard.push(row);
    }
    setBoard(newBoard);
  };

  const handleTileClick = (row: number, col: number) => {
    if (movesLeft <= 0) return;

    if (!selectedTile) {
      setSelectedTile({ row, col });
      return;
    }

    const isAdjacent = (
      Math.abs(selectedTile.row - row) === 1 && selectedTile.col === col ||
      Math.abs(selectedTile.col - col) === 1 && selectedTile.row === row
    );

    if (isAdjacent) {
      swapTiles(selectedTile, { row, col });
      setMovesLeft(prev => prev - 1);
    }

    setSelectedTile(null);
  };

  const swapTiles = (pos1: Position, pos2: Position) => {
    const newBoard = [...board];
    const temp = newBoard[pos1.row][pos1.col];
    newBoard[pos1.row][pos1.col] = newBoard[pos2.row][pos2.col];
    newBoard[pos2.row][pos2.col] = temp;
    setBoard(newBoard);

    // Проверяем совпадения после обмена
    setTimeout(() => {
      const matches = findMatches(newBoard);
      if (matches.length > 0) {
        removeMatches(matches);
        setScore(prev => prev + matches.length * 100);
      } else {
        // Если совпадений нет, возвращаем плитки на место
        const revertBoard = [...newBoard];
        const temp = revertBoard[pos1.row][pos1.col];
        revertBoard[pos1.row][pos1.col] = revertBoard[pos2.row][pos2.col];
        revertBoard[pos2.row][pos2.col] = temp;
        setBoard(revertBoard);
      }
    }, 300);
  };

  const findMatches = (board: TileType[][]): Position[] => {
    const matches: Position[] = [];

    // Проверка горизонтальных совпадений
    for (let i = 0; i < boardSize; i++) {
      for (let j = 0; j < boardSize - 2; j++) {
        if (
          board[i][j] === board[i][j + 1] &&
          board[i][j] === board[i][j + 2]
        ) {
          matches.push({ row: i, col: j });
          matches.push({ row: i, col: j + 1 });
          matches.push({ row: i, col: j + 2 });
        }
      }
    }

    // Проверка вертикальных совпадений
    for (let i = 0; i < boardSize - 2; i++) {
      for (let j = 0; j < boardSize; j++) {
        if (
          board[i][j] === board[i + 1][j] &&
          board[i][j] === board[i + 2][j]
        ) {
          matches.push({ row: i, col: j });
          matches.push({ row: i + 1, col: j });
          matches.push({ row: i + 2, col: j });
        }
      }
    }

    return matches;
  };

  const removeMatches = (matches: Position[]) => {
    const newBoard = [...board];
    matches.forEach(({ row, col }) => {
      newBoard[row][col] = tileTypes[Math.floor(Math.random() * tileTypes.length)];
    });
    setBoard(newBoard);
  };

  useEffect(() => {
    if (score >= targetScore) {
      onLevelComplete();
    }
  }, [score, targetScore]);

  return (
    <div className="flex flex-col items-center gap-6">
      <div className="flex justify-between w-full max-w-md">
        <div className="text-white">
          <p>Уровень: {level}</p>
          <p>Счёт: {score}/{targetScore}</p>
        </div>
        <div className="text-white">
          <p>Ходов осталось: {movesLeft}</p>
        </div>
      </div>

      <div className="grid grid-cols-8 gap-1 bg-background p-4 rounded-lg">
        {board.map((row, rowIndex) =>
          row.map((tile, colIndex) => (
            <motion.div
              key={`${rowIndex}-${colIndex}`}
              className={`
                w-12 h-12 rounded-lg cursor-pointer
                ${selectedTile?.row === rowIndex && selectedTile?.col === colIndex
                  ? 'ring-4 ring-white'
                  : 'hover:opacity-80'
                }
                bg-tile-${tile}
              `}
              onClick={() => handleTileClick(rowIndex, colIndex)}
              animate="pop"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            />
          ))
        )}
      </div>
    </div>
  );
} 