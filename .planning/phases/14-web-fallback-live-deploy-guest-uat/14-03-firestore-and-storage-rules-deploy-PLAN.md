---
phase: 14-web-fallback-live-deploy-guest-uat
plan: 03
type: execute
wave: 2
depends_on: [14-01]
files_modified: []
autonomous: false
requirements: [WEB-04]
must_haves:
  truths:
    - "firestore.rules currently on disk (including the users/{uid}/notifications block from quick-260420-ozb commit 04d007d) is deployed to gift-registry-ro"
    - "storage.rules currently on disk (cross-service registry-ownership rules from Phase 12 Plan 02 commit e979e45) is deployed to gift-registry-ro for the first time"
    - "Programmatic check: `firebase firestore:rules get --project gift-registry-ro | diff - firestore.rules` reports no differences (deployed rules byte-identical to on-disk file)"
    - "Programmatic check: `firebase deploy --only firestore,storage` log contains `released rules firestore.rules` AND `released rules storage.rules` lines (Task 2 grep gate)"
    - "Rules Simulator confirms a non-owner write to /users/{otherUid}/registries/{registryId}/cover.jpg returns DENY (storage owner-only rule effective)"
    - "Rules Simulator confirms an unauthenticated read of a PRIVATE registry document returns DENY (firestore privacy rule effective — gates WEB-04 UAT item 5)"
  artifacts:
    - path: "firestore.rules"
      provides: "Production access control for registries, items, reservations, AND notifications subcollection (Phase 1 + 6 + quick-260420-ozb)"
      contains: "users/{uid}/notifications"
    - path: "storage.rules"
      provides: "Cross-service owner-only writes to /users/{uid}/registries/{registryId}/cover.jpg"
      contains: "firestore.get"
  key_links:
    - from: "web/src/firebase.ts useRegistryQuery"
      to: "firestore.rules canReadRegistry function"
      via: "Firestore SDK enforces deployed rules on every read"
      pattern: "match /registries/\\{registryId\\}"
    - from: "Android cover-photo upload path"
      to: "storage.rules /users/{userId}/registries/{registryId}/cover.jpg"
      via: "Storage SDK enforces deployed rules on every write; cross-service firestore.get() looks up registry ownership"
      pattern: "firestore\\.get\\("
---

<objective>
Deploy the current `firestore.rules` AND `storage.rules` to the gift-registry-ro project in a single `firebase deploy --only firestore,storage` invocation. This is the first-time deploy of `storage.rules` (committed in Phase 12 Plan 02 commit `e979e45` but deferred at UAT time per CONTEXT.md folded-todo `2026-04-28-deploy-phase-12-storage-rules.md`), AND it deploys the `users/{uid}/notifications` Firestore rules block from quick-260420-ozb commit `04d007d` (currently in `firestore.rules` but never deployed to prod).

Purpose: Closes folded todo `2026-04-28-deploy-phase-12-storage-rules.md` and the Firestore-rules half of folded todo `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` (rules deploy was the Part-C step in that todo). Runs in parallel with Plan 14-02 (functions deploy) per CONTEXT.md D-01 — disjoint `--only` scopes, same project. Unblocks WEB-04 UAT item 5 (private-registry deep-link rules-deny) in Plan 14-04.

Output: Both rule sets live in production. Plan SUMMARY captures the previous-known-good rules deploy timestamp + rollback runbook (D-03).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md
@.planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md
@firestore.rules
@storage.rules
@firebase.json

<interfaces>
<!-- Both rules files are already authored and wired in firebase.json. -->
<!-- This plan does NOT modify either file — only deploys current on-disk state. -->

From firebase.json:
```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "storage": {
    "rules": "storage.rules"
  }
}
```

Recent commits affecting rules (per `git log firestore.rules storage.rules` during planning):
- e979e45 — feat(12-02): author cross-service storage.rules + wire firebase.json (D-08)  [STORAGE — never deployed]
- 04d007d — feat(quick-260420-ozb): add writeNotification helper, Firestore rules, wire 5 event sources  [FIRESTORE — never deployed]
- da1b7e1 — fix(quick-260420-gat-01): harden firestore.rules against missing fields  [FIRESTORE — never deployed]
- 5c60f27 — feat(07-00): seed data, seed script, and Firestore config rules
- cc609ad — feat(06-00): extend firestore.rules + rules tests for Phase 6 collections (D-22)
</interfaces>
</context>

