<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TemplateRequest, ShadowStyle, StrokeStyle } from '../services/api'

type ResizeMode = 'none' | 'w' | 'h' | 'pct'
type StandardPreset =
  | 'top-left'
  | 'top-center'
  | 'top-right'
  | 'center-left'
  | 'center'
  | 'center-right'
  | 'bottom-left'
  | 'bottom-center'
  | 'bottom-right'
type LayoutPreset = StandardPreset | 'custom'

const PRESET_COORDINATES: Record<StandardPreset, [number, number]> = {
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

const PRESET_OPTIONS: Array<{ value: LayoutPreset; label: string }> = [
  { value: 'top-left', label: '左上角' },
  { value: 'top-center', label: '顶部居中' },
  { value: 'top-right', label: '右上角' },
  { value: 'center-left', label: '左侧居中' },
  { value: 'center', label: '画面中心' },
  { value: 'center-right', label: '右侧居中' },
  { value: 'bottom-left', label: '左下角' },
  { value: 'bottom-center', label: '底部居中' },
  { value: 'bottom-right', label: '右下角' },
  { value: 'custom', label: '自定义' },
]

const props = defineProps<{
  form: TemplateRequest
  availableFonts: string[]
  templateSaving: boolean
  templateError: string | null
  lastSettingsTimestamp: string | null
  lastSettingsLoading: boolean
  savingLastSettings: boolean
  clearingLastSettings: boolean
  lastSettingsError: string | null
}>()

const emit = defineEmits<{
  (e: 'update:form', value: TemplateRequest): void
  (e: 'reset'): void
  (e: 'save-template'): void
  (e: 'save-last-settings'): void
  (e: 'clear-last-settings'): void
}>()

const imageFileInput = ref<HTMLInputElement | null>(null)

function updateForm(mutator: (draft: TemplateRequest) => void) {
  const draft: TemplateRequest = JSON.parse(JSON.stringify(props.form))
  mutator(draft)
  emit('update:form', draft)
}

function ensureText(draft: TemplateRequest) {
  draft.watermarkConfig.type = 'text'
  if (!draft.watermarkConfig.text) {
    draft.watermarkConfig.text = { content: '' }
  }
  return draft.watermarkConfig.text
}

function ensureLayout(draft: TemplateRequest) {
  if (!draft.watermarkConfig.layout) {
    draft.watermarkConfig.layout = {
      preset: 'bottom-right',
      x: 0.85,
      y: 0.9,
      rotationDeg: 0,
    }
  }
  return draft.watermarkConfig.layout
}

function clampOpacityValue(value?: number | null): number {
  const numeric = typeof value === 'number' && Number.isFinite(value) ? value : 80
  return Math.min(100, Math.max(0, numeric))
}

function clampImageScaleValue(value?: number | null): number {
  const numeric = typeof value === 'number' && Number.isFinite(value) ? value : 0.3
  return Math.min(1, Math.max(0.05, numeric))
}

function ensureImage(draft: TemplateRequest) {
  draft.watermarkConfig.type = 'image'
  if (!draft.watermarkConfig.image) {
    draft.watermarkConfig.image = {
      name: '',
      mime: 'image/png',
      data: '',
      scale: 0.3,
      opacity: 80,
      cacheKey: `${Date.now()}`,
    }
  }
  const image = draft.watermarkConfig.image
  image.name = image.name ?? ''
  image.mime = image.mime ?? 'image/png'
  image.cacheKey = image.cacheKey ?? `${Date.now()}`
  image.opacity = clampOpacityValue(image.opacity)
  image.scale = clampImageScaleValue(image.scale)
  return image
}

function onNameChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    draft.name = value
  })
}

function onTextContentChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    const text = ensureText(draft)
    text.content = value
  })
}

function onFontFamilyChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  updateForm((draft) => {
    const text = ensureText(draft)
    text.fontFamily = value
  })
}

function onFontSizeChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    const text = ensureText(draft)
    text.fontSize = Number.isFinite(value) ? value : undefined
  })
}

function onBoldToggle(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  updateForm((draft) => {
    const text = ensureText(draft)
    text.bold = checked
  })
}

