import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
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

export async function signInWithGoogle(): Promise<User | null> {
  const provider = new GoogleAuthProvider()
  try {
    const res = await signInWithPopup(auth, provider)
    return res.user
  } catch (err: unknown) {
    // User-closed popup is not an error we want to surface — return null so the caller
    // can silently no-op instead of showing a toast.
    const code = (err as { code?: string })?.code
    if (code === 'auth/popup-closed-by-user' || code === 'auth/cancelled-popup-request') {
      return null
    }
    throw err
  }
}

export async function signOut(): Promise<void> {
  await fbSignOut(auth)
}
