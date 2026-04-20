# Quick Task 260420-ozb: Add persistent in-app notifications inbox for invite events - Context

**Gathered:** 2026-04-20
**Status:** Ready for planning

<domain>
## Task Boundary

Add a persistent in-app notifications inbox so a logged-in Android user can see a history of events relevant to them: registries they've been invited to, actions taken on their registries (reservation, purchase, reservation expiry), and re-reserve window openings.

**Explicitly out of scope (confirmed with user):**
- FCM push delivery (token registration, Android 13+ permission prompts, push payload tuning). Assume push delivery is a separate concern. The inbox is the authoritative in-app record — pushes are a secondary channel that the user has opted to defer.
- Notification preferences / opt-out settings. User gets all notifications; opt-out is a future enhancement.
- Web fallback parity (React/hosting). Android-only for now.
- Historical backfill. Forward-only: only events that occur *after* this feature ships land in the inbox.

</domain>

<decisions>
## Implementation Decisions

### Event types in scope

User selected: **All notifiable events.**

The inbox must support:
- **Invite received** — "Maria invited you to 'Maria's Birthday 2026'" (triggered by `inviteToRegistry` Cloud Function)
- **Item reserved on your registry** — "Someone reserved 'Coffee maker' on 'My Wedding'" (triggered when a reservation is created on a registry you own)
- **Item purchased on your registry** — "Your gift 'Coffee maker' has been purchased" (triggered by the purchase-confirmation flow — likely the same Cloud Function already sending email)
- **Reservation expired (giver-side)** — "Your 30-minute reservation on 'Coffee maker' expired" (triggered by `releaseReservation` Cloud Function — sent to the giver only)
- **Re-reserve window opened (giver-side)** — may coincide with or immediately follow the expiry event

Broad scope means a single shared "create notification" helper should be used on the server side, not 5 separate per-event paths. All events write into the same `notifications` subcollection shape with a `type` discriminator.

### UI entry point

User selected: **Top-bar bell icon on Home.**

- Add a Material 3 bell icon (`Icons.Filled.Notifications` or `NotificationsNone`) to the top app bar on the Home screen.
- Unread count shown as a Material 3 `Badge` overlaid on the bell.
- Tap opens a new `NotificationsScreen` (standalone destination via Navigation3, not a bottom sheet — sheets are harder to navigate back from into a specific registry).
- Bottom nav is NOT touched. The entry point is the bell only.
- Bell is visible only when the user is signed in — not on auth/onboarding screens.

### Read/unread state

User selected: **Unread badge + mark-as-read on view.**

- Each notification doc has `readAt: Timestamp?` (null = unread).
- Entry-point badge shows the unread count (capped at "9+" for cleanliness).
- When the user opens the `NotificationsScreen`, all currently-unread notifications that appear in the list flip to `readAt = serverTimestamp()` in a single batch write (client-side) — debounced so scrolling doesn't fire per-item writes.
- Read state is not used to hide notifications; read items stay in the list just visually muted.
- "Mark all as read" action not required for v1 — tap-to-open marks them all.

### Claude's Discretion

Areas the user deferred:

**Data source — Firestore collection, not derived.** Given the broad event scope, events must be written server-side at the moment they happen. Use a dedicated `users/{uid}/notifications` subcollection (per-user, good security rules story, easy to query by the Android client with a `limit` + `orderBy createdAt desc`). Each doc:
```
{
  id: string                   // auto
  type: "invite" | "reservation_created" | "item_purchased" | "reservation_expired" | "re_reserve_window"
  createdAt: serverTimestamp
  readAt: Timestamp | null
  title: string                // client-localized via key; see Localization below
  titleKey: string             // i18n key; title is a pre-rendered fallback in English
  body: string                 // pre-rendered for diagnostics/fallback
  payload: map                 // type-specific fields (registryId, reservationId, itemId, actorUid, registryName, itemName)
}
```

**Localization — key + payload.** Server writes the i18n key + payload fields; client resolves the final string from `strings.xml` using the user's current locale. This avoids server-side locale detection (user may change locale on device after notification was written) and keeps translation work on Android side.

**Security rules.** `users/{uid}/notifications/{id}` — read: self only; create: never from client (only Admin SDK); update: self-only AND only the `readAt` field. The Cloud Functions that currently send emails add notification writes using the Admin SDK.

**Retention / deletion.** v1: no auto-delete. v2 could add a 90-day TTL. User can't delete individual items from the UI in v1.

**Tapping a notification navigates to its payload:**
- `invite` → registry detail for `payload.registryId`
- `reservation_created` / `item_purchased` → registry detail, scrolled to the item
- `reservation_expired` / `re_reserve_window` → item detail (if it has one) or back to the registry

Missing deep-link targets (e.g., item detail route may not exist yet) fall back to the closest existing destination. Planner should confirm which routes exist today.

**Scope size caveat.** Five event sources × server-side write × client-side UI × i18n × rules is a big surface. If the planner judges this exceeds the 1-3 task quick-task limit, split into:
- Plan task 1: data model + rules + one reference event source (invites), including client UI
- Follow-up quick task: wire the remaining 4 event sources once the pattern is proven

</decisions>

<specifics>
## Specific Ideas

- Entry-point bell: Material 3 `TopAppBar` `actions` slot with `IconButton` wrapping `BadgedBox` + `Icon`. The Compose M3 library already pulls in `Badge` via the Compose BOM used by this project.
- Notifications screen: `LazyColumn` of `Card` items, with the card's leading icon chosen by `type`. Empty state: simple centered text "No notifications yet." The list sorts by `createdAt DESC`.
- Unread count flow: `NotificationRepository.observeUnreadCount(uid): Flow<Int>` — backed by `.whereEqualTo("readAt", null)` + `.count()` if Firestore count aggregation is available, else the list snapshot filtered client-side. `count()` is preferred (less data over the wire).
- Android 13+ notification POST permission is NOT required for this in-app inbox — only for system push. Out of scope per user decision.
- Romanian (`values-ro/strings.xml`) strings must be authored alongside English. Use keys like `notification_invite_title`, `notification_reservation_created_body`, etc., with positional format args for dynamic payload values.

</specifics>

<canonical_refs>
## Canonical References

- `functions/src/registry/inviteToRegistry.ts` — invite event source, already sends FCM push + email
- `functions/src/notifications/onPurchaseNotification.ts` — purchase event source, already sends email
- `functions/src/reservation/releaseReservation.ts` — expiry event source, already sends email
- Reservation creation — likely in `functions/src/reservation/` (planner should locate the specific file)
- `firestore.rules` — needs new `match /users/{uid}/notifications/{id}` block
- `app/src/main/res/values/strings.xml` + `values-ro/strings.xml` — notification string keys
- `app/src/main/java/com/giftregistry/ui/` — add `notifications/` package with `NotificationsScreen`, `NotificationsViewModel`, `NotificationsInboxBellIcon` composable

</canonical_refs>
