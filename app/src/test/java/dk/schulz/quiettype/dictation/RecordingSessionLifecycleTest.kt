package dk.schulz.quiettype.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSessionLifecycleTest {
    @Test
    fun stopIntentSignalsRunningWorkerInsteadOfDestroyingServiceImmediately() {
        val action = RecordingSessionLifecycle.stopIntentAction(hasRunningWorker = true)

        assertEquals(RecordingSessionLifecycle.StopIntentAction.SignalWorkerAndKeepServiceAlive, action)
        assertFalse(action.shouldStopServiceImmediately)
    }

    @Test
    fun stopIntentCanStopServiceWhenNoWorkerIsRunning() {
        val action = RecordingSessionLifecycle.stopIntentAction(hasRunningWorker = false)

        assertEquals(RecordingSessionLifecycle.StopIntentAction.StopServiceNow, action)
        assertTrue(action.shouldStopServiceImmediately)
    }

    @Test
    fun terminalStateAlwaysClearsProcessingOverlay() {
        assertTrue(RecordingSessionLifecycle.shouldBroadcastProcessingFalseOnTerminalState)
    }
}
