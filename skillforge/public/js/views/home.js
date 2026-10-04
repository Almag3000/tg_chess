import { h, plural, localDate } from '../util.js';
import { loadAll } from '../skills/index.js';
import { ring } from '../components.js';
import * as store from '../store.js';

function activityGrid() {
  const act = store.activityMap();
  const weeks = 18;
  const today = new Date();
  const start = new Date(today);
  start.setDate(start.getDate() - ((today.getDay() + 6) % 7) - (weeks - 1) * 7); // понедельник
  const cols = [];
  for (let w = 0; w < weeks; w++) {
    const col = h('div', { class: 'ag-col' });
    for (let d = 0; d < 7; d++) {
      const date = new Date(start); date.setDate(start.getDate() + w * 7 + d);
      const key = localDate(date);
      const n = date > today ? -1 : (act[key] || 0);
      col.append(h('i', { class: `ag-cell l${n < 0 ? 'x' : n === 0 ? 0 : n < 3 ? 1 : n < 6 ? 2 : 3}`, title: n < 0 ? '' : `${key}: ${n}` }));
    }
    cols.push(col);
  }
  return h('div', { class: 'ag', role: 'img', 'aria-label': 'Активность за последние недели' }, cols);
}

export async function homeView(root) {
  const skills = await loadAll();
  const xp = store.totalXP();
  const lvl = store.levelInfo(xp);
  const sk = store.streak();
  const name = store.getName();

  let cont = null;
  for (const s of skills) {
    const nx = store.nextLesson(s);
    const stats = store.skillStats(s);
    if (nx && (stats.solved > 0 || !cont)) cont = { skill: s, ...nx, started: stats.solved > 0 };
    if (cont?.started) break;
  }
  const dueTotal = skills.reduce((a, s) => a + store.dueReviews(s).length, 0);
  const dueSkill = skills.find((s) => store.dueReviews(s).length);

  const hero = h('section', { class: 'hero' },
    h('div', { class: 'hero-text' },
      h('h1', null, name ? `Привет, ${name}!` : 'Учись делом'),
      h('p', null, cont?.started
        ? 'Продолжайте с того места, где остановились — пара задач в день работает лучше марафона раз в месяц.'
        : 'Короткая теория, сразу практика, автоматическая проверка и повторение в нужный момент. Выберите навык и начните.')),
    h('div', { class: 'stats' },
      h('div', { class: 'stat' }, h('div', { class: 'stat-v' }, `${sk.current}`, h('span', null, ' 🔥')), h('div', { class: 'stat-l' }, plural(sk.current, 'день подряд', 'дня подряд', 'дней подряд'))),
      h('div', { class: 'stat' }, h('div', { class: 'stat-v' }, `${xp}`, h('span', null, ' XP')), h('div', { class: 'stat-l' }, `Ур. ${lvl.level} · ${lvl.title}`),
        h('div', { class: 'bar' }, h('i', { style: `width:${Math.round(lvl.pct * 100)}%` })),
        lvl.next ? h('div', { class: 'stat-sub' }, `до следующего уровня ${lvl.next - xp} XP`) : null)));

  const actions = h('div', { class: 'actions-row' });
  if (cont) {
    actions.append(h('a', { class: 'card action primary-card', href: `#/${cont.skill.id}/lesson/${cont.lesson.id}` },
      h('div', { class: 'a-ico' }, cont.skill.icon),
      h('div', null, h('div', { class: 'a-k' }, cont.started ? 'Продолжить' : 'Начать'),
        h('div', { class: 'a-t' }, cont.lesson.title), h('div', { class: 'a-s' }, `${cont.skill.title} · ${cont.module.title}`)),
      h('div', { class: 'a-go' }, '→')));
  }
  if (dueTotal) {
    actions.append(h('a', { class: 'card action', href: `#/${dueSkill.id}/review` },
      h('div', { class: 'a-ico' }, '🔁'),
      h('div', null, h('div', { class: 'a-k' }, 'Пора повторить'),
        h('div', { class: 'a-t' }, `${dueTotal} ${plural(dueTotal, 'задача', 'задачи', 'задач')}`),
        h('div', { class: 'a-s' }, 'Повторение в срок закрепляет знания надолго')),
      h('div', { class: 'a-go' }, '→')));
  }

  const grid = h('div', { class: 'skills-grid' },
    skills.map((s) => {
      const st = store.skillStats(s);
      return h('a', { class: 'card skill-card', href: `#/${s.id}`, style: `--accent:${s.color}` },
        h('div', { class: 'sc-top' }, h('div', { class: 'sc-ico' }, s.icon), ring(st.pct, { size: 54 })),
        h('h3', null, s.title),
        h('p', null, s.tagline),
        h('div', { class: 'sc-meta' }, `${st.lessons} ${plural(st.lessons, 'урок', 'урока', 'уроков')} · ${st.total} ${plural(st.total, 'задача', 'задачи', 'задач')}`),
        h('div', { class: 'sc-prog' }, `Решено ${st.solved} из ${st.total}`));
    }),
    h('div', { class: 'card skill-card soon' },
      h('div', { class: 'sc-top' }, h('div', { class: 'sc-ico' }, '✨')),
      h('h3', null, 'Новые навыки'),
      h('p', null, 'Скоро здесь появятся другие навыки — с той же методикой и общим прогрессом.')));

  root.replaceChildren(hero, actions,
    h('h2', { class: 'section-title' }, 'Навыки'), grid,
    h('h2', { class: 'section-title' }, 'Ваша активность'),
    h('div', { class: 'card activity' }, activityGrid(),
      h('div', { class: 'ag-legend' }, `Дней с занятиями: ${sk.days}`, h('span', { class: 'spacer' }), 'меньше', h('i', { class: 'ag-cell l0' }), h('i', { class: 'ag-cell l1' }), h('i', { class: 'ag-cell l2' }), h('i', { class: 'ag-cell l3' }), 'больше')));
}
