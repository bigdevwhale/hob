package app.hob.ui.house

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.container
import app.hob.data.now
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.components.Btn
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Field
import app.hob.ui.components.Kind
import app.hob.ui.components.Label
import app.hob.ui.components.R14
import app.hob.ui.components.TopBar
import app.hob.ui.theme.C
import app.hob.ui.theme.T


/** A new room with its spots, or an existing one's name, sign and place. */
@Composable
fun RoomEdit(o: Overlay.RoomEdit, h: House, nav: Nav) {
    val c = LocalContext.current.container
    val old = h.roomOf[o.id]
    var name by remember { mutableStateOf(old?.name ?: "") }
    var emoji by remember { mutableStateOf(old?.emoji ?: "📦") }
    var place by remember { mutableStateOf(old?.place ?: o.place.takeIf { p -> h.places.any { it.id == p } } ?: h.places.firstOrNull()?.id ?: 0L) }
    val presets = stringResource(R.string.spot_presets).split('|')
    val spots = remember { mutableStateListOf<String>() }
    var own by remember { mutableStateOf("") }
    val suggestions = stringResource(R.string.room_presets).split('|').map { it.split(':') }

    Column(Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(stringResource(if (old == null) R.string.new_room else R.string.room_settings), null, { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            Field(name, { name = it }, stringResource(R.string.room_name), hint = stringResource(R.string.room_name_hint))
            if (old == null && name.isBlank()) Box(Modifier.padding(top = 9.dp)) {
                Chips { for (s in suggestions) Chip("${s[1]} ${s[0]}", false) { name = s[0]; emoji = s[1] } }
            }
            if (h.places.size > 1) {
                Label(stringResource(R.string.room_where))
                Chips { for (p in h.places) Chip(p.name, place == p.id) { place = p.id } }
            }
            Label(stringResource(R.string.room_sign))
            app.hob.ui.add.SignGrid(app.hob.ui.add.ROOM_SIGNS, emoji, 10) { emoji = it }
            app.hob.ui.add.OwnSign { emoji = it }
            if (old == null) {
                Label(stringResource(R.string.room_spots))
                Chips {
                    for (s in (presets + spots).distinct()) Chip(s, s in spots) { if (s in spots) spots.remove(s) else spots.add(s) }
                }
                Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Field(own, { own = it }, null, Modifier.weight(1f), hint = stringResource(R.string.own_spot))
                    Box(Modifier.padding(start = 8.dp)) { Btn("+", { if (own.isNotBlank()) { spots.add(own.trim()); own = "" } }, small = true) }
                }
                Text(stringResource(R.string.room_spots_b), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            }
            Btn(stringResource(if (old == null) R.string.add_room_btn else R.string.save), {
                c.write {
                    if (old == null) {
                        val id = addRoom(place, name, emoji, spots.ifEmpty { listOf(presets[0]) })
                        nav.replace(Overlay.Room(id))
                    } else {
                        dao.save(old.copy(name = name.trim(), emoji = emoji, place = place, updated = now()))
                        nav.close()
                    }
                }
            }, Modifier.fillMaxWidth().padding(top = 22.dp), enabled = name.isNotBlank())
            if (old != null && h.thingsIn(old.id).isEmpty()) Btn(stringResource(R.string.delete_room), {
                c.write { dropRoom(old) }
                nav.stack.clear()
            }, Modifier.padding(top = 10.dp).fillMaxWidth(), Kind.GHOST, small = true)
        }
    }
}
