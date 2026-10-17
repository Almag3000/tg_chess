package com.almag.audioshelf.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almag.audioshelf.core.BookDraft
import com.almag.audioshelf.core.Csv
import com.almag.audioshelf.core.CsvFormatException
import com.almag.audioshelf.core.EditResult
import com.almag.audioshelf.core.Filter
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.LibraryOps
import com.almag.audioshelf.core.ShelfView
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** Which sheet or dialog is open on top of the tabs. */
sealed interface Sheet {
    data object None : Sheet
    data class Detail(val bookId: String) : Sheet
    data class Editor(val bookId: String?, val presetSeries: String = "", val presetAuthor: String = "") : Sheet
    data object AddAuthor : Sheet
    data class AddSeries(val presetAuthor: String = "") : Sheet
    data class RenameAuthor(val name: String) : Sheet
    data class RenameSeries(val name: String) : Sheet
}

enum class Page(val title: String) {
    HOME("Главная"),
    SHELF("Полка"),
    SERIES("Серии"),
    AUTHORS("Авторы"),
    MORE("Ещё"),
}

class ShelfViewModel(
    private val repo: LibraryRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    val library: StateFlow<Library?> = repo.library

    private val _filter = MutableStateFlow(Filter.ALL)
    val filter: StateFlow<Filter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _seriesQuery = MutableStateFlow("")
    val seriesQuery: StateFlow<String> = _seriesQuery.asStateFlow()

    private val _sheet = MutableStateFlow<Sheet>(Sheet.None)
    val sheet: StateFlow<Sheet> = _sheet.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _pages = MutableSharedFlow<Page>(extraBufferCapacity = 4)
    /** Requests to switch tab, e.g. "все →" on the home page. */
    val pages: SharedFlow<Page> = _pages.asSharedFlow()

    private val _focusSearch = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusSearch: SharedFlow<Unit> = _focusSearch.asSharedFlow()

    init {
        viewModelScope.launch { repo.load() }
    }

    fun zone(): ZoneId = zone
    fun now(): Long = repo.now()

    // ── navigation and filters ─────────────────────────────────────────

    fun go(page: Page) { _pages.tryEmit(page) }

    fun setFilter(f: Filter) { _filter.value = f }
    fun setQuery(q: String) { _query.value = q }
    fun setSeriesQuery(q: String) { _seriesQuery.value = q }

    fun showFiltered(f: Filter) {
        _filter.value = f
        _query.value = ""
        go(Page.SHELF)
    }

    /** Series or author card tapped: show their books on the shelf. */
    fun showOnShelf(text: String) {
        _filter.value = Filter.ALL
        _query.value = text
        go(Page.SHELF)
    }

    fun openSearch() {
        go(Page.SHELF)
        _focusSearch.tryEmit(Unit)
    }

    fun open(sheet: Sheet) { _sheet.value = sheet }
    fun close() { _sheet.value = Sheet.None }

    fun setShelfView(view: ShelfView) = launchSet { it.copy(settings = it.settings.copy(shelfView = view)) }
    fun setYearlyGoal(goal: Int) = launchSet { it.copy(settings = it.settings.copy(yearlyGoal = goal.coerceIn(0, 999))) }

    // ── books ──────────────────────────────────────────────────────────

    fun saveBook(bookId: String?, draft: BookDraft) {
        viewModelScope.launch {
            val result = repo.update { lib ->
                when (val r = LibraryOps.save(lib, bookId, draft, repo.now(), repo::newId)) {
                    is EditResult.Saved -> r.library to r
                    is EditResult.Invalid -> lib to r
                }
            }
            when (result) {
                is EditResult.Saved -> {
                    _sheet.value = if (bookId == null) Sheet.None else Sheet.Detail(result.bookId)
                    say(result.message)
                }
                is EditResult.Invalid -> say(result.message)
            }
        }
    }

    fun setStatus(bookId: String, status: Status) = launchSet { LibraryOps.setStatus(it, bookId, status, repo.now()) }
    fun setRating(bookId: String, rating: Int) = launchSet { LibraryOps.setRating(it, bookId, rating) }
    fun setProgress(bookId: String, progress: Int) = launchSet { LibraryOps.setProgress(it, bookId, progress, repo.now()) }

    fun startListening(bookId: String) {
        setStatus(bookId, Status.LISTENING)
        say("Отмечено: слушаю")
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            val r = repo.update { lib -> LibraryOps.delete(lib, bookId).let { it.library to it } }
            _sheet.value = Sheet.None
            say(r.message)
        }
    }

    // ── authors and series ─────────────────────────────────────────────

    fun addAuthor(name: String) = launchChecked("Автор добавлен") { LibraryOps.addAuthor(it, name) }
    fun addSeries(name: String, author: String) = launchChecked("Серия добавлена") { LibraryOps.addSeries(it, name, author) }
    fun renameAuthor(from: String, to: String) = launchChecked("Автор переименован") { LibraryOps.renameAuthor(it, from, to) }
    fun renameSeries(from: String, to: String) = launchChecked("Серия переименована") { LibraryOps.renameSeries(it, from, to) }

    // ── data ───────────────────────────────────────────────────────────

    fun exportCsv(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val lib = repo.load()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    resolver.openOutputStream(uri, "wt")!!.use { it.write(Csv.export(lib, zone).toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            say(if (ok) "CSV сохранён · ${lib.books.size} книг" else "Не удалось сохранить файл")
        }
    }

    fun importCsv(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            }
            if (text == null) {
                say("Не удалось прочитать файл")
                return@launch
            }
            importCsvText(text)
        }
    }

    fun importCsvText(text: String) {
        viewModelScope.launch {
            val message = repo.update { lib ->
                try {
                    val r = Csv.import(lib, text, repo.now(), zone, repo::newId)
                    r.library to r.message
                } catch (e: CsvFormatException) {
                    lib to (e.message ?: "Ошибка чтения файла")
                }
            }
            say(message)
        }
    }

    fun resetToSeed() {
        viewModelScope.launch {
            repo.resetToSeed()
            _filter.value = Filter.ALL
            _query.value = ""
            say("Данные сброшены")
        }
    }

    private fun launchSet(transform: (Library) -> Library) {
        viewModelScope.launch { repo.set(transform) }
    }

    private fun launchChecked(success: String, op: (Library) -> Pair<Library, String?>) {
        viewModelScope.launch {
            val error = repo.update(op)
            if (error == null) {
                _sheet.value = Sheet.None
                say(success)
            } else {
                say(error)
            }
        }
    }

    private fun say(text: String) { _messages.tryEmit(text) }
}
