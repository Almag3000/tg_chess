import { h } from '../util.js';
import { loadSkill } from '../skills/index.js';
import { createEditor, renderOutput, buildSchemaPanel } from '../components.js';

const EXAMPLES = [
  ['Топ клиентов', `SELECT c.name, ROUND(SUM(oi.qty * oi.unit_price)) AS total\nFROM customers c\nJOIN orders o ON o.customer_id = c.id\nJOIN order_items oi ON oi.order_id = o.id\nWHERE o.status <> 'cancelled'\nGROUP BY c.id\nORDER BY total DESC\nLIMIT 5;`],
  ['Выручка по месяцам', `SELECT strftime('%Y-%m', o.order_date) AS month,\n       SUM(oi.qty * oi.unit_price) AS revenue\nFROM orders o\nJOIN order_items oi ON oi.order_id = o.id\nGROUP BY month\nORDER BY month;`],
  ['Все таблицы', `SELECT name, sql FROM sqlite_master WHERE type = 'table';`],
  ['План запроса', `EXPLAIN QUERY PLAN\nSELECT * FROM orders WHERE customer_id = 5;`],
];

export async function sandboxView(root, skillId) {
  const skill = await loadSkill(skillId);
  const key = `sf:sandbox:${skillId}`;
  let saved = ''; try { saved = localStorage.getItem(key) || ''; } catch { /* ignore */ }
  const out = h('div', { class: 'ex-out' }, renderOutput(null));
  const editorHost = h('div', { class: 'ex-editor tall' });
  let editor;
  const run = async () => {
    out.replaceChildren(h('div', { class: 'out-empty' }, 'Выполняется…'));
    try { localStorage.setItem(key, editor.value); } catch { /* ignore */ }
    out.replaceChildren(renderOutput(await skill.runner.run(editor.value)));
  };
  const schema = await buildSchemaPanel(skill.runner, (q) => { editor.value = q; run(); });
  const examples = h('div', { class: 'chips' }, EXAMPLES.map(([t, q]) => h('button', { class: 'chip', type: 'button', onclick: () => { editor.value = q; editor.focus(); } }, t)));

  root.replaceChildren(
    h('nav', { class: 'crumbs' }, h('a', { href: '#/' }, 'Навыки'), ' › ', h('a', { href: `#/${skillId}` }, skill.title), ' › Песочница'),
    h('div', { class: 'lesson-layout' },
      h('div', { class: 'lesson-main' },
        h('h1', { class: 'page-title' }, '🧪 Песочница'),
        h('p', { class: 'lead' }, 'Свободные эксперименты на учебной базе. Каждый запуск начинается с чистой копии базы, поэтому ломать здесь можно всё: INSERT, UPDATE, DROP — ничего не сохраняется.'),
        examples, h('div', { class: 'ex-card' }, editorHost,
          h('div', { class: 'toolbar' }, h('button', { class: 'btn primary', type: 'button', onclick: run }, '▶ Запустить'), h('span', { class: 'muted tiny' }, 'Ctrl+Enter'),
            h('span', { class: 'spacer' }), h('button', { class: 'btn ghost schema-open', type: 'button', onclick: () => document.dispatchEvent(new CustomEvent('open-schema', { detail: { skill } })) }, '🗂 Схема')),
          out)),
      h('aside', { class: 'lesson-side' }, schema)));
  editor = createEditor(editorHost, { value: saved || EXAMPLES[0][1], onRun: run });
  requestAnimationFrame(() => editor.refresh());
}
