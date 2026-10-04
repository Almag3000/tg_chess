## Фильтр по результатам группировки

Как выбрать только города, где **три и более** клиента? Условие относится к *группе* (к итогу `COUNT(*)`), а не к отдельной строке. `WHERE` тут бессилен: к моменту его работы групп ещё нет.

Для этого есть `HAVING` — это «`WHERE` для групп»:

```sql try
SELECT city, COUNT(*) AS customers
FROM customers
GROUP BY city
HAVING COUNT(*) >= 3;
```

## WHERE или HAVING?

| | `WHERE` | `HAVING` |
|---|---|---|
| Работает | **до** группировки | **после** группировки |
| Фильтрует | строки | группы |
| Может ли использовать агрегаты | нет | да |

Если условие можно проверить по одной строке (`status <> 'cancelled'`) — это `WHERE`: он отсечёт строки заранее, и группировать придётся меньше. Если условие про итог (`COUNT(*) > 5`) — `HAVING`.

Обычно они работают в паре. «Клиенты, у которых больше 5 **неотменённых** заказов»:

```sql try
SELECT customer_id, COUNT(*) AS orders
FROM orders
WHERE status <> 'cancelled'
GROUP BY customer_id
HAVING COUNT(*) > 5
ORDER BY orders DESC;
```

## Типичная ошибка

```sql
SELECT city, COUNT(*) FROM customers
WHERE COUNT(*) >= 3     -- ошибка: misuse of aggregate
GROUP BY city;
```

Видите `misuse of aggregate` — перенесите условие в `HAVING`.

## Несколько условий в HAVING

Условия объединяются через `AND`/`OR`, как и в `WHERE`:

```sql try
SELECT category_id, COUNT(*) AS n, ROUND(AVG(price)) AS avg_price
FROM products
GROUP BY category_id
HAVING COUNT(*) >= 3 AND AVG(price) > 10000;
```

## Запомните

- `HAVING` фильтрует **группы** по агрегатам, `WHERE` — **строки** до группировки.
- Что можно отсечь через `WHERE` — отсекайте там: так быстрее.
- `misuse of aggregate` в ошибке = агрегат стоит в `WHERE`.
