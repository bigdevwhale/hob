package app.hob.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.hob.R

/**
 * «An evening at home»: deep green walls, the warm light of a lamp, cream text and a terracotta for what's urgent.
 * By day the same house with the curtains open: linen, the same greens and amber.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color, val bgDeep: Color, val card: Color, val card2: Color, val line: Color,
    val ink: Color, val muted: Color,
    val amber: Color, val amber2: Color, val onAmber: Color, val amberText: Color,
    val terra: Color, val onTerra: Color,
    val sage: Color,
    /** The lamp's glow in the corner of the room. */
    val glow: Color,
    val scrim: Color,
    val dim: Color,
)

val Evening = Palette(
    dark = true,
    bg = Color(0xFF22302B), bgDeep = Color(0xFF1A2420), card = Color(0xFF2C3C35), card2 = Color(0xFF35473F), line = Color(0x14FFFFFF),
    ink = Color(0xFFF3EAD8), muted = Color(0xFFA9B3A6),
    amber = Color(0xFFF0B75E), amber2 = Color(0xFFE39A4A), onAmber = Color(0xFF2A1F0E), amberText = Color(0xFFF0B75E),
    terra = Color(0xFFE07A55), onTerra = Color(0xFF2A140A),
    sage = Color(0xFF8FB08A),
    glow = Color(0x30F0B75E),
    scrim = Color(0xCC0B100E),
    dim = Color(0xFF1C2622),
)

val Day = Palette(
    dark = false,
    bg = Color(0xFFF4EFE4), bgDeep = Color(0xFFEAE3D3), card = Color(0xFFFFFCF5), card2 = Color(0xFFEDE6D6), line = Color(0x1A2B3A33),
    ink = Color(0xFF26332D), muted = Color(0xFF7A8378),
    amber = Color(0xFFEBA545), amber2 = Color(0xFFDB8A37), onAmber = Color(0xFF2A1F0E), amberText = Color(0xFFB0701C),
    terra = Color(0xFFD0663F), onTerra = Color(0xFFFFFFFF),
    sage = Color(0xFF6E9469),
    glow = Color(0x26F0B75E),
    scrim = Color(0x9926332D),
    dim = Color(0xFFE6DFCF),
)

/** The palette in use. Read from anywhere; switching theme recomposes. */
object C {
    var p by mutableStateOf(Evening)
    val dark get() = p.dark
    val bg get() = p.bg
    val bgDeep get() = p.bgDeep
    val card get() = p.card
    val card2 get() = p.card2
    val line get() = p.line
    val ink get() = p.ink
    val muted get() = p.muted
    val amber get() = p.amber
    val amber2 get() = p.amber2
    val onAmber get() = p.onAmber
    val amberText get() = p.amberText
    val terra get() = p.terra
    val onTerra get() = p.onTerra
    val sage get() = p.sage
    val glow get() = p.glow
    val scrim get() = p.scrim
    val dim get() = p.dim

    /** Children's colours, in order. */
    val kids = listOf(Color(0xFFF0B75E), Color(0xFF8FB08A), Color(0xFFE07A55), Color(0xFF9DB4D6), Color(0xFFD7A6C4), Color(0xFFC9B27A))
    fun kid(i: Int) = kids[((i % kids.size) + kids.size) % kids.size]
}

/** Lora by Cyreal (SIL OFL). */
val Serif = FontFamily(Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_bold, FontWeight.Bold))

/** Nunito by Vernon Adams (SIL OFL). */
val Sans = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

object T {
    val Hero get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = C.ink)
    val Title get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 30.sp, color = C.ink)
    val Heading get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 23.sp, color = C.ink)
    val Label get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 19.sp, color = C.ink)
    val Strong get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 19.sp, color = C.ink)
    val Body get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Small get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, lineHeight = 17.sp, color = C.muted)
    val Tiny get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, color = C.muted)
    val Button get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, lineHeight = 18.sp)
    val Number get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp, color = C.ink)
}

/** 0: evening, 1: day, 2: as the system. */
@Composable
fun HobTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 0 -> true; 1 -> false; else -> isSystemInDarkTheme() }
    val p = if (dark) Evening else Day
    if (C.p != p) C.p = p
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        if (C.p != p) C.p = p
        (view.context as? android.app.Activity)?.window?.let { w ->
            androidx.core.view.WindowCompat.getInsetsController(w, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(primary = p.amber, onPrimary = p.onAmber, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.terra)
        else lightColorScheme(primary = p.amber, onPrimary = p.onAmber, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink, outline = p.line, error = p.terra),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
