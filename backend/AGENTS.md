# Backend AGENTS.md

本目录是 AI 模拟面试网站后端工程，采用 Spring Boot 3 + Maven。

## 当前阶段

当前只搭建基础工程框架，不包含业务接口、鉴权逻辑、数据库表或大模型调用。

## 目录约定

- `config`：跨域、OpenAPI、鉴权等配置；
- `security`：JWT、登录态、角色权限；
- `common`：统一响应、异常处理、公共常量；
- `controller`：REST API 控制器；
- `service`：业务服务；
- `repository`：数据库访问；
- `domain`：实体和值对象；
- `dto`：请求和响应对象；
- `integration`：大模型、语音、第三方资源服务封装。

## 开发规则

- 不在 Controller 中直接调用第三方大模型 API；
- 管理员接口必须放在 `/api/admin/**` 并做后端权限校验；
- 不在日志中输出密码、JWT、API Key、简历全文或视频数据；
- 数据库变更后续通过迁移脚本演进。

