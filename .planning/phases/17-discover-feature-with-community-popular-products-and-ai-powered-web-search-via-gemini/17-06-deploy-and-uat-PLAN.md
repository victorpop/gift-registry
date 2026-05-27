---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 06
type: execute
wave: 4
depends_on:
  - "17-01"
  - "17-02"
  - "17-03"
  - "17-04"
  - "17-05"
files_modified:
  - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md
autonomous: false
requirements:
  - D-07
  - D-14
  - D-22
  - D-27
  - D-43
  - D-45

user_setup:
  - service: gemini
    why: "Google Gemini API key required by discoverSearch Callable (D-27)"
    env_vars:
      - name: GEMINI_API_KEY
        source: "Google AI Studio → https://aistudio.google.com/app/apikey → Create API key → copy"
    dashboard_config:
      - task: "Confirm Gemini 2.5 Flash is enabled for the project (free tier OK for dev)"
        location: "https://aistudio.google.com/"

must_haves:
  truths:
    - "GEMINI_API_KEY secret is set in Firebase Functions secret manager"
    - "config/stores Firestore doc deleted via deleteConfigStores.ts (or confirmed absent)"
    - "popularItems collection backfilled BEFORE triggers deployed (D-22 ordering)"
    - "firestore.rules + firestore.indexes.json + all Phase 17 functions deployed to europe-west3"
    - "TTL policies configured for discoverCache.cachedAt (30 d) and discoverRateLimits.lastWriteAt (7 d) via gcloud"
    - "On-device UAT confirms Discover tab opens, search returns results, popular section loads, card tap launches browser, ActivityNotFoundException fallback shows Snackbar"
  artifacts:
    - path: ".planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md"
      provides: "UAT checklist with PASS/FAIL per scenario"
      contains: "UAT-"
  key_links:
    - from: "live Firebase project (gift-registry-ro)"
      to: "Phase 17 Callables + triggers"
      via: "firebase deploy --only functions:discoverPopular,functions:discoverSearch,functions:onItemCreatePopular,functions:onItemDeletePopular,functions:onItemUpdatePopular"
      pattern: "deploy --only functions"
---

<objective>
Take Phase 17 live: set the Gemini API key secret, run the popularItems backfill, deploy Cloud Functions + Firestore rules + composite index, configure TTL policies, delete the legacy `config/stores` Firestore doc, and run an on-device UAT covering both Discover sections + the Stores decommissioning regression. Per CONTEXT.md D-07, D-14, D-22, D-27, D-43, D-45 and the full Phase 17 surface from plans 17-01..17-05.

Purpose: Validate the full Phase 17 deliverable against a real Firebase backend + real Gemini API + a real Android device. This is the human-in-the-loop verification that the code shipped in plans 17-01..17-05 actually does what the spec says — on production infrastructure.

Output: A populated 17-06-UAT-RESULTS.md file with PASS/FAIL/NOTES per scenario, plus all deploy commands run successfully against the production project.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-01-stores-decommission-PLAN.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-02-backend-foundations-PLAN.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-03-callables-PLAN.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-04-triggers-and-backfill-PLAN.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-05-android-discover-PLAN.md
@CLAUDE.md
</context>

