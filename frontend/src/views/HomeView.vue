<script setup lang="ts">
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
  <main class="home-page">
    <section class="home-panel">
      <p class="eyebrow">v0.1 MVP</p>
      <h1>AI 模拟面试</h1>
      <p class="summary">
        当前用户：{{ authStore.user?.displayName }}（{{ authStore.user?.role }}）。你可以开始一次文字模拟面试，或查看自己的历史报告。
      </p>
      <div class="action-row">
        <el-button type="primary" @click="router.push('/positions')">
          开始面试
        </el-button>
        <el-button type="primary" plain @click="router.push('/interviews')">
          历史记录
        </el-button>
        <el-button v-if="authStore.user?.role === 'ADMIN'" type="primary" @click="router.push('/admin')">
          管理员入口
        </el-button>
        <el-button type="primary" plain @click="logout">退出登录</el-button>
      </div>
    </section>
  </main>
</template>
