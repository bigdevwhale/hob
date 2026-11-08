package app.hob.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.hob.ui.components.LocalAbove
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.R
import app.hob.container
import app.hob.data.Settings
import app.hob.domain.House
import app.hob.ui.components.HobFigure
import app.hob.ui.components.Tip
import app.hob.ui.components.TipOf
import app.hob.ui.components.room
import app.hob.ui.add.AddMenu
import app.hob.ui.add.EditThing
import app.hob.ui.hob.ClearOut
import app.hob.ui.hob.HobScreen
import app.hob.ui.home.FindScreen
import app.hob.ui.home.HomeScreen
import app.hob.ui.house.BoxScreen
import app.hob.ui.house.RoomEdit
import app.hob.ui.house.RoomScreen
import app.hob.ui.onboarding.Onboarding
import app.hob.ui.people.KidEdit
import app.hob.ui.people.KidScreen
import app.hob.ui.people.PeopleScreen
import app.hob.ui.settings.SettingsPage
import app.hob.ui.thing.ThingScreen
import app.hob.ui.theme.C
import app.hob.ui.theme.T

enum class Tab { HOME, FIND, PEOPLE, HOB }

sealed interface Overlay {
    data class Room(val id: Long) : Overlay
    data class Box(val id: Long) : Overlay
    data class Thing(val id: Long) : Overlay
    /** A new thing (id 0) placed into [room]/[spot]/[box] if given, or editing an existing one. */
    data class Edit(val id: Long = 0, val room: Long = 0, val spot: Long = 0, val box: Long = 0, val camera: Boolean = false, val name: String = "") : Overlay
    /** A new room (id 0) in [place], or an existing room's settings. */
    data class RoomEdit(val id: Long = 0, val place: Long = 0) : Overlay
    data class Kid(val id: Long) : Overlay
    data class KidEdit(val id: Long = 0) : Overlay
    data object ClearOut : Overlay
    data object Settings : Overlay
}

class Nav(tab: Tab, first: Overlay?) {
    var tab by mutableStateOf(tab)
    val stack = mutableStateListOf<Overlay>().apply { if (first != null) add(first) }
    var menu by mutableStateOf(false)
    fun open(o: Overlay) { menu = false; stack.add(o) }
    fun close() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(o: Overlay) { close(); open(o) }
    fun home(t: Tab) { menu = false; tab = t; stack.clear() }
}

