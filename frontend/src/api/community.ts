import request from './request'
import type { ApiResult, PageData } from '../types/common'

export type PostCategory = 'EXPERIENCE' | 'HELP' | 'QUESTION' | 'KNOWLEDGE'
export const postCategories: { value: PostCategory; label: string }[] = [
  { value: 'EXPERIENCE', label: '分享' }, { value: 'HELP', label: '求助' },
  { value: 'QUESTION', label: '问答' }, { value: 'KNOWLEDGE', label: '知识' }
]
export type AuditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type VisibilityStatus = 'VISIBLE' | 'HIDDEN'
export interface CommunityPost {
  id: number
  userId: number
  title: string
  authorName?: string
  category: PostCategory
  commentCount?: number
  content: string
  auditStatus: AuditStatus
  visibilityStatus: VisibilityStatus
  moderationReason?: string | null
  createdAt: string
}
export interface CommunityComment {
  id: number
  postId: number
  userId: number
  content: string
  createdAt: string
}
export async function listCommunityPosts(page = 1, size = 10, category?: PostCategory) {
  const r = await request.get<ApiResult<PageData<CommunityPost>>>('/api/community/posts', { params: { page, size, category } })
  return r.data
}
export async function listMyCommunityPosts(page = 1, size = 10) {
  const r = await request.get<ApiResult<PageData<CommunityPost>>>('/api/community/posts/mine', { params: { page, size } })
  return r.data
}
export async function getCommunityPost(id: number) {
  const r = await request.get<ApiResult<CommunityPost>>(`/api/community/posts/${id}`)
  return r.data
}
export async function createCommunityPost(data: { title: string; content: string; category?: PostCategory }) {
  const r = await request.post<ApiResult<CommunityPost>>('/api/community/posts', data)
  return r.data
}
export async function updateCommunityPost(id: number, data: { title: string; content: string; category?: PostCategory }) {
  const r = await request.put<ApiResult<CommunityPost>>(`/api/community/posts/${id}`, data)
  return r.data
}
export async function listCommunityComments(postId: number) {
  const r = await request.get<ApiResult<CommunityComment[]>>(`/api/community/posts/${postId}/comments`)
  return r.data
}
export async function createCommunityComment(postId: number, content: string) {
  const r = await request.post<ApiResult<CommunityComment>>(`/api/community/posts/${postId}/comments`, { content })
  return r.data
}
