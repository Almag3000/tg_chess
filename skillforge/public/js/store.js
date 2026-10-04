// Хранилище прогресса: localStorage + необязательная синхронизация с сервером.
import { emptyState, mergeStates, sanitizeState } from './merge.js';
import { localDate } from './util.js';

const KEY = 'sf:state';
const TOKEN_KEY = 'sf:token';
const DAY = 86400000;
export const BOX_DAYS = [1, 3, 7, 16, 35];
const BASE_XP = [0, 10, 20, 35];

const safe = {
  get(k) { try { return localStorage.getItem(k); } catch { return null; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch { /* приватный режим */ } },
};

function makeToken() {
  const alphabet = 'abcdefghjkmnpqrstuvwxyz23456789';
  const bytes = crypto.getRandomValues(new Uint8Array(24));
  return [...bytes].map((b) => alphabet[b % alphabet.length]).join('');
}

let state;
try { state = sanitizeState(JSON.parse(safe.get(KEY) || 'null')); } catch { state = emptyState(); }
let token = safe.get(TOKEN_KEY);
if (!token) { token = makeToken(); safe.set(TOKEN_KEY, token); }

const listeners = new Set();
const sync = { available: false, status: 'off', last: 0, error: '' };
let saveTimer = null; let pushTimer = null; let pushing = false; let dirty = false;

const emit = () => listeners.forEach((f) => f());
function setSync(patch) { Object.assign(sync, patch); emit(); }

function persist() {
  safe.set(KEY, JSON.stringify(state));
  dirty = true;
  clearTimeout(pushTimer);
  if (sync.available) pushTimer = setTimeout(push, 1500);
}
function change() { persist(); emit(); }

// ---------- синхронизация ----------
async function api(path, opts) {
  const r = await fetch(path, { ...opts, headers: { 'Content-Type': 'application/json' }, cache: 'no-store' });
  if (!r.ok && r.status !== 404) throw new Error(`HTTP ${r.status}`);
  return r;
}

export async function pull() {
  if (!sync.available) return;
  setSync({ status: 'syncing' });
  try {
    const r = await api(`/api/progress/${token}`);
    const body = await r.json();
    if (body.state) {
      const remote = sanitizeState(body.state);
      state = mergeStates(state, remote);
      safe.set(KEY, JSON.stringify(state));
    }
    setSync({ status: 'ok', last: Date.now(), error: '' });
    emit();
    if (dirty) await push();
  } catch (e) { setSync({ status: 'error', error: String(e.message || e) }); }
}

export async function push() {
  if (!sync.available || pushing) return;
  pushing = true; dirty = false;
  setSync({ status: 'syncing' });
  try {
    const r = await api(`/api/progress/${token}`, { method: 'PUT', body: JSON.stringify({ state }) });
    const merged = sanitizeState((await r.json()).state);
    state = mergeStates(state, merged);
    safe.set(KEY, JSON.stringify(state));
    setSync({ status: 'ok', last: Date.now(), error: '' });
    emit();
  } catch (e) { dirty = true; setSync({ status: 'error', error: String(e.message || e) }); }
  pushing = false;
}

export async function initSync() {
  try {
    const r = await fetch('/api/health', { cache: 'no-store' });
    if (!r.ok || !(await r.json()).ok) throw new Error('no api');
    sync.available = true;
    await pull();
    window.addEventListener('focus', () => pull());
    document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'hidden') push(); });
  } catch { setSync({ available: false, status: 'off' }); }
}

// ---------- профиль ----------
export const getToken = () => token;
export const getSync = () => sync;
export async function useToken(newToken) {
  newToken = String(newToken).trim().toLowerCase();
  if (!/^[a-z0-9]{20,64}$/.test(newToken)) throw new Error('Неверный формат кода');
  token = newToken; safe.set(TOKEN_KEY, token);
  await pull();
  if (sync.available && sync.status === 'error') throw new Error('Сервер недоступен');
}
export function exportJSON() { return JSON.stringify({ token, state }, null, 1); }
export function importJSON(text) {
  const data = JSON.parse(text);
  const incoming = sanitizeState(data.state || data);
  state = mergeStates(state, incoming);
  change();
}
export function resetAll() {
  state = emptyState(); change();
}
export const getName = () => state.name;
export function setName(n) { state.name = n.slice(0, 40); state.nameU = Date.now(); change(); }
export function subscribe(fn) { listeners.add(fn); return () => listeners.delete(fn); }

// ---------- прогресс ----------
const skillState = (id) => (state.skills[id] ||= { ex: {}, lessons: {} });
export const rec = (skillId, exId) => state.skills[skillId]?.ex[exId] || null;

function bumpActivity() {
  const d = localDate();
  state.activity[d] = (state.activity[d] || 0) + 1;
}

function xpFor(ex, r) {
  let k = 1;
  if (r.v) k = 0.25;
  else if (ex.type === 'quiz') k = r.a > 1 ? 0.6 : 1;
  else k = Math.max(0.4, 1 - 0.2 * r.h);
  return Math.max(2, Math.round(BASE_XP[ex.level || 1] * k));
}

/** Фиксирует попытку решения. Возвращает { first, xp }. */
export function recordAttempt(skillId, ex, { passed, hintsUsed = 0, solutionSeen = false, code = '' }) {
  const s = skillState(skillId);
  const now = Date.now();
  const r = (s.ex[ex.id] ||= { s: 0, a: 0, h: 0, v: 0, t: 0, xp: 0, b: 0, d: 0, u: 0, c: '' });
  r.a += 1;
  r.c = code.slice(0, 4000);
  if (solutionSeen) r.v = 1;
  let res = { first: false, xp: 0 };
  if (passed && !r.s) {
    r.s = 1; r.t = now; r.h = hintsUsed;
    if (solutionSeen) r.v = 1;
    r.xp = xpFor(ex, r);
    r.b = 1; r.d = now + BOX_DAYS[0] * DAY;
    res = { first: true, xp: r.xp };
    bumpActivity();
  }
  r.u = now;
  change();
  return res;
}

