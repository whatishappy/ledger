<script setup lang="ts">
/** 快捷面额块：点击将金额设为对应快捷值 */
const QUICK_AMOUNTS = [10, 20, 50, 100, 500] as const
const amount = defineModel<number>({ default: 0 })

function pick(value: number) {
  amount.value = value
}
</script>

<template>
  <div class="quick-pad">
    <button
      v-for="v in QUICK_AMOUNTS"
      :key="v"
      type="button"
      class="quick-btn"
      :class="{ active: amount === v }"
      @click="pick(v)"
    >
      ¥{{ v }}
    </button>
  </div>
</template>

<style scoped>
.quick-pad {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.quick-btn {
  padding: 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.quick-btn:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.quick-btn.active {
  color: #fff;
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
</style>
