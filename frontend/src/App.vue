<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ImageWorkspace from './components/ImageWorkspace.vue'
import TemplateFormPanel from './components/TemplateFormPanel.vue'
import TemplateListPanel from './components/TemplateListPanel.vue'
import type { ImportedImageMeta } from './types/images'
import {
  deleteLastSettings,
  deleteTemplate,
  fetchFonts,
  fetchHealth,
  fetchLastSettings,
  fetchTemplates,
  saveLastSettings,
  saveTemplate,
  type ExportConfig,
  type HealthResponse,
  type LastSettings,
  type Template,
  type TemplateRequest,
  type TextWatermarkConfig,
  type WatermarkConfig,
} from './services/api'

type TemplateForm = TemplateRequest

const loading = ref(false)
const error = ref<string | null>(null)
const health = ref<HealthResponse | null>(null)

const fonts = ref<string[]>([])
const fontsLoading = ref(false)
const templates = ref<Template[]>([])
const templatesLoading = ref(false)
const templateError = ref<string | null>(null)

const selectedTemplateId = ref<string | null>(null)

const lastSettingsTimestamp = ref<string | null>(null)
const lastSettingsLoading = ref(false)
const lastSettingsError = ref<string | null>(null)
const savingLastSettings = ref(false)
const clearingLastSettings = ref(false)

const images = ref<ImportedImageMeta[]>([])

const defaultTextWatermark: TextWatermarkConfig = {
  content: '示例水印',
  fontFamily: 'Microsoft YaHei',
  fontSize: 32,
  bold: false,
  italic: false,
  color: '#FFFFFF',
  opacity: 80,
  shadow: {
    color: 'rgba(0,0,0,0.4)',
    offsetX: 2,
    offsetY: 2,
    blur: 4,
  },
}

const initialExportConfig: ExportConfig = {
  format: 'png',
  naming: {
    keepOriginal: true,
    prefix: '',
    suffix: '_watermark',
  },
}

const templateForm = ref<TemplateForm>(createDefaultForm())

const templateSaving = ref(false)
const deleteBusyId = ref<string | null>(null)

const availableFonts = computed(() => (fonts.value.length ? fonts.value : [defaultTextWatermark.fontFamily ?? 'Microsoft YaHei']))
const lastSettingsDisplay = computed(() => formatDate(lastSettingsTimestamp.value))

function createDefaultForm(): TemplateForm {
  return {
    name: '默认模板',
    watermarkConfig: {
      type: 'text',
      text: { ...defaultTextWatermark },
      layout: {
        preset: 'bottom-right',
        x: 0.9,
        y: 0.9,
        rotationDeg: 0,
      },
    },
    exportConfig: {
      ...initialExportConfig,
      naming: { ...initialExportConfig.naming },
    },
  }
}

function ensureTextConfig(config: WatermarkConfig) {
  if (config.type === 'text') {
    if (!config.text) {
      config.text = { ...defaultTextWatermark }
    }
    if (!config.layout) {
      config.layout = {
        preset: 'bottom-right',
        x: 0.9,
        y: 0.9,
        rotationDeg: 0,
      }
    }
  }
}

function ensureExportConfig(config: ExportConfig) {
  if (!config.format) {
    config.format = 'png'
  }
  if (!config.naming) {
    config.naming = { keepOriginal: true, prefix: '', suffix: '_watermark' }
  }
}

function handleImagesUpdated(list: ImportedImageMeta[]) {
  images.value = list
}

async function loadHealth() {
  loading.value = true
  error.value = null
  try {
    health.value = await fetchHealth()
  } catch (err) {
    console.error(err)
    error.value = err instanceof Error ? err.message : '未知错误'
  } finally {
    loading.value = false
  }
}

async function loadFonts() {
  fontsLoading.value = true
  try {
    fonts.value = await fetchFonts()
  } catch (err) {
    console.warn('字体加载失败', err)
  } finally {
    fontsLoading.value = false
  }
}

async function loadTemplates() {
  templatesLoading.value = true
  templateError.value = null
  try {
    templates.value = await fetchTemplates()
  } catch (err) {
    console.error(err)
    templateError.value = err instanceof Error ? err.message : '无法获取模板列表'
  } finally {
    templatesLoading.value = false
  }
}

async function loadLastSettings() {
  lastSettingsLoading.value = true
  lastSettingsError.value = null
  try {
    const result = await fetchLastSettings()
    if (result) {
      applyLastSettings(result)
      lastSettingsTimestamp.value = result.updatedAt
    } else {
      lastSettingsTimestamp.value = null
    }
  } catch (err) {
    console.error(err)
    lastSettingsError.value = err instanceof Error ? err.message : '无法读取最近设置'
  } finally {
    lastSettingsLoading.value = false
  }
}