/** Фиксирует отметку «увидел решение» без попытки проверки. */
export function markSolutionSeen(skillId, ex) {
  const r = (skillState(skillId).ex[ex.id] ||= { s: 0, a: 0, h: 0, v: 0, t: 0, xp: 0, b: 0, d: 0, u: 0, c: '' });
  if (!r.s) r.v = 1;
  r.u = Date.now();
  change();
}

export function recordReview(skillId, ex, passed) {
  const r = rec(skillId, ex.id);
  if (!r) return { xp: 0 };
  const now = Date.now();
  let xp = 0;
  if (passed) {
    r.b = Math.min(BOX_DAYS.length, (r.b || 1) + 1);
    r.xp += 4; xp = 4;
  } else {
    r.b = 1;
  }
  r.d = now + BOX_DAYS[r.b - 1] * DAY;
  r.u = now;
  bumpActivity();
  change();
  return { xp };
}

export function markLessonRead(skillId, lessonId) {
  const s = skillState(skillId);
  if (s.lessons[lessonId]?.r) return;
  s.lessons[lessonId] = { r: 1, u: Date.now() };
  change();
}
export const lessonRead = (skillId, lessonId) => !!state.skills[skillId]?.lessons[lessonId]?.r;

const isClean = (r) => r && r.s && !r.v && r.h === 0;

export function lessonStats(skillId, lesson) {
  const total = lesson.exercises.length;
  let solved = 0; let clean = 0; let started = 0;
  for (const ex of lesson.exercises) {
    const r = rec(skillId, ex.id);
    if (r) started++;
    if (r?.s) solved++;
    if (isClean(r)) clean++;
  }
  const done = solved === total;
  const mastered = done && clean / total >= 0.7;
  const status = done ? (mastered ? 'mastered' : 'done') : (started || lessonRead(skillId, lesson.id)) ? 'progress' : 'new';
  return { total, solved, clean, done, mastered, status, pct: total ? solved / total : 0 };
}

export function skillStats(skill) {
  let total = 0; let solved = 0; let lessons = 0; let lessonsDone = 0;
  for (const m of skill.modules) for (const l of m.lessons) {
    const st = lessonStats(skill.id, l);
    total += st.total; solved += st.solved; lessons++; if (st.done) lessonsDone++;
  }
  return { total, solved, lessons, lessonsDone, pct: total ? solved / total : 0 };
}

export function moduleStats(skillId, mod) {
  let total = 0; let solved = 0;
  for (const l of mod.lessons) { const st = lessonStats(skillId, l); total += st.total; solved += st.solved; }
  return { total, solved, pct: total ? solved / total : 0 };
}

export function nextLesson(skill) {
  for (const m of skill.modules) for (const l of m.lessons) {
    if (!lessonStats(skill.id, l).done) return { module: m, lesson: l };
  }
  return null;
}

export function allExercises(skill) {
  const out = [];
  for (const m of skill.modules) for (const l of m.lessons) for (const ex of l.exercises) out.push({ module: m, lesson: l, ex });
  return out;
}

export function dueReviews(skill, includeEarly = 0) {
  const now = Date.now();
  const solved = allExercises(skill).filter((x) => rec(skill.id, x.ex.id)?.s);
  const due = solved.filter((x) => rec(skill.id, x.ex.id).d <= now)
    .sort((a, b) => rec(skill.id, a.ex.id).d - rec(skill.id, b.ex.id).d);
  if (due.length || !includeEarly) return due;
  return solved.sort((a, b) => rec(skill.id, a.ex.id).d - rec(skill.id, b.ex.id).d).slice(0, includeEarly);
}

export function nextDue(skill) {
  const ds = allExercises(skill).map((x) => rec(skill.id, x.ex.id)).filter((r) => r?.s).map((r) => r.d);
  return ds.length ? Math.min(...ds) : 0;
}

// ---------- XP, уровни, серия ----------
export function totalXP() {
  let xp = 0;
  for (const s of Object.values(state.skills)) for (const r of Object.values(s.ex)) xp += r.xp || 0;
  return xp;
}
export const skillXP = (skillId) => Object.values(state.skills[skillId]?.ex || {}).reduce((a, r) => a + (r.xp || 0), 0);

export const LEVELS = ['Новичок', 'Стажёр', 'Практик', 'Специалист', 'Эксперт', 'Мастер'];
export function levelInfo(xp) {
  const thresholds = [0, 60, 200, 450, 800, 1300];
  let lvl = 0;
  while (lvl + 1 < thresholds.length && xp >= thresholds[lvl + 1]) lvl++;
  const next = thresholds[lvl + 1];
  return { level: lvl + 1, title: LEVELS[lvl], xp, from: thresholds[lvl], next: next ?? null,
    pct: next ? (xp - thresholds[lvl]) / (next - thresholds[lvl]) : 1 };
}

export function streak() {
  const act = state.activity;
  const d = new Date();
  let cur = 0;
  if (!act[localDate(d)]) d.setDate(d.getDate() - 1); // сегодня ещё можно успеть
  while (act[localDate(d)]) { cur++; d.setDate(d.getDate() - 1); }
  return { current: cur, today: !!act[localDate()], days: Object.keys(act).length };
}
export const activityMap = () => state.activity;
