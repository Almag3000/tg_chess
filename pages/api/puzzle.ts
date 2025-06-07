import type { NextApiRequest, NextApiResponse } from 'next';
import { Chess } from 'chess.js';
import { Game } from 'js-chess-engine';

function randomFen(): string {
  const chess = new Chess();
  const moves = 10 + Math.floor(Math.random() * 10);
  for (let i = 0; i < moves; i++) {
    const legal = chess.moves();
    if (!legal.length) break;
    const move = legal[Math.floor(Math.random() * legal.length)];
    chess.move(move);
  }
  return chess.fen();
}

function aiLine(fen: string): string[] | null {
  const game = new Game(fen);
  const solution: string[] = [];
  for (let i = 0; i < 6; i++) {
    const m = game.aiMove(4);
    const from = Object.keys(m)[0];
    const to = m[from];
    solution.push(from + to);
    const status = game.exportJson();
    if (status.checkMate) break;
  }
  return solution.length === 4 || solution.length === 6 ? solution : null;
}

export default function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  try {
    for (let i = 0; i < 50; i++) {
      const fen = randomFen();
      const solution = aiLine(fen);
      if (solution) {
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
