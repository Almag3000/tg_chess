import { h, plural, relTime } from '../util.js';
import { loadSkill } from '../skills/index.js';
import { mountExercise } from '../exercise.js';
import * as store from '../store.js';

export async function reviewView(root, skillId, early) {
  const skill = await loadSkill(skillId);
  const queue = store.dueReviews(skill, early ? 5 : 0);
  const crumbs = h('nav', { class: 'crumbs' }, h('a', { href: '#/' }, 'Навыки'), ' › ', h('a', { href: `#/${skill.id}` }, skill.title), ' › Повторение');

  if (!queue.length) {
    const nd = store.nextDue(skill);
    root.replaceChildren(crumbs, h('div', { class: 'card empty-state' },
      h('div', { class: 'big-emoji' }, nd ? '🌱' : '📚'),
      h('h2', null, nd ? 'Сейчас повторять нечего' : 'Здесь пока пусто'),
      h('p', null, nd ? `Следующее повторение — ${relTime(nd)}. Знания закрепляются, когда вы вспоминаете их в нужный момент, а не заранее.`
        : 'Повторение появится, когда вы решите первые задачи.'),
      h('div', { class: 'cta-row center' },
        h('a', { class: 'btn primary', href: `#/${skill.id}` }, 'К программе'),
        nd ? h('a', { class: 'btn', href: `#/${skill.id}/review/early` }, 'Повторить досрочно (5 задач)') : null)));
    return;
  }

  let i = 0; let ok = 0; let xp = 0;
  const bar = h('div', { class: 'bar' }, h('i'));
  const counter = h('div', { class: 'rv-count' });
  const slot = h('div', { class: 'ex-slot' });
  const nextRow = h('div', { class: 'next-row' });
  const openSchema = () => document.dispatchEvent(new CustomEvent('open-schema', { detail: { skill } }));

  const show = () => {
    nextRow.replaceChildren();
    counter.textContent = `Задача ${i + 1} из ${queue.length}`;
    bar.firstChild.style.width = `${(i / queue.length) * 100}%`;
    const { ex, lesson } = queue[i];
    const label = h('div', { class: 'rv-from' }, `Из урока: ${lesson.title}`);
    const holder = h('div');
    slot.replaceChildren(label, holder);
    mountExercise(holder, {
      skill, ex, mode: 'review', openSchema,
      onResult: ({ passed, firstTry }) => {
        if (passed) { ok++; if (firstTry) xp += 4; }
        nextRow.replaceChildren(h('button', { class: 'btn primary big', type: 'button', onclick: () => { i++; i < queue.length ? show() : finish(); } },
          i + 1 < queue.length ? 'Дальше →' : 'Завершить'));
        nextRow.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      },
    });
  };

  const finish = () => {
    const pct = Math.round((ok / queue.length) * 100);
    root.replaceChildren(crumbs, h('div', { class: 'card empty-state' },
      h('div', { class: 'big-emoji' }, pct >= 80 ? '🏆' : pct >= 50 ? '👍' : '💪'),
      h('h2', null, 'Повторение завершено'),
      h('p', null, `Вспомнили ${ok} из ${queue.length} · +${xp} XP`),
      h('p', { class: 'muted' }, 'Верно решённые задачи вернутся через больший срок, а забытые — уже завтра.'),
      h('div', { class: 'cta-row center' }, h('a', { class: 'btn primary', href: `#/${skill.id}` }, 'К программе'), h('a', { class: 'btn', href: '#/' }, 'На главную'))));
  };

  root.replaceChildren(crumbs,
    h('div', { class: 'review-wrap' },
      h('h1', { class: 'page-title' }, '🔁 Повторение'),
      h('p', { class: 'lead' }, 'Без подсказок: вспомните и решите. Это самый эффективный способ закрепить материал.'),
      h('div', { class: 'rv-bar' }, counter, bar), slot, nextRow));
  show();
}
