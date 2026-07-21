<script setup lang="ts">
import {
  ArrowLeft,
  Calendar,
  Camera,
  ChatLineRound,
  CircleCheck,
  Document,
  Download,
  Finished,
  Headset,
  Microphone,
  MuteNotification,
  OfficeBuilding,
  User,
  UserFilled,
  VideoCamera,
  VideoPause,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import html2pdf from 'html2pdf.js'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchPostureThresholdsApi } from '@/api/adminPosture'
import {
  answerInterviewApi,
  fetchAdminInterviewApi,
  fetchInterviewApi,
  finishInterviewApi,
  reportPostureEventApi,
} from '@/api/interviews'
import {
  createPostureEventRequest,
  createPostureMonitor,
  isCameraSupported,
  type LocalPostureEvent,
  type PostureMonitor,
} from '@/services/posture'
import {
  resolveVirtualHuman,
  resolveVirtualHumanMotion,
  type VirtualHumanMotionState,
} from '@/services/virtualHuman'
import {
  cancelSpeech,
  createSpeechRecognitionSession,
  isSpeechRecognitionSupported,
  isSpeechSynthesisSupported,
  speakText,
  type SpeechRecognitionSession,
} from '@/services/voice'
import type { InterviewDetail, PostureEvent, PostureThreshold } from '@/types'
import { useFeatureFlags } from '@/composables/useFeatureFlags'

const flags = useFeatureFlags()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const sending = ref(false)
const finishing = ref(false)
const answer = ref('')
const interview = ref<InterviewDetail | null>(null)
const cameraVideo = ref<HTMLVideoElement | null>(null)
const recognizing = ref(false)
const interimSpeechText = ref('')
const voiceError = ref('')
const autoSpeak = ref(false)
const speaking = ref(false)
const postureActive = ref(false)
const postureStarting = ref(false)
const postureStatus = ref('摄像头姿态检测未开启')
const postureError = ref('')
const postureReporting = ref(false)
const postureThresholds = ref<PostureThreshold[]>([])
const virtualHumanLoadFailed = ref(false)
const speechRecognitionSupported = isSpeechRecognitionSupported()
const speechSynthesisSupported = isSpeechSynthesisSupported()
const cameraSupported = isCameraSupported()
let recognitionSession: SpeechRecognitionSession | null = null
let lastSpokenMessageId: number | null = null
let postureMonitor: PostureMonitor | null = null

