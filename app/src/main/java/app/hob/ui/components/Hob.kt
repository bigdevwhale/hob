package app.hob.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.hob.R
import app.hob.ui.theme.C

/** Sleepy (the house is dark), calm, pleased (just had his milk). */
enum class HobMood { SLEEPY, CALM, PLEASED }

private val DIM = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.62f, 0.62f, 0.66f, 1f) })

/**
 * The hob's portrait: a round window onto him and his lamp. His lamp breathes when [alive]; the light follows his mood –
 * dim when the house is dark, brighter after a treat.
 */
@Composable
fun HobFigure(modifier: Modifier, mood: HobMood = HobMood.CALM, alive: Boolean = true) {
    val t = rememberInfiniteTransition(label = "hob")
    val breath by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "breath")
    val b = if (alive) breath else 0f
    val glow = when (mood) { HobMood.SLEEPY -> 0f; HobMood.CALM -> .22f; HobMood.PLEASED -> .45f } * (0.8f + 0.2f * b)
    Box(modifier.drawBehind {
        if (glow > 0f) drawCircle(Brush.radialGradient(listOf(C.amber.copy(alpha = glow), Color.Transparent), radius = size.minDimension * .62f), radius = size.minDimension * .62f)
    }, contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.hob), null,
            Modifier.fillMaxSize().padding(2.dp).scale(1f + 0.012f * b).clip(CircleShape),
            contentScale = ContentScale.Crop,
            colorFilter = if (mood == HobMood.SLEEPY) DIM else null,
        )
    }
}

