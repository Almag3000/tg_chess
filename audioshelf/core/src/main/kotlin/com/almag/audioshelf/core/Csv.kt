package com.almag.audioshelf.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ImportResult(
    val library: Library,
    val added: Int,
    val updated: Int,
    val skipped: Int,
) {
    val message: String
        get() = buildString {
            append("Импорт: добавлено $added")
            if (updated > 0) append(", обновлено $updated")
            if (skipped > 0) append(", пропущено $skipped")
        }
}

class CsvFormatException(message: String) : Exception(message)

/**
 * CSV in the format of the web version's export (Название, Серия, Автор,
 * Статус, Заметка) plus optional extra columns. Import also understands the
 * web version's file, Excel's ";"-separated files and a UTF-8 BOM.
 */
object Csv {

    private val HEADER = listOf("Название", "Серия", "Автор", "Статус", "Заметка", "Оценка", "Том", "Чтец", "Прогресс", "Начато", "Закончено")

    fun export(lib: Library, zone: ZoneId): String {
        val rows = mutableListOf(HEADER)
        for (b in lib.books) {
            rows += listOf(
                b.title,
                b.series,
                b.author,
                b.status.label,
                b.note,
                if (b.rating > 0) b.rating.toString() else "",
                b.volume?.toString().orEmpty(),
                b.narrator,
                if (b.status == Status.LISTENING) b.progress.toString() else "",
                date(b.startedAt, zone),
                date(b.finishedAt, zone),
            )
        }
        return "﻿" + rows.joinToString("\r\n") { row -> row.joinToString(",") { quote(it) } } + "\r\n"
    }

    private fun quote(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    private fun date(ms: Long?, zone: ZoneId) =
        ms?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toString() }.orEmpty()

    private val STATUS_WORDS = mapOf(
        "прочитано" to Status.LISTENED, "прослушано" to Status.LISTENED, "read" to Status.LISTENED,
        "listened" to Status.LISTENED, "готово" to Status.LISTENED,
        "читаю" to Status.LISTENING, "слушаю" to Status.LISTENING, "в процессе" to Status.LISTENING,
        "reading" to Status.LISTENING, "listening" to Status.LISTENING, "prog" to Status.LISTENING,
        "брошено" to Status.DROPPED, "dropped" to Status.DROPPED, "drop" to Status.DROPPED,
        "хочу" to Status.WANT, "хочу прочитать" to Status.WANT, "хочу послушать" to Status.WANT,
        "want" to Status.WANT, "в планах" to Status.WANT,
    )

    fun parseStatus(raw: String): Status? = STATUS_WORDS[Text.normalize(raw)]