function onItalicToggle(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  updateForm((draft) => {
    const text = ensureText(draft)
    text.italic = checked
  })
}
function onWatermarkTypeChange(type: 'text' | 'image') {
  updateForm((draft) => {
    draft.watermarkConfig.type = type
    if (type === 'text') {
      ensureText(draft)
    } else {
      ensureImage(draft)
    }
    ensureLayout(draft)
  })
}

function triggerImageFileDialog() {
  imageFileInput.value?.click()
}

async function onImageSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const fileList = input.files
  if (!fileList || fileList.length === 0) {
    return
  }
  const file = fileList.item(0)
  if (!file) {
    return
  }
  const dataUrl = await readFileAsDataUrl(file)
  updateForm((draft) => {
    const image = ensureImage(draft)
    image.name = file.name
    image.mime = file.type || 'image/png'
    image.data = dataUrl
    image.cacheKey = `${file.name}-${file.size}-${file.lastModified}-${Date.now()}`
  })
  input.value = ''
}

function onImageScaleChange(event: Event) {
  let percent = Number((event.target as HTMLInputElement).value)
  if (!Number.isFinite(percent)) {
    percent = 30
  }
  percent = Math.min(100, Math.max(5, percent))
  const scale = clampImageScaleValue(percent / 100)
  updateForm((draft) => {
    const image = ensureImage(draft)
    image.scale = scale
  })
}
function onImageOpacityChange(event: Event) {
  let value = Number((event.target as HTMLInputElement).value)
  if (!Number.isFinite(value)) {
    value = 80
  }
  const opacity = clampOpacityValue(value)
  updateForm((draft) => {
    const image = ensureImage(draft)
    image.opacity = opacity
  })
}
function onImageClear() {
  updateForm((draft) => {
    const image = ensureImage(draft)
    image.name = ''
    image.mime = 'image/png'
    image.data = ''
    image.cacheKey = `${Date.now()}`
    image.opacity = 80
    image.scale = 0.3
  })
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '')
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

function applyTextColor(value: string) {
  const normalized = value.trim()
  updateForm((draft) => {
    const text = ensureText(draft)
    text.color = normalized ? normalized : undefined
  })
}

function onTextColorPickerChange(event: Event) {
  applyTextColor((event.target as HTMLInputElement).value)
}

function onTextColorInputChange(event: Event) {
  applyTextColor((event.target as HTMLInputElement).value)
}

function onOpacityChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    const text = ensureText(draft)
    text.opacity = Number.isFinite(value) ? value : undefined
  })
}

function onStrokeToggle(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  updateForm((draft) => {
    const text = ensureText(draft)
    if (checked) {
      text.stroke = text.stroke ?? { color: '#000000', width: 2 }
    } else {
      text.stroke = undefined
    }
  })
}

function applyStroke(mutator: (stroke: StrokeStyle) => void) {
  updateForm((draft) => {
    const text = ensureText(draft)
    if (!text.stroke) {
      text.stroke = { color: '#000000', width: 2 }
    }
    mutator(text.stroke)
  })
}

function onStrokeColorChange(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  applyStroke((stroke) => {
    stroke.color = value || '#000000'
  })
}

function onStrokeWidthChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  applyStroke((stroke) => {
    stroke.width = Number.isFinite(value) && value > 0 ? value : 1
  })
}

function onShadowToggle(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  updateForm((draft) => {
    const text = ensureText(draft)
    if (checked) {
      text.shadow =
        text.shadow ?? {
          color: 'rgba(0,0,0,0.4)',
          offsetX: 2,
          offsetY: 2,
          blur: 4,
        }
    } else {
      text.shadow = undefined
    }
  })
}

function applyShadow(mutator: (shadow: ShadowStyle) => void) {
  updateForm((draft) => {
    const text = ensureText(draft)
    if (!text.shadow) {
      text.shadow = {
        color: 'rgba(0,0,0,0.4)',
        offsetX: 2,
        offsetY: 2,
        blur: 4,
      }
    }
    mutator(text.shadow)
  })
}

function onShadowColorChange(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  applyShadow((shadow) => {
    shadow.color = value || 'rgba(0,0,0,0.4)'
  })
}

function onShadowOffsetXChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  applyShadow((shadow) => {
    shadow.offsetX = Number.isFinite(value) ? value : 0
  })
}

function onShadowOffsetYChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  applyShadow((shadow) => {
    shadow.offsetY = Number.isFinite(value) ? value : 0
  })
}

function onShadowBlurChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  applyShadow((shadow) => {
    shadow.blur = Number.isFinite(value) && value >= 0 ? value : 0
  })
}

function onFormatChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value as 'png' | 'jpeg'
  updateForm((draft) => {
    draft.exportConfig.format = value
    if (value === 'jpeg') {
      draft.exportConfig.jpegQuality = draft.exportConfig.jpegQuality ?? 90
    } else {
      delete draft.exportConfig.jpegQuality
    }
  })
}

function onKeepOriginalChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value === 'true'
  updateForm((draft) => {
    if (!draft.exportConfig.naming) {
      draft.exportConfig.naming = {}
    }
    draft.exportConfig.naming.keepOriginal = value
  })
}

function onPrefixChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    if (!draft.exportConfig.naming) {
      draft.exportConfig.naming = {}
    }
    draft.exportConfig.naming.prefix = value
  })
}

function onSuffixChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    if (!draft.exportConfig.naming) {
      draft.exportConfig.naming = {}
    }
    draft.exportConfig.naming.suffix = value
  })
}

function onOutputDirChange(event: Event) {
  const value = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    draft.exportConfig.outputDir = value.trim()
  })
}

function onJpegQualityChange(event: Event) {
  let value = Number((event.target as HTMLInputElement).value)
  if (!Number.isFinite(value)) {
    value = 90
  }
  value = Math.min(100, Math.max(10, value))
  updateForm((draft) => {
    draft.exportConfig.jpegQuality = value
  })
}

function onResizeModeChange(event: Event) {
  const mode = (event.target as HTMLSelectElement).value as ResizeMode
  updateForm((draft) => {
    if (mode === 'none') {
      draft.exportConfig.resize = undefined
      return
    }
    const previous = draft.exportConfig.resize
    if (mode === 'w') {
      draft.exportConfig.resize = {
        mode: 'w',
        width: previous?.width ?? 1920,
      }
    } else if (mode === 'h') {
      draft.exportConfig.resize = {
        mode: 'h',
        height: previous?.height ?? 1080,
      }
    } else {
      draft.exportConfig.resize = {
        mode: 'pct',
        percent: previous?.percent ?? 50,
      }
    }
  })
}

function onResizeWidthChange(event: Event) {
  const value = normalizePositiveNumber((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    if (!draft.exportConfig.resize || draft.exportConfig.resize.mode !== 'w') {
      draft.exportConfig.resize = { mode: 'w', width: value ?? 1920 }
    } else {
      draft.exportConfig.resize.width = value
    }
  })
}

function onResizeHeightChange(event: Event) {
  const value = normalizePositiveNumber((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    if (!draft.exportConfig.resize || draft.exportConfig.resize.mode !== 'h') {
      draft.exportConfig.resize = { mode: 'h', height: value ?? 1080 }
    } else {
      draft.exportConfig.resize.height = value
    }
  })
}

function onResizePercentChange(event: Event) {
  let value = normalizePositiveNumber((event.target as HTMLInputElement).value)
  if (value) {
    value = Math.min(400, value)
  }
  updateForm((draft) => {
    if (!draft.exportConfig.resize || draft.exportConfig.resize.mode !== 'pct') {
      draft.exportConfig.resize = { mode: 'pct', percent: value ?? 50 }
    } else {
      draft.exportConfig.resize.percent = value
    }
  })
}

function onLayoutPresetChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value as LayoutPreset
  updateForm((draft) => {
    const layout = ensureLayout(draft)
    layout.preset = value
    if (value !== 'custom') {
      const [x, y] = PRESET_COORDINATES[value]
      layout.x = x
      layout.y = y
    }
  })
}

