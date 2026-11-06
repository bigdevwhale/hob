package app.hob.ui.thing

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.container
import app.hob.data.Ev
import app.hob.data.Gone
import app.hob.data.Status
import app.hob.data.Thing
import app.hob.domain.Cat
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.Words
import app.hob.ui.catName
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Field
import app.hob.ui.components.IconBtn
import app.hob.ui.components.Kind
import app.hob.ui.components.Label
import app.hob.ui.components.MenuRow
import app.hob.ui.components.Mood
import app.hob.ui.components.Pic
import app.hob.ui.components.R14
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.Sheet
import app.hob.ui.components.Tag
import app.hob.ui.components.TopBar
import app.hob.ui.eta
import app.hob.ui.house.Picker
import app.hob.ui.house.Where
import app.hob.ui.house.settle
import app.hob.ui.span
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import app.hob.ui.where

/** Under a thing in a list: who it fits and when, or who has it. */
@Composable
fun KidTag(h: House, t: Thing) {
    if (t.lent) { Tag(stringResource(R.string.tag_lent, t.lentTo), Mood.PLAIN); return }
    val n = h.next(t) ?: return
    Tag("${n.first.name}: ${eta(n.second)}", if (n.second == 0) Mood.NOW else if (n.second <= 90) Mood.SOON else Mood.PLAIN)
}

private enum class Ask { LEND, MOVE, GO, PUT_AWAY }

