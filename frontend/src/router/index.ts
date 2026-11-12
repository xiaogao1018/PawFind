import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import UserView from '../views/UserView.vue'
import PetLostListView from '../views/PetLostListView.vue'
import PetLostCreateView from '../views/PetLostCreateView.vue'
import PetLostDetailView from '../views/PetLostDetailView.vue'
import PetLostEditView from '../views/PetLostEditView.vue'
import PetFoundListView from '../views/PetFoundListView.vue'
import PetFoundFormView from '../views/PetFoundFormView.vue'
import PetFoundDetailView from '../views/PetFoundDetailView.vue'
import CommunityFeedView from '../views/CommunityFeedView.vue'
import CommunityPostFormView from '../views/CommunityPostFormView.vue'
import CommunityPostDetailView from '../views/CommunityPostDetailView.vue'
import MessagesView from '../views/MessagesView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'
import { getCurrentUser } from '../api/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/pet-lost' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/register', component: RegisterView, meta: { public: true } },
    { path: '/user', component: UserView, meta: { title: '用户中心' } },
    { path: '/pet-lost', component: PetLostListView, meta: { title: '走失信息', public: true } },
    { path: '/pet-lost/create', component: PetLostCreateView, meta: { title: '发布走失' } },
    { path: '/pet-lost/:id', component: PetLostDetailView, meta: { title: '走失详情', public: true } },
    { path: '/pet-lost/:id/edit', component: PetLostEditView, meta: { title: '编辑走失' } },
    { path: '/pet-found', component: PetFoundListView, meta: { title: '招领信息', public: true } },
    { path: '/pet-found/create', component: PetFoundFormView, meta: { title: '发布招领' } },
    { path: '/pet-found/:id', component: PetFoundDetailView, meta: { title: '招领详情', public: true } },
    { path: '/pet-found/:id/edit', component: PetFoundFormView, meta: { title: '编辑招领' } },
    { path: '/community', component: CommunityFeedView, meta: { title: '社区互动' } },
    { path: '/community/create', component: CommunityPostFormView, meta: { title: '发布动态' } },
    { path: '/community/posts/:id', component: CommunityPostDetailView, meta: { title: '动态详情' } },
    { path: '/community/posts/:id/edit', component: CommunityPostFormView, meta: { title: '编辑动态' } },
    { path: '/messages', component: MessagesView, meta: { title: '消息中心' } },
    { path: '/admin', redirect: '/admin/lost' },
    { path: '/admin/:section(lost|found|community|users|logs|account)', component: AdminDashboardView,
      meta: { requiresAdmin: true, title: '管理平台' } }
  ]
})
router.beforeEach(async to => {
  if (to.meta.public === true) return true
  const token = localStorage.getItem('token')
  if (!token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path === '/user') {
    try {
      const response = await getCurrentUser()
      if (response.data.data?.role === 'ADMIN') return '/admin/account'
    } catch { return { path: '/login' } }
  }
  if (to.meta.requiresAdmin === true) {
    try {
      const response = await getCurrentUser()
      if (response.data?.data?.role !== 'ADMIN') return { path: '/pet-lost' }
    } catch { return { path: '/login' } }
  }
  return true
})
export default router
