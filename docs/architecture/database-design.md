# 数据库设计

## 初步实体

| 表名 | 阶段 | 作用 |
|---|---|---|
| `sys_user` | MVP | 用户、密码摘要、角色、状态 |
| `job_position` | MVP | 岗位、技术栈、难度、提示词模板 |
| `interviewer_style` | MVP | 面试官风格和专属提示词 |
| `interview_session` | MVP | 单场面试基本信息和状态 |
| `interview_message` | MVP | AI 与用户的问答消息 |
| `interview_report` | MVP | 评分和总结报告 |
| `posture_event` | 后续 | 姿态异常事件 |
| `posture_config` | 后续 | 姿态检测阈值 |
| `virtual_human` | 后续 | 虚拟人素材配置 |

## 关系

- 一个用户可以有多场面试；
- 一场面试属于一个用户；
- 一场面试选择一个岗位；
- 一场面试选择一种面试官风格；
- 一场面试包含多条消息；
- 一场面试生成一份报告；
- 后续一场面试可包含多条姿态异常事件。

## 迁移规则

数据库变更应通过迁移脚本逐步演进：

```text
V001__initial_schema.sql
V002__add_resume_context.sql
V003__add_voice_fields.sql
V004__add_posture_tables.sql
V005__add_virtual_human_tables.sql
```

已发布迁移脚本原则上不直接修改。

