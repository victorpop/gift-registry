import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithRedirect,
  getRedirectResult,
  GoogleAuthProvider,
  signOut as fbSignOut,
  type User,
} from 'firebase/auth'
import { auth } from '../../firebase'

export async function signInEmail(email: string, password: string): Promise<User> {
  const res = await signInWithEmailAndPassword(auth, email, password)
  return res.user
}

export async function signUpEmail(email: string, password: string): Promise<User> {
  const res = await createUserWithEmailAndPassword(auth, email, password)
  return res.user
}

/**
 * Initiates Google sign-in via full-page redirect. Returns immediately after
 * triggering the navigation; the browser then leaves the page entirely. When
 * Google redirects the user back to this app, getRedirectResult() (called once
 * on app boot in main.tsx) captures the credentials and onAuthStateChanged
 * fires. Callers should NOT navigate or do post-success work synchronously
 * after this resolves — the page is about to unload.
 *
 * Switched from signInWithPopup on 2026-05-22 because the popup-to-opener
 * postMessage channel is unreliable in modern Chrome and Safari (Plan 14-04
 * UAT-7 found that the popup completed and persisted credentials but the
 * opener tab's in-memory Auth state never updated without a manual refresh).
 */
export async function signInWithGoogle(): Promise<void> {
  const provider = new GoogleAuthProvider()
  await signInWithRedirect(auth, provider)
  // Page navigates to Google here; execution does not continue past this point.
}

export { getRedirectResult }

export async function signOut(): Promise<void> {
  await fbSignOut(auth)
}