<tasks>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 1: Capture previous-known-good rules state (D-03 rollback prep) AND confirm cross-service grant readiness</name>
  <files>(no repo file changes — recording into SUMMARY)</files>
  <read_first>
    - .planning/phases/14-web-fallback-live-deploy-guest-uat/14-CONTEXT.md (D-03 rollback strategy)
    - .planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md ("First-time prompt — accept the cross-service grant" section)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Nothing — pre-deploy snapshot + a heads-up to the user that this is the first-ever storage.rules deploy for the project, which means Firebase CLI will prompt for cross-service rules grant acceptance.</what-built>
  <how-to-verify>
    Run locally and paste outputs back into the chat:

    1. Capture current git HEAD SHA (rollback code reference):
    ```
    git -C /Users/victorpop/ai-projects/gift-registry rev-parse HEAD
    ```

    2. Capture the current LIVE Firestore rules (so SUMMARY documents what was running before — used for rollback if the new deploy is broken):
    ```
    firebase firestore:rules get --project gift-registry-ro 2>/dev/null | head -80 || echo "Note: subcommand may not exist on older firebase-tools — alternative is Firebase Console > Firestore > Rules > release history"
    ```
    (If the subcommand isn't available in your firebase-tools version, that's fine — Firebase Console retains release history visually; SUMMARY can reference the Console URL `https://console.firebase.google.com/project/gift-registry-ro/firestore/rules`.)

    3. **IMPORTANT — first-time storage prompt heads-up:**

    Per the folded todo `2026-04-28-deploy-phase-12-storage-rules.md`: "On the FIRST deploy of storage.rules for this project, Firebase CLI will ask for permission to enable cross-service rules (Storage Rules calling firestore.get(...) to look up registry ownership / visibility). Accept the prompt (Y)."

    Confirm you understand: when Task 2 runs `firebase deploy --only firestore,storage`, it may pause interactively asking:
    ```
    ? Cloud Storage for Firebase needs to enable cross-service rules to look up ownership in Cloud Firestore. Enable? (Y/n)
    ```
    You must type `Y` and press Enter (Claude cannot answer this prompt — non-interactive flag would auto-fail it).

    If you'd rather pre-accept via Firebase Console (avoids the CLI prompt), do this BEFORE Task 2 runs:
    - Open https://console.firebase.google.com/project/gift-registry-ro/storage/rules
    - If you see a banner about enabling cross-service rules, accept it now.
    - Then notify the executor "cross-service grant pre-accepted in console" so the executor doesn't expect the prompt.

    Either way is fine — just pick one path.

    Paste outputs (git SHA + rules-get output OR console URL note + cross-service-grant approach choice) back into the chat.
  </how-to-verify>
  <acceptance_criteria>
    - User pastes a 40-char git SHA
    - User EITHER pastes a snapshot of current live firestore.rules content OR notes "release history available in Firebase Console for rollback reference"
    - User explicitly states ONE of: (a) "I will accept the cross-service grant prompt interactively in Task 2", OR (b) "Cross-service grant pre-accepted in console"
    - SUMMARY's Rollback runbook section will document: (a) git SHA, (b) command `git checkout <SHA> && firebase deploy --only firestore,storage --project gift-registry-ro` to revert rules, (c) Console URL for visual release-history rollback `https://console.firebase.google.com/project/gift-registry-ro/firestore/rules`
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the git SHA + chosen cross-service-grant approach, then type "rules rollback prep done".</resume-signal>
</task>

