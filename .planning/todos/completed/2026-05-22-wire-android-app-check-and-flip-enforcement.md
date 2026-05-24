---
date: 2026-05-22
category: security
phase_origin: 14-web-fallback-live-deploy-guest-uat
plan_origin: 14-04
priority: high
deferred_from: Plan 14-04 Task 9 (originally a must_have)
surfaced_during: Plan 14-04 close-out — Android App Check unwired discovered
---

# Wire Android App Check (Play Integrity + Debug), then flip enforcement Storage → Functions → Firestore

## Why this exists

Plan 14-04 Task 9 was originally a **must_have**: flip App Check from
monitor-only to ENFORCED for Cloud Storage, Cloud Functions, and Cloud
Firestore in Firebase Console (D-04 in `14-CONTEXT.md`).

That flip was **deferred at close-out** because a grep across the Android
app source tree returned ZERO matches for the App Check provider wiring:

```bash
grep -rn "installAppCheckProviderFactory|firebase-appcheck|Firebase.appCheck|PlayIntegrityAppCheck|DebugAppCheckProvider" \
  app/src/ app/build.gradle.kts gradle/libs.versions.toml
# (no output)
```

No `firebase-appcheck` / `firebase-appcheck-playintegrity` Gradle deps,
no `Firebase.appCheck.installAppCheckProviderFactory(...)` call anywhere
in `AppModule.kt` or any `Application` subclass, no debug-token registration.
The Android app currently sends NO App Check token with its Firestore /
Functions / Storage requests.

**If enforcement were flipped today, every Android-originated request would
be 403-rejected at the Firebase API ingress.** All authenticated owner
flows — registry creation, item add, reservation, invite — would break for
every Android user in production. The web fallback (which IS App-Check-wired
since Plan 14-04 Task 3 + commit `78fed8d`, verified UAT-1) would continue
working, but Android is the primary product surface.

So the work to close this todo is two-phased:

1. **Wire Android App Check** (Play Integrity for prod, Debug provider for
   debug builds), confirm Android sends valid tokens, let monitor mode run
   ≥24h to confirm verified % is high (>95%) across all three services.
2. **Flip enforcement** Storage → Functions → Firestore, smoke-test between
   each flip, keep rollback ready (D-03 in `14-CONTEXT.md`).

Both phases are needed to honor the original Plan 14-04 must_have. Until
both are done, App Check posture in production remains:

- **Web fallback:** reCAPTCHA v3 wired, `appcheck:exchange` returns 200,
  monitor mode active (UAT-1 PASS).
- **Android app:** NOT WIRED. No tokens sent. Currently invisible in
  monitor-mode metrics because no requests are tokenized.
- **Backend (Firestore / Functions / Storage):** monitor mode only — no
  enforcement. Untokenized requests succeed.

## Step-by-step close-out

### Phase 1: Wire Android App Check

#### 1.1 Add Gradle dependencies

Edit `gradle/libs.versions.toml`. Find the `[libraries]` section, add (BoM
already resolves the version per the existing `firebase-bom` ref):

```toml
firebase-appcheck = { group = "com.google.firebase", name = "firebase-appcheck" }
firebase-appcheck-playintegrity = { group = "com.google.firebase", name = "firebase-appcheck-playintegrity" }
firebase-appcheck-debug = { group = "com.google.firebase", name = "firebase-appcheck-debug" }
```

Edit `app/build.gradle.kts`. Add to the `dependencies { }` block:

```kotlin
implementation(libs.firebase.appcheck.playintegrity)
debugImplementation(libs.firebase.appcheck.debug)
```

(The base `firebase-appcheck` is pulled in transitively by the provider
modules. List it as `implementation(libs.firebase.appcheck)` only if you
want to use it directly — usually not needed.)

#### 1.2 Register Play Integrity provider in Firebase Console

1. Open https://console.firebase.google.com/project/gift-registry-ro/appcheck
2. Click the "Apps" tab.
3. Find the Android app `com.giftregistry`.
4. Click "Register" or "Get started" on the Play Integrity row.
5. Confirm registration. No further action needed in the console for
   Play Integrity — the deployment of a properly-signed APK is what
   makes attestation work end-to-end.

Note: Play Integrity requires:
- The APK is signed with the same SHA-256 fingerprint registered in
  Firebase Project Settings > Your apps > Android > SHA certificate
  fingerprints. (Likely already correct since Google Sign-in works.)
- The app is downloaded from Play Store OR installed via an Internal
  Testing track for the same package name `com.giftregistry`. Local-built
  debug APKs fall back to the Debug provider (next step).

#### 1.3 Initialize App Check in AppModule.kt BEFORE any Firebase call

The init MUST happen BEFORE any other Firebase API call (Firestore, Auth,
Functions, Storage). Easiest place: the `@HiltAndroidApp` `Application`
subclass `onCreate()` — runs before any `@Inject`ed Firebase singleton is
ever read.

Find the existing `Application` subclass. Add the App Check init at the
top of `onCreate()`:

