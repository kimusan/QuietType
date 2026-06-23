package dk.schulz.voiceme.accessibility

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dk.schulz.voiceme.R
import dk.schulz.voiceme.dictation.DictationCommand
import dk.schulz.voiceme.dictation.DictationInteractionController
import dk.schulz.voiceme.dictation.VoiceMeRecordingService
import dk.schulz.voiceme.settings.AppSettingsStore
import dk.schulz.voiceme.settings.DictationInteraction

class VoiceMeAccessibilityService : AccessibilityService() {
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isDictationRecording = false
    private var lastDetection: FocusedFieldDetection? = null

    override fun onCreate() {
        super.onCreate()
        settingsStore = AppSettingsStore(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        showServiceReadyNotification()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !event.isFocusRelevant()) {
            return
        }

        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: event.source
        if (node == null) {
            hideOverlay()
            lastDetection = null
            return
        }

        try {
            val snapshot = node.toFocusedFieldSnapshot(
                eventPackageName = event.packageName?.toString(),
                eventClassName = event.className?.toString(),
            )
            val detection = FocusedFieldDetector.detect(
                snapshot = snapshot,
                settings = settingsStore.load(),
            )
            lastDetection = detection

            if (detection.shouldShowOverlay) {
                showOrUpdateOverlay(detection)
            } else {
                hideOverlay()
            }
        } finally {
            node.recycle()
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        hideOverlay()
        stopDictationRecording()
        cancelServiceReadyNotification()
        super.onDestroy()
    }

    private fun showOrUpdateOverlay(detection: FocusedFieldDetection) {
        val existing = overlayView as? TextView
        if (existing != null) {
            existing.text = overlayLabel(detection)
            return
        }

        val view = TextView(this).apply {
            text = overlayLabel(detection)
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF6750A4.toInt())
            setPadding(28, 18, 28, 18)
            elevation = 12f
            contentDescription = "VoiceMe dictation preview button"
            setOnClickListener {
                insertStubTranscriptIntoFocusedField()
            }
            setOnTouchListener(
                OverlayDragTouchListener(
                    windowManager = windowManager,
                    density = resources.displayMetrics.density,
                    onMoved = ::persistOverlayPosition,
                    onDictationCommand = ::handleDictationCommand,
                    interaction = { settingsStore.load().dictationInteraction },
                    isRecording = { isDictationRecording },
                ),
            )
        }

        windowManager.addView(view, overlayLayoutParams())
        overlayView = view
    }

