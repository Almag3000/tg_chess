import { h, plural } from './util.js';
import * as store from './store.js';
import { loadAll, loadSkill } from './skills/index.js';
import { buildSchemaPanel } from './components.js';
import { homeView } from './views/home.js';
import { skillView } from './views/skill.js';
import { lessonView } from './views/lesson.js';
import { reviewView } from './views/review.js';
import { sandboxView } from './views/sandbox.js';
import { settingsView } from './views/settings.js';

const view = document.getElementById('view');

// ---------- тема ----------
const THEME_KEY = 'sf:theme';
const getTheme = () => { try { return localStorage.getItem(THEME_KEY); } catch { return null; } };
function applyTheme(t) {
  if (t) document.documentElement.dataset.theme = t; else delete document.documentElement.dataset.theme;
}
applyTheme(getTheme());

// ---------- шапка ----------
const header = document.getElementById('topbar');
async function renderHeader() {
  const skills = await loadAll();
  const xp = store.totalXP();
  const lvl = store.levelInfo(xp);
  const sk = store.streak();
  const due = skills.reduce((a, s) => a + store.dueReviews(s).length, 0);
  const sync = store.getSync();
  const syncIcon = !sync.available ? { t: 'Прогресс хранится только в этом браузере', i: '💾' }
    : sync.status === 'error' ? { t: 'Ошибка синхронизации: ' + sync.error, i: '⚠️' }
    : sync.status === 'syncing' ? { t: 'Синхронизация…', i: '⏳' } : { t: 'Прогресс синхронизирован с сервером', i: '☁️' };
  const dueSkill = skills.find((s) => store.dueReviews(s).length);
  header.replaceChildren(h('div', { class: 'topbar-in' },
    h('a', { class: 'logo', href: '#/' }, h('span', { class: 'logo-mark' }, '◆'), h('span', null, 'Skillforge')),
    h('span', { class: 'spacer' }),
    due ? h('a', { class: 'chip due', href: `#/${dueSkill.id}/review`, title: 'Пора повторить' }, `🔁 ${due}`) : null,
    h('span', { class: 'chip', title: sk.today ? 'Серия продолжается' : 'Решите задачу сегодня, чтобы продолжить серию' }, `🔥 ${sk.current}`),
    h('a', { class: 'chip xp', href: '#/settings', title: `Уровень ${lvl.level}: ${lvl.title}` }, `⭐ ${xp}`, h('span', { class: 'chip-sub' }, ` ур.${lvl.level}`)),
    h('span', { class: 'chip sync', title: syncIcon.t }, syncIcon.i),
    h('button', { class: 'icon-btn', type: 'button', title: 'Сменить тему', 'aria-label': 'Сменить тему', onclick: () => {
      const dark = (document.documentElement.dataset.theme || (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')) === 'dark';
      const next = dark ? 'light' : 'dark'; applyTheme(next); try { localStorage.setItem(THEME_KEY, next); } catch { /* ignore */ }
    } }, '🌓'),
    h('a', { class: 'icon-btn', href: '#/settings', title: 'Настройки и синхронизация', 'aria-label': 'Настройки' }, '⚙')));
}

// ---------- панель схемы для узких экранов ----------
document.addEventListener('open-schema', async (e) => {
  const skill = e.detail?.skill;
  if (!skill) return;
  const dlg = h('dialog', { class: 'schema-dialog' },
    h('div', { class: 'dlg-head' }, h('strong', null, 'Схема данных'), h('button', { class: 'icon-btn', type: 'button', 'aria-label': 'Закрыть', onclick: () => dlg.close() }, '✕')),
    await buildSchemaPanel(skill.runner, null));
  dlg.addEventListener('close', () => dlg.remove());
  dlg.addEventListener('click', (ev) => { if (ev.target === dlg) dlg.close(); });
  document.body.append(dlg);
  dlg.showModal();
});

// ---------- роутер ----------
const routes = [
  [/^$/, () => homeView(view)],
  [/^settings$/, () => settingsView(view)],
  [/^([\w-]+)$/, (m) => skillView(view, m[1])],
  [/^([\w-]+)\/lesson\/([\w-]+)$/, (m) => lessonView(view, m[1], m[2])],
  [/^([\w-]+)\/lesson\/([\w-]+)\/(ex)\/(\d+)$/, (m) => lessonView(view, m[1], m[2], 'ex', m[4])],
  [/^([\w-]+)\/lesson\/([\w-]+)\/done$/, (m) => lessonView(view, m[1], m[2], 'done')],
  [/^([\w-]+)\/review$/, (m) => reviewView(view, m[1], false)],
  [/^([\w-]+)\/review\/early$/, (m) => reviewView(view, m[1], true)],
  [/^([\w-]+)\/sandbox$/, (m) => sandboxView(view, m[1])],
];

let navToken = 0;
async function route() {
  const path = location.hash.replace(/^#\/?/, '').replace(/\/+$/, '');
  const token = ++navToken;
  for (const [re, fn] of routes) {
    const m = path.match(re);
    if (!m) continue;
    view.classList.add('loading');
    try {
      await fn(m);
    } catch (err) {
      console.error(err);
      if (token === navToken) view.replaceChildren(h('div', { class: 'card empty-state' }, h('h2', null, 'Что-то пошло не так'), h('p', null, String(err.message || err)), h('a', { class: 'btn', href: '#/' }, 'На главную')));
    }
    if (token === navToken) { view.classList.remove('loading'); window.scrollTo(0, 0); view.focus({ preventScroll: true }); }
    renderHeader();
    return;
  }
  view.replaceChildren(h('div', { class: 'card empty-state' }, h('h2', null, 'Страница не найдена'), h('a', { class: 'btn', href: '#/' }, 'На главную')));
}

window.addEventListener('hashchange', route);
store.subscribe(() => renderHeader());
renderHeader();
route();
store.initSync();
