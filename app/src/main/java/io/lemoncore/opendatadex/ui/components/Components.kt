package io.lemoncore.opendatadex.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.ui.theme.CategoryColors
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.TypeColors
import io.lemoncore.opendatadex.ui.theme.oklch

/** Ease used by progress and stat bars. */
val BarEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** A type as a soft tag: a dot and the name on the type's own tint. */
@Composable
fun TypeTag(typeId: Int, name: String, modifier: Modifier = Modifier, large: Boolean = false, dot: Boolean = true, onClick: (() -> Unit)? = null) {
    val t = TypeColors.tones(typeId, LocalDexColors.current.isDark)
    Row(
        modifier
            .clip(RoundedCornerShape(if (large) 8.dp else 7.dp))
            .background(t.bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                start = if (!dot) (if (large) 10.dp else 9.dp) else if (large) 8.dp else 7.dp,
                end = if (large) 10.dp else 9.dp, top = if (large) 4.dp else 3.dp, bottom = if (large) 4.dp else 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(t.dot))
            Spacer(Modifier.width(5.dp))
        }
        Text(name, color = t.text, style = DexType.typeTag.copy(fontSize = if (large) 12.sp else 11.sp), maxLines = 1)
    }
}

/** Type label used in tables and headers (kept as a name for the screens that used the old pill). */
@Composable
fun TypePill(typeId: Int, name: String, modifier: Modifier = Modifier, height: Dp = 30.dp, onClick: (() -> Unit)? = null) {
    Box(modifier.heightIn(min = 0.dp), contentAlignment = Alignment.Center) {
        TypeTag(typeId, name, large = height >= 30.dp, onClick = onClick)
    }
}

/** A move's type in move lists. */
@Composable
fun TypeBar(typeId: Int, name: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.CenterStart) { TypeTag(typeId, name, dot = false) }
}

/** Physical / Special / Status as a coloured word. */
@Composable
fun CategoryBadge(category: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(CategoryColors.label(category)), modifier = modifier,
        color = CategoryColors.of(category, LocalDexColors.current.isDark), style = DexType.caption.copy(fontSize = 11.sp), maxLines = 1,
    )
}

