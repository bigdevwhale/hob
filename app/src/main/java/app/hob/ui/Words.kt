package app.hob.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.hob.R
import app.hob.data.Thing
import app.hob.domain.Cat
import app.hob.domain.Hint
import app.hob.domain.House
import kotlin.math.roundToInt

/** Ready-made phrases; work from a [Context] so the reminders can use them too. */
object Words {
    fun things(ctx: Context, n: Int) = ctx.resources.getQuantityString(R.plurals.things, n, n)

    /** «Кладовка · стеллаж · коробка №3», with the place first when it isn't the main one. */
    fun where(ctx: Context, h: House, t: Thing): String {
        val spot = h.spotOf[t.spot] ?: return ctx.getString(R.string.not_home)
        val room = h.roomOf[spot.room] ?: return spot.name
        val place = h.placeOf[room.place]
        val parts = mutableListOf<String>()
        if (place != null && place.id != h.places.firstOrNull()?.id) parts += place.name
        parts += room.name
        parts += spot.name
        h.boxOf[t.box]?.let { parts += ctx.getString(R.string.box_n, it.num) }
        return parts.joinToString(" · ")
    }

    /** «уже впору», «через пару недель», «через 2 месяца», «через 1,5 года». */
    fun eta(ctx: Context, days: Int): String {
        val r = ctx.resources
        return when {
            days <= 0 -> ctx.getString(R.string.fits_now)
            days < 25 -> ctx.getString(R.string.in_weeks)
            days < 350 -> { val m = (days / 30.4f).roundToInt().coerceAtLeast(1); ctx.getString(R.string.in_x, r.getQuantityString(R.plurals.months, m, m)) }
            else -> {
                val y = (days / 365.25f * 2).roundToInt() / 2f
                if (y % 1f == 0f) ctx.getString(R.string.in_x, r.getQuantityString(R.plurals.years, y.toInt(), y.toInt()))
                else ctx.getString(R.string.in_x, ctx.getString(R.string.years_half, y.toString().replace('.', if (ctx.resources.configuration.locales[0].language == "ru") ',' else '.')))
            }
        }
    }

    /** «на этой неделе», «3 месяца назад», «2 года назад». */
    fun ago(ctx: Context, days: Long): String {
        val r = ctx.resources
        return when {
            days < 7 -> ctx.getString(R.string.this_week)
            days < 30 -> ctx.getString(R.string.x_ago, r.getQuantityString(R.plurals.weeks, (days / 7).toInt(), (days / 7).toInt()))
            days < 365 -> { val m = (days / 30.4f).toInt().coerceAtLeast(1); ctx.getString(R.string.x_ago, r.getQuantityString(R.plurals.months, m, m)) }
            else -> { val y = (days / 365.25f).toInt(); ctx.getString(R.string.x_ago, r.getQuantityString(R.plurals.years, y, y)) }
        }
    }

    /** «3 месяца», «2 года» – how long, without «ago». */
    fun span(ctx: Context, days: Long): String {
        val r = ctx.resources
        return when {
            days < 14 -> r.getQuantityString(R.plurals.days, days.toInt().coerceAtLeast(1), days.toInt().coerceAtLeast(1))
            days < 60 -> r.getQuantityString(R.plurals.weeks, (days / 7).toInt(), (days / 7).toInt())
            days < 365 -> { val m = (days / 30.4f).toInt(); r.getQuantityString(R.plurals.months, m, m) }
            else -> { val y = (days / 365.25f).toInt(); r.getQuantityString(R.plurals.years, y, y) }
        }
    }

    /** A hint as the hob says it: emoji, title, the line under it. */
    fun note(ctx: Context, h: House, hint: Hint): Triple<String, String, String> = when (hint) {
        is Hint.Season -> {
            val t = hint.thing
            val title = if (hint.count > 1) ctx.getString(R.string.hint_season_many, t.name.lowercase(), hint.count - 1) else ctx.getString(R.string.hint_season, t.name.lowercase())
            Triple(t.emoji, title, where(ctx, h, t) + " · " + ctx.getString(R.string.hint_season_when, month(ctx, t.season)))
        }
        is Hint.Fits -> Triple("🧸", ctx.getString(R.string.hint_fits, hint.kid.name, things(ctx, hint.things.size)),
            hint.things.take(3).joinToString(", ") { it.name.lowercase() }.replaceFirstChar { it.uppercase() })
        is Hint.Lent -> Triple(hint.thing.emoji, ctx.getString(R.string.hint_lent, hint.thing.name), ctx.getString(R.string.hint_lent_sub, hint.thing.lentTo, span(ctx, hint.days.toLong())))
        is Hint.Soon -> Triple(hint.thing.emoji, ctx.getString(R.string.hint_soon, hint.kid.name, hint.thing.name.lowercase()), where(ctx, h, hint.thing) + " · " + eta(ctx, hint.days))
        is Hint.Return -> Triple(hint.b.emoji, ctx.getString(R.string.hint_return, hint.b.name.lowercase()), ctx.getString(R.string.hint_return_sub, hint.b.from, span(ctx, hint.days.toLong())))
        is Hint.Measure -> Triple("📏", ctx.getString(R.string.hint_measure, hint.kid.name), ctx.getString(R.string.hint_measure_sub))
    }

    fun cat(ctx: Context, c: Cat): String = ctx.getString(when (c) {
        Cat.CLOTHES -> R.string.cat_clothes; Cat.SHOES -> R.string.cat_shoes; Cat.TOOLS -> R.string.cat_tools; Cat.TECH -> R.string.cat_tech
        Cat.HOLIDAY -> R.string.cat_holiday; Cat.DOCS -> R.string.cat_docs; Cat.SPORT -> R.string.cat_sport; Cat.KITCHEN -> R.string.cat_kitchen
        Cat.TOYS -> R.string.cat_toys; Cat.BOOKS -> R.string.cat_books; Cat.GARDEN -> R.string.cat_garden; Cat.OTHER -> R.string.cat_other
    })

    /** 12 500 with a thin gap. */
    fun n(x: Int): String = "%,d".format(java.util.Locale.ENGLISH, x).replace(',', ' ')

    fun month(ctx: Context, m: Int): String = ctx.resources.getStringArray(R.array.months)[(m - 1).coerceIn(0, 11)]
}

@Composable fun where(h: House, t: Thing) = Words.where(LocalContext.current, h, t)
@Composable fun eta(days: Int) = Words.eta(LocalContext.current, days)
@Composable fun ago(days: Long) = Words.ago(LocalContext.current, days)
@Composable fun span(days: Long) = Words.span(LocalContext.current, days)
@Composable fun catName(c: Cat) = Words.cat(LocalContext.current, c)
@Composable fun catName(key: String) = app.hob.domain.OwnCat.of(key) ?: Words.cat(LocalContext.current, Cat.of(key))
fun catSign(key: String) = if (app.hob.domain.OwnCat.of(key) != null) "🏷️" else Cat.of(key).emoji
@Composable fun things(n: Int) = Words.things(LocalContext.current, n)
