<script setup lang="ts">
import { Refresh, SwitchButton, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchInterviewsApi } from '@/api/interviews'
import type { InterviewSummary } from '@/types'

const router = useRouter()
const loading = ref(false)
const interviews = ref<InterviewSummary[]>([])

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
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(loadInterviews)
</script>

<template>
  <main class="workspace-page">
    <section class="workspace-header">
      <div>
        <p class="eyebrow">History</p>
        <h1>我的面试记录</h1>
        <p class="summary">查看已完成报告，或回到进行中的文字面试。</p>
      </div>
      <div class="action-row compact">
        <el-button type="primary" @click="router.push('/positions')">开始新面试</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadInterviews">刷新</el-button>
        <el-button :icon="SwitchButton" plain @click="router.push('/home')">返回首页</el-button>
      </div>
    </section>

    <section class="admin-section">
      <el-table v-loading="loading" :data="interviews" border class="admin-table">
        <el-table-column prop="positionName" label="岗位" min-width="160" />
        <el-table-column prop="styleName" label="风格" min-width="130" />
        <el-table-column label="简历" width="90">
          <template #default="{ row }">
            <el-tag :type="row.resumeUsed ? 'success' : 'info'">
              {{ row.resumeUsed ? '已用' : '未用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">
              {{ row.status === 'COMPLETED' ? '已完成' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionCount" label="问题数" width="90" />
        <el-table-column label="总分" width="90">
          <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button :icon="View" link type="primary" @click="router.push(`/interviews/${row.id}`)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && interviews.length === 0" description="暂无面试记录" />
    </section>
  </main>
</template>
