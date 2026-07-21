<script setup lang="ts">
import {
  Calendar,
  Delete,
  Document,
  InfoFilled,
  Search,
  UserFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchInterviewsApi, deleteInterviewApi } from '@/api/interviews'
import type { InterviewSummary, PageResponse } from '@/types'

const router = useRouter()
const loading = ref(false)
const page = ref(1)
const pageSize = ref(6)
const pageData = ref<PageResponse<InterviewSummary>>({
  items: [],
  total: 0,
  page: 1,
  pageSize: 6,
})

function scoreClass(score: number | null): string {
  if (score == null) return ''
  if (score >= 80) return 'score-green'
  if (score >= 60) return 'score-orange'
  return 'score-red'
}

async function loadInterviews() {
  loading.value = true
  try {
    pageData.value = await fetchInterviewsApi(page.value, pageSize.value)
  } catch {
    ElMessage.error('历史记录加载失败')
  } finally {
    loading.value = false
  }
}

function formatDate(value: string) {
  const d = new Date(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function resumeLabel(rec: InterviewSummary) {
  if (rec.resumeFileName) return rec.resumeFileName
  return rec.resumeUsed ? '已使用简历' : '未使用简历'
}

async function handleDelete(rec: InterviewSummary) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除该面试记录吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteInterviewApi(rec.id)
    ElMessage.success('删除成功')
    if (pageData.value.items.length === 1 && page.value > 1) {
      page.value--
    }
    await loadInterviews()
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}

function goToReport(rec: InterviewSummary) {
  if (rec.status === 'COMPLETED' && rec.totalScore != null) {
    router.push(`/interviews/${rec.id}`)
  }
}

function goToDetail(rec: InterviewSummary) {
  router.push(`/interviews/${rec.id}`)
}

function handlePageChange(newPage: number) {
  page.value = newPage
  loadInterviews()
}

function handleSizeChange(newSize: number) {
  pageSize.value = newSize
  page.value = 1
  loadInterviews()
}

onMounted(loadInterviews)
</script>

<template>
  <main class="history-page">
    <!-- 顶部品牌栏 -->
    <header class="history-topbar">
      <div class="brand-group">
        <div class="brand-avatar">
          <el-icon :size="28"><UserFilled /></el-icon>
        </div>
        <span class="brand-title">AI模拟面试</span>
      </div>
      <el-button type="primary" class="start-interview-btn" @click="router.push('/interviews/resume')">
        <el-icon style="margin-right: 4px"><Search /></el-icon>
        开始面试
      </el-button>
    </header>

    <!-- 顶部导航底下的白色毛玻璃蒙版条，内含标题 -->
    <div class="topbar-frosted-bar" v-if="pageData.items.length">
      <div class="frosted-bar-inner">
        <h1>面试记录 ({{ pageData.total }})</h1>
      </div>
    </div>

    <section class="history-body" v-loading="loading">
      <template v-if="pageData.items.length">
        <p class="result-label">面试结果</p>
        <div class="history-cards">
          <article v-for="rec in pageData.items" :key="rec.id" class="history-card">
            <div class="card-row">
              <!-- 信息区：单行横向排列 -->
              <div class="card-meta">
                <span class="meta-item">
                  <span class="meta-icon meta-icon-calendar">
                    <el-icon><Calendar /></el-icon>
                  </span>
                  <span class="meta-text">{{ formatDate(rec.startedAt) }}</span>
                </span>
                <span class="meta-divider" />
                <span class="meta-item">
                  <span class="meta-icon meta-icon-position">
                    <el-icon><Search /></el-icon>
                  </span>
                  <span class="meta-text meta-text-bold">{{ rec.positionName }}</span>
                  <el-icon class="meta-info-icon"><InfoFilled /></el-icon>
                </span>
                <span class="meta-divider" />
                <span class="meta-item">
                  <span class="meta-icon meta-icon-user">
                    <el-icon><UserFilled /></el-icon>
                  </span>
                  <span class="meta-text">{{ rec.displayName }}</span>
                </span>
                <span class="meta-divider" />
                <span class="meta-item">
                  <span class="meta-icon meta-icon-resume">
                    <el-icon><Document /></el-icon>
                  </span>
                  <span class="meta-text meta-text-resume">{{ resumeLabel(rec) }}</span>
                </span>
              </div>

              <!-- 右侧操作区 -->
              <div class="card-actions">
                <span v-if="rec.status === 'COMPLETED' && rec.totalScore != null"
                      :class="['card-score', scoreClass(rec.totalScore)]">
                  {{ rec.totalScore }}<span class="score-unit">分</span>
                </span>
                <el-button
                  v-if="rec.status === 'COMPLETED' && rec.totalScore != null"
                  type="primary"
                  class="report-btn"
                  @click="goToReport(rec)"
                >
                  <span class="btn-icon-circle">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                  </span>
                  查看报告
                </el-button>
                <el-button v-else type="primary" class="report-btn" @click="goToDetail(rec)">
                  <span class="btn-icon-circle">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                  </span>
                  查看详情
                </el-button>
                <el-button circle class="delete-btn" @click="handleDelete(rec)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </article>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrap" v-if="pageData.total > pageSize">
          <div class="pagination-inner">
            <el-pagination
              :current-page="page"
              :page-size="pageData.pageSize"
              :total="pageData.total"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
            <el-select
              :model-value="pageSize"
              size="small"
              class="page-size-select"
              @change="handleSizeChange"
            >
              <el-option :value="6" label="6条/页" />
              <el-option :value="10" label="10条/页" />
              <el-option :value="20" label="20条/页" />
            </el-select>
          </div>
        </div>
      </template>

      <template v-else-if="!loading">
        <div class="empty-state">
          <div class="empty-illustration glass-card" aria-hidden="true">
            <svg class="illustration-svg" viewBox="0 0 760 220" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <filter id="glow">
                  <feGaussianBlur stdDeviation="3" result="g"/>
                  <feMerge><feMergeNode in="g"/><feMergeNode in="SourceGraphic"/></feMerge>
                </filter>
                <linearGradient id="aiGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="#60a5fa"/><stop offset="1" stop-color="#3b82f6"/>
                </linearGradient>
              </defs>

              <!-- Soft wave background -->
              <rect x="-40" y="8" width="840" height="204" rx="40" fill="#dce9ff" fill-opacity="0.22"/>
              <rect x="-40" y="28" width="840" height="164" rx="30" fill="#dce9ff" fill-opacity="0.16"/>
              <rect x="-40" y="48" width="840" height="124" rx="20" fill="#dce9ff" fill-opacity="0.10"/>

              <!-- Left card: AI面试官 -->
              <rect x="88" y="22" width="130" height="118" rx="14" fill="#fff" fill-opacity="0.93" stroke="#e0e8ff" stroke-width="0.8"/>
              <text x="102" y="42" fill="#1e293b" font-size="10" font-weight="700">AI面试官</text>
              <circle cx="153" cy="76" r="16" fill="url(#aiGrad)" opacity="0.18"/>
              <circle cx="153" cy="76" r="11" fill="url(#aiGrad)" opacity="0.48"/>
              <circle cx="153" cy="71" r="4.5" fill="#fff" opacity="0.95"/>
              <path d="M140 84c0-7.18 5.82-13 13-13s13 5.82 13 13" fill="#fff" opacity="0.95"/>
              <text x="100" y="114" fill="#facc15" font-size="9" font-weight="700">✦</text>
              <text x="112" y="114" fill="#3b82f6" font-size="9" font-weight="700">AI实时问答</text>
              <rect x="102" y="122" width="40" height="5" rx="2.5" fill="#f1f5f9"/>

              <!-- Center card: AI简历分析 -->
              <rect x="240" y="28" width="280" height="170" rx="16" fill="#fff" fill-opacity="0.95" stroke="#dde6ff" stroke-width="1"/>
              <text x="256" y="52" fill="#1e293b" font-size="13" font-weight="700">AI简历分析</text>
              <text x="498" y="52" fill="#60a5fa" font-size="10" text-anchor="end">✦ 分析中...</text>
              <line x1="250" y1="60" x2="510" y2="60" stroke="#f1f5f9" stroke-width="0.8"/>
              <text x="256" y="76" fill="#94a3b8" font-size="9" font-weight="600">AI简历报告</text>
              <circle cx="280" cy="104" r="18" fill="url(#aiGrad)" opacity="0.14"/>
              <circle cx="280" cy="104" r="12" fill="url(#aiGrad)" opacity="0.42"/>
              <circle cx="280" cy="98" r="4" fill="#fff" opacity="0.95"/>
              <path d="M267 113c0-7.18 6.34-13 14.17-13S295.3 105.82 295.3 113" fill="#fff" opacity="0.95"/>
              <rect x="312" y="90" width="185" height="5" rx="2.5" fill="#f1f5f9"/>
              <rect x="312" y="100" width="150" height="4" rx="2" fill="#f8fafc"/>
              <rect x="312" y="108" width="170" height="4" rx="2" fill="#f8fafc"/>
              <rect x="312" y="116" width="125" height="4" rx="2" fill="#f8fafc"/>
              <rect x="312" y="124" width="155" height="4" rx="2" fill="#f8fafc"/>
              <rect x="312" y="132" width="85" height="4" rx="2" fill="#f8fafc"/>
              <rect x="312" y="140" width="60" height="4" rx="2" fill="#f8fafc"/>
              <rect x="256" y="154" width="48" height="16" rx="4" fill="#f1f5f9"/>
              <text x="280" y="166" fill="#475569" font-size="8" text-anchor="middle">个人信息</text>
              <rect x="308" y="154" width="48" height="16" rx="4" fill="#f1f5f9"/>
              <text x="332" y="166" fill="#475569" font-size="8" text-anchor="middle">教育经历</text>
              <rect x="360" y="154" width="60" height="16" rx="4" fill="#f1f5f9"/>
              <text x="390" y="166" fill="#475569" font-size="8" text-anchor="middle">工作/实习经历</text>
              <rect x="256" y="174" width="48" height="16" rx="4" fill="#f1f5f9"/>
              <text x="280" y="186" fill="#475569" font-size="8" text-anchor="middle">技能特长</text>
              <rect x="308" y="174" width="48" height="16" rx="4" fill="#f1f5f9"/>
              <text x="332" y="186" fill="#475569" font-size="8" text-anchor="middle">自我评价</text>
              <rect x="360" y="174" width="28" height="16" rx="4" fill="#f1f5f9"/>
              <text x="374" y="186" fill="#475569" font-size="8" text-anchor="middle">...</text>

              <!-- Right card: AI多维评估报告 -->
              <rect x="548" y="28" width="140" height="128" rx="14" fill="#fff" fill-opacity="0.93" stroke="#e0e8ff" stroke-width="0.8"/>
              <text x="562" y="50" fill="#1e293b" font-size="10" font-weight="700">AI多维评估报告</text>
              <path d="M618 76l14 8v16l-14 8-14-8V84z" fill="#60a5fa" fill-opacity="0.1" stroke="#60a5fa" stroke-width="1" stroke-opacity="0.35"/>
              <path d="M618 86l8 4.6v9.2l-8 4.6-8-4.6V90.6z" fill="url(#aiGrad)" fill-opacity="0.32"/>
              <path d="M618 86l8 4.6v9.2l-8 4.6-8-4.6V90.6z" stroke="#60a5fa" stroke-width="1" fill="none"/>
              <circle cx="618" cy="96" r="7" fill="#3b82f6"/>
              <path d="M613 96l3.5 3.5 5.5-6.5" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>

              <!-- Soft wave decorations -->
              <ellipse cx="380" cy="158" rx="340" ry="42" fill="#dce9ff" fill-opacity="0.10"/>
              <ellipse cx="380" cy="168" rx="300" ry="28" fill="#dce9ff" fill-opacity="0.07"/>

              <!-- Central AI badge -->
              <circle cx="380" cy="132" r="36" fill="url(#aiGrad)" filter="url(#glow)"/>
              <circle cx="380" cy="132" r="30" fill="none" stroke="#fff" stroke-width="1.2" stroke-opacity="0.40"/>
              <circle cx="380" cy="132" r="20" fill="none" stroke="#fff" stroke-width="0.8" stroke-opacity="0.25"/>
              <text x="380" y="138" fill="#fff" font-size="16" font-weight="800" text-anchor="middle" letter-spacing="1">AI</text>
              <ellipse cx="380" cy="114" rx="14" ry="8" fill="#fff" fill-opacity="0.30"/>
              <ellipse cx="380" cy="113" rx="9" ry="5" fill="#fff" fill-opacity="0.40"/>
            </svg>
          </div>
          <h1 class="empty-title">暂无面试记录</h1>
          <p class="empty-subtitle">开始你的第一次AI模拟面试吧</p>
          <el-button type="primary" class="empty-action-btn" @click="router.push('/interviews/resume')">
            开始第一次面试
          </el-button>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.history-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #dce9ff 0%, #eef2ff 35%, #f5f7ff 65%, #ffffff 100%);
}

