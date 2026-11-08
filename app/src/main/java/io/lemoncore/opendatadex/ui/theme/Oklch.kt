package io.lemoncore.opendatadex.ui.theme

import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** A colour from OKLCH lightness (0–1), chroma and hue (degrees). Out-of-gamut colours are clamped to sRGB. */
fun oklch(l: Float, c: Float, hDeg: Float, alpha: Float = 1f): Color {
    val h = Math.toRadians(hDeg.toDouble())
    return Color(l, (c * cos(h)).toFloat(), (c * sin(h)).toFloat(), alpha, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
}

/** OKLCH hue (0–360) of a colour; turns the artwork's swatch into the wash hue. */
fun Color.oklchHue(): Float {
    val lab = convert(ColorSpaces.Oklab)
    val deg = Math.toDegrees(atan2(lab.blue, lab.green).toDouble()).toFloat()
    return (deg + 360f) % 360f
}

/** OKLCH chroma of a colour: how far from grey it is. */
fun Color.oklchChroma(): Float {
    val lab = convert(ColorSpaces.Oklab)
    return kotlin.math.sqrt(lab.green * lab.green + lab.blue * lab.blue)
}

/**
 * Every colour a Pokémon (or game) brings to the page, from one hue. Lightness and chroma are fixed per role, so every
 * card is equally bright whatever its colour.
 */
@Immutable
data class WashTones(
    val glow: Color, val baseStart: Color, val baseEnd: Color, val border: Color,
    val haloInner: Color, val haloMid: Color,
    val headerTop: Color, val headerGlow: Color, val detailHalo: Color, val statBar: Color,
    val swatchStart: Color, val swatchEnd: Color,
) {
    companion object {
        fun from(hue: Float, dark: Boolean) = if (dark) WashTones(
            glow = oklch(.50f, .13f, hue, .55f), baseStart = Color(0xFF0C0C0F), baseEnd = oklch(.20f, .05f, hue),
            border = oklch(.32f, .06f, hue, .70f), haloInner = oklch(.66f, .15f, hue, .55f), haloMid = oklch(.30f, .08f, hue, .15f),
            headerTop = oklch(.21f, .055f, hue), headerGlow = oklch(.50f, .14f, hue, .50f), detailHalo = oklch(.60f, .15f, hue, .45f),
            statBar = oklch(.70f, .12f, hue), swatchStart = oklch(.55f, .13f, hue), swatchEnd = oklch(.32f, .08f, hue),
        ) else WashTones(
            glow = oklch(.88f, .08f, hue, .85f), baseStart = Color.White, baseEnd = oklch(.96f, .03f, hue),
            border = oklch(.88f, .04f, hue), haloInner = oklch(.85f, .10f, hue, .80f), haloMid = Color.Transparent,
            headerTop = oklch(.95f, .035f, hue), headerGlow = oklch(.86f, .10f, hue, .90f), detailHalo = oklch(.82f, .12f, hue, .70f),
            statBar = oklch(.66f, .13f, hue), swatchStart = oklch(.72f, .12f, hue), swatchEnd = oklch(.52f, .12f, hue),
        )
    }
}

// Glows are native radial shaders of one colour whose alpha eases out over several stops, drawn over a whole rectangle
// with dithering. Fading to Color.Transparent (transparent black) or clipping a circle left grey rings, hard curved
// edges and banding on OLED screens.
private val GLOW_STOPS = floatArrayOf(0f, 0.12f, 0.26f, 0.40f, 0.52f, 0.62f)
private val GLOW_ALPHA = floatArrayOf(1f, 0.90f, 0.66f, 0.38f, 0.14f, 0f)

private fun shaderPaint(shader: Shader) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    this.shader = shader
    isDither = true
}

private fun RadialGradient.ellipse(cx: Float, cy: Float, rx: Float, ry: Float) = apply {
    if (ry != rx) setLocalMatrix(Matrix().apply { setScale(1f, ry / rx, cx, cy) })
}

/** One colour glowing out from ([cx], [cy]) as an [rx] by [ry] ellipse, gone at [end] of its radius. */
fun glowPaint(color: Color, cx: Float, cy: Float, rx: Float, ry: Float = rx, end: Float = 0.62f): android.graphics.Paint {
    val stops = FloatArray(GLOW_STOPS.size) { GLOW_STOPS[it] * end / 0.62f }
    val colors = IntArray(stops.size) { color.copy(alpha = color.alpha * GLOW_ALPHA[it]).toArgb() }
    return shaderPaint(RadialGradient(cx, cy, rx, colors, stops, Shader.TileMode.CLAMP).ellipse(cx, cy, rx, ry))
}

/** Fills the whole area with [paint], so a glow never shows an edge. */
fun DrawScope.drawPaint(paint: android.graphics.Paint) = drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) }

