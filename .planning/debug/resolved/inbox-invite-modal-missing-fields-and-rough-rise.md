---
status: resolved
trigger: "inbox-invite-modal-missing-fields-and-rough-rise"
created: 2026-05-30T00:00:00Z
updated: 2026-05-30T12:00:00Z
---

## Current Focus

hypothesis: CONFIRMED — two independent bugs:
  1. description field never written to notification payload by Cloud Function (inviteNotificationHelpers.ts buildEnrichedInvitePayload) AND never read/rendered by InviteResponseSheet composable
  2. rememberModalBottomSheetState() has no skipPartiallyExpanded=true, so sheet defaults to half-expanded state requiring user drag
test: Read buildEnrichedInvitePayload, InviteResponseSheet sheetState line
expecting: Both confirmed — now proceeding to fix
next_action: Fix both: add description to Cloud Function payload + render it in composable + set skipPartiallyExpanded=true

## Symptoms

expected: Modal rises smoothly to full content height; shows title, cover, occasion, description, event date, event time; Accept/Decline at bottom; no manual scroll needed.
actual:
  - Description and event date/time not rendered even when registry has them
  - Modal requires manual drag/scroll to reveal content; does not animate to full height in one step
errors: None — pure UI rendering/animation issue
reproduction: Tap INBOX tab → tap invite-to-registry notification → observe modal
started: First time this flow was exercised; modal is scaffolding from earlier commit, never formally shipped

## Eliminated

- hypothesis: eventDateMs not written to payload by Cloud Function
  evidence: buildEnrichedInvitePayload DOES write eventDateMs (line 66); the composable DOES have rendering logic for it (lines 159-178). However InviteResponseSheet uses DateUtils.FORMAT_SHOW_DATE|FORMAT_SHOW_TIME which combines date+time in one label — no separate date vs time fields needed.
  timestamp: 2026-05-30T00:01:00Z

## Evidence

- timestamp: 2026-05-30T00:01:00Z
  checked: functions/src/registry/inviteNotificationHelpers.ts buildEnrichedInvitePayload (lines 57-68)
  found: Returns { pendingEntryKey, occasion, coverUrl, eventDateMs } — NO description field
  implication: description is never written to the notification payload, so it can never appear in the sheet

- timestamp: 2026-05-30T00:01:00Z
  checked: app/.../ui/notifications/InviteResponseSheet.kt full composable
  found: Reads payload["coverUrl"], payload["occasion"], payload["actorName"], payload["registryName"], payload["eventDateMs"] — NO payload["description"] read or rendered
  implication: Even if description were in the payload, the UI has no code to display it

- timestamp: 2026-05-30T00:01:00Z
  checked: InviteResponseSheet.kt line 85-87 — rememberModalBottomSheetState
  found: rememberModalBottomSheetState(confirmValueChange = { ... }) — skipPartiallyExpanded not set, defaults to false
  implication: Sheet opens in half-expanded state; user must drag to see full content

- timestamp: 2026-05-30T00:01:00Z
  checked: app/.../res/values/strings.xml
  found: registry_description_label, registry_event_date_label, registry_event_time_label all exist in both en and ro locales
  implication: Can reuse existing string keys for description label in invite sheet — no new strings needed

## Resolution

root_cause: Two bugs: (1) buildEnrichedInvitePayload omits description field entirely — neither written to payload nor read by composable; (2) rememberModalBottomSheetState() missing skipPartiallyExpanded=true causes sheet to land in half-expanded state
fix: (1) Add description field to buildEnrichedInvitePayload + render it in InviteResponseSheet between registryName and eventDateLabel; (2) Add skipPartiallyExpanded=true to rememberModalBottomSheetState call, keep confirmValueChange guard
verification: Confirmed on physical device after Cloud Function deployed to europe-west3 and Android build installed. Sheet rises smoothly in one continuous animation. Fresh invites carry description and event date/time renders correctly. Note: existing inbox notifications retain old payload (written before deploy) — description only appears on new invites sent post-deploy. Expected behavior for denormalized notification payloads.
files_changed:
  - functions/src/registry/inviteNotificationHelpers.ts
  - app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt
