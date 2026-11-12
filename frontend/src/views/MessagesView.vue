<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import AiChatPanel from '../components/AiChatPanel.vue'
import { getCurrentUser } from '../api/user'
import {
  getUnreadCounts, getNotifications, readNotification, readAllNotifications, getConversations, findRecipient, getRecipient,
  getThread, sendDirectMessage, readConversation,
  type Notice, type Peer, type Conversation, type DirectMessage
} from '../api/messages'

const route = useRoute(), router = useRouter()
const tab = ref<'notifications' | 'private' | 'ai'>('notifications')
const userId = ref<number | null>(null)
const notices = ref<Notice[]>([]), conversations = ref<Conversation[]>([])
const noticePage = ref(1), noticePages = ref(0), noticeTotal = ref(0)
const conversationPage = ref(1), conversationPages = ref(0)
const selected = ref<Peer | null>(null), messages = ref<DirectMessage[]>([])
const readingAll = ref(false)
const loading = ref(false), conversationLoading = ref(false), threadLoading = ref(false)
const sending = ref(false), searching = ref(false), syncing = ref(false), olderLoading = ref(false)
const error = ref(''), threadError = ref(''), searchName = ref(''), draft = ref('')
const hasOlder = ref(false), threadBody = ref<HTMLElement | null>(null)
const unreadNotices = ref(0)
let timer: ReturnType<typeof setInterval> | undefined
let noticeVersion = 0, conversationVersion = 0, peerVersion = 0, lastSyncId = 0
let destroyed = false

function report(e: unknown) {
  error.value = e instanceof Error ? e.message : '消息加载失败'
  if (!localStorage.getItem('token')) router.replace('/login')
}
async function loadNotices(quiet = false) {
  const version = ++noticeVersion
  if (!quiet) { loading.value = true; error.value = '' }
  try {
    const [result, counts] = await Promise.all([
      getNotifications(noticePage.value), getUnreadCounts()
    ])
    if (!result.data) throw new Error(result.message)
    if (version !== noticeVersion || destroyed) return
    notices.value = result.data.records
    unreadNotices.value = counts.data?.notifications || 0
    noticePages.value = result.data.totalPages
    noticeTotal.value = result.data.total
  } catch (e) {
    if (version === noticeVersion && !destroyed) report(e)
  } finally { if (version === noticeVersion) loading.value = false }
}
async function loadConversations(more = false, quiet = false) {
  const version = ++conversationVersion
  const requestedPage = more ? conversationPage.value + 1 : 1
  if (!quiet) conversationLoading.value = true
  try {
    const result = await getConversations(requestedPage)
    if (!result.data) throw new Error(result.message)
    if (version !== conversationVersion || destroyed) return
    const incoming = result.data.records
    const ids = new Set(incoming.map(item => item.userId))
    conversations.value = more
      ? [...conversations.value.filter(item => !ids.has(item.userId)), ...incoming]
      : [...incoming, ...conversations.value.filter(item => !ids.has(item.userId))]
    conversationPages.value = result.data.totalPages
    if (more) conversationPage.value = requestedPage
  } catch (e) {
    if (version === conversationVersion && !destroyed) report(e)
  } finally { if (version === conversationVersion) conversationLoading.value = false }
}
async function readAll() {
  if (readingAll.value) return
  readingAll.value = true
  try {
    const latest = await getNotifications(1)
    const maxId = latest.data?.records[0]?.id
    if (!maxId) return
    await readAllNotifications(maxId)
    noticeVersion++
    loading.value = false
    notices.value.forEach(item => { if (item.id <= maxId) item.isRead = true })
    unreadNotices.value = (await getUnreadCounts()).data?.notifications || 0
  } catch (e) { report(e) }
  finally { readingAll.value = false }
}

