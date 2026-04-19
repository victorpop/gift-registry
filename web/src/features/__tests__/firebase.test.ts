import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'

// Hoisted mock factories — vi.hoisted is required because vi.mock factories run before imports
const mocks = vi.hoisted(() => ({
  initializeApp: vi.fn(() => ({ name: 'test-app' })),
  getFirestore: vi.fn(() => ({ type: 'firestore' })),
  getFunctions: vi.fn(() => ({ type: 'functions' })),
  getAuth: vi.fn(() => ({ type: 'auth' })),
  setPersistence: vi.fn(() => Promise.resolve()),
  connectFirestoreEmulator: vi.fn(),
  connectFunctionsEmulator: vi.fn(),
  connectAuthEmulator: vi.fn(),
  browserLocalPersistence: { type: 'browserLocalPersistence' },
}))

vi.mock('firebase/app', () => ({ initializeApp: mocks.initializeApp }))
vi.mock('firebase/firestore', () => ({
  getFirestore: mocks.getFirestore,
  connectFirestoreEmulator: mocks.connectFirestoreEmulator,
}))
vi.mock('firebase/functions', () => ({
  getFunctions: mocks.getFunctions,
  connectFunctionsEmulator: mocks.connectFunctionsEmulator,
}))
vi.mock('firebase/auth', () => ({
  getAuth: mocks.getAuth,
  setPersistence: mocks.setPersistence,
  connectAuthEmulator: mocks.connectAuthEmulator,
  browserLocalPersistence: mocks.browserLocalPersistence,
}))

describe('firebase.ts', () => {
  beforeEach(() => {
    vi.resetModules()
    Object.values(mocks).forEach((m) => {
      if (typeof m === 'function' && 'mockReset' in m) m.mockReset()
    })
    mocks.initializeApp.mockReturnValue({ name: 'test-app' } as never)
    mocks.getFirestore.mockReturnValue({ type: 'firestore' } as never)
    mocks.getFunctions.mockReturnValue({ type: 'functions' } as never)
    mocks.getAuth.mockReturnValue({ type: 'auth' } as never)
    mocks.setPersistence.mockResolvedValue(undefined as never)
  })

  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('pins getFunctions to europe-west3 (WEB-D-17)', async () => {
    vi.stubEnv('VITE_USE_EMULATORS', 'false')
    await import('../../firebase')
    expect(mocks.getFunctions).toHaveBeenCalledWith(
      expect.anything(),
      'europe-west3',
    )
  })

  it('uses browserLocalPersistence (WEB-D-12)', async () => {
    vi.stubEnv('VITE_USE_EMULATORS', 'false')
    await import('../../firebase')
    expect(mocks.setPersistence).toHaveBeenCalledWith(
      expect.anything(),
      mocks.browserLocalPersistence,
    )
  })

  it('connects emulators when VITE_USE_EMULATORS=true', async () => {
    vi.stubEnv('VITE_USE_EMULATORS', 'true')
    await import('../../firebase')
    expect(mocks.connectAuthEmulator).toHaveBeenCalledWith(
      expect.anything(),
      'http://localhost:9099',
      { disableWarnings: true },
    )
    expect(mocks.connectFirestoreEmulator).toHaveBeenCalledWith(
      expect.anything(),
      'localhost',
      8080,
    )
    expect(mocks.connectFunctionsEmulator).toHaveBeenCalledWith(
      expect.anything(),
      'localhost',
      5001,
    )
  })

  it('does NOT connect emulators when VITE_USE_EMULATORS is not true', async () => {
    vi.stubEnv('VITE_USE_EMULATORS', 'false')
    await import('../../firebase')
    expect(mocks.connectAuthEmulator).not.toHaveBeenCalled()
    expect(mocks.connectFirestoreEmulator).not.toHaveBeenCalled()
    expect(mocks.connectFunctionsEmulator).not.toHaveBeenCalled()
  })
})
