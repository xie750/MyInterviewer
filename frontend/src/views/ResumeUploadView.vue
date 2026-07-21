<script setup lang="ts">
import { Document, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import { parseResumeApi } from '@/api/resumes'
import { useFeatureFlags } from '@/composables/useFeatureFlags'

const flags = useFeatureFlags()
const router = useRouter()

const file = ref<File | null>(null)
const parsing = ref(false)
const parsedResult = ref<{ summary: string; skills: string[]; projects: string[]; warnings: string[] } | null>(null)
const dragOver = ref(false)
const pdfUrl = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

const canNext = computed(() => file.value !== null)

function openFilePicker() {
  fileInputRef.value?.click()
}

function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const selected = target.files?.[0] ?? null
  processFile(selected)
  target.value = ''
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  dragOver.value = false
  const dropped = e.dataTransfer?.files[0] ?? null
  processFile(dropped)
}

function handleDragOver(e: DragEvent) {
  e.preventDefault()
  dragOver.value = true
}

function handleDragLeave() {
  dragOver.value = false
}

async function processFile(f: File | null) {
  if (!f) return

  const ext = f.name.split('.').pop()?.toLowerCase()
  if (!['pdf', 'doc', 'docx'].includes(ext ?? '')) {
    ElMessage.warning('仅支持 Word(.doc/.docx) 和 PDF 格式')
    return
  }
  if (f.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    return
  }

  file.value = f
  pdfUrl.value = URL.createObjectURL(f)
  parsedResult.value = null

  if (!flags.resume) {
    return
  }

  parsing.value = true
  try {
    const raw = await parseResumeApi(f)
    parsedResult.value = {
      summary: raw.summary ?? '',
      skills: raw.skills,
      projects: raw.projects,
      warnings: raw.warnings,
    }
  } catch {
    parsedResult.value = null
  } finally {
    parsing.value = false
  }
}

function clearFile() {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
  }
  pdfUrl.value = ''
  file.value = null
  parsedResult.value = null
}

function goNext() {
  if (!canNext.value || !file.value) return

  router.push({
    path: '/interviews/select',
    state: {
      fileName: file.value.name,
      fileSize: file.value.size,
      parsedResult: parsedResult.value,
    },
  })
}

function goBack() {
  router.push('/interviews')
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <main class="resume-upload-page">
    <!-- 顶部导航栏 -->
    <header class="upload-topbar">
      <button class="back-button" type="button" @click="goBack">
        <el-icon><RefreshLeft /></el-icon>
        返回
      </button>
      <div class="step-indicator">
        <span class="step-dot done">
          <el-icon><Document /></el-icon>
        </span>
        <span class="step-line" />
        <span class="step-dot active">2</span>
      </div>
    </header>

    <!-- 内容区域 -->
    <section class="upload-content">
      <div class="upload-header">
        <h1>上传简历</h1>
        <p>上传您的简历，AI 将根据简历内容为您定制面试</p>
      </div>

      <!-- 已上传状态 -->
      <div v-if="file" class="uploaded-state">
        <div class="file-info-card">
          <div class="file-icon-wrap">
            <el-icon><Document /></el-icon>
          </div>
          <div class="file-meta">
            <strong class="file-name">{{ file.name }}</strong>
            <span class="file-size">{{ formatSize(file.size) }}</span>
          </div>
          <el-button circle plain size="small" class="remove-btn" @click="clearFile">
            <el-icon><RefreshLeft /></el-icon>
          </el-button>
        </div>
        <div v-if="pdfUrl" class="file-preview-wrap">
          <iframe v-if="pdfUrl" :src="pdfUrl" class="preview-iframe" />
        </div>
      </div>

      <!-- 上传区域 -->
      <div
        v-else
        class="upload-card"
        @click="openFilePicker"
        @drop="handleDrop"
        @dragover="handleDragOver"
        @dragleave="handleDragLeave"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".doc,.docx,.pdf"
          class="hidden-input"
          @change="handleFileChange"
        />
        <div class="upload-card-icon">
          <el-icon><Document /></el-icon>
        </div>
        <p class="upload-card-title">点击或拖拽文件到此处</p>
        <p class="upload-card-hint">支持 Word (.doc/.docx) 和 PDF 格式，最大 50MB</p>
      </div>
    </section>

    <!-- 底部操作栏 -->
    <footer class="upload-footer">
      <el-button
        type="primary"
        size="large"
        :disabled="!canNext"
        @click="goNext"
      >
        下一步
      </el-button>
    </footer>
  </main>
</template>

<style scoped>
.resume-upload-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 16% 24%, rgb(178 230 255 / 62%), transparent 34%),
    radial-gradient(circle at 78% 24%, rgb(223 210 255 / 58%), transparent 32%),
    linear-gradient(135deg, #f5fcff 0%, #f7f8ff 52%, #ffffff 100%);
  display: flex;
  flex-direction: column;
}

/* ====== 顶部栏 ====== */
.upload-topbar {
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

/* ====== 步骤指示器（右上角水平排列） ====== */
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
  background: #2f6bff;
}

/* ====== 内容区域 ====== */
.upload-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 120px;
  overflow-y: auto;
}

/* ====== 上传卡片（空状态） ====== */
.upload-card {
  width: min(760px, 100%);
  min-height: 380px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  border: 1px dashed rgb(215 226 242 / 80%);
  border-radius: 12px;
  background: rgb(255 255 255 / 95%);
  box-shadow: 0 22px 60px rgb(47 107 255 / 9%);
  cursor: pointer;
  transition: all 0.2s;
  padding: 48px 24px;
}

.upload-card:hover {
  border-color: #2563eb;
  border-style: dashed;
  box-shadow: 0 8px 32px rgba(37, 99, 235, 0.1);
}

.upload-card:active {
  border-color: #2563eb;
  border-style: dashed;
  background: #eff6ff;
}

.hidden-input {
  display: none;
}

.upload-card-icon {
  display: grid;
  width: 80px;
  height: 80px;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #ffffff;
  font-size: 36px;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.3);
  border: none;
  overflow: hidden;
}

.upload-card-icon :deep(.el-icon) {
  border: none;
  background: transparent;
  color: #ffffff;
}

.upload-card-title {
  margin: 0;
  color: #1e293b;
  font-size: 17px;
  font-weight: 600;
}

.upload-card-hint {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
}

/* ====== 已上传状态 ====== */
.uploaded-state {
  width: min(760px, 100%);
}

.file-info-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgb(215 226 242 / 80%);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 4px 12px rgba(37, 99, 235, 0.04);
  margin-bottom: 16px;
}

.file-icon-wrap {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 24px;
  flex-shrink: 0;
}

.file-meta {
  flex: 1;
  min-width: 0;
}

.file-name {
  display: block;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.remove-btn {
  flex-shrink: 0;
}

.file-preview-wrap {
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid rgb(215 226 242 / 80%);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 4px 12px rgba(37, 99, 235, 0.04);
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 420px;
  border: none;
  display: block;
}

/* ====== 底部操作栏 ====== */
.upload-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: flex;
  justify-content: center;
  border-top: 1px solid rgb(194 205 225 / 65%);
  background: rgb(255 255 255 / 90%);
  padding: 18px 24px;
  backdrop-filter: blur(14px);
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .upload-topbar {
    padding: 0 18px;
  }

  .upload-content {
    padding: 32px 16px 120px;
  }

  .upload-card {
    min-height: 280px;
  }

  .preview-iframe {
    height: 300px;
  }
}
</style>
