<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, Plus, Wallet } from '@element-plus/icons-vue'
import * as budgetApi from '@/api/budget'
import type { BudgetVO } from '@/types/api'
import { AccountType, getCategoriesByType } from '@/types/business'
import { getCategoryIcon, formatMoney } from '@/utils/format'
import { currentMonth, isFutureMonth } from '@/utils/date'

const loading = ref(false)
const month = ref(currentMonth())
const budgets = ref<BudgetVO[]>([])

const dialogVisible = ref(false)
const saving = ref(false)

const EXPENSE_CATEGORIES = getCategoriesByType(AccountType.EXPENSE)

async function loadBudgets() {
  loading.value = true
  try {
    budgets.value = await budgetApi.listBudgets(month.value)
  } finally {
    loading.value = false
  }
}

/* ---------- 进度条颜色：<60% 绿 / <90% 黄 / ≥100% 红 ---------- */
function progressColor(progress: number): string {
  if (progress >= 100) return 'var(--color-budget-over)'
  if (progress >= 60) return 'var(--color-budget-warn)'
  return 'var(--color-budget-normal)'
}

function existingCategorySet(): Set<string> {
  return new Set(budgets.value.map((b) => b.category))
}

/* ---------- 设预算 Dialog ---------- */
const draft = reactive<Record<string, number | undefined>>({})

function openDialog() {
  // 预填当月已有预算金额
  EXPENSE_CATEGORIES.forEach((cat) => {
    const existing = budgets.value.find((b) => b.category === cat)
    draft[cat] = existing ? Number(existing.amountLimit) : undefined
  })
  dialogVisible.value = true
}

async function saveBudgets() {
  const existing = existingCategorySet()
  const toSubmit = EXPENSE_CATEGORIES.filter((cat) => {
    const amount = draft[cat]
    return !existing.has(cat) && amount !== undefined && amount > 0
  })
  if (toSubmit.length === 0) {
    ElMessage.info('没有需要新增的预算分类')
    return
  }
  saving.value = true
  try {
    // 后端为单条设定，重复设定返回 3001；这里仅提交未设置过的分类
    for (const cat of toSubmit) {
      await budgetApi.addBudget({ category: cat, month: month.value, amountLimit: draft[cat]! })
    }
    ElMessage.success(`已设置 ${toSubmit.length} 个分类的预算`)
    dialogVisible.value = false
    loadBudgets()
  } catch {
    /* 拦截器已提示（如 3001 / 频率限制） */
  } finally {
    saving.value = false
  }
}

function disabledFuture(date: Date): boolean {
  const ym = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  return isFutureMonth(ym)
}

watch(month, loadBudgets)
onMounted(loadBudgets)
</script>

<template>
  <div class="budget-page" v-loading="loading">
    <!-- 顶栏 -->
    <div class="budget-toolbar">
      <h2 class="page-title">预算管理</h2>
      <div class="toolbar-right">
        <el-date-picker
          v-model="month"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          format="YYYY年MM月"
          :disabled-date="disabledFuture"
          placeholder="选择月份"
        />
        <el-button type="primary" :icon="Plus" @click="openDialog">设预算</el-button>
      </div>
    </div>

    <!-- 预算卡片网格 -->
    <div v-if="budgets.length" class="budget-grid">
      <div
        v-for="item in budgets"
        :key="item.id"
        class="budget-card"
        :class="{ over: item.isOverBudget }"
      >
        <div class="card-head">
          <div class="cat-info">
            <span class="cat-icon">
              <el-icon :size="20"><component :is="getCategoryIcon(item.category)" /></el-icon>
            </span>
            <span class="cat-name">{{ item.category }}</span>
          </div>
          <el-tag v-if="item.isOverBudget" type="danger" size="small" effect="dark">超支</el-tag>
          <el-tag v-else-if="item.progress >= 60" type="warning" size="small" effect="plain">预警</el-tag>
        </div>

        <el-progress
          :percentage="Math.min(item.progress, 100)"
          :stroke-width="10"
          :show-text="false"
          :color="progressColor(item.progress)"
          class="progress"
        />

        <div class="card-foot">
          <span class="spent money">{{ formatMoney(item.spent) }}</span>
          <span class="limit">/ {{ formatMoney(item.amountLimit) }}</span>
          <span class="percent" :style="{ color: progressColor(item.progress) }">
            {{ item.progress.toFixed(1) }}%
          </span>
        </div>
      </div>
    </div>

    <!-- 空态 -->
    <el-empty v-else description="该月份还没有设置预算" :image-size="120">
      <el-button type="primary" :icon="Wallet" @click="openDialog">立即设置预算</el-button>
    </el-empty>

    <!-- 设预算 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="设置月度预算"
      width="min(480px, 92vw)"
      :close-on-click-modal="false"
    >
      <p class="dialog-month">预算月份：{{ month }}</p>
      <div class="budget-form">
        <div v-for="cat in EXPENSE_CATEGORIES" :key="cat" class="budget-row">
          <div class="row-cat">
            <el-icon :size="18"><component :is="getCategoryIcon(cat)" /></el-icon>
            <span>{{ cat }}</span>
          </div>
          <el-input-number
            v-model="draft[cat]"
            :precision="2"
            :min="0.01"
            :max="99999999.99"
            :controls="false"
            :placeholder="'0.00'"
            :disabled="existingCategorySet().has(cat)"
            style="width: 160px"
          />
          <el-tag v-if="existingCategorySet().has(cat)" type="info" size="small" effect="plain">
            已设置
          </el-tag>
        </div>
      </div>
      <p class="dialog-tip">
        <el-icon><InfoFilled /></el-icon>
        已设置的分类本月不可调整；填写未设置分类的预算后点击保存即可生效。
      </p>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBudgets">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.budget-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.budget-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}
.toolbar-right {
  display: flex;
  gap: 12px;
}

.budget-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.budget-card {
  padding: 20px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-left: 4px solid var(--color-budget-normal);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s ease;
}

.budget-card:hover {
  box-shadow: var(--shadow-md);
}

.budget-card.over {
  border-left-color: var(--color-budget-over);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.cat-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.cat-name {
  font-weight: 600;
  font-size: 15px;
}

.progress {
  margin-bottom: 10px;
}

.card-foot {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.spent {
  font-size: 20px;
  font-weight: 700;
}
.limit {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.percent {
  margin-left: auto;
  font-size: 14px;
  font-weight: 600;
}

.dialog-month {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.budget-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.budget-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.budget-row:last-child {
  border-bottom: none;
}
.row-cat {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  color: var(--el-text-color-regular);
}

.dialog-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
