<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useImageStore } from '../composables/useImageStore'
import type { ImageWatermarkConfig, WatermarkConfig } from '../services/api'
import type { ImportedImageMeta } from '../types/images'

const props = defineProps<{
  watermarkConfig: WatermarkConfig | null
}>()

const emit = defineEmits<{
  (e: 'images-updated', images: ImportedImageMeta[]): void
  (e: 'layout-change', payload: { x: number; y: number }): void
}>()

const {
  items,
  activeImageId,
  activeImage,
  addFiles,
  removeImage,
  clear,
  setActive,
  isDraggingOver,
  handleDragEnter,
  handleDragLeave,
  handleDragOver,
  handleDrop,
} = useImageStore()

const fileInput = ref<HTMLInputElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvasContainer = ref<HTMLDivElement | null>(null)
const scaleMode = ref<'contain' | 'cover' | 'actual'>('contain')
const currentImageElement = ref<HTMLImageElement | null>(null)
const renderOptions = ref<CanvasRenderOptions | null>(null)
const isDraggingHandle = ref(false)
let pointerId: number | null = null
const showLayoutHandle = computed(() => {
  const options = renderOptions.value
  const config = props.watermarkConfig
  if (!options || !config || !config.layout) {
    return false
  }
  if (config.type === 'image') {
    return Boolean(config.image?.data)
  }
  if (config.type === 'text') {
    return Boolean(config.text?.content?.trim())
  }
  return false
})

const layoutHandleStyle = computed(() => {
  const options = renderOptions.value
  const layout = props.watermarkConfig?.layout
  if (!options || !layout) {
    return { display: 'none' }
  }
  const [anchorX, anchorY] = resolveAnchor(layout, options.drawWidth, options.drawHeight)
  const left = options.offsetX + anchorX
  const top = options.offsetY + anchorY
  return {
    display: 'block',
    transform: 'translate(-50%, -50%)',
    left: `${left}px`,
    top: `${top}px`,
  }
})

const defaultFontFamily = 'Microsoft YaHei'
const PRESET_POSITIONS: Record<string, [number, number]> = {
  'top-left': [0.1, 0.15],
  'top-center': [0.5, 0.15],
  'top-right': [0.9, 0.15],
  'center-left': [0.15, 0.5],
  center: [0.5, 0.5],
  'center-right': [0.85, 0.5],
  'bottom-left': [0.15, 0.85],
  'bottom-center': [0.5, 0.9],
  'bottom-right': [0.85, 0.9],
}

const watermarkImageCache = new Map<string, HTMLImageElement>()

interface CanvasRenderOptions {
  offsetX: number
  offsetY: number
  drawWidth: number
  drawHeight: number
  scale: number
}

let resizeObserver: ResizeObserver | null = null


const activeImageInfo = computed(() => {
  const image = activeImage.value
  if (!image) {
    return null
  }
  const sizeInMb = image.size / (1024 * 1024)
  return {
    name: image.name,
    dimension: image.width && image.height ? `${image.width}×${image.height}` : '解析中…',
    size: sizeInMb >= 1 ? `${sizeInMb.toFixed(2)} MB` : `${(image.size / 1024).toFixed(0)} KB`,
    type: image.type,
    id: image.id,
  }
})

function triggerFileDialog() {
  fileInput.value?.click()
}

async function handleFileSelection(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files?.length) {
    return
  }
  await addFiles(target.files)
  emit('images-updated', items.value)
  target.value = ''
}

async function handleReplaceSelection(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files?.length) {
    return
  }
  await addFiles(target.files, { replace: true })
  emit('images-updated', items.value)
  target.value = ''
}

async function handleDropWithEmit(event: DragEvent) {
  await handleDrop(event)
  emit('images-updated', items.value)
}

function handleRemoveImage(id: string) {
  removeImage(id)
  emit('images-updated', items.value)
}

