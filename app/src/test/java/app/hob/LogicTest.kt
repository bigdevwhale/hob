package app.hob

import app.hob.data.Kid
import app.hob.data.Measure
import app.hob.data.Size
import app.hob.data.Thing
import app.hob.domain.Cat
import app.hob.domain.Growth
import app.hob.domain.Guess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private val today = 20_000L
    private fun kid(years: Double) = Kid(id = 1, name = "V", born = today - (years * 365.25).toLong())
    private fun jacket(label: String, kind: Int = Size.HEIGHT): Thing {
        val r = Growth.parse(kind, label)!!
        return Thing(name = "j", emoji = "", cat = "", spot = 1, added = 0, touched = 0, sizeKind = kind, sizeMin = r.first, sizeMax = r.second, sizeLabel = label)
    }

    @Test fun parsesLabels() {
        assertEquals(111f to 116f, Growth.parse(Size.HEIGHT, "116"))
        assertEquals(110f to 116f, Growth.parse(Size.HEIGHT, "110–116"))
        assertEquals(28.5f to 29.5f, Growth.parse(Size.FOOT, "29"))
        assertEquals(29.5f to 33.5f, Growth.parse(Size.FOOT, "30-33"))
        val years = Growth.parse(Size.HEIGHT, "4–5 лет")!!
        assertTrue(years.first in 100f..104f && years.second in 113f..118f)
        assertNull(Growth.parse(Size.HEIGHT, "big"))
    }

    @Test fun fitsNowLaterOrNever() {
        val k = kid(5.4)
        val ms = listOf(Measure(kid = 1, day = today, height = 109f, foot = 28f))
        assertEquals(0, Growth.daysUntil(jacket("110"), k, ms, today))
        val later = Growth.daysUntil(jacket("116"), k, ms, today)!!
        assertTrue("a 116 fits in about four months, got $later", later in 60..160)
        assertNull(Growth.daysUntil(jacket("98"), k, ms, today))
        assertNotNull(Growth.daysUntil(jacket("30", Size.FOOT), k, ms, today))
    }

    @Test fun ownPaceFollowsMeasurements() {
        val k = kid(6.0)
        val slow = listOf(Measure(kid = 1, day = today - 365, height = 112f, foot = 0f), Measure(kid = 1, day = today, height = 115f, foot = 0f))
        val fast = listOf(Measure(kid = 1, day = today - 365, height = 110f, foot = 0f), Measure(kid = 1, day = today, height = 118f, foot = 0f))
        val a = Growth.estimate(Size.HEIGHT, k, slow, today + 365) - 115f
        val b = Growth.estimate(Size.HEIGHT, k, fast, today + 365) - 118f
        assertTrue("slow $a < fast $b", a < b)
    }

    @Test fun neverMeasuredUsesTheAge() {
        val v = Growth.estimate(Size.HEIGHT, kid(5.0), emptyList(), today)
        assertEquals(110f, v, 1f)
    }

    @Test fun guessesFromNames() {
        assertEquals(Cat.TECH, Guess.of("Удлинитель 5 м")?.cat)
        assertEquals("🧥", Guess.of("Пуховик синий")?.emoji)
        assertEquals(Size.FOOT, Guess.of("Коньки раздвижные")?.size)
        assertEquals(Cat.HOLIDAY, Guess.of("Ёлочные игрушки")?.cat)
        assertEquals("🗝️", Guess.of("Запасные ключи")?.emoji)
        assertEquals(Cat.TOOLS, Guess.of("drill")?.cat)
        assertEquals("🎈", Guess.of("balloons")?.emoji)
        assertNull(Guess.of("штуковина"))
    }
}
