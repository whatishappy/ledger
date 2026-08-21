<script setup lang="ts">
/**
 * 交易图片上传组件（V2.1 新增）
 * 多图上传（≤5MB/张，最多 6 张），v-model 为已上传图片列表。
 *
 * 约束：后端 /images/upload 需要 accountId，故仅编辑已有账目时可上传；
 * 新建账目（accountId 为空）时展示提示，待保存后再上传。
 */
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import * as imageApi from '@/api/image'
import type { TransactionImageVO } from '@/types/api'

const modelValue = defineModel<TransactionImageVO[]>({ default: () => [] })
const props = defineProps<{
  accountId?: number | null
  /** 单张最大字节，默认 5MB */
  maxSize?: number
  /** 最多张数，默认 6 */
  limit?: number
}>()

const maxSize = props.maxSize ?? 5 * 1024 * 1024
const limit = props.limit ?? 6

async function handleUpload(options: UploadRequestOptions) {
  const file = options.file as File
  if (!props.accountId) {
    ElMessage.warning('请先保存账目，再上传图片')
    return
  }
  if (file.size > maxSize) {
    ElMessage.warning('单张图片不能超过 5MB')
    return
  }
  if (modelValue.value.length >= limit) {
    ElMessage.warning(`最多上传 ${limit} 张图片`)
    return
  }
  try {
    const img = await imageApi.uploadImage(props.accountId, file)
    modelValue.value = [...modelValue.value, img]
  } catch {
    // 拦截器已提示
  }
}

async function removeImage(index: number) {
  const img = modelValue.value[index]
  if (img.id) {
    await imageApi.deleteImage(img.id).catch(() => {})
  }
  modelValue.value = modelValue.value.filter((_, i) => i !== index)
}
</script>

<template>
  <div class="img-uploader">
    <el-alert
      v-if="!accountId"
      type="info"
      :closable="false"
      show-icon
      title="保存账目后可上传小票/发票图片"
    />
    <el-upload
      v-else
      :show-file-list="false"
      :auto-upload="true"
      :http-request="handleUpload"
      accept="image/png,image/jpeg"
      multiple
    >
      <el-button :icon="null" type="primary" plain>点击上传图片</el-button>
    </el-upload>

    <div v-if="modelValue.length" class="thumb-list">
      <div v-for="(img, i) in modelValue" :key="img.id ?? i" class="thumb-item">
        <el-image :src="img.imageUrl" fit="cover" :preview-src-list="modelValue.map((m) => m.imageUrl)" :initial-index="i" class="thumb" />
        <el-button class="rm-btn" type="danger" size="small" circle @click="removeImage(i)">×</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.thumb-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.thumb-item {
  position: relative;
}
.thumb {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
}
.rm-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  min-height: 20px;
  padding: 0;
}
</style>
