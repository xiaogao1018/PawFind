import request from './request'
import type { ApiResult } from '../types/common'

export type ClueStatus = 'PENDING' | 'VERIFIED' | 'INVALID'
export interface PetClue {
  id: number
  petLostId: number
  userId: number
  content: string
  contactInfo: string | null
  status: ClueStatus
  createdAt: string
}
export async function listPetClues(petLostId: number) {
  const r = await request.get<ApiResult<PetClue[]>>(`/api/pet-lost/${petLostId}/clues`)
  return r.data
}
export async function createPetClue(petLostId: number, data: { content: string; contactInfo: string | null }) {
  const r = await request.post<ApiResult<PetClue>>(`/api/pet-lost/${petLostId}/clues`, data)
  return r.data
}
export async function updatePetClueStatus(clueId: number, status: 'VERIFIED' | 'INVALID') {
  const r = await request.patch<ApiResult<PetClue>>(`/api/pet-clues/${clueId}/status`, { status })
  return r.data
}
