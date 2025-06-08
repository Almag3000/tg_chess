import handler from '../pages/api/puzzle';
import type { NextApiRequest, NextApiResponse } from 'next';
import assert from 'assert';

function runHandler(): Promise<any> {
  return new Promise((resolve, reject) => {
    const res = {
      status(code: number) {
        this.statusCode = code;
        return this;
      },
      json(data: any) {
        if (this.statusCode !== 200) {
          reject(new Error(JSON.stringify(data)));
        } else {
          resolve(data);
        }
      },
    } as any as NextApiResponse;
    handler({} as NextApiRequest, res);
  });
}

(async () => {
  for (let i = 0; i < 10; i++) {
    const data = await runHandler();
    assert.ok(data.fen && Array.isArray(data.solution));
    assert.ok(data.solution.length >= 3);
  }
  console.log('ok');
})();
