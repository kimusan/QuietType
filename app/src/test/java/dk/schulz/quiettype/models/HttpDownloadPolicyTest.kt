package dk.schulz.quiettype.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HttpDownloadPolicyTest {
    @Test
    fun acceptsSuccessfulResponseCodes() {
        assertEquals(200, HttpDownloadPolicy.requireSuccessfulStatus(200))
        assertEquals(206, HttpDownloadPolicy.requireSuccessfulStatus(206))
    }

    @Test
    fun rejectsRedirectsClientErrorsAndServerErrors() {
        assertThrows(IllegalStateException::class.java) { HttpDownloadPolicy.requireSuccessfulStatus(302) }
        assertThrows(IllegalStateException::class.java) { HttpDownloadPolicy.requireSuccessfulStatus(404) }
        assertThrows(IllegalStateException::class.java) { HttpDownloadPolicy.requireSuccessfulStatus(500) }
    }
}
