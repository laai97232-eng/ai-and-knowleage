import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const instance = axios.create({ baseURL: '/api', timeout: 20000 })

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
  const response = await axios.get(`/api/documents/${id}/file`, {
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
