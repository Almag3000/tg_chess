package com.almag.audioshelf.core

/** What the add/edit form submits. */
data class BookDraft(
    val title: String,
    /** Existing series name, a new series name, or "" for none. */
    val series: String = "",
    /** Existing author, a new author, or "" to take the series' author. */
    val author: String = "",
    val status: Status = Status.WANT,
    val note: String = "",
    val rating: Int = 0,
    val volume: Int? = null,
    val progress: Int = 0,
    val narrator: String = "",
)

sealed interface EditResult {
    data class Saved(
        val library: Library,
        val bookId: String,
        val created: Boolean,
        val newAuthor: String? = null,
        val newSeries: String? = null,
    ) : EditResult {
        /** Toast text, same wording style as the web version. */
        val message: String
            get() {
                val extra = when {
                    newAuthor != null && newSeries != null -> " (+ автор и серия)"
                    newAuthor != null -> " (+ новый автор)"
                    newSeries != null -> " (+ новая серия)"
                    else -> ""
                }
                return if (created) "Добавлено$extra" else "Сохранено$extra"
            }
    }

    data class Invalid(val message: String) : EditResult
}

data class DeleteResult(
    val library: Library,
    val removedSeries: String? = null,
    val removedAuthor: String? = null,
) {
    val message: String
        get() {
            val cleaned = listOfNotNull(
                removedSeries?.let { "серия «$it»" },
                removedAuthor?.let { "автор «$it»" },
            )
            return if (cleaned.isEmpty()) "Книга удалена" else "Удалено · также убран " + cleaned.joinToString(" и ")
        }
}

object LibraryOps {

    /** Adds a book ([bookId] == null) or replaces an existing one. */
    fun save(
        library: Library,
        bookId: String?,
        draft: BookDraft,
        now: Long,
        newId: () -> String,
    ): EditResult {
        val title = draft.title.trim()
        if (title.isEmpty()) return EditResult.Invalid("Введи название книги")
        val seriesName = draft.series.trim()
        val existingSeries = if (seriesName.isEmpty()) null else library.seriesNamed(seriesName)
        val author = draft.author.trim().ifEmpty { existingSeries?.author.orEmpty() }
        if (author.isEmpty()) {
            return EditResult.Invalid(
                if (seriesName.isNotEmpty()) "Сначала выбери или впиши автора для серии" else "Выбери автора или серию",
            )
        }
        val old = bookId?.let { library.book(it) ?: return EditResult.Invalid("Книга не найдена") }

        val newAuthor = author.takeIf { a -> library.authors.none { it == a } }
        val newSeries = seriesName.takeIf { it.isNotEmpty() && existingSeries == null }

        val book = applyStatusDates(
            old = old,
            next = Book(
                id = old?.id ?: newId(),
                title = title,
                author = author,
                series = seriesName,
                status = draft.status,
                note = draft.note.trim(),
                rating = draft.rating.coerceIn(0, 5),
                volume = draft.volume?.takeIf { it > 0 },
                progress = draft.progress.coerceIn(0, 100),
                narrator = draft.narrator.trim(),
                addedAt = old?.addedAt ?: now,
                startedAt = old?.startedAt,
                finishedAt = old?.finishedAt,
            ),
            now = now,
        )

        val books = if (old == null) listOf(book) + library.books else library.books.map { if (it.id == book.id) book else it }
        var result = library.copy(
            books = books,
            authors = if (newAuthor != null) library.authors + newAuthor else library.authors,
            series = if (newSeries != null) library.series + Series(newSeries, author) else library.series,
        )
        // Editing may leave the previous series/author unused; tidy them like delete does.
        if (old != null) result = cleanup(result, old.series, old.author).library
        return EditResult.Saved(result, book.id, created = old == null, newAuthor = newAuthor, newSeries = newSeries)
    }

    fun setStatus(library: Library, bookId: String, status: Status, now: Long): Library =
        updateBook(library, bookId) { applyStatusDates(it, it.copy(status = status), now) }

    fun setRating(library: Library, bookId: String, rating: Int): Library =
        updateBook(library, bookId) { it.copy(rating = rating.coerceIn(0, 5)) }

