import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { getRedirectResult } from 'firebase/auth'
import { QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import './i18n'
import App from './App'
import { queryClient } from './queryClient'
import { auth } from './firebase'
import { ToastProvider } from './components/ToastProvider'
import { ActiveReservationProvider } from './features/reservation/useActiveReservation'

// Capture credentials from a returning OAuth redirect (post-signInWithRedirect).
// Runs once at module-load time. onAuthStateChanged (via useAuth) propagates the
// new user to the UI. Errors are logged and swallowed so the app still mounts.
// See web/src/features/auth/authProviders.ts:signInWithGoogle for the redirect-vs-popup
// rationale (Plan 14-04 UAT-7, 2026-05-22).
// App Check init + FIREBASE_APPCHECK_DEBUG_TOKEN setup live in firebase.ts so they
// run before any Firebase service call (firebase.ts is imported above and its
// top-level statements execute before this module body — Plan 14-04 authDomain
// + App Check dedupe, 2026-05-22).
void getRedirectResult(auth).catch((err) => {
  console.error('[main] getRedirectResult failed:', err)
})

// Render React with providers wrapping App
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
