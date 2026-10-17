package com.almag.audioshelf.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextTest {
    @Test fun plural() {
        assertEquals("1 книга", Text.books(1))
        assertEquals("3 книги", Text.books(3))
        assertEquals("5 книг", Text.books(5))
        assertEquals("11 книг", Text.books(11))
        assertEquals("21 книга", Text.books(21))
        assertEquals("112 книг", Text.books(112))
        assertEquals("134 книги", Text.books(134))
        assertEquals("0 авторов", Text.authors(0))
        assertEquals("22 серии", Text.series(22))
    }

    @Test fun normalizeAndMatch() {
        assertEquals("ежик в тумане", Text.normalize("  Ёжик   в ТУМАНЕ "))
        assertTrue(Text.matches("ёж", "Ежик"))
        assertTrue(Text.matches("", "что угодно"))
        assertTrue(Text.matches("сугра", "Дисгардиум", "Данияр Сугралинов"))
        assertFalse(Text.matches("пылаев", "Дисгардиум", "Данияр Сугралинов"))
    }

    @Test fun initials() {
        assertEquals("ДС", Text.initials("Данияр Сугралинов"))
        assertEquals("LE", Text.initials("Leach23"))
        assertEquals("ГЕ", Text.initials("Гедеон"))
        assertEquals("?", Text.initials("  "))
    }

    @Test fun guessVolume() {
        assertEquals(12, Text.guessVolume("Том 12"))
        assertEquals(1, Text.guessVolume("Похоже, я доигрался. Том первый"))
        assertEquals(3, Text.guessVolume("Похоже, он доигрался. Том третий"))
        assertEquals(4, Text.guessVolume("Похоже, игра подходит к концу. Том четвертый"))
        assertEquals(2, Text.guessVolume("Господин наследник, вы всё ещё приёмный. Том 2"))
        assertEquals(2, Text.guessVolume("Похоже, я шизофреник, пожирающий магию. Том 2"))
        assertEquals(3, Text.guessVolume("Книга 3"))
        assertEquals(5, Text.guessVolume("Пятый том"))
        assertEquals(7, Text.guessVolume("Level up 7"))
        assertNull(Text.guessVolume("Обратный отсчет"))
        assertNull(Text.guessVolume("Господин наследник, теперь вы не приемный"))
    }

    @Test fun spineColorsMatchWebVersion() {
        // Values computed with the web version's JS spineColor().
        assertEquals(0xFF5E7BA8, Spines.color("Дисгардиум"))
        assertEquals(0xFFA87B5E, Spines.color("Leach23"))
        assertEquals(0xFF8F5EA8, Spines.color("Игрок, забравшийся на вершину"))
        assertEquals(137, Spines.height("Том 1"))
        assertEquals(135, Spines.height("Обратный отсчет"))
    }
}