@Composable
fun HobRoot(s: Settings, tab: Tab = Tab.HOME, overlay: Overlay? = null) {
    val c = LocalContext.current.container
    if (!s.onboarded) { Onboarding(); return }

    val places by remember { c.dao.places() }.collectAsState(null)
    val rooms by remember { c.dao.rooms() }.collectAsState(null)
    val spots by remember { c.dao.spots() }.collectAsState(null)
    val boxes by remember { c.dao.boxes() }.collectAsState(null)
    val things by remember { c.dao.things() }.collectAsState(null)
    val kids by remember { c.dao.kids() }.collectAsState(null)
    val measures by remember { c.dao.measures() }.collectAsState(emptyList())
    val events by remember { c.dao.events() }.collectAsState(emptyList())
    val borrowed by remember { c.dao.borrowed() }.collectAsState(emptyList())
    val nav = remember { Nav(tab, overlay) }

    val h = House(s, places ?: return, rooms ?: return, spots ?: return, boxes ?: return, things ?: return, kids ?: return, measures, events, borrowed)

    Box(Modifier.fillMaxSize().room()) {
        // Only the top layer is heard by screen readers; the ones under it are still kept, so going back keeps their state.
        Box(Modifier.fillMaxSize().then(if (nav.stack.isNotEmpty()) Modifier.clearAndSetSemantics {} else Modifier)) {
            val above = remember { mutableStateListOf<@Composable () -> Unit>() }
            CompositionLocalProvider(LocalAbove provides above) {
                when (nav.tab) {
                    Tab.HOME -> HomeScreen(h, nav)
                    Tab.FIND -> FindScreen(h, nav)
                    Tab.PEOPLE -> PeopleScreen(h, nav)
                    Tab.HOB -> HobScreen(h, nav)
                }
            }
            StatusScrim()
            TabBar(nav, Modifier.align(Alignment.BottomCenter))
            above.forEach { it() }
        }
        if (nav.stack.isNotEmpty()) BackHandler { nav.close() }
        nav.stack.forEachIndexed { i, o -> Box(Modifier.fillMaxSize().room()
            // A page swallows the touches that land on its empty parts, so they never reach the page under it.
            .clickable(remember { MutableInteractionSource() }, null) {}
            .then(if (i < nav.stack.lastIndex) Modifier.clearAndSetSemantics {} else Modifier)) {
            when (o) {
                is Overlay.Room -> RoomScreen(o.id, h, nav)
                is Overlay.Box -> BoxScreen(o.id, h, nav)
                is Overlay.Thing -> ThingScreen(o.id, h, nav)
                is Overlay.Edit -> EditThing(o, h, nav)
                is Overlay.RoomEdit -> RoomEdit(o, h, nav)
                is Overlay.Kid -> KidScreen(o.id, h, nav)
                is Overlay.KidEdit -> KidEdit(o.id, h, nav)
                Overlay.ClearOut -> ClearOut(h, nav)
                Overlay.Settings -> SettingsPage(h, nav)
            }
            StatusScrim()
        } }
        if (nav.menu) AddMenu(h, nav)
        // The first visit to each screen explains it once.
        if (!nav.menu) {
            val tip = when (val top = nav.stack.lastOrNull()) {
                null -> when (nav.tab) { Tab.HOME -> TipOf.HOME; Tab.FIND -> TipOf.FIND; Tab.PEOPLE -> TipOf.PEOPLE; Tab.HOB -> TipOf.HOB }
                is Overlay.Room -> TipOf.ROOM
                is Overlay.Thing -> TipOf.THING
                is Overlay.Edit -> if (top.id == 0L) TipOf.ADD else null
                Overlay.ClearOut -> TipOf.CLEAR
                is Overlay.Kid -> TipOf.KID
                else -> null
            }
            if (tip != null) androidx.compose.runtime.key(tip) { Tip(tip, s) }
        }
    }
}

/** Home, find, the lamp-coloured «+», people, the hob. */
@Composable
private fun TabBar(nav: Nav, modifier: Modifier) {
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, C.bgDeep.copy(alpha = .96f), C.bgDeep), startY = 0f, endY = 60f))) {
        Row(Modifier.navigationBarsPadding().padding(top = 16.dp, bottom = 6.dp).fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            @Composable
            fun item(t: Tab, label: Int, icon: @Composable (Color) -> Unit) {
                val on = nav.tab == t
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { nav.home(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    icon(if (on) C.amberText else C.muted)
                    Text(stringResource(label), style = T.Tiny.copy(color = if (on) C.amberText else C.muted, fontSize = 10.5.sp), maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                }
            }
            item(Tab.HOME, R.string.tab_home) { Icon(Icons.Outlined.Home, null, tint = it, modifier = Modifier.size(24.dp)) }
            item(Tab.FIND, R.string.tab_find) { Icon(Icons.Outlined.Search, null, tint = it, modifier = Modifier.size(24.dp)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.offset(y = (-6).dp).size(56.dp).shadow(18.dp, CircleShape, ambientColor = C.amber, spotColor = C.amber).clip(CircleShape).background(C.amber)
                    .clickable { nav.menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.add), tint = C.onAmber, modifier = Modifier.size(30.dp))
                }
            }
            item(Tab.PEOPLE, R.string.tab_people) { Icon(Icons.Outlined.Groups, null, tint = it, modifier = Modifier.size(24.dp)) }
            item(Tab.HOB, R.string.tab_hob) { HobFigure(Modifier.size(26.dp), alive = false) }
        }
    }
}

/** Room to leave at the bottom of a tab's list so the bar doesn't hide its end. */
val BAR_SPACE = 110.dp

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** A strip behind the status bar in the room's colour so scrolled lists stay readable under it. */
@Composable
fun StatusScrim() {
    Box(Modifier.fillMaxWidth().windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars).background(C.bg))
}
