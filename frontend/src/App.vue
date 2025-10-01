<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import ImageWorkspace from './components/ImageWorkspace.vue'
import TemplateFormPanel from './components/TemplateFormPanel.vue'
import TemplateListPanel from './components/TemplateListPanel.vue'
import { useExportJobs } from './composables/useExportJobs'
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

const {
  submitExport,
  cancelJob,
  latestJobs: exportJobs,
  activeJob,
  loading: exportLoading,
  error: exportJobError,
  refreshJobs,
  dispose: disposeExport,
} = useExportJobs()

const exportSubmitError = ref<string | null>(null)
const exportSuccessMessage = ref<string | null>(null)

const canExport = computed(() => images.value.length > 0 && !exportLoading.value)
const activeJobProgress = computed(() => (activeJob.value ? Math.round(activeJob.value.progress * 100) : 0))
const hasActiveRunningJob = computed(
  () =>
    !!activeJob.value &&
    (activeJob.value.status === 'RUNNING' || activeJob.value.status === 'QUEUED')
)

const activeJobResults = computed(() => (activeJob.value?.results ?? []))
const activeJobOutputDir = computed(() => activeJob.value?.outputDirectory ?? '')

const statusLabels: Record<string, string> = {
  QUEUED: '排队中',
  RUNNING: '处理中',
  COMPLETED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消',
}

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

function ensureWatermarkConfig(config: WatermarkConfig) {
  if (!config.type) {
    config.type = 'text'
  }

  if (config.type === 'image') {
    if (!config.image) {
      config.image = {
        name: '',
        mime: 'image/png',
        data: '',
        scale: 0.3,
        opacity: 80,
        cacheKey: `${Date.now()}`,
      }
    } else {
      config.image.name = config.image.name ?? ''
      config.image.mime = config.image.mime ?? 'image/png'
      config.image.cacheKey = config.image.cacheKey ?? `${Date.now()}`
      config.image.opacity = clampOpacity(config.image.opacity)
      config.image.scale = clampImageScale(config.image.scale)
    }
  } else {
    config.type = 'text'
    if (!config.text) {
      config.text = { ...defaultTextWatermark }
    }
    const text = config.text
    text.content = text.content ?? defaultTextWatermark.content
    text.fontFamily = text.fontFamily ?? defaultTextWatermark.fontFamily
    text.fontSize = text.fontSize ?? defaultTextWatermark.fontSize
    text.bold = text.bold ?? false
    text.italic = text.italic ?? false
    text.color = text.color ?? defaultTextWatermark.color
    text.opacity = text.opacity ?? defaultTextWatermark.opacity
    if (text.stroke && (!text.stroke.width || text.stroke.width <= 0)) {
      text.stroke.width = 1
    }
    if (text.shadow === undefined && defaultTextWatermark.shadow) {
      text.shadow = JSON.parse(JSON.stringify(defaultTextWatermark.shadow))
    }
  }

  ensureLayoutConfig(config)
}

function ensureLayoutConfig(config: WatermarkConfig) {
  if (!config.layout) {
    config.layout = {
      preset: 'bottom-right',
      x: 0.9,
      y: 0.9,
      rotationDeg: 0,
    }
  }
  config.layout.preset = config.layout.preset ?? 'custom'
  config.layout.x = clamp01(config.layout.x ?? 0.9)
  config.layout.y = clamp01(config.layout.y ?? 0.9)
  config.layout.rotationDeg = Number.isFinite(config.layout.rotationDeg)
    ? (config.layout.rotationDeg as number)
    : 0
}

function clamp01(value: number) {
  return Math.min(1, Math.max(0, value))
}

function clampOpacity(value?: number | null) {
  const numeric = typeof value === 'number' && Number.isFinite(value) ? value : 80
  return Math.min(100, Math.max(0, numeric))
}