function resetForm() {
  selectedTemplateId.value = null
  templateForm.value = createDefaultForm()
}

function applyTemplateToForm(template: Template) {
  selectedTemplateId.value = template.id
  const cloned = JSON.parse(JSON.stringify(template)) as TemplateForm
  ensureTextConfig(cloned.watermarkConfig)
  ensureExportConfig(cloned.exportConfig)
  templateForm.value = cloned
}

function applyLastSettings(settings: LastSettings) {
  const clonedWatermark = JSON.parse(JSON.stringify(settings.watermarkConfig)) as WatermarkConfig
  const clonedExport = JSON.parse(JSON.stringify(settings.exportConfig)) as ExportConfig
  ensureTextConfig(clonedWatermark)
  ensureExportConfig(clonedExport)
  templateForm.value = {
    ...templateForm.value,
    watermarkConfig: clonedWatermark,
    exportConfig: clonedExport,
  }
}

async function handleSaveTemplate() {
  templateSaving.value = true
  templateError.value = null
  try {
    const payload: TemplateRequest = {
      ...templateForm.value,
      watermarkConfig: JSON.parse(JSON.stringify(templateForm.value.watermarkConfig)),
      exportConfig: JSON.parse(JSON.stringify(templateForm.value.exportConfig)),
    }
    ensureTextConfig(payload.watermarkConfig)
    ensureExportConfig(payload.exportConfig)

    const saved = await saveTemplate(payload)
    await loadTemplates()
    applyTemplateToForm(saved)
  } catch (err) {
    console.error(err)
    templateError.value = err instanceof Error ? err.message : '保存模板失败'
  } finally {
    templateSaving.value = false
  }
}

async function handleDeleteTemplate(id: string) {
  deleteBusyId.value = id
  templateError.value = null
  try {
    await deleteTemplate(id)
    if (selectedTemplateId.value === id) {
      resetForm()
    }
    await loadTemplates()
  } catch (err) {
    console.error(err)
    templateError.value = err instanceof Error ? err.message : '删除模板失败'
  } finally {
    deleteBusyId.value = null
  }
}

async function handleSaveLastSettings() {
  savingLastSettings.value = true
  lastSettingsError.value = null
  try {
    const payload = {
      watermarkConfig: JSON.parse(JSON.stringify(templateForm.value.watermarkConfig)),
      exportConfig: JSON.parse(JSON.stringify(templateForm.value.exportConfig)),
    }
    ensureTextConfig(payload.watermarkConfig)
    ensureExportConfig(payload.exportConfig)
    const saved = await saveLastSettings(payload)
    lastSettingsTimestamp.value = saved.updatedAt
  } catch (err) {
    console.error(err)
    lastSettingsError.value = err instanceof Error ? err.message : '保存默认设置失败'
  } finally {
    savingLastSettings.value = false
  }
}

async function handleClearLastSettings() {
  clearingLastSettings.value = true
  lastSettingsError.value = null
  try {
    await deleteLastSettings()
    lastSettingsTimestamp.value = null
  } catch (err) {
    console.error(err)
    lastSettingsError.value = err instanceof Error ? err.message : '清除默认设置失败'
  } finally {
    clearingLastSettings.value = false
  }
}

function onTemplateFormUpdate(next: TemplateForm) {
  templateForm.value = next
}

function onTemplateSelect(id: string) {
  const template = templates.value.find((item) => item.id === id)
  if (template) {
    applyTemplateToForm(template)
  }
}

function onTemplatesRefresh() {
  void loadTemplates()
}

function formatDate(value: string | null): string | null {
  if (!value) {
    return null
  }
  try {
    return new Date(value).toLocaleString()
  } catch (err) {
    return value
  }
}

function formatTemplateDate(iso: string): string | null {
  return formatDate(iso) ?? iso
}

onMounted(() => {
  void loadHealth()
  void loadFonts()
  void loadTemplates()
  void loadLastSettings()
})
</script>

