<script setup lang="ts">
/**
 * AI 消息气泡（V2.1 新增）
 * 用户消息纯文本；助手消息走 Markdown 渲染，并解析 [TOOL_CALL_BEGIN/END]
 * 围栏标记拆分为文本段与 Tool 卡片。
 */
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'
import AiToolCard from '@/components/AiToolCard.vue'
import type { AiChatMessageVO } from '@/types/api'

const props = defineProps<{ message: AiChatMessageVO; streaming?: boolean }>()

const TOOL_RE = /\[TOOL_CALL_BEGIN\]\n([^\n]*)\n([\s\S]*?)\n\[TOOL_CALL_END\]/g

interface Segment {
  type: 'text' | 'tool'
  html?: string
  tool?: string
  result?: unknown
}

const segments = computed<Segment[]>(() => {
  const content = props.message.content || ''
  if (props.message.role !== 'assistant') {
    return [{ type: 'text', html: content }]
  }
  const list: Segment[] = []
  let last = 0
  let m: RegExpExecArray | null
  TOOL_RE.lastIndex = 0
  while ((m = TOOL_RE.exec(content)) !== null) {
    if (m.index > last) {
      const text = content.slice(last, m.index).trim()
      if (text) list.push({ type: 'text', html: renderMarkdown(text) })
    }
    const tool = m[1]
    let result: unknown = m[2]
    try {
      result = JSON.parse(m[2])
    } catch {
      // 保留原始字符串
    }
    list.push({ type: 'tool', tool, result })
    last = m.index + m[0].length
  }
  if (last < content.length) {
    const text = content.slice(last).trim()
    if (text) list.push({ type: 'text', html: renderMarkdown(text) })
  }
  return list
})

const isUser = computed(() => props.message.role === 'user')
const showCursor = computed(() => props.streaming && !props.message.content)
</script>

<template>
  <div class="bubble" :class="isUser ? 'user' : 'assistant'">
    <div v-if="isUser" class="text">{{ message.content }}</div>
    <template v-else>
      <span v-if="showCursor" class="cursor">AI 思考中…</span>
      <template v-for="(seg, i) in segments" :key="i">
        <div v-if="seg.type === 'text'" class="md" v-html="seg.html" />
        <AiToolCard v-else :tool="seg.tool ?? ''" :result="seg.result" />
      </template>
    </template>
  </div>
</template>

<style scoped>
.bubble {
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.user {
  background: var(--el-color-primary);
  color: #fff;
  align-self: flex-end;
}
.assistant {
  background: var(--el-fill-color-light);
  align-self: flex-start;
}
.text {
  white-space: pre-wrap;
}
.md :deep(p) {
  margin: 4px 0;
}
.md :deep(ul),
.md :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}
.md :deep(table) {
  border-collapse: collapse;
  margin: 6px 0;
}
.md :deep(th),
.md :deep(td) {
  border: 1px solid var(--el-border-color);
  padding: 4px 8px;
}
.cursor {
  color: var(--el-text-color-secondary);
}
</style>
