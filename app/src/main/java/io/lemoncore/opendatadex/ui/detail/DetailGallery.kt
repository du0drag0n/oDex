package io.lemoncore.opendatadex.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.HuntMethod
import io.lemoncore.opendatadex.data.Images
import io.lemoncore.opendatadex.data.hasMasuda
import io.lemoncore.opendatadex.data.hasShinies
import io.lemoncore.opendatadex.data.hasShinyCharm
import io.lemoncore.opendatadex.data.seed.SeedPokemon
import io.lemoncore.opendatadex.data.seed.SeedSpecies
import io.lemoncore.opendatadex.data.shinyOdds
import io.lemoncore.opendatadex.data.user.ShinyHuntEntity
import io.lemoncore.opendatadex.ui.components.Section
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.theme.WashTones
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.halo
import io.lemoncore.opendatadex.ui.components.rowCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.flow.map
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

/**
 * dataDex 4's Gallery tab: the official artwork, normal and shiny; a shiny hunt counter with the game's odds; the
 * selected game's sprites; other looks; and every game's sprites, oldest first. Any picture opens larger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GalleryTab(
    container: AppContainer, dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game, tones: WashTones, artwork: Map<Int, String>,
    onOpenHunt: (Long) -> Unit = {},
) {
    val sub = LocalDexColors.current.subtleText
    var viewing by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    Column {
        Section(stringResource(R.string.section_artwork)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArtTile(stringResource(R.string.sprite_normal), Images.officialArtwork(p.id, artwork), tones, Modifier.weight(1f)) { viewing = it to false }
                ArtTile(stringResource(R.string.sprite_shiny), Images.shinyArtwork(p.id), tones, Modifier.weight(1f)) { viewing = it to false }
            }
        }

        if (game.hasShinies) ShinyHunt(container, s, p, game, tones, onOpenHunt)

        Section(stringResource(R.string.section_sprites)) {
            val normal = Images.gameSprite(p.id, game)
            val shiny = Images.gameSprite(p.id, game, shiny = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SpriteColumn(stringResource(R.string.sprite_normal), normal, Images.homeSprite(p.id))
                if (shiny != null) SpriteColumn(stringResource(R.string.sprite_shiny), shiny, Images.homeSprite(p.id, shiny = true))
                else SpriteColumn(stringResource(R.string.sprite_shiny_artwork), Images.shinyArtwork(p.id), null)
            }
            // Female sprites differ for some species from Generation IV, when sprites started showing it.
            if (s.genderDiff && p.isDefault && (game.isNational || game.dataGen >= 4)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SpriteColumn(stringResource(R.string.sprite_female), Images.gameSprite(p.id, game, female = true), Images.homeSprite(p.id, female = true))
                    SpriteColumn(stringResource(R.string.sprite_female_shiny), Images.gameSprite(p.id, game, shiny = true, female = true), Images.homeSprite(p.id, shiny = true, female = true))
                }
            }
        }

        val looks = if (p.isDefault) dex.cosmeticFormsIn(s, game) else emptyList()
        if (looks.isNotEmpty()) Section(stringResource(R.string.section_other_looks)) {
            Text(stringResource(if (looks.any { it.types.isNotEmpty() }) R.string.other_looks_type_note else R.string.other_looks_note, s.name),
                color = sub, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                // The default look first, named after the default form ("Plant Cloak", "A").
                LookTile(p.form.ifEmpty { s.name }, Images.homeSprite(s.id), Images.gameSprite(s.id, Game.NATIONAL) ?: "", emptyList(), dex, Modifier.weight(1f))
                looks.forEach { f ->
                    LookTile(f.name, Images.cosmeticForm(s.id, f.key), Images.cosmeticFormSprite(s.id, f.key), f.types, dex, Modifier.weight(1f))
                }
                // Keep the last row's tiles the same width as the others.
                repeat((3 - (looks.size + 1) % 3) % 3) { Spacer(Modifier.weight(1f)) }
            }
        }

        Section(stringResource(R.string.section_every_sprite)) {
            Text(stringResource(R.string.every_sprite_note, s.name), color = sub, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            // Sprite sets from games the species existed in; a set without this Pokémon (or form) drops out when its sprite fails.
            Images.spriteSets.filter { set -> s.gen <= set.first().dataGen }.forEach { set ->
                SpriteSetRow(set, p.id) { url -> viewing = url to true }
            }
            Text(stringResource(R.string.sprites_internet), color = sub, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
    viewing?.let { (url, pixel) -> LargeImage(url, pixel) { viewing = null } }
}

@Composable
private fun ArtTile(label: String, url: String, tones: WashTones, modifier: Modifier, onOpen: (String) -> Unit) {
    val c = LocalDexColors.current
    Column(modifier) {
        // A soft tint of the Pokémon's colour behind the picture.
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).rowCard(c).halo(tones, detail = true).clickable { onOpen(url) },
            contentAlignment = Alignment.Center,
        ) { AsyncImage(url, contentDescription = label, modifier = Modifier.fillMaxSize(0.78f)) }
        Text(label, color = c.textMuted, style = DexType.chip, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
    }
}

/** One sprite set (games that share sprites, such as Ruby and Sapphire): its games, then the normal and shiny sprites. */
@Composable
private fun SpriteSetRow(games: List<Game>, pokemonId: Int, onOpen: (String) -> Unit) {
    val normal = Images.gameSprite(pokemonId, games.first()) ?: return
    val shiny = Images.gameSprite(pokemonId, games.first(), shiny = true)
    var missing by remember(normal) { mutableStateOf(false) }
    if (missing) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(games.map { gameName(it) }.joinToString(" / "), style = DexType.itemTitle.copy(fontSize = 13.sp), color = LocalDexColors.current.text, modifier = Modifier.weight(1f))
        AsyncImage(normal, contentDescription = null, filterQuality = FilterQuality.None, onError = { missing = true },
            modifier = Modifier.size(72.dp).clickable { onOpen(normal) })
        if (shiny != null) AsyncImage(shiny, contentDescription = null, filterQuality = FilterQuality.None,
            modifier = Modifier.size(72.dp).clickable { onOpen(shiny) })
        else Spacer(Modifier.size(72.dp))
    }
}

