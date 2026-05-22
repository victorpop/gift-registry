---
created: 2026-04-20T14:30:27.676Z
title: Register Firebase Web app and deploy real web config
area: general
files:
  - web/.env.local (to be created)
  - web/.env.example
  - web/src/firebase.ts:10-17
  - web/vite.config.ts:15
  - firebase.json
  - hosting/public/index.html
---

## Problem

https://gift-registry-ro.web.app renders a blank page. Two root causes:

1. No Firebase **Web** app is registered in the `gift-registry-ro` project — only the Android app exists. `firebase apps:sdkconfig web` fails with "There are no WEB apps associated with this Firebase project".
2. `web/.env.local` doesn't exist, so the last production build baked empty strings into `VITE_FIREBASE_*`. At runtime, `initializeApp({})` in `web/src/firebase.ts:19` throws before React mounts, leaving `<div id="root">` empty.

Deployed bundle is at `hosting/public/assets/index-DHUD1oo1.js` (from April 19 build). Grepping for `apiKey:"..."` / `projectId:"..."` returns nothing, confirming empty config.

## Solution

1. Register the web app (shared-state change — user confirmation required):
   ```
   firebase apps:create WEB "Gift Registry Web"
   ```
2. Fetch the SDK config:
   ```
   firebase apps:sdkconfig web
   ```
3. Create `web/.env.local` with the values (API key is public, safe in `.env.local`; `.gitignore` already excludes it). Required keys from `web/.env.example`:
   - `VITE_FIREBASE_API_KEY`
   - `VITE_FIREBASE_AUTH_DOMAIN` (= `gift-registry-ro.firebaseapp.com`)
   - `VITE_FIREBASE_PROJECT_ID` (= `gift-registry-ro`)
   - `VITE_FIREBASE_STORAGE_BUCKET`
   - `VITE_FIREBASE_MESSAGING_SENDER_ID`
   - `VITE_FIREBASE_APP_ID`
   - `VITE_USE_EMULATORS=false`
4. Rebuild and redeploy (Vite outputs directly to `hosting/public/` per `web/vite.config.ts:15`):
   ```
   npm --prefix web run build
   firebase deploy --only hosting
   ```
5. Verify `https://gift-registry-ro.web.app/` shows `app.name` / `app.subtitle` from `web/src/pages/AppRootPage.tsx` and that `/registry/:id` loads without console errors.

Note: `VITE_RECAPTCHA_SITE_KEY` and `VITE_APP_CHECK_DEBUG_TOKEN` from `.env.example` are separate (App Check) — not required to unblock the blank page, but follow-up if App Check is enforced.
