---
phase: quick-260420-gat
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - firestore.rules
  - tests/rules/firestore.rules.test.ts
autonomous: false
requirements:
  - QUICK-RULES-01
must_haves:
  truths:
    - "Owner listing registries with whereEqualTo('ownerId', uid) no longer throws PERMISSION_DENIED on legacy docs missing `visibility`"
    - "Owner can read legacy registries missing `invitedUsers`"
    - "Docs missing `ownerId` fail-closed — no client can read them"
    - "Public registries with `visibility='public'` remain readable by anyone (regression preserved)"
    - "Private registries with `visibility='private'` + invited user remain readable by the invitee (regression preserved)"
    - "Short-circuit evaluation in canReadRegistry skips visibility checks when `isOwner` is true"
  artifacts:
    - path: "firestore.rules"
      provides: "Hardened helper functions using resource.data.get(key, default)"
      contains: "resource.data.get("
    - path: "tests/rules/firestore.rules.test.ts"
      provides: "Legacy-doc test cases + regression guards"
      contains: "legacy"
  key_links:
    - from: "firestore.rules:isPublicRegistry"
      to: "resource.data.get('visibility', 'public')"
      via: "defensive field access with default matching RegistryDto"
      pattern: "get\\(['\"]visibility['\"], ['\"]public['\"]\\)"
    - from: "firestore.rules:isInvited"
      to: "resource.data.get('invitedUsers', {})"
      via: "defensive field access with empty-map default matching RegistryDto"
      pattern: "get\\(['\"]invitedUsers['\"], \\{\\}\\)"
    - from: "firestore.rules:isOwner"
      to: "resource.data.get('ownerId', '')"
      via: "defensive field access with empty-string default (fail-closed)"
      pattern: "get\\(['\"]ownerId['\"], ['\"]['\"]\\)"
    - from: "firestore.rules:canReadRegistry"
      to: "isOwner short-circuit first"
      via: "reorder so owner-filtered list queries skip visibility evaluation"
      pattern: "isOwner.*\\|\\|.*isPublicRegistry"
---

<objective>
Harden `firestore.rules` against missing fields on legacy registry documents to
resolve the production `PERMISSION_DENIED: Property visibility is undefined on
object. for 'list' @ L32` error.

Purpose: Legacy registry docs predate the `visibility` and `invitedUsers` fields
(added during Phase 03). Firestore's `list` rule evaluation is strict on
undefined field access and throws PERMISSION_DENIED instead of treating the
field as null. The fix is to switch every optional-field access in helper
functions to the defensive `resource.data.get(key, default)` pattern, with
defaults matching `RegistryDto` so behavior is semantically identical for new
docs.

Output: Updated `firestore.rules` with defensive helpers, new test cases
covering legacy docs, and a deployed ruleset on production.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@firestore.rules
@app/src/main/java/com/giftregistry/data/model/RegistryDto.kt
@app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
@tests/rules/firestore.rules.test.ts
@firebase.json

<interfaces>
<!-- Key contracts the executor needs. Do not re-explore the codebase. -->

**Production error (reproducible root cause):**
```
PERMISSION_DENIED: Property visibility is undefined on object. for 'list' @ L32
```
Triggered by `FirestoreDataSource.observeRegistries()` which runs:
```kotlin
firestore.collection("registries")
    .whereEqualTo("ownerId", ownerId)
    .addSnapshotListener { ... }
```
`list` rule evaluation is strict: field-access expressions that hit undefined
throw instead of evaluating to null. `get` is forgiving; `list` is not.

**Current helpers that hit the bug (firestore.rules:10-22):**
```
function isOwner(registryData) {
  return isSignedIn() && request.auth.uid == registryData.ownerId;
}
function isPublicRegistry(registryData) {
  return registryData.visibility == 'public';
}
function isInvited(registryData) {
  return isSignedIn() &&
         registryData.visibility == 'private' &&
         registryData.invitedUsers[request.auth.uid] == true;
}
```

**RegistryDto defaults (source of truth for rule defaults):**
```kotlin
val ownerId: String = ""
val visibility: String = "public"
val invitedUsers: Map<String, Any?> = emptyMap()
```
Rule defaults MUST match DTO defaults so new-doc behavior is unchanged.

