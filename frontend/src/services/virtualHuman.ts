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

interface VirtualHumanAsset {
  src: string
  accent: string
  badge: string
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
    src: asset?.src ?? null,
    accent: asset?.accent ?? '#64748b',
    badge: asset?.badge ?? '降级',
    initials: normalized.name.slice(0, 2),
    missingAsset: !asset,
  }
}
