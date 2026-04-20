---
phase: quick-260420-hgb
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt
  - app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt
  - app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt
  - app/src/main/java/com/giftregistry/domain/preferences/LanguagePreferencesRepository.kt
  - app/src/main/java/com/giftregistry/di/DataModule.kt
  - app/src/main/java/com/giftregistry/domain/usecase/ObserveEmailLocaleUseCase.kt
  - app/src/main/java/com/giftregistry/domain/usecase/SetEmailLocaleUseCase.kt
  - app/src/main/java/com/giftregistry/domain/preferences/EmailLocaleRepository.kt
  - app/src/main/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImpl.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
  - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt
  - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelEmailLocaleTest.kt
  - app/src/test/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImplTest.kt
autonomous: true
requirements:
  - QUICK-260420-HGB
must_haves:
  truths:
    - "Settings screen displays exactly ONE language picker (app language)"
    - "Choosing a language in Settings immediately updates the in-app UI locale (Romanian strings render when RO is chosen)"
    - "Choosing a language also writes users/{uid}.preferredLocale in Firestore so subsequent emails (purchase notifications) render in that language"
    - "No references to ObserveEmailLocaleUseCase, SetEmailLocaleUseCase, or EmailLocaleRepository remain in the codebase"
    - "app compiles (./gradlew :app:compileDebugKotlin) and existing unit tests pass (./gradlew :app:testDebugUnitTest)"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt"
      provides: "Settings UI with single language picker"
      contains: "auth_settings_language_label"
    - path: "app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt"
      provides: "Single changeLocale() that writes DataStore + Firestore + AppCompatDelegate"
      contains: "changeLocale"
  key_links:
    - from: "SettingsViewModel.changeLocale"
      to: "FirebaseFirestore users/{uid}.preferredLocale"
      via: "firestore.collection('users').document(uid).set(mapOf('preferredLocale' to tag), SetOptions.merge())"
      pattern: "preferredLocale"
    - from: "functions/src/notifications/onPurchaseNotification.ts"
      to: "users/{uid}.preferredLocale"
      via: "ownerSnap.data()?.preferredLocale (unchanged — already reads this field)"
      pattern: "preferredLocale"
---

<objective>
Collapse the two language pickers on the Settings screen into ONE unified language preference. The single "Language" setting drives both the in-app locale AND the locale used for server-rendered emails.

Purpose: The user reports "when I change language to Romanian, nothing happens" — during /gsd:discuss we clarified the real ask: there should be only ONE language knob. Whatever the user picks must also be the email language. No separate email-language picker.

Output:
- Settings screen with a single language ListItem (app language)
- SettingsViewModel.changeLocale() writes to BOTH local DataStore (drives AppCompatDelegate + `stringResource()` recomposition) AND Firestore `users/{uid}.preferredLocale` (already read by Cloud Functions for email rendering)
- All email-language-only plumbing removed (use cases, repo interface + impl, DI binding, tests)
- Orphaned string resources removed
- No migration: existing user docs that already have `preferredLocale` continue to work; docs that don't will be populated the next time the user changes language OR on first invocation (see Task 2 details — we do NOT backfill).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@./CLAUDE.md
@.planning/STATE.md

# Current (pre-change) code — executor should read these before editing
@app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt
@app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt
@app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt
@app/src/main/java/com/giftregistry/domain/preferences/LanguagePreferencesRepository.kt
@app/src/main/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImpl.kt
@app/src/main/java/com/giftregistry/di/DataModule.kt
@app/src/main/java/com/giftregistry/domain/usecase/ObserveEmailLocaleUseCase.kt
@app/src/main/java/com/giftregistry/domain/usecase/SetEmailLocaleUseCase.kt
@app/src/main/java/com/giftregistry/domain/preferences/EmailLocaleRepository.kt
@app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt
@app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelEmailLocaleTest.kt
@app/src/test/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImplTest.kt
@app/src/main/res/values/strings.xml
@app/src/main/res/values-ro/strings.xml

