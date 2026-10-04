import { h, plural, toast } from '../util.js';
import { loadSkill } from '../skills/index.js';
import { renderMd } from '../md.js';
import { mountPlayground, buildSchemaPanel } from '../components.js';
import { mountExercise } from '../exercise.js';
import * as store from '../store.js';

const mdCache = new Map();
async function loadMd(skill, file) {
  const key = `${skill.id}/${file}`;
  if (!mdCache.has(key)) {
    mdCache.set(key, fetch(new URL(`../skills/${skill.id}/lessons/${file}`, import.meta.url), { cache: 'no-cache' })
      .then((r) => { if (!r.ok) throw new Error(`Не удалось загрузить урок (${r.status})`); return r.text(); }));
  }
  return mdCache.get(key);
}

function find(skill, lessonId) {
  const flat = [];
  for (const m of skill.modules) for (const l of m.lessons) flat.push({ module: m, lesson: l });
  const i = flat.findIndex((x) => x.lesson.id === lessonId);
  return { cur: flat[i], prev: flat[i - 1], next: flat[i + 1], index: i, flat };
}

export async function lessonView(root, skillId, lessonId, sub, n) {
  const skill = await loadSkill(skillId);
  const { cur, prev, next } = skill ? find(skill, lessonId) : {};
  if (!cur) return root.replaceChildren(h('div', { class: 'empty' }, 'Урок не найден.'));
  const { lesson, module: mod } = cur;
  const base = `#/${skill.id}/lesson/${lesson.id}`;
  const stats = store.lessonStats(skill.id, lesson);

  const crumbs = h('nav', { class: 'crumbs' }, h('a', { href: '#/' }, 'Навыки'), ' › ', h('a', { href: `#/${skill.id}` }, skill.title), ' › ', mod.title);
  const title = h('div', { class: 'lesson-title' },
    h('h1', null, lesson.title, lesson.exam ? h('span', { class: 'badge exam' }, 'экзамен · без подсказок') : null),
    h('p', { class: 'lead' }, lesson.goal));

  const tabs = h('div', { class: 'tabs', role: 'tablist' },
    h('a', { class: `tab ${sub ? '' : 'active'}`, href: base, role: 'tab' }, '📖 Теория'),
    h('a', { class: `tab ${sub ? 'active' : ''}`, href: `${base}/ex/${firstOpen(skill, lesson)}`, role: 'tab' }, `✍️ Практика ${stats.solved}/${stats.total}`));

  const main = h('div', { class: 'lesson-main' }, crumbs, title, tabs);
  const side = h('aside', { class: 'lesson-side' });
  let editorPick = null;
  root.replaceChildren(h('div', { class: 'lesson-layout' }, main, side));
  buildSchemaPanel(skill.runner, null).then((p) => side.replaceChildren(p));

  if (!sub) {
    const box = h('div', { class: 'md' }, h('div', { class: 'out-empty' }, 'Загрузка…'));
    main.append(box);
    try {
      const { html, tries } = renderMd(await loadMd(skill, lesson.body));
      box.innerHTML = html;
      box.querySelectorAll('.try-slot').forEach((slot) => mountPlayground(slot, skill.runner, tries[+slot.dataset.try]));
    } catch (e) { box.replaceChildren(h('div', { class: 'out-error' }, e.message)); }
    const go = firstOpen(skill, lesson);
    main.append(h('div', { class: 'lesson-cta' },
      prev ? h('a', { class: 'btn ghost', href: `#/${skill.id}/lesson/${prev.lesson.id}` }, '← ' + prev.lesson.title) : h('span'),
      h('a', { class: 'btn primary big', href: `${base}/ex/${go}`, onclick: () => store.markLessonRead(skill.id, lesson.id) },
        stats.solved === stats.total ? 'К упражнениям →' : stats.solved ? 'Продолжить практику →' : 'К практике →')));
    return;
  }

  if (sub === 'done') return renderDone(main, skill, cur, next);

  // ---- практика ----
  const idx = Math.min(Math.max(parseInt(n, 10) || 1, 1), lesson.exercises.length) - 1;
  const ex = lesson.exercises[idx];
  const pills = h('div', { class: 'pills' }, lesson.exercises.map((e, i) => {
    const r = store.rec(skill.id, e.id);
    return h('a', { class: `pill ${i === idx ? 'cur' : ''} ${r?.s ? 'ok' : r?.a ? 'tried' : ''}`, href: `${base}/ex/${i + 1}`, title: `Задача ${i + 1}` }, r?.s ? '✓' : i + 1);
  }));
  const slot = h('div', { class: 'ex-slot' });
  main.append(pills, slot);

  const nextBtnHolder = h('div', { class: 'next-row' });
  const openSchema = () => document.dispatchEvent(new CustomEvent('open-schema', { detail: { skill } }));
  mountExercise(slot, {
    skill, ex, mode: 'learn', exam: !!lesson.exam, openSchema,
    onResult: ({ passed }) => { if (passed) showNext(); },
  });
  main.append(nextBtnHolder);

  const solvedNow = () => store.rec(skill.id, ex.id)?.s;
  function showNext() {
    const last = idx === lesson.exercises.length - 1;
    const allDone = store.lessonStats(skill.id, lesson).done;
    const target = allDone && (last || lesson.exercises.slice(idx + 1).every((e) => store.rec(skill.id, e.id)?.s))
      ? `${base}/done`
      : `${base}/ex/${nextOpenAfter(skill, lesson, idx)}`;
    nextBtnHolder.replaceChildren(h('a', { class: 'btn primary big', href: target }, allDone && target.endsWith('/done') ? 'Завершить урок 🎉' : 'Следующая задача →'));
    nextBtnHolder.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    // обновить пилюли и вкладку
    const r = store.rec(skill.id, ex.id);
    pills.children[idx].className = `pill cur ${r?.s ? 'ok' : 'tried'}`; pills.children[idx].textContent = r?.s ? '✓' : idx + 1;
    const st2 = store.lessonStats(skill.id, lesson);
    tabs.children[1].textContent = `✍️ Практика ${st2.solved}/${st2.total}`;
  }
  if (solvedNow()) showNext();
}

