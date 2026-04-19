import * as Toast from '@radix-ui/react-toast'
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

export type ToastVariant = 'success' | 'error' | 'neutral'

interface ToastItem {
  id: number
  title: string
  variant: ToastVariant
}

interface Ctx {
  showToast: (title: string, variant?: ToastVariant) => void
}

const ToastContext = createContext<Ctx | null>(null)

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within <ToastProvider />')
  return ctx
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const showToast = useCallback((title: string, variant: ToastVariant = 'neutral') => {
    setItems((prev) => [...prev, { id: Date.now() + Math.random(), title, variant }])
  }, [])

  const value = useMemo<Ctx>(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      <Toast.Provider swipeDirection="down" duration={5000}>
        {children}
        {items.map((t) => {
          const borderClass =
            t.variant === 'success'  ? 'border-l-primary' :
            t.variant === 'error'    ? 'border-l-destructive' :
                                       'border-l-outline'
          return (
            <Toast.Root
              key={t.id}
              onOpenChange={(open) => {
                if (!open) setItems((prev) => prev.filter((x) => x.id !== t.id))
              }}
              className={`bg-surface text-surface-on rounded-md shadow-lg p-4 border-l-4 ${borderClass} data-[state=open]:animate-in data-[state=closed]:animate-out`}
            >
              <Toast.Title className="text-base font-semibold leading-tight">{t.title}</Toast.Title>
            </Toast.Root>
          )
        })}
        <Toast.Viewport
          className="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 w-[min(420px,90vw)] outline-none"
        />
      </Toast.Provider>
    </ToastContext.Provider>
  )
}
