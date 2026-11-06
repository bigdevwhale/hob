package app.hob.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.container
import app.hob.data.Room
import app.hob.domain.Hint
import app.hob.domain.House
import app.hob.ui.BAR_SPACE
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.Tab
import app.hob.ui.Words
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Chip
import app.hob.ui.components.Em
import app.hob.ui.components.Field
import app.hob.ui.components.HobFigure
import app.hob.ui.components.HobMood
import app.hob.ui.components.IconBtn
import app.hob.ui.components.R18
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.Sheet
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import java.time.LocalTime

/** One of the hob's notes, ready to show. */
data class Note(val emoji: String, val title: String, val sub: String, val lamp: Boolean, val go: () -> Unit)

@Composable
fun notes(h: House, nav: Nav): List<Note> {
    val ctx = LocalContext.current
    return h.hints().map { hint ->
        val (emoji, title, sub) = Words.note(ctx, h, hint)
        Note(emoji, title, sub, hint is Hint.Season) {
            when (hint) {
                is Hint.Season -> nav.open(Overlay.Thing(hint.thing.id))
                is Hint.Fits -> nav.open(Overlay.Kid(hint.kid.id))
                is Hint.Lent -> nav.open(Overlay.Thing(hint.thing.id))
                is Hint.Soon -> nav.open(Overlay.Thing(hint.thing.id))
                is Hint.Return -> nav.home(Tab.PEOPLE)
                is Hint.Measure -> nav.open(Overlay.Kid(hint.kid.id))
            }
        }
    }
}

@Composable
fun HomeScreen(h: House, nav: Nav) {
    val c = LocalContext.current.container
    var place by rememberSaveable { mutableLongStateOf(h.places.firstOrNull()?.id ?: 0L) }
    if (h.places.none { it.id == place }) place = h.places.firstOrNull()?.id ?: 0L
    var newPlace by remember { mutableStateOf(false) }
    val dusty = h.dusty()
    val hour = LocalTime.now().hour
    val hello = stringResource(when (hour) { in 5..11 -> R.string.hello_morning; in 12..17 -> R.string.hello_day; in 18..22 -> R.string.hello_evening; else -> R.string.hello_night })

    Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = BAR_SPACE)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 18.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            HobFigure(Modifier.size(70.dp).clip(CircleShape).clickable { nav.home(Tab.HOB) }, if (h.level() == 0 && h.home.isNotEmpty()) HobMood.SLEEPY else HobMood.CALM)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(hello, style = T.Hero)
                Text(if (h.home.isEmpty()) stringResource(R.string.hob_knows_nothing) else stringResource(R.string.hob_knows, Words.things(LocalContext.current, h.home.size)), style = T.Small.copy(fontSize = 13.sp))
            }
            IconBtn(Icons.Outlined.Settings, { nav.open(Overlay.Settings) }, desc = stringResource(R.string.settings))
        }
        // Looks like a field; opens the search.
        Row(Modifier.padding(horizontal = 18.dp).padding(top = 16.dp).fillMaxWidth().clip(R18).background(C.card).border(1.dp, C.line, R18).clickable { nav.home(Tab.FIND) }
            .padding(horizontal = 14.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = C.amber, modifier = Modifier.size(21.dp))
            Text(stringResource(R.string.search_hint), style = T.Body.copy(color = C.muted), modifier = Modifier.padding(start = 10.dp))
        }

        val ns = notes(h, nav)
        if (ns.isNotEmpty()) Column(Modifier.padding(horizontal = 18.dp).padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            ns.take(3).forEachIndexed { i, n -> NoteCard(n, lamp = i == 0) }
        }

        SectionTitle(stringResource(R.string.rooms), action = stringResource(R.string.add_room)) { nav.open(Overlay.RoomEdit(place = place)) }
        Row(Modifier.padding(horizontal = 18.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (p in h.places) Chip(p.name, p.id == place) { place = p.id }
            Chip("+", false) { newPlace = true }
        }
        val rooms = h.roomsIn(place)
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            (rooms.map { it as Room? } + listOf(null)).chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    for (r in row) Box(Modifier.weight(1f)) {
                        if (r != null) RoomTile(r, h) { nav.open(Overlay.Room(r.id)) }
                        else AddTile { nav.open(Overlay.RoomEdit(place = place)) }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }

        if (dusty.isNotEmpty()) {
            SectionTitle(stringResource(R.string.clear_out))
            Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Em("🕸")
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(stringResource(R.string.dusty_t, Words.things(LocalContext.current, dusty.size)), style = T.Strong)
                        Text(stringResource(R.string.dusty_b), style = T.Small)
                    }
                }
                Btn(stringResource(R.string.clear_out_go), { nav.open(Overlay.ClearOut) }, Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }

    if (newPlace) {
        var name by remember { mutableStateOf("") }
        Sheet(stringResource(R.string.new_place), { newPlace = false }) {
            Field(name, { name = it }, null, hint = stringResource(R.string.new_place_hint))
            Text(stringResource(R.string.new_place_b), style = T.Small, modifier = Modifier.padding(top = 10.dp))
            Btn(stringResource(R.string.add_btn), {
                if (name.isNotBlank()) c.write { place = addPlace(name); newPlace = false }
            }, Modifier.fillMaxWidth().padding(top = 16.dp), enabled = name.isNotBlank())
        }
    }
}