/* ====== 顶部品牌栏 ====== */
.history-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 40px;
  background: rgb(255 255 255 / 65%);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgb(210 220 240 / 50%);
}

.brand-group {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.brand-avatar {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.01em;
}

.start-interview-btn {
  border-radius: 8px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: none;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

/* ====== 顶部导航底下白色毛玻璃蒙版条 ====== */
.topbar-frosted-bar {
  width: 100%;
  background: rgb(255 255 255 / 65%);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgb(210 220 240 / 50%);
  position: sticky;
  top: 0;
  z-index: 10;
}

.frosted-bar-inner {
  width: min(1120px, calc(100% - 80px));
  margin: 0 auto;
  padding: 11px 0;
}

.frosted-bar-inner h1 {
  margin: 0;
  color: #1e293b;
  font-size: 17px;
  font-weight: 600;
}

/* ====== 内容标题 ====== */
.history-body {
  width: min(1120px, calc(100% - 80px));
  margin: 0 auto;
  padding: 20px 0 80px;
}

.history-body h1 {
  margin: 0 0 16px;
  color: #1e293b;
  font-size: 18px;
  font-weight: 600;
}

/* ====== 记录卡片 ====== */
.history-cards {
  display: grid;
  gap: 16px;
}

.result-label {
  margin: 0 0 16px;
  color: #334155;
  font-size: 15px;
  font-weight: 500;
}

.history-card {
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 4px 12px rgba(37, 99, 235, 0.04);
  overflow: hidden;
}

.card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 24px;
  border-left: 4px solid #2563eb;
}

/* 信息区：单行横向 */
.card-meta {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 0;
  min-width: 0;
  flex: 1;
  color: #475569;
  font-size: 14px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 8px;
  white-space: nowrap;
  line-height: 1.4;
}

.meta-icon {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 6px;
  font-size: 13px;
  flex-shrink: 0;
}

.meta-icon-calendar {
  background: #eff6ff;
  color: #2563eb;
}

.meta-icon-position {
  background: #eff6ff;
  color: #2563eb;
}

.meta-icon-user {
  background: #f5f3ff;
  color: #7c3aed;
}

.meta-icon-resume {
  background: #f0fdfa;
  color: #0d9488;
}

.meta-text {
  color: #475569;
  font-size: 13px;
}

.meta-text-bold {
  color: #1e293b;
  font-weight: 600;
  font-size: 14px;
}

.meta-text-resume {
  color: #2563eb;
}

.meta-info-icon {
  color: #2563eb;
  font-size: 15px;
  cursor: help;
  margin-left: 2px;
}

.meta-divider {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
  border-radius: 1px;
  margin: 0 4px;
  flex-shrink: 0;
}

/* 右侧操作区 */
.card-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.card-score {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  margin-right: 4px;
}

.score-green {
  color: #22c55e;
}

.score-orange {
  color: #f59e0b;
}

.score-red {
  color: #ef4444;
}

.score-unit {
  font-size: 14px;
  font-weight: 500;
  margin-left: 1px;
}

.score-green .score-unit {
  color: #22c55e;
}

.score-orange .score-unit {
  color: #f59e0b;
}

.score-red .score-unit {
  color: #ef4444;
}

.report-btn {
  border-radius: 8px;
  font-weight: 600;
  background: #2563eb;
  border-color: #2563eb;
}

.btn-icon-circle {
  display: inline-grid;
  width: 18px;
  height: 18px;
  place-items: center;
  margin-right: 4px;
}

.delete-btn {
  color: #94a3b8;
  border-color: #e2e8f0;
}

.delete-btn:hover {
  color: #ef4444;
  border-color: #fecaca;
  background: #fef2f2;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 24px;
  gap: 12px;
}

.pagination-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-size-select {
  width: 120px;
}

/* ====== 空状态 ====== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 24px 56px;
  text-align: center;
}

.empty-illustration {
  position: relative;
  width: 100%;
  max-width: 720px;
  height: auto;
  aspect-ratio: 760 / 220;
  margin-bottom: 32px;
  border: 1px solid rgb(215 226 242 / 80%);
  border-radius: 12px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 22px 60px rgb(47 107 255 / 9%);
  padding: 24px;
}

.illustration-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.empty-title {
  margin: 0 0 10px;
  color: #111827;
  font-size: 22px;
  font-weight: 700;
}

.empty-subtitle {
  margin: 0 0 28px;
  color: #64748b;
  font-size: 15px;
}

.empty-action-btn {
  border-radius: 8px;
  font-weight: 600;
  padding: 10px 28px;
  background: #2563eb;
  border-color: #2563eb;
}

/* ====== 响应式 ====== */
@media (max-width: 860px) {
  .card-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .card-meta {
    flex-wrap: wrap;
  }

  .meta-divider {
    display: none;
  }
}

@media (max-width: 540px) {
  .history-topbar {
    padding: 0 18px;
  }

  .topbar-frosted-bar {
    padding: 10px 0;
  }

  .frosted-bar-inner {
    padding: 10px 0;
  }

  .frosted-bar-inner h1 {
    font-size: 16px;
  }

  .history-body {
    width: calc(100% - 36px);
    padding: 20px 0 60px;
  }

  .card-row {
    padding: 16px;
  }
}
</style>
