import { http } from './http'

import type { AdminUser, ApiResponse, UserStatus } from '@/types'

export async function fetchAdminUsersApi(): Promise<AdminUser[]> {
  const response = await http.get<ApiResponse<AdminUser[]>>('/admin/users')
  return response.data.data
}

export async function updateAdminUserStatusApi(id: number, status: UserStatus): Promise<AdminUser> {
  const response = await http.patch<ApiResponse<AdminUser>>(`/admin/users/${id}/status`, { status })
  return response.data.data
}
