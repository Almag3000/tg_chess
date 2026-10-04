// Выполняет SQL в изолированном воркере: бесконечный запрос можно просто «убить» по таймауту.
importScripts('../vendor/sql-wasm.js');

let SQL = null;
let base = null;
const MAX_ROWS = 20000;

function send(id, payload) { postMessage({ id, ...payload }); }

function lastSet(db, sql) {
  const res = db.exec(sql);
  return res.length ? res[res.length - 1] : null;
}

onmessage = async (e) => {
  const m = e.data;
  try {
    if (m.type === 'init') {
      SQL = await initSqlJs({ locateFile: (f) => '../vendor/' + f });
      const db = new SQL.Database();
      db.run(m.sql);
      base = db.export();
      db.close();
      send(m.id, { ok: true });
      return;
    }
    // exec: каждый запуск идёт на свежей копии базы
    const db = new SQL.Database(base);
    try {
      let results = db.exec(m.sql);
      const modified = db.getRowsModified();
      let truncated = false;
      results = results.map((r) => {
        if (r.values.length > MAX_ROWS) { truncated = true; return { columns: r.columns, values: r.values.slice(0, MAX_ROWS) }; }
        return r;
      });
      let afterResult = null;
      if (m.after) afterResult = lastSet(db, m.after);
      send(m.id, { ok: true, results, modified, truncated, afterResult });
    } catch (err) {
      send(m.id, { ok: true, error: String(err.message || err) });
    } finally {
      db.close();
    }
  } catch (err) {
    send(m.id, { ok: false, error: String(err.message || err) });
  }
};
