<script setup lang="ts">
import {
  Camera,
  ChatLineRound,
  CircleCheck,
  Delete,
  Finished,
  Headset,
  Microphone,
  MuteNotification,
  SwitchButton,
  UserFilled,
  Warning,
  VideoCamera,
  VideoPause,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  answerInterviewApi,
  fetchAdminInterviewApi,
  fetchInterviewApi,
  finishInterviewApi,
  reportPostureEventApi,
} from '@/api/interviews'
import {
  cancelSpeech,
  createSpeechRecognitionSession,
  isSpeechRecognitionSupported,
  isSpeechSynthesisSupported,
  speakText,
  type SpeechRecognitionSession,
} from '@/services/voice'
import {
  createPostureEventRequest,
  createPostureMonitor,
  isCameraSupported,
  type LocalPostureEvent,
  type PostureMonitor,
} from '@/services/posture'
import { resolveVirtualHuman } from '@/services/virtualHuman'
import type { InterviewDetail, PostureEvent, PostureEventType, PostureSeverity } from '@/types'

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
const postureEvents = computed(() => interview.value?.postureEvents ?? [])
const virtualHuman = computed(() => resolveVirtualHuman(interview.value?.style.virtualHuman))
const postureWarningCount = computed(
  () => postureEvents.value.filter((event) => event.severity !== 'INFO').length,
)
const latestAiMessage = computed(() => {
  const messages = interview.value?.messages ?? []
  return [...messages].reverse().find((message) => message.role === 'ASSISTANT') ?? null
})
const voiceStatusText = computed(() => {
  if (!speechRecognitionSupported) {
    return '当前浏览器不支持语音识别，已保留文字输入'
  }
  if (!canAnswer.value) {
    return '当前面试不可回答，语音输入已关闭'
  }
  if (recognizing.value) {
    return '正在听写回答'
  }
  return '可使用麦克风听写回答'
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
  postureMonitor = createPostureMonitor({
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
  })

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

function postureTypeLabel(type: PostureEventType) {
  const labels: Record<PostureEventType, string> = {
    FACE_MISSING: '未检测到人脸',
    FACE_OFF_CENTER: '人脸偏离中央',
    TOO_CLOSE: '距离过近',
    TOO_FAR: '距离过远',
    TOO_STILL: '画面长时间静止',
    LOW_LIGHT: '光线偏暗',
    CAMERA_UNAVAILABLE: '摄像头不可用',
  }
  return labels[type]
}

function severityTagType(severity: PostureSeverity) {
  if (severity === 'CRITICAL') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
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

onMounted(loadInterview)
onBeforeUnmount(() => {
  stopRecognition()
  cancelSpeech()
  stopPostureMonitor()
})
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

        <div v-if="interview.resume.used" class="resume-context-strip">
          <el-tag type="success">已使用简历</el-tag>
          <p v-if="interview.resume.summary">{{ interview.resume.summary }}</p>
          <p v-else>本场面试创建时使用过简历；面试结束后临时解析上下文已清理。</p>
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
          <div class="voice-panel">
            <div class="voice-status">
              <el-icon><Headset /></el-icon>
              <div>
                <strong>语音辅助</strong>
                <p>{{ voiceError || voiceStatusText }}</p>
              </div>
            </div>
            <div class="voice-actions">
              <el-button
                v-if="!recognizing"
                :icon="Microphone"
                :disabled="!speechRecognitionSupported || sending || finishing"
                @click="startRecognition"
              >
                开始听写
              </el-button>
              <el-button v-else :icon="VideoPause" type="warning" plain @click="stopRecognition">
                停止听写
              </el-button>
              <el-button
                :icon="Headset"
                :loading="speaking"
                :disabled="!speechSynthesisSupported || !latestAiMessage"
                plain
                @click="speakLatestQuestion()"
              >
                播报问题
              </el-button>
              <el-switch
                v-model="autoSpeak"
                :disabled="!speechSynthesisSupported"
                inline-prompt
                active-text="自动播报"
                inactive-text="手动播报"
              />
              <el-button :icon="Delete" plain @click="clearVoiceDraft">清空草稿</el-button>
            </div>
            <div v-if="interimSpeechText" class="speech-preview" aria-live="polite">
              <el-icon><MuteNotification /></el-icon>
              <span>{{ interimSpeechText }}</span>
            </div>
          </div>
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
        <div class="virtual-human-block">
          <div class="section-toolbar">
            <div>
              <h2>虚拟面试官</h2>
              <p>{{ interview.style.name }} · {{ virtualHuman.badge }}</p>
            </div>
            <el-tag type="success">静态形象</el-tag>
          </div>

          <div class="virtual-human-stage" :style="{ '--avatar-accent': virtualHuman.accent }">
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
          </div>

          <div class="virtual-human-copy">
            <strong>{{ virtualHuman.name }}</strong>
            <p>{{ virtualHuman.description }}</p>
            <p v-if="virtualHuman.missingAsset || virtualHumanLoadFailed" class="virtual-human-degraded">
              虚拟人资源不可用，已切换为占位展示。
            </p>
          </div>
        </div>

        <div class="posture-block">
          <div class="section-toolbar">
            <div>
              <h2>姿态检测</h2>
              <p>
                {{ postureEvents.length }} 条结构化事件，{{ postureWarningCount }} 条需要关注。
              </p>
            </div>
            <el-tag :type="postureActive ? 'success' : 'info'">
              {{ postureActive ? '检测中' : '未开启' }}
            </el-tag>
          </div>

          <div class="camera-preview">
            <video
              ref="cameraVideo"
              muted
              playsinline
              aria-label="本地摄像头预览"
            />
            <div v-if="!postureActive" class="camera-placeholder">
              <el-icon><Camera /></el-icon>
              <span>{{ postureError || postureStatus }}</span>
            </div>
          </div>

          <div v-if="canAnswer" class="posture-actions">
            <el-button
              v-if="!postureActive"
              :icon="VideoCamera"
              :loading="postureStarting"
              :disabled="postureStarting"
              @click="startPostureMonitor"
            >
              开启检测
            </el-button>
            <el-button v-else :icon="VideoPause" type="warning" plain @click="stopPostureMonitor">
              关闭检测
            </el-button>
          </div>

          <div class="posture-status" :class="{ warning: Boolean(postureError) }">
            <el-icon>
              <Warning v-if="postureError" />
              <CircleCheck v-else />
            </el-icon>
            <span>{{ postureError || postureStatus }}</span>
          </div>

          <div v-if="postureEvents.length" class="posture-event-list">
            <div v-for="event in postureEvents.slice(0, 5)" :key="event.id" class="posture-event-item">
              <div>
                <strong>{{ postureTypeLabel(event.eventType) }}</strong>
                <span>{{ event.detail || '已记录结构化姿态事件' }}</span>
              </div>
              <el-tag :type="severityTagType(event.severity)" size="small">{{ event.score }}</el-tag>
            </div>
          </div>
          <el-empty v-else :image-size="72" description="暂无姿态事件" />
        </div>

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