/** A picture shown large; game sprites stay crisp ([pixel]) instead of being blurred when scaled up. */
@Composable
private fun LargeImage(url: String, pixel: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        AsyncImage(
            url, contentDescription = null, filterQuality = if (pixel) FilterQuality.None else FilterQuality.Medium,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface).clickable(onClick = onDismiss).padding(16.dp),
        )
    }
}

// ---- Shiny hunt ---------------------------------------------------------------------------------------------------------------

/**
 * A shiny hunt for this Pokémon in the page's game: the count of encounters, soft resets or Masuda eggs, the game's
 * odds (with the Shiny Charm where it exists) and the chance of having found one by now. "Found it!" ends the hunt
 * and marks the Pokémon in the Shiny Dex.
 */
@Composable
private fun ShinyHunt(container: AppContainer, s: SeedSpecies, p: SeedPokemon, game: Game, tones: WashTones, onOpenHunt: (Long) -> Unit) {
    // Wrapped so "no hunt yet" can be told apart from "not loaded yet".
    val huntFlow = remember(game, s.id) { container.hunt(game, s.id).map { listOfNotNull(it) } }
    val loaded by huntFlow.collectAsState(initial = null)
    val sub = LocalDexColors.current.subtleText
    // An unfinished hunt can also be counted full screen.
    val active = loaded?.firstOrNull()?.takeIf { it.foundAt == null }
    Section(
        stringResource(R.string.section_shiny_hunt),
        trailing = active?.let { h -> { TextButton({ onOpenHunt(h.id) }) { Text(stringResource(R.string.open_counter)) } } },
    ) {
        val hunt = (loaded ?: return@Section).firstOrNull()
        when {
            hunt == null -> {
                Text(stringResource(R.string.hunt_intro, s.name), color = sub, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                Button({ container.startHunt(game, s.id, p.id, HuntMethod.ENCOUNTERS) }, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
                    Text(stringResource(R.string.hunt_start))
                }
            }
            hunt.foundAt != null -> {
                val locale = LocalConfiguration.current.locales[0]
                val date = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(hunt.foundAt))
                Text(stringResource(R.string.hunt_found_after, NumberFormat.getIntegerInstance(locale).format(hunt.count),
                    stringResource(HuntMethod.fromName(hunt.method).unit), date),
                    color = LocalDexColors.current.text, textAlign = TextAlign.Center, fontSize = 17.sp, modifier = Modifier.fillMaxWidth())
                TextButton({ container.startHunt(game, s.id, p.id, HuntMethod.fromName(hunt.method)) }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.hunt_new))
                }
            }
            else -> HuntCounter(container, hunt, game, tones)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HuntCounter(container: AppContainer, hunt: ShinyHuntEntity, game: Game, tones: WashTones) {
    val sub = LocalDexColors.current.subtleText
    val haptics = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]
    val numbers = NumberFormat.getIntegerInstance(locale)
    val method = HuntMethod.fromName(hunt.method)
    val odds = shinyOdds(game, method, hunt.shinyCharm)
    var stopping by remember { mutableStateOf(false) }

    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        HuntMethod.entries.filter { it != HuntMethod.MASUDA || game.hasMasuda }.forEach { m ->
            FilterChip(m == method, { container.updateHunt(hunt.copy(method = m.name)) }, label = { Text(stringResource(m.label)) })
        }
    }
    if (game.hasShinyCharm) Row(Modifier.fillMaxWidth().clickable { container.updateHunt(hunt.copy(shinyCharm = !hunt.shinyCharm)) },
        verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.hunt_charm), color = LocalDexColors.current.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(hunt.shinyCharm, { container.updateHunt(hunt.copy(shinyCharm = it)) })
    }
    Text(numbers.format(hunt.count), color = LocalDexColors.current.text, fontSize = 56.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    Text(stringResource(method.unit), color = sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val minus = stringResource(R.string.hunt_remove_one)
        OutlinedButton({ container.countHunt(hunt.id, -1) }, Modifier.weight(1f).height(64.dp).semantics { contentDescription = minus },
            enabled = hunt.count > 0) { Text("−1", fontSize = 22.sp) }
        val plus = stringResource(R.string.hunt_add_one)
        Button(
            { haptics.performHapticFeedback(HapticFeedbackType.LongPress); container.countHunt(hunt.id, 1) },
            Modifier.weight(2f).height(64.dp).semantics { contentDescription = plus },
        ) { Text("+1", fontSize = 26.sp) }
    }
    Text(stringResource(R.string.hunt_odds, numbers.format(odds.oneIn)), color = LocalDexColors.current.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(stringResource(R.string.hunt_found_by, "%.1f%%".format(locale, odds.foundBy(hunt.count) * 100)), color = sub,
        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton({ stopping = true }) { Text(stringResource(R.string.hunt_stop)) }
        Button({ container.foundShiny(hunt) }) { Text(stringResource(R.string.hunt_found)) }
    }
    if (stopping) AlertDialog(
        onDismissRequest = { stopping = false },
        title = { Text(stringResource(R.string.hunt_stop_title)) },
        text = { Text(stringResource(R.string.hunt_stop_body, numbers.format(hunt.count), stringResource(method.unit))) },
        confirmButton = { TextButton({ stopping = false; container.deleteHunt(hunt.id) }) { Text(stringResource(R.string.hunt_stop)) } },
        dismissButton = { TextButton({ stopping = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
