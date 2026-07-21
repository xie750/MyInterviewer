/**
 * 虚拟人服务：面试官形象展示与状态切换
 *
 * Feature Flag：VITE_FEATURE_VIRTUAL_HUMAN（暂时默认开启，暂无独立开关）
 * 当前为静态 SVG + CSS 类切换，口型同步和动画尚未实现。
 */

import defaultInterviewerAvatar from '@/assets/virtual-humans/default-interviewer.svg'
import hrPartnerAvatar from '@/assets/virtual-humans/hr-partner.svg'
import sternPanelAvatar from '@/assets/virtual-humans/stern-panel.svg'
import techArchitectAvatar from '@/assets/virtual-humans/tech-architect.svg'
import warmGuideAvatar from '@/assets/virtual-humans/warm-guide.svg'
import type { VirtualHumanProfile } from '@/types'

export interface VirtualHumanViewModel extends VirtualHumanProfile {
  src: string | null
  accent: string
  badge: string
  initials: string
  missingAsset: boolean
}

export type VirtualHumanMotionState =
  | 'QUESTIONING'
  | 'WAITING'
  | 'LISTENING'
  | 'THINKING'
  | 'SUMMARY'
  | 'READONLY'

export interface VirtualHumanMotion {
  state: VirtualHumanMotionState
  label: string
  description: string
  cue: string
  tagType: 'success' | 'warning' | 'info' | 'primary'
  className: string
}

interface VirtualHumanAsset {
  src: string
  accent: string
  badge: string
}

const motions: Record<VirtualHumanMotionState, Omit<VirtualHumanMotion, 'state'>> = {
  QUESTIONING: {
    label: '提问中',
    description: '正在向候选人抛出当前问题，请关注题干和追问重点。',
    cue: '问题播报',
    tagType: 'primary',
    className: 'is-questioning',
  },
  WAITING: {
    label: '等待回答',
    description: '面试官正在等待你的作答，可以输入文字或开启听写。',
    cue: '等待输入',
    tagType: 'info',
    className: 'is-waiting',
  },
  LISTENING: {
    label: '倾听中',
    description: '正在接收你的回答草稿，提交前仍可继续补充和修改。',
    cue: '记录回答',
    tagType: 'success',
    className: 'is-listening',
  },
  THINKING: {
    label: '思考中',
    description: '正在整理回答上下文，并准备下一轮追问或总结。',
    cue: '生成中',
    tagType: 'warning',
    className: 'is-thinking',
  },
  SUMMARY: {
    label: '总结中',
    description: '本场面试已完成，面试官正在呈现评分和复盘建议。',
    cue: '报告完成',
    tagType: 'success',
    className: 'is-summary',
  },
  READONLY: {
    label: '只读查看',
    description: '当前为历史或管理员只读详情，虚拟人保持静态展示。',
    cue: '只读',
    tagType: 'info',
    className: 'is-readonly',
  },
}

const defaultProfile: VirtualHumanProfile = {
  key: 'default-interviewer',
  name: 'AI 面试官',
  description: '基础静态面试官形象，资源不可用时保持占位展示。',
}

const assets: Record<string, VirtualHumanAsset> = {
  'default-interviewer': {
    src: defaultInterviewerAvatar,
    accent: '#2563eb',
    badge: '通用',
  },
  'stern-panel': {
    src: sternPanelAvatar,
    accent: '#be123c',
    badge: '压力',
  },
  'warm-guide': {
    src: warmGuideAvatar,
    accent: '#0f766e',
    badge: '引导',
  },
  'tech-architect': {
    src: techArchitectAvatar,
    accent: '#7c3aed',
    badge: '技术',
  },
  'hr-partner': {
    src: hrPartnerAvatar,
    accent: '#b45309',
    badge: '综合',
  },
}

export function resolveVirtualHuman(profile: VirtualHumanProfile | null | undefined): VirtualHumanViewModel {
  const normalized = {
    key: profile?.key || defaultProfile.key,
    name: profile?.name || defaultProfile.name,
    description: profile?.description || defaultProfile.description,
  }
  const asset = assets[normalized.key]

  return {
    ...normalized,
    src: profile?.imageUrl || asset?.src || null,
    accent: profile?.accentColor || asset?.accent || '#64748b',
    badge: profile?.badge || asset?.badge || '降级',
    initials: normalized.name.slice(0, 2),
    missingAsset: !profile?.imageUrl && !asset,
  }
}

export function resolveVirtualHumanMotion(state: VirtualHumanMotionState): VirtualHumanMotion {
  return {
    state,
    ...motions[state],
  }
}