function handleClearAll() {
  clear()
  currentImageElement.value = null
  renderOptions.value = null
  clearCanvas()
  stopLayoutDrag()
  emit("images-updated", items.value)
}

function clearCanvas() {
  const canvas = canvasRef.value
  const ctx = canvas?.getContext('2d')
  if (canvas && ctx) {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }
}

function startLayoutDrag(event: PointerEvent) {
  if (!renderOptions.value || !canvasContainer.value) {
    return
  }
  event.preventDefault()
  if (pointerId !== null) {
    stopLayoutDrag()
  }
  pointerId = event.pointerId
  isDraggingHandle.value = true
  window.addEventListener('pointermove', updateLayoutFromPointer)
  window.addEventListener('pointerup', stopLayoutDrag)
  updateLayoutFromPointer(event)
}

function updateLayoutFromPointer(event: PointerEvent) {
  if (!renderOptions.value || !canvasContainer.value) {
    return
  }
  if (pointerId !== null && event.pointerId !== pointerId) {
    return
  }
  const container = canvasContainer.value
  const rect = container.getBoundingClientRect()
  const options = renderOptions.value
  const relativeX = (event.clientX - rect.left - options.offsetX) / options.drawWidth
  const relativeY = (event.clientY - rect.top - options.offsetY) / options.drawHeight
  const clampedX = Math.min(1, Math.max(0, relativeX))
  const clampedY = Math.min(1, Math.max(0, relativeY))
  emit('layout-change', { x: clampedX, y: clampedY })
}

function stopLayoutDrag(event?: PointerEvent) {
  if (pointerId !== null && event && event.pointerId !== pointerId) {
    return
  }
  window.removeEventListener('pointermove', updateLayoutFromPointer)
  window.removeEventListener('pointerup', stopLayoutDrag)
  pointerId = null
  isDraggingHandle.value = false
}

function drawImageToCanvas(image: ImportedImageMeta) {
  renderOptions.value = null
  const container = canvasContainer.value
  if (!container) {
    return
  }
  const imgElement = new Image()
  imgElement.onload = () => {
    currentImageElement.value = imgElement
    renderCanvas(imgElement)
  }
  imgElement.onerror = () => {
    currentImageElement.value = null
    renderOptions.value = null
    clearCanvas()
  }
  imgElement.src = image.objectUrl
}

function renderCanvas(imageEl: HTMLImageElement) {
  const canvas = canvasRef.value
  const container = canvasContainer.value
  if (!canvas || !container) {
    return
  }

  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return
  }

  const width = container.clientWidth
  const height = container.clientHeight

  canvas.width = width
  canvas.height = height
  ctx.clearRect(0, 0, width, height)

  let drawWidth = imageEl.width
  let drawHeight = imageEl.height

  if (scaleMode.value === 'contain') {
    const scale = Math.min(width / imageEl.width, height / imageEl.height, 1)
    drawWidth = imageEl.width * scale
    drawHeight = imageEl.height * scale
  } else if (scaleMode.value === 'cover') {
    const scale = Math.max(width / imageEl.width, height / imageEl.height)
    drawWidth = imageEl.width * scale
    drawHeight = imageEl.height * scale
  }

  const offsetX = (width - drawWidth) / 2
  const offsetY = (height - drawHeight) / 2

  ctx.drawImage(imageEl, offsetX, offsetY, drawWidth, drawHeight)
  renderOptions.value = {
    offsetX,
    offsetY,
    drawWidth,
    drawHeight,
    scale: drawWidth / imageEl.width,
  }

  renderWatermark(ctx, {
    offsetX,
    offsetY,
    drawWidth,
    drawHeight,
    scale: drawWidth / imageEl.width,
  })
}

