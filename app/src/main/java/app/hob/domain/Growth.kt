package app.hob.domain

import app.hob.data.Kid
import app.hob.data.Measure
import app.hob.data.Size
import app.hob.data.Thing
import kotlin.math.floor

/**
 * How children grow, roughly: a median height and shoe size by age (boys and girls averaged, WHO height curves), and
 * the child's own pace once there are two measurements far enough apart. Good enough to say «in about two months»,
 * which is all the reminders need.
 */
object Growth {
    /** Median height in cm at whole years 0–18. */
    private val HEIGHT = floatArrayOf(50f, 75f, 87f, 96f, 103f, 110f, 116f, 122f, 128f, 133f, 138f, 144f, 150f, 157f, 163f, 167f, 170f, 172f, 173f)
    /** Typical EU shoe size at whole years 0–18. */
    private val FOOT = floatArrayOf(16f, 20f, 23f, 25f, 27f, 29f, 30.5f, 32f, 33f, 34.5f, 35.5f, 36.5f, 37.5f, 38.5f, 39.5f, 40f, 41f, 41.5f, 42f)

    private fun table(kind: Int) = if (kind == Size.FOOT) FOOT else HEIGHT

    /** The table value at [age] years, linearly between whole years. */
    fun median(kind: Int, age: Float): Float {
        val t = table(kind)
        val a = age.coerceIn(0f, (t.size - 1).toFloat())
        val i = floor(a).toInt().coerceAtMost(t.size - 2)
        return t[i] + (t[i + 1] - t[i]) * (a - i)
    }

    /** Growth per year at [age] by the table. */
    fun pace(kind: Int, age: Float): Float = (median(kind, age + 0.5f) - median(kind, age - 0.5f).coerceAtLeast(0f)).coerceAtLeast(0f)

    fun age(kid: Kid, day: Long): Float = (day - kid.born) / 365.25f

    private fun points(kind: Int, ms: List<Measure>): List<Pair<Long, Float>> =
        ms.mapNotNull { m -> val v = if (kind == Size.FOOT) m.foot else m.height; if (v > 0f) m.day to v else null }.sortedBy { it.first }

    /** The child's own pace (per year) from the last measurement and the latest one at least four months before it. */
    private fun ownPace(kind: Int, pts: List<Pair<Long, Float>>, kid: Kid): Float? {
        if (pts.size < 2) return null
        val last = pts.last()
        val prev = pts.lastOrNull { last.first - it.first >= 120 } ?: return null
        val years = (last.first - prev.first) / 365.25f
        val own = (last.second - prev.second) / years
        val norm = pace(kind, age(kid, last.first))
        return own.coerceIn(norm * 0.4f, norm * 2f)
    }

    /** The child's size of [kind] on [day]: grown from the last measurement, or the median for the age if never measured. */
    fun estimate(kind: Int, kid: Kid, ms: List<Measure>, day: Long): Float {
        val pts = points(kind, ms.filter { it.kid == kid.id })
        val last = pts.lastOrNull() ?: return median(kind, age(kid, day))
        val own = ownPace(kind, pts, kid)
        // Step through the years so the pace slows down with age, as it does.
        var v = last.second
        var d = last.first
        while (d < day) {
            val step = minOf(30L, day - d)
            val p = own ?: pace(kind, age(kid, d))
            v += p * step / 365.25f
            d += step
        }
        return v
    }

    /** True when the child was measured for [kind] at all (otherwise the estimate is only the age median). */
    fun measured(kind: Int, kid: Kid, ms: List<Measure>) = points(kind, ms.filter { it.kid == kid.id }).isNotEmpty()

    /**
     * Days until [thing] fits [kid]: 0 when it fits now, null when the child has outgrown it already (or won't reach
     * it within six years).
     */
    fun daysUntil(thing: Thing, kid: Kid, ms: List<Measure>, today: Long): Int? {
        if (!thing.sized) return null
        val now = estimate(thing.sizeKind, kid, ms, today)
        if (now > thing.sizeMax) return null
        if (now >= thing.sizeMin) return 0
        var d = 7
        while (d <= 6 * 366) {
            val v = estimate(thing.sizeKind, kid, ms, today + d)
            if (v >= thing.sizeMin) return if (v > thing.sizeMax) null else d
            d += 7
        }
        return null
    }

    /** Who of [kids] it fits soonest, and in how many days. */
    fun next(thing: Thing, kids: List<Kid>, ms: List<Measure>, today: Long): Pair<Kid, Int>? =
        kids.mapNotNull { k -> daysUntil(thing, k, ms, today)?.let { k to it } }.minByOrNull { it.second }

    /**
     * Reads a size label into a range: «116» (height: fits 111–116), «110–116», «29» (shoe), «30–33», «4–5 лет»
     * (height by the median for those ages). Returns (min, max) or null if there's no number.
     */
    fun parse(kind: Int, label: String): Pair<Float, Float>? {
        val nums = Regex("""\d+(?:[.,]\d+)?""").findAll(label).map { it.value.replace(',', '.').toFloat() }.toList()
        if (nums.isEmpty() || kind == Size.NONE) return null
        val a = nums.first(); val b = nums.getOrElse(1) { a }
        val years = Regex("""(лет|год|y|yr|age)""", RegexOption.IGNORE_CASE).containsMatchIn(label) || (kind == Size.HEIGHT && b < 19f)
        return when {
            kind == Size.HEIGHT && years -> median(Size.HEIGHT, a) - 2f to median(Size.HEIGHT, b + 1f) - 2f
            kind == Size.HEIGHT && nums.size == 1 -> a - 5f to a
            kind == Size.HEIGHT -> minOf(a, b) to maxOf(a, b)
            nums.size == 1 -> a - 0.5f to a + 0.5f
            else -> minOf(a, b) - 0.5f to maxOf(a, b) + 0.5f
        }
    }
}