**`resource.data.get(key, default)` contract:**
Firestore Security Rules `Map.get(key, default)` returns `default` when `key`
is absent. It is the canonical fix for optional-field access in `list` queries.
Reference: https://firebase.google.com/docs/reference/rules/rules.Map#get

**Short-circuit order in `canReadRegistry`:**
The current order is `isPublicRegistry || isOwner || isInvited`. For the owner's
own list query (`whereEqualTo('ownerId', uid)`), putting `isOwner` first skips
visibility/invitedUsers evaluation entirely when the user owns the doc — a
belt-and-suspenders defense in addition to `.get()` defaults.

**Test harness (tests/rules/):**
- `npm test` runs Jest against `@firebase/rules-unit-testing`.
- Requires Firestore emulator on `127.0.0.1:8080`.
- `npm run test:emulator` boots the emulator via `firebase emulators:exec`.
- `seedRegistry(id, data)` uses `withSecurityRulesDisabled` so legacy docs
  (missing fields) can be seeded directly.
- Use `firebase/firestore` `query` + `getDocs` for `list` rule testing
  (the current test file only exercises `get` rules — new tests for
  legacy-list need `query(collection(...), where('ownerId', '==', uid))`).
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Harden firestore.rules helper functions with defensive field access</name>
  <files>firestore.rules</files>
  <action>
Update `firestore.rules` helper functions at lines 10-22 to use the defensive
`resource.data.get(key, default)` pattern for every optional-field access.
Defaults MUST match `RegistryDto` defaults so new-doc semantics are unchanged.

**Edits:**

1. `isOwner` — defensive `ownerId` access with empty-string default (fail-closed
   for docs missing owner):
```
function isOwner(registryData) {
  // Use .get() with empty-string default so a doc missing ownerId fails closed
  // (nobody matches ''). Legacy docs all have ownerId, but defensive for
  // the PERMISSION_DENIED 'list' strictness reported in production.
  return isSignedIn() && request.auth.uid == registryData.get('ownerId', '');
}
```

2. `isPublicRegistry` — defensive `visibility` access defaulting to `'public'`
   (matches `RegistryDto.visibility: String = "public"`):
```
function isPublicRegistry(registryData) {
  // Legacy docs predate the `visibility` field (added in Phase 03). Default to
  // 'public' here to match RegistryDto.visibility default so Firestore 'list'
  // evaluation does not throw PERMISSION_DENIED on undefined field access.
  return registryData.get('visibility', 'public') == 'public';
}
```

3. `isInvited` — defensive `visibility` + `invitedUsers` access, defaulting to
   empty map (matches `RegistryDto.invitedUsers = emptyMap()`):
```
function isInvited(registryData) {
  // Defensive .get() for both fields. Legacy docs predate `invitedUsers` map;
  // returning {} means no one is invited, which is safe.
  return isSignedIn() &&
         registryData.get('visibility', 'public') == 'private' &&
         registryData.get('invitedUsers', {})[request.auth.uid] == true;
}
```

4. Reorder `canReadRegistry` to short-circuit on `isOwner` first. This is a
   belt-and-suspenders defense in addition to the `.get()` defaults — for
   owner-filtered list queries (`whereEqualTo('ownerId', uid)`), `isOwner`
   evaluates true and the visibility/invite checks are skipped entirely:
```
function canReadRegistry(registryData) {
  // isOwner first: for owner-filtered list queries the other checks are
  // skipped via short-circuit, avoiding any visibility-field evaluation path.
  return isOwner(registryData) ||
         isPublicRegistry(registryData) ||
         isInvited(registryData);
}
```

**Do NOT change:**
- Any `allow` rule
- The `create` rule's `request.resource.data.ownerId` check (that is
  `request.resource.data`, not `resource.data` — the client IS required to
  set `ownerId` on create, and the field is guaranteed present)
- Any collection rules outside the registry helpers (items, invites, users,
  mail, notifications_failures, fcmTokens, config, reservations)
- Rule semantics: a new doc with `visibility='public'` still passes
  `isPublicRegistry` because `get('visibility', 'public') == 'public'` is true
  both for new docs and for legacy docs missing the field (that is the desired
  unification)

