package com.almag.audioshelf

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.almag.audioshelf.core.Filter
import com.almag.audioshelf.core.Seed
import com.almag.audioshelf.core.ShelfView
import com.almag.audioshelf.core.Status
import com.almag.audioshelf.data.LibraryRepository
import com.almag.audioshelf.ui.Page
import com.almag.audioshelf.ui.Sheet
import com.almag.audioshelf.ui.ShelfAppUi
import com.almag.audioshelf.ui.ShelfViewModel
import com.almag.audioshelf.ui.theme.ShelfTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class UiFlowsTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var vm: ShelfViewModel
    private lateinit var repo: LibraryRepository

    @Before fun setUp() {
        repo = LibraryRepository(tmp.root, Dispatchers.Unconfined) { 1_780_000_000_000L }
        vm = ShelfViewModel(repo, ZoneOffset.UTC)
        compose.setContent { ShelfTheme { ShelfAppUi(vm) } }
        compose.waitUntil(5_000) { vm.library.value != null }
        compose.waitForIdle()
    }

    private val lib get() = vm.library.value!!

    /** Saves the whole screen, including an open sheet or dialog, as PNG for the release notes. */
    @OptIn(ExperimentalRoborazziApi::class)
    private fun shot(name: String) {
        compose.waitForIdle()
        runCatching { captureScreenRoboImage("build/robolectric-screens/$name.png") }
            .onFailure { System.err.println("SCREENSHOT FAILED $name: $it") }
    }

    private fun tag(t: String): SemanticsNodeInteraction = compose.onNodeWithTag(t, useUnmergedTree = true)

    @Test fun homeShowsTheSeedLibrary() {
        tag("total").assertTextContains("${Seed.size}")
        tag("subtitle").assertTextContains("135 книг · 14 авторов")
        compose.onNodeWithText("Сейчас слушаю".uppercase()).assertIsDisplayed()
        shot("01-home")
    }

    @Test fun everyTabRenders() {
        for (p in Page.entries) {
            tag("tab:${p.name}").performClick()
            compose.waitForIdle()
            tag("page:${p.name.lowercase()}").assertExists()
            shot("0${p.ordinal + 2}-${p.name.lowercase()}")
        }
    }

    @Test fun swipeMovesToNextTab() {
        tag("page:home").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        tag("page:shelf").assertIsDisplayed()
    }

    @Test fun shelfListViewAndFilter() {
        tag("tab:SHELF").performClick()
        tag("filter:DROPPED").performClick()
        compose.waitForIdle()
        tag("shelf:Антимаг").assertExists()
        tag("shelf:Пивной барон").assertExists()
        shot("10-shelf-dropped")
        compose.onNodeWithText("☰ Список").performClick()
        compose.waitUntil(3_000) { lib.settings.shelfView == ShelfView.LIST }
        shot("11-shelf-list")
    }

    @Test fun addBookToExistingSeries() {
        tag("fab").performClick()
        compose.waitForIdle()
        tag("sheet:editor").assertExists()
        tag("field:series").performTextInput("Дисгарди")
        compose.waitForIdle()
        compose.onNodeWithText("Дисгардиум · Данияр Сугралинов").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Том 15").performClick() // suggested next volume
        tag("status:LISTENING").performClick()
        shot("20-editor")
        tag("save").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.books.size == Seed.size + 1 }
        val added = lib.books.first()
        assertEquals("Том 15", added.title)
        assertEquals("Данияр Сугралинов", added.author)
        assertEquals(15, added.volume)
        assertEquals(Status.LISTENING, added.status)
        assertTrue(compose.onAllNodes(hasTestTag("sheet:editor")).fetchSemanticsNodes().isEmpty())
    }

    @Test fun addBookWithNewAuthorAndSeries() {
        tag("fab").performClick()
        tag("field:title").performTextInput("Первая")
        tag("field:series").performTextInput("Совсем новая серия")
        tag("field:author").performTextInput("Новый Автор")
        tag("save").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.books.size == Seed.size + 1 }
        assertTrue(lib.authors.contains("Новый Автор"))
        assertNotNull(lib.seriesNamed("Совсем новая серия"))
        compose.waitUntil(3_000) { compose.onAllNodes(hasText("Добавлено (+ автор и серия)")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun validationMessage() {
        tag("fab").performClick()
        tag("save").performScrollTo().performClick()
        compose.waitUntil(3_000) { compose.onAllNodes(hasText("Введи название книги")).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(Seed.size, lib.books.size)
    }

    @Test fun detailChangesStatusRatingAndProgress() {
        val book = lib.books.first { it.status == Status.LISTENING }
        compose.runOnIdle { vm.open(Sheet.Detail(book.id)) }
        compose.waitForIdle()
        tag("sheet:detail").assertExists()
        shot("30-detail-listening")
        compose.onNodeWithText("+10%").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.book(book.id)!!.progress == 10 }
        compose.onNodeWithText("Готово ✓").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.book(book.id)!!.status == Status.LISTENED }
        assertNotNull(lib.book(book.id)!!.finishedAt)
        compose.onNodeWithContentDescriptionCompat("4 из 5").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.book(book.id)!!.rating == 4 }
    }

    @Test fun editAndDelete() {
        val beer = lib.books.filter { it.series == "Пивной барон" }
        compose.runOnIdle { vm.open(Sheet.Editor(beer[0].id)) }
        compose.waitForIdle()
        tag("field:note").performScrollTo().performTextReplacement("Смешно")
        tag("save").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.book(beer[0].id)!!.note == "Смешно" }
        // after saving an edit the detail sheet is shown
        tag("sheet:detail").assertExists()
        tag("edit").performScrollTo().performClick()
        tag("delete").performScrollTo().performClick()
        tag("confirm").performClick()
        compose.waitUntil(3_000) { lib.book(beer[0].id) == null }
        assertEquals(Seed.size - 1, lib.books.size)
    }

    @Test fun nextUpStartsListening() {
        // Make a series where the next volume is waiting.
        val dis = lib.books.filter { it.series == "Дисгардиум" }.sortedBy { it.title.removePrefix("Том ").toInt() }
        compose.runOnIdle { for (b in dis.takeLast(3)) vm.setStatus(b.id, Status.WANT) }
        compose.waitUntil(3_000) { lib.books.count { it.series == "Дисгардиум" && it.status == Status.WANT } == 3 }
        val next = dis[11]
        tag("start:${next.id}").performScrollTo().performClick()
        compose.waitUntil(3_000) { lib.book(next.id)!!.status == Status.LISTENING }
        shot("12-home-after-start")
    }

    @Test fun seriesCardOpensShelfFilteredBySeries() {
        tag("tab:SERIES").performClick()
        compose.waitForIdle()
        tag("page:series").performScrollToNode(hasTestTag("series:Лесовик"))
        tag("series:Лесовик").performClick()
        compose.waitUntil(3_000) { vm.query.value == "Лесовик" }
        assertEquals(Filter.ALL, vm.filter.value)
        tag("shelf:Лесовик").assertExists()
    }

    @Test fun renameAuthorFromLongPress() {
        compose.runOnIdle { vm.open(Sheet.RenameAuthor("Leach23")) }
        tag("field:name").performTextReplacement("Лич")
        tag("confirm").performClick()
        compose.waitUntil(3_000) { lib.authors.contains("Лич") }
        assertEquals(12, lib.books.count { it.author == "Лич" })
    }

    @Test fun csvImportFromWebVersion() {
        val web = "﻿\"Название\",\"Серия\",\"Автор\",\"Статус\",\"Заметка\"\n" +
            "\"Том 12\",\"Игрок, забравшийся на вершину\",\"Leach23\",\"Прочитано\",\"\"\n" +
            "\"Новая\",\"\",\"Кто-то\",\"Хочу прочитать\",\"\"\n"
        compose.runOnIdle { vm.importCsvText(web) }
        compose.waitUntil(3_000) { lib.books.size == Seed.size + 1 }
        compose.waitUntil(3_000) { compose.onAllNodes(hasText("Импорт: добавлено 1, обновлено 1")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescriptionCompat(d: String) =
        onNode(androidx.compose.ui.test.hasContentDescription(d), useUnmergedTree = true)
}
