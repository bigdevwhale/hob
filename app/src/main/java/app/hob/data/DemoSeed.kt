package app.hob.data

import app.hob.AppContainer
import app.hob.R
import app.hob.domain.Growth
import app.hob.domain.today
import app.hob.localized
import kotlin.math.roundToLong

/**
 * A lived-in house for the demo build and the screenshots: a flat and a dacha, boxes on the top shelves, children's
 * clothes waiting for the younger one, a drill lent to a friend, a few things nobody has touched for years.
 */
object DemoSeed {
    private class T(val name: String, val emoji: String, val cat: String, val room: String, val spot: String, val box: Int = 0, val ago: Int,
                    val size: Int = Size.NONE, val label: String = "", val note: String = "", val season: Int = 0, val past: List<String> = emptyList(),
                    val lent: String = "", val lentAgo: Int = 0)

    suspend fun seedIfEmpty(c: AppContainer) {
        if (c.dao.placesNow().isNotEmpty()) return
        val ru = c.app.localized().resources.configuration.locales[0].language == "ru"
        val d = today()
        fun L(en: String, r: String) = if (ru) r else en

        val flat = c.addPlace(c.app.localized().getString(R.string.place_flat))
        val dacha = c.addPlace(c.app.localized().getString(R.string.place_dacha))
        val rooms = mutableMapOf<String, Long>()
        suspend fun room(key: String, place: Long, name: String, emoji: String, vararg spots: String) { rooms[key] = c.addRoom(place, name, emoji, spots.toList()) }
        room("hall", flat, L("Hallway", "Прихожая"), "🚪", L("top shelf", "антресоль"), L("wardrobe", "шкаф-купе"), L("shoe rack", "обувница"))
        room("kitchen", flat, L("Kitchen", "Кухня"), "🍳", L("top cupboards", "верхние шкафы"), L("under the sink", "под мойкой"))
        room("pantry", flat, L("Pantry", "Кладовка"), "🧺", L("top shelf", "верхняя полка"), L("shelving", "стеллаж"))
        room("balcony", flat, L("Balcony", "Балкон"), "🪴", L("cupboard", "шкафчик"))
        room("kids", flat, L("Kids’ room", "Детская"), "🧸", L("wardrobe", "шкаф"), L("under the bed", "под кроватью"))
        room("bedroom", flat, L("Bedroom", "Спальня"), "🛏️", L("chest of drawers", "комод"), L("under the bed", "под кроватью"))
        room("shed", dacha, L("Shed", "Сарай"), "🪵", L("shelving", "стеллаж"), L("by the wall", "у стены"))
        room("attic", dacha, L("Attic", "Чердак"), "🕯️", L("trunk", "сундук"))

        val spots = c.dao.spotsNow()
        fun spot(room: String, i: Int) = spots.filter { it.room == rooms[room] }.sortedBy { it.sort }[i].id
        val box = mutableMapOf<Int, Long>()
        box[1] = c.addBox(spot("hall", 0), L("Winter 110–116", "Зима 110–116"))
        box[2] = c.addBox(spot("hall", 1), L("Papers", "Документы"))
        box[3] = c.addBox(spot("pantry", 1), L("Cables", "Провода"))
        box[4] = c.addBox(spot("pantry", 0), L("New Year", "Новый год"))
        box[5] = c.addBox(spot("shed", 0), L("Tools", "Инструменты"))
        box[6] = c.addBox(spot("hall", 0), L("Summer 98–104", "Лето 98–104"))
        val boxSpot = c.dao.boxesNow().associate { it.id to it.spot }

        val misha = L("Misha", "Миша")
        val vanya = L("Vanya", "Ваня")
        val tema = L("Tema (cousin)", "Тёма, племянник")
        val list = listOf(
            T(L("Blue down jacket", "Пуховик синий"), "🧥", "clothes", "hall", "", 1, 240, Size.HEIGHT, "116", L("the zip sticks – wax it", "молния тугая, смазать воском"), past = listOf(tema, misha)),
            T(L("Waterproof trousers", "Штаны непромокаемые"), "👖", "clothes", "hall", "", 1, 240, Size.HEIGHT, "110", past = listOf(misha)),
            T(L("Bobble hat", "Шапка с помпоном"), "🧢", "clothes", "hall", "", 1, 240, Size.HEIGHT, "104–118", past = listOf(misha)),
            T(L("Mittens on a string", "Варежки на резинке"), "🧤", "clothes", "hall", "", 1, 240, Size.HEIGHT, "4–5", past = listOf(misha)),
            T(L("Shorts with pockets", "Шорты с карманами"), "👖", "clothes", "hall", "", 6, 150, Size.HEIGHT, "104", past = listOf(misha)),
            T(L("Birth certificates", "Свидетельства о рождении"), "📄", "docs", "hall", "", 2, 90),
            T(L("Warranty cards", "Гарантийные талоны"), "🧾", "docs", "hall", "", 2, 60),
            T(L("Extension lead 5 m", "Удлинитель 5 м"), "🔌", "tech", "pantry", "", 3, 20),
            T(L("Old chargers", "Старые зарядки"), "🔋", "tech", "pantry", "", 3, 790),
            T(L("HDMI cable", "HDMI-кабель"), "🔌", "tech", "pantry", "", 3, 270),
            T(L("Tree decorations", "Ёлочные игрушки"), "🎄", "holiday", "pantry", "", 4, 300, season = 12),
            T(L("Fairy lights", "Гирлянда"), "💡", "holiday", "pantry", "", 4, 300, note = L("one section is out", "одна секция не горит"), season = 12),
            T(L("Drill", "Дрель"), "🪛", "tools", "shed", "", 5, 30, lent = L("Serge", "Серёжа"), lentAgo = 21),
            T(L("Screwdriver set", "Набор отвёрток"), "🪛", "tools", "shed", "", 5, 120),
            T(L("Multicooker", "Мультиварка"), "🍲", "kitchen", "kitchen", L("top cupboards", "верхние шкафы"), ago = 420),
            T(L("Fondue set", "Фондюшница"), "🫕", "kitchen", "kitchen", L("top cupboards", "верхние шкафы"), ago = 1150, note = L("a wedding present", "подарок на свадьбу")),
            T(L("Adjustable skates 30–33", "Коньки раздвижные"), "⛸️", "sport", "balcony", L("cupboard", "шкафчик"), ago = 270, size = Size.FOOT, label = "30–33", past = listOf(misha)),
            T(L("Tent", "Палатка"), "⛺", "sport", "balcony", L("cupboard", "шкафчик"), ago = 400),
            T(L("Winter boots", "Зимние ботинки"), "🥾", "shoes", "kids", L("under the bed", "под кроватью"), ago = 600, size = Size.FOOT, label = "29", past = listOf(misha)),
            T(L("Bike 16″", "Велосипед 16″"), "🚲", "sport", "shed", L("by the wall", "у стены"), ago = 150, size = Size.HEIGHT, label = "100–118", past = listOf(misha)),
            T(L("Old vacuum cleaner", "Старый пылесос"), "🧹", "tech", "shed", L("by the wall", "у стены"), ago = 1220),
            T(L("Board games", "Настольные игры"), "🎲", "toys", "kids", L("wardrobe", "шкаф"), ago = 35),
            T(L("Lego", "Лего"), "🧱", "toys", "kids", L("wardrobe", "шкаф"), ago = 10),
        )
        for (t in list) {
            val sp = if (t.box != 0) boxSpot.getValue(box.getValue(t.box)) else spots.first { it.room == rooms[t.room] && it.name == t.spot }.id
            val r = Growth.parse(t.size, t.label)
            val id = c.dao.add(Thing(name = t.name, emoji = t.emoji, cat = t.cat, spot = sp, box = if (t.box != 0) box.getValue(t.box) else 0, note = t.note,
                added = d - 700, touched = d - t.ago, sizeKind = if (r != null) t.size else Size.NONE, sizeMin = r?.first ?: 0f, sizeMax = r?.second ?: 0f,
                sizeLabel = t.label, season = t.season,
                lentTo = t.lent, lentDay = if (t.lent.isNotEmpty()) d - t.lentAgo else 0, lentRemind = if (t.lent.isNotEmpty()) d - t.lentAgo + 14 else 0))
            c.dao.add(Event(thing = id, day = d - 700, kind = Ev.ADDED))
            t.past.forEach { c.dao.add(Event(thing = id, day = d - 700, kind = Ev.PAST, who = it)) }
            if (t.lent.isNotEmpty()) c.dao.add(Event(thing = id, day = d - t.lentAgo, kind = Ev.LENT, who = t.lent))
        }
        // A few that already went on.
        val gone = listOf(Triple(L("Bread maker", "Хлебопечка"), "🍞", Gone.SOLD to 2500), Triple(L("Sledge", "Санки"), "🛷", Gone.GIVEN to 0), Triple(L("Pram", "Коляска"), "👶", Gone.CHARITY to 0))
        for ((i, g) in gone.withIndex()) c.dao.add(Thing(name = g.first, emoji = g.second, cat = "other", spot = spot("pantry", 1), added = d - 900, touched = d - 400,
            state = Status.GONE, goneHow = g.third.first, goneDay = d - 40 - i * 120, sold = g.third.second))

        // Misha, 9, and Vanya, 5½, with their measurements over the years.
        val m = c.dao.add(Kid(name = misha, born = d - (9.6 * 365.25).roundToLong(), color = 1))
        val v = c.dao.add(Kid(name = vanya, born = d - (5.45 * 365.25).roundToLong(), color = 0))
        for ((age, hgt, foot) in listOf(Triple(7.0, 123f, 31f), Triple(8.0, 129f, 33f), Triple(9.0, 132f, 34f), Triple(9.5, 134f, 34.5f)))
            c.dao.add(Measure(kid = m, day = d - ((9.6 - age) * 365.25).roundToLong(), height = hgt, foot = foot))
        for ((age, hgt, foot) in listOf(Triple(3.0, 94f, 24f), Triple(4.0, 101f, 26f), Triple(5.0, 106.5f, 27.5f), Triple(5.4, 109f, 28.3f)))
            c.dao.add(Measure(kid = v, day = d - ((5.45 - age) * 365.25).roundToLong(), height = hgt, foot = foot))

        c.dao.add(Borrowed(name = L("Step-ladder", "Стремянка"), emoji = "🪜", from = L("Peter next door", "сосед Пётр"), day = d - 63))
        c.settings.update { it.copy(onboarded = true, treats = 2, clearOut = d - 200) }
    }
}
