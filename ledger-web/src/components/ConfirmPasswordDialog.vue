<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as userApi from '@/api/user'
import { useUserStore } from '@/stores/user'

const visible = defineModel<boolean>({ default: false })
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({ password: '' })
const loading = ref(false)

const rules: FormRules = {
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function resetForm() {
  formRef.value?.resetFields()
  form.password = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm(
      '注销后账号将无法恢复，所有账目、预算数据将被清理。确定继续吗？',
      '危险操作',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  loading.value = true
  try {
    await userApi.deleteAccount(form.password)
    userStore.reset()
    ElMessage.success('账号已注销')
    visible.value = false
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="注销账号"
    width="min(420px, 92vw)"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-alert
      title="此操作不可撤销，注销后数据将全部清除。请输入当前登录密码确认。"
      type="error"
      :closable="false"
      show-icon
      class="warn-alert"
    />
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="pwd-form">
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入当前登录密码"
          show-password
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit">确认注销</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.warn-alert {
  margin-bottom: 16px;
}
.pwd-form {
  margin-top: 4px;
}
</style>
