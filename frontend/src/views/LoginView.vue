
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { login } from '../api/auth'

const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  errorMessage.value = ''

  if (!username.value || !password.value) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  loading.value = true

  try {
    const response = await login({
      username: username.value,
      password: password.value
    })

    const result = response.data

    if (result.code !== 200 || !result.data?.token) {
      errorMessage.value = result.message || '登录失败'
      return
    }

    // 保存 JWT
    localStorage.setItem('token', result.data.token)

    // 登录成功，进入用户中心
    await router.push('/user')

  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">

      <div class="brand">
        <span class="brand-name">PawFind-宠友邻</span>
        <span class="brand-subtitle">PET COMMUNITY</span>
      </div>

      <div class="welcome">
        <h1>欢迎回来</h1>
        <p>登录你的 PawFind 账号，继续关注宠物动态。</p>
      </div>

      <form @submit.prevent="handleLogin">

        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model.trim="username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            required
          />
        </div>

        <p v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </p>

        <button
          class="login-button"
          type="submit"
          :disabled="loading"
        >
          {{ loading ? '正在登录...' : '登录' }}
        </button>

      </form>

      <div class="register-link">
        还没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </div>

      <div class="footer">
        PAWFIND · FINDING EVERY PAW A HOME
      </div>

    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px 20px;
  background: #f5f7f2;
  color: #29352d;
}

.login-card {
  width: 100%;
  max-width: 440px;
  padding: 48px 42px;
  background: #ffffff;
  border: 1px solid #e6ebe4;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(36, 75, 56, 0.06);
}

.brand {
  display: flex;
  flex-direction: column;
  margin-bottom: 42px;
}

.brand-name {
  color: #244b38;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -1px;
}

.brand-subtitle {
  margin-top: 5px;
  color: #849487;
  font-size: 10px;
  letter-spacing: 2px;
}

.welcome {
  margin-bottom: 30px;
}

.welcome h1 {
  margin: 0 0 10px;
  color: #29352d;
  font-size: 26px;
  font-weight: 650;
}

.welcome p {
  margin: 0;
  color: #849087;
  font-size: 14px;
  line-height: 1.7;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-bottom: 22px;
}

.form-group label {
  color: #34483a;
  font-size: 14px;
  font-weight: 600;
}

.form-group input {
  width: 100%;
  height: 48px;
  padding: 0 14px;
  border: 1px solid #dce5dc;
  border-radius: 7px;
  outline: none;
  background: #ffffff;
  color: #29352d;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #527b60;
  box-shadow: 0 0 0 3px rgba(82, 123, 96, 0.10);
}

.form-group input::placeholder {
  color: #a5afa7;
}

.error-message {
  margin: 0 0 16px;
  color: #c34b43;
  font-size: 13px;
}

.login-button {
  width: 100%;
  height: 49px;
  border: none;
  border-radius: 7px;
  background: #244b38;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.login-button:hover {
  background: #345e47;
}

.login-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.register-link {
  margin-top: 26px;
  text-align: center;
  color: #849087;
  font-size: 13px;
}

.register-link a {
  color: #386849;
  font-weight: 600;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}

.footer {
  margin-top: 40px;
  text-align: center;
  color: #a1aaa2;
  font-size: 9px;
  letter-spacing: 1.5px;
}

@media (max-width: 480px) {
  .login-card {
    padding: 36px 24px;
  }
}
</style>
