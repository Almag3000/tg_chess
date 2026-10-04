## EXISTS — «есть ли хоть одна строка?»

`EXISTS (подзапрос)` возвращает «истина», если подзапрос вернул **хотя бы одну строку**. Что именно выбирается — неважно (принято писать `SELECT 1`). Обычно подзапрос коррелированный:

```sql try
SELECT c.name
FROM customers c
WHERE EXISTS (
  SELECT 1 FROM orders o
  WHERE o.customer_id = c.id AND o.status = 'cancelled'
);
```

«Клиенты, у которых есть отменённый заказ». Как только нашлась первая подходящая строка, поиск прекращается — `EXISTS` часто быстрее `IN` и `JOIN` + `DISTINCT`.

## NOT EXISTS — «нет ни одной»

Идеальный способ искать «без пары»: не боится `NULL` (в отличие от `NOT IN`) и читается как предложение:

```sql try
SELECT c.name
FROM customers c
WHERE NOT EXISTS (
  SELECT 1 FROM orders o WHERE o.customer_id = c.id
);
```

Сравните с anti-join из урока про `LEFT JOIN` — результат тот же. Используйте то, что читается лучше.

## Операции над множествами

Результаты запросов можно объединять, как множества. Условие: одинаковое **количество столбцов** с совместимыми типами; имена берутся из первого запроса.

| Операция | Результат |
|---|---|
| `UNION` | объединение, **дубликаты убираются** |
| `UNION ALL` | объединение, дубликаты **остаются** (быстрее) |
| `INTERSECT` | только строки, которые есть в обоих |
| `EXCEPT` | строки первого, которых нет во втором |

```sql try
SELECT name, 'клиент' AS kind FROM customers
UNION ALL
SELECT name, 'сотрудник' FROM employees
ORDER BY name
LIMIT 10;
```

`ORDER BY` пишется один раз, в самом конце, и относится ко всему объединению.

`EXCEPT` и `INTERSECT` удобны для сравнений: «города клиентов, у которых нет ни одного отменённого заказа»:

```sql try
SELECT city FROM customers
EXCEPT
SELECT c.city FROM customers c JOIN orders o ON o.customer_id = c.id AND o.status = 'cancelled';
```

> **UNION или UNION ALL?** Если дубликатов быть не может или они вам нужны — берите `UNION ALL`: он не тратит время на дедупликацию. `UNION` без нужды — частая причина медленных запросов.

## Запомните

- `EXISTS` / `NOT EXISTS` проверяют наличие строк в подзапросе; `NOT EXISTS` безопаснее `NOT IN`.
- `UNION` убирает дубли, `UNION ALL` — нет; `INTERSECT` — пересечение; `EXCEPT` — разность.
- У объединяемых запросов должно совпадать число столбцов.
