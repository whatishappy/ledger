<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Loading } from '@element-plus/icons-vue'
import * as exportApi from '@/api/export'
import { ExportTaskStatusCode } from '@/types/api'
import type { ExportRequest, Result } from '@/types/api'
import { AccountType, type AccountTypeValue } from '@/types/business'
import { ALL_CATEGORIES } from '@/types/business'
import { downloadBlob, resolveDownloadFilename } from '@/utils/auth'

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ exported: [] }>()

const exporting = ref(false)
const phase = ref<'idle' | 'sync' | 'polling' | 'done'>('idle')

const form = reactive({
  type: undefined as AccountTypeValue | undefined,
  category: '' as string,
  dateRange: [] as string[],
})

function resetForm() {
  form.type = undefined
  form.category = ''
  form.dateRange = []
  phase.value = 'idle'
}

const exportPhaseText = {
  idle: '',
  sync: '正在生成文件…',
  polling: '异步导出任务处理中，请稍候…',
  done: '',
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  phase.value = 'idle'
  try {
    // 组装筛选条件
    const params: ExportRequest = {}
    if (form.type !== undefined) params.type = form.type
    if (form.category) params.category = form.category
    if (form.dateRange.length === 2) {
      params.startDate = form.dateRange[0]
      params.endDate = form.dateRange[1]
    }

    const response = await exportApi.exportExcel(params)
    const contentType = response.headers?.['content-type'] as string | undefined

    if (contentType && contentType.includes('application/json')) {
      // 异步任务：解析 taskId 后轮询
      phase.value = 'polling'
      const text = await (response.data as Blob).text()
      const result = JSON.parse(text) as Result<string>
      if (result.code !== 0) {
        ElMessage.error(result.message || '导出失败')
        return
      }
      await pollTask(result.data)
    } else {
      // 同步导出：文件流直接下载
      phase.value = 'sync'
      const filename = resolveDownloadFilename(response, '账目导出.xlsx')
      downloadBlob(response.data as Blob, filename)
      phase.value = 'done'
      ElMessage.success('导出完成')
    }
    emit('exported')
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    exporting.value = false
  }
}

/** 轮询异步导出任务，statusCode===2 才下载（§3.3） */
async function pollTask(taskId: string) {
  const maxTries = 60 // 最多等 2 分钟
  for (let i = 0; i < maxTries; i++) {
    await sleep(2000)
    let task
    try {
      task = await exportApi.getExportStatus(taskId)
    } catch {
      continue
    }
    if (task.statusCode === ExportTaskStatusCode.COMPLETED) {
      const resp = await exportApi.downloadExportFile(taskId)
      const filename = resolveDownloadFilename(resp, '账目导出.xlsx')
      downloadBlob(resp.data as Blob, filename)
      phase.value = 'done'
      ElMessage.success(`导出完成，共 ${task.rowCount ?? 0} 行`)
      return
    }
    if (task.statusCode === ExportTaskStatusCode.FAILED) {
      phase.value = 'done'
      ElMessage.error(task.errorMsg || '导出失败')
      return
    }
    if (task.statusCode === ExportTaskStatusCode.EXPIRED) {
      phase.value = 'done'
      ElMessage.warning('文件已过期，请重新导出')
      return
    }
  }
  phase.value = 'done'
  ElMessage.warning('导出超时，请稍后重试')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="导出 Excel"
    width="min(440px, 92vw)"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-form :model="form" label-position="top">
      <div class="row">
        <el-form-item label="收支类型" class="half">
          <el-select v-model="form.type" placeholder="全部类型" clearable style="width: 100%">
            <el-option :value="AccountType.EXPENSE" label="支出" />
            <el-option :value="AccountType.INCOME" label="收入" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" class="half">
          <el-select v-model="form.category" placeholder="全部分类" clearable style="width: 100%">
            <el-option v-for="cat in ALL_CATEGORIES" :key="cat" :value="cat" :label="cat" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="form.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 100%"
        />
      </el-form-item>
      <p class="tip">导出数据为符合当前筛选条件的账目记录。</p>
    </el-form>

    <div v-if="exporting" class="exporting">
      <el-icon class="spin"><Loading /></el-icon>
      <span>{{ exportPhaseText[phase] }}</span>
    </div>

    <template #footer>
      <el-button @click="visible = false" :disabled="exporting">取消</el-button>
      <el-button type="primary" :loading="exporting" :icon="Download" @click="handleExport">
        开始导出
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.row {
  display: flex;
  gap: 12px;
}
.half {
  flex: 1;
}

.tip {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.exporting {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
