import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const TOKEN_KEY = 'museum_token'

export type HttpConfig = AxiosRequestConfig & { silent?: boolean }

const instance = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  // 不覆盖调用方显式传入的 Authorization（如退出登录时携带令牌快照）
  const hasAuth = Boolean((config.headers as Record<string, unknown> | undefined)?.Authorization)
  if (token && !hasAuth) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    const silent = Boolean((response.config as HttpConfig)?.silent)
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      if (body.code === 401 && !silent) {
        clearAuthAndRedirect()
      }
      if (!silent) {
        ElMessage.error(body.message || '操作失败')
      }
      return Promise.reject(new Error(body.message || '操作失败'))
    }
    return response.data
  },
  (error) => {
    const status = error?.response?.status
    const silent = Boolean((error?.config as HttpConfig)?.silent)
    if (status === 401) {
      if (!silent) {
        clearAuthAndRedirect()
        ElMessage.error('登录状态已失效，请重新登录')
      }
    } else if (status === 403) {
      if (!silent) {
        ElMessage.error('没有权限执行该操作')
      }
    } else if (!error?.response || status === 502 || status === 503 || status === 504) {
      // 后端未启动、端口不通或跨域被拦截时走这里，明确提示而不是笼统的“网络异常”
      if (!silent) {
        ElMessage.error('无法连接服务器，请确认后端服务（http://localhost:8080）已启动')
      }
    } else if (status >= 500 && !error?.response?.data?.message) {
      // Vite 代理在后端未启动时通常返回 500，这里给出可排查的提示
      if (!silent) {
        ElMessage.error('后端服务暂时不可用，请确认后端已启动后重试')
      }
    } else {
      if (!silent) {
        ElMessage.error(error?.response?.data?.message || '请求失败，请稍后重试')
      }
    }
    return Promise.reject(error)
  },
)

function clearAuthAndRedirect() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('museum_user')
  if (!location.pathname.startsWith('/login')) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    location.href = `/login?redirect=${redirect}`
  }
}

export const get = <T>(url: string, params?: Record<string, unknown>) =>
  instance.request<unknown, T>({ url, method: 'get', params })

export const post = <T>(url: string, data?: unknown, config?: HttpConfig) =>
  instance.request<unknown, T>({ url, method: 'post', data, ...config })

export const put = <T>(url: string, data?: unknown) =>
  instance.request<unknown, T>({ url, method: 'put', data })

export const del = <T>(url: string) =>
  instance.request<unknown, T>({ url, method: 'delete' })

export { TOKEN_KEY }
