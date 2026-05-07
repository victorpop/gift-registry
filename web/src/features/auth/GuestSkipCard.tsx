import { useTranslation } from 'react-i18next'
import { Btn } from '../../components/giftmaison'

export interface GuestSkipCardProps {
  onSkip: () => void
}

/**
 * Dashed-border guest skip card (UI-SPEC Auth screen "Guest skip card" — bg-gm-paperDeep,
 * rounded-[10px], border 1 px DASHED gm-line, 18/20 padding, two-column flex
 * with copy block + Skip → ghost button).
 *
 * On the AuthScreen mobile layout, the parent wraps this in <StickyMobileBar />
 * to pin it to the viewport bottom (D-05 mandate: "must be reachable in 1 tap").
 */
export default function GuestSkipCard({ onSkip }: GuestSkipCardProps) {
  const { t } = useTranslation()
  return (
    <div
      className="flex items-center gap-4 p-[18px_20px] bg-gm-paperDeep rounded-[10px] border border-dashed border-gm-line"
      data-testid="guest-skip-card"
    >
      <div className="flex-1 min-w-0">
        <h4 className="m-0 font-body text-[14px] font-medium text-gm-ink leading-tight">
          {t('web_auth.guest_skip_title')}
        </h4>
        <p className="m-0 mt-1 font-body text-[12.5px] text-gm-inkSoft leading-[1.4]">
          {t('web_auth.guest_skip_body')}
        </p>
      </div>
      <Btn variant="ghost" size="sm" onClick={onSkip} className="flex-shrink-0">
        {t('web_auth.guest_skip_cta')}
      </Btn>
    </div>
  )
}
