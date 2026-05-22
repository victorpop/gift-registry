import { initializeApp, type FirebaseApp } from 'firebase/app'
import { getFirestore, connectFirestoreEmulator, type Firestore } from 'firebase/firestore'
import { getFunctions, connectFunctionsEmulator, type Functions } from 'firebase/functions'
import { getAuth, connectAuthEmulator, setPersistence, browserLocalPersistence, type Auth } from 'firebase/auth'
import { initializeAppCheck, ReCaptchaV3Provider } from 'firebase/app-check'

// CRITICAL: Region pin. Firebase JS SDK defaults to us-central1 if second arg is omitted.
// Same defect was fixed in AppModule.kt on 2026-04-19. Do NOT remove this constant.
const FUNCTIONS_REGION = 'europe-west3'

// App Check debug token MUST be set BEFORE initializeAppCheck.
// Setting to `true` auto-generates a UUID on first run — copy it from the browser console
// and register it at Firebase Console > App Check > Debug tokens.
// Alternatively, pre-register a fixed token and expose it via VITE_APP_CHECK_DEBUG_TOKEN.
// Lives here (and not in main.tsx) because firebase.ts module-eval initializes App Check
// below, and that must happen AFTER the debug token is on `self` — Plan 14-04 dedupe
// 2026-05-22.
if (import.meta.env.DEV) {
  // @ts-expect-error — property is not in the global TS types
  self.FIREBASE_APPCHECK_DEBUG_TOKEN =
    import.meta.env.VITE_APP_CHECK_DEBUG_TOKEN ?? true
}

const firebaseConfig = {
  apiKey:            import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain:        import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId:         import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket:     import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId:             import.meta.env.VITE_FIREBASE_APP_ID,
}

export const app: FirebaseApp = initializeApp(firebaseConfig)

// App Check — reCAPTCHA v3 in production (WEB-D-18). Site key from .env.local.
// Skipped in emulator mode (emulators don't enforce App Check) and when the key is
// empty (defensive guard so a missing-env build doesn't crash at module-eval time).
// Single source of truth — main.tsx no longer initializes App Check (Plan 14-04 dedupe
// 2026-05-22).
if (import.meta.env.VITE_USE_EMULATORS !== 'true' && import.meta.env.VITE_RECAPTCHA_SITE_KEY) {
  initializeAppCheck(app, {
    provider: new ReCaptchaV3Provider(import.meta.env.VITE_RECAPTCHA_SITE_KEY),
    isTokenAutoRefreshEnabled: true,
  })
}

export const db: Firestore = getFirestore(app)
export const functions: Functions = getFunctions(app, FUNCTIONS_REGION)
export const auth: Auth = getAuth(app)

// WEB-D-12: persist sessions across tab close (parity with Android AUTH-04)
void setPersistence(auth, browserLocalPersistence)

// Emulator wiring — ports match /firebase.json
if (import.meta.env.VITE_USE_EMULATORS === 'true') {
  // connectAuthEmulator: disable the "are you sure?" warning banner (only visible when loaded
  // from the emulator host) by passing { disableWarnings: true } — second positional arg
  connectAuthEmulator(auth, 'http://localhost:9099', { disableWarnings: true })
  connectFirestoreEmulator(db, 'localhost', 8080)
  connectFunctionsEmulator(functions, 'localhost', 5001)
}
