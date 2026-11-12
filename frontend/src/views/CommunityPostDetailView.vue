<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import RecordImages from '../components/RecordImages.vue'
import AppNav from '../components/AppNav.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { createCommunityComment, getCommunityPost, listCommunityComments, type CommunityComment, type CommunityPost } from '../api/community'
import { getCurrentUser } from '../api/user'
const route = useRoute(), id = Number(route.params.id)
const post = ref<CommunityPost | null>(null), comments = ref<CommunityComment[]>([]), userId = ref<number | null>(null)
const content = ref(''), loading = ref(true), busy = ref(false), error = ref('')
const owner = () => !!post.value && userId.value !== null && Number(post.value.userId) === userId.value
async function load() {
  try {
    const r = await getCommunityPost(id); if (r.code !== 200 || !r.data) throw new Error(r.message || '帖子不存在或不可见')
    post.value = r.data
    const [u, c] = await Promise.all([getCurrentUser().catch(() => null), listCommunityComments(id)])
    userId.value = u?.data?.data?.id ?? null; comments.value = c.data || []
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
}
async function submit() {
  error.value = ''; if (!content.value.trim()) return
  busy.value = true
  try {
    const r = await createCommunityComment(id, content.value.trim()); if (r.code !== 200) throw new Error(r.message || '评论失败')
    content.value = ''; comments.value = (await listCommunityComments(id)).data || []
  } catch (e) { error.value = e instanceof Error ? e.message : '评论失败' }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
  <AppNav /><main class="container narrow">
    <div v-if="loading" class="state-panel">正在加载……</div><div v-else-if="error && !post" class="state-panel state-error">{{ error }}</div>
    <template v-else-if="post"><article class="panel"><div class="page-heading"><h1>{{ post.title }}</h1><StatusBadge :status="post.auditStatus" /></div>
      <p v-if="post.moderationReason" class="muted">审核或隐藏原因：{{ post.moderationReason }}</p><p class="muted">{{ post.authorName || '宠友' }} · {{ post.createdAt.replace('T', ' ') }}</p><p class="post-content">{{ post.content }}</p>
      <RecordImages :path="`/api/community/posts/${id}`" :alt="post.title" /><RouterLink v-if="userId && !owner()" class="button button-light" :to="{ path: '/messages', query: { to: post.userId } }">私信作者</RouterLink><RouterLink v-if="owner()" class="button button-light" :to="`/community/posts/${id}/edit`">编辑帖子</RouterLink>
    </article><section class="panel"><h2>评论（{{ comments.length }}）</h2>
      <form v-if="post.auditStatus === 'APPROVED' && post.visibilityStatus === 'VISIBLE'" class="form-stack" @submit.prevent="submit"><label>写下评论<textarea v-model="content" rows="3" maxlength="2000" required></textarea></label>
        <p v-if="error" class="form-error">{{ error }}</p><button class="button" :disabled="busy">{{ busy ? '发送中……' : '发表评论' }}</button></form>
      <p v-if="!comments.length" class="muted">还没有评论。</p><article v-for="comment in comments" :key="comment.id" class="list-row">
        <div><p>{{ comment.content }}</p><small class="muted">用户 {{ comment.userId }} · {{ comment.createdAt }}</small><RouterLink v-if="userId && userId !== comment.userId" class="text-link" :to="{ path: '/messages', query: { to: comment.userId } }">私信</RouterLink></div>
      </article>
    </section></template>
  </main>
</template>
