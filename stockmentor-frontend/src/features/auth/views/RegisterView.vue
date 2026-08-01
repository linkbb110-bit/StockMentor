<script setup lang="ts">
import axios from 'axios'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const router = useRouter()

const email = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const pending = ref(false)
const errorMessage = ref('')

const nicknameRuleMessage =
  '昵称须为 2–20 个字符，仅可包含中文、英文字母、数字、空格、下划线和短横线'

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

  const normalizedNickname = nickname.value.trim()
  const nicknameLength = Array.from(normalizedNickname).length
  if (
    nicknameLength < 2 ||
    nicknameLength > 20 ||
    !/^[\p{Script=Han}A-Za-z0-9 _-]+$/u.test(normalizedNickname)
  ) {
    return nicknameRuleMessage
  }

  if (
    password.value.length < 8 ||
    password.value.length > 64 ||
    !/[A-Za-z]/.test(password.value) ||
    !/[0-9]/.test(password.value)
  ) {
    return '密码须为 8–64 个字符，并至少包含一个英文字母和一个数字'
  }
  if (password.value !== confirmPassword.value) {
    return '两次输入的密码不一致'
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
    nickname: nickname.value,
    password: password.value,
  }
  password.value = ''
  confirmPassword.value = ''
  errorMessage.value = ''
  pending.value = true

  try {
    await authStore.register(payload)
    await router.push('/dashboard')
  } catch (error: unknown) {
    const code = publicErrorCode(error)
    if (code === 'USER_EMAIL_ALREADY_EXISTS') {
      errorMessage.value = '该邮箱已注册，请直接登录'
    } else if (code === 'USER_NICKNAME_INVALID') {
      errorMessage.value = nicknameRuleMessage
    } else {
      errorMessage.value = '注册失败，请检查填写内容后重试'
    }
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <main class="page">
    <section class="panel" aria-labelledby="register-title">
      <header class="panel-header">
        <p class="eyebrow">StockMentor</p>
        <h1 id="register-title">注册</h1>
        <p>创建账号后将自动登录。</p>
      </header>

      <form class="form-grid" novalidate :aria-busy="pending" @submit.prevent="submit">
        <div class="field">
          <label for="register-email">邮箱</label>
          <input
            id="register-email"
            v-model="email"
            type="text"
            name="email"
            inputmode="email"
            autocomplete="email"
            maxlength="254"
            required
          />
        </div>

        <div class="field">
          <label for="register-nickname">昵称</label>
          <input
            id="register-nickname"
            v-model="nickname"
            type="text"
            name="nickname"
            autocomplete="nickname"
            required
          />
          <p class="hint">2–20 个字符，可使用中文、英文字母、数字、空格、下划线和短横线。</p>
        </div>

        <div class="field">
          <label for="register-password">密码</label>
          <input
            id="register-password"
            v-model="password"
            type="password"
            name="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="64"
            aria-describedby="password-rule"
            required
          />
          <p id="password-rule" class="hint" data-testid="password-rule">
            8–64 个字符，至少包含一个英文字母和一个数字；不会自动去除首尾空格。
          </p>
        </div>

        <div class="field">
          <label for="register-confirm-password">确认密码</label>
          <input
            id="register-confirm-password"
            v-model="confirmPassword"
            type="password"
            name="confirmPassword"
            autocomplete="new-password"
            minlength="8"
            maxlength="64"
            required
          />
        </div>

        <p v-if="errorMessage" class="feedback feedback-error" role="alert" aria-live="assertive">
          {{ errorMessage }}
        </p>

        <button data-testid="register-submit" type="submit" :disabled="pending">
          {{ pending ? '注册中…' : '注册并登录' }}
        </button>
      </form>

      <p class="secondary-action">已有账号？<RouterLink to="/login">登录</RouterLink></p>
    </section>
  </main>
</template>
