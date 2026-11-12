<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import RecordComments from '../components/RecordComments.vue'
import AppNav from '../components/AppNav.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getPetLost, type PetLost } from '../api/petLost'
import { getCurrentUser } from '../api/user'
import { getPetLostImageBlob, listPetLostImages } from '../api/fileAttachment'
import { createPetClue, listPetClues, updatePetClueStatus, type PetClue } from '../api/petClue'
const route = useRoute(), id = Number(route.params.id)
const item = ref<PetLost | null>(null), images = ref<{ id: number; url: string; name: string }[]>([])
const clues = ref<PetClue[]>([]), currentUserId = ref<number | null>(null)
const clueForm = ref({ content: '', contactInfo: '' })
const loading = ref(true), error = ref(''), clueError = ref(''), clueBusy = ref(false)
const objectUrls: string[] = []
const owner = () => !!item.value && currentUserId.value !== null && Number(item.value.userId) === currentUserId.value
async function load() {
  try {
    const r = await getPetLost(id)
    if (r.code !== 200 || !r.data) throw new Error(r.message || '记录不存在')
    item.value = r.data
    try { const u = await getCurrentUser(); currentUserId.value = u.data?.data?.id ?? null } catch { currentUserId.value = null }
    const imageResult = await listPetLostImages(id)
    for (const image of imageResult.data || []) {
      const blob = await getPetLostImageBlob(id, image.id)
      const url = URL.createObjectURL(blob); objectUrls.push(url)
      images.value.push({ id: image.id, url, name: image.originalFilename })
    }
    const clueResult = await listPetClues(id)
    clues.value = clueResult.data || []
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
}
async function submitClue() {
  clueError.value = ''
  if (!clueForm.value.content.trim()) { clueError.value = '请填写线索内容。'; return }
  clueBusy.value = true
  try {
    const r = await createPetClue(id, { content: clueForm.value.content.trim(), contactInfo: clueForm.value.contactInfo.trim() || null })
    if (r.code !== 200) throw new Error(r.message || '提交失败')
    clueForm.value = { content: '', contactInfo: '' }
    clues.value = (await listPetClues(id)).data || []
  } catch (e) { clueError.value = e instanceof Error ? e.message : '提交线索失败' }
  finally { clueBusy.value = false }
}
async function verify(id: number, status: 'VERIFIED' | 'INVALID') {
  clueError.value = ''
  try {
    const r = await updatePetClueStatus(id, status)
    if (r.code !== 200) throw new Error(r.message || '更新失败')
    clues.value = (await listPetClues(Number(route.params.id))).data || []
  } catch (e) { clueError.value = e instanceof Error ? e.message : '操作失败' }
}
onMounted(load)
onBeforeUnmount(() => objectUrls.forEach(URL.revokeObjectURL))
</script>
<template>
  <AppNav /><main class="container narrow">
    <div v-if="loading" class="state-panel">正在加载……</div><div v-else-if="error" class="state-panel state-error">{{ error }}</div>
    <template v-else-if="item">
      <section class="panel"><div class="page-heading"><div><h1>{{ item.petName || '未命名宠物' }}</h1><p class="accent">{{ item.species }}</p></div><StatusBadge :status="item.status" /></div>
        <p><b>走失地点：</b>{{ item.lostLocation }}</p><p><b>走失时间：</b>{{ item.lostTime?.replace('T', ' ') }}</p><p><b>描述：</b>{{ item.description || '暂无描述' }}</p>
        <div v-if="images.length" class="image-grid"><img v-for="image in images" :key="image.id" :src="image.url" :alt="image.name"></div><p v-else class="muted">暂无照片</p>
        <RouterLink v-if="owner() && item.status === 'LOOKING'" class="button" :to="`/pet-lost/${id}/edit`">编辑本人信息</RouterLink>
      </section>
      <section class="panel"><h2>提供线索</h2>
        <form class="form-stack" @submit.prevent="submitClue">
          <label>线索内容<textarea v-model="clueForm.content" rows="3" maxlength="2000" required></textarea></label>
          <label>联系方式（选填）<input v-model="clueForm.contactInfo" maxlength="100"></label>
          <p v-if="clueError" class="form-error">{{ clueError }}</p><button class="button" :disabled="clueBusy">{{ clueBusy ? '提交中……' : '提交线索' }}</button>
        </form>
        <h3>线索记录</h3><p v-if="!clues.length" class="muted">暂无线索。</p>
        <article v-for="clue in clues" :key="clue.id" class="list-row"><div><StatusBadge :status="clue.status" /><p>{{ clue.content }}</p><small class="muted">{{ clue.contactInfo || '未提供联系方式' }} · {{ clue.createdAt }}</small></div>
          <div v-if="owner() && clue.status === 'PENDING'" class="button-row"><button class="button button-small" @click="verify(clue.id, 'VERIFIED')">核实有效</button><button class="button button-small button-danger" @click="verify(clue.id, 'INVALID')">标记无效</button></div>
        </article><p class="muted">核实线索不会自动将走失状态改为已寻回。</p>
      </section>
      <RecordComments :path="`/api/pet-lost/${id}`" :owner-id="item.userId" />
    </template>
  </main>
</template>
