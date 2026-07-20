# AI 模拟面试网站

Spring Boot 3 + Vue 3 前后端分离的 AI 模拟面试网站，当前已完成 `v1.0` 本地完整交付版。

## 已交付能力

- 账号密码登录、JWT 鉴权、普通用户/管理员双角色隔离；
- 岗位选择、面试官风格选择、文字 AI 多轮面试、报告和历史记录；
- 简历临时解析，支持 `txt`、`md`、`pdf`、`docx`，不保存原始文件；
- 浏览器本地语音听写和 AI 问题播报，失败时降级到文字；
- 浏览器本地摄像头轻量姿态检测，只上报结构化事件；
- 虚拟人面试官展示和提问、等待、倾听、思考、总结、只读状态动作；
- 管理员后台：岗位、用户、全站面试、全站姿态记录、姿态阈值、虚拟人素材管理；
- 后端测试和前端生产构建通过。

## 技术栈

- 后端：Spring Boot 3、JDK 17、Maven、MyBatis-Plus、Flyway、JWT、MySQL 8；
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios、Element Plus；
- 测试：Spring Boot Test、MockMvc、H2。

## 本地启动

后端默认连接 MySQL，并通过 Flyway 自动执行迁移脚本。请先按 `backend/src/main/resources/application.yml` 准备数据库连接。

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

如需指定后端地址，在 `frontend/.env` 中设置：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## 演示账号

```text
user / password123
admin / password123
```

## 主要流程

```text
登录 -> 选择岗位/风格 -> 可选上传简历 -> 创建面试
-> 文字/语音/姿态辅助面试 -> AI 追问 -> 结束面试
-> 生成报告 -> 查看历史记录
```

管理员：

```text
登录管理员 -> 进入后台 -> 管理岗位/用户/面试记录/姿态记录/阈值/虚拟人素材
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

当前验证结果：

- 后端 `mvn test` 通过，共 27 个测试；
- 前端 `npm run build` 通过；
- 前端构建存在 Vite 大包提示和 VueUse 纯注释提示，不影响当前功能运行。

## 隐私与降级

- 简历原文件不保存，面试结束后清理临时解析上下文；
- 摄像头原始视频流、截图、音频流和虚拟人动作数据不上传后端；
- 姿态阈值读取失败时前端使用默认阈值；
- 语音能力不可用时使用文字输入；
- 摄像头权限拒绝或不可用时关闭姿态检测，不阻断面试；
- 虚拟人图片不可用时展示占位。

## 文档入口

- [AGENTS.md](AGENTS.md)：长期开发规则；
- [docs/product/current-scope.md](docs/product/current-scope.md)：当前真实范围；
- [docs/product/roadmap.md](docs/product/roadmap.md)：版本路线图；
- [docs/architecture/api-contract.md](docs/architecture/api-contract.md)：接口契约；
- [docs/architecture/database-design.md](docs/architecture/database-design.md)：数据库设计；
- [docs/architecture/permission-design.md](docs/architecture/permission-design.md)：权限设计；
- [docs/architecture/privacy-design.md](docs/architecture/privacy-design.md)：隐私设计；
- [docs/releases/v1.0-delivery.md](docs/releases/v1.0-delivery.md)：完整交付说明；
- [docs/tasks/final-acceptance.md](docs/tasks/final-acceptance.md)：最终验收清单。

## 已知限制

- 默认 AI 服务是本地规则型 `InterviewAiService`，未接入真实大模型；
- 语音依赖浏览器 Web Speech API；
- 姿态检测是浏览器端轻量检测，不是正式姿态模型；
- 虚拟人素材管理保存元数据和图片地址，不做媒体文件上传；
- 当前不包含 Docker Compose、HTTPS、正式域名和生产监控。
