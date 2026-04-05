---
phase: 02-android-core-auth
plan: 01
subsystem: infra
tags: [android, gradle, kotlin, compose, hilt, firebase, material3, navigation3, ksp, agp9]

# Dependency graph
requires:
  - phase: 01-firebase-foundation
    provides: Firebase project with Firestore, Auth, Functions emulator config on ports 9099/8080/5001; strings.xml and strings-ro.xml resource files
provides:
  - Compilable Android Gradle project (AGP 9.1.0 + Kotlin 2.3.20 + KSP 2.3.6)
  - Version catalog (gradle/libs.versions.toml) with all dependency versions
  - Hilt DI wired end-to-end (@HiltAndroidApp application, @AndroidEntryPoint activity)
  - Material3 theme with static seed color #6750A4 (light + dark schemes, no dynamic color)
  - Firebase Auth SDK initialized with emulator connection at 10.0.2.2:9099 in debug builds
  - Locale config declaring en and ro for per-app language support (I18N-01)
  - Placeholder google-services.json (user must replace with real file from Firebase Console)
affects: [02-02, 02-03, 02-04, all subsequent Android plans]

# Tech tracking
tech-stack:
  added:
    - Android Gradle Plugin 9.1.0 (built-in Kotlin support, no kotlin-android plugin)
    - Kotlin 2.3.20
    - KSP 2.3.6 (replaces KAPT, required for AGP 9 + Hilt)
    - Gradle wrapper 9.3.1
    - Compose BOM 2026.03.00
    - Firebase BoM 34.11.0 (main modules, no KTX suffix)
    - Hilt 2.56.2 with KSP annotation processing
    - Navigation3 1.0.1
    - Material3 (via Compose BOM)
    - Credential Manager 1.6.0-rc02 + googleid 1.2.0 (for Google Sign-In)
    - DataStore Preferences 1.2.1
    - AppCompat 1.7.0 (for AppCompatDelegate locale switching)
  patterns:
    - KSP over KAPT for Hilt annotation processing (AGP 9 requirement)
    - No org.jetbrains.kotlin.android plugin (AGP 9 built-in)
    - Firebase main modules directly (no -ktx suffix, BoM v34+)
    - AppCompatActivity (not ComponentActivity) for AppCompatDelegate locale support
    - Static seed color scheme (no dynamic color) per UI-SPEC
    - Firebase Auth emulator at 10.0.2.2:9099 in debug builds (Android emulator localhost alias)

key-files:
  created:
    - gradle/libs.versions.toml (version catalog for all dependencies)
    - gradle/wrapper/gradle-wrapper.properties (Gradle 9.3.1)
    - settings.gradle.kts (root project with plugin management)
    - build.gradle.kts (root — all plugins applied false)
    - app/build.gradle.kts (app module with AGP 9, KSP, Hilt, Compose, Firebase)
    - app/proguard-rules.pro (placeholder)
    - app/google-services.json (placeholder — user must replace)
    - gradlew / gradlew.bat (Gradle wrapper scripts)
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/giftregistry/GiftRegistryApp.kt
    - app/src/main/java/com/giftregistry/MainActivity.kt
    - app/src/main/java/com/giftregistry/ui/theme/Color.kt
    - app/src/main/java/com/giftregistry/ui/theme/Type.kt
    - app/src/main/java/com/giftregistry/ui/theme/Theme.kt
    - app/src/main/java/com/giftregistry/di/AppModule.kt
    - app/src/main/java/com/giftregistry/di/DataModule.kt
    - app/src/main/res/xml/locales_config.xml
  modified: []

key-decisions:
  - "KSP over KAPT for Hilt — AGP 9 built-in Kotlin makes KAPT incompatible; KSP is the only viable annotation processor path"
  - "AppCompatActivity (not ComponentActivity) — required for AppCompatDelegate.setApplicationLocales() for locale switching (I18N-UX-03)"
  - "Static seed color #6750A4 — no dynamic color per UI-SPEC; ensures consistent brand identity across all Android versions"
  - "Firebase Auth emulator at 10.0.2.2:9099 — Android emulator alias for host machine localhost; established in Phase 1 firebase.json"
  - "No org.jetbrains.kotlin.android plugin — AGP 9.1.0 has Kotlin built in; adding it causes duplicate plugin errors"

patterns-established:
  - "Version catalog pattern: all dependency versions in gradle/libs.versions.toml; app/build.gradle.kts uses alias(libs.*) references"
  - "Hilt DI pattern: SingletonComponent for app-scoped dependencies; @HiltAndroidApp on Application; @AndroidEntryPoint on Activity"
  - "Firebase emulator pattern: BuildConfig.DEBUG guard in DI provider; 10.0.2.2 for Android emulator to host machine"
  - "Material3 theme pattern: static lightColorScheme/darkColorScheme from Color.kt; isSystemInDarkTheme() toggle in Theme.kt"

requirements-completed: [AUTH-04, I18N-01]

# Metrics
duration: 3min
completed: 2026-04-05
---

# Phase 2 Plan 01: Android Gradle Project Scaffold Summary

**AGP 9.1.0 + Kotlin 2.3.20 project bootstrap with KSP-based Hilt DI, Compose Material3 theme, Firebase Auth emulator wiring, and en/ro locale config**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-05T06:38:28Z
- **Completed:** 2026-04-05T06:41:47Z
- **Tasks:** 2 of 3 complete (Task 3 is a human-action gate — see below)
- **Files created:** 17

