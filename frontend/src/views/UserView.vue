<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppNav from '../components/AppNav.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { getCurrentUser, type UserInfo } from '../api/user'
import { listMyCommunityPosts } from '../api/community'
import {
  getPersonalSummary, listMyPets, listMyLost, listMyFound, savePet, deletePet,
  type PersonalSummary, type PetProfile
} from '../api/personal'

type Section = 'pets' | 'lost' | 'found' | 'posts' | 'account'
interface Entry { id: number; title: string; description: string; status: string; date: string; path: string | null }
const router = useRouter()
const user = ref<UserInfo | null>(null)
const summary = ref<PersonalSummary>({ pets: 0, lost: 0, found: 0, posts: 0 })
const route = useRoute()
const requestedSection = String(route.query.section || 'pets')
const section = ref<Section>(['pets', 'lost', 'found', 'posts', 'account'].includes(requestedSection)
  ? requestedSection as Section : 'pets')
const menu = [
  { key: 'pets' as const, icon: '🐾', label: '我的宠物', hint: '记录它的每一份独特' },
  { key: 'lost' as const, icon: '⌕', label: '我的寻宠', hint: '我发布的走失信息' },
  { key: 'found' as const, icon: '⌂', label: '我的发现', hint: '我上报的发现记录' },
  { key: 'posts' as const, icon: '♧', label: '我的动态', hint: '帖子与审核进度' },
  { key: 'account' as const, icon: '♙', label: '账号信息', hint: '查看个人账号资料' }
]
const contentPanel = ref<HTMLElement | null>(null)
const heading = computed(() => menu.find(item => item.key === section.value)?.label)
const loading = ref(true), recordsLoading = ref(false), busy = ref(false)
const error = ref(''), recordError = ref(''), formError = ref('')
const pets = ref<PetProfile[]>([]), entries = ref<Entry[]>([])
const page = ref(1), totalPages = ref(0), total = ref(0)
const editingId = ref<number | null>(null), showForm = ref(false)
const petForm = reactive({ name: '', species: '', breed: '', birthDate: '', description: '' })
let requestVersion = 0

