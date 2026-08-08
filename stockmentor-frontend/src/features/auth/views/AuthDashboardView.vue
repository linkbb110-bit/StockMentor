<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const router = useRouter()
const currentUser = computed(() => authStore.currentUser)

const logout = (): void => {
  authStore.logout()
  void router.replace('/login')
}
</script>

<template>
  <main class="page">
    <section class="panel" aria-labelledby="dashboard-title">
      <header class="panel-header">
        <p class="eyebrow">StockMentor</p>
        <h1 id="dashboard-title">欢迎，{{ currentUser?.nickname }}</h1>
      </header>

      <dl class="identity-list">
        <div>
          <dt>邮箱</dt>
          <dd>{{ currentUser?.email }}</dd>
        </div>
        <div>
          <dt>角色</dt>
          <dd>{{ currentUser?.role }}</dd>
        </div>
      </dl>

      <div class="placeholder" role="status">
        <p>V0.2 用户与认证已完成</p>
        <p>课程系统将在 V0.3 开放</p>
      </div>

      <nav class="actions" aria-label="用户操作">
        <RouterLink class="button-link" to="/profile">个人中心</RouterLink>
        <button data-testid="dashboard-logout" class="button-secondary" type="button" @click="logout">
          退出登录
        </button>
      </nav>
    </section>
  </main>
</template>
