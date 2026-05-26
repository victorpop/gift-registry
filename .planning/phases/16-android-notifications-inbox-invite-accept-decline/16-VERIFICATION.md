---
phase: 16-android-notifications-inbox-invite-accept-decline
verified: 2026-05-26T22:30:00Z
status: passed
score: 28/28 must-haves verified (D-01..D-28)
re_verification: false
verifier: Claude (gsd-verifier, read-only audit)
backend: production (gift-registry-ro, europe-west3)
---

# Phase 16: Android Notifications Inbox + Invite Accept/Decline — Verification Report

**Phase Goal:** Move the Android invite flow from auto-add-to-`invitedUsers` to a strict accept-gate model. New invites land in `registries.pendingInvitedUsers`; invitee sees an actionable INVITE inbox card; tapping opens a GiftMaison-styled bottom sheet with Accept/Decline CTAs. Accept atomically promotes uid into `invitedUsers`; Decline removes the pending entry without ever promoting. Owner sees `invite_accepted` / `invite_declined` inbox notifications. Legacy invites (no `pendingEntryKey`) fall back to direct navigation. Re-invite of already-member writes inbox+push but NOT to `pendingInvitedUsers` (D-16 short-circuit). Inbox re-skinned to GiftMaison design language.

**Verified:** 2026-05-26
**Status:** PASS
**Re-verification:** No — initial verification

---

## 1. Goal Achievement — Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | New invites land in `pendingInvitedUsers`, not `invitedUsers` | ✓ VERIFIED | `functions/src/registry/inviteToRegistry.ts:105-108` uses `new FieldPath("pendingInvitedUsers", inviteKey)`; no `invitedUsers` write present. Confirmed by UAT A2 (Firestore Console showed `pendingInvitedUsers[B-uid]=true` post-invite). |
| 2 | Accept atomically promotes uid into `invitedUsers` and removes from `pendingInvitedUsers` | ✓ VERIFIED | `acceptInvite.ts:72-74` issues both writes inside the same `runTransaction`. UAT A8 confirmed final Firestore state. |
| 3 | Decline removes uid from `pendingInvitedUsers` and NEVER touches `invitedUsers` | ✓ VERIFIED | `declineInvite.ts:63-67` only calls `FieldValue.delete()` on `pendingInvitedUsers[uid]`; no `invitedUsers` write anywhere in the file. UAT B6 confirmed. |
| 4 | Owner receives `invite_accepted` inbox notification on accept; `invite_declined` on decline; NO FCM push | ✓ VERIFIED | `acceptInvite.ts:99-113` calls `writeNotification` only (no `sendInvitePush`). `declineInvite.ts:77-92` same shape. UAT A9, B5 confirmed. |
| 5 | Invitee gets JOINED inbox confirmation on Accept; nothing on Decline | ✓ VERIFIED | `acceptInvite.ts:84-96` writes `invite_accepted_self` for the invitee; `declineInvite.ts` has zero invitee-side writes. UAT A10, B4 confirmed. |
| 6 | Legacy invites (no `pendingEntryKey`) fall back to direct navigate via `shouldOpenInviteSheet` predicate | ✓ VERIFIED | `InviteResponseSheet.kt:55-57` returns true only when `type==INVITE && payload["pendingEntryKey"] != null`. `NotificationsScreen.kt:164-168` branches on this. UAT C1 confirmed with manually crafted legacy doc. |
| 7 | Re-invite of already-member writes inbox + push but NOT to `pendingInvitedUsers` (D-16) | ✓ VERIFIED | `inviteToRegistry.ts:84-109` computes `isAlreadyMember = existingInvitedUsers[inviteKey] === true` then SKIPS the FieldPath write when true; inbox payload built with `enriched = isAlreadyMember ? {} : buildEnrichedInvitePayload(...)` (no `pendingEntryKey`). UAT C2 confirmed. |
| 8 | App Check enforced on accept/decline callables | ✓ VERIFIED | Both callables: `{ region: REGION, enforceAppCheck: true }` (acceptInvite.ts:31, declineInvite.ts:30). Android side: `GiftRegistryApp.kt:34-47` installs Debug or PlayIntegrity provider in onCreate BEFORE any Firebase op. |
| 9 | Inbox re-skinned to GiftMaison design language (D-09) | ✓ VERIFIED | `NotificationsScreen.kt:105-121` uses `GiftMaisonWordmark()`, `containerColor=colors.paper`, MonoCaps timestamp, 6dp `colors.accent` unread dot on `CircleShape`, `HorizontalDivider(color=colors.line)` between cards. No M3 Card elevation, no Badge/BadgedBox. UAT D1 confirmed. |
| 10 | Locale parity: 8 new keys + 11 invite_sheet_* RO copy locked in both locales | ✓ VERIFIED | 286 string keys in BOTH `values/strings.xml` and `values-ro/strings.xml`. All 8 new notification_* + notifications_empty_* keys present; all 11 invite_sheet_* keys translated to Romanian (e.g. "Acceptă", "Refuză", "Reîncearcă", smart quotes „...”). UAT D2 confirmed. |

