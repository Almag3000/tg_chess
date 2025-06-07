import type { NextApiRequest, NextApiResponse } from 'next';
import { kv } from '@vercel/kv';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === 'POST') {
    const { user, rating } = req.body || {};
    if (typeof user === 'string' && typeof rating === 'number') {
      try {
        await kv.zadd('leaderboard', { score: rating, member: user });
      } catch (e) {
        console.error('kv error', e);
      }
    }
  }
  try {
    const data = await kv.zrange('leaderboard', 0, 9, { rev: true, withScores: true });
    res.status(200).json(data);
  } catch (e) {
    console.error('kv error', e);
    res.status(200).json([]);
  }
}