async function loadSummary() {
  const result = await getPersonalSummary()
  if (result.code !== 200 || !result.data) throw new Error(result.message)
  summary.value = result.data
}
async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = (await getCurrentUser()).data
    if (result.code !== 200 || !result.data) throw new Error(result.message)
    user.value = result.data
    await loadSummary()
    await loadRecords()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '个人中心加载失败'
    if (!localStorage.getItem('token')) await router.replace('/login')
  } finally {
    loading.value = false
  }
}
async function loadRecords() {
  const version = ++requestVersion
  recordError.value = ''
  recordsLoading.value = true
  try {
    let data
    let rows: Entry[] = []
    if (section.value === 'pets') {
      const result = await listMyPets(page.value)
      if (!result.data) throw new Error(result.message)
      data = result.data
      if (version === requestVersion) pets.value = result.data.records
    } else if (section.value === 'lost') {
      const result = await listMyLost(page.value)
      if (!result.data) throw new Error(result.message)
      data = result.data
      rows = data.records.map(item => ({
        id: item.id, title: item.petName || '未命名宠物', description: item.moderationReason || item.lostLocation,
        status: item.visibilityStatus === 'HIDDEN' ? 'HIDDEN' : item.status, date: item.createdAt,
        path: item.status === 'WITHDRAWN' || item.visibilityStatus === 'HIDDEN' ? null : '/pet-lost/' + item.id
      }))
    } else if (section.value === 'found') {
      const result = await listMyFound(page.value)
      if (!result.data) throw new Error(result.message)
      data = result.data
      rows = data.records.map(item => ({
        id: item.id, title: item.petName || '未命名宠物', description: item.moderationReason || item.foundLocation,
        status: item.visibilityStatus === 'HIDDEN' ? 'HIDDEN' : item.status, date: item.createdAt,
        path: item.status === 'OPEN' && item.visibilityStatus !== 'HIDDEN' ? '/pet-found/' + item.id : null
      }))
    } else if (section.value === 'posts') {
      const result = await listMyCommunityPosts(page.value)
      if (!result.data) throw new Error(result.message)
      data = result.data
      rows = data.records.map(item => ({
        id: item.id, title: item.title, description: item.moderationReason || item.content,
        status: item.visibilityStatus === 'HIDDEN' ? 'HIDDEN' : item.auditStatus,
        date: item.createdAt, path: '/community/posts/' + item.id
      }))
    }
    if (version !== requestVersion) return
    entries.value = rows
    totalPages.value = data?.totalPages || 0
    total.value = data?.total || 0
  } catch (e) {
    if (version === requestVersion) recordError.value = e instanceof Error ? e.message : '记录加载失败'
  } finally {
    if (version === requestVersion) recordsLoading.value = false
  }
}
async function chooseSection(value: Section) {
  section.value = value
  page.value = 1
  showForm.value = false
  loadRecords()
  await nextTick()
  if (window.matchMedia('(max-width: 720px)').matches) {
    contentPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
function editPet(pet?: PetProfile) {
  editingId.value = pet?.id ?? null
  Object.assign(petForm, {
    name: pet?.name || '', species: pet?.species || '', breed: pet?.breed || '',
    birthDate: pet?.birthDate || '', description: pet?.description || ''
  })
  formError.value = ''
  showForm.value = true
}
async function submitPet() {
  busy.value = true
  formError.value = ''
  try {
    const result = await savePet({
      name: petForm.name.trim(), species: petForm.species.trim(), breed: petForm.breed.trim() || null,
      birthDate: petForm.birthDate || null, description: petForm.description.trim() || null
    }, editingId.value)
    if (result.code !== 200) throw new Error(result.message)
    showForm.value = false
    page.value = 1
    await Promise.all([loadSummary(), loadRecords()])
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    busy.value = false
  }
}
async function removePet(pet: PetProfile) {
  if (!confirm('确定删除「' + pet.name + '」的宠物档案吗？')) return
  busy.value = true
  try {
    await deletePet(pet.id)
    page.value = 1
    await Promise.all([loadSummary(), loadRecords()])
  } catch (e) {
    recordError.value = e instanceof Error ? e.message : '删除失败'
  } finally {
    busy.value = false
  }
}
function logout() {
  localStorage.removeItem('token')
  router.replace('/login')
}
function move(delta: number) {
  page.value += delta
  loadRecords()
}
onMounted(load)
</script>

<template>
  <AppNav />
  <main class="container personal-center">
    <div v-if="loading" class="state-panel">正在打开你的个人空间……</div>
    <div v-else-if="error" class="state-panel state-error">{{ error }} <button class="button button-light" @click="load">重新加载</button></div>
    <template v-else-if="user">
      <section class="profile-banner">
        <div class="profile-identity">
          <div class="profile-avatar">{{ user.username.charAt(0).toUpperCase() }}</div>
          <div><p class="eyebrow">MY PAWFIND</p><h1>{{ user.username }}</h1><p>ID {{ user.id }} · {{ user.role === 'ADMIN' ? '社区管理员' : '宠友邻成员' }}</p></div>
        </div>
        <div class="profile-stats">
          <button @click="chooseSection('pets')"><strong>{{ summary.pets }}</strong><span>我的宠物</span></button>
          <button @click="chooseSection('lost')"><strong>{{ summary.lost + summary.found }}</strong><span>寻宠与发现</span></button>
          <button @click="chooseSection('posts')"><strong>{{ summary.posts }}</strong><span>发布动态</span></button>
        </div>
      </section>
      <div class="personal-layout">
        <aside class="personal-sidebar">
          <nav class="profile-menu" aria-label="个人中心栏目">
            <button v-for="item in menu" :key="item.key" :class="{ active: section === item.key }" :aria-pressed="section === item.key" @click="chooseSection(item.key)">
              <span class="menu-icon" aria-hidden="true">{{ item.icon }}</span>
              <span class="grow"><b>{{ item.label }}</b><small>{{ item.hint }}</small></span><span aria-hidden="true">›</span>
            </button>
          </nav>
          <button class="profile-logout" @click="logout"><span aria-hidden="true">⏻</span>退出登录</button>
        </aside>
        <section ref="contentPanel" class="panel personal-content">
          <div class="page-heading"><div><p class="eyebrow">YOUR LITTLE WORLD</p><h2>{{ heading }}</h2></div>
            <button v-if="section === 'pets' && !showForm" class="button button-small" @click="editPet()">＋ 添加宠物</button>
            <RouterLink v-if="section === 'lost'" class="button button-small" to="/pet-lost/create">发布走失</RouterLink>
            <RouterLink v-if="section === 'found'" class="button button-small" to="/pet-found/create">上报发现</RouterLink>
            <RouterLink v-if="section === 'posts'" class="button button-small" to="/community/create">发布动态</RouterLink>
          </div>
          <form v-if="showForm && section === 'pets'" class="form-stack pet-editor" @submit.prevent="submitPet">
            <h3>{{ editingId === null ? '添加宠物档案' : '编辑宠物档案' }}</h3>
            <label>宠物名字 *<input v-model="petForm.name" maxlength="50" required placeholder="它叫什么名字？"></label>
            <div class="form-columns"><label>种类 *<input v-model="petForm.species" maxlength="30" required placeholder="例如 猫、狗"></label><label>品种<input v-model="petForm.breed" maxlength="50" placeholder="选填"></label></div>
            <label>生日<input v-model="petForm.birthDate" type="date"></label>
            <label>关于它<textarea v-model="petForm.description" rows="3" maxlength="2000" placeholder="毛色、性格，还有它的小习惯"></textarea></label>
            <p v-if="formError" class="form-error">{{ formError }}</p>
            <div class="button-row"><button class="button" :disabled="busy">{{ busy ? '保存中……' : '保存档案' }}</button><button type="button" class="button button-light" :disabled="busy" @click="showForm = false">取消</button></div>
          </form>
          <div v-else-if="section === 'account'" class="account-details">
            <div><span>用户名</span><b>{{ user.username }}</b></div><div><span>用户 ID</span><b>{{ user.id }}</b></div><div><span>角色</span><b>{{ user.role === 'ADMIN' ? '管理员' : '普通成员' }}</b></div>
          </div>
          <template v-else>
            <p v-if="recordError" class="form-error">{{ recordError }} <button class="text-button" @click="loadRecords">重试</button></p>
            <p v-if="recordsLoading" class="muted">正在加载记录……</p>
            <template v-else-if="section === 'pets'">
              <div v-if="!pets.length" class="empty-personal"><span>🐾</span><h3>给它留一个专属位置</h3><p class="muted">添加名字、生日和特征，建立你的第一份宠物档案。</p><button class="button button-light" @click="editPet()">添加我的宠物</button></div>
              <article v-for="pet in pets" :key="pet.id" class="my-pet-card">
                <div class="pet-avatar" aria-hidden="true">🐾</div><div class="grow"><h3>{{ pet.name }}</h3><p>{{ pet.species }}<span v-if="pet.breed"> · {{ pet.breed }}</span></p><small v-if="pet.birthDate" class="muted">生日 {{ pet.birthDate }}</small><p v-if="pet.description" class="muted">{{ pet.description }}</p>
                  <div class="button-row"><button class="text-button" :disabled="busy" @click="editPet(pet)">编辑档案</button><button class="text-button danger-text" :disabled="busy" @click="removePet(pet)">删除</button></div>
                </div>
              </article>
            </template>
            <template v-else>
              <div v-if="!entries.length" class="empty-personal"><span>🌿</span><h3>这里还没有记录</h3><p class="muted">你的发布与处理进度都会在这里保留。</p></div>
              <article v-for="entry in entries" :key="entry.id" class="personal-entry">
                <div class="card-title-row"><h3>{{ entry.title }}</h3><StatusBadge :status="entry.status" /></div>
                <p class="clamp">{{ entry.description }}</p><small class="muted">{{ entry.date.replace('T', ' ') }}</small>
                <RouterLink v-if="entry.path" class="text-link" :to="entry.path">查看与管理 →</RouterLink>
              </article>
            </template>
            <div v-if="totalPages > 1" class="pagination"><button class="button button-light button-small" :disabled="page <= 1 || recordsLoading" @click="move(-1)">上一页</button><span>{{ page }} / {{ totalPages }} · {{ total }} 条</span><button class="button button-light button-small" :disabled="page >= totalPages || recordsLoading" @click="move(1)">下一页</button></div>
          </template>
        </section>
      </div>
    </template>
  </main>
</template>
