<script setup lang="ts">
/**
 * 账单导入页 — /bill/import（V2.1 新增，IM-01~IM-03）
 * 支付宝/微信账单 CSV 三步导入：上传 → 预览 → 确认。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type UploadFile, type UploadRawFile } from 'element-plus'
import { UploadFilled, Check, Document } from '@element-plus/icons-vue'
import * as billApi from '@/api/bill'
import type { BillImportPreviewVO, BillImportResultVO, ImportedBillRowVO } from '@/types/api'
import { ALL_CATEGORIES, type CategoryName } from '@/types/business'
import { formatMoney } from '@/utils/format'

const active = ref<0 | 1 | 2>(0)
const loadError = ref(false)

/* ---------- 步骤 1：上传 ---------- */
const supportedSources = ref<Record<string, number>>({})
const selectedSource = ref<string>('alipay')
const uploadedFile = ref<File | null>(null)
const preview = ref<BillImportPreviewVO | null>(null)

/** 来源选项：仅展示状态码为 1 的来源 */
const sourceOptions = computed(() =>
  Object.entries(supportedSources.value)
    .filter(([, code]) => code === 1)
    .map(([key]) => ({ value: key, label: sourceLabel(key) })),
)

function sourceLabel(key: string): string {
  if (key === 'alipay') return '支付宝'
  if (key === 'wechat') return '微信'
  return key
}

async function loadSupport() {
  try {
    supportedSources.value = await billApi.getSupportedSources()
    // 默认选中第一个支持项
    const first = sourceOptions.value[0]?.value
    if (first) selectedSource.value = first
  } catch {
    // 接口失败不阻塞，仍允许尝试上传
  }
}

function handleUpload(file: UploadFile) {
  const raw = file.raw as UploadRawFile
  if (!raw) return
  // 限制 ≤10MB
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 10MB')
    return
  }
  if (!raw.name.toLowerCase().endsWith('.csv')) {
    ElMessage.warning('仅支持 .csv 账单文件')
    return
  }
  uploadedFile.value = raw as File
  active.value = 1
}

function resetUpload() {
  uploadedFile.value = null
  preview.value = null
  active.value = 0
}

/* ---------- 步骤 2：预览 ---------- */
const previewing = ref(false)

async function parsePreview() {
  if (!uploadedFile.value) {
    ElMessage.warning('请先上传文件')
    return
  }
  previewing.value = true
  loadError.value = false
  try {
    preview.value = await billApi.previewBill(uploadedFile.value, selectedSource.value)
    active.value = 2
  } catch {
    loadError.value = true
    preview.value = null
  } finally {
    previewing.value = false
  }
}

/* ---------- 步骤 3：导入 ---------- */
/** 分类覆盖映射：preCategory -> 系统分类 */
const categoryOverrides = reactive<Record<string, string>>({})

/** 标记需手动确认的行（未识别分类） */
const unconfirmedRows = computed(() =>
  (preview.value?.sampleRows ?? []).filter((r) => !r.preCategory),
)

function applyOverride(row: ImportedBillRowVO, target: string) {
  categoryOverrides[row.preCategory ?? '__unknown__'] = target
}

const confirming = ref(false)
const importResult = ref<BillImportResultVO | null>(null)

async function confirmImport(skipConflicts: boolean) {
  if (!preview.value) return
  confirming.value = true
  try {
    const result = await billApi.confirmBillImport({
      token: preview.value.token,
      categoryOverrides,
      skipConflicts,
    })
    importResult.value = result
    ElMessage.success(`成功导入 ${result.imported} 条`)
  } finally {
    confirming.value = false
  }
}

function startOver() {
  resetUpload()
  importResult.value = null
  active.value = 0
  // 清空覆盖映射
  Object.keys(categoryOverrides).forEach((k) => delete categoryOverrides[k])
}

/* ---------- 系统分类下拉选项 ---------- */
const categoryOptions: CategoryName[] = [...ALL_CATEGORIES]

onMounted(loadSupport)
</script>

