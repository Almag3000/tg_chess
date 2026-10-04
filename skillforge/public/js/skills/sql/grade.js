// Проверка решений. Не зависит от браузера: executor передаётся снаружи
// (в браузере — воркер, в тесте контента — sql.js под Node).
//
// exec({sql, after}) -> {results:[{columns, values}], modified, error?, afterResult}

export function friendlyError(msg) {
  let m;
  if ((m = msg.match(/no such table: (.+)/))) return `Таблицы «${m[1]}» не существует. Сверьтесь со схемой данных.`;
  if ((m = msg.match(/no such column: (.+)/))) return `Столбец «${m[1]}» не найден. Проверьте название и то, из какой таблицы вы его берёте.`;
  if ((m = msg.match(/ambiguous column name: (.+)/))) return `Столбец «${m[1]}» есть сразу в нескольких таблицах — укажите, из какой (например, t.${m[1].split('.').pop()}).`;
  if (/incomplete input/.test(msg)) return 'Запрос не закончен: проверьте скобки, кавычки и не потерялось ли что-то в конце.';
  if ((m = msg.match(/near "(.*)": syntax error/))) return `Синтаксическая ошибка рядом с «${m[1]}». Частые причины: лишняя или пропущенная запятая, неверный порядок ключевых слов.`;
  if (/syntax error/.test(msg)) return 'Синтаксическая ошибка. Проверьте порядок ключевых слов и запятые.';
  if (/misuse of aggregate/.test(msg)) return 'Агрегатную функцию (COUNT, SUM, …) нельзя использовать в WHERE. Фильтровать по агрегату нужно в HAVING.';
  if (/UNIQUE constraint failed/.test(msg)) return `Нарушено ограничение уникальности: ${msg.replace(/^.*failed: /, '')}.`;
  if (/NOT NULL constraint failed/.test(msg)) return `Столбец ${msg.replace(/^.*failed: /, '')} обязателен — NULL туда записать нельзя.`;
  if (/FOREIGN KEY constraint failed/.test(msg)) return 'Нарушена связь между таблицами (внешний ключ).';
  if (/CHECK constraint failed/.test(msg)) return 'Значение не проходит проверку CHECK.';
  if (/more than one row returned by a subquery|only a single result allowed/.test(msg)) return 'Подзапрос вернул не одно значение, а несколько.';
  if (/timeout/i.test(msg)) return 'Запрос выполняется слишком долго и был остановлен. Возможно, потерялось условие соединения (получилось декартово произведение).';
  return msg;
}

const norm = (v) => {
  if (v === null || v === undefined) return 'N:';
  if (typeof v === 'number') return 'n:' + (Math.round(v * 1e4) / 1e4);
  if (v instanceof Uint8Array) return 'b:' + v.length;
  return 's:' + String(v);
};
const rowKey = (r) => r.map(norm).join('\u0001');

export function compareSets(user, ref, { ordered = false, names = false } = {}) {
  if (!user) return { ok: false, message: 'Запрос не вернул таблицу результата. Нужен SELECT.' };
  if (user.columns.length !== ref.columns.length) {
    return { ok: false, message: `Столбцов должно быть ${ref.columns.length}, а у вас ${user.columns.length}.` };
  }
  if (names) {
    const a = user.columns.map((c) => c.toLowerCase()).join(',');
    const b = ref.columns.map((c) => c.toLowerCase()).join(',');
    if (a !== b) return { ok: false, message: `Названия столбцов должны быть: ${ref.columns.join(', ')}. Используйте AS.` };
  }
  if (user.values.length !== ref.values.length) {
    return { ok: false, message: `Строк должно быть ${ref.values.length}, а получилось ${user.values.length}.` };
  }
  const uk = user.values.map(rowKey);
  const rk = ref.values.map(rowKey);
  if (ordered) {
    for (let i = 0; i < rk.length; i++) {
      if (uk[i] !== rk[i]) {
        const same = [...uk].sort().join('|') === [...rk].sort().join('|');
        return { ok: false, message: same ? 'Набор строк верный, но порядок неправильный — проверьте ORDER BY.' : `Строка №${i + 1} отличается от ожидаемой.` };
      }
    }
    return { ok: true };
  }
  uk.sort(); rk.sort();
  for (let i = 0; i < rk.length; i++) {
    if (uk[i] !== rk[i]) return { ok: false, message: 'Количество строк совпало, но значения отличаются. Проверьте условия и выражения.' };
  }
  return { ok: true };
}

const stripComments = (s) => s.replace(/--.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');

export async function gradeCode(exec, ex, code) {
  const clean = stripComments(code).trim();
  if (!clean) return { passed: false, message: 'Сначала напишите запрос.', output: null };

  const usr = await exec({ sql: code, after: ex.check });
  const output = usr.error
    ? { kind: 'error', message: friendlyError(usr.error), raw: usr.error }
    : toOutput(usr);
  if (usr.error) return { passed: false, message: friendlyError(usr.error), output };

  for (const [src, msg] of ex.require || []) {
    if (!new RegExp(src, 'i').test(clean)) return { passed: false, message: msg, output };
  }
  for (const [src, msg] of ex.forbid || []) {
    if (new RegExp(src, 'i').test(clean)) return { passed: false, message: msg, output };
  }

  const ref = await exec({ sql: ex.solution, after: ex.check });
  if (ref.error) throw new Error(`Эталонное решение «${ex.id}» не выполняется: ${ref.error}`);

  let userSet; let refSet;
  if (ex.check) {
    userSet = usr.afterResult;
    refSet = ref.afterResult;
  } else {
    userSet = usr.results.length ? usr.results[usr.results.length - 1] : null;
    refSet = ref.results[ref.results.length - 1];
  }
  if (!userSet && !ex.check && /^\s*(select|with|values)\b/i.test(clean)) {
    return { passed: false, message: 'Запрос выполнился, но не вернул ни одной строки. Проверьте условия фильтрации.', output };
  }
  const cmp = compareSets(userSet, refSet, ex);
  return { passed: cmp.ok, message: cmp.ok ? 'Верно!' : cmp.message, output };
}

export function toOutput(r) {
  if (r.error) return { kind: 'error', message: friendlyError(r.error), raw: r.error };
  if (!r.results.length) return { kind: 'ok', message: r.modified ? `Готово. Затронуто строк: ${r.modified}.` : 'Готово. Команда выполнена.' };
  const last = r.results[r.results.length - 1];
  return { kind: 'table', columns: last.columns, rows: last.values, total: last.values.length, truncated: r.truncated };
}
