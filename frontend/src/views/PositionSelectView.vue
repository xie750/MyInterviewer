<script setup lang="ts">
import { Check, RefreshLeft, Select } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import { createInterviewApi } from '@/api/interviews'
import { fetchInterviewerStylesApi } from '@/api/interviewerStyles'
import { fetchPositionsApi } from '@/api/positions'
import { resolveInterviewerAsset } from '@/utils/interviewerAssets'
import type { InterviewerStyle, Position } from '@/types'

const router = useRouter()

const loading = ref(false)
const starting = ref(false)
const positions = ref<Position[]>([])
const styles = ref<InterviewerStyle[]>([])
const selectedPositionId = ref<number | null>(null)
const selectedStyleId = ref<number | null>(null)
const avatarLoadFailed = ref<Record<number, boolean>>({})

// 从 location state 获取简历信息
const routeState = (router.currentRoute.value as unknown as { state?: Record<string, unknown> }).state
const parsedResult = computed(() => {
  const raw = routeState?.parsedResult as { summary: string; skills: string[]; projects: string[]; warnings: string[] } | null | undefined
  return raw ?? null
})
const resumeFileName = computed(() => {
  return (routeState?.fileName as string | null | undefined) ?? null
})

const selectedPosition = computed(() => positions.value.find(p => p.id === selectedPositionId.value) ?? null)

function interviewerTagText(style: InterviewerStyle): string {
  return style.virtualHuman.badge || resolveInterviewerAsset(style.virtualHuman.key).badge
}

function interviewerAvatar(style: InterviewerStyle): string {
  return style.virtualHuman.imageUrl || resolveInterviewerAsset(style.virtualHuman.key).imageUrl
}

function interviewerDisplayName(style: InterviewerStyle): string {
  return style.virtualHuman.name || resolveInterviewerAsset(style.virtualHuman.key).displayName
}

function interviewerDescription(style: InterviewerStyle): string {
  return style.virtualHuman.description || resolveInterviewerAsset(style.virtualHuman.key).description
}

function markAvatarFailed(style: InterviewerStyle) {
  avatarLoadFailed.value = { ...avatarLoadFailed.value, [style.id]: true }
}

async function loadOptions() {
  loading.value = true
  try {
    const [posList, styleList] = await Promise.all([
      fetchPositionsApi(),
      fetchInterviewerStylesApi(),
    ])
    positions.value = posList
    styles.value = styleList
  } catch {
    ElMessage.error('面试配置加载失败')
  } finally {
    loading.value = false
  }
}

async function startInterview() {
  if (!selectedPosition.value) {
    ElMessage.warning('请先选择应聘岗位')
    return
  }
  if (!selectedStyleId.value) {
    ElMessage.warning('请选择面试官')
    return
  }
  if (starting.value) {
    return
  }

  starting.value = true
  try {
    const interview = await createInterviewApi({
      positionId: selectedPosition.value.id,
      styleId: selectedStyleId.value,
      resumeFileName: resumeFileName.value ?? undefined,
      resume: parsedResult.value
        ? {
            summary: parsedResult.value.summary,
            skills: parsedResult.value.skills,
            projects: parsedResult.value.projects,
            warnings: parsedResult.value.warnings,
          }
        : undefined,
    })
    await router.push(`/interviews/${interview.id}`)
  } catch (error) {
    const message = (error as any)?.response?.data?.message || '面试创建失败'
    ElMessage.error(message)
  } finally {
    starting.value = false
  }
}

function goBack() {
  router.push('/interviews/resume')
}

onMounted(loadOptions)
</script>

<template>
  <main class="position-select-page" v-loading="loading">
    <!-- 顶部导航栏 -->
    <header class="select-topbar">
      <button class="back-button" type="button" @click="goBack">
        <el-icon><RefreshLeft /></el-icon>
        返回
      </button>
      <div class="step-indicator">
        <span class="step-dot done">
          <el-icon><Check /></el-icon>
        </span>
        <span class="step-line done-line" />
        <span class="step-dot active">2</span>
      </div>
    </header>

    <!-- 主体背景 + 内容区 -->
    <section class="page-main">
      <div class="content-container">
        <!-- 选择应聘岗位 -->
        <div class="position-section">
          <div class="section-header">
            <h2>选择应聘岗位</h2>
            <p>搜索并选择一个岗位，AI 面试官将基于该岗位需求进行面试</p>
          </div>

          <el-select
            v-model="selectedPositionId"
            class="position-search"
            size="default"
            filterable
            placeholder="选择/搜索岗位"
            :teleported="false"
            style="width: 100%"
          >
            <el-option
              v-for="position in positions"
              :key="position.id"
              :label="position.name"
              :value="position.id"
            >
              <div class="position-option">
                <strong>{{ position.name }}</strong>
                <span>{{ position.description || position.techStack || '暂无岗位说明' }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <!-- 分隔线 -->
        <div class="section-divider" />

        <!-- 选择面试官 -->
        <div class="interviewer-section">
          <div class="section-header">
            <h2>选择面试官</h2>
            <p>选择一位面试官，不同的风格将带来不同的面试体验</p>
          </div>

          <div class="interviewer-grid">
            <div
              v-for="style in styles"
              :key="style.id"
              :class="['interviewer-card', { selected: selectedStyleId === style.id }]"
              @click="selectedStyleId = style.id"
            >
              <div class="card-copy">
                <div class="card-name">{{ interviewerDisplayName(style) }}</div>
                <div class="card-tag">{{ interviewerTagText(style) }}</div>
                <div class="card-desc">
                  <span class="desc-arrow" />
                  {{ interviewerDescription(style) }}
                </div>
              </div>
              <div class="card-avatar-wrap">
                <img
                  v-if="interviewerAvatar(style) && !avatarLoadFailed[style.id]"
                  :src="interviewerAvatar(style)"
                  :alt="interviewerDisplayName(style)"
                  @error="markAvatarFailed(style)"
                />
                <div v-else class="avatar-fallback">
                  <el-icon><User /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部操作栏 -->
    <footer class="select-footer">
      <el-button size="default" @click="goBack">上一步</el-button>
      <el-button
        type="primary"
        size="default"
        :icon="Select"
        :loading="starting"
        @click="startInterview"
      >
        进入面试
      </el-button>
    </footer>
  </main>
</template>

<style scoped>
.position-select-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 20%, rgba(196, 241, 255, 0.70), transparent 34%),
    radial-gradient(circle at 78% 12%, rgba(232, 221, 255, 0.58), transparent 38%),
    linear-gradient(180deg, #f7fbff 0%, #f9fbff 100%);
  display: flex;
  flex-direction: column;
}

