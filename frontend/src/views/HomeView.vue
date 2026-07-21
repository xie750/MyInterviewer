<script setup lang="ts">
import { ArrowRight, Clock, DocumentChecked, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

async function logout() {
  authStore.logout()
  await router.replace('/login')
}
</script>

<template>
  <main class="flow-page">
    <header class="flow-topbar">
      <div class="brand-mark">AI</div>
      <div class="flow-topbar-actions">
        <el-button v-if="authStore.user?.role === 'ADMIN'" plain @click="router.push('/admin')">
          管理员后台
        </el-button>
        <el-button :icon="SwitchButton" plain @click="logout">退出登录</el-button>
      </div>
    </header>

    <section class="intro-shell">
      <div class="intro-copy">
        <p class="eyebrow">AI 模拟面试</p>
        <h1>完成一次接近真实场景的岗位面试练习</h1>
        <p class="summary">
          {{ authStore.user?.displayName }}。系统会根据岗位、面试官风格和可选简历上下文生成多轮追问，
          面试结束后给出评分、复盘建议和历史记录。
          <span v-if="!flags.resume">（简历功能已关闭，可直接进入面试）</span>
          <span v-if="!flags.speech">（语音功能已关闭，使用纯文字面试）</span>
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" :icon="ArrowRight" @click="router.push('/positions')">
            开始面试
          </el-button>
          <el-button size="large" plain @click="router.push('/interviews')">查看面试记录</el-button>
        </div>
      </div>

      <div class="intro-preview glass-card glass-card-md" aria-label="面试流程预览">
        <div class="preview-toolbar">
          <span />
          <span />
          <span />
        </div>
        <div v-if="flags.resume" class="preview-card active">
          <el-icon><DocumentChecked /></el-icon>
          <div>
            <strong>上传简历</strong>
            <p>解析技能、项目和经历要点</p>
          </div>
        </div>
        <div :class="['preview-card', { active: !flags.resume }]">
          <el-icon><UserFilled /></el-icon>
          <div>
            <strong>选择面试官</strong>
            <p>轻松、技术、HR 等风格可选</p>
          </div>
        </div>
        <div class="preview-card">
          <el-icon><Clock /></el-icon>
          <div>
            <strong>生成评估报告</strong>
            <p>分数、亮点、建议和对话复盘</p>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
