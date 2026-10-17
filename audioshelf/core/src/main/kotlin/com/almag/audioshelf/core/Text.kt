package com.almag.audioshelf.core

import kotlin.math.abs

object Text {

    /** Case- and ё-insensitive form used for search and duplicate checks. */
    fun normalize(s: String): String =
        s.trim().lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ")

    fun matches(query: String, vararg fields: String): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        return fields.any { normalize(it).contains(q) }
    }

    /** Russian plural: plural(5, "книга", "книги", "книг") == "книг". */
    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100
        val b = a % 10
        return when {
            a in 11..14 -> many
            b == 1 -> one
            b in 2..4 -> few
            else -> many
        }
    }

    fun count(n: Int, one: String, few: String, many: String) = "$n ${plural(n, one, few, many)}"

    fun books(n: Int) = count(n, "книга", "книги", "книг")
    fun authors(n: Int) = count(n, "автор", "автора", "авторов")
    fun series(n: Int) = count(n, "серия", "серии", "серий")

    /** "Данияр Сугралинов" -> "ДС", "Leach23" -> "LE". Same rule as the web version. */
    fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        if (parts.size >= 2) return (parts[0].take(1) + parts[1].take(1)).uppercase()
        return parts[0].take(2).uppercase()
    }

    private val ordinals = listOf(
        "перв" to 1, "втор" to 2, "трет" to 3, "четв" to 4, "пят" to 5,
        "шест" to 6, "седьм" to 7, "восьм" to 8, "девят" to 9, "десят" to 10,
    )

    /**
     * Guesses the volume number from a title: "Том 12", "Том первый",
     * "Похоже, он доигрался. Том третий", "Книга 3", "Часть 2".
     */
    fun guessVolume(title: String): Int? {
        val t = normalize(title)
        Regex("(?:том|книга|часть|кн\\.|т\\.)\\s*(\\d{1,3})").find(t)?.let { return it.groupValues[1].toInt() }
        Regex("(?:том|книга|часть)\\s+([а-я]+)").find(t)?.let { m ->
            val word = m.groupValues[1]
            ordinals.firstOrNull { word.startsWith(it.first) }?.let { return it.second }
        }
        Regex("([а-я]+)\\s+(?:том|книга|часть)(?:$|[\\s.,])").find(t)?.let { m ->
            val word = m.groupValues[1]
            ordinals.firstOrNull { word.startsWith(it.first) }?.let { return it.second }
        }
        Regex("(?:^|\\s)(\\d{1,3})$").find(t)?.let { return it.groupValues[1].toInt() }
        return null
    }
}

/**
 * Spine colours and heights. The hash is the same as in the web version, so
 * every series keeps the colour the user is used to.
 */
object Spines {
    val palette: List<Long> = listOf(
        0xFF7B5EA7, 0xFF5E8FA8, 0xFF6B8F6B, 0xFFA87B5E, 0xFF8FA85E,
        0xFFA85E7B, 0xFF5EA88F, 0xFFA8A85E, 0xFF5E7BA8, 0xFF8F5EA8,
        0xFFA87B5E, 0xFF5EA87B, 0xFF8FA87B, 0xFF7BA85E, 0xFFA85E5E,
    )

    fun colorIndex(key: String): Int {
        var h = 0
        for (c in key) h = (h * 31 + c.code) and 0xFFFFFF
        return abs(h) % palette.size
    }

    fun color(key: String): Long = palette[colorIndex(key)]

    /** Spine height in dp, 120..147, stable per title. */
    fun height(title: String): Int {
        var h = 0
        for (c in title) h = (h * 7 + c.code) and 255
        return 120 + (h % 28)
    }

    fun colorFor(book: Book): Long = color(book.series.ifEmpty { book.title })
}