const interviewId = computed(() => Number(route.params.id))
const readonlyAdminView = computed(() => route.path.startsWith('/admin/interviews'))
const canAnswer = computed(() => interview.value?.status === 'IN_PROGRESS' && !readonlyAdminView.value)
const isCompleted = computed(() => interview.value?.status === 'COMPLETED')
const postureEvents = computed(() => interview.value?.postureEvents ?? [])
const virtualHuman = computed(() => resolveVirtualHuman(interview.value?.style.virtualHuman))
const virtualHumanMotionState = computed<VirtualHumanMotionState>(() => {
  if (!interview.value) {
    return 'READONLY'
  }
  if (sending.value || finishing.value) {
    return 'THINKING'
  }
  if (speaking.value) {
    return 'QUESTIONING'
  }
  if (interview.value.status === 'COMPLETED') {
    return 'SUMMARY'
  }
  if (!canAnswer.value) {
    return 'READONLY'
  }
  if (recognizing.value || Boolean(answer.value.trim()) || Boolean(interimSpeechText.value.trim())) {
    return 'LISTENING'
  }
  return 'WAITING'
})
const virtualHumanMotion = computed(() => resolveVirtualHumanMotion(virtualHumanMotionState.value))
const postureWarningCount = computed(
  () => postureEvents.value.filter((event) => event.severity !== 'INFO').length,
)
const latestAiMessage = computed(() => {
  const messages = interview.value?.messages ?? []
  return [...messages].reverse().find((message) => message.role === 'ASSISTANT') ?? null
})
const answeredCount = computed(() => interview.value?.messages.filter((message) => message.role === 'USER').length ?? 0)
const voiceStatusText = computed(() => {
  if (!speechRecognitionSupported) {
    return '当前浏览器不支持语音识别，已保留文字输入'
  }
  if (!canAnswer.value) {
    return '当前面试不可回答，语音输入已关闭'
  }
  if (recognizing.value) {
    return '正在聆听...'
  }
  return '可使用麦克风听写回答'
})
const scoreRingStyle = computed(() => {
  const score = interview.value?.report?.totalScore ?? 0
  return { '--score-deg': `${Math.max(0, Math.min(score, 100)) * 3.6}deg` }
})
const reportMetrics = computed(() => {
  const report = interview.value?.report
  if (!report) {
    return []
  }
  return [
    { label: '专业知识', value: report.technicalScore },
    { label: '技能匹配', value: Math.round((report.technicalScore + report.totalScore) / 2) },
    { label: '语言表达', value: report.communicationScore },
    { label: '逻辑思维', value: report.logicScore },
    { label: '创新能力', value: Math.max(45, report.totalScore - 10) },
    { label: '应变抗压', value: Math.max(45, Math.round((report.logicScore + report.communicationScore) / 2) - 8) },
  ]
})
const radarPoints = computed(() => {
  if (!reportMetrics.value.length) {
    return ''
  }
  const center = 50
  const maxRadius = 38
  return reportMetrics.value
    .map((metric, index) => {
      const angle = (-90 + index * 60) * (Math.PI / 180)
      const radius = maxRadius * (metric.value / 100)
      return `${center + Math.cos(angle) * radius}% ${center + Math.sin(angle) * radius}%`
    })
    .join(', ')
})

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

async function loadPostureThresholds() {
  try {
    postureThresholds.value = await fetchPostureThresholdsApi()
  } catch {
    postureThresholds.value = []
    postureStatus.value = '姿态阈值配置读取失败，已使用本地默认配置'
  }
}

async function submitAnswer() {
  const content = answer.value.trim()
  if (!content) {
    ElMessage.warning('请先输入回答内容')
    return
  }

  stopRecognition()
  cancelSpeech()
  sending.value = true
  try {
    interview.value = await answerInterviewApi(interviewId.value, { content })
    answer.value = ''
    interimSpeechText.value = ''
  } catch {
    ElMessage.error('回答提交失败')
  } finally {
    sending.value = false
  }
}

async function finishInterview() {
  stopRecognition()
  cancelSpeech()
  stopPostureMonitor()
  finishing.value = true
  try {
    interview.value = await finishInterviewApi(interviewId.value)
    ElMessage.success('面试已结束，评估报告已生成')
  } catch {
    ElMessage.error('结束面试失败')
  } finally {
    finishing.value = false
  }
}

