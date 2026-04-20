---
created: 2026-04-20T14:56:21.942Z
title: Group registries by ownership and clarify invitee permissions
area: ui
files:
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
  - firestore.rules
---

## Problem

Surfaced during live test of quick task 260420-o6w (shared-with-me registries query). After merging owned + invited registries into a single flat list, user observed Maria's invited registry appearing "under a separate category Shared lists" — but the code has no UI grouping. Victor just happened to own zero registries so the list visually read as shared-only.

Two coupled concerns:

1. **UI grouping.** Users expect owned and shared registries to be visually distinct (e.g., "Your lists" + "Shared with you" section headers on the Home list). Current state: flat list sorted by `updatedAt DESC` with no visual ownership cue — a Victor with 5 owned + 2 shared sees 7 intermixed cards with no way to tell which are his.

2. **Invitee permission clarity.** Less obvious but more important: what is an invited user actually allowed to do on a shared registry? Current `firestore.rules` lets invitees **read** the registry and its items. But can they:
   - Reserve an item? (Yes — reservations have their own doc namespace; no ownership check prevents it. Live test confirmed Victor can reserve + purchase.)
   - Mark an item purchased? (Same as reserve — technically yes.)
   - Edit the registry metadata (title, visibility)? (Rules: owner-only for writes, so no — but the UI may not disable the edit affordances, leading to silent rule failures.)
   - Add items to someone else's registry? (Rules: owner-only — but is the Add Item FAB shown on shared registries?)
   - Invite more people? (Rules: owner-only — but is the Invite button shown on shared registries?)
   - Delete items or the registry itself? (Rules: owner-only — same UX question.)

The UI should either (a) hide/disable owner-only actions when the viewer is an invitee, or (b) allow the taps and surface a clean error — the latter is worse because the affordance implies it should work. Today, we don't know which actions are visible to invitees in the detail screen because the detail screen was built before shared-list visibility shipped.

## Solution

TBD — but two-part scope:

**Part A — Grouping on list screen:**
- In `RegistryListViewModel`, partition the merged list into `ownedRegistries` and `sharedRegistries` (predicate: `registry.ownerId == currentUid`).
- In `RegistryListScreen`, render two `LazyColumn` sections with Material3 `ListItem`-style headers (or `Text` with section typography). Use `strings.xml` keys `registry_list_section_owned` / `registry_list_section_shared` (both locales — English + Romanian).
- Skip the section header if one group is empty (so a user with no shared registries doesn't see an empty "Shared with you" header).
- Keep existing sort within each section (updatedAt DESC).

**Part B — Invitee-aware detail screen:**
- Derive `isOwner` state in `RegistryDetailViewModel` from `registry.ownerId == currentUid`.
- In `RegistryDetailScreen`, conditionally render/hide: Add Item FAB, Invite button, Edit registry metadata button, Delete registry button, Delete item actions.
- Reserve and Purchase affordances stay visible for invitees — they are the point of shared access.
- Audit `firestore.rules` to confirm the exact write surface for invitees is what we intend. If reservation writes by invitees need a tighter rule (e.g., "invitees can only create reservations, not arbitrary writes"), update the rule.
- Decide display policy for an invitee who opens a registry they've been removed from (or whose invite was revoked) — soft error, graceful exit.

**Plan suggestion:** split into two quick tasks if scope grows — grouping is low-risk UI, permissions audit is security-adjacent and should probably go through `/gsd:quick --discuss` to settle the per-action policy before coding.

## Related

- Completed: `.planning/quick/260420-o6w-show-shared-with-me-registries-query-reg/` — the data-layer change that made this visible
- Deferred in that SUMMARY as "Shared with me section header" follow-up
