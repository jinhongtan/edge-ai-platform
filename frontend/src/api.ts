export type TensorMetadata = {
  name: string
  elementType: string
  shape: Array<number | string | null>
}

export type ModelInspection = {
  fileSizeBytes: number
  irVersion: number
  opsets: Array<{ domain: string; version: number }>
  inputs: TensorMetadata[]
  outputs: TensorMetadata[]
}

export type ModelSummary = {
  id: string
  filename: string
  digestSha256: string
  uploadedAt: string
  inspection: ModelInspection
}

export type ApiError = {
  code: string
  message: string
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json() as ApiError
    if (body && body.message) {
      return body
    }
  } catch {
    // Fall through to a generic error when the body is not JSON.
  }
  return { code: 'INTERNAL_ERROR', message: 'The API could not complete this request.' }
}

export async function listModels(): Promise<ModelSummary[]> {
  const response = await fetch('/api/v1/models')
  if (!response.ok) {
    throw await parseError(response)
  }
  const body = await response.json() as { data: ModelSummary[] }
  return body.data ?? []
}

export async function uploadModel(file: File): Promise<ModelSummary> {
  const form = new FormData()
  form.append('file', file)
  const response = await fetch('/api/v1/models', { method: 'POST', body: form })
  if (!response.ok) {
    throw await parseError(response)
  }
  const body = await response.json() as { data: ModelSummary }
  return body.data
}
