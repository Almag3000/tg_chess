## Проектируем таблицы

Команды, которые создают и меняют **структуру** базы, называются **DDL** (Data Definition Language): `CREATE`, `ALTER`, `DROP`.

```sql try
CREATE TABLE reviews (
  id          INTEGER PRIMARY KEY,
  product_id  INTEGER NOT NULL REFERENCES products(id),
  rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment     TEXT,
  created_at  TEXT DEFAULT CURRENT_DATE
);
INSERT INTO reviews (product_id, rating, comment) VALUES (1, 5, 'Отличный ноутбук');
SELECT * FROM reviews;
```

## Типы данных SQLite

SQLite очень гибок: есть пять «классов хранения» — `INTEGER`, `REAL`, `TEXT`, `BLOB`, `NULL`. Типы столбцов (`INT`, `VARCHAR(50)`, `DATE`…) скорее подсказки. В строгих СУБД (PostgreSQL, MySQL) типы строгие — всегда выбирайте тип осознанно.

| Для чего | Тип |
|---|---|
| целые числа, id, флаги 0/1 | `INTEGER` |
| дробные | `REAL` (деньги в строгих СУБД — `NUMERIC`/`DECIMAL`) |
| текст, даты ISO | `TEXT` |

## Ограничения (constraints)

Ограничения — это **правила, которые база проверяет за вас**. Чем больше правил описано в схеме, тем меньше «грязных» данных попадёт в систему.

| Ограничение | Что гарантирует |
|---|---|
| `PRIMARY KEY` | строка идентифицируется однозначно; значения уникальны и не `NULL` |
| `NOT NULL` | значение обязательно |
| `UNIQUE` | значения не повторяются (например, email) |
| `DEFAULT x` | значение по умолчанию |
| `CHECK (условие)` | значение удовлетворяет условию |
| `REFERENCES t(col)` | внешний ключ: значение должно существовать в другой таблице |

**Составной ключ** — из нескольких столбцов, когда уникальна комбинация:

```sql try error
CREATE TABLE wishlist (
  customer_id INTEGER NOT NULL,
  product_id  INTEGER NOT NULL,
  PRIMARY KEY (customer_id, product_id)
);
INSERT INTO wishlist VALUES (1, 4), (1, 5);
INSERT INTO wishlist VALUES (1, 4);
```

Третья вставка падает: пара `(1, 4)` уже есть.

## Создать таблицу из запроса

```sql try
CREATE TABLE vip_customers AS
SELECT id, name, city FROM customers WHERE vip = 1;
SELECT * FROM vip_customers;
```

Так удобно делать копии и «витрины» — но ограничения (`PRIMARY KEY`, `NOT NULL`) при этом **не копируются**.

## Изменение структуры

```sql try
ALTER TABLE products ADD COLUMN discount INTEGER DEFAULT 0;
UPDATE products SET discount = 15 WHERE category_id = 2;
SELECT name, price, discount FROM products WHERE category_id = 2;
```

`DROP TABLE имя;` удаляет таблицу вместе с данными — **безвозвратно**. Безопасная версия: `DROP TABLE IF EXISTS имя;`.

## Индексы — ускорители поиска

Без индекса запрос `WHERE customer_id = 5` читает **всю** таблицу, строку за строкой. **Индекс** — отдельная отсортированная структура (как оглавление книги), по которой нужную строку находят за миллисекунды.

```sql
CREATE INDEX idx_orders_customer ON orders(customer_id);
```

Что стоит запомнить:

- индексируют столбцы, по которым часто **фильтруют** (`WHERE`), **соединяют** (`JOIN … ON`) и **сортируют**;
- индекс ускоряет чтение, но **замедляет запись** (его нужно обновлять) и занимает место — «индекс на всё» — плохая идея;
- первичные ключи индексируются автоматически.

Посмотреть, как база собирается выполнять запрос, можно командой `EXPLAIN QUERY PLAN`:

```sql try
CREATE INDEX idx_orders_customer ON orders(customer_id);
EXPLAIN QUERY PLAN SELECT * FROM orders WHERE customer_id = 5;
```

Слово `SEARCH … USING INDEX` вместо `SCAN` означает, что индекс используется.

## Запомните

- `CREATE TABLE` задаёт столбцы, типы и ограничения; ограничения защищают данные.
- `PRIMARY KEY`, `NOT NULL`, `UNIQUE`, `CHECK`, `DEFAULT`, `REFERENCES` — ваши охранники.
- `ALTER TABLE … ADD COLUMN` расширяет таблицу; `DROP TABLE` необратим.
- Индексы ускоряют чтение по фильтруемым столбцам, но замедляют запись.