```kotlin
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.Firebase

override fun onCreate() {
  super.onCreate()

  // App Check MUST be installed before any other Firebase API call.
  // Debug builds use the debug provider (paired with a debug token
  // registered in Firebase Console > App Check > Apps > ⋯ menu >
  // Manage debug tokens). Release builds use Play Integrity.
  Firebase.appCheck.installAppCheckProviderFactory(
    if (BuildConfig.DEBUG) {
      DebugAppCheckProviderFactory.getInstance()
    } else {
      PlayIntegrityAppCheckProviderFactory.getInstance()
    }
  )

  // ... rest of existing onCreate
}
```

If there's no `Application` subclass yet (which would be unusual for a
Hilt-using project — `@HiltAndroidApp` requires one), check the
`AndroidManifest.xml` `<application>` element for the `android:name`
attribute. If absent, create one — but a Hilt project almost certainly
already has one.

#### 1.4 Build debug APK + register debug token

```bash
cd /Users/victorpop/ai-projects/gift-registry
./gradlew :app:assembleDebug -Puse_emulator=false
~/Library/Android/sdk/platform-tools/adb -s WCR0219729000994 \
  install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb -s WCR0219729000994 \
  logcat -c
~/Library/Android/sdk/platform-tools/adb -s WCR0219729000994 \
  logcat | grep -i appcheck
```

Launch the app. The `DebugAppCheckProviderFactory` will print a debug
token to logcat on first run, similar to:

```
D/com.google.firebase.appcheck.debug.DebugAppCheckProvider: Enter this
  debug secret into the allow list in the Firebase Console for your
  project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
```

Copy that UUID. Then:

1. Firebase Console > App Check > Apps tab > Android app `com.giftregistry`
   > ⋯ menu > "Manage debug tokens"
2. Click "Add debug token".
3. Paste the UUID. Name it (e.g. `pop-phone-WCR0219729000994-debug`).
4. Save.

After this, debug builds on that phone send tokens that the Firebase
backend treats as valid for monitor / enforce decisions.

#### 1.5 Verify Android-originated traffic in monitor mode

1. Use the app for ~5 minutes (create registry, add item, browse list,
   reserve from web with the same account). Generates traffic across
   Firestore, Functions, Storage.
2. Open https://console.firebase.google.com/project/gift-registry-ro/appcheck
3. Look at the "Verified requests" column for each of: Cloud Firestore,
   Cloud Functions, Cloud Storage.
4. The verified % should now include traffic that originates from the
   Android app. After 24h with regular use, verified % should be >95%
   across all three services. (Web fallback traffic was already verified
   per UAT-1; Android adds to that pool.)

**If verified % is <90% after 24h:** STOP. Something is producing
untokenized traffic — likely a missed call site (Admin SDK from a script
that should be allowlisted, or an Android subsystem that initialized
Firebase before App Check installed). Investigate BEFORE flipping
enforcement.

### Phase 2: Flip enforcement (D-04, per-service, smoke between each)

Do these **one at a time**, in this exact order (least-blast-radius first
per D-04 reasoning in `14-CONTEXT.md`). Wait 5-10 minutes between flips so
the Firebase backend propagates the enforcement change and so you have
time to observe Android + web traffic before moving on.

#### 2.1 Cloud Storage (lowest blast radius — only cover-photo uploads)

1. Firebase Console > App Check > APIs tab > Cloud Storage > "Enforce".
2. Confirm the dialog. State changes from "Unenforced" to "Enforced".
3. **Smoke test from Android:** Open prod app, navigate to Create Registry,
   pick a cover photo from gallery, save. Confirm the upload succeeds
   (registry card shows the picked photo). If the upload fails with a 403,
   ROLLBACK by clicking "Unenforce" on Cloud Storage immediately.
4. **Smoke test from web:** web fallback does not upload images (giver-only
   surface) — nothing to test here, but verify the registry page still
   renders cover photos from existing registries (read path).

#### 2.2 Cloud Functions (medium blast radius — every callable)

1. Firebase Console > App Check > APIs tab > Cloud Functions > "Enforce".
2. Confirm. Wait 30 seconds.
3. **Smoke test from Android:** Open a public registry in the app, reserve
   an item, confirm the retailer Intent fires and the item flips to
   "Reserved". The reservation flow exercises `createReservation` callable
   (the canonical Cloud Function smoke path). If reserve fails with a 401
   from `cloudfunctions.net`, ROLLBACK by clicking "Unenforce" on
   Cloud Functions immediately.
4. **Smoke test from web:** open the same public registry in incognito
   Chrome, complete a guest reservation. Same callable path. If 401,
   ROLLBACK.

#### 2.3 Cloud Firestore (highest blast radius — every read/write)

1. Firebase Console > App Check > APIs tab > Cloud Firestore > "Enforce".
2. Confirm. Wait 30 seconds.
3. **Smoke test from Android:** Open the Home screen (registries list).
   Confirm registries render. Open a registry. Confirm items render. If
   either fails with `permission-denied`, ROLLBACK immediately.