# Cloud Functions — DO NOT MODIFY, but the executor should confirm these still
# read the same field after the change:
@functions/src/notifications/onPurchaseNotification.ts
@functions/src/reservation/releaseReservation.ts

<interfaces>
<!-- Extracted from codebase — executor does not need to re-explore. -->

**Current SettingsViewModel contract (to be simplified):**
```kotlin
class SettingsViewModel @Inject constructor(
    private val languagePrefsRepo: LanguagePreferencesRepository,
    private val signOutUseCase: SignOutUseCase,
    private val observeEmailLocaleUseCase: ObserveEmailLocaleUseCase,  // REMOVE
    private val setEmailLocaleUseCase: SetEmailLocaleUseCase,          // REMOVE
) : ViewModel() {
    val currentLocale: StateFlow<String>            // KEEP
    val emailLocale: StateFlow<String?>             // REMOVE
    fun signOut()                                   // KEEP
    fun changeLocale(languageTag: String)           // MODIFY: also write Firestore
    fun onEmailLocaleChange(locale: String)         // REMOVE
}
```

**LanguagePreferencesRepository (unchanged interface — local DataStore only):**
```kotlin
interface LanguagePreferencesRepository {
    fun observeLanguageTag(): Flow<String?>
    suspend fun setLanguageTag(tag: String)
    suspend fun getLanguageTag(): String?
}
```

**Firestore user doc writes (from EmailLocaleRepositoryImpl — pattern being relocated):**
```kotlin
firestore.collection("users").document(uid)
    .set(mapOf("preferredLocale" to locale), SetOptions.merge())
    .await()
```

**Allowed locales (from EmailLocaleRepositoryImpl):**
```kotlin
private val ALLOWED_LOCALES = setOf("en", "ro")
```

**Cloud Functions read path (onPurchaseNotification.ts:121) — MUST continue to resolve correctly:**
```typescript
const ownerSnap = await db.collection("users").doc(ownerUid).get();
locale = normalizeLocale(ownerSnap.data()?.preferredLocale);
```

**firestore.rules allow self-write on users/{uid} (no field validation — safe to write preferredLocale):**
```
match /users/{userId} {
  allow read, update: if isSignedIn() && request.auth.uid == userId;
}
```
</interfaces>
</context>

<assumptions_to_reverify>
The planner did not run the app; assumptions below MUST be confirmed by the executor before blindly applying Task 1.

1. **The underlying bug might not be in the email-language plumbing at all.** The user said "changing to Romanian does nothing." Before deleting any code, the executor SHOULD:
   - Start the app, open Settings, pick Romanian from the FIRST picker (app language — `auth_settings_language_label`).
   - Observe whether any string in the app switches to Romanian.
   - If YES → the recomposition works; the bug was just user confusion about which picker does what. Proceed with Task 1 as planned.
   - If NO → stop. The real bug is that `AppCompatDelegate.setApplicationLocales()` isn't triggering recomposition (possible missing `android:configChanges` or `appcompat` init issue). File a diagnostic note in the SUMMARY and either (a) fix the recomposition issue as a precondition or (b) halt and escalate to user.

2. **The Cloud Functions DO currently read `preferredLocale` from `users/{uid}`.** Verified at `functions/src/notifications/onPurchaseNotification.ts:121`. The plan below preserves that contract.

3. **No migration is required.** Old user docs with only `preferredLocale` (from the old email-language-only code path) → still work. New users who never open Settings → get "en" fallback via `normalizeLocale`. Users who DO change language → write gets persisted to `preferredLocale`. This is acceptable for MVP per the task description.

4. **No other screen/ViewModel reads `EmailLocaleRepository` or the email-locale use cases.** Grep confirmed usage is SettingsViewModel-only. If the executor finds a new caller introduced after 2026-04-20, they MUST surface it and pause.