## Accomplishments
- Created complete Android Gradle project from zero: version catalog, build files, Gradle wrapper 9.3.1
- Wired Hilt DI end-to-end with KSP annotation processing (no KAPT — required for AGP 9)
- Material3 theme with static #6750A4 seed color, light/dark color schemes, and 4 typography roles
- Firebase Auth SDK initialized with emulator connection guarded by BuildConfig.DEBUG
- Locale config declares en and ro for per-app language support (I18N-01 requirement)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Gradle project scaffold with version catalog, build files, and wrapper** - `6053c02` (feat)
2. **Task 2: Create Application class, MainActivity, Material3 theme, DI modules, locale config, and AndroidManifest** - `af8c4de` (feat)
3. **Task 3: Register Android app in Firebase and place google-services.json** - AWAITING HUMAN ACTION (checkpoint)

## Files Created/Modified
- `gradle/libs.versions.toml` - Version catalog: AGP 9.1.0, Kotlin 2.3.20, KSP 2.3.6, Compose BOM 2026.03.00, Firebase BoM 34.11.0, Hilt 2.56.2, Navigation3 1.0.1
- `settings.gradle.kts` - Root project with plugin management and repository configuration
- `build.gradle.kts` - Root build file with all plugins applied false
- `app/build.gradle.kts` - App module: AGP 9, KSP, Hilt, Compose, Firebase, Credential Manager dependencies
- `gradlew` / `gradlew.bat` - Gradle wrapper scripts for Gradle 9.3.1
- `gradle/wrapper/gradle-wrapper.properties` - Gradle distribution URL
- `gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper JAR
- `app/google-services.json` - Placeholder (user must replace with real file from Firebase Console)
- `app/proguard-rules.pro` - Placeholder for project-specific ProGuard rules
- `app/src/main/AndroidManifest.xml` - App manifest with INTERNET permission, AppCompat theme, localeConfig
- `app/src/main/java/com/giftregistry/GiftRegistryApp.kt` - @HiltAndroidApp Application class
- `app/src/main/java/com/giftregistry/MainActivity.kt` - @AndroidEntryPoint AppCompatActivity with Compose setContent
- `app/src/main/java/com/giftregistry/ui/theme/Color.kt` - Light/dark Material3 color schemes with seed #6750A4
- `app/src/main/java/com/giftregistry/ui/theme/Type.kt` - Typography with 4 roles (headlineLarge, titleLarge, bodyLarge, labelLarge)
- `app/src/main/java/com/giftregistry/ui/theme/Theme.kt` - GiftRegistryTheme composable (static, no dynamic color)
- `app/src/main/java/com/giftregistry/di/AppModule.kt` - Hilt module providing FirebaseAuth singleton with emulator config
- `app/src/main/java/com/giftregistry/di/DataModule.kt` - Placeholder Hilt module for Plan 02 repository bindings
- `app/src/main/res/xml/locales_config.xml` - Declares en and ro locales for per-app language support

## Decisions Made
- KSP over KAPT: AGP 9 built-in Kotlin makes org.jetbrains.kotlin.kapt incompatible; KSP 2.3.6 is the only viable path for Hilt annotation processing
- AppCompatActivity (not ComponentActivity): Required for AppCompatDelegate.setApplicationLocales() locale switching (I18N-UX-03 constraint)
- Static color scheme: No dynamic color per UI-SPEC; ensures consistent brand identity across all Android versions including pre-Android 12
- No kotlin-android plugin: AGP 9.1.0 has Kotlin built in; the separate plugin would cause duplicate plugin application error

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- gradle binary not on PATH — downloaded gradle-wrapper.jar from GitHub (v9.3.1 tag) and wrote standard gradlew/gradlew.bat scripts manually. Gradle wrapper JAR is 46KB and present at gradle/wrapper/gradle-wrapper.jar.

## Known Stubs
- `app/google-services.json` — Placeholder with package_name=com.giftregistry but fake project_number/mobilesdk_app_id. Runtime Firebase calls will fail until replaced with real file from Firebase Console (Task 3 human-action checkpoint).
- `app/src/main/java/com/giftregistry/MainActivity.kt` — Hardcoded Text("Gift Registry") placeholder. Will be replaced with Navigation3 NavHost in Plan 03.
- `app/src/main/java/com/giftregistry/di/DataModule.kt` — Empty module placeholder. Repository bindings added in Plan 02.

## User Setup Required

Task 3 is a blocking human-action checkpoint. The user must:

1. Go to Firebase Console -> Project Settings -> General -> Your apps
2. Click "Add app" -> Android, enter package name `com.giftregistry`
3. (Optional) Add debug SHA-1 fingerprint for Google Sign-In on emulator
4. Download `google-services.json` and place it at `app/google-services.json` (overwriting placeholder)
5. Enable Google Sign-In: Firebase Console -> Authentication -> Sign-in method -> Google -> Enable
6. Verify: `cd /Users/victorpop/ai-projects/gift-registry && ./gradlew assembleDebug 2>&1 | tail -5`

## Next Phase Readiness
- Project scaffold is complete: Plans 02, 03, 04 can implement domain layers, auth screens, and navigation on this foundation
- Placeholder google-services.json allows structural development but runtime Firebase calls require real credentials
- All dependency versions locked in version catalog; no version drift possible between plans
