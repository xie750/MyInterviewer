# API 契约设计

## API 风格

- 使用 REST 风格；
- 所有响应采用统一结构；
- 所有业务接口默认需要登录；
- 管理员接口统一放在 `/api/admin/**` 下并做角色校验；
- API 文档后续可通过 OpenAPI/Swagger 暴露。

## 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

业务异常使用相同结构返回，HTTP 状态码与 `code` 对齐。

## v0.2 已实现接口

| 模块 | 路径 | 说明 |
|---|---|---|
| 认证 | `POST /api/auth/login` | 账号密码登录并返回 JWT |
| 当前用户 | `GET /api/me` | 获取当前登录用户 |
| 岗位 | `GET /api/positions` | 普通用户可选启用岗位 |
| 面试官风格 | `GET /api/interviewer-styles` | 普通用户可选启用风格 |
| 简历解析 | `POST /api/resumes/parse` | 登录用户上传简历并返回临时结构化解析结果 |
| 面试会话 | `GET /api/interviews` | 当前用户个人历史 |
| 面试会话 | `POST /api/interviews` | 创建文字面试并生成首问，可选携带简历解析结果 |
| 面试会话 | `GET /api/interviews/{id}` | 当前用户面试详情 |
| 面试消息 | `POST /api/interviews/{id}/messages` | 提交回答并生成追问 |
| 面试结束 | `POST /api/interviews/{id}/finish` | 结束面试并生成报告 |
| 面试报告 | `GET /api/interviews/{id}/report` | 当前用户报告 |
| 后台岗位 | `GET/POST/PUT/PATCH /api/admin/positions` | 岗位基础管理 |
| 后台用户 | `GET /api/admin/users` | 用户列表 |
| 后台用户 | `PATCH /api/admin/users/{id}/status` | 启停普通用户 |
| 后台面试记录 | `GET /api/admin/interviews` | 全站面试记录 |
| 后台面试详情 | `GET /api/admin/interviews/{id}` | 管理员只读查看任意面试详情 |

## 关键请求示例

### 创建面试

```json
{
  "positionId": 1,
  "styleId": 1,
  "resume": {
    "summary": "候选人熟悉 Java、Spring Boot、MySQL，主导过智能面试平台项目。",
    "skills": ["Java", "Spring Boot", "MySQL"],
    "projects": ["智能面试平台项目，负责登录鉴权和面试报告生成"],
    "warnings": ["建议补充量化指标"]
  }
}
```

`resume` 可省略。响应中的 `data.messages[0]` 是 AI 首次提问；携带简历时首问会结合简历摘要。

### 简历解析

`POST /api/resumes/parse` 使用 `multipart/form-data`，字段名为 `file`。当前支持 `txt`、`md`、`pdf`、`docx`，单文件不超过 10MB。

响应示例：

```json
{
  "fileName": "resume.txt",
  "summary": "技能关键词：Java、Spring Boot。代表经历：智能面试平台项目。",
  "skills": ["Java", "Spring Boot"],
  "projects": ["智能面试平台项目，负责登录鉴权和报告生成"],
  "warnings": ["建议补充量化指标"],
  "extractedTextLength": 240
}
```

后端不保存原始简历文件。解析失败、格式不支持或文件过大返回 `400`。

### 提交回答

```json
{
  "content": "我负责设计登录鉴权、权限隔离、异常处理和接口测试。"
}
```

响应返回更新后的面试详情，并追加用户回答与 AI 追问。

### 结束面试

`POST /api/interviews/{id}/finish` 无请求体。首次结束时生成 `interview_report`，重复调用返回既有结果。

### 用户状态

```json
{
  "status": "DISABLED"
}
```

基础用户管理只允许启停普通用户，不启停管理员账号。

## 后续接口模块

- `/api/voice/*`：语音识别和语音合成；
- `/api/posture-events`：姿态异常结果上报；
- `/api/admin/posture-events`：姿态记录后台；
- `/api/admin/posture-config`：姿态阈值配置；
- `/api/admin/virtual-humans`：虚拟人素材管理。
