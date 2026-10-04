// Учебная база «ТехноМаркет». Генерируется детерминированно — у всех пользователей одинаковые данные.

const categories = [
  [1, 'Ноутбуки'], [2, 'Смартфоны'], [3, 'Аксессуары'], [4, 'Аудио'], [5, 'Умный дом'],
];

// id, name, category_id, price, stock, created_at
const products = [
  [1, 'Ноутбук Aero 14', 1, 89990, 12, '2024-03-10'],
  [2, 'Ноутбук Forge 16', 1, 129990, 5, '2024-06-01'],
  [3, 'Ноутбук Lite 13', 1, 54990, 20, '2023-11-20'],
  [4, 'Смартфон Nova X', 2, 69990, 30, '2024-09-05'],
  [5, 'Смартфон Nova Mini', 2, 39990, 45, '2024-09-05'],
  [6, 'Смартфон Pulse 5G', 2, 49990, 0, '2023-05-14'],
  [7, 'Чехол Nova X', 3, 1490, 120, '2024-09-10'],
  [8, 'Зарядка 65W', 3, 2990, 80, '2023-08-01'],
  [9, 'Кабель USB-C 2м', 3, 590, 300, '2022-12-12'],
  [10, 'Мышь Click', 3, 1990, 60, '2023-02-02'],
  [11, 'Клавиатура Type', 3, 4990, 25, '2023-02-02'],
  [12, 'Наушники Beat', 4, 7990, 40, '2024-01-15'],
  [13, 'Наушники Silence Pro', 4, 19990, 15, '2024-10-01'],
  [14, 'Колонка Boom', 4, 5990, 0, '2023-07-07'],
  [15, 'Умная лампа Glow', 5, 1290, 90, '2024-02-20'],
  [16, 'Умная розетка Plug', 5, 990, 150, '2024-02-20'],
  [17, 'Датчик движения', 5, 1590, 70, '2024-04-04'],
  [18, 'Робот-пылесос Sweep', 5, 24990, 8, '2024-05-05'],
  [19, 'Веб-камера Look', 3, 3490, 35, '2023-09-09'],
  [20, 'Подарочная карта', null, 3000, 999, '2023-01-01'],
];

// id, name, city, signup_date, vip, email, referred_by
const customers = [
  [1, 'Анна Смирнова', 'Москва', '2023-01-15', 1, 'anna@mail.test', null],
  [2, 'Борис Кузнецов', 'Санкт-Петербург', '2023-02-20', 0, 'boris@mail.test', 1],
  [3, 'Виктория Попова', 'Москва', '2023-03-05', 1, null, 1],
  [4, 'Григорий Лебедев', 'Казань', '2023-05-18', 0, 'grig@mail.test', 2],
  [5, 'Дарья Новикова', 'Новосибирск', '2023-07-01', 0, 'dasha@mail.test', null],
  [6, 'Егор Морозов', 'Москва', '2023-09-12', 0, null, 3],
  [7, 'Елена Волкова', 'Санкт-Петербург', '2024-01-09', 1, 'elena@mail.test', 1],
  [8, 'Жанна Соколова', 'Казань', '2024-02-14', 0, 'zhanna@mail.test', 4],
  [9, 'Захар Орлов', 'Екатеринбург', '2024-04-22', 0, null, null],
  [10, 'Ирина Фёдорова', 'Москва', '2024-06-30', 0, 'irina@mail.test', 7],
  [11, 'Кирилл Макаров', 'Новосибирск', '2024-08-08', 0, 'kirill@mail.test', 5],
  [12, 'Лариса Зайцева', 'Екатеринбург', '2024-10-19', 1, 'larisa@mail.test', 9],
  [13, 'Максим Белов', 'Москва', '2025-01-11', 0, 'max@mail.test', null],
  [14, 'Наталья Громова', 'Казань', '2025-02-02', 0, null, 8],
  [15, 'Олег Титов', 'Санкт-Петербург', '2025-03-03', 0, 'oleg@mail.test', null],
];