5. **`preferredLocale` field stays the canonical Firestore field name.** No renaming. Changing the field name would require a Cloud Functions change which is out of scope for this quick task.
</assumptions_to_reverify>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Unify language setting in ViewModel + UI, and delete email-language-only plumbing</name>
  <files>
    app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt,
    app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt,
    app/src/main/java/com/giftregistry/di/DataModule.kt,
    app/src/main/java/com/giftregistry/domain/usecase/ObserveEmailLocaleUseCase.kt,
    app/src/main/java/com/giftregistry/domain/usecase/SetEmailLocaleUseCase.kt,
    app/src/main/java/com/giftregistry/domain/preferences/EmailLocaleRepository.kt,
    app/src/main/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImpl.kt,
    app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt,
    app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelEmailLocaleTest.kt,
    app/src/test/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImplTest.kt
  </files>
  <behavior>
    After this task:
    - SettingsViewModelTest: `changeLocale("ro")` calls `languagePrefsRepo.setLanguageTag("ro")` AND writes `users/{uid}.preferredLocale = "ro"` to Firestore (via FirebaseAuth + FirebaseFirestore injected into the ViewModel OR via a method on LanguagePreferencesRepository — see action for which approach).
    - SettingsViewModelTest: `changeLocale("en")` also writes `"en"` to Firestore.
    - SettingsViewModelTest: `changeLocale` when user is NOT signed in → DataStore still gets written, no crash, Firestore write is skipped (swallowed `runCatching` / early-return). This preserves the onboarding + guest flows where user reaches Settings pre-auth (if that path exists; if not, still handle defensively).
    - SettingsViewModelTest: invalid tag (e.g., "zz") → DataStore write still happens (we trust the caller); optionally reject before Firestore write. Keep permissive-to-DataStore to match existing `changeLocale` signature (no throw on odd values — the picker only ever passes "en"/"ro").
    - No import of `ObserveEmailLocaleUseCase`, `SetEmailLocaleUseCase`, `EmailLocaleRepository` anywhere in `app/`.
  </behavior>
  <action>
    **Step 1 — Re-verify assumption 1 (app-language picker DOES cause UI recomposition).** Run the app manually (user will do this as part of the SUMMARY verification) OR at minimum add a log line in `changeLocale` and verify the DataStore write completes. If the planner assumption is wrong, STOP and write findings to `260420-hgb-SUMMARY.md` before proceeding with deletes.

    **Step 2 — Approach decision (keep the data layer thin):**
    Add ONE method to `LanguagePreferencesRepository` to write the remote mirror. Rationale: the ViewModel already depends on `LanguagePreferencesRepository`; adding Firebase to the ViewModel directly would leak Firebase into UI. Instead, extend the repository:

    In `domain/preferences/LanguagePreferencesRepository.kt`, add:
    ```kotlin
    /** Best-effort sync of the chosen language to the user's Firestore profile so Cloud Functions
     *  can render emails in the same language. No-op for unauthenticated users. Swallows failures. */
    suspend fun syncRemoteLocale(tag: String)
    ```

    In `data/preferences/LanguagePreferencesDataStore.kt`, inject `FirebaseAuth` and `FirebaseFirestore` via the constructor (add `@Inject constructor(@ApplicationContext context, private val auth: FirebaseAuth, private val firestore: FirebaseFirestore)`) and implement:
    ```kotlin
    override suspend fun syncRemoteLocale(tag: String) {
        val uid = auth.currentUser?.uid ?: return
        if (tag !in setOf("en", "ro")) return
        runCatching {
            firestore.collection("users").document(uid)
                .set(mapOf("preferredLocale" to tag), SetOptions.merge())
                .await()
        }.onFailure { e -> android.util.Log.w("LanguagePrefs", "Remote locale sync failed", e) }
    }
    ```
    Note: FirebaseAuth and FirebaseFirestore are already `@Provides`-available via `AppModule` / similar — confirm by grep. If they are not yet provided at the Singleton scope, add providers.

    **Step 3 — Simplify `SettingsViewModel`:**
    - Remove constructor params: `observeEmailLocaleUseCase`, `setEmailLocaleUseCase`.
    - Remove `emailLocale` StateFlow.
    - Remove `onEmailLocaleChange()` function.
    - Modify `changeLocale(languageTag)` to also call `languagePrefsRepo.syncRemoteLocale(languageTag)` inside the same `viewModelScope.launch { ... }` (after `setLanguageTag`, so the local write is never blocked by Firestore latency). Reference decision context: unified language per user ask on 2026-04-20.

    **Step 4 — Simplify `SettingsScreen.kt`:**
    - Remove the `emailLocale` `collectAsStateWithLifecycle()` line.
    - Remove the `emailLocaleDialogOpen` state.
    - Remove the entire "Phase 6 (UI-SPEC Contract 5): Email language picker" ListItem (lines ~87-109).
    - Remove the entire second `AlertDialog` block for email-locale (lines ~184-238).
    - Remove unused imports: `androidx.compose.material.icons.filled.Email`, `androidx.compose.foundation.layout.size`, `heightIn` if no longer used.

    **Step 5 — Delete email-language-only files:**
    - `app/src/main/java/com/giftregistry/domain/usecase/ObserveEmailLocaleUseCase.kt`
    - `app/src/main/java/com/giftregistry/domain/usecase/SetEmailLocaleUseCase.kt`
    - `app/src/main/java/com/giftregistry/domain/preferences/EmailLocaleRepository.kt`
    - `app/src/main/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImpl.kt`
    - `app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelEmailLocaleTest.kt`
    - `app/src/test/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImplTest.kt`

    **Step 6 — Update `DataModule.kt`:**
    - Remove the `bindEmailLocaleRepository` `@Binds` entry (lines 63-65).
    - Remove the unused imports (`com.giftregistry.data.preferences.EmailLocaleRepositoryImpl`, `com.giftregistry.domain.preferences.EmailLocaleRepository`).

    **Step 7 — Update `SettingsViewModelTest.kt`:**
    - Remove the `fakeObserveEmailLocaleUseCase` and `fakeSetEmailLocaleUseCase` fields.
    - Update the 3 `SettingsViewModel(...)` constructor calls to pass only the 2 remaining params.
    - Add one NEW test: `changeLocale syncs remote locale via repository`. Give the `FakeLanguagePreferencesRepository` a counter on `syncRemoteLocale` calls and assert it was called with "ro" after `viewModel.changeLocale("ro")`.
    - The `FakeLanguagePreferencesRepository` needs an override for `syncRemoteLocale` — record the last tag in a field.

    **Step 8 — Strings (DEFER delete to Task 2 so this task isolates code changes).** Do not touch strings.xml in this task.

    **Gotchas / why:**
    - Do NOT inject `FirebaseAuth` / `FirebaseFirestore` directly into `SettingsViewModel` — keeps the UI layer Firebase-free (per [Phase 02-android-core-auth]: Zero Firebase imports in domain layer pattern — apply the same discipline to UI).
    - Do NOT block `AppCompatDelegate.setApplicationLocales()` on the Firestore write — it's already called synchronously after `viewModelScope.launch { ... }`, which is correct.
    - Keep `runCatching` around the Firestore write — a network failure must NOT prevent the local language switch.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.settings.SettingsViewModelTest" --tests "com.giftregistry.data.preferences.LanguagePreferencesDataStoreTest" :app:compileDebugKotlin 2>&1 | tail -60</automated>
  </verify>
  <done>
    - `./gradlew :app:compileDebugKotlin` succeeds.
    - `./gradlew :app:testDebugUnitTest` passes (including the new `changeLocale syncs remote locale` test).
    - `grep -r "ObserveEmailLocaleUseCase\|SetEmailLocaleUseCase\|EmailLocaleRepository" app/src/` returns zero matches.
    - `SettingsScreen.kt` has exactly one ListItem with `Icons.Default.Language` and no ListItem with `Icons.Default.Email`.
    - `SettingsViewModel` constructor has exactly 2 parameters.
  </done>
