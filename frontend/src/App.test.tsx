import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('dashboard', () => {
  it('renders main workspace sections and mock status', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: /From model file/ })).toBeInTheDocument()
    for (const section of ['Uploaded models', 'Hardware targets', 'Compatibility reports', 'Deployment jobs']) {
      expect(screen.getByRole('heading', { name: section })).toBeInTheDocument()
    }
    expect(screen.getByText(/Preview · mock data/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Upload a model/ })).toBeDisabled()
  })
})
