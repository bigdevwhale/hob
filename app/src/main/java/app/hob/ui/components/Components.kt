package app.hob.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hob.container
import app.hob.data.Thing
import app.hob.ui.theme.C
import app.hob.ui.theme.T

val R18 = RoundedCornerShape(18.dp)
val R14 = RoundedCornerShape(14.dp)
val PILL = RoundedCornerShape(50)

/** The room's walls with the lamp glowing in the top right corner. */
fun Modifier.room(): Modifier = this.background(C.bg).drawBehind {
    drawCircle(Brush.radialGradient(listOf(C.glow, Color.Transparent), center = Offset(size.width * 0.88f, 0f), radius = size.width * 0.75f),
        radius = size.width * 0.75f, center = Offset(size.width * 0.88f, 0f))
}

@Composable
fun Card(modifier: Modifier = Modifier, pad: Dp = 14.dp, color: Color = C.card, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(R18).background(color).border(1.dp, C.line, R18).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        content = content,
    )
}

enum class Kind { AMBER, GHOST, TERRA, SOFT }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.AMBER, small: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null) {
    val (bg, fg) = when (kind) {
        Kind.AMBER -> C.amber to C.onAmber
        Kind.TERRA -> C.terra to C.onTerra
        Kind.SOFT -> C.card2 to C.ink
        Kind.GHOST -> Color.Transparent to C.ink
    }
    Row(
        modifier.clip(PILL).background(if (enabled) bg else C.card2)
            .then(if (kind == Kind.GHOST) Modifier.border(1.5.dp, C.ink.copy(alpha = .2f), PILL) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (small) 13.dp else 18.dp, vertical = if (small) 8.dp else 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val col = if (enabled) fg else C.muted
        if (icon != null) Icon(icon, null, tint = col, modifier = Modifier.padding(end = 6.dp).size(if (small) 16.dp else 18.dp))
        Text(text, style = T.Button.copy(color = col, fontSize = if (small) 12.5.sp else 14.5.sp), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, desc: String? = null) {
    Box(modifier.size(42.dp).clip(R14).background(C.card).border(1.dp, C.line, R14).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = C.ink, modifier = Modifier.size(21.dp))
    }
}

/** A page's title row: back, the title (and a line under it), actions on the right. */
@Composable
fun TopBar(title: String, sub: String? = null, onBack: (() -> Unit)? = null, right: @Composable (RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onBack); Box(Modifier.size(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = T.Small, modifier = Modifier.padding(top = 2.dp))
        }
        if (right != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { right() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, note: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = T.Heading, modifier = Modifier.weight(1f))
        if (note != null) Text(note, style = T.Small)
        if (action != null) Text(action, style = T.Small.copy(color = C.amberText, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            modifier = Modifier.clip(PILL).clickable { onAction?.invoke() }.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

/** A selectable pill. */
@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, style = T.Small.copy(color = if (on) C.onAmber else C.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp),
        modifier = Modifier.clip(PILL).background(if (on) C.amber else C.card).border(1.dp, if (on) C.amber else C.line, PILL)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { content() }
}

enum class Mood { NOW, SOON, PLAIN, GOOD }

/** A small label under a line: «Ваня: уже впору». */
@Composable
fun Tag(text: String, mood: Mood = Mood.PLAIN) {
    val (bg, fg) = when (mood) {
        Mood.NOW -> C.terra to C.onTerra
        Mood.SOON -> C.amber to C.onAmber
        Mood.GOOD -> C.sage.copy(alpha = .25f) to C.sage
        Mood.PLAIN -> C.card2 to C.muted
    }
    Text(text, style = T.Tiny.copy(color = fg, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), maxLines = 1,
        modifier = Modifier.padding(top = 5.dp).clip(PILL).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
}

/** An emoji on a soft square. */
@Composable
fun Em(emoji: String, modifier: Modifier = Modifier, size: Dp = 48.dp, bg: Color = C.card2) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.31f)).background(bg), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

/** The thing's photo if it has one, otherwise its emoji. */
@Composable
fun Pic(t: Thing, modifier: Modifier = Modifier, size: Dp = 48.dp, big: Boolean = false) {
    if (!t.photo) { Em(t.emoji, modifier, size); return }
    val c = LocalContext.current.container
    val key = "${t.id}:$big:${t.updated}"
    val state = remember(key) { mutableStateOf<ImageBitmap?>(c.photos.cached(t.id, big, t.updated)?.asImageBitmap()) }
    LaunchedEffect(key) { if (state.value == null) state.value = c.photos.load(t.id, big, t.updated)?.asImageBitmap() }
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.31f)).background(C.card2), contentAlignment = Alignment.Center) {
        val b = state.value
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Text(t.emoji, fontSize = (size.value * 0.5f).sp)
    }
}

/** A thing in a list: picture, name, where it lies, an optional label and something on the right. */
@Composable
fun ThingRow(t: Thing, where: String?, onClick: () -> Unit, tag: (@Composable () -> Unit)? = null, right: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(R14).clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Pic(t)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(t.name, style = T.Strong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (where != null) Text(where, style = T.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            tag?.invoke()
        }
        if (right != null) Box(Modifier.padding(start = 8.dp)) { right() }
    }
}

@Composable
fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))

