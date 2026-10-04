// Проверка контента: эталонные решения выполняются, проходят собственную проверку,
// у каждого упражнения есть подсказки, у quiz — корректный ответ, а файлы уроков существуют.
import { readFileSync, existsSync } from 'node:fs';
import { makeExec } from './exec.mjs';
import { gradeCode } from '../public/js/skills/sql/grade.js';
import { datasetSQL } from '../public/js/skills/sql/dataset.js';

// index.js тянет runner.js (браузерный Worker) — импортируем только список модулей
const src = readFileSync(new URL('../public/js/skills/sql/index.js', import.meta.url), 'utf8');
const files = [...src.matchAll(/from '\.\/(m\d+\.js)'/g)].map((m) => m[1]);

const exec = await makeExec();
let errors = 0; let warnings = 0; let total = 0;
const fail = (id, msg) => { errors++; console.log(`  ✗ ${id}: ${msg}`); };
const warn = (id, msg) => { warnings++; console.log(`  ! ${id}: ${msg}`); };
const ids = new Set();

for (const f of files) {
  const mod = (await import(`../public/js/skills/sql/${f}`)).default;
  console.log(`\n# ${mod.title}`);
  for (const lesson of mod.lessons) {
    if (!existsSync(new URL(`../public/js/skills/sql/lessons/${lesson.body}`, import.meta.url))) fail(lesson.id, `нет файла ${lesson.body}`);
    else {
      const md = readFileSync(new URL(`../public/js/skills/sql/lessons/${lesson.body}`, import.meta.url), 'utf8');
      // интерактивные примеры в теории должны выполняться
      for (const m of md.matchAll(/```sql try( error)?\n([\s\S]*?)```/g)) {
        const r = await exec({ sql: m[2] });
        if (r.error && !m[1]) fail(`${lesson.id}/theory`, `пример не выполняется: ${r.error}\n${m[2]}`);
        if (!r.error && m[1]) fail(`${lesson.id}/theory`, `пример помечен error, но выполнился:\n${m[2]}`);
      }
    }
    console.log(`- ${lesson.title} (${lesson.exercises.length})`);
    for (const ex of lesson.exercises) {
      total++;
      if (ids.has(ex.id)) fail(ex.id, 'дубликат id'); ids.add(ex.id);
      if (!ex.prompt) fail(ex.id, 'нет prompt');
      if (ex.type === 'quiz') {
        if (!Array.isArray(ex.options) || ex.options.length < 2) fail(ex.id, 'мало вариантов');
        if (!(ex.answer >= 0 && ex.answer < ex.options.length)) fail(ex.id, 'answer вне диапазона');
        if (!ex.explain) fail(ex.id, 'нет explain');
        continue;
      }
      if (!ex.solution) { fail(ex.id, 'нет solution'); continue; }
      if (!lesson.exam && (!ex.hints || ex.hints.length < 2)) fail(ex.id, 'нужно минимум 2 подсказки');
      const ref = await exec({ sql: ex.solution, after: ex.check });
      if (ref.error) { fail(ex.id, `solution: ${ref.error}`); continue; }
      const set = ex.check ? ref.afterResult : ref.results[ref.results.length - 1];
      if (!set || set.values.length === 0) warn(ex.id, 'эталон возвращает пустой результат');
      const g = await gradeCode(exec, ex, ex.solution);
      if (!g.passed) fail(ex.id, `эталон не проходит свою проверку: ${g.message}`);
      if (ex.starter) {
        const gs = await gradeCode(exec, ex, ex.starter);
        if (gs.passed) fail(ex.id, 'starter уже проходит проверку — задача «починить» тривиальна');
      }
      // последняя подсказка не должна быть пустой, а эталонный запрос не должен совпадать с starter
    }
  }
}
console.log(`\nУпражнений: ${total}, ошибок: ${errors}, предупреждений: ${warnings}`);
process.exit(errors ? 1 : 0);