async function handleDownloadPdf() {
  if (!interview.value) {
    return
  }
  try {
    const reportEl = document.querySelector('.report-shell') as HTMLElement | null
    if (!reportEl) {
      ElMessage.warning('报告内容未准备好，请稍后再试')
      return
    }

    const positionName = interview.value.position.name
    const fileName = `面试报告_${positionName}_${new Date().toISOString().slice(0, 10)}.pdf`

    await html2pdf().set({
      margin: 12,
      filename: fileName,
      image: { type: 'jpeg', quality: 0.95 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
        logging: false,
      },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
    }).from(reportEl).save()

    ElMessage.success('PDF报告下载成功')
  } catch {
    ElMessage.error('PDF生成失败，请重试')
  }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

async function startPostureMonitor() {
  if (!cameraSupported) {
    const event: LocalPostureEvent = {
      eventType: 'CAMERA_UNAVAILABLE',
      severity: 'WARNING',
      score: 100,
      detail: '当前浏览器不支持摄像头访问，请继续文字或语音面试',
    }
    postureError.value = event.detail
    await handlePostureEvent(event)
    return
  }
  if (!canAnswer.value || postureActive.value || postureStarting.value) {
    return
  }

  postureStarting.value = true
  postureError.value = ''
  if (!postureThresholds.value.length) {
    await loadPostureThresholds()
  }
  postureMonitor = createPostureMonitor(
    {
      onStatus(message) {
        postureStatus.value = message
      },
      onEvent(event) {
        void handlePostureEvent(event)
      },
      onError(event) {
        postureError.value = event.detail
        void handlePostureEvent(event)
      },
    },
    postureThresholds.value,
  )

  try {
    await nextTick()
    if (!cameraVideo.value) {
      throw new Error('摄像头预览区域未准备好')
    }
    await postureMonitor.start(cameraVideo.value)
    postureActive.value = true
  } catch (error) {
    stopPostureMonitor()
    const event = resolveCameraError(error)
    postureError.value = event.detail
    ElMessage.warning(event.detail)
    await handlePostureEvent(event)
  } finally {
    postureStarting.value = false
  }
}

function stopPostureMonitor() {
  postureMonitor?.stop()
  postureMonitor = null
  postureActive.value = false
  postureStatus.value = '摄像头姿态检测已关闭'
  if (cameraVideo.value) {
    cameraVideo.value.srcObject = null
  }
}

function startRecognition() {
  if (!speechRecognitionSupported) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用文字输入')
    return
  }
  if (!canAnswer.value || recognizing.value) {
    return
  }

  voiceError.value = ''
  recognitionSession = createSpeechRecognitionSession({
    onStart() {
      recognizing.value = true
    },
    onEnd() {
      recognizing.value = false
    },
    onResult(result) {
      interimSpeechText.value = result.interimText
      if (result.finalText) {
        answer.value = mergeAnswerText(answer.value, result.finalText)
        interimSpeechText.value = ''
      }
    },
    onError(message) {
      voiceError.value = message
      recognizing.value = false
      ElMessage.warning(message)
    },
  })

  if (!recognitionSession) {
    voiceError.value = '当前浏览器不支持语音识别，请使用文字输入'
    return
  }

  try {
    recognitionSession.start()
  } catch {
    voiceError.value = '语音识别启动失败，请刷新页面或改用文字输入'
    ElMessage.warning(voiceError.value)
  }
}

function stopRecognition() {
  if (!recognitionSession) {
    recognizing.value = false
    return
  }
  try {
    recognitionSession.stop()
  } catch {
    recognitionSession.abort()
  } finally {
    recognitionSession = null
    recognizing.value = false
    interimSpeechText.value = ''
  }
}

function clearVoiceDraft() {
  answer.value = ''
  interimSpeechText.value = ''
  voiceError.value = ''
}

async function handlePostureEvent(event: LocalPostureEvent) {
  if (!canAnswer.value || postureReporting.value) {
    return
  }

  postureReporting.value = true
  try {
    const saved = await reportPostureEventApi(createPostureEventRequest(interviewId.value, event))
    appendPostureEvent(saved)
  } catch {
    postureError.value = '姿态事件上报失败，面试主流程不受影响'
  } finally {
    postureReporting.value = false
  }
}

function appendPostureEvent(event: PostureEvent) {
  if (!interview.value) {
    return
  }
  interview.value = {
    ...interview.value,
    postureEvents: [event, ...interview.value.postureEvents.filter((item) => item.id !== event.id)],
  }
}

async function speakLatestQuestion(manual = true) {
  if (!latestAiMessage.value) {
    if (manual) {
      ElMessage.warning('暂无可播报的问题')
    }
    return
  }
  if (!speechSynthesisSupported) {
    if (manual) {
      ElMessage.warning('当前浏览器不支持语音播报')
    }
    return
  }

  speaking.value = true
  try {
    await speakText(latestAiMessage.value.content)
    lastSpokenMessageId = latestAiMessage.value.id
  } catch (error) {
    if (manual) {
      ElMessage.warning(error instanceof Error ? error.message : '语音播报失败')
    }
  } finally {
    speaking.value = false
  }
}

