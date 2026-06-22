# VoiceMe Privacy Policy

VoiceMe is designed to be privacy-first. This document describes the intended privacy behavior for the app as it is developed. It must be updated before every public release.

## Summary

- Voice dictation is processed on-device by default.
- Audio is not sent to cloud transcription services.
- Transcripts are not sent to the developer.
- No analytics or telemetry are enabled by default.
- Any network use must be visible, user-initiated, and documented here.

## Audio

VoiceMe uses the microphone only while the user actively starts dictation. Audio is streamed to the local ASR engine running on the device. The app should not persist raw audio unless a future feature explicitly asks for user consent and this policy is updated.

## Transcripts

Dictated text is inserted into the user's chosen input field. VoiceMe should not store transcript history by default. If transcript history is added later, it must be local-only, opt-in, and deletable from settings.

## Accessibility service

VoiceMe includes an AccessibilityService registration so users can enable the keyboard-adjacent dictation mode from Android settings. This capability is needed to detect focused editable fields and place a mic control next to the normal keyboard. In the current preview build, the service looks only at accessibility metadata needed to decide whether a focused node is editable or password/sensitive; it does not read field text. If an eligible editable field is focused, it shows a draggable microphone preview overlay. The preview overlay does not record audio, transcribe speech, or insert dictated text.

## Local preferences

VoiceMe stores a small set of local preferences, such as onboarding completion and preview settings for dictation interaction, offline-only mode, and sensitive-field behavior. These preferences stay in the app's private storage and Android backup is disabled for the app.

## Model files

ASR models may be downloaded after the user chooses a model. Downloads must use HTTPS and checksum verification. Users must be able to delete downloaded models. Model licenses and approximate sizes must be shown before download.

## Network

The offline/F-Droid flavor should avoid network access during dictation. If the app includes network permission, the exact reasons must be listed here and in `docs/PERMISSIONS.md`.

## Diagnostics

Crash reports, logs, analytics, or diagnostics must be off by default unless implemented in a privacy-preserving and explicitly disclosed way. Logs must avoid audio, transcript text, and focused-field content.

## Data deletion

Settings must provide controls to delete downloaded models, local preferences, and any optional history if such history exists.

## Third parties

Third-party dependencies and model providers must be documented in release notes and license/NOTICE files. Proprietary analytics or advertising SDKs are not part of the planned app.
