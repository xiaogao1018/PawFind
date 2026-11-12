<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import RecordImages from '../components/RecordImages.vue'
import RecordComments from '../components/RecordComments.vue'
import AppNav from '../components/AppNav.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { deletePetFound, getPetFound, type PetFound } from '../api/petFound'
import { getCurrentUser } from '../api/user'
const route = useRoute(), router = useRouter(), id = Number(route.params.id)
const item = ref<PetFound | null>(null), owner = ref(false), loading = ref(true), error = ref('')
onMounted(async () => {
  try {
    const r = await getPetFound(id); if (r.code !== 200 || !r.data) throw new Error(r.message || '记录不存在')
    item.value = r.data
    try { const u = await getCurrentUser(); owner.value = Number(u.data?.data?.id) === Number(r.data.userId) } catch { owner.value = false }
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
})
async function remove() {
  if (!confirm('确定删除这条招领信息吗？')) return
  try { const r = await deletePetFound(id); if (r.code !== 200) throw new Error(r.message || '删除失败'); await router.push('/pet-found') }
  catch (e) { error.value = e instanceof Error ? e.message : '删除失败' }
}
</script>
<template>
  <AppNav /><main class="container narrow">
    <div v-if="loading" class="state-panel">正在加载……</div><div v-else-if="error" class="state-panel state-error">{{ error }}</div>
    <section v-else-if="item" class="panel"><div class="page-heading"><div><h1>{{ item.petName || '未命名宠物' }}</h1><p class="accent">{{ item.species }}</p></div><StatusBadge :status="item.status" /></div>
      <p><b>发现地点：</b>{{ item.foundLocation }}</p><p><b>发现时间：</b>{{ item.foundTime?.replace('T', ' ') }}</p><p><b>描述：</b>{{ item.description || '暂无描述' }}</p>
      <RecordImages :path="`/api/pet-found/${id}`" :alt="item.petName || '宠物照片'" />
      <div v-if="owner" class="button-row"><RouterLink class="button" :to="`/pet-found/${id}/edit`">编辑</RouterLink><button class="button button-danger" @click="remove">删除</button></div>
    </section>
    <RecordComments v-if="item && !error" :path="`/api/pet-found/${id}`" :owner-id="item.userId" />
  </main>
</template>
