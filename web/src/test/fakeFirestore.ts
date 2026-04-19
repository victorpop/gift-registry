/**
 * In-memory Firestore fake for Vitest component tests.
 * Does NOT replicate full Firestore semantics — just enough to drive component logic.
 *
 * Plan 04+ tests use `vi.mock('firebase/firestore')` and drive this fake via `useRegistryQuery` mocks.
 * Emulator-dependent tests live in web/e2e/ (Playwright).
 */

export type DocData = Record<string, unknown>

export interface FakeDocSnapshot {
  id: string
  exists: () => boolean
  data: () => DocData | undefined
}

export interface FakeQuerySnapshot {
  docs: FakeDocSnapshot[]
  size: number
  empty: boolean
}

type Listener<T> = (snapshot: T) => void
type ErrorListener = (err: Error) => void

class FakeDocRef {
  constructor(public path: string, public id: string, private store: FakeFirestore) {}

  onSnapshot(onNext: Listener<FakeDocSnapshot>, onError?: ErrorListener): () => void {
    return this.store.subscribeDoc(this.path, onNext, onError)
  }
}

class FakeCollectionRef {
  constructor(public path: string, private store: FakeFirestore) {}

  onSnapshot(onNext: Listener<FakeQuerySnapshot>, onError?: ErrorListener): () => void {
    return this.store.subscribeCollection(this.path, onNext, onError)
  }
}

export class FakeFirestore {
  private docs = new Map<string, DocData>()
  private docListeners = new Map<string, Set<{ onNext: Listener<FakeDocSnapshot>; onError?: ErrorListener }>>()
  private collectionListeners = new Map<string, Set<{ onNext: Listener<FakeQuerySnapshot>; onError?: ErrorListener }>>()
  private errors = new Map<string, Error>()

  setDoc(path: string, data: DocData) {
    this.docs.set(path, data)
    this.errors.delete(path)
    this.emit(path)
  }

  deleteDoc(path: string) {
    this.docs.delete(path)
    this.emit(path)
  }

  setError(path: string, err: Error) {
    this.errors.set(path, err)
    const docListeners = this.docListeners.get(path)
    docListeners?.forEach((l) => l.onError?.(err))
    const collPath = path
    const collListeners = this.collectionListeners.get(collPath)
    collListeners?.forEach((l) => l.onError?.(err))
  }

  doc(path: string, id: string) {
    return new FakeDocRef(`${path}/${id}`, id, this)
  }

  collection(path: string) {
    return new FakeCollectionRef(path, this)
  }

  subscribeDoc(path: string, onNext: Listener<FakeDocSnapshot>, onError?: ErrorListener): () => void {
    if (!this.docListeners.has(path)) this.docListeners.set(path, new Set())
    const listener = { onNext, onError }
    this.docListeners.get(path)!.add(listener)
    // Emit current state synchronously (matches onSnapshot behaviour)
    queueMicrotask(() => {
      if (this.errors.has(path)) {
        onError?.(this.errors.get(path)!)
      } else {
        onNext(this.snapshotOf(path))
      }
    })
    return () => {
      this.docListeners.get(path)?.delete(listener)
    }
  }

  subscribeCollection(path: string, onNext: Listener<FakeQuerySnapshot>, onError?: ErrorListener): () => void {
    if (!this.collectionListeners.has(path)) this.collectionListeners.set(path, new Set())
    const listener = { onNext, onError }
    this.collectionListeners.get(path)!.add(listener)
    queueMicrotask(() => onNext(this.querySnapshotOf(path)))
    return () => {
      this.collectionListeners.get(path)?.delete(listener)
    }
  }

  private emit(path: string) {
    const docListeners = this.docListeners.get(path)
    docListeners?.forEach((l) => l.onNext(this.snapshotOf(path)))

    // Emit to any collection listeners whose path is a prefix of this doc's path
    for (const [collPath, listeners] of this.collectionListeners.entries()) {
      if (path.startsWith(collPath + '/')) {
        listeners.forEach((l) => l.onNext(this.querySnapshotOf(collPath)))
      }
    }
  }

  private snapshotOf(path: string): FakeDocSnapshot {
    const data = this.docs.get(path)
    const id = path.split('/').pop() || ''
    return {
      id,
      exists: () => data !== undefined,
      data: () => data,
    }
  }

  private querySnapshotOf(collPath: string): FakeQuerySnapshot {
    const docs: FakeDocSnapshot[] = []
    for (const [path, data] of this.docs.entries()) {
      const prefix = collPath + '/'
      if (path.startsWith(prefix) && !path.slice(prefix.length).includes('/')) {
        const id = path.slice(prefix.length)
        docs.push({
          id,
          exists: () => true,
          data: () => data,
        })
      }
    }
    return { docs, size: docs.length, empty: docs.length === 0 }
  }

  reset() {
    this.docs.clear()
    this.docListeners.clear()
    this.collectionListeners.clear()
    this.errors.clear()
  }
}

export const fakeFirestore = new FakeFirestore()
