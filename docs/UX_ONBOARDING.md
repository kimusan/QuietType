# QuietType UX, Onboarding, and Configuration Plan

## Design principles

- Stay out of the way until text entry is active.
- Make privacy state visible, not buried in legal copy.
- Use Material 3 components, dynamic color, dark mode, large touch targets, and adaptive layouts.
- Ask for permissions just in time and explain each one before the Android system dialog/settings screen.
- Prefer safe defaults: offline, no telemetry, no transcript history unless the user opts in, no automatic clipboard fallback.

## Onboarding flow

1. Welcome
   - Explain: “Private voice dictation for any text field.”
   - Show a short animation or illustration of the mic button beside the keyboard.
2. Privacy promise
   - Audio stays on-device.
   - No telemetry.
   - User controls model downloads and local data.
3. Choose mode
   - Recommended: floating button with normal keyboard, requiring Accessibility.
   - Fallback: QuietType keyboard/IME, requiring keyboard enablement.
4. Microphone setup
   - Explain why mic access is needed.
   - Request permission only when the user continues.
5. Model setup
   - Show recommended compact offline model.
   - Show size, language, license, accuracy/latency expectation, and checksum.
   - Download only after explicit confirmation.
6. Guided test dictation
   - Let the user record a short phrase.
   - Show partial/final transcript behavior.
   - Offer “Try in another app” instructions.
7. Finish
   - Show how to move the button, hold/toggle dictation, and open settings.

## Daily usage

- User focuses an editable text field.
- QuietType shows a small floating mic button near the keyboard area.
- User holds the button to dictate, or taps to toggle if configured.
- Button changes state using Material motion/color and haptic feedback.
- Partial text appears where reliable; final text is committed after pause/endpoint.
- User can drag the button; position is remembered.

## Configuration after onboarding

- Dictation interaction: hold-to-talk or toggle.
- Button size, color/accent, opacity, and saved position.
- Language/model selection and model deletion.
- Offline-only mode.
- Auto punctuation/capitalization.
- Sensitive-field behavior.
- Clipboard fallback, disabled by default.
- Data deletion controls.
- Troubleshooting: permissions, battery optimization, unsupported apps, model download issues.

## Material You details

- Use Material 3 theme and dynamic color on Android 12+.
- Use edge-to-edge layouts and proper window insets.
- Use top app bars, cards, switches, list items, snackbars, and dialogs from Material 3.
- Use adaptive navigation: bottom navigation for compact screens, navigation rail for larger screens.
- Respect font scaling, contrast, reduced motion, and TalkBack.


## History screen

- History is off by default and can be enabled from Settings or the empty History screen.
- Saved entries are local-only successful final dictations. Live partial text and raw audio are not saved.
- Users can copy an entry for pasting/reinsertion, delete one entry, or clear all history.


## Model/language profiles

- The Models screen starts with friendly profiles instead of requiring users to understand ASR runtimes first.
- Danish + multilingual is the default profile and selects the Parakeet v3 multilingual model.
- Compact multilingual and English low-latency profiles switch to smaller/faster candidate models and start their recommended model download when needed.


## Quick correction mode

- The floating overlay exposes a compact Fix action beside the microphone.
- Correction targets the active input field, not saved history: selected text is cleaned when a selection exists; otherwise QuietType cleans the whole focused field.
- The first implementation keeps deterministic cleanup as a safety net, but QuietType can now attempt local SmolLM2 GGUF correction when the user enables a downloaded correction model. If model output is empty or low-quality, Fix falls back to built-in cleanup instead of writing garbage into the field.
- Sensitive/password-like fields remain blocked from both dictation insertion and correction.
- Fix should remain one tap from the overlay. If model correction fails, the UI should say it fell back to built-in cleanup.
