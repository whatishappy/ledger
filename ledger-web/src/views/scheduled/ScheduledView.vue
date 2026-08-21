<script setup lang="ts">
/**
 * 定时交易管理页 — /scheduled（V2.1 新增，§15.8）
 * 管理周期性自动记账规则：Cron 表达式驱动，支持启用/停用/立即执行。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Calendar, Plus, Refresh } from '@element-plus/icons-vue'
import * as scheduledApi from '@/api/scheduled'
import type { ScheduledTransaction } from '@/types/api'
import { AccountType, getCategoriesByType, type AccountTypeValue } from '@/types/business'
import { formatMoney, typeLabel } from '@/utils/format'

const loading = ref(false)
const records = ref<ScheduledTransaction[]>([])

const enabledCount = computed(() => records.value.filter((r) => r.enabled === 1).length)
const disabledCount = computed(() => records.value.length - enabledCount.value)

async function loadData() {
  loading.value = true
  try {
    records.value = await scheduledApi.listScheduled()
  } finally {
    loading.value = false
  }
}

/* ---------- 弹窗 ---------- */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)

const form = reactive({
  id: undefined as number | undefined,
  cron: '0 0 9 1 * ?',
  type: AccountType.EXPENSE as AccountTypeValue,
  category: '',
  amount: undefined as number | undefined,
  remark: '',
  enabled: 1,
})

