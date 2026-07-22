<script setup lang="ts">
import {
  ArrowLeft,
  Calendar,
  Camera,
  ChatLineRound,
  Document,
  Download,
  Headset,
  Microphone,
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

import {
  answerInterviewApi,
  fetchAdminInterviewApi,
  fetchInterviewApi,
  finishInterviewApi,
  reportPostureEventApi,
} from '@/api/interviews'
import { fetchPostureThresholdsApi } from '@/api/adminPosture'
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
  type SpeechRecognitionResultText,
  type SpeechRecognitionSession,
} from '@/services/voice'
import type { InterviewDetail, PostureEvent, PostureThreshold } from '@/types'
import { useFeatureFlags } from '@/composables/useFeatureFlags'

// ─── 常量 ──────────────────────────────────────────────────────
const MAX_QUESTION_ROUNDS = 3 // 面试总轮数（包含开场问题）

const flags = useFeatureFlags()
const route = useRoute()
const router = useRouter()

// ─── 基础响应式状态 ────────────────────────────────────────────
const loading = ref(false)
const sending = ref(false)
const finishing = ref(false)
const answer = ref('')
const interview = ref<InterviewDetail | null>(null)
const cameraVideo = ref<HTMLVideoElement | null>(null)
const recognizing = ref(false)
const voiceError = ref('')
const speaking = ref(false)
const settingsOpen = ref(false)
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

// ─── 自动面试流程状态机 ────────────────────────────────────────
// 阶段：
//   idle             — 初始 / 等待
//   ai_thinking      — 面试官正在思考（LLM 生成中）
//   ai_speaking      — 面试官语音播报问题
//   listening        — 聆听候选人回答
//   processing       — 提交回答，等待 LLM 生成追问
//   summarizing      — 最后一轮，面试官总结
//   completed        — 面试结束
const interviewPhase = ref<'idle' | 'ai_thinking' | 'ai_speaking' | 'listening' | 'processing' | 'summarizing' | 'completed'>('idle')

const silenceTimer = ref<number | null>(null)
const SILENCE_DURATION = 3000 // 3 秒无语音 → 自动提交

// 回答完成后的过渡延时（让用户看到自己的回答文字再进入下一轮）
const PROCESSING_DELAY = 1500

// ─── 计算属性 ──────────────────────────────────────────────────
const interviewId = computed(() => Number(route.params.id))
const readonlyAdminView = computed(() => route.path.startsWith('/admin/interviews'))
const canAnswer = computed(() => interview.value?.status === 'IN_PROGRESS' && !readonlyAdminView.value)
const isCompleted = computed(() => interview.value?.status === 'COMPLETED')

const virtualHuman = computed(() => resolveVirtualHuman(interview.value?.style.virtualHuman))
const virtualHumanMotionState = computed<VirtualHumanMotionState>(() => {
  if (!interview.value) return 'READONLY'
  if (finishing.value) return 'THINKING'
  if (sending.value) return 'THINKING'
  if (speaking.value) return 'QUESTIONING'
  if (interview.value.status === 'COMPLETED') return 'SUMMARY'
  if (!canAnswer.value) return 'READONLY'
  if (interviewPhase.value === 'listening' || recognizing.value) return 'LISTENING'
  return 'WAITING'
})
const virtualHumanMotion = computed(() => resolveVirtualHumanMotion(virtualHumanMotionState.value))

// 最新的面试官消息
const latestAiMessage = computed(() => {
  const messages = interview.value?.messages ?? []
  return [...messages].reverse().find((m) => m.role === 'ASSISTANT') ?? null
})

// 已完成的回答轮数
const answeredCount = computed(() => interview.value?.messages.filter((m) => m.role === 'USER').length ?? 0)

// 面试进度（几轮 / 共几轮）
const roundInfo = computed(() => {
  const total = Math.max(interview.value?.questionCount ?? MAX_QUESTION_ROUNDS, answeredCount.value + 1)
  return { current: answeredCount.value, total: Math.min(total, MAX_QUESTION_ROUNDS) }
})

