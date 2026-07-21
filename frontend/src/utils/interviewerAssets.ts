/**
 * 面试官资源映射 —— 将 virtualHuman.key 解析为本地角色图、强调色与标签文字。
 * 后端 virtual_human_asset 表中 image_url / accent_color / badge 字段暂未透出，
 * 因此前端按 key 维护一份独立映射，数据来源与虚拟人资产种子数据一致。
 */
export interface InterviewerAsset {
  imageUrl: string
  accentColor: string
  badge: string
}

const ASSET_MAP: Record<string, InterviewerAsset> = {
  'stern-panel': {
    imageUrl: '/interviewers/stern-panel.png',
    accentColor: '#be123c',
    badge: '压力',
  },
  'warm-guide': {
    imageUrl: '/interviewers/warm-guide.png',
    accentColor: '#0f766e',
    badge: '引导',
  },
  'tech-architect': {
    imageUrl: '/interviewers/tech-architect.png',
    accentColor: '#7c3aed',
    badge: '技术',
  },
  'hr-partner': {
    imageUrl: '/interviewers/hr-partner.png',
    accentColor: '#b45309',
    badge: '综合',
  },
  'challenge-master': {
    imageUrl: '/interviewers/challenge-master.png',
    accentColor: '#dc2626',
    badge: '挑战',
  },
  'practice-coach': {
    imageUrl: '/interviewers/practice-coach.png',
    accentColor: '#059669',
    badge: '实战',
  },
}

const DEFAULT_ASSET: InterviewerAsset = {
  imageUrl: '',
  accentColor: '#2563eb',
  badge: '通用',
}

export function resolveInterviewerAsset(key: string): InterviewerAsset {
  return ASSET_MAP[key] ?? DEFAULT_ASSET
}
