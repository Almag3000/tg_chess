import type { NextApiRequest, NextApiResponse } from 'next';
import { Chess } from 'chess.js';
import { spawn } from 'child_process';

function randomFen(): string {
  const chess = new Chess();
  const moves = 6 + Math.floor(Math.random() * 6); // shorter random game
  for (let i = 0; i < moves; i++) {
    const legal = chess.moves();
    if (!legal.length) break;
    const move = legal[Math.floor(Math.random() * legal.length)];
    chess.move(move);
  }
  return chess.fen();
}

async function stockfishLine(fen: string): Promise<string[] | null> {
  return new Promise((resolve) => {
    const engine = spawn('/usr/games/stockfish');
    let pv: string[] = [];
    let found = false;
    engine.stdout.on('data', (data) => {
      const lines = data.toString().split('\n');
      for (const line of lines) {
        if (line.startsWith('info')) {
          const m = line.match(/score mate (-?\d+)/);
          if (m) {
            const ply = Math.abs(parseInt(m[1], 10));
            if (ply <= 6) {
              found = true;
              const pvMatch = line.match(/ pv (.+)/);
              if (pvMatch) {
                pv = pvMatch[1].trim().split(' ');
              }
            }
          }
        } else if (line.startsWith('bestmove')) {
          engine.kill();
          resolve(found ? pv.slice(0, 6) : null);
        }
      }
    });
    engine.stdin.write('uci\n');
    engine.stdin.write('isready\n');
    engine.stdin.write('position fen ' + fen + '\n');
    engine.stdin.write('go depth 6\n');
  });
}

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  try {
    for (let i = 0; i < 20; i++) {
      const fen = randomFen();
      const solution = await stockfishLine(fen);
      if (solution && solution.length >= 2) {
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