4. **Smoke test from web:** open https://gift-registry-ro.web.app/ in
   incognito Chrome, navigate to a public registry URL, confirm the page
   renders the item grid with real-time status. If `permission-denied`,
   ROLLBACK.

#### 2.4 Post-flip end-to-end re-verification

Run the same flows as UAT-2 + UAT-6 in `14-04-UAT-RESULTS.md`:
- Reserve an item from web incognito → retailer redirect opens → countdown
  banner appears.
- Wait the natural 30 min → expiry email arrives → click re-reserve link
  → new reservation created.

If either flow regresses post-enforcement, something is sending untokenized
requests. Use Firebase Console > App Check > "Failed requests" log to
identify the offending caller.

### Phase 3: If rollback is needed

Per D-03 in `14-CONTEXT.md`:

1. Firebase Console > App Check > APIs tab > <affected service> > "Unenforce".
   Effective within ~1 minute. No code change required.
2. Tell affected users to clear site data for the web fallback.
   Per the user's accumulated memory at
   `~/.claude/projects/-Users-victorpop-ai-projects-gift-registry/memory/reference_appcheck_cached_failure.md`:
   the Firebase JS SDK aggressively caches 400-class App Check failures
   and will NOT auto-retry after enforcement is flipped off. Users must
   either clear IndexedDB for `gift-registry-ro.web.app` OR use a fresh
   incognito window. Android users are not affected by this caching bug
   (different SDK).
3. Document what went wrong in a new `.planning/todos/pending/` file so
   the next attempt addresses the root cause before re-flipping.

## How to close this todo

Once all three services show "Enforced" in Firebase Console AND post-flip
smokes from BOTH Android and web pass:

1. Update `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`
   "App Check Enforcement Flip" table — fill in the "Flipped At" column
   for all three services with the actual timestamp.
2. Move this todo from `pending/` to `completed/`:
   ```bash
   git mv \
     /Users/victorpop/ai-projects/gift-registry/.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md \
     /Users/victorpop/ai-projects/gift-registry/.planning/todos/completed/
   git commit -m "chore(todos): close App Check enforcement flip"
   ```
3. Consider whether this work justifies a Phase 14.1 entry in
   `ROADMAP.md` (versus closing it as quick-tasks) — the Android wiring
   alone is non-trivial (new Gradle deps + Application subclass changes
   + Console registration + debug-token management for every dev device).

## Related references

- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md`
  D-04 (monitor → enforce flip decision), D-03 (rollback procedure).
- `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-04-UAT-RESULTS.md`
  "App Check Enforcement Flip" table (currently TBD for all three rows).
- `~/.claude/projects/-Users-victorpop-ai-projects-gift-registry/memory/reference_appcheck_cached_failure.md`
  — SDK-caches-400 recovery procedure (clear IndexedDB / use incognito).
- Existing wired web side: `web/src/firebase.ts` `initializeAppCheck()`
  call (added in Plan 14-04 Task 3, commit `78fed8d`).
- Firebase docs: https://firebase.google.com/docs/app-check/android/play-integrity-provider
- Firebase docs: https://firebase.google.com/docs/app-check/android/debug-provider

---

## Completion note (Phase 16 plan 16-06, 2026-05-24)

**Phase 1 (Android App Check wiring) completed by Phase 16 Plan 16-06 Task 1.**

- `firebase-appcheck-playintegrity` (release) and `firebase-appcheck-debug` (debug) Gradle deps added to `gradle/libs.versions.toml` + `app/build.gradle.kts`.
- `FirebaseAppCheck.getInstance().installAppCheckProviderFactory(...)` wired in `GiftRegistryApp.onCreate()` right after `super.onCreate()` (Hilt injection completes there) and before the existing locale-restore block. Debug builds get `DebugAppCheckProviderFactory`; release builds get `PlayIntegrityAppCheckProviderFactory`. Gated by `BuildConfig.DEBUG`.
- `./gradlew :app:assembleDebug` exits 0 — wiring compiles clean.
- Reason this work was folded into Phase 16: Phase 16's `acceptInvite` + `declineInvite` Cloud Functions (Plan 16-02) enforce App Check (`enforceAppCheck: true`). Without an Android App Check provider, those callables would 403-reject every Android invocation. Wiring here is a hard prerequisite for Phase 16 UAT.

**Phase 2 (flip enforcement Storage → Functions → Firestore) — STILL DEFERRED.**

- The enforcement-flip work documented above in Phases 2.1 / 2.2 / 2.3 is NOT in scope of Phase 16. Phase 16 only wires the Android provider so that `enforceAppCheck: true` on the new callables works as designed.
- A new follow-up todo will need to track the org-wide monitor → enforce flip (24h verified-rate observation period, then per-service flips with smoke between each). Re-open with a fresh dated todo when ready to flip enforcement.

**Closed by:** Phase 16 Plan 16-06 Task 1.
