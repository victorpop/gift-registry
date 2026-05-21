---
phase: quick/260521-myv-update-shareable-registry-link-from-r-re
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt
  - app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt
autonomous: true
requirements:
  - QUICK-260521-myv: "Android share/copy flows must produce https://gift-registry-ro.web.app/registry/{registryId} (replacing the old /r/{registryId})"

must_haves:
  truths:
    - "shareUrlOf(\"abc123\") returns \"https://gift-registry-ro.web.app/registry/abc123\" (not /r/abc123)"
    - "ShareBanner pill on RegistryDetailScreen displays \"gift-registry-ro.web.app/registry/{id}\""
    - "Tapping the ShareBanner pill copies https://gift-registry-ro.web.app/registry/{id} to clipboard and shares the same URL via ACTION_SEND"
    - "ShareUrlTest.kt asserts the /registry/ path (no test still pins /r/)"
    - "Unit tests pass after the change"
    - "grep \"/r/\" across app/src/main and app/src/test returns no production-code matches"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt"
      provides: "URL builder returning https://gift-registry-ro.web.app/registry/{id}"
      contains: "\"https://gift-registry-ro.web.app/registry/$registryId\""
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt"
      provides: "ShareBanner with updated display-format comment"
      contains: "gift-registry-ro.web.app/registry/"
    - path: "app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt"
      provides: "Tests pinning /registry/{id} format (5 tests, all updated)"
      contains: "/registry/abc123"
  key_links:
    - from: "ShareBanner.kt"
      to: "ShareUrl.kt::shareUrlOf"
      via: "val fullShareUrl = shareUrlOf(registryId)"
      pattern: "shareUrlOf\\(registryId\\)"
    - from: "ShareUrlTest.kt"
      to: "ShareUrl.kt::shareUrlOf"
      via: "JVM unit test (testDebugUnitTest)"
      pattern: "shareUrlOf\\("
---

<objective>
Replace the public shareable registry link prefix on Android from `/r/{registryId}` to `/registry/{registryId}` so the URL produced by the ShareBanner pill (and ACTION_SEND share intent / clipboard copy) matches the canonical web fallback route `https://gift-registry-ro.web.app/registry/{registryId}` that the React router (`web/src/App.tsx`) and Cloud Functions email templates (`functions/src/config/publicUrls.ts::buildRegistryUrl`) already use.

Purpose: Today the Android share flow emits `https://gift-registry-ro.web.app/r/{id}`, but the web app has no `/r/:id` route — it only defines `{ path: '/registry/:id', ... }`. Recipients of an Android-shared link land on the SPA wildcard route and see `NotFoundPage`. This breaks the core gift-giver flow.

Output: Android emits the canonical `/registry/{id}` URL. All other places (web routes, Cloud Functions invite emails, purchase emails, deep links) are already correct and unchanged. No web/functions changes, no redirect rule, no `firebase.json` change.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md

<investigation_findings>
<!-- Codebase grep performed during planning. Recording results here so the executor does not have to re-discover. -->

**Every `/r/` occurrence in non-test, non-node_modules code (find every place):**

| # | File | Line | Context |
|---|------|------|---------|
| 1 | `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt` | 8 | KDoc: "The web fallback router's /r/:id param matches this path exactly." |
| 2 | `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt` | 16 | Return string: `"https://gift-registry-ro.web.app/r/$registryId"` |
| 3 | `app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt` | 53 | Comment: `// Display without scheme prefix: "gift-registry-ro.web.app/r/abc"` |
| 4 | `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` | 10 | KDoc example: `shareUrlOf(registryId) == "https://gift-registry-ro.web.app/r/${registryId}"` |
| 5 | `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` | 12 | KDoc: "CONTEXT.md § Share banner locks the exact host + /r/ path." |
| 6 | `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` | 21 | `assertEquals("https://gift-registry-ro.web.app/r/abc123", shareUrlOf("abc123"))` |
| 7 | `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` | 30 | `assertTrue(shareUrlOf("my-registry-42").endsWith("/r/my-registry-42"))` |
| 8 | `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` | 35 | Comment: "...that would break the web fallback router's /r/:id param match." |

