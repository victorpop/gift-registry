import { beforeEach, describe, expect, it } from 'vitest'
import i18n from '../i18n'

describe('i18n', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en')
  })

  it('resolves UI-SPEC keys in English', () => {
    expect(i18n.t('reservation.reserve_item')).toBe('Reserve Gift')
    expect(i18n.t('registry.not_found_title')).toBe('Registry not available')
    expect(i18n.t('auth.guest_modal_title')).toBe('Who are you?')
    expect(i18n.t('common.error_generic')).toBe('Something went wrong. Please try again.')
  })

  it('resolves UI-SPEC keys in Romanian after language switch', async () => {
    await i18n.changeLanguage('ro')
    expect(i18n.t('reservation.reserve_item')).toBe('Rezervă Cadoul')
    expect(i18n.t('registry.not_found_title')).toBe('Registru indisponibil')
    expect(i18n.t('auth.guest_modal_title')).toBe('Cine ești?')
  })

  it('interpolates banner_text placeholders', () => {
    const rendered = i18n.t('reservation.banner_text', { itemName: 'Coffee Grinder', minutes: 12 })
    expect(rendered).toBe('You reserved Coffee Grinder — 12 min left')
  })

  it('persists language selection to localStorage under key "lang"', async () => {
    await i18n.changeLanguage('ro')
    expect(localStorage.getItem('lang')).toBe('ro')
  })

  it('falls back to English for unknown language', async () => {
    await i18n.changeLanguage('zz')
    // fallback triggers — resolvedLanguage is still 'en' or the closest match
    expect(i18n.t('reservation.reserve_item')).toBe('Reserve Gift')
  })
})