**Preserve:** Keep explanatory comments referencing the production
PERMISSION_DENIED error so a future reader understands why `.get()` defaults
exist (per constraint).
  </action>
  <verify>
    <automated>node -e "const r = require('fs').readFileSync('firestore.rules','utf8'); const checks = [/\.get\(['\x22]ownerId['\x22], ['\x22]['\x22]\)/, /\.get\(['\x22]visibility['\x22], ['\x22]public['\x22]\)/, /\.get\(['\x22]invitedUsers['\x22], \{\}\)/, /isOwner\(registryData\)\s*\|\|\s*isPublicRegistry/]; const missing = checks.filter(c=>!c.test(r)); if (missing.length){ console.error('MISSING:', missing.map(String)); process.exit(1);} console.log('OK: defensive .get() defaults + short-circuit order in place');"</automated>
  </verify>
  <done>
- firestore.rules helpers use `resource.data.get('ownerId', '')`, `get('visibility', 'public')`, and `get('invitedUsers', {})`
- `canReadRegistry` evaluates `isOwner` first
- Comments explain the PERMISSION_DENIED context
- No other rules touched; semantics for new docs unchanged
  </done>
</task>

<task type="auto">
  <name>Task 2: Add emulator rules tests for legacy-doc cases and regression guards</name>
  <files>tests/rules/firestore.rules.test.ts</files>
  <action>
Add a new `describe("Legacy registry docs (missing fields)")` block to
`tests/rules/firestore.rules.test.ts` exercising the `list` rule path (not
just `get`), covering the 5 cases from the planning context.

**Imports to add** (merge into the existing import line 7):
```typescript
import { collection, doc, getDoc, getDocs, query, setDoc, where } from "firebase/firestore";
```

**New describe block to append at end of file (before the final closing
bracket, if any):**

```typescript
// ─────────────────────────────────────────────────────────────────────────────
// describe("Legacy registry docs (missing fields)")
// Regression guard for production PERMISSION_DENIED on list evaluation:
//   "Property visibility is undefined on object. for 'list' @ L32"
// ─────────────────────────────────────────────────────────────────────────────

describe("Legacy registry docs (missing fields)", () => {
  it("allows owner to list legacy registry missing `visibility`", async () => {
    await seedRegistry("legacy-no-vis", {
      ownerId: "owner-legacy-1",
      title: "Legacy",
      invitedUsers: {},
      // visibility intentionally absent
    });
    const db = testEnv.authenticatedContext("owner-legacy-1").firestore();
    const q = query(
      collection(db, "registries"),
      where("ownerId", "==", "owner-legacy-1")
    );
    await assertSucceeds(getDocs(q));
  });

  it("allows owner to list legacy registry missing `invitedUsers`", async () => {
    await seedRegistry("legacy-no-invites", {
      ownerId: "owner-legacy-2",
      visibility: "private",
      title: "Legacy private",
      // invitedUsers intentionally absent
    });
    const db = testEnv.authenticatedContext("owner-legacy-2").firestore();
    const q = query(
      collection(db, "registries"),
      where("ownerId", "==", "owner-legacy-2")
    );
    await assertSucceeds(getDocs(q));
  });

  it("denies read of a doc missing `ownerId` (fail-closed)", async () => {
    await seedRegistry("legacy-no-owner", {
      visibility: "private",
      title: "Orphan",
      invitedUsers: {},
      // ownerId intentionally absent
    });
    const db = testEnv.authenticatedContext("any-user").firestore();
    await assertFails(getDoc(doc(db, "registries", "legacy-no-owner")));
  });

  it("regression: public registry remains readable by unauthenticated users", async () => {
    await seedRegistry("pub-reg", {
      ownerId: "owner-p",
      visibility: "public",
      title: "Public",
      invitedUsers: {},
    });
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertSucceeds(getDoc(doc(unauthDb, "registries", "pub-reg")));
  });

  it("regression: private registry remains readable by invited user", async () => {
    await seedRegistry("priv-reg", {
      ownerId: "owner-priv",
      visibility: "private",
      title: "Private",
      invitedUsers: { "guest-user": true },
    });
    const db = testEnv.authenticatedContext("guest-user").firestore();
    await assertSucceeds(getDoc(doc(db, "registries", "priv-reg")));
  });
});
```

