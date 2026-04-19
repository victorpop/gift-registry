import { initializeApp, type FirebaseApp } from 'firebase/app'
import { getFirestore, connectFirestoreEmulator, type Firestore } from 'firebase/firestore'
import { getFunctions, connectFunctionsEmulator, type Functions } from 'firebase/functions'
import { getAuth, connectAuthEmulator, setPersistence, browserLocalPersistence, type Auth } from 'firebase/auth'

// CRITICAL: Region pin. Firebase JS SDK defaults to us-central1 if second arg is omitted.
// Same defect was fixed in AppModule.kt on 2026-04-19. Do NOT remove this constant.
const FUNCTIONS_REGION = 'europe-west3'

const firebaseConfig = {
  apiKey:            import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain:        import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId:         import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket:     import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId:             import.meta.env.VITE_FIREBASE_APP_ID,
}

export const app: FirebaseApp = initializeApp(firebaseConfig)
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
