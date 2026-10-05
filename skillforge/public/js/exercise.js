// Карточка упражнения: общая для уроков и повторений.
import { h, toast } from './util.js';
import { renderMd, highlightSQL } from './md.js';
import { createEditor, renderOutput } from './components.js';
import * as store from './store.js';

const LEVELS = ['', 'Разминка', 'Практика', 'Вызов'];
const levelDots = (n) => h('span', { class: `lvl lvl${n}`, title: LEVELS[n] || '' },
  [1, 2, 3].map((i) => h('i', { class: i <= n ? 'on' : '' })), h('span', { class: 'lvl-t' }, LEVELS[n] || ''));

function shuffled(n) {
  const a = [...Array(n).keys()];
  for (let i = n - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; }
  return a;
}

function mdBlock(src) {
  const { html } = renderMd(src);
  return h('div', { class: 'md small', html });
}

/**
 * ctx: { skill, ex, mode: 'learn'|'review', exam, onResult(result), nextButton: Element|null }
 * Возвращает { destroy }.
 */
export function mountExercise(container, ctx) {
  const { skill, ex, mode = 'learn', exam = false } = ctx;
  const review = mode === 'review';
  const existing = store.rec(skill.id, ex.id);
  const solvedBefore = !!existing?.s;
  const st = { hints: 0, fails: 0, solutionSeen: false, solved: review ? false : solvedBefore, done: false };
  const hintsTotal = (ex.hints || []).length;
  const hintsAllowed = !review && !exam && hintsTotal > 0;

  const head = h('div', { class: 'ex-head' },
    levelDots(ex.level || 1),
    h('span', { class: 'ex-type' }, ex.type === 'quiz' ? 'Вопрос' : ex.starter ? 'Исправьте запрос' : 'Напишите запрос'),
    review ? h('span', { class: 'badge review' }, 'Повторение') : null,
    !review && solvedBefore ? h('span', { class: 'badge solved' }, '✓ решено') : null);
  const prompt = h('div', { class: 'ex-prompt' }, mdBlock(ex.prompt));
  const feedback = h('div', { class: 'feedback', 'aria-live': 'polite' });
  const body = h('div', { class: 'ex-body' });
  const tablesBox = h('div', { class: 'ex-tables', hidden: true });
  container.replaceChildren(h('article', { class: 'ex-card' }, head, prompt, tablesBox, body, feedback));
  if (ex.type !== 'quiz' && skill.runner?.tablesFor) {
    skill.runner.tablesFor(ex).then((ts) => {
      if (!ts.length) return;
      tablesBox.hidden = false;
      tablesBox.replaceChildren(h('div', { class: 'ex-tables-t' }, ts.length > 1 ? 'Данные берём из таблиц' : 'Данные берём из таблицы'),
        ...ts.map((t) => h('div', { class: 'tb' },
          h('span', { class: 'tb-n' }, t.name),
          h('span', { class: 'tb-c' }, t.columns.map((c) => c.name).join(', ')))));
    }).catch(() => {});
  }

  const showFeedback = (kind, text, extra) => {
    feedback.className = `feedback ${kind}`;
    feedback.replaceChildren(...[h('div', { class: 'fb-text' }, text), extra].filter(Boolean));
  };

  const finish = (passed, firstTry) => {
    st.done = true;
    ctx.onResult && ctx.onResult({ passed, firstTry, ex });
  };

  const explainBlock = () => (ex.explain ? h('div', { class: 'explain' }, h('strong', null, 'Разбор. '), mdBlock(ex.explain)) : null);

  // ---------------- quiz ----------------
  if (ex.type === 'quiz') {
    const order = shuffled(ex.options.length);
    const list = h('div', { class: 'options', role: 'radiogroup' });
    const buttons = order.map((idx) => {
      const b = h('button', { type: 'button', class: 'option', role: 'radio', 'aria-checked': 'false' },
        h('span', { class: 'opt-mark' }), h('span', { class: 'opt-text', html: renderMd(ex.options[idx]).html.replace(/^<p>|<\/p>$/g, '') }));
      b.addEventListener('click', () => choose(idx, b));
      list.append(b);
      return b;
    });
    const revealCorrect = () => {
      order.forEach((idx, i) => { if (idx === ex.answer) buttons[i].classList.add('correct'); });
      buttons.forEach((b) => { b.disabled = true; });
    };
    const choose = (idx, btn) => {
      if (st.done) return;
      if (idx === ex.answer) {
        const first = !st.solved && !review;
        const res = review ? { xp: store.recordReview(skill.id, ex, st.fails === 0).xp } : store.recordAttempt(skill.id, ex, { passed: true, hintsUsed: 0, solutionSeen: false });
        btn.classList.add('correct'); revealCorrect();
        showFeedback('ok', first && res.xp ? `Верно! +${res.xp} XP` : 'Верно!', explainBlock());
        st.solved = true; finish(true, st.fails === 0);
        if (first && res.xp) toast(`+${res.xp} XP`, 'xp');
      } else {
        st.fails++;
        btn.classList.add('wrong'); btn.disabled = true;
        if (!review) store.recordAttempt(skill.id, ex, { passed: false });
        if (review && st.fails >= 1) {
          store.recordReview(skill.id, ex, false);
          revealCorrect();
          showFeedback('bad', 'Неверно. Правильный ответ подсвечен.', explainBlock());
          finish(false, false);
        } else if (st.fails >= buttons.length - 1) {
          revealCorrect(); showFeedback('bad', 'Правильный ответ подсвечен.', explainBlock()); finish(true, false);
        } else showFeedback('bad', 'Не совсем. Прочитайте вопрос ещё раз и попробуйте другой вариант.');
      }
    };
    body.append(list);
    if (solvedBefore && !review) { revealCorrect(); showFeedback('ok', 'Вы уже отвечали на этот вопрос.', explainBlock()); st.done = true; }
    return { destroy() {} };
  }

  // ---------------- code ----------------
  const runner = skill.runner;
  const startCode = review ? (ex.starter || '') : (existing?.c || ex.starter || '');
  const out = h('div', { class: 'ex-out' }, renderOutput(null));
  const hintBox = h('div', { class: 'hints' });
  const solBox = h('div', { class: 'solution-box' });
  let editor;

  const setOut = (o) => out.replaceChildren(renderOutput(o));
  const busy = (b) => { runBtn.disabled = b; checkBtn.disabled = b; };

  const run = async () => {
    busy(true);
    out.replaceChildren(h('div', { class: 'out-empty' }, 'Выполняется…'));
    try { setOut(await runner.run(editor.value)); } catch (e) { setOut({ kind: 'error', message: String(e.message || e) }); }
    busy(false);
  };

  const check = async () => {
    if (st.done && review) return;
    busy(true);
    try {
      const code = editor.value;
      const res = await runner.grade(code, ex);
      setOut(res.output);
      if (res.passed) {
        const firstTry = st.fails === 0 && !st.solutionSeen;
        let xpText = '';
        if (review) {
          const r = store.recordReview(skill.id, ex, firstTry);
          xpText = firstTry && r.xp ? ` +${r.xp} XP` : '';
        } else {
          const r = store.recordAttempt(skill.id, ex, { passed: true, hintsUsed: st.hints, solutionSeen: st.solutionSeen, code });
          if (r.first) { xpText = ` +${r.xp} XP`; toast(`+${r.xp} XP`, 'xp'); }
        }
        showFeedback('ok', `Верно!${xpText}`, explainBlock());
        st.solved = true;
        solBtn.hidden = true;
        finish(true, firstTry);
      } else {
        st.fails++;
        if (!review) store.recordAttempt(skill.id, ex, { passed: false, code });
        showFeedback('bad', res.message);
        updateSolutionLock();
        if (review && st.fails >= 2) { showSolution(true); }
      }
    } catch (e) {
      showFeedback('bad', 'Что-то пошло не так: ' + (e.message || e));
    }
    busy(false);
  };

  const showHint = () => {
    if (st.hints >= hintsTotal) return;
    const text = ex.hints[st.hints];
    st.hints++;
    hintBox.append(h('div', { class: 'hint' }, h('span', { class: 'hint-n' }, `Подсказка ${st.hints}`), mdBlock(text)));
    hintBtn.textContent = st.hints >= hintsTotal ? '💡 Подсказки закончились' : `💡 Подсказка (${st.hints}/${hintsTotal})`;
    if (st.hints >= hintsTotal) hintBtn.disabled = true;
    updateSolutionLock();
  };

  const solutionUnlocked = () => st.solved || (hintsAllowed ? st.hints >= hintsTotal || st.fails >= 3 : st.fails >= (review ? 1 : 3));
  function updateSolutionLock() {
    const open = solutionUnlocked();
    solBtn.disabled = !open;
    solBtn.title = open ? 'Посмотреть эталонное решение' : hintsAllowed ? 'Откроется после всех подсказок или трёх неудачных проверок' : 'Откроется после трёх неудачных проверок';
  }

  function showSolution(auto = false) {
    if (!st.solutionSeen) {
      st.solutionSeen = true;
      if (review) { if (!st.done) { store.recordReview(skill.id, ex, false); } }
      else store.markSolutionSeen(skill.id, ex);
    }
    solBox.replaceChildren(h('div', { class: 'sol' },
      h('div', { class: 'sol-title' }, 'Эталонное решение', h('span', null, review ? 'повторение засчитано как «не вспомнил»' : 'за задачу будет меньше XP')),
      h('pre', { class: 'code' }, h('code', { html: highlightSQL(ex.solution.replace(/;\s+(?=[A-Z])/g, ';\n')) })),
      explainBlock()));
    solBtn.hidden = true;
    if (review && !st.done) { st.done = true; showFeedback('bad', auto ? 'Две неудачные попытки — вот решение.' : 'Ничего страшного — разберите решение.'); finish(false, false); }
  }

  const runBtn = h('button', { class: 'btn', type: 'button', onclick: run, title: 'Выполнить без проверки (Ctrl+Enter)' }, '▶ Запустить');
  const checkBtn = h('button', { class: 'btn primary', type: 'button', onclick: check }, '✔ Проверить');
  const hintBtn = h('button', { class: 'btn ghost', type: 'button', onclick: showHint, hidden: !hintsAllowed }, `💡 Подсказка (0/${hintsTotal})`);
  const solBtn = h('button', { class: 'btn ghost', type: 'button', disabled: true, onclick: () => showSolution(false) }, review ? '🤷 Не помню — показать' : '👁 Решение');
  const resetBtn = h('button', { class: 'btn ghost', type: 'button', title: 'Вернуть начальный код', onclick: () => { editor.value = ex.starter || ''; editor.focus(); } }, '↺');
  const schemaBtn = h('button', { class: 'btn ghost schema-open', type: 'button', onclick: () => ctx.openSchema && ctx.openSchema() }, '🗂 Схема');

  const editorHost = h('div', { class: 'ex-editor' });
  body.append(editorHost,
    h('div', { class: 'toolbar' }, runBtn, checkBtn, hintBtn, solBtn, h('span', { class: 'spacer' }), schemaBtn, resetBtn),
    hintBox, out, solBox);
  editor = createEditor(editorHost, { value: startCode, placeholder: runner.placeholder, onRun: run });
  if (review) { solBtn.disabled = false; solBtn.title = ''; }
  updateSolutionLock();
  if (review) solBtn.disabled = false;
  requestAnimationFrame(() => { editor.refresh(); if (!solvedBefore || review) editor.focus(); });
  if (solvedBefore && !review) { showFeedback('ok', 'Вы уже решили эту задачу. Можно потренироваться ещё.', explainBlock()); solBtn.hidden = true; }
  return { destroy() {} };
}
