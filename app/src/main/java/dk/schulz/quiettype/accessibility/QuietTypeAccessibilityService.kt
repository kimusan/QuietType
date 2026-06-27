package dk.schulz.quiettype.accessibility

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dk.schulz.quiettype.R
import dk.schulz.quiettype.dictation.DictationCommand
import dk.schulz.quiettype.dictation.DictationInteractionController
import dk.schulz.quiettype.dictation.DictationTranscriptContract
import dk.schulz.quiettype.dictation.QuietTypeRecordingService
import dk.schulz.quiettype.history.DictationHistoryStore
import dk.schulz.quiettype.settings.AppSettingsStore
import dk.schulz.quiettype.settings.DictationInteraction
import dk.schulz.quiettype.settings.OverlayColorPreset

class QuietTypeAccessibilityService : AccessibilityService() {
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var historyStore: DictationHistoryStore
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isDictationRecording = false
    private var isDictationProcessing = false
    private var lastDetection: FocusedFieldDetection? = null
    private var lastFocusedSnapshot: FocusedFieldSnapshot? = null
    private val dictationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DictationTranscriptContract.ActionFinalTranscript -> {
                    isDictationProcessing = false
                    keepActiveOverlayVisible()
                    val transcript = DictationTranscriptContract.cleanFinalTranscript(
                        intent.getStringExtra(DictationTranscriptContract.ExtraTranscript),
                    ) ?: return
                    insertTranscriptIntoFocusedField(transcript, recordHistory = true)
                }
                DictationTranscriptContract.ActionLiveTranscript -> {
                    val transcript = DictationTranscriptContract.cleanLiveTranscript(
                        intent.getStringExtra(DictationTranscriptContract.ExtraTranscript),
                    ) ?: return
                    insertTranscriptIntoFocusedField(transcript, recordHistory = false)
                }
                DictationTranscriptContract.ActionProcessingState -> {
                    isDictationProcessing = intent.getBooleanExtra(DictationTranscriptContract.ExtraIsProcessing, false)
                    keepActiveOverlayVisible()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsStore = AppSettingsStore(this)
        historyStore = DictationHistoryStore(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ContextCompat.registerReceiver(
            this,
            dictationReceiver,
            IntentFilter().apply {
                addAction(DictationTranscriptContract.ActionFinalTranscript)
                addAction(DictationTranscriptContract.ActionLiveTranscript)
                addAction(DictationTranscriptContract.ActionProcessingState)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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
            if (isDictationRecording || isDictationProcessing) {
                keepActiveOverlayVisible()
            } else {
                hideOverlay()
                lastDetection = null
                lastFocusedSnapshot = null
            }
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
            lastFocusedSnapshot = snapshot

            if (detection.shouldShowOverlay) {
                showOrUpdateOverlay(detection)
            } else if (isDictationRecording || isDictationProcessing) {
                keepActiveOverlayVisible()
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
        runCatching { unregisterReceiver(dictationReceiver) }
        cancelServiceReadyNotification()
        super.onDestroy()
    }

    private fun showOrUpdateOverlay(detection: FocusedFieldDetection) {
        val existing = overlayView as? LinearLayout
        if (existing != null) {
            updateOverlayPresentation(existing, detection)
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            elevation = dp(6).toFloat()
            setPadding(dp(3), dp(2), dp(3), dp(2))
        }
        val handle = overlayPart("☰").apply {
            contentDescription = "Drag QuietType floating dictation controls"
            setOnTouchListener(
                OverlayMoveTouchListener(
                    windowManager = windowManager,
                    density = resources.displayMetrics.density,
                    onMoved = ::persistOverlayPosition,
                ),
            )
        }
        val mic = overlayPart("").apply {
            setOnTouchListener(
                OverlayDictationTouchListener(
                    onDictationCommand = ::handleDictationCommand,
                    interaction = { settingsStore.load().dictationInteraction },
                    isRecording = { isDictationRecording },
                ),
            )
        }
        val hide = overlayPart("×").apply {
            contentDescription = "Hide QuietType for this field"
            setOnClickListener { confirmHideCurrentTarget() }
        }
        container.addView(handle)
        container.addView(mic)
        container.addView(hide)
        updateOverlayPresentation(container, detection)

        windowManager.addView(container, overlayLayoutParams())
        overlayView = container
    }

    private fun overlayPart(label: String): TextView = TextView(this).apply {
        text = label
        textSize = 16f
        gravity = Gravity.CENTER
        minWidth = dp(48)
        minHeight = dp(40)
        setPadding(dp(10), dp(4), dp(10), dp(4))
        setTextColor(0xFFFFFFFF.toInt())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hideOverlay() {
        hideOverlay(stopRecording = true)
    }

    private fun hideOverlay(stopRecording: Boolean) {
        if (stopRecording) stopDictationRecording()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    private fun keepActiveOverlayVisible() {
        val detection = lastDetection
        val view = overlayView as? LinearLayout
        if (detection != null && view != null) {
            updateOverlayPresentation(view, detection)
        }
    }

    private fun updateOverlayPresentation(view: LinearLayout, detection: FocusedFieldDetection) {
        val state = currentOverlayState()
        val label = overlayLabel(detection, state)
        val colorPreset = settingsStore.load().overlayColorPreset
        val color = overlayColorFor(state, colorPreset)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(28).toFloat()
            setColor(color)
        }
        view.contentDescription = label
        (view.getChildAt(1) as? TextView)?.apply {
            text = label
            contentDescription = label
        }
    }

    private fun confirmHideCurrentTarget() {
        val target = lastFocusedSnapshot?.let(HiddenFieldTarget::bestFor)
        if (target == null) {
            showToast("QuietType cannot identify this field well enough to hide it.")
            return
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hide QuietType here?")
            .setMessage(
                "QuietType will stop showing the floating button for ${target.displayName}. " +
                    "You can re-enable it from Settings.",
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Hide here") { _, _ ->
                val settings = settingsStore.load()
                if (settings.hiddenTargets.none { it == target }) {
                    settingsStore.save(settings.copy(hiddenTargets = settings.hiddenTargets + target))
                }
                hideOverlay(stopRecording = true)
                showToast("QuietType hidden for ${target.displayName}.")
            }
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }

    private fun insertTranscriptIntoFocusedField(transcript: String, recordHistory: Boolean) {
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
                    hintText = focusedNode.hintText?.toString(),
                    selectionStart = focusedNode.textSelectionStart,
                    selectionEnd = focusedNode.textSelectionEnd,
                    transcript = transcript,
                ),
            )
            if (!draft.canInsert) {
                showToast("QuietType did not insert text: ${draft.blockReason}.")
                return
            }

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    draft.textToSet,
                )
            }
            if (focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                draft.cursorPosition?.let { cursor ->
                    val selectionArguments = Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
                    }
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArguments)
                }
                if (recordHistory) {
                    historyStore.recordTranscript(
                        transcript = transcript,
                        historyEnabled = settingsStore.load().transcriptHistoryEnabled,
                    )
                }
                showToast("Inserted QuietType dictation.")
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
            showToast("Allow microphone in QuietType before dictating.")
            isDictationRecording = false
            return
        }
        runCatching {
            ContextCompat.startForegroundService(
                this,
                QuietTypeRecordingService.startIntent(this),
            )
        }.onSuccess {
            isDictationRecording = true
            isDictationProcessing = false
            keepActiveOverlayVisible()
        }.onFailure { error ->
            isDictationRecording = false
            keepActiveOverlayVisible()
            showToast("QuietType could not start dictation: ${error.message ?: error::class.java.simpleName}")
        }
    }

