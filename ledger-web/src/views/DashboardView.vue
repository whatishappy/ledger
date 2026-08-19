<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { Calendar, Money, TrendCharts, WalletFilled } from '@element-plus/icons-vue'
import type { ECharts } from 'echarts/core'
import * as statisticsApi from '@/api/statistics'
import type { CategoryStatVO, DashboardVO, TrendVO } from '@/types/api'
import { useAppStore } from '@/stores/app'
import { currentMonth } from '@/utils/date'
import { formatMoney } from '@/utils/format'

echarts.use([LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const CHART_PALETTE = ['#1E40AF', '#059669', '#3B82F6', '#EAB308', '#F59E0B', '#8B5CF6', '#DC2626']

const appStore = useAppStore()
const loading = ref(false)
const month = ref(currentMonth())
const dashboard = ref<DashboardVO | null>(null)

const trendRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
let trendChart: ECharts | null = null
let pieChart: ECharts | null = null

const isDark = computed(() => appStore.isDark)

/** 预算剩余：总预算 - 已消费 */
const budgetRemaining = computed(() => {
  if (!dashboard.value?.budgetProgress.length) return null
  return dashboard.value.budgetProgress.reduce((acc, b) => acc + (b.amountLimit - b.spent), 0)
})

const hasCategoryData = computed(() => (dashboard.value?.categoryStats.length ?? 0) > 0)
const hasTrendData = computed(() => (dashboard.value?.trend.length ?? 0) > 0)

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await statisticsApi.getDashboard(month.value)
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

/* ---------- 图表渲染 ---------- */
function textColors() {
  return {
    text: isDark.value ? '#CBD5E1' : '#475569',
    axis: isDark.value ? 'rgba(255,255,255,0.12)' : '#E2E8F0',
  }
}

function renderTrendChart(data: TrendVO[]) {
  const { text, axis } = textColors()
  trendChart?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 16, top: 30, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((d) => d.date.slice(5)),
      axisLine: { lineStyle: { color: axis } },
      axisLabel: { color: text },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: axis } },
      axisLabel: { color: text },
    },
    series: [
      {
        name: '支出',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: data.map((d) => d.expense),
        lineStyle: { color: '#1E40AF', width: 2 },
        itemStyle: { color: '#1E40AF' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(30, 64, 175, 0.16)' },
              { offset: 1, color: 'rgba(30, 64, 175, 0)' },
            ],
          },
        },
      },
    ],
  })
}

function renderPieChart(data: CategoryStatVO[]) {
  const { text, axis } = textColors()
  const palette = [...CHART_PALETTE]
  // 超过 6 个分类折叠进「其他」
  const slice = data.length > 6 ? data.slice(0, 5) : data
  const othersAmount = data.length > 6 ? data.slice(5).reduce((sum, d) => sum + d.amount, 0) : 0
  if (othersAmount > 0) {
    slice.push({ category: '其他', amount: othersAmount, percentage: 0 })
  }
  pieChart?.setOption({
    tooltip: { trigger: 'item', formatter: '{b}<br/>¥{c} ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'center',
      icon: 'circle',
      textStyle: { color: text },
    },
    series: [
      {
        name: '支出占比',
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: 'transparent', borderWidth: 2 },
        label: { color: text, formatter: '{b} {d}%' },
        labelLine: { lineStyle: { color: axis } },
        data: slice.map((s, i) => ({
          name: s.category,
          value: s.amount,
          itemStyle: { color: palette[i % palette.length] },
        })),
      },
    ],
  })
}

function renderCharts() {
  if (trendChart && hasTrendData.value) renderTrendChart(dashboard.value!.trend)
  if (pieChart && hasCategoryData.value) renderPieChart(dashboard.value!.categoryStats)
}

function initCharts() {
  if (trendRef.value) trendChart = echarts.init(trendRef.value)
  if (pieRef.value) pieChart = echarts.init(pieRef.value)
}

function resizeCharts() {
  trendChart?.resize()
  pieChart?.resize()
}

watch(month, loadDashboard)
watch(isDark, () => {
  // 主题切换后重绘（颜色随主题）
  if (dashboard.value) {
    renderCharts()
  }
})

