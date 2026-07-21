# CR-UI-TEMP-USER-FLOW-01 基于 UITemp 的用户端主流程页面还原

## 当前行为

- 前端用户端主流程功能已经可用，但视觉与 `UITemp/` 本地导出稿不一致。
- 当前 UI Harness 已建立 `docs/ui/` 文档，但此前主要记录 MasterGo MCP 读取阻塞。
- `UITemp/` 已提供上传简历、岗位选择、进入面试、面试中、报告生成、报告、历史记录和空状态等 PNG 导出图。

## 目标行为

- 在不改后端接口、数据库、权限和隐私策略的前提下，按 `UITemp` 能确认的页面推进用户端 UI。
- 页面继续使用真实 API，不新增假字段和假接口。
- 资源不可用时沿用现有降级策略，不阻断文字面试主流程。

## 新增内容

- 浅色流程页视觉体系：入口、登录、无权限、上传简历/岗位选择、历史记录、报告。
- 深色面试房视觉体系：双视频区、右侧面试官资料、问题气泡、语音/摄像头/回答控制。
- 评估报告独立页面：综合分环形图、指标条、雷达图、亮点/建议、对话记录。
- 历史记录空状态：插画化空状态和进入首次面试入口。

## 修改内容

- `frontend/src/views/HomeView.vue`
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/ForbiddenView.vue`
- `frontend/src/views/PositionsView.vue`
- `frontend/src/views/InterviewHistoryView.vue`
- `frontend/src/views/InterviewRoomView.vue`
- `frontend/src/services/virtualHuman.ts`
- `frontend/src/styles/main.css`
- `docs/tasks/current-task.md`
- `docs/ui/ui-progress.md`
- `docs/ui/page-api-mapping.md`
- `docs/product/current-scope.md`
- `docs/tasks/completed.md`

## 保持不变

- 后端接口契约保持不变。
- JWT 登录、路由拦截、管理员权限和本人数据权限保持不变。
- 简历文件不保存、音视频流不上送后端、姿态事件只上传结构化结果的隐私策略保持不变。
- 语音、摄像头、虚拟人资源不可用时的降级策略保持不变。

## 本次不做

- 不实现真实 PDF 下载接口。
- 不接入真实视频素材、真人头像素材、Live2D、3D 数字人或第三方数字人服务。
- 不重绘管理员后台，因为本轮没有后台高保真稿。
- 不进行 MasterGo 在线节点读取。

## 异常降级

- 简历解析失败：允许跳过简历。
- 语音识别失败：保留文字输入。
- 语音播报失败：只展示文字问题。
- 摄像头权限拒绝：关闭姿态检测，不阻断面试。
- 虚拟人资源不可用：展示内置 SVG 或占位头像。

## 验收标准

- 前端 `npm run build` 通过。
- 用户端主流程页面可以继续调用真实接口。
- 空状态、加载状态、提交状态和资源不可用状态有可见反馈。
- 后台功能不因全局样式调整而失去基础可用布局。

## 验证结果

- 2026-07-20：`cd frontend && npm run build` 通过。
- Vite 输出 chunk 体积警告，属于后续打包优化项，不影响本次 UI 交付。
- 本地未安装 Playwright，且本轮未启动后端，未完成 80%、100%、125% 缩放浏览器视觉验收。
