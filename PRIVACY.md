# VoiceMe Privacy Policy

VoiceMe is designed to be privacy-first. This document describes the intended privacy behavior for the app as it is developed. It must be updated before every public release.

## Summary

- Voice dictation is processed on-device by default.
- Audio is not sent to cloud transcription services.
- Transcripts are not sent to the developer.
- No analytics or telemetry are enabled by default.
- Any network use must be visible, user-initiated, and documented here.

## Audio

VoiceMe uses the microphone only while the user actively starts dictation. The current prototype has a foreground microphone shell that opens `AudioRecord` locally and shows a visible notification, but no ASR engine is connected yet. Future builds will stream audio to the local ASR engine running on the device. The app should not persist raw audio unless a future feature explicitly asks for user consent and this policy is updated.

## Transcripts

Dictated text is inserted into the user's chosen input field. The current Accessibility overlay inserts only a fixed test phrase (`VoiceMe dictation test`) when the user taps the preview overlay; this is an insertion prototype, not real speech transcription. To append the test phrase, the service reads the focused node's existing text at the moment of the explicit tap. VoiceMe should not store transcript history by default. If transcript history is added later, it must be local-only, opt-in, and deletable from settings.

## Accessibility service

VoiceMe includes an AccessibilityService registration so users can enable the keyboard-adjacent dictation mode from Android settings. This capability is needed to detect focused editable fields and place a mic control next to the normal keyboard. The service looks at accessibility metadata needed to decide whether a focused node is editable or password/sensitive. It reads the focused field text only after the user explicitly taps the overlay insertion prototype so it can append the fixed ASR-stub phrase instead of overwriting the field. The preview overlay does not run real speech transcription yet.

## Local preferences

VoiceMe stores a small set of local preferences, such as onboarding completion and preview settings for dictation interaction, offline-only mode, sensitive-field behavior, selected model ID, and model install markers. These preferences stay in the app's private storage and Android backup is disabled for the app.

## Model files

ASR models may be downloaded after the user chooses a model. The current UI starts explicit HTTPS model downloads, verifies the artifact SHA-256 before storing a downloaded-archive marker, writes verified artifacts under the app's private `filesDir/models/` directory, and deletes those private files when the user deletes a model. The compact sherpa-onnx English streaming archive is locked to a real GitHub release URL and SHA-256 checksum. Downloaded archives are deliberately not treated as dictation-ready until the ASR runtime preparation step verifies/extracts the files needed by the engine. Model licenses and approximate sizes must be shown before download.

## Network

VoiceMe declares network access for explicit user-initiated model downloads. Network access is not used for cloud transcription, telemetry, or dictation. The offline/F-Droid flavor should avoid network access during dictation and may need a bundled-model or side-loaded-model path.

## Diagnostics

Crash reports, logs, analytics, or diagnostics must be off by default unless implemented in a privacy-preserving and explicitly disclosed way. Logs must avoid audio, transcript text, and focused-field content.

## Data deletion

Settings must provide controls to delete downloaded models, local preferences, and any optional history if such history exists.

## Third parties

Third-party dependencies and model providers must be documented in release notes and license/NOTICE files. Proprietary analytics or advertising SDKs are not part of the planned app.