**Run the tests:** Use `npm run test:emulator` from `tests/rules/` which boots
the Firestore emulator and runs jest. If the emulator fails to start (e.g.
port 8080 already bound or firebase CLI missing), stop, document in the
SUMMARY that tests were written but not executed, and move to Task 3. Do
not block the deploy on this task.
  </action>
  <verify>
    <automated>cd tests/rules && npm run test:emulator 2>&1 | tail -40</automated>
  </verify>
  <done>
- 5 new test cases added under `describe("Legacy registry docs (missing fields)")`
- All 5 pass against the hardened rules OR executor documents emulator failure in SUMMARY and proceeds
- No existing tests regressed
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Deploy hardened rules to production (checkpoint — user-authorized)</name>
  <files>firestore.rules</files>
  <action>
**Automation-first:** Attempt the deploy via Firebase CLI from repo root.
This IS a checkpoint because it affects the live app — pause for the user to
confirm the reporter's account now loads the Registry list without
PERMISSION_DENIED before marking done.

```bash
firebase deploy --only firestore:rules
```

Expected output contains:
- `cloud.firestore: rules file firestore.rules compiled successfully`
- `+ firestore: released rules firestore.rules to cloud.firestore`

If the command fails with an auth error, run `firebase login --reauth` then
retry. If it fails with a compile error, revert Task 1 edits and investigate
— do NOT proceed with a broken ruleset on production.

After a successful deploy, wait for the user to confirm on device:
1. Sign in as a user with legacy registry docs (the reporter's production
   account).
2. Open the Registry list screen (`RegistryListViewModel` →
   `ObserveRegistriesUseCase(uid)` → `FirestoreDataSource.observeRegistries`).
3. Confirm the list loads with no PERMISSION_DENIED snackbar/log.
4. (Optional) Confirm public registries still load unauthenticated via web
   fallback and private-invite registries still load for invitees.
  </action>
  <what-built>
Hardened `firestore.rules` (Task 1) + test coverage (Task 2), deployed to the
production Firebase project.
  </what-built>
  <how-to-verify>
See `<action>` — CLI command + on-device check by the user.
  </how-to-verify>
  <verify>
    <automated>MISSING — checkpoint verification is the user's on-device confirmation; no automated command can attest to a successful production deploy + reporter's account loading the list.</automated>
  </verify>
  <done>
- `firebase deploy --only firestore:rules` completed successfully
- Reporter's production account loads the Registry list without PERMISSION_DENIED
- User explicitly approves via resume-signal
  </done>
  <resume-signal>Type "approved" once deploy succeeds and the reporter confirms the list loads, or describe any issues seen.</resume-signal>
</task>

</tasks>

<verification>
**Manual smoke checks after Task 3 approval:**

- Production Firebase Console → Firestore → Rules tab shows the updated rules
  with `.get(` patterns in helper functions.
- No new PERMISSION_DENIED errors in Crashlytics / logs within 1 hour of deploy.
- Reporter's account (who hit the original error) can load the Registry list
  screen.
</verification>

<success_criteria>
- [ ] `firestore.rules` helpers use `resource.data.get(key, default)` for `ownerId`, `visibility`, and `invitedUsers`
- [ ] `canReadRegistry` evaluates `isOwner` first (short-circuit belt-and-suspenders)
- [ ] New test cases cover: legacy-no-visibility list, legacy-no-invitedUsers list, legacy-no-ownerId fail-closed, public-regression, invited-regression
- [ ] Tests run green against the Firestore emulator (OR documented emulator failure in SUMMARY)
- [ ] Rules deployed via `firebase deploy --only firestore:rules`
- [ ] Original reporter's account loads the Registry list without PERMISSION_DENIED
</success_criteria>

<output>
After completion, create `.planning/quick/260420-gat-harden-firestore-rules-against-missing-f/260420-gat-SUMMARY.md` including:

- Files changed (firestore.rules, tests/rules/firestore.rules.test.ts)
- Diff summary of helper function changes
- Test run result (pass count OR emulator-skipped note)
- Deploy timestamp and command used
- Note in STATE.md Quick Tasks Completed table with commit SHA
- Any residual risk: e.g., downstream rules that also dereference optional
  registry fields (the current audit found none — items/invites subcollections
  use `isOwner(get(...).data)` which goes through the hardened helper)
</output>
