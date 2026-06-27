# QuietType Threat Model

## Assets

- User speech audio.
- Dictated transcript text.
- Focused input-field content visible through accessibility APIs.
- Downloaded model files.
- User preferences, including accessibility/IME configuration.
- Release signing keys and build artifacts.

## Privacy and security goals

- Keep speech recognition local by default.
- Avoid collecting audio, transcripts, or analytics.
- Make powerful permissions understandable and narrowly used.
- Verify downloaded model files before use.
- Provide user controls to delete local data.
- Produce reproducible, auditable release artifacts where practical.

## In scope

- Accidental network transmission.
- Overbroad logging of transcripts or focused-field content.
- Malicious or corrupted model downloads.
- Accessibility-service misuse or excessive data access.
- Clipboard leakage if clipboard fallback is enabled.
- Build/release artifact tampering.

## Out of scope

- Compromised/rooted operating systems.
- Malicious keyboards or apps outside QuietType.
- Hardware microphone compromise.
- Users intentionally pasting dictated text into third-party cloud apps.

## Mitigations

- Offline-first architecture with no telemetry.
- Explicit user consent for model downloads.
- HTTPS plus checksum verification for model files.
- No raw-audio persistence by default.
- No transcript history by default; opt-in history stores final text only in private app storage and provides per-entry/all-entry deletion.
- Accessibility service activates only around editable fields and user-triggered dictation.
- Sensitive-field blocking.
- Clipboard fallback disabled by default and clearly labeled if added.
- Signed releases with published checksums.
