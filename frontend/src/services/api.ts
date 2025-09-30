export interface HealthResponse {
  status: string
  timestamp: string
}

export interface ShadowStyle {
  color?: string
  offsetX?: number
  offsetY?: number
  blur?: number
}

export interface StrokeStyle {
  color?: string
  width?: number
}

export interface TextWatermarkConfig {
  content: string
  fontFamily?: string
  fontSize?: number
  bold?: boolean
  italic?: boolean
  color?: string
  opacity?: number
  stroke?: StrokeStyle
  shadow?: ShadowStyle
}

export interface ImageWatermarkConfig {
  name?: string
  mime?: string
  data?: string
  cacheKey?: string
  scale?: number
  opacity?: number
}

export interface LayoutConfig {
  preset?: string
  x?: number
  y?: number
  rotationDeg?: number
  scale?: number
}

export interface WatermarkConfig {
  type: 'text' | 'image'
  text?: TextWatermarkConfig
  image?: ImageWatermarkConfig
  layout?: LayoutConfig
}

export interface ResizeConfig {
  mode?: 'w' | 'h' | 'pct'
  width?: number
  height?: number
  percent?: number
}

export interface NamingRule {
  keepOriginal?: boolean
  prefix?: string
  suffix?: string
}

export interface ExportConfig {
  outputDir?: string
  format: 'jpeg' | 'png'
  jpegQuality?: number
  resize?: ResizeConfig
  naming?: NamingRule
}

export interface TemplateRequest {
  id?: string
  name: string
  watermarkConfig: WatermarkConfig
  exportConfig: ExportConfig
}

export interface Template extends TemplateRequest {
  id: string
  updatedAt: string
}

const jsonHeaders: HeadersInit = {
  Accept: 'application/json',
}

const jsonPostHeaders: HeadersInit = {
  ...jsonHeaders,
  'Content-Type': 'application/json',
}

async function handleJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

export async function fetchHealth(signal?: AbortSignal): Promise<HealthResponse> {
  const response = await fetch('/api/health', {
    method: 'GET',
    headers: jsonHeaders,
    signal,
  })
  return handleJson<HealthResponse>(response)
}

export async function fetchFonts(signal?: AbortSignal): Promise<string[]> {
  const response = await fetch('/api/fonts', {
    method: 'GET',
    headers: jsonHeaders,
    signal,
  })
  return handleJson<string[]>(response)
}

export async function fetchTemplates(signal?: AbortSignal): Promise<Template[]> {
  const response = await fetch('/api/templates', {
    method: 'GET',
    headers: jsonHeaders,
    signal,
  })
  return handleJson<Template[]>(response)
}

export async function saveTemplate(payload: TemplateRequest): Promise<Template> {
  const response = await fetch('/api/templates', {
    method: 'POST',
    headers: jsonPostHeaders,
    body: JSON.stringify(payload),
  })
  return handleJson<Template>(response)
}

export async function deleteTemplate(id: string): Promise<void> {
  const response = await fetch(`/api/templates/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: jsonHeaders,
  })
  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `删除模板失败，状态码 ${response.status}`)
  }
}