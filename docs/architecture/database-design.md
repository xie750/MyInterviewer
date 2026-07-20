# 数据库设计

## v0.2 已实现实体

| 表名 | 阶段 | 作用 |
|---|---|---|
| `sys_user` | MVP | 用户、密码摘要、角色、状态 |
| `job_position` | MVP | 岗位、技术栈、难度、提示词模板 |
| `interviewer_style` | MVP | 面试官风格和专属提示词 |
| `interview_session` | MVP/v0.2 | 单场面试基本信息、状态和临时简历上下文 |
| `interview_message` | MVP | AI 与用户的问答消息 |
| `interview_report` | MVP | 评分和总结报告 |

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

## 关系

- 一个用户可以有多场面试；
- 一场面试属于一个用户；
- 一场面试选择一个岗位和一种面试官风格；
- 一场面试可选使用一份简历解析结果作为临时上下文；
- 一场面试包含多条消息；
- 一场已结束面试生成一份报告；
- 后续一场面试可包含多条姿态异常事件。

## 迁移规则

数据库变更通过 Flyway 迁移脚本演进，已发布迁移原则上不直接改写。当前迁移：

```text
V001__initial_auth_schema.sql
V002__add_job_position.sql
V003__add_interview_mvp_tables.sql
V004__add_resume_context_to_interview_session.sql
```

根目录 `database/migrations/` 保留同名迁移副本，便于独立查看数据库演进。

## 简历上下文清理

v0.2 不创建独立简历表，不保存原始简历文件。`interview_session` 中的简历上下文字段只服务进行中的当前面试；面试结束生成报告后，系统清理 `resume_summary`、`resume_skills`、`resume_projects`、`resume_warnings`，保留 `resume_used` 作为历史展示标识。