**Places that are ALREADY correct — DO NOT modify:**

- `web/src/App.tsx` — defines `{ path: '/registry/:id', element: <RegistryPage /> }` (line 11) and `/registry/:id/item/:itemId` (line 12). No `/r/:id` route exists. NotFoundPage handles `*`.
- `functions/src/config/publicUrls.ts::buildRegistryUrl(registryId)` — already returns `${DEFAULT_BASE_URL}/registry/${registryId}` (used by `inviteToRegistry.ts` line 95 and the purchase email at `functions/src/notifications/onPurchaseNotification.ts`).
- `functions/src/email/templates/invite.ts`, `functions/src/email/templates/purchase.ts` — render `registryUrl` from the value passed by Functions; already `/registry/{id}` (verified by `functions/src/__tests__/emailTemplates.test.ts` lines 64, 106).
- `app/src/main/AndroidManifest.xml` — App Link intent-filter uses `android:host="giftregistry.app"` + `android:pathPrefix="/registry/"` (lines 25-28). Different host (not `gift-registry-ro.web.app`), and the prefix is already `/registry/`. No change.
- `hosting/public/.well-known/assetlinks.json` — Android-app-link delegation only; no URL paths.
- `firebase.json` — single SPA rewrite `**` → `/index.html`. No `/r/` rewrite.
- `hosting/public/assets/index-*.js` — built bundle, has zero `/r/` matches; will be re-built by `npm run build` whenever web is next deployed (out of scope here).

**Redirect decision (explicit, per constraints):**

NO redirect needed. The web app never had a `/r/:id` route (`web/src/App.tsx` only registers `/registry/:id`). Any link of the form `https://gift-registry-ro.web.app/r/{id}` shared by the Android app to date has already been broken — it falls into the `*` wildcard and renders `NotFoundPage`. So:

- We will not add a `/r/:id` → `/registry/:id` redirect in `firebase.json`.
- We will not add a `/r/:id` route in `web/src/App.tsx`.
- The fix is purely Android-side: emit the correct URL going forward. Any users who saved an `/r/{id}` link in their history will need to be re-shared — but since those links never worked, this is not a regression.

**Test files that pass through (no change needed):**

- `functions/src/__tests__/publicUrls.test.ts` and `functions/src/__tests__/emailTemplates.test.ts` already assert `/registry/{id}` (verified).
- Web router tests in `web/src/__tests__/App.test.tsx`, `web/src/features/registry/__tests__/RegistryPage.test.tsx`, etc., all use `/registry/:id` paths.

**No checkpoints in this plan.** Pure code change, fully verifiable by unit test (`./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.ShareUrlTest"`) and a final grep. Manual on-device verification (open RegistryDetailScreen, tap share pill, confirm the URL the OS share sheet shows now reads `gift-registry-ro.web.app/registry/...`) is recommended after merge but is NOT a gating step in this plan.
</investigation_findings>

<interfaces>
<!-- Existing contract — unchanged by this plan -->

```kotlin
// app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt
fun shareUrlOf(registryId: String): String  // signature stays the same; only return-value path segment changes
```

```kotlin
// app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt (consumer, unchanged signature)
@Composable internal fun ShareBanner(
    registryId: String,
    onShared: () -> Unit,
    modifier: Modifier = Modifier,
)
```

