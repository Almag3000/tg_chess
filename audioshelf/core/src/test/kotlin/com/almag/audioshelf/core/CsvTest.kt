package com.almag.audioshelf.core

import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CsvTest {
    private val zone = ZoneOffset.UTC
    private val ids = Ids()

    @Test fun parseQuotedFields() {
        val rows = Csv.parse("﻿a,b,c\r\n\"x, y\",\"он сказал \"\"привет\"\"\",\"две\nстроки\"\n\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("x, y", "он сказал \"привет\"", "две\nстроки")), rows)
    }

    @Test fun parseSemicolonExcel() {
        val rows = Csv.parse("Название;Автор\nКнига;Кто-то\n")
        assertEquals(listOf("Книга", "Кто-то"), rows[1])
    }

    @Test fun importWebVersionExport() {
        // Exactly what the web version's exportCSV() produced.
        val web = "﻿\"Название\",\"Серия\",\"Автор\",\"Статус\",\"Заметка\"\n" +
            "\"Том 12\",\"Игрок, забравшийся на вершину\",\"Leach23\",\"Прочитано\",\"\"\n" +
            "\"Новая книга\",\"\",\"Новый автор\",\"Хочу прочитать\",\"посоветовали\"\n" +
            "\"Срыв\",\"Играть, чтобы жить\",\"Дмитрий Рус\",\"Прочитано\",\"\"\n"
        val seed = seedLibrary()
        val r = Csv.import(seed, web, 5, zone, ids::next)
        assertEquals(1, r.added)
        assertEquals(1, r.updated) // Том 12 went from "слушаю" to "прослушано"
        assertEquals(1, r.skipped) // Срыв unchanged
        val igrok12 = r.library.books.first { it.series == "Игрок, забравшийся на вершину" && it.title == "Том 12" }
        assertEquals(Status.LISTENED, igrok12.status)
        val nb = r.library.books.first()
        assertEquals("Новая книга", nb.title)
        assertEquals(Status.WANT, nb.status)
        assertEquals("посоветовали", nb.note)
        assertTrue(r.library.authors.contains("Новый автор"))
        assertEquals("Импорт: добавлено 1, обновлено 1, пропущено 1", r.message)
    }

    @Test fun roundTrip() {
        var lib = seedLibrary()
        lib = LibraryOps.setRating(lib, lib.books[0].id, 4)
        lib = LibraryOps.setStatus(lib, lib.books[1].id, Status.LISTENING, 86_400_000L * 400)
        lib = LibraryOps.setProgress(lib, lib.books[1].id, 37, 0)
        val text = Csv.export(lib, zone)
        val back = Csv.import(Library(), text, 1, zone, ids::next)
        assertEquals(135, back.added)
        val pairs = lib.books.zip(back.library.books)
        for ((a, b) in pairs) {
            assertEquals(a.title, b.title)
            assertEquals(a.series, b.series)
            assertEquals(a.author, b.author)
            assertEquals(a.status, b.status)
            assertEquals(a.rating, b.rating)
            assertEquals(a.startedAt, b.startedAt)
        }
        assertEquals(37, back.library.books[1].progress)
        assertEquals(lib.authors.toSet(), back.library.authors.toSet())
        assertEquals(lib.series.toSet(), back.library.series.toSet())
        // importing the same file again changes nothing
        val again = Csv.import(back.library, text, 2, zone, ids::next)
        assertEquals(0, again.added)
        assertEquals(0, again.updated)
        assertEquals(135, again.skipped)
    }

    @Test fun sameTitleInDifferentSeriesIsNotADuplicate() {
        val text = "Название,Серия,Автор,Статус\nТом 1,Защита периметра,Михаил Атаманов,Прочитано\nТом 1,Темный травник,Михаил Атаманов,Брошено\n"
        val r = Csv.import(Library(), text, 1, zone, ids::next)
        assertEquals(2, r.added)
        assertEquals(Status.DROPPED, r.library.books.first { it.series == "Темный травник" }.status)
    }

    @Test fun badFiles() {
        assertFailsWith<CsvFormatException> { Csv.import(Library(), "", 0, zone, ids::next) }
        assertFailsWith<CsvFormatException> { Csv.import(Library(), "a,b\n1,2\n", 0, zone, ids::next) }
    }

    @Test fun datesInRussianFormat() {
        val r = Csv.import(Library(), "Название;Автор;Статус;Закончено\nКнига;А;Прослушано;05.03.2026\n", 0, zone, ids::next)
        val b = r.library.books.single()
        assertEquals(java.time.LocalDate.of(2026, 3, 5).atStartOfDay(zone).toInstant().toEpochMilli(), b.finishedAt)
    }
}
