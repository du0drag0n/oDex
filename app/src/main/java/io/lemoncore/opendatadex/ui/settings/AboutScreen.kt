package io.lemoncore.opendatadex.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.lemoncore.opendatadex.BuildConfig
import io.lemoncore.opendatadex.R
import io.lemoncore.opendatadex.ui.components.OdexMark
import io.lemoncore.opendatadex.ui.theme.DexType
import io.lemoncore.opendatadex.ui.theme.LocalDexColors

private const val REPO = "https://github.com/du0drag0n/oDex"
private const val CONTACT_EMAIL = "mailto:admin@opendex.cc"

/** The website holds the about, support, credits and legal pages; the links below are all built from this. */
private const val WEBSITE_BASE_URL = "https://opendex.lemoncore.io"

/** What ships in the app and its license. Names aren't translated. */
private val OPEN_SOURCE = listOf(
    "oDex" to "GNU General Public License 3.0",
    "PokéAPI game data" to "BSD 3-Clause License",
    "AndroidX and Jetpack Compose" to "Apache License 2.0",
    "Kotlin and kotlinx libraries" to "Apache License 2.0",
    "OkHttp" to "Apache License 2.0",
    "Coil" to "Apache License 2.0",
    "Manrope" to "SIL Open Font License 1.1",
    "JetBrains Mono" to "SIL Open Font License 1.1",
)

/**
 * Support only ever appears here, reached from the Settings row and the drawer: no pop-ups or nags, and nothing is
 * locked. Money is the middle of three ways to say thanks, between two free ones.
 */
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val c = LocalDexColors.current
    val context = LocalContext.current
    SettingsScaffold(stringResource(R.string.support_odex), onBack) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp).fillMaxWidth().aboutCard().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.free_for_good), color = c.text, style = DexType.sectionTitle.copy(fontSize = 17.sp))
            Text(stringResource(R.string.free_for_good_body), color = if (c.isDark) Color(0xFFB4B5BD) else c.textSecondary,
                style = DexType.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportButton(Icons.Filled.Star, Color(0xFFF2C14E), stringResource(R.string.star_github), Icons.AutoMirrored.Filled.OpenInNew) { context.open(REPO) }
                if (BuildConfig.KOFI_URL.isNotBlank()) {
                    SupportButton(Icons.Filled.LocalCafe, Color(0xFFFF8A80), stringResource(R.string.kofi), Icons.AutoMirrored.Filled.OpenInNew) { context.open(BuildConfig.KOFI_URL) }
                }
                SupportButton(Icons.AutoMirrored.Filled.Chat, Color(0xFF9A9BA3), stringResource(R.string.tell_a_friend), Icons.Filled.Share) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, REPO))
                    context.startActivity(Intent.createChooser(send, null))
                }
            }
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 24.dp).fillMaxWidth().aboutCard()) {
            LinkRow(Icons.Filled.Favorite, stringResource(R.string.support_project)) { context.open("$WEBSITE_BASE_URL/support") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.PrivacyTip, stringResource(R.string.kofi_privacy)) { context.open("$WEBSITE_BASE_URL/privacy#kofi") }
        }
    }
}

/** The app itself: version, links out to the website's pages, and the notices that stay readable offline. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = LocalDexColors.current
    val context = LocalContext.current
    var licenses by remember { mutableStateOf(false) }
    SettingsScaffold(stringResource(R.string.nav_about), onBack) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OdexMark(72.dp, color = c.text)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), color = c.text, style = DexType.wordmark.copy(fontSize = 28.sp, letterSpacing = (-0.03).em))
            Text(stringResource(R.string.about_powered, BuildConfig.VERSION_NAME), color = c.textSubtle, style = DexType.monoMeta)
        }
        Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().aboutCard()) {
            LinkRow(Icons.Filled.Code, stringResource(R.string.source_code), "du0drag0n/oDex") { context.open(REPO) }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.BugReport, stringResource(R.string.report_problem)) { context.open(CONTACT_EMAIL) }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Email, stringResource(R.string.contact)) { context.open("$WEBSITE_BASE_URL/about#contact") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Info, stringResource(R.string.about_opendex)) { context.open("$WEBSITE_BASE_URL/about") }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().aboutCard()) {
            LinkRow(Icons.Filled.PrivacyTip, stringResource(R.string.privacy)) { context.open("$WEBSITE_BASE_URL/privacy#odex") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Description, stringResource(R.string.terms)) { context.open("$WEBSITE_BASE_URL/terms") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Gavel, stringResource(R.string.legal_notices)) { context.open("$WEBSITE_BASE_URL/legal") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Copyright, stringResource(R.string.copyright_request)) { context.open("$WEBSITE_BASE_URL/legal#takedown") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Groups, stringResource(R.string.credits)) { context.open("$WEBSITE_BASE_URL/credits") }
            RowDivider(46.dp)
            LinkRow(Icons.Filled.Code, stringResource(R.string.open_source_licenses)) { licenses = true }
        }
        Text(
            stringResource(R.string.fan_made), color = c.textFaint, style = DexType.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        )
    }
    if (licenses) LicensesDialog { licenses = false }
}

@Composable
private fun Modifier.aboutCard(): Modifier {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(20.dp)
    return clip(shape).background(if (c.isDark) Color(0xFF0C0C0F) else Color.White).border(1.dp, if (c.isDark) Color(0xFF1A1B1F) else c.line, shape)
}

@Composable
private fun SupportButton(icon: ImageVector, iconColor: Color, label: String, trailing: ImageVector, onClick: () -> Unit) {
    val c = LocalDexColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(shape).background(if (c.isDark) Color(0xFF121317) else c.surface3)
            .border(1.dp, if (c.isDark) Color(0xFF26272D) else c.line, shape).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        Text(label, color = c.text, style = DexType.itemTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        Icon(trailing, null, tint = c.textFaint, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, trailing: String? = null, onClick: () -> Unit) {
    val c = LocalDexColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
        Text(title, color = c.text, style = rowTitle.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = c.textFaint, style = DexType.monoMeta)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textFaint, modifier = Modifier.size(18.dp))
    }
}

/** The open-source software and fonts in the app, which stay readable offline. */
@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    val c = LocalDexColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.open_source_licenses)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OPEN_SOURCE.forEach { (name, license) ->
                    Column {
                        Text(name, color = c.text, style = DexType.itemTitle)
                        Text(license, color = c.textMuted, style = DexType.body)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private fun Context.open(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // No browser: nothing to open it in.
    }
}
