<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppNav from '../components/AppNav.vue'
import RecordImages from '../components/RecordImages.vue'
import StatusBadge from '../components/StatusBadge.vue'
import {
  listCommunityPosts, listMyCommunityPosts, postCategories, type CommunityPost, type PostCategory
} from '../api/community'

const posts = ref<CommunityPost[]>([])
const page = ref(1), totalPages = ref(0), total = ref(0)
const loading = ref(false), error = ref('')
const mine = ref(false), category = ref<PostCategory | undefined>()
let requestVersion = 0

async function choose(value?: PostCategory, own = false) {
  category.value = value
  mine.value = own
  page.value = 1
  await load()
}
async function load() {
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const result = mine.value
      ? await listMyCommunityPosts(page.value)
      : await listCommunityPosts(page.value, 10, category.value)
    if (result.code !== 200 || !result.data) throw new Error(result.message || '加载社区失败')
    if (version !== requestVersion) return
    posts.value = result.data.records
    total.value = result.data.total
    totalPages.value = result.data.totalPages
  } catch (e) {
    if (version === requestVersion) error.value = e instanceof Error ? e.message : '加载社区失败'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}
function label(value: PostCategory) {
  return postCategories.find(item => item.value === value)?.label || '分享'
}
function date(value: string) {
  return value.slice(0, 10).replaceAll('-', '/')
}
function move(delta: number) {
  page.value += delta
  load()
}
onMounted(load)
</script>

<template>
  <AppNav />
  <main class="container community-container">
    <header class="community-heading">
      <div><p class="eyebrow">PAWFIND COMMUNITY</p><h1>宠友社区</h1><p class="muted">分享养宠日常，记录你和它的小故事。</p></div>
      <button class="button button-light" :class="{ selected: mine }" @click="choose(undefined, !mine)">{{ mine ? '返回社区' : '我的动态' }}</button>
    </header>
    <nav class="community-tabs" aria-label="帖子分类">
      <button :class="{ active: !mine && !category }" :aria-pressed="!mine && !category" @click="choose()">全部</button>
      <button v-for="item in postCategories" :key="item.value" :class="{ active: !mine && category === item.value }" :aria-pressed="!mine && category === item.value" @click="choose(item.value)">{{ item.label }}</button>
    </nav>
    <p v-if="mine" class="muted">我的动态 · 包含待审核、已通过和已驳回的帖子</p>
    <div v-if="loading" class="state-panel">正在加载社区动态……</div>
    <div v-else-if="error" class="state-panel state-error">{{ error }} <button class="button button-light" @click="load">重试</button></div>
    <div v-else-if="!posts.length" class="state-panel community-empty"><div class="empty-icon">♧</div><h2>{{ mine ? '写下你的第一篇动态' : '这里还在等待第一篇故事' }}</h2><p>日常分享、寻宠求助、养宠疑问，都可以从这里开始。</p><RouterLink class="button" to="/community/create">发布动态</RouterLink></div>
    <template v-else>
      <div class="community-feed">
        <article v-for="post in posts" :key="post.id" class="feed-card">
          <header class="feed-author">
            <div class="author-avatar" aria-hidden="true">{{ (post.authorName || '宠友').charAt(0).toUpperCase() }}</div>
            <div class="grow"><b>{{ post.authorName || '宠友 ' + post.userId }}</b><small>{{ date(post.createdAt) }}</small></div>
            <span class="category-chip">{{ label(post.category) }}</span>
          </header>
          <RouterLink class="feed-title" :to="`/community/posts/${post.id}`">{{ post.title }}</RouterLink>
          <p class="feed-text">{{ post.content }}</p>
          <RecordImages :path="`/api/community/posts/${post.id}`" :alt="post.title" />
          <div class="feed-tags"><span>#{{ label(post.category) }}</span><StatusBadge v-if="mine" :status="post.visibilityStatus === 'HIDDEN' ? 'HIDDEN' : post.auditStatus" /></div>
          <p v-if="mine && post.moderationReason" class="muted">处理原因 · {{ post.moderationReason }}</p>
          <footer class="feed-footer"><span>♧ {{ post.commentCount || 0 }} 条评论</span><RouterLink :to="`/community/posts/${post.id}`">{{ mine ? '查看与管理' : '进入讨论' }} →</RouterLink></footer>
        </article>
      </div>
      <div v-if="totalPages > 1" class="pagination"><button class="button button-light" :disabled="page <= 1" @click="move(-1)">上一页</button><span>{{ page }} / {{ totalPages }} · {{ total }} 篇</span><button class="button button-light" :disabled="page >= totalPages" @click="move(1)">下一页</button></div>
    </template>
    <RouterLink class="floating-compose" to="/community/create" aria-label="发布社区动态"><span aria-hidden="true">＋</span><span>发布动态</span></RouterLink>
  </main>
</template>
