import { h, toast, relTime } from '../util.js';
import * as store from '../store.js';

export function settingsView(root) {
  const sync = store.getSync();
  const name = h('input', { type: 'text', maxlength: 40, value: store.getName(), placeholder: 'Как к вам обращаться?', id: 'name' });
  name.addEventListener('change', () => { store.setName(name.value.trim()); toast('Имя сохранено'); });

  const tokenField = h('input', { type: 'password', readonly: true, value: store.getToken(), id: 'token', 'aria-label': 'Код синхронизации' });
  const show = h('button', { class: 'btn small', type: 'button', onclick: () => { tokenField.type = tokenField.type === 'password' ? 'text' : 'password'; show.textContent = tokenField.type === 'password' ? 'Показать' : 'Скрыть'; } }, 'Показать');
  const copy = h('button', { class: 'btn small', type: 'button', onclick: async () => { try { await navigator.clipboard.writeText(store.getToken()); toast('Код скопирован'); } catch { tokenField.type = 'text'; tokenField.select(); } } }, 'Копировать');

  const loginField = h('input', { type: 'text', placeholder: 'Вставьте код с другого устройства', autocomplete: 'off', spellcheck: 'false' });
  const login = h('button', { class: 'btn small primary', type: 'button', onclick: async () => {
    try { await store.useToken(loginField.value); toast('Прогресс загружен'); location.hash = '#/'; location.reload(); } catch (e) { toast(e.message, 'bad'); }
  } }, 'Войти');

  const syncState = !sync.available
    ? h('p', { class: 'muted' }, 'Сервер синхронизации недоступен — прогресс хранится только в этом браузере. Используйте экспорт ниже, чтобы сохранить резервную копию.')
    : h('p', null, sync.status === 'ok' ? `✅ Синхронизировано ${sync.last ? relTime(sync.last) : ''}` : sync.status === 'syncing' ? '⏳ Синхронизация…' : `⚠️ Ошибка синхронизации: ${sync.error}`);

  const file = h('input', { type: 'file', accept: 'application/json', hidden: true });
  file.addEventListener('change', async () => {
    try { store.importJSON(await file.files[0].text()); toast('Прогресс импортирован'); location.hash = '#/'; location.reload(); } catch { toast('Не удалось прочитать файл', 'bad'); }
  });

  const exportBtn = h('button', { class: 'btn', type: 'button', onclick: () => {
    const a = h('a', { href: URL.createObjectURL(new Blob([store.exportJSON()], { type: 'application/json' })), download: `skillforge-progress-${new Date().toISOString().slice(0, 10)}.json` });
    document.body.append(a); a.click(); a.remove();
  } }, '⬇ Экспорт прогресса');

  const reset = h('button', { class: 'btn danger', type: 'button', onclick: () => {
    if (confirm('Удалить весь прогресс на этом устройстве? Если включена синхронизация, при следующем подключении он вернётся с сервера.')) { store.resetAll(); toast('Прогресс сброшен'); location.hash = '#/'; location.reload(); }
  } }, 'Сбросить прогресс');

  root.replaceChildren(
    h('nav', { class: 'crumbs' }, h('a', { href: '#/' }, 'Навыки'), ' › Настройки'),
    h('h1', { class: 'page-title' }, 'Профиль и прогресс'),
    h('section', { class: 'card form' }, h('h3', null, 'Профиль'), h('label', { for: 'name' }, 'Имя'), name,
      h('p', { class: 'muted tiny' }, 'Регистрации нет: прогресс привязан к секретному коду ниже. Имя нужно только для приветствия.')),
    h('section', { class: 'card form' }, h('h3', null, 'Синхронизация между устройствами'), syncState,
      h('label', { for: 'token' }, 'Ваш код'), h('div', { class: 'row' }, tokenField, show, copy),
      h('p', { class: 'muted tiny' }, 'Код — ваш «пароль»: любой, кто его знает, видит ваш прогресс. Чтобы продолжить на другом устройстве, откройте сайт там и войдите по этому коду.'),
      h('label', null, 'Войти по коду'), h('div', { class: 'row' }, loginField, login)),
    h('section', { class: 'card form' }, h('h3', null, 'Резервная копия'),
      h('div', { class: 'row wrap' }, exportBtn, h('button', { class: 'btn', type: 'button', onclick: () => file.click() }, '⬆ Импорт'), file),
      h('p', { class: 'muted tiny' }, 'Импорт объединяет файл с текущим прогрессом — ничего не стирает.')),
    h('section', { class: 'card form' }, h('h3', null, 'Опасная зона'), reset));
}
