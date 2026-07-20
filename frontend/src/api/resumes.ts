import { http } from './http'

import type { ApiResponse, ResumeParseResponse } from '@/types'

export async function parseResumeApi(file: File): Promise<ResumeParseResponse> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.post<ApiResponse<ResumeParseResponse>>('/resumes/parse', formData, {
    skipUnauthorizedRedirect: true,
    timeout: 30000,
  })
  return response.data.data
}
