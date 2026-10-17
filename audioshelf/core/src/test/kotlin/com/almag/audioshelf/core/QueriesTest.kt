package com.almag.audioshelf.core

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueriesTest {
    @Test fun shelfGroupsBySeriesInVolumeOrder() {
        val lib = seedLibrary()
        val groups = Queries.shelf(lib, Filter.ALL, "")
        assertEquals(22, groups.size)
        val dis = groups.first { it.series == "Дисгардиум" }
        assertEquals((1..14).map { "Том $it" }, dis.books.map { it.title })
        val doig = groups.first { it.series == "Похоже, я доигрался" }
        assertEquals("Похоже, я доигрался. Том первый", doig.books.first().title)
        val anti = groups.first { it.series == "Антимаг" }
        assertEquals(listOf("Том 1", "Том 2", "Одержимый", "Древние", "Изгнанник", "Клан"), anti.books.map { it.title })
    }

    @Test fun filterAndSearch() {
        val lib = seedLibrary()
        val dropped = Queries.shelf(lib, Filter.DROPPED, "")
        assertEquals(listOf("Антимаг", "Пивной барон"), dropped.map { it.series })
        assertEquals(6, dropped.first().total) // counts the whole series
        assertEquals(5, dropped.first().listened)
        val found = Queries.search(lib, Filter.ALL, "пылаев")
        assertEquals(7, found.size)
        assertEquals(1, Queries.search(lib, Filter.LISTENING, "").size)
        assertTrue(Queries.shelf(lib, Filter.WANT, "").isEmpty())
    }

    @Test fun booksWithoutSeriesGoLast() {
        val l = lib(
            book("1", "Одиночка", author = "Z"),
            book("2", "Том 1", author = "A", series = "S"),
        )
        val g = Queries.shelf(l, Filter.ALL, "")
        assertEquals(listOf("S", ""), g.map { it.series })
        assertEquals(ShelfGroup.NO_SERIES, g.last().title)
    }

    @Test fun newBookGoesAfterSeedVolumes() {
        val lib = seedLibrary(now = 1)
        val r = LibraryOps.save(lib, null, BookDraft("Победители 2", series = "Искажающие реальность"), 2) { "new" } as EditResult.Saved
        val s = Queries.series(r.library).first { it.name == "Искажающие реальность" }
        assertEquals("Победители 2", s.books.last().title)
        assertEquals("Обратный отсчет", s.books.first().title)
    }

    @Test fun seriesAndAuthorSummaries() {
        val lib = seedLibrary()
        val s = Queries.series(lib)
        val igrok = s.first { it.name == "Игрок, забравшийся на вершину" }
        assertEquals(12, igrok.total)
        assertEquals(11, igrok.listened)
        assertEquals(92, igrok.percent)
        assertTrue(s.first { it.name == "Лесовик" }.completed)
        val authors = Queries.authors(lib)
        val ata = authors.first { it.name == "Михаил Атаманов" }
        assertEquals(4, ata.seriesCount)
        assertEquals(24, ata.total)
        assertEquals(100, ata.percent)
        assertEquals(1, Queries.authors(lib, "атам").size)
    }

    @Test fun nextUp() {
        var lib = lib(
            book("1", "Том 1", series = "S", status = Status.LISTENED),
            book("2", "Том 2", series = "S", status = Status.WANT),
            book("3", "Том 3", series = "S", status = Status.WANT),
            book("4", "Том 1", series = "T", status = Status.WANT),
        )
        val n = Queries.nextUp(lib)
        assertEquals(1, n.size)
        assertEquals("2", n[0].next.id)
        lib = LibraryOps.setStatus(lib, "2", Status.LISTENING, 1)
        assertTrue(Queries.nextUp(lib).isEmpty())
    }

    @Test fun finishedByMonth() {
        val zone = ZoneOffset.UTC
        fun at(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()
        val l = lib(
            book("1", "a").copy(status = Status.LISTENED, finishedAt = at(2026, 3, 1)),
            book("2", "b").copy(status = Status.LISTENED, finishedAt = at(2026, 3, 30)),
            book("3", "c").copy(status = Status.LISTENED, finishedAt = at(2025, 3, 1)),
            book("4", "d").copy(status = Status.LISTENED),
        )
        val m = Queries.finishedByMonth(l, 2026, zone)
        assertEquals(2, m[2].count)
        assertEquals(2, Queries.finishedInYear(l, 2026, zone))
        assertEquals(1, Queries.finishedInYear(l, 2025, zone))
    }
}
