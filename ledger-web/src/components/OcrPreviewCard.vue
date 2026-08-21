<script setup lang="ts">
/**
 * OCR 预填卡片（V2.1 新增，A2 场景）
 * 上传小票后 AI 返回结构化数据，渲染为可编辑表单，确认后入库。
 * 确认记账调用 POST /account/add（复用 A-01 新增记账接口）。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { AccountType, getCategoriesByType } from '@/types/business'
import type { AccountTypeValue } from '@/types/business'
import * as accountApi from '@/api/account'
import type { AiOcrResult } from '@/types/api'

const props = defineProps<{ result: AiOcrResult }>()
const emit = defineEmits<{ booked: []; discard: [] }>()

const form = reactive({
  type: AccountType.EXPENSE as AccountTypeValue,
  merchant: '',
  date: '',
  total: undefined as number | undefined,
  category: '',
})

watch(
  () => props.result,
  (r) => {
    form.merchant = r.merchant ?? ''
    form.date = r.date ?? ''
    form.total = r.total != null ? Number(r.total) : undefined
    form.category = r.category ?? ''
  },
  { immediate: true },
)

const categories = computed(() => getCategoriesByType(form.type))
const saving = ref(false)

async function confirm() {
  if (!form.total || form.total <= 0) {
    ElMessage.warning('金额需大于 0')
    return
  }
  if (!form.date) {
    ElMessage.warning('请选择日期')
    return
  }
  saving.value = true
  try {
    await accountApi.addAccount({
      type: form.type,
      category: form.category || '其他',
      amount: form.total,
      accountDate: form.date,
      remark: form.merchant || undefined,
    })
    ElMessage.success('已记账')
    emit('booked')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="ocr-card">
    <div class="head">📷 小票识别结果</div>
    <el-image v-if="result.imageUrl" :src="result.imageUrl" fit="contain" class="receipt" />
    <el-form label-width="64px" size="small">
      <el-form-item label="商户">
        <el-input v-model="form.merchant" placeholder="商户" />
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" placeholder="日期" style="width: 100%" />
      </el-form-item>
      <el-form-item label="金额">
        <el-input-number v-model="form.total" :precision="2" :min="0.01" :controls="false" style="width: 100%" />
      </el-form-item>
      <el-form-item label="类型">
        <el-radio-group v-model="form.type">
          <el-radio-button :value="AccountType.EXPENSE">支出</el-radio-button>
          <el-radio-button :value="AccountType.INCOME">收入</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
    </el-form>
    <ul v-if="result.items?.length" class="items">
      <li v-for="(it, i) in result.items" :key="i">
        {{ it.name }} ×{{ it.quantity ?? 1 }} ¥{{ it.price }}
      </li>
    </ul>
    <div class="actions">
      <el-button size="small" @click="emit('discard')">丢弃</el-button>
      <el-button size="small" type="primary" :loading="saving" @click="confirm">确认记账</el-button>
    </div>
  </div>
</template>

<style scoped>
.ocr-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 10px;
  margin: 8px 0;
  background: var(--el-fill-color-blank);
}
.head {
  font-weight: 600;
  margin-bottom: 6px;
}
.receipt {
  width: 100%;
  max-height: 160px;
  margin-bottom: 8px;
  border-radius: 6px;
}
.items {
  margin: 6px 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
