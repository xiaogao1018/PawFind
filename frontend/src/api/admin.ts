import request from './request'
import type { ApiResult, PageData } from '../types/common'
import type { CommunityPost, AuditStatus, VisibilityStatus } from './community'

export interface AdminUser { id: number; username: string; role: string; status: string; createdAt?: string }
export interface AdminOperationLog {
  id: number; adminId: number; operation: string; targetType: string; targetId: number; detail: string | null; createdAt: string
}
export async function listAdminPosts(auditStatus?: AuditStatus, page = 1, size = 10, visibilityStatus?: VisibilityStatus) {
  const r = await request.get<ApiResult<PageData<CommunityPost>>>('/api/admin/community/posts', { params: { auditStatus, visibilityStatus, page, size } })
  return r.data
}
export async function auditPost(id: number, auditStatus: Extract<AuditStatus, 'APPROVED' | 'REJECTED'>, reason = '') {
  const r = await request.patch<ApiResult<CommunityPost>>(`/api/admin/community/posts/${id}/audit`, { auditStatus, reason })
  return r.data
}
export async function setPostVisibility(id: number, visibilityStatus: VisibilityStatus, reason = '') {
  const r = await request.patch<ApiResult<CommunityPost>>(`/api/admin/community/posts/${id}/visibility`, { visibilityStatus, reason })
  return r.data
}
export async function listAdminUsers(page = 1, size = 20) {
  const r = await request.get<ApiResult<PageData<AdminUser>>>('/api/admin/users', { params: { page, size } })
  return r.data
}
export async function updateUserStatus(id: number, status: string) {
  const r = await request.patch<ApiResult<AdminUser>>(`/api/admin/users/${id}/status`, { status })
  return r.data
}
export async function listOperationLogs(page = 1, size = 20) {
  const r = await request.get<ApiResult<PageData<AdminOperationLog>>>('/api/admin/operation-logs', { params: { page, size } })
  return r.data
}

export interface ManagedPetRecord {
  id: number; userId: number; petName: string | null; species: string; description: string | null
  status: string; visibilityStatus: VisibilityStatus; moderationReason: string | null; createdAt: string
  lostLocation?: string; foundLocation?: string; lostTime?: string; foundTime?: string
}
export interface ManagedComment {
  id: number; postId: number; userId: number; content: string; visibilityStatus: string; createdAt: string
}
export async function listManagedPets(kind: 'lost' | 'found', page = 1, visibilityStatus?: VisibilityStatus) {
  return (await request.get<ApiResult<PageData<ManagedPetRecord>>>('/api/admin/pet-' + kind, {
    params: { page, size: 10, visibilityStatus }
  })).data
}
export async function setManagedPetVisibility(kind: 'lost' | 'found', id: number, visibilityStatus: VisibilityStatus, reason: string) {
  return (await request.patch<ApiResult<unknown>>('/api/admin/pet-' + kind + '/' + id + '/visibility', {
    visibilityStatus, reason
  })).data
}
export async function listManagedComments(page = 1, visibilityStatus?: string) {
  return (await request.get<ApiResult<PageData<ManagedComment>>>('/api/admin/community/comments', {
    params: { page, size: 10, visibilityStatus }
  })).data
}
export async function setCommentVisibility(id: number, visibilityStatus: VisibilityStatus, reason: string) {
  return (await request.patch<ApiResult<unknown>>('/api/admin/community/comments/' + id + '/visibility', {
    visibilityStatus, reason
  })).data
}