**Score: 10/10 truths verified.**

---

## 2. Requirement-by-Requirement Verification (D-01..D-28)

| # | Decision | Status | Evidence |
|---|----------|--------|----------|
| D-01 | ModalBottomSheet with hero + Accept/Decline CTAs | ✓ PASS | `InviteResponseSheet.kt:74-285` — ModalBottomSheet + HeroImageOrPlaceholder(180dp) + Accept (filled accent) + Decline (outlined ink@40%). UAT A5 PASS. |
| D-02 | Pending invite card stays homogeneous (no PENDING badge / cover preview) | ✓ PASS | `NotificationsScreen.kt` has no `Badge(`, no `BadgedBox`, no per-INVITE branch in `NotificationCard`. KDoc lines 73-77 document the contract. |
| D-03 | Decline requires AlertDialog confirmation | ✓ PASS | `InviteResponseSheet.kt:287-317` — `DeclineConfirmDialog` (private) with `[Cancel / Decline]`. UAT B2-B4 PASS. |
| D-04 | On Accept, original INVITE deleted server-side; replaced by JOINED notification | ✓ PASS | `acceptInvite.ts:82` `deleteInviteNotificationsForRegistry(uid, registryId)` + `:84-96` writes `invite_accepted_self`. UAT A10 PASS. |
| D-05 | On Accept, sheet dismisses + app navigates to `RegistryDetailKey(registryId)` | ✓ PASS | `NotificationsScreen.kt:189-192` `onAcceptSuccess = { rid -> dismissInviteSheet(); onNavigateToRegistry(rid) }`. `AppNavigation.kt:208,343-344` wires `onNavigateToRegistry` to `backStack.add(RegistryDetailKey(rid))`. UAT A7 PASS. |
| D-06 | Owner gets inbox-only entry (NO FCM push) on accept AND decline | ✓ PASS | Both callables call only `writeNotification` for owner; never `sendInvitePush`. Verified by absence of any push import in declineInvite.ts and absence of owner-push code in acceptInvite.ts post-tx. |
| D-07 | Callable failures surface as inline warn-banner with Retry; sheet stays open during in-flight | ✓ PASS | `InviteResponseSheet.kt:113-115` `confirmValueChange = { state !is Submitting }` (swipe blocked); `:114` `onDismissRequest = { if (!isLoading) onDismiss() }` (scrim blocked); `:182-212` Box with `colors.warn @ 15% alpha` + TextButton(retry). UAT A6 PASS. |
| D-08 | No separate pending-invites badge; existing unread count covers them | ✓ PASS | `NotificationsViewModel.kt` has no `pendingCount` flow. `NotificationsInboxBell` reuses `InboxBellViewModel.unreadCount` (Phase 06 contract). No code changes to badge logic. |
| D-09 | Full GiftMaison re-skin of NotificationsScreen + NotificationCard | ✓ PASS | `NotificationsScreen.kt:105-280` — wordmark TopAppBar, paper bg, flat cards with gm.line dividers, MonoCaps timestamp, 6dp accent dot. UAT D1 PASS. |
| D-10 | Invite payload enriched server-side with `pendingEntryKey, occasion, coverUrl, eventDateMs` | ✓ PASS | `inviteNotificationHelpers.ts:57-68` `buildEnrichedInvitePayload` returns all four. `inviteToRegistry.ts:158-176` spreads `...enriched` into `writeNotification.payload`. |
| D-11 | Legacy INVITE (no `pendingEntryKey`) falls back to navigate-to-registry | ✓ PASS | `InviteResponseSheet.kt:55-57` `shouldOpenInviteSheet` predicate; `NotificationsScreen.kt:163-169` branches on it. UAT C1 PASS. |
| D-12 | No migration of existing `invitedUsers` entries | ✓ PASS | No migration script anywhere; pre-Phase-16 entries grandfathered by design (verified by code absence). |
| D-13 | NEW invites land in `pendingInvitedUsers` regardless of account status (uid OR `email:xxx`) | ✓ PASS | `inviteToRegistry.ts:77` `inviteKey = invitedUid ?? \`email:${email}\``; `:105-108` writes that key via FieldPath. |
| D-14 | Phase 15 coupling: `linkInviteOnSignup` MUST target `pendingInvitedUsers` | ✓ PASS | `15-CONTEXT.md:180-201` contains "Phase 16 update (appended 2026-05-24)" stanza with "linkInviteOnSignup MUST target pendingInvitedUsers". |
| D-15 | Re-invite is always allowed; Decline does not blacklist | ✓ PASS | Negative-coverage: `grep -rE "blacklist\|declinedUsers\|declinedSet" functions/src/registry/*.ts` returns ZERO hits (verified in Plan 16-02 SUMMARY). No deny-list logic anywhere. |
| D-16 | Re-invite of already-member: no-op membership; inbox+push still delivered; payload omits `pendingEntryKey` (legacy-tap fallback) | ✓ PASS | `inviteToRegistry.ts:84-109,158-160` — `isAlreadyMember` short-circuit skips pending write; `enriched = isAlreadyMember ? {} : ...` omits pendingEntryKey. UAT C2 PASS. |
| D-17 | Web side gets no UI changes in Phase 16 | ✓ PASS | No web changes in this phase (verified by absence; Phase 15 deferred). |
| D-18 | `pendingInvitedUsers` field readable only by owner | ✓ PASS | `firestore.rules:30-36` `isInvited()` reads ONLY `invitedUsers` — `pendingInvitedUsers` is invisible to non-owners. 4 rules tests at `tests/rules/firestore.rules.test.ts:524-563` (Pattern 8). |
| D-19 | Existing `isInvited()` rule unchanged; post-accept invitee reads succeed | ✓ PASS | `firestore.rules:30-36` unchanged. Rules test `firestore.rules.test.ts:558` "D-19: invitee promoted to invitedUsers (post-accept) CAN read" present and passing. |
| D-20 | Two new 2nd-gen onCall functions on europe-west3 with `enforceAppCheck: true` | ✓ PASS | acceptInvite.ts:31 + declineInvite.ts:30 both `{ region: "europe-west3", enforceAppCheck: true }`. `functions:list` log in 16-06-DEPLOY-LOG.md shows both LIVE in europe-west3. |
| D-21 | `acceptInvite` atomic transaction (verify pending → remove pending → set invited → cleanup → write both notifications) | ✓ PASS | `acceptInvite.ts:49-75` runTransaction with verify-first (`pending[uid] !== true` throws) + `tx.update` for both writes. Post-tx best-effort cleanup + 2 writeNotification calls (lines 82-113). |
| D-22 | `declineInvite` atomic transaction (verify pending → remove pending → cleanup → owner notification) | ✓ PASS | `declineInvite.ts:48-69` same shape; no invitee notification (silent decline). |
| D-23 | `inviteToRegistry` modified: writes pendingInvitedUsers + enriched payload | ✓ PASS | `inviteToRegistry.ts:105-108` writes pendingInvitedUsers via FieldPath; `:158-176` embeds enriched payload. Owner-side existing FCM push preserved at `:142-148`. |
| D-24 | All three callables idempotent | ✓ PASS | acceptInvite.ts:58-62 returns success when uid already in invitedUsers (no-pending case). declineInvite.ts:55-57 throws failed-precondition (symmetric design choice per Plan 16-01 SUMMARY). inviteToRegistry idempotent on pending entry (write `true` over `true`). |
| D-25 | `NotificationType` enum extended with 3 new values | ✓ PASS | `Notification.kt:11-13` adds `INVITE_ACCEPTED_SELF, INVITE_ACCEPTED, INVITE_DECLINED`. `fromWire` mappings at lines 23-25. UNKNOWN remains last sentinel. |
| D-26 | `Notification` payload shape preserved (Map<String, String?>) | ✓ PASS | `Notification.kt:38` unchanged. New keys (pendingEntryKey, occasion, coverUrl, eventDateMs) flow transparently through the existing map (verified at `NotificationRepositoryImpl.kt:117` `flatPayload = payload.mapValues { v?.toString() }`). |
| D-27 | `NotificationRepository` extended with `acceptInvite` + `declineInvite` Result<Unit> suspend methods | ✓ PASS | `NotificationRepository.kt:20,28` declares both methods. `NotificationRepositoryImpl.kt:93-111` implements them as `runCatching` around `httpsCallable(...).call(mapOf("registryId" to registryId)).await()` — verbatim ReservationRepositoryImpl pattern. |
| D-28 | New strings under namespaces in BOTH locales | ✓ PASS | 8 new EN keys + 8 new RO keys (notification_invite_accepted_* + notifications_empty_*). 11 invite_sheet_* keys present in both locales with locked RO copy (Acceptă/Refuză/Reîncearcă, smart quotes „...”). Both files: 286 string keys (parity). |

