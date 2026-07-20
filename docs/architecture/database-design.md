# 数据库设计

## v1.0 已实现实体

| 表名 | 阶段 | 作用 |
|---|---|---|
| `sys_user` | MVP | 用户、密码摘要、角色、状态 |
| `job_position` | MVP | 岗位、技术栈、难度、提示词模板 |
| `interviewer_style` | MVP/v0.5 | 面试官风格、专属提示词和内置虚拟人展示配置 |
| `interview_session` | MVP/v0.2 | 单场面试基本信息、状态和临时简历上下文 |
| `interview_message` | MVP | AI 与用户的问答消息 |
| `interview_report` | MVP | 评分和总结报告 |
| `posture_event` | v0.4 | 单场面试的结构化姿态/画面状态事件 |
| `virtual_human_asset` | v0.7 | 管理员维护的虚拟人素材元数据 |
| `posture_threshold_config` | v0.7 | 管理员维护的姿态检测阈值配置 |

## 关键表结构

### `sys_user`

- `username` 唯一；
- `password_hash` 使用 BCrypt；
- `role` 为 `USER` 或 `ADMIN`；
- `status` 为 `ENABLED` 或 `DISABLED`。

默认演示账号：

```text
user / password123
admin / password123
```

### `job_position`

- `name` 唯一；
- `prompt_template` 用于组装 AI 面试上下文；
- `enabled=false` 的岗位不出现在普通用户选择列表。

### `interviewer_style`

- `name` 唯一；
- `prompt_template` 定义追问语气和侧重点；
- `virtual_human_key`：前端内置虚拟人资源映射标识；
- `virtual_human_name`：虚拟人展示名称；
- `virtual_human_description`：虚拟人展示说明；
- `enabled=false` 的风格不出现在普通用户选择列表。

当前初始化风格：

```text
严厉压力面
温和引导面
技术深挖面
HR 综合面
```

### `interview_session`

- `user_id`：所属用户；
- `position_id`：本场岗位；
- `style_id`：本场面试官风格；
- `resume_used`：本场是否使用过简历解析结果；
- `resume_summary`、`resume_skills`、`resume_projects`、`resume_warnings`：进行中面试的临时简历上下文；
- `status`：`IN_PROGRESS` 或 `COMPLETED`；
- `question_count`：已生成 AI 问题数量；
- `started_at`、`ended_at` 记录流程时间。

### `interview_message`

- `session_id`：所属面试；
- `role`：`ASSISTANT` 或 `USER`；
- `content`：文字问题或回答；
- `round_no`：对应轮次。

### `interview_report`

- `session_id` 唯一，一场面试最多一份报告；
- 包含总分、技术、表达、逻辑三个维度分；
- 包含总结、优势、改进项和建议。

### `posture_event`

- `session_id`：所属面试；
- `user_id`：所属用户，用于权限隔离；
- `event_type`：结构化事件类型，如 `FACE_MISSING`、`LOW_LIGHT`、`CAMERA_UNAVAILABLE`；
- `severity`：`INFO`、`WARNING` 或 `CRITICAL`；
- `score`：0 到 100 的事件分数；
- `detail`：前端生成的简短说明，不包含原始视频帧；
- `occurred_at`、`created_at`：事件发生和入库时间。

## 关系

- 一个用户可以有多场面试；
- 一场面试属于一个用户；
- 一场面试选择一个岗位和一种面试官风格；
- 一场面试可选使用一份简历解析结果作为临时上下文；
- 一场面试包含多条消息；
- 一场已结束面试生成一份报告；
- 一场面试可包含多条结构化姿态异常事件。

## 迁移规则

数据库变更通过 Flyway 迁移脚本演进，已发布迁移原则上不直接改写。当前迁移：

```text
V001__initial_auth_schema.sql
V002__add_job_position.sql
V003__add_interview_mvp_tables.sql
V004__add_resume_context_to_interview_session.sql
V005__add_posture_event.sql
V006__add_virtual_human_to_interviewer_style.sql
V007__add_admin_delivery_tables.sql
```

根目录 `database/migrations/` 保留同名迁移副本，便于独立查看数据库演进。

## 简历上下文清理

v0.2 不创建独立简历表，不保存原始简历文件。`interview_session` 中的简历上下文字段只服务进行中的当前面试；面试结束生成报告后，系统清理 `resume_summary`、`resume_skills`、`resume_projects`、`resume_warnings`，保留 `resume_used` 作为历史展示标识。

## 姿态事件隐私边界

v0.4 起只保存结构化姿态事件，不保存原始视频流、截图或帧数据。事件用于本人面试详情、管理员只读详情和 v0.7 全站姿态记录后台；阈值配置保存在 `posture_threshold_config` 中，仍只影响浏览器本地检测逻辑。

## 虚拟人展示配置边界

v0.5 到 v0.6 阶段，虚拟人配置以字段形式绑定在 `interviewer_style` 上，只保存内置资源 key、展示名称和说明。v0.7 新增 `virtual_human_asset` 作为管理员可维护的素材元数据表，但仍不保存上传媒体文件。

### `virtual_human_asset`

- `asset_key`：资源 key，唯一，可对应前端内置资源；
- `name`、`description`：展示名称和说明；
- `image_url`：可选外部图片地址，不做后端文件存储；
- `accent_color`、`badge`：前端预览和展示辅助信息；
- `enabled`、`sort_order`：后台启停和排序。

### `posture_threshold_config`

- `event_type`：姿态事件类型，唯一；
- `display_name`、`description`：后台展示文案；
- `warning_threshold`、`critical_threshold`：前端本地检测使用的阈值；
- `enabled`、`sort_order`：启停和排序。

阈值只影响浏览器端结构化检测逻辑，不改变“原始视频不上传”的隐私边界。
