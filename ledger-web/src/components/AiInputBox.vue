<script setup lang="ts">
/**
 * AI 输入框（V2.1 新增）
 * 多行文本 + 图片上传按钮。Ctrl+Enter 或点击发送按钮发送。
 * 图片上传 emit('upload', file)，由父组件触发 OCR 预览。
 */
import { ref } from 'vue'

const props = defineProps<{ disabled?: boolean }>()
const emit = defineEmits<{ send: [text: string]; upload: [file: File] }>()

const text = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

function send() {
  const t = text.value.trim()
  if (!t || props.disabled) return
  emit('send', t)
  text.value = ''
}

function onKeydown(e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 'Enter') {
    e.preventDefault()
    send()
  }
}

function pickFile() {
  fileInput.value?.click()
}

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (f) emit('upload', f)
  if (fileInput.value) fileInput.value.value = ''
}
</script>

<template>
  <div class="input-box">
    <el-input
      v-model="text"
      type="textarea"
      :rows="2"
      :disabled="disabled"
      placeholder="输入消息，Ctrl+Enter 发送"
      resize="none"
      @keydown="onKeydown"
    />
    <div class="bar">
      <el-button :icon="null" :disabled="disabled" @click="pickFile">📷</el-button>
      <el-button type="primary" :disabled="disabled || !text.trim()" :loading="disabled" @click="send">
        发送
      </el-button>
    </div>
    <input ref="fileInput" type="file" accept="image/png,image/jpeg" hidden @change="onFileChange" />
  </div>
</template>

<style scoped>
.input-box {
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 8px 10px;
}
.bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}
</style>