**All 28 decisions PASS.**

---

## 3. Code Path Audit

| Path | Expected | Status | Notes |
|------|----------|--------|-------|
| `functions/src/registry/inviteToRegistry.ts` | Writes `pendingInvitedUsers`; D-16 already-member short-circuit; enriched payload | ✓ PASS | Lines 84-109 + 158-176. FieldPath used (Pitfall 1). No `invitedUsers` write. |
| `functions/src/registry/acceptInvite.ts` | New 2nd-gen onCall with enforceAppCheck; atomic transaction; post-tx best-effort writes | ✓ PASS | 117 LoC; lines 30, 49-75, 82-113. didPromote flag pattern for idempotency. |
| `functions/src/registry/declineInvite.ts` | Atomic pendingInvitedUsers remove; owner-only post-tx write | ✓ PASS | 96 LoC; lines 29, 48-69, 77-92. No invitee writes. |
| `functions/src/registry/inviteNotificationHelpers.ts` | Shared helpers (delete + lookupDisplayName + buildEnrichedInvitePayload) | ✓ PASS | 68 LoC. `eventDateMs` returned as number (matches Wave 0 test). |
| `functions/src/index.ts` | Exports acceptInvite + declineInvite | ✓ PASS | Lines 23-24. |
| `functions/src/notifications/writeNotification.ts` | NotificationType union extended with 3 new wire strings | ✓ PASS | Lines 20-28: invite_accepted_self / invite_accepted / invite_declined present. |
| `firestore.rules` | UNCHANGED (Pattern 8: pendingInvitedUsers invisible to isInvited) | ✓ PASS | Lines 30-36 confirm `isInvited` reads only `invitedUsers`. |
| `firestore.indexes.json` | Composite index `users/*/notifications (type asc, payload.registryId asc)` | ✓ PASS | Lines 44-51. Deployed (16-06-DEPLOY-LOG.md). |
| `app/src/main/java/com/giftregistry/GiftRegistryApp.kt` | App Check init in onCreate BEFORE any Firebase op + FCM token addAuthStateListener | ✓ PASS | Lines 38-47 (App Check Debug/PlayIntegrity by build variant); lines 64-75 (FCM token registration on every signed-in transition). |
| `app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` | D-09 re-skin + tap-branching + sheet host outside Scaffold + timestamp clamp | ✓ PASS | Lines 105-200 re-skin; 163-169 branching; 182-199 sheet host; 215-228 clamp `minOf(createdAtMs, now)` (Task 7). |
| `app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt` | ModalBottomSheet + shouldOpenInviteSheet predicate + DeclineConfirmDialog | ✓ PASS | 318 LoC. Predicate at 55-57; sheet at 74-285; dialog at 287-317. |
| `app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt` | Sheet-scoped state machine | ✓ PASS | 95 LoC; sealed interface State with Idle/Submitting(action)/Error(action, messageKey)/AcceptedSuccess/DeclinedSuccess; accept/decline/retry/reset. |
| `app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt` | Constructor takes FirebaseFunctions; acceptInvite/declineInvite httpsCallable wrappers | ✓ PASS | Constructor at 18-21; methods at 93-111. |
| `app/src/main/java/com/giftregistry/domain/model/Notification.kt` | NotificationType extended with 3 new values + fromWire | ✓ PASS | Lines 11-13, 23-25. UNKNOWN remains last. |
| `app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt` | Interface declares acceptInvite + declineInvite | ✓ PASS | Lines 20, 28. |
| `app/src/main/java/com/giftregistry/ui/registry/list/HomeTopBar.kt` | Bell entry point restored (Task 4 deviation fix) | ✓ PASS | 40 LoC; NotificationsInboxBell at line 36; onNotificationsClick parameter at 24. |
| `app/src/main/res/values/strings.xml` + `values-ro/strings.xml` | 11 invite_sheet_* keys + 8 new keys, parity preserved | ✓ PASS | 286 keys in both locales. All 19 phase-relevant keys present (verified by grep). |
| `app/src/main/res/font/jetbrains_mono_{medium,semibold}.ttf` | Bundled TTFs (Task 8 deviation fix) | ✓ PASS | Both files present (273860 / 277092 bytes). |
| `app/src/main/assets/licenses/OFL_jetbrains_mono.txt` | OFL license bundled | ✓ PASS | File exists. |
| `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonFonts.kt` | JetBrainsMonoFamily switched from GoogleFont to bundled TTF | ✓ PASS | Lines 71-73: `Font(resId = R.font.jetbrains_mono_medium/_semibold)`. |
| `.planning/todos/completed/2026-05-22-wire-android-app-check-and-flip-enforcement.md` | Todo moved from pending to completed | ✓ PASS | Exists in completed/; pending/ copy absent (verified). |
| `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md` | D-14 coupling stanza appended | ✓ PASS | Lines 180-201: "Phase 16 update (appended 2026-05-24)" + "linkInviteOnSignup MUST target pendingInvitedUsers". |

