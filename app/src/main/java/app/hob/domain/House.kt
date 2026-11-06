package app.hob.domain

import app.hob.data.Borrowed
import app.hob.data.Box
import app.hob.data.Ev
import app.hob.data.Event
import app.hob.data.Kid
import app.hob.data.Measure
import app.hob.data.Place
import app.hob.data.Room
import app.hob.data.Settings
import app.hob.data.Spot
import app.hob.data.Thing
import java.time.LocalDate
import kotlin.math.ln
import kotlin.math.roundToInt

fun today(): Long = LocalDate.now().toEpochDay()

/** Everything the screens read, with the lookups they need. */
class House(
    val s: Settings,
    val places: List<Place>,
    val rooms: List<Room>,
    val spots: List<Spot>,
    val boxes: List<Box>,
    val things: List<Thing>,
    val kids: List<Kid>,
    val measures: List<Measure>,
    val events: List<Event>,
    val borrowed: List<Borrowed>,
    val day: Long = today(),
) {
    val placeOf = places.associateBy { it.id }
    val roomOf = rooms.associateBy { it.id }
    val spotOf = spots.associateBy { it.id }
    val boxOf = boxes.associateBy { it.id }
    val kidOf = kids.associateBy { it.id }
    val home = things.filter { it.home }
    private val inSpot = home.groupBy { it.spot }

    fun thing(id: Long) = things.firstOrNull { it.id == id }
    fun roomOfSpot(spot: Long) = spotOf[spot]?.let { roomOf[it.room] }
    fun roomOf(t: Thing) = roomOfSpot(t.spot)
    fun placeOf(r: Room) = placeOf[r.place]
    fun spotsIn(room: Long) = spots.filter { it.room == room }
    fun thingsIn(room: Long): List<Thing> = spotsIn(room).flatMap { inSpot[it.id].orEmpty() }
    fun thingsAt(spot: Long) = inSpot[spot].orEmpty()
    fun boxesAt(spot: Long) = boxes.filter { it.spot == spot }
    fun thingsInBox(box: Long) = home.filter { it.box == box }
    fun roomsIn(place: Long) = rooms.filter { it.place == place }
    fun events(thing: Long) = events.filter { it.thing == thing }

    /** A room is lit once something in it is written down. */
    fun lit(room: Long) = thingsIn(room).isNotEmpty() || spotsIn(room).any { s -> boxes.any { it.spot == s.id } }

    /** The child already wore it (now or before): it waits for someone else. */
    fun wore(t: Thing, k: Kid) = events(t.id).any { (it.kind == Ev.WORN || it.kind == Ev.PAST) && it.who == k.name }
    fun next(t: Thing) = if (t.home && t.sized) Growth.next(t, kids.filter { !wore(t, it) }, measures, day) else null
    fun days(t: Thing, k: Kid) = if (wore(t, k)) null else Growth.daysUntil(t, k, measures, day)

    /** Kept a long time without being touched, and not obviously kept on purpose. */
    fun dusty(): List<Thing> = home.filter { t ->
        day - t.touched >= 365 && day - t.kept >= 365 && t.cat != Cat.DOCS.key && t.season == 0 && !t.lent && next(t) == null
    }.sortedBy { it.touched }

    fun lentOut() = home.filter { it.lent }.sortedBy { it.lentDay }
    fun overdue() = lentOut().filter { it.lentRemind in 1..day }
    fun stillHere() = borrowed.filter { it.back == 0L }

    /** Things a child wears now, taken off the shelves. */
    fun worn(kid: Long) = things.filter { it.state == app.hob.data.Status.OUT && it.outKid == kid }

    /** Days from today to the start of the month [m] this year (or next, if it has passed and isn't now). */
    fun daysToMonth(m: Int): Int {
        val d = LocalDate.ofEpochDay(day)
        if (d.monthValue == m) return 0
        var start = LocalDate.of(d.year, m, 1)
        if (start.isBefore(d)) start = start.plusYears(1)
        return (start.toEpochDay() - day).toInt()
    }

    /** The hob's notes for the home screen, most pressing first. */
    fun hints(): List<Hint> {
        val out = mutableListOf<Hint>()
        home.filter { it.season != 0 }.map { it to daysToMonth(it.season) }.filter { it.second <= 50 }
            .groupBy { it.first.season }.values.forEach { g ->
                val first = g.minBy { it.first.name }
                out += Hint.Season(first.first, g.size, first.second)
            }
        val fits = home.mapNotNull { t -> next(t)?.let { Triple(t, it.first, it.second) } }
        fits.filter { it.third == 0 }.groupBy { it.second.id }.forEach { (_, g) -> out += Hint.Fits(g[0].second, g.map { it.first }) }
        overdue().forEach { out += Hint.Lent(it, (day - it.lentDay).toInt()) }
        fits.filter { it.third in 1..90 }.sortedBy { it.third }.take(2).forEach { out += Hint.Soon(it.first, it.second, it.third) }
        stillHere().filter { day - it.day >= 30 }.forEach { out += Hint.Return(it, (day - it.day).toInt()) }
        kids.filter { k -> measures.none { it.kid == k.id && day - it.day < 120 } }.forEach { out += Hint.Measure(it) }
        return out
    }

    /** How cosy the house is, 0–100: lit rooms, no dust, things written down, everything returned, kids measured. */
    fun cozy(): Int {
        val lit = if (rooms.isEmpty()) 0f else rooms.count { lit(it.id) }.toFloat() / rooms.size
        val dust = if (home.isEmpty()) 0f else 1f - (dusty().size.toFloat() / home.size * 3f).coerceAtMost(1f)
        val count = (ln(1f + home.size) / ln(101f)).coerceAtMost(1f)
        val back = if (overdue().isEmpty()) 1f else 0.3f
        val measured = if (kids.isEmpty() || kids.all { k -> measures.any { it.kid == k.id && day - it.day < 120 } }) 1f else 0.4f
        val treats = (s.treats.coerceAtMost(5) / 5f)
        return (lit * 30 + dust * 20 + count * 25 + back * 10 + measured * 7 + treats * 8).roundToInt().coerceIn(0, 100)
    }

    /** 0 guest, 1 lodger, 2 master, 3 old-timer. */
    fun level(): Int = when (cozy()) { in 0..39 -> 0; in 40..69 -> 1; in 70..89 -> 2; else -> 3 }

    fun badges(): List<Pair<Badge, Boolean>> = Badge.entries.map { b ->
        b to when (b) {
            Badge.BOXES -> boxes.size >= 5
            Badge.LIT -> rooms.isNotEmpty() && rooms.all { lit(it.id) }
            Badge.CLEAN -> s.treats >= 1
            Badge.HUNDRED -> things.size >= 100
            Badge.RELAY -> things.any { t -> events(t.id).filter { it.kind == Ev.WORN || it.kind == Ev.PAST }.map { it.who }.distinct().size >= 2 }
            Badge.SEASON -> home.any { it.season != 0 }
            Badge.BACK -> events.any { it.kind == Ev.BACK }
            Badge.OLD -> cozy() >= 90
        }
    }

    /** Sold, all time. */
    fun earned() = things.sumOf { it.sold }

    /** Built-in sections, then the owner's own (kept in settings or used by a thing). */
    fun catKeys(onlyUsed: Boolean = false): List<String> {
        val used = home.map { it.cat }.toSet()
        val own = (s.cats.map { OwnCat.key(it) } + used.filter { OwnCat.of(it) != null }).distinct()
        val all = Cat.entries.map { it.key } + own
        return if (onlyUsed) all.filter { it in used } else all
    }

    fun search(q: String, cat: String?): List<Thing> {
        val words = q.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        return home.filter { t ->
            (cat == null || t.cat == cat) && words.all { w ->
                val room = roomOf(t)
                listOf(t.name, t.note, OwnCat.of(t.cat) ?: "", t.sizeLabel, t.lentTo, room?.name ?: "", spotOf[t.spot]?.name ?: "", boxOf[t.box]?.label ?: "", boxOf[t.box]?.num?.toString() ?: "")
                    .any { it.lowercase().contains(w) }
            }
        }.sortedBy { it.name.lowercase() }
    }
}

sealed interface Hint {
    data class Season(val thing: Thing, val count: Int, val days: Int) : Hint
    data class Fits(val kid: Kid, val things: List<Thing>) : Hint
    data class Lent(val thing: Thing, val days: Int) : Hint
    data class Soon(val thing: Thing, val kid: Kid, val days: Int) : Hint
    data class Return(val b: Borrowed, val days: Int) : Hint
    data class Measure(val kid: Kid) : Hint
}

enum class Badge(val emoji: String) { BOXES("🧺"), LIT("🔦"), CLEAN("🧹"), HUNDRED("📦"), RELAY("🧥"), SEASON("🎄"), BACK("🤝"), OLD("🗝️") }
