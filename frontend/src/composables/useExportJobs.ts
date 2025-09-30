import { computed, reactive, ref } from 'vue'

interface ExportJobView {
  id: string
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  message?: string
  currentFile?: string
  outputDirectory?: string
  totalFiles: number
  processedFiles: number
  successCount: number
  failureCount: number
  progress: number
  createdAt: string
  updatedAt: string
  results?: Array<{ sourceName: string; outputName?: string; success: boolean; message?: string }>
}

interface SubmitExportPayload {
  files: File[]
  config: Record<string, unknown>
}

const POLL_INTERVAL = 2000

export function useExportJobs() {
  const activeJobId = ref<string | null>(null)
  const jobs = reactive(new Map<string, ExportJobView>())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pollingHandles = new Map<string, number>()

  const latestJobs = computed(() => Array.from(jobs.values()).sort((a, b) => b.createdAt.localeCompare(a.createdAt)))
  const activeJob = computed(() => (activeJobId.value ? jobs.get(activeJobId.value) ?? null : null))

  async function submitExport(payload: SubmitExportPayload) {
    loading.value = true
    error.value = null

    try {
      const formData = new FormData()
      formData.append('config', JSON.stringify(payload.config))
      payload.files.forEach((file) => formData.append('files', file, file.name))

      const response = await fetch('/api/export', {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        throw new Error(await response.text())
      }

      const job = (await response.json()) as ExportJobView
      jobs.set(job.id, job)
      activeJobId.value = job.id
      startPolling(job.id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '导出失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  function startPolling(jobId: string) {
    stopPolling(jobId)
    const handle = window.setInterval(async () => {
      try {
        const response = await fetch(`/api/export/${encodeURIComponent(jobId)}/status`)
        if (response.status === 404) {
          stopPolling(jobId)
          jobs.delete(jobId)
          return
        }

        if (!response.ok) {
          throw new Error(await response.text())
        }

        const job = (await response.json()) as ExportJobView
        jobs.set(job.id, job)

        if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(job.status)) {
          stopPolling(jobId)
        }
      } catch (err) {
        console.error(err)
        stopPolling(jobId)
      }
    }, POLL_INTERVAL)
    pollingHandles.set(jobId, handle)
  }

  function stopPolling(jobId: string) {
    const handle = pollingHandles.get(jobId)
    if (handle !== undefined) {
      window.clearInterval(handle)
      pollingHandles.delete(jobId)
    }
  }

  async function cancelJob(jobId: string) {
    try {
      const response = await fetch(`/api/export/${encodeURIComponent(jobId)}/cancel`, {
        method: 'POST',
      })
      if (!response.ok) {
        throw new Error(await response.text())
      }
      const job = (await response.json()) as ExportJobView
      jobs.set(job.id, job)
      stopPolling(jobId)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '取消任务失败'
      throw err
    }
  }

  async function refreshJobs() {
    try {
      const response = await fetch('/api/export')
      if (!response.ok) {
        throw new Error(await response.text())
      }
      const jobList = (await response.json()) as ExportJobView[]
      jobs.clear()
      jobList.forEach((job) => {
        jobs.set(job.id, job)
        if (job.status === 'RUNNING' || job.status === 'QUEUED') {
          startPolling(job.id)
        }
      })
    } catch (err) {
      console.error(err)
    }
  }

  function dispose() {
    pollingHandles.forEach((handle) => window.clearInterval(handle))
    pollingHandles.clear()
  }

  return {
    activeJobId,
    activeJob,
    jobs,
    latestJobs,
    loading,
    error,
    submitExport,
    cancelJob,
    refreshJobs,
    startPolling,
    stopPolling,
    dispose,
  }
}
