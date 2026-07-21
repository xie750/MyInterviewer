<script setup lang="ts">
import { useRouter } from 'vue-router'

import '@/styles/page-transition.css'
import { ref } from 'vue'

const router = useRouter()

const transitionName = ref('fade')
const transitioning = ref(false)

router.afterEach((to) => {
  if (transitioning.value) return

  const slidePaths = ['/', '/interviews', '/interviews/resume', '/interviews/select']
  const isSlide = slidePaths.includes(to.path) || to.path.startsWith('/interviews/')
  transitionName.value = isSlide ? 'fade-slide' : 'fade'
})

function onBeforeEnter() {
  transitioning.value = true
  document.body.classList.add('transitioning')
}

function onAfterLeave() {
  transitioning.value = false
  document.body.classList.remove('transitioning')
}
</script>

<template>
  <div class="transition-wrapper">
    <RouterView v-slot="{ Component, route }">
      <Transition
        :name="transitionName"
        mode="in-out"
        @before-enter="onBeforeEnter"
        @after-leave="onAfterLeave"
      >
        <Suspense>
          <Component :is="Component" :key="route.fullPath" />
          <template #fallback>
            <div class="app-loading-fallback" />
          </template>
        </Suspense>
      </Transition>
    </RouterView>
  </div>
</template>

<style>
.transition-wrapper {
  position: relative;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
}

body.transitioning {
  overflow: hidden;
}

.app-loading-fallback {
  min-height: 100vh;
}
</style>
