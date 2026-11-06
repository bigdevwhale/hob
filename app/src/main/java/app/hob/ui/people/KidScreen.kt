package app.hob.ui.people

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hob.R
import app.hob.data.now
import app.hob.container
import app.hob.data.Kid
import app.hob.data.Size
import app.hob.domain.Growth
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.Words
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Field
import app.hob.ui.components.IconBtn
import app.hob.ui.components.Kind
import app.hob.ui.components.Label
import app.hob.ui.components.Mood
import app.hob.ui.components.R14
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.Sheet
import app.hob.ui.components.Tag
import app.hob.ui.components.ThingRow
import app.hob.ui.components.TopBar
import app.hob.ui.eta
import app.hob.ui.things
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import app.hob.ui.where
import java.time.LocalDate
import kotlin.math.roundToInt

private fun fmt(v: Float) = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v).replace('.', ',')

@Composable
fun KidScreen(id: Long, h: House, nav: Nav) {
    val ctx = LocalContext.current
    val k = h.kidOf[id] ?: run { nav.close(); return }
    var measure by remember { mutableStateOf(false) }
    val height = Growth.estimate(Size.HEIGHT, k, h.measures, h.day)
    val foot = Growth.estimate(Size.FOOT, k, h.measures, h.day)
    val age = Growth.age(k, h.day)
    val waiting = h.home.filter { it.sized }.mapNotNull { t -> h.days(t, k)?.takeIf { it <= 540 }?.let { t to it } }.sortedBy { it.second }
    val worn = h.worn(k.id)
    val last = h.measures.filter { it.kid == id }.maxByOrNull { it.day }

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(k.name, ageText(age), { nav.close() }) {
            IconBtn(Icons.Outlined.Edit, { nav.open(Overlay.KidEdit(id)) }, desc = stringResource(R.string.edit))
            Btn(stringResource(R.string.measure), { measure = true }, small = true)
        }
        Card(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(stringResource(R.string.st_height), height.roundToInt().toString(), Modifier.weight(1f))
                Stat(stringResource(R.string.st_foot), fmt((foot * 2).roundToInt() / 2f), Modifier.weight(1f))
                Stat(stringResource(R.string.st_year), "+" + Growth.pace(Size.HEIGHT, age).roundToInt(), Modifier.weight(1f))
            }
            GrowthChart(k, h, Modifier.fillMaxWidth().height(170.dp).padding(top = 12.dp))
            Text(
                if (last == null) stringResource(R.string.chart_guess) else stringResource(R.string.chart_b, Words.ago(ctx, h.day - last.day)),
                style = T.Small, modifier = Modifier.padding(top = 8.dp))
        }

        SectionTitle(stringResource(R.string.waits), note = things(waiting.size))
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
            if (waiting.isEmpty()) Text(stringResource(R.string.waits_none), style = T.Small)
            for ((t, d) in waiting) ThingRow(t, where(h, t), { nav.open(Overlay.Thing(t.id)) },
                tag = { Tag(eta(d), if (d == 0) Mood.NOW else if (d <= 90) Mood.SOON else Mood.PLAIN) })
        }
        if (worn.isNotEmpty()) {
            SectionTitle(stringResource(R.string.wears_now))
            Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
                for (t in worn) ThingRow(t, t.sizeLabel.ifBlank { null }, { nav.open(Overlay.Thing(t.id)) })
                Text(stringResource(R.string.wears_b), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
    if (measure) MeasureSheet(k, height, foot) { measure = false }
}

@Composable
fun ageText(age: Float): String {
    val y = age.toInt(); val m = ((age - y) * 12).toInt()
    val r = LocalContext.current.resources
    return if (y == 0) r.getQuantityString(R.plurals.months, m, m)
    else r.getQuantityString(R.plurals.years, y, y) + if (m > 0) " " + r.getQuantityString(R.plurals.months, m, m) else ""
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(R14).background(C.card2).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Text(label, style = T.Tiny)
        Text(value, style = T.Number)
    }
}

