<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useImageStore } from '../composables/useImageStore'
import type { ImportedImageMeta } from '../types/images'

const emit = defineEmits<{
  (e: 'images-updated', images: ImportedImageMeta[]): void
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

const activeImageInfo = computed(() => {
  const image = activeImage.value
  if (!image) {
    return null
  }
  const sizeInMb = image.size / (1024 * 1024)
  return {
    name: image.name,
    dimension: image.width && image.height ? `${image.width} x ${image.height}` : 'Detecting…',
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
  emit('images-updated', items.value)
}

function drawImageToCanvas(image: ImportedImageMeta) {
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

  const imgElement = new Image()
  imgElement.onload = () => {
    ctx.clearRect(0, 0, width, height)

    let drawWidth = imgElement.width
    let drawHeight = imgElement.height

    if (scaleMode.value === 'contain') {
      const scale = Math.min(width / imgElement.width, height / imgElement.height, 1)
      drawWidth = imgElement.width * scale
      drawHeight = imgElement.height * scale
    } else if (scaleMode.value === 'cover') {
      const scale = Math.max(width / imgElement.width, height / imgElement.height)
      drawWidth = imgElement.width * scale
      drawHeight = imgElement.height * scale
    }

    const x = (width - drawWidth) / 2
    const y = (height - drawHeight) / 2
    ctx.drawImage(imgElement, x, y, drawWidth, drawHeight)
  }
  imgElement.onerror = () => {
    const ctx = canvas.getContext('2d')
    ctx?.clearRect(0, 0, width, height)
  }
  imgElement.src = image.objectUrl
}

watch([activeImage, scaleMode], ([image]) => {
  if (!image) {
    const canvas = canvasRef.value
    if (canvas) {
      const ctx = canvas.getContext('2d')
      ctx?.clearRect(0, 0, canvas.width, canvas.height)
    }
    return
  }
  drawImageToCanvas(image)
})

onMounted(() => {
  emit('images-updated', items.value)
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
        <p class="headline">Drag images here, or</p>
        <div class="buttons">
          <button type="button" class="primary" @click="triggerFileDialog">Select images</button>
          <label class="outline">
            Replace all
            <input hidden multiple accept="image/*" type="file" @change="handleReplaceSelection" />
          </label>
        </div>
        <p class="muted">Supports multi-select or whole folders (requires browsers that support `webkitdirectory`).</p>
        <input ref="fileInput" hidden multiple accept="image/*" type="file" @change="handleFileSelection" />
      </div>
    </div>

    <div class="workspace-body" v-if="items.length">
      <aside class="sidebar">
        <header class="sidebar-header">
          <h3>Imported images ({{ items.length }})</h3>
          <button type="button" class="ghost" @click="handleClearAll">Clear</button>
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
                <span>{{ image.width && image.height ? `${image.width} x ${image.height}` : 'Unknown size' }}</span>
                <span>&bull;</span>
                <span>{{ (image.size / 1024).toFixed(0) }} KB</span>
              </p>
            </div>
            <button type="button" class="icon" @click.stop="handleRemoveImage(image.id)">&times;</button>
          </li>
        </ul>
      </aside>

      <div class="preview">
        <header class="preview-header">
          <div>
            <h3>Preview</h3>
            <p v-if="activeImageInfo" class="muted">
              {{ activeImageInfo.name }} · {{ activeImageInfo.dimension }} · {{ activeImageInfo.size }}
            </p>
          </div>
          <div class="scale-controls">
            <label>
              Zoom mode:
              <select v-model="scaleMode">
                <option value="contain">Contain (fit)</option>
                <option value="cover">Cover (may crop)</option>
                <option value="actual">Actual size</option>
              </select>
            </label>
          </div>
        </header>
        <div ref="canvasContainer" class="canvas-wrapper">
          <canvas ref="canvasRef" class="preview-canvas">Canvas is not supported in this browser.</canvas>
          <p v-if="!activeImage" class="muted empty">Select an image on the left to preview</p>
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

@media (max-width: 960px) {
  .workspace-body {
    grid-template-columns: 1fr;
  }

  .sidebar {
    max-height: 240px;
  }
}
</style>