// 是否已到达最后一轮（用户回答完第 N 轮后）
const isLastRound = computed(() => roundInfo.value.current >= MAX_QUESTION_ROUNDS)

// 阶段标签文案
const phaseLabel = computed(() => {
  switch (interviewPhase.value) {
    case 'ai_thinking':
      return '面试官思考中...'
    case 'ai_speaking':
      return '面试官提问中...'
    case 'listening':
      return '正在聆听，请开始回答...'
    case 'processing':
      return '正在分析回答...'
    case 'summarizing':
      return '面试总结中...'
    default:
      return ''
  }
})

// 主状态区文案
const voiceStatusText = computed(() => {
  if (!speechRecognitionSupported) {
    return '当前浏览器不支持语音识别，已保留文字输入'
  }
  if (!canAnswer.value) {
    return '当前面试不可回答，语音输入已关闭'
  }
  if (interviewPhase.value === 'listening') return '正在聆听...'
  if (interviewPhase.value === 'ai_speaking') return '面试官正在提问，请稍候...'
  if (interviewPhase.value === 'ai_thinking') return '面试官正在思考...'
  if (interviewPhase.value === 'summarizing') return '面试官正在总结...'
  if (interviewPhase.value === 'processing') return '正在分析回答...'
  return '点击"语音回答"开始'
})

// 当前面试官消息是否正在播放
const isAiSpeaking = computed(() => interviewPhase.value === 'ai_speaking')
// 是否处于聆听阶段
const isListening = computed(() => interviewPhase.value === 'listening')

// 状态指示灯 CSS 类
const statusDotClass = computed(() => {
  switch (interviewPhase.value) {
    case 'listening':
      return 'green'
    case 'ai_speaking':
    case 'ai_thinking':
      return 'blue'
    case 'processing':
    case 'summarizing':
      return 'purple'
    default:
      return ''
  }
})

// ─── 报告雷达图 ─────────────────────────────────────────────────
const reportMetrics = computed(() => {
  const report = interview.value?.report
  if (!report) return []
  const { totalScore, technicalScore, communicationScore, logicScore } = report
  return [
    { label: '专业知识', value: clamp01(Math.round(totalScore * 0.5 + technicalScore * 0.5)) },
    { label: '技能匹配', value: clamp01(Math.round(totalScore * 0.5 + technicalScore * 0.5)) },
    { label: '语言表达', value: communicationScore },
    { label: '逻辑思维', value: logicScore },
    { label: '应变抗压', value: clamp01(Math.round(communicationScore * 0.5 + logicScore * 0.5 + 5)) },
    { label: '创新能力', value: clamp01(Math.round(totalScore * 0.5 + Math.max(0, totalScore - technicalScore) * 0.5)) },
  ]
})

const RADAR_CX = 150
const RADAR_CY = 150
const RADAR_R = 108
const RADAR_LABEL_R = 138
const RADAR_LEVELS = 4
const RADAR_ANGLES = [-90, -30, 30, 90, 150, 210].map((d) => (d * Math.PI) / 180)

function polar(cx: number, cy: number, r: number, angle: number) {
  return { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) }
}

function clamp01(v: number) {
  return Math.max(0, Math.min(100, Math.round(v)))
}

const radarGridHex = computed(() =>
  Array.from({ length: RADAR_LEVELS }, (_, level) => {
    const r = (RADAR_R / RADAR_LEVELS) * (level + 1)
    return RADAR_ANGLES.map((a) => polar(RADAR_CX, RADAR_CY, r, a))
      .map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
  })
)

const radarAxes = computed(() =>
  RADAR_ANGLES.map((a) => polar(RADAR_CX, RADAR_CY, RADAR_R, a))
)

const radarDataPts = computed(() =>
  reportMetrics.value.map((m, i) => {
    const r = RADAR_R * (m.value / 100)
    return polar(RADAR_CX, RADAR_CY, r, RADAR_ANGLES[i])
  })
)

const radarPolygon = computed(() =>
  radarDataPts.value.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
)

