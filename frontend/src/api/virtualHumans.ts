import { http } from './http'

import type { ApiResponse, VirtualHumanAsset, VirtualHumanAssetRequest } from '@/types'

export interface AdminVirtualHumanQuery {
  keyword?: string
  enabled?: boolean | null
}

export async function fetchAdminVirtualHumansApi(
  params: AdminVirtualHumanQuery = {},
): Promise<VirtualHumanAsset[]> {
  const response = await http.get<ApiResponse<VirtualHumanAsset[]>>('/admin/virtual-humans', { params })
  return response.data.data
}

export async function createAdminVirtualHumanApi(
  payload: VirtualHumanAssetRequest,
): Promise<VirtualHumanAsset> {
  const response = await http.post<ApiResponse<VirtualHumanAsset>>('/admin/virtual-humans', payload)
  return response.data.data
}

export async function updateAdminVirtualHumanApi(
  id: number,
  payload: VirtualHumanAssetRequest,
): Promise<VirtualHumanAsset> {
  const response = await http.put<ApiResponse<VirtualHumanAsset>>(`/admin/virtual-humans/${id}`, payload)
  return response.data.data
}
