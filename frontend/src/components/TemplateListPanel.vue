<script setup lang="ts">
import type { Template } from '../services/api'

const { templates, selectedId, loading, deleteBusyId, formatDate } = defineProps<{
  templates: Template[]
  selectedId: string | null
  loading: boolean
  deleteBusyId: string | null
  formatDate: (iso: string) => string | null
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'select', id: string): void
  (e: 'delete', id: string): void
}>()

function onRefresh() {
  emit('refresh')
}

function onSelect(id: string) {
  emit('select', id)
}

function onDelete(id: string) {
  emit('delete', id)
}
</script>

<template>
  <div class="template-list">
    <div class="list-header">
      <h3>已保存模板</h3>
      <button type="button" class="ghost small" :disabled="loading" @click="onRefresh">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <p v-if="loading" class="muted">正在加载模板...</p>
    <p v-else-if="!templates.length" class="muted">暂无模板。保存后将显示在此处。</p>

    <ul v-else class="list">
      <li v-for="item in templates" :key="item.id" :class="{ active: selectedId === item.id }">
        <div class="list-main">
          <div>
            <p class="list-title">{{ item.name }}</p>
            <p class="list-meta">最近更新：{{ formatDate(item.updatedAt) }}</p>
          </div>
          <div class="list-actions">
            <button type="button" class="ghost small" @click="onSelect(item.id)">加载</button>
            <button
              type="button"
              class="danger small"
              :disabled="deleteBusyId === item.id"
              @click="onDelete(item.id)"
            >
              {{ deleteBusyId === item.id ? '删除中...' : '删除' }}
            </button>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.template-list {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  background-color: #f8fafc;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.list li {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 0.85rem 1rem;
  background-color: #ffffff;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.list li.active {
  border-color: #2563eb;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.16);
}

.list-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.list-title {
  margin: 0;
  font-weight: 600;
}

.list-meta {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
  color: #6b7280;
}

.list-actions {
  display: flex;
  gap: 0.5rem;
}

button.ghost {
  background-color: transparent;
  border: 1px solid #cbd5f5;
  color: #1e3a8a;
  padding: 0.35rem 0.9rem;
  border-radius: 10px;
  font-size: 0.85rem;
  cursor: pointer;
}

button.danger {
  background-color: #ef4444;
  border: 1px solid #ef4444;
  color: #ffffff;
  padding: 0.35rem 0.75rem;
  border-radius: 10px;
  font-size: 0.85rem;
  cursor: pointer;
}

button.danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.muted {
  color: #6b7280;
  font-size: 0.95rem;
}
</style>
