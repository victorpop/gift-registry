---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 06
type: execute
wave: 5
depends_on:
  - 16-02-backend-callables-and-invite-pending
  - 16-05-inbox-reskin-and-strings
files_modified:
  - app/src/main/java/com/giftregistry/GiftRegistryApp.kt
autonomous: false
requirements:
  - D-20
  - D-23
  - D-09
  - D-01
  - D-03
  - D-05
  - D-07
user_setup:
  - service: firebase-app-check
    why: "New Phase 16 callables enforce App Check (D-20). Without an Android App Check provider, callables return unauthenticated from the device."
    env_vars: []
    dashboard_config:
      - task: "Verify Phase 14 reCAPTCHA registration is still active for the gift-registry-ro project; debug provider for emulator already wired by Phase 14 (verify)"
        location: "Firebase Console → App Check → gift-registry-ro Android app → Provider settings"

must_haves:
  truths:
    - "Android App Check provider is initialized in GiftRegistryApp.onCreate before any Firebase operation"
    - "Composite index for users/*/notifications (type asc, payload.registryId asc) is deployed to gift-registry-ro"
    - "Cloud Functions acceptInvite + declineInvite + modified inviteToRegistry are deployed to europe-west3"
    - "Firestore security rules unchanged (D-18 Pattern 8 — no rule edits needed); verified by passing rules tests"
    - "On-device UAT confirms end-to-end accept flow: invite → inbox → sheet → accept → auto-nav to RegistryDetail"
    - "On-device UAT confirms end-to-end decline flow: invite → inbox → sheet → decline → confirmation dialog → owner inbox shows decline notification"
    - "On-device UAT confirms legacy invite fallback: legacy INVITE notification (no pendingEntryKey) still opens RegistryDetail via tap (D-11)"
    - "On-device UAT confirms inbox visual re-skin matches UI-SPEC contract (D-09)"
    - "On-device UAT confirms locale parity: full sheet + dialog + owner notifications render correctly in both EN and RO"
    - "On-device UAT confirms re-invite to already-member writes inbox+push but NOT pendingInvitedUsers (D-16) and inbox card falls back to legacy navigate (no sheet open)"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/GiftRegistryApp.kt"
      provides: "App Check provider initialization in Application.onCreate"
      contains: "FirebaseAppCheck"
    - path: ".planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-UAT-RESULTS.md"
      provides: "Per-scenario UAT pass/fail log"
      contains: "PASS"
    - path: ".planning/todos/completed/2026-05-22-wire-android-app-check-and-flip-enforcement.md"
      provides: "Pending todo from Phase 14 marked completed by this plan's App Check wiring"
      contains: "Phase 16"
  key_links:
    - from: "Android FirebaseAppCheck initialization"
      to: "Cloud Functions enforceAppCheck: true callables (acceptInvite, declineInvite)"
      via: "App Check token attached to every callable invocation"
      pattern: "FirebaseAppCheck.getInstance\\(\\)"
    - from: "firestore.indexes.json composite index"
      to: "Cloud Functions inbox cleanup query in acceptInvite/declineInvite post-tx"
      via: "Deployed before callable invoke to avoid index-required error (Pitfall 7)"
      pattern: "deploy.*firestore:indexes"
---

