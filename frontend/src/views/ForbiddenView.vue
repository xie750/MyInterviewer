<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <main class="home-page">
    <section class="home-panel">
      <p class="eyebrow">403</p>
      <h1>无权限访问</h1>
      <p class="summary">当前账号不是管理员，不能进入管理员区域。</p>
      <div class="action-row">
        <el-button type="primary" plain @click="router.push('/home')">返回首页</el-button>
      </div>
    </section>
  </main>
</template>

