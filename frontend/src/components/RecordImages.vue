<script setup lang="ts">
import { ref, watch } from 'vue'
import request from '../api/request'
import type { FileAttachment } from '../api/fileAttachment'
import type { ApiResult } from '../types/common'

const props = withDefaults(defineProps<{ path: string; cover?: boolean; alt?: string }>(), {
  cover: false, alt: '宠物照片'
})
const urls = ref<string[]>([])
const loading = ref(false)
const failed = ref(false)
const retry = ref(0)

watch(() => [props.path, props.cover, retry.value] as const, async (_, __, onCleanup) => {
  const controller = new AbortController()
  const allocated: string[] = []
  onCleanup(() => {
    controller.abort()
    allocated.forEach(url => URL.revokeObjectURL(url))
  })
  urls.value = []
  loading.value = true
  failed.value = false
  try {
    const response = await request.get<ApiResult<FileAttachment[]>>(props.path + '/images', {
      signal: controller.signal
    })
    if (response.data.code !== 200) throw new Error(response.data.message)
    const images = props.cover ? response.data.data?.slice(0, 1) : response.data.data
    const results = await Promise.allSettled((images || []).map(image => request.get<Blob>(image.url, {
      responseType: 'blob', signal: controller.signal
    })))
    if (controller.signal.aborted) return
    for (const result of results) {
      if (result.status === 'fulfilled') allocated.push(URL.createObjectURL(result.value.data))
      else failed.value = true
    }
    urls.value = [...allocated]
  } catch {
    if (!controller.signal.aborted) failed.value = true
  } finally {
    if (!controller.signal.aborted) loading.value = false
  }
}, { immediate: true })
</script>
<template>
  <div v-if="cover" class="record-cover">
    <img v-if="urls[0]" :src="urls[0]" :alt="alt" loading="lazy">
    <div v-else class="cover-placeholder"><span aria-hidden="true">🐾</span><small>{{ loading ? '照片加载中' : '暂无照片' }}</small></div>
  </div>
  <div v-else-if="urls.length || loading || failed" class="record-gallery">
    <div v-if="urls.length" class="image-grid"><img v-for="(url, index) in urls" :key="url" :src="url" :alt="alt + ' ' + (index + 1)" loading="lazy"></div>
    <p v-if="loading" class="muted">照片加载中……</p>
    <button v-if="failed" type="button" class="text-button" @click="retry++">部分照片加载失败，点击重试</button>
  </div>
</template>
