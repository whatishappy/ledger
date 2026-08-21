<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Download, Plus, Refresh, Search } from '@element-plus/icons-vue'
import * as accountApi from '@/api/account'
import type { AccountVO } from '@/types/api'
import { AccountType, ALL_CATEGORIES, type AccountTypeValue } from '@/types/business'
import { getCategoryIcon, formatSignedMoney, typeLabel } from '@/utils/format'
import AccountEditorDialog from '@/components/AccountEditorDialog.vue'
import ExportDialog from '@/components/ExportDialog.vue'

const route = useRoute()
const loading = ref(false)
const records = ref<AccountVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  type: undefined as AccountTypeValue | undefined,
  category: '' as string,
  keyword: '',
})
const dateRange = ref<[string, string] | []>([])
const advanced = ref(false)

async function loadData() {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value.length === 2 ? dateRange.value : []
    const page = await accountApi.pageAccounts({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      type: query.type,
      category: query.category || undefined,
      startDate,
      endDate,
      keyword: query.keyword.trim() || undefined,
    })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

/** 支持从日历热力图等页面带 ?date=YYYY-MM-DD 跳转，自动设置单日筛选 */
function applyDateQuery() {
  const date = route.query.date
  if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
    dateRange.value = [date, date]
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.type = undefined
  query.category = ''
  query.keyword = ''
  dateRange.value = []
  advanced.value = false
  handleSearch()
}

function handlePageChange(page: number) {
  query.pageNum = page
  loadData()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  loadData()
}

/* ---------- 记账弹窗 ---------- */
const editorVisible = ref(false)
const editingAccount = ref<AccountVO | null>(null)

function openCreate() {
  editingAccount.value = null
  editorVisible.value = true
}

function openEdit(row: AccountVO) {
  editingAccount.value = row
  editorVisible.value = true
}

function onSaved() {
  loadData()
}

/* ---------- 导出 ---------- */
const exportVisible = ref(false)

/* ---------- 删除 ---------- */
async function handleDelete(row: AccountVO) {
  await accountApi.deleteAccount(row.id)
  ElMessage.success('删除成功')
  // 删除当前页最后一条时回退一页
  if (records.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  loadData()
}

function formatTime(t: string): string {
  if (!t) return ''
  return t.slice(0, 16).replace('T', ' ')
}

onMounted(() => {
  applyDateQuery()
  loadData()
})
</script>

<template>
  <div class="account-page">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
            <el-option :value="AccountType.EXPENSE" label="支出" />
            <el-option :value="AccountType.INCOME" label="收入" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="备注搜索"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button text @click="advanced = !advanced">
            {{ advanced ? '收起' : '高级筛选' }}
            <el-icon :class="{ rotate: advanced }" class="arrow"><ArrowDown /></el-icon>
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 高级筛选：分类 -->
      <el-collapse-transition>
        <div v-show="advanced" class="advanced-row">
          <span class="adv-label">分类</span>
          <el-select v-model="query.category" placeholder="全部分类" clearable style="width: 160px">
            <el-option v-for="cat in ALL_CATEGORIES" :key="cat" :value="cat" :label="cat" />
          </el-select>
        </div>
      </el-collapse-transition>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <h3 class="table-title">账目列表</h3>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="Plus" @click="openCreate">记一笔</el-button>
          <el-button :icon="Download" @click="exportVisible = true">导出 Excel</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" stripe class="account-table">
        <el-table-column prop="accountDate" label="日期" width="120" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag
              size="small"
              effect="plain"
              :class="row.type === AccountType.EXPENSE ? 'tag-expense' : 'tag-income'"
            >
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <div class="category-cell">
              <el-icon :size="16"><component :is="getCategoryIcon(row.category)" /></el-icon>
              <span>{{ row.category }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="150" align="right">
          <template #default="{ row }">
            <span
              class="money"
              :class="row.type === AccountType.EXPENSE ? 'text-expense' : 'text-income'"
            >
              {{ formatSignedMoney(row.amount, row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该账目吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <AccountEditorDialog v-model="editorVisible" :account="editingAccount" @saved="onSaved" />
    <ExportDialog v-model="exportVisible" />
  </div>
</template>

<style scoped lang="scss">
.account-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.advanced-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color-light);
}
.adv-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.arrow {
  font-size: 12px;
  margin-left: 2px;
  transition: transform 0.2s ease;
}
.arrow.rotate {
  transform: rotate(180deg);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.table-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.toolbar-actions {
  display: flex;
  gap: 10px;
}

.category-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
