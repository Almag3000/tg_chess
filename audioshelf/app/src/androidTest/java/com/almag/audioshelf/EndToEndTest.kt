package com.almag.audioshelf

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import com.almag.audioshelf.core.Seed
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.data.LibraryRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ShelfApp
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun freshLibrary() {
        runBlocking { app.repository.resetToSeed() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("total"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun close() {
        scenario?.close()
    }

    private fun tag(t: String) = compose.onNodeWithTag(t, useUnmergedTree = true)

    private fun screenshot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400) // let animations settle before the system screenshot
        val bmp = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        TestStorage().openOutputFile("device-$name.png").use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun startsWithTheSeedLibrary() {
        tag("total").assertTextContains("${Seed.size}")
        screenshot("01-home")
        tag("tab:SHELF").performClick()
        compose.waitForIdle()
        screenshot("02-shelf")
        tag("tab:SERIES").performClick()
        compose.waitForIdle()
        screenshot("03-series")
        tag("tab:AUTHORS").performClick()
        compose.waitForIdle()
        screenshot("04-authors")
    }

    @Test fun addBookAndItIsSavedToDisk() {
        tag("fab").performClick()
        tag("field:title").performTextInput("Пятая книга")
        tag("field:series").performTextInput("Тестовая серия")
        tag("field:author").performTextInput("Тест Тестов")
        tag("status:LISTENING").performScrollTo().performClick()
        screenshot("05-editor")
        tag("save").performScrollTo().performClick()
        compose.waitUntil(5_000) { app.repository.library.value?.books?.size == Seed.size + 1 }

        // A fresh repository reads the file the app wrote.
        val fromDisk = runBlocking { LibraryRepository(app.filesDir).load() }
        val book = fromDisk.books.first()
        assertEquals("Пятая книга", book.title)
        assertEquals("Тест Тестов", book.author)
        assertEquals(Status.LISTENING, book.status)
        assertTrue(fromDisk.series.any { it.name == "Тестовая серия" })

        tag("tab:HOME").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Пятая книга")).fetchSemanticsNodes().isNotEmpty() }
        screenshot("06-home-with-new-book")
    }

    @Test fun shelfSpineOpensDetailAndChangesStatus() {
        tag("page:home").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        tag("page:shelf").assertIsDisplayed()
        val listening = app.repository.library.value!!.books.first { it.status == Status.LISTENING }
        tag("filter:LISTENING").performClick()
        compose.waitForIdle()
        tag("spine:${listening.id}").performClick()
        compose.waitForIdle()
        tag("sheet:detail").assertExists()
        screenshot("07-detail")
        compose.onNodeWithText("Готово ✓").performScrollTo().performClick()
        compose.waitUntil(5_000) { app.repository.library.value!!.book(listening.id)!!.status == Status.LISTENED }
    }

    @Test fun surviveActivityRecreation() {
        tag("fab").performClick()
        tag("field:title").performTextInput("Не потеряется")
        tag("field:author").performTextInput("Автор")
        tag("save").performScrollTo().performClick()
        compose.waitUntil(5_000) { app.repository.library.value?.books?.size == Seed.size + 1 }
        scenario!!.recreate()
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("total"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        tag("total").assertTextContains("${Seed.size + 1}")
    }
}
