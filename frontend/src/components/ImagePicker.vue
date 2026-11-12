<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
const props = withDefaults(defineProps<{ modelValue: File[]; maxCount?: number; maxSizeMb?: number }>(), { maxCount: 5, maxSizeMb: 5 })
const emit = defineEmits<{ (event: 'update:modelValue', files: File[]): void }>()
const previews = ref<string[]>([])
const error = ref('')
function clearPreviews() {
  previews.value.forEach(url => URL.revokeObjectURL(url))
  previews.value = []
}
function onChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  error.value = ''
  if (files.length > props.maxCount) error.value = `最多选择 ${props.maxCount} 张图片`
  else if (files.some(file => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type))) error.value = '只允许 JPG、PNG、WebP 图片'
  else if (files.some(file => file.size > props.maxSizeMb * 1024 * 1024)) error.value = `单张图片不能超过 ${props.maxSizeMb}MB`
  if (error.value) {
    emit('update:modelValue', [])
    input.value = ''
    return
  }
  emit('update:modelValue', files)
}
watch(() => props.modelValue, files => {
  clearPreviews()
  previews.value = files.map(file => URL.createObjectURL(file))
}, { immediate: true })
onBeforeUnmount(clearPreviews)
</script>
<template>
  <div class="image-picker">
    <div class="picker-controls">
      <label class="photo-picker-button">
        <input type="file" accept="image/jpeg,image/png,image/webp" multiple :disabled="maxCount === 0" aria-label="选择照片" @change="onChange">
        <span>{{ maxCount === 0 ? '照片已达上限' : '＋ 选择照片' }}</span>
      </label>
      <span v-if="modelValue.length" class="muted">已选择 {{ modelValue.length }} 张</span>
    </div>
    <p class="muted">JPG、PNG、WebP；最多 {{ maxCount }} 张，每张不超过 {{ maxSizeMb }}MB。</p>
    <p v-if="error" class="form-error">{{ error }}</p>
    <div v-if="previews.length" class="image-grid">
      <img v-for="(url, index) in previews" :key="url" :src="url" :alt="`预览图片 ${index + 1}`">
    </div>
  </div>
</template>


<style scoped>
.picker-controls { display: flex; align-items: center; gap: 14px; }
.photo-picker-button { position: relative; display: inline-flex; justify-content: center; align-items: center; padding: 13px 19px; border: 1px dashed #8ab398; border-radius: 10px; background: #f0f7f1; color: #267a52; cursor: pointer; font-size: 14px; }
.photo-picker-button input { position: absolute; inset: 0; opacity: 0; width: 100%; height: 100%; cursor: pointer; }
.photo-picker-button:focus-within { outline: 3px solid #86b89a; outline-offset: 3px; }
.photo-picker-button:has(input:disabled) { opacity: .55; }
</style>
