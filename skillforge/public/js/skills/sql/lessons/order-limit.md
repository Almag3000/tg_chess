## Порядок строк не гарантирован

Если не попросить явно, база вернёт строки в *любом* порядке — обычно в том, в котором ей удобнее, но **без гарантий**. Нужен определённый порядок — пишите `ORDER BY`.

```sql try
SELECT name, price
FROM products
ORDER BY price DESC;
```

- `ASC` — по возрастанию (по умолчанию);
- `DESC` — по убыванию.

## Сортировка по нескольким столбцам

Когда значения в первом столбце совпадают, в дело вступает второй:

```sql try
SELECT name, city
FROM customers
ORDER BY city ASC, name DESC;
```

Сортировать можно и по псевдониму из `SELECT`, и по выражению:

```sql try
SELECT name, price * stock AS stock_value
FROM products
ORDER BY stock_value DESC;
```

## Ограничиваем количество: LIMIT и OFFSET

`LIMIT n` оставляет первые `n` строк. Вместе с `ORDER BY` получаем рейтинги («топ-3»):

```sql try
SELECT name, price
FROM products
ORDER BY price DESC
LIMIT 3;
```

`OFFSET k` пропускает первые `k` строк — так делают **постраничный вывод**: страница 3 по 10 строк — это `LIMIT 10 OFFSET 20`.

```sql try
SELECT id, name
FROM products
ORDER BY id
LIMIT 5 OFFSET 5;
```

> **Всегда сортируйте при использовании LIMIT.** Без `ORDER BY` «первые 3 строки» — это случайные три строки.

## Уникальные значения: DISTINCT

`DISTINCT` убирает дубликаты строк в результате:

```sql try
SELECT DISTINCT city
FROM customers;
```

`DISTINCT` относится ко **всей строке результата**: `SELECT DISTINCT city, vip` вернёт уникальные *пары*.

## Порядок записи ключевых слов

Части запроса пишутся в строго заданном порядке:

```sql
SELECT DISTINCT ...
FROM ...
WHERE ...
ORDER BY ...
LIMIT ... OFFSET ...;
```

## Запомните

- Без `ORDER BY` порядок строк не определён.
- `DESC` — по убыванию; можно сортировать по нескольким столбцам.
- `LIMIT` + `ORDER BY` = топ-N; `OFFSET` = постраничность.
- `DISTINCT` убирает повторяющиеся строки результата.