async function openNotice(notice: Notice) {
  try {
    await readNotification(notice.id)
    noticeVersion++
    notice.isRead = true
    const section = notice.targetType === 'LOST' ? 'lost' : 'found'
    if (notice.type === 'MODERATION') {
      await router.push({ path: '/user', query: { section } })
    } else {
      const prefix = { LOST: '/pet-lost/', FOUND: '/pet-found/', POST: '/community/posts/' }[notice.targetType]
      await router.push(prefix + notice.targetId)
    }
  } catch (e) { report(e) }
}
function choosePeer(id: number) {
  tab.value = 'private'
  if (selected.value?.id === id) return
  router.replace({ path: '/messages', query: { to: id } })
}
async function search() {
  if (!searchName.value.trim() || searching.value) return
  searching.value = true
  error.value = ''
  try {
    const result = await findRecipient(searchName.value.trim())
    if (!result.data) throw new Error(result.message)
    choosePeer(result.data.id)
    searchName.value = ''
  } catch (e) { report(e) }
  finally { searching.value = false }
}
function mergeMessages(incoming: DirectMessage[]) {
  const combined = new Map(messages.value.map(item => [item.id, item]))
  incoming.forEach(item => combined.set(item.id, item))
  messages.value = [...combined.values()].sort((a, b) => a.id - b.id)
}
async function scrollBottom() {
  await nextTick()
  threadBody.value?.scrollTo({ top: threadBody.value.scrollHeight })
}
async function openThread(id: number) {
  const version = ++peerVersion
  threadLoading.value = true
  selected.value = null
  messages.value = []
  lastSyncId = 0
  draft.value = ''
  threadError.value = ''
  tab.value = 'private'
  try {
    const [peer, data] = await Promise.all([getRecipient(id), getThread(id)])
    if (!peer.data || !data.data) throw new Error(peer.message || data.message)
    if (version !== peerVersion || destroyed) return
    selected.value = peer.data
    messages.value = data.data.records
    hasOlder.value = data.data.hasMore
    lastSyncId = messages.value.at(-1)?.id || 0
    await scrollBottom()
    if (lastSyncId) await readConversation(id, lastSyncId)
    await loadConversations(false, true)
  } catch (e) {
    if (version === peerVersion && !destroyed) threadError.value = e instanceof Error ? e.message : '对话加载失败'
  } finally { if (version === peerVersion) threadLoading.value = false }
}
async function syncThread(forceScroll = false) {
  const peer = selected.value
  if (!peer || syncing.value || threadLoading.value) return
  const version = peerVersion
  syncing.value = true
  try {
    let more = true
    const nearBottom = !threadBody.value
      || threadBody.value.scrollHeight - threadBody.value.scrollTop - threadBody.value.clientHeight < 90
    while (more && version === peerVersion) {
      const initial = lastSyncId === 0
      const result = await getThread(peer.id, initial ? {} : { afterId: lastSyncId })
      if (!result.data) throw new Error(result.message)
      if (version !== peerVersion || destroyed) return
      mergeMessages(result.data.records)
      if (!lastSyncId) hasOlder.value = result.data.hasMore
      lastSyncId = Math.max(lastSyncId, result.data.records.at(-1)?.id || 0)
      more = !initial && !!result.data.records.length && result.data.hasMore
      // The initial latest page's hasMore points backwards, so it is loaded by the history button.
      if (!messages.value.length) more = false
      if (nearBottom || forceScroll) await scrollBottom()
    }
    if (lastSyncId && version === peerVersion) await readConversation(peer.id, lastSyncId)
  } catch (e) {
    if (version === peerVersion && !destroyed) threadError.value = e instanceof Error ? e.message : '同步消息失败'
  } finally { syncing.value = false }
}
async function loadOlder() {
  const peer = selected.value, firstId = messages.value[0]?.id
  if (!peer || !firstId || olderLoading.value) return
  const version = peerVersion
  olderLoading.value = true
  const height = threadBody.value?.scrollHeight || 0
  const top = threadBody.value?.scrollTop || 0
  try {
    const result = await getThread(peer.id, { beforeId: firstId })
    if (!result.data) throw new Error(result.message)
    if (version !== peerVersion || destroyed) return
    mergeMessages(result.data.records)
    hasOlder.value = result.data.hasMore
    await nextTick()
    if (threadBody.value) threadBody.value.scrollTop = top + threadBody.value.scrollHeight - height
  } catch (e) { if (version === peerVersion) threadError.value = e instanceof Error ? e.message : '历史消息加载失败' }
  finally { olderLoading.value = false }
}
async function send() {
  const peer = selected.value
  const content = draft.value.trim()
  if (!peer || !content || sending.value) return
  const version = peerVersion
  sending.value = true
  threadError.value = ''
  try {
    const result = await sendDirectMessage(peer.id, content)
    if (!result.data) throw new Error(result.message)
    if (version !== peerVersion || destroyed) return
    draft.value = ''
    mergeMessages([result.data])
    await scrollBottom()
    await syncThread(true)
    await loadConversations(false, true)
  } catch (e) {
    if (version === peerVersion) threadError.value = e instanceof Error ? e.message : '消息发送失败'
  } finally { sending.value = false }
}
async function chooseTab(value: 'notifications' | 'private' | 'ai') {
  tab.value = value
  if (value === 'notifications') await loadNotices()
  if (value === 'private') await Promise.all([loadConversations(), syncThread()])
}
async function refreshCurrent() {
  error.value = ''
  if (tab.value === 'notifications') await loadNotices()
  if (tab.value === 'private') await Promise.all([loadConversations(), syncThread()])
}
function typeLabel(value: string) {
  return ({ CLUE: '寻宠线索', RECORD_COMMENT: '信息留言', COMMENT: '社区互动',
    AUDIT: '审核通知', MODERATION: '管理通知' } as Record<string, string>)[value] || '互动通知'
}
function moveNotices(delta: number) { noticePage.value += delta; loadNotices() }
watch(() => route.query.to, value => {
  if (value === undefined) return
  const id = Number(value)
  if (Number.isSafeInteger(id) && id > 0) openThread(id)
  else error.value = '请选择有效的私信用户'
}, { immediate: true })
onMounted(async () => {
  try { userId.value = (await getCurrentUser()).data.data?.id || null } catch (e) { report(e) }
  await Promise.all([loadNotices(), loadConversations()])
  timer = setInterval(async () => {
    if (document.hidden) return
    if (tab.value === 'notifications' && noticePage.value === 1) await loadNotices(true)
    if (tab.value === 'private') await Promise.all([loadConversations(false, true), syncThread()])
  }, 10000)
})
onBeforeUnmount(() => {
  destroyed = true
  peerVersion++
  noticeVersion++
  conversationVersion++
  if (timer) clearInterval(timer)
})
</script>