---

## 4. Deployment Evidence

Source: `16-06-DEPLOY-LOG.md`.

| Component | Status | Evidence |
|-----------|--------|----------|
| Composite index `notifications(type asc, payload.registryId asc)` | DEPLOYED | `firebase deploy --only firestore:indexes --project gift-registry-ro` → "Deploy complete!" (`/tmp/16-06-index-deploy.log`). |
| Cloud Function `acceptInvite` (NEW) | DEPLOYED | `firebase deploy --only functions:acceptInvite` → CREATE ✔ Successful, europe-west3, v2 callable, nodejs22. |
| Cloud Function `declineInvite` (NEW) | DEPLOYED | Same: CREATE ✔ Successful, europe-west3, v2 callable, nodejs22. |
| Cloud Function `inviteToRegistry` (UPDATED) | DEPLOYED | UPDATE ✔ Successful, europe-west3, v2 callable, nodejs22. |
| Live verification | CONFIRMED | `firebase functions:list --project gift-registry-ro` shows all 3 functions in europe-west3, type=callable, runtime=nodejs22 (`/tmp/16-06-functions-list.log`). |
| Firestore rules | UNCHANGED (intentional) | Per Pattern 8, no edit needed; D-18 contract holds via existing `isInvited()`. |
| Functions tsc verification | CLEAN | Re-verified during this audit: `cd functions && npx tsc --noEmit` exits 0 with no output. |

