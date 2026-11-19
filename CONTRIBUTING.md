# Contributing to Serpentis

Thank you for your interest in contributing to **Serpentis**! This document outlines the workflow, architecture conventions, and verification steps for submitting improvements.

---

## Development Setup

1. **Clone the repository**:
   ```bash
   git clone <your-repo-url>
   cd Serpentis
   ```
2. **Open in Android Studio** (Ladybug / Meerkat or newer) or build from the command line with **JDK 21+**.

---

## Architecture & Coding Standards

- **Kotlin & Jetpack Compose First**: All UI is written in declarative Jetpack Compose using Material Design 3 (`SerpentisTheme`). Do not add legacy XML layouts.
- **Deterministic Game Engine**: Core game rules, collision detection, Flux Magnet attraction, and Poison Skull spawning live in `com.example.engine.SnakeGameEngine`. Keep engine logic pure and deterministic so it can be unit-tested with a seeded `Random`.
- **Fixed-Height HUD Invariant**: Never add dynamic-height elements outside `ArenaHudHeader` in `ArenaScreen.kt` that could shift or resize the play grid when buffs or timers activate.
- **Test Tags & Accessibility**: Every interactive control should include a descriptive `Modifier.testTag("snake_case_tag")`, a minimum touch target of `48.dp`, and meaningful content descriptions.

---

## Verification & Testing

Before opening a Pull Request, verify that all unit tests, Robolectric UI tests, and APK assembly tasks pass:

```bash
# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Assemble the debug APK
gradle :app:assembleDebug
```

---

## Pull Request Checklist

- [ ] Code compiles cleanly with zero warnings or errors.
- [ ] New game mechanics or bug fixes include corresponding unit tests in `ExampleUnitTest.kt` or UI tests in `ExampleRobolectricTest.kt`.
- [ ] All tests pass via `gradle :app:testDebugUnitTest`.
- [ ] Documentation (`README.md`, `CHANGELOG.md`, and in-app Tactical Manual if applicable) is updated to reflect user-visible changes.
