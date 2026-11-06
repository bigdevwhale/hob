package app.hob.ui.hob

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.container
import app.hob.data.Gone
import app.hob.data.Status
import app.hob.data.Thing
import app.hob.domain.House
import app.hob.ui.BAR_SPACE
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.Tab
import app.hob.ui.Words
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Em
import app.hob.ui.components.Field
import app.hob.ui.components.HobFigure
import app.hob.ui.components.HobMood
import app.hob.ui.components.Kind
import app.hob.ui.components.Meter
import app.hob.ui.components.R18
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.Sheet
import app.hob.ui.components.ThingRow
import app.hob.ui.components.TopBar
import app.hob.ui.components.Pic
import app.hob.ui.span
import app.hob.ui.things
import app.hob.ui.thing.goneWord
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import app.hob.ui.where

@Composable
fun HobScreen(h: House, nav: Nav) {
    val cozy = h.cozy()
    val lvl = h.level()
    val levels = stringArrayResource(R.array.levels)
    val levelsB = stringArrayResource(R.array.levels_b)
    val dark = h.rooms.filter { !h.lit(it.id) }
    val dusty = h.dusty()
    val gone = h.things.filter { it.state == Status.GONE }.sortedByDescending { it.goneDay }
    val treated = h.s.clearOut == h.day

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = BAR_SPACE)) {
        TopBar(stringResource(R.string.hob_t), stringResource(R.string.hob_sub))
        Card(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth().drawBehind {
            drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = .33f), Color.Transparent), center = Offset(size.width / 2, size.height * .3f), radius = size.width * .55f),
                center = Offset(size.width / 2, size.height * .3f), radius = size.width * .55f)
        }, pad = 20.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                HobFigure(Modifier.size(130.dp), if (treated) HobMood.PLEASED else if (lvl == 0 && h.home.isNotEmpty()) HobMood.SLEEPY else HobMood.CALM)
                Text(levels[lvl], style = T.Title, modifier = Modifier.padding(top = 4.dp))
                Text(levelsB[lvl], style = T.Small, textAlign = TextAlign.Center)
                Meter(cozy / 100f, Modifier.padding(top = 14.dp))
                Text(stringResource(R.string.cozy_line, cozy) + if (h.s.treats > 0) " · " + "🥛".repeat(h.s.treats.coerceAtMost(7)) else "", style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
        }

        SectionTitle(stringResource(R.string.likes_t))
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
            Like("🕯️", stringResource(R.string.like_lit), stringResource(R.string.like_lit_b, h.rooms.size - dark.size, h.rooms.size),
                dark.firstOrNull()?.let { r -> stringResource(R.string.light_it) to { nav.open(Overlay.Room(r.id)) } })
            Like("🕸", stringResource(R.string.like_clean), if (dusty.isEmpty()) stringResource(R.string.like_clean_ok) else stringResource(R.string.like_clean_b, things(dusty.size)),
                if (dusty.isEmpty()) null else stringResource(R.string.clear_go_short) to { nav.open(Overlay.ClearOut) })
            val lent = h.lentOut().size
            Like("🔁", stringResource(R.string.like_back), if (lent == 0) stringResource(R.string.like_back_ok) else stringResource(R.string.like_back_b, things(lent)),
                if (lent == 0) null else stringResource(R.string.look) to { nav.home(Tab.PEOPLE) })
            if (h.kids.isNotEmpty()) {
                val stale = h.kids.firstOrNull { k -> h.measures.none { it.kid == k.id && h.day - it.day < 120 } }
                Like("📏", stringResource(R.string.like_measure), if (stale == null) stringResource(R.string.like_measure_ok) else stringResource(R.string.like_measure_b, stale.name),
                    stale?.let { k -> stringResource(R.string.measure) to { nav.open(Overlay.Kid(k.id)) } })
            }
        }

        SectionTitle(stringResource(R.string.badges_t))
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
            val names = stringArrayResource(R.array.badges)
            val subs = stringArrayResource(R.array.badges_b)
            h.badges().chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    for ((b, on) in row) Column(Modifier.weight(1f).alpha(if (on) 1f else .35f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(50.dp).clip(CircleShape).background(if (on) C.amber.copy(alpha = .22f) else C.card2), contentAlignment = Alignment.Center) { Text(b.emoji, fontSize = 22.sp) }
                        Text(names[b.ordinal], style = T.Tiny.copy(color = C.ink), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
                        Text(subs[b.ordinal], style = T.Tiny.copy(fontSize = 9.5.sp), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (gone.isNotEmpty()) {
            val earned = h.earned()
            SectionTitle(stringResource(R.string.gone_t), note = if (earned > 0) stringResource(R.string.earned, Words.n(earned)) else null)
            Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
                for (t in gone.take(12)) ThingRow(t, goneWord(t.goneHow) + if (t.sold > 0) " · " + stringResource(R.string.money, Words.n(t.sold)) else "", { nav.open(Overlay.Thing(t.id)) })
            }
        }
    }
}

