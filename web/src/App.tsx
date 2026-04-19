import { createBrowserRouter, RouterProvider } from 'react-router'
import AppRootPage from './pages/AppRootPage'
import RegistryPage from './pages/RegistryPage'
import ReReservePage from './pages/ReReservePage'
import NotFoundPage from './pages/NotFoundPage'

const router = createBrowserRouter([
  { path: '/',                           element: <AppRootPage /> },
  { path: '/registry/:id',               element: <RegistryPage />,  errorElement: <NotFoundPage /> },
  { path: '/reservation/:id/re-reserve', element: <ReReservePage /> },
  { path: '*',                           element: <NotFoundPage /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
