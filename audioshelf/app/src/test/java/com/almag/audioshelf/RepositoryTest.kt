package com.almag.audioshelf

import com.almag.audioshelf.core.LibraryOps
import com.almag.audioshelf.core.Seed
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(dir: File = tmp.root) = LibraryRepository(dir, Dispatchers.Unconfined) { 1_000L }

    @Test fun firstLaunchSeedsTheLibrary() = runBlocking {
        val lib = repo().load()
        assertEquals(Seed.size, lib.books.size)
        assertTrue(File(tmp.root, "library.json").exists())
    }

    @Test fun changesSurviveRestart() = runBlocking {
        val r = repo()
        val lib = r.load()
        val id = lib.books.first().id
        r.set { LibraryOps.setStatus(it, id, Status.DROPPED, 5) }
        val again = repo().load()
        assertEquals(Status.DROPPED, again.book(id)!!.status)
        assertTrue(File(tmp.root, "backups").listFiles()!!.isNotEmpty())
    }

    @Test fun brokenFileFallsBackToBackup() = runBlocking {
        val r = repo()
        val lib = r.load()
        val id = lib.books.first().id
        r.set { LibraryOps.setRating(it, id, 5) } // makes a backup of the seed file
        File(tmp.root, "library.json").writeText("{not json")
        val recovered = repo().load()
        assertEquals(Seed.size, recovered.books.size)
        assertTrue(tmp.root.listFiles()!!.any { it.name.startsWith("library.broken-") })
    }

    @Test fun resetKeepsSettings() = runBlocking {
        val r = repo()
        r.load()
        r.set { it.copy(settings = it.settings.copy(yearlyGoal = 30), books = it.books.drop(10)) }
        r.resetToSeed()
        val lib = r.library.value!!
        assertEquals(Seed.size, lib.books.size)
        assertEquals(30, lib.settings.yearlyGoal)
    }
}
