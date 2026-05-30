---
phase: quick-260530-p7w
plan: "01"
subsystem: android-chrome-nav
tags: [bottom-nav, notifications, navigation, ui, strings]
dependency_graph:
  requires: [NotificationsKey, NotificationsScreen (quick-260420-ozb)]
  provides: [bottom-nav slot 4 = INBOX → NotificationsKey]
  affects: [GiftMaisonBottomNav, AppNavigation, StyleGuidePreview, strings EN+RO]
tech_stack:
  added: []
  patterns: [Icons.Outlined.Inbox for nav slot, currentKey !is check for duplicate-push guard]
key_files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
decisions:
  - "Icons.Outlined.Inbox chosen over Icons.Outlined.MoveToInbox (action arrow wrong semantic) and Icons.Outlined.Mail (envelope, not tray). Outlined variant matches existing nav-slot convention."
  - "RO locale keeps literal 'INBOX' — user explicitly locked, no translation to 'Mesaje' or 'Notificari'."
  - "Duplicate-push guard: `if (currentKey !is NotificationsKey)` — same pattern as onDiscover. Bell on Home top bar kept as additional entry point, not removed."
metrics:
  duration: ~8min
  completed_date: "2026-05-30"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 5
verified_on: emulator-5554
---

# Quick 260530-p7w: Rename Bottom-Nav Slot 4 LISTS → INBOX Summary

**One-liner:** Bottom-nav slot 4 rebranded LISTS → INBOX with `Icons.Outlined.Inbox` icon, rewired to push `NotificationsKey` (same destination as the Home top-bar bell, which is retained as an additional entry point).

## Task Execution

### Task 1 — Completed (commit b6ca0aa)

Five-file mechanical rename. All verified by grep + Kotlin compile + unit tests.

**Changes made:**

| File | Change |
|------|--------|
| `GiftMaisonBottomNav.kt` | `NavSlotId.LISTS → INBOX`; `onLists → onInbox`; `Icons.AutoMirrored.Outlined.List → Icons.Outlined.Inbox`; `RegistryDetailKey → NotificationsKey` in selected-state `when`; import of `RegistryDetailKey` removed, `NotificationsKey` added; KDoc updated. |
| `AppNavigation.kt` | `onLists = { RegistryDetail-jump }` replaced with `onInbox = { if (currentKey !is NotificationsKey) backStack.add(NotificationsKey) }`. Home bell unchanged. |
| `StyleGuidePreview.kt` | `BottomNavListsSelectedPreview` → `BottomNavInboxSelectedPreview`; `currentKey = RegistryDetailKey("preview")` → `NotificationsKey`; both previews now use `onInbox = {}`. |
| `values/strings.xml` | `nav_lists_tab` retired; `nav_inbox_tab = "INBOX"` added. |
| `values-ro/strings.xml` | `nav_lists_tab` (LISTE) retired; `nav_inbox_tab = "INBOX"` (literal, not translated) added. |

**Verification results:**

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- `:app:testDebugUnitTest --tests "*BottomNavVisibilityTest*"` — BUILD SUCCESSFUL (14 cases, predicate unchanged)
- `:app:assembleDebug -Puse_emulator=false` — BUILD SUCCESSFUL
- All stray-reference greps (`nav_lists_tab`, `NavSlotId.LISTS`, `onLists`, `AutoMirrored.Outlined.List`) — no matches

**Chrome dimensions preserved:** 72dp bar height, 54dp FAB requiredSize, 44dp icon-pill, 22dp icon (quick-260427-nkn / -n67 / -lwz fixes intact).

**APK:** `/Users/victorpop/ai-projects/gift-registry/app/build/outputs/apk/debug/app-debug.apk`

### Task 2 — PASSED (visual + functional verification on emulator-5554, 2026-05-30)

Walked through all 11 checks on the emulator and approved: slot 4 shows the Inbox tray glyph + "INBOX" label in both EN and RO (literal, not translated), tap opens the same notifications screen the Home top-bar bell does, selected-state pill appears on the notifications screen, repeated-tap is a no-op (duplicate-push guard), bell still works as an additional entry point, chrome dimensions unchanged (72dp bar, FAB plus-icon below the top border line — 260427-nkn/n67/lwz fixes intact), bottom nav stays hidden on auth/onboarding (NavVisibility predicate unchanged), other slots untouched.

## Deviations from Plan

None — plan executed exactly as written. `primaryRegistryId` and `hasRegistries` remain declared in AppNavigation.kt (still used elsewhere for registryListState).

## Known Stubs

None introduced.

## Checkpoint Outstanding

**Task 2: Visual + functional verification on device**

Install and verify on emulator-5554 per the 11-point checklist in the PLAN.

## Self-Check: PASSED

- `GiftMaisonBottomNav.kt` contains `Icons.Outlined.Inbox` and `NavSlotId.INBOX` — FOUND
- `AppNavigation.kt` contains `onInbox` and `backStack.add(NotificationsKey)` — FOUND
- `values/strings.xml` contains `nav_inbox_tab` — FOUND
- `values-ro/strings.xml` contains `nav_inbox_tab` — FOUND
- `StyleGuidePreview.kt` contains `BottomNavInboxSelectedPreview` — FOUND
- Commit b6ca0aa — FOUND
