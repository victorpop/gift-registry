---
phase: 260510-sai-add-gradle-property-pemulatorhost-for-co
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/build.gradle.kts
  - app/src/main/java/com/giftregistry/di/AppModule.kt
  - app/src/main/java/com/giftregistry/di/StorageModule.kt
autonomous: false
requirements:
  - QUICK-260510-SAI

must_haves:
  truths:
    - "Default `./gradlew :app:assembleDebug` produces a BuildConfig where FIREBASE_EMULATOR_HOST equals \"10.0.2.2\" (no behavior change for the existing AVD-based dev loop)"
    - "`./gradlew :app:assembleDebug -PemulatorHost=192.168.1.10` produces a BuildConfig where FIREBASE_EMULATOR_HOST equals \"192.168.1.10\""
    - "No source file under app/src/main/ contains the literal string \"10.0.2.2\" after the change (build.gradle.kts is the only place that literal lives)"
    - "Release build still hardcodes FIREBASE_EMULATOR_HOST to \"10.0.2.2\" (defense-in-depth literal — release never uses the field because USE_FIREBASE_EMULATOR=false)"
    - "Physical Android device on same Wi-Fi as host machine connects to local Firebase emulators when launched with -PemulatorHost=<host LAN IP>"
  artifacts:
    - path: "app/build.gradle.kts"
      provides: "Gradle property `emulatorHost` plumbed into BuildConfig.FIREBASE_EMULATOR_HOST (debug variant) + hardcoded literal (release variant)"
      contains: "FIREBASE_EMULATOR_HOST"
    - path: "app/src/main/java/com/giftregistry/di/AppModule.kt"
      provides: "Three Firebase emulator wirings (Auth/Firestore/Functions) sourcing host from BuildConfig.FIREBASE_EMULATOR_HOST"
      contains: "BuildConfig.FIREBASE_EMULATOR_HOST"
    - path: "app/src/main/java/com/giftregistry/di/StorageModule.kt"
      provides: "Firebase Storage emulator wiring sourcing host from BuildConfig.FIREBASE_EMULATOR_HOST"
      contains: "BuildConfig.FIREBASE_EMULATOR_HOST"
  key_links:
    - from: "app/build.gradle.kts"
      to: "BuildConfig.FIREBASE_EMULATOR_HOST"
      via: "providers.gradleProperty(\"emulatorHost\").getOrElse(\"10.0.2.2\") + buildConfigField(\"String\", ...)"
      pattern: "buildConfigField\\(\"String\", \"FIREBASE_EMULATOR_HOST\""
    - from: "AppModule.kt + StorageModule.kt"
      to: "BuildConfig.FIREBASE_EMULATOR_HOST"
      via: "useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, <port>)"
      pattern: "useEmulator\\(BuildConfig\\.FIREBASE_EMULATOR_HOST"
---

<objective>
Add a `-PemulatorHost=<ip>` Gradle property that overrides the hardcoded `"10.0.2.2"` Firebase emulator host across all four DI call sites, enabling on-device testing against the local Firebase emulator from a physical Android device on the same Wi-Fi network. Default value `10.0.2.2` preserves the existing AVD dev loop with zero behavior change for users who don't pass the flag.

Purpose: Unblocks physical-device testing of the Firebase emulator stack — currently impossible without source edits, since `10.0.2.2` is the AVD's loopback alias for the host machine and is meaningless on a physical device.

Output: One Gradle build-config plumbing change (debug + release variants) and three DI source edits (AppModule.kt × 3 call sites, StorageModule.kt × 1 call site). No new tests — config-plumbing change with no observable runtime behavior on default args; verification is dual-build grep.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@/Users/victorpop/ai-projects/gift-registry/CLAUDE.md
@/Users/victorpop/ai-projects/gift-registry/app/build.gradle.kts
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/di/AppModule.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/di/StorageModule.kt