function onLayoutXChange(event: Event) {
  const raw = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    const layout = ensureLayout(draft)
    if (raw === '') {
      layout.x = undefined
      layout.preset = 'custom'
      return
    }
    const value = Number(raw)
    if (Number.isFinite(value)) {
      layout.x = clampRelative(value)
      layout.preset = 'custom'
    }
  })
}

function onLayoutYChange(event: Event) {
  const raw = (event.target as HTMLInputElement).value
  updateForm((draft) => {
    const layout = ensureLayout(draft)
    if (raw === '') {
      layout.y = undefined
      layout.preset = 'custom'
      return
    }
    const value = Number(raw)
    if (Number.isFinite(value)) {
      layout.y = clampRelative(value)
      layout.preset = 'custom'
    }
  })
}

function onLayoutRotationChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    const layout = ensureLayout(draft)
    layout.rotationDeg = Number.isFinite(value) ? Math.max(-180, Math.min(180, value)) : 0
  })
}

function normalizePositiveNumber(raw: string): number | undefined {
  const numeric = Number(raw)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : undefined
}

function clampRelative(value: number) {
  return Math.min(1, Math.max(0, value))
}

function onSubmit() {
  emit('save-template')
}

function onReset() {
  emit('reset')
}

function onSaveLastSettings() {
  emit('save-last-settings')
}

function onClearLastSettings() {
  emit('clear-last-settings')
}

const watermarkType = computed<'text' | 'image'>(() =>
  props.form.watermarkConfig.type === 'image' ? 'image' : 'text'
)
const imageConfig = computed(() => props.form.watermarkConfig.image ?? null)
const imagePreviewSrc = computed(() => imageConfig.value?.data ?? '')
const imageScalePercent = computed(() => {
  const scale = clampImageScaleValue(imageConfig.value?.scale)
  return Math.round(scale * 100)
})
const imageOpacityValue = computed(() => imageConfig.value?.opacity ?? 80)
const hasImageData = computed(() => Boolean(imageConfig.value?.data))

const isStrokeEnabled = computed(() => Boolean(props.form.watermarkConfig.text?.stroke))
const isShadowEnabled = computed(() => Boolean(props.form.watermarkConfig.text?.shadow))
const textColorPicker = computed(() => {
  const color = props.form.watermarkConfig.text?.color
  return color && color.startsWith('#') ? color : '#ffffff'
})
const textColorValue = computed(() => props.form.watermarkConfig.text?.color ?? '#FFFFFF')
const strokeColorPicker = computed(() => {
  const color = props.form.watermarkConfig.text?.stroke?.color
  return color && color.startsWith('#') ? color : '#000000'
})
const strokeColorValue = computed(() => props.form.watermarkConfig.text?.stroke?.color ?? '#000000')
const strokeWidthValue = computed(() => props.form.watermarkConfig.text?.stroke?.width ?? 2)
const shadowColorValue = computed(() => props.form.watermarkConfig.text?.shadow?.color ?? 'rgba(0,0,0,0.4)')
const shadowOffsetXValue = computed(() => props.form.watermarkConfig.text?.shadow?.offsetX ?? 2)
const shadowOffsetYValue = computed(() => props.form.watermarkConfig.text?.shadow?.offsetY ?? 2)
const shadowBlurValue = computed(() => props.form.watermarkConfig.text?.shadow?.blur ?? 4)
const isJpegFormat = computed(() => props.form.exportConfig.format === 'jpeg')
const currentResizeMode = computed<ResizeMode>(
  () => (props.form.exportConfig.resize?.mode as ResizeMode | undefined) ?? 'none'
)
const resizeWidth = computed(() =>
  props.form.exportConfig.resize?.mode === 'w' && props.form.exportConfig.resize.width != null
    ? props.form.exportConfig.resize.width
    : ''
)
const resizeHeight = computed(() =>
  props.form.exportConfig.resize?.mode === 'h' && props.form.exportConfig.resize.height != null
    ? props.form.exportConfig.resize.height
    : ''
)
const resizePercent = computed(() =>
  props.form.exportConfig.resize?.mode === 'pct' && props.form.exportConfig.resize.percent != null
    ? props.form.exportConfig.resize.percent
    : ''
)
const layoutPresetValue = computed<LayoutPreset>(
  () => (props.form.watermarkConfig.layout?.preset as LayoutPreset | undefined) ?? 'bottom-right'
)
const rotationValue = computed(() => props.form.watermarkConfig.layout?.rotationDeg ?? 0)
const layoutXValue = computed(() => props.form.watermarkConfig.layout?.x ?? '')
const layoutYValue = computed(() => props.form.watermarkConfig.layout?.y ?? '')
</script>