@Composable
private fun Like(emoji: String, title: String, sub: String, action: Pair<String, () -> Unit>?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Em(emoji)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = T.Strong)
            Text(sub, style = T.Small)
        }
        if (action != null) Btn(action.first, action.second, small = true) else Text("✓", style = T.Strong.copy(color = C.sage))
    }
}

/** Things untouched for a year, one card at a time: keep, give, sell or throw away. Ends with milk for the hob. */
@Composable
fun ClearOut(h: House, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val list = remember { mutableStateListOf<Thing>().apply { addAll(h.dusty()) } }
    var i by remember { mutableIntStateOf(0) }
    var selling by remember { mutableIntStateOf(-1) }
    var freed by remember { mutableIntStateOf(0) }

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(stringResource(R.string.clear_out), if (i < list.size) stringResource(R.string.n_of, i + 1, list.size) else null, { nav.close() })
        if (i < list.size) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (k in list.indices) Box(Modifier.weight(1f).size(5.dp).clip(CircleShape).background(if (k <= i) C.amber else C.card2))
            }
            val t = h.thing(list[i].id) ?: list[i]
            Card(Modifier.padding(horizontal = 18.dp).padding(top = 8.dp).fillMaxWidth().drawBehind {
                drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = .18f), Color.Transparent), center = Offset(size.width / 2, size.height * .2f), radius = size.width * .5f),
                    center = Offset(size.width / 2, size.height * .2f), radius = size.width * .5f)
            }, pad = 20.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (t.photo) Pic(t, size = 120.dp, big = true) else Text(t.emoji, fontSize = 68.sp)
                    Text(t.name, style = T.Title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Text(where(h, t), style = T.Small, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.untouched_for, span(h.day - t.touched)), style = T.Tiny.copy(color = C.onAmber),
                        modifier = Modifier.padding(top = 12.dp).clip(R18).background(C.amber).padding(horizontal = 10.dp, vertical = 3.dp))
                    if (t.note.isNotBlank()) Text("«${t.note}»", style = T.Small.copy(fontStyle = FontStyle.Italic), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                }
                fun next() { i++ }
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Btn(stringResource(R.string.keep), { c.write { keep(t) }; next() }, Modifier.weight(1f), Kind.GHOST)
                    Btn(stringResource(R.string.go_give), { c.write { letGo(t, Gone.GIVEN) }; freed++; next() }, Modifier.weight(1f))
                }
                Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Btn(stringResource(R.string.go_sell), { selling = i }, Modifier.weight(1f), Kind.GHOST)
                    Btn(stringResource(R.string.go_bin), { c.write { letGo(t, Gone.BINNED) }; freed++; next() }, Modifier.weight(1f), Kind.TERRA)
                }
            }
            Text(stringResource(R.string.clear_hint), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 14.dp))
        } else {
            Card(Modifier.padding(horizontal = 18.dp).padding(top = 10.dp).fillMaxWidth(), pad = 22.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    HobFigure(Modifier.size(120.dp), HobMood.PLEASED)
                    Text(stringResource(R.string.clear_done_t), style = T.Title, modifier = Modifier.padding(top = 6.dp))
                    Text(if (freed > 0) stringResource(R.string.clear_done_b, things(freed)) else stringResource(R.string.clear_done_kept), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Btn(stringResource(R.string.leave_milk), {
                        c.write { treat() }
                        nav.home(Tab.HOB)
                    }, Modifier.padding(top = 18.dp))
                }
            }
        }
    }

    if (selling >= 0) {
        var sum by remember { mutableIntStateOf(0) }
        val t = list[selling]
        Sheet(stringResource(R.string.go_sell), { selling = -1 }) {
            Field(if (sum == 0) "" else sum.toString(), { sum = it.filter { ch -> ch.isDigit() }.take(9).toIntOrNull() ?: 0 }, stringResource(R.string.sold_for), hint = "1500", number = true)
            Btn(stringResource(R.string.go_sell), { c.write { letGo(t, Gone.SOLD, sum) }; freed++; selling = -1; i++ }, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    }
}

