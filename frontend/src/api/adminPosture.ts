import { http } from './http'

import type {
  AdminPostureEvent,
  ApiResponse,
  PageResponse,
  PostureEventType,
  PostureSeverity,
  PostureThreshold,
  PostureThresholdRequest,
} from '@/types'

export interface AdminPostureEventQuery {
  keyword?: string
  sessionId?: number | null
  eventType?: PostureEventType | ''
  severity?: PostureSeverity | ''
  page?: number
  pageSize?: number
}

export async function fetchAdminPostureEventsApi(
  params: AdminPostureEventQuery,
): Promise<PageResponse<AdminPostureEvent>> {
  const response = await http.get<ApiResponse<PageResponse<AdminPostureEvent>>>('/admin/posture-events', {
    params,
  })
  return response.data.data
}

export async function fetchAdminPostureThresholdsApi(): Promise<PostureThreshold[]> {
  const response = await http.get<ApiResponse<PostureThreshold[]>>('/admin/posture-thresholds')
  return response.data.data
}

export async function updateAdminPostureThresholdApi(
  id: number,
  payload: PostureThresholdRequest,
): Promise<PostureThreshold> {
  const response = await http.put<ApiResponse<PostureThreshold>>(`/admin/posture-thresholds/${id}`, payload)
  return response.data.data
}

export async function fetchPostureThresholdsApi(): Promise<PostureThreshold[]> {
  const response = await http.get<ApiResponse<PostureThreshold[]>>('/posture-thresholds')
  return response.data.data
}
