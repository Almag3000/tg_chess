package com.almag.audioshelf.core

class Ids {
    private var n = 0
    fun next(): String = "id${++n}"
}

fun seedLibrary(now: Long = 1_000L): Library {
    val ids = Ids()
    return Seed.library(now, ids::next)
}

fun lib(vararg books: Book): Library = LibraryCodec.repair(Library(books = books.toList()))

fun book(id: String, title: String, author: String = "A", series: String = "", status: Status = Status.WANT, addedAt: Long = 0) =
    Book(id = id, title = title, author = author, series = series, status = status, addedAt = addedAt)
