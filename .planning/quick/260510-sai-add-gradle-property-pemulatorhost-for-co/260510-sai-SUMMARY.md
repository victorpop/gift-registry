---
phase: quick/260510-sai
plan: 01
subsystem: infra
tags: [android, gradle, firebase, emulator, buildconfig, network-security, physical-device, on-device-testing]

requires:
  - phase: quick/260420-gv2
    provides: "Existing `use_emulator` BuildConfig wiring pattern in app/build.gradle.kts — the template the new emulatorHost property mirrors"
provides:
  - "`-PemulatorHost=<ip>` Gradle property plumbed into `BuildConfig.FIREBASE_EMULATOR_HOST` (debug variant; default `10.0.2.2`) so the same APK can target either AVD loopback or a host LAN IP"
  - "Four DI call sites (AppModule.kt × 3 + StorageModule.kt × 1) now read host from BuildConfig instead of a hardcoded `\"10.0.2.2\"` literal"
  - "Debug network-security config permits cleartext HTTP to private RFC1918 ranges (10.0.0.0/8, 192.168.0.0/16, 172.16.0.0/12) so the debug APK can hit a host emulator on the LAN without TLS"
  - "Firebase emulator host binding flipped from `127.0.0.1` (default) to `0.0.0.0` so a physical device can actually reach the host listener over Wi-Fi"
affects: [on-device-debugging, future-physical-device-testing, future-quick-tasks-needing-emulator-traffic-from-real-hardware]

tech-stack:
  added: []
  patterns:
    - "Gradle property → BuildConfig field via `providers.gradleProperty(...).getOrElse(default)` + `buildConfigField(\"String\", \"X\", \"\\\"$value\\\"\")` — applied symmetrically to debug (overridable) and release (hardcoded literal as defense-in-depth)"
    - "Debug-only `network_security_config.xml` cleartextTrafficPermitted scoped to private RFC1918 domains — release variant remains TLS-only by default"
    - "Emulator binding: `firebase.json` per-emulator `host: \"0.0.0.0\"` for Auth/Firestore/Functions/Storage so a physical device on the same Wi-Fi can reach the host listener"

key-files:
  created: []
  modified:
    - "app/build.gradle.kts — emulatorHost Gradle property + FIREBASE_EMULATOR_HOST BuildConfig field (debug + release variants)"
    - "app/src/main/java/com/giftregistry/di/AppModule.kt — three useEmulator call sites read BuildConfig.FIREBASE_EMULATOR_HOST"
    - "app/src/main/java/com/giftregistry/di/StorageModule.kt — one useEmulator call site reads BuildConfig.FIREBASE_EMULATOR_HOST"
    - "app/src/debug/res/xml/network_security_config.xml — cleartext permitted to private RFC1918 ranges (debug variant only)"
    - "firebase.json — Auth/Firestore/Functions/Storage emulators bound to 0.0.0.0 instead of default loopback"

key-decisions:
  - "Symmetric debug+release BuildConfig field even though release is gated by USE_FIREBASE_EMULATOR=false — defense-in-depth literal lets the same Kotlin code compile under both variants without `#ifdef`-style branching"
  - "Cleartext permitted ONLY in debug variant ONLY for RFC1918 ranges (10/8, 192.168/16, 172.16/12) — not a blanket cleartext bypass; release stays TLS-only"
  - "Emulator binding `0.0.0.0` lives in `firebase.json` (developer-machine config) — not a per-developer change; checked in because every Android contributor needs LAN-reachable emulators if they ever test on hardware"
  - "Out of scope (documented, not coded): macOS firewall rules — developer's host machine concern; addressed once per machine via System Settings → Network → Firewall when the user enables it"

patterns-established:
  - "When a Gradle-property-driven BuildConfig field is added, mirror the existing use_emulator pattern: provider→getOrElse→buildConfigField in debug, hardcoded literal in release"
  - "On-device emulator testing requires three layers of permission: (a) reachable host (`0.0.0.0` binding), (b) reachable network (cleartext + RFC1918 in network-security-config), (c) right host literal at runtime (BuildConfig.FIREBASE_EMULATOR_HOST). All three must align or traffic fails silently."

