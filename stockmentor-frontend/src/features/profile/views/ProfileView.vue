<script setup lang="ts">
import axios from 'axios'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../../auth/stores/authStore'

const authStore = useAuthStore()
const router = useRouter()

const email = computed(() => authStore.currentUser?.email ?? '')
const nickname = ref(authStore.currentUser?.nickname ?? '')
const pending = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

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

const validateNickname = (): boolean => {
  const normalizedNickname = nickname.value.trim()
  const length = Array.from(normalizedNickname).length

  return (
    length >= 2 &&
    length <= 20 &&
    /^[\p{Script=Han}A-Za-z0-9 _-]+$/u.test(normalizedNickname)
  )
}

const save = async (): Promise<void> => {
  if (pending.value) {
    return
  }
  if (!validateNickname()) {
    successMessage.value = ''
    errorMessage.value = nicknameRuleMessage
    return
  }

  const submittedNickname = nickname.value
  errorMessage.value = ''
  successMessage.value = ''
  pending.value = true

  try {
    const updatedUser = await authStore.updateNickname(submittedNickname)
    if (!updatedUser) {
      return
    }
    nickname.value = updatedUser.nickname
    successMessage.value = '昵称已更新'
  } catch (error: unknown) {
    errorMessage.value =
      publicErrorCode(error) === 'USER_NICKNAME_INVALID'
        ? nicknameRuleMessage
        : '昵称更新失败，请稍后重试'
  } finally {
    pending.value = false
  }
}

const logout = (): void => {
  if (pending.value) {
    return
  }

  authStore.logout()
  void router.replace('/login')
}
</script>

<template>
  <main class="page">
    <section class="panel" aria-labelledby="profile-title">
      <header class="panel-header">
        <p class="eyebrow">StockMentor</p>
        <h1 id="profile-title">个人中心</h1>
      </header>

      <form class="form-grid" novalidate :aria-busy="pending" @submit.prevent="save">
        <div class="field">
          <label for="profile-email">邮箱</label>
          <input id="profile-email" type="email" name="email" :value="email" readonly />
        </div>

        <div class="field">
          <label for="profile-nickname">昵称</label>
          <input
            id="profile-nickname"
            v-model="nickname"
            type="text"
            name="nickname"
            autocomplete="nickname"
            required
          />
          <p class="hint">2–20 个字符，可使用中文、英文字母、数字、空格、下划线和短横线。</p>
        </div>

        <p v-if="errorMessage" class="feedback feedback-error" role="alert" aria-live="assertive">
          {{ errorMessage }}
        </p>
        <p v-if="successMessage" class="feedback feedback-success" role="status" aria-live="polite">
          {{ successMessage }}
        </p>

        <button data-testid="profile-save" type="submit" :disabled="pending">
          {{ pending ? '保存中…' : '保存昵称' }}
        </button>
      </form>

      <button
        data-testid="profile-logout"
        class="button-secondary full-width"
        type="button"
        :disabled="pending"
        @click="logout"
      >
        退出登录
      </button>
    </section>
  </main>
</template>