@Composable
fun NoteCard(n: Note, lamp: Boolean) {
    val bg = if (lamp) Modifier.background(Brush.linearGradient(listOf(C.amber, C.amber2))) else Modifier.background(C.card).border(1.dp, C.line, R18)
    Row(Modifier.fillMaxWidth().clip(R18).then(bg).clickable(onClick = n.go).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Em(n.emoji, size = 42.dp, bg = if (lamp) Color.White.copy(alpha = .35f) else C.card2)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(n.title, style = T.Strong.copy(fontSize = 14.sp, color = if (lamp) C.onAmber else C.ink), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(n.sub, style = T.Small.copy(color = if (lamp) C.onAmber.copy(alpha = .7f) else C.muted), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val GREY = ColorMatrix().apply { setToSaturation(0f) }

@Composable
fun RoomTile(r: Room, h: House, onClick: () -> Unit) {
    val n = h.thingsIn(r.id).size
    val lit = h.lit(r.id)
    val glow = h.thingsIn(r.id).any { t -> h.next(t)?.second?.let { it <= 90 } == true || t.lent || (t.season != 0 && h.daysToMonth(t.season) <= 50) }
    val dash = if (!lit) Modifier.drawBehind {
        drawRoundRect(Color.White.copy(alpha = .14f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
    } else Modifier.border(1.dp, C.line, R18)
    Column(Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(R18).background(if (lit) C.card else C.dim).then(dash).clickable(onClick = onClick).padding(11.dp)) {
        Row {
            Text(r.emoji, fontSize = 24.sp, modifier = if (lit) Modifier else Modifier.alpha(.4f))
            Box(Modifier.weight(1f))
            if (glow) Box(Modifier.size(9.dp).drawBehind {
                drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = .8f), Color.Transparent)), radius = size.minDimension * 1.4f)
            }.clip(CircleShape).background(C.amber))
        }
        Text(r.name, style = T.Strong.copy(fontSize = 13.sp, color = if (lit) C.ink else C.muted), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(if (lit) pluralStringResource(R.plurals.things, n, n) else stringResource(R.string.dark_corner), style = T.Tiny.copy(fontSize = 11.sp))
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(96.dp).clip(R18).drawBehind {
        drawRoundRect(C.ink.copy(alpha = .22f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
    }.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("+", style = T.Title.copy(color = C.amberText))
        Text(stringResource(R.string.room), style = T.Tiny)
    }
}

