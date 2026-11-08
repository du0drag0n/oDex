package io.lemoncore.opendatadex.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.seed.SeedPokemon
import io.lemoncore.opendatadex.data.seed.SeedSpecies
import io.lemoncore.opendatadex.data.user.Backup
import io.lemoncore.opendatadex.data.user.TeamEntity
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.Section
import io.lemoncore.opendatadex.ui.components.gameChipLabel
import io.lemoncore.opendatadex.ui.team.TeamCard
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

// ---- Teams ---------------------------------------------------------------------------------------------------------------------

/** The trainer's teams with this Pokémon on them, and a way to add it to one. */
@Composable
internal fun TeamsTab(
    container: AppContainer, dex: Dex, s: SeedSpecies, p: SeedPokemon, game: Game, artwork: Map<Int, String>, nav: DetailNav,
) {
    val teams by container.teams.collectAsState(initial = emptyList())
    val members by container.teamMembers.collectAsState(initial = emptyList())
    val name = if (p.isDefault) s.name else p.name
    val onTeams = teams.filter { t -> members.any { it.teamId == t.id && it.speciesId == s.id } }
    var adding by remember { mutableStateOf(false) }
    Column {
        Section(stringResource(R.string.teams_with, name)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onTeams.isEmpty()) EmptyNote(stringResource(R.string.not_on_any_team, name))
                onTeams.forEach { t -> TeamCard(dex, t, members.filter { it.teamId == t.id }, artwork) { nav.openTeam(t.id) } }
                val c = LocalDexColors.current
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                        .dashedBorder(if (c.isDark) Color(0xFF2A2B31) else c.lineStrong, 12.dp).clickable { adding = true },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(R.string.add_to_team_row), color = c.textSecondary, style = DexType.itemTitle.copy(fontSize = 13.sp))
                }
            }
        }
    }
    if (adding) {
        val counts = members.groupingBy { it.teamId }.eachCount()
        val newName = stringResource(R.string.team_default_name, teams.size + 1)
        AddToTeamSheet(
            name, game, teams.filter { (counts[it.id] ?: 0) < Backup.TEAM_SIZE }, counts,
            onDismiss = { adding = false },
            onNew = { container.createTeamWith(newName, game, s.id, p.id); adding = false },
            onPick = { container.addToTeam(it.id, s.id, p.id); adding = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToTeamSheet(
    name: String, game: Game, open: List<TeamEntity>, counts: Map<Long, Int>,
    onDismiss: () -> Unit, onNew: () -> Unit, onPick: (TeamEntity) -> Unit,
) {
    io.lemoncore.opendatadex.ui.pokedex.DexSheet(stringResource(R.string.add_to_team_title, name), onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SheetRow(stringResource(R.string.new_team_for, gameChipLabel(game)), null, onNew)
            open.forEach { t ->
                SheetRow(t.name, "${gameChipLabel(Game.fromKey(t.game))} · ${counts[t.id] ?: 0}/${Backup.TEAM_SIZE}") { onPick(t) }
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, detail: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(LocalDexColors.current.surface2)
            .border(1.dp, LocalDexColors.current.line, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (detail == null) Icon(Icons.Filled.Add, null, tint = LocalDexColors.current.chipText)
        Text(title, color = LocalDexColors.current.text, style = DexType.itemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (detail == null) 8.dp else 0.dp))
        if (detail != null) Text(detail, color = LocalDexColors.current.textSubtle, style = DexType.caption)
    }
}

/** A dashed outline, for the "Add to a team" row. */
private fun Modifier.dashedBorder(color: Color, radius: Dp) = drawBehind {
    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
    drawRoundRect(color, cornerRadius = CornerRadius(radius.toPx()), style = Stroke(width = 1.dp.toPx(), pathEffect = dash))
}
