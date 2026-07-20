# Database

数据库脚本目录。v0.1 当前通过 Flyway 迁移管理核心表结构。

当前迁移脚本：

- `migrations/V001__initial_auth_schema.sql`
- `migrations/V002__add_job_position.sql`
- `migrations/V003__add_interview_mvp_tables.sql`

后端运行时使用 `backend/src/main/resources/db/migration/` 下的同名脚本。根目录副本用于独立查看和归档。
