# VoiceMe Permissions

This document tracks every Android permission VoiceMe requests and why. Keep it current with each release.

## Implemented capabilities

| Permission / capability | Required for | Current status |
| --- | --- | --- |
| `INTERNET` | Explicit user-initiated ASR model downloads | Declared for the Models screen download action. Downloads must use HTTPS and SHA-256 verification before a model archive is marked downloaded; network is not used for dictation, telemetry, or cloud transcription. |
| `RECORD_AUDIO` | Capturing speech while the user explicitly starts dictation | Runtime permission requested from onboarding or the Status screen. Granting permission does not start capture. The overlay starts the current foreground shell only while dictation is active: hold-to-talk captures while pressed, and tap-to-toggle captures until the next non-drag tap. No ASR model is connected and raw audio is not persisted. |
| `FOREGROUND_SERVICE` | Running a visible service while microphone capture is active | Declared for the recording shell. The service shows a persistent low-importance notification while active. |
| `FOREGROUND_SERVICE_MICROPHONE` | Android foreground-service type for microphone capture | Declared for `VoiceMeRecordingService` so microphone capture happens under the explicit microphone foreground-service type. |
| `POST_NOTIFICATIONS` | Showing foreground-service status on Android versions that gate notifications | Declared so the app can present the microphone foreground-service notification. Runtime notification prompting still needs a polished production flow. |
| Accessibility service binding (`android.permission.BIND_ACCESSIBILITY_SERVICE`) | User-enabled service that detects focused editable fields and hosts the keyboard-adjacent mic preview control | Registered in the app manifest. The service requests window-content capability so Android can identify focused editable fields. It now listens to focus, click, text, and window-content events, falls back to Android's active focused input node when an event source is missing, posts a low-importance “VoiceMe floating button ready” notification while connected, hides for password fields by default, and shows a draggable accessibility overlay preview. The Status screen includes an editable test field so users can trigger the actual overlay after enabling the service. On explicit overlay tap, it reads the focused field text to append the fixed ASR-stub phrase and calls `ACTION_SET_TEXT`. |

## Planned permissions

| Permission / capability | Required for | Notes |
| --- | --- | --- |
| Optional release/update metadata | Future update checks or model catalog refreshes | Not implemented. Any future network metadata must remain explicit and documented; avoid in offline/F-Droid flavor if practical and never use for cloud transcription by default. |
| Input method service | Optional VoiceMe keyboard fallback | More reliable text insertion, but user must switch keyboards. |

## Permissions intentionally avoided

- Contacts, calendar, location, SMS, call log, camera, and account permissions are not part of the product scope.
- Overlay permission (`SYSTEM_ALERT_WINDOW`) should not be needed for the MVP if the accessibility overlay is sufficient.
- Network permission is limited to real, user-initiated model download code.

## Sensitive-field policy

VoiceMe should not dictate into password fields or other fields Android marks as sensitive. The app hides or disables the dictation button when such a field is detected by default.