function firstOpen(skill, lesson) {
  const i = lesson.exercises.findIndex((e) => !store.rec(skill.id, e.id)?.s);
  return i < 0 ? 1 : i + 1;
}
function nextOpenAfter(skill, lesson, idx) {
  const L = lesson.exercises;
  for (let k = 1; k <= L.length; k++) {
    const j = (idx + k) % L.length;
    if (!store.rec(skill.id, L[j].id)?.s) return j + 1;
  }
  return Math.min(idx + 2, L.length);
}

function renderDone(main, skill, cur, next) {
  const { lesson } = cur;
  const st = store.lessonStats(skill.id, lesson);
  if (!st.done) { location.hash = `#/${skill.id}/lesson/${lesson.id}/ex/${firstOpen(skill, lesson)}`; return; }
  const xp = lesson.exercises.reduce((a, e) => a + (store.rec(skill.id, e.id)?.xp || 0), 0);
  const stars = st.mastered ? (st.clean === st.total ? 3 : 2) : 1;
  main.append(h('section', { class: 'done-card' },
    h('div', { class: 'confetti', 'aria-hidden': 'true' }, Array.from({ length: 24 }, (_, i) => h('i', { style: `--i:${i}` }))),
    h('div', { class: 'stars', 'aria-label': `${stars} из 3` }, [1, 2, 3].map((i) => h('span', { class: i <= stars ? 'on' : '' }, '★'))),
    h('h2', null, st.mastered ? 'Урок освоен!' : 'Урок пройден!'),
    h('p', null, `Решено ${st.solved} из ${st.total} · без подсказок ${st.clean} · заработано ${xp} XP`),
    st.mastered ? null : h('p', { class: 'muted' }, 'Звёзды зависят от того, сколько задач вы решили без подсказок и без просмотра решения. Задачи, с которыми были трудности, вернутся на повторении.'),
    h('p', { class: 'muted' }, '🔁 Решённые задачи появятся в повторении завтра — так знания закрепляются надолго.'),
    h('div', { class: 'cta-row center' },
      next ? h('a', { class: 'btn primary big', href: `#/${skill.id}/lesson/${next.lesson.id}` }, `Дальше: ${next.lesson.title} →`) : h('a', { class: 'btn primary big', href: `#/${skill.id}` }, 'К программе курса'),
      h('a', { class: 'btn big', href: `#/${skill.id}` }, 'К программе'))));
}
