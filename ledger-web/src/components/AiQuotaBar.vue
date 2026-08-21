<script setup lang="ts">
/**
 * AI 配额进度条（V2.1 新增）
 * 展示今日对话次数与 Token 用量，颜色随百分比动态变化。
 */
import { computed } from 'vue'
import type { AiQuotaVO } from '@/types/api'

const props = defineProps<{ quota: AiQuotaVO | null }>()

const chatPercent = computed(() => props.quota?.chatPercent ?? 0)
const tokenPercent = computed(() => props.quota?.tokenPercent ?? 0)

function colorOf(percent: number): string {
  if (percent >= 90) return '#f56c6c'
  if (percent >= 60) return '#e6a23c'
  return '#67c23a'
}
</script>

<template>
  <div class="quota-bar">
    <div class="quota-row">
      <span class="label">今日对话</span>
      <el-progress :percentage="Math.min(chatPercent, 100)" :color="colorOf(chatPercent)" :stroke-width="8" />
      <span class="num">{{ quota?.chatUsed ?? 0 }}/{{ quota?.chatTotal ?? 0 }}</span>
    </div>
    <div class="quota-row">
      <span class="label">Token</span>
      <el-progress :percentage="Math.min(tokenPercent, 100)" :color="colorOf(tokenPercent)" :stroke-width="8" />
      <span class="num">{{ quota?.tokenUsed ?? 0 }}/{{ quota?.tokenTotal ?? 0 }}</span>
    </div>
  </div>
</template>

<style scoped>
.quota-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.quota-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  width: 56px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}
.el-progress {
  flex: 1;
}
.num {
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-secondary);
  width: 88px;
  text-align: right;
  flex-shrink: 0;
}
</style>
