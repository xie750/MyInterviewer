<script setup lang="ts">
import { Check, RefreshLeft, Select } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import { createInterviewApi } from '@/api/interviews'
import { fetchInterviewerStylesApi } from '@/api/interviewerStyles'
import { fetchPositionsApi } from '@/api/positions'
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
  return style.name || style.virtualHuman.badge || '通用'
}

function interviewerAvatar(style: InterviewerStyle): string {
  return style.virtualHuman.imageUrl || ''
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
  } catch {
    ElMessage.error('面试创建失败')
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

    <!-- 内容区域 -->
    <section class="select-content">
      <!-- 选择应聘岗位 -->
      <div class="position-section">
        <div class="section-header">
          <h2>选择应聘岗位</h2>
          <p>搜索并选择一个岗位，AI 面试官将基于该岗位需求进行面试</p>
        </div>

        <el-select
          v-model="selectedPositionId"
          class="position-search"
          size="large"
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
            <div class="card-left">
              <div class="card-name">{{ style.virtualHuman.name }}</div>
              <div class="card-tag">{{ interviewerTagText(style) }}</div>
              <div class="card-desc">
                <span class="desc-arrow" />
                {{ style.description || style.scenario || '通用面试场景' }}
              </div>
            </div>
            <div class="card-avatar-wrap">
              <img
                v-if="style.virtualHuman.imageUrl && !avatarLoadFailed[style.id]"
                :src="interviewerAvatar(style)"
                :alt="style.virtualHuman.name"
                @error="markAvatarFailed(style)"
              />
              <div v-else class="avatar-fallback">
                <el-icon><Select /></el-icon>
              </div>
              <div v-if="selectedStyleId === style.id" class="avatar-check">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部操作栏 -->
    <footer class="select-footer">
      <el-button size="large" @click="goBack">上一步</el-button>
      <el-button
        type="primary"
        size="large"
        :icon="Select"
        :loading="starting"
        @click="startInterview"
      >
        <template #default>
          <span class="btn-icon-sparkle" />
          进入面试
        </template>
      </el-button>
    </footer>
  </main>
</template>

<style scoped>
.position-select-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 16% 24%, rgb(178 230 255 / 62%), transparent 34%),
    radial-gradient(circle at 78% 24%, rgb(223 210 255 / 58%), transparent 32%),
    linear-gradient(135deg, #f5fcff 0%, #f7f8ff 52%, #ffffff 100%);
  display: flex;
  flex-direction: column;
}

/* ====== 顶部栏 ====== */
.select-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 40px;
  background: rgb(255 255 255 / 65%);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgb(210 220 240 / 50%);
  flex-shrink: 0;
}

/* ====== 步骤指示器 ====== */
.step-indicator {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.step-dot {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  font-weight: 800;
  font-size: 16px;
  transition: all 0.3s;
}

.step-dot.done {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 10px 24px rgb(37 99 235 / 16%);
}

.step-dot.done :deep(.el-icon) {
  font-size: 20px;
}

.step-dot.active {
  background: linear-gradient(135deg, #1d6fff, #5b50ff);
  color: #ffffff;
  box-shadow: 0 10px 24px rgb(37 99 235 / 16%);
}

.step-dot:not(.done):not(.active) {
  background: #e5e7eb;
  color: #9ca3af;
}

.step-line {
  width: 58px;
  height: 2px;
  background: #e5e7eb;
}

.step-line.done-line {
  background: #2f6bff;
}

/* ====== 内容区域 ====== */
.select-content {
  flex: 1;
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 40px 0 120px;
  overflow-y: auto;
}

/* ====== 选择岗位区域 ====== */
.position-section {
  padding-bottom: 32px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.section-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.position-search {
  width: 100%;
}

.position-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.position-option strong {
  color: #1e293b;
  font-size: 15px;
  font-weight: 600;
}

.position-option span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

/* ====== 分隔线 ====== */
.section-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 0 0 32px;
}

/* ====== 面试官区域 ====== */
.interviewer-section {
  padding-top: 0;
}

.interviewer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

/* ====== 面试官卡片 ====== */
.interviewer-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-radius: 12px;
  background: #ffffff;
  border: 2px solid transparent;
  border-left: 3px solid transparent;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 4px 12px rgba(37, 99, 235, 0.04);
  cursor: pointer;
  transition: all 0.2s;
  padding: 20px 20px 20px 22px;
  overflow: visible;
}

.interviewer-card:hover {
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.1);
}

