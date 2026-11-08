package io.lemoncore.opendatadex.ui.extras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.HuntMethod
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.hasMasuda
import io.lemoncore.opendatadex.data.hasShinyCharm
import io.lemoncore.opendatadex.data.shinyOdds
import io.lemoncore.opendatadex.data.user.ShinyHuntEntity
import io.lemoncore.opendatadex.ui.components.ArtworkColors
import io.lemoncore.opendatadex.ui.components.LocalFeel
import io.lemoncore.opendatadex.ui.components.PokemonArtwork
import io.lemoncore.opendatadex.ui.components.fallbackHue
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.JetBrainsMono
import io.lemoncore.opendatadex.ui.theme.drawPaint
import io.lemoncore.opendatadex.ui.theme.glowPaint
import io.lemoncore.opendatadex.ui.theme.oklch
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date

private val Gold = Color(0xFFF2C14E)
private val Panel = Color(0xFF0F1013)
private val PanelLine = Color(0xFF26272D)

/**
 * A shiny hunt as its own screen: the shiny artwork, a big count and a big "One more" button, with the odds and the
 * chance someone would have found it by now. It keeps the screen on, and the volume-up key can count too. It is always
 * dark and takes the Pokémon's colour, not the game's.
 */
@Composable
fun ShinyHuntScreen(container: AppContainer, dex: Dex, huntId: Long, onBack: () -> Unit) {
    val flow = remember(huntId) { container.huntById(huntId) }
    val hunt = flow.collectAsStateWithLifecycle(initialValue = null).value ?: return
    val game = Game.fromKey(hunt.game)
    val species = dex.speciesOf(hunt.speciesId) ?: return
    val feel = LocalFeel.current
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val method = HuntMethod.fromName(hunt.method)
    val odds = shinyOdds(game, method, hunt.shinyCharm)
    val hue = ArtworkColors.hue(hunt.formId) ?: fallbackHue(species.color)
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var changingMethod by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var volumeKey by rememberSaveable { mutableStateOf(false) }
    val pop = remember { Animatable(1f) }
    val focus = remember { FocusRequester() }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val found = hunt.foundAt != null
    fun count(delta: Int) {
        if (found) return
        if (delta > 0) feel.huntStep()
        container.countHunt(hunt.id, delta)
        scope.launch {
            pop.snapTo(1f)
            pop.animateTo(1.04f, tween(60))
            pop.animateTo(1f, tween(60))
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF050506)).focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (volumeKey && e.key == Key.VolumeUp) {
                    if (e.type == KeyEventType.KeyDown) count(1)
                    true
                } else false
            },
    ) {
        // The Pokémon's colour glowing down from the top.
        Box(
            Modifier.fillMaxWidth().height(440.dp).drawWithCache {
                val base = Brush.verticalGradient(listOf(oklch(0.2f, 0.04f, hue), Color(0xFF050506)))
                val glow = glowPaint(oklch(0.5f, 0.12f, hue, 0.45f), size.width * 0.5f, size.height * 0.45f, size.width * 0.7f, size.height * 0.6f, end = 0.70f)
                onDrawBehind {
                    drawRect(base)
                    drawPaint(glow)
                }
            },
        )
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color(0xFFEEEEF0)) }
                Text(stringResource(R.string.shiny_hunt), color = Color(0xFFEEEEF0), style = DexType.sheetTitle, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more), tint = Color(0xFFEEEEF0)) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text(stringResource(R.string.edit_count)) }, { menu = false; editing = true })
                        DropdownMenuItem({ Text(stringResource(R.string.change_method)) }, { menu = false; changingMethod = true })
                        DropdownMenuItem(
                            { Text(stringResource(R.string.volume_key_counts)) }, { menu = false; volumeKey = !volumeKey },
                            trailingIcon = { if (volumeKey) Icon(Icons.Filled.Check, null) },
                        )
                        if (!found) DropdownMenuItem({ Text(stringResource(R.string.mark_found)) }, {
                            menu = false
                            container.foundShiny(hunt)
                            feel.shinyFound()
                        })
                        DropdownMenuItem({ Text(stringResource(R.string.delete)) }, { menu = false; deleting = true })
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PokemonArtwork(-hunt.formId, Images.shinyArtwork(hunt.formId), Modifier.size(200.dp), species.name)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(species.name, color = Color(0xFFEEEEF0), style = DexType.screenTitle.copy(fontSize = 20.sp))
                }
                Text(
                    "${gameName(game)} · ${stringResource(method.label)} · ${stringResource(R.string.hunt_started, SimpleDateFormat("d MMM", locale).format(Date(hunt.startedAt)))}".uppercase(locale),
                    color = Color(0xFF8D8E96), style = DexType.monoMeta, modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    NumberFormat.getIntegerInstance(locale).format(hunt.count), color = Color.White,
                    style = DexType.monoNumber.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.04).em),
                    modifier = Modifier.padding(top = 18.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                )
                Text(
                    if (found) stringResource(R.string.hunt_found_short) else stringResource(method.unit),
                    color = if (found) Gold else Color(0xFF8D8E96), style = DexType.body.copy(fontSize = 12.sp),
                )
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (game.hasMasuda) HuntChip(stringResource(R.string.hunt_masuda), method == HuntMethod.MASUDA) {
                        container.updateHunt(hunt.copy(method = if (method == HuntMethod.MASUDA) HuntMethod.ENCOUNTERS.name else HuntMethod.MASUDA.name))
                    }
                    if (game.hasShinyCharm) HuntChip(stringResource(R.string.hunt_charm), hunt.shinyCharm) {
                        container.updateHunt(hunt.copy(shinyCharm = !hunt.shinyCharm))
                    }
                }
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(stringResource(R.string.odds), "1 / ${NumberFormat.getIntegerInstance(locale).format(odds.oneIn)}", Gold, Modifier.weight(1f))
                    StatCard(stringResource(R.string.luckier_than_you), "${(odds.foundBy(hunt.count) * 100).toInt()}%", Color(0xFFEEEEF0), Modifier.weight(1f))
                }
            }
        }
        if (!found) Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier.size(72.dp).clip(shape).background(Panel).border(1.dp, PanelLine, shape).clickable(enabled = hunt.count > 0) { count(-1) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Remove, stringResource(R.string.count_down), tint = Color(0xFFC9CAD0), modifier = Modifier.size(22.dp)) }
            Row(
                Modifier.weight(1f).height(72.dp).shadow(28.dp, shape, ambientColor = Gold, spotColor = Gold).clip(shape).background(Gold).clickable { count(1) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = Color(0xFF1A1405), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.one_more), color = Color(0xFF1A1405), style = DexType.screenTitle.copy(fontSize = 20.sp))
            }
        }
    }

    if (editing) EditCountDialog(hunt, { editing = false }) { container.updateHunt(hunt.copy(count = it)) }
    if (changingMethod) AlertDialog(
        onDismissRequest = { changingMethod = false },
        title = { Text(stringResource(R.string.change_method)) },
        text = {
            Column {
                HuntMethod.entries.filter { it != HuntMethod.MASUDA || game.hasMasuda }.forEach { m ->
                    Row(Modifier.fillMaxWidth().clickable { container.updateHunt(hunt.copy(method = m.name)); changingMethod = false },
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(m == method, { container.updateHunt(hunt.copy(method = m.name)); changingMethod = false })
                        Text(stringResource(m.label))
                    }
                }
            }
        },
        confirmButton = { TextButton({ changingMethod = false }) { Text(stringResource(R.string.close)) } },
    )
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.delete_hunt)) },
        confirmButton = { TextButton({ deleting = false; container.deleteHunt(hunt.id); onBack() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton({ deleting = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun HuntChip(label: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.height(36.dp).clip(shape).background(if (on) Gold.copy(alpha = 0.16f) else Panel)
            .border(1.dp, if (on) Gold.copy(alpha = 0.5f) else PanelLine, shape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (on) Icons.Filled.Check else Icons.Filled.Add, null, tint = if (on) Gold else Color(0xFF8D8E96), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (on) Gold else Color(0xFF8D8E96), style = DexType.chip)
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.clip(shape).background(Panel).border(1.dp, Color(0xFF1D1E23), shape).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(title, color = Color(0xFF8D8E96), style = DexType.overline)
        Text(value, color = color, style = DexType.monoValue.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp))
    }
}

@Composable
private fun EditCountDialog(hunt: ShinyHuntEntity, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(hunt.count.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_count)) },
        text = {
            OutlinedTextField(text, { v -> text = v.filter { it.isDigit() }.take(7) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        },
        confirmButton = { TextButton({ text.toIntOrNull()?.let(onSave); onDismiss() }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