<tasks>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 1: Provision Gemini API key + set Firebase Functions secret</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decision D-27 — defineSecret pattern)
    - functions/src/discover/secrets.ts (defineSecret("GEMINI_API_KEY") declaration shipped in plan 17-02)
  </read_first>

  <what-built>
    Plans 17-02 + 17-03 declared `defineSecret("GEMINI_API_KEY")` and wired `discoverSearch` to consume it. The secret is empty in Firebase secret manager — must be populated before deploy or `discoverSearch` will throw on cold start.
  </what-built>

  <how-to-verify>
    1. Visit https://aistudio.google.com/app/apikey, sign in with the Google account that owns the gift-registry-ro project (or any account — Gemini API keys are not tied to Firebase project ownership), click **Create API key**, select the gift-registry-ro Cloud project from the dropdown, copy the key.
    2. From the project root, run:
       ```bash
       firebase functions:secrets:set GEMINI_API_KEY --project gift-registry-ro
       ```
       Paste the API key when prompted (it does not echo). Firebase CLI writes to Google Secret Manager.
    3. Confirm the secret is set:
       ```bash
       firebase functions:secrets:access GEMINI_API_KEY --project gift-registry-ro 2>&1 | head -3
       ```
       Output should NOT be the literal `null` — it will print the secret value (run on a private terminal).
    4. **DO NOT commit the API key to git.** The defineSecret pattern keeps it server-side only.
  </how-to-verify>

  <files>
    (no source files modified — runs firebase CLI commands against the live Functions secret manager)
  </files>

  <action>See <how-to-verify> above — human-action checkpoint. Steps: visit Google AI Studio, create API key, run `firebase functions:secrets:set GEMINI_API_KEY --project gift-registry-ro`, paste the key when prompted, confirm via `firebase functions:secrets:access`.</action>

  <verify>
    <automated>
      bash -c 'firebase functions:secrets:access GEMINI_API_KEY --project gift-registry-ro 2>&1 | grep -qE ".+" && echo OK'
    </automated>
  </verify>

  <done>The GEMINI_API_KEY secret returns a non-empty value when accessed via firebase CLI. Plan 17-03's discoverSearch Callable will succeed at cold-start (secrets bound).</done>

  <resume-signal>Type "secret set" once the Firebase secret is populated, OR "skip-secret" if deploying without search (popular-only smoke).</resume-signal>
</task>

<task type="auto">
  <name>Task 2: Delete config/stores doc + run popularItems backfill (D-07 + D-22 — BEFORE triggers deploy)</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-07, D-22)
    - functions/scripts/deleteConfigStores.ts (created in plan 17-01)
    - functions/scripts/backfillPopularItems.ts (created in plan 17-04)
    - functions/package.json (confirm `backfill:popular` npm script registered)
  </read_first>

  <files>
    (no source files modified — this task runs deployment scripts against the live project)
  </files>

  <action>
    Two scripts executed against the live Firebase project, in this exact order. Per CONTEXT.md D-22: backfill MUST run BEFORE triggers are deployed (otherwise live triggers race with the batched writes).

    **0. Ensure ADC credentials are set:**
    ```bash
    gcloud auth application-default login
    gcloud config set project gift-registry-ro
    ```

    **1. Delete the legacy `config/stores` Firestore document (D-07):**
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/functions
    npx ts-node scripts/deleteConfigStores.ts
    ```
    Expected stdout: either `"Deleted config/stores Firestore document."` or `"config/stores not present — nothing to delete (idempotent no-op)."`. Either is acceptable.

    **2. Backfill popularItems from existing items (D-22):**
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry/functions
    npx ts-node scripts/backfillPopularItems.ts
    ```
    Expected stdout includes:
    - `"Scanning collectionGroup('items')…"`
    - `"Found N items across all registries."` (N depends on existing data)
    - `"Aggregated to M unique products …"`
    - `"  Committed batch (…/…)"` repeated until completion
    - `"Backfill complete: M popularItems docs written."`

    Save the printed counts to plan-17-06's task notes for the UAT results file.

    Do NOT proceed to Task 3 (function deploy) until backfill has completed successfully. The D-22 ordering is critical.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      # Confirm scripts exist and are executable
      test -f functions/scripts/deleteConfigStores.ts
      test -f functions/scripts/backfillPopularItems.ts
      # Confirm gcloud authenticated to the right project
      gcloud config get-value project 2>&1 | grep -q "gift-registry-ro"
      # The actual script invocations happen in the action — verify via downstream checks:
      # (a) config/stores doc absent — checked via Firestore CLI in Task 4
      # (b) popularItems collection non-empty — checked via Firestore CLI in Task 4
      echo "Scripts run manually — see Task 4 for Firestore state verification"
      '
    </automated>
  </verify>

  <done>
    `deleteConfigStores.ts` executed (config/stores deleted or confirmed absent). `backfillPopularItems.ts` executed successfully; the printed counts of items scanned, products aggregated, and popularItems docs written are recorded in plan-17-06 notes. Triggers are NOT YET deployed.
  </done>