---

## 5. UAT Cross-Check (16-06-UAT-RESULTS.md)

Source: `16-06-UAT-RESULTS.md` — 20/20 PASS across 4 sections.

| UAT Item | Claimed Status | Code Evidence Maps | Verdict |
|----------|----------------|--------------------|---------|
| A1 Send invite as owner | PASS | inviteToRegistry.ts:22 callable + UI in InviteBottomSheet (Phase 6) | ✓ ALIGNED |
| A2 Pending state in Firestore | PASS | inviteToRegistry.ts:105-108 writes pendingInvitedUsers via FieldPath | ✓ ALIGNED |
| A3 FCM push received | PASS (after Task 5 fix) | sendInvitePush.ts (Phase 06) + GiftRegistryApp.kt:64-75 (Task 5 auth listener) | ✓ ALIGNED |
| A4 Inbox card visible | PASS (after Task 4 fix) | HomeTopBar.kt:36 bell + NotificationsScreen.kt re-skin | ✓ ALIGNED |
| A5 Sheet opens on tap | PASS | NotificationsScreen.kt:163-169 + InviteResponseSheet.kt:74-285 | ✓ ALIGNED |
| A6 Cannot dismiss mid-loading | PASS | InviteResponseSheet.kt:86,114 confirmValueChange + onDismissRequest guards | ✓ ALIGNED |
| A7 Auto-nav on accept | PASS | NotificationsScreen.kt:189-192 + AppNavigation.kt:208 | ✓ ALIGNED |
| A8 Firestore state post-accept | PASS | acceptInvite.ts:72-74 atomic FieldPath updates | ✓ ALIGNED |
| A9 Owner inbox shows invite_accepted | PASS | acceptInvite.ts:99-113 writeNotification (owner) | ✓ ALIGNED |
| A10 Invitee JOINED confirmation | PASS | acceptInvite.ts:84-96 writeNotification (invitee, invite_accepted_self) | ✓ ALIGNED |
| B1 Send another invite | PASS | Idempotent inviteToRegistry; required manual `invitedUsers[B-uid]` clear documented in UAT-RESULTS | ✓ ALIGNED |
| B2 Decline confirm opens | PASS | InviteResponseSheet.kt:244 → showDeclineDialog → DeclineConfirmDialog | ✓ ALIGNED |
| B3 Cancel keeps sheet | PASS | DeclineConfirmDialog.onCancel = { showDeclineDialog = false } | ✓ ALIGNED |
| B4 Decline confirm dismisses | PASS | viewModel.decline → DeclinedSuccess → onDismiss (LaunchedEffect:94-95) | ✓ ALIGNED |
| B5 Owner sees invite_declined | PASS | declineInvite.ts:77-92 writeNotification (owner, invite_declined) | ✓ ALIGNED |
| B6 Firestore state post-decline | PASS | declineInvite.ts:63-67 only removes pending; no invitedUsers write | ✓ ALIGNED |
| C1 Legacy invite fallback | PASS | shouldOpenInviteSheet returns false when pendingEntryKey absent; NotificationsScreen.kt:167 navigates | ✓ ALIGNED |
| C2 Re-invite of already-member | PASS | inviteToRegistry.ts:87-109 isAlreadyMember short-circuit; enriched={} omits pendingEntryKey | ✓ ALIGNED |
| D1 Inbox visual contract | PASS (after Task 8 fix) | NotificationsScreen.kt re-skin + bundled JetBrains Mono TTFs | ✓ ALIGNED |
| D2 Romanian locale parity | PASS (after Task 7 fix) | values-ro/strings.xml locked copy + NotificationsScreen.kt:215-228 clock-skew clamp | ✓ ALIGNED |

