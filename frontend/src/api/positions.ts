import { http } from './http'

import type { ApiResponse, Position, PositionRequest } from '@/types'

export async function fetchPositionsApi(): Promise<Position[]> {
  const response = await http.get<ApiResponse<Position[]>>('/positions')
  return response.data.data
}

export async function fetchAdminPositionsApi(): Promise<Position[]> {
  const response = await http.get<ApiResponse<Position[]>>('/admin/positions')
  return response.data.data
}

export async function createAdminPositionApi(payload: PositionRequest): Promise<Position> {
  const response = await http.post<ApiResponse<Position>>('/admin/positions', payload)
  return response.data.data
}

export async function updateAdminPositionApi(id: number, payload: PositionRequest): Promise<Position> {
  const response = await http.put<ApiResponse<Position>>(`/admin/positions/${id}`, payload)
  return response.data.data
}

export async function updateAdminPositionEnabledApi(id: number, enabled: boolean): Promise<Position> {
  const response = await http.patch<ApiResponse<Position>>(`/admin/positions/${id}/enabled`, { enabled })
  return response.data.data
}
