import { http } from './http'

import type {
  AdminInterview,
  AnswerInterviewRequest,
  ApiResponse,
  CreateInterviewRequest,
  InterviewDetail,
  InterviewSummary,
  PageResponse,
  PostureEvent,
  PostureEventRequest,
} from '@/types'

export async function fetchInterviewsApi(page = 1, pageSize = 6): Promise<PageResponse<InterviewSummary>> {
  const response = await http.get<ApiResponse<PageResponse<InterviewSummary>>>('/interviews', {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function createInterviewApi(payload: CreateInterviewRequest): Promise<InterviewDetail> {
  const response = await http.post<ApiResponse<InterviewDetail>>('/interviews', payload)
  return response.data.data
}

export async function fetchInterviewApi(id: number): Promise<InterviewDetail> {
  const response = await http.get<ApiResponse<InterviewDetail>>(`/interviews/${id}`)
  return response.data.data
}

export async function answerInterviewApi(id: number, payload: AnswerInterviewRequest): Promise<InterviewDetail> {
  const response = await http.post<ApiResponse<InterviewDetail>>(`/interviews/${id}/messages`, payload)
  return response.data.data
}

export async function finishInterviewApi(id: number): Promise<InterviewDetail> {
  const response = await http.post<ApiResponse<InterviewDetail>>(`/interviews/${id}/finish`)
  return response.data.data
}

export async function fetchAdminInterviewsApi(): Promise<AdminInterview[]> {
  const response = await http.get<ApiResponse<AdminInterview[]>>('/admin/interviews')
  return response.data.data
}

export async function fetchAdminInterviewApi(id: number): Promise<InterviewDetail> {
  const response = await http.get<ApiResponse<InterviewDetail>>(`/admin/interviews/${id}`)
  return response.data.data
}

export async function deleteInterviewApi(id: number): Promise<void> {
  await http.delete(`/interviews/${id}`)
}

export async function reportPostureEventApi(payload: PostureEventRequest): Promise<PostureEvent> {
  const response = await http.post<ApiResponse<PostureEvent>>('/posture-events', payload)
  return response.data.data
}