const radarLabelPos = computed(() =>
  RADAR_ANGLES.map((a) => {
    const p = polar(RADAR_CX, RADAR_CY, RADAR_LABEL_R, a)
    const cosA = Math.cos(a)
    const anchor: 'start' | 'middle' | 'end' =
      cosA > 0.2 ? 'start' : cosA < -0.2 ? 'end' : 'middle'
    return { x: p.x, y: p.y, anchor }
  })
)

function scoreColorClass(value: number) {
  if (value >= 75) return 'high'
  if (value >= 60) return 'mid'
  return 'low'
}

// ─── 静默检测 ───────────────────────────────────────────────────
function clearSilenceTimer() {
  if (silenceTimer.value !== null) {
    clearTimeout(silenceTimer.value)
    silenceTimer.value = null
  }
}

function resetSilenceTimer() {
  clearSilenceTimer()
  silenceTimer.value = window.setTimeout(() => {
    silenceTimer.value = null
    if (interviewPhase.value === 'listening') {
      onSilenceDetected()
    }
  }, SILENCE_DURATION)
}

async function onSilenceDetected() {
  if (sending.value || finishing.value) return
  const content = answer.value.trim()
  if (!content) return

  // 3 秒静默 → 自动提交
  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()

  interviewPhase.value = 'processing'
  sending.value = true
  try {
    interview.value = await answerInterviewApi(interviewId.value, { content })
    // 回答已提交，前端显示用户回答文字
    // 稍作延迟让用户看到自己的回答，然后进入下一轮
    setTimeout(() => {
      answer.value = ''
      sending.value = false
      if (isLastRound.value) {
        startSummarizeFlow()
      }
      // 否则等待 watcher 检测到新 AI 消息自动进入下一轮
    }, PROCESSING_DELAY)
  } catch {
    ElMessage.error('回答提交失败')
    interviewPhase.value = 'listening'
    sending.value = false
  }
}

// ─── 自动流程：面试官思考 → 播报 → 聆听 ────────────────────────
async function startAiThinkingFlow() {
  if (!canAnswer.value) return
  const msg = latestAiMessage.value
  if (!msg) return
  if (msg.id === lastSpokenMessageId) return

  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()

  interviewPhase.value = 'ai_thinking'
  lastSpokenMessageId = msg.id

  // 短暂延迟模拟"思考"，然后开始播报
  await nextTick()
  await new Promise((resolve) => setTimeout(resolve, 800))

  if (!canAnswer.value || latestAiMessage.value?.id !== msg.id) return

  interviewPhase.value = 'ai_speaking'
  speaking.value = true

  try {
    await speakText(msg.content)
  } catch {
    // TTS 失败继续
  } finally {
    speaking.value = false
    if (canAnswer.value && interviewPhase.value === 'ai_speaking') {
      interviewPhase.value = 'listening'
      // 播报结束后自动启动语音识别
      setTimeout(() => {
        if (interviewPhase.value === 'listening' && canAnswer.value && !recognizing.value) {
          startRecognition()
        }
      }, 400)
    }
  }
}

// ─── 最后一轮：面试官总结 → 结束面试 ───────────────────────────
async function startSummarizeFlow() {
  if (!canAnswer.value) return

  interviewPhase.value = 'summarizing'
  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()

  // 延迟让用户看到总结状态
  await nextTick()
  await new Promise((resolve) => setTimeout(resolve, 2000))

  if (!canAnswer.value) return

  // 自动调用结束面试，后端 LLM 生成总结 + 评测报告
  finishing.value = true
  try {
    interview.value = await finishInterviewApi(interviewId.value)
    interviewPhase.value = 'completed'
    ElMessage.success('面试已结束，评估报告已生成')
  } catch {
    ElMessage.error('结束面试失败')
    interviewPhase.value = 'idle'
  } finally {
    finishing.value = false
  }
}

// ─── 手动控制 ──────────────────────────────────────────────────
function toggleListening() {
  if (interviewPhase.value === 'listening') {
    // 手动停止聆听
    stopRecognition()
    clearSilenceTimer()
    interviewPhase.value = 'idle'
  } else if (canAnswer.value && latestAiMessage.value) {
    // 手动开始聆听
    startRecognition()
    interviewPhase.value = 'listening'
  }
}

