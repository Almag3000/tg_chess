import { h, esc } from './util.js';

export function createEditor(parent, { value = '', mode = 'text/x-sqlite', onRun, placeholder = '', readOnly = false, onChange } = {}) {
  const cm = window.CodeMirror(parent, {
    value, mode, lineNumbers: false, lineWrapping: true, viewportMargin: Infinity,
    indentUnit: 2, tabSize: 2, indentWithTabs: false, autoCloseBrackets: true, matchBrackets: true,
    readOnly, placeholder, theme: 'sf',
    extraKeys: {
      'Ctrl-Enter': () => onRun && onRun(), 'Cmd-Enter': () => onRun && onRun(),
      Tab: (c) => c.replaceSelection('  ', 'end'),
    },
  });
  if (onChange) cm.on('change', () => onChange(cm.getValue()));
  return {
    cm,
    get value() { return cm.getValue(); },
    set value(v) { cm.setValue(v); },
    focus: () => cm.focus(),
    refresh: () => cm.refresh(),
  };
}

const MAX_ROWS = 100;

export function renderOutput(out) {
  if (!out) return h('div', { class: 'out-empty' }, 'Здесь появится результат запроса');
  if (out.kind === 'error') {
    return h('div', { class: 'out-error', role: 'alert' },
      h('strong', null, 'Ошибка. '), out.message,
      out.raw && out.raw !== out.message ? h('div', { class: 'raw' }, out.raw) : null);
  }
  if (out.kind === 'ok') return h('div', { class: 'out-ok' }, out.message);
  const rows = out.rows.slice(0, MAX_ROWS);
  const table = h('table', { class: 'result' },
    h('thead', null, h('tr', null, out.columns.map((c) => h('th', null, c)))),
    h('tbody', null, rows.map((r) => h('tr', null, r.map((v) => {
      if (v === null) return h('td', { class: 'null' }, 'NULL');
      const num = typeof v === 'number';
      return h('td', { class: num ? 'num' : '' }, num && !Number.isInteger(v) ? String(Math.round(v * 1e6) / 1e6) : String(v));
    })))));
  const meta = out.rows.length === 0
    ? h('div', { class: 'out-meta' }, 'Запрос вернул 0 строк')
    : h('div', { class: 'out-meta' }, `${out.rows.length} ${out.rows.length === 1 ? 'строка' : 'строк'}` +
      (out.rows.length > MAX_ROWS ? ` · показаны первые ${MAX_ROWS}` : '') + (out.truncated ? ' · результат обрезан' : ''));
  return h('div', { class: 'out-table' }, h('div', { class: 'table-scroll' }, table), meta);
}

/** Интерактивный пример в теории. */
export function mountPlayground(slot, runner, { code, expectError }) {
  const out = h('div', { class: 'pg-out' });
  let editor;
  const run = async () => {
    btn.disabled = true;
    out.replaceChildren(h('div', { class: 'out-empty' }, 'Выполняется…'));
    try { out.replaceChildren(renderOutput(await runner.run(editor.value))); } finally { btn.disabled = false; }
  };
  const btn = h('button', { class: 'btn small primary', type: 'button', onclick: run }, '▶ Запустить');
  const reset = h('button', { class: 'btn small ghost', type: 'button', title: 'Вернуть исходный код', onclick: () => { editor.value = code; } }, '↺');
  const box = h('div', { class: 'pg' },
    h('div', { class: 'pg-bar' }, h('span', { class: 'pg-label' }, expectError ? 'Пример (здесь ошибка — это нормально)' : 'Попробуйте сами — код можно менять'), h('span', { class: 'spacer' }), reset, btn),
    h('div', { class: 'pg-editor' }), out);
  slot.replaceChildren(box);
  editor = createEditor(box.querySelector('.pg-editor'), { value: code.replace(/\n$/, ''), onRun: run });
  requestAnimationFrame(() => editor.refresh());
}

/** Панель со схемой данных. */
export async function buildSchemaPanel(runner, onPick) {
  const root = h('div', { class: 'schema' }, h('div', { class: 'out-empty' }, 'Загрузка схемы…'));
  try {
    const tables = await runner.schema();
    root.replaceChildren(
      h('div', { class: 'schema-title' }, 'Схема данных', h('span', null, 'клик по таблице — пример запроса')),
      ...tables.map((t) => h('details', { class: 'tbl', open: t.name === 'orders' || undefined },
        h('summary', null,
          h('span', { class: 'tname' }, t.name),
          h('span', { class: 'rows' }, `${t.rows} стр.`)),
        h('ul', null, t.columns.map((c) => h('li', null,
          h('span', { class: 'cname' }, c.name),
          h('span', { class: 'ctype' }, (c.type || '').toLowerCase()),
          c.pk ? h('span', { class: 'tag pk' }, 'PK') : null,
          c.notnull && !c.pk ? h('span', { class: 'tag nn' }, 'NOT NULL') : null))),
        onPick ? h('button', { class: 'btn small ghost peek', type: 'button', onclick: () => onPick(runner.sampleQuery(t.name)) }, `SELECT * FROM ${t.name}`) : null)),
      h('div', { class: 'schema-links' },
        h('div', { class: 'rel' }, 'Связи: ', h('code', null, 'products.category_id → categories.id'), ' ',
          h('code', null, 'orders.customer_id → customers.id'), ' ',
          h('code', null, 'order_items.order_id → orders.id'), ' ',
          h('code', null, 'order_items.product_id → products.id'), ' ',
          h('code', null, 'customers.referred_by → customers.id'), ' ',
          h('code', null, 'employees.manager_id → employees.id'))));
  } catch (e) {
    root.replaceChildren(h('div', { class: 'out-error' }, 'Не удалось загрузить схему: ' + esc(e.message)));
  }
  return root;
}

export function ring(pct, { size = 56, stroke = 6, label } = {}) {
  const r = (size - stroke) / 2; const c = 2 * Math.PI * r;
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', `0 0 ${size} ${size}`); svg.setAttribute('width', size); svg.setAttribute('height', size);
  svg.setAttribute('class', 'ring'); svg.setAttribute('role', 'img'); svg.setAttribute('aria-label', `${Math.round(pct * 100)}%`);
  svg.innerHTML = `<circle cx="${size / 2}" cy="${size / 2}" r="${r}" class="ring-bg" stroke-width="${stroke}" fill="none"/>
    <circle cx="${size / 2}" cy="${size / 2}" r="${r}" class="ring-fg" stroke-width="${stroke}" fill="none" stroke-linecap="round"
      stroke-dasharray="${c}" stroke-dashoffset="${c * (1 - Math.min(1, pct))}" transform="rotate(-90 ${size / 2} ${size / 2})"/>
    <text x="50%" y="50%" text-anchor="middle" dominant-baseline="central" class="ring-t">${label ?? Math.round(pct * 100) + '%'}</text>`;
  return svg;
}