    /** Splits CSV text into rows; handles quotes, doubled quotes and newlines inside quotes. */
    fun parse(text: String): List<List<String>> {
        val src = text.removePrefix("﻿")
        val firstLine = src.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (countOutsideQuotes(firstLine, ';') > countOutsideQuotes(firstLine, ',')) ';' else ','
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                inQuotes && c == '"' && i + 1 < src.length && src[i + 1] == '"' -> { cell.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == delimiter -> { row.add(cell.toString().trim()); cell.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < src.length && src[i + 1] == '\n') i++
                    row.add(cell.toString().trim()); cell.clear()
                    if (row.any { it.isNotEmpty() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        row.add(cell.toString().trim())
        if (row.any { it.isNotEmpty() }) rows.add(row)
        return rows
    }

    private fun countOutsideQuotes(line: String, ch: Char): Int {
        var n = 0
        var q = false
        for (c in line) {
            if (c == '"') q = !q else if (c == ch && !q) n++
        }
        return n
    }

    fun import(lib: Library, text: String, now: Long, zone: ZoneId, newId: () -> String): ImportResult {
        val rows = parse(text)
        if (rows.size < 2) throw CsvFormatException("Файл пустой")
        val header = rows[0].map { Text.normalize(it) }
        fun col(vararg keys: String) = header.indexOfFirst { h -> keys.any { k -> h.contains(k) } }
        val iTitle = col("назван", "title")
        val iSeries = col("сери", "series")
        val iAuthor = col("автор", "author")
        val iStatus = col("стат", "status")
        val iNote = col("замет", "note")
        val iRating = col("оцен", "rating")
        val iVolume = header.indexOfFirst { it == "том" || it == "volume" || it == "№" }
        val iNarrator = col("чтец", "narrator", "исполн")
        val iProgress = col("прогресс", "progress")
        val iStarted = col("начат", "started")
        val iFinished = col("закончен", "заверш", "finished")
        if (iTitle < 0 || iAuthor < 0) throw CsvFormatException("Нет колонок «Название» и «Автор»")

        var library = lib
        var added = 0
        var updated = 0
        var skipped = 0
        // New books are prepended in file order reversed, so the file's order is kept on top.
        val newBooks = mutableListOf<Book>()

        for (r in rows.drop(1)) {
            fun get(i: Int) = if (i in r.indices) r[i].trim() else ""
            val title = get(iTitle)
            val author = get(iAuthor)
            if (title.isEmpty() || author.isEmpty()) { skipped++; continue }
            val series = get(iSeries)
            val status = parseStatus(get(iStatus)) ?: if (iStatus >= 0 && get(iStatus).isNotEmpty()) Status.WANT else Status.LISTENED
            val rating = get(iRating).toIntOrNull()?.coerceIn(0, 5)
            val volume = get(iVolume).toIntOrNull()?.takeIf { it > 0 }
            val progress = get(iProgress).removeSuffix("%").trim().toIntOrNull()?.coerceIn(0, 100)
            val started = parseDate(get(iStarted), zone)
            val finished = parseDate(get(iFinished), zone)

            if (library.authors.none { it == author }) library = library.copy(authors = library.authors + author)
            if (series.isNotEmpty() && library.series.none { it.name == series }) {
                library = library.copy(series = library.series + Series(series, author))
            }

            val key = Triple(Text.normalize(title), Text.normalize(author), Text.normalize(series))
            val existing = (library.books + newBooks).firstOrNull {
                Triple(Text.normalize(it.title), Text.normalize(it.author), Text.normalize(it.series)) == key
            }
            if (existing != null) {
                val merged = existing.copy(
                    status = status,
                    note = if (iNote >= 0) get(iNote) else existing.note,
                    rating = rating ?: existing.rating,
                    volume = volume ?: existing.volume,
                    narrator = if (iNarrator >= 0 && get(iNarrator).isNotEmpty()) get(iNarrator) else existing.narrator,
                    progress = when {
                        status == Status.LISTENED -> 100
                        progress != null -> progress
                        status != existing.status -> 0
                        else -> existing.progress
                    },
                    startedAt = started ?: existing.startedAt,
                    finishedAt = finished ?: if (status == Status.LISTENED) existing.finishedAt else null,
                )
                if (merged == existing) { skipped++; continue }
                if (newBooks.any { it.id == existing.id }) {
                    newBooks.replaceAll { if (it.id == existing.id) merged else it }
                } else {
                    library = library.copy(books = library.books.map { if (it.id == existing.id) merged else it })
                    updated++
                }
                continue
            }
            newBooks += Book(
                id = newId(),
                title = title,
                author = author,
                series = series,
                status = status,
                note = get(iNote),
                rating = rating ?: 0,
                volume = volume,
                narrator = get(iNarrator),
                progress = if (status == Status.LISTENED) 100 else if (status == Status.LISTENING) progress ?: 0 else 0,
                addedAt = now,
                startedAt = started,
                finishedAt = if (status == Status.LISTENED) finished else null,
            )
            added++
        }
        library = library.copy(books = newBooks + library.books)
        return ImportResult(library, added, updated, skipped)
    }

    private fun parseDate(s: String, zone: ZoneId): Long? {
        if (s.isBlank()) return null
        val iso = runCatching { LocalDate.parse(s.trim()) }.getOrNull()
        val ru = runCatching {
            val (d, m, y) = s.trim().split('.').map { it.toInt() }
            LocalDate.of(y, m, d)
        }.getOrNull()
        return (iso ?: ru)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    }
}
