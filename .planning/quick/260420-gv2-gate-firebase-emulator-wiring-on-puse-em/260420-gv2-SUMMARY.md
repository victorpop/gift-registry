---
phase: 260420-gv2
plan: 01
subsystem: build-configuration
tags: [gradle, buildconfig, firebase-emulator, android, dx]
requires:
  - app/build.gradle.kts (buildFeatures.buildConfig = true)
  - com.giftregistry.BuildConfig (generated)
provides:
  - BuildConfig.USE_FIREBASE_EMULATOR (boolean, per-buildType)
  - -Puse_emulator Gradle project property
affects:
  - app/src/main/java/com/giftregistry/di/AppModule.kt (emulator gates)
tech-stack:
  added: []
  patterns:
    - "providers.gradleProperty(\"...\").getOrElse(default) — modern configuration-cache-safe Gradle property read"
    - "buildConfigField(\"boolean\", NAME, \"true\"|\"false\") — unquoted boolean literal, not a String"
key-files:
  modified:
    - app/build.gradle.kts
    - app/src/main/java/com/giftregistry/di/AppModule.kt
  created: []
decisions:
  - "No entry in gradle.properties — letting .getOrElse(\"true\") own the default keeps the dev loop unchanged for everyone pulling the repo"
  - "Release hardcodes USE_FIREBASE_EMULATOR = false (no property lookup) — safety gate against ever shipping a release pointed at the emulator"
  - "Boolean buildConfigField (not String) — keeps `if (BuildConfig.USE_FIREBASE_EMULATOR)` idiomatic in Kotlin"
metrics:
  duration: 4m
  completed: 2026-04-20
  tasks: 2
  files: 2
---

# Phase 260420-gv2 Plan 01: Gate Firebase emulator wiring on -Puse_emulator Summary

**One-liner:** Debug builds now read a Gradle project property (`use_emulator`) via a boolean `BuildConfig.USE_FIREBASE_EMULATOR`, letting on-device debug APKs target production Firebase with a single CLI flag while the default dev loop stays emulator-backed.

## What Changed

1. **`app/build.gradle.kts`** — added a `buildTypes { debug { ... } release { ... } }` block inside `android { ... }`:
   - **Debug variant:** reads `providers.gradleProperty("use_emulator").getOrElse("true")` and emits it as a boolean `BuildConfig.USE_FIREBASE_EMULATOR`. Unset → `true` (dev loop default); `-Puse_emulator=false` → `false` (on-device debug against prod).
   - **Release variant:** hardcodes `USE_FIREBASE_EMULATOR = false`. No property lookup — release builds can never be tricked into pointing at an emulator.

2. **`app/src/main/java/com/giftregistry/di/AppModule.kt`** — all three `if (BuildConfig.DEBUG)` emulator gates flipped to `if (BuildConfig.USE_FIREBASE_EMULATOR)`:
   - `provideFirebaseAuth()` → `auth.useEmulator("10.0.2.2", 9099)`
   - `provideFirebaseFirestore()` → `db.useEmulator("10.0.2.2", 8080)`
   - `provideFirebaseFunctions()` → `fns.useEmulator("10.0.2.2", 5001)` (on the `europe-west3` instance)
   - Hosts, ports, and region unchanged — only the gate condition moved.

## Usage Pattern

| Goal | Command |
|------|---------|
| Default dev loop (emulator-backed debug build) | `./gradlew :app:installDebug` |
| On-device debug against production Firebase | `./gradlew :app:installDebug -Puse_emulator=false` |
| Release build | `./gradlew :app:assembleRelease` — `-Puse_emulator` is ignored (always `false`) |

## Immediate Next Step (User Action)

Rebuild and reinstall on your phone with:

```bash
./gradlew :app:installDebug -Puse_emulator=false
```

No further production configuration is needed — the Firebase project, `google-services.json`, and App Check are already wired. The APK this produces will hit real Firebase Auth / Firestore / Functions (europe-west3) instead of `10.0.2.2`, so you will no longer see `Failed to connect to /10.0.2.2:9099` on a physical device.

## Why No Default in `gradle.properties`

Leaving the property unset — and relying on `.getOrElse("true")` in `app/build.gradle.kts` — is deliberate:

- Anyone pulling the repo fresh gets the emulator-backed debug build with zero configuration (dev loop unchanged).
- Opting out is explicit and per-invocation (`-Puse_emulator=false`), so there is no sticky local state that could silently make a teammate's debug build point at production.
- `gradle.properties` is checked in, so a default there would be the *team* default — keeping it unset scopes the decision to the command line where the engineer is already aware of the intent.

## Verified Invariants

- `./gradlew :app:assembleDebug` (no flag) → `BuildConfig.USE_FIREBASE_EMULATOR = true` (verified in generated `BuildConfig.java`).
- `./gradlew :app:assembleDebug -Puse_emulator=false` → `BuildConfig.USE_FIREBASE_EMULATOR = false` (verified in generated `BuildConfig.java`).
- `./gradlew :app:assembleRelease -Puse_emulator=true` → `BuildConfig.USE_FIREBASE_EMULATOR = false` (release safety gate holds even when flag is explicitly set to `true`).
- `app/src/main/java/com/giftregistry/di/AppModule.kt` contains **zero** `BuildConfig.DEBUG` references and **exactly three** `BuildConfig.USE_FIREBASE_EMULATOR` references.
- `app/src/debug/res/xml/network_security_config.xml` cleartext allowlist for `10.0.2.2` remains scoped to debug builds via source-set placement — no change needed.
- Hosts (`10.0.2.2`) and ports (`9099`, `8080`, `5001`) unchanged.

## Requirements Completed

- **GV2-01** — Debug builds opt out of the Firebase emulator via `-Puse_emulator=false`; default dev loop still wires the emulator; release builds always skip the emulator.

## Deviations from Plan

None — plan executed exactly as written.

## Commits

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Emit `BuildConfig.USE_FIREBASE_EMULATOR` from `app/build.gradle.kts` | `8523217` | `app/build.gradle.kts` |
| 2 | Migrate `AppModule.kt` emulator gates from `DEBUG` to `USE_FIREBASE_EMULATOR` | `994e8f2` | `app/src/main/java/com/giftregistry/di/AppModule.kt` |

## Reinstall Command (Copy/Paste)

```bash
./gradlew :app:installDebug -Puse_emulator=false
```

## Self-Check: PASSED

- SUMMARY.md exists at expected path
- Commit `8523217` (Task 1) present in `git log`
- Commit `994e8f2` (Task 2) present in `git log`
- All referenced source files exist
