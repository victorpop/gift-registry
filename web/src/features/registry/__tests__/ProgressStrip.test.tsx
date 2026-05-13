import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import '../../../i18n'
import ProgressStrip from '../ProgressStrip'

describe('ProgressStrip', () => {
  it('body reads "of N chosen" with no leading numeral (EN)', () => {
    render(<ProgressStrip totalChosen={2} total={3} isOwner={false} />)
    // Body should read "of 3 chosen" — no leading count
    expect(screen.getByText('of 3 chosen')).toBeInTheDocument()
    // The big display numeral "2" is present (separate span)
    expect(screen.getByText('2')).toBeInTheDocument()
    // The "2 of 3 chosen" string from the old {{n}} bug must NOT exist
    expect(screen.queryByText('2 of 3 chosen')).not.toBeInTheDocument()
  })

  it('does NOT render Share button when isOwner is false', () => {
    render(<ProgressStrip totalChosen={2} total={3} isOwner={false} />)
    expect(screen.queryByRole('button', { name: /share this registry/i })).toBeNull()
  })

  it('renders Share button when isOwner is true', () => {
    render(<ProgressStrip totalChosen={2} total={3} isOwner={true} />)
    expect(screen.getByRole('button', { name: /share this registry/i })).toBeInTheDocument()
  })

  it('zero state renders correctly without JS error', () => {
    render(<ProgressStrip totalChosen={0} total={5} isOwner={false} />)
    expect(screen.getByText('of 5 chosen')).toBeInTheDocument()
  })
})
