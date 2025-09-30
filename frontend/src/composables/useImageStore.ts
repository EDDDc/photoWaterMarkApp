import { computed, onBeforeUnmount, ref } from 'vue'
import type { ImportedImageMeta } from '../types/images'

interface AddImagesOptions {
  replace?: boolean
}

function createImageId(file: File): string {
  return `${file.name}-${file.size}-${file.lastModified}`
}

async function loadImageDimensions(url: string): Promise<{ width: number; height: number } | null> {
  return await new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      resolve({ width: img.width, height: img.height })
    }
    img.onerror = () => {
      resolve(null)
    }
    img.src = url
  })
}

export function useImageStore() {
  const items = ref<ImportedImageMeta[]>([])
  const activeImageId = ref<string | null>(null)
  const isDraggingOver = ref(false)

  const activeImage = computed(() => items.value.find((item) => item.id === activeImageId.value) ?? null)

  function revokeObjectUrl(id: string) {
    const item = items.value.find((entry) => entry.id === id)
    if (item) {
      URL.revokeObjectURL(item.objectUrl)
    }
  }

  async function addFiles(fileList: FileList | File[], options?: AddImagesOptions) {
    const files = Array.from(fileList).filter((file) => file.type.startsWith('image/'))
    if (!files.length) {
      return
    }

    if (options?.replace) {
      clear()
    }

    const uniqueMap = new Map<string, ImportedImageMeta>()
    for (const existing of items.value) {
      uniqueMap.set(existing.id, existing)
    }

    for (const file of files) {
      const id = createImageId(file)
      if (uniqueMap.has(id)) {
        continue
      }

      const objectUrl = URL.createObjectURL(file)
      const dimensions = await loadImageDimensions(objectUrl)
      uniqueMap.set(id, {
        id,
        file,
        name: file.name,
        size: file.size,
        type: file.type,
        objectUrl,
        width: dimensions?.width,
        height: dimensions?.height,
        lastModified: file.lastModified,
      })
    }

    items.value = Array.from(uniqueMap.values()).sort((a, b) => a.name.localeCompare(b.name))
    if (!activeImageId.value && items.value.length > 0) {
      activeImageId.value = items.value[0]?.id ?? null
    }
  }

  function removeImage(id: string) {
    revokeObjectUrl(id)
    items.value = items.value.filter((item) => item.id !== id)
    if (activeImageId.value === id) {
      activeImageId.value = items.value.length > 0 ? items.value[0]?.id ?? null : null
    }
  }

  function clear() {
    for (const item of items.value) {
      URL.revokeObjectURL(item.objectUrl)
    }
    items.value = []
    activeImageId.value = null
  }

  function setActive(id: string) {
    if (items.value.some((item) => item.id === id)) {
      activeImageId.value = id
    }
  }

  function handleDragEnter(event: DragEvent) {
    event.preventDefault()
    isDraggingOver.value = true
  }

  function handleDragOver(event: DragEvent) {
    event.preventDefault()
  }

  function handleDragLeave(event: DragEvent) {
    event.preventDefault()
    const currentTarget = event.currentTarget as HTMLElement | null
    const relatedTarget = event.relatedTarget as Node | null
    if (!currentTarget || (relatedTarget && currentTarget.contains(relatedTarget))) {
      return
    }
    isDraggingOver.value = false
  }

  async function handleDrop(event: DragEvent) {
    event.preventDefault()
    isDraggingOver.value = false
    const files = event.dataTransfer?.files ?? null
    if (files && files.length > 0) {
      await addFiles(files)
    }
  }

  onBeforeUnmount(() => {
    clear()
  })

  return {
    items,
    activeImageId,
    activeImage,
    isDraggingOver,
    addFiles,
    removeImage,
    clear,
    setActive,
    handleDragEnter,
    handleDragLeave,
    handleDragOver,
    handleDrop,
  }
}