<template>
  <main class="page">
    <header class="hero">
      <h1>Photo Watermark App</h1>
      <p class="subtitle">Spring Boot + Vue 3 开发环境</p>
    </header>

    <section class="status-card" aria-live="polite">
      <div class="status-row">
        <span class="label">后端连通性</span>
        <span class="value" :class="{ ok: health?.status === 'ok', error: !!error }">
          <template v-if="loading">检测中...</template>
          <template v-else-if="error">{{ error }}</template>
          <template v-else-if="health">{{ health.status }}</template>
          <template v-else>待检测</template>
        </span>
      </div>
      <p v-if="health?.timestamp" class="timestamp">最近响应：{{ formatDate(health.timestamp) }}</p>
      <button type="button" class="refresh" :disabled="loading" @click="loadHealth">
        {{ loading ? '刷新中...' : '重新检测' }}
      </button>
    </section>

    <section class="workspace-section">
      <header class="section-header">
        <div>
          <h2>图片工作区</h2>
          <p class="section-subtitle">导入原始图片并预览，为水印叠加和导出做准备。</p>
        </div>
        <p class="muted">当前图片：{{ images.length }}</p>
      </header>
      <ImageWorkspace @images-updated="handleImagesUpdated" />
    </section>

    <section class="templates">
      <header class="section-header">
        <div>
          <h2>水印模板管理</h2>
          <p class="section-subtitle">保存/加载您的水印配置，快速应用到批量导出流程。</p>
        </div>
        <div class="fonts-info">
          <span class="dot" :class="{ loading: fontsLoading }"></span>
          <span>可用字体：{{ fonts.length || '加载中' }}</span>
        </div>
      </header>

      <div class="template-grid">
        <TemplateFormPanel
          :form="templateForm"
          :available-fonts="availableFonts"
          :template-saving="templateSaving"
          :template-error="templateError"
          :last-settings-timestamp="lastSettingsDisplay ?? ''"
          :last-settings-loading="lastSettingsLoading"
          :saving-last-settings="savingLastSettings"
          :clearing-last-settings="clearingLastSettings"
          :last-settings-error="lastSettingsError"
          @update:form="onTemplateFormUpdate"
          @reset="resetForm"
          @save-template="handleSaveTemplate"
          @save-last-settings="handleSaveLastSettings"
          @clear-last-settings="handleClearLastSettings"
        />

        <TemplateListPanel
          :templates="templates"
          :selected-id="selectedTemplateId"
          :loading="templatesLoading"
          :delete-busy-id="deleteBusyId"
          :format-date="formatTemplateDate"
          @refresh="onTemplatesRefresh"
          @select="onTemplateSelect"
          @delete="handleDeleteTemplate"
        />
      </div>
    </section>

    <section class="instructions">
      <h2>开发指引</h2>
      <ol>
        <li>运行 <code>./mvnw spring-boot:run</code> 启动后端。</li>
        <li>运行 <code>npm run dev</code> 启动前端，访问 <code>http://localhost:5173</code>。</li>
        <li>导入图片并保存模板/默认设置，为后续批量导出与水印渲染做准备。</li>
      </ol>
    </section>
  </main>
</template>

<style scoped>
.page {
  margin: 0 auto;
  max-width: 1120px;
  padding: 2.5rem 1.75rem 4rem;
  font-family: 'Segoe UI', 'Microsoft YaHei', system-ui, -apple-system, sans-serif;
  color: #1f2937;
}

.hero {
  text-align: center;
  margin-bottom: 2rem;
}

.subtitle {
  margin: 0.5rem 0 0;
  color: #4b5563;
}

.status-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1.5rem;
  background-color: #f9fafb;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.125rem;
  font-weight: 500;
}

.label {
  color: #374151;
}

.value {
  text-transform: uppercase;
}

.value.ok {
  color: #15803d;
}

.value.error {
  color: #b91c1c;
}

.timestamp {
  margin: 0.75rem 0 1rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.refresh {
  border: 1px solid #2563eb;
  background-color: #2563eb;
  color: #ffffff;
  padding: 0.55rem 1.6rem;
  border-radius: 999px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: transform 150ms ease, box-shadow 150ms ease;
}

.refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh:not(:disabled):hover {
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.25);
  transform: translateY(-1px);
}

.workspace-section,
.templates {
  margin-top: 2.5rem;
}

.templates {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 2rem;
  background-color: #ffffff;
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  margin: 0;
  font-size: 1.6rem;
}

.section-subtitle {
  margin: 0.35rem 0 0;
  color: #6b7280;
}

.fonts-info {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: #4b5563;
  font-size: 0.95rem;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #22c55e;
}

.dot.loading {
  background-color: #facc15;
  animation: pulse 1.2s infinite ease-in-out;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.muted {
  color: #6b7280;
  font-size: 0.95rem;
}

.instructions {
  margin-top: 3rem;
}

.instructions h2 {
  font-size: 1.25rem;
  margin-bottom: 0.75rem;
}

.instructions ol {
  margin: 0;
  padding-left: 1.25rem;
  color: #374151;
  line-height: 1.6;
}

code {
  background-color: #e0e7ff;
  color: #1e3a8a;
  padding: 0.1rem 0.35rem;
  border-radius: 6px;
  font-size: 0.95rem;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

