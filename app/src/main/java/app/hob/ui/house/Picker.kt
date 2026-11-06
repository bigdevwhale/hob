package app.hob.ui.house

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hob.R
import app.hob.container
import app.hob.domain.House
import app.hob.ui.components.Btn
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Field
import app.hob.ui.components.Label
import app.hob.ui.theme.T

/** Where something goes: a room, a spot in it, and a box on that spot (or none). */
data class Where(val room: Long = 0, val spot: Long = 0, val box: Long = 0)

/** Fills in what's missing: the first room, its first spot. */
fun House.settle(w: Where): Where {
    val room = (if (w.room != 0L && roomOf[w.room] != null) w.room else spotOf[w.spot]?.room ?: rooms.firstOrNull()?.id) ?: 0L
    val spot = if (spotOf[w.spot]?.room == room) w.spot else spotsIn(room).firstOrNull()?.id ?: 0L
    val box = if (boxOf[w.box]?.spot == spot) w.box else 0L
    return Where(room, spot, box)
}

@Composable
fun Picker(h: House, w: Where, boxes: Boolean = true, onChange: (Where) -> Unit) {
    val c = LocalContext.current.container
    var adding by remember { mutableStateOf<Int?>(null) } // 1 spot, 2 box
    val many = h.places.size > 1
    Column {
        Label(stringResource(R.string.pick_room), top = 4.dp)
        Chips {
            for (r in h.rooms.sortedWith(compareBy({ h.places.indexOfFirst { p -> p.id == it.place } }, { it.sort }))) {
                val place = if (many) h.placeOf[r.place]?.name?.let { " · $it" } ?: "" else ""
                Chip("${r.emoji} ${r.name}$place", w.room == r.id) { onChange(h.settle(Where(room = r.id))) }
            }
        }
        if (w.room != 0L) {
            Label(stringResource(R.string.pick_spot))
            Chips {
                for (s in h.spotsIn(w.room)) Chip(s.name, w.spot == s.id) { onChange(Where(w.room, s.id, 0)) }
                Chip(stringResource(R.string.plus_spot), false) { adding = 1 }
            }
        }
        if (boxes && w.spot != 0L) {
            Label(stringResource(R.string.pick_box))
            Chips {
                Chip(stringResource(R.string.no_box), w.box == 0L) { onChange(w.copy(box = 0)) }
                for (b in h.boxesAt(w.spot)) Chip(stringResource(R.string.box_label, b.num, b.label), w.box == b.id) { onChange(w.copy(box = b.id)) }
                Chip(stringResource(R.string.plus_box), false) { adding = 2 }
            }
        }
    }
    // Inline, not a sheet: the picker itself often sits in a sheet.
    adding?.let { what ->
        var name by remember(what) { mutableStateOf("") }
        Column(Modifier.padding(top = 12.dp)) {
            Field(name, { name = it }, stringResource(if (what == 1) R.string.new_spot else R.string.new_box), hint = stringResource(if (what == 1) R.string.new_spot_hint else R.string.new_box_hint))
            if (what == 1) Column(Modifier.padding(top = 8.dp)) { Chips { for (s in stringResource(R.string.spot_presets).split('|')) Chip(s, name == s) { name = s } } }
            else Text(stringResource(R.string.new_box_b, (h.boxes.maxOfOrNull { it.num } ?: 0) + 1), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            Btn(stringResource(R.string.add_btn), {
                c.write {
                    if (what == 1) { val id = addSpot(w.room, name); onChange(Where(w.room, id, 0)) }
                    else { val id = addBox(w.spot, name); onChange(w.copy(box = id)) }
                    adding = null
                }
            }, Modifier.padding(top = 10.dp), small = true, enabled = what == 2 || name.isNotBlank())
        }
    }
}
