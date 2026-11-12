import request from './request'
import type { ApiResult, PageData } from '../types/common'

export interface Notice {
  id: number; actorId: number; actorName: string; type: string
  targetType: 'LOST' | 'FOUND' | 'POST'; targetId: number
  content: string; isRead: boolean; createdAt: string
}
export interface Peer { id: number; username: string; status: string }
export interface Conversation {
  userId: number; username: string; status: string; preview: string; updatedAt: string; unreadCount: number
}
export interface DirectMessage {
  id: number; senderId: number; recipientId: number; content: string; isRead: boolean; createdAt: string
}
export interface MessageSlice { records: DirectMessage[]; hasMore: boolean }
export interface UnreadCounts { notifications: number; direct: number }
export interface RecordComment { id: number; userId: number; username: string; content: string; createdAt: string }

export async function getUnreadCounts() {
  return (await request.get<ApiResult<UnreadCounts>>('/api/messages/unread')).data
}
export async function getNotifications(page = 1) {
  return (await request.get<ApiResult<PageData<Notice>>>('/api/messages/notifications', { params: { page } })).data
}
export async function readNotification(id: number) {
  await request.patch('/api/messages/notifications/' + id + '/read')
  refreshMessageBadges()
}
export async function readAllNotifications(upToId: number) {
  await request.patch('/api/messages/notifications/read', { upToId })
  refreshMessageBadges()
}
export async function getConversations(page = 1) {
  return (await request.get<ApiResult<PageData<Conversation>>>('/api/messages/conversations', { params: { page } })).data
}
export async function findRecipient(username: string) {
  return (await request.get<ApiResult<Peer>>('/api/messages/recipients', { params: { username } })).data
}
export async function getRecipient(id: number) {
  return (await request.get<ApiResult<Peer>>('/api/messages/recipients/' + id)).data
}
export async function getThread(id: number, params: { beforeId?: number; afterId?: number } = {}) {
  return (await request.get<ApiResult<MessageSlice>>('/api/messages/conversations/' + id + '/messages', { params })).data
}
export async function sendDirectMessage(id: number, content: string) {
  return (await request.post<ApiResult<DirectMessage>>('/api/messages/conversations/' + id + '/messages', { content })).data
}
export async function readConversation(id: number, upToId: number) {
  await request.patch('/api/messages/conversations/' + id + '/read', { upToId })
  refreshMessageBadges()
}
export async function getRecordComments(path: string, page = 1) {
  return (await request.get<ApiResult<PageData<RecordComment>>>(path + '/comments', { params: { page } })).data
}
export async function createRecordComment(path: string, content: string) {
  return (await request.post<ApiResult<RecordComment>>(path + '/comments', { content })).data
}
export function refreshMessageBadges() {
  window.dispatchEvent(new Event('pawfind:messages-changed'))
}
