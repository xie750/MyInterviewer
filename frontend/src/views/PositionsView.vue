<script setup lang="ts">
import {
  ArrowLeft,
  Check,
  Delete,
  DocumentChecked,
  Refresh,
  Select,
  UploadFilled,
  UserFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createInterviewApi } from '@/api/interviews'
import { fetchInterviewerStylesApi } from '@/api/interviewerStyles'
import { fetchPositionsApi } from '@/api/positions'
import { parseResumeApi } from '@/api/resumes'
import { resolveVirtualHuman } from '@/services/virtualHuman'
import type { InterviewerStyle, Position, ResumeParseResponse } from '@/types'

const router = useRouter()
const loading = ref(false)
const starting = ref(false)
const resumeParsing = ref(false)
const positions = ref<Position[]>([])
const styles = ref<InterviewerStyle[]>([])
const selectedPositionId = ref<number | null>(null)
const selectedStyleId = ref<number | null>(null)
const resumeContext = ref<ResumeParseResponse | null>(null)
const avatarLoadFailed = ref<Record<number, boolean>>({})

const selectedPosition = computed(() => positions.value.find((item) => item.id === selectedPositionId.value) ?? null)
const flowStep = computed(() => (resumeContext.value ? 2 : 1))

async function loadOptions() {
  loading.value = true
  try {
    const [positionList, styleList] = await Promise.all([
      fetchPositionsApi(),
      fetchInterviewerStylesApi(),
    ])
    positions.value = positionList
    styles.value = styleList
    selectedPositionId.value = positionList[0]?.id ?? null
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

async function startInterview() {
  if (!selectedPosition.value) {
    ElMessage.warning('请先选择岗位')
    return
  }
  if (!selectedStyleId.value) {
    ElMessage.warning('请选择面试官风格')
    return
  }

  starting.value = true
  try {
    const interview = await createInterviewApi({
      positionId: selectedPosition.value.id,
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
    starting.value = false
  }
}

onMounted(loadOptions)
</script>

<template>
  <main class="flow-page" v-loading="loading">
    <header class="flow-topbar">
      <button class="back-button" type="button" @click="router.push('/home')">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>
      <div class="flow-steps" aria-label="面试准备进度">
        <span class="step-dot done"><el-icon><Check /></el-icon></span>
        <span class="step-line" />
        <span class="step-dot active">{{ flowStep }}</span>
      </div>
    </header>

    <section class="flow-shell">
      <div class="flow-title">
        <h1>上传简历并选择岗位</h1>
        <p>简历只用于当前面试上下文，结束后会清理；也可以不上传，直接按照岗位进行模拟面试。</p>
      </div>

      <div class="prep-grid">
        <section class="upload-card">
          <div class="section-toolbar">
            <div>
              <h2>上传简历</h2>
              <p>支持 txt、md、pdf、docx，解析失败不会阻断面试。</p>
            </div>
            <el-button v-if="resumeContext" :icon="Delete" plain @click="clearResume">移除</el-button>
          </div>

          <el-upload
            v-if="!resumeContext"
            drag
            accept=".txt,.md,.pdf,.docx"
            :auto-upload="false"
            :show-file-list="false"
            :limit="1"
            :on-change="parseResume"
            :disabled="resumeParsing"
            class="harness-upload"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">点击或拖拽上传简历</div>
            <template #tip>
              <div class="el-upload__tip">单个文件不超过 10MB</div>
            </template>
          </el-upload>

          <div v-else v-loading="resumeParsing" class="resume-preview-card">
            <div class="resume-file">
              <el-icon><DocumentChecked /></el-icon>
              <div>
                <strong>{{ resumeContext.fileName }}</strong>
                <span>已解析 {{ resumeContext.extractedTextLength }} 字</span>
              </div>
            </div>
            <p>{{ resumeContext.summary || '已获得简历上下文，AI 将结合岗位进行追问。' }}</p>
            <div v-if="resumeContext.skills.length" class="tag-list">
              <el-tag v-for="skill in resumeContext.skills.slice(0, 8)" :key="skill" effect="plain">
                {{ skill }}
              </el-tag>
            </div>
          </div>
        </section>

        <section class="select-card">
          <div class="section-toolbar">
            <div>
              <h2>选择应聘岗位</h2>
              <p>AI 面试官会基于岗位要求生成首问和追问。</p>
            </div>
            <el-button :icon="Refresh" :loading="loading" plain @click="loadOptions">刷新</el-button>
          </div>

          <el-select
            v-model="selectedPositionId"
            class="position-select"
            size="large"
            filterable
            placeholder="搜索并选择岗位"
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

          <div v-if="selectedPosition" class="selected-position">
            <strong>{{ selectedPosition.name }}</strong>
            <p>{{ selectedPosition.description || '暂无岗位说明' }}</p>
            <span>{{ selectedPosition.techStack || '暂未配置技术栈' }}</span>
          </div>
        </section>
      </div>

      <section class="interviewer-section">
        <div class="flow-title compact-title">
          <h2>选择面试官</h2>
          <p>不同风格会影响追问方式、反馈侧重点和面试节奏。</p>
        </div>

        <el-radio-group v-model="selectedStyleId" class="interviewer-grid">
          <el-radio-button v-for="style in styles" :key="style.id" :label="style.id">
            <span class="interviewer-card" :style="{ '--avatar-accent': styleVirtualHuman(style).accent }">
              <span class="interviewer-copy">
                <strong>{{ styleVirtualHuman(style).name }}</strong>
                <em>{{ styleVirtualHuman(style).badge || style.name }}</em>
                <span>{{ style.description || style.scenario || '通用面试场景' }}</span>
              </span>
              <span class="interviewer-avatar">
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
            </span>
          </el-radio-button>
        </el-radio-group>
      </section>
    </section>

    <footer class="flow-footer">
      <el-button size="large" @click="router.push('/home')">上一步</el-button>
      <el-button type="primary" size="large" :icon="Select" :loading="starting" @click="startInterview">
        进入面试
      </el-button>
    </footer>
  </main>
</template>
