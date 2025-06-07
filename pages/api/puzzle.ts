import type { NextApiRequest, NextApiResponse } from 'next';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  try {
    const r = await fetch('https://api.chess.com/pub/puzzle/random', { cache: 'no-store' });
    if (!r.ok) throw new Error(`status ${r.status}`);
    const data = await r.json();
    res.status(200).json(data);
  } catch (e: any) {
    res.status(500).json({ error: e.message });
  }
}
