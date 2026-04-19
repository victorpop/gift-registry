import { initializeApp, deleteApp, getApps } from 'firebase-admin/app';

export function ensureAdminInitialized() {
  if (getApps().length === 0) {
    initializeApp({ projectId: 'gift-registry-test' });
  }
}

export async function cleanupAdmin() {
  await Promise.all(getApps().map(deleteApp));
}
