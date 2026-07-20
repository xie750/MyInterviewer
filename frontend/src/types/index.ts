export type UserRole = 'USER' | 'ADMIN'
export type UserStatus = 'ENABLED' | 'DISABLED'
export type InterviewStatus = 'IN_PROGRESS' | 'COMPLETED'
export type MessageRole = 'ASSISTANT' | 'USER'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserProfile {
  id: number
  username: string
  displayName: string
  role: UserRole
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: 'Bearer'
  expiresIn: number
  user: UserProfile
}

export interface Position {
  id: number
  name: string
  description: string | null
  techStack: string | null
  difficulty: string | null
  promptTemplate: string | null
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface PositionRequest {
  name: string
  description: string
  techStack: string
  difficulty: string
  promptTemplate: string
  enabled: boolean
  sortOrder: number
}

export interface InterviewerStyle {
  id: number
  name: string
  description: string | null
  promptTemplate: string
  scenario: string | null
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface CreateInterviewRequest {
  positionId: number
  styleId: number
  resume?: ResumeContextRequest
}

export interface ResumeContextRequest {
  summary: string | null
  skills: string[]
  projects: string[]
  warnings: string[]
}

export interface ResumeParseResponse extends ResumeContextRequest {
  fileName: string
  extractedTextLength: number
}

export interface ResumeContext extends ResumeContextRequest {
  used: boolean
}

export interface AnswerInterviewRequest {
  content: string
}

export interface InterviewMessage {
  id: number
  role: MessageRole
  content: string
  roundNo: number
  createdAt: string
}

export interface InterviewReport {
  id: number
  sessionId: number
  totalScore: number
  technicalScore: number
  communicationScore: number
  logicScore: number
  summary: string
  strengths: string
  improvements: string
  recommendation: string
  createdAt: string
  updatedAt: string
}

export interface InterviewDetail {
  id: number
  status: InterviewStatus
  questionCount: number
  position: Position
  style: InterviewerStyle
  resume: ResumeContext
  messages: InterviewMessage[]
  report: InterviewReport | null
  startedAt: string
  endedAt: string | null
  updatedAt: string
}

export interface InterviewSummary {
  id: number
  status: InterviewStatus
  questionCount: number
  positionName: string
  styleName: string
  resumeUsed: boolean
  totalScore: number | null
  startedAt: string
  endedAt: string | null
  updatedAt: string
}

export interface AdminInterview {
  id: number
  username: string
  displayName: string
  positionName: string
  styleName: string
  status: InterviewStatus
  questionCount: number
  resumeUsed: boolean
  totalScore: number | null
  startedAt: string
  endedAt: string | null
  updatedAt: string
}

export interface AdminUser {
  id: number
  username: string
  displayName: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}
