
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '../api/user'
import type { UserInfo } from '../api/user'

const router = useRouter()

const user = ref<UserInfo | null>(null)
const loading = ref(true)
const errorMessage = ref('')

// 获取当前登录用户资料
async function loadCurrentUser() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getCurrentUser()
    const result = response.data

    if (result.code !== 200 || !result.data) {
      errorMessage.value = result.message || '获取用户资料失败'
      return
    }

    // 保存后端返回的用户资料
    user.value = result.data

  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '获取用户资料失败'

    // 如果 JWT 失效，返回登录页面
    if (!localStorage.getItem('token')) {
      await router.replace('/login')
    }

  } finally {
    loading.value = false
  }
}

// 退出登录
async function handleLogout() {
  localStorage.removeItem('token')
  user.value = null
  await router.push('/login')
}

// 页面加载时自动获取用户资料
onMounted(() => {
  loadCurrentUser()
})
</script>

<template>
  <div class="user-page">

    <header class="topbar">
      <div class="brand">PawFind</div>
      <span class="topbar-label">PET COMMUNITY</span>
    </header>

    <main class="main-content">

      <section class="welcome">
        <p class="eyebrow">PERSONAL CENTER</p>
        <h1>个人中心</h1>
        <p>欢迎回来，查看你的 PawFind 账户信息。</p>
      </section>

      <section class="profile-card">

        <div v-if="loading" class="state-message">
          正在加载用户资料...
        </div>

        <div v-else-if="errorMessage" class="state-message error">
          {{ errorMessage }}
          <button @click="loadCurrentUser">
            重新加载
          </button>
        </div>

        <div v-else-if="user">

          <div class="profile-heading">
            <div class="avatar">
              {{ user.username.charAt(0).toUpperCase() }}
            </div>

            <div>
              <h2>{{ user.username }}</h2>
              <span class="status">已登录</span>
            </div>
          </div>

          <div class="divider"></div>

          <div class="info-row">
            <span>用户 ID</span>
            <strong>{{ user.id }}</strong>
          </div>

          <div class="info-row">
            <span>用户名</span>
            <strong>{{ user.username }}</strong>
          </div>

          <div class="info-row">
            <span>用户角色</span>
            <strong>{{ user.role }}</strong>
          </div>

          <button class="logout-button" @click="handleLogout">
            退出登录
          </button>

        </div>

      </section>

      <footer>
        PAWFIND · FINDING EVERY PAW A HOME
      </footer>

    </main>
  </div>
</template>

<style scoped>
.user-page {
  min-height: 100vh;
  background: #f5f7f2;
  color: #29352d;
}

.topbar {
  height: 68px;
  padding: 0 8%;
  display: flex;
  align-items: center;
  gap: 14px;
  background: white;
  border-bottom: 1px solid #e6ebe4;
}

.brand {
  color: #244b38;
  font-size: 23px;
  font-weight: 700;
}

.topbar-label {
  color: #849487;
  font-size: 9px;
  letter-spacing: 1.5px;
}

.main-content {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 65px 24px;
}

.welcome {
  margin-bottom: 32px;
}

.eyebrow {
  margin-bottom: 10px;
  color: #527b60;
  font-size: 11px;
  letter-spacing: 2px;
}

.welcome h1 {
  margin: 0 0 12px;
  font-size: 30px;
  font-weight: 650;
}

.welcome > p:last-child {
  color: #849087;
}

.profile-card {
  padding: 36px;
  background: white;
  border: 1px solid #e6ebe4;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(36, 75, 56, 0.04);
}

.profile-heading {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar {
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #dce9de;
  color: #244b38;
  font-size: 23px;
  font-weight: 650;
}

.profile-heading h2 {
  margin: 0 0 7px;
  font-size: 20px;
}

.status {
  color: #527b60;
  font-size: 12px;
}

.divider {
  height: 1px;
  margin: 28px 0;
  background: #e6ebe4;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 18px 0;
  border-bottom: 1px solid #edf0eb;
}

.info-row span {
  color: #849087;
}

.info-row strong {
  color: #34483a;
  font-weight: 600;
}

.logout-button {
  width: 100%;
  height: 46px;
  margin-top: 30px;
  border: 1px solid #244b38;
  border-radius: 7px;
  background: white;
  color: #244b38;
  font-weight: 600;
  cursor: pointer;
}

.logout-button:hover {
  background: #244b38;
  color: white;
}

.state-message {
  padding: 30px 0;
  color: #849087;
  text-align: center;
}

.state-message button {
  display: block;
  margin: 15px auto 0;
  color: #244b38;
}

.error {
  color: #c34b43;
}

footer {
  margin-top: 60px;
  color: #a1aaa2;
  font-size: 10px;
  letter-spacing: 1.5px;
  text-align: center;
}

@media (max-width: 480px) {
  .main-content {
    padding: 40px 18px;
  }

  .profile-card {
    padding: 24px 18px;
  }
}
</style>
