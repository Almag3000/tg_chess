## Проблема, которую не решает GROUP BY

`GROUP BY` схлопывает строки: из 14 сотрудников получится 5 отделов. А как показать **каждого** сотрудника и рядом — его место по зарплате в отделе? Строки терять нельзя, а считать нужно «по группе».

Для этого есть **оконные функции**. Они вычисляют значение для каждой строки, «глядя» на связанное с ней окно (набор строк), но **не схлопывают** результат.

```sql try
SELECT name, dept, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC) AS place
FROM employees
ORDER BY place;
```

Ключевая конструкция — `OVER (…)`:

- `PARTITION BY` — на какие группы делить (необязательно); как `GROUP BY`, но строки остаются;
- `ORDER BY` — в каком порядке нумеровать внутри группы.

## Нумерация внутри групп

```sql try
SELECT name, dept, salary,
       ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary DESC) AS rn
FROM employees
ORDER BY dept, rn;
```

В каждом отделе нумерация начинается заново.

## ROW_NUMBER, RANK, DENSE_RANK

Что делать, если у двух сотрудников одинаковая зарплата? Три функции отвечают по-разному:

| Функция | Одинаковые значения | После «ничьей» |
|---|---|---|
| `ROW_NUMBER()` | получают разные номера (произвольно) | 1, 2, 3, 4 |
| `RANK()` | получают один номер | 1, 2, 2, **4** (пропуск) |
| `DENSE_RANK()` | получают один номер | 1, 2, 2, **3** (без пропусков) |

```sql try
SELECT name, salary,
       ROW_NUMBER() OVER w AS row_num,
       RANK()       OVER w AS rnk,
       DENSE_RANK() OVER w AS dense
FROM employees
WINDOW w AS (ORDER BY salary DESC)
ORDER BY salary DESC, name;
```

(Именованное окно `WINDOW w AS …` избавляет от повторов.) Найдите Татьяну Ершову и Игоря Савина — у них одинаковая зарплата.

## Топ-N в каждой группе

Классическая задача: «два самых дорогих товара в **каждой** категории». Оконные функции нельзя писать в `WHERE` (они вычисляются после него), поэтому оборачиваем в подзапрос или CTE:

```sql try
WITH ranked AS (
  SELECT name, category_id, price,
         ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY price DESC) AS rn
  FROM products
)
SELECT name, category_id, price
FROM ranked
WHERE rn <= 2
ORDER BY category_id, rn;
```

Этот шаблон — «нумерация + фильтр по номеру» — вы будете применять постоянно: последний заказ каждого клиента, лучший сотрудник отдела, первый визит пользователя.

## Запомните

- Оконная функция считает «по окну», не схлопывая строки.
- `OVER (PARTITION BY … ORDER BY …)` задаёт группы и порядок.
- `ROW_NUMBER` — уникальные номера; `RANK` — с пропусками при равенстве; `DENSE_RANK` — без пропусков.
- Топ-N в группе: нумерация в CTE/подзапросе и фильтр по номеру снаружи.
