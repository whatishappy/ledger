<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as userApi from '@/api/user'
import { useUserStore } from '@/stores/user'

const visible = defineModel<boolean>({ default: false })
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const loading = ref(false)

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 位', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value && value === form.oldPassword) {
          callback(new Error('新密码不能与旧密码相同'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  formRef.value?.resetFields()
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userApi.changePassword(form.oldPassword, form.newPassword)
    // 后端成功会使 token_version 自增 → 当前会话一并失效，必须重新登录
    userStore.reset()
    ElMessage.success('密码已修改，请重新登录')
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
    title="修改密码"
    width="min(420px, 92vw)"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入当前密码"
          show-password
          autocomplete="current-password"
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="6-100 位"
          show-password
          autocomplete="new-password"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="再次输入新密码"
          show-password
          autocomplete="new-password"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
