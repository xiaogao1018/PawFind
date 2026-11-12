<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ImagePicker from '../components/ImagePicker.vue'
import RecordImages from '../components/RecordImages.vue'
import { uploadRecordImage, listRecordImages } from '../api/fileAttachment'
import AppNav from '../components/AppNav.vue'
import { createPetFound, getPetFound, updatePetFound, type PetFoundPayload } from '../api/petFound'
import { getCurrentUser } from '../api/user'
const route = useRoute(), router = useRouter(), id = route.params.id ? Number(route.params.id) : null, editing = id !== null
const form = reactive({ petName: '', species: '', description: '', foundTime: '', foundLocation: '' })
const files = ref<File[]>([]), existingCount = ref(0), savedId = ref<number | null>(id)
const loading = ref(editing), busy = ref(false), error = ref('')
onMounted(async () => {
  if (!editing || id === null) return
  try {
    const [r, u] = await Promise.all([getPetFound(id), getCurrentUser()])
    if (r.code !== 200 || !r.data) throw new Error(r.message || '记录不存在')
    if (Number(r.data.userId) !== Number(u.data?.data?.id)) throw new Error('只有发布者本人可以编辑')
    existingCount.value = (await listRecordImages(`/api/pet-found/${id}`)).data?.length || 0
    Object.assign(form, { petName: r.data.petName || '', species: r.data.species, description: r.data.description || '', foundTime: r.data.foundTime.slice(0, 16), foundLocation: r.data.foundLocation })
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
})
async function submit() {
  error.value = ''
  if (!form.species.trim() || !form.foundTime || !form.foundLocation.trim()) { error.value = '请填写宠物种类、发现时间和发现地点。'; return }
  const payload: PetFoundPayload = { petName: form.petName.trim() || null, species: form.species.trim(), description: form.description.trim() || null, foundTime: form.foundTime.length === 16 ? `${form.foundTime}:00` : form.foundTime, foundLocation: form.foundLocation.trim() }
  busy.value = true
  let detailsSaved = false
  try {
    const r = savedId.value !== null ? await updatePetFound(savedId.value, payload) : await createPetFound(payload)
    if (r.code !== 200 || !r.data) throw new Error(r.message || '保存失败')
    detailsSaved = true
    savedId.value = r.data.id
    while (files.value.length) {
      const file = files.value[0]
      if (!file) break
      const uploaded = await uploadRecordImage(`/api/pet-found/${savedId.value}`, file)
      if (uploaded.code !== 200) throw new Error(uploaded.message)
      files.value = files.value.slice(1)
      existingCount.value++
    }
    await router.push(`/pet-found/${savedId.value}`)
  } catch (e) { error.value = (detailsSaved ? '信息已保存，照片上传可重试。' : '') + (e instanceof Error ? e.message : '保存失败') }
  finally { busy.value = false }
}
</script>
<template>
  <AppNav /><main class="container narrow"><section class="panel"><h1>{{ editing ? '编辑招领信息' : '发布宠物招领' }}</h1><p class="muted">上传照片并描述发现地点，帮助它找到主人。</p>
    <div v-if="loading" class="muted">正在加载……</div><form v-else class="form-stack" @submit.prevent="submit">
      <label>宠物名称<input v-model="form.petName" maxlength="50"></label><label>宠物种类 *<input v-model="form.species" maxlength="30" required></label>
      <label>特征描述<textarea v-model="form.description" maxlength="2000" rows="4"></textarea></label><label>发现时间 *<input v-model="form.foundTime" type="datetime-local" required></label>
      <label>发现地点 *<input v-model="form.foundLocation" maxlength="255" required></label><label>宠物照片</label>
      <RecordImages v-if="savedId !== null && existingCount" :key="existingCount" :path="`/api/pet-found/${savedId}`" />
      <ImagePicker v-model="files" :max-count="Math.max(0, 5 - existingCount)" />
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="button" :disabled="busy">{{ busy ? '保存中……' : '提交' }}</button>
    </form></section></main>
</template>
