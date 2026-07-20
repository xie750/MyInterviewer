<script setup lang="ts">
import { Edit, Plus, Refresh, SwitchButton, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchAdminUsersApi, updateAdminUserStatusApi } from '@/api/adminUsers'
import { fetchAdminInterviewsApi } from '@/api/interviews'
import {
  createAdminPositionApi,
  fetchAdminPositionsApi,
  updateAdminPositionApi,
  updateAdminPositionEnabledApi,
} from '@/api/positions'
import type { AdminInterview, AdminUser, Position, PositionRequest } from '@/types'

const router = useRouter()
const loading = ref(false)
const usersLoading = ref(false)
const interviewsLoading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const editingPosition = ref<Position | null>(null)
const positions = ref<Position[]>([])
const users = ref<AdminUser[]>([])
const interviews = ref<AdminInterview[]>([])

const form = reactive<PositionRequest>({
  name: '',
  description: '',
  techStack: '',
  difficulty: '',
  promptTemplate: '',
  enabled: true,
  sortOrder: 0,
})

const drawerTitle = computed(() => (editingPosition.value ? '编辑岗位' : '新增岗位'))

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

async function loadAll() {
  await Promise.all([loadPositions(), loadUsers(), loadInterviews()])
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.techStack = ''
  form.difficulty = ''
  form.promptTemplate = ''
  form.enabled = true
  form.sortOrder = 0
}

function openCreateDrawer() {
  editingPosition.value = null
  resetForm()
  drawerVisible.value = true
}

function openEditDrawer(position: Position) {
  editingPosition.value = position
  form.name = position.name
  form.description = position.description ?? ''
  form.techStack = position.techStack ?? ''
  form.difficulty = position.difficulty ?? ''
  form.promptTemplate = position.promptTemplate ?? ''
  form.enabled = position.enabled
  form.sortOrder = position.sortOrder
  drawerVisible.value = true
}

async function savePosition() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写岗位名称')
    return
  }

  saving.value = true
  try {
    if (editingPosition.value) {
      await updateAdminPositionApi(editingPosition.value.id, form)
      ElMessage.success('岗位已更新')
    } else {
      await createAdminPositionApi(form)
      ElMessage.success('岗位已新增')
    }
    drawerVisible.value = false
    await loadPositions()
  } catch {
    ElMessage.error('保存岗位失败')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(position: Position) {
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
    await updateAdminUserStatusApi(user.id, user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED')
    ElMessage.success(user.status === 'ENABLED' ? '用户已禁用' : '用户已启用')
    await loadUsers()
  } catch {
    ElMessage.error('用户状态更新失败')
  }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(loadAll)
</script>

<template>
  <main class="workspace-page">
    <section class="workspace-header">
      <div>
        <p class="eyebrow">Admin</p>
        <h1>管理员后台</h1>
        <p class="summary">管理岗位、用户状态，并查看全站文字面试记录。</p>
      </div>
      <div class="action-row compact">
        <el-button :icon="Refresh" :loading="loading || usersLoading || interviewsLoading" @click="loadAll">
          刷新全部
        </el-button>
        <el-button :icon="SwitchButton" plain @click="router.push('/home')">返回首页</el-button>
      </div>
    </section>

    <section class="admin-section">
      <el-tabs>
        <el-tab-pane label="岗位管理">
          <div class="section-toolbar">
            <div>
              <h2>岗位管理</h2>
              <p>维护用户创建面试前可选择的岗位基础数据。</p>
            </div>
            <el-button :icon="Plus" type="primary" @click="openCreateDrawer">新增岗位</el-button>
          </div>

          <el-table v-loading="loading" :data="positions" border class="admin-table">
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="name" label="岗位名称" min-width="160" />
            <el-table-column prop="techStack" label="技术栈" min-width="180" show-overflow-tooltip />
            <el-table-column prop="difficulty" label="难度" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">
                  {{ row.enabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button :icon="Edit" link type="primary" @click="openEditDrawer(row)">编辑</el-button>
                <el-button link type="primary" @click="toggleEnabled(row)">
                  {{ row.enabled ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="用户管理">
          <div class="section-toolbar">
            <div>
              <h2>用户管理</h2>
              <p>查看账号状态，并启用或禁用普通用户。</p>
            </div>
            <el-button :icon="Refresh" :loading="usersLoading" @click="loadUsers">刷新用户</el-button>
          </div>

          <el-table v-loading="usersLoading" :data="users" border class="admin-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="账号" min-width="130" />
            <el-table-column prop="displayName" label="显示名" min-width="130" />
            <el-table-column prop="role" label="角色" width="110" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
                  {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="180">
              <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="row.role === 'ADMIN'"
                  @click="toggleUserStatus(row)"
                >
                  {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="面试记录">
          <div class="section-toolbar">
            <div>
              <h2>全站面试记录</h2>
              <p>管理员可以查看所有用户的面试状态、问题数和报告分数。</p>
            </div>
            <el-button :icon="Refresh" :loading="interviewsLoading" @click="loadInterviews">刷新记录</el-button>
          </div>

          <el-table v-loading="interviewsLoading" :data="interviews" border class="admin-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="displayName" label="用户" min-width="130" />
            <el-table-column prop="positionName" label="岗位" min-width="160" />
            <el-table-column prop="styleName" label="风格" min-width="130" />
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
                <el-button :icon="View" link type="primary" @click="router.push(`/admin/interviews/${row.id}`)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="460px">
      <el-form label-position="top">
        <el-form-item label="岗位名称" required>
          <el-input v-model="form.name" maxlength="80" />
        </el-form-item>
        <el-form-item label="岗位说明">
          <el-input v-model="form.description" maxlength="500" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="form.techStack" maxlength="200" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input v-model="form.difficulty" maxlength="40" />
        </el-form-item>
        <el-form-item label="提示词模板">
          <el-input v-model="form.promptTemplate" maxlength="1000" type="textarea" :rows="4" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          </el-form-item>
          <el-form-item label="启用状态">
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePosition">保存</el-button>
      </template>
    </el-drawer>
  </main>
</template>
