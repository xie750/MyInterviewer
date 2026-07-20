<script setup lang="ts">
import { Delete, DocumentChecked, Refresh, Select, SwitchButton, UploadFilled, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createInterviewApi } from '@/api/interviews'
import { fetchInterviewerStylesApi } from '@/api/interviewerStyles'
import { fetchPositionsApi } from '@/api/positions'
import { parseResumeApi } from '@/api/resumes'
import { resolveVirtualHuman } from '@/services/virtualHuman'
import type { InterviewerStyle, Position, ResumeParseResponse } from '@/types'

const router = useRouter()
const loading = ref(false)
const startingPositionId = ref<number | null>(null)
const resumeParsing = ref(false)
const positions = ref<Position[]>([])
const styles = ref<InterviewerStyle[]>([])
const selectedStyleId = ref<number | null>(null)
const resumeContext = ref<ResumeParseResponse | null>(null)
const avatarLoadFailed = ref<Record<number, boolean>>({})

async function loadOptions() {
  loading.value = true
  try {
    const [positionList, styleList] = await Promise.all([
      fetchPositionsApi(),
      fetchInterviewerStylesApi(),
    ])
    positions.value = positionList
    styles.value = styleList
    selectedStyleId.value = styleList[0]?.id ?? null
  } catch {
    ElMessage.error('面试配置加载失败')
  } finally {
    loading.value = false
  }
}

async function parseResume(uploadFile: UploadFile) {
  if (!uploadFile.raw) {
    ElMessage.warning('请选择简历文件')
    return
  }

  resumeParsing.value = true
  try {
    resumeContext.value = await parseResumeApi(uploadFile.raw)
    ElMessage.success('简历解析完成')
  } catch {
    resumeContext.value = null
    ElMessage.error('简历解析失败，可跳过简历继续面试')
  } finally {
    resumeParsing.value = false
  }
}

function clearResume() {
  resumeContext.value = null
}

function styleVirtualHuman(style: InterviewerStyle) {
  return resolveVirtualHuman(style.virtualHuman)
}

function markAvatarLoadFailed(style: InterviewerStyle) {
  avatarLoadFailed.value = {
    ...avatarLoadFailed.value,
    [style.id]: true,
  }
}

async function startInterview(position: Position) {
  if (!selectedStyleId.value) {
    ElMessage.warning('请选择面试官风格')
    return
  }

  startingPositionId.value = position.id
  try {
    const interview = await createInterviewApi({
      positionId: position.id,
      styleId: selectedStyleId.value,
      resume: resumeContext.value
        ? {
            summary: resumeContext.value.summary,
            skills: resumeContext.value.skills,
            projects: resumeContext.value.projects,
            warnings: resumeContext.value.warnings,
          }
        : undefined,
    })
    await router.push(`/interviews/${interview.id}`)
  } catch {
    ElMessage.error('面试创建失败')
  } finally {
    startingPositionId.value = null
  }
}

onMounted(loadOptions)
</script>

<template>
  <main class="workspace-page">
    <section class="workspace-header">
      <div>
        <p class="eyebrow">Positions</p>
        <h1>选择面试岗位</h1>
        <p class="summary">选择面试风格和岗位，可选上传简历让 AI 追问更贴近你的项目经历。</p>
      </div>
      <div class="action-row compact">
        <el-button :icon="Refresh" :loading="loading" @click="loadOptions">刷新</el-button>
        <el-button plain @click="router.push('/interviews')">历史记录</el-button>
        <el-button :icon="SwitchButton" plain @click="router.push('/home')">返回首页</el-button>
      </div>
    </section>

    <section v-loading="loading" class="style-panel">
      <div class="section-toolbar">
        <div>
          <h2>面试官风格</h2>
          <p>选择本场面试的追问方式和评价侧重点。</p>
        </div>
      </div>
      <el-radio-group v-model="selectedStyleId" class="style-grid">
        <el-radio-button v-for="style in styles" :key="style.id" :label="style.id">
          <span class="style-card-content">
            <span class="style-avatar" :style="{ '--avatar-accent': styleVirtualHuman(style).accent }">
              <img
                v-if="styleVirtualHuman(style).src && !avatarLoadFailed[style.id]"
                :src="styleVirtualHuman(style).src || undefined"
                :alt="styleVirtualHuman(style).name"
                @error="markAvatarLoadFailed(style)"
              >
              <span v-else class="avatar-fallback">
                <el-icon><UserFilled /></el-icon>
              </span>
            </span>
            <span>
              <span class="style-name">{{ style.name }}</span>
              <span class="style-scenario">{{ styleVirtualHuman(style).name }} · {{ style.scenario || '通用场景' }}</span>
            </span>
          </span>
        </el-radio-button>
      </el-radio-group>
    </section>

    <section class="resume-upload-panel">
      <div class="section-toolbar">
        <div>
          <h2>简历上下文</h2>
          <p>支持 txt、md、pdf、docx，文件不保存；解析失败可直接跳过。</p>
        </div>
        <el-button v-if="resumeContext" :icon="Delete" plain @click="clearResume">清除简历</el-button>
      </div>

      <div class="resume-upload-layout">
        <el-upload
          drag
          accept=".txt,.md,.pdf,.docx"
          :auto-upload="false"
          :show-file-list="false"
          :limit="1"
          :on-change="parseResume"
          :disabled="resumeParsing"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖入或点击选择简历</div>
          <template #tip>
            <div class="el-upload__tip">单个文件不超过 10MB。</div>
          </template>
        </el-upload>

        <div v-loading="resumeParsing" class="resume-preview">
          <template v-if="resumeContext">
            <div class="resume-preview-title">
              <el-icon><DocumentChecked /></el-icon>
              <strong>{{ resumeContext.fileName }}</strong>
              <el-tag type="success">已解析</el-tag>
            </div>
            <p>{{ resumeContext.summary }}</p>
            <div class="tag-list">
              <el-tag v-for="skill in resumeContext.skills" :key="skill" effect="plain">{{ skill }}</el-tag>
            </div>
            <ul v-if="resumeContext.projects.length" class="resume-list">
              <li v-for="project in resumeContext.projects" :key="project">{{ project }}</li>
            </ul>
            <p v-if="resumeContext.warnings.length" class="resume-warning">
              {{ resumeContext.warnings[0] }}
            </p>
          </template>
          <el-empty v-else description="未使用简历，将按岗位和风格开始面试" />
        </div>
      </div>
    </section>

    <section v-loading="loading" class="position-grid">
      <article v-for="position in positions" :key="position.id" class="position-card">
        <div class="card-title-row">
          <h2>{{ position.name }}</h2>
          <el-tag>{{ position.difficulty || '未设置' }}</el-tag>
        </div>
        <p class="card-description">{{ position.description || '暂无岗位说明' }}</p>
        <p class="card-meta">{{ position.techStack || '暂未配置技术栈' }}</p>
        <el-button
          :icon="Select"
          type="primary"
          :loading="startingPositionId === position.id"
          @click="startInterview(position)"
        >
          开始面试
        </el-button>
      </article>
      <el-empty v-if="!loading && positions.length === 0" description="暂无可选岗位" />
    </section>
  </main>
</template>
