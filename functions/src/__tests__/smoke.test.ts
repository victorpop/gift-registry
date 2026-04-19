import { createFakeFirestore } from './fixtures/firestore';

describe('Jest harness smoke test', () => {
  it('runs a trivial assertion', () => {
    expect(1 + 1).toBe(2);
  });

  it('createFakeFirestore seeds + reads a doc', async () => {
    const db = createFakeFirestore({ 'users': { 'u1': { email: 'a@b.com' } } });
    const snap = await db.doc('users/u1').get();
    expect((snap as { exists: boolean }).exists).toBe(true);
  });
});
