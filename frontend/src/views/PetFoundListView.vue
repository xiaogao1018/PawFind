<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listPetFound, type PetFound } from '../api/petFound'
import RecordImages from '../components/RecordImages.vue'
import AppNav from '../components/AppNav.vue'
import StatusBadge from '../components/StatusBadge.vue'
const records = ref<PetFound[]>([]), page = ref(1), totalPages = ref(0), total = ref(0)
const loading = ref(false), error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { const r = await listPetFound(page.value); if (r.code !== 200 || !r.data) throw new Error(r.message || '加载失败'); records.value = r.data.records || []; total.value = r.data.total; totalPages.value = r.data.totalPages }
  catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
}
onMounted(load)
</script>
<template>
  <AppNav /><main class="container"><div class="page-heading"><div><h1>宠物招领信息</h1><p class="muted">发布你捡到的宠物信息，帮助它找到主人。</p></div><RouterLink class="button" to="/pet-found/create">发布招领信息</RouterLink></div>
    <div v-if="loading" class="state-panel">正在加载……</div><div v-else-if="error" class="state-panel state-error">{{ error }} <button class="button button-light" @click="load">重试</button></div>
    <div v-else-if="!records.length" class="state-panel"><div class="empty-icon">🐕</div><h2>暂时没有招领信息</h2><RouterLink class="button" to="/pet-found/create">发布第一条</RouterLink></div>
    <template v-else><p class="muted">共 {{ total }} 条记录</p><div class="card-grid"><article v-for="item in records" :key="item.id" class="card pet-card">
          <RouterLink :to="`/pet-found/${item.id}`" :aria-label="(item.petName || '宠物') + '详情'"><RecordImages :path="`/api/pet-found/${item.id}`" cover :alt="item.petName || '宠物照片'" /></RouterLink>
          <div class="card-body">
      <div class="card-title-row"><h2>{{ item.petName || '未命名宠物' }}</h2><StatusBadge :status="item.status" /></div><p class="accent">{{ item.species }}</p><p>📍 {{ item.foundLocation }}</p><p class="muted">发现时间：{{ item.foundTime?.replace('T', ' ') }}</p><p class="clamp">{{ item.description || '暂无描述' }}</p><RouterLink class="text-link" :to="`/pet-found/${item.id}`">查看详情 →</RouterLink>
    </div></article></div><div class="pagination"><button class="button button-light" :disabled="page <= 1" @click="page--; load()">上一页</button><span>第 {{ page }} / {{ totalPages }} 页</span><button class="button button-light" :disabled="page >= totalPages" @click="page++; load()">下一页</button></div></template>
  </main>
</template>
