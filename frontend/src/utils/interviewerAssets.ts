/**
 * 面试官资源映射 —— 将 virtualHuman.key 解析为本地角色图、强调色与展示文案。
 * 后端 virtual_human_asset 表中 image_url / accent_color / badge 字段暂未透出，
 * 因此前端按 key 维护一份独立映射，数据来源与虚拟人资产种子数据一致。
 */
export interface InterviewerAsset {
  imageUrl: string
  accentColor: string
  badge: string
  displayName: string
  description: string
}

const ASSET_MAP: Record<string, InterviewerAsset> = {
  'stern-panel': {
    imageUrl: '/interviewers/stern-panel.png',
    accentColor: '#be123c',
    badge: '专业严谨型',
    displayName: '李越',
    description: '逻辑严谨、不冷不热、中立客观',
  },
  'warm-guide': {
    imageUrl: '/interviewers/warm-guide.png',
    accentColor: '#0f766e',
    badge: '压力拷问型',
    displayName: '陆晓宇',
    description: '气场强、爱打断、喜欢反问质疑',
  },
  'tech-architect': {
    imageUrl: '/interviewers/tech-architect.png',
    accentColor: '#7c3aed',
    badge: '引导耐心型',
    displayName: '李雪',
    description: '包容耐心、善于引导、提问温和',
  },
  'hr-partner': {
    imageUrl: '/interviewers/hr-partner.png',
    accentColor: '#b45309',
    badge: '轻松亲和型',
    displayName: '孙小岚',
    description: '提问生活化、不刻板教条',
  },
  'challenge-master': {
    imageUrl: '/interviewers/challenge-master.png',
    accentColor: '#dc2626',
    badge: '发散开放型',
    displayName: '王博洋',
    description: '看重逻辑思维、解决问题能力',
  },
  'practice-coach': {
    imageUrl: '/interviewers/practice-coach.png',
    accentColor: '#059669',
    badge: 'HR综合型',
    displayName: '刘彬',
    description: '资深HRBP、重人品、情商',
  },
}

const DEFAULT_ASSET: InterviewerAsset = {
  imageUrl: '',
  accentColor: '#2563eb',
  badge: '通用',
  displayName: 'AI 面试官',
  description: '通用面试场景',
}

export { ASSET_MAP, DEFAULT_ASSET }
export function resolveInterviewerAsset(key: string): InterviewerAsset {
  return ASSET_MAP[key] ?? DEFAULT_ASSET
}
