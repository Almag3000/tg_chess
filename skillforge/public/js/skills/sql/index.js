import { runner } from './runner.js';
import m1 from './m1.js';
import m2 from './m2.js';
import m3 from './m3.js';
import m4 from './m4.js';
import m5 from './m5.js';
import m6 from './m6.js';
import m7 from './m7.js';
import m8 from './m8.js';

export default {
  id: 'sql',
  title: 'SQL',
  tagline: 'Язык данных: от первого SELECT до оконных функций',
  icon: '🗄️',
  color: '#2f7cf6',
  description:
    'Практический курс на реальной базе интернет-магазина. Каждый запрос выполняется прямо в браузере — ничего устанавливать не нужно.',
  audience: 'Подойдёт с нуля: аналитикам, разработчикам, тестировщикам, продактам.',
  runner,
  modules: [m1, m2, m3, m4, m5, m6, m7, m8],
  sandbox: true,
};
