import request from './request'
import type { ApiResult } from '../types/common'

export interface FileAttachment {
  id: number
  petLostId: number
  originalFilename: string
  contentType: string
  fileSize: number
  url: string
  createdAt: string
}

export async function listPetLostImages(petLostId: number) {
  const response = await request.get<ApiResult<FileAttachment[]>>(`/api/pet-lost/${petLostId}/images`)
  return response.data
}
export async function uploadPetLostImage(petLostId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  const response = await request.post<ApiResult<FileAttachment>>(`/api/pet-lost/${petLostId}/images`, form)
  return response.data
}
export async function getPetLostImageBlob(petLostId: number, attachmentId: number) {
  const response = await request.get<Blob>(`/api/pet-lost/${petLostId}/images/${attachmentId}`, { responseType: 'blob' })
  return response.data
}

export async function uploadRecordImage(recordPath: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  const response = await request.post<ApiResult<FileAttachment>>(recordPath + '/images', form)
  return response.data
}

export async function listRecordImages(recordPath: string) {
  const response = await request.get<ApiResult<FileAttachment[]>>(recordPath + '/images')
  return response.data
}
