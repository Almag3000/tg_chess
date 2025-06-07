import type { NextApiRequest, NextApiResponse } from 'next';
import { spawn } from 'child_process';
import { Chess } from 'chess.js';

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  try {
    const engine = spawn('node', ['node_modules/stockfish/src/stockfish-nnue-16.js']);
    const send = (cmd: string) => engine.stdin.write(cmd + '\n');
    const waitFor = (pattern: RegExp): Promise<string> =>
      new Promise((resolve) => {
        const onData = (data: Buffer) => {
          const line = data.toString();
          if (pattern.test(line)) {
            engine.stdout.off('data', onData);
            resolve(line);
          }
        };
        engine.stdout.on('data', onData);
      });

    send('uci');
    await waitFor(/uciok/);
    send('isready');
    await waitFor(/readyok/);

    const game = new Chess();
    for (let i = 0; i < 10; i++) {
      const moves = game.moves();
      game.move(moves[Math.floor(Math.random() * moves.length)]);
    }
    const fen = game.fen();

    send(`position fen ${fen}`);
    send('go depth 12');
    const bestLine = await waitFor(/bestmove/);
    const move = bestLine.split(' ')[1].slice(0, 4);

    send('quit');

    res.status(200).json({ fen, solution: [move] });
  } catch (err) {
    console.error('Stockfish error', err);
    res.status(500).json({ error: 'failed' });
  }
}
