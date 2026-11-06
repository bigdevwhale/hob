package app.hob.domain

import app.hob.data.Size

/** The kinds of things; the names come from resources (cat_<key>). */
enum class Cat(val key: String, val emoji: String) {
    CLOTHES("clothes", "👕"), SHOES("shoes", "👟"), TOOLS("tools", "🔧"), TECH("tech", "🔌"), HOLIDAY("holiday", "🎄"),
    DOCS("docs", "📄"), SPORT("sport", "⚽"), KITCHEN("kitchen", "🍳"), TOYS("toys", "🧸"), BOOKS("books", "📚"),
    GARDEN("garden", "🌱"), OTHER("other", "📦");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Own sections live in the same field as the built-in ones, as «u:<name>». */
object OwnCat {
    const val PREFIX = "u:"
    fun key(name: String) = PREFIX + name.trim()
    fun of(key: String): String? = if (key.startsWith(PREFIX)) key.removePrefix(PREFIX) else null
}

/** What a typed name suggests: an emoji, a kind and how its size is measured. */
data class Guessed(val emoji: String, val cat: Cat, val size: Int = Size.NONE)

/**
 * Guesses from the name, in Russian and English, by word stems. The first match wins, so the list goes from the
 * particular to the general.
 */
object Guess {
    private class Rule(stems: String, val emoji: String, val cat: Cat, val size: Int = Size.NONE) {
        val stems = stems.split('|')
    }

