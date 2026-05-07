import { useTranslation } from 'react-i18next'
import { Btn, MonoCaption } from '../../components/giftmaison'
import { Share2 } from 'lucide-react'

export interface ProgressStripProps {
  /** Number of items chosen (reserved + purchased). */
  totalChosen: number
  /** Total items in registry. */
  total: number
  /** Click handler for the Share button. Defaults to navigator.share() when present, else copies registry URL to clipboard. */
  onShare?: () => void
}

/**
 * Hero-side progress widget. Shows mono-caps PROGRESS label + a large
 * Display M number + 'of {total} chosen' subline + a 4 px progress bar in
 * gm.accent + a Share ghost button.
 *
 * Layout: vertical stack inside a paperDeep rounded-[10px] surface, padding 14/18 px.
 * On mobile (< 1024 px) this drops below the hero (full-bleed); on desktop ≥ 1024 px
 * it sits to the right of the hero (parent grid handles placement).
 */
export function ProgressStrip({ totalChosen, total, onShare }: ProgressStripProps) {
  const { t } = useTranslation()
  const pct = total > 0 ? Math.min(100, Math.round((totalChosen / total) * 100)) : 0

  return (
    <div className="flex flex-col gap-3 p-[14px_18px] bg-gm-paperDeep rounded-[10px] min-w-[200px]">
      <MonoCaption size="micro" tone="faint">{t('web_hero.progress_label')}</MonoCaption>
      <div className="flex items-baseline gap-[6px]">
        <span className="font-display text-[34px] text-gm-ink leading-none">{totalChosen}</span>
        <span className="font-body text-[14px] text-gm-inkFaint">
          {t('web_hero.progress_copy', { n: totalChosen, total })}
        </span>
      </div>
      <div className="h-1 bg-gm-line rounded-[2px] overflow-hidden">
        <div className="h-full bg-gm-accent transition-[width] duration-500 ease-out" style={{ width: `${pct}%` }} />
      </div>
      <Btn variant="ghost" size="sm" onClick={onShare} icon={<Share2 className="w-3.5 h-3.5" aria-hidden="true" />}>
        {t('web_hero.share_button')}
      </Btn>
    </div>
  )
}

export default ProgressStrip