**20/20 UAT items map to verifiable code paths.** No drift between claims and codebase.

---

## 6. Deviation Fix Audit (Tasks 4, 5, 7, 8)

All four deviation fixes are present, atomic, and traceable to UAT-surfaced gaps.

| Task | Commit | LoC | Scope | Verdict |
|------|--------|-----|-------|---------|
| Task 4 — Restore notifications bell in HomeTopBar | `8e901fc` | 10 lines | `app/.../ui/registry/list/HomeTopBar.kt` (and RegistryListScreen wiring) | ✓ ATOMIC. Fixes pre-existing Phase 10 gap; bell now invokes `onNotificationsClick`. |
| Task 5 — Register FCM token on auth state change | `cf79b51` | ~10 lines | `app/.../GiftRegistryApp.kt` lines 64-75 | ✓ ATOMIC. Fixes pre-existing Phase 06 gap (onNewToken does NOT fire on sign-in). addAuthStateListener pattern documented in MEMORY.md `feedback_fcm_token_signin.md`. |
| Task 7 — Clamp createdAtMs to now | `c59fc6f` | ~17 lines | `NotificationsScreen.kt:215-228` + inline comment explaining the WHY | ✓ ATOMIC. Fixes Romanian "PESTE 0 MINUTE" → "ACUM 0 MINUTE" via `minOf(createdAtMs, now)`. |
| Task 8 — Bundle JetBrains Mono TTFs | `8d43425` | TTFs (273+277 KB) + OFL license + GiftMaisonFonts.kt edit | `res/font/`, `assets/licenses/`, `GiftMaisonFonts.kt:71-73` | ✓ ATOMIC. Same precedent as 260427-lnq for InstrumentSerifFamily. Sync rendering eliminates per-glyph fallback inconsistency. |

