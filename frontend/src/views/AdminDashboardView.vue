<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import RecordImages from '../components/RecordImages.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import {
  auditPost, listAdminUsers, listOperationLogs, listAdminPosts, setPostVisibility, updateUserStatus,
  listManagedPets, setManagedPetVisibility, listManagedComments, setCommentVisibility,
  type AdminOperationLog, type AdminUser, type ManagedPetRecord, type ManagedComment
} from '../api/admin'
import type { AuditStatus, CommunityPost, VisibilityStatus } from '../api/community'
import type { PageData } from '../types/common'
import { getCurrentUser, type UserInfo } from '../api/user'

type Action = { title: string; description: string; reason: boolean; run: (reason: string) => Promise<unknown> }
const route = useRoute(), router = useRouter()
const section = computed(() => String(route.params.section || 'lost'))
const headings: Record<string, { title: string; text: string }> = {
  lost: { title: '走失管理', text: '查看用户发布的走失信息，处理违规内容。' },
  found: { title: '招领管理', text: '查看捡到宠物的招领信息，维护公开展示。' },
  community: { title: '社区管理', text: '审核帖子、管理评论，记录每一次处理。' },
  users: { title: '用户管理', text: '查看账号状态，停用或解封用户。' },
  logs: { title: '操作日志', text: '查阅审核、内容管理及账号处理记录。' },
  account: { title: '用户中心', text: '管理员账号信息。' }
}
const heading = computed(() => headings[section.value] || headings.lost!)
const petKind = computed(() => section.value === 'found' ? 'found' as const : 'lost' as const)
const account = ref<UserInfo | null>(null)
const pets = ref<ManagedPetRecord[]>([]), posts = ref<CommunityPost[]>([])
const comments = ref<ManagedComment[]>([]), users = ref<AdminUser[]>([]), logs = ref<AdminOperationLog[]>([])
const loading = ref(false), error = ref(''), busy = ref(false), actionError = ref('')
const page = ref(1), totalPages = ref(0), total = ref(0)
const auditFilter = ref<AuditStatus | ''>('PENDING')
const visibilityFilter = ref<VisibilityStatus | ''>('')
const communityTab = ref<'posts' | 'comments'>('posts')
const action = ref<Action | null>(null)
let loadVersion = 0

