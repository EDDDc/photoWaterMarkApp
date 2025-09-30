export interface ImportedImageMeta {
  id: string
  file: File
  name: string
  size: number
  type: string
  objectUrl: string
  width?: number
  height?: number
  lastModified: number
}

export interface ImagePreviewState {
  activeImageId: string | null
  zoom: number
  fitMode: 'contain' | 'cover' | 'actual'
}