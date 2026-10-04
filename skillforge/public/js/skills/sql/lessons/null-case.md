## NULL — это «неизвестно», а не «ноль»

`NULL` обозначает **отсутствие значения**: email не указан, дата закрытия ещё не наступила. Это не `0` и не пустая строка. Главное свойство: **любая операция с `NULL` даёт `NULL`**, а любое сравнение с `NULL` — не «истина» и не «ложь», а «неизвестно».

```sql try
SELECT 1 + NULL AS a, NULL = NULL AS b, NULL <> 5 AS c;
```

Всё `NULL`! А `WHERE` пропускает только строки, где условие **истинно**, — «неизвестно» отбрасывается. Поэтому:

- `WHERE email = NULL` не вернёт **ничего** — никогда;
- правильно: `WHERE email IS NULL` / `IS NOT NULL`.

```sql try
SELECT name FROM customers WHERE email = NULL;
```

Запустите и убедитесь: пусто. Теперь с `IS NULL`:

```sql try
SELECT name FROM customers WHERE email IS NULL;
```

## Подстановка значения: COALESCE

`COALESCE(a, b, c, …)` возвращает **первое не-`NULL`** значение:

```sql try
SELECT name, COALESCE(email, 'нет email') AS contact
FROM customers;
```

Парные функции: `IFNULL(a, b)` — то же для двух аргументов, `NULLIF(a, b)` — наоборот, возвращает `NULL`, если `a = b`. Классическое применение `NULLIF` — защита от деления на ноль:

```sql try
SELECT name, price * 1.0 / NULLIF(stock, 0) AS price_per_item
FROM products;
```

Деление на `NULL` даёт `NULL`, а не ошибку — скрипт не упадёт.

## Условная логика: CASE

`CASE` — это «если … то … иначе …» внутри запроса:

```sql try
SELECT name, price,
  CASE
    WHEN price < 5000  THEN 'дешёвый'
    WHEN price < 30000 THEN 'средний'
    ELSE 'дорогой'
  END AS tier
FROM products;
```

Ветки проверяются **сверху вниз**, срабатывает первая подошедшая. Если ни одна не подошла и `ELSE` нет — получится `NULL`.

Короткая форма — когда сравниваем одно значение с вариантами:

```sql try
SELECT name,
  CASE vip WHEN 1 THEN 'VIP' ELSE 'обычный' END AS segment
FROM customers;
```

> **Совет.** `CASE` можно использовать где угодно, где допустимо выражение: в `SELECT`, `WHERE`, `ORDER BY`, внутри агрегатов. Позже мы увидим, как с его помощью считать «условные» суммы.

## Запомните

- `NULL` = «неизвестно». Операции с ним дают `NULL`.
- Проверяйте `IS NULL` / `IS NOT NULL`, но никогда `= NULL`.
- `COALESCE` подставляет значение по умолчанию; `NULLIF(x, 0)` спасает от деления на ноль.
- `CASE WHEN … THEN … ELSE … END` — условия внутри запроса; порядок веток важен.
