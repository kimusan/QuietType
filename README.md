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
- Network access, if present, is only for explicit user-initiated model downloads or release/update metadata.
- Model files are downloaded only after user consent and verified before use.
- Users can delete downloaded models and local data from settings.

See `PRIVACY.md`, `docs/THREAT_MODEL.md`, and `docs/PERMISSIONS.md` for details.

## Installation

VoiceMe is not released yet. Planned release channels:

- Signed APKs attached to GitHub Releases.
- F-Droid-compatible flavor once the dependency/model packaging story is validated.

## Development status

The project has an initial Android/Kotlin/Compose bootstrap with a Material 3 shell. A local-only `ROADMAP.md` file may exist in developer checkouts and is intentionally ignored by git. Tracked planning and release documents live under `docs/`.

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
