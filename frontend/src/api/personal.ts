import request from './request'
import type { ApiResult, PageData } from '../types/common'
import type { PetLost } from './petLost'
import type { PetFound } from './petFound'

export interface PetProfile {
  id: number
  userId: number
  name: string
  species: string
  breed: string | null
  birthDate: string | null
  description: string | null
}
export type PetProfilePayload = Omit<PetProfile, 'id' | 'userId'>
export interface PersonalSummary { pets: number; lost: number; found: number; posts: number }

export async function getPersonalSummary() {
  return (await request.get<ApiResult<PersonalSummary>>('/api/users/me/summary')).data
}
export async function listMyPets(page = 1) {
  return (await request.get<ApiResult<PageData<PetProfile>>>('/api/pets', { params: { page } })).data
}
export async function savePet(data: PetProfilePayload, id: number | null) {
  return (await (id === null
    ? request.post<ApiResult<PetProfile>>('/api/pets', data)
    : request.put<ApiResult<PetProfile>>('/api/pets/' + id, data))).data
}
export async function deletePet(id: number) {
  return (await request.delete<ApiResult<unknown>>('/api/pets/' + id)).data
}
export async function listMyLost(page = 1) {
  return (await request.get<ApiResult<PageData<PetLost>>>('/api/pet-lost/mine', { params: { page } })).data
}
export async function listMyFound(page = 1) {
  return (await request.get<ApiResult<PageData<PetFound>>>('/api/pet-found/mine', { params: { page } })).data
}