/** The usual band for the age, the child's own measurements and a dashed line on into the next year and a half. */
@Composable
fun GrowthChart(k: Kid, h: House, modifier: Modifier) {
    val age = Growth.age(k, h.day)
    val from = (age - 4f).coerceAtLeast(0f)
    val to = age + 1.5f
    val pts = h.measures.filter { it.kid == k.id && it.height > 0f }.map { Growth.age(k, it.day) to it.height }.filter { it.first >= from - 0.01f }
    val lo = Growth.median(Size.HEIGHT, from) - 10f
    val hi = Growth.median(Size.HEIGHT, to) + 10f
    val col = C.kid(k.color)
    val band = C.ink.copy(alpha = .07f)
    val grid = C.ink.copy(alpha = .08f)
    Canvas(modifier) {
        fun x(a: Float) = (a - from) / (to - from) * size.width
        fun y(v: Float) = size.height - (v - lo) / (hi - lo) * size.height
        val steps = 30
        val up = Path(); val dn = Path()
        for (i in 0..steps) {
            val a = from + (to - from) * i / steps
            val m = Growth.median(Size.HEIGHT, a)
            if (i == 0) up.moveTo(x(a), y(m + 6f)) else up.lineTo(x(a), y(m + 6f))
        }
        for (i in steps downTo 0) {
            val a = from + (to - from) * i / steps
            up.lineTo(x(a), y(Growth.median(Size.HEIGHT, a) - 6f))
        }
        up.close()
        drawPath(up, band)
        var v = (lo / 10f).toInt() * 10f
        while (v < hi) { if (v > lo) drawLine(grid, Offset(0f, y(v)), Offset(size.width, y(v)), 1f); v += 10f }
        drawLine(grid, Offset(x(age), 0f), Offset(x(age), size.height), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        val line = Path()
        pts.forEachIndexed { i, (a, hgt) -> if (i == 0) line.moveTo(x(a), y(hgt)) else line.lineTo(x(a), y(hgt)) }
        if (pts.size > 1) drawPath(line, col, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        val nowV = Growth.estimate(Size.HEIGHT, k, h.measures, h.day)
        val endV = Growth.estimate(Size.HEIGHT, k, h.measures, h.day + 548)
        val start = pts.lastOrNull() ?: (age to nowV)
        drawLine(col, Offset(x(start.first), y(start.second)), Offset(x(to), y(endV)), 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
        for ((a, hgt) in pts) drawCircle(col, 4.dp.toPx(), Offset(x(a), y(hgt)))
        drawCircle(col.copy(alpha = .25f), 10.dp.toPx(), Offset(x(age), y(nowV)))
        drawCircle(col, 5.dp.toPx(), Offset(x(age), y(nowV)))
    }
}

@Composable
fun MeasureSheet(k: Kid, height: Float, foot: Float, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    var hgt by remember { mutableStateOf(height.roundToInt().toString()) }
    var ft by remember { mutableStateOf(fmt((foot * 2).roundToInt() / 2f)) }
    Sheet(stringResource(R.string.measure_t, k.name), onDone) {
        Field(hgt, { hgt = it }, stringResource(R.string.height_cm), number = true)
        Box(Modifier.padding(top = 12.dp)) { Field(ft, { ft = it }, stringResource(R.string.foot_size), number = true) }
        Text(stringResource(R.string.measure_b), style = T.Small, modifier = Modifier.padding(top = 10.dp))
        Btn(stringResource(R.string.save), {
            val hv = hgt.replace(',', '.').toFloatOrNull() ?: 0f
            val fv = ft.replace(',', '.').toFloatOrNull() ?: 0f
            c.write { measure(k.id, hv, fv) }
            onDone()
        }, Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

/** A new child or an existing one: name, month and year of birth, first measurements. */
@Composable
fun KidEdit(id: Long, h: House, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val old = h.kidOf[id]
    val born = old?.let { LocalDate.ofEpochDay(it.born) }
    var name by remember { mutableStateOf(old?.name ?: "") }
    var year by remember { mutableStateOf(born?.year?.toString() ?: "") }
    var month by remember { mutableIntStateOf(born?.monthValue ?: 0) }
    var hgt by remember { mutableStateOf("") }
    var ft by remember { mutableStateOf("") }
    val y = year.toIntOrNull()?.takeIf { it in 1990..LocalDate.now().year }
    Column(Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(stringResource(if (old == null) R.string.new_kid else R.string.edit_kid), null, { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            Field(name, { name = it }, stringResource(R.string.kid_name))
            Box(Modifier.padding(top = 12.dp)) { Field(year, { year = it.filter { ch -> ch.isDigit() }.take(4) }, stringResource(R.string.kid_year), hint = "2021", number = true) }
            Label(stringResource(R.string.kid_month))
            Chips { for (m in 1..12) Chip(Words.month(ctx, m), month == m) { month = m } }
            if (old == null) {
                Label(stringResource(R.string.kid_first))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(hgt, { hgt = it }, stringResource(R.string.height_cm), Modifier.weight(1f), number = true)
                    Field(ft, { ft = it }, stringResource(R.string.foot_size), Modifier.weight(1f), number = true)
                }
                Text(stringResource(R.string.kid_first_b), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            }
            Btn(stringResource(R.string.save), {
                val day = LocalDate.of(y ?: return@Btn, month.coerceIn(1, 12), 15).toEpochDay()
                c.write {
                    if (old == null) {
                        val nid = addKid(name, day, hgt.replace(',', '.').toFloatOrNull() ?: 0f, ft.replace(',', '.').toFloatOrNull() ?: 0f)
                        nav.replace(Overlay.Kid(nid))
                    } else { dao.save(old.copy(name = name.trim(), born = day, updated = now())); nav.close() }
                }
            }, Modifier.fillMaxWidth().padding(top = 22.dp), enabled = name.isNotBlank() && y != null && month > 0)
            if (old != null) Btn(stringResource(R.string.delete_kid), { c.write { dropKid(old) }; nav.stack.clear() }, Modifier.padding(top = 10.dp).fillMaxWidth(), Kind.GHOST, small = true)
        }
    }
}
