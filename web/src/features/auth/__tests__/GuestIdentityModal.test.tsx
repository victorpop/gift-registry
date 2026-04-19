import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import '../../../i18n'
import GuestIdentityModal from '../GuestIdentityModal'
import { GUEST_IDENTITY_STORAGE_KEY } from '../useGuestIdentity'

describe('GuestIdentityModal', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('renders empty fields when no stored identity', () => {
    render(<GuestIdentityModal open onOpenChange={() => {}} onSubmit={() => {}} />)
    expect(screen.getByText('Who are you?')).toBeInTheDocument()
    expect((screen.getByLabelText('First Name') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Last Name') as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('')
  })

  it('pre-fills from localStorage when identity exists', () => {
    localStorage.setItem(
      GUEST_IDENTITY_STORAGE_KEY,
      JSON.stringify({ firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' }),
    )
    render(<GuestIdentityModal open onOpenChange={() => {}} onSubmit={() => {}} />)
    expect((screen.getByLabelText('First Name') as HTMLInputElement).value).toBe('Ana')
    expect((screen.getByLabelText('Last Name') as HTMLInputElement).value).toBe('Pop')
    expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('ana@x.com')
  })

  it('shows validation errors for empty submission', async () => {
    const user = userEvent.setup()
    render(<GuestIdentityModal open onOpenChange={() => {}} onSubmit={() => {}} />)
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    await waitFor(() => {
      const alerts = screen.getAllByRole('alert')
      expect(alerts.length).toBeGreaterThanOrEqual(3)
    })
  })

  it('shows email-format error for invalid email', async () => {
    const user = userEvent.setup()
    render(<GuestIdentityModal open onOpenChange={() => {}} onSubmit={() => {}} />)
    await user.type(screen.getByLabelText('First Name'), 'A')
    await user.type(screen.getByLabelText('Last Name'), 'B')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    await waitFor(() => {
      expect(screen.getAllByRole('alert').some(a => a.textContent === 'email')).toBe(true)
    })
  })

  it('calls onSubmit with identity and persists to localStorage on valid submit', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    const onOpenChange = vi.fn()
    render(<GuestIdentityModal open onOpenChange={onOpenChange} onSubmit={onSubmit} />)
    await user.type(screen.getByLabelText('First Name'), 'Ana')
    await user.type(screen.getByLabelText('Last Name'), 'Pop')
    await user.type(screen.getByLabelText('Email'), 'ana@x.com')
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({ firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' })
    })
    expect(localStorage.getItem(GUEST_IDENTITY_STORAGE_KEY)).toBe(
      JSON.stringify({ firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' }),
    )
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })
})
