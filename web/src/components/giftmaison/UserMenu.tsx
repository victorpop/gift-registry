import { useTranslation } from 'react-i18next'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import type { User } from 'firebase/auth'
import { signOut } from '../../features/auth/authProviders'
import { useToast } from '../ToastProvider'

export interface UserMenuProps {
  user: User
  initials: string
}

export function UserMenu({ user, initials }: UserMenuProps) {
  const { t } = useTranslation()
  const { showToast } = useToast()
  const displayName = user.displayName ?? user.email ?? ''

  const handleSignOut = async () => {
    try {
      await signOut()
    } catch {
      showToast(t('common.error_generic'), 'error')
    }
  }

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger
        aria-label={t('auth.user_menu_label', { name: displayName })}
        className="w-8 h-8 rounded-full bg-gm-second text-gm-paper flex items-center justify-center font-body text-[12px] font-medium cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
      >
        {initials}
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={8}
          className="min-w-[160px] bg-gm-paper border border-gm-line rounded-lg shadow-lg p-1 z-50"
        >
          <DropdownMenu.Item
            onSelect={handleSignOut}
            className="font-body text-[13.5px] text-gm-ink px-3 py-2 rounded-md cursor-pointer outline-none data-[highlighted]:bg-gm-paperDeep data-[highlighted]:text-gm-ink"
          >
            {t('auth.sign_out')}
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}

export default UserMenu
