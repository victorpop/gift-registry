import { useTranslation } from 'react-i18next'
import { MonoCaption } from '../../components/giftmaison'

/**
 * Atmospheric photo + quote overlay for the auth screen right column.
 * Desktop (≥ 1024 px) only — caller hides via lg:block (default hidden).
 *
 * Image: a curated atmospheric photograph. Self-hosted at
 * web/public/auth-editorial.jpg (downloaded once at plan execution time —
 * Plan 13-06 Task 1). Vite serves /auth-editorial.jpg directly from
 * web/public/. This removes the runtime dependency on the upstream Unsplash
 * CDN (offline emulator dev, first-paint determinism, no upstream drift).
 * Source photo: Unsplash photo-1513694203232-719a280e022f (housewarming
 * couple lifestyle).
 *
 * Bottom gradient overlay: linear-gradient(180deg, transparent 40%, rgba(ink, 0.53) 100%)
 * Quote: italic Display M 28 px gm.paper.
 * Attribution: mono caps gm.paper opacity 0.8.
 *
 * alt="" (decorative — quote provides semantic content; image is mood).
 */
export default function EditorialPhoto() {
  const { t } = useTranslation()
  const photoUrl = '/auth-editorial.jpg'

  return (
    <div className="hidden lg:block relative h-full min-h-[640px] overflow-hidden">
      <img
        src={photoUrl}
        alt=""
        className="absolute inset-0 w-full h-full object-cover"
        loading="lazy"
      />
      <div
        className="absolute inset-0 pointer-events-none"
        style={{ background: 'linear-gradient(180deg, transparent 40%, rgba(42, 36, 32, 0.53) 100%)' }}
        aria-hidden="true"
      />
      <div className="absolute bottom-10 left-10 right-10 max-w-[420px]">
        <p className="font-display italic text-[28px] text-gm-paper leading-[1.15] m-0">
          {t('web_auth.editorial_quote')}
        </p>
        <div className="mt-3">
          <MonoCaption size="sm" tone="faint" className="text-gm-paper/80">
            {t('web_auth.editorial_attribution')}
          </MonoCaption>
        </div>
      </div>
    </div>
  )
}