<interfaces>
<!-- The pattern to mirror is already in app/build.gradle.kts:34-46. -->
<!-- Debug variant uses providers.gradleProperty(...).getOrElse(default); release hardcodes a safe literal. -->

From app/build.gradle.kts (existing `use_emulator` pattern, lines 34-46):
```kt
buildTypes {
    debug {
        // Default to "true" so the dev loop (./gradlew :app:assembleDebug) keeps hitting the emulator.
        // Opt out for on-device testing with: ./gradlew :app:assembleDebug -Puse_emulator=false
        val useEmulator = providers.gradleProperty("use_emulator").getOrElse("true")
        buildConfigField("boolean", "USE_FIREBASE_EMULATOR", useEmulator)
    }
    release {
        // Hardcoded false — release builds MUST NEVER point at the emulator,
        // regardless of whether -Puse_emulator is passed. This is a safety gate.
        buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "false")
    }
}
```

From app/src/main/java/com/giftregistry/di/AppModule.kt (current call sites — three to replace):
- line 21: `auth.useEmulator("10.0.2.2", 9099)` — Firebase Auth
- line 30: `db.useEmulator("10.0.2.2", 8080)` — Firestore
- line 42: `fns.useEmulator("10.0.2.2", 5001)` — Functions

`import com.giftregistry.BuildConfig` already present (line 3).

From app/src/main/java/com/giftregistry/di/StorageModule.kt (current call site — one to replace):
- line 55: `storage.useEmulator("10.0.2.2", 9199)` — Firebase Storage

`import com.giftregistry.BuildConfig` already present (line 5).
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Wire emulatorHost Gradle property and replace hardcoded "10.0.2.2" literals</name>
  <files>app/build.gradle.kts, app/src/main/java/com/giftregistry/di/AppModule.kt, app/src/main/java/com/giftregistry/di/StorageModule.kt</files>
  <action>
Make four edits, then verify with two builds.

**Edit 1 — `app/build.gradle.kts` (debug block, alongside existing `use_emulator` lines):**

Inside the `buildTypes { debug { ... } }` block, AFTER the existing two `use_emulator` lines (39), add (matching the existing comment style):

```kt
// Default to "10.0.2.2" (AVD loopback alias for host machine). Override for on-device
// testing on a physical device with: ./gradlew :app:assembleDebug -PemulatorHost=192.168.1.10
val emulatorHost = providers.gradleProperty("emulatorHost").getOrElse("10.0.2.2")
buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$emulatorHost\"")
```

Note the doubly-quoted form `"\"$emulatorHost\""` — `buildConfigField` expects the literal Java/Kotlin source expression for the value, which for a String must include the quotes.

**Edit 2 — `app/build.gradle.kts` (release block):**

Inside `buildTypes { release { ... } }`, AFTER the existing `USE_FIREBASE_EMULATOR=false` line (44), add (matching the safety-gate comment style above):

```kt
// Hardcoded literal — release builds never hit the emulator (gated by USE_FIREBASE_EMULATOR=false),
// but the field must still exist so the same Kotlin code compiles for both variants.
buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"10.0.2.2\"")
```

**Edit 3 — `app/src/main/java/com/giftregistry/di/AppModule.kt` (three replacements):**

Replace ALL three occurrences of the literal string `"10.0.2.2"` with `BuildConfig.FIREBASE_EMULATOR_HOST` (NOT a string interpolation — the BuildConfig field is already a String):

- Line 21: `auth.useEmulator("10.0.2.2", 9099)` → `auth.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9099)`
- Line 30: `db.useEmulator("10.0.2.2", 8080)` → `db.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 8080)`
- Line 42: `fns.useEmulator("10.0.2.2", 5001)` → `fns.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 5001)`

`import com.giftregistry.BuildConfig` is already present (line 3) — do not re-add.

**Edit 4 — `app/src/main/java/com/giftregistry/di/StorageModule.kt` (one replacement):**

- Line 55: `storage.useEmulator("10.0.2.2", 9199)` → `storage.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9199)`

