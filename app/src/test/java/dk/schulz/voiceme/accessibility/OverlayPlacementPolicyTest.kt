package dk.schulz.voiceme.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPlacementPolicyTest {
    @Test
    fun defaultPositionSitsAboveTypicalKeyboardArea() {
        assertEquals(16, OverlayPlacementPolicy.DefaultPosition.xDp)
        assertTrue(OverlayPlacementPolicy.DefaultPosition.yDp >= 320)
    }

    @Test
    fun overlayPositionRoundTripsBetweenDpSettingsAndWindowPixels() {
        val saved = OverlayPosition(xDp = 24, yDp = 360)

        val (xPx, yPx) = OverlayPlacementPolicy.toPx(saved, density = 2f)
        val restored = OverlayPlacementPolicy.fromPx(xPx, yPx, density = 2f)

        assertEquals(saved, restored)
    }
}
