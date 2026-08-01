import {
  createRouter,
  createWebHistory,
  type RouteLocationNormalizedGeneric,
} from 'vue-router'

import { useAuthStore } from '../features/auth/stores/authStore'
import { pinia } from '../stores'
import FoundationView from '../views/FoundationView.vue'

interface AuthenticationStore {
  readonly isAuthenticated: boolean
  restoreSession: () => Promise<void>
}

export const createAuthenticationGuard = (
  resolveStore: () => AuthenticationStore = () => useAuthStore(pinia),
) => {
  let restoration: Promise<void> | null = null
  let restorationFailed = false

  return async (to: Pick<RouteLocationNormalizedGeneric, 'meta'>) => {
    const authStore = resolveStore()
    restoration ??= Promise.resolve()
      .then(() => authStore.restoreSession())
      .catch(() => {
        restorationFailed = true
      })

    await restoration

    if (restorationFailed) {
      return to.meta.requiresAuth ? { path: '/login' } : undefined
    }

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return { path: '/login' }
    }

    if (to.meta.publicOnly && authStore.isAuthenticated) {
      return { path: '/dashboard' }
    }
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/login',
      name: 'login',
      component: FoundationView,
      meta: { publicOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: FoundationView,
      meta: { publicOnly: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: FoundationView,
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: FoundationView,
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(createAuthenticationGuard())

export default router
