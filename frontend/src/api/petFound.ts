import request from './request'
import type { ApiResult, PageData } from '../types/common'

export interface PetFound {
  id: number
  userId: number
  petName: string | null
  species: string
  description: string | null
  foundTime: string
  foundLocation: string
  status: string
  visibilityStatus?: string
  moderationReason?: string | null
  createdAt: string
}
export interface PetFoundPayload {
  petName: string | null
  species: string
  description: string | null
  foundTime: string
  foundLocation: string
}
export async function listPetFound(page = 1, size = 10) {
  const r = await request.get<ApiResult<PageData<PetFound>>>('/api/pet-found', { params: { page, size } })
  return r.data
}
export async function getPetFound(id: number) {
  const r = await request.get<ApiResult<PetFound>>(`/api/pet-found/${id}`)
  return r.data
}
export async function createPetFound(data: PetFoundPayload) {
  const r = await request.post<ApiResult<PetFound>>('/api/pet-found', data)
  return r.data
}
export async function updatePetFound(id: number, data: PetFoundPayload) {
  const r = await request.put<ApiResult<PetFound>>(`/api/pet-found/${id}`, data)
  return r.data
}
export async function deletePetFound(id: number) {
  const r = await request.delete<ApiResult<unknown>>(`/api/pet-found/${id}`)
  return r.data
}
