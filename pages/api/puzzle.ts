import type { NextApiRequest, NextApiResponse } from 'next';
import { Chess } from 'chess.js';
import { aiMove } from 'js-chess-engine';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  try {
    const game = new Chess();
    for (let i = 0; i < 10; i++) {
      const moves = game.moves();
      game.move(moves[Math.floor(Math.random() * moves.length)]);
    }
    const fen = game.fen();
    const moveObj = aiMove(fen, 2);
    const from = Object.keys(moveObj)[0];
    const to = moveObj[from];
    const move = from.toLowerCase() + to.toLowerCase();
    res.status(200).json({ fen, solution: [move] });
  } catch (err) {
    console.error('Engine error', err);
    res.status(500).json({ error: 'failed' });
  }
}
