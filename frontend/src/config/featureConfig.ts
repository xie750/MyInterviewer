/**
 * Feature Flags 配置
 *
 * 所有值来自 Vite 注入的环境变量（import.meta.env）。
 * 前端以 .env.local 为准，.env.example 为模板。
 *
 * 修改方式：
 * 1. 在 .env.local 中修改变量值
 * 2. 重新启动前端开发服务器（vite 不会热更新 env 变量）
 */

// ---------- Feature Flags ----------
export const featureFlags = {
  /** 是否开启简历上传与解析 */
  resume: import.meta.env.VITE_FEATURE_RESUME === 'true',

  /** 是否开启本地 AI 模式（true = 使用后端本地模板, false = 需要后端配置真实 LLM） */
  aiLocal: import.meta.env.VITE_FEATURE_AI_LOCAL === 'true',

  /** 是否开启语音识别与语音合成 */
  speech: import.meta.env.VITE_FEATURE_SPEECH === 'true',

  /** 是否开启摄像头姿态检测 */
  posture: import.meta.env.VITE_FEATURE_POSTURE === 'true',

  /** 是否开启 PDF 报告下载 */
  pdfReport: import.meta.env.VITE_FEATURE_PDF_REPORT === 'true',
} as const

// ---------- AI 提供商 ----------
export const aiProvider = (import.meta.env.VITE_AI_PROVIDER || 'local') as
  | 'local'
  | 'openai'
  | 'qwen'
  | 'deepseek'
  | 'custom'

// ---------- API ----------
export const apiBaseUrl =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api'

// ---------- 应用信息 ----------
export const appTitle =
  (import.meta.env.VITE_APP_TITLE as string | undefined) ?? 'AI 模拟面试'
