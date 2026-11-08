package io.lemoncore.opendatadex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.TypeColors
import io.lemoncore.opendatadex.ui.theme.oklch

/**
 * A glyph for each type (PokéAPI type id). These are stand-ins from the icon set until oDex has its own drawn set; they
 * deliberately aren't the games' type symbols.
 */
fun typeGlyph(typeId: Int): ImageVector = when (typeId) {
    2 -> Icons.Filled.SportsMma
    3 -> Icons.Filled.FlutterDash
    4 -> Icons.Filled.Science
    5 -> Icons.Filled.Terrain
    6 -> Icons.Filled.Diamond
    7 -> Icons.Filled.BugReport
    8 -> Icons.Filled.TheaterComedy
    9 -> Icons.Filled.Shield
    10 -> Icons.Filled.LocalFireDepartment
    11 -> Icons.Filled.WaterDrop
    12 -> Icons.Filled.Eco
    13 -> Icons.Filled.Bolt
    14 -> Icons.Filled.Visibility
    15 -> Icons.Filled.AcUnit
    16 -> Icons.Filled.Star
    17 -> Icons.Filled.DarkMode
    18 -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.Circle
}

/** The colour a type's glyph is drawn in. */
@Composable
fun typeMarkColor(typeId: Int): Color {
    val (h, k) = TypeColors.hueOf(typeId)
    return if (LocalDexColors.current.isDark) oklch(0.92f, 0.08f * k, h) else oklch(0.42f, 0.13f * k, h)
}

/** A type's badge: its glyph in a round, softly lit disc of the type's colour with a thin ring. */
@Composable
fun TypeMark(typeId: Int, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val (h, k) = TypeColors.hueOf(typeId)
    val dark = LocalDexColors.current.isDark
    val inner = if (dark) oklch(0.42f, 0.13f * k, h) else oklch(0.94f, 0.06f * k, h)
    val outer = if (dark) oklch(0.22f, 0.07f * k, h) else oklch(0.86f, 0.09f * k, h)
    val ring = if (dark) oklch(0.5f, 0.12f * k, h, 0.6f) else oklch(0.75f, 0.10f * k, h, 0.6f)
    Box(
        modifier.size(size).clip(CircleShape)
            .drawWithCache {
                val brush = Brush.radialGradient(listOf(inner, outer), center = Offset(this.size.width / 2, this.size.height * 0.35f), radius = this.size.width * 0.75f)
                onDrawBehind { drawRect(brush) }
            }
            .border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(typeGlyph(typeId), null, tint = typeMarkColor(typeId), modifier = Modifier.size(size * 24 / 52))
    }
}

/** A Type Dex tile: the badge with the type's name under it. */
@Composable
fun TypeMarkTile(typeId: Int, name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(if (c.isDark) Color(0xFF0C0C0F) else Color.White).border(1.dp, if (c.isDark) Color(0xFF1A1B1F) else c.line, shape)
            .clickable(onClick = onClick).padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TypeMark(typeId)
        Text(name, color = typeMarkColor(typeId), style = DexType.itemTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}
