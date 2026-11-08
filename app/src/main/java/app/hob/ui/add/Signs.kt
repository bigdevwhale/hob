package app.hob.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.ui.components.Btn
import app.hob.ui.components.Field
import app.hob.ui.components.Label
import app.hob.ui.components.R14
import app.hob.ui.theme.C

/** Signs to pick from, by kind (#3). The first sign of the first group is the default. */
val THING_SIGNS: List<Pair<Int, List<String>>> = listOf(
    R.string.signs_clothes to listOf("👕", "👚", "🧥", "🥼", "🦺", "👖", "👗", "👘", "🩱", "🩳", "🧦", "🧤", "🧣", "🧢", "👒", "🎩", "🧶", "🧵", "👜", "👛", "🎒", "🧳", "☂️", "👓"),
    R.string.signs_shoes_sport to listOf("👟", "👞", "👠", "👢", "🥾", "🩴", "⛸️", "🎿", "🛷", "🛼", "🚲", "🛴", "⚽", "🏀", "🎾", "🏐", "🏓", "🏸", "🥊", "🎣", "⛺", "🏕️", "🧗", "🏊"),
    R.string.signs_tech to listOf("📱", "💻", "🖥️", "⌨️", "🖱️", "🖨️", "📷", "📹", "🎥", "🎧", "🎮", "🕹️", "📺", "📻", "🔌", "🔋", "💡", "🔦", "🕯️", "⏰", "⌚", "📡", "💾", "💿"),
    R.string.signs_tools to listOf("🧰", "🔨", "🪛", "🔧", "🪚", "⚒️", "🪓", "🔩", "⚙️", "🪜", "🧲", "📏", "📐", "✂️", "🖌️", "🪣", "🧹", "🧽", "🧯", "🪤", "🔒", "🗝️", "⛓️", "🪝"),
    R.string.signs_kitchen to listOf("🍳", "🍲", "🥘", "🍽️", "🥄", "🍴", "🔪", "🥢", "🫖", "☕", "🍵", "🥛", "🍷", "🧃", "🫙", "🥫", "🧂", "🍯", "🍞", "🧁", "🎂", "🥡", "🧊", "🍶"),
    R.string.signs_papers to listOf("📄", "📃", "📑", "🧾", "📋", "🗂️", "📁", "📒", "📔", "📕", "📚", "📖", "✉️", "📦", "🪪", "💳", "💵", "🗺️", "📅", "🖊️", "✏️", "🖍️", "📌", "📎"),
    R.string.signs_kids to listOf("👶", "🍼", "🧸", "🪀", "🪁", "🧩", "🎲", "🧱", "🪆", "🚂", "🚗", "🛻", "✈️", "🚀", "🦖", "🐻", "🐰", "🎨", "🖍️", "📏", "🎠", "🛝", "🎈", "🎒"),
    R.string.signs_holidays to listOf("🎄", "🎅", "⛄", "🎃", "🎁", "🎀", "🎈", "🎉", "🎊", "🪅", "🕯️", "🪔", "🧨", "🎆", "💐", "🌹", "🥚", "🐣", "❤️", "💍", "🎗️", "🏮", "🪩", "🎵"),
    R.string.signs_garden to listOf("🌱", "🪴", "🌵", "🌻", "🌷", "🌳", "🍂", "🌾", "🪵", "🔥", "🧺", "🪣", "🚿", "🛁", "🧴", "🧼", "🪥", "🧻", "🛏️", "🛋️", "🪑", "🚪", "🪟", "🖼️"),
    R.string.signs_health_hobby to listOf("💊", "🩹", "🩺", "🌡️", "💉", "🧴", "🎸", "🎹", "🎻", "🥁", "🎺", "📷", "🧵", "🪡", "🧶", "🎨", "♟️", "🃏", "🪀", "🔭", "🔬", "🐟", "🐕", "🐈"),
)

val ROOM_SIGNS = listOf(
    "🚪", "🍳", "🧺", "🛏️", "🛋️", "🛁", "🚿", "🪴", "🧸", "📦", "🪵", "🚗", "🕯️", "🧳", "🧰", "📚", "🎒", "🏚️", "🧹", "🪜",
    "🌿", "🏠", "🏡", "🏢", "🏕️", "🛖", "🏗️", "🚲", "👕", "🖥️", "🎮", "🎨", "🎸", "🪑", "🍷", "🧊", "🔧", "🐕", "👶", "🌳",
)

/**
 * A grid of [signs] with [chosen] marked, and a field for a sign of one's own typed from the keyboard's
 * emoji (#3). [columns] signs a row.
 */
@Composable
fun SignGrid(signs: List<String>, chosen: String, columns: Int, onPick: (String) -> Unit) {
    signs.chunked(columns).forEach { row ->
        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (e in row) Box(Modifier.weight(1f).aspectRatio(1f).clip(R14).background(if (chosen == e) C.amber.copy(alpha = .3f) else C.card)
                .border(1.dp, if (chosen == e) C.amber else C.line, R14).clickable { onPick(e) }, contentAlignment = Alignment.Center) {
                Text(e, fontSize = 19.sp)
            }
            repeat(columns - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

/** A sign typed in by hand: the first emoji (or character) of what was typed. */
@Composable
fun OwnSign(onPick: (String) -> Unit) {
    var own by remember { mutableStateOf("") }
    Label(stringResource(R.string.sign_own))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Field(own, { own = it }, null, Modifier.weight(1f), hint = stringResource(R.string.sign_own_hint))
        Box(Modifier.padding(start = 8.dp)) {
            Btn("OK", { firstSign(own)?.let(onPick); own = "" }, small = true)
        }
    }
}

/** The first grapheme of [s]: one emoji, with its joiners and variation marks, or one letter. */
fun firstSign(s: String): String? {
    val t = s.trim().ifEmpty { return null }
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(t)
    return t.substring(0, it.next().coerceAtLeast(1))
}

/** The picking sheet's body for a thing's sign: the groups, then one's own. */
@Composable
fun ThingSigns(chosen: String, onPick: (String) -> Unit) {
    Column {
        THING_SIGNS.forEachIndexed { i, (title, signs) ->
            Label(stringResource(title), top = if (i == 0) 0.dp else 12.dp)
            SignGrid(signs, chosen, 8, onPick)
        }
        OwnSign(onPick)
    }
}
