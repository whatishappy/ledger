<script setup lang="ts">
import { computed } from 'vue'
import { getCategoriesByType, type AccountTypeValue } from '@/types/business'
import { getCategoryIcon } from '@/utils/format'

const props = defineProps<{ type: AccountTypeValue }>()
const modelValue = defineModel<string>({ default: '' })

const categories = computed(() => getCategoriesByType(props.type))

function select(category: string) {
  modelValue.value = category
}
</script>

<template>
  <div class="cat-grid">
    <button
      v-for="cat in categories"
      :key="cat"
      type="button"
      class="cat-item"
      :class="{ active: modelValue === cat }"
      :aria-pressed="modelValue === cat"
      @click="select(cat)"
    >
      <el-icon :size="22"><component :is="getCategoryIcon(cat)" /></el-icon>
      <span class="cat-name">{{ cat }}</span>
    </button>
  </div>
</template>

<style scoped>
.cat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 4px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  cursor: pointer;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.cat-item:hover {
  border-color: var(--el-color-primary-light-5);
}

.cat-item.active {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.cat-name {
  font-size: 13px;
}
</style>
