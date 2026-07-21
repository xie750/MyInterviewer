/**
 * 姿态检测服务：浏览器本地摄像头检测
 *
 * Feature Flag：VITE_FEATURE_POSTURE
 *  - true  （默认）：正常加载姿态检测
 *  - false       ：所有检测函数直接返回，不启动摄像头
 *
 * 依赖浏览器 FaceDetector API（仅 Chrome 支持），
 * 不可用时会自动降级为亮度 + 画面变化检测。
 */

import { featureFlags } from '@/config/featureConfig'

// ---- Feature Gate ----
const POSTURE_ENABLED = featureFlags.posture

import type { PostureEventRequest, PostureEventType, PostureSeverity, PostureThreshold } from '@/types'

interface FaceDetectorBox {
  x: number
  y: number
  width: number
  height: number
}

interface FaceDetectorResult {
  boundingBox: FaceDetectorBox
}

interface FaceDetectorLike {
  detect(source: CanvasImageSource): Promise<FaceDetectorResult[]>
}

type FaceDetectorConstructor = new (options?: { fastMode?: boolean; maxDetectedFaces?: number }) => FaceDetectorLike

declare global {
  interface Window {
    FaceDetector?: FaceDetectorConstructor
  }
}

export interface LocalPostureEvent {
  eventType: PostureEventType
  severity: PostureSeverity
  score: number
  detail: string
}

export interface PostureMonitorCallbacks {
  onStatus: (message: string) => void
  onEvent: (event: LocalPostureEvent) => void
  onError: (event: LocalPostureEvent) => void
}

export interface PostureMonitor {
  start(video: HTMLVideoElement): Promise<void>
  stop(): void
}

export interface PostureRuntimeThresholds {
  lowLightWarning: number
  lowLightCritical: number
  stillFrameLimit: number
  faceCenterEdgePercent: number
  tooClosePercent: number
  tooFarPercent: number
}

const SAMPLE_WIDTH = 96
const SAMPLE_HEIGHT = 54
const SAMPLE_INTERVAL_MS = 2500
const EVENT_COOLDOWN_MS = 30000
const DEFAULT_THRESHOLDS: PostureRuntimeThresholds = {
  lowLightWarning: 45,
  lowLightCritical: 28,
  stillFrameLimit: 5,
  faceCenterEdgePercent: 28,
  tooClosePercent: 68,
  tooFarPercent: 14,
}

export function isCameraSupported() {
  if (!POSTURE_ENABLED) return false
  return typeof navigator !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia)
}

export function createPostureEventRequest(
  interviewId: number,
  event: LocalPostureEvent,
): PostureEventRequest {
  if (!POSTURE_ENABLED) {
    throw new Error('姿态检测功能未开启')
  }
  return {
    interviewId,
    eventType: event.eventType,
    severity: event.severity,
    score: event.score,
    detail: event.detail,
    occurredAt: toLocalDateTimeString(new Date()),
  }
}

