// Сервер Skillforge: отдаёт статику из public/ и хранит прогресс пользователей.
// Без зависимостей — достаточно Node.js 18+.
//
//   PORT=8080 HOST=0.0.0.0 DATA_DIR=./data node server/server.js
//
// Прогресс хранится в JSON-файлах, имя файла — SHA-256 от секретного кода пользователя
// (сам код на диск не пишется).
import http from 'node:http';
import { promises as fs, createReadStream } from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';
import { mergeStates, sanitizeState, emptyState } from '../public/js/merge.js';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../public');
const DATA_DIR = path.resolve(process.env.DATA_DIR || path.join(ROOT, '../data'));
const PORT = Number(process.env.PORT || 8080);
const HOST = process.env.HOST || '0.0.0.0';
const MAX_BODY = 2 * 1024 * 1024;
const TRUST_PROXY = process.env.TRUST_PROXY === '1';

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8', '.md': 'text/markdown; charset=utf-8',
  '.wasm': 'application/wasm', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon', '.txt': 'text/plain; charset=utf-8',
};
const COMPRESSIBLE = new Set(['.html', '.js', '.mjs', '.css', '.json', '.md', '.wasm', '.svg', '.txt']);

const SECURITY_HEADERS = {
  'X-Content-Type-Options': 'nosniff',
  'Referrer-Policy': 'no-referrer',
  'X-Frame-Options': 'DENY',
  'Content-Security-Policy':
    "default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; " +
    "worker-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
};

// ---------- хранилище ----------
const keyFile = (token) => path.join(DATA_DIR, crypto.createHash('sha256').update(token).digest('hex') + '.json');
const locks = new Map(); // простая очередь на токен, чтобы параллельные PUT не затирали друг друга

async function withLock(token, fn) {
  const prev = locks.get(token) || Promise.resolve();
  let release;
  const next = new Promise((r) => { release = r; });
  locks.set(token, prev.then(() => next));
  await prev;
  try { return await fn(); } finally { release(); if (locks.get(token) === next) locks.delete(token); }
}

async function readState(token) {
  try { return sanitizeState(JSON.parse(await fs.readFile(keyFile(token), 'utf8')).state); } catch (e) { if (e.code === 'ENOENT') return null; throw e; }
}
async function writeState(token, state) {
  const file = keyFile(token);
  const tmp = `${file}.${process.pid}.tmp`;
  await fs.writeFile(tmp, JSON.stringify({ state, updated: Date.now() }));
  await fs.rename(tmp, file);
}

// ---------- ограничение частоты ----------
const hits = new Map();
function rateLimited(ip) {
  const now = Date.now();
  const rec = hits.get(ip) || { n: 0, t: now };
  if (now - rec.t > 60000) { rec.n = 0; rec.t = now; }
  rec.n++;
  hits.set(ip, rec);
  return rec.n > 240;
}
setInterval(() => { const now = Date.now(); for (const [k, v] of hits) if (now - v.t > 120000) hits.delete(k); }, 60000).unref();