<task type="auto">
  <name>Task 2: Deploy firestore.rules and storage.rules together + diff deployed-vs-on-disk</name>
  <files>(deploys firestore.rules + storage.rules to gift-registry-ro — no repo file changes)</files>
  <read_first>
    - firestore.rules (entire file — verify it parses, no syntax errors before deploy; firebase CLI will reject on compile errors)
    - storage.rules (entire file — same; the `firestore.get(...)` cross-service helpers need correct paths)
    - firebase.json (confirm `firestore.rules` and `storage.rules` paths are correctly wired — already verified during planning)
    - .firebaserc (confirm default project is gift-registry-ro)
  </read_first>
  <action>
    **Step A — Deploy both rule sets in a single command:**

    ```bash
    cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only firestore,storage --project gift-registry-ro 2>&1 | tee /tmp/14-03-deploy.log
    ```

    NOTE on `--only firestore`: this deploys BOTH `firestore.rules` AND `firestore.indexes.json` per firebase.json. If you want rules-only (skip index changes), use `--only firestore:rules,storage`. Per Task 1's heads-up, this plan deploys both rules AND indexes — indexes is a no-op if `firestore.indexes.json` hasn't changed since last deploy.

    Expected interactive moment (FIRST STORAGE DEPLOY ONLY):
    ```
    ? Cloud Storage for Firebase needs to enable cross-service rules to look up ownership in Cloud Firestore. Enable? (Y/n)
    ```
    If this appears and the user opted for "accept interactively" in Task 1, the user must type `Y`. If the user pre-accepted in console (Task 1 option b), this prompt won't appear.

    Expected final lines:
    ```
    ✔  firestore: released rules firestore.rules to cloud.firestore
    ✔  storage: released rules storage.rules to firebase.storage/<bucket>
    ✔  Deploy complete!
    ```

    **Step B — Programmatic post-deploy verification (replaces console-eyeball check):**

    After the deploy succeeds, verify the deployed Firestore rules are byte-identical to the on-disk source file:
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry && firebase firestore:rules get --project gift-registry-ro > /tmp/14-03-deployed-firestore.rules 2>/dev/null && diff /tmp/14-03-deployed-firestore.rules firestore.rules
    ```
    Expected: `diff` exits 0 (no differences — deployed === on-disk).

    If `firebase firestore:rules get` is not available in your firebase-tools version (older CLI), fall back to sha256 by capturing the rules via the REST API:
    ```bash
    # Fallback when CLI subcommand missing: compare sha256 against the Source field reported in the deploy log
    sha256sum firestore.rules
    ```
    And cross-reference against the Console rules tab manually (Task 3 visual confirmation step covers this).

    Acceptance:
    - Exit code 0 on the deploy
    - Both `released rules firestore.rules` AND `released rules storage.rules` lines present
    - No `Error: HTTP Error: 403` (would mean cross-service grant not accepted)
    - No `Error: Failed to parse rules` (would mean a syntax error in either file)
    - `diff` between deployed firestore.rules and on-disk firestore.rules exits 0 (or fallback sha256 check passes)

    If a parse error appears, STOP. The on-disk rules files have a syntax issue that needs a quick-task fix before retrying.

    If a 403 cross-service error appears, STOP. The user needs to accept the grant in console then retry.

    If the diff reports differences, STOP. Something is wrong with the deploy pipeline (e.g., a different rules file was deployed than the on-disk one — possibly because of a stale firebase.json or wrong project).
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && firebase deploy --only firestore,storage --project gift-registry-ro 2>&1 | tee /tmp/14-03-deploy.log && grep -q "Deploy complete" /tmp/14-03-deploy.log && grep -qE "released rules firestore\.rules" /tmp/14-03-deploy.log && grep -qE "released rules storage\.rules" /tmp/14-03-deploy.log && ! grep -qiE "Failed to parse|HTTP Error: 403" /tmp/14-03-deploy.log && { firebase firestore:rules get --project gift-registry-ro > /tmp/14-03-deployed-firestore.rules 2>/dev/null && diff /tmp/14-03-deployed-firestore.rules firestore.rules; } || echo "Note: firestore:rules get not available — fall back to Task 3 visual verification"</automated>
  </verify>
  <acceptance_criteria>
    - `firebase deploy --only firestore,storage --project gift-registry-ro` exits 0
    - Deploy log contains literal string `Deploy complete!`
    - Deploy log contains a line matching `released rules firestore.rules` (Firestore rules shipped)
    - Deploy log contains a line matching `released rules storage.rules` (Storage rules shipped — first time)
    - Deploy log does NOT contain `Failed to parse rules`
    - Deploy log does NOT contain `HTTP Error: 403` (cross-service grant accepted)
    - Deploy log does NOT contain `permission denied` (project access OK)
    - **Programmatic diff:** `firebase firestore:rules get --project gift-registry-ro | diff - firestore.rules` exits 0 (deployed rules byte-identical to on-disk file). If the `firestore:rules get` subcommand is unavailable in the installed firebase-tools version, log a note in SUMMARY and rely on Task 3's visual confirmation for parity verification.
  </acceptance_criteria>
  <done>Both rule sets live on gift-registry-ro; first-time storage cross-service grant accepted; programmatic diff confirms deployed firestore.rules matches on-disk source byte-for-byte; ready for visual + simulator verification.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Console + Rules Simulator verification</name>
  <files>(no repo file changes — visual verification in Firebase Console)</files>
  <read_first>
    - .planning/todos/pending/2026-04-28-deploy-phase-12-storage-rules.md ("Verification" section — items 1-4 are the source of these checks)
    - storage.rules (so user knows what to look for in the Console rules tab)
    - firestore.rules (specifically the `users/{uid}/notifications` block from commit 04d007d — must appear in deployed rules)
  </read_first>
  <action>
    Run the human checkpoint workflow described in <how-to-verify> below. The user performs the listed steps manually and pastes the requested outputs / confirmations back into the chat via the resume-signal. Claude consumes the resume-signal, validates against <acceptance_criteria>, and proceeds to the next task only if all criteria are met.
  </action>
  <what-built>Both rule sets just deployed and Task 2 already verified the deployed firestore.rules matches on-disk byte-for-byte (programmatic check). This task layers in user-eyes verification in the Firebase Console + a smoke test via the Rules Simulator to confirm the rules actually enforce what we expect — gating WEB-04 UAT item 5 (private-registry rules-deny) in Plan 14-04.</what-built>
  <how-to-verify>
    Run these checks IN ORDER, paste evidence/screenshots back into the chat:

    **1. Firestore rules visual confirmation:**
    - Open https://console.firebase.google.com/project/gift-registry-ro/firestore/rules
    - Confirm the displayed rules content includes:
      - The line `match /users/{userId}/notifications/{notificationId}` (from quick-260420-ozb commit 04d007d — was NEVER live before this deploy)
      - All existing Phase 1/3/4/6 rules (registries, items, reservations, config/stores)
    - Confirm the "Last updated" / version timestamp at the top is from JUST NOW (within the last 5 minutes).
    - Paste back: timestamp + confirmation that the notifications match block is visible.

    **2. Storage rules visual confirmation:**
    - Open https://console.firebase.google.com/project/gift-registry-ro/storage/rules
    - Confirm the displayed rules content includes:
      - `firestore.get(...)` cross-service helpers (e.g. `isOwnerOfRegistry`, `isPublicOrInvited` per the folded todo's reference)
      - `match /{allPaths=**} { allow read, write: if false }` default-deny at the bottom
    - Confirm timestamp is from just now.
    - Paste back: timestamp + confirmation that `firestore.get` appears in the deployed rules content.

    **3. Storage Rules Simulator — non-owner write should DENY (per todo Verification step 3):**
    - In https://console.firebase.google.com/project/gift-registry-ro/storage/rules click "Rules playground" (or "Simulator")
    - Configure:
      - Location: `/users/some-other-uid/registries/some-registry-id/cover.jpg`
      - Authenticated: yes
      - Firebase UID: any value DIFFERENT from `some-other-uid` (e.g., `my-uid`)
      - Operation type: `create`
    - Click "Run"
    - Expected: **Simulated request denied** (red banner). The cross-service rule looked up the registry, found the simulator's UID is not the owner, and denied the write.
    - Paste back: confirmation of "denied" result (or screenshot if easy).

    **4. Firestore Rules Simulator — unauthenticated read of a private registry should DENY (gates UAT item 5):**
    - Open https://console.firebase.google.com/project/gift-registry-ro/firestore/rules > Rules playground
    - Configure:
      - Location: `/registries/<a-real-private-registry-id-you-own>` (if you don't have one, create a private registry first via the Android app or skip this step and rely on UAT item 5)
      - Authenticated: NO (unauthenticated)
      - Operation type: `get`
    - Expected: **Simulated request denied** (rule `canReadRegistry` requires either public visibility or owner/invitee match).
    - Paste back: confirmation of "denied" result.

    If any check shows ALLOW where DENY was expected, STOP — rules deploy succeeded but the on-disk rules content has a regression. File a quick-task to fix, do not proceed to Plan 14-04 UAT.
  </how-to-verify>
  <acceptance_criteria>
    - User confirms Firestore Console rules timestamp is fresh (within 5 min of Task 2 deploy)
    - User confirms the deployed Firestore rules content contains `users/{userId}/notifications` (from commit 04d007d — was missing before this deploy)
    - User confirms Storage Console rules timestamp is fresh and content contains `firestore.get(` substring (cross-service helpers present)
    - User confirms Storage Rules Simulator: non-owner create on `/users/{otherUid}/registries/{id}/cover.jpg` returns DENY
    - User confirms Firestore Rules Simulator: unauthenticated read of a private registry returns DENY (this proves UAT item 5 will pass)
  </acceptance_criteria>
  <verify>
    <automated>echo "Manual checkpoint — automated verification not applicable. Resume signal from user determines outcome (see acceptance_criteria)."</automated>
  </verify>
  <done>User typed the resume-signal AND all <acceptance_criteria> bullets are met. Next task is unblocked.</done>
  <resume-signal>Paste the 4 verification confirmations (or screenshot links), then type "rules verified live".</resume-signal>
</task>

</tasks>

<verification>
Combined Plan 14-03 verification (all must pass):

1. Deploy command `firebase deploy --only firestore,storage --project gift-registry-ro` exited 0 — Task 2
2. Deploy log contains both `released rules firestore.rules` AND `released rules storage.rules` — Task 2
3. Cross-service grant accepted (first-time storage deploy didn't 403) — Task 2
4. Programmatic diff: `firebase firestore:rules get | diff - firestore.rules` exits 0 (deployed === on-disk) — Task 2
5. Firebase Console > Firestore > Rules shows fresh timestamp + `users/{userId}/notifications` match block — Task 3
6. Firebase Console > Storage > Rules shows fresh timestamp + `firestore.get(` cross-service helpers — Task 3
7. Storage Rules Simulator: non-owner write denied — Task 3
8. Firestore Rules Simulator: unauthenticated read of private registry denied — Task 3 (gates UAT item 5)
</verification>

<success_criteria>
Plan 14-03 is complete when:
- [ ] firestore.rules + storage.rules deployed in a single command, deploy log clean
- [ ] First-time storage cross-service grant accepted (one-time prompt)
- [ ] Programmatic diff (or sha256 fallback) confirms deployed Firestore rules match on-disk source
- [ ] Firebase Console visual: both rules tabs show fresh deploy timestamps + content matches on-disk files
- [ ] Storage Rules Simulator: owner-only write rule effective (non-owner DENIED)
- [ ] Firestore Rules Simulator: private-registry privacy rule effective (unauthenticated DENIED) — gates UAT item 5
- [ ] SUMMARY.md captures: previous git SHA + rules rollback command (`git checkout <SHA> && firebase deploy --only firestore,storage --project gift-registry-ro`) + console URL for visual release history
- [ ] Plan 14-04 UAT item 5 (private-registry deep-link 404) is unblocked
- [ ] Folded todo `2026-04-28-deploy-phase-12-storage-rules.md` can be moved from `pending/` to `complete/`
</success_criteria>

<output>
After completion, create `.planning/phases/14-web-fallback-live-deploy-guest-uat/14-03-SUMMARY.md` including:

1. What was deployed (firestore.rules size + content highlights, storage.rules first-time deploy note)
2. Cross-service grant: accepted interactively in CLI OR pre-accepted in console (whichever path Task 1 chose)
3. Programmatic diff result (deployed firestore.rules == on-disk firestore.rules)
4. Console verification evidence (timestamps, content sanity)
5. Rules Simulator results (non-owner write DENY, unauthenticated private read DENY)
6. **Rollback runbook (D-03 — REQUIRED):**
   - Previous-known-good git SHA: `<from Task 1>`
   - Rollback command: `git checkout <SHA> && firebase deploy --only firestore,storage --project gift-registry-ro`
   - Visual release-history rollback URL: `https://console.firebase.google.com/project/gift-registry-ro/firestore/rules` (click previous release in version history)
7. Note: folded todo `2026-04-28-deploy-phase-12-storage-rules.md` can be moved from `pending/` to `complete/` after this plan ships
</output>
</output>