</task>

<task type="auto">
  <name>Task 2: Remove orphaned email-language strings and confirm end-to-end behavior</name>
  <files>
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml
  </files>
  <action>
    **Step 1 — Confirm `settings_email_language_label` is orphaned.** Run:
    ```bash
    grep -rn "settings_email_language_label" app/src/ .planning/ 2>/dev/null
    ```
    Expected: only matches in `values/strings.xml`, `values-ro/strings.xml`, and `.planning/` (historical plans — leave those untouched).

    **Step 2 — Delete the orphaned string lines:**
    - `app/src/main/res/values/strings.xml` line 150 (and the preceding comment on line 149 `<!-- Phase 6: Email language setting -->`).
    - `app/src/main/res/values-ro/strings.xml` line 151 (and the preceding comment on line 150 `<!-- Faza 6: Setare limba email -->`).

    **Step 3 — Keep the following strings** (they are still used by the app-language picker):
    - `auth_settings_language_label`
    - `auth_settings_language_english`
    - `auth_settings_language_romanian`
    - `settings_language_confirm`

    **Step 4 — Do NOT touch Cloud Functions.** `functions/src/notifications/onPurchaseNotification.ts` continues to read `preferredLocale` from `users/{uid}` unchanged. Verify with a final grep that `preferredLocale` still appears in `functions/src/` — that's the contract we're relying on.

    **Step 5 — Final compile + lint check.**
  </action>
  <verify>
    <automated>grep -rn "settings_email_language_label" app/src/ 2>/dev/null ; ./gradlew :app:compileDebugKotlin :app:lintDebug 2>&1 | tail -40</automated>
  </verify>
  <done>
    - `grep -rn "settings_email_language_label" app/src/` returns zero matches.
    - `./gradlew :app:compileDebugKotlin` succeeds.
    - `./gradlew :app:lintDebug` shows no new errors regarding missing string resources.
    - `grep -n "preferredLocale" functions/src/notifications/onPurchaseNotification.ts` still returns the read at line ~121 (Cloud Functions contract preserved).
  </done>
