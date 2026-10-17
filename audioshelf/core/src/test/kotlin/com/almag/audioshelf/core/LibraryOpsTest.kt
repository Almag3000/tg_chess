package com.almag.audioshelf.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LibraryOpsTest {
    private val ids = Ids()

    @Test fun seedMatchesWebVersion() {
        val lib = seedLibrary()
        assertEquals(135, lib.books.size)
        assertEquals(14, lib.authors.size)
        assertEquals(22, lib.series.size)
        val o = Queries.overview(lib)
        assertEquals(1, o.listening)
        assertEquals(2, o.dropped)
        assertEquals(132, o.listened)
        assertEquals(0, o.want)
        // every book's series and author are listed
        assertTrue(lib.books.all { b -> lib.authors.contains(b.author) && lib.series.any { it.name == b.series } })
        assertEquals(lib, LibraryCodec.repair(lib))
    }

    @Test fun addBookWithNewAuthorAndSeries() {
        val r = LibraryOps.save(
            Library(), null,
            BookDraft(title = "Том 1", series = "Новая", author = "Новый Автор", status = Status.LISTENING),
            now = 50, newId = ids::next,
        )
        assertIs<EditResult.Saved>(r)
        assertTrue(r.created)
        assertEquals("Добавлено (+ автор и серия)", r.message)
        val b = r.library.book(r.bookId)!!
        assertEquals(50, b.startedAt)
        assertEquals(listOf("Новый Автор"), r.library.authors)
        assertEquals(listOf(Series("Новая", "Новый Автор")), r.library.series)
    }

    @Test fun authorIsTakenFromSeries() {
        val lib = seedLibrary()
        val r = LibraryOps.save(lib, null, BookDraft(title = "Том 15", series = "Дисгардиум"), 5, ids::next)
        assertIs<EditResult.Saved>(r)
        assertEquals("Данияр Сугралинов", r.library.book(r.bookId)!!.author)
        assertEquals("Добавлено", r.message)
        assertEquals(r.bookId, r.library.books.first().id) // newest first
    }

    @Test fun validation() {
        assertEquals(EditResult.Invalid("Введи название книги"), LibraryOps.save(Library(), null, BookDraft("  "), 0, ids::next))
        assertEquals(EditResult.Invalid("Выбери автора или серию"), LibraryOps.save(Library(), null, BookDraft("X"), 0, ids::next))
        assertEquals(
            EditResult.Invalid("Сначала выбери или впиши автора для серии"),
            LibraryOps.save(Library(), null, BookDraft("X", series = "Новая серия"), 0, ids::next),
        )
    }

    @Test fun statusDates() {
        var lib = lib(book("b", "Том 1"))
        lib = LibraryOps.setStatus(lib, "b", Status.LISTENING, 10)
        assertEquals(10, lib.book("b")!!.startedAt)
        lib = LibraryOps.setProgress(lib, "b", 40, 15)
        assertEquals(40, lib.book("b")!!.progress)
        lib = LibraryOps.setStatus(lib, "b", Status.LISTENED, 20)
        lib.book("b")!!.let {
            assertEquals(20, it.finishedAt)
            assertEquals(100, it.progress)
            assertEquals(10, it.startedAt)
        }
        lib = LibraryOps.setStatus(lib, "b", Status.WANT, 30)
        lib.book("b")!!.let {
            assertNull(it.finishedAt)
            assertEquals(0, it.progress)
        }
    }

    @Test fun progressDrivesStatus() {
        var lib = lib(book("b", "Том 1"))
        lib = LibraryOps.setProgress(lib, "b", 5, 7)
        assertEquals(Status.LISTENING, lib.book("b")!!.status)
        lib = LibraryOps.setProgress(lib, "b", 100, 9)
        assertEquals(Status.LISTENED, lib.book("b")!!.status)
        assertEquals(9, lib.book("b")!!.finishedAt)
    }

    @Test fun editingSeedBookKeepsItUndated() {
        val lib = seedLibrary()
        val b = lib.books.first { it.status == Status.LISTENED }
        val r = LibraryOps.save(lib, b.id, BookDraft(b.title, b.series, b.author, Status.LISTENED, note = "класс"), 99, ids::next)
        assertIs<EditResult.Saved>(r)
        assertNull(r.library.book(b.id)!!.finishedAt)
        assertEquals("класс", r.library.book(b.id)!!.note)
        assertEquals("Сохранено", r.message)
        assertEquals(lib.books.size, r.library.books.size)
    }

    @Test fun deleteCleansUpLastBookOfSeriesAndAuthor() {
        val lib = seedLibrary()
        val beer = lib.books.filter { it.series == "Пивной барон" }
        val first = LibraryOps.delete(lib, beer[0].id)
        assertEquals("Книга удалена", first.message)
        val second = LibraryOps.delete(first.library, beer[1].id)
        assertEquals("Пивной барон", second.removedSeries)
        assertEquals("Иван Магазинников", second.removedAuthor)
        assertEquals("Удалено · также убран серия «Пивной барон» и автор «Иван Магазинников»", second.message)
        assertNull(second.library.seriesNamed("Пивной барон"))
    }

    @Test fun deleteKeepsAuthorWithOtherSeries() {
        val lib = seedLibrary()
        val beer = lib.books.filter { it.series == "Нить миров" }
        var l = lib
        var last: DeleteResult? = null
        for (b in beer) { last = LibraryOps.delete(l, b.id); l = last.library }
        assertEquals("Нить миров", last!!.removedSeries)
        assertEquals("Дмитрий Билик", last.removedAuthor) // only series of that author
        val novikov = lib.books.filter { it.series == "Похоже, я шизофреник" }
        l = lib
        for (b in novikov) { last = LibraryOps.delete(l, b.id); l = last.library }
        assertNull(last!!.removedAuthor)
        assertTrue(l.authors.contains("Николай Новиков"))
    }

    @Test fun editMovingLastBookOutOfSeriesRemovesIt() {
        var lib = lib(book("a", "Одна", author = "X", series = "S"))
        val r = LibraryOps.save(lib, "a", BookDraft("Одна", series = "", author = "Y"), 1, ids::next)
        assertIs<EditResult.Saved>(r)
        lib = r.library
        assertNull(lib.seriesNamed("S"))
        assertEquals(listOf("Y"), lib.authors)
    }

    @Test fun addAuthorAndSeries() {
        var (l, err) = LibraryOps.addAuthor(Library(), "Иван")
        assertNull(err)
        assertEquals("Автор уже есть", LibraryOps.addAuthor(l, " иван ").second)
        val (l2, err2) = LibraryOps.addSeries(l, "Серия", "Иван")
        assertNull(err2)
        assertNotNull(l2.seriesNamed("Серия"))
        assertEquals("Выбери автора", LibraryOps.addSeries(l2, "Другая", "").second)
        l = l2
        assertEquals("Такая серия уже есть", LibraryOps.addSeries(l, "серия", "Иван").second)
    }

    @Test fun renameAuthorCascades() {
        val lib = seedLibrary()
        val (l, err) = LibraryOps.renameAuthor(lib, "Leach23", "Лич")
        assertNull(err)
        assertTrue(l.books.none { it.author == "Leach23" })
        assertEquals(12, l.books.count { it.author == "Лич" })
        assertEquals("Лич", l.seriesNamed("Игрок, забравшийся на вершину")!!.author)
        assertEquals(lib.authors.size, l.authors.size)
    }

    @Test fun renameSeriesCascades() {
        val lib = seedLibrary()
        val (l, err) = LibraryOps.renameSeries(lib, "Level up", "Level Up")
        assertNull(err)
        assertEquals(5, l.books.count { it.series == "Level Up" })
        assertEquals("Такая серия уже есть", LibraryOps.renameSeries(l, "Level Up", "Дисгардиум").second)
    }
}
