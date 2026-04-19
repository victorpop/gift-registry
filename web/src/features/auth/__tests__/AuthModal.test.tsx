import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import '../../../i18n'

const providerMocks = vi.hoisted(() => ({
  signInEmail: vi.fn(),
  signUpEmail: vi.fn(),
  signInWithGoogle: vi.fn(),
}))
vi.mock('../authProviders', () => providerMocks)

import AuthModal from '../AuthModal'

describe('AuthModal', () => {
  beforeEach(() => {
    providerMocks.signInEmail.mockReset()
    providerMocks.signUpEmail.mockReset()
    providerMocks.signInWithGoogle.mockReset()
    providerMocks.signInWithGoogle.mockResolvedValue({ uid: 'u1' })
    providerMocks.signInEmail.mockResolvedValue({ uid: 'u1' })
    providerMocks.signUpEmail.mockResolvedValue({ uid: 'u1' })
  })

  it('renders Sign In heading by default', () => {
    render(<AuthModal open onOpenChange={() => {}} onContinueAsGuest={() => {}} />)
    expect(screen.getByRole('heading', { name: 'Sign In' })).toBeInTheDocument()
  })

  it('switches to Create Account when the signup tab is clicked', async () => {
    const user = userEvent.setup()
    render(<AuthModal open onOpenChange={() => {}} onContinueAsGuest={() => {}} />)
    await user.click(screen.getByRole('tab', { name: 'Create Account' }))
    expect(screen.getByRole('heading', { name: 'Create Account' })).toBeInTheDocument()
  })

  it('invokes signInWithGoogle when the Google button is clicked', async () => {
    const user = userEvent.setup()
    const onOpenChange = vi.fn()
    render(<AuthModal open onOpenChange={onOpenChange} onContinueAsGuest={() => {}} />)
    await user.click(screen.getByRole('button', { name: /Continue with Google/ }))
    expect(providerMocks.signInWithGoogle).toHaveBeenCalled()
  })

  it('calls onContinueAsGuest when the guest link is clicked', async () => {
    const user = userEvent.setup()
    const onContinueAsGuest = vi.fn()
    const onOpenChange = vi.fn()
    render(<AuthModal open onOpenChange={onOpenChange} onContinueAsGuest={onContinueAsGuest} />)
    await user.click(screen.getByRole('button', { name: 'Continue as guest' }))
    expect(onOpenChange).toHaveBeenCalledWith(false)
    expect(onContinueAsGuest).toHaveBeenCalled()
  })

  it('calls signInEmail on sign-in submit', async () => {
    const user = userEvent.setup()
    render(<AuthModal open onOpenChange={() => {}} onContinueAsGuest={() => {}} />)
    await user.type(screen.getByLabelText('Email'), 'a@b.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Sign In' }))
    expect(providerMocks.signInEmail).toHaveBeenCalledWith('a@b.com', 'secret123')
  })

  it('calls signUpEmail on create-account submit', async () => {
    const user = userEvent.setup()
    render(<AuthModal open onOpenChange={() => {}} onContinueAsGuest={() => {}} />)
    await user.click(screen.getByRole('tab', { name: 'Create Account' }))
    await user.type(screen.getByLabelText('Email'), 'new@x.com')
    await user.type(screen.getByLabelText('Password'), 'supersecret')
    await user.click(screen.getByRole('button', { name: 'Create Account' }))
    expect(providerMocks.signUpEmail).toHaveBeenCalledWith('new@x.com', 'supersecret')
  })
})
