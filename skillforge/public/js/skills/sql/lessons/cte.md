## Запрос по шагам: WITH

Когда подзапросы вкладываются друг в друга на три уровня, запрос превращается в «матрёшку». **CTE** (Common Table Expression, «общее табличное выражение») решает это: вы даёте промежуточному результату имя и пишете запрос сверху вниз, **по шагам**.

```sql try
WITH customer_totals AS (
  SELECT o.customer_id, SUM(oi.qty * oi.unit_price) AS total
  FROM orders o
  JOIN order_items oi ON oi.order_id = o.id
  WHERE o.status <> 'cancelled'
  GROUP BY o.customer_id
)
SELECT c.name, ct.total
FROM customer_totals ct
JOIN customers c ON c.id = ct.customer_id
ORDER BY ct.total DESC
LIMIT 5;
```

Структура: `WITH имя AS ( запрос ) основной_запрос`. После этого `имя` можно использовать как обычную таблицу — но только внутри этого запроса.

## Несколько CTE подряд

Через запятую. Следующие могут использовать предыдущие:

```sql try
WITH
  cat_revenue AS (
    SELECT c.name AS category, SUM(oi.qty * oi.unit_price) AS revenue
    FROM order_items oi
    JOIN products p   ON p.id = oi.product_id
    JOIN categories c ON c.id = p.category_id
    GROUP BY c.name
  ),
  total AS (
    SELECT SUM(revenue) AS all_revenue FROM cat_revenue
  )
SELECT category,
       revenue,
       ROUND(100.0 * revenue / all_revenue, 1) AS pct
FROM cat_revenue, total
ORDER BY revenue DESC;
```

Именно такой стиль — «разбей сложный вопрос на шаги и назови каждый» — отличает читаемый аналитический SQL от нечитаемого.

## Рекурсивные CTE

CTE может ссылаться **сама на себя** — так обходят деревья и генерируют последовательности. Устройство:

1. **Якорь** — начальные строки.
2. `UNION ALL`
3. **Рекурсивная часть** — как получить следующий уровень из предыдущего. Останавливается, когда новых строк нет.

Числа от 1 до 5:

```sql try
WITH RECURSIVE n(x) AS (
  SELECT 1
  UNION ALL
  SELECT x + 1 FROM n WHERE x < 5
)
SELECT x FROM n;
```

А вот что действительно полезно — **иерархия**. Уровень каждого сотрудника в оргструктуре (директор — 0):

```sql try
WITH RECURSIVE tree(id, name, depth) AS (
  SELECT id, name, 0 FROM employees WHERE manager_id IS NULL
  UNION ALL
  SELECT e.id, e.name, tree.depth + 1
  FROM employees e
  JOIN tree ON e.manager_id = tree.id
)
SELECT name, depth FROM tree ORDER BY depth, name;
```

> Без условия остановки рекурсия бесконечна. Когда пишете `WHERE x < …` — убедитесь, что оно действительно сработает.

## CTE или подзапрос?

Функционально они часто эквивалентны. Выбирайте CTE, когда: промежуточный результат используется **несколько раз**, шагов больше двух или вы хотите, чтобы запрос читался как рассказ.

## Запомните

- `WITH имя AS (…)` даёт имя промежуточному результату; пишем запрос шагами.
- CTE можно перечислять через запятую, они видят друг друга.
- `WITH RECURSIVE` = якорь + `UNION ALL` + шаг; нужен для иерархий и последовательностей.
