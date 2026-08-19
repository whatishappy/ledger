<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { AccountType, type AccountTypeValue } from '@/types/business'
import type { AccountVO } from '@/types/api'
import * as accountApi from '@/api/account'
import AmountQuickPad from '@/components/AmountQuickPad.vue'
import CategoryIconGrid from '@/components/CategoryIconGrid.vue'
import { today } from '@/utils/date'

const visible = defineModel<boolean>({ default: false })
const props = defineProps<{ account?: AccountVO | null }>()
const emit = defineEmits<{ saved: [] }>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const isEdit = ref(false)

const form = reactive({
  type: AccountType.EXPENSE as AccountTypeValue,
  category: '',
  amount: undefined as number | undefined,
  accountDate: today(),
  remark: '',
  version: 0,
})

const rules: FormRules = {
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  accountDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

/** 后端限制业务日期不能晚于当前日期 + 7 天 */
function disabledFutureDate(date: Date): boolean {
  const limit = Date.now() + 7 * 24 * 60 * 60 * 1000
  return date.getTime() > limit
}

function initForm() {
  if (props.account) {
    isEdit.value = true
    form.type = props.account.type
    form.category = props.account.category
    form.amount = Number(props.account.amount)
    form.accountDate = props.account.accountDate
    form.remark = props.account.remark || ''
    form.version = props.account.version
  } else {
    isEdit.value = false
    form.type = AccountType.EXPENSE
    form.category = ''
    form.amount = undefined
    form.accountDate = today()
    form.remark = ''
    form.version = 0
  }
  formRef.value?.clearValidate()
}

watch(visible, (v) => {
  if (v) initForm()
})

function handleTypeChange(type: AccountTypeValue) {
  form.type = type
  const allowed =
    type === AccountType.INCOME ? ['工资', '其他'] : ['餐饮', '交通', '购物', '娱乐', '其他']
  if (!allowed.includes(form.category)) form.category = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.amount === undefined || form.amount <= 0) {
    ElMessage.warning('金额必须大于 0')
    return
  }

  const payload = {
    type: form.type,
    category: form.category,
    amount: form.amount,
    accountDate: form.accountDate,
    remark: form.remark.trim() || undefined,
  }

  saving.value = true
  try {
    if (isEdit.value && props.account) {
      // 跨月修改二次确认（§4.4）：重置新旧月份预算/仪表盘缓存
      const oldMonth = props.account.accountDate.slice(0, 7)
      const newMonth = form.accountDate.slice(0, 7)
      if (oldMonth !== newMonth) {
        try {
          await ElMessageBox.confirm(
            '跨月修改将重置本月及上月的预算进度缓存与仪表盘缓存，是否继续？',
            '提示',
            { confirmButtonText: '继续修改', cancelButtonText: '取消', type: 'warning' },
          )
        } catch {
          return
        }
      }
      // 乐观锁：version 随请求体回传，冲突时后端返回 2002
      await accountApi.updateAccount({ id: props.account.id, version: form.version, ...payload })
      ElMessage.success('修改成功')
    } else {
      await accountApi.addAccount(payload)
      ElMessage.success('记账成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑账目' : '记一笔'"
    width="min(480px, 92vw)"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- 收支类型切换 -->
      <el-radio-group v-model="form.type" class="type-switch" @change="handleTypeChange">
        <el-radio-button :value="AccountType.EXPENSE">支出</el-radio-button>
        <el-radio-button :value="AccountType.INCOME">收入</el-radio-button>
      </el-radio-group>

      <!-- 金额 + 快捷面额 -->
      <el-form-item prop="amount" class="amount-field">
        <div class="amount-input-wrap">
          <span class="currency">¥</span>
          <el-input-number
            v-model="form.amount"
            :precision="2"
            :min="0.01"
            :max="99999999.99"
            :controls="false"
            :placeholder="'0.00'"
            aria-label="金额"
            class="amount-input"
          />
        </div>
      </el-form-item>
      <AmountQuickPad v-model="form.amount" />

      <!-- 分类九宫格 -->
      <el-form-item prop="category" class="cat-field">
        <CategoryIconGrid v-model="form.category" :type="form.type" />
      </el-form-item>
      <div class="visually-hidden" role="status" aria-live="polite">
        {{ form.category ? `已选择分类：${form.category}` : '' }}
      </div>

      <!-- 日期 + 备注 -->
      <div class="row">
        <el-form-item prop="accountDate" class="date-field">
          <template #label>日期</template>
          <el-date-picker
            v-model="form.accountDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledFutureDate"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          placeholder="备注（选填）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        {{ isEdit ? '保存修改' : '确认记账' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.type-switch {
  display: flex;
  margin-bottom: 20px;
}
.type-switch :deep(.el-radio-button__inner) {
  width: 110px;
  justify-content: center;
}

.amount-field {
  margin-bottom: 12px;
}

.amount-input-wrap {
  display: flex;
  align-items: baseline;
  gap: 6px;
  width: 100%;
}

.currency {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.amount-input {
  flex: 1;
}
.amount-input :deep(.el-input__inner) {
  font-size: 32px;
  font-weight: 700;
  height: 52px;
  text-align: left;
  font-variant-numeric: tabular-nums;
}

.cat-field {
  margin-top: 16px;
}

.row {
  display: flex;
  gap: 16px;
}
.date-field {
  flex: 1;
}
</style>