@Composable
fun ThingScreen(id: Long, h: House, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val t = h.thing(id) ?: run { nav.close(); return }
    val next = h.next(t)
    var ask by remember { mutableStateOf<Ask?>(null) }

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(t.name, null, { nav.close() }) {
            if (t.state != Status.GONE) IconBtn(Icons.Outlined.Edit, { nav.open(Overlay.Edit(id = t.id)) }, desc = stringResource(R.string.edit))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(150.dp).drawBehind { drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = .28f), Color.Transparent))) }, contentAlignment = Alignment.Center) {
                if (t.photo) Pic(t, Modifier.clip(CircleShape), size = 128.dp, big = true) else Text(t.emoji, fontSize = 64.sp)
            }
            Text(
                when (t.state) {
                    Status.GONE -> stringResource(R.string.gone_line, goneWord(t.goneHow), Words.ago(ctx, h.day - t.goneDay))
                    Status.OUT -> stringResource(R.string.worn_by, h.kidOf[t.outKid]?.name ?: "")
                    else -> where(h, t)
                },
                style = T.Small.copy(color = C.ink.copy(alpha = .75f)), textAlign = TextAlign.Center)
            if (t.note.isNotBlank()) Text("«${t.note}»", style = T.Small.copy(fontStyle = FontStyle.Italic), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }

        Row(Modifier.padding(horizontal = 18.dp).padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact(stringResource(R.string.f_cat), catName(t.cat), Modifier.weight(1f))
            Fact(stringResource(R.string.f_touched), if (h.day - t.touched < 7) stringResource(R.string.recently) else span(h.day - t.touched), Modifier.weight(1f))
            if (t.sized) Fact(stringResource(R.string.f_size), t.sizeLabel, Modifier.weight(1f))
            else Fact(stringResource(R.string.f_box), h.boxOf[t.box]?.let { "№${it.num}" } ?: "—", Modifier.weight(1f))
        }

        val evs = h.events(t.id).filter { it.kind != Ev.ADDED }
        if (evs.isNotEmpty() || next != null || t.lent) {
            SectionTitle(stringResource(R.string.history))
            Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
                val lines = evs.map { e -> Line(eventText(e.kind, e.who), if (e.kind == Ev.PAST) "" else Words.ago(ctx, h.day - e.day), C.sage) }.toMutableList()
                if (next != null) lines += Line(stringResource(R.string.next_kid, next.first.name), eta(next.second), C.amber, true)
                Timeline(lines)
            }
        }

        if (t.state != Status.GONE) Column(Modifier.padding(horizontal = 18.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                when {
                    t.state == Status.OUT -> Btn(stringResource(R.string.put_away), { ask = Ask.PUT_AWAY }, Modifier.weight(1f))
                    t.lent -> {
                        Btn(stringResource(R.string.came_back), { c.write { back(t) } }, Modifier.weight(1f))
                        Btn(stringResource(R.string.remind), {
                            val text = ctx.getString(R.string.remind_text, t.name.lowercase())
                            runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)) }
                        }, Modifier.weight(1f), Kind.GHOST)
                    }
                    next != null && next.second == 0 -> {
                        Btn(stringResource(R.string.give_to, next.first.name), { c.write { wear(t, next.first) } }, Modifier.weight(1f))
                        Btn(stringResource(R.string.lend), { ask = Ask.LEND }, Modifier.weight(1f), Kind.GHOST)
                    }
                    else -> {
                        Btn(stringResource(R.string.took), { c.write { touch(t) } }, Modifier.weight(1f))
                        Btn(stringResource(R.string.lend), { ask = Ask.LEND }, Modifier.weight(1f), Kind.GHOST)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (t.state == Status.HOME) Btn(stringResource(R.string.move), { ask = Ask.MOVE }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.let_go), { ask = Ask.GO }, Modifier.weight(1f), Kind.GHOST)
            }
        } else Btn(stringResource(R.string.delete_forever), { c.write { delete(t) }; nav.close() }, Modifier.padding(18.dp), Kind.GHOST, small = true)
    }

    when (ask) {
        Ask.LEND -> LendSheet(h, t) { ask = null }
        Ask.MOVE, Ask.PUT_AWAY -> {
            var w by remember { mutableStateOf(h.settle(Where(spot = t.spot, box = t.box))) }
            Sheet(stringResource(if (ask == Ask.MOVE) R.string.move_t else R.string.put_away_t), { ask = null }) {
                Picker(h, w) { w = it }
                Btn(stringResource(R.string.move_here), {
                    val room = h.roomOf[w.room]; val spot = h.spotOf[w.spot]
                    val label = listOfNotNull(room?.name, spot?.name).joinToString(" · ")
                    c.write { if (ask == Ask.MOVE) move(t, w.spot, w.box, label) else putAway(t, w.spot, w.box); ask = null }
                }, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = w.spot != 0L)
            }
        }
        Ask.GO -> LetGoSheet(t, { ask = null }) { nav.close() }
        null -> Unit
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(R14).background(C.card).border(1.dp, C.line, R14).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Text(label, style = T.Tiny)
        Text(value, style = T.Label.copy(fontSize = if (value.length > 9) 13.sp else 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

data class Line(val title: String, val sub: String, val dot: Color, val next: Boolean = false)

@Composable
fun Timeline(lines: List<Line>) {
    Column {
        lines.forEachIndexed { i, l ->
            Row {
                Column(Modifier.width(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.padding(top = 4.dp).size(11.dp).clip(CircleShape).background(l.dot))
                    if (i < lines.lastIndex) Box(Modifier.padding(top = 3.dp).size(1.5.dp, if (l.sub.isBlank()) 14.dp else 30.dp).background(C.ink.copy(alpha = .18f)))
                }
                Column(Modifier.padding(start = 8.dp, bottom = if (i < lines.lastIndex) 8.dp else 0.dp)) {
                    Text(l.title, style = T.Strong.copy(color = if (l.next) C.amberText else C.ink))
                    if (l.sub.isNotBlank()) Text(l.sub, style = T.Small)
                }
            }
        }
    }
}

@Composable
private fun eventText(kind: Int, who: String): String = when (kind) {
    Ev.MOVED -> stringResource(R.string.ev_moved, who)
    Ev.LENT -> stringResource(R.string.ev_lent, who)
    Ev.BACK -> stringResource(R.string.ev_back)
    Ev.WORN -> stringResource(R.string.ev_worn, who)
    Ev.RETURNED -> stringResource(R.string.ev_returned)
    Ev.GONE -> goneWord(who.toIntOrNull() ?: 0).replaceFirstChar { it.uppercase() }
    Ev.PAST -> stringResource(R.string.ev_past, who)
    else -> stringResource(R.string.ev_added)
}

@Composable
fun goneWord(how: Int): String = stringResource(when (how) { Gone.SOLD -> R.string.gone_sold; Gone.CHARITY -> R.string.gone_charity; Gone.BINNED -> R.string.gone_binned; else -> R.string.gone_given })

@Composable
fun LendSheet(h: House, t: Thing, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var who by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(14) }
    val known = (h.things.map { it.lentTo } + h.events.filter { it.kind == Ev.LENT }.map { it.who }).filter { it.isNotBlank() }.distinct().take(6)
    Sheet(stringResource(R.string.lend_t, t.name), onDone) {
        Field(who, { who = it }, null, hint = stringResource(R.string.lend_who))
        if (known.isNotEmpty()) Box(Modifier.padding(top = 9.dp)) { Chips { for (k in known) Chip(k, who == k) { who = k } } }
        Label(stringResource(R.string.lend_remind))
        Chips {
            for ((d, l) in listOf(7 to R.string.in_week, 14 to R.string.in_two_weeks, 30 to R.string.in_month, 0 to R.string.no_remind)) Chip(stringResource(l), days == d) { days = d }
        }
        Btn(stringResource(R.string.lend), { c.write { lend(t, who, days) }; onDone() }, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = who.isNotBlank())
    }
}

@Composable
fun LetGoSheet(t: Thing, onDismiss: () -> Unit, onGone: () -> Unit) {
    val c = LocalContext.current.container
    var selling by remember { mutableStateOf(false) }
    var sum by remember { mutableStateOf("") }
    Sheet(stringResource(R.string.let_go_t, t.name), onDismiss) {
        if (!selling) {
            fun go(how: Int) { c.write { letGo(t, how) }; onDismiss(); onGone() }
            MenuRow("🎁", stringResource(R.string.go_give), stringResource(R.string.go_give_sub)) { go(Gone.GIVEN) }
            MenuRow("💰", stringResource(R.string.go_sell), stringResource(R.string.go_sell_sub)) { selling = true }
            MenuRow("🤲", stringResource(R.string.go_charity), stringResource(R.string.go_charity_sub)) { go(Gone.CHARITY) }
            MenuRow("🗑️", stringResource(R.string.go_bin), stringResource(R.string.go_bin_sub)) { go(Gone.BINNED) }
        } else {
            Field(sum, { sum = it.filter { ch -> ch.isDigit() } }, stringResource(R.string.sold_for), hint = "1500", number = true)
            Btn(stringResource(R.string.go_sell), {
                c.write { letGo(t, Gone.SOLD, sum.toIntOrNull() ?: 0) }; onDismiss(); onGone()
            }, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    }
}
