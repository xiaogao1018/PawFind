<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getAiStatus, streamAiReply, type AiMessage } from '../api/ai'
const messages = ref<AiMessage[]>([])
const input = ref(''), error = ref(''), busy = ref(false)
const configured = ref(false), statusText = ref('正在连接助手……')
const chatBody = ref<HTMLElement | null>(null)
let controller: AbortController | null = null
const suggestions = ['帮我整理一份寻宠启事', '宠物走失后可以先做什么？', '帮我整理宠物的外貌特征']
async function scroll() {
  await nextTick()
  chatBody.value?.scrollTo({ top: chatBody.value.scrollHeight, behavior: 'smooth' })
}
async function send() {
  if (!input.value.trim() || busy.value) return
  const content = input.value.trim()
  input.value = ''
  error.value = ''
  messages.value.push({ role: 'user', content })
  const history = messages.value.map(message => ({ ...message }))
  const replyIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  busy.value = true
  controller = new AbortController()
  await scroll()
  try {
    await streamAiReply(history, delta => {
      const reply = messages.value[replyIndex]
      if (reply) reply.content += delta
      scroll()
    }, controller.signal)
  } catch (e) {
    const reply = messages.value[replyIndex]
    if (!reply?.content) {
      messages.value.splice(replyIndex - 1, 2)
      input.value = content
    }
    if (!(e instanceof DOMException && e.name === 'AbortError')) {
      error.value = e instanceof Error ? e.message : 'AI 回复失败，请重试'
    }
  } finally {
    busy.value = false
    controller = null
  }
}
function clear() {
  if (busy.value) return
  messages.value = []
  error.value = ''
}
onMounted(async () => {
  try {
    const result = await getAiStatus()
    configured.value = result.data?.configured || false
    statusText.value = result.data?.message || '助手暂时无法连接'
  } catch { statusText.value = '助手暂时无法连接' }
})
onBeforeUnmount(() => controller?.abort())
</script>
<template>
  <section class="ai-chat-panel">
    <header class="chat-heading"><div class="assistant-avatar" aria-hidden="true">✧</div><div class="grow"><h2>AI 寻宠助手</h2><small class="muted">{{ configured ? '在线 · 可以开始对话' : statusText }}</small></div><button class="text-button" :disabled="busy || !messages.length" @click="clear">清空对话</button></header>
    <div ref="chatBody" class="chat-body ai-body" aria-live="polite" aria-relevant="additions text">
      <div v-if="!messages.length" class="ai-welcome"><span aria-hidden="true">✧</span><h3>关于它的事，都可以慢慢说</h3><p class="muted">整理宠物特征、准备寻宠启事，或聊聊养宠日常。</p><div class="ai-suggestions"><button v-for="item in suggestions" :key="item" @click="input = item">{{ item }} <span aria-hidden="true">↗</span></button></div></div>
      <div v-for="(message, index) in messages" :key="index" class="message-line" :class="{ mine: message.role === 'user' }"><div class="message-bubble">{{ message.content || (busy ? '正在思考……' : '') }}</div></div>
    </div>
    <p v-if="error" class="form-error" role="alert">{{ error }}</p>
    <form class="chat-composer" @submit.prevent="send">
      <label class="sr-only" for="ai-input">给 AI 助手的消息</label>
      <textarea id="ai-input" v-model="input" :disabled="busy" rows="3" maxlength="4000" placeholder="描述你和宠物的情况……"></textarea>
      <div class="composer-bottom"><small class="muted">{{ configured ? '请核实重要信息，建议仅供参考。' : '助手正在准备中，接通后即可开始对话。' }}</small><button v-if="busy" type="button" class="button button-light button-small" @click="controller?.abort()">停止回复</button><button v-else class="button button-small" :disabled="!input.trim()">发送消息 ↑</button></div>
    </form>
  </section>
</template>
