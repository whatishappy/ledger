<script setup lang="ts">
/**
 * 标签选择器（V2.1 新增）
 * 多选已有标签，v-model 为 tagId 数组；选项带颜色圆点。
 * 用于 AccountEditorDialog 标签字段、账目列表按标签筛选。
 */
import { ref, watch } from 'vue'
import * as tagApi from '@/api/tag'
import type { TagVO } from '@/types/api'

const modelValue = defineModel<number[]>({ default: () => [] })
const props = defineProps<{
  /** 仅展示该类型标签：0全部 1支出 2收入 */
  type?: number
  placeholder?: string
}>()

const options = ref<TagVO[]>([])
const loading = ref(false)

async function loadTags() {
  loading.value = true
  try {
    options.value = await tagApi.listTags({ type: props.type })
  } finally {
    loading.value = false
  }
}

watch(() => props.type, loadTags, { immediate: true })
</script>

<template>
  <el-select
    v-model="modelValue"
    multiple
    collapse-tags
    collapse-tags-tooltip
    :placeholder="placeholder ?? '选择标签'"
    :loading="loading"
    style="width: 100%"
  >
    <el-option v-for="t in options" :key="t.id" :label="t.name" :value="t.id">
      <span class="dot" :style="{ background: t.color }" />
      <span>{{ t.name }}</span>
    </el-option>
  </el-select>
</template>

<style scoped>
.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
</style>