function renderWatermark(ctx: CanvasRenderingContext2D, options: CanvasRenderOptions) {
  const config = props.watermarkConfig
  if (!config) {
    return
  }

  if (config.type === 'image') {
    renderImageWatermark(ctx, options, config)
    return
  }

  if (config.type !== 'text') {
    return
  }

  const text = config.text
  if (!text || !text.content?.trim()) {
    return
  }

  const layout = config.layout ?? {}
  const [anchorX, anchorY] = resolveAnchor(layout, options.drawWidth, options.drawHeight)
  const anchorCanvasX = options.offsetX + anchorX
  const anchorCanvasY = options.offsetY + anchorY

  const fontParts: string[] = []
  if (text.bold) {
    fontParts.push('bold')
  }
  if (text.italic) {
    fontParts.push('italic')
  }
  fontParts.push(`${text.fontSize ?? 32}px`)
  fontParts.push(`"${text.fontFamily ?? defaultFontFamily}"`)

  ctx.save()
  ctx.font = fontParts.join(' ')
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  const rotateRad = layout.rotationDeg ? (layout.rotationDeg * Math.PI) / 180 : 0
  ctx.translate(anchorCanvasX, anchorCanvasY)
  if (rotateRad) {
    ctx.rotate(rotateRad)
  }

  const opacity = Math.min(1, Math.max(0, (text.opacity ?? 80) / 100))
  ctx.globalAlpha = opacity

  if (text.shadow) {
    ctx.shadowColor = text.shadow.color ?? 'rgba(0,0,0,0.35)'
    ctx.shadowOffsetX = text.shadow.offsetX ?? 2
    ctx.shadowOffsetY = text.shadow.offsetY ?? 2
    ctx.shadowBlur = text.shadow.blur ?? 4
  } else {
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0
  }

  const content = text.content
  if (text.stroke?.width && text.stroke.width > 0) {
    ctx.lineWidth = text.stroke.width
    ctx.strokeStyle = text.stroke.color ?? 'rgba(0,0,0,0.8)'
    ctx.strokeText(content, 0, 0)
  }

  ctx.fillStyle = text.color ?? '#FFFFFF'
  ctx.fillText(content, 0, 0)
  ctx.restore()
}

function renderImageWatermark(
  ctx: CanvasRenderingContext2D,
  options: CanvasRenderOptions,
  config: WatermarkConfig,
) {
  const imageConfig = config.image
  if (!imageConfig || !imageConfig.data) {
    return
  }

  const img = getWatermarkImage(imageConfig)
  if (!img) {
    return
  }

  const layout = config.layout ?? {}
  const [anchorX, anchorY] = resolveAnchor(layout, options.drawWidth, options.drawHeight)
  const anchorCanvasX = options.offsetX + anchorX
  const anchorCanvasY = options.offsetY + anchorY

  const scale = clampImageScale(imageConfig.scale)
  const targetWidth = options.drawWidth * scale
  if (targetWidth <= 0) {
    return
  }
  const ratio = targetWidth / img.width
  const targetHeight = img.height * ratio
  const drawX = anchorCanvasX - targetWidth / 2
  const drawY = anchorCanvasY - targetHeight / 2

  ctx.save()
  ctx.globalAlpha = Math.min(1, Math.max(0, (imageConfig.opacity ?? 80) / 100))
  ctx.drawImage(img, drawX, drawY, targetWidth, targetHeight)
  ctx.restore()
}

function getWatermarkImage(imageConfig: ImageWatermarkConfig): HTMLImageElement | null {
  if (!imageConfig.data) {
    return null
  }
  const key = imageConfig.cacheKey ?? imageConfig.data
  const src = imageConfig.data.startsWith('data:')
    ? imageConfig.data
    : `data:${imageConfig.mime ?? 'image/png'};base64,${imageConfig.data}`
  let cached = watermarkImageCache.get(key)
  if (!cached) {
    cached = new Image()
    cached.onload = () => {
      if (currentImageElement.value) {
        renderCanvas(currentImageElement.value)
      }
    }
    cached.onerror = () => {
      watermarkImageCache.delete(key)
    }
    cached.src = src
    watermarkImageCache.set(key, cached)
  } else if (cached.src !== src) {
    cached.src = src
  }
  if (!cached.complete || cached.naturalWidth === 0) {
    return null
  }
  return cached
}

