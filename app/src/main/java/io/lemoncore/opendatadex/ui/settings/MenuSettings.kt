package io.lemoncore.opendatadex.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.lemoncore.opendatadex.AppContainer
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.ui.DrawerEntry
import io.lemoncore.opendatadex.ui.MENU_POKEDEX
import io.lemoncore.opendatadex.ui.orderedMenu
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import kotlinx.coroutines.launch

/**
 * Settings → Side menu: switch each screen on or off and move it up or down. Pokédex always stays at the top, and
 * Support, Settings and About are always at the bottom of the menu. Changes apply straight away.
 */
@Composable
fun MenuSettingsScreen(container: AppContainer, settings: Settings, onBack: () -> Unit) {
    val c = LocalDexColors.current
    val scope = rememberCoroutineScope()
    val entries = orderedMenu(settings.menuOrder)
    val hidden = settings.menuHidden
    fun save(order: List<DrawerEntry>, hidden: Set<String>) = scope.launch { container.settings.setMenu(order.map { it.key }, hidden) }
    fun move(from: Int, to: Int) = save(entries.toMutableList().apply { add(to, removeAt(from)) }, hidden)

    SettingsScaffold(stringResource(R.string.menu_customise), onBack, trailing = {
        if (settings.menuOrder.isNotEmpty() || hidden.isNotEmpty()) TextButton(onClick = { scope.launch { container.settings.resetMenu() } }) {
            Text(stringResource(R.string.menu_reset), color = c.accentText, style = DexType.chip)
        }
    }) {
        Text(
            stringResource(R.string.menu_hint), color = c.textSubtle, style = rowSubtitle,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
        )
        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp).fillMaxWidth().groupCard(false)) {
            entries.forEachIndexed { i, entry ->
                if (i > 0) RowDivider(inset = 56.dp)
                val locked = entry.key == MENU_POKEDEX
                val on = locked || entry.key !in hidden
                val name = stringResource(entry.label)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 60.dp)
                        .then(if (locked) Modifier else Modifier.toggleable(on, role = Role.Switch) {
                            save(entries, if (it) hidden - entry.key else hidden + entry.key)
                        })
                        .padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(entry.icon, null, tint = if (on) c.text else c.textFaint, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = if (on) c.text else c.textMuted, style = rowTitle)
                        if (locked) Text(stringResource(R.string.menu_always_shown), color = c.textSubtle, style = rowSubtitle)
                    }
                    if (!locked) {
                        DexSwitch(on)
                        // Pokédex stays first, so the second entry can't move up.
                        IconButton(onClick = { move(i, i - 1) }, enabled = i > 1) {
                            Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.menu_move_up, name),
                                tint = if (i > 1) c.textSecondary else c.textFaint.copy(alpha = 0.4f))
                        }
                        IconButton(onClick = { move(i, i + 1) }, enabled = i < entries.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.menu_move_down, name),
                                tint = if (i < entries.lastIndex) c.textSecondary else c.textFaint.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}
