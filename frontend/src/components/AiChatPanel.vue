<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  deleteAiSession, getAiMessages, getAiSessions, getAiStatus, streamAiReply,
  type AiAssistantType, type AiMessage, type AiSession
} from '../api/ai'

const messages = ref<AiMessage[]>([]), sessions = ref<AiSession[]>([])
const input = ref(''), error = ref(''), busy = ref(false)
const configured = ref(false), statusText = ref('正在连接助手……'), modelText = ref('')
const activeSessionId = ref<number | null>(null)
const loadingHistory = ref(false), loadingSessions = ref(false)
const assistantType = ref<AiAssistantType>('GENERAL')
const chatBody = ref<HTMLElement | null>(null)
let controller: AbortController | null = null

const modes: Array<{ value: AiAssistantType; label: string }> = [
  { value: 'GENERAL', label: '日常咨询' },
  { value: 'LOST_INFO', label: '资料整理' },
  { value: 'NOTICE_DRAFT', label: '启事草稿' },
  { value: 'PET_QA', label: '养宠问答' }
]
const suggestions: Array<{ type: AiAssistantType; text: string }> = [
  { type: 'NOTICE_DRAFT', text: '帮我生成一份可编辑的寻宠启事' },
  { type: 'LOST_INFO', text: '帮我检查寻宠资料还缺少什么' },
  { type: 'PET_QA', text: '宠物走失后应该先做什么？' }
]

async function scroll() {
  await nextTick()
  chatBody.value?.scrollTo({ top: chatBody.value.scrollHeight, behavior: 'smooth' })
}

async function refreshSessions() {
  loadingSessions.value = true
  try {
    const result = await getAiSessions()
    if (!result.data) throw new Error(result.message)
    sessions.value = result.data
  } catch (e) {
    error.value = e instanceof Error ? e.message : '会话列表加载失败'
  } finally { loadingSessions.value = false }
}

async function openSession(sessionId: number) {
  if (busy.value || activeSessionId.value === sessionId && messages.value.length) return
  loadingHistory.value = true
  error.value = ''
  try {
    const result = await getAiMessages(sessionId)
    if (!result.data) throw new Error(result.message)
    activeSessionId.value = sessionId
    messages.value = result.data
    await scroll()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '聊天记录加载失败'
  } finally { loadingHistory.value = false }
}

function newChat() {
  if (busy.value) return
  activeSessionId.value = null
  messages.value = []
  input.value = ''
  error.value = ''
}

async function removeSession(session: AiSession) {
  if (busy.value || !window.confirm(`确定删除会话“${session.title}”吗？删除后无法恢复。`)) return
  try {
    await deleteAiSession(session.id)
    if (activeSessionId.value === session.id) newChat()
    await refreshSessions()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '删除会话失败'
  }
}

function useSuggestion(item: { type: AiAssistantType; text: string }) {
  assistantType.value = item.type
  input.value = item.text
}

async function send() {
  if (!configured.value || !input.value.trim() || busy.value) return
  const content = input.value.trim()
  input.value = ''
  error.value = ''
  messages.value.push({ role: 'user', content })
  const replyIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  busy.value = true
  controller = new AbortController()
  await scroll()
  try {
    await streamAiReply(
      { sessionId: activeSessionId.value, content, assistantType: assistantType.value },
      delta => {
        const reply = messages.value[replyIndex]
        if (reply) reply.content += delta
        scroll()
      },
      sessionId => { activeSessionId.value = sessionId },
      controller.signal
    )
  } catch (e) {
    const reply = messages.value[replyIndex]
    if (!reply?.content) messages.value.splice(replyIndex, 1)
    if (!(e instanceof DOMException && e.name === 'AbortError')) {
      error.value = e instanceof Error ? e.message : 'AI 回复失败，请重试'
    }
  } finally {
    busy.value = false
    controller = null
    await refreshSessions()
  }
}

onMounted(async () => {
  try {
    const result = await getAiStatus()
    configured.value = result.data?.configured || false
    statusText.value = result.data?.message || '助手暂时无法连接'
    modelText.value = result.data?.model || ''
    await refreshSessions()
    if (sessions.value[0]) await openSession(sessions.value[0].id)
  } catch { statusText.value = '助手暂时无法连接' }
})
onBeforeUnmount(() => controller?.abort())
</script>

<template>
  <section class="ai-chat-panel ai-chat-layout">
    <aside class="ai-session-sidebar">
      <div class="ai-session-title"><b>历史会话</b><button class="button button-small" :disabled="busy" @click="newChat">＋ 新对话</button></div>
      <p v-if="loadingSessions" class="muted conversation-state">正在加载……</p>
      <p v-else-if="!sessions.length" class="muted conversation-state">发送第一条消息后，会话会保存在这里。</p>
      <div v-else class="ai-session-list">
        <button v-for="session in sessions" :key="session.id" class="ai-session-item" :class="{ active: activeSessionId === session.id }" :disabled="busy" @click="openSession(session.id)">
          <span><b>{{ session.title }}</b><small>{{ session.updatedAt.replace('T', ' ') }}</small></span>
          <i role="button" tabindex="0" aria-label="删除会话" @click.stop="removeSession(session)" @keydown.enter.stop="removeSession(session)">×</i>
        </button>
      </div>
    </aside>

    <div class="ai-chat-main">
      <header class="chat-heading"><div class="assistant-avatar" aria-hidden="true">✧</div><div class="grow"><h2>AI 寻宠助手</h2><small class="muted">{{ configured ? `在线 · DeepSeek ${modelText}` : statusText }}</small></div></header>
      <div class="ai-mode-tabs" aria-label="助手模式">
        <button v-for="mode in modes" :key="mode.value" :class="{ active: assistantType === mode.value }" :disabled="busy" @click="assistantType = mode.value">{{ mode.label }}</button>
      </div>
      <div ref="chatBody" class="chat-body ai-body" aria-live="polite" aria-relevant="additions text">
        <p v-if="loadingHistory" class="muted ai-loading">正在恢复聊天记录……</p>
        <div v-else-if="!messages.length" class="ai-welcome"><span aria-hidden="true">✧</span><h3>关于它的事，都可以慢慢说</h3><p class="muted">整理宠物特征、准备寻宠启事，或聊聊养宠日常。</p><div class="ai-suggestions"><button v-for="item in suggestions" :key="item.text" :disabled="!configured" @click="useSuggestion(item)">{{ item.text }} <span aria-hidden="true">↗</span></button></div></div>
        <div v-for="(message, index) in messages" :key="message.id || index" class="message-line" :class="{ mine: message.role === 'user' }"><div class="message-bubble">{{ message.content || (busy ? '正在思考……' : '') }}</div></div>
      </div>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <form class="chat-composer" @submit.prevent="send">
        <label class="sr-only" for="ai-input">给 AI 助手的消息</label>
        <textarea id="ai-input" v-model="input" :disabled="busy || !configured" rows="3" maxlength="4000" :placeholder="configured ? '描述你和宠物的情况……' : '请先在后端配置 DeepSeek API Key'"></textarea>
        <div class="composer-bottom"><small class="muted">{{ configured ? 'AI 可能出错，请核实重要信息；健康建议不能替代兽医。' : '完成 DeepSeek 配置并重启后端后即可使用。' }}</small><button v-if="busy" type="button" class="button button-light button-small" @click="controller?.abort()">停止回复</button><button v-else class="button button-small" :disabled="!configured || !input.trim()">发送消息 ↑</button></div>
      </form>
    </div>
  </section>
</template>
