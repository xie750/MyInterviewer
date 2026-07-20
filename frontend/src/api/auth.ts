import { http } from './http'

import type { ApiResponse, LoginRequest, LoginResponse, UserProfile } from '@/types'

export async function loginApi(payload: LoginRequest): Promise<LoginResponse> {
  const response = await http.post<ApiResponse<LoginResponse>>('/auth/login', payload)
  return response.data.data
}

export async function fetchCurrentUserApi(): Promise<UserProfile> {
  const response = await http.get<ApiResponse<UserProfile>>('/me')
  return response.data.data
}

