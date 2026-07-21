<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="flow-page">
    <section class="empty-panel">
      <p class="eyebrow">403</p>
      <h1>无权限访问</h1>
      <p class="summary">当前账号不是管理员，不能进入管理员区域。</p>
      <el-button type="primary" @click="router.push('/interviews')">返回面试记录</el-button>
    </section>
  </main>
</template>
