import type { NextApiRequest, NextApiResponse } from 'next';
import { Chess } from 'chess.js';

function randomFen(): string {
  const chess = new Chess();
  const moves = 3 + Math.floor(Math.random() * 4);
  for (let i = 0; i < moves; i++) {
    const legal = chess.moves();
    if (!legal.length) break;
    const move = legal[Math.floor(Math.random() * legal.length)];
    chess.move(move);
  }
  return chess.fen();
}

function findMate(fen: string): string[] | null {
  const chess = new Chess(fen);
  const moves1 = chess.moves({ verbose: true });
  for (const m1 of moves1) {
    chess.move(m1);
    const replies = chess.moves({ verbose: true });
    for (const r1 of replies) {
      chess.move(r1);
      const moves2 = chess.moves({ verbose: true });
      for (const m2 of moves2) {
        chess.move(m2);
        if (chess.isCheckmate()) {
          const line = [m1.from + m1.to, r1.from + r1.to, m2.from + m2.to];
          chess.undo();
          chess.undo();
          chess.undo();
          return line;
        }
        chess.undo();
      }
      chess.undo();
    }
    chess.undo();
  }
  return null;
}

export default function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  try {
    for (let i = 0; i < 50; i++) {
      const fen = randomFen();
      const solution = findMate(fen);
      if (solution && solution.length >= 3) {
        res.status(200).json({ fen, solution });
        return;
      }
    }
    res.status(500).json({ error: 'no puzzle' });
  } catch (err) {
    console.error('Puzzle error', err);
    res.status(500).json({ error: 'failed' });
  }
}
