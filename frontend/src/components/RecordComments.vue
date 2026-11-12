<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getCurrentUser } from '../api/user'
import { createRecordComment, getRecordComments, type RecordComment } from '../api/messages'
const props = defineProps<{ path: string; ownerId: number }>()
const records = ref<RecordComment[]>([])
const userId = ref<number | null>(null)
const loading = ref(true), busy = ref(false), error = ref('')
const content = ref(''), page = ref(1), totalPages = ref(0), total = ref(0)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await getRecordComments(props.path, page.value)
    if (!result.data) throw new Error(result.message)
    records.value = result.data.records
    totalPages.value = result.data.totalPages
    total.value = result.data.total
  } catch (e) {
    error.value = e instanceof Error ? e.message : '留言加载失败'
  } finally { loading.value = false }
}
async function submit() {
  if (!content.value.trim() || busy.value) return
  busy.value = true
  error.value = ''
  try {
    await createRecordComment(props.path, content.value.trim())
    content.value = ''
    page.value = 1
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '留言发送失败'
  } finally { busy.value = false }
}
function move(delta: number) { page.value += delta; load() }
onMounted(async () => {
  if (localStorage.getItem('token')) {
    try { userId.value = (await getCurrentUser()).data.data?.id || null } catch { userId.value = null }
  }
  await load()
})
</script>
<template>
  <section class="panel record-discussion">
    <div class="page-heading"><h2>交流留言 <small class="muted">{{ total }} 条</small></h2><RouterLink v-if="userId && userId !== ownerId" class="button button-light button-small" :to="{ path: '/messages', query: { to: ownerId } }">私信发布者</RouterLink></div>
    <form v-if="userId" class="form-stack" @submit.prevent="submit">
      <label>写下留言<textarea v-model="content" maxlength="2000" rows="3" required placeholder="向发布者询问情况，或分享你了解的信息"></textarea></label>
      <div class="comment-submit"><small class="muted">留言将公开展示，发布者会收到通知。</small><button class="button button-small" :disabled="busy">{{ busy ? '发送中……' : '发送留言' }}</button></div>
    </form>
    <p v-else class="muted"><RouterLink class="text-link" to="/login">登录</RouterLink>后可以留言或发起私信。</p>
    <p v-if="error" class="form-error">{{ error }} <button class="text-button" @click="load">重试</button></p>
    <p v-if="loading" class="muted">留言加载中……</p>
    <p v-else-if="!records.length" class="muted">暂时还没有留言。</p>
    <article v-for="record in records" :key="record.id" class="discussion-comment">
      <div class="member-avatar">{{ record.username.charAt(0).toUpperCase() }}</div>
      <div class="grow"><div class="card-title-row"><b>{{ record.username }}</b><RouterLink v-if="userId && userId !== record.userId" class="text-link" :to="{ path: '/messages', query: { to: record.userId } }">私信</RouterLink></div><p>{{ record.content }}</p><small class="muted">{{ record.createdAt.replace('T', ' ') }}</small></div>
    </article>
    <div v-if="totalPages > 1" class="pagination"><button class="button button-light button-small" :disabled="page <= 1 || loading" @click="move(-1)">上一页</button><span>{{ page }} / {{ totalPages }}</span><button class="button button-light button-small" :disabled="page >= totalPages || loading" @click="move(1)">下一页</button></div>
  </section>
</template>
