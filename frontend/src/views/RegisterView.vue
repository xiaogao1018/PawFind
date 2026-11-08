
<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { register } from '../api/auth'

const username = ref('')
const password = ref('')
const confirmPassword = ref('')

const loading = ref(false)
const errorMessage = ref('')
const registered = ref(false)

async function handleRegister() {
  errorMessage.value = ''

  // 检查必填内容
  if (!username.value.trim() || !password.value) {
    errorMessage.value = '请输入用户名和密码'
    return
  }

  // 检查两次密码是否一致
  if (password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  loading.value = true

  try {
    const response = await register({
      username: username.value.trim(),
      password: password.value
    })

    const result = response.data

    if (result.code !== 200) {
      errorMessage.value = result.message || '注册失败'
      return
    }

    // 注册成功
    registered.value = true

  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-card">

      <div class="brand">
        <span class="brand-name">PawFind</span>
        <span class="brand-subtitle">PET COMMUNITY</span>
      </div>

      <div class="welcome">
        <h1>创建账号</h1>
        <p>加入 PawFind，开启你的宠物社区之旅。</p>
      </div>

      <div v-if="registered" class="success-panel">
        <h2>注册成功</h2>
        <p>你的 PawFind 账号已经创建完成。</p>
        <RouterLink to="/login" class="success-button">
          前往登录
        </RouterLink>
      </div>

      <form v-else @submit.prevent="handleRegister">

        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model.trim="username"
            type="text"
            placeholder="请设置用户名"
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
            placeholder="请设置密码"
            autocomplete="new-password"
            required
          />
        </div>

        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input
            id="confirmPassword"
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            required
          />
        </div>

        <p v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </p>

        <button
          class="register-button"
          type="submit"
          :disabled="loading"
        >
          {{ loading ? '正在注册...' : '创建账号' }}
        </button>

      </form>

      <div class="login-link">
        已有账号？
        <RouterLink to="/login">立即登录</RouterLink>
      </div>

      <div class="footer">
        PAWFIND · FINDING EVERY PAW A HOME
      </div>

    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px 20px;
  background: #f5f7f2;
  color: #29352d;
}

.register-card {
  width: 100%;
  max-width: 440px;
  padding: 42px;
  background: #fff;
  border: 1px solid #e6ebe4;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(36, 75, 56, 0.06);
}

.brand {
  display: flex;
  flex-direction: column;
  margin-bottom: 32px;
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
  margin-bottom: 28px;
}

.welcome h1 {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 650;
}

.welcome p,
.success-panel p {
  color: #849087;
  font-size: 14px;
  line-height: 1.7;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-bottom: 19px;
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
  background: white;
  color: #29352d;
  font-size: 14px;
}

.form-group input:focus {
  border-color: #527b60;
  box-shadow: 0 0 0 3px rgba(82, 123, 96, 0.1);
}

.form-group input::placeholder {
  color: #a5afa7;
}

.error-message {
  margin-bottom: 16px;
  color: #c34b43;
  font-size: 13px;
}

.register-button,
.success-button {
  display: flex;
  width: 100%;
  height: 49px;
  justify-content: center;
  align-items: center;
  border: none;
  border-radius: 7px;
  background: #244b38;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
}

.register-button:hover,
.success-button:hover {
  background: #345e47;
}

.register-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.login-link {
  margin-top: 25px;
  text-align: center;
  color: #849087;
  font-size: 13px;
}

.login-link a {
  color: #386849;
  font-weight: 600;
  text-decoration: none;
}

.success-panel {
  padding: 20px;
  border-radius: 8px;
  background: #f5f7f2;
}

.success-panel h2 {
  margin: 0 0 10px;
  color: #244b38;
  font-size: 20px;
}

.success-button {
  margin-top: 20px;
}

.footer {
  margin-top: 35px;
  text-align: center;
  color: #a1aaa2;
  font-size: 9px;
  letter-spacing: 1.5px;
}

@media (max-width: 480px) {
  .register-card {
    padding: 32px 24px;
  }
}
</style>