function clampImageScale(value?: number | null) {
  const numeric = typeof value === 'number' && Number.isFinite(value) ? value : 0.3
  return Math.min(1, Math.max(0.05, numeric))
}

function resolveAnchor(
  layout: WatermarkConfig['layout'],
  width: number,
  height: number,
): [number, number] {
  let relativeX = 0.5
  let relativeY = 0.85

  if (layout?.preset) {
    const preset = PRESET_POSITIONS[layout.preset]
    if (preset) {
      const [presetX, presetY] = preset
      relativeX = presetX
      relativeY = presetY
    }
  }
  if (typeof layout?.x === 'number') {
    relativeX = clamp(layout.x, 0, 1)
  }
  if (typeof layout?.y === 'number') {
    relativeY = clamp(layout.y, 0, 1)
  }

  return [relativeX * width, relativeY * height]
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

watch(activeImage, (image) => {
  if (!image) {
    currentImageElement.value = null
    clearCanvas()
    return
  }
  drawImageToCanvas(image)
})

watch(scaleMode, () => {
  if (currentImageElement.value) {
    renderCanvas(currentImageElement.value)
  }
})

watch(
  () => props.watermarkConfig,
  () => {
    if (currentImageElement.value) {
      renderCanvas(currentImageElement.value)
    }
  },
  { deep: true },
)

onMounted(() => {
  emit('images-updated', items.value)
  if (canvasContainer.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (currentImageElement.value) {
        renderCanvas(currentImageElement.value)
      }
    })
    resizeObserver.observe(canvasContainer.value)
  }
})

onBeforeUnmount(() => {
  stopLayoutDrag()
  resizeObserver?.disconnect()
})
</script>


<template>
  <section class="workspace">
    <div
      class="dropzone"
      :class="{ 'dragging': isDraggingOver }"
      @dragenter="handleDragEnter"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDropWithEmit"
    >
      <div class="dropzone-content">
        <p class="headline">将图片拖拽到此处，或</p>
        <div class="buttons">
          <button type="button" class="primary" @click="triggerFileDialog">选择图片</button>
          <label class="outline">
            替换所有
            <input hidden multiple accept="image/*" type="file" @change="handleReplaceSelection" />
          </label>
        </div>
        <p class="muted">支持多张图片或整个文件夹（取决于浏览器是否支持 `webkitdirectory`）。</p>
        <input ref="fileInput" hidden multiple accept="image/*" type="file" @change="handleFileSelection" />
      </div>
    </div>

    <div class="workspace-body" v-if="items.length">
      <aside class="sidebar">
        <header class="sidebar-header">
          <h3>已导入图片 ({{ items.length }})</h3>
          <button type="button" class="ghost" @click="handleClearAll">清空</button>
        </header>
        <ul class="image-list">
          <li
            v-for="image in items"
            :key="image.id"
            :class="{ active: activeImageId === image.id }"
            @click="setActive(image.id)"
          >
            <div class="thumb">
              <img :src="image.objectUrl" :alt="image.name" />
            </div>
            <div class="meta">
              <p class="name" :title="image.name">{{ image.name }}</p>
              <p class="info">
                <span>{{ image.width && image.height ? `${image.width}×${image.height}` : '未知尺寸' }}</span>
                <span>•</span>
                <span>{{ (image.size / 1024).toFixed(0) }} KB</span>
              </p>
            </div>
            <button type="button" class="icon" @click.stop="handleRemoveImage(image.id)">×</button>
          </li>
        </ul>
      </aside>

      <div class="preview">
        <header class="preview-header">
          <div>
            <h3>预览</h3>
            <p v-if="activeImageInfo" class="muted">
              {{ activeImageInfo.name }} · {{ activeImageInfo.dimension }} · {{ activeImageInfo.size }}
            </p>
          </div>
          <div class="scale-controls">
            <label>
              缩放模式：
              <select v-model="scaleMode">
                <option value="contain">适应（留白）</option>
                <option value="cover">填充（可能裁切）</option>
                <option value="actual">原始尺寸</option>
              </select>
            </label>
          </div>
        </header>
        <div ref="canvasContainer" class="canvas-wrapper">
          <canvas ref="canvasRef" class="preview-canvas">当前浏览器不支持 Canvas。</canvas>
          <button
            v-if="showLayoutHandle"
            type="button"
            class="layout-handle"
            :class="{ dragging: isDraggingHandle }"
            :style="layoutHandleStyle"
            @pointerdown="startLayoutDrag"
            aria-label="拖拽调整水印位置"
          ></button>
          <p v-if="!activeImage" class="muted empty">请选择左侧图片以预览</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.workspace {
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background-color: #f8fafc;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.dropzone {
  border: 2px dashed #93c5fd;
  border-radius: 16px;
  background-color: #eff6ff;
  padding: 1.75rem;
  transition: border-color 150ms ease, background-color 150ms ease;
}

