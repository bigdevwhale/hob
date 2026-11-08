package app.hob.ui.add

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.hob.R
import app.hob.container
import app.hob.data.Size
import app.hob.data.Thing
import app.hob.domain.Cat
import app.hob.domain.Growth
import app.hob.domain.Guess
import app.hob.domain.House
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.Words
import app.hob.ui.catName
import app.hob.ui.catSign
import app.hob.ui.components.Btn
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Field
import app.hob.ui.components.Kind
import app.hob.ui.components.Label
import app.hob.ui.components.Pic
import app.hob.ui.components.R14
import app.hob.ui.components.R18
import app.hob.ui.components.Sheet
import app.hob.ui.components.TopBar
import app.hob.ui.house.Picker
import app.hob.ui.house.Where
import app.hob.ui.house.settle
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import kotlinx.coroutines.launch
import java.io.File


/** A new thing, or editing one: photo, name (the sign and kind guessed from it), where it lies, size for children, season. */
@Composable
fun EditThing(o: Overlay.Edit, h: House, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val old = h.thing(o.id)
    var name by remember { mutableStateOf(old?.name ?: o.name) }
    var emoji by remember { mutableStateOf(old?.emoji ?: Guess.of(o.name)?.emoji ?: "📦") }
    var cat by remember { mutableStateOf(old?.cat ?: Guess.of(o.name)?.cat?.key ?: Cat.OTHER.key) }
    var manual by remember { mutableStateOf(old != null) }
    var where by remember { mutableStateOf(h.settle(if (old != null) Where(spot = old.spot, box = old.box) else Where(o.room, o.spot, o.box))) }
    var sizeKind by remember { mutableStateOf(old?.sizeKind ?: Size.NONE) }
    var sizeLabel by remember { mutableStateOf(old?.sizeLabel ?: "") }
    var season by remember { mutableStateOf(old?.season ?: 0) }
    var note by remember { mutableStateOf(old?.note ?: "") }
    val past = remember { mutableStateListOf<String>() }
    var pastOwn by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var shot by remember { mutableStateOf<Uri?>(null) }
    var picking by remember { mutableStateOf(false) }
    var newCat by remember { mutableStateOf<String?>(null) }

    // The sign, the kind and how the size is measured follow the name until the owner picks them by hand.
    LaunchedEffect(name) {
        if (manual) return@LaunchedEffect
        Guess.of(name)?.let { g ->
            emoji = g.emoji; cat = g.cat.key
            if (sizeKind == Size.NONE && g.size != Size.NONE && h.kids.isNotEmpty()) sizeKind = g.size
        }
    }
    LaunchedEffect(photo) { photo?.let { u -> preview = c.photos.read(u, 900)?.asImageBitmap() } }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) photo = shot }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u -> if (u != null) photo = u }
    fun shoot() {
        val f = File(c.photos.inbox, "shot_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        shot = uri
        runCatching { camera.launch(uri) }.onFailure { Toast.makeText(ctx, R.string.no_camera, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) { if (o.camera && old == null) shoot() }

    val range = Growth.parse(sizeKind, sizeLabel)

    Column(Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(bottom = 24.dp)) {
        TopBar(stringResource(if (old == null) R.string.new_thing else R.string.edit_thing), null, { nav.close() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp).clip(R18).background(C.card2).clickable { picking = true }, contentAlignment = Alignment.Center) {
                    val p = preview
                    when {
                        p != null -> Image(p, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        old?.photo == true -> Pic(old, size = 84.dp, big = true)
                        else -> Text(emoji, fontSize = 40.sp)
                    }
                }
                Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(stringResource(R.string.photo_camera), { shoot() }, Modifier.fillMaxWidth(), Kind.SOFT, small = true, icon = Icons.Outlined.PhotoCamera)
                    Btn(stringResource(R.string.photo_gallery), { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.fillMaxWidth(), Kind.SOFT, small = true, icon = Icons.Outlined.PhotoLibrary)
                }
            }
            Field(name, { name = it }, stringResource(R.string.thing_name), Modifier.padding(top = 16.dp), hint = stringResource(R.string.thing_name_hint))
            Text(stringResource(R.string.tap_sign), style = T.Small, modifier = Modifier.padding(top = 6.dp, start = 2.dp).clickable { picking = true })

            Label(stringResource(R.string.thing_cat))
            Chips {
                for (k in h.catKeys()) Chip("${catSign(k)} ${catName(k)}", cat == k) { cat = k; manual = true }
                Chip(stringResource(R.string.own_cat_plus), false) { newCat = "" }
            }
            newCat?.let { v ->
                Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Field(v, { newCat = it }, null, Modifier.weight(1f), hint = stringResource(R.string.own_cat_hint))
                    Box(Modifier.padding(start = 8.dp)) {
                        Btn(stringResource(R.string.add_btn), {
                            val name = v.trim()
                            if (name.isNotEmpty()) {
                                c.write { settings.update { s -> s.copy(cats = (s.cats + name).distinct()) } }
                                cat = app.hob.domain.OwnCat.key(name); manual = true; newCat = null
                            }
                        }, small = true)
                    }
                }
            }

            Label(stringResource(R.string.thing_where))
            if (h.rooms.isEmpty()) Text(stringResource(R.string.no_rooms), style = T.Small)
            else Picker(h, where) { where = it }

            Label(stringResource(R.string.grow_q))
            Chips {
                Chip(stringResource(R.string.grow_no), sizeKind == Size.NONE) { sizeKind = Size.NONE }
                Chip(stringResource(R.string.grow_height), sizeKind == Size.HEIGHT) { sizeKind = Size.HEIGHT; manual = true }
                Chip(stringResource(R.string.grow_foot), sizeKind == Size.FOOT) { sizeKind = Size.FOOT; manual = true }
            }
            if (sizeKind != Size.NONE) {
                Field(sizeLabel, { sizeLabel = it }, null, Modifier.padding(top = 9.dp), hint = stringResource(if (sizeKind == Size.HEIGHT) R.string.size_hint_h else R.string.size_hint_f))
                val fits = range?.let { r ->
                    val probe = Thing(name = "", emoji = "", cat = "", spot = 0, added = 0, touched = 0, sizeKind = sizeKind, sizeMin = r.first, sizeMax = r.second)
                    Growth.next(probe, h.kids, h.measures, h.day)
                }
                Text(
                    when {
                        range == null -> stringResource(R.string.size_explain)
                        h.kids.isEmpty() -> stringResource(R.string.size_no_kids)
                        fits == null -> stringResource(R.string.size_nobody)
                        else -> stringResource(R.string.size_fits, fits.first.name, Words.eta(ctx, fits.second))
                    }, style = T.Small.copy(color = if (fits != null) C.amberText else C.muted), modifier = Modifier.padding(top = 6.dp, start = 2.dp))
                if (old == null && h.kids.isNotEmpty()) {
                    Label(stringResource(R.string.worn_before))
                    Chips {
                        for (k in h.kids.map { it.name } + past.filter { p -> h.kids.none { it.name == p } }) Chip(k, k in past) { if (k in past) past.remove(k) else past.add(k) }
                    }
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Field(pastOwn, { pastOwn = it }, null, Modifier.weight(1f), hint = stringResource(R.string.worn_other))
                        Box(Modifier.padding(start = 8.dp)) { Btn("+", { if (pastOwn.isNotBlank()) { past.add(pastOwn.trim()); pastOwn = "" } }, small = true) }
                    }
                }
            }

            Label(stringResource(R.string.season_q))
            Chips {
                Chip(stringResource(R.string.season_any), season == 0) { season = 0 }
                for (m in 1..12) Chip(Words.month(ctx, m), season == m) { season = m }
            }

            Field(note, { note = it }, stringResource(R.string.note), Modifier.padding(top = 16.dp), hint = stringResource(R.string.note_hint), lines = 2)

            Btn(stringResource(if (old == null) R.string.remember else R.string.save), {
                val r = range
                val base = (old ?: Thing(name = "", emoji = "", cat = "", spot = 0, added = 0, touched = 0)).copy(
                    name = name.trim(), emoji = emoji, cat = cat, spot = where.spot, box = where.box, note = note.trim(), season = season,
                    sizeKind = if (r == null) Size.NONE else sizeKind, sizeMin = r?.first ?: 0f, sizeMax = r?.second ?: 0f, sizeLabel = if (r == null) "" else sizeLabel.trim(),
                )
                scope.launch {
                    if (old == null) {
                        val id = c.addThing(base, photo, past.toList())
                        val room = h.roomOf[where.room]
                        Toast.makeText(ctx, ctx.getString(R.string.remembered, listOfNotNull(room?.name, h.spotOf[where.spot]?.name).joinToString(", ").lowercase()), Toast.LENGTH_SHORT).show()
                        nav.replace(Overlay.Thing(id))
                    } else {
                        if (old.spot != base.spot || old.box != base.box) c.move(base, base.spot, base.box, listOfNotNull(h.roomOf[where.room]?.name, h.spotOf[where.spot]?.name).joinToString(" · "))
                        else c.save(base)
                        photo?.let { c.setPhoto(old.id, it) }
                        nav.close()
                    }
                }
            }, Modifier.fillMaxWidth().padding(top = 22.dp), enabled = name.isNotBlank() && where.spot != 0L)
            if (old != null) Btn(stringResource(R.string.delete_thing), { c.write { delete(old) }; nav.stack.clear() }, Modifier.padding(top = 10.dp).fillMaxWidth(), Kind.GHOST, small = true)
        }
    }

    if (picking) Sheet(stringResource(R.string.pick_sign), { picking = false }) {
        ThingSigns(emoji) { e -> emoji = e; manual = true; picking = false }
    }
}
