import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import i18n from '../../../i18n'
import type { User } from 'firebase/auth'

const providerMocks = vi.hoisted(() => ({
  signOut: vi.fn(),
}))
vi.mock('../../../features/auth/authProviders', () => providerMocks)

// Stub the firebase module so jsdom doesn't choke on a real config
// (matches the pattern in AuthModal.test.tsx).
vi.mock('../../../firebase', () => ({
  auth: { _kind: 'fakeAuth' },
  app: { _kind: 'fakeApp' },
  db: { _kind: 'fakeDb' },
  functions: { _kind: 'fakeFunctions' },
}))

const toastMocks = vi.hoisted(() => ({ showToast: vi.fn() }))
vi.mock('../../ToastProvider', () => ({
  useToast: () => toastMocks,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

import { UserMenu } from '../UserMenu'

const fakeUser = {
  uid: 'u1',
  email: 'jane@example.com',
  displayName: 'Jane Doe',
} as unknown as User

describe('UserMenu', () => {
  beforeAll(async () => {
    // Ensure i18n is initialized before any test renders components
    await i18n.init()
    await i18n.changeLanguage('en')
  })

  beforeEach(async () => {
    await i18n.changeLanguage('en')
    providerMocks.signOut.mockReset()
    providerMocks.signOut.mockResolvedValue(undefined)
    toastMocks.showToast.mockReset()
  })

  it('renders avatar trigger with localized aria-label and initials', () => {
    render(<UserMenu user={fakeUser} initials="JD" />)
    const trigger = screen.getByRole('button', { name: /account menu for jane doe/i })
    expect(trigger).toHaveTextContent('JD')
    // Menu content is portal-mounted lazily — Sign out item should not be in DOM yet.
    expect(screen.queryByRole('menuitem', { name: /sign out/i })).not.toBeInTheDocument()
  })

  it('opens menu on trigger click and reveals Sign out item', async () => {
    const user = userEvent.setup()
    render(<UserMenu user={fakeUser} initials="JD" />)
    await user.click(screen.getByRole('button', { name: /account menu for jane doe/i }))
    expect(await screen.findByRole('menuitem', { name: /sign out/i })).toBeInTheDocument()
  })

  it('calls signOut() when Sign out menuitem is selected', async () => {
    const user = userEvent.setup()
    render(<UserMenu user={fakeUser} initials="JD" />)
    await user.click(screen.getByRole('button', { name: /account menu for jane doe/i }))
    await user.click(await screen.findByRole('menuitem', { name: /sign out/i }))
    await waitFor(() => expect(providerMocks.signOut).toHaveBeenCalledTimes(1))
    expect(toastMocks.showToast).not.toHaveBeenCalled()
  })

  it('shows an error toast when signOut() rejects', async () => {
    providerMocks.signOut.mockRejectedValueOnce(new Error('network'))
    const user = userEvent.setup()
    render(<UserMenu user={fakeUser} initials="JD" />)
    await user.click(screen.getByRole('button', { name: /account menu for jane doe/i }))
    await user.click(await screen.findByRole('menuitem', { name: /sign out/i }))
    await waitFor(() =>
      expect(toastMocks.showToast).toHaveBeenCalledWith(
        expect.stringMatching(/something went wrong/i),
        'error',
      ),
    )
  })
})