function mergeAnswerText(current: string, incoming: string) {
  const normalizedIncoming = incoming.trim()
  if (!current.trim()) {
    return normalizedIncoming
  }
  return `${current.trimEnd()} ${normalizedIncoming}`
}

function resolveCameraError(error: unknown): LocalPostureEvent {
  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return {
      eventType: 'CAMERA_UNAVAILABLE',
      severity: 'WARNING',
      score: 100,
      detail: '摄像头权限被拒绝，请继续使用文字或语音面试',
    }
  }
  if (error instanceof DOMException && error.name === 'NotFoundError') {
    return {
      eventType: 'CAMERA_UNAVAILABLE',
      severity: 'WARNING',
      score: 100,
      detail: '没有检测到可用摄像头，请继续使用文字或语音面试',
    }
  }
  return {
    eventType: 'CAMERA_UNAVAILABLE',
    severity: 'WARNING',
    score: 100,
    detail: '摄像头启动失败，请继续使用文字或语音面试',
  }
}

watch(
  () => [autoSpeak.value, latestAiMessage.value?.id, canAnswer.value] as const,
  async ([enabled, messageId, answerable]) => {
    if (!enabled || !answerable || !messageId || messageId === lastSpokenMessageId) {
      return
    }
    await nextTick()
    await speakLatestQuestion(false)
  },
)

watch(canAnswer, (answerable) => {
  if (!answerable) {
    stopRecognition()
    cancelSpeech()
    stopPostureMonitor()
  }
})

watch(
  () => interview.value?.style.virtualHuman?.key,
  () => {
    virtualHumanLoadFailed.value = false
  },
)

onMounted(async () => {
  await loadInterview()
  // 姿态阈值是非关键配置，后台静默加载，不阻塞面试主界面
  loadPostureThresholds().catch(() => {
    postureStatus.value = '姿态阈值配置读取失败，已使用本地默认配置'
  })
})
onBeforeUnmount(() => {
  stopRecognition()
  cancelSpeech()
  stopPostureMonitor()
})
</script>

