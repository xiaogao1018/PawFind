<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import ImagePicker from '../components/ImagePicker.vue'
import { getPetLost, updatePetLost, type PetLostPayload } from '../api/petLost'
import { getCurrentUser } from '../api/user'
import { listPetLostImages, uploadPetLostImage } from '../api/fileAttachment'
const route = useRoute(), router = useRouter(), id = Number(route.params.id)
const form = reactive({ petName: '', species: '', description: '', lostTime: '', lostLocation: '' })
const files = ref<File[]>([]), existingCount = ref(0), owner = ref(false)
const loading = ref(true), busy = ref(false), error = ref('')
async function load() {
  try {
    const [r, u] = await Promise.all([getPetLost(id), getCurrentUser()])
    if (r.code !== 200 || !r.data) throw new Error(r.message || '记录不存在')
    owner.value = Number(r.data.userId) === Number(u.data?.data?.id) && r.data.status === 'LOOKING'
    if (!owner.value) throw new Error('只有发布者本人可以编辑正在寻找中的记录')
    Object.assign(form, { petName: r.data.petName || '', species: r.data.species, description: r.data.description || '', lostTime: r.data.lostTime.slice(0, 16), lostLocation: r.data.lostLocation })
    const images = await listPetLostImages(id); existingCount.value = images.data?.length || 0
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
}
async function submit() {
  error.value = ''
  if (!form.species.trim() || !form.lostTime || !form.lostLocation.trim()) { error.value = '请填写所有必填字段。'; return }
  if (existingCount.value + files.value.length > 5) { error.value = '每条走失记录最多关联5张图片。'; return }
  busy.value = true
  try {
    const payload: PetLostPayload = { petName: form.petName.trim() || null, species: form.species.trim(), description: form.description.trim() || null, lostTime: form.lostTime.length === 16 ? `${form.lostTime}:00` : form.lostTime, lostLocation: form.lostLocation.trim() }
    const r = await updatePetLost(id, payload); if (r.code !== 200) throw new Error(r.message || '更新失败')
    for (const file of files.value) { const u = await uploadPetLostImage(id, file); if (u.code !== 200) throw new Error(u.message || '信息已更新，但图片上传失败') }
    await router.push(`/pet-lost/${id}`)
  } catch (e) { error.value = e instanceof Error ? e.message : '更新失败' }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
  <AppNav /><main class="container narrow"><section class="panel"><h1>编辑走失信息</h1>
    <div v-if="loading" class="muted">正在加载……</div><template v-else-if="owner">
      <form class="form-stack" @submit.prevent="submit">
        <label>宠物名称<input v-model="form.petName" maxlength="50"></label><label>宠物种类<input v-model="form.species" maxlength="30" required></label>
        <label>特征描述<textarea v-model="form.description" maxlength="2000" rows="4"></textarea></label><label>走失时间<input v-model="form.lostTime" type="datetime-local" required></label>
        <label>走失地点<input v-model="form.lostLocation" maxlength="255" required></label>
        <p class="muted">已有 {{ existingCount }} 张图片；此页面仅追加图片，删除附件需后端提供接口。</p>
        <ImagePicker v-model="files" :max-count="Math.max(0, 5 - existingCount)" />
        <p v-if="error" class="form-error">{{ error }}</p><button class="button" :disabled="busy">{{ busy ? '保存中……' : '保存修改' }}</button>
      </form>
    </template><p v-else-if="error" class="form-error">{{ error }}</p></section></main>
</template>
