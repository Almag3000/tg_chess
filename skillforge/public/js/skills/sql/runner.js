import { datasetSQL } from './dataset.js';
import { gradeCode, toOutput } from './grade.js';

// Раннер навыка: всё, что нужно ядру сайта, чтобы запускать и проверять код этого навыка.
let worker = null;
let seq = 0;
const pending = new Map();
let readyPromise = null;

function spawn() {
  worker = new Worker(new URL('../../../worker/sql-worker.js', import.meta.url));
  worker.onmessage = (e) => {
    const p = pending.get(e.data.id);
    if (p) { pending.delete(e.data.id); p.resolve(e.data); }
  };
  return call({ type: 'init', sql: datasetSQL() });
}

function call(msg) {
  return new Promise((resolve) => {
    const id = ++seq;
    pending.set(id, { resolve });
    worker.postMessage({ ...msg, id });
  });
}

function init() {
  if (!readyPromise) readyPromise = spawn();
  return readyPromise;
}

async function exec({ sql, after }) {
  await init();
  const id = seq + 1;
  const timer = setTimeout(() => {
    // убиваем зависший воркер и поднимаем новый
    const p = pending.get(id);
    worker.terminate();
    readyPromise = spawn();
    if (p) { pending.delete(id); p.resolve({ ok: true, error: 'timeout' }); }
  }, 5000);
  const res = await call({ type: 'exec', sql, after });
  clearTimeout(timer);
  if (res.ok === false) return { error: res.error, results: [] };
  return res;
}

let schemaCache = null;
async function loadSchema() {
  await init();
  const t = await exec({ sql: "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY rowid" });
  const out = [];
  for (const [name] of t.results[0].values) {
    const cols = await exec({ sql: `PRAGMA table_info(${name})` });
    const cnt = await exec({ sql: `SELECT COUNT(*) FROM ${name}` });
    out.push({ name, rows: cnt.results[0].values[0][0], columns: cols.results[0].values.map((c) => ({ name: c[1], type: c[2], notnull: !!c[3], pk: !!c[5] })) });
  }
  return out;
}

export const runner = {
  language: 'sql',
  mode: 'text/x-sqlite',
  placeholder: '-- Напишите запрос здесь и нажмите Ctrl+Enter',
  init,
  async run(code) {
    if (!code.trim()) return { kind: 'ok', message: 'Пустой запрос.' };
    return toOutput(await exec({ sql: code }));
  },
  grade: (code, ex) => gradeCode(exec, ex, code),
  schema() { return (schemaCache ||= loadSchema()); },
  /** Таблицы учебной базы, которые нужны для упражнения (по порядку появления в решении). */
  async tablesFor(ex) {
    const text = `${ex.starter || ''}\n${ex.solution || ''}\n${ex.check || ''}`.toLowerCase();
    const found = [];
    for (const t of await this.schema()) {
      const m = text.match(new RegExp(`\\b${t.name}\\b`));
      if (m) found.push({ t, at: m.index });
    }
    return found.sort((a, b) => a.at - b.at).map((x) => x.t);
  },
  sampleQuery: (table) => `SELECT * FROM ${table} LIMIT 10;`,
};