    fun setProgress(library: Library, bookId: String, progress: Int, now: Long): Library =
        updateBook(library, bookId) { book ->
            val p = progress.coerceIn(0, 100)
            when {
                p >= 100 -> applyStatusDates(book, book.copy(progress = 100, status = Status.LISTENED), now)
                book.status == Status.WANT && p > 0 -> applyStatusDates(book, book.copy(progress = p, status = Status.LISTENING), now)
                else -> book.copy(progress = p)
            }
        }

    fun delete(library: Library, bookId: String): DeleteResult {
        val book = library.book(bookId) ?: return DeleteResult(library)
        val without = library.copy(books = library.books.filterNot { it.id == bookId })
        return cleanup(without, book.series, book.author)
    }

    fun addAuthor(library: Library, name: String): Pair<Library, String?> {
        val n = name.trim()
        if (n.isEmpty()) return library to "Введи имя"
        if (library.authors.any { Text.normalize(it) == Text.normalize(n) }) return library to "Автор уже есть"
        return library.copy(authors = library.authors + n) to null
    }

    fun addSeries(library: Library, name: String, author: String): Pair<Library, String?> {
        val n = name.trim()
        val a = author.trim()
        if (n.isEmpty()) return library to "Введи название"
        if (a.isEmpty()) return library to "Выбери автора"
        if (library.series.any { Text.normalize(it.name) == Text.normalize(n) }) return library to "Такая серия уже есть"
        val authors = if (library.authors.contains(a)) library.authors else library.authors + a
        return library.copy(series = library.series + Series(n, a), authors = authors) to null
    }

    /** Renames an author everywhere: author list, series and books. */
    fun renameAuthor(library: Library, from: String, to: String): Pair<Library, String?> {
        val n = to.trim()
        if (n.isEmpty()) return library to "Введи имя"
        if (n == from) return library to null
        val merged = library.authors.contains(n)
        return library.copy(
            authors = if (merged) library.authors.filterNot { it == from } else library.authors.map { if (it == from) n else it },
            series = library.series.map { if (it.author == from) it.copy(author = n) else it },
            books = library.books.map { if (it.author == from) it.copy(author = n) else it },
        ) to null
    }

    fun renameSeries(library: Library, from: String, to: String): Pair<Library, String?> {
        val n = to.trim()
        if (n.isEmpty()) return library to "Введи название"
        if (n == from) return library to null
        if (library.series.any { it.name == n }) return library to "Такая серия уже есть"
        return library.copy(
            series = library.series.map { if (it.name == from) it.copy(name = n) else it },
            books = library.books.map { if (it.series == from) it.copy(series = n) else it },
        ) to null
    }

    private fun updateBook(library: Library, bookId: String, f: (Book) -> Book): Library =
        library.copy(books = library.books.map { if (it.id == bookId) f(it) else it })

    /** Keeps start/finish dates and progress consistent with the status. */
    internal fun applyStatusDates(old: Book?, next: Book, now: Long): Book {
        val changed = old?.status != next.status
        return when (next.status) {
            Status.LISTENING -> next.copy(
                startedAt = next.startedAt ?: now,
                finishedAt = if (changed) null else next.finishedAt,
                progress = if (changed && old?.status == Status.LISTENED) 0 else next.progress.coerceAtMost(99),
            )
            Status.LISTENED -> next.copy(
                // Books marked listened before dates were tracked stay undated.
                finishedAt = if (changed) now else next.finishedAt,
                progress = 100,
            )
            Status.WANT -> next.copy(
                finishedAt = if (changed) null else next.finishedAt,
                progress = if (changed) 0 else next.progress,
            )
            Status.DROPPED -> next.copy(
                finishedAt = if (changed) null else next.finishedAt,
            )
        }
    }

    private fun cleanup(library: Library, series: String, author: String): DeleteResult {
        var lib = library
        var removedSeries: String? = null
        var removedAuthor: String? = null
        if (series.isNotEmpty() && lib.books.none { it.series == series } && lib.series.any { it.name == series }) {
            lib = lib.copy(series = lib.series.filterNot { it.name == series })
            removedSeries = series
        }
        if (author.isNotEmpty() &&
            lib.books.none { it.author == author } &&
            lib.series.none { it.author == author } &&
            lib.authors.contains(author)
        ) {
            lib = lib.copy(authors = lib.authors.filterNot { it == author })
            removedAuthor = author
        }
        return DeleteResult(lib, removedSeries, removedAuthor)
    }
}