// id, name, dept, manager_id, salary, hire_date
const employees = [
  [1, 'Сергей Иванов', 'Руководство', null, 400000, '2019-01-10'],
  [2, 'Мария Петрова', 'Продажи', 1, 250000, '2019-06-01'],
  [3, 'Алексей Крылов', 'Продажи', 2, 120000, '2020-03-15'],
  [4, 'Ольга Мельник', 'Продажи', 2, 135000, '2021-07-01'],
  [5, 'Павел Дроздов', 'Продажи', 2, 110000, '2023-02-10'],
  [6, 'Николай Гусев', 'Склад', 1, 180000, '2020-01-20'],
  [7, 'Татьяна Ершова', 'Склад', 6, 90000, '2021-11-11'],
  [8, 'Игорь Савин', 'Склад', 6, 90000, '2022-04-04'],
  [9, 'Юлия Антонова', 'Аналитика', 1, 210000, '2021-02-02'],
  [10, 'Руслан Карпов', 'Аналитика', 9, 150000, '2022-09-09'],
  [11, 'Евгения Лисина', 'Аналитика', 9, 150000, '2024-01-15'],
  [12, 'Денис Тарасов', 'Поддержка', 1, 160000, '2022-05-05'],
  [13, 'Вера Комарова', 'Поддержка', 12, 85000, '2023-08-08'],
  [14, 'Артём Носов', 'Поддержка', 12, 85000, '2024-06-06'],
];

// Заказы генерируются псевдослучайно, но детерминированно (LCG).
function makeRng(seed) {
  let s = seed >>> 0;
  return () => {
    s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
    return s / 4294967296;
  };
}

function buildOrders() {
  const rnd = makeRng(20250101);
  const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
  const buyers = [1, 1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 7, 8, 10, 11, 12, 12, 13]; // 9, 14, 15 не покупали
  const signup = Object.fromEntries(customers.map((c) => [c[0], c[3]]));
  const orders = [];
  const items = [];
  const start = Date.UTC(2025, 0, 3);
  let day = 0;
  for (let id = 1; id <= 72; id++) {
    day += 1 + Math.floor(rnd() * 9);
    const date = new Date(start + day * 86400000).toISOString().slice(0, 10);
    let cust = pick(buyers);
    for (let guard = 0; guard < 20 && signup[cust] > date; guard++) cust = pick(buyers);
    const r = rnd();
    const status = r < 0.58 ? 'delivered' : r < 0.74 ? 'shipped' : r < 0.84 ? 'paid' : 'cancelled';
    orders.push([id, cust, date, status]);
    const n = 1 + Math.floor(rnd() * 3);
    const used = new Set();
    for (let k = 0; k < n; k++) {
      let pid = 1 + Math.floor(rnd() * 20);
      if (pid === 20 && rnd() < 0.7) pid = 9;
      if (used.has(pid)) continue;
      used.add(pid);
      const p = products[pid - 1];
      const qty = p[3] < 5000 ? 1 + Math.floor(rnd() * 3) : 1;
      items.push([items.length + 1, id, pid, qty, p[3]]);
    }
  }
  return { orders, items };
}

const q = (v) => (v === null ? 'NULL' : typeof v === 'number' ? String(v) : `'${v.replace(/'/g, "''")}'`);
const insert = (table, rows) => rows.map((r) => `INSERT INTO ${table} VALUES (${r.map(q).join(',')});`).join('\n');

export function datasetSQL() {
  const { orders, items } = buildOrders();
  return `
CREATE TABLE categories (id INTEGER PRIMARY KEY, name TEXT NOT NULL);
CREATE TABLE products (
  id INTEGER PRIMARY KEY, name TEXT NOT NULL, category_id INTEGER REFERENCES categories(id),
  price REAL NOT NULL, stock INTEGER NOT NULL, created_at TEXT NOT NULL
);
CREATE TABLE customers (
  id INTEGER PRIMARY KEY, name TEXT NOT NULL, city TEXT NOT NULL, signup_date TEXT NOT NULL,
  vip INTEGER NOT NULL DEFAULT 0, email TEXT, referred_by INTEGER REFERENCES customers(id)
);
CREATE TABLE orders (
  id INTEGER PRIMARY KEY, customer_id INTEGER NOT NULL REFERENCES customers(id),
  order_date TEXT NOT NULL, status TEXT NOT NULL
);
CREATE TABLE order_items (
  id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL REFERENCES orders(id),
  product_id INTEGER NOT NULL REFERENCES products(id), qty INTEGER NOT NULL, unit_price REAL NOT NULL
);
CREATE TABLE employees (
  id INTEGER PRIMARY KEY, name TEXT NOT NULL, dept TEXT NOT NULL,
  manager_id INTEGER REFERENCES employees(id), salary INTEGER NOT NULL, hire_date TEXT NOT NULL
);
${insert('categories', categories)}
${insert('products', products)}
${insert('customers', customers)}
${insert('orders', orders)}
${insert('order_items', items)}
${insert('employees', employees)}
`;
}
