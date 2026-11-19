# Changelog

All notable changes to **Serpentis** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-18

### Added
- **Complete Serpentis Arcade Release**:
  - **5 Game Modes**: *Classic Arena*, *Zen Flow*, *Labyrinth* (with 4 selectable sector layouts), *Portal Flux*, and *Time Blitz (120s)*.
  - **4 Speed Engine Tiers**: *Chill*, *Arcade*, *Hyper*, and *Overclocked* with optional dynamic speed scaling.
  - **Flux Magnet Power-Up (`FoodType.MAGNET_CORE`)**: Distinct horseshoe magnet orb (`🧲`) that activates a 4-tile Chebyshev attraction field for `55` ticks, pulling non-hazard items 1 tile closer toward the serpent's head each tick while strictly ignoring Poison Skulls.
  - **Togglable Poison Skull Hazard (`FoodType.POISON_SKULL`)**:
    - Distinct skull-and-crossbones hazard (`☠`) with a 45-tick countdown ring.
    - Enforces at most 1 Poison Skull on the board at a time.
    - Head-safe spawning (`manhattanDistance >= 5`, `chebyshevDistance >= 4`, and never in the 5 tiles directly ahead of the serpent's heading).
    - Relocates to a different tile after expiring and causes instant game over on contact (`DeathReason.POISON_CONSUMED`) unless `PHASE_GHOST` is active.
    - Enabled by default on *Hyper* and *Overclocked* tiers, and manually togglable on both the *Home* and *Settings* screens.
  - **Power-Up Suite**: *Cyber Apple*, *Golden Core*, *Flux Magnet*, *Chrono Shard*, *Phase Ghost*, *2x Overdrive*, and *Nano Shrink*.
  - **Full-Area Adaptive Play Grid**: Expands the play board to fill the entire rectangular space between the fixed-height HUD header and bottom controls with square cells.
  - **Precision Gesture & Control Engine**:
    - 2-step buffered turn queue preventing 180° double-input self-collisions.
    - L-swipe gesture filtering (max 2 perpendicular turns per continuous gesture with cross-axis drift decay).
    - Outer-margin swipe support across the entire play area outside the grid.
    - Virtual D-Pad, Split Turn Paddles, Pure Swipe mode, and hardware keyboard (`WASD` / `Arrows` / `Space`) support.
  - **Customization Workshop**: 6 Serpent skins, 4 Arena themes (including *Retro 1998 Nokia 3310 LCD*), and 4 base grid densities.
  - **Procedural 16-Bit PCM Audio & Haptics**: Real-time waveform synthesis via `AudioTrack` and tactile feedback via `Vibrator`.
  - **Offline Persistence**: Room SQLite database for leaderboards, career stats, 10 achievements, and automatic saved-run resume.