@Composable
fun Gap(h: Dp) = Box(Modifier.height(h))

/** A text field with its label above. */
@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String?, modifier: Modifier = Modifier, hint: String? = null, number: Boolean = false, lines: Int = 1) {
    Column(modifier.fillMaxWidth()) {
        if (label != null) Text(label, style = T.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
        BasicTextField(
            value, onChange, textStyle = T.Body.copy(color = C.ink), singleLine = lines == 1, minLines = lines, cursorBrush = SolidColor(C.amber),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(R14).background(C.card).border(1.dp, C.line, R14).padding(horizontal = 14.dp, vertical = 13.dp)) {
                    if (value.isEmpty() && hint != null) Text(hint, style = T.Body.copy(color = C.muted))
                    inner()
                }
            },
        )
    }
}

/** A dimmed layer; tapping outside or Back closes it. */
@Composable
fun Scrim(onDismiss: (() -> Unit)?, bottom: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    if (onDismiss != null) BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(C.scrim).clickable(remember { MutableInteractionSource() }, null) { onDismiss?.invoke() }.imePadding()
            .padding(if (bottom) 0.dp else 18.dp),
        contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 560.dp).clickable(remember { MutableInteractionSource() }, null) {}) { content() }
    }
}

/** A bottom sheet with a title. */
@Composable
fun Sheet(title: String?, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scrim(onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(if (C.dark) Color(0xFF26352F) else C.bg)
            .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 18.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(42.dp, 5.dp).clip(PILL).background(C.ink.copy(alpha = .2f)))
            if (title != null) Text(title, style = T.Title.copy(fontSize = 21.sp), modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

/** A label above a group in a form. */
@Composable
fun Label(text: String, top: Dp = 16.dp) = Text(text, style = T.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), modifier = Modifier.padding(top = top, bottom = 7.dp))

/** A row in a menu sheet: emoji, a title and a line under it. */
@Composable
fun MenuRow(emoji: String, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).clip(R18).background(C.card).border(1.dp, C.line, R18).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Em(emoji, size = 46.dp)
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = T.Label.copy(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Text(sub, style = T.Small)
        }
    }
}

/** A thin progress bar in lamp colours. */
@Composable
fun Meter(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(10.dp).clip(PILL).background(C.bgDeep)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(10.dp).clip(PILL).background(Brush.horizontalGradient(listOf(C.amber2, C.amber))))
    }
}

/** A circle with a letter, for people. */
@Composable
fun Avatar(name: String, color: Color, size: Dp = 42.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), style = T.Label.copy(color = Color(0xFF22302B), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 17.sp))
    }
}
