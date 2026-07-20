import axios from 'axios'

const TOKEN_KEY = 'auth_token'

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipUnauthorizedRedirect?: boolean
  }
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401
      && error.config?.url !== '/auth/login'
      && !error.config?.skipUnauthorizedRedirect
    ) {
      localStorage.removeItem(TOKEN_KEY)
      if (window.location.pathname !== '/login') {
        const redirect = `${window.location.pathname}${window.location.search}`
        window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
      }
    }
    return Promise.reject(error)
  },
)
