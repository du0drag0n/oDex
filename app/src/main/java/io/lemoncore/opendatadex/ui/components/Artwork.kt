package io.lemoncore.opendatadex.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.lemoncore.opendatadex.BuildConfig
import io.lemoncore.opendatadex.data.Images
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.WashTones
import io.lemoncore.opendatadex.ui.theme.oklchHue
import io.lemoncore.opendatadex.ui.theme.oklchChroma
import io.lemoncore.opendatadex.ui.theme.speciesColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.runtime.mutableStateMapOf

/**
 * The hue of each Pokémon's artwork, read from its palette as the artwork loads and kept for the process. Every
 * colour a card or page uses is built from this one hue (see [WashTones]).
 */
object ArtworkColors {
    private val hues = mutableStateMapOf<Int, Float>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun hue(id: Int): Float? = hues[id]

    /** Reads [id]'s hue from its artwork; [colorId], the species' PokéAPI colour, breaks near-ties between colours. */
    fun learn(id: Int, bitmap: android.graphics.Bitmap, colorId: Int? = null) {
        if (hues.containsKey(id)) return
        scope.launch {
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            // The body colour: the largest swatch that is actually colourful, so a white belly, dark outline or
            // small bright detail (a flame, a flower) does not win. Dark swatches are mostly outline and shading.
            // Shades of one body colour come back as several swatches, so compare hue families, not single swatches.
            // Families are weighed by population x saturation, so a big pale area counts for less than a vivid one, and
            // near-grey swatches are left out.
            val families = palette.swatches
                .filter { it.hsl[1] >= 0.2f && it.hsl[2] in 0.35f..0.85f && Color(it.rgb).oklchChroma() >= 0.04f }
                .groupBy { ((it.hsl[0] + 15f) % 360f / 30f).toInt() }
                .values.map { family -> family.sumOf { (it.population * it.hsl[1]).toInt() } to family.maxBy { it.population * it.hsl[1] } }
            val biggest = families.maxOfOrNull { it.first } ?: 0
            // Where two colours are within a third of each other in size (Charizard's orange body and teal wings), take the one nearer the
            // species' own colour. Grey, white and black species give no useful hint.
            val hint = colorId?.takeIf { it !in setOf(1, 4, 9) }?.let { fallbackHue(it) }
            val swatch = (if (hint != null) families.filter { it.first * 3 >= biggest }.minByOrNull { hueDistance(Color(it.second.rgb).oklchHue(), hint) }
                else families.maxByOrNull { it.first })?.second
                ?: palette.dominantSwatch ?: return@launch
            val hue = Color(swatch.rgb).oklchHue()
            // Halfway to the species' colour: light oranges and tans read as yellow on their own, and the species
            // colour pulls them back to the orange the eye sees (Charmander, Charizard).
            hues[id] = if (hint == null) hue else blendHue(hue, hint)
        }
    }

    private fun hueDistance(a: Float, b: Float): Float = abs(a - b).let { minOf(it, 360f - it) }

    /** The hue halfway between [a] and [b], the short way round. */
    private fun blendHue(a: Float, b: Float): Float {
        var d = b - a
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return (a + d / 2 + 360f) % 360f
    }
}

fun fallbackHue(colorId: Int): Float = speciesColor(colorId).oklchHue()

/**
 * The wash colours for a Pokémon, easing from its species colour to its artwork's colour once that has been read.
 * The hue takes the short way round the colour wheel.
 */
@Composable
fun animatedWash(id: Int, colorId: Int): WashTones {
    val target = ArtworkColors.hue(id) ?: fallbackHue(colorId)
    val hue = remember(id) { Animatable(target) }
    LaunchedEffect(target) {
        var to = target
        while (to - hue.value > 180f) to -= 360f
        while (hue.value - to > 180f) to += 360f
        hue.animateTo(to, tween(350))
    }
    val dark = LocalDexColors.current.isDark
    val h = hue.value
    return remember(h, dark) { WashTones.from(h, dark) }
}

/** Pokémon pictures as flat shapes, for store and README screenshots that don't reproduce the artwork. See odex.silhouettes. */
private val SILHOUETTE = ColorFilter.tint(Color(0xFF2B3A4D).copy(alpha = 0.45f), BlendMode.SrcIn)

/** Official artwork; feeds [ArtworkColors] once loaded. */
@Composable
fun PokemonArtwork(
    id: Int, url: String, modifier: Modifier = Modifier, contentDescription: String? = null, colorFilter: ColorFilter? = null,
    /** The species' PokéAPI colour, a hint for reading the artwork's colour. */
    colorId: Int? = null,
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(url).allowHardware(false).build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter ?: SILHOUETTE.takeIf { BuildConfig.SILHOUETTES && id > 0 },
        filterQuality = if (Images.isPixelArt(url)) FilterQuality.None else DrawScope.DefaultFilterQuality,
        modifier = modifier,
        onSuccess = { state -> ArtworkColors.learn(id, state.result.image.toBitmap(), colorId) },
    )
}
