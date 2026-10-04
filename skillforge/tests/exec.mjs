// Executor на sql.js под Node — тот же интерфейс, что у воркера в браузере.
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
import { datasetSQL } from '../public/js/skills/sql/dataset.js';

const require = createRequire(import.meta.url);
const initSqlJs = require('../public/vendor/sql-wasm.js');

export async function makeExec() {
  const SQL = await initSqlJs({ wasmBinary: readFileSync(new URL('../public/vendor/sql-wasm.wasm', import.meta.url)) });
  const seed = new SQL.Database();
  seed.run(datasetSQL());
  const base = seed.export();
  seed.close();
  return async ({ sql, after }) => {
    const db = new SQL.Database(base);
    try {
      const results = db.exec(sql);
      const modified = db.getRowsModified();
      let afterResult = null;
      if (after) { const r = db.exec(after); afterResult = r.length ? r[r.length - 1] : null; }
      return { results, modified, afterResult };
    } catch (e) {
      return { error: String(e.message), results: [] };
    } finally { db.close(); }
  };
}
