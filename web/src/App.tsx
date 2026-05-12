import { createBrowserRouter, RouterProvider } from 'react-router'
import AppRootPage from './pages/AppRootPage'
import RegistryPage from './pages/RegistryPage'
import ItemDetailPage from './pages/ItemDetailPage'
import ReReservePage from './pages/ReReservePage'
import NotFoundPage from './pages/NotFoundPage'
import AuthScreen from './features/auth/AuthScreen'

const router = createBrowserRouter([
  { path: '/',                               element: <AppRootPage /> },
  { path: '/registry/:id',                   element: <RegistryPage />,    errorElement: <NotFoundPage /> },
  { path: '/registry/:id/item/:itemId',      element: <ItemDetailPage />,  errorElement: <NotFoundPage /> },
  { path: '/reservation/:id/re-reserve',     element: <ReReservePage /> },
  { path: '/sign-in',                        element: <AuthScreen /> },
  { path: '*',                               element: <NotFoundPage /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
