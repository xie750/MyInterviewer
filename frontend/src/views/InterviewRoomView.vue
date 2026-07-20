<script setup lang="ts">
import { ChatLineRound, Finished, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  answerInterviewApi,
  fetchAdminInterviewApi,
  fetchInterviewApi,
  finishInterviewApi,
} from '@/api/interviews'
import type { InterviewDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const sending = ref(false)
const finishing = ref(false)
const answer = ref('')
const interview = ref<InterviewDetail | null>(null)

const interviewId = computed(() => Number(route.params.id))
const readonlyAdminView = computed(() => route.path.startsWith('/admin/interviews'))
const canAnswer = computed(() => interview.value?.status === 'IN_PROGRESS' && !readonlyAdminView.value)

async function loadInterview() {
  if (!Number.isFinite(interviewId.value)) {
    ElMessage.error('面试编号无效')
    await router.replace('/interviews')
    return
  }

  loading.value = true
  try {
    interview.value = readonlyAdminView.value
      ? await fetchAdminInterviewApi(interviewId.value)
      : await fetchInterviewApi(interviewId.value)
  } catch {
    ElMessage.error('面试加载失败')
    await router.replace(readonlyAdminView.value ? '/admin' : '/interviews')
  } finally {
    loading.value = false
  }
}

async function submitAnswer() {
  const content = answer.value.trim()
  if (!content) {
    ElMessage.warning('请先输入回答内容')
    return
  }

  sending.value = true
  try {
    interview.value = await answerInterviewApi(interviewId.value, { content })
    answer.value = ''
  } catch {
    ElMessage.error('回答提交失败')
  } finally {
    sending.value = false
  }
}

async function finishInterview() {
  finishing.value = true
  try {
    interview.value = await finishInterviewApi(interviewId.value)
    ElMessage.success('面试已结束，报告已生成')
  } catch {
    ElMessage.error('结束面试失败')
  } finally {
    finishing.value = false
  }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(loadInterview)
</script>

<template>
  <main v-loading="loading" class="workspace-page">
    <section v-if="interview" class="workspace-header">
      <div>
        <p class="eyebrow">Interview</p>
        <h1>{{ interview.position.name }}</h1>
        <p class="summary">
          {{ interview.style.name }} · {{ interview.position.difficulty || '未设置难度' }} ·
          {{ interview.status === 'COMPLETED' ? '已完成' : '进行中' }}
        </p>
      </div>
      <div class="action-row compact">
        <el-button v-if="!readonlyAdminView" plain @click="router.push('/interviews')">历史记录</el-button>
        <el-button v-if="readonlyAdminView" plain @click="router.push('/admin')">返回后台</el-button>
        <el-button :icon="SwitchButton" plain @click="router.push('/home')">返回首页</el-button>
      </div>
    </section>

    <section v-if="interview" class="interview-layout">
      <div class="interview-main">
        <div class="section-toolbar">
          <div>
            <h2>文字面试</h2>
            <p>已生成 {{ interview.questionCount }} 个问题，当前会话开始于 {{ formatTime(interview.startedAt) }}。</p>
          </div>
          <el-tag :type="interview.status === 'COMPLETED' ? 'success' : 'warning'">
            {{ interview.status === 'COMPLETED' ? '已结束' : '可继续回答' }}
          </el-tag>
        </div>

        <div class="message-list">
          <article
            v-for="message in interview.messages"
            :key="message.id"
            :class="['message-bubble', message.role === 'USER' ? 'from-user' : 'from-ai']"
          >
            <div class="message-meta">
              <span>{{ message.role === 'USER' ? '我的回答' : 'AI 面试官' }}</span>
              <span>第 {{ message.roundNo }} 轮 · {{ formatTime(message.createdAt) }}</span>
            </div>
            <p>{{ message.content }}</p>
          </article>
        </div>

        <div v-if="canAnswer" class="answer-panel">
          <el-input
            v-model="answer"
            type="textarea"
            :rows="5"
            maxlength="4000"
            show-word-limit
            placeholder="输入你的回答，提交后 AI 会基于上下文继续追问。"
          />
          <div class="action-row compact">
            <el-button :icon="ChatLineRound" type="primary" :loading="sending" @click="submitAnswer">
              提交回答
            </el-button>
            <el-button :icon="Finished" :loading="finishing" @click="finishInterview">
              结束并生成报告
            </el-button>
          </div>
        </div>
      </div>

      <aside class="report-panel">
        <div class="section-toolbar">
          <div>
            <h2>面试报告</h2>
            <p>{{ interview.report ? '报告已生成' : '结束面试后自动生成评分和总结。' }}</p>
          </div>
        </div>

        <template v-if="interview.report">
          <div class="score-grid">
            <div>
              <strong>{{ interview.report.totalScore }}</strong>
              <span>总分</span>
            </div>
            <div>
              <strong>{{ interview.report.technicalScore }}</strong>
              <span>技术</span>
            </div>
            <div>
              <strong>{{ interview.report.communicationScore }}</strong>
              <span>表达</span>
            </div>
            <div>
              <strong>{{ interview.report.logicScore }}</strong>
              <span>逻辑</span>
            </div>
          </div>
          <h3>总结</h3>
          <p>{{ interview.report.summary }}</p>
          <h3>优势</h3>
          <p>{{ interview.report.strengths }}</p>
          <h3>改进</h3>
          <p>{{ interview.report.improvements }}</p>
          <h3>建议</h3>
          <p>{{ interview.report.recommendation }}</p>
        </template>
        <el-empty v-else description="暂无报告" />
      </aside>
    </section>
  </main>
</template>
