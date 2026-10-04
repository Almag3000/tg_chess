// Мини-рендерер Markdown для уроков. Поддерживает: заголовки, абзацы, списки, таблицы,
// цитаты-врезки, блоки кода (```sql, ```sql try — интерактивный пример) и инлайн-разметку.
import { esc } from './util.js';

const KEYWORDS = new Set(('select from where group by having order limit offset distinct as and or not in is null like between exists case when then else end ' +
  'join inner left right full outer cross on using union all intersect except insert into values update set delete create table drop alter add column ' +
  'primary key foreign references default check unique index view with recursive over partition rows range unbounded preceding following current row ' +
  'asc desc cast autoincrement integer text real blob numeric if explain query plan pragma begin commit rollback transaction window').split(' '));
const FUNCS = new Set(('count sum avg min max round abs length upper lower substr instr replace trim coalesce ifnull nullif date time datetime strftime julianday ' +
  'row_number rank dense_rank lag lead ntile first_value last_value total group_concat typeof random').split(' '));

export function highlightSQL(code) {
  const re = /(--[^\n]*|\/\*[\s\S]*?\*\/)|('(?:[^']|'')*')|(\b\d+(?:\.\d+)?\b)|([A-Za-zА-Яа-яЁё_][\wА-Яа-яЁё]*)|(\s+|.)/gu;
  let out = '';
  for (const m of code.matchAll(re)) {
    const [t, com, str, num, word] = m;
    if (com) out += `<span class="tk-c">${esc(t)}</span>`;
    else if (str) out += `<span class="tk-s">${esc(t)}</span>`;
    else if (num) out += `<span class="tk-n">${esc(t)}</span>`;
    else if (word) {
      const l = word.toLowerCase();
      if (KEYWORDS.has(l)) out += `<span class="tk-k">${esc(t)}</span>`;
      else if (FUNCS.has(l)) out += `<span class="tk-f">${esc(t)}</span>`;
      else out += esc(t);
    } else out += esc(t);
  }
  return out;
}

function inline(s) {
  const codes = [];
  s = s.replace(/`([^`]+)`/g, (_, c) => { codes.push(c); return `\u0000${codes.length - 1}\u0000`; });
  s = esc(s)
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[\s(«"])\*([^*\s][^*]*?)\*(?=[\s).,;:!?»"]|$)/g, '$1<em>$2</em>');
  return s.replace(/\u0000(\d+)\u0000/g, (_, i) => `<code>${esc(codes[+i])}</code>`);
}

const isTableSep = (l) => /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?\s*$/.test(l);
const cells = (l) => l.trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim());

/** Возвращает { html, tries } — html содержит заглушки <div data-try="i"> для интерактивных блоков. */
export function renderMd(src) {
  const lines = src.replace(/\r/g, '').split('\n');
  const tries = [];
  let html = '';
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    let m;
    if ((m = line.match(/^```(\w*)\s*(.*)$/))) {
      const lang = m[1]; const flags = m[2].split(/\s+/);
      const buf = [];
      i++;
      while (i < lines.length && !lines[i].startsWith('```')) buf.push(lines[i++]);
      i++;
      const code = buf.join('\n');
      if (lang === 'sql' && flags.includes('try')) {
        tries.push({ code, expectError: flags.includes('error') });
        html += `<div class="try-slot" data-try="${tries.length - 1}"></div>`;
      } else {
        html += `<pre class="code"><code>${lang === 'sql' ? highlightSQL(code) : esc(code)}</code></pre>`;
      }
      continue;
    }
    if ((m = line.match(/^(#{2,3})\s+(.*)$/))) { html += `<h${m[1].length}>${inline(m[2])}</h${m[1].length}>`; i++; continue; }
    if (line.startsWith('>')) {
      const buf = [];
      while (i < lines.length && lines[i].startsWith('>')) buf.push(lines[i++].replace(/^>\s?/, ''));
      html += `<aside class="callout">${inline(buf.join(' '))}</aside>`;
      continue;
    }
    if (line.includes('|') && i + 1 < lines.length && isTableSep(lines[i + 1])) {
      const head = cells(line); i += 2;
      const rows = [];
      while (i < lines.length && lines[i].includes('|') && lines[i].trim()) rows.push(cells(lines[i++]));
      html += '<div class="table-wrap"><table><thead><tr>' + head.map((c) => `<th>${inline(c)}</th>`).join('') + '</tr></thead><tbody>' +
        rows.map((r) => '<tr>' + r.map((c) => `<td>${inline(c)}</td>`).join('') + '</tr>').join('') + '</tbody></table></div>';
      continue;
    }
    if (/^\s*([-*]|\d+\.)\s+/.test(line)) {
      const ordered = /^\s*\d+\./.test(line);
      const items = [];
      while (i < lines.length && /^\s*([-*]|\d+\.)\s+/.test(lines[i])) {
        let t = lines[i++].replace(/^\s*([-*]|\d+\.)\s+/, '');
        while (i < lines.length && /^\s{2,}\S/.test(lines[i]) && !/^\s*([-*]|\d+\.)\s+/.test(lines[i])) t += ' ' + lines[i++].trim();
        items.push(t);
      }
      const tag = ordered ? 'ol' : 'ul';
      html += `<${tag}>${items.map((t) => `<li>${inline(t)}</li>`).join('')}</${tag}>`;
      continue;
    }
    if (!line.trim()) { i++; continue; }
    const buf = [];
    while (i < lines.length && lines[i].trim() && !/^(```|#{2,3}\s|>|\s*([-*]|\d+\.)\s+)/.test(lines[i]) &&
      !(lines[i].includes('|') && lines[i + 1] && isTableSep(lines[i + 1]))) buf.push(lines[i++]);
    html += `<p>${inline(buf.join(' '))}</p>`;
  }
  return { html, tries };
}

/** Рендер короткого текста (условия задач, подсказки): абзацы, код, списки, таблицы. */
export function renderInline(src) { return renderMd(src).html; }
