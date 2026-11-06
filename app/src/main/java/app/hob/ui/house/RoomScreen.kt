package app.hob.ui.house

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.data.now
import app.hob.container
import app.hob.data.Box as BoxRow
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.ago
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Field
import app.hob.ui.components.IconBtn
import app.hob.ui.components.Kind
import app.hob.ui.components.R14
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.Sheet
import app.hob.ui.components.Tag
import app.hob.ui.components.ThingRow
import app.hob.ui.components.TopBar
import app.hob.ui.thing.KidTag
import app.hob.ui.things
import app.hob.ui.theme.C
import app.hob.ui.theme.T

/** A little cardboard box with its number. */
@Composable
fun BoxIcon(num: Int) {
    Box(Modifier.size(42.dp, 36.dp)) {
        Box(Modifier.padding(top = 5.dp).size(42.dp, 31.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFB08A5C)))
        Box(Modifier.padding(top = 2.dp).size(42.dp, 9.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFC69C69)))
        Text("$num", style = T.Tiny.copy(color = Color(0xFF3B2A16), fontSize = 11.sp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
    }
}

@Composable
fun BoxLine(b: BoxRow, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(R14).background(C.card2).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxIcon(b.num)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(b.label.ifBlank { stringResource(R.string.box_n, b.num) }, style = T.Label.copy(fontStyle = FontStyle.Italic, fontSize = 16.sp))
            Text(pluralStringResource(R.plurals.things, count, count), style = T.Small)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
    }
}

@Composable
fun RoomScreen(id: Long, h: House, nav: Nav) {
    val c = LocalContext.current.container
    val r = h.roomOf[id] ?: run { nav.close(); return }
    val inside = h.thingsIn(id)
    var newSpot by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar("${r.emoji} ${r.name}", listOfNotNull(h.placeOf[r.place]?.name, if (inside.isEmpty()) stringResource(R.string.empty_yet) else things(inside.size)).joinToString(" · "), { nav.close() }) {
            IconBtn(Icons.Outlined.Tune, { nav.open(Overlay.RoomEdit(id = id)) }, desc = stringResource(R.string.room_settings))
        }
        if (!h.lit(id)) Card(Modifier.padding(horizontal = 18.dp).padding(top = 8.dp).fillMaxWidth().drawBehind {
            drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = .22f), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.width * .6f), center = Offset(size.width / 2, 0f), radius = size.width * .6f)
        }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🕯️", fontSize = 40.sp)
                Text(stringResource(R.string.dark_t), style = T.Heading, modifier = Modifier.padding(top = 6.dp))
                Text(stringResource(R.string.dark_b), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Btn(stringResource(R.string.dark_go), { nav.open(Overlay.Edit(room = id)) }, Modifier.padding(top = 14.dp))
            }
        }
        for (s in h.spotsIn(id)) {
            val here = h.thingsAt(s.id)
            val boxes = h.boxesAt(s.id)
            SectionTitle(s.name.replaceFirstChar { it.uppercase() }, note = if (here.isEmpty()) null else things(here.size))
            Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
                for (b in boxes) Box(Modifier.padding(bottom = 8.dp)) { BoxLine(b, h.thingsInBox(b.id).size) { nav.open(Overlay.Box(b.id)) } }
                for (t in here.filter { it.box == 0L }) ThingRow(t, null, { nav.open(Overlay.Thing(t.id)) },
                    tag = { if (h.next(t) != null || t.lent) KidTag(h, t) else Tag(stringResource(R.string.touched_ago, ago(h.day - t.touched))) })
                if (here.isEmpty() && boxes.isEmpty()) Text(stringResource(R.string.spot_empty), style = T.Small, modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.padding(top = 4.dp)) {
                    Btn(stringResource(R.string.put_here), { nav.open(Overlay.Edit(room = id, spot = s.id)) }, kind = Kind.GHOST, small = true)
                    if (here.isEmpty() && boxes.isEmpty()) {
                        Box(Modifier.weight(1f))
                        Icon(Icons.Outlined.Delete, stringResource(R.string.delete), tint = C.muted, modifier = Modifier.clip(R14).clickable { c.write { dao.drop(s) } }.padding(8.dp).size(20.dp))
                    }
                }
            }
        }
        Btn(stringResource(R.string.plus_spot_long), { newSpot = true }, Modifier.padding(18.dp).fillMaxWidth(), Kind.GHOST)
    }

    if (newSpot) {
        var name by remember { mutableStateOf("") }
        Sheet(stringResource(R.string.new_spot), { newSpot = false }) {
            Field(name, { name = it }, null, hint = stringResource(R.string.new_spot_hint))
            Box(Modifier.padding(top = 10.dp)) { Chips { for (s in stringResource(R.string.spot_presets).split('|')) Chip(s, name == s) { name = s } } }
            Btn(stringResource(R.string.add_btn), { c.write { addSpot(id, name) }; newSpot = false }, Modifier.fillMaxWidth().padding(top = 16.dp), enabled = name.isNotBlank())
        }
    }
}

@Composable
fun BoxScreen(id: Long, h: House, nav: Nav) {
    val c = LocalContext.current.container
    val b = h.boxOf[id] ?: run { nav.close(); return }
    val spot = h.spotOf[b.spot]
    val room = spot?.let { h.roomOf[it.room] }
    val inside = h.thingsInBox(id)
    var rename by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(stringResource(R.string.box_n, b.num).replaceFirstChar { it.uppercase() }, b.label.ifBlank { null }?.let { "«$it»" }, { nav.close() })
        Card(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoxIcon(b.num)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(stringResource(R.string.lies_at), style = T.Tiny)
                    Text(listOfNotNull(room?.let { "${it.emoji} ${it.name}" }, spot?.name).joinToString(" · "), style = T.Strong)
                }
            }
            Text(stringResource(R.string.box_write_b, b.num), style = T.Small, modifier = Modifier.padding(top = 10.dp))
        }
        SectionTitle(stringResource(R.string.inside), note = things(inside.size))
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
            if (inside.isEmpty()) Text(stringResource(R.string.box_empty), style = T.Small)
            for (t in inside) ThingRow(t, null, { nav.open(Overlay.Thing(t.id)) }, tag = { KidTag(h, t) })
        }
        Row(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp)) {
            Btn(stringResource(R.string.put_in_box), { nav.open(Overlay.Edit(room = room?.id ?: 0, spot = b.spot, box = b.id)) }, Modifier.weight(1f))
            Box(Modifier.size(9.dp))
            Btn(stringResource(R.string.rename), { rename = true }, Modifier.weight(1f), Kind.GHOST)
        }
        if (inside.isEmpty()) Btn(stringResource(R.string.delete_box), { c.write { dao.drop(b) }; nav.close() }, Modifier.padding(18.dp), Kind.GHOST, small = true)
    }

    if (rename) {
        var name by remember { mutableStateOf(b.label) }
        Sheet(stringResource(R.string.rename), { rename = false }) {
            Field(name, { name = it }, null, hint = stringResource(R.string.new_box_hint))
            Btn(stringResource(R.string.save), { c.write { dao.save(b.copy(label = name.trim(), updated = now())) }; rename = false }, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    }
}
