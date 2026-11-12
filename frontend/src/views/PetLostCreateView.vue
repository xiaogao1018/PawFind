<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import ImagePicker from '../components/ImagePicker.vue'
import { createPetLost, type PetLostPayload } from '../api/petLost'
import { uploadPetLostImage } from '../api/fileAttachment'
const router = useRouter()
const form = reactive({ petName: '', species: '', description: '', lostTime: '', lostLocation: '' })
const files = ref<File[]>([]), busy = ref(false), error = ref('')
async function submit() {
  error.value = ''
  if (!form.species.trim() || !form.lostTime || !form.lostLocation.trim()) { error.value = '请填写宠物种类、走失时间和走失地点。'; return }
  const payload: PetLostPayload = {
    petName: form.petName.trim() || null, species: form.species.trim(),
    description: form.description.trim() || null,
    lostTime: form.lostTime.length === 16 ? `${form.lostTime}:00` : form.lostTime,
    lostLocation: form.lostLocation.trim()
  }
  busy.value = true
  try {
    const r = await createPetLost(payload)
    if (r.code !== 200 || !r.data) throw new Error(r.message || '发布失败')
    for (const file of files.value) {
      const upload = await uploadPetLostImage(r.data.id, file)
      if (upload.code !== 200) throw new Error(upload.message || '记录已发布，但图片上传失败')
    }
    await router.push(`/pet-lost/${r.data.id}`)
  } catch (e) { error.value = e instanceof Error ? e.message : '发布失败' }
  finally { busy.value = false }
}
</script>
<template>
  <AppNav />
  <main class="container narrow"><RouterLink class="text-link" to="/pet-lost">← 返回列表</RouterLink>
    <section class="panel"><h1>发布宠物走失信息</h1><p class="muted">请填写准确的信息，方便大家辨认和寻找。</p>
      <form class="form-stack" @submit.prevent="submit">
        <label>宠物名称<input v-model="form.petName" maxlength="50" placeholder="例如：豆豆"></label>
        <label>宠物种类 *<input v-model="form.species" maxlength="30" required placeholder="猫、狗等"></label>
        <label>宠物特征描述<textarea v-model="form.description" maxlength="2000" rows="4"></textarea></label>
        <label>走失时间 *<input v-model="form.lostTime" type="datetime-local" required></label>
        <label>走失地点 *<input v-model="form.lostLocation" maxlength="255" required></label>
        <label>宠物照片</label><ImagePicker v-model="files" />
        <p v-if="error" class="form-error">{{ error }}</p><button class="button" :disabled="busy">{{ busy ? '正在发布……' : '发布走失信息' }}</button>
      </form>
    </section>
  </main>
</template>
