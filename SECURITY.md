# Security Policy

## Supported Versions

| Version | Supported |
| :--- | :---: |
| `1.0.x` | :white_check_mark: |

## Security & Privacy Architecture

**Serpentis** is designed as a 100% offline, self-contained Android application:
- **Zero Network Surface**: The app does not request `android.permission.INTERNET` and performs no remote network requests or telemetry collection.
- **Least-Privilege Permissions**: Only `android.permission.VIBRATE` is declared in `AndroidManifest.xml` for optional haptic feedback.
- **Local Storage Hardening**:
  - `android:allowBackup="false"` is enforced in `AndroidManifest.xml`.
  - All deserialized game states (`GameRepository.restoreSavedGame`) validate grid bounds (`coerceIn(10, 48)`), segment counts, tick intervals, and enum values safely using `runCatching`.
  - Secrets are excluded from version control via `.gitignore` (`.env`, `*.jks`, `*.keystore`, `local.properties`).

## Reporting a Vulnerability

If you discover a security or data-integrity issue in **Serpentis**, please open a confidential security advisory on GitHub or report it via the repository issue tracker with steps to reproduce.
