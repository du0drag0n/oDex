package io.lemoncore.opendatadex.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** The wash and type colours against the sRGB reference values in the redesign's handoff notes. */
class OklchTest {
    private fun assertColor(expected: Long, actual: Color, what: String) {
        val e = Color(expected)
        val worst = listOf(e.red - actual.red, e.green - actual.green, e.blue - actual.blue).maxOf { abs(it) } * 255
        assertTrue("$what: expected ${"%06X".format(expected and 0xFFFFFF)}, got $actual", worst <= 3f)
    }

    @Test fun `card wash matches the reference colours`() {
        val bulbasaur = WashTones.from(165f, dark = true)
        assertColor(0xFF007950, bulbasaur.glow.copy(alpha = 1f), "Bulbasaur glow")
        assertColor(0xFF001D10, bulbasaur.baseEnd, "Bulbasaur base end")
        assertEquals(0.55f, bulbasaur.glow.alpha, 0.001f)
        val charmander = WashTones.from(48f, dark = false)
        assertColor(0xFFFFC8AA, charmander.glow.copy(alpha = 1f), "Charmander light glow")
        assertColor(0xFFFFECE1, charmander.baseEnd, "Charmander light base end")
        assertColor(0xFF00709A, WashTones.from(225f, dark = true).glow.copy(alpha = 1f), "Squirtle glow")
    }

    @Test fun `type tags match the reference colours`() {
        val fire = TypeColors.tones(10, isDark = true)
        assertColor(0xFF391609, fire.bg, "Fire tag")
        assertColor(0xFFFFC4AB, fire.text, "Fire text")
        assertColor(0xFF49A9FF, TypeColors.tones(11, isDark = true).dot, "Water dot")
        assertColor(0xFF4D4737, TypeColors.tones(1, isDark = false).text, "Normal light text")
    }

    @Test fun `hue round-trips`() {
        assertEquals(165f, oklch(0.6f, 0.1f, 165f).oklchHue(), 1f)
        assertEquals(48f, oklch(0.7f, 0.12f, 48f).oklchHue(), 1f)
    }
}
