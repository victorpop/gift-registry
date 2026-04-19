import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import '../i18n'
import AppRootPage from '../pages/AppRootPage'
import RegistryPage from '../pages/RegistryPage'
import ReReservePage from '../pages/ReReservePage'
import NotFoundPage from '../pages/NotFoundPage'

function renderAt(path: string) {
  const router = createMemoryRouter(
    [
      { path: '/',                           element: <AppRootPage /> },
      { path: '/registry/:id',               element: <RegistryPage /> },
      { path: '/reservation/:id/re-reserve', element: <ReReservePage /> },
      { path: '*',                           element: <NotFoundPage /> },
    ],
    { initialEntries: [path] },
  )
  return render(<RouterProvider router={router} />)
}

describe('App router', () => {
  it('renders AppRootPage at /', () => {
    renderAt('/')
    expect(screen.getByText('Gift Registry')).toBeInTheDocument()
    expect(screen.getByText('Download for Android')).toBeInTheDocument()
  })

  it('renders RegistryPage at /registry/:id with the id param visible', () => {
    renderAt('/registry/abc123')
    expect(screen.getByText(/Registry abc123/)).toBeInTheDocument()
  })

  it('renders ReReservePage at /reservation/:id/re-reserve', () => {
    renderAt('/reservation/res-999/re-reserve')
    expect(screen.getByText('Checking your reservation\u2026')).toBeInTheDocument()
  })

  it('renders NotFoundPage at an unknown path', () => {
    renderAt('/totally-not-a-real-path')
    expect(screen.getByText('Registry not available')).toBeInTheDocument()
  })
})
