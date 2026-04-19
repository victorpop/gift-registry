---
phase: 6
slug: notifications-email-flows
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-19
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | jest 29.x (Cloud Functions); JUnit 4 + MockK (Android); vitest (web — already configured) |
| **Config file** | `functions/jest.config.js` (Wave 0 installs); `app/build.gradle.kts` test deps already present; `web/vitest.config.ts` already present |
| **Quick run command** | `cd functions && npm test -- --testPathPattern={modified-module} --silent` |
| **Full suite command** | `cd functions && npm test && ./gradlew :app:testDebugUnitTest && cd web && npm test -- --run` |
| **Estimated runtime** | ~90 seconds (functions ~30s, Android ~45s, web ~15s) |

---

## Sampling Rate

- **After every task commit:** Run quick command for the modified module
- **After every plan wave:** Run full suite for that surface (functions / Android / web)
- **Before `/gsd:verify-work`:** Full suite must be green across all three surfaces
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD — populated by planner | | | NOTF-01/02/03 | | | | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `functions/jest.config.js` + `functions/package.json` test scripts — no test infra exists in functions yet (per research)
- [ ] `functions/src/__tests__/` directory with fixtures for Firestore trigger + callable mocks
- [ ] Firebase Trigger Email extension installed and configured with `SMTP_URI` secret — human action, prerequisite for live email flows (emulator can use console-log path without it)
- [ ] Extend `tests/rules/` with new collection rules (`mail`, `users/{uid}/fcmTokens`, `notifications_failures`, `items.purchaseNotificationSentAt`)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| FCM push notification delivery on real device (foreground + background) | NOTF-01 | Requires physical Android device with Google Play Services; emulator FCM paths are unreliable | Install app on real device, enable notifyOnPurchase, trigger confirmPurchase from a second giver, verify push arrives in both foreground (snackbar) and background (system tray) |
| Email delivery rendering in real clients (Gmail web/Android, Outlook web) | NOTF-02/03 | Email client rendering cannot be automated; HTML compatibility varies | Trigger each of the 3 flows via SendGrid real send (staging SMTP), inspect rendered email in Gmail + Outlook for brand band, CTA button, en/ro copy |
| Re-reserve deep link opens correct flow on real device | NOTF-03 | Deep link app-linking requires signed APK + real email client launch | Click re-reserve link in received email on Android device, verify app opens to the correct item in the correct registry and autoselects reservation flow |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s
- [ ] `nyquist_compliant: true` set in frontmatter (planner flips this after populating the per-task map)

**Approval:** pending