requirements-completed:
  - QUICK-260510-SAI

duration: ~90min (across two sessions; gap was for the physical-device verification round-trip)
completed: 2026-05-12
---

# Quick Task 260510-sai: Configurable `-PemulatorHost` Gradle Property Summary

**Configurable Firebase emulator host (`-PemulatorHost=<ip>`) for physical-device testing, plus the two follow-on fixes (debug cleartext to RFC1918 + emulator `0.0.0.0` bind) that the on-device verification surfaced.**

## Performance

- **Duration:** ~90 min (across two sessions — second session triggered by physical-device verification round-trip)
- **Completed:** 2026-05-12
- **Tasks:** 2 planned (1 auto + 1 human-verify checkpoint), grew to 3 commits via the deviation surfaced at the checkpoint
- **Files modified:** 5

## Accomplishments

- **`-PemulatorHost=<ip>` flag works end-to-end on physical hardware.** User connected device WCR0219729000994 over Wi-Fi to the host emulator at `192.168.1.10` and successfully completed Google sign-in + email/password sign-in flows — every Firebase service (Auth, Firestore, Functions, Storage) reachable from the device.
- **AVD dev loop unchanged.** Default `./gradlew :app:assembleDebug` still wires `"10.0.2.2"` — zero behavior change for any contributor who doesn't pass the new flag.
- **Three-layer on-device emulator stack stitched together.** Not just the BuildConfig plumbing (planned), but also the two layers underneath it (cleartext permission + emulator bind) that the original plan didn't anticipate.

## Task Commits

This task grew from 1 commit to 3 commits because human-verify caught two real layers the planner couldn't see ahead of time. See "Deviations from Plan" below for the scope-expansion rationale.

1. **Task 1 (planned): Wire emulatorHost Gradle property + BuildConfig field + DI call sites** — `e42ec00` (feat)
2. **Deviation 1 (added during verification): Permit cleartext HTTP in debug network security config** — `9cc06b1` (fix)
3. **Deviation 2 (added during verification): Bind Firebase emulators to `0.0.0.0`** — `62515e5` (fix)
4. **Task 2: Human-verify checkpoint** — no commit; user-confirmed device walkthrough cleared

**Plan metadata:** committed alongside this SUMMARY.

## Files Created/Modified

- `app/build.gradle.kts` (modified) — debug block adds `val emulatorHost = providers.gradleProperty("emulatorHost").getOrElse("10.0.2.2")` + `buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$emulatorHost\"")`; release block hardcodes the same field to `"10.0.2.2"` as defense-in-depth (never read at runtime because USE_FIREBASE_EMULATOR=false gates the entire emulator path).
- `app/src/main/java/com/giftregistry/di/AppModule.kt` (modified) — three `useEmulator("10.0.2.2", ...)` call sites (Auth 9099, Firestore 8080, Functions 5001) now read `BuildConfig.FIREBASE_EMULATOR_HOST`.
- `app/src/main/java/com/giftregistry/di/StorageModule.kt` (modified) — one `useEmulator("10.0.2.2", 9199)` call site (Storage) reads `BuildConfig.FIREBASE_EMULATOR_HOST`.
- `app/src/debug/res/xml/network_security_config.xml` (modified) — added `<domain includeSubdomains="true">` entries with `cleartextTrafficPermitted="true"` for `10.0.0.0/8`, `192.168.0.0/16`, `172.16.0.0/12`. Release variant has no such file and remains TLS-only.
- `firebase.json` (modified) — Auth/Firestore/Functions/Storage emulators each get `"host": "0.0.0.0"` so the host machine's listener accepts connections from non-loopback peers (i.e., the physical device on the same Wi-Fi).

## Automated Gates

