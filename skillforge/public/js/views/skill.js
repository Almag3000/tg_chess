import { h, plural, relTime } from '../util.js';
import { loadSkill } from '../skills/index.js';
import { ring } from '../components.js';
import * as store from '../store.js';

const STATUS = {
  new: { ico: '○', cls: 'new', t: 'не начат' },
  progress: { ico: '◐', cls: 'progress', t: 'в процессе' },
  done: { ico: '●', cls: 'done', t: 'пройден' },
  mastered: { ico: '★', cls: 'mastered', t: 'освоен' },
};

const METHOD = [
  ['📖', 'Коротко объясняем', 'Теория на 5–10 минут: идея, живой пример, типичные ошибки. Примеры можно запускать и менять.'],
  ['⚡', 'Сразу пробуете', 'Каждое упражнение проверяется автоматически и объясняет, что не так, — не «неверно», а «строк должно быть 7».'],
  ['🪜', 'Усложняем постепенно', 'Разминка → практика → вызов. Подсказки открываются по шагам, за них немного снижаются очки.'],
  ['🔁', 'Повторяем вовремя', 'Решённые задачи возвращаются через 1, 3, 7, 16 и 35 дней — так знания переходят в долгую память.'],
  ['🎓', 'Проверяем без подсказок', 'Финальный экзамен — бизнес-кейсы без подсказок. Уровень освоения видно по звёздам.'],
];

export async function skillView(root, skillId) {
  const skill = await loadSkill(skillId);
  if (!skill) return root.replaceChildren(h('div', { class: 'empty' }, 'Навык не найден.'));
  const stats = store.skillStats(skill);
  const nx = store.nextLesson(skill);
  const due = store.dueReviews(skill).length;
  const nd = store.nextDue(skill);

  const head = h('section', { class: 'skill-head', style: `--accent:${skill.color}` },
    h('div', { class: 'sh-ico' }, skill.icon),
    h('div', { class: 'sh-text' },
      h('h1', null, skill.title),
      h('p', { class: 'lead' }, skill.description),
      h('p', { class: 'muted' }, skill.audience),
      h('div', { class: 'cta-row' },
        nx ? h('a', { class: 'btn primary big', href: `#/${skill.id}/lesson/${nx.lesson.id}` }, stats.solved ? 'Продолжить обучение' : 'Начать с первого урока') : h('span', { class: 'badge solved' }, '🎉 Курс пройден'),
        h('a', { class: `btn big ${due ? 'accent' : ''}`, href: `#/${skill.id}/review` }, due ? `🔁 Повторить (${due})` : '🔁 Повторение'),
        skill.sandbox ? h('a', { class: 'btn big ghost', href: `#/${skill.id}/sandbox` }, '🧪 Песочница') : null),
      !due && nd ? h('p', { class: 'muted tiny' }, `Следующее повторение — ${relTime(nd)}.`) : null),
    h('div', { class: 'sh-ring' }, ring(stats.pct, { size: 96, stroke: 9 }),
      h('div', { class: 'muted tiny' }, `${stats.solved}/${stats.total} задач`),
      h('div', { class: 'muted tiny' }, `${store.skillXP(skill.id)} XP`)));

  const method = h('details', { class: 'card method', open: stats.solved === 0 || undefined },
    h('summary', null, 'Как устроено обучение'),
    h('div', { class: 'method-grid' }, METHOD.map(([i, t, d], n) => h('div', { class: 'm-step' }, h('div', { class: 'm-n' }, `${n + 1}`), h('div', { class: 'm-i' }, i), h('h4', null, t), h('p', null, d)))));

  const mods = skill.modules.map((m, mi) => {
    const ms = store.moduleStats(skill.id, m);
    return h('section', { class: 'card module' },
      h('div', { class: 'mod-head' },
        h('div', { class: 'mod-num' }, mi + 1),
        h('div', { class: 'mod-t' }, h('h3', null, m.title), h('p', null, m.desc)),
        h('div', { class: 'mod-p' }, `${ms.solved}/${ms.total}`, h('div', { class: 'bar' }, h('i', { style: `width:${Math.round(ms.pct * 100)}%` })))),
      h('ul', { class: 'lessons' }, m.lessons.map((l) => {
        const ls = store.lessonStats(skill.id, l);
        const s = STATUS[ls.status];
        return h('li', null, h('a', { class: `lesson-row ${s.cls}`, href: `#/${skill.id}/lesson/${l.id}` },
          h('span', { class: 'ls-ico', title: s.t }, s.ico),
          h('span', { class: 'ls-t' }, l.title, l.exam ? h('span', { class: 'badge exam' }, 'экзамен') : null),
          h('span', { class: 'ls-m' }, `${l.minutes} мин · ${ls.solved}/${ls.total}`)));
      })));
  });

  root.replaceChildren(
    h('nav', { class: 'crumbs' }, h('a', { href: '#/' }, 'Навыки'), ' › ', skill.title),
    head, method, h('h2', { class: 'section-title' }, 'Программа'), h('div', { class: 'modules' }, mods));
}
