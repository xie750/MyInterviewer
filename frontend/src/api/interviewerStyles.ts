import { http } from './http'

import type { ApiResponse, InterviewerStyle } from '@/types'

export async function fetchInterviewerStylesApi(): Promise<InterviewerStyle[]> {
  const response = await http.get<ApiResponse<InterviewerStyle[]>>('/interviewer-styles')
  return response.data.data
}
