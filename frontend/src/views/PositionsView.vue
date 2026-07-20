<script setup lang="ts">
import { Refresh, Select, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createInterviewApi } from '@/api/interviews'
import { fetchInterviewerStylesApi } from '@/api/interviewerStyles'
import { fetchPositionsApi } from '@/api/positions'
import type { InterviewerStyle, Position } from '@/types'

const router = useRouter()
const loading = ref(false)
const startingPositionId = ref<number | null>(null)
const positions = ref<Position[]>([])
const styles = ref<InterviewerStyle[]>([])
const selectedStyleId = ref<number | null>(null)

async function loadOptions() {
  loading.value = true
  try {
    const [positionList, styleList] = await Promise.all([
      fetchPositionsApi(),
      fetchInterviewerStylesApi(),
    ])
    positions.value = positionList
    styles.value = styleList
    selectedStyleId.value = styleList[0]?.id ?? null
  } catch {
    ElMessage.error('面试配置加载失败')
  } finally {
    loading.value = false
  }
}

async function startInterview(position: Position) {
  if (!selectedStyleId.value) {
    ElMessage.warning('请选择面试官风格')
    return
  }

  startingPositionId.value = position.id
  try {
    const interview = await createInterviewApi({
      positionId: position.id,
      styleId: selectedStyleId.value,
    })
    await router.push(`/interviews/${interview.id}`)
  } catch {
    ElMessage.error('面试创建失败')
  } finally {
    startingPositionId.value = null
  }
}

onMounted(loadOptions)
</script>

<template>
  <main class="workspace-page">
    <section class="workspace-header">
      <div>
        <p class="eyebrow">Positions</p>
        <h1>选择面试岗位</h1>
        <p class="summary">当前先提供岗位选择基线，后续会接入面试官风格和正式面试会话。</p>
      </div>
      <div class="action-row compact">
        <el-button :icon="Refresh" :loading="loading" @click="loadOptions">刷新</el-button>
        <el-button plain @click="router.push('/interviews')">历史记录</el-button>
        <el-button :icon="SwitchButton" plain @click="router.push('/home')">返回首页</el-button>
      </div>
    </section>

    <section v-loading="loading" class="style-panel">
      <div class="section-toolbar">
        <div>
          <h2>面试官风格</h2>
          <p>选择本场面试的追问方式和评价侧重点。</p>
        </div>
      </div>
      <el-radio-group v-model="selectedStyleId" class="style-grid">
        <el-radio-button v-for="style in styles" :key="style.id" :label="style.id">
          <span class="style-name">{{ style.name }}</span>
          <span class="style-scenario">{{ style.scenario || '通用场景' }}</span>
        </el-radio-button>
      </el-radio-group>
    </section>

    <section v-loading="loading" class="position-grid">
      <article v-for="position in positions" :key="position.id" class="position-card">
        <div class="card-title-row">
          <h2>{{ position.name }}</h2>
          <el-tag>{{ position.difficulty || '未设置' }}</el-tag>
        </div>
        <p class="card-description">{{ position.description || '暂无岗位说明' }}</p>
        <p class="card-meta">{{ position.techStack || '暂未配置技术栈' }}</p>
        <el-button
          :icon="Select"
          type="primary"
          :loading="startingPositionId === position.id"
          @click="startInterview(position)"
        >
          开始面试
        </el-button>
      </article>
      <el-empty v-if="!loading && positions.length === 0" description="暂无可选岗位" />
    </section>
  </main>
</template>