// ---------- вспомогательное ----------
function send(res, status, body, headers = {}) {
  res.writeHead(status, { ...SECURITY_HEADERS, ...headers });
  res.end(body);
}
const sendJSON = (res, status, obj) => send(res, status, JSON.stringify(obj), { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on('data', (c) => { size += c.length; if (size > MAX_BODY) { reject(Object.assign(new Error('too large'), { status: 413 })); req.destroy(); } else chunks.push(c); });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

// ---------- API ----------
const TOKEN_RE = /^[a-z0-9]{20,64}$/;

async function handleApi(req, res, url) {
  if (url.pathname === '/api/health') return sendJSON(res, 200, { ok: true, v: 1 });
  const m = url.pathname.match(/^\/api\/progress\/([^/]+)$/);
  if (!m || !TOKEN_RE.test(m[1])) return sendJSON(res, 404, { error: 'not found' });
  const token = m[1];

  if (req.method === 'GET') {
    const state = await readState(token);
    return sendJSON(res, 200, { state });
  }
  if (req.method === 'PUT') {
    let incoming;
    try { incoming = sanitizeState(JSON.parse(await readBody(req)).state); } catch (e) {
      return sendJSON(res, e.status || 400, { error: e.status === 413 ? 'too large' : 'bad json' });
    }
    const merged = await withLock(token, async () => {
      const existing = (await readState(token)) || emptyState();
      const out = mergeStates(existing, incoming);
      await writeState(token, out);
      return out;
    });
    return sendJSON(res, 200, { state: merged });
  }
  return sendJSON(res, 405, { error: 'method not allowed' });
}

// ---------- статика ----------
const cache = new Map(); // путь -> { mtime, raw, gz, etag }

async function serveStatic(req, res, url) {
  let rel = decodeURIComponent(url.pathname);
  if (rel.endsWith('/')) rel += 'index.html';
  const file = path.join(ROOT, path.normalize(rel));
  if (!file.startsWith(ROOT + path.sep) && file !== ROOT) return send(res, 403, 'Forbidden');
  let st;
  try { st = await fs.stat(file); } catch { st = null; }
  if (!st || !st.isFile()) {
    if (path.extname(rel)) return send(res, 404, 'Not found', { 'Content-Type': 'text/plain; charset=utf-8' });
    return serveStatic(req, res, new URL('/index.html', url));
  }
  const ext = path.extname(file).toLowerCase();
  const type = MIME[ext] || 'application/octet-stream';
  const etag = `W/"${st.size.toString(16)}-${Math.floor(st.mtimeMs).toString(16)}"`;
  const headers = {
    'Content-Type': type, ETag: etag,
    // vendor-библиотеки не меняются; остальное всегда перепроверяем по ETag
    'Cache-Control': rel.startsWith('/vendor/') ? 'public, max-age=604800' : 'no-cache',
  };
  if (req.headers['if-none-match'] === etag) return send(res, 304, null, headers);

  const wantsGzip = /\bgzip\b/.test(req.headers['accept-encoding'] || '') && COMPRESSIBLE.has(ext) && st.size > 512;
  if (!wantsGzip) {
    res.writeHead(200, { ...SECURITY_HEADERS, ...headers, 'Content-Length': st.size });
    if (req.method === 'HEAD') return res.end();
    return createReadStream(file).pipe(res);
  }
  let c = cache.get(file);
  if (!c || c.etag !== etag) {
    c = { etag, gz: zlib.gzipSync(await fs.readFile(file), { level: 9 }) };
    cache.set(file, c);
  }
  res.writeHead(200, { ...SECURITY_HEADERS, ...headers, 'Content-Encoding': 'gzip', Vary: 'Accept-Encoding', 'Content-Length': c.gz.length });
  res.end(req.method === 'HEAD' ? undefined : c.gz);
}

// ---------- сервер ----------
const server = http.createServer(async (req, res) => {
  try {
    const ip = (TRUST_PROXY && String(req.headers['x-forwarded-for'] || '').split(',')[0].trim()) || req.socket.remoteAddress || '?';
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname.startsWith('/api/')) {
      if (rateLimited(ip)) return sendJSON(res, 429, { error: 'too many requests' });
      return await handleApi(req, res, url);
    }
    if (!['GET', 'HEAD'].includes(req.method)) return send(res, 405, 'Method Not Allowed');
    return await serveStatic(req, res, url);
  } catch (e) {
    console.error(e);
    if (!res.headersSent) send(res, 500, 'Internal Server Error', { 'Content-Type': 'text/plain; charset=utf-8' });
    else res.end();
  }
});

await fs.mkdir(DATA_DIR, { recursive: true });
server.listen(PORT, HOST, () => console.log(`Skillforge: http://${HOST}:${PORT}  (данные: ${DATA_DIR})`));
for (const sig of ['SIGINT', 'SIGTERM']) process.on(sig, () => server.close(() => process.exit(0)));
