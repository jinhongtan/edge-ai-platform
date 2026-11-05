import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
describe('core UI', () => {
  beforeEach(() => { vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ data: [] }) })); sessionStorage.clear(); location.hash = '' })
  afterEach(() => vi.unstubAllGlobals())
  it('opens the workspace shell', async () => { render(<App />); await userEvent.setup().click(screen.getByRole('button', { name: /Open workspace/ })); expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument() })
  it('opens models from fixed navigation', async () => { render(<App />); const user = userEvent.setup(); await user.click(screen.getByRole('button', { name: /Open workspace/ })); await user.click(screen.getByRole('button', { name: 'Models' })); expect(screen.getByRole('heading', { name: 'Models' })).toBeInTheDocument() })
  it('does not pretend devices can connect', async () => { render(<App />); const user = userEvent.setup(); await user.click(screen.getByRole('button', { name: /Open workspace/ })); await user.click(screen.getByRole('button', { name: 'Devices' })); expect(screen.getByText('No device connection API')).toBeInTheDocument(); expect(screen.getByRole('button', { name: 'Connect device' })).toBeDisabled() })
})
