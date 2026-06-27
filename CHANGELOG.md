# Changelog

All notable user-facing changes to QuietType will be documented in this file.

The format follows Keep a Changelog principles, and versions should follow semantic versioning where practical.

## Unreleased

### Changed

- Onboarding now blocks Continue/Finish until required setup tasks are complete: Accessibility service enabled, microphone permission allowed, and a dictation model prepared.
- Removed the noisy Status page; Settings now contains the floating-button test field and Accessibility settings shortcut, and a new About page links to the project.
- Removed unimplemented transcript-history and sensitive-field toggles from the visible settings UI. Sensitive-field hiding remains enabled by default.
- Removed unsupported benchmark-only models from the visible model catalog so every listed model is actionable.
- Marked live insertion as experimental and added preset floating-button color choices.
- Added a separate drag handle on the Accessibility floating control so repositioning does not trigger dictation.
- Added a hide-here control with warning confirmation; hidden apps/screens/fields can be removed again from Settings.
- The Settings screen now keeps only the correction-runtime toggle/summary, while the Models screen owns correction-model download, selection, and deletion flows.
- Whisper tiny/base downloadable catalog entries now use verified upstream SHA-256 checksums so downloads do not fail on checksum mismatch.

### Added

- Active-field quick correction from the floating overlay: cleans selected text, or the whole focused field when nothing is selected. A separate Settings section now lets users choose a future local correction model while built-in cleanup remains the current fallback.
- Compact model/language profiles on the Models screen for Danish/multilingual, compact multilingual, English low-latency, and Custom workflows; selecting a non-custom profile switches to and starts downloading its recommended local ASR model when needed.
- Separate correction-model management on the Models screen: choosing a downloadable Fix model now immediately starts its download, downloaded correction models can be deleted, and the first real downloadable candidate is SmolLM2 360M Instruct Q4_K_M.
- Immediate floating-overlay Fix feedback: pressing Fix now shows an instant toast and changes the overlay label to `✨ Fixing` while correction is running.
- Optional local-only dictation history with an off-by-default setting, History screen, copy/delete/clear controls, and final-transcript-only recording after successful insertion.
- Initial product, architecture, privacy, permissions, UX, and release planning documents.
- Android/Kotlin project bootstrap with Gradle wrapper, Compose Material 3 app shell, dynamic color theme, debug/release build configuration, and a unit test.
- Interactive Material 3 setup preview with onboarding, status, and settings sections for the privacy-first dictation flow.
- Local settings persistence for onboarding completion, dictation interaction preference, offline-only mode, and sensitive-field behavior.
- Accessibility service registration, focused editable-field detection, and a draggable safe microphone preview overlay, plus an in-app shortcut to Android Accessibility settings.
- Floating dictation button now consumes full tap/hold gestures, stays visible while dictation is active despite accessibility focus churn, switches to a red `● Listening` label while microphone capture is active, then returns to idle color with a `⏳ Thinking` label as soon as recording stop is requested while offline decoding finishes.
- Upgraded the vendored sherpa-onnx Android runtime to v1.13.3 so packaged arm64 native libraries are 16 KB page-size compatible on newer Android devices.
- Switched sherpa recognizer creation to file-mode loading for app-private absolute model paths, fixing a native crash when dictation starts from the floating button.
- Added runtime-file validation so missing or empty prepared model files are rejected before native ASR startup.
- Model catalog UI with persisted selected-model and prepare/delete marker state for local ASR model planning.
- Final transcript insertion that appends recognized on-device dictation text through accessibility `ACTION_SET_TEXT` when a local dictation session stops.
- HTTPS model artifact download plumbing with SHA-256 verification, private model-file storage, progress bar/percentage text, and deletion semantics before a model archive is marked downloaded.
- Switched the default catalog candidate to sherpa-onnx Parakeet TDT v3 int8 with Danish/multilingual support, a real GitHub release URL, and SHA-256 checksum.
- Kept Parakeet v2 int8, compact streaming English, compact CTC, and Whisper.cpp tiny/base GGML entries as fallback/custom candidates; Whisper entries are downloadable for runtime planning while whisper.cpp Android acceleration integration is still tracked before release.
- Separate downloaded-archive and prepared-for-dictation model states; verified sherpa `.tar.bz2` archives are unpacked into private runtime files and marked prepared when all runtime-required files are present.
- Release signing template and release-gate checklist for reproducible signed APK builds without committing keystores.
- Action-based onboarding buttons for Accessibility settings, microphone permission, and model setup, with “already enabled/allowed/ready” labels when setup is complete.
- Status-screen overlay test field that lets users reliably focus an editable field and trigger the actual Accessibility floating button after enabling the service.
- Accessibility service readiness notification and broader text/window event handling so the floating button is easier to surface in modern input fields.
- Floating mic default placement now starts higher above the bottom edge, and dragged overlay position is saved locally for the next focused text field.
- Overlay dictation interaction now owns the microphone only during active hold-to-talk or tap-to-toggle dictation instead of starting capture immediately after microphone permission is granted.
- Compact multilingual CTC fallback models now use the offline CTC sherpa recognizer path instead of the transducer/online path.
- Model downloads now reject benchmark/reference entries before checksum work starts, keep progress visible above the model-list scroll, and disable duplicate download/delete/select actions while a download is active.
- Setup/status/settings/models navigation now stays pinned at the top while each section scrolls.
- Placeholder/hint text from target fields is no longer prepended to inserted dictation text.
- Added a Settings toggle for live insertion. Streaming recognizers can insert stable words/phrases while recording and normalize all-caps ASR output; offline Parakeet remains final-on-stop until an offline chunking/partial adapter is implemented.
