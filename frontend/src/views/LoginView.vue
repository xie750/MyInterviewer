<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const form = reactive({
  username: 'user',
  password: 'password123',
})

async function submitLogin() {
  loading.value = true
  try {
    await authStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/interviews'
    await router.replace(redirect)
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <p class="eyebrow">MyInterviewer</p>
      <h1>登录</h1>
      <p class="summary">使用演示账号进入 AI 模拟面试系统，继续完成岗位选择、面试和评估报告流程。</p>

      <el-form class="login-form" label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" autocomplete="current-password" show-password type="password" />
        </el-form-item>
        <el-button class="submit-button" type="primary" :loading="loading" @click="submitLogin">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>
