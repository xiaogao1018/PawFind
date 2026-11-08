
import axios from 'axios'

// 创建统一的 Axios 实例
const request = axios.create({

  // 后端 Spring Boot 的地址
  baseURL: 'http://localhost:8080',

  // 请求超时时间：10 秒
  timeout: 10000,

  // 默认使用 JSON 格式
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：发送请求之前执行
request.interceptors.request.use(
  (config) => {

    // 从浏览器本地存储中获取 JWT
    const token = localStorage.getItem('token')

    // 如果有 Token，自动放进请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  }
)

// 响应拦截器：后端返回响应之后执行
request.interceptors.response.use(
  (response) => {

    // 直接返回后端的 JSON 数据
    return response
  },

  (error) => {

    // 获取 HTTP 状态码
    const status = error.response?.status

    // 如果登录身份失效，清除本地 Token
    if (status === 401) {
      localStorage.removeItem('token')
    }

    // 获取后端返回的错误信息
    const message =
      error.response?.data?.message || '请求失败，请稍后重试'

    return Promise.reject(new Error(message))
  }
)

export default request