@Composable
fun FavoriteToggle(on: Boolean, tint: Color, onToggle: () -> Unit, size: Dp = 26.dp) {
    IconButton(onClick = onToggle, modifier = Modifier.size(size + 18.dp)) {
        Icon(
            if (on) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = stringResource(if (on) R.string.remove_favorite else R.string.add_favorite),
            tint = if (on) LocalDexColors.current.gold else tint,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
fun CaughtToggle(on: Boolean, tint: Color, onToggle: () -> Unit, size: Dp = 26.dp) {
    val feel = LocalFeel.current
    val label = stringResource(if (on) R.string.mark_not_caught else R.string.mark_caught)
    IconButton(
        onClick = {
            if (on) feel.unmarked() else feel.caught()
            onToggle()
        },
        modifier = Modifier.size(size + 14.dp).semantics { contentDescription = label },
    ) {
        AnimatedPokeballIcon(on, tint, Modifier.size(size))
    }
}

@Composable
fun ShinyToggle(on: Boolean, tint: Color, onToggle: () -> Unit, size: Dp = 26.dp) {
    IconButton(onClick = onToggle, modifier = Modifier.size(size + 14.dp)) {
        Icon(
            if (on) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
            contentDescription = stringResource(if (on) R.string.mark_shiny_not_caught else R.string.mark_shiny_caught),
            tint = if (on) LocalDexColors.current.gold else tint,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * The dex number as a pill that is also the caught toggle: red with a filled Poké Ball when caught, grey with an empty one when not.
 * [shiny] makes it gold with a sparkle (Shiny Dex). The pill is 30 dp tall but takes taps over 48 dp.
 */
@Composable
fun NumberPill(
    number: Int, caught: Boolean, onToggle: (() -> Unit)?, modifier: Modifier = Modifier,
    large: Boolean = false, shiny: Boolean = false, offBackground: Color? = null,
) {
    val c = LocalDexColors.current
    val bgTarget = when {
        caught && shiny -> c.gold.copy(alpha = 0.16f)
        // The chip follows the game's accent; the ball itself keeps its own colours.
        caught -> c.accentTint
        else -> offBackground ?: c.pillOff
    }
    val fgTarget = when {
        caught && shiny -> c.gold
        caught -> c.accentText
        large -> c.textMuted
        else -> c.textSubtle
    }
    val bg by animateColorAsState(bgTarget, tween(200), label = "pillBg")
    val fg by animateColorAsState(fgTarget, tween(200), label = "pillFg")
    val label = stringResource(if (caught) R.string.mark_not_caught else R.string.mark_caught)
    val feel = LocalFeel.current
    // Compose widens a clickable this small to a 48 dp touch target on its own, without making the pill taller.
    Row(
        modifier
            .height(if (large) 32.dp else 30.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (onToggle != null) Modifier.clickable {
                    if (caught) feel.unmarked() else feel.caught()
                    onToggle()
                }.semantics { contentDescription = label } else Modifier,
            )
            .padding(horizontal = if (large) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shiny) Icon(if (caught) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome, null, tint = fg, modifier = Modifier.size(16.dp))
        else AnimatedPokeballIcon(caught, c.textMuted)
        Spacer(Modifier.width(7.dp))
        Text(dexNumber(number), color = fg, style = DexType.monoNumber.copy(fontSize = if (large) 13.sp else 12.sp), maxLines = 1)
    }
}

/** The oDex lens from the app icon: the ring and the eye's rim in the content colour, on a 24-unit grid. */
@Composable
fun OdexMark(size: Dp, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        val center = Offset(12f * s, 12f * s)
        val r = 10f * s
        drawArc(
            color, startAngle = -19.9f, sweepAngle = 309.8f, useCenter = false,
            topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2), style = Stroke(2f * s, cap = StrokeCap.Round),
        )
        drawCircle(Color(0xFF5BC0F0), 5.2f * s, center)
        drawCircle(color, 5.2f * s, center, style = Stroke(2f * s))
        drawCircle(Color.White, 1.4f * s, Offset(10.2f * s, 10.2f * s))
        drawCircle(Color(0xFFFFCB05), 1.5f * s, Offset(19.1f * s, 4.9f * s))
    }
}

/** The oDex wordmark: the lens standing in for the "o", then "Dex". */
@Composable
fun OdexWordmark(modifier: Modifier = Modifier, large: Boolean = false) {
    val color = LocalDexColors.current.text
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        OdexMark(if (large) 29.dp else 20.dp, Modifier.offset(y = if (large) 2.dp else 1.dp), color)
        Spacer(Modifier.width(if (large) 2.dp else 1.dp))
        Text("Dex", color = color, style = if (large) DexType.wordmarkLarge else DexType.wordmark)
    }
}

/** A game's colour. The National Pokédex has none, so it takes a neutral grey. */
@Composable
fun gameColor(game: Game): Color = if (game.isNational) LocalDexColors.current.textFaint else Color(game.color)

/** A square of the game's colour from its hue, lighter at the top left. The National Pokédex is grey. */
@Composable
fun GameSwatch(game: Game, size: Dp, modifier: Modifier = Modifier, radius: Dp = 6.dp) {
    val brush = if (game.isNational) {
        val base = gameColor(game)
        val light = !LocalDexColors.current.isDark
        Brush.linearGradient(listOf(lerp(base, Color.White, if (light) 0.25f else 0.12f), lerp(base, Color.Black, if (light) 0.25f else 0.45f)))
    } else Brush.linearGradient(listOf(oklch(0.6f, 0.15f, game.hue), oklch(0.32f, 0.08f, game.hue)))
    Box(modifier.size(size).clip(RoundedCornerShape(radius)).background(brush))
}

/**
 * Filter chip with a trailing caret, from the Pokédex header. [leading] is drawn before the label (the game swatch).
 * [shrink] lets the label ellipsize to keep the caret in view when the chip is given less room than it wants.
 */
@Composable
fun HeaderChip(
    label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    activeColors: Triple<Color, Color, Color>? = null, caret: Boolean = true, background: Color? = null,
    shrink: Boolean = false, leading: (@Composable () -> Unit)? = null,
) {
    val c = LocalDexColors.current
    // Background, border and label colours.
    val (bg, border, fg) = when {
        active -> activeColors ?: Triple(c.accentTint, c.accent.copy(alpha = 0.5f), c.accentText)
        else -> Triple(background ?: c.surface2, c.lineStrong, c.text)
    }
    Row(
        modifier.height(34.dp).clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(start = if (leading != null) 6.dp else 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        // Weighted only on request: in a scrolling row a weighted label would get no width at all.
        Text(label, color = fg, style = DexType.chip, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = (if (shrink) Modifier.weight(1f, fill = false) else Modifier).widthIn(max = 220.dp))
        if (caret) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.ExpandMore, null, tint = if (active) fg else c.textFaint, modifier = Modifier.size(14.dp))
        }
    }
}

/** A chip for the game, with its colour swatch. */
@Composable
fun GameChip(game: Game, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, background: Color? = null, shrink: Boolean = false) {
    HeaderChip(label, false, onClick, modifier, background = background, shrink = shrink, leading = { GameSwatch(game, 22.dp) })
}

/** A titled card section. */
@Composable
fun Section(
    title: String, titleColor: Color = LocalDexColors.current.text, modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalDexColors.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = titleColor, style = DexType.sectionTitle, modifier = Modifier.weight(1f))
            trailing?.invoke(this)
        }
        content()
    }
}

