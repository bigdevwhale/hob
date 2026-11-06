package app.hob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.container
import app.hob.data.Settings
import app.hob.ui.theme.C
import app.hob.ui.theme.T

/** Screens that explain themselves on the first visit: the hob says what this place is for. */
enum class TipOf(val text: Int) {
    HOME(R.array.tip_home), FIND(R.array.tip_find), PEOPLE(R.array.tip_people), HOB(R.array.tip_hob),
    ROOM(R.array.tip_room), THING(R.array.tip_thing), ADD(R.array.tip_add), CLEAR(R.array.tip_clear), KID(R.array.tip_kid),
}

@Composable
fun Tip(tip: TipOf, s: Settings) {
    val c = LocalContext.current.container
    var gone by remember { mutableStateOf(false) }
    if (gone || s.tipsOff || tip.name in s.tips) return
    val lines = stringArrayResource(tip.text)
    fun close(all: Boolean) {
        gone = true
        c.write { settings.update { it.copy(tips = it.tips + tip.name, tipsOff = it.tipsOff || all) } }
    }
    Scrim({ close(false) }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(if (C.dark) Color(0xFF26352F) else C.bg)
            .navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HobFigure(Modifier.size(58.dp))
                Text(lines[0], style = T.Title.copy(fontSize = 22.sp), modifier = Modifier.padding(start = 10.dp).weight(1f))
            }
            lines.drop(1).forEachIndexed { k, line ->
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(C.amber.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                        Text((k + 1).toString(), style = T.Strong.copy(fontSize = 12.sp, color = C.amberText))
                    }
                    Text(line, style = T.Body.copy(fontSize = 14.5.sp), modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
            }
            Btn(stringResource(R.string.tip_ok), { close(false) }, Modifier.padding(top = 20.dp).fillMaxWidth())
            Text(stringResource(R.string.tip_off), style = T.Small, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { close(true) }.padding(vertical = 12.dp))
        }
    }
}
