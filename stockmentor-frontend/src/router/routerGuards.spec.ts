import type { RouteLocationRaw } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import router, * as routerModule from './index'

interface TestAuthStore {
  isAuthenticated: boolean
  restoreSession: () => Promise<void>
}

type TestRoute = {
  path: string
  meta: {
    publicOnly?: boolean
    requiresAuth?: boolean
  }
}

type AuthenticationGuard = (to: TestRoute) => Promise<RouteLocationRaw | undefined>
type AuthenticationGuardFactory = (resolveStore: () => TestAuthStore) => AuthenticationGuard

const createAuthenticationGuard = (
  routerModule as unknown as {
    createAuthenticationGuard: AuthenticationGuardFactory
  }
).createAuthenticationGuard

describe('authentication routes', () => {
  it('redirects the root and defines public-only and protected route metadata', () => {
    const routes = new Map(router.getRoutes().map((route) => [route.path, route]))

    expect(routes.get('/')?.redirect).toBe('/dashboard')
    expect(routes.get('/login')?.meta).toMatchObject({ publicOnly: true })
    expect(routes.get('/register')?.meta).toMatchObject({ publicOnly: true })
    expect(routes.get('/dashboard')?.meta).toMatchObject({ requiresAuth: true })
    expect(routes.get('/profile')?.meta).toMatchObject({ requiresAuth: true })
    expect(routes.get('/courses')?.meta.requiresAuth).not.toBe(true)
    expect(routes.get('/courses/:courseId')?.meta.requiresAuth).not.toBe(true)
    expect(routes.get('/lessons/:lessonId')?.meta.requiresAuth).not.toBe(true)
    expect(routes.get('/lessons/:lessonId/quiz')?.meta.requiresAuth).not.toBe(true)
    expect(routes.get('/wrong-questions')?.meta).toMatchObject({ requiresAuth: true })
  })
})

describe('authentication route guard', () => {
  let store: TestAuthStore

  beforeEach(() => {
    store = {
      isAuthenticated: false,
      restoreSession: vi.fn().mockResolvedValue(undefined),
    }
  })

  it('waits for restoration before redirecting an unauthenticated protected route', async () => {
    let finishRestoration: (() => void) | undefined
    store.restoreSession = vi.fn().mockReturnValue(
      new Promise<void>((resolve) => {
        finishRestoration = resolve
      }),
    )
    const guard = createAuthenticationGuard(() => store)

    let guardSettled = false
    const decision = guard({ path: '/dashboard', meta: { requiresAuth: true } }).finally(() => {
      guardSettled = true
    })
    await vi.waitFor(() => expect(store.restoreSession).toHaveBeenCalledOnce())

    expect(guardSettled).toBe(false)
    finishRestoration?.()
    await expect(decision).resolves.toEqual({ path: '/login' })
  })

  it('restores exactly once across later navigation decisions', async () => {
    const guard = createAuthenticationGuard(() => store)

    await expect(
      guard({ path: '/dashboard', meta: { requiresAuth: true } }),
    ).resolves.toEqual({ path: '/login' })
    await expect(guard({ path: '/register', meta: { publicOnly: true } })).resolves.toBeUndefined()

    expect(store.restoreSession).toHaveBeenCalledOnce()
  })

  it('redirects an authenticated user away from the login page', async () => {
    store.isAuthenticated = true
    const guard = createAuthenticationGuard(() => store)

    await expect(guard({ path: '/login', meta: { publicOnly: true } })).resolves.toEqual({
      path: '/dashboard',
    })
  })

  it('fails closed when restoration rejects even if stale state says authenticated', async () => {
    store.isAuthenticated = true
    store.restoreSession = vi.fn().mockRejectedValue(new Error('restoration failed'))
    const guard = createAuthenticationGuard(() => store)

    await expect(
      guard({ path: '/profile', meta: { requiresAuth: true } }),
    ).resolves.toEqual({ path: '/login' })
    expect(store.restoreSession).toHaveBeenCalledOnce()
  })

  it('keeps a public course route available when session restoration fails', async () => {
    store.restoreSession = vi.fn().mockRejectedValue(new Error('restoration failed'))
    const guard = createAuthenticationGuard(() => store)

    await expect(guard({ path: '/courses/7', meta: {} })).resolves.toBeUndefined()
    expect(store.restoreSession).toHaveBeenCalledOnce()
  })

  it('keeps a public Quiz route available but protects wrong questions', async () => {
    const publicGuard = createAuthenticationGuard(() => store)
    await expect(publicGuard({ path: '/lessons/101/quiz', meta: {} })).resolves.toBeUndefined()

    const privateGuard = createAuthenticationGuard(() => store)
    await expect(
      privateGuard({ path: '/wrong-questions', meta: { requiresAuth: true } }),
    ).resolves.toEqual({ path: '/login' })
  })

  it('allows an authenticated user to open wrong questions', async () => {
    store.isAuthenticated = true
    const guard = createAuthenticationGuard(() => store)

    await expect(
      guard({ path: '/wrong-questions', meta: { requiresAuth: true } }),
    ).resolves.toBeUndefined()
  })
})
