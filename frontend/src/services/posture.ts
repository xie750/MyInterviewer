import type { PostureEventRequest, PostureEventType, PostureSeverity } from '@/types'

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

const SAMPLE_WIDTH = 96
const SAMPLE_HEIGHT = 54
const SAMPLE_INTERVAL_MS = 2500
const EVENT_COOLDOWN_MS = 30000

export function isCameraSupported() {
  return typeof navigator !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia)
}

export function createPostureEventRequest(
  interviewId: number,
  event: LocalPostureEvent,
): PostureEventRequest {
  return {
    interviewId,
    eventType: event.eventType,
    severity: event.severity,
    score: event.score,
    detail: event.detail,
    occurredAt: toLocalDateTimeString(new Date()),
  }
}

export function createPostureMonitor(callbacks: PostureMonitorCallbacks): PostureMonitor {
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
    if (average < 45) {
      emit({
        eventType: 'LOW_LIGHT',
        severity: average < 28 ? 'CRITICAL' : 'WARNING',
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

    if (stillFrames >= 5) {
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

    if (centerX < 0.28 || centerX > 0.72 || centerY < 0.24 || centerY > 0.76) {
      emit({
        eventType: 'FACE_OFF_CENTER',
        severity: 'WARNING',
        score: 70,
        detail: '人脸偏离画面中央，建议调整坐姿或摄像头角度',
      })
    }
    if (widthRatio > 0.68) {
      emit({
        eventType: 'TOO_CLOSE',
        severity: 'WARNING',
        score: 65,
        detail: '人脸距离摄像头过近，建议稍微后移',
      })
    }
    if (widthRatio < 0.14) {
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

function toLocalDateTimeString(value: Date) {
  const pad = (input: number) => input.toString().padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}
