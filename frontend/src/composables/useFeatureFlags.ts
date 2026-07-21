import type { FeatureFlags } from '@/types'
import { featureFlags } from '@/config/featureConfig'

/**
 * Vue composable：提供 feature flags 响应式访问
 *
 * 用法：
 *   const { resume, speech, posture } = useFeatureFlags()
 *   if (speech) { /* 渲染语音相关 UI *\/ }
 */
export function useFeatureFlags(): FeatureFlags {
  return featureFlags
}