    private fun stopDictationRecording() {
        val wasRecording = isDictationRecording
        stopService(QuietTypeRecordingService.stopIntent(this))
        isDictationRecording = false
        isDictationProcessing = QuietTypeAccessibilityPresentation.stateAfterStopRequested(wasRecording) == OverlayDictationState.Processing
        keepActiveOverlayVisible()
    }

    private fun currentOverlayState(): OverlayDictationState = when {
        isDictationRecording -> OverlayDictationState.Listening
        isDictationProcessing -> OverlayDictationState.Processing
        else -> OverlayDictationState.Idle
    }

    private fun overlayLabel(@Suppress("UNUSED_PARAMETER") detection: FocusedFieldDetection, state: OverlayDictationState): String =
        QuietTypeAccessibilityPresentation.overlayLabel(state = state)

    @SuppressLint("MissingPermission")
    private fun showServiceReadyNotification() {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(this, ServiceChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(QuietTypeAccessibilityPresentation.NotificationTitle)
            .setContentText(QuietTypeAccessibilityPresentation.NotificationText)
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
                    "QuietType accessibility",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows when the QuietType accessibility floating button service is enabled."
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
        viewIdResourceName = viewIdResourceName,
        hintText = hintText?.toString(),
    )

    companion object {
        private const val ServiceChannelId = "quiettype_accessibility"
        private const val ServiceNotificationId = 2001

        @ColorInt
        private fun overlayColorFor(state: OverlayDictationState, preset: OverlayColorPreset): Int = when (state) {
            OverlayDictationState.Listening -> preset.listeningColor
            OverlayDictationState.Idle,
            OverlayDictationState.Processing -> preset.idleColor
        }
    }

    private class OverlayMoveTouchListener(
        private val windowManager: WindowManager,
        private val density: Float,
        private val onMoved: (OverlayPosition) -> Unit,
    ) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downRawX = 0f
        private var downRawY = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val params = (view.parent as? View)?.layoutParams as? WindowManager.LayoutParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    params.x = (startX - dx).coerceAtLeast(0)
                    params.y = (startY - dy).coerceAtLeast(0)
                    windowManager.updateViewLayout(view.parent as View, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    onMoved(OverlayPlacementPolicy.fromPx(params.x, params.y, density))
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    onMoved(OverlayPlacementPolicy.fromPx(params.x, params.y, density))
                    return true
                }
            }
            return false
        }
    }

    private class OverlayDictationTouchListener(
        private val onDictationCommand: (DictationCommand) -> Unit,
        private val interaction: () -> DictationInteraction,
        private val isRecording: () -> Boolean,
    ) : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onDictationCommand(
                        DictationInteractionController.onButtonDown(
                            interaction = interaction(),
                            isRecording = isRecording(),
                        ),
                    )
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    onDictationCommand(
                        DictationInteractionController.onButtonUp(
                            interaction = interaction(),
                            isRecording = isRecording(),
                            wasDragging = false,
                        ),
                    )
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    onDictationCommand(
                        DictationInteractionController.onButtonUp(
                            interaction = interaction(),
                            isRecording = isRecording(),
                            wasDragging = false,
                        ),
                    )
                    return true
                }
            }
            return true
        }
    }

}
