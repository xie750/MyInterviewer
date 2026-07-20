# 系统架构设计

## 架构方向

项目采用前后端分离架构：

```text
Vue 3 前端
  -> REST API
Spring Boot 3 后端
  -> MySQL 8
  -> 大模型服务
  -> 后续语音、虚拟人、姿态检测配置服务
```

## 后端分层建议

- Controller：接收请求、参数校验、返回统一响应；
- Service：业务编排、权限边界、AI 调用编排；
- Repository/Mapper：数据库访问；
- Security：JWT、登录态、角色权限；
- Integration：大模型、语音、第三方资源服务封装；
- Config：跨域、Swagger、鉴权、第三方配置。

## 前端分层建议

- `views/`：页面；
- `components/`：通用组件和业务组件；
- `api/`：接口请求封装；
- `stores/`：Pinia 状态；
- `router/`：路由和权限守卫；
- `services/`：语音、姿态检测、虚拟人等前端能力封装；当前 `voice.ts` 使用浏览器 Web Speech API 做本地语音识别和播报降级；
- `types/`：接口和业务类型定义。

## 当前决策状态

以下内容待确认：

- 大模型供应商；
- 后端语音识别和语音合成供应商；
- 虚拟人技术方案；
- 是否引入 Redis；
- 部署方式。

以下内容已在框架阶段确定：

- 后端使用 Maven；
- 后端基础包名为 `com.kedaxunfei.myinterviewer`；
- 后端 ORM 采用 MyBatis-Plus；
- 后端数据库迁移采用 Flyway；
- 后端 AI 能力通过 `InterviewAiService` 统一封装，v0.1 默认使用本地规则型实现；
- 前端使用 Vue 3 + TypeScript + Vite；
- 前端已引入 Pinia、Vue Router、Axios 和 Element Plus。
- v0.3 语音输入与问题播报优先使用浏览器本地 Web Speech API，不新增后端音频上传、转写或合成接口。
