import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '@/types'
import { loginApi, logoutApi, meApi } from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'

const USER_KEY = 'museum_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<User | null>(readUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const roles = computed(() => user.value?.roles || [])
  const isAdmin = computed(
    () => roles.value.includes('RESERVATION_ADMIN') || roles.value.includes('SYSTEM_ADMIN'),
  )
  const isSystemAdmin = computed(() => roles.value.includes('SYSTEM_ADMIN'))
  const displayName = computed(() => user.value?.realName || user.value?.username || '游客')

  function readUser(): User | null {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? (JSON.parse(raw) as User) : null
    } catch {
      return null
    }
  }

  function persist() {
    if (token.value) {
      localStorage.setItem(TOKEN_KEY, token.value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
    if (user.value) {
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  async function login(username: string, password: string) {
    const data = await loginApi({ username, password })
    token.value = data.token
    user.value = data.user
    persist()
    return data.user
  }

  async function fetchMe() {
    const data = await meApi()
    user.value = data
    persist()
    return data
  }

  function logout(callApi = true) {
    const snapshot = token.value
    if (callApi && snapshot) {
      // 先带着当前令牌快照通知后端，再清理本地状态，避免清空后请求变 401 触发红色提示
      logoutApi(snapshot).catch(() => undefined)
    }
    token.value = ''
    user.value = null
    persist()
  }

  function setUser(value: User) {
    user.value = value
    persist()
  }

  return {
    token,
    user,
    isLoggedIn,
    roles,
    isAdmin,
    isSystemAdmin,
    displayName,
    login,
    fetchMe,
    logout,
    setUser,
  }
})
