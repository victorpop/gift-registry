import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { initializeAppCheck, ReCaptchaV3Provider } from 'firebase/app-check'
import { getRedirectResult } from 'firebase/auth'
import { QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import './i18n'
import App from './App'
import { queryClient } from './queryClient'
import { app, auth } from './firebase'
import { ToastProvider } from './components/ToastProvider'
import { ActiveReservationProvider } from './features/reservation/useActiveReservation'

// STEP 1: Debug token MUST be set BEFORE initializeAppCheck.
// Setting to `true` auto-generates a UUID on first run — copy it from the browser console
// and register it at Firebase Console > App Check > Debug tokens.
// Alternatively, pre-register a fixed token and expose it via VITE_APP_CHECK_DEBUG_TOKEN.
if (import.meta.env.DEV) {
  // @ts-expect-error — property is not in the global TS types
  self.FIREBASE_APPCHECK_DEBUG_TOKEN =
    import.meta.env.VITE_APP_CHECK_DEBUG_TOKEN ?? true
}

// STEP 2: Initialize App Check IMMEDIATELY after app construction, BEFORE any
// read/write/callable. `isTokenAutoRefreshEnabled` keeps the token fresh during the session.
// Note: firebase.ts import (above) triggers initializeApp/getFirestore/getFunctions/getAuth
// but those construct lazy clients — no network calls fire until a read/write happens.
const recaptchaSiteKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY
if (recaptchaSiteKey) {
  initializeAppCheck(app, {
    provider: new ReCaptchaV3Provider(recaptchaSiteKey),
    isTokenAutoRefreshEnabled: true,
  })
} else if (import.meta.env.DEV) {
  // In dev without a site key, skip App Check init entirely — the debug token flow
  // requires a provider registered in Firebase Console. Warn once.
  // eslint-disable-next-line no-console
  console.warn('[Phase 5] VITE_RECAPTCHA_SITE_KEY is empty. App Check disabled for this dev session.')
}

// STEP 3: Capture credentials from a returning OAuth redirect (post-signInWithRedirect).
// Runs once at module-load time. onAuthStateChanged (via useAuth) propagates the
// new user to the UI. Errors are logged and swallowed so the app still mounts.
// See web/src/features/auth/authProviders.ts:signInWithGoogle for the redirect-vs-popup
// rationale (Plan 14-04 UAT-7, 2026-05-22).
void getRedirectResult(auth).catch((err) => {
  console.error('[main] getRedirectResult failed:', err)
})

// STEP 4: Render React with providers wrapping App
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ActiveReservationProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </ActiveReservationProvider>
    </QueryClientProvider>
  </StrictMode>,
)
