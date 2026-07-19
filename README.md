# AI 模拟面试网站

本仓库用于开发“AI 模拟面试网站”，采用 Spring Boot 3 + Vue 3 前后端分离架构。

当前阶段已完成 Harness 文档设计和前后端基础工程搭建，尚未开始完整业务代码开发。项目先完成 MVP 基础闭环，再逐步加入简历解析、语音面试、姿态检测、虚拟人和后台进阶管理能力。

## 目标闭环

完整目标流程：

```text
登录 -> 上传简历 -> 选择岗位 -> 选择面试官风格 -> 开启摄像头姿态检测
-> AI 虚拟人提问 -> 用户语音/文字回答 -> AI 追问
-> 结束面试 -> 生成面试报告和姿态检测报告
```

MVP 优先流程：

```text
登录 -> 选择岗位 -> 选择面试官风格 -> 文字 AI 面试
-> AI 追问 -> 结束面试 -> 生成报告 -> 查看历史记录
```

## 文档入口

- [AGENTS.md](AGENTS.md)：AI 开发代理必须遵守的长期规则。
- [docs/product/product-overview.md](docs/product/product-overview.md)：产品目标与用户角色。
- [docs/product/current-scope.md](docs/product/current-scope.md)：当前系统真实状态。
- [docs/product/roadmap.md](docs/product/roadmap.md)：版本路线图。
- [docs/releases/v0.1-mvp.md](docs/releases/v0.1-mvp.md)：MVP 版本说明。
- [docs/tasks/current-task.md](docs/tasks/current-task.md)：当前任务边界。

## 技术方向

- 后端：Spring Boot 3、JDK 17、Maven、MySQL 8、JWT、OpenAPI/Swagger。
- 前端：Vue 3、TypeScript、Vite、Pinia、Element Plus 或按设计稿自定义。
- AI：统一封装大模型调用层，面试提示词、岗位模板、面试官风格配置化。
- 进阶：浏览器端姿态检测、语音识别/合成、2D/轻量 3D 虚拟人。

## 当前工程结构

```text
backend/    Spring Boot 3 后端基础工程
frontend/   Vue 3 + TypeScript + Vite 前端基础工程
database/   数据库脚本占位目录
docs/       Harness 文档
scripts/    本地验证脚本
tests/      API 与 E2E 测试占位目录
```

## 本地启动

后端：

```powershell
cd backend
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

验证：

```powershell
.\scripts\verify.ps1
```

当前工程只包含基础框架，不包含登录、面试、后台管理等业务功能。