</task>

</tasks>

<verification>
Overall quick-task verification:

1. **Build:** `./gradlew :app:assembleDebug` succeeds.
2. **Tests:** `./gradlew :app:testDebugUnitTest` — all green, including the new `changeLocale syncs remote locale` assertion.
3. **Grep sanity:**
   - `grep -r "EmailLocale" app/src/` → zero matches.
   - `grep -r "settings_email_language" app/src/` → zero matches.
   - `grep -r "preferredLocale" app/src/` → exactly one reference (the new `syncRemoteLocale` in `LanguagePreferencesDataStore.kt`).
4. **Manual smoke (user to verify in SUMMARY):**
   - Launch app → Settings → only ONE Language row is visible (no Email language row).
   - Tap it → dialog → pick Romanian → Confirm. App immediately recomposes with Romanian strings. (If this does not happen, the original bug lives deeper than the UI — see Assumption 1.)
   - Check Firestore console `users/{your-uid}` → `preferredLocale: "ro"` is now present.
   - Trigger a purchase notification (confirmPurchase flow) → the resulting email renders in Romanian.
</verification>

<success_criteria>
- Single language picker in Settings UI (observable truth 1).
- Picking Romanian updates app UI immediately AND writes Firestore `preferredLocale` field (observable truths 2 + 3).
- No dead code: `ObserveEmailLocaleUseCase`, `SetEmailLocaleUseCase`, `EmailLocaleRepository`, `EmailLocaleRepositoryImpl` and their tests are deleted; DataModule binding gone (observable truth 4).
- Strings `settings_email_language_label` removed from both locale resource files.
- Cloud Functions `preferredLocale` read path in `onPurchaseNotification.ts` unchanged — contract preserved.
- All existing unit tests still pass; one new test covers the remote-sync behavior (observable truth 5).
</success_criteria>

<output>
After completion, create `.planning/quick/260420-hgb-remove-the-email-language-setting-from-s/260420-hgb-SUMMARY.md` with:
- Confirmation of assumption 1 (did pre-change Romanian switch actually work?).
- Files deleted + files modified count.
- Test output: `./gradlew :app:testDebugUnitTest` tail.
- Any surprises (e.g., additional callers discovered, FirebaseAuth not available at Singleton scope, etc.).
- Updated `.planning/STATE.md` → append row to `## Quick Tasks Completed`.
</output>
