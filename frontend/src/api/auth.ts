
import request from './request'

// 注册和登录提交的数据格式，和之前在 Apifox 中发送的 JSON 一致
export interface AuthRequest {
  username: string
  password: string
}

// 后端返回的统一响应结构
export interface ApiResult<T> {
  code: number
  message: string
  data: T | null
}

// 登录成功后返回的 Token 信息
export interface LoginData {
  token: string
  tokenType: string
  expiresIn: number
}

// 用户注册
export function register(data: AuthRequest) {
  return request.post<ApiResult<unknown>>(
    '/api/auth/register',
    data
  )
}

// 用户登录
export function login(data: AuthRequest) {
  return request.post<ApiResult<LoginData>>(
    '/api/auth/login',
    data
  )
}
