import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('dashboard', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ data: [] }),
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the landing page and opens the workspace', async () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: /Know what your model needs/i })).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: /Explore workspace/i }))
    expect(screen.getByRole('heading', { name: /From model file/ })).toBeInTheDocument()
    for (const section of ['Uploaded models', 'Hardware targets', 'Compatibility reports', 'Deployment jobs']) {
      expect(screen.getByRole('heading', { name: section })).toBeInTheDocument()
    }
    expect(screen.getByText(/Local storage · compatibility still planned/)).toBeInTheDocument()
    expect(screen.getAllByText(/Upload a model/).length).toBeGreaterThan(0)
    expect(screen.getByLabelText(/Upload a model/)).not.toBeDisabled()
    await waitFor(() => expect(screen.getByText(/No ONNX files have been stored yet/)).toBeInTheDocument())
  })

  it('uploads an onnx file and lists the stored model', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    const model = { id: 'model-1', fileName: 'tiny.onnx', digestSha256: 'abc', uploadedAt: '2026-01-01T00:00:00Z', inspection: { fileSizeBytes: 12, irVersion: 9, opsets: [], inputs: [{ name: 'features', elementType: 'FLOAT', shape: [1, 3] }], outputs: [] } }
    fetchMock.mockImplementation(async (input, init) => {
      if (input === '/api/v1/models' && init?.method === 'POST') return { ok: true, json: async () => ({ data: model }) } as Response
      if (input === '/api/v1/models') return { ok: true, json: async () => ({ data: [model] }) } as Response
      return { ok: true, json: async () => ({ data: [] }) } as Response
    })

    render(<App />)
    await user.click(screen.getByRole('button', { name: /Explore workspace/i }))
    const file = new File([new Uint8Array([1, 2, 3])], 'tiny.onnx', { type: 'application/octet-stream' })
    await user.upload(screen.getByLabelText(/Upload a model/), file)
    expect(await screen.findByText('tiny.onnx')).toBeInTheDocument()
    expect(screen.getByText(/Inputs: features/)).toBeInTheDocument()
    const uploadCall = fetchMock.mock.calls.find((call) => call[1]?.method === 'POST')
    expect(uploadCall?.[0]).toBe('/api/v1/models')
  })

  it('shows login and registration forms from the landing page', async () => {
    const user = userEvent.setup()
    render(<App />)
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(screen.getByRole('heading', { name: /Sign in to your workspace/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Create an account/i }))
    expect(screen.getByRole('heading', { name: /Start with a secure workspace/i })).toBeInTheDocument()
    expect(screen.getByLabelText('Organization')).toBeInTheDocument()
  })
})
