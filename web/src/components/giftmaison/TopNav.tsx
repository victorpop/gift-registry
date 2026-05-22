import { useTranslation } from 'react-i18next'
import { useAuth } from '../../features/auth/useAuth'
import { Wordmark } from './Wordmark'
import { Btn } from './Btn'
import LanguageSwitcher from '../LanguageSwitcher'
import { UserMenu } from './UserMenu'

export interface TopNavProps {
  /**
   * Optional click handler for the "Sign in" button on the nav. When provided,
   * the button calls onSignInClick (e.g., to open the AuthModal). When omitted,
   * the button is rendered as an <a href="/sign-in"> link (Plan 06 hooks the route).
   */
  onSignInClick?: () => void
}

/**
 * Shared top navigation chrome (CONTEXT D-14 + UI-SPEC).
 *
 * Layout:
 *   [Wordmark size=24 withTag] ………… [LanguageSwitcher] [Sign in ghost / avatar circle]
 *
 * - 1 px gm.line bottom border
 * - bg-gm-paper, padding 20/40 (desktop), 16/16 (mobile via responsive utilities)
 * - No hamburger collapse — keep all three slots visible at every breakpoint (D-14)
 *
 * Authenticated users get an olive avatar circle (gm.second bg, gm.paper fg)
 * with their initials. Anonymous users get the "Sign in" ghost button.
 */
export function TopNav({ onSignInClick }: TopNavProps) {
  const { t } = useTranslation()
  const { user, isReady } = useAuth()

  const initials = user?.displayName
    ? user.displayName.trim().split(/\s+/).map(p => p[0]?.toUpperCase()).slice(0, 2).join('')
    : user?.email
      ? user.email[0]?.toUpperCase() ?? ''
      : ''

  return (
    <nav className="flex items-center justify-between bg-gm-paper border-b border-gm-line px-4 py-4 sm:px-7 md:px-10 sm:py-5">
      <Wordmark size={24} withTag />
      <div className="flex items-center gap-5">
        <LanguageSwitcher />
        {/*
          Gate ONLY the auth indicator on isReady — wordmark + LanguageSwitcher
          render normally during cold-boot. Without this gate, the "Sign in"
          button briefly flashes before getRedirectResult resolves and
          onAuthStateChanged propagates the post-redirect user (Plan 14-04
          UAT-7 polish).
        */}
        {!isReady ? null : user ? (
          <UserMenu user={user} initials={initials || 'A'} />
        ) : onSignInClick ? (
          <Btn variant="ghost" size="sm" onClick={onSignInClick}>
            {t('auth.sign_in_link')}
          </Btn>
        ) : (
          <a
            href="/sign-in"
            className="inline-flex items-center justify-center gap-2 rounded-full border border-gm-line bg-transparent text-gm-ink font-body font-medium tracking-[-0.1px] leading-none cursor-pointer px-3 py-[7px] text-[12px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
          >
            {t('auth.sign_in_link')}
          </a>
        )}
      </div>
    </nav>
  )
}

export default TopNav