<template>
  <form class="template-form" @submit.prevent="onSubmit">
    <h3>水印模板</h3>

    <div class="type-switch">
      <label class="option-toggle">
        <input
          type="radio"
          name="watermark-type"
          value="text"
          :checked="watermarkType === 'text'"
          @change="onWatermarkTypeChange('text')"
        />
        <span>文本水印</span>
      </label>
      <label class="option-toggle">
        <input
          type="radio"
          name="watermark-type"
          value="image"
          :checked="watermarkType === 'image'"
          @change="onWatermarkTypeChange('image')"
        />
        <span>图片水印</span>
      </label>
    </div>


    <label class="field">
      <span>模板名称</span>
      <input :value="form.name" type="text" required maxlength="120" placeholder="输入模板名称" @input="onNameChange" />
    </label>

    <fieldset v-if="watermarkType === 'text'" class="field">
      <legend>文本水印</legend>
      <div class="field">
        <span>水印内容</span>
        <input
          :value="form.watermarkConfig.text?.content ?? ''"
          type="text"
          required
          maxlength="200"
          placeholder="例：版权所有 PhotoWatermark"
          @input="onTextContentChange"
        />
      </div>

      <div class="field row">
        <label>
          <span>字体</span>
          <select :value="form.watermarkConfig.text?.fontFamily" @change="onFontFamilyChange">
            <option v-for="font in availableFonts" :key="font" :value="font">{{ font }}</option>
          </select>
        </label>

        <label>
          <span>字号</span>
          <input :value="form.watermarkConfig.text?.fontSize ?? ''" type="number" min="8" max="300" @input="onFontSizeChange" />
        </label>

        <label>
          <span>透明度 (%)</span>
          <input :value="form.watermarkConfig.text?.opacity ?? ''" type="number" min="0" max="100" @input="onOpacityChange" />
        </label>
      </div>

      <div class="field row">
        <label>
          <span>字体颜色</span>
          <div class="color-input">
            <input :value="textColorPicker" type="color" @input="onTextColorPickerChange" />
            <input
              :value="textColorValue"
              type="text"
              maxlength="40"
              placeholder="#FFFFFF 或 rgba(255,255,255,1)"
              @input="onTextColorInputChange"
            />
          </div>
        </label>

        <div class="option-group">
          <label class="option-toggle">
            <input type="checkbox" :checked="form.watermarkConfig.text?.bold ?? false" @change="onBoldToggle" />
            <span>粗体</span>
          </label>
          <label class="option-toggle">
            <input type="checkbox" :checked="form.watermarkConfig.text?.italic ?? false" @change="onItalicToggle" />
            <span>斜体</span>
          </label>
        </div>
      </div>

      <div class="advanced-block">
        <div class="toggle-header">
          <label class="option-toggle">
            <input type="checkbox" :checked="isStrokeEnabled" @change="onStrokeToggle" />
            <span>描边</span>
          </label>
          <span class="hint">描边可增强复杂背景下的可读性。</span>
        </div>
        <div v-if="isStrokeEnabled" class="advanced-grid">
          <label>
            <span>描边颜色</span>
            <div class="color-input">
              <input :value="strokeColorPicker" type="color" @input="onStrokeColorChange" />
              <input :value="strokeColorValue" type="text" maxlength="40" @input="onStrokeColorChange" />
            </div>
          </label>
          <label>
            <span>描边宽度 (px)</span>
            <input :value="strokeWidthValue" type="number" min="1" max="20" step="1" @input="onStrokeWidthChange" />
          </label>
        </div>
      </div>

      <div class="advanced-block">
        <div class="toggle-header">
          <label class="option-toggle">
            <input type="checkbox" :checked="isShadowEnabled" @change="onShadowToggle" />
            <span>阴影</span>
          </label>
          <span class="hint">阴影帮助水印在浅色背景下保持对比。</span>
        </div>
        <div v-if="isShadowEnabled" class="advanced-grid">
          <label>
            <span>阴影颜色</span>
            <input :value="shadowColorValue" type="text" maxlength="60" placeholder="例如 rgba(0,0,0,0.4)" @input="onShadowColorChange" />
          </label>
          <label>
            <span>偏移 X (px)</span>
            <input :value="shadowOffsetXValue" type="number" step="0.5" @input="onShadowOffsetXChange" />
          </label>
          <label>
            <span>偏移 Y (px)</span>
            <input :value="shadowOffsetYValue" type="number" step="0.5" @input="onShadowOffsetYChange" />
          </label>
          <label>
            <span>模糊 (px)</span>
            <input :value="shadowBlurValue" type="number" min="0" step="0.5" @input="onShadowBlurChange" />
          </label>
        </div>
      </div>
    </fieldset>

    <fieldset v-else class="field">
      <legend>图片水印</legend>
      <div class="field">
        <span>图片文件</span>
        <div class="image-upload">
          <button type="button" class="outline" @click="triggerImageFileDialog">选择图片</button>
          <button type="button" class="ghost" :disabled="!hasImageData" @click="onImageClear">移除</button>
          <input ref="imageFileInput" hidden type="file" accept="image/png,image/jpeg,image/webp" @change="onImageSelect" />
        </div>
        <p class="hint">支持 PNG（含透明）、JPEG、WebP；请选择清晰度较高的图片。</p>
        <div v-if="imagePreviewSrc" class="image-preview">
          <img :src="imagePreviewSrc" alt="水印预览" />
        </div>
      </div>
      <div class="field">
        <span>图片宽度占比 (%)</span>
        <div class="slider-row">
          <input
            :value="imageScalePercent"
            type="range"
            min="5"
            max="100"
            step="1"
            @input="onImageScaleChange"
          />
          <input
            class="slider-value"
            :value="imageScalePercent"
            type="number"
            min="5"
            max="100"
            step="1"
            @input="onImageScaleChange"
          />
        </div>
        <p class="hint">控制水印宽度占原始图片宽度的百分比。</p>
      </div>
      <div class="field">
        <span>透明度 (%)</span>
        <div class="slider-row">
          <input
            :value="imageOpacityValue"
            type="range"
            min="0"
            max="100"
            step="1"
            @input="onImageOpacityChange"
          />
          <input
            class="slider-value"
            :value="imageOpacityValue"
            type="number"
            min="0"
            max="100"
            step="1"
            @input="onImageOpacityChange"
          />
        </div>
      </div>
    </fieldset>

    <fieldset class="field">
      <legend>水印布局</legend>
      <div class="field row">
        <label>
          <span>预设位置</span>
          <select :value="layoutPresetValue" @change="onLayoutPresetChange">
            <option v-for="option in PRESET_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <label>
          <span>相对位置 X (0-1)</span>
          <input :value="layoutXValue" type="number" step="0.01" min="0" max="1" @input="onLayoutXChange" />
        </label>

        <label>
          <span>相对位置 Y (0-1)</span>
          <input :value="layoutYValue" type="number" step="0.01" min="0" max="1" @input="onLayoutYChange" />
        </label>
      </div>

      <div class="rotation-control">
        <span>旋转角度</span>
        <div class="rotation-inputs">
          <input :value="rotationValue" type="range" min="-180" max="180" step="1" @input="onLayoutRotationChange" />
          <input :value="rotationValue" type="number" min="-180" max="180" step="1" @input="onLayoutRotationChange" />
        </div>
        <p class="hint">支持 -180° 至 180°，用于打造倾斜水印效果。</p>
      </div>
    </fieldset>

    <fieldset class="field">
      <legend>导出配置</legend>
      <div class="field row">
        <label>
          <span>输出格式</span>
          <select :value="form.exportConfig.format" @change="onFormatChange">
            <option value="png">PNG（透明）</option>
            <option value="jpeg">JPEG</option>
          </select>
        </label>

        <label>
          <span>保留原文件名</span>
          <select :value="String(form.exportConfig.naming?.keepOriginal ?? true)" @change="onKeepOriginalChange">
            <option value="true">是</option>
            <option value="false">否</option>
          </select>
        </label>
      </div>

      <div class="field row">
        <label>
          <span>前缀</span>
          <input :value="form.exportConfig.naming?.prefix ?? ''" type="text" maxlength="40" @input="onPrefixChange" />
        </label>
        <label>
          <span>后缀</span>
          <input :value="form.exportConfig.naming?.suffix ?? ''" type="text" maxlength="40" @input="onSuffixChange" />
        </label>
      </div>

      <div class="field">
        <span>输出目录</span>
        <input
          :value="form.exportConfig.outputDir ?? ''"
          type="text"
          placeholder="留空则使用默认 tar-photos/exports"
          @input="onOutputDirChange"
        />
        <p class="hint">例如：E:/exports 或 D:/watermark。空白表示使用默认目录。</p>
      </div>

      <div v-if="isJpegFormat" class="field">
        <span>JPEG 质量</span>
        <div class="slider-row">
          <input
            :value="form.exportConfig.jpegQuality ?? 90"
            type="range"
            min="10"
            max="100"
            step="1"
            @input="onJpegQualityChange"
          />
          <input
            class="slider-value"
            :value="form.exportConfig.jpegQuality ?? 90"
            type="number"
            min="10"
            max="100"
            step="1"
            @input="onJpegQualityChange"
          />
        </div>
        <p class="hint">数值越高质量越好，同时文件也会更大。</p>
      </div>

      <div class="field">
        <span>尺寸调整</span>
        <div class="resize-controls">
          <label>
            <span>模式</span>
            <select :value="currentResizeMode" @change="onResizeModeChange">
              <option value="none">不调整</option>
              <option value="w">按宽度</option>
              <option value="h">按高度</option>
              <option value="pct">按百分比</option>
            </select>
          </label>

          <label v-if="currentResizeMode === 'w'">
            <span>宽度 (px)</span>
            <input :value="resizeWidth" type="number" min="1" placeholder="例如 1920" @input="onResizeWidthChange" />
          </label>

          <label v-if="currentResizeMode === 'h'">
            <span>高度 (px)</span>
            <input :value="resizeHeight" type="number" min="1" placeholder="例如 1080" @input="onResizeHeightChange" />
          </label>

          <label v-if="currentResizeMode === 'pct'">
            <span>百分比 (%)</span>
            <input :value="resizePercent" type="number" min="1" max="400" placeholder="例如 50" @input="onResizePercentChange" />
          </label>
        </div>
        <p class="hint">若选择“按宽度/高度/百分比”，未填写数值时将使用默认建议值。</p>
      </div>
    </fieldset>

    <div class="form-actions">
      <button type="submit" class="primary" :disabled="templateSaving">
        {{ templateSaving ? '保存中...' : '保存模板' }}
      </button>
      <button type="button" class="ghost" :disabled="templateSaving" @click="onReset">重置</button>
    </div>

    <div class="last-settings-panel">
      <div class="last-settings-header">
        <div>
          <h4>默认设置</h4>
          <p class="muted">将当前表单保存为“上次使用的设置”，下次打开应用时自动加载。</p>
        </div>
        <div class="last-settings-meta">
          <span v-if="lastSettingsLoading" class="muted">读取中...</span>
          <span v-else-if="lastSettingsTimestamp" class="muted">最近更新：{{ lastSettingsTimestamp }}</span>
          <span v-else class="muted">尚未保存默认设置</span>
        </div>
      </div>

      <div class="last-settings-actions">
        <button type="button" class="outline" :disabled="savingLastSettings || templateSaving" @click="onSaveLastSettings">
          {{ savingLastSettings ? '保存中...' : '保存为默认' }}
        </button>
        <button
          type="button"
          class="ghost"
          :disabled="clearingLastSettings || !lastSettingsTimestamp"
          @click="onClearLastSettings"
        >
          {{ clearingLastSettings ? '清除中...' : '清除默认' }}
        </button>
      </div>

      <p v-if="lastSettingsError" class="error">{{ lastSettingsError }}</p>
    </div>

    <p v-if="templateError" class="error">{{ templateError }}</p>
  </form>
