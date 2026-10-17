package com.almag.audioshelf.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Listening status. The serial names match the codes the web version
 * ("Книжная полка") stored, so data moves between the two without mapping.
 */
@Serializable
enum class Status(val label: String, val shortLabel: String) {
    @SerialName("read")
    LISTENED("Прослушано", "Прослушано"),

    @SerialName("prog")
    LISTENING("Слушаю", "Слушаю"),

    @SerialName("drop")
    DROPPED("Брошено", "Брошено"),

    @SerialName("want")
    WANT("Хочу послушать", "Хочу"),
}

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val series: String = "",
    val status: Status = Status.WANT,
    val note: String = "",
    /** 0 = no rating, otherwise 1..5. */
    val rating: Int = 0,
    /** Explicit position in the series; null means "guess from the title". */
    val volume: Int? = null,
    /** Listening progress in percent, meaningful while [Status.LISTENING]. */
    val progress: Int = 0,
    val narrator: String = "",
    val addedAt: Long = 0,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
)

@Serializable
data class Series(
    val name: String,
    val author: String,
)

@Serializable
enum class ShelfView { SHELF, LIST }

@Serializable
data class Settings(
    val shelfView: ShelfView = ShelfView.SHELF,
    val yearlyGoal: Int = 0,
)

@Serializable
data class Library(
    val version: Int = CURRENT_VERSION,
    /** Newest first, like the web version. */
    val books: List<Book> = emptyList(),
    val authors: List<String> = emptyList(),
    val series: List<Series> = emptyList(),
    val settings: Settings = Settings(),
) {
    fun book(id: String): Book? = books.firstOrNull { it.id == id }
    fun seriesNamed(name: String): Series? = series.firstOrNull { it.name == name }

    companion object {
        const val CURRENT_VERSION = 1
    }
}
