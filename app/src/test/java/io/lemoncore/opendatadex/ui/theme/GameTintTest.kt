package io.lemoncore.opendatadex.ui.theme

import io.lemoncore.opendatadex.data.Game
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTintTest {
    @Test
    fun accentLabelIsReadableForEveryGame() {
        for (base in listOf(DarkDex, LightDex)) for (game in Game.entries) {
            val c = base.tinted(game.hue)
            val ratio = contrast(c.onAccent, c.accent)
            assertTrue("${game.name} (dark=${base.isDark}) is $ratio:1", ratio >= 4.5f)
        }
    }
}
