# VoiceMe

VoiceMe is an Android voice dictation app designed to stay out of the way until it is needed. When an editable field is active, VoiceMe provides a small Material You floating microphone control so users can dictate text into the current input without switching away from their normal workflow.

The long-term goal is private, near-real-time transcription that runs on-device using downloadable ASR models. The current default candidate is the sherpa-onnx Parakeet TDT v3 int8 multilingual pack, which includes Danish support; it is large, so compact fallback models remain available for benchmarking.

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
- On-device ASR using sherpa-onnx v1.13.3 with 16 KB-aligned arm64 native libraries, the Parakeet TDT v3 int8 multilingual pack as the default candidate, plus compact fallback models and runtime-readiness gates before any archive is treated as dictation-ready.
- Optional later engines/model packs: fp32 Parakeet benchmark packs on capable devices, whisper.cpp, and/or Vosk fallback.

See `docs/ARCHITECTURE.md` for the current technical plan.

## Privacy posture

VoiceMe is planned as a privacy-first app:

- Audio and transcripts stay on-device during dictation.
- No analytics or telemetry by default.
- Network access is limited to explicit user-initiated model downloads or future release/update metadata.
- Model files are downloaded over HTTPS only after user action and are SHA-256 verified before being stored as downloaded archives.
- A downloaded archive is not considered dictation-ready until a runtime preparation step verifies the files needed by the ASR engine.
- Users can delete downloaded models and local data from settings.

See `PRIVACY.md`, `docs/THREAT_MODEL.md`, and `docs/PERMISSIONS.md` for details.

## Installation

VoiceMe is not released yet. Planned release channels:

- Signed APKs attached to GitHub Releases.
- F-Droid-compatible flavor once the dependency/model packaging story is validated.

## Development status

The project has an Android/Kotlin/Compose prototype with interactive Material 3 setup, status, settings, and local-model screens. Onboarding/settings/model choices are persisted locally. The app can download and verify sherpa-onnx model archives with a Material progress bar and percentage text, unpack prepared runtime files, start a foreground local `AudioRecord` + sherpa-onnx dictation session from the floating overlay, and broadcast final transcripts back to the Accessibility service for focused-field insertion. The Models screen starts explicit HTTPS model downloads, verifies SHA-256 before storing a downloaded/prepared marker, and deletes private model files. The default Parakeet TDT v3 int8 archive is locked to a real GitHub release URL and SHA-256 checksum. It is still not public-release-ready: streaming partial insertion and production text-insertion safeguards remain future work. A local-only `ROADMAP.md` file may exist in developer checkouts and is intentionally ignored by git. Tracked planning and release documents live under `docs/`.

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

Release signing can be configured with an ignored root-level `keystore.properties` copied from `keystore.properties.example`. Without that file, `assembleRelease` builds an unsigned artifact for local verification. See `docs/RELEASE.md` for the release gate, signing setup, and artifact checklist.

## License

VoiceMe is released under the MIT license.

## Credits

VoiceMe is designed and developed by Kim Schulz <kim@schulz.dk> with help from AI-assisted development.

More information: https://github.com/kimusan/VoiceMe
