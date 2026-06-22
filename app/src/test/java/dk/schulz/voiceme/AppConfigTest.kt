package dk.schulz.voiceme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {
    @Test
    fun appConfigReflectsPrivacyFirstBootstrap() {
        assertEquals("0.1.0-dev", AppConfig.versionLabel)
        assertTrue(AppConfig.OFFLINE_FIRST)
    }
}
