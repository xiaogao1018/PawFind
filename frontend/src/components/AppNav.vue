<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getCurrentUser } from '../api/user'
import { getUnreadCounts } from '../api/messages'
import PageBack from './PageBack.vue'

const route = useRoute()
const isAdmin = ref(route.meta.requiresAdmin === true)
const loggedIn = ref(!!localStorage.getItem('token'))
const unread = ref(0)
let timer: ReturnType<typeof setInterval> | undefined
const links = computed(() => isAdmin.value ? [
  { path: '/admin/lost', icon: '⌕', label: '走失管理' },
  { path: '/admin/found', icon: '⌂', label: '招领管理' },
  { path: '/admin/community', icon: '♧', label: '社区管理' },
  { path: '/admin/users', icon: '♙', label: '用户管理' },
  { path: '/admin/logs', icon: '≡', label: '操作日志' },
  { path: '/admin/account', icon: '○', label: '用户中心' }
] : [
  { path: '/pet-lost', icon: '⌕', label: '走失发布' },
  { path: '/pet-found', icon: '⌂', label: '招领发布' },
  { path: '/community', icon: '♧', label: '社区互动' },
  { path: '/messages', icon: '✉', label: '消息中心' },
  { path: '/user', icon: '○', label: '用户中心' }
])
const home = computed(() => isAdmin.value ? '/admin/lost' : '/pet-lost')

function active(path: string) {
  if (path === '/community') return route.path.startsWith('/community')
  if (path === '/pet-lost') return route.path.startsWith('/pet-lost')
  if (path === '/pet-found') return route.path.startsWith('/pet-found')
  return route.path === path
}
async function loadUnread() {
  if (!loggedIn.value || isAdmin.value || document.hidden) return
  try {
    const result = await getUnreadCounts()
    unread.value = (result.data?.notifications || 0) + (result.data?.direct || 0)
  } catch {
    loggedIn.value = !!localStorage.getItem('token')
  }
}
onMounted(async () => {
  if (!loggedIn.value) return
  try {
    const response = await getCurrentUser()
    isAdmin.value = response.data.data?.role === 'ADMIN'
    await loadUnread()
    timer = setInterval(loadUnread, 30000)
    window.addEventListener('pawfind:messages-changed', loadUnread)
  } catch {
    loggedIn.value = !!localStorage.getItem('token')
  }
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('pawfind:messages-changed', loadUnread)
})
</script>
<template>
  <header class="app-nav">
    <div class="app-nav__inner">
      <RouterLink class="app-brand" :to="home">🐾 PawFind<small v-if="isAdmin">管理平台</small></RouterLink>
      <nav class="square-navigation" aria-label="主要导航">
        <RouterLink v-for="link in links" :key="link.path" :to="link.path" :class="{ selected: active(link.path) }" :aria-current="active(link.path) ? 'page' : undefined">
          <span class="nav-icon" aria-hidden="true">{{ link.icon }}</span><span>{{ link.label }}</span>
          <b v-if="link.path === '/messages' && unread" class="unread-badge">{{ unread > 99 ? '99+' : unread }}</b>
        </RouterLink>
        <RouterLink v-if="!loggedIn" to="/login" class="nav-signin">登录</RouterLink>
      </nav>
    </div>
  </header>
  <PageBack :home="home" />
  <nav class="mobile-nav" :style="{ '--nav-count': links.length }" aria-label="移动端导航">
    <RouterLink v-for="link in links" :key="link.path" :to="link.path" :class="{ selected: active(link.path) }">
      <span aria-hidden="true">{{ link.icon }}</span>{{ link.label }}
      <b v-if="link.path === '/messages' && unread" class="unread-badge">{{ unread > 99 ? '99+' : unread }}</b>
    </RouterLink>
  </nav>
</template>