`import com.giftregistry.BuildConfig` is already present (line 5) — do not re-add.

**Out of scope (do NOT touch):**
- macOS firewall config (developer-machine concern, documented in SUMMARY.md)
- Existing unit tests (they mock Firebase singletons — no contract change)
- CLAUDE.md / README updates (user can request later)
- Any release-variant runtime behavior — release stays gated by USE_FIREBASE_EMULATOR=false
  </action>
  <verify>
<automated>
# 1) Default build — must succeed AND BuildConfig must contain FIREBASE_EMULATOR_HOST = "10.0.2.2"
./gradlew :app:assembleDebug && \
  grep -rn 'FIREBASE_EMULATOR_HOST' app/build/generated/source/buildConfig/debug/ | grep -q '"10.0.2.2"' && \
  echo "OK: default build wires 10.0.2.2"

# 2) Override build — must succeed AND BuildConfig must contain the overridden value
./gradlew :app:assembleDebug -PemulatorHost=192.168.1.10 && \
  grep -rn 'FIREBASE_EMULATOR_HOST' app/build/generated/source/buildConfig/debug/ | grep -q '"192.168.1.10"' && \
  echo "OK: override build wires 192.168.1.10"

# 3) Source must contain ZERO "10.0.2.2" literals under app/src/main/ (build.gradle.kts excluded)
test "$(grep -rn '10.0.2.2' app/src/main/ | wc -l | tr -d ' ')" = "0" && \
  echo "OK: no hardcoded 10.0.2.2 literals remain in app/src/main/"

# 4) BuildConfig.FIREBASE_EMULATOR_HOST is referenced at all four DI call sites
test "$(grep -rn 'BuildConfig\.FIREBASE_EMULATOR_HOST' app/src/main/java/com/giftregistry/di/ | wc -l | tr -d ' ')" = "4" && \
  echo "OK: 4 DI call sites use BuildConfig.FIREBASE_EMULATOR_HOST"
</automated>
  </verify>
  <done>
- `app/build.gradle.kts` declares `FIREBASE_EMULATOR_HOST` BuildConfig field in BOTH debug (Gradle-property-driven, default `"10.0.2.2"`) and release (hardcoded literal `"10.0.2.2"`) variants
- All three `useEmulator("10.0.2.2", ...)` calls in `AppModule.kt` replaced with `useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, ...)`
- The single `useEmulator("10.0.2.2", ...)` call in `StorageModule.kt` replaced with `useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, ...)`
- `./gradlew :app:assembleDebug` succeeds with default args and produces BuildConfig with `FIREBASE_EMULATOR_HOST = "10.0.2.2"`
- `./gradlew :app:assembleDebug -PemulatorHost=192.168.1.10` succeeds and produces BuildConfig with `FIREBASE_EMULATOR_HOST = "192.168.1.10"`
- Zero matches for `"10.0.2.2"` literal under `app/src/main/`
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Human-verify on AVD (no regression) and physical device (new capability)</name>
  <files>n/a — verification-only task</files>
  <action>Pause for the user to manually confirm both scenarios described in <how-to-verify>: (1) AVD dev loop still works with default args (no regression), and (2) physical Android device successfully connects to host-machine emulators when launched with -PemulatorHost=&lt;host LAN IP&gt;. No code changes — this is a checkpoint:human-verify gate.</action>
  <verify>Manual — see <how-to-verify> below.</verify>
  <done>User confirms both AVD and physical-device scenarios work, OR reports issues for triage.</done>
  <what-built>
- Gradle property `-PemulatorHost=<ip>` plumbed into BuildConfig.FIREBASE_EMULATOR_HOST (debug variant; default `10.0.2.2`)
- Four DI call sites (AppModule.kt × 3 + StorageModule.kt × 1) now read the host from BuildConfig instead of a hardcoded `"10.0.2.2"` literal
- Release variant gets a hardcoded `"10.0.2.2"` BuildConfig literal as defense-in-depth (never used at runtime — gated by USE_FIREBASE_EMULATOR=false)
  </what-built>
  <how-to-verify>
