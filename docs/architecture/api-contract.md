# API 契约设计

## API 风格

- 使用 REST 风格；
- 所有响应采用统一结构；
- 所有业务接口默认需要登录；
- 管理员接口统一放在 `/api/admin/**` 下并做角色校验；
- API 文档后续通过 OpenAPI/Swagger 暴露。

## MVP 接口模块

| 模块 | 示例路径 | 说明 |
|---|---|---|
| 认证 | `/api/auth/login` | 登录并返回 JWT |
| 当前用户 | `/api/me` | 当前用户信息 |
| 岗位 | `/api/positions` | 岗位列表与详情 |
| 面试官风格 | `/api/interviewer-styles` | 风格列表 |
| 面试会话 | `/api/interviews` | 创建、查询、结束面试 |
| 面试消息 | `/api/interviews/{id}/messages` | 用户回答与 AI 追问 |
| 面试报告 | `/api/interviews/{id}/report` | 查看报告 |
| 后台用户 | `/api/admin/users` | 用户管理 |
| 后台岗位 | `/api/admin/positions` | 岗位管理 |
| 后台面试记录 | `/api/admin/interviews` | 全站面试记录 |

## 后续接口模块

- `/api/resumes/parse`：简历临时解析；
- `/api/voice/*`：语音识别和语音合成；
- `/api/posture-events`：姿态异常结果上报；
- `/api/admin/posture-events`：姿态记录后台；
- `/api/admin/posture-config`：姿态阈值配置；
- `/api/admin/virtual-humans`：虚拟人素材管理。

