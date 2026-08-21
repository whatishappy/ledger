<script setup lang="ts">
/**
 * 日历热力图页 — /calendar（V2.1 新增，CA-01）
 * 基于月度每日消费强度，使用 ECharts heatmap + visualMap 渲染色阶。
 * 点击日期跳转 /account?date=YYYY-MM-DD 查看当日明细。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { HeatmapChart } from 'echarts/charts'
import {
  CalendarComponent,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { Calendar as CalendarIcon } from '@element-plus/icons-vue'
import type { ECharts } from 'echarts/core'
import * as calendarApi from '@/api/calendar'
import type { CalendarDayVO, CalendarHeatmapVO } from '@/types/api'
import { useAppStore } from '@/stores/app'
import { currentMonth } from '@/utils/date'
import { formatMoney } from '@/utils/format'

echarts.use([
  HeatmapChart,
  CalendarComponent,
  GridComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer,
])

const router = useRouter()
const appStore = useAppStore()

const loading = ref(false)
const loadError = ref(false)
const month = ref(currentMonth())
const heatmap = ref<CalendarHeatmapVO | null>(null)

const heatRef = ref<HTMLDivElement>()
let heatChart: ECharts | null = null

const isDark = computed(() => appStore.isDark)

/** 最贵日（支出最高的日期） */
const topDay = computed<CalendarDayVO | null>(() => {
  if (!heatmap.value?.days.length) return null
  return [...heatmap.value.days].sort((a, b) => b.expense - a.expense)[0]
})

/** 日均支出（按本月有支出日平均，含 0 元日） */
const dailyAvg = computed(() => {
  const d = heatmap.value
  if (!d?.days.length) return 0
  // 按当月日历天数（含 0 元）平均，更真实反映日均
  return d.totalExpense / d.days.length
})

async function loadHeatmap() {
  loading.value = true
  loadError.value = false
  try {
    heatmap.value = await calendarApi.getCalendarHeatmap(month.value)
    await nextTick()
    renderChart()
  } catch {
    loadError.value = true
    heatmap.value = null
  } finally {
    loading.value = false
  }
}

/* ---------- 图表渲染 ---------- */
function textColors() {
  return {
    text: isDark.value ? '#CBD5E1' : '#475569',
    axis: isDark.value ? 'rgba(255,255,255,0.12)' : '#E2E8F0',
    split: isDark.value ? 'rgba(255,255,255,0.06)' : '#F1F5F9',
  }
}

/** 将日数据转换为 ECharts heatmap 需要的 [date, value] 数据 */
function buildHeatData(days: CalendarDayVO[]) {
  return days.map((d) => [d.date, d.expense])
}

function renderChart() {
  if (!heatChart || !heatmap.value) return
  const { text, split } = textColors()
  const yearMonth = heatmap.value.month
  const [y, m] = yearMonth.split('-').map(Number)
  // 当月起止
  const start = `${yearMonth}-01`
  const lastDay = new Date(y, m, 0).getDate()
  const end = `${yearMonth}-${String(lastDay).padStart(2, '0')}`

  heatChart.setOption({
    tooltip: {
      formatter: (params: unknown) => {
        const value = (params as { value?: unknown[] })?.value
        if (!Array.isArray(value) || value.length < 2) return ''
        const date = String(value[0])
        const val = Number(value[1] ?? 0)
        const day = heatmap.value?.days.find((d) => d.date === date)
        const count = day?.count ?? 0
        const income = day?.income ?? 0
        return `${date}<br/>支出：¥${formatMoney(val)}<br/>收入：¥${formatMoney(income)}<br/>笔数：${count}`
      },
    },
    visualMap: {
      min: 0,
      max: Math.max(heatmap.value.maxDailyExpense, 200),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 8,
      textStyle: { color: text },
      inRange: {
        // 5 档色阶：0 元浅灰 → >200 元深红
        color: ['#F1F5F9', '#FECACA', '#FCA5A5', '#F87171', '#DC2626'],
      },
    },
    calendar: {
      top: 30,
      left: 30,
      right: 30,
      bottom: 60,
      range: [start, end],
      cellSize: ['auto', 56],
      orient: 'horizontal',
      dayLabel: {
        firstDay: 1,
        color: text,
        nameMap: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
      },
      monthLabel: {
        color: text,
        margin: 12,
        formatter: `${m}月`,
      },
      yearLabel: { show: false },
      splitLine: { lineStyle: { color: split } },
      itemStyle: {
        color: isDark.value ? 'rgba(255,255,255,0.04)' : '#FAFAFA',
        borderColor: split,
        borderWidth: 1,
      },
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: buildHeatData(heatmap.value.days),
        emphasis: { itemStyle: { borderColor: '#1E40AF', borderWidth: 2 } },
      },
    ],
  })
}

