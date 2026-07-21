<script setup lang="ts">
import {
  Edit,
  Plus,
  Refresh,
  Search,
  SwitchButton,
  View,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  fetchAdminPostureEventsApi,
  fetchAdminPostureThresholdsApi,
  updateAdminPostureThresholdApi,
} from '@/api/adminPosture'
import { fetchAdminUsersApi, updateAdminUserStatusApi } from '@/api/adminUsers'
import { fetchAdminInterviewsApi } from '@/api/interviews'
import {
  createAdminPositionApi,
  fetchAdminPositionsApi,
  updateAdminPositionApi,
  updateAdminPositionEnabledApi,
} from '@/api/positions'
import {
  createAdminVirtualHumanApi,
  fetchAdminVirtualHumansApi,
  updateAdminVirtualHumanApi,
} from '@/api/virtualHumans'
import { resolveVirtualHuman } from '@/services/virtualHuman'
import type {
  AdminInterview,
  AdminPostureEvent,
  AdminUser,
  Position,
  PositionRequest,
  PostureEventType,
  PostureSeverity,
  PostureThreshold,
  PostureThresholdRequest,
  UserStatus,
  VirtualHumanAsset,
  VirtualHumanAssetRequest,
} from '@/types'

const router = useRouter()
const activeTab = ref('positions')
const loading = ref(false)
const usersLoading = ref(false)
const interviewsLoading = ref(false)
const postureLoading = ref(false)
const thresholdsLoading = ref(false)
const assetsLoading = ref(false)
const saving = ref(false)

const positionDrawerVisible = ref(false)
const assetDrawerVisible = ref(false)
const thresholdDrawerVisible = ref(false)
const editingPosition = ref<Position | null>(null)
const editingAsset = ref<VirtualHumanAsset | null>(null)
const editingThreshold = ref<PostureThreshold | null>(null)

const positions = ref<Position[]>([])
const users = ref<AdminUser[]>([])
const interviews = ref<AdminInterview[]>([])
const postureEvents = ref<AdminPostureEvent[]>([])
const postureTotal = ref(0)
const thresholds = ref<PostureThreshold[]>([])
const assets = ref<VirtualHumanAsset[]>([])

const positionFilters = reactive({ keyword: '', status: '' })
const userFilters = reactive({ keyword: '', status: '' as UserStatus | '' })
const interviewFilters = reactive({ keyword: '', status: '' })
const assetFilters = reactive({ keyword: '', status: '' })
const postureFilters = reactive({
  keyword: '',
  sessionId: null as number | null,
  eventType: '' as PostureEventType | '',
  severity: '' as PostureSeverity | '',
})

const pageState = reactive({
  positions: 1,
  users: 1,
  interviews: 1,
  posture: 1,
  assets: 1,
})
const pageSize = 8

const positionForm = reactive<PositionRequest>({
  name: '',
  description: '',
  techStack: '',
  difficulty: '',
  promptTemplate: '',
  enabled: true,
  sortOrder: 0,
})

const assetForm = reactive<VirtualHumanAssetRequest>({
  assetKey: '',
  name: '',
  description: '',
  imageUrl: '',
  accentColor: '#2563eb',
  badge: '',
  enabled: true,
  sortOrder: 0,
})

const thresholdForm = reactive<PostureThresholdRequest>({
  eventType: 'LOW_LIGHT',
  displayName: '',
  description: '',
  warningThreshold: null,
  criticalThreshold: null,
  enabled: true,
  sortOrder: 0,
})

const postureTypeOptions: PostureEventType[] = [
  'FACE_MISSING',
  'FACE_OFF_CENTER',
  'TOO_CLOSE',
  'TOO_FAR',
  'TOO_STILL',
  'LOW_LIGHT',
  'CAMERA_UNAVAILABLE',
]
const severityOptions: PostureSeverity[] = ['INFO', 'WARNING', 'CRITICAL']

const positionDrawerTitle = computed(() => (editingPosition.value ? '编辑岗位' : '新增岗位'))
const assetDrawerTitle = computed(() => (editingAsset.value ? '编辑虚拟人素材' : '新增虚拟人素材'))

