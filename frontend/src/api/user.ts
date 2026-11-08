
import request from './request'
import type { ApiResult } from './auth'

// 用户资料的数据结构
export interface UserInfo {
  id: number
  username: string
  role: string
}

// 获取当前登录用户资料
export function getCurrentUser() {
  return request.get<ApiResult<UserInfo>>(
    '/api/users/me'
  )
}
