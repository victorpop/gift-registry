---
status: partial
phase: 06-notifications-email-flows
source: [06-VERIFICATION.md]
started: 2026-04-19T22:35:00Z
updated: 2026-04-19T22:35:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. FCM push on real Android device (foreground + background)
expected: Foreground — Material3 snackbar appears at top of screen when owner is in-app. Background — notification appears in system tray. Both routes only fire when registry.notifyOnPurchase === true.
setup: Install app on real Android device with Google Play Services; sign in as registry owner; enable notifyOnPurchase on a test registry; have a second giver reserve and complete a purchase.
result: [pending]

### 2. Email rendering in Gmail and Outlook
expected: Expiry, invite, and purchase emails render the 600px centred shell correctly with #6750A4 header band, bulletproof CTA button, and bilingual footer. No broken images or clipped content across Gmail web + Android app and Outlook web.
setup: Install Trigger Email extension (`firebase ext:install firebase/firestore-send-email`) with SendGrid SMTP URI configured as secret. Trigger each flow: (a) invite a user, (b) let a reservation expire, (c) complete a purchase on an opted-in registry.
result: [pending]

### 3. Re-reserve deep link opens app from email on a real device
expected: Tapping the "Re-reserve this gift" CTA in the expiry email opens the app (or web fallback) at the re-reserve screen for the correct reservationId. URL format: https://giftregistry.app/reservation/{reservationId}/re-reserve
setup: Signed APK installed on Android device (unsigned debug builds do not resolve App Links). Trigger an expiry email send. Tap CTA from Gmail or Outlook mobile client.
result: [pending]

### 4. Owner opt-out end-to-end: toggle notification setting off then purchase a gift
expected: After owner disables notifyOnPurchase in SettingsScreen, completing a purchase triggers no FCM push and no mail doc written to the mail collection.
setup: Firebase emulator running (`firebase emulators:start`). Use Emulator UI (http://localhost:4000) to inspect Firestore collections after the test purchase.
result: [pending]

### 5. ConfirmPurchaseBanner appears and dismisses on Android
expected: A giver with an active reservation sees the banner sticky at top of RegistryDetailScreen. Tapping "I completed the purchase" shows loading indicator, then success snackbar "Purchase confirmed. Thank you!" appears and banner unmounts. Tapping on an expired reservation shows error snackbar "Could not confirm. Please try again."
setup: Android emulator or device with app installed. Create a registry + item, reserve as a guest giver, navigate to RegistryDetailScreen.
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps

_(none recorded yet — fill in only if a test fails)_
