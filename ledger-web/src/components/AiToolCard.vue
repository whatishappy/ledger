<script setup lang="ts">
/**
 * AI Tool 调用结果卡片（V2.1 新增）
 * 展示 AI 调用工具的名称与返回结果，增强用户对 AI 行为的信任感。
 */
import { computed } from 'vue'

const props = defineProps<{
  tool: string
  result?: unknown
  error?: string
}>()

const success = computed(() => !props.error)
const resultText = computed(() => {
  if (props.error) return props.error
  if (props.result == null) return ''
  try {
    return typeof props.result === 'string' ? props.result : JSON.stringify(props.result, null, 2)
  } catch {
    return String(props.result)
  }
})
</script>

<template>
  <div class="tool-card" :class="{ fail: !success }">
    <div class="tool-head">
      <el-tag :type="success ? 'success' : 'danger'" size="small">
        {{ success ? '✅' : '❌' }} {{ tool }}
      </el-tag>
    </div>
    <pre v-if="resultText" class="tool-result">{{ resultText }}</pre>
  </div>
</template>

<style scoped>
.tool-card {
  margin: 6px 0;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  border-left: 3px solid var(--el-color-primary);
  border-radius: 4px;
}
.tool-card.fail {
  border-left-color: var(--el-color-danger);
}
.tool-result {
  margin: 6px 0 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 180px;
  overflow: auto;
  color: var(--el-text-color-regular);
}
</style>