<objective>
Ship Phase 16 to production: wire Android App Check (resolving the pending Phase 14 todo as a hard prerequisite for D-20's `enforceAppCheck: true` callables), deploy the composite index + Cloud Functions, and run on-device UAT covering the full Phase 16 surface (accept flow, decline flow, legacy fallback, locale parity, re-invite of existing member, owner-side notifications, inbox re-skin visual contract).

Purpose: Production-ready Phase 16 with verified end-to-end correctness. App Check wiring closes a parked v1 hardening gap. UAT signs off the goal: "Strict accept-gate invite flow on Android."
Output: 1 modified Android file (App Check init) + deploy log + UAT results doc + closed pending todo.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md
@.planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md
@app/src/main/java/com/giftregistry/GiftRegistryApp.kt
@functions/src/index.ts
@firestore.indexes.json

<interfaces>
<!-- FirebaseAppCheck init pattern (Phase 14 wired the web side; Android side is the gap): -->
```kotlin
// Recommended Play Integrity provider for production; Debug provider for emulator
FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
    PlayIntegrityAppCheckProviderFactory.getInstance(),
)
// Debug provider for emulator:
// DebugAppCheckProviderFactory.getInstance()
```

<!-- gradle dependency required (verify libs.versions.toml): -->
firebase-appcheck-playintegrity (via Firebase BoM 34.11.0)
firebase-appcheck-debug (for debug builds)
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Wire Android App Check provider in GiftRegistryApp.onCreate</name>
  <read_first>
    - app/src/main/java/com/giftregistry/GiftRegistryApp.kt (current Application class — verify there's no App Check init yet)
    - .planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md (folded todo context — defines the contract this plan fulfills)
    - gradle/libs.versions.toml (verify firebase-appcheck-* dependencies are available or need adding)
    - app/build.gradle.kts (verify dependencies block — add new ones if needed)
  </read_first>
  <action>
    1. Inspect dependencies. If firebase-appcheck-playintegrity and firebase-appcheck-debug are NOT in libs.versions.toml + app/build.gradle.kts, add them via the existing Firebase BoM:
    ```toml
    # libs.versions.toml — under [libraries]
    firebase-appcheck-playintegrity = { module = "com.google.firebase:firebase-appcheck-playintegrity" }
    firebase-appcheck-debug = { module = "com.google.firebase:firebase-appcheck-debug" }
    ```
    ```kotlin
    // app/build.gradle.kts — under dependencies block
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    ```

    2. Modify app/src/main/java/com/giftregistry/GiftRegistryApp.kt. Add to imports:
    ```kotlin
    import com.google.firebase.appcheck.FirebaseAppCheck
    import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
    import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
    ```

    Add inside the existing `override fun onCreate()` method, BEFORE any other Firebase access (and BEFORE Hilt component initialization continues). Pattern:
    ```kotlin
        override fun onCreate() {
            super.onCreate()
            // ----- D-20 / Phase 14 follow-up: App Check provider wiring -----
            // Phase 16 callables (acceptInvite, declineInvite) enforce App Check.
            // Phase 14 web side already uses reCAPTCHA v3; Android side is wired here.
            //
            // Debug builds use the DebugAppCheckProvider so the Functions emulator
            // and physical device debug builds work without Play Integrity tokens.
            // Release builds use PlayIntegrity (Play Integrity API auto-enrolls
            // installed builds — no Play Store presence required for sideload).
            //
            // MUST be installed BEFORE any other Firebase SDK call to ensure the
            // App Check token is attached to the FIRST request.
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance(),
                )
            }

            // (existing onCreate body continues unchanged)
        }
    ```

    Preserve all existing onCreate body verbatim AFTER this insertion.

    3. After confirming build green, move .planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md to .planning/todos/completed/. Update the file to note completion via Phase 16 in a final note section.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug 2>&1 | tail -10 && grep -q "FirebaseAppCheck" app/src/main/java/com/giftregistry/GiftRegistryApp.kt && echo "App Check wiring present" && ls .planning/todos/completed/2026-05-22-wire-android-app-check-and-flip-enforcement.md 2>&1</automated>
  </verify>
  <acceptance_criteria>
    - app/src/main/java/com/giftregistry/GiftRegistryApp.kt contains string "FirebaseAppCheck.getInstance()"
    - GiftRegistryApp.kt contains string "DebugAppCheckProviderFactory"
    - GiftRegistryApp.kt contains string "PlayIntegrityAppCheckProviderFactory"
    - GiftRegistryApp.kt contains string "BuildConfig.DEBUG"
    - GiftRegistryApp.kt contains string "installAppCheckProviderFactory"
    - app/build.gradle.kts contains string "firebase.appcheck.playintegrity"
    - app/build.gradle.kts contains string "firebase.appcheck.debug"
    - ./gradlew :app:assembleDebug exits 0
    - File .planning/todos/pending/2026-05-22-wire-android-app-check-and-flip-enforcement.md no longer exists at pending path
    - File .planning/todos/completed/2026-05-22-wire-android-app-check-and-flip-enforcement.md exists
  </acceptance_criteria>
  <done>Android App Check provider wired; debug build green; folded todo closed.</done>
</task>

<task type="auto">
  <name>Task 2: Deploy composite index + Cloud Functions to gift-registry-ro</name>
  <read_first>
    - firestore.indexes.json (verify composite index from Plan 16-01 is present)
    - functions/src/index.ts (verify acceptInvite + declineInvite exports added in Plan 16-02)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md Pitfall 7 (index MUST deploy BEFORE functions invoke)
  </read_first>
  <action>
    1. Deploy indexes FIRST (Pitfall 7). Wait for build to complete (Firebase Console "Index Status: Enabled" — may take 1-5 minutes for empty collections):
    ```bash
    firebase deploy --only firestore:indexes --project gift-registry-ro 2>&1 | tee /tmp/16-06-indexes-deploy.log
    ```
    If output indicates index is building (not yet enabled), poll the console URL until status is "Enabled" before proceeding.

    2. Deploy Cloud Functions (includes modified inviteToRegistry + new acceptInvite + new declineInvite):
    ```bash
    cd functions && npm run build && cd ..
    firebase deploy --only functions:acceptInvite,functions:declineInvite,functions:inviteToRegistry --project gift-registry-ro 2>&1 | tee /tmp/16-06-functions-deploy.log
    ```
    Verify deploy log shows all 3 functions as "successfully deployed".

    3. Verify functions are live in europe-west3:
    ```bash
    gcloud functions list --regions=europe-west3 --project=gift-registry-ro --filter="name~acceptInvite OR name~declineInvite OR name~inviteToRegistry" 2>&1 | tee /tmp/16-06-functions-list.log
    ```

    4. Do NOT redeploy firestore.rules — Plan 16-01 verified via Pattern 8 testing that no rule edit is needed. Rules tests still cover the contract.

    Capture all three log outputs as evidence in 16-06-DEPLOY-LOG.md.
  </action>
  <verify>
    <automated>grep -E "successfully deployed|Index already built|Index built" /tmp/16-06-indexes-deploy.log && grep -E "successfully deployed.*acceptInvite|successfully deployed.*declineInvite|successfully deployed.*inviteToRegistry" /tmp/16-06-functions-deploy.log && grep -E "acceptInvite|declineInvite" /tmp/16-06-functions-list.log</automated>
  </verify>
  <acceptance_criteria>
    - /tmp/16-06-indexes-deploy.log contains string "successfully deployed" or "indexes are up-to-date" or "Index already built"
    - /tmp/16-06-functions-deploy.log contains string "acceptInvite"
    - /tmp/16-06-functions-deploy.log contains string "declineInvite"
    - /tmp/16-06-functions-deploy.log contains string "inviteToRegistry"
    - /tmp/16-06-functions-deploy.log contains string "successfully deployed" OR "Deploy complete"
    - /tmp/16-06-functions-list.log contains strings "acceptInvite" AND "declineInvite"
    - File .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-DEPLOY-LOG.md exists with deploy command outputs captured
  </acceptance_criteria>
  <done>Index deployed and enabled, Cloud Functions deployed, deploy log captured.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: On-device UAT — Phase 16 end-to-end acceptance</name>
  <what-built>
    Phase 16 complete: backend accept/decline callables deployed; Android InviteResponseSheet + decline confirmation + re-skinned inbox + extended notification types + App Check provider wired. Ready for human verification on a physical Android device.
  </what-built>
  <how-to-verify>
    Build + install the debug APK on a Pixel test device. Sign in as a known test user (Account A — "owner"). You'll need a SECOND device or emulator with Account B ("invitee") signed in for invite delivery testing.

    Run the 18 UAT scenarios below. Record PASS / FAIL / FLAG for each in `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-UAT-RESULTS.md`. Document with screenshots where visual.

    **Phase 16 UAT Checklist (18 scenarios):**

    ### Section A — Accept-gate flow (D-01, D-05, D-21, D-23)

    1. **A1 — Send invite as owner:** From Account A, invite Account B's email to a private registry. Confirm app reports invite sent.
    2. **A2 — Verify pending state in Firestore Console:** Open Firestore Console → registries/{id} → confirm `pendingInvitedUsers[B-uid] = true` AND `invitedUsers[B-uid]` is absent or false.
    3. **A3 — Invitee receives FCM push:** Account B's device shows a system-tray notification with title "Account A invited you to {registry}". Tap → opens app.
    4. **A4 — Invitee sees actionable card in inbox:** Open Account B's inbox → first card is the INVITE with proper title, body, MonoCaps timestamp, accent unread dot.
    5. **A5 — Tap card opens sheet:** Tap the INVITE card → ModalBottomSheet rises with: registry hero (gradient placeholder or cover image), "Account A invited you to" title, registry name in italic-accent Display L, optional event date in MonoCaps. NO accent dot on the card after sheet open (mark-as-read fires).
    6. **A6 — Cannot dismiss during loading:** Tap Accept → CircularProgressIndicator appears inside Accept button → swipe down on sheet → sheet stays open (D-07 dismissal guard).
    7. **A7 — Auto-navigation on Accept:** When Accept completes, sheet auto-dismisses + app navigates to RegistryDetailScreen for the registry. Items list renders correctly (Account B is now a member; rules permit read).
    8. **A8 — Firestore state post-accept:** Open Firestore Console → registries/{id} → confirm `pendingInvitedUsers[B-uid]` is GONE AND `invitedUsers[B-uid] = true`.
    9. **A9 — Owner inbox receives invite_accepted:** On Account A's inbox, open → see a new notification "Account B accepted your invite to {registry}". Icon = CheckCircle, MonoCaps timestamp present, accent dot if unread.
    10. **A10 — Invitee inbox shows JOINED confirmation:** On Account B's inbox, the original INVITE card is gone; a new card "You joined {registry}" is present. Tapping it navigates to the registry.

    ### Section B — Decline flow (D-03, D-22)

    11. **B1 — Send another invite as owner (same B uid OK — re-invite is allowed):** Repeat A1.
    12. **B2 — Tap Decline opens confirmation:** Account B opens inbox → tap INVITE → sheet opens → tap Decline → AlertDialog appears: "Decline invite to {registry}?" with [Cancel / Decline] buttons.
    13. **B3 — Cancel dismisses dialog, keeps sheet:** Tap Cancel → dialog dismisses; sheet stays open in Idle state with Accept + Decline both enabled.
    14. **B4 — Decline confirm dismisses sheet:** Tap Decline again → dialog → tap Decline (destructive) → spinner on Decline button → sheet dismisses; inbox INVITE card is gone.
    15. **B5 — Owner sees invite_declined:** On Account A's inbox, new card "Account B declined your invite to {registry}". Icon = Block.
    16. **B6 — Firestore state post-decline:** registries/{id} confirms `pendingInvitedUsers[B-uid]` is GONE AND `invitedUsers[B-uid]` is unchanged (no promote).

    ### Section C — Legacy + edge cases (D-11, D-16)

    17. **C1 — Legacy invite fallback:** Pre-Phase-16 inbox docs (any existing INVITE notifications from before this phase, OR — if none exist — manually craft one in Firestore Console: type="invite", payload={registryId, registryName, actorName, actorUid} with NO pendingEntryKey). Tap that card → sheet does NOT open; app navigates directly to RegistryDetail per D-11 fallback.
    18. **C2 — Re-invite of already-member:** From Account A (owner), invite Account B's email a third time (B is now in invitedUsers from A8). Confirm Firestore registries/{id}.pendingInvitedUsers does NOT acquire a B-uid entry (D-16). Account B receives an FCM push + inbox card; the card has no pendingEntryKey (D-16 already-member branch). Tapping it navigates to RegistryDetail (no sheet).

    ### Section D — Visual + locale (D-09, D-28)

    19. **D1 — Inbox visual contract:** Confirm GiftMaisonWordmark in TopAppBar, gm.paper background, 1dp gm.line dividers between cards (no Card elevation), MonoCaps timestamp like "5M AGO", 6dp accent dot for unread cards. Capture screenshot.
    20. **D2 — Romanian locale parity:** Switch device language to Romanian via Settings screen. Re-trigger A1-A10 (or open existing notification cards from RO state). Verify sheet title is "Account A te-a invitat la", Accept = "Acceptă", Decline = "Refuză", confirmation dialog = "Refuzi invitația la „{registry}"?", owner notifications display proper Romanian translations.

    **(Scenarios 19-20 are renumbered D1-D2 even though above count says 18 — count those as 18 total scenarios; D1 and D2 are subsumed into the 18 list. Adjust if you prefer 20 strict items.)**

    For each scenario, record:
    - Status: PASS / FAIL / FLAG (working but with concern)
    - Notes / screenshots if FAIL or FLAG.
    - For any FAIL: file a follow-up todo in .planning/todos/pending/ and link from UAT-RESULTS.md.

    Pass criteria: ALL 18 scenarios PASS for phase completion. If 1-2 minor FLAGs, document and decide whether to ship or fix-in-phase. If any blocker FAIL, return to the appropriate plan (16-02 backend / 16-04 sheet / 16-05 inbox re-skin) for revision.
  </how-to-verify>
  <action>See &lt;how-to-verify&gt; above — checkpoint task. Human operator builds debug APK, signs in on two test accounts, and runs the 18 UAT scenarios documented in &lt;how-to-verify&gt;. Record results in 16-06-UAT-RESULTS.md.</action>
  <verify>
    <automated>test -f .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-UAT-RESULTS.md &amp;&amp; grep -cE "^[0-9]+\." .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-UAT-RESULTS.md | awk '$1 >= 18 {exit 0} {exit 1}'</automated>
  </verify>
  <done>All 18 UAT scenarios recorded with PASS / FAIL / FLAG status in 16-06-UAT-RESULTS.md; any FAILs converted to follow-up todos in .planning/todos/pending/; user has explicitly approved phase completion via the resume-signal prompt.</done>
  <resume-signal>
    Type "approved — all 18 scenarios PASS" to mark phase complete. OR list FAILs/FLAGs to trigger a revision plan. OR type "deferred to follow-up: {item-ids}" if you accept shipping with documented follow-ups.
  </resume-signal>
</task>

</tasks>

<verification>
- App Check Android provider wired and debug APK builds clean.
- Composite index deployed and Enabled in Firebase Console.
- 3 Cloud Functions (acceptInvite, declineInvite, inviteToRegistry) deployed to europe-west3.
- 18 UAT scenarios documented in 16-06-UAT-RESULTS.md with PASS/FAIL/FLAG outcomes.
- Folded todo 2026-05-22-wire-android-app-check-and-flip-enforcement.md moved to completed/.
- ROADMAP.md Phase 16 entry updated to "Completed" status post-UAT-PASS.
</verification>

<success_criteria>
- App Check wiring closes the Phase 14 follow-up gap.
- Indexes + functions deployed in the correct order (indexes BEFORE functions per Pitfall 7).
- 18 UAT scenarios cover all 28 CONTEXT decisions (D-01..D-28) end-to-end.
- Owner and invitee see correct notifications on both accept and decline flows.
- Legacy and already-member edge cases verified (D-11 + D-16).
- Locale parity verified for the full sheet + dialog + owner notifications.
</success_criteria>

<output>
After UAT completion, create `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-06-SUMMARY.md` listing:
- App Check provider wiring (debug + release factories).
- Deploy log paths.
- 16-06-UAT-RESULTS.md scenario tally + any deferred follow-ups.
- Folded todo closure.

If all green, update ROADMAP.md Phase 16 to `[x] **Phase 16: Android Notifications Inbox + Invite Accept/Decline** - (completed YYYY-MM-DD)` and STATE.md to note phase completion + readiness for the next phase / `/gsd:verify-work`.
</output>