/** A glow of [color] in a box; [geometry] gives its centre and radii for the box size. */
fun Modifier.glow(color: Color, end: Float = 0.62f, geometry: (Size) -> Pair<Offset, Size>) = drawWithCache {
    val (c, r) = geometry(size)
    val paint = glowPaint(color, c.x, c.y, r.width, r.height, end)
    onDrawBehind { drawPaint(paint) }
}

/**
 * The card wash: a base that deepens towards the right, and a glow centred on the right edge (an ellipse 90% of the
 * width by 140% of the height).
 */
fun Modifier.wash(t: WashTones, shape: Shape, border: Color = t.border) = this
    .clip(shape)
    .drawWithCache {
        val base = Brush.horizontalGradient(0f to t.baseStart, 0.40f to t.baseStart, 0.70f to lerp(t.baseStart, t.baseEnd, 0.55f), 1f to t.baseEnd)
        val glow = glowPaint(t.glow, size.width, size.height / 2, size.width * 0.90f, size.height * 1.40f)
        onDrawBehind {
            drawRect(base)
            drawPaint(glow)
        }
    }
    .border(1.dp, border, shape)

/** The square card wash (the tablet's Grid list): the colour fading down the card, with a glow behind the artwork. */
fun Modifier.tileWash(t: WashTones, shape: Shape, border: Color = t.border) = this
    .clip(shape)
    .drawWithCache {
        val base = Brush.verticalGradient(listOf(t.headerTop, t.baseStart))
        val glow = glowPaint(t.glow, size.width / 2, size.height * 0.42f, size.width * 0.70f, size.height * 0.58f)
        onDrawBehind {
            drawRect(base)
            drawPaint(glow)
        }
    }
    .border(1.dp, border, shape)

/** The Pokémon page header: the Pokémon's colour fading down into the page, with a glow behind the artwork. */
fun Modifier.headerWash(t: WashTones, bg: Color) = drawWithCache {
    val base = Brush.verticalGradient(listOf(t.headerTop, bg))
    val glow = glowPaint(t.headerGlow, size.width * 0.85f, size.height * 0.45f, size.width * 0.80f, size.height * 0.90f, end = 0.65f)
    onDrawBehind {
        drawRect(base)
        drawPaint(glow)
    }
}

/** Soft halo behind artwork, filling the box and brightest just below the middle. */
fun Modifier.halo(t: WashTones, detail: Boolean = false) = drawWithCache {
    val r = size.minDimension / 2
    val cx = size.width / 2
    val cy = size.height * 0.55f
    val paint = when {
        detail -> glowPaint(t.detailHalo, cx, cy, r, end = 0.68f)
        t.haloMid.alpha > 0f -> shaderPaint(
            RadialGradient(
                cx, cy, r, intArrayOf(t.haloInner.toArgb(), t.haloMid.toArgb(), t.haloMid.copy(alpha = 0f).toArgb()),
                floatArrayOf(0f, 0.55f, 0.72f), Shader.TileMode.CLAMP,
            ),
        )
        else -> glowPaint(t.haloInner, cx, cy, r, end = 0.68f)
    }
    onDrawBehind { drawPaint(paint) }
}

/** The tag colours of a type: a soft fill, light text and a dot. */
@Immutable
data class TypeTones(val bg: Color, val text: Color, val dot: Color)

object TypeColors {
    // PokéAPI type id to (hue, chroma factor); the naturally grey types get less chroma.
    private val hues = mapOf(
        1 to (90f to .25f), 2 to (25f to 1f), 3 to (270f to 1f), 4 to (320f to 1f), 5 to (65f to .8f), 6 to (80f to .6f),
        7 to (120f to 1f), 8 to (295f to 1f), 9 to (220f to .3f), 10 to (40f to 1f), 11 to (250f to 1f), 12 to (140f to 1f),
        13 to (95f to 1f), 14 to (355f to 1f), 15 to (205f to 1f), 16 to (280f to 1f), 17 to (30f to .3f), 18 to (345f to 1f),
    )
    /** A type's hue and chroma factor (0 to 1), for colours built from it (type marks). */
    fun hueOf(typeId: Int): Pair<Float, Float> = hues[typeId] ?: (0f to 0f)

    private val dark = HashMap<Int, TypeTones>()
    private val light = HashMap<Int, TypeTones>()

    fun tones(typeId: Int, isDark: Boolean): TypeTones = (if (isDark) dark else light).getOrPut(typeId) {
        val (h, k) = hues[typeId] ?: (0f to 0f)
        if (isDark) TypeTones(oklch(.25f, .06f * k, h), oklch(.88f, .09f * k, h), oklch(.72f, .16f * k, h))
        else TypeTones(oklch(.93f, .045f * k, h), oklch(.40f, .11f * k, h), oklch(.64f, .16f * k, h))
    }
}