</template>

<style scoped>
.template-form {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  background-color: #f8fafc;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-bottom: 1rem;
  font-size: 0.95rem;
}

.field.row {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 1rem;
}

.field.row > label {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  min-width: 160px;
}

input,
select,
textarea {
  padding: 0.55rem 0.75rem;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  font-size: 0.95rem;
  font-family: inherit;
  background-color: #ffffff;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

fieldset {
  border: 1px solid #d1d5db;
  border-radius: 14px;
  padding: 1rem;
}

legend {
  padding: 0 0.5rem;
  font-weight: 600;
  color: #374151;
}

.form-actions {
  display: flex;
  gap: 0.8rem;
  margin-top: 1.25rem;
}

button.primary {
  background-color: #2563eb;
  border: 1px solid #2563eb;
  color: #ffffff;
  padding: 0.55rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 150ms ease, box-shadow 150ms ease;
}

button.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button.primary:not(:disabled):hover {
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.25);
  transform: translateY(-1px);
}

button.ghost {
  background-color: transparent;
  border: 1px solid #cbd5f5;
  color: #1e3a8a;
  padding: 0.55rem 1.25rem;
  border-radius: 10px;
  cursor: pointer;
}

button.outline {
  background-color: transparent;
  border: 1px solid #2563eb;
  color: #2563eb;
  padding: 0.55rem 1.25rem;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 150ms ease, box-shadow 150ms ease;
}