<template>
  <main v-loading="loading" :class="isCompleted ? 'flow-page report-page' : 'interview-room-page'">
    <template v-if="interview && isCompleted">
      <header class="report-topbar">
        <button class="back-button" type="button" @click="router.push(readonlyAdminView ? '/admin' : '/interviews')">
          ‹ 返回
        </button>
        <el-button v-if="flags.pdfReport" :icon="Download" type="primary" class="pdf-btn" @click="handleDownloadPdf">
          下载 PDF 报告
        </el-button>
      </header>

      <section class="report-shell">
        <div class="report-heading">
          <h1>面试评估报告</h1>
          <p>
            <span><el-icon><Calendar /></el-icon> {{ formatTime(interview.endedAt || interview.updatedAt) }}</span>
            <span><el-icon><OfficeBuilding /></el-icon> {{ interview.position.name }}</span>
            <span><el-icon><User /></el-icon> {{ virtualHuman.name }}</span>
          </p>
        </div>

        <section v-if="interview.report" class="score-hero">
          <div class="score-ring" :style="scoreRingStyle">
            <span class="score-value">{{ interview.report.totalScore }}</span>
            <span class="score-divider">/</span>
            <span class="score-max">100</span>
          </div>
          <h2>综合评分</h2>
          <p>候选人完成了 {{ answeredCount }} 轮面试问答，共回答了 {{ interview.questionCount }} 个问题。</p>
        </section>

        <section v-if="interview.report" class="report-grid">
          <article class="report-card radar-card">
            <h2>能力雷达图</h2>
            <div class="radar-chart">
              <span class="radar-grid r1" />
              <span class="radar-grid r2" />
              <span class="radar-grid r3" />
              <span class="radar-fill" :style="{ clipPath: `polygon(${radarPoints})` }" />
              <div class="radar-labels">
                <span class="radar-lbl top">专业知识</span>
                <span class="radar-lbl top-right">
                  技能匹配
                  <span class="radar-tooltip">60 分</span>
                </span>
                <span class="radar-lbl right">语言表达</span>
                <span class="radar-lbl bottom-right">逻辑思维</span>
                <span class="radar-lbl bottom">创新能力</span>
                <span class="radar-lbl bottom-left">应变抗压</span>
              </div>
            </div>
          </article>

          <article class="report-card metrics-card">
            <h2>各项指标</h2>
            <div class="metric-list">
              <div v-for="metric in reportMetrics" :key="metric.label" class="metric-item">
                <span class="metric-label">{{ metric.label }}</span>
                <div class="metric-track">
                  <i :class="{ pass: metric.value >= 60, fail: metric.value < 60 }" :style="{ width: `${metric.value}%` }" />
                </div>
                <strong :class="metric.value >= 60 ? 'pass' : 'fail'">{{ metric.value }}</strong>
              </div>
            </div>
          </article>
        </section>

        <section v-if="interview.report" class="insight-grid">
          <article class="insight-card success">
            <h2><span class="insight-icon star-icon">✦</span> 亮点</h2>
            <p>{{ interview.report.strengths }}</p>
          </article>
          <article class="insight-card warning">
            <h2><span class="insight-icon chat-icon">💬</span> 建议</h2>
            <p>{{ interview.report.recommendation || interview.report.improvements }}</p>
          </article>
        </section>

        <section class="dialogue-card">
          <div class="section-toolbar">
            <div>
              <h2><el-icon><Document /></el-icon> 面试对话记录</h2>
              <p>共 {{ interview.messages.length }} 条消息，候选人回答了 {{ answeredCount }} 个问题。</p>
            </div>
          </div>
          <div class="dialogue-list">
            <div v-for="message in interview.messages" :key="message.id" class="dialogue-row" :class="message.role">
              <span class="role-label">{{ message.role === 'USER' ? '候选人' : '面试官' }}</span>
              <p>{{ message.content }}</p>
            </div>
          </div>
        </section>
      </section>
    </template>

    <template v-else-if="interview">
      <header class="room-topbar">
        <button class="back-button dark" type="button" @click="router.push(readonlyAdminView ? '/admin' : '/interviews')">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <div class="room-status">
          <span />
          {{ readonlyAdminView ? '只读查看' : '面试进行中' }}
        </div>
      </header>

      <section class="room-layout">
        <div class="video-stage">
          <article class="video-tile interviewer-tile" :style="{ '--avatar-accent': virtualHuman.accent }">
            <img
              v-if="virtualHuman.src && !virtualHumanLoadFailed"
              :src="virtualHuman.src"
              :alt="virtualHuman.name"
              @error="virtualHumanLoadFailed = true"
            >
            <div v-else class="virtual-human-fallback">
              <el-icon><UserFilled /></el-icon>
              <strong>{{ virtualHuman.initials }}</strong>
            </div>
            <div class="video-chip">
              <span />
              {{ virtualHuman.name }}
            </div>
            <div class="microphone-chip">
              <el-icon><Microphone /></el-icon>
            </div>
          </article>

          <article class="video-tile candidate-tile">
            <video ref="cameraVideo" muted playsinline aria-label="本地摄像头预览" />
            <div v-if="!postureActive" class="camera-placeholder">
              <el-icon><Camera /></el-icon>
              <span>{{ postureError || postureStatus }}</span>
            </div>
            <div class="video-chip">
              <span />
              您的画面
            </div>
            <div class="microphone-chip">
              <el-icon><Microphone /></el-icon>
            </div>
          </article>
        </div>

        <aside class="room-sidebar">
          <div class="interviewer-profile">
            <div class="profile-avatar" :style="{ '--avatar-accent': virtualHuman.accent }">
              <img
                v-if="virtualHuman.src && !virtualHumanLoadFailed"
                :src="virtualHuman.src"
                :alt="virtualHuman.name"
              >
              <span v-else>{{ virtualHuman.initials }}</span>
            </div>
            <div>
              <strong>{{ virtualHuman.name }}</strong>
              <p>{{ virtualHuman.badge }} · {{ virtualHumanMotion.label }}</p>
            </div>
          </div>

          <div v-if="latestAiMessage" class="question-bubble">
            {{ latestAiMessage.content }}
          </div>

          <!-- 语音模块（FEATURE_SPEECH） -->
          <template v-if="flags.speech">
            <div class="voice-strip">
              <el-icon><Headset /></el-icon>
              <span>{{ voiceError || voiceStatusText }}</span>
            </div>

            <div v-if="interimSpeechText" class="speech-preview" aria-live="polite">
              <el-icon><MuteNotification /></el-icon>
              <span>{{ interimSpeechText }}</span>
            </div>

            <div v-if="canAnswer" class="room-actions">
              <el-button
                v-if="!recognizing"
                :icon="Microphone"
                :disabled="!speechRecognitionSupported || sending || finishing"
                @click="startRecognition"
              >
                开始听写
              </el-button>
              <el-button v-else :icon="VideoPause" type="success" @click="stopRecognition">
                停止听写
              </el-button>
              <el-button
                :icon="Headset"
                :loading="speaking"
                :disabled="!speechSynthesisSupported || !latestAiMessage"
                @click="speakLatestQuestion()"
              >
                播报问题
              </el-button>
              <el-switch
                v-model="autoSpeak"
                :disabled="!speechSynthesisSupported"
                inline-prompt
                active-text="自动"
                inactive-text="手动"
              />
            </div>
          </template>

          <!-- 姿态检测模块（FEATURE_POSTURE） -->
          <template v-if="flags.posture">
            <div v-if="canAnswer" class="posture-actions">
              <el-button
                v-if="!postureActive"
                :icon="VideoCamera"
                :loading="postureStarting"
                :disabled="postureStarting"
                @click="startPostureMonitor"
              >
                开启摄像头检测
              </el-button>
              <el-button v-else :icon="VideoPause" type="warning" plain @click="stopPostureMonitor">
                关闭摄像头检测
              </el-button>
            </div>

            <div class="posture-status" :class="{ warning: Boolean(postureError) }">
              <el-icon>
                <Warning v-if="postureError" />
                <CircleCheck v-else />
              </el-icon>
              <span>{{ postureError || postureStatus }}，{{ postureWarningCount }} 条需关注</span>
            </div>
          </template>

          <el-input
            v-if="canAnswer"
            v-model="answer"
            type="textarea"
            :rows="5"
            maxlength="4000"
            show-word-limit
            placeholder="输入你的回答，提交后 AI 会继续追问。"
          />

          <div v-if="canAnswer" class="finish-actions">
            <el-button :icon="ChatLineRound" type="primary" :loading="sending" @click="submitAnswer">
              提交回答
            </el-button>
            <el-button plain @click="clearVoiceDraft">清空草稿</el-button>
            <el-button :icon="Finished" type="danger" plain :loading="finishing" @click="finishInterview">
              结束面试
            </el-button>
          </div>

          <div class="room-message-list">
            <article
              v-for="message in interview.messages.slice(-4)"
              :key="message.id"
              :class="['room-message', message.role === 'USER' ? 'candidate' : 'interviewer']"
            >
              <span>{{ message.role === 'USER' ? '候选人' : '面试官' }}</span>
              <p>{{ message.content }}</p>
            </article>
          </div>
        </aside>
      </section>

      <div v-if="finishing" class="report-loading-mask">
        <div class="report-loading-card">
          <span class="loading-spinner" />
          <strong>正在生成评估报告...</strong>
        </div>
      </div>
    </template>
  </main>
</template>
