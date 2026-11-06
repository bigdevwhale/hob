package app.hob.ui.add

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hob.R
import app.hob.container
import app.hob.domain.Guess
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.components.Btn
import app.hob.ui.components.Field
import app.hob.ui.components.Label
import app.hob.ui.components.MenuRow
import app.hob.ui.components.Sheet
import app.hob.ui.house.Picker
import app.hob.ui.house.Where
import app.hob.ui.house.settle
import app.hob.ui.theme.T

/** What the «+» offers: a thing (with or without the camera), a box, a room, a child's measurements, something borrowed. */
@Composable
fun AddMenu(h: House, nav: Nav) {
    val c = LocalContext.current.container
    var step by remember { mutableIntStateOf(0) } // 0 menu, 1 box, 2 borrowed
    fun close() { nav.menu = false }
    when (step) {
        0 -> Sheet(stringResource(R.string.add_t), ::close) {
            MenuRow("📷", stringResource(R.string.add_photo), stringResource(R.string.add_photo_sub)) { nav.open(Overlay.Edit(camera = true)) }
            MenuRow("✍️", stringResource(R.string.add_typed), stringResource(R.string.add_typed_sub)) { nav.open(Overlay.Edit()) }
            MenuRow("📦", stringResource(R.string.add_box), stringResource(R.string.add_box_sub)) { step = 1 }
            MenuRow("🚪", stringResource(R.string.add_room_m), stringResource(R.string.add_room_m_sub)) { nav.open(Overlay.RoomEdit()) }
            MenuRow("📏", stringResource(R.string.add_measure), stringResource(R.string.add_measure_sub)) {
                nav.open(h.kids.firstOrNull()?.let { Overlay.Kid(it.id) } ?: Overlay.KidEdit())
            }
            MenuRow("🤝", stringResource(R.string.add_borrowed), stringResource(R.string.add_borrowed_sub)) { step = 2 }
        }
        1 -> {
            var w by remember { mutableStateOf(h.settle(Where())) }
            var label by remember { mutableStateOf("") }
            Sheet(stringResource(R.string.add_box), ::close) {
                Field(label, { label = it }, stringResource(R.string.box_label_q), hint = stringResource(R.string.new_box_hint))
                Text(stringResource(R.string.new_box_b, (h.boxes.maxOfOrNull { it.num } ?: 0) + 1), style = T.Small, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                Picker(h, w.copy(box = 0), boxes = false) { w = it.copy(box = 0) }
                Btn(stringResource(R.string.add_btn), {
                    c.write { val id = addBox(w.spot, label); close(); nav.open(Overlay.Box(id)) }
                }, Modifier.fillMaxWidth().padding(top = 18.dp), enabled = w.spot != 0L)
            }
        }
        else -> {
            var name by remember { mutableStateOf("") }
            var from by remember { mutableStateOf("") }
            Sheet(stringResource(R.string.add_borrowed), ::close) {
                Field(name, { name = it }, stringResource(R.string.borrowed_what), hint = stringResource(R.string.borrowed_what_hint))
                Box(Modifier.padding(top = 12.dp)) { Field(from, { from = it }, stringResource(R.string.borrowed_from), hint = stringResource(R.string.borrowed_from_hint)) }
                Label(stringResource(R.string.borrowed_b), top = 10.dp)
                Btn(stringResource(R.string.add_btn), {
                    c.write { borrow(name, Guess.of(name)?.emoji ?: "📦", from); close() }
                }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = name.isNotBlank() && from.isNotBlank())
            }
        }
    }
}
