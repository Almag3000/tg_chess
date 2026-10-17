package com.almag.audioshelf.core

import kotlinx.serialization.json.Json

/** JSON storage format of the library file. */
object LibraryCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun encode(lib: Library): String = json.encodeToString(Library.serializer(), lib)

    fun decode(text: String): Library = repair(json.decodeFromString(Library.serializer(), text))

    /**
     * Makes a loaded library self-consistent: every book's author and series
     * is listed, ids are unique, values are in range.
     */
    fun repair(lib: Library): Library {
        val seen = HashSet<String>()
        val books = lib.books
            .filter { it.title.isNotBlank() && it.author.isNotBlank() }
            .filter { seen.add(it.id) }
            .map { it.copy(rating = it.rating.coerceIn(0, 5), progress = it.progress.coerceIn(0, 100)) }
        val authors = LinkedHashSet<String>()
        lib.authors.filter { it.isNotBlank() }.forEach { authors += it }
        lib.series.forEach { authors += it.author }
        books.forEach { authors += it.author }
        val series = LinkedHashMap<String, Series>()
        lib.series.filter { it.name.isNotBlank() }.forEach { series.putIfAbsent(it.name, it) }
        books.filter { it.series.isNotEmpty() }.forEach { series.putIfAbsent(it.series, Series(it.series, it.author)) }
        return lib.copy(
            version = Library.CURRENT_VERSION,
            books = books,
            authors = authors.filter { it.isNotBlank() },
            series = series.values.toList(),
        )
    }
}
