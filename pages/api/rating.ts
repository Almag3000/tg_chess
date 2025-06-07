import type { NextApiRequest, NextApiResponse } from 'next';

const url = process.env.UPSTASH_REDIS_REST_URL;
const token = process.env.UPSTASH_REDIS_REST_TOKEN;

async function redisCmd(cmd: string[]): Promise<any> {
  if (!url || !token) throw new Error('Redis env vars not set');
  const res = await fetch(`${url}/${cmd.join('/')}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: 'no-store',
  });
  return res.json();
}

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === 'GET') {
    const { user } = req.query;
    try {
      if (typeof user === 'string') {
        const data = await redisCmd(['get', `rating:${user}`]);
        const rating = data.result ? parseInt(data.result) : 1000;
        res.status(200).json({ rating });
        return;
      }
      const data = await redisCmd(['zrevrange', 'leaderboard', '0', '-1', 'withscores']);
      if (!Array.isArray(data.result)) throw new Error('bad data');
      const leaderboard: { name: string; rating: number }[] = [];
      for (let i = 0; i < data.result.length; i += 2) {
        leaderboard.push({ name: data.result[i], rating: parseInt(data.result[i + 1]) });
      }
      res.status(200).json({ leaderboard });
    } catch (e: any) {
      res.status(500).json({ error: e.message });
    }
  } else if (req.method === 'POST') {
    const { user, rating } = req.body as { user: string; rating: number };
    if (!user) return res.status(400).json({ error: 'no user' });
    try {
      await redisCmd(['set', `rating:${user}`, String(rating)]);
      await redisCmd(['zadd', 'leaderboard', String(rating), user]);
      res.status(200).json({ ok: true });
    } catch (e: any) {
      res.status(500).json({ error: e.message });
    }
  } else {
    res.status(405).end();
  }
}
