# Pre-Built APK Downloads

This directory holds local pre-built APK outputs (`dist/serpentis.apk`).

> **Note for GitHub Users**: Binary `.apk` files are not committed directly to Git history when syncing from AI Studio. Instead, **GitHub Actions** automatically builds, tests, and publishes `serpentis.apk` on every push:
> 1. **GitHub Releases** — Open the **Releases** tab on the repository page and download `serpentis.apk` from the `latest` release.
> 2. **GitHub Actions Artifacts** — Open the **Actions** tab → latest **Serpentis Android CI** run → download `serpentis-debug-apk`.
> 3. **Build Locally** — Run `gradle :app:assembleDebug` (requires **JDK 21+**) and copy `app/build/outputs/apk/debug/app-debug.apk` here.