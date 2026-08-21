<script setup lang="ts">
/**
 * AI 对话抽屉（V2.1 新增，核心组件）
 * 全局 Drawer，挂载于 BasicLayout。会话列表 + 消息区（SSE 流式 Markdown）
 * + 配额条 + 输入框 + OCR 预览。可全屏展开跳 /ai/chat。
 */
import { ref, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAiStore } from '@/stores/ai'
import AiSessionList from '@/components/AiSessionList.vue'
import AiMessageBubble from '@/components/AiMessageBubble.vue'
import AiQuotaBar from '@/components/AiQuotaBar.vue'
import AiInputBox from '@/components/AiInputBox.vue'
import OcrPreviewCard from '@/components/OcrPreviewCard.vue'
import type { AiOcrResult } from '@/types/api'

const aiStore = useAiStore()
const router = useRouter()
const ocrResult = ref<AiOcrResult | null>(null)
const msgScroll = ref<HTMLDivElement | null>(null)

// 抽屉打开时加载会话与配额
watch(
  () => aiStore.drawerVisible,
  async (v) => {
    if (v) {
      await Promise.all([aiStore.loadSessions(), aiStore.refreshQuota()].map((p) => p.catch(() => {})))
    }
  },
)

// 消息变化时滚动到底部
watch(
  () => aiStore.messages.map((m) => m.content).join(''),
  async () => {
    await nextTick()
    if (msgScroll.value) msgScroll.value.scrollTop = msgScroll.value.scrollHeight
  },
)

function onSend(text: string) {
  ocrResult.value = null
  aiStore.sendMessage(text)
}

async function onUpload(file: File) {
  try {
    ocrResult.value = await aiStore.uploadReceipt(file)
  } catch {
    // 拦截器已提示
  }
}

function goFullscreen() {
  aiStore.closeDrawer()
  router.push({ path: '/ai/chat', query: aiStore.currentSessionId ? { sessionId: String(aiStore.currentSessionId) } : {} })
}
</script>

<template>
  <el-drawer
    v-model="aiStore.drawerVisible"
    title="AI 助手"
    direction="rtl"
    size="600px"
    :with-header="true"
  >
    <template #header>
      <div class="head">
        <span>AI 助手</span>
        <el-button text size="small" @click="goFullscreen">展开全屏</el-button>
      </div>
    </template>
    <div class="panel">
      <aside class="aside">
        <AiSessionList />
      </aside>
      <section class="main">
        <div ref="msgScroll" class="messages">
          <AiMessageBubble
            v-for="m in aiStore.messages"
            :key="m.id"
            :message="m"
            :streaming="aiStore.isStreaming && m.id === aiStore.messages[aiStore.messages.length - 1]?.id"
          />
          <el-empty v-if="!aiStore.messages.length" description="开始与 AI 对话吧" :image-size="80" />
        </div>
        <OcrPreviewCard
          v-if="ocrResult"
          :result="ocrResult"
          @booked="ocrResult = null"
          @discard="ocrResult = null"
        />
        <AiQuotaBar :quota="aiStore.quota" />
        <AiInputBox :disabled="aiStore.isStreaming" @send="onSend" @upload="onUpload" />
        <el-button v-if="aiStore.isStreaming" text size="small" class="stop" @click="aiStore.stopStreaming">停止生成</el-button>
      </section>
    </div>
  </el-drawer>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.panel {
  display: flex;
  height: 100%;
}
.aside {
  width: 200px;
  border-right: 1px solid var(--el-border-color-lighter);
  padding: 8px;
  flex-shrink: 0;
}
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.stop {
  align-self: center;
  margin: 4px 0;
}
:deep(.el-drawer__body) {
  padding: 0;
}
</style>