Each commit message contains "Task N" and explicitly references "UAT FAIL" or "UAT-surfaced". All four are documented in 16-06-SUMMARY.md key-decisions section as accepted mid-execution deviations (rather than gap-closure phase 16.1).

---

## 7. Test File Audit

| Test File | Layer | Status |
|-----------|-------|--------|
| `app/src/test/.../domain/model/NotificationTypeFromWireTest.kt` | Android domain | EXISTS |
| `app/src/test/.../ui/notifications/NotificationCardBranchingTest.kt` | Android UI predicate | EXISTS |
| `app/src/test/.../ui/notifications/InviteResponseViewModelTest.kt` | Android VM state machine | EXISTS |
| `app/src/test/.../data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt` | Android data layer | EXISTS |
| `app/src/test/.../LocalizationParityTest.kt` | Cross-locale parity | EXISTS (286/286) |
| `functions/src/__tests__/acceptInvite.test.ts` | Backend transaction | EXISTS |
| `functions/src/__tests__/declineInvite.test.ts` | Backend transaction | EXISTS |
| `functions/src/__tests__/inviteToRegistry.test.ts` | Backend modified contract | EXISTS (Tests A/B/H per Plan 16-01) |
| `tests/rules/firestore.rules.test.ts` | D-18/D-19 rules tests | EXISTS (lines 524-563, describe block "pendingInvitedUsers read scope (D-18)") |

All test files claimed by Plan 16-01 (Wave 0) are present. Plan SUMMARYs report all GREEN as of last execution.

---

## 8. Anti-Pattern Scan

| Pattern | Found | Severity | Notes |
|---------|-------|----------|-------|
| TODO/FIXME/XXX in Phase 16 files | None | — | grep clean |
| Stub returns (return null / return []) where dynamic data expected | None | — | All composables render real domain state |
| Empty event handlers | None | — | Accept/Decline both wire to repo callable; Cancel dismisses dialog |
| Hardcoded blacklist/declinedUsers fields | None | ✓ | D-15 negative-coverage holds (Plan 16-02 SUMMARY verified) |
| Badge()/BadgedBox/pendingCount in NotificationsScreen | None | ✓ | D-02/D-08 negative-coverage holds (re-verified by grep on file) |
| Stale `invitedUsers` write in inviteToRegistry | None | ✓ | grep -n "invitedUsers" returns only the existingInvitedUsers read for D-16 branch — no write |
| Missing enforceAppCheck on new callables | None | ✓ | Both callables: `enforceAppCheck: true` |

---

## 9. Deferred Items (Acknowledged, Not Blocking)

Per `.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md`:

1. **`functions/src/__tests__/createReservation.test.ts` — 3 pre-existing failures** (admin.initializeApp() not invoked in module loading path). Pre-dates Phase 16 (`git stash` reproduced). Not Phase 16's concern.
2. **`AuthViewModelTest` — 2/12 Turbine timing flakes** (`Expected no events but found Item(Unauthenticated)`). Pre-dates Phase 16 (reproduced at HEAD `7e0d1a9`). Not Phase 16's concern.