.dropzone.dragging {
  border-color: #2563eb;
  background-color: #dbeafe;
}

.dropzone-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.headline {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.buttons {
  display: flex;
  gap: 1rem;
}

button.primary {
  background-color: #2563eb;
  border: 1px solid #2563eb;
  color: #ffffff;
  padding: 0.55rem 1.5rem;
  border-radius: 999px;
  cursor: pointer;
}

button.ghost,
label.outline {
  background-color: transparent;
  border: 1px solid #94a3b8;
  color: #1e293b;
  padding: 0.55rem 1.25rem;
  border-radius: 999px;
  cursor: pointer;
}

label.outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.muted {
  color: #64748b;
  font-size: 0.95rem;
}

.workspace-body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 1.5rem;
}

.sidebar {
  background-color: #eef2ff;
  border-radius: 16px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 460px;
  overflow-y: auto;
}

.image-list li {
  display: grid;
  grid-template-columns: 64px 1fr 28px;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid transparent;
  background-color: rgba(255, 255, 255, 0.6);
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.image-list li.active {
  border-color: #2563eb;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.25);
}

.thumb {
  width: 64px;
  height: 64px;
  overflow: hidden;
  border-radius: 10px;
  background-color: #cbd5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.meta .name {
  margin: 0;
  font-weight: 600;
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta .info {
  margin: 0.2rem 0 0;
  display: flex;
  gap: 0.35rem;
  color: #64748b;
  font-size: 0.85rem;
}

button.icon {
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 1.2rem;
  align-self: flex-start;
  cursor: pointer;
}

button.icon:hover {
  color: #ef4444;
}

.preview {
  background-color: #0f172a;
  border-radius: 18px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  color: #f8fafc;
  min-height: 520px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.scale-controls select {
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  border: 1px solid #94a3b8;
  background-color: #0f172a;
  color: #f1f5f9;
}

.canvas-wrapper {
  flex: 1;
  position: relative;
  border: 1px solid rgba(148, 163, 184, 0.5);
  border-radius: 12px;
  overflow: hidden;
  background: repeating-conic-gradient(#1e293b 0% 25%, transparent 0% 50%) 50% / 20px 20px;
}

.preview-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(15, 23, 42, 0.75);
}


.layout-handle {
  position: absolute;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.9);
  background: rgba(37, 99, 235, 0.85);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
  transition: box-shadow 0.2s ease, background-color 0.2s ease;
  pointer-events: auto;
}

.layout-handle.dragging {
  cursor: grabbing;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.5);
}

.layout-handle::after {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ffffff;
}

@media (max-width: 960px) {
  .workspace-body {
    grid-template-columns: 1fr;
  }

  .sidebar {
    max-height: 240px;
  }
}
</style>




