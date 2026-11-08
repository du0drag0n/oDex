package io.lemoncore.opendatadex.ui.dexes

import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.lemoncore.opendatadex.ui.theme.LocalDexColors
import io.lemoncore.opendatadex.ui.theme.DexType
import androidx.compose.ui.text.style.TextOverflow

/**
 * Shared frame for the secondary dexes: app bar (drawer or back), an optional inline name filter and a chip row.
 * [content] gets the current filter text and the inner padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexScaffold(
    title: String,
    onNavigation: () -> Unit,
    isRoot: Boolean,
    filterable: Boolean = true,
    chips: (@Composable RowScope.() -> Unit)? = null,
    /** Extra top-bar buttons, after the name filter. */
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (query: String, padding: androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val c = LocalDexColors.current
    Scaffold(
        containerColor = c.bg,
        topBar = {
            Column(Modifier.background(c.bg)) {
                TopAppBar(
                    title = {
                        if (searching) {
                            TextField(
                                value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text(stringResource(R.string.filter_by_name)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else Text(title, style = DexType.screenTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigation) {
                            Icon(if (isRoot) Icons.Filled.Menu else Icons.AutoMirrored.Filled.ArrowBack, stringResource(if (isRoot) R.string.open_menu else R.string.back))
                        }
                    },
                    actions = {
                        if (filterable) IconButton(onClick = { if (searching) query = ""; searching = !searching }) {
                            Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, stringResource(if (searching) R.string.stop_filtering else R.string.filter_by_name))
                        }
                        actions?.invoke(this)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = c.bg, navigationIconContentColor = c.text, actionIconContentColor = c.textSecondary, titleContentColor = c.text,
                    ),
                )
                if (chips != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) { chips() }
                }
                HorizontalDivider(color = LocalDexColors.current.divider)
            }
        },
    ) { padding -> content(query, padding) }
}
