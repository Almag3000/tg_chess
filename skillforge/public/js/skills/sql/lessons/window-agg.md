## Агрегаты в роли оконных

Любой агрегат (`SUM`, `AVG`, `COUNT`, `MIN`, `MAX`) становится оконным, если добавить `OVER (…)`. Тогда он **не схлопывает** строки, а добавляет итог группы к каждой строке:

```sql try
SELECT name, dept, salary,
       ROUND(AVG(salary) OVER (PARTITION BY dept)) AS dept_avg,
       salary - ROUND(AVG(salary) OVER (PARTITION BY dept)) AS vs_avg
FROM employees
ORDER BY dept, salary DESC;
```

Без `PARTITION BY` окно — вся таблица:

```sql try
SELECT name, price,
       ROUND(100.0 * price / SUM(price) OVER (), 1) AS pct_of_total
FROM products
ORDER BY price DESC
LIMIT 5;
```

## Нарастающий итог

Если в `OVER` добавить `ORDER BY`, агрегат считается **накопительно** — от начала окна до текущей строки:

```sql try
WITH monthly AS (
  SELECT strftime('%Y-%m', o.order_date) AS ym,
         SUM(oi.qty * oi.unit_price) AS revenue
  FROM orders o
  JOIN order_items oi ON oi.order_id = o.id
  WHERE o.status <> 'cancelled'
  GROUP BY ym
)
SELECT ym, revenue,
       SUM(revenue) OVER (ORDER BY ym) AS cumulative
FROM monthly
ORDER BY ym;
```

Обратите внимание на комбинацию: сначала `GROUP BY` в CTE, потом оконная функция поверх агрегата.

## Рамка окна

По умолчанию при наличии `ORDER BY` рамка — «от начала до текущей строки». Её можно задать явно — например, скользящее среднее за три месяца:

```sql try
WITH monthly AS (
  SELECT strftime('%Y-%m', order_date) AS ym, COUNT(*) AS orders
  FROM orders GROUP BY ym
)
SELECT ym, orders,
       ROUND(AVG(orders) OVER (ORDER BY ym ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 1) AS moving_avg_3
FROM monthly
ORDER BY ym;
```

`ROWS BETWEEN 2 PRECEDING AND CURRENT ROW` — «две предыдущие строки и текущая».

## LAG и LEAD: соседние строки

- `LAG(x, n)` — значение из строки на `n` позиций **раньше** (по `ORDER BY` окна);
- `LEAD(x, n)` — из строки на `n` позиций **позже**.

Если соседа нет, вернётся `NULL`. Идеально для «изменения к прошлому периоду»:

```sql try
WITH monthly AS (
  SELECT strftime('%Y-%m', order_date) AS ym, COUNT(*) AS orders
  FROM orders GROUP BY ym
)
SELECT ym, orders,
       LAG(orders) OVER (ORDER BY ym) AS prev_month,
       orders - LAG(orders) OVER (ORDER BY ym) AS change
FROM monthly
ORDER BY ym;
```

Для каждого клиента — когда был предыдущий заказ:

```sql try
SELECT customer_id, order_date,
       LAG(order_date) OVER (PARTITION BY customer_id ORDER BY order_date, id) AS prev_order
FROM orders
ORDER BY customer_id, order_date, id
LIMIT 10;
```

## Запомните

- `агрегат() OVER (PARTITION BY …)` — итог группы рядом с каждой строкой.
- `SUM() OVER (ORDER BY …)` — нарастающий итог; рамка настраивается через `ROWS BETWEEN …`.
- `LAG` / `LEAD` берут значение из предыдущей / следующей строки — база для динамики «к прошлому периоду».
- Оконные функции выполняются после `WHERE`/`GROUP BY` — фильтровать по ним можно только через подзапрос или CTE.
