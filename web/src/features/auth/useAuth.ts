import { useEffect, useState } from 'react'
import { onAuthStateChanged, type User } from 'firebase/auth'
import { auth } from '../../firebase'

/**
 * Subscribes to Firebase Auth state changes.
 *
 * Returns:
 *   - isReady === false before the first onAuthStateChanged emission
 *     (use this to gate cold-start behavior — WEB-D Pitfall 7)
 *   - isReady === true once Firebase has resolved the persisted session
 *     (user may still be null if no one is signed in)
 */
export function useAuth() {
  const [user, setUser] = useState<User | null>(null)
  const [isReady, setIsReady] = useState(false)

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(u)
      setIsReady(true)
    })
    return unsub
  }, [])

  return { user, isReady }
}
