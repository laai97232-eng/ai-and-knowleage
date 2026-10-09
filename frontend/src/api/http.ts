import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 默认与页面同源，适合后端托管前端静态页的部署方式。
// 需要前后端分离（例如前端放 GitHub Pages）时，用 VITE_API_BASE 指定后端绝对地址，
// 例如 VITE_API_BASE=https://study-assistant.onrender.com
const API_BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '')

const instance = axios.create({ baseURL: `${API_BASE}/api`, timeout: 20000 })

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => response.data.data,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || '无法连接服务器'
    if (status === 401 && !location.pathname.startsWith('/login') && !location.pathname.startsWith('/register')) {
      localStorage.removeItem('token')
      location.href = '/login'
    }
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

const http = instance as unknown as {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T>
}

export default http

export async function downloadDocument(id: number, filename: string) {
  const token = localStorage.getItem('token')
  const response = await axios.get(`${API_BASE}/api/documents/${id}/file`, {
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'file'
  link.click()
  URL.revokeObjectURL(url)
}
