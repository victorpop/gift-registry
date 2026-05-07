import { useTranslation } from 'react-i18next'
import { ChevronUp } from 'lucide-react'
import { MonoCaption } from '../../components/giftmaison'

export interface HowTimerWorksProps {
  /** Retailer label substituted into step 2's headline. */
  retailer: string
}

/**
 * "How the timer works" 4-step explanation (UI-SPEC Section 5 of Reserve Detail).
 *
 * Layout:
 *   - Mobile (< 1024 px): renders inside a <details> element collapsed by default.
 *     <summary> shows the heading + chevron-up icon (rotates open via group-open:rotate-180).
 *   - Desktop (>= 1024 px): always-open variant via the parent grid (lg:grid-cols-[1fr_340px]).
 *     The component itself doesn't change layout — the parent imposes width.
 *
 * Container: bg-gm-paperDeep, rounded-gm-card, border-gm-line, padding 24 px.
 * Each step: 22 px circle badge (gm.ink bg, gm.paper fg, mono 11 px, numeric 1-4)
 * + text block (Body M 13.5 px gm.ink + Body S 12.5 px gm.inkSoft).
 */
export default function HowTimerWorks({ retailer }: HowTimerWorksProps) {
  const { t } = useTranslation()

  const steps: { h: string; b: string }[] = [
    { h: t('web_reserve.how_timer_step1_h'), b: t('web_reserve.how_timer_step1_b') },
    { h: t('web_reserve.how_timer_step2_h', { retailer }), b: t('web_reserve.how_timer_step2_b') },
    { h: t('web_reserve.how_timer_step3_h'), b: t('web_reserve.how_timer_step3_b') },
    { h: t('web_reserve.how_timer_step4_h'), b: t('web_reserve.how_timer_step4_b') },
  ]

  const StepList = (
    <ol className="m-0 p-0 list-none flex flex-col gap-[14px] mt-4">
      {steps.map((s, i) => (
        <li key={i} className="flex gap-3">
          <span className="w-[22px] h-[22px] rounded-full bg-gm-ink text-gm-paper flex items-center justify-center font-mono text-[11px] flex-shrink-0">
            {i + 1}
          </span>
          <div>
            <div className="font-body text-[13.5px] text-gm-ink font-medium">{s.h}</div>
            <div className="font-body text-[12.5px] text-gm-inkSoft mt-[2px] leading-[1.45]">{s.b}</div>
          </div>
        </li>
      ))}
    </ol>
  )

  return (
    <aside className="bg-gm-paperDeep rounded-gm-card border border-gm-line p-6">
      {/* Mobile: <details> collapsible. Desktop: always-open via lg:open. */}
      <details className="group [&_summary::-webkit-details-marker]:hidden" open>
        <summary className="lg:hidden flex items-center justify-between cursor-pointer list-none">
          <MonoCaption size="micro" tone="faint">{t('web_reserve.how_timer_heading')}</MonoCaption>
          <ChevronUp className="w-4 h-4 text-gm-inkFaint transition-transform group-open:rotate-180" aria-hidden="true" />
        </summary>
        <div className="hidden lg:block">
          <MonoCaption size="micro" tone="faint">{t('web_reserve.how_timer_heading')}</MonoCaption>
        </div>
        {StepList}
      </details>
    </aside>
  )
}
