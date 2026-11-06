package app.hob.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hob.R
import app.hob.container
import app.hob.ui.components.Btn
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.HobFigure
import app.hob.ui.components.Label
import app.hob.ui.components.room
import app.hob.ui.theme.T

/**
 * Two steps: the hob introduces himself, then the rooms of the flat (and a dacha, if there is one) are chosen from
 * the usual ones. Each comes with its usual spots; everything can be renamed later.
 */
@Composable
fun Onboarding() {
    val c = LocalContext.current.container
    var step by remember { mutableIntStateOf(0) }
    val rooms = stringResource(R.string.room_presets).split('|').map { it.split(':') }
    val dachaRooms = stringResource(R.string.dacha_presets).split('|').map { it.split(':') }
    val chosen = remember { mutableStateListOf<String>().apply { addAll(rooms.take(5).map { it[0] }) } }
    var dacha by remember { mutableStateOf(false) }
    val chosenDacha = remember { mutableStateListOf<String>().apply { addAll(dachaRooms.take(2).map { it[0] }) } }

    Box(Modifier.fillMaxSize().room()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (step == 0) {
                HobFigure(Modifier.padding(top = 50.dp).size(170.dp))
                Text(stringResource(R.string.ob_t), style = T.Hero, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
                Text(stringResource(R.string.ob_b), style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text(stringResource(R.string.ob_b2), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Btn(stringResource(R.string.ob_go), { step = 1 }, Modifier.padding(top = 30.dp).fillMaxWidth())
            } else {
                HobFigure(Modifier.padding(top = 10.dp).size(90.dp))
                Text(stringResource(R.string.ob_rooms_t), style = T.Title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.ob_rooms_b), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Column(Modifier.fillMaxWidth()) {
                    Label(stringResource(R.string.place_flat), top = 18.dp)
                    Chips { for (r in rooms) Chip("${r[1]} ${r[0]}", r[0] in chosen) { if (r[0] in chosen) chosen.remove(r[0]) else chosen.add(r[0]) } }
                    Label(stringResource(R.string.ob_dacha_q))
                    Chips {
                        Chip(stringResource(R.string.no), !dacha) { dacha = false }
                        Chip(stringResource(R.string.yes), dacha) { dacha = true }
                    }
                    if (dacha) {
                        Label(stringResource(R.string.place_dacha))
                        Chips { for (r in dachaRooms) Chip("${r[1]} ${r[0]}", r[0] in chosenDacha) { if (r[0] in chosenDacha) chosenDacha.remove(r[0]) else chosenDacha.add(r[0]) } }
                    }
                }
                val flatName = stringResource(R.string.place_flat)
                val dachaName = stringResource(R.string.place_dacha)
                Btn(stringResource(R.string.ob_done), {
                    c.write {
                        val flat = addPlace(flatName)
                        for (r in rooms.filter { it[0] in chosen }) addRoom(flat, r[0], r[1], r.drop(2))
                        if (dacha) {
                            val d = addPlace(dachaName)
                            for (r in dachaRooms.filter { it[0] in chosenDacha }) addRoom(d, r[0], r[1], r.drop(2))
                        }
                        settings.update { it.copy(onboarded = true) }
                    }
                }, Modifier.padding(top = 26.dp).fillMaxWidth(), enabled = chosen.isNotEmpty())
            }
        }
    }
}
