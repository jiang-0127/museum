<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { uploadImage } from '@/api/file'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    height?: string
    tip?: string
  }>(),
  {
    modelValue: '',
    height: '180px',
    tip: '支持 JPG、PNG、WebP、GIF，单张不超过 8MB',
  },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const uploading = ref(false)

async function handleUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const result = await uploadImage(options.file)
    emit('update:modelValue', result.url)
    options.onSuccess?.(result)
    ElMessage.success('图片上传成功')
  } catch (error) {
    options.onError?.(error as never)
  } finally {
    uploading.value = false
  }
}

function clear() {
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="image-upload">
    <el-upload
      class="uploader"
      :show-file-list="false"
      accept="image/jpeg,image/png,image/webp,image/gif"
      :http-request="handleUpload"
    >
      <div class="upload-box" :class="{ filled: props.modelValue }" :style="{ height: props.height }">
        <img v-if="props.modelValue" :src="props.modelValue" alt="已上传图片" />
        <div v-else class="upload-placeholder">
          <el-icon><Plus /></el-icon>
          <strong>点击上传本地图片</strong>
          <span>从电脑中选择图片文件</span>
        </div>
        <div v-if="props.modelValue" class="replace-mask">点击更换图片</div>
        <div v-if="uploading" class="uploading-mask">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在上传…</span>
        </div>
      </div>
    </el-upload>
    <div class="upload-footer">
      <span>{{ props.tip }}</span>
      <el-button v-if="props.modelValue" link size="small" type="danger" @click="clear">移除图片</el-button>
    </div>
  </div>
</template>

<style scoped>
.image-upload {
  width: 100%;
}

.uploader,
.uploader :deep(.el-upload) {
  display: block;
  width: 100%;
}

.upload-box {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  overflow: hidden;
  border: 1px dashed var(--line);
  border-radius: 10px;
  background: var(--paper-light);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.upload-box:hover {
  border-color: var(--gold);
  background: #fdf8ee;
}

.upload-box.filled {
  border-style: solid;
  background: var(--ink);
}

.upload-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 11px;
  text-align: center;
}

.upload-placeholder .el-icon {
  color: var(--gold-deep);
  font-size: 26px;
}

.upload-placeholder strong {
  color: var(--text);
  font-size: 13px;
  font-weight: 500;
}

.upload-placeholder span {
  font-size: 10px;
}

.replace-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(15, 21, 33, 0.52);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.08em;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.upload-box:hover .replace-mask {
  opacity: 1;
}

.uploading-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(15, 21, 33, 0.62);
  color: #fff;
  font-size: 12px;
}

.upload-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.5;
}
</style>
