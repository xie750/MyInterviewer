<script setup lang="ts">
import { ArrowLeft, Calendar, DocumentAdd, Refresh, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchInterviewsApi } from '@/api/interviews'
import type { InterviewSummary } from '@/types'

const router = useRouter()
const loading = ref(false)
const interviews = ref<InterviewSummary[]>([])
const completedCount = computed(() => interviews.value.filter((item) => item.status === 'COMPLETED').length)

async function loadInterviews() {
  loading.value = true
  try {
    interviews.value = await fetchInterviewsApi()
  } catch {
    ElMessage.error('历史记录加载失败')
  } finally {
    loading.value = false
  }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleDateString() : '-'
}

function statusText(status: InterviewSummary['status']) {
  return status === 'COMPLETED' ? '已完成' : '进行中'
}

function statusTagType(status: InterviewSummary['status']) {
  return status === 'COMPLETED' ? 'success' : 'warning'
}

onMounted(loadInterviews)
</script>

<template>
  <main class="flow-page">
    <header class="flow-topbar">
      <button class="back-button" type="button" @click="router.push('/home')">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </button>
      <el-button type="primary" @click="router.push('/positions')">开始新面试</el-button>
    </header>

    <section class="history-shell" v-loading="loading">
      <div class="flow-title history-title">
        <h1>面试记录</h1>
        <p>共 {{ interviews.length }} 场面试，{{ completedCount }} 场已生成评估报告。</p>
      </div>

      <div v-if="interviews.length" class="history-list">
        <article v-for="item in interviews" :key="item.id" class="history-item">
          <div class="history-main">
            <span class="record-index">#{{ item.id }}</span>
            <div>
              <h2>{{ item.positionName }}</h2>
              <p>
                <el-icon><Calendar /></el-icon>
                {{ formatTime(item.updatedAt) }}
                <span>{{ item.styleName }}</span>
                <span>{{ item.resumeUsed ? '已使用简历' : '未使用简历' }}</span>
              </p>
            </div>
          </div>

          <div class="history-score">
            <strong>{{ item.totalScore ?? '--' }}</strong>
            <span>综合评分</span>
          </div>

          <el-tag :type="statusTagType(item.status)" round>{{ statusText(item.status) }}</el-tag>

          <el-button :icon="View" type="primary" plain @click="router.push(`/interviews/${item.id}`)">
            查看详情
          </el-button>
        </article>
      </div>

      <section v-else-if="!loading" class="empty-panel">
        <div class="empty-illustration" aria-hidden="true">
          <span class="empty-orbit one" />
          <span class="empty-orbit two" />
          <span class="empty-card small">AI 面试官</span>
          <span class="empty-card main">面试评估报告</span>
          <span class="empty-badge">AI</span>
        </div>
        <h1>暂无面试记录</h1>
        <p class="summary">完成第一场模拟面试后，这里会展示面试记录、综合评分和报告入口。</p>
        <div class="action-row">
          <el-button :icon="DocumentAdd" type="primary" @click="router.push('/positions')">
            开始第一场面试
          </el-button>
          <el-button :icon="Refresh" plain @click="loadInterviews">刷新</el-button>
        </div>
      </section>
    </section>
  </main>
</template>