<template>
  <div class="bill-import-page">
    <!-- 标题 + 步骤 -->
    <div class="toolbar">
      <h2 class="page-title">账单导入</h2>
      <el-steps v-if="!importResult" :active="active" finish-status="success" align-center>
        <el-step title="上传账单" />
        <el-step title="预览解析" />
        <el-step title="确认导入" />
      </el-steps>
    </div>

    <!-- 导入结果视图 -->
    <el-card v-if="importResult" shadow="never" class="result-card">
      <div class="result-summary">
        <el-icon :size="40" class="success-icon"><Check /></el-icon>
        <div>
          <div class="result-title">导入完成</div>
          <div class="result-detail">
            成功 {{ importResult.imported }} 条 ｜ 跳过 {{ importResult.skipped }} 条
          </div>
          <div class="result-detail">
            收入合计：¥{{ formatMoney(importResult.amountSumIncome) }} ｜
            支出合计：¥{{ formatMoney(importResult.amountSumExpense) }}
          </div>
        </div>
      </div>
      <div class="actions">
        <el-button type="primary" @click="startOver">继续导入</el-button>
      </div>
    </el-card>

    <!-- 步骤 1：上传 -->
    <el-card v-else-if="active === 0" shadow="never" class="step-card">
      <div class="source-row">
        <span class="row-label">账单来源：</span>
        <el-radio-group v-model="selectedSource">
          <el-radio
            v-for="opt in sourceOptions"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".csv"
        :on-change="handleUpload"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 CSV 文件到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持支付宝/微信 CSV 账单，单文件 ≤ 10MB</div>
        </template>
      </el-upload>
    </el-card>

    <!-- 步骤 2：预览解析 -->
    <el-card v-else shadow="never" class="step-card">
      <div class="upload-info">
        <el-icon><Document /></el-icon>
        <span>{{ uploadedFile?.name }}</span>
        <el-button text @click="resetUpload">重新选择</el-button>
      </div>
      <div class="actions">
        <el-button type="primary" :loading="previewing" @click="parsePreview">
          {{ previewing ? '解析中...' : '解析预览' }}
        </el-button>
      </div>
      <el-result
        v-if="loadError"
        icon="error"
        title="解析失败"
        sub-title="账单格式不支持或文件损坏"
      >
        <template #extra>
          <el-button @click="resetUpload">重新上传</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 步骤 3：预览 + 导入 -->
    <el-card v-else shadow="never" class="step-card">
      <div v-if="preview" class="preview-summary">
        共 {{ preview.count }} 条 ｜ 合计 ¥{{ formatMoney(preview.amountSum) }} ｜
        冲突 {{ preview.conflicts }} 条
      </div>

      <el-table v-if="preview" :data="preview.sampleRows" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="counterparty" label="交易对方" min-width="140" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            ¥ {{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column label="分类" width="200">
          <template #default="{ row }">
            <el-tag v-if="row.preCategory" size="small" type="success">✅ {{ row.preCategory }}</el-tag>
            <el-select
              v-else
              size="small"
              placeholder="⚠️ 选择分类"
              @change="(val: string) => applyOverride(row, val)"
            >
              <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
      </el-table>

      <div v-if="unconfirmedRows.length" class="warn-tip">
        ⚠️ 有 {{ unconfirmedRows.length }} 条记录未识别分类，可手动选择映射后导入，
        或选择「跳过冲突项」仅导入已识别的记录。
      </div>

      <div class="actions">
        <el-button :loading="confirming" @click="confirmImport(true)">仅导入已映射（跳过冲突）</el-button>
        <el-button type="primary" :loading="confirming" @click="confirmImport(false)">
          全部导入（未分类留草稿）
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.bill-import-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.step-card,
.result-card {
  padding: 8px 4px;
}

.source-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.row-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.upload-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--el-text-color-regular);
}

.preview-summary {
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.warn-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: var(--el-color-warning-light-9);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-color-warning-dark-2);
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
}

.result-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
}
.success-icon {
  color: var(--el-color-success);
}
.result-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}
.result-detail {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