.interviewer-card.selected {
  border-color: #2563eb;
  border-left: 3px solid #2563eb;
  background: #f8faff;
  box-shadow: 0 8px 28px rgba(37, 99, 235, 0.12);
}

.card-left {
  flex: 1;
  min-width: 0;
}

.card-name {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.3;
}

.card-tag {
  display: inline-block;
  margin-top: 10px;
  border-radius: 6px;
  background: #2563eb;
  padding: 4px 10px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.card-desc {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 12px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.desc-arrow {
  display: inline-block;
  width: 0;
  height: 0;
  margin-top: 3px;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 6px solid #2563eb;
  flex-shrink: 0;
}

/* ====== 头像区域 ====== */
.card-avatar-wrap {
  position: relative;
  flex-shrink: 0;
  display: grid;
  width: 88px;
  height: 88px;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: #f1f5f9;
}

.interviewer-card.selected .card-avatar-wrap {
  box-shadow: 0 0 0 3px #2563eb;
}

.card-avatar-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 50%;
  background: #e2e8f0;
  color: #94a3b8;
  font-size: 20px;
}

/* ====== 头像上的选中勾 ====== */
.avatar-check {
  position: absolute;
  right: -2px;
  bottom: -2px;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #2563eb;
  color: #ffffff;
  font-size: 16px;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.35);
  z-index: 2;
}

/* ====== 底部操作栏 ====== */
.select-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  justify-content: center;
  gap: 16px;
  border-top: 1px solid rgb(194 205 225 / 65%);
  background: rgb(255 255 255 / 90%);
  padding: 18px 24px;
  backdrop-filter: blur(14px);
}

.btn-icon-sparkle {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 2px;
  background: currentColor;
  mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 2l1.5 5.5L19 8l-5.5 1.5L12 14l-1.5-5.5L5 8l5.5-1.5z'/%3E%3Cpath d='M19 14l.8 2.8L22 17.5l-2.2 1.3-.8 2.8-1.6-5.3 1.6-2.3z'/%3E%3Cpath d='M8.5 5.5L7 8.8 4 9.5l2.5 2.2L5.5 15l2.5-1.2L10.5 15 8 12.8 10.5 10.6z'/%3E%3C/svg%3E");
  mask-size: contain;
  mask-repeat: no-repeat;
  mask-position: center;
  -webkit-mask-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M12 2l1.5 5.5L19 8l-5.5 1.5L12 14l-1.5-5.5L5 8l5.5-1.5z'/%3E%3Cpath d='M19 14l.8 2.8L22 17.5l-2.2 1.3-.8 2.8-1.6-5.3 1.6-2.3z'/%3E%3Cpath d='M8.5 5.5L7 8.8 4 9.5l2.5 2.2L5.5 15l2.5-1.2L10.5 15 8 12.8 10.5 10.6z'/%3E%3C/svg%3E");
  -webkit-mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  vertical-align: -2px;
}

/* ====== 响应式 ====== */
@media (max-width: 1080px) {
  .interviewer-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .select-topbar {
    padding: 0 18px;
  }

  .select-content {
    width: calc(100% - 28px);
    padding: 24px 0 120px;
  }

  .interviewer-grid {
    grid-template-columns: 1fr;
  }

  .card-avatar-wrap {
    width: 72px;
    height: 72px;
  }
}
</style>
