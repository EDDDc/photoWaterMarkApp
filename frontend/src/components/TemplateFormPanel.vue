<script setup lang="ts">
import type { TemplateRequest } from '../services/api'

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

function updateForm(mutator: (draft: TemplateRequest) => void) {
  const draft: TemplateRequest = JSON.parse(JSON.stringify(props.form))
  mutator(draft)
  emit('update:form', draft)
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
    if (!draft.watermarkConfig.text) {
      draft.watermarkConfig.text = { content: value }
    }
    draft.watermarkConfig.type = 'text'
    draft.watermarkConfig.text!.content = value
  })
}

function onFontFamilyChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  updateForm((draft) => {
    draft.watermarkConfig.type = 'text'
    if (!draft.watermarkConfig.text) {
      draft.watermarkConfig.text = { content: '' }
    }
    draft.watermarkConfig.text!.fontFamily = value
  })
}

function onFontSizeChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    draft.watermarkConfig.type = 'text'
    if (!draft.watermarkConfig.text) {
      draft.watermarkConfig.text = { content: '' }
    }
    draft.watermarkConfig.text!.fontSize = Number.isFinite(value) ? value : undefined
  })
}

function onOpacityChange(event: Event) {
  const value = Number((event.target as HTMLInputElement).value)
  updateForm((draft) => {
    draft.watermarkConfig.type = 'text'
    if (!draft.watermarkConfig.text) {
      draft.watermarkConfig.text = { content: '' }
    }
    draft.watermarkConfig.text!.opacity = Number.isFinite(value) ? value : undefined
  })
}

function onFormatChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value as 'png' | 'jpeg'
  updateForm((draft) => {
    draft.exportConfig.format = value
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
</script>

<template>
  <form class="template-form" @submit.prevent="onSubmit">
    <h3>水印模板</h3>

    <label class="field">
      <span>模板名称</span>
      <input :value="form.name" type="text" required maxlength="120" placeholder="输入模板名称" @input="onNameChange" />
    </label>

    <fieldset class="field">
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
    </fieldset>

    <fieldset class="field">
      <legend>导出配置</legend>
      <div class="field row">
        <label>
          <span>输出格式</span>
          <select :value="form.exportConfig.format" @change="onFormatChange">
            <option value="png">PNG（透明通道）</option>
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
          <p class="muted">将当前表单保存为“上次使用的设置”，下次打开应用时会自动加载。</p>
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
  min-width: 140px;
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

@media (max-width: 768px) {
  .field.row {
    flex-direction: column;
  }
}
</style>