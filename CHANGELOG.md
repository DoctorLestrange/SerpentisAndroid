# Changelog

All notable changes to **Serpentis** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2026-11-25

### Added
- **Multi-Theme App Interface Studio**:
  - **Dark Graphite (New Default)**: Deep matte charcoal gray theme (`#121316` / `#1A1C20` / `#22252B`) with layered surface hierarchy.
  - **Midnight Cyber**: Classic deep obsidian blue-navy arcade theme preserved as a selectable option.
  - **Soft Light (Warm Off-White)**: Eye-friendly warm alabaster off-white theme (`#F2EFE9` / `#F7F5F0` / `#E9E5DC`) designed without harsh pure-white glare, complete with automatic light status/navigation bar icons.
  - **Match Device (System Light/Dark)**: Follows your Android system theme mode automatically.
- **Follow Device Theme Colors (Material You)**:
  - Toggleable Android 12+ Material You dynamic color integration (`dynamicDarkColorScheme` / `dynamicLightColorScheme`) synced to your wallpaper, with automatic graceful fallback on older Android versions.
- **Customizable Accent Color Palettes**:
  - Choose from 6 calibrated dual-tone accent palettes (*Emerald & Cyan*, *Cobalt & Teal*, *Amber & Flame*, *Crimson & Rose*, *Violet & Orchid*, and *Monochrome Ink*), each fine-tuned separately for dark and soft-light contrast.
- **Minimalist Light & Dark Play Arena Themes + Matching Serpent Skins**:
  - **Soft Paper Play Theme**: Warm off-white playing area (`#F2EFE9`) auto-paired with the new **Charcoal Ink** black/slate-grey serpent, plus light-board calibrated food, skull, magnet, and countdown-ring contrast.
  - **Graphite Matte Play Theme**: Dark charcoal grey playing area (`#24272C`) auto-paired with the new **Soft Ivory** warm off-white serpent.
- **Automated Versioned GitHub Releases**:
  - Dedicated `Release Serpentis APK` workflow (`.github/workflows/release.yml`) automatically reads `versionName` (`0.1.1`) from `app/build.gradle.kts`, runs tests, packages `serpentis-v0.1.1.apk`, extracts changelog notes, and publishes the **`v0.1.1`** GitHub Release.

### Fixed
- Prevented `"Overclocked"` speed tier chip text from breaking mid-word on narrow portrait screens.
- Added high-contrast light-theme calibration for Home hero banner text, Arena active buff chips, and Workshop color swatch borders.

## [0.1.0] - 2026-11-18

### Added
- **Initial Serpentis Arcade Release**:
  - **5 Game Modes**: *Classic Arena*, *Zen Flow*, *Labyrinth* (with 4 selectable sector layouts), *Portal Flux*, and *Time Blitz (120s)*.
  - **4 Speed Engine Tiers**: *Chill*, *Arcade*, *Hyper*, and *Overclocked* with optional dynamic speed scaling.
  - **Flux Magnet Power-Up (`FoodType.MAGNET_CORE`)**: Distinct horseshoe magnet orb (`🧲`) that activates a 4-tile Chebyshev attraction field for `55` ticks, pulling non-hazard items 1 tile closer toward the serpent's head each tick while strictly ignoring Poison Skulls.
  - **Togglable Poison Skull Hazard (`FoodType.POISON_SKULL`)**: Single-instance head-safe skull hazard (`☠`) with timed relocation, instant game over on contact (unless `PHASE_GHOST` is active), and default-on in *Hyper* & *Overclocked* tiers.
  - **Full-Area Adaptive Play Grid**, **2-Step Buffered Turn Queue**, **L-Swipe Protection**, **Outer-Margin Swipe Controls**, **16-Bit PCM Procedural Audio**, **Haptics**, and **Room SQLite Persistence**.
