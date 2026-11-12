import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  // FormData 的 Content-Type boundary 由浏览器自动设置
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    config.headers.delete('Content-Type')
  }
  return config
})

request.interceptors.response.use(
  response => response,
  async error => {
    const status = error.response?.status
    if (status === 401) localStorage.removeItem('token')
    let message = error.response?.data?.message
    if (!message && typeof Blob !== 'undefined' && error.response?.data instanceof Blob) {
      try { message = JSON.parse(await error.response.data.text())?.message } catch { /* ignore */ }
    }
    return Promise.reject(new Error(message || error.message || '请求失败，请稍后重试'))
  }
)

export default request