const filteredPositions = computed(() => {
  return positions.value.filter((item) => {
    const keywordMatched = matchesKeyword(positionFilters.keyword, [
      item.name,
      item.description,
      item.techStack,
      item.difficulty,
    ])
    const statusMatched = !positionFilters.status
      || (positionFilters.status === 'enabled' ? item.enabled : !item.enabled)
    return keywordMatched && statusMatched
  })
})
const filteredUsers = computed(() => users.value.filter((item) => {
  const keywordMatched = matchesKeyword(userFilters.keyword, [item.username, item.displayName, item.role])
  const statusMatched = !userFilters.status || item.status === userFilters.status
  return keywordMatched && statusMatched
}))
const filteredInterviews = computed(() => interviews.value.filter((item) => {
  const keywordMatched = matchesKeyword(interviewFilters.keyword, [
    item.username,
    item.displayName,
    item.positionName,
    item.styleName,
  ])
  const statusMatched = !interviewFilters.status || item.status === interviewFilters.status
  return keywordMatched && statusMatched
}))
const filteredAssets = computed(() => assets.value.filter((item) => {
  const keywordMatched = matchesKeyword(assetFilters.keyword, [
    item.assetKey,
    item.name,
    item.description,
    item.badge,
  ])
  const statusMatched = !assetFilters.status || (assetFilters.status === 'enabled' ? item.enabled : !item.enabled)
  return keywordMatched && statusMatched
}))

const visiblePositions = computed(() => paginate(filteredPositions.value, pageState.positions))
const visibleUsers = computed(() => paginate(filteredUsers.value, pageState.users))
const visibleInterviews = computed(() => paginate(filteredInterviews.value, pageState.interviews))
const visibleAssets = computed(() => paginate(filteredAssets.value, pageState.assets))