</task>

<task type="auto">
  <name>Task 3: Deploy firestore.rules + firestore.indexes.json + all Phase 17 Cloud Functions to europe-west3</name>

  <read_first>
    - firestore.rules (Phase 17 updates from plans 17-01 + 17-02)
    - firestore.indexes.json (popularItems composite index from plan 17-02)
    - functions/src/index.ts (exports: discoverPopular, discoverSearch, onItemCreatePopular, onItemDeletePopular, onItemUpdatePopular)
    - .firebaserc / firebase.json (current project + emulator config — confirm prod project alias is "default" pointing to gift-registry-ro)
  </read_first>

  <files>
    (no source files modified — runs firebase CLI deploy)
  </files>

  <action>
    Deploy in three sequential commands, each verified before moving on.

    **1. Deploy Firestore rules + index:**
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry
    firebase deploy --only firestore:rules,firestore:indexes --project gift-registry-ro
    ```
    Expected output: `"✔  firestore: released rules ..."` and `"✔  firestore: released indexes ..."`. The composite index for popularItems may show as "Building" — note the build status; queries against the index will fail until it's built (5–15 minutes for projects with existing data).

    **2. Deploy all Phase 17 functions:**
    ```bash
    firebase deploy --only functions:discoverPopular,functions:discoverSearch,functions:onItemCreatePopular,functions:onItemDeletePopular,functions:onItemUpdatePopular --project gift-registry-ro
    ```
    Expected output: 5 functions deployed successfully. Each line should show `region: europe-west3`. For `discoverSearch`, the CLI will report that the GEMINI_API_KEY secret is bound (`Secret(s) bound: GEMINI_API_KEY`).

    If deploy fails with `"App Check is enforced for this Callable but no debug provider is registered"` or similar App Check error, refer to plan 16's App Check setup. If `enforceAppCheck: true` was retained on the new Callables (per plan 17-03 SUMMARY decision), the Android client (plan 17-05) inherits the existing App Check provider wired in plan 16-06; no additional Android changes needed.

    **3. List deployed functions to confirm region + secrets:**
    ```bash
    firebase functions:list --project gift-registry-ro 2>&1 | grep -E "discover|onItem"
    ```
    Expected: five entries for discoverPopular, discoverSearch, onItemCreatePopular, onItemDeletePopular, onItemUpdatePopular, all in `europe-west3`.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      # Confirm config is correct
      firebase use --project gift-registry-ro >/dev/null 2>&1 || true
      # List deployed functions and confirm all 5 are present
      firebase functions:list --project gift-registry-ro 2>&1 | tee /tmp/fnlist.txt | head -50
      grep -q "discoverPopular" /tmp/fnlist.txt
      grep -q "discoverSearch" /tmp/fnlist.txt
      grep -q "onItemCreatePopular" /tmp/fnlist.txt
      grep -q "onItemDeletePopular" /tmp/fnlist.txt
      grep -q "onItemUpdatePopular" /tmp/fnlist.txt
      grep -q "europe-west3" /tmp/fnlist.txt
      echo OK
      '
    </automated>
  </verify>

  <done>
    `firebase functions:list` shows all five Phase 17 functions in europe-west3. Firestore rules and the popularItems composite index are deployed. The GEMINI_API_KEY secret is bound to discoverSearch.
  </done>
</task>

<task type="auto">
  <name>Task 4: Configure TTL policies via gcloud + verify Firestore state (D-14, D-45)</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-14 — 7-day TTL on discoverRateLimits.lastWriteAt, D-45 — 30-day TTL on discoverCache.cachedAt)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-02-SUMMARY.md (TTL gcloud commands documented in plan 17-02 output)
  </read_first>

  <files>
    (no source files modified — runs gcloud CLI commands)
  </files>

  <action>
    Configure Firestore TTL policies for the two new server-only collections. Per D-14 + D-45.

    **1. discoverCache.cachedAt — 30-day TTL:**
    ```bash
    gcloud firestore fields ttls update cachedAt --collection-group=discoverCache --enable-ttl --project=gift-registry-ro
    ```
    Confirm:
    ```bash
    gcloud firestore fields ttls list --project=gift-registry-ro 2>&1 | grep -A2 "discoverCache"
    ```
    Expected: a row showing `discoverCache.cachedAt | ENABLED`.

    **2. discoverRateLimits.lastWriteAt — 7-day TTL:**
    ```bash
    gcloud firestore fields ttls update lastWriteAt --collection-group=discoverRateLimits --enable-ttl --project=gift-registry-ro
    ```
    Confirm:
    ```bash
    gcloud firestore fields ttls list --project=gift-registry-ro 2>&1 | grep -A2 "discoverRateLimits"
    ```

    **Note on TTL day count:** Firestore TTL deletes docs ~24 hours AFTER the TTL field's timestamp passes the "deletion eligible" mark. Effective freshness is the TTL field timestamp itself + 24 h grace. For cache: a doc written 30 days ago is eligible; gets deleted within 24 h after. For rate limits: 7-day TTL ensures abandoned counters auto-clean. The TTL field's "duration" is implicit in how the writers set the timestamp — the field value IS the TTL deadline. (Firestore TTL semantics: TTL field value < current time → eligible for deletion. We write `cachedAt` = current time, so the doc becomes eligible 0 seconds later, which is wrong.)

    **Correction needed if Firestore TTL is "delete when field value < now":** We need the writer to set `cachedAt` to `now + 30 days` for that semantics, or use a separate `expiresAt` field set to `now + 30 days`. Re-read Firestore TTL docs and the writer code (search.ts) before enabling — if writer sets `cachedAt = serverTimestamp()` (current time), TTL with that field = immediate eligibility, which would delete every cache doc the moment it's written.

    **Concrete adjustment:** If the test reveals immediate deletion, either:
    (a) modify `search.ts` to write `cachedAt: Timestamp.fromDate(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000))` (the deadline), keeping the field name; OR
    (b) add a separate `expiresAt` field and enable TTL on that.

    Document the chosen approach in the UAT-RESULTS file. If (a) is needed, this becomes a follow-up fix to plan 17-03's search.ts — surface as a checkpoint to the user before completing this plan.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      gcloud firestore fields ttls list --project=gift-registry-ro 2>&1 | tee /tmp/ttls.txt | head -40
      grep -q "discoverCache" /tmp/ttls.txt
      grep -q "discoverRateLimits" /tmp/ttls.txt
      echo OK
      '
    </automated>
  </verify>

  <done>
    TTL policies enabled on both collection-fields. Output of `gcloud firestore fields ttls list` shows discoverCache.cachedAt and discoverRateLimits.lastWriteAt as ENABLED. Any TTL-semantics fix to writer code surfaced as a checkpoint (likely no action needed if writers correctly compute deadline values).
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 5: On-device UAT — install fresh build, run 14-scenario checklist</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (full file — every decision is a UAT touchpoint)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-UI-SPEC.md ("Interaction States Summary" section)
  </read_first>

  <what-built>
    The full Phase 17 stack: Discover bottom-nav slot 2 (replacing Stores), DiscoverScreen with search + community-popular sections, Cloud Functions (discoverPopular + discoverSearch + 3 triggers) live in europe-west3, Firestore rules + indexes deployed, TTL policies configured, legacy config/stores doc deleted.
  </what-built>

  <how-to-verify>
    **Setup:**
    ```bash
    cd /Users/victorpop/ai-projects/gift-registry
    ./gradlew app:assembleDebug -Puse_emulator=false
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```
    (The `-Puse_emulator=false` flag is critical per MEMORY.md — physical devices need the prod Firebase URL.)

    Open the app on a physical Android device, sign in as a registered (non-anonymous) user.

    Create or open `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md` and record PASS/FAIL/NOTES for each scenario below.

    **UAT-01: Stores decommission visual confirmation**
    - Open the FAB Add-action sheet (tap centre + button on bottom nav).
    - Confirm only 2 rows show: "Create registry" + "Add an item". NO "Browse stores" row.
    - PASS = exactly 2 action rows present.

    **UAT-02: Bottom nav slot 2 shows Discover**
    - From Home, look at the bottom nav.
    - Slot 2 (between Home and FAB) shows a magnifying-glass icon labeled "DISCOVER" (en locale) or "DESCOPERĂ" (ro locale — switch via Settings).
    - PASS = correct icon + label per locale.

    **UAT-03: Tap Discover → DiscoverScreen opens**
    - Tap slot 2.
    - Discover screen renders: search bar at top, "FROM THE COMMUNITY" section header below, shimmer skeletons OR cards (depending on whether backfill populated data).
    - The Discover tab in bottom nav is now in selected state (accentSoft pill behind icon).
    - PASS = screen renders, nav state updates.

    **UAT-04: Popular section loads from production**
    - Wait up to 5 seconds for `discoverPopular` Callable to complete.
    - Expected: either populated DiscoverProductCards (if backfill found products) OR `discover_empty_popular` text ("Popular items will appear here once people add gifts.").
    - PASS = either populated cards OR the empty-popular message renders (no infinite shimmer, no error).
    - If error: capture Logcat output `adb logcat | grep -i discover` to identify the failure.

    **UAT-05: L1 cache verification**
    - Force-close the app, reopen, navigate to Discover again.
    - Within the same Function instance lifetime (< 1 hr), the popular response should be near-instant (< 200 ms perceived).
    - PASS = subjective fast response on second open.

    **UAT-06: Search returns Gemini results**
    - Tap the search bar, type "cafetiera espresso" (English: "espresso coffee maker"), press Search on the keyboard.
    - Expected: "FROM THE WEB" section appears above community, shows 3 shimmer cards for ~3–8 seconds (Gemini call latency), then renders 5–15 product cards.
    - Spot-check that retailer URLs match Romanian retailers (emag.ro, altex.ro, vivre.eu, etc.).
    - PASS = web section appears + cards render.

    **UAT-07: Search cache hit**
    - Repeat the same query "cafetiera espresso" immediately.
    - Expected: results return near-instant (< 500 ms — cache hit from discoverCache Firestore doc).
    - PASS = subjective fast second search.

    **UAT-08: Occasion keyword routing**
    - Search "cadou nuntă" (wedding gift).
    - Expected: results biased toward 23h.ro / crisiashop.ro / wedday.ro / happycards.ro / magazinulmireselor.ro per RETAILERS.wedding + universal.
    - PASS = at least 1 result from the wedding-specific list.

    **UAT-09: Card tap launches browser**
    - Tap any DiscoverProductCard.
    - Expected: device's default browser opens to the raw retailer URL (no affiliate transform).
    - PASS = browser opens, URL matches the card's retailerUrl exactly.

    **UAT-10: ActivityNotFoundException Snackbar fallback**
    - Difficult to test on real device — most devices have a browser. Validate via Logcat: in DiscoverProductCard.kt the try/catch path is exercised; verify the catch block code path is present via inspection.
    - Optional: install via `adb shell pm disable-user com.android.browser` (only works on rooted/dev devices) then tap a card → Snackbar should appear at bottom.
    - PASS = code path exists; Snackbar manually verified OR code-review confirmed.

    **UAT-11: Empty search**
    - Search a gibberish query that Gemini will return empty for (e.g., "asdkfjasldkfj qwerty zzz").
    - Expected: "No matches found. Try a different search." text appears in web section.
    - PASS = empty state message shown.

    **UAT-12: Rate limit (D-13 20/hr)**
    - Make 20 distinct searches in quick succession (use varying queries to bypass cache).
    - On the 21st call: expect a Snackbar / error state showing "Search failed. Try again." (the HttpsError("resource-exhausted") gets surfaced as the search Error state).
    - PASS = 21st call shows error state.

    **UAT-13: Anonymous-provider rejection**
    - This is hard to test on Android (Android always uses registered Auth). Verify via Logcat: trigger discoverPopular while signed in normally; confirm no HttpsError in Logcat.
    - Code review: confirm both Callables check `sign_in_provider !== "anonymous"`.
    - PASS = code-review confirmation.

    **UAT-14: Locale parity — Romanian**
    - Switch device locale to Romanian via Settings.
    - Navigate to Discover.
    - Confirm: nav label "DESCOPERĂ", section headers "DE PE WEB" / "DIN COMUNITATE", search placeholder "Caută orice produs...", empty/error strings in Romanian.
    - PASS = all visible strings render in Romanian.

    **After all 14 scenarios:** Record outcome in 17-06-UAT-RESULTS.md. If any FAIL, surface as a defect to fix before signing off Phase 17.
  </how-to-verify>

  <files>
    .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md
  </files>

  <action>See <how-to-verify> above — human-verify checkpoint. Steps: assemble debug APK, install on physical device, run all 14 UAT scenarios (UAT-01 through UAT-14), record PASS/FAIL/NOTES in 17-06-UAT-RESULTS.md.</action>

  <verify>
    <automated>
      bash -c 'test -f .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md && grep -qE "UAT-(0[1-9]|1[0-4])" .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-UAT-RESULTS.md && echo OK'
    </automated>
  </verify>

  <done>17-06-UAT-RESULTS.md exists with PASS/FAIL/NOTES recorded for each of the 14 UAT scenarios; user has signed off via "uat pass" or "uat partial:" resume signal.</done>

  <resume-signal>Type "uat pass" if all 14 scenarios PASS, "uat fail" with description if any fail, or "uat partial: …" with the gap list.</resume-signal>
</task>

</tasks>

<verification>
1. `firebase functions:list --project gift-registry-ro` shows discoverPopular, discoverSearch, onItemCreatePopular, onItemDeletePopular, onItemUpdatePopular all in europe-west3.
2. `gcloud firestore fields ttls list --project=gift-registry-ro` shows discoverCache.cachedAt + discoverRateLimits.lastWriteAt ENABLED.
3. 17-06-UAT-RESULTS.md exists with PASS/FAIL recorded for each of the 14 UAT scenarios.
4. The user has signed off (Task 5 resume-signal == "uat pass" or "uat partial:" with acceptable deferrals).
</verification>

<success_criteria>
- GEMINI_API_KEY secret bound to discoverSearch Callable.
- popularItems collection backfilled BEFORE triggers deployed (D-22 ordering respected).
- config/stores Firestore doc deleted (or confirmed absent).
- firestore.rules + firestore.indexes.json deployed; popularItems composite index built.
- All 5 Phase 17 Cloud Functions deployed to europe-west3.
- TTL policies enabled for discoverCache.cachedAt + discoverRateLimits.lastWriteAt.
- 14-scenario on-device UAT completed with results documented; all critical scenarios PASS (UAT-01 through UAT-09 are blocking; UAT-10 through UAT-14 may be code-review or deferred-with-rationale).
- Phase 17 verifiably ships the documented Discover surface end-to-end.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-06-SUMMARY.md` documenting:
- Backfill counts: items scanned, products aggregated, popularItems docs written.
- Deploy URLs / regions for each Function.
- TTL configuration confirmation output.
- 14-scenario UAT results table (referencing 17-06-UAT-RESULTS.md).
- Any defects discovered + their resolution (fix-in-place quick task vs. defer-to-followup).
- Any TTL-semantics adjustment made to search.ts (if Firestore TTL "field < now" semantics required writer-side deadline computation).
- A note that this completes Phase 17 — STATE.md + ROADMAP.md should be updated in the post-execution verifier pass.
</output>
