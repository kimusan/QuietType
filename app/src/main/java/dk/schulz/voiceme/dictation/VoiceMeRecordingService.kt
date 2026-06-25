package dk.schulz.voiceme.dictation

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
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineStream
import dk.schulz.voiceme.R
import dk.schulz.voiceme.models.ModelCatalog
import dk.schulz.voiceme.settings.AppSettingsStore
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class VoiceMeRecordingService : Service() {
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
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasMicrophonePermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startForeground(NotificationId, buildNotification("Listening locally with VoiceMe."))
        startRecognitionSession()
        return START_STICKY
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
        if (!SherpaRuntimeConfig.canRunOnline(model, runtimeDirectory)) {
            notifyStatus("Download and prepare ${model.name} before dictating.")
            stopSelf()
            return
        }

        keepRecording.set(true)
        recordingThread = Thread({
            runRecognitionLoop(modelName = model.name, runtimeDirectory = runtimeDirectory)
        }, "VoiceMeSherpaRecognition").apply { start() }
    }

    @SuppressLint("MissingPermission")
    private fun runRecognitionLoop(modelName: String, runtimeDirectory: File) {
        var recognizer: OnlineRecognizer? = null
        var stream: OnlineStream? = null
        var lastText = ""
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
                notifyStatus("Could not open microphone for VoiceMe.")
                return
            }
            recorder = audioRecord
            val shortBuffer = ShortArray(minBufferSize / 2)
            audioRecord.startRecording()
            notifyStatus("VoiceMe is dictating with $modelName.")
            while (keepRecording.get()) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (read <= 0) continue
                val floatSamples = FloatArray(read) { index -> shortBuffer[index] / Short.MAX_VALUE.toFloat() }
                stream.acceptWaveform(floatSamples, SherpaRuntimeConfig.SampleRateHz)
                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream)
                }
                val text = recognizer.getResult(stream).text.trim()
                if (text.isNotBlank()) lastText = text
            }
            stream.inputFinished()
            while (recognizer.isReady(stream)) {
                recognizer.decode(stream)
            }
            val finalText = recognizer.getResult(stream).text.trim().ifBlank { lastText }
            broadcastFinalTranscript(finalText)
        } catch (error: Throwable) {
            notifyStatus("VoiceMe dictation stopped: ${error.message ?: error::class.java.simpleName}.")
        } finally {
            runCatching { recorder?.stop() }
            recorder?.release()
            recorder = null
            stream?.release()
            recognizer?.release()
            keepRecording.set(false)
        }
    }

    private fun stopRecognitionSession() {
        keepRecording.set(false)
        runCatching { recorder?.stop() }
        recordingThread?.join(1_500)
        recordingThread = null
        recorder?.release()
        recorder = null
    }

    private fun broadcastFinalTranscript(transcript: String) {
        val clean = DictationTranscriptContract.cleanFinalTranscript(transcript) ?: return
        sendBroadcast(
            Intent(DictationTranscriptContract.ActionFinalTranscript).apply {
                setPackage(packageName)
                putExtra(DictationTranscriptContract.ExtraTranscript, clean)
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
        .setContentTitle("VoiceMe dictation")
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
                    "VoiceMe dictation",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows when VoiceMe microphone capture is active."
                },
            )
        }
    }

    companion object {
        private const val ChannelId = "voiceme_dictation"
        private const val NotificationId = 1001
        private const val ActionStop = "dk.schulz.voiceme.action.STOP_RECORDING"

        fun startIntent(context: Context): Intent = Intent(context, VoiceMeRecordingService::class.java)
        fun stopIntent(context: Context): Intent = Intent(context, VoiceMeRecordingService::class.java).apply {
            action = ActionStop
        }
    }
}
