package app.hob.ui.people

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import app.hob.R
import app.hob.container
import app.hob.data.Size
import app.hob.domain.Growth
import app.hob.domain.House
import app.hob.ui.BAR_SPACE
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.components.Avatar
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Em
import app.hob.ui.components.Kind
import app.hob.ui.components.R14
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.ThingRow
import app.hob.ui.components.TopBar
import app.hob.ui.span
import app.hob.ui.things
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import kotlin.math.roundToInt

@Composable
fun PeopleScreen(h: House, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val lent = h.lentOut()
    val here = h.stillHere()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = BAR_SPACE)) {
        TopBar(stringResource(R.string.people_t), stringResource(R.string.people_sub))

        SectionTitle(stringResource(R.string.lent_out), note = if (lent.isEmpty()) null else lent.size.toString())
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
            if (lent.isEmpty()) Text(stringResource(R.string.lent_none), style = T.Small)
            for (t in lent) ThingRow(t, stringResource(R.string.lent_line, t.lentTo, span(h.day - t.lentDay)), { nav.open(Overlay.Thing(t.id)) }, right = {
                Btn(stringResource(R.string.remind), {
                    runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.remind_text, t.name.lowercase())), null)) }
                }, small = true)
            })
        }

        SectionTitle(stringResource(R.string.borrowed_t), note = if (here.isEmpty()) null else here.size.toString())
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
            if (here.isEmpty()) Text(stringResource(R.string.borrowed_none), style = T.Small)
            for (b in here) Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Em(b.emoji)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(b.name, style = T.Strong)
                    Text(stringResource(R.string.borrowed_line, b.from, span(h.day - b.day)), style = T.Small)
                }
                Btn(stringResource(R.string.gave_back), { c.write { giveBack(b) } }, small = true, kind = Kind.GHOST)
            }
        }

        SectionTitle(stringResource(R.string.kids_t), action = stringResource(R.string.add_kid)) { nav.open(Overlay.KidEdit()) }
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 12.dp) {
            if (h.kids.isEmpty()) {
                Text(stringResource(R.string.kids_none), style = T.Small)
                Btn(stringResource(R.string.add_kid_long), { nav.open(Overlay.KidEdit()) }, Modifier.padding(top = 12.dp), small = true)
            }
            for (k in h.kids) {
                val wait = h.home.count { t -> t.sized && (h.days(t, k)?.let { it <= 365 } == true) }
                val height = Growth.estimate(Size.HEIGHT, k, h.measures, h.day).roundToInt()
                Row(Modifier.fillMaxWidth().clip(R14).clickable { nav.open(Overlay.Kid(k.id)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(k.name, C.kid(k.color))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(k.name, style = T.Strong)
                        Text(stringResource(R.string.kid_line, height, if (wait == 0) stringResource(R.string.nothing_waits) else stringResource(R.string.things_wait, things(wait))), style = T.Small)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = C.muted)
                }
            }
        }

        SectionTitle(stringResource(R.string.family_t))
        Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
            Text(stringResource(R.string.family_b), style = T.Small)
            Btn(stringResource(R.string.family_copy), { nav.open(Overlay.Settings) }, Modifier.padding(top = 12.dp), Kind.GHOST, small = true)
        }
    }
}
