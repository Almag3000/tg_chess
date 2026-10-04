// Применяем сохранённую тему до отрисовки, чтобы страница не мигала.
try { var t = localStorage.getItem('sf:theme'); if (t) document.documentElement.dataset.theme = t; } catch (e) { /* ignore */ }
