package com.almag.audioshelf.data

import android.util.Log
import com.almag.audioshelf.core.Library
import com.almag.audioshelf.core.LibraryCodec
import com.almag.audioshelf.core.Seed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * The whole library lives in one JSON file. It is small (hundreds of books),
 * so every change rewrites the file atomically. Before the first write of a
 * day a copy of the previous file is kept as a backup.
 */
class LibraryRepository(
    private val dir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val file = File(dir, "library.json")
    private val backupDir = File(dir, "backups")
    private val mutex = Mutex()
    private val _library = MutableStateFlow<Library?>(null)

    /** null until the file has been read. */
    val library: StateFlow<Library?> = _library.asStateFlow()

    fun now(): Long = clock()

    fun newId(): String = UUID.randomUUID().toString().substring(0, 12)

    suspend fun load(): Library = mutex.withLock {
        _library.value ?: withContext(io) { readOrSeed() }.also { _library.value = it }
    }

    /** Applies [transform] and saves. Returns what the transform returned alongside. */
    suspend fun <T> update(transform: (Library) -> Pair<Library, T>): T = mutex.withLock {
        val current = _library.value ?: withContext(io) { readOrSeed() }
        val (next, result) = transform(current)
        if (next != current) {
            withContext(io) { write(next) }
        }
        _library.value = next
        result
    }

    suspend fun set(transform: (Library) -> Library) = update { transform(it) to Unit }

    suspend fun resetToSeed() = set { Seed.library(clock(), ::newId).copy(settings = it.settings) }

    private fun readOrSeed(): Library {
        if (file.exists()) {
            try {
                return LibraryCodec.decode(file.readText())
            } catch (e: Exception) {
                Log.e(TAG, "library.json is unreadable, keeping it aside", e)
                file.copyTo(File(dir, "library.broken-${clock()}.json"), overwrite = true)
                latestBackup()?.let { backup ->
                    runCatching { return LibraryCodec.decode(backup.readText()) }
                }
            }
        }
        val seeded = Seed.library(clock(), ::newId)
        write(seeded)
        return seeded
    }

    private fun write(lib: Library) {
        dir.mkdirs()
        if (file.exists()) dailyBackup()
        val tmp = File(dir, "library.json.tmp")
        tmp.writeText(LibraryCodec.encode(lib))
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Не удалось сохранить библиотеку" }
        }
    }

    private fun dailyBackup() {
        backupDir.mkdirs()
        val today = File(backupDir, "library-${LocalDate.now()}.json")
        if (!today.exists()) file.copyTo(today)
        backupDir.listFiles()?.sortedByDescending { it.name }?.drop(KEEP_BACKUPS)?.forEach { it.delete() }
    }

    private fun latestBackup(): File? = backupDir.listFiles()?.maxByOrNull { it.name }

    companion object {
        private const val TAG = "LibraryRepository"
        private const val KEEP_BACKUPS = 10
    }
}