async function load() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''
  try {
    let data: PageData<unknown> | null = null
    if (section.value === 'lost' || section.value === 'found') {
      const result = await listManagedPets(petKind.value, page.value, visibilityFilter.value || undefined)
      data = result.data
      if (version === loadVersion) pets.value = result.data?.records || []
    } else if (section.value === 'community' && communityTab.value === 'posts') {
      const result = await listAdminPosts(auditFilter.value || undefined, page.value, 10, visibilityFilter.value || undefined)
      data = result.data
      if (version === loadVersion) posts.value = result.data?.records || []
    } else if (section.value === 'community') {
      const result = await listManagedComments(page.value, visibilityFilter.value || undefined)
      data = result.data
      if (version === loadVersion) comments.value = result.data?.records || []
    } else if (section.value === 'users') {
      const result = await listAdminUsers(page.value, 10)
      data = result.data
      if (version === loadVersion) users.value = result.data?.records || []
    } else if (section.value === 'logs') {
      const result = await listOperationLogs(page.value, 10)
      data = result.data
      if (version === loadVersion) logs.value = result.data?.records || []
    }
    if (version !== loadVersion) return
    total.value = data?.total || 0
    totalPages.value = data?.totalPages || 0
  } catch (e) {
    if (version === loadVersion) error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
function resetPage() {
  page.value = 1
  load()
}
function showAction(title: string, description: string, reason: boolean, run: Action['run']) {
  actionError.value = ''
  action.value = { title, description, reason, run }
}
async function confirmAction(reason: string) {
  if (!action.value || busy.value) return
  busy.value = true
  actionError.value = ''
  try {
    await action.value.run(reason)
    action.value = null
    await load()
  } catch (e) {
    actionError.value = e instanceof Error ? e.message : '处理失败'
  } finally {
    busy.value = false
  }
}
function moderatePet(record: ManagedPetRecord) {
  const status = record.visibilityStatus === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  const kind = petKind.value
  showAction(status === 'HIDDEN' ? '隐藏这条信息' : '恢复这条信息',
    '处理对象为「' + (record.petName || '未命名宠物') + '」，操作将写入日志。',
    status === 'HIDDEN', reason => setManagedPetVisibility(kind, record.id, status, reason))
}
function review(post: CommunityPost, status: 'APPROVED' | 'REJECTED') {
  showAction(status === 'APPROVED' ? '通过帖子审核' : '驳回帖子',
    '正在审核「' + post.title + '」，作者会收到审核结果。', status === 'REJECTED',
    reason => auditPost(post.id, status, reason))
}
function moderatePost(post: CommunityPost) {
  const status = post.visibilityStatus === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  showAction(status === 'HIDDEN' ? '隐藏帖子' : '恢复帖子', post.title, status === 'HIDDEN',
    reason => setPostVisibility(post.id, status, reason))
}
function moderateComment(comment: ManagedComment) {
  const status = comment.visibilityStatus === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  showAction(status === 'HIDDEN' ? '隐藏评论' : '恢复评论', comment.content, status === 'HIDDEN',
    reason => setCommentVisibility(comment.id, status, reason))
}
function toggleUser(user: AdminUser) {
  const status = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  showAction(status === 'DISABLED' ? '停用用户' : '解封用户', '账号「' + user.username + '」的状态将被更新。', false,
    () => updateUserStatus(user.id, status))
}
function operation(value: string) {
  return ({ APPROVE: '审核通过', REJECT: '审核驳回', HIDE: '隐藏内容', RESTORE: '恢复内容',
    DISABLE: '停用账号', ENABLE: '解封账号' } as Record<string, string>)[value] || value
}
function target(value: string) {
  return ({ COMMUNITY_POST: '社区帖子', COMMUNITY_COMMENT: '社区评论', PET_LOST: '走失信息',
    PET_FOUND: '招领信息', USER: '用户账号' } as Record<string, string>)[value] || value
}
function move(delta: number) {
  page.value += delta
  load()
}
function logout() {
  localStorage.removeItem('token')
  router.replace('/login')
}
watch(section, () => {
  action.value = null
  page.value = 1
  auditFilter.value = 'PENDING'
  visibilityFilter.value = ''
  communityTab.value = 'posts'
  load()
}, { immediate: true })
onMounted(async () => {
  try { account.value = (await getCurrentUser()).data.data }
  catch (e) { error.value = e instanceof Error ? e.message : '账号信息加载失败' }
})
onBeforeUnmount(() => { loadVersion++ })
</script>

<template>
  <AppNav />
  <main class="container management-page">
    <header class="management-heading">
      <div><p class="eyebrow">PAWFIND MANAGEMENT</p><h1>{{ heading.title }}</h1><p class="muted">{{ heading.text }}</p></div>
      <span class="management-identity"><i></i>{{ account?.username || '管理员' }}</span>
    </header>
    <div v-if="section === 'community'" class="tab-row"><button :class="{ active: communityTab === 'posts' }" @click="communityTab = 'posts'; resetPage()">帖子管理</button><button :class="{ active: communityTab === 'comments' }" @click="communityTab = 'comments'; resetPage()">评论管理</button></div>
    <div v-if="['lost', 'found', 'community'].includes(section)" class="management-filters">
      <label v-if="section === 'community' && communityTab === 'posts'">审核状态<select v-model="auditFilter" @change="resetPage"><option value="">全部审核状态</option><option value="PENDING">待审核</option><option value="APPROVED">已通过</option><option value="REJECTED">已驳回</option></select></label>
      <label>展示状态<select v-model="visibilityFilter" @change="resetPage"><option value="">全部展示状态</option><option value="VISIBLE">公开</option><option value="HIDDEN">已隐藏</option></select></label>
      <span class="muted">共 {{ total }} 条记录</span>
    </div>
    <p v-if="error" class="form-error" role="alert">{{ error }} <button class="text-button" @click="load">重试</button></p>
    <div v-if="loading" class="state-panel">正在加载管理数据……</div>
    <template v-else-if="section === 'lost' || section === 'found'">
      <div v-if="!pets.length" class="state-panel">暂无符合条件的信息。</div>
      <article v-for="pet in pets" :key="pet.id" class="panel managed-pet">
        <div class="managed-thumbnail"><RecordImages :path="`/api/pet-${petKind}/${pet.id}`" cover :alt="pet.petName || '宠物照片'" /></div>
        <div class="grow"><div class="card-title-row"><h2>{{ pet.petName || '未命名宠物' }} <small class="muted">{{ pet.species }}</small></h2><div class="badge-row"><StatusBadge :status="pet.status" /><StatusBadge :status="pet.visibilityStatus" /></div></div>
          <p>📍 {{ pet.lostLocation || pet.foundLocation }}</p><p class="muted">发布者 #{{ pet.userId }} · {{ pet.createdAt.replace('T', ' ') }}</p>
          <details><summary>查看信息描述</summary><p class="post-content">{{ pet.description || '暂无描述' }}</p></details>
          <p v-if="pet.moderationReason" class="moderation-reason">处理原因 · {{ pet.moderationReason }}</p>
          <div class="button-row"><button class="button button-small" :class="pet.visibilityStatus === 'HIDDEN' ? 'button-light' : 'button-danger'" @click="moderatePet(pet)">{{ pet.visibilityStatus === 'HIDDEN' ? '恢复展示' : '隐藏信息' }}</button></div>
        </div>
      </article>
    </template>
    <section v-else-if="section === 'community'" class="panel">
      <template v-if="communityTab === 'posts'">
        <p v-if="!posts.length" class="muted">暂无符合条件的帖子。</p>
        <article v-for="post in posts" :key="post.id" class="management-row">
          <div class="grow"><div class="card-title-row"><h3>{{ post.title }}</h3><div class="badge-row"><StatusBadge :status="post.auditStatus" /><StatusBadge :status="post.visibilityStatus" /></div></div>
            <p class="muted">作者 #{{ post.userId }} · {{ post.createdAt.replace('T', ' ') }}</p>
            <details><summary>查看正文和配图</summary><p class="post-content">{{ post.content }}</p><RecordImages :path="`/api/community/posts/${post.id}`" :alt="post.title" /></details>
            <p v-if="post.moderationReason" class="moderation-reason">{{ post.moderationReason }}</p>
            <div v-if="post.auditStatus === 'PENDING'" class="button-row"><button class="button button-small" @click="review(post, 'APPROVED')">审核通过</button><button class="button button-small button-danger" @click="review(post, 'REJECTED')">驳回帖子</button></div>
            <button v-else-if="post.auditStatus === 'APPROVED'" class="button button-small button-light" @click="moderatePost(post)">{{ post.visibilityStatus === 'HIDDEN' ? '恢复展示' : '隐藏帖子' }}</button>
          </div>
        </article>
      </template>
      <template v-else>
        <p v-if="!comments.length" class="muted">暂无符合条件的评论。</p>
        <article v-for="comment in comments" :key="comment.id" class="management-row">
          <div class="grow"><div class="card-title-row"><b>帖子 #{{ comment.postId }} · 用户 #{{ comment.userId }}</b><StatusBadge :status="comment.visibilityStatus" /></div>
            <p class="post-content">{{ comment.content }}</p><small class="muted">{{ comment.createdAt.replace('T', ' ') }}</small></div>
          <button v-if="comment.visibilityStatus !== 'DELETED'" class="button button-small button-light" @click="moderateComment(comment)">{{ comment.visibilityStatus === 'HIDDEN' ? '恢复评论' : '隐藏评论' }}</button>
        </article>
      </template>
    </section>
    <section v-else-if="section === 'users'" class="panel">
      <p v-if="!users.length" class="muted">暂无用户。</p>
      <article v-for="user in users" :key="user.id" class="management-row">
        <div class="member-avatar">{{ user.username.charAt(0).toUpperCase() }}</div>
        <div class="grow"><h3>{{ user.username }}</h3><p class="muted">ID {{ user.id }} · {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}</p><StatusBadge :status="user.status" /></div>
        <button class="button button-small" :class="user.status === 'ACTIVE' ? 'button-danger' : 'button-light'" :disabled="user.id === account?.id" @click="toggleUser(user)">{{ user.id === account?.id ? '当前账号' : user.status === 'ACTIVE' ? '停用用户' : '解封用户' }}</button>
      </article>
    </section>
    <section v-else-if="section === 'logs'" class="panel">
      <p v-if="!logs.length" class="muted">暂无操作日志。</p>
      <article v-for="log in logs" :key="log.id" class="management-row log-row"><span class="log-marker" aria-hidden="true">✓</span><div class="grow"><h3>{{ operation(log.operation) }}</h3><p>{{ target(log.targetType) }} #{{ log.targetId }}<span v-if="log.detail"> · {{ log.detail }}</span></p><small class="muted">管理员 #{{ log.adminId }} · {{ log.createdAt.replace('T', ' ') }}</small></div></article>
    </section>
    <section v-else-if="account" class="panel admin-account">
      <div class="profile-identity"><div class="profile-avatar">{{ account.username.charAt(0).toUpperCase() }}</div><div><h2>{{ account.username }}</h2><span class="status-badge" data-status="ACTIVE">管理员账号</span></div></div>
      <div class="account-details"><div><span>用户 ID</span><b>{{ account.id }}</b></div><div><span>用户名</span><b>{{ account.username }}</b></div><div><span>账号角色</span><b>管理员</b></div></div>
      <button class="profile-logout" @click="logout">⏻ 退出登录</button>
    </section>
    <div v-if="totalPages > 1 && !loading" class="pagination"><button class="button button-light" :disabled="page <= 1" @click="move(-1)">上一页</button><span>{{ page }} / {{ totalPages }} · {{ total }} 条</span><button class="button button-light" :disabled="page >= totalPages" @click="move(1)">下一页</button></div>
    <ConfirmDialog :open="!!action" :title="action?.title || ''" :description="action?.description || ''" :require-reason="action?.reason" :busy="busy" :error="actionError" @cancel="action = null" @confirm="confirmAction" />
  </main>
</template>