const rules: FormRules = {
  cron: [{ required: true, message: '请填写 Cron 表达式', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  amount: [{ required: true, message: '请填写金额', trigger: 'blur' }],
}

/* ---------- 可视化 Cron 选择器 ---------- */
/** 频率模式 */
type CronPreset = 'monthly' | 'weekly' | 'daily'
const cronPreset = ref<CronPreset>('monthly')
const cronDayOfMonth = ref(1)
const cronDayOfWeek = ref(1)
const cronHour = ref(9)
const cronMinute = ref(0)

function applyPresetToForm() {
  // 6 字段 Quartz Cron：秒 分 时 日 月 周
  if (cronPreset.value === 'monthly') {
    form.cron = `${cronMinute.value} ${cronHour.value} ${cronDayOfMonth.value} * ?`
  } else if (cronPreset.value === 'weekly') {
    form.cron = `${cronMinute.value} ${cronHour.value} ? * ${cronDayOfWeek.value}`
  } else {
    form.cron = `${cronMinute.value} ${cronHour.value} * * ?`
  }
}

function parseFormToPreset(cron: string) {
  // 简单解析已知格式
  const parts = cron.split(/\s+/).filter(Boolean)
  if (parts.length < 5) return
  const minute = Number(parts[0])
  const hour = Number(parts[1])
  if (!Number.isNaN(minute)) cronMinute.value = minute
  if (!Number.isNaN(hour)) cronHour.value = hour
  const dom = parts[2]
  const dow = parts[4]
  if (dom === '*') {
    if (dow === '?') cronPreset.value = 'daily'
    else {
      cronPreset.value = 'weekly'
      cronDayOfWeek.value = Number(dow) || 1
    }
  } else if (dom !== '?') {
    cronPreset.value = 'monthly'
    cronDayOfMonth.value = Number(dom) || 1
  }
}

function onPresetChange() {
  applyPresetToForm()
}

function onPresetValueChange() {
  applyPresetToForm()
}

/* ---------- 弹窗操作 ---------- */
function openCreate() {
  isEdit.value = false
  form.id = undefined
  form.cron = '0 0 9 1 * ?'
  form.type = AccountType.EXPENSE
  form.category = ''
  form.amount = undefined
  form.remark = ''
  form.enabled = 1
  parseFormToPreset(form.cron)
  dialogVisible.value = true
}

function openEdit(row: ScheduledTransaction) {
  isEdit.value = true
  form.id = row.id
  form.cron = row.cron
  form.type = (row.type === 1 ? AccountType.INCOME : AccountType.EXPENSE) as AccountTypeValue
  form.category = row.category
  form.amount = row.amount
  form.remark = row.remark ?? ''
  form.enabled = row.enabled ?? 1
  parseFormToPreset(row.cron)
  dialogVisible.value = true
}

const categories = computed(() => getCategoriesByType(form.type))

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.amount == null || form.amount <= 0) {
    ElMessage.warning('金额必须大于 0')
    return
  }
  saving.value = true
  try {
    const payload: ScheduledTransaction = {
      id: form.id,
      cron: form.cron,
      type: form.type,
      category: form.category,
      amount: form.amount,
      remark: form.remark || undefined,
      enabled: form.enabled,
    }
    if (isEdit.value && form.id) {
      await scheduledApi.updateScheduled(form.id, payload)
      ElMessage.success('规则已更新')
    } else {
      await scheduledApi.createScheduled(payload)
      ElMessage.success('规则已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

/* ---------- 行操作 ---------- */
async function toggleEnabled(row: ScheduledTransaction) {
  const target = row.enabled === 1 ? 0 : 1
  try {
    await scheduledApi.toggleScheduled(row.id!, target)
    row.enabled = target
    ElMessage.success(target === 1 ? '已启用' : '已停用')
  } catch {
    /* 拦截器处理错误提示 */
  }
}

async function execute(row: ScheduledTransaction) {
  try {
    await ElMessageBox.confirm(
      `确认立即执行「${row.category} ¥${row.amount}」规则？`,
      '立即执行',
      { confirmButtonText: '执行', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await scheduledApi.executeScheduled(row.id!)
    ElMessage.success('已触发执行')
  } catch {
    /* 拦截器处理 */
  }
}

async function handleDelete(row: ScheduledTransaction) {
  try {
    await ElMessageBox.confirm('确定删除该规则吗？', '删除规则', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await scheduledApi.deleteScheduled(row.id!)
  ElMessage.success('已删除')
  loadData()
}

/* ---------- 工具 ---------- */
function formatTime(t?: string): string {
  if (!t) return '—'
  return t.slice(0, 16).replace('T', ' ')
}

const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const daysOfMonth = Array.from({ length: 28 }, (_, i) => i + 1)
const hours = Array.from({ length: 24 }, (_, i) => i)
const minutes = [0, 15, 30, 45]

onMounted(loadData)
</script>

<template>
  <div class="scheduled-page">
    <!-- 标题 + 工具栏 -->
    <div class="toolbar">
      <h2 class="page-title">
        <el-icon><Calendar /></el-icon>
        定时交易管理
      </h2>
      <div class="actions">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建规则</el-button>
      </div>
    </div>

    <!-- 空态 -->
    <el-empty
      v-if="!loading && !records.length"
      description="暂无定时交易规则，点击「新建规则」开始"
    >
      <el-button type="primary" @click="openCreate">新建规则</el-button>
    </el-empty>

    <!-- 规则卡片列表 -->
    <div v-else v-loading="loading" class="rule-list">
      <el-card
        v-for="row in records"
        :key="row.id"
        shadow="never"
        class="rule-card"
        :class="{ disabled: row.enabled === 0 }"
      >
        <div class="rule-head">
          <span class="rule-title">
            <el-icon><Calendar /></el-icon>
            {{ row.category }}
          </span>
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
            {{ row.enabled === 1 ? '✅ 启用' : '⏸ 已停用' }}
          </el-tag>
        </div>
        <div class="rule-cron">Cron：{{ row.cron }}</div>
        <div class="rule-detail">
          <span>类型：{{ typeLabel(row.type) }}</span>
          <span>分类：{{ row.category }}</span>
          <span>金额：¥{{ formatMoney(row.amount) }}</span>
          <span v-if="row.remark">备注：{{ row.remark }}</span>
        </div>
        <div class="rule-meta">
          <span>下次执行：{{ formatTime(row.nextRunAt) }}</span>
          <span>创建时间：{{ formatTime(row.createdAt) }}</span>
        </div>
        <div class="rule-actions">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link size="small" @click="toggleEnabled(row)">
            {{ row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="success" size="small" @click="execute(row)">立即执行</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </div>
      </el-card>

      <!-- 统计 -->
      <div class="summary">
        共 {{ records.length }} 条规则 ｜ 启用 {{ enabledCount }} 条 ｜ 停用 {{ disabledCount }} 条
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑规则' : '新建定时规则'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        @submit.prevent
      >
        <el-form-item label="频率">
          <el-radio-group v-model="cronPreset" @change="onPresetChange">
            <el-radio value="monthly">每月</el-radio>
            <el-radio value="weekly">每周</el-radio>
            <el-radio value="daily">每天</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="cronPreset === 'monthly'" label="日">
          <el-select v-model="cronDayOfMonth" style="width: 120px" @change="onPresetValueChange">
            <el-option v-for="d in daysOfMonth" :key="d" :label="`${d}日`" :value="d" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="cronPreset === 'weekly'" label="星期">
          <el-select v-model="cronDayOfWeek" style="width: 120px" @change="onPresetValueChange">
            <el-option v-for="(n, i) in weekNames" :key="i" :label="n" :value="i" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间">
          <el-select v-model="cronHour" style="width: 100px" @change="onPresetValueChange">
            <el-option v-for="h in hours" :key="h" :label="`${h}时`" :value="h" />
          </el-select>
          <span class="time-sep">:</span>
          <el-select v-model="cronMinute" style="width: 100px" @change="onPresetValueChange">
            <el-option v-for="m in minutes" :key="m" :label="`${String(m).padStart(2, '0')}`" :value="m" />
          </el-select>
        </el-form-item>

        <el-form-item label="Cron">
          <el-input v-model="form.cron" placeholder="如：0 0 9 1 * ?" />
          <div class="cron-tip">如需更复杂表达式，可直接修改上方 Cron 文本</div>
        </el-form-item>

        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button :value="AccountType.EXPENSE">支出</el-radio-button>
            <el-radio-button :value="AccountType.INCOME">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="金额">
          <el-input-number
            v-model="form.amount"
            :precision="2"
            :min="0.01"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="备注（可选）" />
        </el-form-item>

        <el-form-item label="状态">
          <el-switch
            v-model="form.enabled"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.scheduled-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.actions {
  display: flex;
  gap: 10px;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rule-card {
  &.disabled {
    opacity: 0.65;
  }
}

.rule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.rule-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.rule-cron {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.rule-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 6px;
}

.rule-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.rule-actions {
  display: flex;
  gap: 4px;
  border-top: 1px dashed var(--el-border-color-light);
  padding-top: 8px;
}

.summary {
  text-align: center;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

.time-sep {
  margin: 0 6px;
  color: var(--el-text-color-secondary);
}
.cron-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
