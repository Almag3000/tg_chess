## От строк к цифрам

Часто нас интересуют не отдельные строки, а **итоги**: сколько клиентов, какая средняя цена, какова выручка. Для этого есть **агрегатные функции** — они «схлопывают» много строк в одно значение.

| Функция | Что считает |
|---|---|
| `COUNT(*)` | количество строк |
| `COUNT(столбец)` | количество строк, где столбец **не `NULL`** |
| `COUNT(DISTINCT столбец)` | количество уникальных значений |
| `SUM(x)` | сумма |
| `AVG(x)` | среднее |
| `MIN(x)`, `MAX(x)` | минимум и максимум |

```sql try
SELECT COUNT(*)        AS products_count,
       MIN(price)      AS cheapest,
       MAX(price)      AS most_expensive,
       ROUND(AVG(price), 2) AS avg_price
FROM products;
```

Без `GROUP BY` результат — **всегда одна строка**.

## Агрегаты игнорируют NULL

Это важное правило: `SUM`, `AVG`, `MIN`, `MAX`, `COUNT(столбец)` просто **пропускают** `NULL`. Сравните:

```sql try
SELECT COUNT(*)     AS all_rows,
       COUNT(email) AS with_email
FROM customers;
```

`COUNT(*)` считает строки, `COUNT(email)` — только те, где email есть. Среднее тоже считается только по известным значениям: `AVG` из `(10, NULL, 20)` равно 15, а не 10.

## Уникальные значения

```sql try
SELECT COUNT(DISTINCT city) AS cities FROM customers;
```

## Фильтр перед подсчётом

`WHERE` отрабатывает **до** агрегации — сначала отбираются строки, потом по ним считаем:

```sql try
SELECT COUNT(*) AS delivered_orders
FROM orders
WHERE status = 'delivered';
```

## Выручка: агрегат от выражения

Внутрь агрегата можно положить выражение:

```sql try
SELECT SUM(qty * unit_price) AS revenue
FROM order_items;
```

> **Нельзя смешивать** агрегат и обычный столбец без группировки: `SELECT name, COUNT(*) FROM customers` — некорректно (SQLite выдаст произвольное имя, другие СУБД — ошибку). Как правильно — в следующем уроке.

## Запомните

- Агрегаты превращают набор строк в одно значение.
- `COUNT(*)` — все строки, `COUNT(col)` — только не-`NULL`.
- `SUM`, `AVG`, `MIN`, `MAX` игнорируют `NULL`.
- `WHERE` работает до агрегации; `COUNT(DISTINCT …)` считает уникальные.
