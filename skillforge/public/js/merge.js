// Чистые функции слияния и валидации состояния прогресса.
// Используются и в браузере (store.js), и на сервере (server/server.js).
//
// Форма состояния:
// {
//   v: 1, name, nameU,
//   skills: { [skillId]: { ex: { [exId]: Rec }, lessons: { [lessonId]: { r, u } } } },
//   activity: { 'YYYY-MM-DD': число_решённых_задач }
// }
// Rec: { s:1 решено, a:попытки, h:макс. подсказок до решения, v:1 смотрел решение,
//        t:время первого решения, xp, b:коробка Лейтнера, d:когда повторять, u:время изменения, c:код }

export const STATE_VERSION = 1;
export const emptyState = () => ({ v: STATE_VERSION, name: '', nameU: 0, skills: {}, activity: {} });

const newest = (x, y) => ((y?.u || 0) > (x?.u || 0) ? y : x);

function mergeMap(a = {}, b = {}) {
  const out = {};
  for (const k of new Set([...Object.keys(a), ...Object.keys(b)])) out[k] = newest(a[k], b[k]) || a[k] || b[k];
  return out;
}

// Запись упражнения: побеждает более свежая, но «решено» не теряется никогда,
// а счётчик попыток и заработанные очки не уменьшаются.
function mergeEx(a, b) {
  if (!a || !b) return a || b;
  const n = newest(a, b);
  const o = n === a ? b : a;
  const out = { ...n, a: Math.max(a.a, b.a), xp: Math.max(a.xp, b.xp) };
  if (o.s && !n.s) Object.assign(out, { s: 1, t: o.t, h: o.h, v: o.v, b: o.b, d: o.d });
  else if (o.s && n.s) { out.t = Math.min(...[a.t, b.t].filter(Boolean)) || 0; }
  return out;
}

function mergeExMap(a = {}, b = {}) {
  const out = {};
  for (const k of new Set([...Object.keys(a), ...Object.keys(b)])) out[k] = mergeEx(a[k], b[k]);
  return out;
}

export function mergeStates(a, b) {
  a = a || emptyState();
  b = b || emptyState();
  const out = emptyState();
  const bn = (b.nameU || 0) > (a.nameU || 0);
  out.name = bn ? b.name : a.name;
  out.nameU = Math.max(a.nameU || 0, b.nameU || 0);
  for (const id of new Set([...Object.keys(a.skills || {}), ...Object.keys(b.skills || {})])) {
    const sa = a.skills?.[id] || {};
    const sb = b.skills?.[id] || {};
    out.skills[id] = { ex: mergeExMap(sa.ex, sb.ex), lessons: mergeMap(sa.lessons, sb.lessons) };
  }
  for (const d of new Set([...Object.keys(a.activity || {}), ...Object.keys(b.activity || {})])) {
    out.activity[d] = Math.max(a.activity?.[d] || 0, b.activity?.[d] || 0);
  }
  return out;
}

const ID = /^[\w.\-]{1,64}$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const int = (v, max = 1e9) => (Number.isFinite(v) ? Math.max(0, Math.min(max, Math.floor(v))) : 0);

// Приводит произвольный JSON к безопасной форме состояния (отбрасывает лишнее).
export function sanitizeState(raw) {
  const out = emptyState();
  if (!raw || typeof raw !== 'object') return out;
  out.name = typeof raw.name === 'string' ? raw.name.slice(0, 40) : '';
  out.nameU = int(raw.nameU, 4e12);
  let skills = 0;
  for (const [sid, s] of Object.entries(raw.skills || {})) {
    if (!ID.test(sid) || !s || typeof s !== 'object' || ++skills > 50) continue;
    const dst = { ex: {}, lessons: {} };
    let n = 0;
    for (const [id, r] of Object.entries(s.ex || {})) {
      if (!ID.test(id) || !r || typeof r !== 'object' || ++n > 3000) continue;
      dst.ex[id] = {
        s: r.s ? 1 : 0, a: int(r.a, 1e5), h: int(r.h, 10), v: r.v ? 1 : 0,
        t: int(r.t, 4e12), xp: int(r.xp, 1e5), b: int(r.b, 10), d: int(r.d, 4e12), u: int(r.u, 4e12),
        c: typeof r.c === 'string' ? r.c.slice(0, 4000) : '',
      };
    }
    n = 0;
    for (const [id, r] of Object.entries(s.lessons || {})) {
      if (!ID.test(id) || !r || typeof r !== 'object' || ++n > 1000) continue;
      dst.lessons[id] = { r: r.r ? 1 : 0, u: int(r.u, 4e12) };
    }
    out.skills[sid] = dst;
  }
  let days = 0;
  for (const [d, n] of Object.entries(raw.activity || {})) {
    if (DATE.test(d) && ++days <= 1500) out.activity[d] = int(n, 1e5);
  }
  return out;
}
