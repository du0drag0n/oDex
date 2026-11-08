package io.lemoncore.opendatadex

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.lemoncore.opendatadex.data.Settings
import io.lemoncore.opendatadex.data.seed.DataLanguage
import io.lemoncore.opendatadex.ui.DexApp
import io.lemoncore.opendatadex.ui.theme.OpenDataDexTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = container
        setContent {
            val settings = container.currentSettings.collectAsStateWithLifecycle().value ?: Settings()
            // The interface language is applied here rather than app-wide, so changing it needs no restart.
            val base = LocalContext.current
            val config = LocalConfiguration.current
            val localized = remember(base, config, settings.appLanguage) { base.inLanguage(settings.appLanguage, config) }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
                LocalResources provides localized.resources,
            ) {
                OpenDataDexTheme(settings.theme, if (settings.tintFromGame) settings.game.hue else null) {
                    DexApp(container, settings)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // A linked save may have changed while the app was away, played on an emulator say.
        container.saveLinks.refreshAll()
    }

    override fun onStop() {
        super.onStop()
        // Catch the last changes before the app may be closed.
        container.autoBackup.flush()
    }
}

/** This context with its strings in [language] (unchanged when null, which follows the phone's language). */
private fun Context.inLanguage(language: DataLanguage?, config: Configuration): Context {
    if (language == null) return this
    val localized = Configuration(config).apply { setLocales(LocaleList(Locale.forLanguageTag(language.locale))) }
    val resources = createConfigurationContext(localized).resources
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = resources
    }
}