<template>
  <AppNav />
  <main class="container messages-page">
    <header class="page-heading"><div><p class="eyebrow">STAY CONNECTED</p><h1>消息中心</h1><p class="muted">收下每一份回应，让寻宠与交流更近一点。</p></div><button v-if="tab !== 'ai'" class="button button-light button-small" @click="refreshCurrent">刷新消息</button></header>
    <nav class="message-tabs" aria-label="消息分类">
      <button :class="{ active: tab === 'notifications' }" @click="chooseTab('notifications')">✉ 互动通知 <b v-if="unreadNotices">{{ unreadNotices }}</b></button>
      <button :class="{ active: tab === 'private' }" @click="chooseTab('private')">♧ 私信会话</button>
      <button :class="{ active: tab === 'ai' }" @click="chooseTab('ai')">✧ AI 助手</button>
    </nav>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <section v-if="tab === 'notifications'" class="panel notification-panel">
      <div class="page-heading"><h2>互动通知 <small class="muted">共 {{ noticeTotal }} 条</small></h2><button class="text-button" :disabled="!notices.length || readingAll" @click="readAll">全部标为已读</button></div>
      <p v-if="loading" class="muted">正在加载通知……</p>
      <div v-else-if="!notices.length" class="message-empty"><span aria-hidden="true">✉</span><h3>新的回应会出现在这里</h3><p class="muted">走失和招领留言、寻宠线索、社区评论及审核结果都会通知你。</p></div>
      <article v-for="notice in notices" :key="notice.id" class="notification-row" :class="{ unread: !notice.isRead }">
        <div class="notice-symbol" aria-hidden="true">{{ notice.type === 'CLUE' ? '⌕' : notice.type === 'AUDIT' ? '✓' : '✉' }}</div>
        <div class="grow"><div class="card-title-row"><b>{{ notice.actorName }} <small class="muted">{{ typeLabel(notice.type) }}</small></b><span v-if="!notice.isRead" class="unread-dot" aria-label="未读"></span></div><p>{{ notice.content }}</p><small class="muted">{{ notice.createdAt.replace('T', ' ') }}</small>
          <div class="notice-actions"><button class="text-button" @click="openNotice(notice)">查看详情 →</button><button v-if="notice.actorId !== userId" class="text-button" @click="choosePeer(notice.actorId)">联系对方</button></div>
        </div>
      </article>
      <div v-if="noticePages > 1" class="pagination"><button class="button button-light button-small" :disabled="noticePage <= 1 || loading" @click="moveNotices(-1)">上一页</button><span>{{ noticePage }} / {{ noticePages }}</span><button class="button button-light button-small" :disabled="noticePage >= noticePages || loading" @click="moveNotices(1)">下一页</button></div>
    </section>
    <section v-if="tab === 'private'" class="private-layout">
      <aside class="conversation-sidebar">
        <div class="conversation-search"><h2>我的私信</h2><form @submit.prevent="search"><label class="sr-only" for="recipient-name">对方用户名</label><input id="recipient-name" v-model="searchName" maxlength="50" required placeholder="输入用户名发起私信"><button class="button button-small" :disabled="searching">{{ searching ? '查找中' : '发起' }}</button></form></div>
        <p v-if="conversationLoading" class="muted conversation-state">会话加载中……</p>
        <p v-else-if="!conversations.length" class="muted conversation-state">还没有会话。你可以输入用户名，或从信息详情页联系发布者。</p>
        <button v-for="conversation in conversations" :key="conversation.userId" class="conversation-item" :class="{ active: selected?.id === conversation.userId }" @click="choosePeer(conversation.userId)">
          <span class="member-avatar">{{ conversation.username.charAt(0).toUpperCase() }}</span><span class="grow"><b>{{ conversation.username }}</b><small>{{ conversation.preview }}</small></span><span v-if="conversation.unreadCount" class="conversation-unread">{{ conversation.unreadCount }}</span>
        </button>
        <button v-if="conversationPage < conversationPages" class="text-button conversation-more" :disabled="conversationLoading" @click="loadConversations(true)">加载更多会话</button>
      </aside>
      <section class="private-thread">
        <p v-if="threadLoading" class="muted conversation-state">正在打开对话……</p>
        <template v-else-if="selected">
          <header class="chat-heading"><span class="member-avatar">{{ selected.username.charAt(0).toUpperCase() }}</span><div class="grow"><h2>{{ selected.username }}</h2><small class="muted">{{ selected.status === 'ACTIVE' ? '私信对话' : '对方账号已停用' }}</small></div></header>
          <div ref="threadBody" class="chat-body" aria-live="polite">
            <button v-if="hasOlder" class="history-button" :disabled="olderLoading" @click="loadOlder">{{ olderLoading ? '加载中……' : '查看更早消息' }}</button>
            <p v-if="!messages.length" class="muted thread-start">从一句问候开始交流吧。</p>
            <div v-for="message in messages" :key="message.id" class="message-line" :class="{ mine: message.senderId === userId }"><div><div class="message-bubble">{{ message.content }}</div><small>{{ message.createdAt.replace('T', ' ') }}</small></div></div>
          </div>
          <form class="chat-composer" @submit.prevent="send"><label class="sr-only" for="direct-input">私信内容</label><textarea id="direct-input" v-model="draft" rows="3" maxlength="2000" :disabled="sending || selected.status !== 'ACTIVE'" :placeholder="selected.status === 'ACTIVE' ? '输入想对对方说的话……' : '暂时无法向该账号发送消息'"></textarea><div class="composer-bottom"><small class="muted">{{ draft.length }} / 2000</small><button class="button button-small" :disabled="sending || !draft.trim() || selected.status !== 'ACTIVE'">{{ sending ? '发送中……' : '发送私信 ↑' }}</button></div></form>
        </template>
        <div v-else class="message-empty"><span aria-hidden="true">♧</span><h3>选一位宠友，开始交流</h3><p class="muted">仅对话双方可以查看私信内容。</p></div>
        <p v-if="threadError" class="form-error" role="alert">{{ threadError }}</p>
      </section>
    </section>
    <div v-show="tab === 'ai'" class="panel ai-wrapper"><AiChatPanel /></div>
  </main>
</template>
