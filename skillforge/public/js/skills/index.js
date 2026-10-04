// Реестр навыков. Чтобы добавить новый навык:
//  1) создайте папку skills/<id>/ с index.js, экспортирующим объект навыка (см. skills/sql/index.js);
//  2) добавьте строку в список ниже.
// Навык — это модули → уроки → упражнения + runner, умеющий запускать и проверять код.
// Упражнения типа 'quiz' работают в любом навыке без runner.
export const registry = [
  { id: 'sql', load: () => import('./sql/index.js').then((m) => m.default) },
];

const cache = new Map();
export async function loadSkill(id) {
  if (cache.has(id)) return cache.get(id);
  const entry = registry.find((r) => r.id === id);
  if (!entry) return null;
  const p = entry.load();
  cache.set(id, p);
  return p;
}
export const loadAll = () => Promise.all(registry.map((r) => loadSkill(r.id)));
