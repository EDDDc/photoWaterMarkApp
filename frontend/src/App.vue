<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchHealth, type HealthResponse } from './services/api'

const loading = ref(false)
const error = ref<string | null>(null)
const health = ref<HealthResponse | null>(null)

async function loadHealth() {
  loading.value = true
  error.value = null
  try {
    health.value = await fetchHealth()
  } catch (err) {
    console.error(err)
    error.value = err instanceof Error ? err.message : 'Unknown error'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadHealth()
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
      <p v-if="health?.timestamp" class="timestamp">最近响应：{{ new Date(health.timestamp).toLocaleString() }}</p>
      <button type="button" class="refresh" :disabled="loading" @click="loadHealth">
        {{ loading ? '刷新中...' : '重新检测' }}
      </button>
    </section>

    <section class="instructions">
      <h2>开发指引</h2>
      <ol>
        <li>运行 <code>./mvnw spring-boot:run</code> 启动后端。</li>
        <li>运行 <code>npm run dev</code> 启动前端，访问 <code>http://localhost:5173</code>。</li>
        <li>观察此页面的后端连通性状态，确认接口可用。</li>
      </ol>
    </section>
  </main>
</template>

<style scoped>
.page {
  margin: 0 auto;
  max-width: 720px;
  padding: 2.5rem 1.5rem 3rem;
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
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.08);
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
  padding: 0.5rem 1.5rem;
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
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.25);
  transform: translateY(-1px);
}

.instructions {
  margin-top: 2.5rem;
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
</style>
