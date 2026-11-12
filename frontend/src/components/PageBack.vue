<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const props = withDefaults(defineProps<{ home?: string; compact?: boolean }>(), {
  home: '/pet-lost', compact: false
})
const router = useRouter(), route = useRoute()
const previous = computed(() => {
  const value = router.options.history.state.back
  return typeof value === 'string' && value.startsWith('/') && value !== route.fullPath
    && !/^\/(login|register)(\?|$)/.test(value) ? value : null
})
const homeLabel = computed(() => props.home.startsWith('/admin') ? '管理首页' : '首页')
function goBack() {
  if (previous.value) router.back()
  else router.push(props.home)
}
</script>
<template>
  <div class="page-context" :class="{ compact }">
    <button class="back-button" type="button" @click="goBack"><span aria-hidden="true">←</span><span>{{ previous ? '返回上一页' : '返回' + homeLabel }}</span></button>
    <div v-if="!compact" class="page-breadcrumb"><RouterLink :to="home">{{ homeLabel }}</RouterLink><span aria-hidden="true">/</span><span>{{ route.meta.title || 'PawFind' }}</span></div>
  </div>
</template>
