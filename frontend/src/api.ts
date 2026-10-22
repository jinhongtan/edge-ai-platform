export type TensorMetadata = {
  name: string
  elementType: string
  shape: Array<number | string | null>
}

export type ModelSummary = {
  id: string
  fileName: string
  digestSha256: string
  uploadedAt: string
  validationStatus: 'VALID' | 'INVALID'
  validationCode: string | null
  validationMessage: string | null
  inspection: {
    fileSizeBytes: number
    irVersion: number
    opsets: { domain: string; version: number }[]
    inputs: TensorMetadata[]
    outputs: TensorMetadata[]
    operators: string[]
    operatorCounts: Record<string, number>
    parameterCount: number
  } | null
}

export type ApiError = {
  code: string
  message: string
}
export type HardwareProfile = { id: string; vendor: string; name: string; architecture: string; runtime: string; supportedPrecisions: string[]; supportedOperators: string[]; minimumOpset: number | null; maximumOpset: number | null }
export type CompatibilityResult = { status: 'COMPATIBLE' | 'INCOMPATIBLE' | 'WARNING'; unsupportedOperators: { operator: string; count: number; reason: string }[]; warnings: string[]; supportedPrecisions: string[] }

async function readError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json() as Partial<ApiError>
    if (body.code && body.message) {
      return { code: body.code, message: body.message }
    }
  } catch {
    // Fall through to a generic message when the body is not JSON.
  }
  if (response.status === 413) {
    return { code: 'FILE_TOO_LARGE', message: 'Model exceeds the configured size limit.' }
  }
  return { code: 'INTERNAL_ERROR', message: 'Upload failed. Confirm the API is running and try again.' }
}

export async function listModels(): Promise<ModelSummary[]> {
  const response = await fetch('/api/v1/models')
  if (!response.ok) {
    throw await readError(response)
  }
  const body = await response.json() as { data: ModelSummary[] }
  return body.data ?? []
}

export async function uploadModel(file: File): Promise<ModelSummary> {
  const payload = new FormData()
  payload.append('file', file)
  const response = await fetch('/api/v1/models', { method: 'POST', body: payload })
  if (!response.ok) {
    throw await readError(response)
  }
  const body = await response.json() as { data: ModelSummary }
  return body.data
}
export async function listHardware(): Promise<HardwareProfile[]> { const response = await fetch('/api/v1/hardware'); if (!response.ok) throw await readError(response); return ((await response.json()) as { data: HardwareProfile[] }).data ?? [] }
export async function checkCompatibility(modelId: string, hardwareId: string): Promise<CompatibilityResult> { const response = await fetch(`/api/v1/models/${modelId}/compatibility-check`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ hardwareId }) }); if (!response.ok) throw await readError(response); return ((await response.json()) as { data: CompatibilityResult }).data }
