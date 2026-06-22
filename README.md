# VoiceMe

VoiceMe is an Android voice dictation app designed to stay out of the way until it is needed. When an editable field is active, VoiceMe provides a small Material You floating microphone control so users can dictate text into the current input without switching away from their normal workflow.

The long-term goal is private, near-real-time transcription that runs on-device using downloadable ASR models. The MVP will prioritize a reliable fully local streaming model stack, with Parakeet-class models evaluated as optional high-accuracy model packs once Android performance is proven.

## Product goals

- Stay hidden until an input field is active.
- Show a compact draggable microphone control near the keyboard area.
- Support hold-to-talk and toggle-to-dictate interaction modes.
- Write dictated text into the active input field with low latency.
- Run transcription locally on the device by default.
- Avoid telemetry and cloud transcription.
- Make permissions, privacy behavior, and model licenses transparent.

## Planned technology stack

- Android app written in Kotlin.
- Jetpack Compose with Material 3 / Material You dynamic color.
- Accessibility overlay mode for the primary “works with your existing keyboard” experience.
- Optional IME mode as a more reliable fallback for text insertion.
- Foreground microphone service built around `AudioRecord`.
- On-device ASR initially using sherpa-onnx with a compact streaming int8 model.
- Optional later engines/model packs: Parakeet ONNX exports, whisper.cpp, and/or Vosk fallback.

See `docs/ARCHITECTURE.md` for the current technical plan.

## Privacy posture

VoiceMe is planned as a privacy-first app:

- Audio and transcripts stay on-device during dictation.
- No analytics or telemetry by default.
- Network access is limited to explicit user-initiated model downloads or future release/update metadata.
- Model files are downloaded over HTTPS only after user action and are SHA-256 verified before being marked ready.
- Users can delete downloaded models and local data from settings.

See `PRIVACY.md`, `docs/THREAT_MODEL.md`, and `docs/PERMISSIONS.md` for details.

## Installation

VoiceMe is not released yet. Planned release channels:

- Signed APKs attached to GitHub Releases.
- F-Droid-compatible flavor once the dependency/model packaging story is validated.

## Development status

The project has an Android/Kotlin/Compose prototype with interactive Material 3 setup, status, settings, and local-model screens. Onboarding/settings/model choices are persisted locally. The app can request microphone permission and start a foreground `AudioRecord` shell with a visible notification, but no ASR model is connected yet. When the user enables the Accessibility service, VoiceMe detects focused editable fields, shows a draggable microphone overlay, and can insert a fixed ASR-stub phrase into the focused field for end-to-end insertion testing. The Models screen now starts explicit HTTPS model downloads, verifies SHA-256 before marking a model ready, and deletes private model files; current catalog checksums are placeholders until final model artifacts are locked. It is still not release-ready: streaming transcription and production text-insertion safeguards remain future work. A local-only `ROADMAP.md` file may exist in developer checkouts and is intentionally ignored by git. Tracked planning and release documents live under `docs/`.

## Build from source

Prerequisites:

- JDK 17 or newer.
- Android SDK with platform 37 installed.
- Android SDK Build Tools 36/37 installed.

Build and verify:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The current bootstrap also verifies a release APK can be assembled locally:

```bash
./gradlew assembleRelease
```

Release APKs are not signed for distribution yet; signing setup will be added before the first public release.

## License

VoiceMe is released under the MIT license.

## Credits

VoiceMe is designed and developed by Kim Schulz <kim@schulz.dk> with help from AI-assisted development.

More information: https://github.com/kimusan/VoiceMe
