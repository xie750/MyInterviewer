# 系统架构设计

## 架构方向

项目采用前后端分离架构：

```text
Vue 3 前端
  -> REST API
Spring Boot 3 后端
  -> MySQL 8
  -> 本地规则型 AI 服务封装
  -> 姿态阈值和虚拟人素材配置
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
- `services/`：语音、姿态检测、虚拟人等前端能力封装；当前 `voice.ts` 使用浏览器 Web Speech API 做本地语音识别和播报降级，`posture.ts` 使用摄像头本地采样、可选 `FaceDetector` 和后端阈值配置生成结构化事件，`virtualHuman.ts` 将后端虚拟人 key 或图片地址解析为展示模型并提供占位降级和轻量状态动作解析；
- `types/`：接口和业务类型定义。

## 当前决策状态

以下内容待确认：

- 大模型供应商；
- 后端语音识别和语音合成供应商；
- 是否引入 Redis；
- 生产部署方式和运维策略。

以下内容已在框架阶段确定：

- 后端使用 Maven；
- 后端基础包名为 `com.kedaxunfei.myinterviewer`；
- 后端 ORM 采用 MyBatis-Plus；
- 后端数据库迁移采用 Flyway；
- 后端 AI 能力通过 `InterviewAiService` 统一封装，v0.1 默认使用本地规则型实现；
- 前端使用 Vue 3 + TypeScript + Vite；
- 前端已引入 Pinia、Vue Router、Axios 和 Element Plus。
- v0.3 语音输入与问题播报优先使用浏览器本地 Web Speech API，不新增后端音频上传、转写或合成接口。
- v0.4 摄像头姿态检测优先在浏览器本地完成，后端只保存结构化姿态事件，不接收原始视频帧。
- v0.5 虚拟人基础展示使用面试官风格内置配置和前端静态资源映射，不新增素材上传或第三方数字人服务。
- v0.6 虚拟人状态动作全部由前端根据页面状态推导，不新增后端动作接口，不上传音频、视频或动作数据。
- v0.7 管理后台新增姿态记录、姿态阈值配置和虚拟人素材元数据管理；仍不上传视频、音频或媒体文件本体。
- v1.0 为本地完整交付版，默认不依赖外部大模型、语音或数字人供应商。
