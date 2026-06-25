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
- Upgraded the vendored sherpa-onnx Android runtime to v1.13.3 so packaged arm64 native libraries are 16 KB page-size compatible on newer Android devices.
- Switched sherpa recognizer creation to file-mode loading for app-private absolute model paths, fixing a native crash when dictation starts from the floating button.
- Added runtime-file validation so missing or empty prepared model files are rejected before native ASR startup.
- Model catalog UI with persisted selected-model and prepare/delete marker state for local ASR model planning.
- Final transcript insertion that appends recognized on-device dictation text through accessibility `ACTION_SET_TEXT` when a local dictation session stops.
- HTTPS model artifact download plumbing with SHA-256 verification, private model-file storage, progress bar/percentage text, and deletion semantics before a model archive is marked downloaded.
- Switched the default catalog candidate to sherpa-onnx Parakeet TDT v3 int8 with Danish/multilingual support, a real GitHub release URL, and SHA-256 checksum.
- Kept Parakeet v2 int8, compact streaming English, and compact CTC models as fallback/benchmark candidates; fp32 Parakeet is listed as a mobile-unfriendly benchmark only.
- Separate downloaded-archive and prepared-for-dictation model states; verified sherpa `.tar.bz2` archives are unpacked into private runtime files and marked prepared when all runtime-required files are present.
- Release signing template and release-gate checklist for reproducible signed APK builds without committing keystores.
- Action-based onboarding buttons for Accessibility settings, microphone permission, and model setup, with “already enabled/allowed/ready” labels when setup is complete.
- Status-screen overlay test field that lets users reliably focus an editable field and trigger the actual Accessibility floating button after enabling the service.
- Accessibility service readiness notification and broader text/window event handling so the floating button is easier to surface in modern input fields.
- Floating mic default placement now starts higher above the bottom edge, and dragged overlay position is saved locally for the next focused text field.
- Overlay dictation interaction now owns the microphone only during active hold-to-talk or tap-to-toggle dictation instead of starting capture immediately after microphone permission is granted.
