export interface HealthResponse {
  status: string
  timestamp: string
}

const jsonHeaders: HeadersInit = {
  Accept: 'application/json',
}

export async function fetchHealth(signal?: AbortSignal): Promise<HealthResponse> {
  const response = await fetch('/api/health', {
    method: 'GET',
    headers: jsonHeaders,
    signal,
  })

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }

  return (await response.json()) as HealthResponse
}
