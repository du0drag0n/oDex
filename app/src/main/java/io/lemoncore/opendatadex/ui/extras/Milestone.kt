package io.lemoncore.opendatadex.ui.extras

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.ui.components.Bar
import io.lemoncore.opendatadex.ui.components.OdexMark
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.JetBrainsMono
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.drawPaint
import io.lemoncore.opendatadex.ui.theme.glowPaint
import io.lemoncore.opendatadex.ui.theme.oklch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/** A checklist ([key]) has just reached [percent]% with [caught] of [total]. */
data class Milestone(val key: String, val percent: Int, val caught: Int, val total: Int, val title: String, val hue: Float)

/** The milestones, in order. */
val MilestonePercents = listOf(25, 50, 75, 100)

/** The milestone [before] → [after] catches crossed going up, if any. */
fun milestoneCrossed(before: Int, after: Int, total: Int): Int? =
    if (total <= 0 || after <= before) null else MilestonePercents.lastOrNull { before * 100 < it * total && after * 100 >= it * total }

/** A full-screen card celebrating a milestone, with a button to share it as an image. */
@Composable
fun MilestoneDialog(container: AppContainer, milestone: Milestone, artwork: Map<Int, String>, onDismiss: () -> Unit) {
    val c = LocalDexColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val datesFlow = remember(milestone.key) { container.caughtDates(milestone.key) }
    val dates by datesFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val latest = dates.entries.sortedByDescending { it.value }.take(3).map { it.key }
    val days = dates.values.minOrNull()?.let { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it).toInt() + 1 }
    val layer = rememberGraphicsLayer()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF020203).copy(alpha = 0.96f)).clickable(interactionSource = null, indication = null, onClick = onDismiss)) {
            Column(Modifier.align(Alignment.Center).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(
                    Modifier.drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }.clickable(interactionSource = null, indication = null) {},
                ) { MilestoneCard(milestone, latest, artwork, days) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        Modifier.weight(1f).height(52.dp).clip(shape).border(1.dp, Color(0xFF26272D), shape).clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.not_now), color = Color(0xFFC9CAD0), style = DexType.fab) }
                    Row(
                        Modifier.weight(1.4f).height(52.dp).clip(shape).background(c.accent).clickable {
                            scope.launch { shareImage(context, layer.toImageBitmap().asAndroidBitmap()) }
                        },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Share, null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.share_image), color = c.onAccent, style = DexType.fab)
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(m: Milestone, latest: List<Int>, artwork: Map<Int, String>, days: Int?) {
    // The card is always dark, whatever the app theme, so it reads the same when shared.
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().shadow(30.dp, shape).clip(shape)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val first = glowPaint(oklch(0.55f, 0.15f, m.hue, 0.55f), w * 0.8f, h * 0.2f, w * 0.9f, h * 0.7f, end = 0.65f)
                val second = glowPaint(oklch(0.45f, 0.12f, (m.hue + 45f) % 360f, 0.35f), w * 0.1f, h, w * 0.7f, h * 0.6f, end = 0.70f)
                onDrawBehind {
                    drawRect(oklch(0.12f, 0.01f, m.hue))
                    drawPaint(first)
                    drawPaint(second)
                }
            }
            .border(1.dp, oklch(0.35f, 0.07f, m.hue, 0.7f), shape)
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OdexMark(15.dp, color = Color(0xFFEEEEF0))
            Text("Dex", color = Color(0xFFEEEEF0), style = DexType.sectionTitle.copy(fontSize = 16.sp))
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(when (m.percent) { 25 -> R.string.milestone_25; 50 -> R.string.milestone_50; 75 -> R.string.milestone_75; else -> R.string.milestone_100 }),
                color = oklch(0.85f, 0.11f, m.hue), style = DexType.overline,
            )
        }
        Column(Modifier.padding(top = 8.dp)) {
            Text(m.title, color = Color(0xFFB4B5BD), style = DexType.itemTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Text(
                "${m.percent}%", color = Color.White,
                style = DexType.monoNumber.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 88.sp, letterSpacing = (-0.05).em, lineHeight = 84.sp),
            )
            Text(
                when {
                    days == null -> stringResource(R.string.milestone_caught, m.caught, m.total)
                    days <= 1 -> stringResource(R.string.milestone_caught_today, m.caught, m.total)
                    else -> stringResource(R.string.milestone_caught_days, m.caught, m.total, days)
                },
                color = Color(0xFFC9CBD0), style = DexType.body.copy(fontSize = 14.sp),
            )
        }
        Bar(m.percent / 100f, oklch(0.70f, 0.16f, m.hue), Modifier.fillMaxWidth(), height = 6.dp, track = Color.White.copy(alpha = 0.08f))
        if (latest.isNotEmpty()) {
            Text(stringResource(R.string.latest_catches), color = Color(0xFF8D8E96), style = DexType.overline)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                latest.forEach { id ->
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(4.dp)) {
                        PokemonArtwork(id, Images.officialArtwork(id, artwork), Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/** Saves [bitmap] (scaled to 1080 px wide) to the cache and opens the share sheet for it. */
private suspend fun shareImage(context: Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val scaled = if (bitmap.width == 1080) bitmap else Bitmap.createScaledBitmap(bitmap, 1080, bitmap.height * 1080 / bitmap.width, true)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "odex-milestone.png")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}