export function createPostureMonitor(
  callbacks: PostureMonitorCallbacks,
  thresholdConfigs: PostureThreshold[] = [],
): PostureMonitor {
  if (!POSTURE_ENABLED) {
    return {
      async start() {
        callbacks.onError({
          eventType: 'CAMERA_UNAVAILABLE',
          severity: 'WARNING',
          score: 100,
          detail: '姿态检测功能当前未开启',
        })
      },
      stop() {
        // no-op
      },
    }
  }

  const thresholds = resolveRuntimeThresholds(thresholdConfigs)
  let stream: MediaStream | null = null
  let timerId: number | null = null
  let previousFrame: Uint8ClampedArray | null = null
  let stillFrames = 0
  const canvas = document.createElement('canvas')
  canvas.width = SAMPLE_WIDTH
  canvas.height = SAMPLE_HEIGHT
  const context = canvas.getContext('2d', { willReadFrequently: true })
  const detector = window.FaceDetector ? new window.FaceDetector({ fastMode: true, maxDetectedFaces: 1 }) : null
  const lastEmittedAt = new Map<PostureEventType, number>()

  async function start(video: HTMLVideoElement) {
    if (!isCameraSupported()) {
      callbacks.onError({
        eventType: 'CAMERA_UNAVAILABLE',
        severity: 'WARNING',
        score: 100,
        detail: '当前浏览器不支持摄像头访问，请继续使用文字或语音面试',
      })
      return
    }

    stream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 640 },
        height: { ideal: 360 },
        facingMode: 'user',
      },
      audio: false,
    })
    video.srcObject = stream
    await video.play()
    callbacks.onStatus(detector ? '摄像头检测已开启：正在检测人脸位置、距离和画面状态' : '摄像头检测已开启：当前浏览器使用亮度和画面变化兜底检测')

    timerId = window.setInterval(() => {
      void sample(video)
    }, SAMPLE_INTERVAL_MS)
    await sample(video)
  }

  function stop() {
    if (timerId !== null) {
      window.clearInterval(timerId)
      timerId = null
    }
    if (stream) {
      stream.getTracks().forEach((track) => track.stop())
      stream = null
    }
    previousFrame = null
    stillFrames = 0
  }

  async function sample(video: HTMLVideoElement) {
    if (!context || video.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) {
      return
    }

    context.drawImage(video, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT)
    const frame = context.getImageData(0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT).data
    analyzeBrightness(frame)
    analyzeMotion(frame)

    if (detector) {
      try {
        const faces = await detector.detect(canvas)
        analyzeFace(faces[0])
      } catch {
        callbacks.onStatus('人脸检测不可用，已自动保留画面状态兜底检测')
      }
    }
  }

  function analyzeBrightness(frame: Uint8ClampedArray) {
    let luminanceTotal = 0
    for (let index = 0; index < frame.length; index += 4) {
      luminanceTotal += frame[index] * 0.299 + frame[index + 1] * 0.587 + frame[index + 2] * 0.114
    }
    const average = luminanceTotal / (frame.length / 4)
    if (average < thresholds.lowLightWarning) {
      emit({
        eventType: 'LOW_LIGHT',
        severity: average < thresholds.lowLightCritical ? 'CRITICAL' : 'WARNING',
        score: Math.min(100, Math.round(100 - average)),
        detail: '摄像头画面亮度偏低，建议打开灯光或调整屏幕角度',
      })
    }
  }

  function analyzeMotion(frame: Uint8ClampedArray) {
    if (!previousFrame) {
      previousFrame = new Uint8ClampedArray(frame)
      return
    }

    let difference = 0
    for (let index = 0; index < frame.length; index += 16) {
      difference += Math.abs(frame[index] - previousFrame[index])
    }
    previousFrame = new Uint8ClampedArray(frame)

    const averageDifference = difference / (frame.length / 16)
    if (averageDifference < 2.5) {
      stillFrames += 1
    } else {
      stillFrames = 0
    }

    if (stillFrames >= thresholds.stillFrameLimit) {
      emit({
        eventType: 'TOO_STILL',
        severity: 'INFO',
        score: 55,
        detail: '画面长时间几乎无变化，请确认候选人仍在镜头前',
      })
      stillFrames = 0
    }
  }

  function analyzeFace(face?: FaceDetectorResult) {
    if (!face) {
      emit({
        eventType: 'FACE_MISSING',
        severity: 'CRITICAL',
        score: 90,
        detail: '摄像头未检测到人脸，请回到镜头中央',
      })
      return
    }

    const box = face.boundingBox
    const centerX = (box.x + box.width / 2) / SAMPLE_WIDTH
    const centerY = (box.y + box.height / 2) / SAMPLE_HEIGHT
    const widthRatio = box.width / SAMPLE_WIDTH

    const centerEdge = thresholds.faceCenterEdgePercent / 100
    const centerVerticalEdge = Math.max(centerEdge - 0.04, 0.1)

    if (
      centerX < centerEdge
      || centerX > 1 - centerEdge
      || centerY < centerVerticalEdge
      || centerY > 1 - centerVerticalEdge
    ) {
      emit({
        eventType: 'FACE_OFF_CENTER',
        severity: 'WARNING',
        score: 70,
        detail: '人脸偏离画面中央，建议调整坐姿或摄像头角度',
      })
    }
    if (widthRatio > thresholds.tooClosePercent / 100) {
      emit({
        eventType: 'TOO_CLOSE',
        severity: 'WARNING',
        score: 65,
        detail: '人脸距离摄像头过近，建议稍微后移',
      })
    }
    if (widthRatio < thresholds.tooFarPercent / 100) {
      emit({
        eventType: 'TOO_FAR',
        severity: 'WARNING',
        score: 65,
        detail: '人脸距离摄像头较远，建议靠近摄像头',
      })
    }
  }

  function emit(event: LocalPostureEvent) {
    const now = Date.now()
    const lastAt = lastEmittedAt.get(event.eventType) ?? 0
    if (now - lastAt < EVENT_COOLDOWN_MS) {
      return
    }
    lastEmittedAt.set(event.eventType, now)
    callbacks.onEvent(event)
  }

  return {
    start,
    stop,
  }
}

function resolveRuntimeThresholds(configs: PostureThreshold[]): PostureRuntimeThresholds {
  const enabled = new Map(configs.filter((config) => config.enabled).map((config) => [config.eventType, config]))

  return {
    lowLightWarning: enabled.get('LOW_LIGHT')?.warningThreshold ?? DEFAULT_THRESHOLDS.lowLightWarning,
    lowLightCritical: enabled.get('LOW_LIGHT')?.criticalThreshold ?? DEFAULT_THRESHOLDS.lowLightCritical,
    stillFrameLimit: enabled.get('TOO_STILL')?.warningThreshold ?? DEFAULT_THRESHOLDS.stillFrameLimit,
    faceCenterEdgePercent: enabled.get('FACE_OFF_CENTER')?.warningThreshold ?? DEFAULT_THRESHOLDS.faceCenterEdgePercent,
    tooClosePercent: enabled.get('TOO_CLOSE')?.warningThreshold ?? DEFAULT_THRESHOLDS.tooClosePercent,
    tooFarPercent: enabled.get('TOO_FAR')?.warningThreshold ?? DEFAULT_THRESHOLDS.tooFarPercent,
  }
}

function toLocalDateTimeString(value: Date) {
  const pad = (input: number) => input.toString().padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}
