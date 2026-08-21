<script setup lang="ts">
/**
 * AI 会话列表侧栏（V2.1 新增）
 * 会话列表 + 新建 + 删除 + 切换。直接操作 useAiStore。
 */
import { ElMessageBox } from 'element-plus'
import { useAiStore } from '@/stores/ai'

const aiStore = useAiStore()

async function remove(id: number) {
  try {
    await ElMessageBox.confirm('删除该会话？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await aiStore.deleteSession(id)
}
</script>

<template>
  <div class="session-list">
    <el-button class="new-btn" type="primary" plain @click="aiStore.newSession()">+ 新建会话</el-button>
    <div class="list">
      <div
        v-for="s in aiStore.sessions"
        :key="s.id"
        class="item"
        :class="{ active: aiStore.currentSessionId === s.id }"
        @click="aiStore.selectSession(s.id)"
      >
        <div class="info">
          <div class="title">{{ s.title }}</div>
          <div class="meta">{{ s.messageCount ?? 0 }} 条 · {{ s.lastMessageAt ?? s.createdAt }}</div>
        </div>
        <el-button class="del" text size="small" @click.stop="remove(s.id)">🗑</el-button>
      </div>
      <el-empty v-if="!aiStore.sessions.length" description="暂无会话" :image-size="60" />
    </div>
  </div>
</template>

<style scoped>
.session-list {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.new-btn {
  margin-bottom: 8px;
}
.list {
  flex: 1;
  overflow-y: auto;
}
.item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
}
.item:hover {
  background: var(--el-fill-color-light);
}
.item.active {
  background: var(--el-color-primary-light-9);
}
.info {
  flex: 1;
  min-width: 0;
}
.title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.del {
  flex-shrink: 0;
}
</style>
