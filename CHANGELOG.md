# Changelog

All notable user-facing changes to VoiceMe will be documented in this file.

The format follows Keep a Changelog principles, and versions should follow semantic versioning where practical.

## Unreleased

### Added

- Initial product, architecture, privacy, permissions, UX, and release planning documents.
- Android/Kotlin project bootstrap with Gradle wrapper, Compose Material 3 app shell, dynamic color theme, debug/release build configuration, and a unit test.
- Interactive Material 3 setup preview with onboarding, status, and settings sections for the privacy-first dictation flow.
- Local settings persistence for onboarding completion, dictation interaction preference, offline-only mode, and sensitive-field behavior.
- Accessibility service registration, focused editable-field detection, and a draggable safe microphone preview overlay, plus an in-app shortcut to Android Accessibility settings.
- Foreground microphone recording shell with runtime microphone permission request, visible service notification, and local `AudioRecord` capture groundwork.
- Model catalog UI with persisted selected-model and prepare/delete marker state for local ASR model planning.
- ASR-stub text insertion prototype that appends a fixed test phrase through accessibility `ACTION_SET_TEXT` when the user taps the overlay.
- HTTPS model artifact download plumbing with SHA-256 verification, private model-file storage, and deletion semantics before a model archive is marked downloaded.
- Locked the compact sherpa-onnx English streaming model catalog entry to a real GitHub release artifact and SHA-256 checksum.
- Separate downloaded-archive and prepared-for-dictation model states so verified archives are not misrepresented as ASR-ready before runtime preparation is implemented.
- Release signing template and release-gate checklist for reproducible signed APK builds without committing keystores.
