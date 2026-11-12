<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ImagePicker from '../components/ImagePicker.vue'
import RecordImages from '../components/RecordImages.vue'
import { uploadRecordImage, listRecordImages } from '../api/fileAttachment'
import AppNav from '../components/AppNav.vue'
import { createCommunityPost, getCommunityPost, updateCommunityPost, postCategories, type PostCategory } from '../api/community'
import { getCurrentUser } from '../api/user'
const route = useRoute(), router = useRouter(), id = route.params.id ? Number(route.params.id) : null, editing = id !== null
const files = ref<File[]>([]), existingCount = ref(0), savedId = ref<number | null>(id)
const form = reactive({ title: '', content: '', category: 'EXPERIENCE' as PostCategory }), loading = ref(editing), busy = ref(false), error = ref('')
onMounted(async () => {
  if (!editing || id === null) return
  try {
    const [r, u] = await Promise.all([getCommunityPost(id), getCurrentUser()])
    if (r.code !== 200 || !r.data) throw new Error(r.message || '帖子不存在')
    if (Number(r.data.userId) !== Number(u.data?.data?.id)) throw new Error('只有作者本人可以编辑')
    existingCount.value = (await listRecordImages(`/api/community/posts/${id}`)).data?.length || 0
    form.category = r.data.category || 'EXPERIENCE'
    form.title = r.data.title; form.content = r.data.content
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
})
async function submit() {
  error.value = ''
  if (!form.title.trim() || !form.content.trim()) { error.value = '标题和正文不能为空。'; return }
  busy.value = true
  let detailsSaved = false
  try {
    const payload = { title: form.title.trim(), content: form.content.trim(), category: form.category }
    const r = savedId.value !== null ? await updateCommunityPost(savedId.value, payload) : await createCommunityPost(payload)
    if (r.code !== 200 || !r.data) throw new Error(r.message || '提交失败')
    detailsSaved = true
    savedId.value = r.data.id
    while (files.value.length) {
      const file = files.value[0]
      if (!file) break
      const uploaded = await uploadRecordImage(`/api/community/posts/${savedId.value}`, file)
      if (uploaded.code !== 200) throw new Error(uploaded.message)
      files.value = files.value.slice(1)
      existingCount.value++
    }
    await router.push(`/community/posts/${savedId.value}`)
  } catch (e) { error.value = (detailsSaved ? '帖子已保存，照片上传可重试。' : '') + (e instanceof Error ? e.message : '提交失败') }
  finally { busy.value = false }
}
</script>
<template>
  <AppNav /><main class="container narrow"><section class="panel"><h1>{{ editing ? '编辑社区帖子' : '发布社区帖子' }}</h1>
    <p class="muted">新发布或编辑后的帖子需要审核，通过且公开后才会出现在信息流。</p><div v-if="loading" class="muted">正在加载……</div>
    <form v-else class="form-stack" @submit.prevent="submit"><label>分类<select v-model="form.category" aria-label="分类"><option v-for="category in postCategories" :key="category.value" :value="category.value">{{ category.label }}</option></select></label><label>标题<input v-model="form.title" maxlength="120" required></label>
      <label>正文<textarea v-model="form.content" rows="9" maxlength="10000" required></textarea></label><label>配图</label>
      <RecordImages v-if="savedId !== null && existingCount" :key="existingCount" :path="`/api/community/posts/${savedId}`" />
      <ImagePicker v-model="files" :max-count="Math.max(0, 5 - existingCount)" />
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="button" :disabled="busy">{{ busy ? '提交中……' : '提交审核' }}</button></form>
  </section></main>
</template>
