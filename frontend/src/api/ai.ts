import request from './request'
import type { ApiResult } from '../types/common'

export interface AiMessage { role: 'user' | 'assistant'; content: string }
export async function getAiStatus() {
  return (await request.get<ApiResult<{ configured: boolean; message: string }>>('/api/ai/status')).data
}

// Expected SSE events: delta {"content":"..."}, done {}, error {"message":"..."}.
export async function streamAiReply(
  messages: AiMessage[], onDelta: (text: string) => void, signal: AbortSignal
) {
  const base = request.defaults.baseURL || window.location.origin
  const endpoint = new URL('/api/ai/chat/stream', new URL(base, window.location.origin))
  const response = await fetch(endpoint, {
    method: 'POST', signal,
    headers: {
      'Content-Type': 'application/json', Accept: 'text/event-stream',
      Authorization: 'Bearer ' + (localStorage.getItem('token') || '')
    },
    body: JSON.stringify({ messages })
  })
  if (!response.ok) {
    if (response.status === 401) localStorage.removeItem('token')
    const result = await response.json().catch(() => null)
    throw new Error(result?.message || 'AI 暂时无法回复，请稍后重试')
  }
  if (!response.body || !response.headers.get('Content-Type')?.includes('text/event-stream')) {
    throw new Error('AI 回复格式不正确，请稍后重试')
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let data: string[] = []
  let event = 'message'
  let done = false

  function dispatch() {
    if (!data.length) { event = 'message'; return }
    const payload = data.join('\n')
    data = []
    const type = event
    event = 'message'
    if (type === 'done' || payload === '[DONE]') { done = true; return }
    let value: { content?: string; message?: string }
    try { value = JSON.parse(payload) } catch { throw new Error('AI 回复数据无法解析') }
    if (type === 'error') throw new Error(value.message || 'AI 回复中断，请重试')
    if (typeof value.content === 'string') onDelta(value.content)
  }
  function line(value: string) {
    if (!value) { dispatch(); return }
    if (value.startsWith(':')) return
    const split = value.indexOf(':')
    const field = split < 0 ? value : value.slice(0, split)
    let body = split < 0 ? '' : value.slice(split + 1)
    if (body.startsWith(' ')) body = body.slice(1)
    if (field === 'data') data.push(body)
    if (field === 'event') event = body
  }
  try {
    while (!done) {
      const part = await reader.read()
      buffer += decoder.decode(part.value, { stream: !part.done })
      // Keep a trailing CR until the next chunk, so split CRLF pairs stay intact.
      let newline = buffer.search(/[\r\n]/)
      while (newline >= 0) {
        if (buffer[newline] === '\r' && newline === buffer.length - 1 && !part.done) break
        line(buffer.slice(0, newline))
        const width = buffer[newline] === '\r' && buffer[newline + 1] === '\n' ? 2 : 1
        buffer = buffer.slice(newline + width)
        if (done) break
        newline = buffer.search(/[\r\n]/)
      }
      if (part.done) {
        if (buffer && !done) line(buffer)
        if (!done) dispatch()
        if (!done) throw new Error('AI 连接提前结束，请重试')
      }
    }
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}
