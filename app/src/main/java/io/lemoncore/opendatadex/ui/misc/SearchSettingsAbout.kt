package io.lemoncore.opendatadex.ui.misc

import io.lemoncore.opendatadex.data.user.AutoBackup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.data.user.RestoreSummary
import io.lemoncore.opendatadex.data.seed.DataLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.lemoncore.opendatadex.BuildConfig
import io.lemoncore.opendatadex.data.Dex
import io.lemoncore.opendatadex.data.Game
import io.lemoncore.opendatadex.data.SearchHit
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.data.ThemeMode
import io.lemoncore.opendatadex.ui.components.EmptyNote
import io.lemoncore.opendatadex.ui.components.gameName
import io.lemoncore.opendatadex.ui.theme.LocalDexColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(dex: Dex, game: Game, onBack: () -> Unit, onOpen: (SearchHit) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                TextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        val hits = remember(query, game) { dex.search(query, game) }
        LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)) {
            if (query.length >= 2 && hits.isEmpty()) item { EmptyNote(stringResource(R.string.search_nothing, query)) }
            if (query.length < 2) item { EmptyNote(stringResource(R.string.search_start, gameName(game))) }
            hits.groupBy { it.kind }.forEach { (kind, list) ->
                item(key = "h-$kind") {
                    Text(stringResource(kind.label), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }
                items(list, key = { "${it.kind}-${it.id}" }) { hit ->
                    Column(Modifier.fillMaxWidth().clickable { onOpen(hit) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(hit.title, fontSize = 17.sp)
                        if (hit.subtitle.isNotBlank()) Text(hit.subtitle, color = LocalDexColors.current.subtleText, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    HorizontalDivider(color = LocalDexColors.current.divider)
                }
            }
        }
    }
}
