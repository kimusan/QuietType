# VoiceMe Build and Release Plan

## Versioning

Use semantic versions for `versionName` and a monotonically increasing integer `versionCode`. Suggest a version bump when a user-visible feature set is complete, privacy behavior changes, ASR model compatibility changes, or release artifacts are ready.

Current development version: `0.1.0-dev` / `versionCode = 1`.

Do not cut the first public release until the release gate below is complete.

## Current release gate

VoiceMe can build debug and release APK artifacts locally, but it is not ready for a public release yet.

Required before first public binary release:

- Real streaming ASR runtime connected to the foreground microphone pipeline.
- Model archive extraction/preparation implemented and tested.
- At least one downloaded model reaches `PreparedForDictation` only after runtime-required files are present and loadable.
- End-to-end dictation test on a physical Android device: mic -> ASR partial/final transcript -> focused-field insertion.
- Manual accessibility-insertion matrix across common target apps/editors.
- Third-party license inventory/NOTICE for Android dependencies, sherpa-onnx runtime, and downloadable model artifacts.
- Signed release artifact built from a clean git tag.

## Release channels

- GitHub Releases: signed APK, checksums, changelog, source tag.
- F-Droid: FOSS-compatible flavor once dependencies and model delivery are acceptable.
- Play Store: optional later; may require an IME-focused or carefully justified accessibility implementation.

## Required release documents

Keep these tracked documents current:

- `README.md` - overview, build/install instructions, screenshots when available.
- `PRIVACY.md` - data flow and privacy behavior.
- `docs/PERMISSIONS.md` - permission table and justification.
- `docs/THREAT_MODEL.md` - assets, threats, mitigations.
- `docs/ARCHITECTURE.md` - current technical design.
- `docs/UX_ONBOARDING.md` - onboarding, usage, settings UX.
- `CHANGELOG.md` - user-facing release changes.
- `SECURITY.md` - vulnerability reporting and supported versions.
- `LICENSE` - project license.
- `NOTICE` or third-party license inventory before first binary release.
- `fastlane/metadata/android/...` before F-Droid/Play submission.

## Local verification

Run from the repository root:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

If a device is attached:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then manually verify:

1. Onboarding explains microphone, accessibility, offline model download, and privacy behavior, and its action buttons open the relevant system/app flows.
2. Microphone permission prompt appears only after user action.
3. Foreground microphone notification appears while recording.
4. After enabling VoiceMe in Android Accessibility settings, tapping the Status screen overlay test field shows the actual draggable floating mic; password/sensitive fields remain blocked.
5. Password/sensitive fields are blocked by default.
6. Model download is explicit, HTTPS-only, SHA-256 verified, and stored under app-private storage.
7. Downloaded archive state is not presented as ASR-ready until runtime preparation succeeds.

## Signing

Release signing is intentionally secret-free in git. The Gradle build reads an ignored root-level `keystore.properties` file when present. If it is absent, `assembleRelease` still builds an unsigned release APK for local verification.

Create a local release keystore outside git or in an ignored path:

```bash
keytool -genkeypair \
  -v \
  -keystore release/voiceme-release.jks \
  -alias voiceme \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Create `keystore.properties` in the repository root:

```properties
storeFile=release/voiceme-release.jks
storePassword=REPLACE_WITH_SECRET
keyAlias=voiceme
keyPassword=REPLACE_WITH_SECRET
```

Never commit `keystore.properties`, `.jks`, `.keystore`, `.apk`, `.aab`, `.apks`, or `.idsig` files.

Build and inspect a signed release:

```bash
./gradlew clean assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

Publish the APK SHA-256 checksum and signing certificate SHA-256 fingerprint with every GitHub Release.

## Tagging and artifact checklist

Before tagging:

1. Confirm `git status --short` is clean.
2. Update `CHANGELOG.md` from `Unreleased` to the target version/date.
3. Bump `versionName` and `versionCode` in `app/build.gradle.kts`.
4. Run full local verification.
5. Install on a physical device and run the manual release gate.
6. Build signed release APK from a clean checkout.
7. Generate SHA-256 checksums.
8. Create an annotated tag: `git tag -a vX.Y.Z -m "VoiceMe X.Y.Z"`.
9. Push branch and tag.
10. Create a GitHub Release with APK, checksum, changelog, known limitations, and privacy/model notes.

## CI goals

When CI is added, it should run:

- Gradle wrapper validation.
- Kotlin formatting/lint.
- Unit tests.
- Android lint.
- Debug APK build.
- Release APK build for release branches/tags.
- Dependency/license report.
- SBOM generation if practical.
- Checksum generation for release artifacts.

## F-Droid readiness

- Use only FOSS dependencies in the F-Droid flavor.
- Avoid proprietary SDKs and analytics.
- Ensure model licenses are documented.
- Avoid opaque binary downloads; if model downloads are required, make them explicit and policy-compliant.
- Provide Fastlane metadata and changelog snippets.
