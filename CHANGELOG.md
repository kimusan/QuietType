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
- Switched the default catalog candidate from English-only to a compact multilingual sherpa-onnx NeMo/FastConformer CTC int8 artifact with a real GitHub release URL and SHA-256 checksum.
- Kept the compact English streaming model as an optional fallback candidate and documented that the small Parakeet artifacts found so far are English-only.
- Separate downloaded-archive and prepared-for-dictation model states; verified sherpa `.tar.bz2` archives are marked prepared when `model.int8.onnx` and `tokens.txt` are present.
- Release signing template and release-gate checklist for reproducible signed APK builds without committing keystores.
- Action-based onboarding buttons for Accessibility settings, microphone permission, and model setup, with “already enabled/allowed/ready” labels when setup is complete.
- Status-screen overlay test field that lets users reliably focus an editable field and trigger the actual Accessibility floating button after enabling the service.
- Accessibility service readiness notification and broader text/window event handling so the floating button is easier to surface in modern input fields.
- Floating mic default placement now starts higher above the bottom edge, and dragged overlay position is saved locally for the next focused text field.
- Overlay dictation interaction now owns the microphone only during active hold-to-talk or tap-to-toggle dictation instead of starting capture immediately after microphone permission is granted.
