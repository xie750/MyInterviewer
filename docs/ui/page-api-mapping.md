# 页面与接口映射

## 使用规则

本文档记录 UI 页面、Vue 路由、真实 API 和字段映射。页面开发前必须先确认这里的映射，禁止自行编造接口字段或在页面中写死业务数据。

## 页面总览

| 页面 | 路由 | Vue 页面 | 数据需求 | 后端接口 | 权限 | UI 对齐状态 |
|---|---|---|---|---|---|---|
| 登录页 | `/login` | `LoginView.vue` | 账号、密码、Token、当前用户 | `POST /api/auth/login`、`GET /api/me` | 公开页，已登录跳转首页 | 已按 UITemp 浅色风格推进 |
| 首页/工作台 | `/home` | `HomeView.vue` | 当前用户、入口状态 | `GET /api/me` | 登录用户 | 已按 UITemp 浅色风格推进 |
| 上传简历/岗位/面试官选择页 | `/positions` | `PositionsView.vue` | 岗位列表、面试官风格、虚拟人信息、简历解析 | `GET /api/positions`、`GET /api/interviewer-styles`、`POST /api/resumes/parse`、`POST /api/interviews` | 登录用户 | 已按 UITemp 推进 |
| AI 面试页面 | `/interviews/:id` | `InterviewRoomView.vue` | 面试详情、消息、报告、姿态事件、姿态阈值、语音状态、虚拟人状态 | `GET /api/interviews/{id}`、`POST /api/interviews/{id}/messages`、`POST /api/interviews/{id}/finish`、`POST /api/posture-events`、`GET /api/posture-thresholds` | 本人面试 | 已按 UITemp 推进 |
| 管理员面试详情 | `/admin/interviews/:id` | `InterviewRoomView.vue` | 任意面试只读详情、姿态事件、报告 | `GET /api/admin/interviews/{id}` | 管理员 | 功能保留，视觉跟随面试详情 |
| 历史记录页 | `/interviews` | `InterviewHistoryView.vue` | 个人面试记录、报告摘要 | `GET /api/interviews` | 登录用户本人 | 已按 UITemp 推进 |
| 管理员后台 | `/admin` | `AdminView.vue` | 岗位、用户、面试记录、姿态记录、阈值配置、虚拟人素材 | `/api/admin/**` | 管理员 | 未提供后台稿，暂不重绘 |
| 无权限页 | `/forbidden` | `ForbiddenView.vue` | 无权限提示和返回入口 | 无业务接口 | 登录用户越权后展示 | 已按浅色风格推进 |

## 字段映射

### 上传简历/岗位/面试官选择页

`GET /api/positions`

| 页面内容 | 接口字段 |
|---|---|
| 岗位 ID | `id` |
| 岗位名称 | `name` |
| 岗位描述 | `description` |
| 技术栈 | `techStack` |
| 面试难度 | `difficulty` |
| 是否启用 | `enabled` |
| 排序 | `sortOrder` |

`GET /api/interviewer-styles`

| 页面内容 | 接口字段 |
|---|---|
| 风格 ID | `id` |
| 风格名称 | `name` |
| 风格说明 | `description` |
| 场景说明 | `scenario` |
| 虚拟人配置 | `virtualHuman` |
| 虚拟人资源 key | `virtualHuman.key` |
| 虚拟人名称 | `virtualHuman.name` |
| 虚拟人说明 | `virtualHuman.description` |
| 虚拟人图片地址 | `virtualHuman.imageUrl` |
| 强调色 | `virtualHuman.accentColor` |
| 标签 | `virtualHuman.badge` |

`POST /api/resumes/parse`

| 页面内容 | 接口字段 |
|---|---|
| 文件名 | `fileName` |
| 简历摘要 | `summary` |
| 技能关键词 | `skills` |
| 项目经历 | `projects` |
| 解析提示 | `warnings` |
| 提取文本长度 | `extractedTextLength` |

`POST /api/interviews`

| 页面内容 | 接口字段 |
|---|---|
| 岗位选择 | `positionId` |
| 面试官风格选择 | `styleId` |
| 可选简历上下文 | `resume` |

### AI 面试页面

`GET /api/interviews/{id}`

| 页面内容 | 接口字段 |
|---|---|
| 面试 ID | `id` |
| 面试状态 | `status` |
| 轮次数 | `questionCount` |
| 岗位信息 | `position` |
| 面试官风格 | `style` |
| 虚拟人信息 | `style.virtualHuman` |
| 简历上下文 | `resume` |
| 对话消息 | `messages` |
| 姿态事件 | `postureEvents` |
| 报告 | `report` |
| 开始时间 | `startedAt` |
| 结束时间 | `endedAt` |

`POST /api/interviews/{id}/messages`

| 页面内容 | 接口字段 |
|---|---|
| 用户回答 | `content` |

`POST /api/interviews/{id}/finish`

| 页面内容 | 接口字段 |
|---|---|
| 结束面试 | 无请求体 |

`POST /api/posture-events`

| 页面内容 | 接口字段 |
|---|---|
| 面试 ID | `interviewId` |
| 事件类型 | `eventType` |
| 严重级别 | `severity` |
| 分数 | `score` |
| 说明 | `detail` |
| 发生时间 | `occurredAt` |

`GET /api/posture-thresholds`

| 页面内容 | 接口字段 |
|---|---|
| 阈值 ID | `id` |
| 事件类型 | `eventType` |
| 显示名称 | `displayName` |
| 说明 | `description` |
| 预警阈值 | `warningThreshold` |
| 严重阈值 | `criticalThreshold` |
| 是否启用 | `enabled` |

### 历史记录页

`GET /api/interviews`

| 页面内容 | 接口字段 |
|---|---|
| 面试 ID | `id` |
| 面试状态 | `status` |
| 岗位名称 | `positionName` |
| 面试官风格 | `styleName` |
| 是否使用简历 | `resumeUsed` |
| 总分 | `totalScore` |
| 开始时间 | `startedAt` |
| 结束时间 | `endedAt` |
| 更新时间 | `updatedAt` |

空状态：当 `GET /api/interviews` 返回空数组时，页面展示“暂无面试记录”类空状态，并引导用户进入 `/positions` 创建首次面试。该状态不需要新增接口。

## UI 与 API 差异记录

| 日期 | 页面 | 原型需要 | 当前接口提供 | 差异 | 处理建议 | 状态 |
|---|---|---|---|---|---|---|
| 2026-07-20 | 评估报告 | 下载 PDF 报告按钮 | 当前仅有报告结构化数据，无 PDF 导出接口 | 缺少 PDF 文件生成/下载能力 | 暂保留按钮外观占位；后续作为独立导出增强任务 | 待决策 |
| 2026-07-20 | 面试房间 | 真人视频素材/数字人视频画面 | 当前为摄像头预览、本地 SVG 虚拟人和资源 URL | 缺少正式视频/数字人素材服务 | 继续使用虚拟人 SVG 和降级占位，不阻断文字面试 | 已降级 |
| 2026-07-20 | 管理员后台 | 未提供后台高保真稿 | `/api/admin/**` 已可用 | 没有设计依据 | 暂不重绘后台，等待补稿后单独推进 | 待补稿 |