async function speakLatestQuestion() {
  if (!latestAiMessage.value) {
    ElMessage.warning('暂无可播报的问题')
    return
  }
  if (!speechSynthesisSupported) {
    ElMessage.warning('当前浏览器不支持语音播报')
    return
  }

  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()

  lastSpokenMessageId = latestAiMessage.value.id
  speaking.value = true
  try {
    await speakText(latestAiMessage.value.content)
    if (canAnswer.value) {
      interviewPhase.value = 'listening'
      setTimeout(() => startRecognition(), 400)
    }
  } catch (error) {
    if (error instanceof Error) ElMessage.warning(error.message)
  } finally {
    speaking.value = false
  }
}

// ─── 语音识别 ──────────────────────────────────────────────────
function startRecognition() {
  if (!speechRecognitionSupported) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用文字输入')
    return
  }
  if (!canAnswer.value || recognizing.value) return

  voiceError.value = ''

  recognitionSession = createSpeechRecognitionSession({
    onStart() {
      recognizing.value = true
      resetSilenceTimer()
    },
    onEnd() {
      recognizing.value = false
      // Chrome 长时间静默会自动停止识别，自动重启
      if (interviewPhase.value === 'listening' && canAnswer.value && !sending.value) {
        setTimeout(() => {
          if (interviewPhase.value === 'listening' && canAnswer.value && !recognizing.value) {
            startRecognition()
          }
        }, 400)
      }
    },
    onResult(result: SpeechRecognitionResultText) {
      if (result.finalText) {
        // 只写入 answer，不显示 interim 文字给用户看
        answer.value = mergeAnswerText(answer.value, result.finalText)
        resetSilenceTimer()
      } else if (result.interimText) {
        //  interim 结果不展示，只用于重置静默计时器
        resetSilenceTimer()
      }
    },
    onError(message) {
      voiceError.value = message
      recognizing.value = false
      clearSilenceTimer()
      // 可恢复错误自动重试
      if (interviewPhase.value === 'listening' && canAnswer.value && !sending.value) {
        setTimeout(() => {
          if (interviewPhase.value === 'listening' && canAnswer.value && !recognizing.value) {
            startRecognition()
          }
        }, 1500)
      }
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
  clearSilenceTimer()
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
  }
}

function clearVoiceDraft() {
  answer.value = ''
  voiceError.value = ''
}

// ─── 提交回答（文字输入按钮） ──────────────────────────────────
async function submitAnswer() {
  const content = answer.value.trim()
  if (!content) {
    ElMessage.warning('请先输入回答内容')
    return
  }

  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()

  interviewPhase.value = 'processing'
  sending.value = true
  try {
    interview.value = await answerInterviewApi(interviewId.value, { content })
    answer.value = ''
    setTimeout(() => {
      sending.value = false
      if (isLastRound.value) {
        startSummarizeFlow()
      }
    }, PROCESSING_DELAY)
  } catch {
    ElMessage.error('回答提交失败')
    interviewPhase.value = 'listening'
    sending.value = false
  }
}

// ─── 结束面试（手动按钮） ──────────────────────────────────────
async function finishInterview() {
  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()
  stopPostureMonitor()
  finishing.value = true
  try {
    interview.value = await finishInterviewApi(interviewId.value)
    interviewPhase.value = 'completed'
    ElMessage.success('面试已结束，评估报告已生成')
  } catch {
    ElMessage.error('结束面试失败')
  } finally {
    finishing.value = false
  }
}

// ─── 加载面试数据 ──────────────────────────────────────────────
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

// ─── 姿态检测 ──────────────────────────────────────────────────
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
  if (!canAnswer.value || postureActive.value || postureStarting.value) return

  postureStarting.value = true
  postureError.value = ''
  if (!postureThresholds.value.length) {
    await loadPostureThresholds()
  }
  postureMonitor = createPostureMonitor(
    {
      onStatus(message: string) { postureStatus.value = message },
      onEvent(event: LocalPostureEvent) { void handlePostureEvent(event) },
      onError(event: LocalPostureEvent) {
        postureError.value = event.detail
        void handlePostureEvent(event)
      },
    },
    postureThresholds.value,
  )

  try {
    await nextTick()
    if (!cameraVideo.value) throw new Error('摄像头预览区域未准备好')
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

async function handlePostureEvent(event: LocalPostureEvent) {
  if (!canAnswer.value || postureReporting.value) return
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
  if (!interview.value) return
  interview.value = {
    ...interview.value,
    postureEvents: [event, ...interview.value.postureEvents.filter((item) => item.id !== event.id)],
  }
}

// ─── 辅助函数 ──────────────────────────────────────────────────
function mergeAnswerText(current: string, incoming: string) {
  const trimmed = incoming.trim()
  if (!current.trim()) return trimmed
  return `${current.trimEnd()} ${trimmed}`
}

function resolveCameraError(error: unknown): LocalPostureEvent {
  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return { eventType: 'CAMERA_UNAVAILABLE', severity: 'WARNING', score: 100, detail: '摄像头权限被拒绝，请继续使用文字或语音面试' }
  }
  if (error instanceof DOMException && error.name === 'NotFoundError') {
    return { eventType: 'CAMERA_UNAVAILABLE', severity: 'WARNING', score: 100, detail: '没有检测到可用摄像头，请继续使用文字或语音面试' }
  }
  return { eventType: 'CAMERA_UNAVAILABLE', severity: 'WARNING', score: 100, detail: '摄像头启动失败，请继续使用文字或语音面试' }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

async function handleDownloadPdf() {
  if (!interview.value) return
  try {
    const reportEl = document.querySelector('.report-shell') as HTMLElement | null
    if (!reportEl) {
      ElMessage.warning('报告内容未准备好，请稍后再试')
      return
    }
    const fileName = `面试报告_${interview.value.position.name}_${new Date().toISOString().slice(0, 10)}.pdf`
    await html2pdf().set({
      margin: 12,
      filename: fileName,
      image: { type: 'jpeg', quality: 0.95 },
      html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff', logging: false },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
    }).from(reportEl).save()
    ElMessage.success('PDF报告下载成功')
  } catch {
    ElMessage.error('PDF生成失败，请重试')
  }
}

// ─── Watchers ──────────────────────────────────────────────────

// 新 AI 消息到达 → 自动进入"思考 → 播报 → 聆听"流程
watch(
  () => [interview.value?.id, latestAiMessage.value?.id] as const,
  () => {
    if (!canAnswer.value) return
    const msg = latestAiMessage.value
    if (!msg) return
    if (msg.id === lastSpokenMessageId) return
    // 不打断正在处理的状态
    if (interviewPhase.value === 'processing' || interviewPhase.value === 'summarizing') return
    // 延迟确保 DOM 已更新
    setTimeout(() => {
      if (canAnswer.value && latestAiMessage.value?.id === msg.id && msg.id !== lastSpokenMessageId) {
        if (interviewPhase.value !== 'processing' && interviewPhase.value !== 'summarizing') {
          startAiThinkingFlow()
        }
      }
    }, 600)
  }
)

watch(canAnswer, (answerable) => {
  if (!answerable) {
    stopRecognition()
    cancelSpeech()
    clearSilenceTimer()
    interviewPhase.value = interview.value?.status === 'COMPLETED' ? 'completed' : 'idle'
    stopPostureMonitor()
  }
})

watch(
  () => interview.value?.style.virtualHuman?.key,
  () => { virtualHumanLoadFailed.value = false }
)

// ─── Lifecycle ─────────────────────────────────────────────────
onMounted(async () => {
  await loadInterview()
  loadPostureThresholds().catch(() => {
    postureStatus.value = '姿态阈值配置读取失败，已使用本地默认配置'
  })
})

onBeforeUnmount(() => {
  stopRecognition()
  cancelSpeech()
  clearSilenceTimer()
  stopPostureMonitor()
})
</script>

<template>
  <main v-loading="loading" :class="isCompleted ? 'flow-page report-page' : 'interview-room-page'">
    <!-- ══════════════════════════════════════════════════════════ -->
    <!--  报告页                                                        -->
    <!-- ══════════════════════════════════════════════════════════ -->
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
          <div class="score-ring">
            <svg viewBox="0 0 120 120" class="score-ring-svg">
              <defs>
                <linearGradient id="scoreRingGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#3b82f6" />
                  <stop offset="100%" stop-color="#6366f1" />
                </linearGradient>
              </defs>
              <circle class="score-ring-bg" cx="60" cy="60" r="54" />
              <circle
                class="score-ring-progress"
                cx="60" cy="60" r="54"
                :stroke-dasharray="`${2 * Math.PI * 54}`"
                :stroke-dashoffset="`${2 * Math.PI * 54 * (1 - Math.max(0, Math.min(interview.report.totalScore, 100)) / 100)}`"
              />
            </svg>
            <div class="score-ring-text">
              <span class="score-value">{{ interview.report.totalScore }}</span>
              <span class="score-divider">/</span>
              <span class="score-max">100</span>
            </div>
          </div>
          <h2>综合评分</h2>
          <p>候选人完成了 {{ answeredCount }} 轮面试问答，共回答了 {{ interview.questionCount }} 个问题。</p>
        </section>

        <section v-if="interview.report" class="report-grid">
          <article class="report-card radar-card">
            <h2>能力雷达图</h2>
            <div class="radar-chart" v-if="reportMetrics.length > 0">
              <svg viewBox="0 0 300 300" class="radar-svg">
                <defs>
                  <linearGradient id="radarFillGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.28" />
                    <stop offset="100%" stop-color="#6366f1" stop-opacity="0.08" />
                  </linearGradient>
                </defs>
                <polygon v-for="(pts, i) in radarGridHex" :key="'g' + i" :points="pts" class="rgrid" />
                <line v-for="(end, i) in radarAxes" :key="'a' + i"
                      x1="150" y1="150" :x2="end.x.toFixed(1)" :y2="end.y.toFixed(1)" class="raxis" />
                <polygon v-if="radarPolygon" :points="radarPolygon" class="rdata" />
                <circle v-for="(pt, i) in radarDataPts" :key="'d' + i"
                        :cx="pt.x.toFixed(1)" :cy="pt.y.toFixed(1)" r="5.5" class="rdot"
                        :style="{ animationDelay: `${0.5 + i * 0.08}s` }" />
                <g v-for="(m, i) in reportMetrics" :key="'l' + i">
                  <text :x="radarLabelPos[i].x" :y="radarLabelPos[i].y - 5"
                        :text-anchor="radarLabelPos[i].anchor" class="rlbl-name">{{ m.label }}</text>
                  <text :x="radarLabelPos[i].x" :y="radarLabelPos[i].y + 11"
                        :text-anchor="radarLabelPos[i].anchor"
                        class="rlbl-score" :class="scoreColorClass(m.value)">{{ m.value }}</text>
                </g>
              </svg>
            </div>
            <div v-else class="radar-empty">
              <p>暂无评估数据</p>
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
            <h2><span class="insight-icon star-icon">&#10022;</span> 亮点</h2>
            <p>{{ interview.report.strengths }}</p>
          </article>
          <article class="insight-card warning">
            <h2><span class="insight-icon chat-icon">&#128172;</span> 建议</h2>
            <p>{{ interview.report.recommendation || interview.report.improvements }}</p>
          </article>
        </section>

        <section class="dialogue-card">
          <div class="section-toolbar">
            <div>
              <h2><el-icon><Document /></el-icon> 面试对话记录</h2>
            </div>
            <p>共 {{ interview.messages.length }} 条消息，候选人回答了 {{ answeredCount }} 个问题。</p>
          </div>
          <div class="dialogue-list">
            <div v-for="message in interview.messages" :key="message.id" class="dialogue-row" :class="message.role">
              <div class="dialogue-bubble">
                <span class="role-label">{{ message.role === 'USER' ? '候选人' : '面试官' }}</span>
                <p>{{ message.content }}</p>
              </div>
            </div>
          </div>
        </section>
      </section>
    </template>

    <!-- ══════════════════════════════════════════════════════════ -->
    <!--  面试进行中                                                     -->
    <!-- ══════════════════════════════════════════════════════════ -->
    <template v-else-if="interview">
      <header class="interview-header">
        <button class="back-button dark" type="button" @click="router.push(readonlyAdminView ? '/admin' : '/interviews')">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <div class="interview-status">
          <span class="status-dot" />
          {{ readonlyAdminView ? '只读查看' : '面试进行中' }}
        </div>
        <div class="round-indicator" v-if="canAnswer && !isLastRound">
          第 {{ roundInfo.current + 1 }} / {{ roundInfo.total }} 轮
        </div>
      </header>

      <section class="interview-main">
        <!-- 面试官视频 -->
        <article class="video-card interviewer-card" :style="{ '--avatar-accent': virtualHuman.accent }">
          <img
            v-if="virtualHuman.src && !virtualHumanLoadFailed"
            :src="virtualHuman.src"
            :alt="virtualHuman.name"
            @error="virtualHumanLoadFailed = true"
          >
          <div v-else class="avatar-placeholder">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="video-badges">
            <span class="name-badge">{{ virtualHuman.name }}</span>
            <button class="mic-btn" type="button" aria-label="麦克风">
              <el-icon><Microphone /></el-icon>
            </button>
          </div>
          <!-- 思考/播报状态浮层 -->
          <div v-if="interviewPhase === 'ai_thinking' || interviewPhase === 'ai_speaking' || interviewPhase === 'summarizing'"
               class="ai-state-overlay">
            <div class="thinking-dots" v-if="interviewPhase === 'ai_thinking'">
              <span /><span /><span />
            </div>
            <div class="speaking-wave" v-else-if="interviewPhase === 'ai_speaking'">
              <span /><span /><span /><span /><span />
            </div>
            <div class="summarizing-icon" v-else>
              <el-icon><Document /></el-icon>
              <span>面试总结中</span>
            </div>
          </div>
        </article>

        <!-- 候选人摄像头 -->
        <article class="video-card candidate-card">
          <video ref="cameraVideo" muted playsinline autoplay aria-label="本地摄像头预览" />
          <div v-if="!postureActive" class="avatar-placeholder camera-off">
            <el-icon><Camera /></el-icon>
            <span>摄像头未开启</span>
          </div>
          <div class="video-badges">
            <span class="name-badge">您的画面</span>
            <button class="mic-btn" type="button" aria-label="麦克风">
              <el-icon><Microphone /></el-icon>
            </button>
          </div>
        </article>

        <!-- 右侧面板 -->
        <aside class="interview-panel">
          <div class="panel-header">
            <div class="interviewer-profile">
              <div class="profile-avatar" :style="{ '--avatar-accent': virtualHuman.accent }">
                <img
                  v-if="virtualHuman.src && !virtualHumanLoadFailed"
                  :src="virtualHuman.src"
                  :alt="virtualHuman.name"
                >
                <span v-else>{{ virtualHuman.initials }}</span>
              </div>
              <div class="profile-copy">
                <strong>{{ virtualHuman.name }}</strong>
                <p>{{ virtualHuman.badge }} · {{ virtualHumanMotion.label }}</p>
              </div>
            </div>
          </div>

          <div class="panel-content">
            <!-- 面试官提问卡片 -->
            <div v-if="latestAiMessage" class="question-card" :class="{ fading: interviewPhase === 'ai_thinking' || interviewPhase === 'ai_speaking' }">
              <div class="question-header">
                <span class="question-label">面试官提问</span>
                <span v-if="phaseLabel" class="phase-indicator" :class="interviewPhase">
                  {{ phaseLabel }}
                </span>
              </div>
              <p>{{ latestAiMessage.content }}</p>
            </div>

            <!-- 语音状态指示 -->
            <div class="listening-status">
              <span class="status-dot" :class="statusDotClass" />
              {{ phaseLabel || voiceError || voiceStatusText }}
            </div>

            <!-- 用户回答区（仅在 processing 或 listening 且已有内容时显示） -->
            <div v-if="answer.trim() && (interviewPhase === 'listening' || interviewPhase === 'processing')"
                 class="answer-card">
              <span class="answer-label">您的回答</span>
              <p>{{ answer }}</p>
            </div>

            <!-- 主控制按钮 -->
            <div class="control-buttons" v-if="canAnswer">
              <button
                v-if="speechRecognitionSupported"
                class="control-btn"
                :class="{ 'mic-active': isListening }"
                type="button"
                :disabled="isAiSpeaking || interviewPhase === 'processing' || interviewPhase === 'ai_thinking'"
                @click="toggleListening"
              >
                <el-icon v-if="!isListening"><Microphone /></el-icon>
                <el-icon v-else><VideoPause /></el-icon>
                {{ isListening ? '停止录音' : '语音回答' }}
              </button>

              <button
                class="control-btn"
                type="button"
                :disabled="!latestAiMessage || isAiSpeaking || interviewPhase === 'processing' || interviewPhase === 'ai_thinking'"
                @click="speakLatestQuestion()"
              >
                <el-icon><Headset /></el-icon>
                重新播报
              </button>

              <template v-if="flags.posture">
                <button
                  v-if="!postureActive"
                  class="control-btn"
                  type="button"
                  :loading="postureStarting"
                  :disabled="postureStarting || !cameraSupported"
                  @click="startPostureMonitor"
                >
                  <el-icon><VideoCamera /></el-icon>
                  开启摄像头
                </button>
                <button
                  v-else
                  class="control-btn stop"
                  type="button"
                  @click="stopPostureMonitor"
                >
                  <el-icon><VideoPause /></el-icon>
                  关闭摄像头
                </button>
              </template>
            </div>

            <!-- 更多设置（文字输入等） -->
            <div class="more-settings">
              <button class="settings-toggle" type="button" @click="settingsOpen = !settingsOpen">
                <span>更多设置</span>
                <el-icon class="toggle-arrow" :class="{ 'is-open': settingsOpen }">
                  <ArrowDown />
                </el-icon>
              </button>
              <div v-if="settingsOpen" class="settings-body">
                <template v-if="flags.speech">
                  <div class="settings-section">
                    <div class="settings-row">
                      <span class="settings-hint">语音功能已自动启用，使用上方按钮控制</span>
                    </div>
                  </div>
                </template>

                <template v-if="flags.posture">
                  <div v-if="canAnswer" class="settings-section">
                    <div class="settings-row">
                      <button
                        v-if="!postureActive"
                        class="el-button el-button--default"
                        :icon="VideoCamera"
                        :loading="postureStarting"
                        :disabled="postureStarting || !cameraSupported"
                        @click="startPostureMonitor"
                      >
                        开启摄像头检测
                      </button>
                      <button
                        v-else
                        class="el-button el-button--warning is-plain"
                        :icon="VideoPause"
                        @click="stopPostureMonitor"
                      >
                        关闭摄像头检测
                      </button>
                    </div>
                  </div>
                </template>

                <div v-if="canAnswer" class="settings-section">
                  <el-input
                    v-model="answer"
                    type="textarea"
                    :rows="3"
                    maxlength="4000"
                    show-word-limit
                    placeholder="或在此输入文字回答..."
                    :disabled="isAiSpeaking || interviewPhase === 'processing' || interviewPhase === 'ai_thinking'"
                  />
                  <div class="settings-row" style="margin-top: 8px;">
                    <el-button
                      :icon="ChatLineRound"
                      type="primary"
                      :loading="sending"
                      :disabled="!answer.trim() || isAiSpeaking || interviewPhase === 'processing' || interviewPhase === 'ai_thinking'"
                      @click="submitAnswer"
                    >
                      提交回答
                    </el-button>
                    <el-button plain :disabled="!answer.trim()" @click="clearVoiceDraft">清空草稿</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="panel-footer">
            <button
              class="end-interview-btn"
              :loading="finishing"
              :disabled="finishing"
              @click="finishInterview"
            >
              结束面试
            </button>
          </div>
        </aside>
      </section>

      <!-- 面试结束加载遮罩 -->
      <div v-if="finishing" class="report-loading-mask">
        <div class="report-loading-card">
          <span class="loading-spinner" />
          <strong>正在生成评估报告...</strong>
        </div>
      </div>
    </template>
  </main>
</template>
