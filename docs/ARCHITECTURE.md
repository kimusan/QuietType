# QuietType Architecture Plan

## Goal

Build a privacy-first Android dictation app that appears only when useful, captures speech locally, transcribes it near real time, and inserts text into the active input field.

## Recommended architecture

QuietType should use a shared core dictation pipeline with two Android integration modes.

### 1. Primary mode: accessibility overlay

This is the only practical way to keep the user's normal keyboard active while displaying a QuietType microphone control beside it.

Components:

- `QuietTypeAccessibilityService`
  - Watches focus/window events.
  - Tracks the currently focused editable node.
  - Shows the floating microphone button when text entry is active.
  - Hides the button outside text-entry contexts.
- Accessibility overlay
  - Uses `TYPE_ACCESSIBILITY_OVERLAY`.
  - Non-focusable and draggable.
  - Supports hold-to-talk and toggle modes.
  - Saves user position/size/color preferences.
- `TextInjector`
  - Uses accessibility actions to update editable fields.
  - Starts with final-segment insertion for reliability.
  - Later supports partial replacement with cursor preservation where possible.
  - Blocks password/sensitive fields by default.

Tradeoffs:

- Best match for “show up along with the normal keyboard.”
- Requires a clearly explained Accessibility permission.
- Text insertion is less reliable than an IME in custom editors, webviews, and some chat apps.
- Play Store policy risk must be handled by honest disclosure and possibly an IME-only Play flavor.

### 2. Fallback mode: QuietType IME

An `InputMethodService` cannot appear alongside another keyboard, but it provides the most reliable text insertion API.

Components:

- Minimal QuietType keyboard/panel.
- Large mic button and basic text controls.
- `InputConnection.setComposingText()` for partials.
- `InputConnection.commitText()` for final text.

Tradeoffs:

- More reliable than accessibility insertion.
- Does not need Accessibility permission.
- User must switch keyboards.
- Useful for Play Store distribution and apps where accessibility injection fails.

## Core dictation pipeline

Shared components for both modes:

- `ModelManager`
  - Lists available model packs.
  - Downloads/imports user-selected models.
  - Verifies checksums/signatures before storing downloaded archive state.
  - Extracts/prepares runtime files and only then marks a model `PreparedForDictation`.
  - Deletes models on request.
- `DictationForegroundService`
  - Requests microphone capture through `AudioRecord`.
  - Runs only while actively listening.
  - Displays a visible microphone notification when required.
  - Streams PCM chunks to the recognizer.
- `AsrEngine` interface
  - Emits partial and final transcript events.
  - Supports cancellation and endpoint detection.
  - Isolates UI/insertion logic from model runtime specifics.
- `PrivacyState`
  - Makes offline/network/data-retention mode explicit in UI.

## ASR stack recommendation

### MVP

Use sherpa-onnx with a compact int8 streaming model.

Reasons:

- Android/Kotlin examples exist.
- Supports true streaming ASR and partial results.
- Apache-2.0 runtime.
- Better fit for low-latency dictation than chunked non-streaming models.
- Runs fully on-device.

### Later model options

- Parakeet ONNX/sherpa-onnx model packs for high accuracy on capable devices.
  - Treat as optional because Parakeet 0.6B-class exports are large and memory-heavy.
- whisper.cpp for broad offline model availability and multilingual batch/alternate mode.
- Vosk as a small low-end-device fallback if accuracy is acceptable.

## UI stack

- Kotlin.
- Jetpack Compose.
- Material 3 / Material You.
- Dynamic color on Android 12+.
- Edge-to-edge layout with proper insets.
- Predictive back through modern AndroidX APIs.
- Adaptive layouts for phones, tablets, foldables, and landscape.

## Initial modules

A practical first implementation can start as a single Android app module, then split into modules once the architecture stabilizes:

- `:app` - activities, onboarding, settings, Android services.
- `:core-dictation` - ASR interfaces, transcript session state, insertion abstractions.
- `:engine-sherpa` - sherpa-onnx runtime integration.
- `:model-catalog` - model metadata, checksums, download policy.

## Validation targets

- Unit tests for transcript session state and partial/final replacement rules.
- Unit tests for model catalog parsing and checksum validation.
- Instrumentation tests for onboarding/settings navigation.
- Manual matrix for insertion reliability across common apps and editors.
- Device benchmarks for latency, memory, battery, and thermal behavior.
