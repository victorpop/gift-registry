import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

export default function AppRootPage() {
  const { t } = useTranslation()
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-surface px-6">
      <div className="absolute top-4 right-4">
        <LanguageSwitcher />
      </div>
      <h1 className="text-[28px] font-semibold text-surface-on leading-tight mb-2">
        {t('app.name')}
      </h1>
      <p className="text-base font-normal text-surface-onVariant leading-relaxed">
        {t('app.subtitle')}
      </p>
    </div>
  )
}