| Gate | Command | Result |
| ---- | ------- | ------ |
| Default debug build wires `10.0.2.2` | `./gradlew :app:assembleDebug` + grep BuildConfig | OK |
| Override build wires the supplied IP | `./gradlew :app:assembleDebug -PemulatorHost=192.168.1.10` + grep BuildConfig | OK |
| Zero `"10.0.2.2"` literals in app/src/main/ | grep recursive | OK (0 matches) |
| Four DI call sites use `BuildConfig.FIREBASE_EMULATOR_HOST` | grep recursive | OK (4 matches) |

## Human Verification

**Status:** approved.

User confirmed on physical Android device (serial `WCR0219729000994`) connected over Wi-Fi to the host machine at `192.168.1.10`:
- Built with `./gradlew :app:installDebug -PemulatorHost=192.168.1.10`.
- Launched the app on the device.
- **Continue with Google** succeeded end-to-end against the host Firebase Auth emulator.
- **Email/password sign-in** succeeded end-to-end against the same emulator.
- Host emulator UI (http://localhost:4000) showed traffic originating from the device.

The two follow-on fixes (cleartext + 0.0.0.0 bind) were derived from the verification feedback loop — see "Deviations from Plan" below.

## Decisions Made

- **Defense-in-depth literal on release variant.** The release variant declares `FIREBASE_EMULATOR_HOST = "10.0.2.2"` even though `USE_FIREBASE_EMULATOR = false` guarantees the emulator code path is never reached. Keeping the field on both variants means the same Kotlin code compiles under both — no `BuildConfig.FIREBASE_EMULATOR_HOST` reference is conditional. The hardcoded literal is unreachable at runtime by construction.
- **RFC1918-scoped cleartext, not a blanket bypass.** The debug `network_security_config.xml` permits cleartext only for the three private IP ranges (10/8, 192.168/16, 172.16/12). Public-internet traffic over HTTP would still be rejected even in debug. Release variant has no override and remains TLS-only.
- **`0.0.0.0` bind in `firebase.json`, not a per-developer instruction.** The bind change is committed because every Android contributor who ever wants to test on hardware needs the same bind. Putting it in `firebase.json` means new contributors get it for free.
- **Macros firewall NOT touched.** Out of scope and documented in this SUMMARY only — once-per-machine developer-machine concern. The user's machine had its firewall already configured permissively enough; if a future contributor hits a firewall block, the verification steps in the original plan flag it as an environmental issue (not a code change).

## Deviations from Plan

The original plan had one task: wire `-PemulatorHost`. The human-verify checkpoint on physical hardware surfaced TWO additional gaps the planner had no way to anticipate without an on-device probe. Both were Rule-3 (blocking) deviations — each prevented progress on the planned task's verification.

### Auto-fixed Issues

**1. [Rule 3 — Blocking] Cleartext HTTP not permitted by Android network policy**

- **Found during:** Task 2 (human-verify on physical device, after Task 1 commit `e42ec00` landed)
- **Issue:** Physical device build with `-PemulatorHost=192.168.1.10` correctly wired the IP into BuildConfig, but every Firebase SDK call failed with `Cleartext HTTP traffic to 192.168.1.10 not permitted`. Android's default `cleartextTrafficPermitted=false` (since API 28) blocks plain HTTP for non-loopback hosts — including LAN IPs. The AVD's `10.0.2.2` is treated as loopback so this was invisible during default-args testing.
- **Fix:** Updated `app/src/debug/res/xml/network_security_config.xml` to add `<domain includeSubdomains="true">10.0.0.0/8 / 192.168.0.0/16 / 172.16.0.0/12</domain>` entries with `cleartextTrafficPermitted="true"`. Scoped strictly to RFC1918 private ranges so the developer's debug build can't accidentally fall back to cleartext for public hosts.
- **Files modified:** `app/src/debug/res/xml/network_security_config.xml`
- **Verification:** Rebuilt + reinstalled APK; Logcat no longer shows `Cleartext HTTP not permitted`. New failure surfaced (deviation 2 below).
- **Committed in:** `9cc06b1` (separate fix commit)

**2. [Rule 3 — Blocking] Firebase emulators bound to `127.0.0.1` reject non-loopback connections**

- **Found during:** Task 2 (human-verify, after deviation 1 above resolved)
- **Issue:** With cleartext now permitted, the physical device's request reached the host's network stack — but Firebase emulators bind to `127.0.0.1` by default. Logcat showed `Failed to connect to 192.168.1.10:9099` from `OkHttpClient`. Telnet/`nc` from another machine confirmed nothing was listening on `192.168.1.10:9099` even though `localhost:9099` was alive on the host.
- **Fix:** Updated `firebase.json` to set `"host": "0.0.0.0"` on each of the four emulators (`auth`, `firestore`, `functions`, `storage`). Restarting the emulator stack makes the listeners bind on all interfaces — including the host's LAN interface.
- **Files modified:** `firebase.json`
- **Verification:** Restarted `firebase emulators:start`; rebuilt APK; the device-side Google sign-in flow completed end-to-end. Host emulator UI showed traffic from `192.168.1.x`. User-confirmed verification approval followed immediately.
- **Committed in:** `62515e5` (separate fix commit)

---

**Total deviations:** 2 auto-fixed (both Rule 3 blocking)

**Impact on plan:** The planned BuildConfig wiring was a necessary-but-not-sufficient first step. The two deviations are the other two layers of the on-device emulator stack (network policy + listener bind). Without them, the planned flag would have compiled cleanly but produced silent network failures at runtime. The on-device verification checkpoint was structurally correct — it caught what no static analysis could have caught. Scope expansion is real and intentional: the closed task delivers a complete, end-to-end physical-device emulator path, not just a build-config flag.

**Note on quick-260510-v4v interaction:** Deviation 1 (cleartext) and deviation 2 (bind) were observable as actionable Logcat lines ONLY because the related quick task 260510-v4v had landed first (`626f716`) and turned previously-silent Continue-with-Google failures into logged WARN lines. Without v4v's error-surfacing fix, this entire on-device debugging session would have stalled at "the button does nothing."

## Issues Encountered

None beyond the two deviations above.

## User Setup Required

None for the typical AVD dev loop. For physical-device testing, see "How to use" below.

## How to Use (on-device testing)

1. Confirm the host machine and Android device are on the same Wi-Fi network.
2. Get the host LAN IP: `ipconfig getifaddr en0` on macOS.
3. Start Firebase emulators: `firebase emulators:start` (the `firebase.json` already binds them to `0.0.0.0`).
4. If macOS firewall is enabled, allow inbound on ports `5001`/`8080`/`9099`/`9199`.
5. Build and install: `./gradlew :app:installDebug -PemulatorHost=<host LAN IP>`.

## Known Stubs

None. All five touched files are wired end-to-end and user-verified on physical hardware.

## Next Phase Readiness

- **Physical-device test loop unblocked.** Any future quick task or phase that needs real-hardware verification against the Firebase emulator stack now has a working invocation pattern (`-PemulatorHost=<ip>`).
- **Future opportunity:** If we ever want to test against a real (non-emulator) Firebase project from a debug build, the `USE_FIREBASE_EMULATOR=false` gate already handles it — no further BuildConfig surgery needed.
- **Latent concern (not in scope):** The `firebase.json` `0.0.0.0` bind exposes the host's emulator UI on the LAN as well. On an untrusted network, that's a low-stakes risk (emulator data is throwaway) but worth noting if a contributor ever runs the emulator on, say, a coffee-shop Wi-Fi. No action taken.

## Self-Check: PASSED

Verified after writing this SUMMARY:
- `app/build.gradle.kts` — exists.
- `app/src/main/java/com/giftregistry/di/AppModule.kt` — exists.
- `app/src/main/java/com/giftregistry/di/StorageModule.kt` — exists.
- `app/src/debug/res/xml/network_security_config.xml` — exists.
- `firebase.json` — exists.
- Commit `e42ec00` (Task 1, feat) — present in `git log`.
- Commit `9cc06b1` (Deviation 1, fix) — present in `git log`.
- Commit `62515e5` (Deviation 2, fix) — present in `git log`.

---

*Phase: quick/260510-sai*
*Completed: 2026-05-12*