button.outline:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button.outline:not(:disabled):hover {
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.15);
  transform: translateY(-1px);
}

.last-settings-panel {
  margin-top: 1.5rem;
  border: 1px dashed #94a3b8;
  border-radius: 14px;
  padding: 1rem 1.25rem;
  background-color: #eef2ff;
}

.last-settings-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.last-settings-header h4 {
  margin: 0 0 0.5rem;
}

.last-settings-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.muted {
  color: #6b7280;
  font-size: 0.95rem;
}

.error {
  margin-top: 0.75rem;
  color: #dc2626;
  font-size: 0.9rem;
}

.hint {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.slider-row input[type='range'] {
  flex: 1 1 auto;
}

.slider-row .slider-value {
  width: 60px;
  text-align: center;
}

.resize-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
}

.resize-controls label {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  min-width: 160px;
}

.option-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.option-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.9rem;
  color: #1f2937;
}

.option-toggle input[type='checkbox'] {
  width: 16px;
  height: 16px;
}

.color-input {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.color-input input[type='text'] {
  flex: 1 1 auto;
}

.type-switch {
  display: flex;
  gap: 0.75rem;
  margin: 0.75rem 0 1rem;
}

.type-switch .option-toggle {
  border: 1px solid #d1d5db;
  border-radius: 999px;
  padding: 0.25rem 0.85rem;
  background-color: #ffffff;
  transition: background-color 150ms ease, border-color 150ms ease;
}

.type-switch .option-toggle input[type='radio'] {
  margin-right: 0.35rem;
}

.type-switch .option-toggle span {
  font-weight: 600;
}
.type-switch .option-toggle input[type='radio']:checked + span {
  color: #2563eb;
  font-weight: 600;
}



.image-upload {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-items: center;
}

.image-preview {
  margin-top: 0.75rem;
  border: 1px dashed #cbd5f5;
  border-radius: 12px;
  padding: 0.75rem;
  background-color: #f5f7ff;
  display: inline-flex;
}

.image-preview img {
  max-width: 260px;
  max-height: 180px;
  border-radius: 10px;
}

.advanced-block {
  border-top: 1px dashed #dbeafe;
  padding-top: 1rem;
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.toggle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.advanced-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.advanced-grid > label {
  flex: 1 1 180px;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.rotation-control {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.rotation-inputs {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.rotation-inputs input[type='range'] {
  flex: 1 1 auto;
}

.rotation-inputs input[type='number'] {
  width: 80px;
}

@media (max-width: 768px) {
  .field.row {
    flex-direction: column;
  }

  .resize-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .rotation-inputs {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>