    private val RULES = listOf(
        Rule("коньк|skate", "⛸️", Cat.SPORT, Size.FOOT),
        Rule("лыжн|ботинки лыж|ski boot", "🎿", Cat.SPORT, Size.FOOT),
        Rule("лыж|ski", "🎿", Cat.SPORT),
        Rule("ролик|roller", "🛼", Cat.SPORT, Size.FOOT),
        Rule("сапог|сапож|boot", "🥾", Cat.SHOES, Size.FOOT),
        Rule("ботин|кроссов|кед|туфл|сандал|босонож|тапк|тапоч|валенк|дутик|shoe|sneaker|sandal|slipper", "👟", Cat.SHOES, Size.FOOT),
        Rule("куртк|пухов|пальт|ветровк|парк|шуб|плащ|комбинезон|jacket|coat|parka|snowsuit", "🧥", Cat.CLOTHES, Size.HEIGHT),
        Rule("штан|брюк|джинс|шорт|леггинс|колгот|trouser|pant|jean|short|legging|tight", "👖", Cat.CLOTHES, Size.HEIGHT),
        Rule("платье|сарафан|юбк|dress|skirt", "👗", Cat.CLOTHES, Size.HEIGHT),
        Rule("свитер|кофт|толстовк|худи|джемпер|sweater|jumper|hoodie|cardigan", "🧶", Cat.CLOTHES, Size.HEIGHT),
        Rule("футболк|рубашк|майк|водолазк|shirt|t-shirt|tee|blouse", "👕", Cat.CLOTHES, Size.HEIGHT),
        Rule("пижам|pyjama|pajama", "👕", Cat.CLOTHES, Size.HEIGHT),
        Rule("шапк|кепк|панам|шляп|hat|cap|beanie", "🧢", Cat.CLOTHES, Size.HEIGHT),
        Rule("шарф|снуд|scarf", "🧣", Cat.CLOTHES),
        Rule("варежк|перчат|рукавиц|mitten|glove", "🧤", Cat.CLOTHES, Size.HEIGHT),
        Rule("носк|sock", "🧦", Cat.CLOTHES),
        Rule("купальник|плавк|swim", "🩱", Cat.CLOTHES, Size.HEIGHT),
        Rule("велосип|беговел|bike|bicycle", "🚲", Cat.SPORT, Size.HEIGHT),
        Rule("самокат|scooter", "🛴", Cat.SPORT),
        Rule("санк|ватрушк|ледянк|sled|sledge", "🛷", Cat.SPORT),
        Rule("шарик|воздушн|balloon", "🎈", Cat.HOLIDAY),
        Rule("мяч|ball", "⚽", Cat.SPORT),
        Rule("палатк|tent", "⛺", Cat.SPORT),
        Rule("спальник|sleeping bag", "🛌", Cat.SPORT),
        Rule("гантел|штанг|dumbbell|kettlebell", "🏋️", Cat.SPORT),
        Rule("удочк|спиннинг|rod|fishing", "🎣", Cat.SPORT),
        Rule("гирлянд|garland|fairy light", "💡", Cat.HOLIDAY),
        Rule("ёлк|елк|ёлоч|елоч|christmas|ornament|bauble", "🎄", Cat.HOLIDAY),
        Rule("пасх|easter", "🥚", Cat.HOLIDAY),
        Rule("подар|упаковк|gift|wrapping", "🎁", Cat.HOLIDAY),
        Rule("гарант|чек|warranty|receipt", "🧾", Cat.DOCS),
        Rule("паспорт|свидетельств|документ|договор|полис|снилс|диплом|аттестат|справк|passport|certificate|document|contract|policy", "📄", Cat.DOCS),
        Rule("дрел|шуруповерт|шуруповёрт|перфорат|drill", "🪛", Cat.TOOLS),
        Rule("отвёрт|отверт|screwdriver", "🪛", Cat.TOOLS),
        Rule("молот|hammer", "🔨", Cat.TOOLS),
        Rule("пил|лобзик|saw", "🪚", Cat.TOOLS),
        Rule("ключи|keys", "🗝️", Cat.OTHER),
        Rule("ключ|wrench|spanner", "🔧", Cat.TOOLS),
        Rule("стремянк|лестниц|ladder", "🪜", Cat.TOOLS),
        Rule("рулетк|уровень|tape measure", "📏", Cat.TOOLS),
        Rule("краск|кист|валик|paint|brush", "🖌️", Cat.TOOLS),
        Rule("инструмент|tool", "🧰", Cat.TOOLS),
        Rule("удлинит|провод|кабел|шнур|зарядк|адаптер|переходник|hdmi|usb|extension|cable|charger|adapter", "🔌", Cat.TECH),
        Rule("батар|аккумулят|battery", "🔋", Cat.TECH),
        Rule("ноутбук|компьютер|laptop|computer", "💻", Cat.TECH),
        Rule("телефон|смартфон|phone", "📱", Cat.TECH),
        Rule("наушник|колонк|headphone|speaker", "🎧", Cat.TECH),
        Rule("фотоаппарат|камер|camera", "📷", Cat.TECH),
        Rule("пылесос|vacuum", "🧹", Cat.TECH),
        Rule("утюг|отпариват|iron", "🧺", Cat.TECH),
        Rule("фонар|torch|flashlight", "🔦", Cat.TECH),
        Rule("ламп|светильник|lamp", "💡", Cat.TECH),
        Rule("мультиварк|скороварк|хлебопеч|блендер|миксер|кофевар|кофемаш|тостер|соковыжим|чайник|multicooker|blender|mixer|kettle|toaster|coffee", "🍲", Cat.KITCHEN),
        Rule("фондю|fondue", "🫕", Cat.KITCHEN),
        Rule("кастрюл|сковород|казан|pan|pot", "🍳", Cat.KITCHEN),
        Rule("тарел|чашк|кружк|бокал|сервиз|посуд|plate|cup|mug|glass|dish", "🍽️", Cat.KITCHEN),
        Rule("банк|jar", "🫙", Cat.KITCHEN),
        Rule("книг|book", "📚", Cat.BOOKS),
        Rule("альбом|фотограф|album|photo", "📷", Cat.BOOKS),
        Rule("пластинк|vinyl|record", "💿", Cat.BOOKS),
        Rule("лего|конструктор|lego|blocks", "🧱", Cat.TOYS),
        Rule("кукл|doll", "🪆", Cat.TOYS),
        Rule("пазл|puzzle", "🧩", Cat.TOYS),
        Rule("настольн|игра|board game|game", "🎲", Cat.TOYS),
        Rule("игрушк|плюш|мишк|машинк|toy|teddy", "🧸", Cat.TOYS),
        Rule("коляск|stroller|pram", "👶", Cat.TOYS),
        Rule("автокресл|car seat", "💺", Cat.TOYS),
        Rule("семен|рассад|seed", "🌱", Cat.GARDEN),
        Rule("лопат|грабл|тяпк|секатор|шланг|лейк|shovel|spade|rake|hose|watering", "🪴", Cat.GARDEN),
        Rule("горш|кашпо|pot plant|planter", "🪴", Cat.GARDEN),
        Rule("мангал|шампур|гриль|bbq|grill|skewer", "🔥", Cat.GARDEN),
        Rule("чемодан|suitcase", "🧳", Cat.OTHER),
        Rule("рюкзак|сумк|backpack|bag", "🎒", Cat.OTHER),
        Rule("зонт|umbrella", "☂️", Cat.OTHER),
        Rule("аптечк|лекарств|таблет|бинт|термометр|medicine|first aid|pill", "💊", Cat.OTHER),
        Rule("плед|одеял|подушк|постел|blanket|pillow|bedding|duvet", "🛏️", Cat.OTHER),
        Rule("швейн|нитк|пряж|sewing|yarn|thread", "🧵", Cat.OTHER),
    )

    fun of(name: String): Guessed? {
        val n = name.lowercase()
        if (n.isBlank()) return null
        val words = Regex("""[\p{L}\d-]+""").findAll(n).map { it.value }.toList()
        for (r in RULES) for (s in r.stems) {
            val hit = if (' ' in s) n.contains(s) else words.any { it.startsWith(s) }
            if (hit) return Guessed(r.emoji, r.cat, r.size)
        }
        return null
    }
}