    private fun hideOverlay() {
        stopDictationRecording()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    private fun insertStubTranscriptIntoFocusedField() {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode == null) {
            showToast("No editable text field is focused.")
            return
        }

        try {
            val snapshot = focusedNode.toFocusedFieldSnapshot(
                eventPackageName = focusedNode.packageName?.toString(),
                eventClassName = focusedNode.className?.toString(),
            )
            val draft = TextInsertionDraft.from(
                TextInsertionRequest(
                    focusedField = snapshot,
                    existingText = focusedNode.text?.toString().orEmpty(),
                    transcript = "VoiceMe dictation test",
                ),
            )
            if (!draft.canInsert) {
                showToast("VoiceMe did not insert text: ${draft.blockReason}.")
                return
            }

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    draft.textToSet,
                )
            }
            if (focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                showToast("Inserted VoiceMe test transcript.")
            } else {
                showToast("This field does not accept accessibility text insertion.")
            }
        } finally {
            focusedNode.recycle()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun persistOverlayPosition(position: OverlayPosition) {
        val settings = settingsStore.load()
        settingsStore.save(
            settings.copy(
                overlayOffsetXDp = position.xDp,
                overlayOffsetYDp = position.yDp,
            ),
        )
    }

    private fun handleDictationCommand(command: DictationCommand) {
        when (command) {
            DictationCommand.None -> Unit
            DictationCommand.StartRecording -> startDictationRecording()
            DictationCommand.StopRecording -> stopDictationRecording()
            DictationCommand.ToggleRecording -> if (isDictationRecording) {
                stopDictationRecording()
            } else {
                startDictationRecording()
            }
        }
    }

    private fun startDictationRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showToast("Allow microphone in VoiceMe before dictating.")
            isDictationRecording = false
            return
        }
        ContextCompat.startForegroundService(
            this,
            VoiceMeRecordingService.startIntent(this),
        )
        isDictationRecording = true
    }

    private fun stopDictationRecording() {
        stopService(VoiceMeRecordingService.stopIntent(this))
        isDictationRecording = false
    }

    private fun overlayLabel(detection: FocusedFieldDetection): String {
        val appLabel = detection.packageName?.substringAfterLast('.')?.takeIf { it.isNotBlank() }
        return if (appLabel == null) {
            "🎙 VoiceMe"
        } else {
            "🎙 VoiceMe · $appLabel"
        }
    }

    @SuppressLint("MissingPermission")
    private fun showServiceReadyNotification() {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(this, ServiceChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(VoiceMeAccessibilityPresentation.NotificationTitle)
            .setContentText(VoiceMeAccessibilityPresentation.NotificationText)
            .setOngoing(true)
            .setSilent(true)
            .build()
        runCatching {
            getSystemService(NotificationManager::class.java).notify(ServiceNotificationId, notification)
        }
    }

    private fun cancelServiceReadyNotification() {
        runCatching {
            getSystemService(NotificationManager::class.java).cancel(ServiceNotificationId)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ServiceChannelId) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ServiceChannelId,
                    "VoiceMe accessibility",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows when the VoiceMe accessibility floating button service is enabled."
                },
            )
        }
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams {
        val settings = settingsStore.load()
        val (overlayX, overlayY) = OverlayPlacementPolicy.toPx(
            position = OverlayPosition(settings.overlayOffsetXDp, settings.overlayOffsetYDp),
            density = resources.displayMetrics.density,
        )
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = overlayX
            y = overlayY
        }
    }

    private fun AccessibilityEvent.isFocusRelevant(): Boolean = eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
        eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
        eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
        eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED

    private fun AccessibilityNodeInfo.toFocusedFieldSnapshot(
        eventPackageName: String?,
        eventClassName: String?,
    ): FocusedFieldSnapshot = FocusedFieldSnapshot(
        packageName = packageName?.toString() ?: eventPackageName,
        className = className?.toString() ?: eventClassName,
        isFocused = isFocused,
        isEditable = isEditable,
        isPassword = isPassword,
    )

    companion object {
        private const val ServiceChannelId = "voiceme_accessibility"
        private const val ServiceNotificationId = 2001
    }

    private class OverlayDragTouchListener(
        private val windowManager: WindowManager,
        private val density: Float,
        private val onMoved: (OverlayPosition) -> Unit,
        private val onDictationCommand: (DictationCommand) -> Unit,
        private val interaction: () -> DictationInteraction,
        private val isRecording: () -> Boolean,
    ) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downRawX = 0f
        private var downRawY = 0f
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dragging = false
                    onDictationCommand(
                        DictationInteractionController.onButtonDown(
                            interaction = interaction(),
                            isRecording = isRecording(),
                        ),
                    )
                    return false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (!dragging && kotlin.math.abs(dx) + kotlin.math.abs(dy) < 12) {
                        return false
                    }
                    dragging = true
                    params.x = (startX - dx).coerceAtLeast(0)
                    params.y = (startY - dy).coerceAtLeast(0)
                    windowManager.updateViewLayout(view, params)
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasDragging = dragging
                    dragging = false
                    onMoved(OverlayPlacementPolicy.fromPx(params.x, params.y, density))
                    onDictationCommand(
                        DictationInteractionController.onButtonUp(
                            interaction = interaction(),
                            isRecording = isRecording(),
                            wasDragging = wasDragging,
                        ),
                    )
                    return wasDragging
                }
            }
            return false
        }
    }
}