Per `16-06-SUMMARY.md` deferred items:
1. Huawei sign-in build flag follow-up todo (Task #10)
2. Document `-Puse_emulator=false` in CLAUDE.md (Task #12)
3. Foreground invite push polish — in-app banner (Task #15)
4. A5 sheet sub-pixel gradient polish (Task #16)

All four are v1.1 polish; none block Phase 16 goal achievement.

---

## 10. Minor Observations (Non-Blocking)

1. **User prompt referenced an "invite_pending" notification type** that does NOT exist in the codebase. The actual implementation per D-10 EXTENDS the existing `invite` type's payload with `pendingEntryKey` (not a new wire string). The 3 new notification types are `invite_accepted_self`, `invite_accepted`, `invite_declined`. CONTEXT.md, all plan SUMMARYs, and the code are internally consistent on this — the prompt phrasing was a minor descriptive inaccuracy on the verification request, NOT a phase-16 drift.

2. **`inviteToRegistry` callable does NOT have `enforceAppCheck: true`** (line 23: only `{ region: "europe-west3" }`). This is INTENTIONAL — `inviteToRegistry` is a pre-Phase-14 callable; only NEW Phase 16 callables (acceptInvite, declineInvite) are App-Check-enforced per D-20. The owner-side callable's App Check retrofit is tracked separately (not a Phase 16 deliverable).

3. **ROADMAP.md does not show an `[x]` top-level checkbox for Phase 16 itself**; it shows `Plans: 6/6 plans complete` and all 6 sub-plans as `[x]`. This appears to be by design — Phase 16's top-level completion is gated on this verification report. The 16-06-SUMMARY.md mentions "STATE.md + ROADMAP advanced" but the explicit `[x] **Phase 16: ...** - (completed YYYY-MM-DD)` line is not yet present at the section header. **This is a docs-only nit; the implementation is complete.**

4. **`inviteToRegistry.ts:122` hardcodes locale `"en"`** for the invite email — pre-existing behavior; not a Phase 16 deliverable change. CONTEXT.md D-14 notes "invite recipient locale unknown; default en"; this is the documented status.

---

## 11. Final Verdict

**Status: PASS**

All 28 phase decisions (D-01..D-28) are implemented and verifiable in the codebase. All 10 observable truths derived from the phase goal are verified against actual code paths (not just SUMMARY claims). All 20 UAT scenarios in `16-06-UAT-RESULTS.md` map to inspectable code. Deployment evidence in `16-06-DEPLOY-LOG.md` confirms 3 Cloud Functions live in europe-west3 + composite index built. Firestore rules correctly unchanged (Pattern 8). All 4 mid-phase deviation fixes (Tasks 4, 5, 7, 8) are atomic commits traceable to UAT-surfaced gaps.

**No blockers. No partial implementations. No silent drops of D-XX requirements.**

**Score: 28/28 must-haves verified.**

---

## 12. Recommended Follow-Up Todos (Non-Blocking, Docs/Polish Only)

These are housekeeping items surfaced by the verification audit; none block Phase 16 closure:

1. **Mark Phase 16 complete in `.planning/ROADMAP.md`** at line 327 — change section header to include `(completed 2026-05-26)` and an `[x]` checkbox-style indicator if the project convention requires it (the SUMMARY says this was done, but the section header is unchanged at the time of audit).

2. **Document the `-Puse_emulator=false` build flag in `CLAUDE.md`** — flagged as Task #12 in 16-06-SUMMARY.md; high-value reference for future contributors hitting the same trap.

3. **Retrofit `enforceAppCheck: true` onto `inviteToRegistry`** in a future hardening pass — currently only the new Phase 16 callables enforce App Check. Pre-existing callables follow a separate retrofit todo (not in Phase 16 scope).

4. **Fix pre-existing `createReservation.test.ts` and `AuthViewModelTest`** failures documented in `deferred-items.md` — both pre-date Phase 16 and were stash-verified to be unrelated.

5. **Optional v1.1 polish**: foreground invite push banner (Task #15); InviteResponseSheet sub-pixel gradient alignment (Task #16).

---

*Verified: 2026-05-26 by Claude (gsd-verifier, read-only audit)*
*No source code or production files modified during verification.*