**Verify two scenarios — first proves no regression, second proves new capability.**

### 1. AVD regression check (default args, no flag)

Confirm the existing dev loop is unchanged.

```bash
# Start Firebase emulators if not already running
firebase emulators:start

# In another terminal, build and install on the running AVD
./gradlew :app:installDebug
```

Then on the AVD:
- Launch the app
- Sign in (or use guest path) — Auth emulator should respond
- Navigate to a screen that reads from Firestore — data should load
- If Functions/Storage are exercised by the user flow, confirm those work too

Expected: Identical behavior to before this change. App connects to all four emulator services on `10.0.2.2`.

### 2. Physical device capability check (override flag)

Confirm the new flag enables on-device emulator testing.

Pre-requisites (developer-machine concerns — NOT part of this code change):
- Physical Android device on the SAME Wi-Fi network as the host machine
- Host machine's LAN IP known (e.g. `ipconfig getifaddr en0` on macOS — for example `192.168.1.10`)
- Firebase emulators running on the host: `firebase emulators:start`
- macOS firewall not blocking the emulator ports (5001, 8080, 9099, 9199) — if a Romanian-style firewall block surfaces, allow the `java` / `node` processes serving the emulators

Then:

```bash
# Replace 192.168.1.10 with your actual host LAN IP
./gradlew :app:installDebug -PemulatorHost=192.168.1.10
```

On the physical device:
- Launch the app
- Sign in (or use guest path) — Auth emulator on host should respond
- Navigate to a screen that reads from Firestore — data should load from host emulator
- Confirm the host's emulator UI logs (http://localhost:4000) show traffic from the device

Expected: App on physical device successfully connects to emulators running on the developer's host machine.

### 3. Approve or report

Report any issues — including environmental ones like firewall blocks or wrong LAN IP — so we can decide whether they warrant a follow-up code change or just a note in the SUMMARY.md.
  </how-to-verify>
  <resume-signal>Type "approved" once both AVD (no regression) and physical-device (new capability) scenarios are confirmed working, OR describe issues encountered.</resume-signal>
</task>

</tasks>

<verification>
- All four DI call sites now reference `BuildConfig.FIREBASE_EMULATOR_HOST` (grep check in Task 1 verify block)
- `app/src/main/` contains zero `"10.0.2.2"` literals (grep check in Task 1 verify block)
- Both build invocations (default + override) produce the expected BuildConfig values (grep on generated BuildConfig in Task 1 verify block)
- AVD dev loop still works (Task 2 human verify)
- Physical-device emulator connectivity works with `-PemulatorHost=<host LAN IP>` (Task 2 human verify)
</verification>

<success_criteria>
- `./gradlew :app:assembleDebug` succeeds with default args; generated BuildConfig has `FIREBASE_EMULATOR_HOST = "10.0.2.2"`
- `./gradlew :app:assembleDebug -PemulatorHost=192.168.1.10` succeeds; generated BuildConfig has `FIREBASE_EMULATOR_HOST = "192.168.1.10"`
- Zero `"10.0.2.2"` string literals remain under `app/src/main/`
- All four DI call sites use `BuildConfig.FIREBASE_EMULATOR_HOST`
- Release variant declares `FIREBASE_EMULATOR_HOST = "10.0.2.2"` (defense-in-depth literal)
- Existing unit tests continue to pass (no contract change — tests mock Firebase singletons)
- Human-verified: AVD dev loop unchanged; physical device connects to host emulators with override flag
</success_criteria>

<output>
After completion, create `.planning/quick/260510-sai-add-gradle-property-pemulatorhost-for-co/260510-sai-SUMMARY.md` capturing:
- What was changed (the four edits)
- The new `-PemulatorHost=<ip>` invocation pattern for physical-device testing
- A pointer to the developer-machine concerns (LAN IP discovery, macOS firewall) that are NOT code changes but are required for the new flag to be useful end-to-end
</output>
