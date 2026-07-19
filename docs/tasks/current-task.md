# 当前任务

## 任务编号

SCAFFOLD-001

## 任务名称

搭建前后端基础工程框架。

## 项目上下文

- 当前阶段：从 Harness 文档设计进入工程框架搭建；
- 当前已有文档骨架和根目录 `AGENTS.md`；
- 本次只创建前后端基础目录和可构建项目，不实现业务功能。

## 本次实现

- 创建 `backend/` Spring Boot 3 + Maven 基础工程；
- 创建 `frontend/` Vue 3 + TypeScript + Vite 基础工程；
- 预留后端分层目录；
- 预留前端 api、router、stores、views、components、services、types 目录；
- 创建根目录环境示例和验证脚本；
- 保证后端测试、前端构建可以运行。

## 本次不实现

- 不写业务代码；
- 不实现登录、JWT、权限；
- 不接入大模型；
- 不实现面试、报告、后台管理；
- 不实现姿态检测、语音或虚拟人；
- 不设计数据库表字段细节。

## 完成标准

- `backend/` 存在 Maven Spring Boot 工程；
- `frontend/` 存在 Vue 3 TypeScript Vite 工程；
- 后端 `mvn test` 通过；
- 前端 `npm run build` 通过；
- 文档说明当前已有框架但尚无业务功能。

## 当前结果

已完成，等待人工审阅。

## 验证结果

- 后端 `mvn test` 通过；
- 前端 `npm run build` 通过。
