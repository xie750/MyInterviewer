type SpeechRecognitionConstructor = new () => SpeechRecognitionLike

interface SpeechRecognitionAlternativeLike {
  transcript: string
  confidence: number
}

interface SpeechRecognitionResultLike {
  readonly isFinal: boolean
  readonly length: number
  item(index: number): SpeechRecognitionAlternativeLike
  [index: number]: SpeechRecognitionAlternativeLike
}

interface SpeechRecognitionResultListLike {
  readonly length: number
  item(index: number): SpeechRecognitionResultLike
  [index: number]: SpeechRecognitionResultLike
}

interface SpeechRecognitionEventLike extends Event {
  readonly resultIndex: number
  readonly results: SpeechRecognitionResultListLike
}

interface SpeechRecognitionErrorEventLike extends Event {
  readonly error: string
  readonly message: string
}

interface SpeechRecognitionLike {
  continuous: boolean
  interimResults: boolean
  lang: string
  maxAlternatives: number
  onstart: (() => void) | null
  onend: (() => void) | null
  onresult: ((event: SpeechRecognitionEventLike) => void) | null
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null
  start(): void
  stop(): void
  abort(): void
}

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor
    webkitSpeechRecognition?: SpeechRecognitionConstructor
  }
}

export interface SpeechRecognitionResultText {
  finalText: string
  interimText: string
}

export interface SpeechRecognitionSession {
  start(): void
  stop(): void
  abort(): void
}

export interface SpeechRecognitionOptions {
  lang?: string
  onStart?: () => void
  onEnd?: () => void
  onResult: (result: SpeechRecognitionResultText) => void
  onError: (message: string) => void
}

export function isSpeechRecognitionSupported() {
  return typeof window !== 'undefined' && Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
}

export function isSpeechSynthesisSupported() {
  return typeof window !== 'undefined' && 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window
}

export function createSpeechRecognitionSession(
  options: SpeechRecognitionOptions,
): SpeechRecognitionSession | null {
  const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!Recognition) {
    return null
  }

  const recognition = new Recognition()
  recognition.lang = options.lang ?? 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true
  recognition.maxAlternatives = 1

  recognition.onstart = () => {
    options.onStart?.()
  }

  recognition.onend = () => {
    options.onEnd?.()
  }

  recognition.onerror = (event) => {
    options.onError(resolveSpeechRecognitionError(event.error, event.message))
  }

  recognition.onresult = (event) => {
    let finalText = ''
    let interimText = ''

    for (let index = event.resultIndex; index < event.results.length; index += 1) {
      const result = event.results[index]
      const transcript = result[0]?.transcript.trim() ?? ''
      if (!transcript) {
        continue
      }
      if (result.isFinal) {
        finalText += transcript
      } else {
        interimText += transcript
      }
    }

    options.onResult({
      finalText,
      interimText,
    })
  }

  return {
    start() {
      recognition.start()
    },
    stop() {
      recognition.stop()
    },
    abort() {
      recognition.abort()
    },
  }
}

export function cancelSpeech() {
  if (isSpeechSynthesisSupported()) {
    window.speechSynthesis.cancel()
  }
}

export function speakText(text: string, lang = 'zh-CN') {
  if (!isSpeechSynthesisSupported()) {
    return Promise.reject(new Error('当前浏览器不支持语音播报'))
  }

  const content = text.trim()
  if (!content) {
    return Promise.resolve()
  }

  window.speechSynthesis.cancel()

  return new Promise<void>((resolve, reject) => {
    const utterance = new SpeechSynthesisUtterance(content)
    utterance.lang = lang
    utterance.rate = 1
    utterance.pitch = 1

    utterance.onend = () => {
      resolve()
    }
    utterance.onerror = () => {
      reject(new Error('语音播报失败，请继续查看文字问题'))
    }

    window.speechSynthesis.speak(utterance)
  })
}

function resolveSpeechRecognitionError(error: string, fallbackMessage: string) {
  if (error === 'not-allowed' || error === 'service-not-allowed') {
    return '麦克风权限被拒绝，请改用文字输入'
  }
  if (error === 'no-speech') {
    return '没有识别到语音，请靠近麦克风后重试'
  }
  if (error === 'audio-capture') {
    return '没有检测到可用麦克风，请改用文字输入'
  }
  if (error === 'network') {
    return '语音识别服务暂时不可用，请改用文字输入'
  }
  return fallbackMessage || '语音识别失败，请改用文字输入'
}
