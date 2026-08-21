<script setup lang="ts">
/**
 * 交易模板选择器（V2.1 新增）
 * 下拉列出模板，选择后 emit('select', TemplateVO)，由父组件填充表单。
 */
import { ref, onMounted } from 'vue'
import * as templateApi from '@/api/template'
import type { TemplateVO } from '@/types/api'

const props = defineProps<{
  /** 过滤收支类型 */
  type?: number
  placeholder?: string
}>()
const emit = defineEmits<{ select: [template: TemplateVO] }>()

const options = ref<TemplateVO[]>([])

onMounted(async () => {
  options.value = await templateApi.listTemplates({ type: props.type, size: 100 })
})

function pick(id: number) {
  const t = options.value.find((x) => x.id === id)
  if (t) emit('select', t)
}
</script>

<template>
  <el-select
    :placeholder="placeholder ?? '从模板选择'"
    style="width: 100%"
    @change="pick"
  >
    <el-option
      v-for="t in options"
      :key="t.id"
      :label="`${t.name}${t.amount ? ' ¥' + t.amount : ''}`"
      :value="t.id"
    />
  </el-select>
</template>
