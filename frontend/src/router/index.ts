import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: {
        public: true,
      },
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/positions',
      name: 'positions',
      component: () => import('@/views/PositionsView.vue'),
    },
    {
      path: '/interviews',
      name: 'interviews',
      component: () => import('@/views/InterviewHistoryView.vue'),
    },
    {
      path: '/interviews/:id',
      name: 'interview-room',
      component: () => import('@/views/InterviewRoomView.vue'),
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/AdminView.vue'),
      meta: {
        requiresAdmin: true,
      },
    },
    {
      path: '/admin/interviews/:id',
      name: 'admin-interview-room',
      component: () => import('@/views/InterviewRoomView.vue'),
      meta: {
        requiresAdmin: true,
      },
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (!authStore.isAuthenticated) {
      return true
    }
    if (!authStore.user) {
      try {
        await authStore.loadCurrentUser()
      } catch {
        authStore.logout()
        return true
      }
    }
    return '/home'
  }

  if (!authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (!authStore.user) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.logout()
      return '/login'
    }
  }

  if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    return '/forbidden'
  }

  return true
})
