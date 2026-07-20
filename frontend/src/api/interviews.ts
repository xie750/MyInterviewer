import { http } from './http'

import type {
  AdminInterview,
  AnswerInterviewRequest,
  ApiResponse,
  CreateInterviewRequest,
  InterviewDetail,
  InterviewSummary,
} from '@/types'

export async function fetchInterviewsApi(): Promise<InterviewSummary[]> {
  const response = await http.get<ApiResponse<InterviewSummary[]>>('/interviews')
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
