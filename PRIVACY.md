# QuietType Privacy Policy

QuietType is designed to be privacy-first. This document describes the intended privacy behavior for the app as it is developed. It must be updated before every public release.

## Summary

- Voice dictation is processed on-device by default.
- Audio is not sent to cloud transcription services.
- Transcripts are not sent to the developer.
- Dictation history is off by default; if enabled, saved entries stay in private app storage and can be deleted.
- No analytics or telemetry are enabled by default.
- Any network use must be visible, user-initiated, and documented here.

## Audio

QuietType uses the microphone only while the user actively starts dictation. Hold-to-talk starts capture on press and stops on release; tap-to-toggle starts or stops capture on each non-drag tap. Granting microphone permission by itself does not start capture. Active dictation opens `AudioRecord` locally, feeds audio into the on-device sherpa-onnx recognizer, and shows a visible notification. The app does not persist raw audio unless a future feature explicitly asks for user consent and this policy is updated.

## Transcripts

Dictated text is inserted into the user's chosen input field. The Accessibility service receives final transcript broadcasts from the local dictation service and reads the focused node's existing text at insertion time so it can append the recognized text instead of overwriting the field. Transcript history is off by default. If the user enables local history, QuietType saves only successful final dictation text entries in private app storage, never raw audio or live partial text. The History screen lets the user copy individual entries, delete individual entries, or clear all saved entries.

## Accessibility service

QuietType includes an AccessibilityService registration so users can enable the keyboard-adjacent dictation mode from Android settings. This capability is needed to detect focused editable fields and place a mic control next to the normal keyboard. The service looks at accessibility metadata needed to decide whether a focused node is editable or password/sensitive. It reads the focused field text only when inserting an explicit final dictation result. It hides or disables the overlay for sensitive fields by default.

## Local preferences

QuietType stores a small set of local preferences, such as onboarding completion and preview settings for dictation interaction, offline-only mode, sensitive-field behavior, selected model ID, and model install markers. These preferences stay in the app's private storage and Android backup is disabled for the app.

## Model files

ASR models may be downloaded after the user chooses a model. The UI starts explicit HTTPS model downloads, verifies the artifact SHA-256 before storing a downloaded/prepared marker, writes verified artifacts under the app's private `filesDir/models/` directory, unpacks runtime files into a private `runtime/` directory, and deletes those private files when the user deletes a model. The default sherpa-onnx Parakeet TDT v3 int8 archive is locked to a real GitHub release URL and SHA-256 checksum; it supports Danish and other European languages but is a large download. Model licenses and approximate sizes must be shown before download.

## Network

QuietType declares network access for explicit user-initiated model downloads. Network access is not used for cloud transcription, telemetry, or dictation. The offline/F-Droid flavor should avoid network access during dictation and may need a bundled-model or side-loaded-model path.

## Diagnostics

Crash reports, logs, analytics, or diagnostics must be off by default unless implemented in a privacy-preserving and explicitly disclosed way. Logs must avoid audio, transcript text, and focused-field content.

## Data deletion

Settings and History provide controls to delete downloaded models, local preferences, individual history entries, and all saved history entries.

## Third parties

Third-party dependencies and model providers must be documented in release notes and license/NOTICE files. Proprietary analytics or advertising SDKs are not part of the planned app.