onMounted(async () => {
  initCharts()
  window.addEventListener('resize', resizeCharts)
  await loadDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 月份选择 + 标题 -->
    <div class="dash-toolbar">
      <h2 class="page-title">仪表盘</h2>
      <el-date-picker
        v-model="month"
        type="month"
        value-format="YYYY-MM"
        :clearable="false"
        format="YYYY年MM月"
        placeholder="选择月份"
      />
    </div>

    <!-- 顶部 4 卡片 -->
    <div class="stat-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-icon income">
          <el-icon :size="22"><Money /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">本月收入</div>
          <div class="stat-value money text-income">
            {{ formatMoney(dashboard?.monthIncome) }}
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="stat-card">
        <div class="stat-icon expense">
          <el-icon :size="22"><Money /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">本月支出</div>
          <div class="stat-value money text-expense">
            {{ formatMoney(dashboard?.monthExpense) }}
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="stat-card">
        <div class="stat-icon balance">
          <el-icon :size="22"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">本月结余</div>
          <div class="stat-value money" :class="(dashboard?.balance ?? 0) >= 0 ? 'text-income' : 'text-expense'">
            {{ formatMoney(dashboard?.balance) }}
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="stat-card">
        <div class="stat-icon budget">
          <el-icon :size="22"><WalletFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">预算剩余</div>
          <div v-if="budgetRemaining !== null" class="stat-value money">
            {{ formatMoney(budgetRemaining) }}
          </div>
          <div v-else class="stat-empty">未设预算</div>
        </div>
      </el-card>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <el-icon><Calendar /></el-icon>
            <span>近 7 日支出趋势</span>
          </div>
        </template>
        <div v-if="hasTrendData" ref="trendRef" class="chart-box" />
        <el-empty v-else description="该月份暂无支出数据" :image-size="80" />
      </el-card>

      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="card-head">
            <el-icon><TrendCharts /></el-icon>
            <span>分类支出占比</span>
          </div>
        </template>
        <div v-if="hasCategoryData" ref="pieRef" class="chart-box" />
        <el-empty v-else description="该月份暂无支出分类数据" :image-size="80" />
      </el-card>
    </div>

    <!-- 预算进度列表 -->
    <el-card shadow="never" class="budget-card">
      <template #header>
        <div class="card-head">
          <el-icon><WalletFilled /></el-icon>
          <span>预算进度</span>
        </div>
      </template>
      <div v-if="dashboard?.budgetProgress.length" class="budget-list">
        <div v-for="item in dashboard.budgetProgress" :key="item.id" class="budget-item">
          <div class="budget-meta">
            <span class="budget-category">{{ item.category }}</span>
            <span class="budget-amount money">
              {{ formatMoney(item.spent) }} / {{ formatMoney(item.amountLimit) }}
              <el-tag v-if="item.isOverBudget" type="danger" size="small" effect="dark">超支</el-tag>
            </span>
          </div>
          <el-progress
            :percentage="Math.min(item.progress, 100)"
            :stroke-width="10"
            :show-text="true"
            :format="() => `${item.progress.toFixed(1)}%`"
            :color="item.progress >= 100 ? 'var(--color-budget-over)' : item.progress >= 90 ? 'var(--color-budget-warn)' : 'var(--color-budget-normal)'"
          />
        </div>
      </div>
      <el-empty v-else description="本月尚未设置预算" :image-size="80">
        <el-button type="primary" @click="$router.push('/budget')">去设置预算</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dash-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  border-radius: 12px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  color: #fff;
  flex-shrink: 0;
}
.stat-icon.income {
  background: var(--color-income);
}
.stat-icon.expense {
  background: var(--color-expense);
}
.stat-icon.balance {
  background: linear-gradient(135deg, #1e40af, #3b82f6);
}
.stat-icon.budget {
  background: linear-gradient(135deg, #f59e0b, #eab308);
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  margin-top: 2px;
}
.stat-empty {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card :deep(.el-card__body) {
  padding: 12px;
}

.chart-box {
  height: 300px;
  width: 100%;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.budget-card :deep(.el-card__body) {
  padding: 20px 24px;
}

.budget-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.budget-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.budget-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.budget-category {
  font-weight: 600;
}
.budget-amount {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-regular);
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
