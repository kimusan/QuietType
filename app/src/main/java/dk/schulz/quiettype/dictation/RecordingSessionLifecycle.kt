package dk.schulz.quiettype.dictation

object RecordingSessionLifecycle {
    enum class StopIntentAction(val shouldStopServiceImmediately: Boolean) {
        SignalWorkerAndKeepServiceAlive(shouldStopServiceImmediately = false),
        StopServiceNow(shouldStopServiceImmediately = true),
    }

    val shouldBroadcastProcessingFalseOnTerminalState: Boolean = true

    fun stopIntentAction(hasRunningWorker: Boolean): StopIntentAction = if (hasRunningWorker) {
        StopIntentAction.SignalWorkerAndKeepServiceAlive
    } else {
        StopIntentAction.StopServiceNow
    }
}
