import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchCurrentUserApi, loginApi } from '@/api/auth'
import type { LoginRequest, UserProfile } from '@/types'

const TOKEN_KEY = 'auth_token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<UserProfile | null>(null)

  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(payload: LoginRequest) {
    const result = await loginApi(payload)
    token.value = result.token
    user.value = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
  }

  async function loadCurrentUser() {
    if (!token.value) {
      return
    }
    user.value = await fetchCurrentUserApi()
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    loadCurrentUser,
    logout,
  }
})

