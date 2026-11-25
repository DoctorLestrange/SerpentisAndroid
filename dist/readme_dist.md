# Pre-Built APK Downloads

This directory holds local pre-built APK outputs (`dist/serpentis-v0.1.1.apk` / `dist/serpentis.apk`).

> **Note for GitHub Users**: Binary `.apk` files are not committed directly to Git history when syncing from AI Studio. Instead, **GitHub Actions** automatically builds, tests, and publishes versioned APKs:
> 1. **GitHub Releases** — Open the **Releases** tab on the repository page and download `serpentis-v0.1.1.apk` from the **`v0.1.1`** release (or run the **Release Serpentis APK** workflow in the **Actions** tab).
> 2. **GitHub Actions Artifacts** — Open the **Actions** tab → latest **Serpentis Android CI** or **Release Serpentis APK** run → download the APK artifact.
> 3. **Build Locally** — Run `gradle :app:assembleDebug` (requires **JDK 21+**) and copy `app/build/outputs/apk/debug/app-debug.apk` here.