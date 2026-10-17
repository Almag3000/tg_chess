package com.almag.audioshelf.core

import java.time.Instant
import java.time.ZoneId

enum class Filter(val label: String, val status: Status?) {
    ALL("Все", null),
    LISTENED("Прослушано", Status.LISTENED),
    LISTENING("Слушаю", Status.LISTENING),
    DROPPED("Брошено", Status.DROPPED),
    WANT("Хочу", Status.WANT),
}

data class ShelfGroup(
    /** Series name, or "" for books without a series. */
    val series: String,
    val author: String,
    val books: List<Book>,
    /** Listened count over the whole series, not just the filtered books. */
    val listened: Int,
    val total: Int,
) {
    val title: String get() = series.ifEmpty { NO_SERIES }

    companion object {
        const val NO_SERIES = "— Без серии —"
    }
}

data class SeriesSummary(
    val name: String,
    val author: String,
    val books: List<Book>,
) {
    val total: Int get() = books.size
    val listened: Int get() = books.count { it.status == Status.LISTENED }
    val percent: Int get() = if (total == 0) 0 else Math.round(listened * 100f / total)
    val completed: Boolean get() = total > 0 && listened == total
    val remaining: Int get() = total - listened
}

data class AuthorSummary(
    val name: String,
    val seriesCount: Int,
    val total: Int,
    val listened: Int,
    val listening: Int,
) {
    val percent: Int get() = if (total == 0) 0 else Math.round(listened * 100f / total)
}

data class Overview(
    val total: Int,
    val listened: Int,
    val listening: Int,
    val dropped: Int,
    val want: Int,
    val authors: Int,
    val series: Int,
) {
    val percent: Int get() = if (total == 0) 0 else Math.round(listened * 100f / total)
    fun count(status: Status) = when (status) {
        Status.LISTENED -> listened
        Status.LISTENING -> listening
        Status.DROPPED -> dropped
        Status.WANT -> want
    }
}

/** A series the user is part-way through, with the next volume to pick up. */
data class NextUp(
    val series: String,
    val author: String,
    val next: Book,
    val listened: Int,
    val total: Int,
)

data class MonthCount(val month: Int, val count: Int)

object Queries {

    fun overview(lib: Library) = Overview(
        total = lib.books.size,
        listened = lib.books.count { it.status == Status.LISTENED },
        listening = lib.books.count { it.status == Status.LISTENING },
        dropped = lib.books.count { it.status == Status.DROPPED },
        want = lib.books.count { it.status == Status.WANT },
        authors = lib.authors.size,
        series = lib.series.size,
    )

    fun byStatus(lib: Library, status: Status): List<Book> = lib.books.filter { it.status == status }

    /**
     * Volume order inside a series: explicit number, then number from the
     * title, then when it was added (the seed shares one timestamp, so its
     * original order is kept).
     */
    fun inSeriesOrder(books: List<Book>): List<Book> {
        // Mixed titles ("Том 1", "Одержимый", …) can't be ordered by number,
        // so the order they were added in is the best guess there.
        val allNumbered = books.all { volumeOf(it) != null }
        return books.withIndex()
            .sortedWith(
                compareBy<IndexedValue<Book>> { if (allNumbered) volumeOf(it.value) else 0 }
                    .thenBy { it.value.addedAt }
                    .thenBy { it.index },
            )
            .map { it.value }
    }

    fun volumeOf(book: Book): Int? = book.volume ?: Text.guessVolume(book.title)

    fun search(lib: Library, filter: Filter, query: String): List<Book> =
        lib.books.filter { b ->
            (filter.status == null || b.status == filter.status) &&
                Text.matches(query, b.title, b.author, b.series, b.narrator, b.note)
        }

    /** Books grouped by series for the shelf, groups in order of first appearance. */
    fun shelf(lib: Library, filter: Filter, query: String): List<ShelfGroup> {
        val found = search(lib, filter, query)
        val groups = LinkedHashMap<String, MutableList<Book>>()
        for (b in found) groups.getOrPut(b.series) { mutableListOf() }.add(b)
        return groups.map { (series, books) ->
            val all = if (series.isEmpty()) books else lib.books.filter { it.series == series }
            ShelfGroup(
                series = series,
                author = lib.seriesNamed(series)?.author ?: books.first().author,
                books = if (series.isEmpty()) books else inSeriesOrder(books),
                listened = all.count { it.status == Status.LISTENED },
                total = all.size,
            )
        }.sortedBy { it.series.isEmpty() } // books without a series go last
    }

    fun series(lib: Library, query: String = ""): List<SeriesSummary> =
        lib.series
            .filter { Text.matches(query, it.name, it.author) }
            .map { s -> SeriesSummary(s.name, s.author, inSeriesOrder(lib.books.filter { it.series == s.name })) }

    fun authors(lib: Library, query: String = ""): List<AuthorSummary> =
        lib.authors
            .filter { Text.matches(query, it) }
            .map { name ->
                val books = lib.books.filter { it.author == name }
                AuthorSummary(
                    name = name,
                    seriesCount = lib.series.count { it.author == name },
                    total = books.size,
                    listened = books.count { it.status == Status.LISTENED },
                    listening = books.count { it.status == Status.LISTENING },
                )
            }

    /**
     * Series to continue: something already listened, nothing in progress,
     * and the first unlistened volume is waiting in "want".
     */
    fun nextUp(lib: Library): List<NextUp> =
        series(lib).mapNotNull { s ->
            if (s.books.none { it.status == Status.LISTENED }) return@mapNotNull null
            if (s.books.any { it.status == Status.LISTENING }) return@mapNotNull null
            val next = s.books.firstOrNull { it.status != Status.LISTENED } ?: return@mapNotNull null
            if (next.status != Status.WANT) return@mapNotNull null
            NextUp(s.name, s.author, next, s.listened, s.total)
        }

    /** Listened books per month of [year], months 1..12 (only books with a finish date). */
    fun finishedByMonth(lib: Library, year: Int, zone: ZoneId): List<MonthCount> {
        val counts = IntArray(12)
        for (b in lib.books) {
            val at = b.finishedAt ?: continue
            if (b.status != Status.LISTENED) continue
            val d = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
            if (d.year == year) counts[d.monthValue - 1]++
        }
        return counts.mapIndexed { i, c -> MonthCount(i + 1, c) }
    }

    fun finishedInYear(lib: Library, year: Int, zone: ZoneId): Int =
        finishedByMonth(lib, year, zone).sumOf { it.count }

    /** Distinct series names, for the picker: grouped by author. */
    fun seriesOf(lib: Library, author: String): List<Series> = lib.series.filter { it.author == author }
}
