---
date: 2026-05-21
category: bug
phase_origin: 14-web-fallback-live-deploy-guest-uat
plan_origin: 14-04
priority: medium
surfaced_during: UAT-5 setup (invitee testing)
---

# Invite-email CTA button not clickable in Gmail mobile app (deliverability)

## Symptom

Real-world invitee received the registry invite email but the "View registry" CTA
button was non-clickable. Email landed in Gmail spam; invitee marked it as
non-spam and moved it to inbox; button still non-clickable.

## Diagnostic state

- The Firestore `mail` collection doc has a fully-populated, correct
  `<a href="https://gift-registry-ro.web.app/registry/S3JR2ntPqz6ruhXBFhXf">`
  in `message.html`. Code path through `functions/src/email/templates/invite.ts`
  → `_shell.ts` → `sendEmail()` is verified correct end-to-end.
- The text fallback also contains the full URL.
- Therefore the bug is NOT in our template rendering nor in URL building
  (`functions/src/config/publicUrls.ts:25` is fine; `PUBLIC_WEB_BASE_URL` is
  resolving to the deployed default).

## Root cause hypothesis

Sender authentication missing for the SMTP configured behind the Firebase
"Trigger Email" extension. Gmail's spam classifier flagged the email; even
after the user moved it to inbox, Gmail's link-protection / link-stripping
heuristics keep CTA buttons disabled when the sender is not
SPF/DKIM/DMARC-authenticated against the From: domain.

## What's needed

1. Inspect the current Firebase Trigger Email extension config — which SMTP
   service is it using (Gmail SMTP, SendGrid, Mailgun, custom)?
2. Configure proper SPF/DKIM/DMARC records for the sending domain so
   Gmail/Outlook trust the sender.
3. Re-run UAT item with the same invitee — confirm the CTA is now clickable
   from inbox AND that the email lands in inbox directly (not spam).
4. If using Gmail SMTP and sender authentication isn't fixable: migrate the
   extension to SendGrid/Postmark/Resend with verified sender domain.

## Also affected

Same SMTP configuration affects ALL transactional emails sent by the app:
- `inviteToRegistry` (REG-06/REG-07) — this bug
- `releaseReservation` expiry emails (NOTF-03)
- `onPurchaseNotification` purchase confirmations (NOTF-02)
- Email re-reserve link in expiry email (Phase 14 UAT item 6 — flag for
  retesting after the fix; the link will likely have the same problem on
  the invitee's device)

## Scope notes

- NOT a blocker for Phase 14 closure. WEB-01..04 are about web-fallback
  access; this is about email deliverability (REG-06/07 + NOTF-02/03).
- Worth filing as a quick-task once the SMTP config is inspected, because
  the fix path differs between "tweak DNS records" and "migrate provider."