function initChart() {
  if (heatRef.value) heatChart = echarts.init(heatRef.value)
  heatChart?.on('click', (params) => {
    const value = (params as { value?: unknown[] })?.value
    if (!Array.isArray(value) || !value[0]) return
    const date = String(value[0])
    router.push({ path: '/account', query: { date } })
  })
}

function resizeChart() {
  heatChart?.resize()
}

watch(month, loadHeatmap)
watch(isDark, () => {
  if (heatmap.value) renderChart()
})

onMounted(async () => {
  initChart()
  window.addEventListener('resize', resizeChart)
  await loadHeatmap()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  heatChart?.dispose()
})
</script>

<template>
  <div class="calendar-page" v-loading="loading">
    <!-- 月份选择 + 标题 -->
    <div class="toolbar">
      <h2 class="page-title">
        <el-icon><CalendarIcon /></el-icon>
        日历热力图
      </h2>
      <el-date-picker
        v-model="month"
        type="month"
        value-format="YYYY-MM"
        :clearable="false"
        format="YYYY年MM月"
        placeholder="选择月份"
      />
    </div>

    <!-- 错误占位 -->
    <el-result
      v-if="loadError"
      icon="warning"
      title="日历数据加载失败"
      sub-title="请检查后端服务后重试"
    >
      <template #extra>
        <el-button type="primary" @click="loadHeatmap">重新加载</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- 热力图卡片 -->
      <el-card shadow="never" class="heat-card">
        <div ref="heatRef" class="heat-chart" />
      </el-card>

      <!-- 色阶图例 -->
      <div class="legend">
        <span class="legend-label">色阶：</span>
        <span class="legend-item"><i class="dot lv0" />0 元</span>
        <span class="legend-item"><i class="dot lv1" />&lt;50</span>
        <span class="legend-item"><i class="dot lv2" />50-100</span>
        <span class="legend-item"><i class="dot lv3" />100-200</span>
        <span class="legend-item"><i class="dot lv4" />&gt;200</span>
      </div>

      <!-- 月度统计 -->
      <div class="stat-row">
        <el-card shadow="never" class="stat-box">
          <div class="stat-label">本月总支出</div>
          <div class="stat-value text-expense">¥ {{ formatMoney(heatmap?.totalExpense) }}</div>
        </el-card>
        <el-card shadow="never" class="stat-box">
          <div class="stat-label">本月总收入</div>
          <div class="stat-value text-income">¥ {{ formatMoney(heatmap?.totalIncome) }}</div>
        </el-card>
        <el-card shadow="never" class="stat-box">
          <div class="stat-label">日均支出</div>
          <div class="stat-value">¥ {{ formatMoney(dailyAvg) }}</div>
        </el-card>
        <el-card shadow="never" class="stat-box">
          <div class="stat-label">最贵日</div>
          <div v-if="topDay" class="stat-value text-expense">
            {{ topDay.date.slice(5) }} ¥ {{ formatMoney(topDay.expense) }}
          </div>
          <div v-else class="stat-empty">无</div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.calendar-page {
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

.heat-card {
  padding: 8px 4px;
}

.heat-chart {
  width: 100%;
  height: 320px;
}

.legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 0 4px;
}
.legend-label {
  font-weight: 600;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}
.dot.lv0 { background: #F1F5F9; }
.dot.lv1 { background: #FECACA; }
.dot.lv2 { background: #FCA5A5; }
.dot.lv3 { background: #F87171; }
.dot.lv4 { background: #DC2626; }

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-box {
  text-align: center;
}
.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.stat-empty {
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}

.text-expense { color: var(--el-color-danger); }
.text-income { color: var(--el-color-success); }

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .heat-chart {
    height: 260px;
  }
}
</style>
