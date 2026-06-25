# Security Policy

## Supported versions

QuietType has not shipped a public release yet. Security support policy will be updated before the first release.

## Reporting a vulnerability

Please report suspected security or privacy vulnerabilities privately to Kim Schulz <kim@schulz.dk>.

Include:

- Affected version or commit.
- Steps to reproduce.
- Impact, especially whether audio, transcripts, permissions, model files, or release artifacts are affected.
- Any suggested mitigation.

## Security-sensitive areas

- Microphone capture and foreground service behavior.
- Accessibility-service access to focused input fields.
- Transcript insertion and clipboard fallback.
- Model download, checksum verification, and model license handling.
- Logging and diagnostics.
- Release signing and artifact checksums.