/* ====== 顶部导航栏 ====== */
.select-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 30px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(31, 41, 55, 0.06);
  flex-shrink: 0;
}

/* ====== 步骤指示器 ====== */
.step-indicator {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.step-dot {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
  transition: all 0.3s;
}

.step-dot.done {
  background: #ffffff;
  color: #2563eb;
  border: 1.5px solid #2563eb;
}

.step-dot.done :deep(.el-icon) {
  font-size: 14px;
}

.step-dot.active {
  background: #2563eb;
  color: #ffffff;
  border: 1.5px solid #2563eb;
}

.step-dot:not(.done):not(.active) {
  background: #e5e7eb;
  color: #9ca3af;
  border: 1.5px solid #e5e7eb;
}

.step-line {
  width: 40px;
  height: 1.5px;
  background: #d1d5db;
}

.step-line.done-line {
  background: #2563eb;
}

/* ====== 主体背景 + 内容容器 ====== */
.page-main {
  flex: 1 1 auto;
  display: flex;
  justify-content: center;
  padding: 18px 0 0;
}

.content-container {
  width: min(710px, calc(100vw - 48px));
}

/* ====== 选择岗位区域 ====== */
.position-section {
  padding-bottom: 0;
}

.section-header {
  margin-bottom: 12px;
}

.section-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.section-header p {
  margin: 4px 0 0;
  color: #7b8798;
  font-size: 12px;
  line-height: 1.6;
}

.position-search {
  width: 100%;
}

.position-search :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(31, 41, 55, 0.08);
  padding: 0 16px;
}

.position-search :deep(.el-input__inner) {
  font-size: 14px;
}

.position-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0;
}

.position-option strong {
  color: #1e293b;
  font-size: 14px;
  font-weight: 600;
}

.position-option span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

/* ====== 分隔线 ====== */
.section-divider {
  height: 1px;
  background: rgba(31, 41, 55, 0.07);
  margin: 24px 0 0;
}

/* ====== 面试官区域 ====== */
.interviewer-section {
  padding-top: 20px;
}

.interviewer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 20px;
  row-gap: 40px;
}

/* ====== 面试官卡片 ====== */
.interviewer-card {
  position: relative;
  min-height: 115px;
  overflow: visible;
  display: flex;
  align-items: stretch;
  border-radius: 10px;
  background: #ffffff;
  border: 1.5px solid transparent;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02), 0 4px 12px rgba(37, 99, 235, 0.03);
  cursor: pointer;
  transition: all 0.2s;
  padding: 16px 16px 16px 18px;
}

.interviewer-card:hover {
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);
}

.interviewer-card.selected {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb, 0 4px 16px rgba(37, 99, 235, 0.08);
}

.card-copy {
  position: relative;
  z-index: 2;
  width: 58%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.card-name {
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}

.card-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 5px;
  background: #eef0f3;
  color: #9aa2ad;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  width: fit-content;
}

.interviewer-card.selected .card-tag {
  background: #2563eb;
  color: #ffffff;
}

.card-desc {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  color: #7b8798;
  font-size: 11px;
  line-height: 1.5;
}

.desc-arrow {
  display: inline-block;
  width: 0;
  height: 0;
  margin-top: 2px;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left: 5px solid #2563eb;
  flex-shrink: 0;
}

/* ====== 头像区域 ====== */
.card-avatar-wrap {
  position: absolute;
  z-index: 1;
  right: 0;
  bottom: 0;
  width: 96px;
  max-height: 142px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  pointer-events: none;
  user-select: none;
}

.card-avatar-wrap img {
  width: 100%;
  max-height: 142px;
  object-fit: contain;
  object-position: bottom right;
}

.avatar-fallback {
  display: grid;
  width: 64px;
  height: 64px;
  place-items: center;
  border-radius: 50%;
  background: #eef0f3;
  color: #9aa2ad;
  font-size: 22px;
}

/* ====== 底部操作栏 ====== */
.select-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  border-top: 1px solid rgba(18, 39, 68, 0.08);
  background: rgba(255, 255, 255, 0.96);
  padding: 14px 24px;
  min-height: 52px;
}

/* ====== 响应式 ====== */
@media (max-width: 860px) {
  .interviewer-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .select-topbar {
    padding: 0 18px;
  }

  .content-container {
    width: calc(100vw - 28px);
  }

  .interviewer-grid {
    grid-template-columns: 1fr;
  }

  .card-avatar-wrap {
    width: 72px;
    max-height: 110px;
  }
}
</style>
