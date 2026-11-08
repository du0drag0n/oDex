package io.lemoncore.opendatadex.ui.extras

import org.junit.Assert.assertEquals
import org.junit.Test

class MilestoneTest {
    @Test
    fun crossingIsCountedOnTheWayUpOnly() {
        assertEquals(25, milestoneCrossed(24, 25, 100))
        assertEquals(null, milestoneCrossed(25, 26, 100))
        assertEquals(50, milestoneCrossed(49, 50, 100))
        assertEquals(100, milestoneCrossed(385, 386, 386))
        assertEquals(null, milestoneCrossed(50, 49, 100))
        // 25% of 151 is 37.75, so the 38th catch reaches it.
        assertEquals(null, milestoneCrossed(36, 37, 151))
        assertEquals(25, milestoneCrossed(37, 38, 151))
        assertEquals(null, milestoneCrossed(0, 1, 0))
    }
}
