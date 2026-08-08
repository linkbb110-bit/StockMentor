<script setup lang="ts">
import axios from 'axios'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/authStore'
import { isValidLoginPassword } from '../validation/passwordPolicy'

const authStore = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const pending = ref(false)
const errorMessage = ref('')

const publicErrorCode = (error: unknown): string | null => {
  if (!axios.isAxiosError(error)) {
    return null
  }

  const data: unknown = error.response?.data
  if (typeof data !== 'object' || data === null || !('code' in data)) {
    return null
  }

  return typeof data.code === 'string' ? data.code : null
}

const validate = (): string | null => {
  const normalizedEmail = email.value.trim()
  if (!normalizedEmail) {
    return '请输入邮箱'
  }
  if (normalizedEmail.length > 254) {
    return '邮箱不能超过 254 个字符'
  }
  if (!isValidLoginPassword(password.value)) {
    return '密码须为 8–64 个字符，且不得超过 72 个 UTF-8 字节'
  }

  return null
}

const submit = async (): Promise<void> => {
  if (pending.value) {
    return
  }

  const validationError = validate()
  if (validationError) {
    errorMessage.value = validationError
    return
  }

  const payload = {
    email: email.value,
    password: password.value,
  }
  password.value = ''
  errorMessage.value = ''
  pending.value = true

  try {
    const applied = await authStore.login(payload)
    if (applied) {
      await router.push('/dashboard')
    }
  } catch (error: unknown) {
    errorMessage.value =
      publicErrorCode(error) === 'AUTH_INVALID_CREDENTIALS'
        ? '邮箱或密码错误'
        : '登录失败，请稍后重试'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <main class="page">
    <section class="panel" aria-labelledby="login-title">
      <header class="panel-header">
        <p class="eyebrow">StockMentor</p>
        <h1 id="login-title">登录</h1>
        <p>继续你的投资基础知识学习。</p>
      </header>

      <form class="form-grid" novalidate :aria-busy="pending" @submit.prevent="submit">
        <div class="field">
          <label for="login-email">邮箱</label>
          <input
            id="login-email"
            v-model="email"
            type="text"
            name="email"
            inputmode="email"
            autocomplete="email"
            required
          />
        </div>

        <div class="field">
          <label for="login-password">密码</label>
          <input
            id="login-password"
            v-model="password"
            type="password"
            name="password"
            autocomplete="current-password"
            minlength="8"
            maxlength="64"
            required
          />
        </div>

        <p v-if="errorMessage" class="feedback feedback-error" role="alert" aria-live="assertive">
          {{ errorMessage }}
        </p>

        <button data-testid="login-submit" type="submit" :disabled="pending">
          {{ pending ? '登录中…' : '登录' }}
        </button>
      </form>

      <p class="secondary-action">还没有账号？<RouterLink to="/register">注册</RouterLink></p>
      <p class="notice">本应用仅用于投资教育，不构成投资建议、收益承诺或短期涨跌预测。</p>
    </section>
  </main>
</template>
