/**
 * In-memory Firestore double for unit tests.
 * Downstream plans import createFakeFirestore to mock Firestore without firebase-admin.
 */

type Store = Record<string, Record<string, Record<string, unknown>>>;

function parseDocPath(path: string): { collectionPath: string; docId: string } {
  const parts = path.split('/');
  // path like "users/u1" or "users/u1/fcmTokens/tok1"
  // last part is docId, everything before is collection path
  const docId = parts[parts.length - 1];
  const collectionPath = parts.slice(0, parts.length - 1).join('/');
  return { collectionPath, docId };
}

function makeDocRef(store: Store, collectionPath: string, docId: string) {
  return {
    id: docId,
    path: `${collectionPath}/${docId}`,
    async get() {
      const col = store[collectionPath];
      const data = col ? col[docId] : undefined;
      return {
        exists: data !== undefined,
        id: docId,
        data: () => (data ? { ...data } : undefined),
      };
    },
    async set(data: Record<string, unknown>) {
      if (!store[collectionPath]) store[collectionPath] = {};
      store[collectionPath][docId] = { ...data };
    },
    async update(data: Record<string, unknown>) {
      if (!store[collectionPath]) store[collectionPath] = {};
      store[collectionPath][docId] = {
        ...(store[collectionPath][docId] || {}),
        ...data,
      };
    },
    async delete() {
      if (store[collectionPath]) {
        delete store[collectionPath][docId];
      }
    },
    collection(subCollection: string) {
      return makeCollectionRef(store, `${collectionPath}/${docId}/${subCollection}`);
    },
  };
}

function makeCollectionRef(store: Store, collectionPath: string) {
  return {
    doc(id: string) {
      return makeDocRef(store, collectionPath, id);
    },
    async add(data: Record<string, unknown>) {
      const id = `auto_${Date.now()}_${Math.random().toString(36).slice(2)}`;
      if (!store[collectionPath]) store[collectionPath] = {};
      store[collectionPath][id] = { ...data };
      return makeDocRef(store, collectionPath, id);
    },
    async get() {
      const col = store[collectionPath] || {};
      const docs = Object.entries(col).map(([id, data]) => ({
        id,
        exists: true,
        data: () => ({ ...data }),
        ref: makeDocRef(store, collectionPath, id),
      }));
      return {
        docs,
        empty: docs.length === 0,
        size: docs.length,
      };
    },
  };
}

export function createFakeFirestore(seed: Store = {}) {
  // Deep-copy seed to avoid mutation
  const store: Store = JSON.parse(JSON.stringify(seed));

  return {
    __store: store,
    collection(path: string) {
      return makeCollectionRef(store, path);
    },
    doc(path: string) {
      const { collectionPath, docId } = parseDocPath(path);
      return makeDocRef(store, collectionPath, docId);
    },
    async runTransaction(fn: (tx: ReturnType<typeof makeTx>) => Promise<unknown>) {
      const tx = makeTx(store);
      return fn(tx);
    },
  };
}

function makeTx(store: Store) {
  return {
    async get(ref: ReturnType<typeof makeDocRef>) {
      return ref.get();
    },
    set(ref: ReturnType<typeof makeDocRef>, data: Record<string, unknown>) {
      ref.set(data);
    },
    update(ref: ReturnType<typeof makeDocRef>, data: Record<string, unknown>) {
      ref.update(data);
    },
    delete(ref: ReturnType<typeof makeDocRef>) {
      ref.delete();
    },
  };
}
