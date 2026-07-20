# AI 模拟面试网站

本仓库用于开发“AI 模拟面试网站”，采用 Spring Boot 3 + Vue 3 前后端分离架构。

当前已完成 `v0.4`：登录鉴权、双角色权限、岗位选择、面试官风格选择、可选简历上传与临时解析、文字 AI 面试、浏览器本地语音听写与问题播报、摄像头本地轻量姿态检测、多轮追问、报告、个人历史记录和基础管理员后台。

## MVP 流程

```text
登录 -> 选择岗位 -> 选择面试官风格 -> 可选上传简历 -> 文字/语音听写/姿态辅助 AI 面试
-> AI 追问 -> 结束面试 -> 生成报告 -> 查看历史记录
```

当前 AI 能力通过后端 `InterviewAiService` 统一封装，默认使用本地规则型实现，无需外部大模型 API Key 即可跑通闭环。简历上传支持 `txt`、`md`、`pdf`、`docx`，单文件不超过 10MB；原始文件不保存，面试结束后清理会话中的临时解析上下文。语音输入与问题播报使用浏览器 Web Speech API，应用不上传或保存原始音频；摄像头检测只在浏览器本地处理，后端只保存结构化姿态事件。

## 文档入口

- [AGENTS.md](AGENTS.md)：AI 开发代理必须遵守的长期规则。
- [docs/product/current-scope.md](docs/product/current-scope.md)：当前系统真实状态。
- [docs/product/roadmap.md](docs/product/roadmap.md)：版本路线图。
- [docs/releases/v0.1-mvp.md](docs/releases/v0.1-mvp.md)：MVP 版本说明。
- [docs/releases/v0.2-resume.md](docs/releases/v0.2-resume.md)：简历上传与临时解析版本说明。
- [docs/releases/v0.3-voice.md](docs/releases/v0.3-voice.md)：语音输入与问题播报版本说明。
- [docs/releases/v0.4-posture.md](docs/releases/v0.4-posture.md)：摄像头姿态检测版本说明。
- [docs/tasks/current-task.md](docs/tasks/current-task.md)：当前任务边界。

## 技术栈

- 后端：Spring Boot 3、JDK 17、Maven、MyBatis-Plus、Flyway、JWT、MySQL 8；
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios、Element Plus；
- 测试：Spring Boot Test、MockMvc、H2。

## 本地启动

后端默认连接 MySQL，并会通过 Flyway 自动执行迁移脚本：

```powershell
cd backend
mvn spring-boot:run
```

演示账号：

```text
user / password123
admin / password123
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

前端默认通过 `/api` 访问后端。如需指定后端地址，可在 `frontend/.env` 中设置：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## 验证

后端：

```powershell
cd backend
mvn test
```

前端：

```powershell
cd frontend
npm run build
```

当前验证结果：后端 25 个测试通过，前端类型检查和生产构建通过。
