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

  it('renders main workspace sections and enables upload', async () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: /From model file/ })).toBeInTheDocument()
    for (const section of ['Uploaded models', 'Hardware targets', 'Compatibility reports', 'Deployment jobs']) {
      expect(screen.getByRole('heading', { name: section })).toBeInTheDocument()
    }
    expect(screen.getByText(/Local storage · compatibility still planned/)).toBeInTheDocument()
    expect(screen.getByText(/Upload a model/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Upload a model/)).not.toBeDisabled()
    await waitFor(() => expect(screen.getByText(/No ONNX files have been stored yet/)).toBeInTheDocument())
  })

  it('uploads an onnx file and lists the stored model', async () => {
    const user = userEvent.setup()
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce({ ok: true, json: async () => ({ data: [] }) } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          data: {
            id: 'model-1',
            fileName: 'tiny.onnx',
            digestSha256: 'abc',
            uploadedAt: '2026-01-01T00:00:00Z',
            inspection: { fileSizeBytes: 12, irVersion: 9, opsets: [], inputs: [{ name: 'features', elementType: 'FLOAT', shape: [1, 3] }], outputs: [] },
          },
        }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          data: [{
            id: 'model-1',
            fileName: 'tiny.onnx',
            digestSha256: 'abc',
            uploadedAt: '2026-01-01T00:00:00Z',
            inspection: { fileSizeBytes: 12, irVersion: 9, opsets: [], inputs: [{ name: 'features', elementType: 'FLOAT', shape: [1, 3] }], outputs: [] },
          }],
        }),
      } as Response)

    render(<App />)
    const file = new File([new Uint8Array([1, 2, 3])], 'tiny.onnx', { type: 'application/octet-stream' })
    await user.upload(screen.getByLabelText(/Upload a model/), file)
    expect(await screen.findByText('tiny.onnx')).toBeInTheDocument()
    expect(screen.getByText(/Inputs: features/)).toBeInTheDocument()
    const uploadCall = fetchMock.mock.calls.find((call) => call[1]?.method === 'POST')
    expect(uploadCall?.[0]).toBe('/api/v1/models')
  })
})