async function loadPositions() {
  loading.value = true
  try {
    positions.value = await fetchAdminPositionsApi()
  } catch {
    ElMessage.error('岗位列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  usersLoading.value = true
  try {
    users.value = await fetchAdminUsersApi()
  } catch {
    ElMessage.error('用户列表加载失败')
  } finally {
    usersLoading.value = false
  }
}

async function loadInterviews() {
  interviewsLoading.value = true
  try {
    interviews.value = await fetchAdminInterviewsApi()
  } catch {
    ElMessage.error('面试记录加载失败')
  } finally {
    interviewsLoading.value = false
  }
}

async function loadPostureEvents() {
  postureLoading.value = true
  try {
    const result = await fetchAdminPostureEventsApi({
      keyword: postureFilters.keyword,
      sessionId: postureFilters.sessionId,
      eventType: postureFilters.eventType,
      severity: postureFilters.severity,
      page: pageState.posture,
      pageSize,
    })
    postureEvents.value = result.items
    postureTotal.value = result.total
  } catch {
    ElMessage.error('姿态记录加载失败')
  } finally {
    postureLoading.value = false
  }
}

async function loadThresholds() {
  thresholdsLoading.value = true
  try {
    thresholds.value = await fetchAdminPostureThresholdsApi()
  } catch {
    ElMessage.error('姿态阈值加载失败')
  } finally {
    thresholdsLoading.value = false
  }
}

async function loadAssets() {
  assetsLoading.value = true
  try {
    assets.value = await fetchAdminVirtualHumansApi({
      keyword: assetFilters.keyword,
      enabled: assetFilters.status ? assetFilters.status === 'enabled' : null,
    })
  } catch {
    ElMessage.error('虚拟人素材加载失败')
  } finally {
    assetsLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadPositions(), loadUsers(), loadInterviews(), loadPostureEvents(), loadThresholds(), loadAssets()])
}

function resetPositionForm() {
  positionForm.name = ''
  positionForm.description = ''
  positionForm.techStack = ''
  positionForm.difficulty = ''
  positionForm.promptTemplate = ''
  positionForm.enabled = true
  positionForm.sortOrder = 0
}

function openCreatePositionDrawer() {
  editingPosition.value = null
  resetPositionForm()
  positionDrawerVisible.value = true
}

function openEditPositionDrawer(position: Position) {
  editingPosition.value = position
  positionForm.name = position.name
  positionForm.description = position.description ?? ''
  positionForm.techStack = position.techStack ?? ''
  positionForm.difficulty = position.difficulty ?? ''
  positionForm.promptTemplate = position.promptTemplate ?? ''
  positionForm.enabled = position.enabled
  positionForm.sortOrder = position.sortOrder
  positionDrawerVisible.value = true
}

async function savePosition() {
  if (!positionForm.name.trim()) {
    ElMessage.warning('请填写岗位名称')
    return
  }

  saving.value = true
  try {
    if (editingPosition.value) {
      await updateAdminPositionApi(editingPosition.value.id, positionForm)
      ElMessage.success('岗位已更新')
    } else {
      await createAdminPositionApi(positionForm)
      ElMessage.success('岗位已新增')
    }
    positionDrawerVisible.value = false
    await loadPositions()
  } catch {
    ElMessage.error('保存岗位失败')
  } finally {
    saving.value = false
  }
}

async function togglePosition(position: Position) {
  try {
    await updateAdminPositionEnabledApi(position.id, !position.enabled)
    ElMessage.success(position.enabled ? '岗位已停用' : '岗位已启用')
    await loadPositions()
  } catch {
    ElMessage.error('岗位状态更新失败')
  }
}

async function toggleUserStatus(user: AdminUser) {
  if (user.role === 'ADMIN') {
    ElMessage.warning('基础用户管理不启停管理员账号')
    return
  }

  try {
    const nextStatus: UserStatus = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    await updateAdminUserStatusApi(user.id, nextStatus)
    ElMessage.success(nextStatus === 'ENABLED' ? '用户已启用' : '用户已禁用')
    await loadUsers()
  } catch {
    ElMessage.error('用户状态更新失败')
  }
}

function resetAssetForm() {
  assetForm.assetKey = ''
  assetForm.name = ''
  assetForm.description = ''
  assetForm.imageUrl = ''
  assetForm.accentColor = '#2563eb'
  assetForm.badge = ''
  assetForm.enabled = true
  assetForm.sortOrder = 0
}

function openCreateAssetDrawer() {
  editingAsset.value = null
  resetAssetForm()
  assetDrawerVisible.value = true
}

function openEditAssetDrawer(asset: VirtualHumanAsset) {
  editingAsset.value = asset
  assetForm.assetKey = asset.assetKey
  assetForm.name = asset.name
  assetForm.description = asset.description ?? ''
  assetForm.imageUrl = asset.imageUrl ?? ''
  assetForm.accentColor = asset.accentColor ?? '#2563eb'
  assetForm.badge = asset.badge ?? ''
  assetForm.enabled = asset.enabled
  assetForm.sortOrder = asset.sortOrder
  assetDrawerVisible.value = true
}

async function saveAsset() {
  if (!assetForm.assetKey.trim() || !assetForm.name.trim()) {
    ElMessage.warning('请填写资源 key 和名称')
    return
  }
  saving.value = true
  try {
    if (editingAsset.value) {
      await updateAdminVirtualHumanApi(editingAsset.value.id, assetForm)
      ElMessage.success('虚拟人素材已更新')
    } else {
      await createAdminVirtualHumanApi(assetForm)
      ElMessage.success('虚拟人素材已新增')
    }
    assetDrawerVisible.value = false
    await loadAssets()
  } catch {
    ElMessage.error('保存虚拟人素材失败')
  } finally {
    saving.value = false
  }
}

function openThresholdDrawer(config: PostureThreshold) {
  editingThreshold.value = config
  thresholdForm.eventType = config.eventType
  thresholdForm.displayName = config.displayName
  thresholdForm.description = config.description ?? ''
  thresholdForm.warningThreshold = config.warningThreshold
  thresholdForm.criticalThreshold = config.criticalThreshold
  thresholdForm.enabled = config.enabled
  thresholdForm.sortOrder = config.sortOrder
  thresholdDrawerVisible.value = true
}

async function saveThreshold() {
  if (!editingThreshold.value) {
    return
  }
  saving.value = true
  try {
    await updateAdminPostureThresholdApi(editingThreshold.value.id, thresholdForm)
    ElMessage.success('姿态阈值已更新')
    thresholdDrawerVisible.value = false
    await loadThresholds()
  } catch {
    ElMessage.error('保存姿态阈值失败')
  } finally {
    saving.value = false
  }
}

function refreshCurrentTab() {
  const loaders: Record<string, () => Promise<void>> = {
    positions: loadPositions,
    users: loadUsers,
    interviews: loadInterviews,
    posture: loadPostureEvents,
    thresholds: loadThresholds,
    assets: loadAssets,
  }
  void loaders[activeTab.value]?.()
}

function applyPostureSearch() {
  pageState.posture = 1
  void loadPostureEvents()
}

function applyAssetSearch() {
  pageState.assets = 1
  void loadAssets()
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

function matchesKeyword(keyword: string, values: Array<string | null | undefined>) {
  if (!keyword.trim()) {
    return true
  }
  const normalized = keyword.trim().toLowerCase()
  return values.some((value) => value?.toLowerCase().includes(normalized))
}

function paginate<T>(items: T[], page: number) {
  const start = (page - 1) * pageSize
  return items.slice(start, start + pageSize)
}

function postureTypeLabel(type: PostureEventType) {
  const labels: Record<PostureEventType, string> = {
    FACE_MISSING: '未检测到人脸',
    FACE_OFF_CENTER: '人脸偏离中央',
    TOO_CLOSE: '距离过近',
    TOO_FAR: '距离过远',
    TOO_STILL: '画面长时间静止',
    LOW_LIGHT: '光线偏暗',
    CAMERA_UNAVAILABLE: '摄像头不可用',
  }
  return labels[type]
}

function severityTagType(severity: PostureSeverity) {
  if (severity === 'CRITICAL') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function assetPreview(asset: VirtualHumanAsset) {
  return resolveVirtualHuman({
    key: asset.assetKey,
    name: asset.name,
    description: asset.description ?? '',
    imageUrl: asset.imageUrl,
    accentColor: asset.accentColor,
    badge: asset.badge,
  })
}

function assetPreviewSrc(asset: VirtualHumanAsset) {
  return assetPreview(asset).src ?? ''
}

onMounted(loadAll)
</script>

<template>
  <main class="workspace-page">
    <section class="workspace-header">
      <div>
        <p class="eyebrow">Admin</p>
        <h1>管理员后台</h1>
        <p class="summary">统一管理岗位、用户、面试记录、姿态配置和虚拟人素材。</p>
      </div>
      <div class="action-row compact">
        <el-button :icon="Refresh" @click="refreshCurrentTab">刷新当前</el-button>
        <el-button :icon="Refresh" :loading="loading || usersLoading || interviewsLoading || postureLoading || thresholdsLoading || assetsLoading" @click="loadAll">
          刷新全部
        </el-button>
        <el-button :icon="SwitchButton" plain @click="router.push('/interviews')">返回面试记录</el-button>
      </div>
    </section>

    <section class="admin-section">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="岗位管理" name="positions">
          <div class="section-toolbar">
            <div>
              <h2>岗位管理</h2>
              <p>维护用户创建面试前可选择的岗位基础数据。</p>
            </div>
            <el-button :icon="Plus" type="primary" @click="openCreatePositionDrawer">新增岗位</el-button>
          </div>
          <div class="admin-filter-row">
            <el-input v-model="positionFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索岗位、技术栈、难度" @input="pageState.positions = 1" />
            <el-select v-model="positionFilters.status" clearable placeholder="状态" @change="pageState.positions = 1">
              <el-option label="启用" value="enabled" />
              <el-option label="停用" value="disabled" />
            </el-select>
          </div>
          <el-table v-loading="loading" :data="visiblePositions" border class="admin-table">
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="name" label="岗位名称" min-width="160" />
            <el-table-column prop="techStack" label="技术栈" min-width="180" show-overflow-tooltip />
            <el-table-column prop="difficulty" label="难度" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button :icon="Edit" link type="primary" @click="openEditPositionDrawer(row)">编辑</el-button>
                <el-button link type="primary" @click="togglePosition(row)">{{ row.enabled ? '停用' : '启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="pageState.positions" layout="prev, pager, next, total" :page-size="pageSize" :total="filteredPositions.length" />
        </el-tab-pane>

        <el-tab-pane label="用户管理" name="users">
          <div class="section-toolbar">
            <div>
              <h2>用户管理</h2>
              <p>查看账号状态，并启用或禁用普通用户。</p>
            </div>
            <el-button :icon="Refresh" :loading="usersLoading" @click="loadUsers">刷新用户</el-button>
          </div>
          <div class="admin-filter-row">
            <el-input v-model="userFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索账号、显示名、角色" @input="pageState.users = 1" />
            <el-select v-model="userFilters.status" clearable placeholder="状态" @change="pageState.users = 1">
              <el-option label="启用" value="ENABLED" />
              <el-option label="禁用" value="DISABLED" />
            </el-select>
          </div>
          <el-table v-loading="usersLoading" :data="visibleUsers" border class="admin-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="账号" min-width="130" />
            <el-table-column prop="displayName" label="显示名" min-width="130" />
            <el-table-column prop="role" label="角色" width="110" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="180">
              <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.role === 'ADMIN'" @click="toggleUserStatus(row)">
                  {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="pageState.users" layout="prev, pager, next, total" :page-size="pageSize" :total="filteredUsers.length" />
        </el-tab-pane>

        <el-tab-pane label="面试记录" name="interviews">
          <div class="section-toolbar">
            <div>
              <h2>全站面试记录</h2>
              <p>管理员可以查看所有用户的面试状态、问题数和报告分数。</p>
            </div>
            <el-button :icon="Refresh" :loading="interviewsLoading" @click="loadInterviews">刷新记录</el-button>
          </div>
          <div class="admin-filter-row">
            <el-input v-model="interviewFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索用户、岗位、风格" @input="pageState.interviews = 1" />
            <el-select v-model="interviewFilters.status" clearable placeholder="状态" @change="pageState.interviews = 1">
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
          </div>
          <el-table v-loading="interviewsLoading" :data="visibleInterviews" border class="admin-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="displayName" label="用户" min-width="120" />
            <el-table-column prop="positionName" label="岗位" min-width="150" />
            <el-table-column prop="styleName" label="风格" min-width="120" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.status === 'COMPLETED' ? '已完成' : '进行中' }}</el-tag>
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
                <el-button :icon="View" link type="primary" @click="router.push(`/admin/interviews/${row.id}`)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="pageState.interviews" layout="prev, pager, next, total" :page-size="pageSize" :total="filteredInterviews.length" />
        </el-tab-pane>

        <el-tab-pane label="姿态记录" name="posture">
          <div class="section-toolbar">
            <div>
              <h2>姿态记录后台</h2>
              <p>只查看结构化检测结果，不接收或保存原始视频流。</p>
            </div>
            <el-button :icon="Refresh" :loading="postureLoading" @click="loadPostureEvents">刷新记录</el-button>
          </div>
          <div class="admin-filter-row">
            <el-input v-model="postureFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索用户、详情或面试 ID" @keyup.enter="applyPostureSearch" />
            <el-input-number v-model="postureFilters.sessionId" :min="1" :controls="false" placeholder="面试 ID" />
            <el-select v-model="postureFilters.eventType" clearable placeholder="事件类型">
              <el-option v-for="type in postureTypeOptions" :key="type" :label="postureTypeLabel(type)" :value="type" />
            </el-select>
            <el-select v-model="postureFilters.severity" clearable placeholder="级别">
              <el-option v-for="severity in severityOptions" :key="severity" :label="severity" :value="severity" />
            </el-select>
            <el-button :icon="Search" type="primary" @click="applyPostureSearch">查询</el-button>
          </div>
          <el-table v-loading="postureLoading" :data="postureEvents" border class="admin-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="sessionId" label="面试" width="90" />
            <el-table-column prop="displayName" label="用户" min-width="110" />
            <el-table-column label="类型" min-width="140">
              <template #default="{ row }">{{ postureTypeLabel(row.eventType) }}</template>
            </el-table-column>
            <el-table-column label="级别" width="110">
              <template #default="{ row }"><el-tag :type="severityTagType(row.severity)">{{ row.severity }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="score" label="分值" width="90" />
            <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
            <el-table-column label="发生时间" min-width="180">
              <template #default="{ row }">{{ formatTime(row.occurredAt) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="pageState.posture"
            layout="prev, pager, next, total"
            :page-size="pageSize"
            :total="postureTotal"
            @current-change="loadPostureEvents"
          />
        </el-tab-pane>

        <el-tab-pane label="阈值配置" name="thresholds">
          <div class="section-toolbar">
            <div>
              <h2>姿态阈值配置</h2>
              <p>阈值配置面向浏览器本地检测，读取失败时前端使用默认配置。</p>
            </div>
            <el-button :icon="Refresh" :loading="thresholdsLoading" @click="loadThresholds">刷新配置</el-button>
          </div>
          <el-table v-loading="thresholdsLoading" :data="thresholds" border class="admin-table">
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="事件" min-width="140">
              <template #default="{ row }">{{ postureTypeLabel(row.eventType) }}</template>
            </el-table-column>
            <el-table-column prop="displayName" label="显示名" min-width="140" />
            <el-table-column prop="warningThreshold" label="预警阈值" width="110" />
            <el-table-column prop="criticalThreshold" label="严重阈值" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }"><el-button :icon="Edit" link type="primary" @click="openThresholdDrawer(row)">编辑</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="虚拟人素材" name="assets">
          <div class="section-toolbar">
            <div>
              <h2>虚拟人素材管理</h2>
              <p>维护素材 key、展示文案、图片地址和预览信息。</p>
            </div>
            <el-button :icon="Plus" type="primary" @click="openCreateAssetDrawer">新增素材</el-button>
          </div>
          <div class="admin-filter-row">
            <el-input v-model="assetFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索 key、名称、标签" @keyup.enter="applyAssetSearch" />
            <el-select v-model="assetFilters.status" clearable placeholder="状态" @change="applyAssetSearch">
              <el-option label="启用" value="enabled" />
              <el-option label="停用" value="disabled" />
            </el-select>
            <el-button :icon="Search" type="primary" @click="applyAssetSearch">查询</el-button>
          </div>
          <el-table v-loading="assetsLoading" :data="visibleAssets" border class="admin-table">
            <el-table-column label="预览" width="110">
              <template #default="{ row }">
                <div class="asset-preview" :style="{ '--avatar-accent': assetPreview(row).accent }">
                  <img v-if="assetPreviewSrc(row)" :src="assetPreviewSrc(row)" :alt="row.name">
                  <span v-else>{{ row.name.slice(0, 2) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="assetKey" label="资源 key" min-width="150" />
            <el-table-column prop="name" label="名称" min-width="130" />
            <el-table-column prop="badge" label="标签" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }"><el-button :icon="Edit" link type="primary" @click="openEditAssetDrawer(row)">编辑</el-button></template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="pageState.assets" layout="prev, pager, next, total" :page-size="pageSize" :total="filteredAssets.length" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-drawer v-model="positionDrawerVisible" :title="positionDrawerTitle" size="460px">
      <el-form label-position="top">
        <el-form-item label="岗位名称" required><el-input v-model="positionForm.name" maxlength="80" /></el-form-item>
        <el-form-item label="岗位说明"><el-input v-model="positionForm.description" maxlength="500" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="技术栈"><el-input v-model="positionForm.techStack" maxlength="200" /></el-form-item>
        <el-form-item label="难度"><el-input v-model="positionForm.difficulty" maxlength="40" /></el-form-item>
        <el-form-item label="提示词模板"><el-input v-model="positionForm.promptTemplate" maxlength="1000" type="textarea" :rows="4" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="排序"><el-input-number v-model="positionForm.sortOrder" :min="0" :max="9999" /></el-form-item>
          <el-form-item label="启用状态"><el-switch v-model="positionForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="positionDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePosition">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="assetDrawerVisible" :title="assetDrawerTitle" size="480px">
      <el-form label-position="top">
        <el-form-item label="资源 key" required><el-input v-model="assetForm.assetKey" maxlength="80" placeholder="如 tech-architect" /></el-form-item>
        <el-form-item label="名称" required><el-input v-model="assetForm.name" maxlength="80" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="assetForm.description" maxlength="500" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="assetForm.imageUrl" maxlength="500" placeholder="可为空，使用前端内置 key 映射" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="强调色"><el-input v-model="assetForm.accentColor" maxlength="20" /></el-form-item>
          <el-form-item label="标签"><el-input v-model="assetForm.badge" maxlength="40" /></el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="排序"><el-input-number v-model="assetForm.sortOrder" :min="0" :max="9999" /></el-form-item>
          <el-form-item label="启用状态"><el-switch v-model="assetForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="assetDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAsset">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="thresholdDrawerVisible" title="编辑姿态阈值" size="460px">
      <el-form label-position="top">
        <el-form-item label="事件类型" required>
          <el-select v-model="thresholdForm.eventType" disabled>
            <el-option v-for="type in postureTypeOptions" :key="type" :label="postureTypeLabel(type)" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="显示名" required><el-input v-model="thresholdForm.displayName" maxlength="80" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="thresholdForm.description" maxlength="500" type="textarea" :rows="3" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="预警阈值"><el-input-number v-model="thresholdForm.warningThreshold" :min="0" :max="1000" /></el-form-item>
          <el-form-item label="严重阈值"><el-input-number v-model="thresholdForm.criticalThreshold" :min="0" :max="1000" /></el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="排序"><el-input-number v-model="thresholdForm.sortOrder" :min="0" :max="9999" /></el-form-item>
          <el-form-item label="启用状态"><el-switch v-model="thresholdForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="thresholdDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveThreshold">保存</el-button>
      </template>
    </el-drawer>
  </main>
</template>
