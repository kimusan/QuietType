package dk.schulz.quiettype.dictation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineStream
import dk.schulz.quiettype.R
import dk.schulz.quiettype.models.ModelCatalog
import dk.schulz.quiettype.models.ModelRuntimeKind
import dk.schulz.quiettype.settings.AppSettingsStore
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class QuietTypeRecordingService : Service() {
    private val keepRecording = AtomicBoolean(false)
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private lateinit var settingsStore: AppSettingsStore

    override fun onCreate() {
        super.onCreate()
        settingsStore = AppSettingsStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ActionStop) {
            keepRecording.set(false)
            runCatching { recorder?.stop() }
            when (RecordingSessionLifecycle.stopIntentAction(hasRunningWorker = recordingThread?.isAlive == true)) {
                RecordingSessionLifecycle.StopIntentAction.SignalWorkerAndKeepServiceAlive -> notifyStatus("Processing QuietType dictation locally.")
                RecordingSessionLifecycle.StopIntentAction.StopServiceNow -> stopSelf()
            }
            return START_NOT_STICKY
        }

        if (!hasMicrophonePermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startForeground(NotificationId, buildNotification("Listening locally with QuietType."))
        startRecognitionSession()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRecognitionSession()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startRecognitionSession() {
        if (recordingThread?.isAlive == true || !hasMicrophonePermission()) return
        val model = ModelCatalog.default().modelById(settingsStore.load().selectedModelId)
        if (model == null) {
            stopSelf()
            return
        }
        val runtimeDirectory = File(filesDir, "models/${model.id}/runtime")
        if (!SherpaRuntimeConfig.canRunDictation(model, runtimeDirectory)) {
            notifyStatus("Download and prepare ${model.name} before dictating.")
            stopSelf()
            return
        }

        keepRecording.set(true)
        recordingThread = Thread({
            when (model.runtime.kind) {
                ModelRuntimeKind.SherpaOnnxOfflineTransducer,
                ModelRuntimeKind.SherpaOnnxOfflineCtc -> runOfflineTransducerLoop(
                    modelName = model.name,
                    runtimeDirectory = runtimeDirectory,
                )
                ModelRuntimeKind.SherpaOnnxStreamingTransducer -> runOnlineStreamingLoop(
                    modelName = model.name,
                    runtimeDirectory = runtimeDirectory,
                )
                else -> notifyStatus("${model.name} is not supported for dictation yet.")
            }
        }, "QuietTypeSherpaRecognition").apply { start() }
    }

    @SuppressLint("MissingPermission")
    private fun runOfflineTransducerLoop(modelName: String, runtimeDirectory: File) {
        var recognizer: OfflineRecognizer? = null
        var stream: OfflineStream? = null
        try {
            val audioChunks = recordHeldAudio(modelName)
            if (audioChunks.isEmpty()) return
            broadcastProcessingState(true)
            val model = ModelCatalog.default().modelById(settingsStore.load().selectedModelId) ?: return
            recognizer = OfflineRecognizer(
                assetManager = null,
                config = SherpaRuntimeConfig.buildOfflineRecognizerConfig(
                    model = model,
                    runtimeDirectory = runtimeDirectory,
                ),
            )
            stream = recognizer.createStream()
            audioChunks.forEach { chunk ->
                stream.acceptWaveform(chunk, SherpaRuntimeConfig.SampleRateHz)
            }
            recognizer.decode(stream)
            broadcastFinalTranscript(recognizer.getResult(stream).text.trim())
        } catch (error: Throwable) {
            notifyStatus("QuietType dictation stopped: ${error.message ?: error::class.java.simpleName}.")
        } finally {
            stream?.release()
            recognizer?.release()
            broadcastProcessingState(false)
            keepRecording.set(false)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun runOnlineStreamingLoop(modelName: String, runtimeDirectory: File) {
        var recognizer: OnlineRecognizer? = null
        var stream: OnlineStream? = null
        var lastText = ""
        val settings = settingsStore.load()
        val liveSegmenter = if (settings.liveSentenceInsertionEnabled) LiveTranscriptSegmenter() else null
        try {
            recognizer = OnlineRecognizer(
                assetManager = null,
                config = SherpaRuntimeConfig.buildOnlineRecognizerConfig(
                    model = ModelCatalog.default().modelById(settingsStore.load().selectedModelId)!!,
                    runtimeDirectory = runtimeDirectory,
                ),
            )
            stream = recognizer.createStream()
            val minBufferSize = AudioRecord.getMinBufferSize(
                SherpaRuntimeConfig.SampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(SherpaRuntimeConfig.SampleRateHz / 5)
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SherpaRuntimeConfig.SampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
            )
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                notifyStatus("Could not open microphone for QuietType.")
                return
            }
            recorder = audioRecord
            val shortBuffer = ShortArray(minBufferSize / 2)
            audioRecord.startRecording()
            notifyStatus("QuietType is dictating with $modelName.")
            while (keepRecording.get()) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (read <= 0) continue
                val floatSamples = FloatArray(read) { index -> shortBuffer[index] / Short.MAX_VALUE.toFloat() }
                stream.acceptWaveform(floatSamples, SherpaRuntimeConfig.SampleRateHz)
                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream)
                }
                val text = recognizer.getResult(stream).text.trim()
                if (text.isNotBlank()) {
                    lastText = text
                    liveSegmenter?.nextStableSegment(text)?.let(::broadcastLiveTranscript)
                }
            }
            // Avoid a final native decode during service teardown. On the current sherpa
            // Android runtime this path can abort the process after the foreground
            // service is stopped, which also restarts the accessibility overlay. Use
            // the last stable partial result until the runtime path is hardened enough
            // for near-real-time partial insertion.
            val finalText = liveSegmenter?.remainingSegment(lastText) ?: lastText
            broadcastFinalTranscript(finalText)
        } catch (error: Throwable) {
            notifyStatus("QuietType dictation stopped: ${error.message ?: error::class.java.simpleName}.")
        } finally {
            runCatching { recorder?.stop() }
            recorder?.release()
            recorder = null
            stream?.release()
            recognizer?.release()
            broadcastProcessingState(false)
            keepRecording.set(false)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun recordHeldAudio(modelName: String): List<FloatArray> {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SherpaRuntimeConfig.SampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(SherpaRuntimeConfig.SampleRateHz / 5)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SherpaRuntimeConfig.SampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            notifyStatus("Could not open microphone for QuietType.")
            return emptyList()
        }
        val chunks = mutableListOf<FloatArray>()
        recorder = audioRecord
        try {
            val shortBuffer = ShortArray(minBufferSize / 2)
            audioRecord.startRecording()
            notifyStatus("QuietType is dictating with $modelName.")
            while (keepRecording.get()) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (read <= 0) continue
                chunks += FloatArray(read) { index -> shortBuffer[index] / Short.MAX_VALUE.toFloat() }
            }
        } finally {
            runCatching { audioRecord.stop() }
            audioRecord.release()
            if (recorder === audioRecord) recorder = null
        }
        return chunks
    }

    private fun stopRecognitionSession() {
        keepRecording.set(false)
        runCatching { recorder?.stop() }
        recordingThread?.join(3_000)
        if (recordingThread?.isAlive != true) {
            recordingThread = null
        }
    }

    private fun broadcastFinalTranscript(transcript: String) {
        val clean = DictationTranscriptContract.cleanFinalTranscript(transcript) ?: return
        sendTranscriptBroadcast(DictationTranscriptContract.ActionFinalTranscript, clean)
    }

    private fun broadcastLiveTranscript(transcript: String) {
        val clean = DictationTranscriptContract.cleanLiveTranscript(transcript) ?: return
        sendTranscriptBroadcast(DictationTranscriptContract.ActionLiveTranscript, clean)
    }

    private fun sendTranscriptBroadcast(action: String, transcript: String) {
        sendBroadcast(
            Intent(action).apply {
                setPackage(packageName)
                putExtra(DictationTranscriptContract.ExtraTranscript, transcript)
            },
        )
    }

    private fun broadcastProcessingState(isProcessing: Boolean) {
        sendBroadcast(
            Intent(DictationTranscriptContract.ActionProcessingState).apply {
                setPackage(packageName)
                putExtra(DictationTranscriptContract.ExtraIsProcessing, isProcessing)
            },
        )
    }

    private fun notifyStatus(message: String) {
        ensureNotificationChannel()
        runCatching {
            getSystemService(NotificationManager::class.java).notify(NotificationId, buildNotification(message))
        }
    }

    private fun hasMicrophonePermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private fun buildNotification(message: String): Notification = NotificationCompat.Builder(this, ChannelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("QuietType dictation")
        .setContentText(message)
        .setOngoing(true)
        .setSilent(true)
        .build()

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(ChannelId)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ChannelId,
                    "QuietType dictation",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows when QuietType microphone capture is active."
                },
            )
        }
    }

    companion object {
        private const val ChannelId = "quiettype_dictation"
        private const val NotificationId = 1001
        private const val ActionStop = "dk.schulz.quiettype.action.STOP_RECORDING"

        fun startIntent(context: Context): Intent = Intent(context, QuietTypeRecordingService::class.java)
        fun stopIntent(context: Context): Intent = Intent(context, QuietTypeRecordingService::class.java).apply {
            action = ActionStop
        }
    }
}
