package io.lemoncore.opendatadex.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuOrderTest {
    private val default = menuEntries.map { it.key }

    @Test fun `no saved order is the default order`() = assertEquals(default, orderedMenu(emptyList()).map { it.key })

    @Test fun `saved order is kept and Pokedex stays first`() {
        val saved = default.drop(1).reversed() + MENU_POKEDEX
        assertEquals(listOf(MENU_POKEDEX) + default.drop(1).reversed(), orderedMenu(saved).map { it.key })
    }

    @Test fun `unknown keys are dropped and new screens take their default place`() {
        // An order saved before Map existed, plus a screen that has since gone.
        val saved = default.filter { it != "map" } + "living_dex"
        assertEquals(default, orderedMenu(saved).map { it.key })
    }
}