```typescript
// functions/src/config/publicUrls.ts (already produces /registry/{id}; reference only — DO NOT touch)
export function buildRegistryUrl(registryId: string): string
// returns `https://gift-registry-ro.web.app/registry/${registryId}`
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Flip ShareUrl.kt + ShareUrlTest.kt from /r/ to /registry/ (RED→GREEN in one atomic change)</name>
  <files>
    app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt,
    app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt
  </files>
  <behavior>
    Test expectations after the change (these must all pass on `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.ShareUrlTest"`):
    - `returnsCorrectHostAndPath`: `shareUrlOf("abc123") == "https://gift-registry-ro.web.app/registry/abc123"`
    - `usesHttpsScheme`: `shareUrlOf("abc123").startsWith("https://")` — unchanged
    - `usesCanonicalHost`: `shareUrlOf("abc123").contains("gift-registry-ro.web.app")` — unchanged
    - `placesRegistryIdAfterRegistrySegment` (renamed from `placesRegistryIdAfterRSegment`): `shareUrlOf("my-registry-42").endsWith("/registry/my-registry-42")`
    - `doesNotUrlEncodeRegistryId`: input `"abc 123"` → result contains literal `"abc 123"` — unchanged behavior, but update the inline comment to refer to `/registry/:id` not `/r/:id`
  </behavior>
  <action>
    Make both edits in this single task — tests and production code flip together (we are intentionally NOT leaving a red-bar window, because both files are part of the same atomic semantic change and the test file is the authoritative spec for the URL format).

    **Edit 1 — `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt`:**
    1. Line 10 (KDoc): change `"https://gift-registry-ro.web.app/r/${registryId}"` to `"https://gift-registry-ro.web.app/registry/${registryId}"`.
    2. Line 12 (KDoc): change `"CONTEXT.md § Share banner locks the exact host + /r/ path."` to `"CONTEXT.md § Share banner locks the exact host + /registry/ path."`.
    3. Line 21 (assertion in `returnsCorrectHostAndPath`): change `"https://gift-registry-ro.web.app/r/abc123"` to `"https://gift-registry-ro.web.app/registry/abc123"`.
    4. Line 29 (function name): rename `placesRegistryIdAfterRSegment` → `placesRegistryIdAfterRegistrySegment`. (Optional but clarifies the test name now that the prefix isn't `/r/` anymore.)
    5. Line 30 (assertion inside that test): change `.endsWith("/r/my-registry-42")` to `.endsWith("/registry/my-registry-42")`.
    6. Line 35 (comment inside `doesNotUrlEncodeRegistryId`): change `"...break the web fallback router's /r/:id param match."` to `"...break the web fallback router's /registry/:id param match."`.

    **Edit 2 — `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt`:**
    1. Line 8 (KDoc): change `"The web fallback router's /r/:id"` to `"The web fallback router's /registry/:id"`.
    2. Line 16 (return string): change `"https://gift-registry-ro.web.app/r/$registryId"` to `"https://gift-registry-ro.web.app/registry/$registryId"`.

    **Do not change** the function signature `fun shareUrlOf(registryId: String): String` — only the path segment in the returned string and the surrounding doc comments.

    **Do not** add URL-encoding, do not change the host, do not change the scheme, do not introduce a `BuildConfig`/`Env` indirection (out of scope; the existing code is a single hardcoded constant and that pattern is preserved).
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.ShareUrlTest"</automated>
  </verify>
  <done>
    All 5 tests in `ShareUrlTest` pass. `shareUrlOf("abc123")` returns `"https://gift-registry-ro.web.app/registry/abc123"`. Grep confirms `/r/` no longer appears in `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt` nor in `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt`.
  </done>
</task>

<task type="auto">
  <name>Task 2: Update ShareBanner.kt display-format comment + final repo-wide /r/ grep sweep</name>
  <files>app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt</files>
  <action>
    **Edit — `app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt`:**
    1. Line 53 (comment above `val displayUrl = fullShareUrl.removePrefix("https://")`): change `// Display without scheme prefix: "gift-registry-ro.web.app/r/abc"` to `// Display without scheme prefix: "gift-registry-ro.web.app/registry/abc"`.

    No code logic change in this file — only the comment. The displayed URL on the share pill is derived at runtime by `shareUrlOf(registryId).removePrefix("https://")`, so once Task 1 flips `ShareUrl.kt`, this component will automatically render `gift-registry-ro.web.app/registry/{id}` on the pill. We are only fixing the stale comment so future readers aren't misled.

    **Then perform a final repo sweep to confirm completeness:**

    Run the following two greps and confirm both return only acceptable matches:

    Grep 1 — production code (must be empty):
    ```
    grep -rn '"https://gift-registry-ro.web.app/r/\|web.app/r/' \
      app/src/main app/src/test web/src functions/src \
      --include="*.kt" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx" --include="*.xml"
    ```
    Expected: zero matches.

    Grep 2 — historical planning docs (matches are EXPECTED and FINE — these are archived plans/summaries describing the prior state; do not edit them):
    ```
    grep -rn '/r/' .planning/phases/ .planning/quick/ 2>/dev/null | grep -v node_modules | head
    ```
    Expected: matches in `.planning/phases/11-*` and the older 260507-veb quick-task — these are historical records and MUST NOT be rewritten.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug && grep -rn '"https://gift-registry-ro.web.app/r/' app/src/main app/src/test web/src functions/src --include='*.kt' --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.xml' ; test $? -eq 1</automated>
  </verify>
  <done>
    `ShareBanner.kt` comment now reads `gift-registry-ro.web.app/registry/abc`. The repo-wide grep for the literal share-URL pattern returns zero matches in production code (`app/src/main`, `app/src/test`, `web/src`, `functions/src`). The Android debug build still compiles cleanly. Historical `.planning/` references to `/r/` are left untouched (they are part of the project's record).
  </done>
</task>

</tasks>

<verification>

After both tasks complete, the executor should perform this final cross-check (one shell block):

```bash
# 1. Unit test — Android share URL builder
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.ShareUrlTest"

# 2. Production code is /r/-free
grep -rn '"https://gift-registry-ro.web.app/r/\|web.app/r/' \
  app/src/main app/src/test web/src functions/src \
  --include="*.kt" --include="*.ts" --include="*.tsx" --include="*.js" --include="*.jsx" --include="*.xml"
# expect: no matches; exit code 1

# 3. The new format is present in production code
grep -rn '"https://gift-registry-ro.web.app/registry/' \
  app/src/main app/src/test functions/src \
  --include="*.kt" --include="*.ts"
# expect: at least 1 match in ShareUrl.kt, 1+ in ShareUrlTest.kt, plus the existing Functions occurrences in publicUrls.ts

# 4. Android debug build still compiles
./gradlew :app:assembleDebug
```

Manual smoke (optional, NOT a gate; recommended after merge):
- Launch the debug APK, open a registry as its owner → RegistryDetailScreen → tap the share-banner pill → confirm the system share sheet preview and the clipboard now read `https://gift-registry-ro.web.app/registry/{id}` (not `/r/{id}`).
- Paste the copied URL into Chrome → it should load the RegistryPage (not NotFoundPage).

</verification>

<success_criteria>

- `shareUrlOf("abc123") == "https://gift-registry-ro.web.app/registry/abc123"` (was `/r/abc123`).
- 5 of 5 tests in `ShareUrlTest` pass.
- `./gradlew :app:assembleDebug` succeeds.
- Production-code grep for `web.app/r/` returns zero matches in `app/src/main`, `app/src/test`, `web/src`, `functions/src`.
- ShareBanner display comment matches the new format.
- No changes to `web/`, `functions/`, `firebase.json`, `AndroidManifest.xml`, or `hosting/` (verified by `git status` showing only the three intended files modified).
- No redirect added (explicit decision — see investigation_findings § Redirect decision).

</success_criteria>

<output>
After completion, create `.planning/quick/260521-myv-update-shareable-registry-link-from-r-re/260521-myv-SUMMARY.md` capturing:
- Files modified (the three Android files)
- Test result (5 ShareUrlTest tests passing)
- Final grep proof
- Why no web/functions/firebase.json change was needed (already canonical)
- Why no `/r/` → `/registry/` redirect was added (no historical `/r/:id` route ever existed on the web)
- Recommended (optional) manual smoke step left to the user
</output>