/** A small "NOT IN THIS GAME"-style tag next to a section title. */
@Composable
fun MonoTag(text: String, modifier: Modifier = Modifier) {
    val c = LocalDexColors.current
    Text(
        text.uppercase(), color = c.textMuted, style = DexType.overline.copy(letterSpacing = 0.sp), maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(if (c.isDark) Color(0xFF1A1B20) else c.surface3).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** A value box with a caption underneath (height, weight, catch rate…). */
@Composable
fun ValueBox(value: String, caption: String, modifier: Modifier = Modifier, accent: Color = LocalDexColors.current.text) {
    val c = LocalDexColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, color = accent, style = DexType.monoValue.copy(fontSize = 15.sp), textAlign = TextAlign.Center)
        }
        Text(caption, color = c.textSubtle, style = DexType.caption, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    }
}

/** A segmented control: a track with equal buttons; the selected one is filled. [counts] are drawn after each label. */
@Composable
fun SegmentedButtons(
    options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
    counts: List<Int>? = null, height: Dp = 34.dp,
) {
    val c = LocalDexColors.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) c.segmentOn else Color.Transparent, tween(200), label = "seg")
            val fg = if (on) c.segmentOnText else c.textMuted
            Row(
                Modifier.weight(1f).height(height).clip(RoundedCornerShape(9.dp)).background(bg).clickable { onSelect(i) }.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = fg, style = DexType.chip, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                counts?.getOrNull(i)?.let {
                    Spacer(Modifier.width(5.dp))
                    Text(it.toString(), color = fg.copy(alpha = 0.6f), style = DexType.monoCount, maxLines = 1)
                }
            }
        }
    }
}

/** Old call shape: the accent colour is now always the theme's. */
@Composable
fun SegmentedButtons(options: List<String>, selected: Int, @Suppress("UNUSED_PARAMETER") accent: Color, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) =
    SegmentedButtons(options, selected, onSelect, modifier)

/** A thin bar that eases to its value. */
@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp, glow: Boolean = false, track: Color = LocalDexColors.current.track) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450, easing = BarEasing), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(f)
                .then(if (glow) Modifier.shadow(6.dp, RoundedCornerShape(height / 2), ambientColor = color, spotColor = color) else Modifier)
                .clip(RoundedCornerShape(height / 2)).background(color),
        )
    }
}

/** Progress line: a track with the caught share in red, then "82/153 · 54%". */
@Composable
fun ProgressLine(done: Int, total: Int, modifier: Modifier = Modifier) {
    val c = LocalDexColors.current
    val pct = if (total == 0) 0 else Math.round(done * 100f / total)
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Bar(if (total == 0) 0f else done.toFloat() / total, c.accent, Modifier.weight(1f), glow = c.isDark, track = c.line)
        Text("$done/$total · $pct%", color = c.accentText, style = DexType.monoMeta, maxLines = 1)
    }
}

/** One base-stat row: label, value and a bar. [max] marks the Pokémon's highest stat, drawn in red. */
@Composable
fun StatBar(label: String, value: Int, max: Int, color: Color, modifier: Modifier = Modifier, highest: Boolean = false) {
    val c = LocalDexColors.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.textSubtle, style = DexType.caption, maxLines = 1, modifier = Modifier.width(40.dp))
        Spacer(Modifier.width(8.dp))
        Text(value.toString(), color = c.text, style = DexType.monoValue, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(36.dp))
        Spacer(Modifier.width(8.dp))
        Bar(value.toFloat() / max, if (highest) c.accent else color, Modifier.weight(1f), height = 6.dp)
    }
}

/** A Material-free tab-content heading. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalDexColors.current.text, style = DexType.sectionTitle, modifier = modifier.padding(horizontal = 4.dp))
}

/** Overline: a small mono uppercase label ("YOUR GAMES", "GEN I"). */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = LocalDexColors.current.textSubtle, align: TextAlign = TextAlign.Start) {
    Text(text.uppercase(), color = color, style = DexType.overline, textAlign = align, modifier = modifier, maxLines = 1)
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalDexColors.current.accent) }
}

@Composable
fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = LocalDexColors.current.textSubtle, textAlign = TextAlign.Center,
        style = DexType.body.copy(fontSize = 14.sp), modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
    )
}

@Composable
fun RowScope.TableHeader(text: String, weight: Float, align: TextAlign = TextAlign.Center) {
    Text(text, modifier = Modifier.weight(weight), textAlign = align, color = LocalDexColors.current.textSubtle, style = DexType.caption)
}

/** Circle used behind sprites in lists. */
@Composable
fun SpriteCircle(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(CircleShape).background(color), contentAlignment = Alignment.Center) { content() }
}

/** A row card: surface, hairline border, radius 14. */
fun Modifier.rowCard(c: io.lemoncore.opendatadex.ui.theme.DexColors, radius: Dp = 14.dp) =
    clip(RoundedCornerShape(radius)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(radius))

fun dexNumber(n: Int) = "#%03d".format(n)

fun formatMoveValue(v: Int) = if (v <= 0) "-" else v.toString()

fun typeName(dex: Dex, id: Int) = dex.type(id)?.name ?: "?"
