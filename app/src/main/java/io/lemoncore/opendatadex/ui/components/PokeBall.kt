package io.lemoncore.opendatadex.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** The caught ball's fixed colours: the same in light and dark themes, never the theme accent. */
object PokeballColors {
    val red = Color(0xFFF0484B)
    val white = Color(0xFFF4F4F5)
    val ink = Color(0xFF0E0F11)
}

/**
 * The caught mark, drawn on a 24-unit grid: a red and white Poké Ball when caught, an outline in [muted] when not. The band
 * stops at the button, which is drawn last so it covers the band.
 */
@Composable
fun PokeballIcon(caught: Boolean, muted: Color, modifier: Modifier = Modifier.size(16.dp)) = Canvas(modifier) {
    val u = size.minDimension / 24f
    val c = center
    if (caught) {
        val r = 10 * u
        val box = Rect(c, r)
        drawArc(PokeballColors.red, 180f, 180f, true, box.topLeft, box.size)
        drawArc(PokeballColors.white, 0f, 180f, true, box.topLeft, box.size)
        drawCircle(PokeballColors.ink, r, c, style = Stroke(1.6f * u))
        drawLine(PokeballColors.ink, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 2.2f * u)
        drawCircle(PokeballColors.white, 3.6f * u, c)
        drawCircle(PokeballColors.ink, 3.6f * u, c, style = Stroke(2.2f * u))
        drawCircle(PokeballColors.ink.copy(alpha = 0.18f), 1.3f * u, c)
    } else {
        val s = Stroke(1.8f * u)
        drawCircle(muted, 9.6f * u, c, style = s)
        drawCircle(muted, 3.4f * u, c, style = s)
        drawLine(muted, Offset(c.x - 9.6f * u, c.y), Offset(c.x - 3.4f * u, c.y), 1.8f * u)
        drawLine(muted, Offset(c.x + 3.4f * u, c.y), Offset(c.x + 9.6f * u, c.y), 1.8f * u)
    }
}

/** [PokeballIcon] that crossfades over 150 ms with a 0.85 to 1 scale pop when [caught] changes (not when first shown). */
@Composable
fun AnimatedPokeballIcon(caught: Boolean, muted: Color, modifier: Modifier = Modifier.size(16.dp)) {
    val scale = remember { Animatable(1f) }
    val shown = remember { booleanArrayOf(caught) }
    LaunchedEffect(caught) {
        if (shown[0] == caught) return@LaunchedEffect
        shown[0] = caught
        scale.snapTo(0.85f)
        scale.animateTo(1f, tween(150))
    }
    Crossfade(caught, modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }, tween(150), label = "pokeball") {
        PokeballIcon(it, muted, Modifier.fillMaxSize())
    }
}