function clampImageScale(value?: number | null) {
  const numeric = typeof value === 'number' && Number.isFinite(value) ? value : 0.3
  return Math.min(1, Math.max(0.05, numeric))
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

function handleWorkspaceLayoutChange(position: { x: number; y: number }) {
  ensureWatermarkConfig(templateForm.value.watermarkConfig)
  if (!templateForm.value.watermarkConfig.layout) {
    templateForm.value.watermarkConfig.layout = {
      preset: 'custom',
      x: clamp01(position.x),
      y: clamp01(position.y),
      rotationDeg: 0,
    }
  } else {
    templateForm.value.watermarkConfig.layout.x = clamp01(position.x)
    templateForm.value.watermarkConfig.layout.y = clamp01(position.y)
  }
  templateForm.value.watermarkConfig.layout.preset = 'custom'
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
  ensureWatermarkConfig(cloned.watermarkConfig)
  ensureExportConfig(cloned.exportConfig)
  templateForm.value = cloned
}

function applyLastSettings(settings: LastSettings) {
  const clonedWatermark = JSON.parse(JSON.stringify(settings.watermarkConfig)) as WatermarkConfig
  const clonedExport = JSON.parse(JSON.stringify(settings.exportConfig)) as ExportConfig
  ensureWatermarkConfig(clonedWatermark)
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
    ensureWatermarkConfig(payload.watermarkConfig)
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
    ensureWatermarkConfig(payload.watermarkConfig)
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

async function handleExport() {
  exportSubmitError.value = null
  exportSuccessMessage.value = null

  if (!images.value.length) {
    exportSubmitError.value = '请先导入图片'
    return
  }

  try {
    const payload = {
      watermarkConfig: JSON.parse(JSON.stringify(templateForm.value.watermarkConfig)),
      exportConfig: JSON.parse(JSON.stringify(templateForm.value.exportConfig)),
    }

    ensureWatermarkConfig(payload.watermarkConfig)
    ensureExportConfig(payload.exportConfig)

    await submitExport({
      files: images.value.map((item) => item.file),
      config: payload,
    })

    exportSuccessMessage.value = '导出任务已提交，正在处理…'
  } catch (err) {
    console.error(err)
    exportSubmitError.value = err instanceof Error ? err.message : '导出任务提交失败'
  }
}

async function handleCancelExport(jobId: string) {
  exportSubmitError.value = null
  try {
    await cancelJob(jobId)
    exportSuccessMessage.value = '已请求取消任务'
  } catch (err) {
    console.error(err)
    exportSubmitError.value = err instanceof Error ? err.message : '取消任务失败'
  }
}

function renderJobStatus(status: string) {
  return statusLabels[status] ?? status
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
  void refreshJobs()
})

onBeforeUnmount(() => {
  disposeExport()
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
      <ImageWorkspace :watermark-config="templateForm.watermarkConfig" @images-updated="handleImagesUpdated" @layout-change="handleWorkspaceLayoutChange" />
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

    <section class="export-section">
      <header class="section-header">
        <div>
          <h2>批量导出</h2>
          <p class="section-subtitle">使用当前图片和水印配置生成导出任务。</p>
        </div>
        <div class="export-actions">
          <button type="button" class="primary" :disabled="!canExport || exportLoading" @click="handleExport">
            {{ exportLoading ? '提交中...' : '开始导出' }}
          </button>
          <button
            v-if="activeJob && hasActiveRunningJob"
            type="button"
            class="ghost"
            :disabled="exportLoading"
            @click="handleCancelExport(activeJob.id)"
          >
            取消当前任务
          </button>
        </div>
      </header>
      <p v-if="exportSubmitError" class="error">{{ exportSubmitError }}</p>
      <p v-else-if="exportJobError" class="error">{{ exportJobError }}</p>
      <p v-else-if="exportSuccessMessage" class="success">{{ exportSuccessMessage }}</p>

      <div v-if="activeJob" class="job-card">
        <div class="job-header">
          <span class="status-tag">{{ renderJobStatus(activeJob.status) }}</span>
          <span class="muted">更新时间：{{ formatDate(activeJob.updatedAt) }}</span>
        </div>
        <p class="muted">{{ activeJob.message }}</p>
        <div class="progress">
          <div class="progress-bar" :style="{ width: activeJobProgress + '%' }"></div>
        </div>
        <p class="progress-text">
          {{ activeJob.processedFiles }} / {{ activeJob.totalFiles }} （成功 {{ activeJob.successCount }}，失败
          {{ activeJob.failureCount }}）
        </p>
        <p v-if="activeJobOutputDir" class="muted output-dir">输出目录：{{ activeJobOutputDir }}</p>
        <div v-if="activeJobResults.length" class="result-list">
          <h4>文件明细</h4>
          <ul>
            <li v-for="item in activeJobResults" :key="item.sourceName" :class="{ fail: !item.success }">
              <span class="result-name">{{ item.sourceName }} → {{ item.outputName || '未生成' }}</span>
              <span class="result-status">{{ item.success ? '成功' : '失败' }}</span>
              <span v-if="!item.success && item.message" class="result-message">{{ item.message }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div v-if="exportJobs.length" class="job-history">
        <h4>任务历史</h4>
        <ul>
          <li v-for="job in exportJobs" :key="job.id">
            <div class="history-main">
              <span class="status-tag">{{ renderJobStatus(job.status) }}</span>
              <span class="muted">{{ formatDate(job.updatedAt) }}</span>
            </div>
            <p class="history-message">{{ job.message || '暂无提示信息' }}</p>
          </li>
        </ul>
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

.export-section {
  margin-top: 2.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 1.5rem 2rem;
  background-color: #ffffff;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.export-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.job-card {
  margin-top: 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 1rem 1.25rem;
  background-color: #f8fafc;
}

.job-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.5rem;
  flex-wrap: wrap;
}

.status-tag {
  display: inline-block;
  padding: 0.15rem 0.65rem;
  border-radius: 999px;
  background-color: #e2e8f0;
  color: #1f2937;
  font-size: 0.85rem;
}

.progress {
  height: 8px;
  background-color: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
  margin: 0.75rem 0;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
  transition: width 0.3s ease;
}

.progress-text {
  margin: 0;
  font-size: 0.9rem;
  color: #475569;
}

.output-dir {
  margin: 0.25rem 0 0;
  font-size: 0.9rem;
}

.result-list {
  margin-top: 1rem;
}

.result-list ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.result-list li {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 0.5rem 0.75rem;
  background-color: #ebf5ff;
  flex-wrap: wrap;
}

.result-list li.fail {
  border-color: #fecaca;
  background-color: #fee2e2;
}

.result-name {
  flex: 1 1 auto;
  font-weight: 600;
  color: #1e3a8a;
}

.result-status {
  font-weight: 600;
  color: #2563eb;
}

.result-list li.fail .result-status {
  color: #b91c1c;
}

.result-message {
  flex: 1 1 100%;
  color: #b91c1c;
  font-size: 0.9rem;
}

.job-history {
  margin-top: 1.5rem;
}

.job-history ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.job-history li {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.75rem 1rem;
  background-color: #f1f5f9;
}

.history-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.history-message {
  margin: 0.35rem 0 0;
  color: #475569;
  font-size: 0.9rem;
}

.success {
  color: #15803d;
  font-size: 0.95rem;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
