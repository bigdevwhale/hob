package app.hob.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import app.hob.R
import app.hob.domain.Cat
import app.hob.domain.House
import app.hob.ui.BAR_SPACE
import app.hob.ui.Nav
import app.hob.ui.Overlay
import app.hob.ui.ago
import app.hob.ui.catName
import app.hob.ui.components.Btn
import app.hob.ui.components.Card
import app.hob.ui.components.Chip
import app.hob.ui.components.Chips
import app.hob.ui.components.Kind
import app.hob.ui.components.R18
import app.hob.ui.components.SectionTitle
import app.hob.ui.components.ThingRow
import app.hob.ui.components.TopBar
import app.hob.ui.components.Tag
import app.hob.ui.things
import app.hob.ui.thing.KidTag
import app.hob.ui.theme.C
import app.hob.ui.theme.T
import app.hob.ui.where

@Composable
fun FindScreen(h: House, nav: Nav) {
    var q by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (h.home.isNotEmpty()) runCatching { focus.requestFocus() } }
    val used = h.catKeys(onlyUsed = true)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = BAR_SPACE)) {
        TopBar(stringResource(R.string.find_t), stringResource(R.string.find_sub))
        Row(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth().clip(R18).background(C.card).border(1.dp, C.line, R18).padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = C.amber, modifier = Modifier.size(21.dp))
            Box(Modifier.weight(1f).padding(start = 10.dp)) {
                if (q.isEmpty()) Text(stringResource(R.string.find_hint), style = T.Body.copy(color = C.muted))
                BasicTextField(q, { q = it }, textStyle = T.Body.copy(color = C.ink), singleLine = true, cursorBrush = SolidColor(C.amber),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth().focusRequester(focus))
            }
            if (q.isNotEmpty()) Icon(Icons.Rounded.Close, null, tint = C.muted, modifier = Modifier.size(20.dp).clip(R18).clickable { q = "" })
        }
        if (used.size > 1) Box(Modifier.padding(horizontal = 18.dp).padding(top = 12.dp)) {
            Chips { for (c in used) Chip(catName(c), cat == c) { cat = if (cat == c) null else c } }
        }

        if (q.isBlank() && cat == null) {
            if (h.home.isEmpty()) {
                Card(Modifier.padding(18.dp).fillMaxWidth()) {
                    Text(stringResource(R.string.find_empty), style = T.Body)
                    Btn(stringResource(R.string.add_thing), { nav.open(Overlay.Edit()) }, Modifier.padding(top = 12.dp))
                }
                return@Column
            }
            SectionTitle(stringResource(R.string.recent))
            Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 10.dp) {
                for (t in h.home.sortedByDescending { it.touched }.take(6))
                    ThingRow(t, where(h, t), { nav.open(Overlay.Thing(t.id)) }, tag = { Tag(ago(h.day - t.touched)) })
            }
            return@Column
        }
        val res = h.search(q, cat)
        SectionTitle(stringResource(R.string.found), note = things(res.size))
        if (res.isEmpty()) Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth()) {
            Text(stringResource(R.string.found_none), style = T.Body)
            Btn(stringResource(R.string.add_this, q.trim()), { nav.open(Overlay.Edit(name = q.trim().replaceFirstChar { it.uppercase() })) }, Modifier.padding(top = 12.dp), Kind.AMBER, small = true)
        } else Card(Modifier.padding(horizontal = 18.dp).fillMaxWidth(), pad = 10.dp) {
            for (t in res) ThingRow(t, where(h, t), { nav.open(Overlay.Thing(t.id)) }, tag = { KidTag(h, t) })
        }
    }
}
