import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type {
  AuthResponse,
  CurrentUser,
  LoginPayload,
  RegisterPayload,
} from '../types/auth'

const mocks = vi.hoisted(() => ({
  register: vi.fn(),
  login: vi.fn(),
  getCurrentUser: vi.fn(),
  updateNickname: vi.fn(),
  loadSession: vi.fn(),
  saveSession: vi.fn(),
  clearSession: vi.fn(),
}))

vi.mock('../api/authApi', () => ({
  register: mocks.register,
  login: mocks.login,
}))

vi.mock('../../profile/api/profileApi', () => ({
  getCurrentUser: mocks.getCurrentUser,
  updateNickname: mocks.updateNickname,
}))

vi.mock('../session/authSession', () => ({
  authSession: {
    load: mocks.loadSession,
    save: mocks.saveSession,
    clear: mocks.clearSession,
  },
}))

import { useAuthStore } from './authStore'

const currentUser: CurrentUser = {
  id: 42,
  email: 'learner@example.com',
  nickname: 'Learner',
  role: 'USER',
  createdAt: '2026-08-01T10:00:00',
}

const refreshedUser: CurrentUser = {
  ...currentUser,
  nickname: 'Latest Learner',
}

const authResponse: AuthResponse = {
  accessToken: 'access-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: currentUser,
}

const apiResponse = <T>(data: T) => ({
  data: {
    code: 'SUCCESS',
    message: 'success',
    data,
  },
})

describe('authStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
    mocks.loadSession.mockReturnValue({ accessToken: null, currentUser: null })
  })

  it('registers and saves the returned Token and user', async () => {
    const payload: RegisterPayload = {
      email: 'learner@example.com',
      password: 'safe-password',
      nickname: 'Learner',
    }
    mocks.register.mockResolvedValue(apiResponse(authResponse))
    const store = useAuthStore()

    await store.register(payload)

    expect(mocks.register).toHaveBeenCalledWith(payload)
    expect(mocks.saveSession).toHaveBeenCalledWith('access-token', currentUser)
    expect(store.accessToken).toBe('access-token')
    expect(store.currentUser).toEqual(currentUser)
    expect(store.isAuthenticated).toBe(true)
  })

  it('logs in and saves the returned Token and user', async () => {
    const payload: LoginPayload = {
      email: 'learner@example.com',
      password: 'safe-password',
    }
    mocks.login.mockResolvedValue(apiResponse(authResponse))
    const store = useAuthStore()

    await store.login(payload)

    expect(mocks.login).toHaveBeenCalledWith(payload)
    expect(mocks.saveSession).toHaveBeenCalledWith('access-token', currentUser)
    expect(store.accessToken).toBe('access-token')
    expect(store.currentUser).toEqual(currentUser)
    expect(store.isAuthenticated).toBe(true)
  })

  it('restores the Token and validates the latest user with /users/me', async () => {
    mocks.loadSession.mockReturnValue({
      accessToken: 'restored-token',
      currentUser,
    })
    mocks.getCurrentUser.mockResolvedValue(apiResponse(refreshedUser))
    const store = useAuthStore()

    await store.restoreSession()

    expect(mocks.loadSession).toHaveBeenCalledOnce()
    expect(mocks.getCurrentUser).toHaveBeenCalledOnce()
    expect(mocks.saveSession).toHaveBeenCalledWith('restored-token', refreshedUser)
    expect(store.accessToken).toBe('restored-token')
    expect(store.currentUser).toEqual(refreshedUser)
    expect(store.isAuthenticated).toBe(true)
  })

  it('clears provisional state when a restored Token is invalid', async () => {
    mocks.loadSession.mockReturnValue({
      accessToken: 'invalid-token',
      currentUser,
    })
    mocks.getCurrentUser.mockRejectedValue(new Error('unauthorized'))
    const store = useAuthStore()

    await expect(store.restoreSession()).resolves.toBeUndefined()

    expect(mocks.clearSession).toHaveBeenCalledOnce()
    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.initialized).toBe(true)
  })

  it('updates the nickname in both store state and session', async () => {
    mocks.login.mockResolvedValue(apiResponse(authResponse))
    mocks.updateNickname.mockResolvedValue(apiResponse(refreshedUser))
    const store = useAuthStore()
    await store.login({ email: 'learner@example.com', password: 'safe-password' })
    mocks.saveSession.mockClear()

    await store.updateNickname('Latest Learner')

    expect(mocks.updateNickname).toHaveBeenCalledWith('Latest Learner')
    expect(mocks.saveSession).toHaveBeenCalledWith('access-token', refreshedUser)
    expect(store.currentUser).toEqual(refreshedUser)
  })

  it('logs out locally by clearing store state and the saved session', async () => {
    mocks.login.mockResolvedValue(apiResponse(authResponse))
    const store = useAuthStore()
    await store.login({ email: 'learner@example.com', password: 'safe-password' })

    store.logout()

    expect(mocks.clearSession).toHaveBeenCalledOnce()
    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('marks initialization complete only after restoration finishes', async () => {
    let finishValidation: ((value: ReturnType<typeof apiResponse<CurrentUser>>) => void) | undefined
    mocks.loadSession.mockReturnValue({
      accessToken: 'restored-token',
      currentUser,
    })
    mocks.getCurrentUser.mockReturnValue(
      new Promise((resolve) => {
        finishValidation = resolve
      }),
    )
    const store = useAuthStore()

    const restoration = store.restoreSession()

    expect(store.initialized).toBe(false)
    finishValidation?.(apiResponse(refreshedUser))
    await restoration
    expect(store.initialized).toBe(true)
  })

  it('finishes initialization without calling /users/me when no Token is saved', async () => {
    const store = useAuthStore()

    await store.restoreSession()

    expect(mocks.loadSession).toHaveBeenCalledOnce()
    expect(mocks.getCurrentUser).not.toHaveBeenCalled()
    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.initialized).toBe(true)
  })

  it('clears existing in-memory authentication when registration cannot save the new session', async () => {
    const persistenceError = new Error('session persistence failed')
    mocks.register.mockResolvedValue(apiResponse(authResponse))
    mocks.saveSession.mockImplementationOnce(() => {
      throw persistenceError
    })
    const store = useAuthStore()
    store.$patch({ accessToken: 'existing-token', currentUser })

    await expect(
      store.register({
        email: 'learner@example.com',
        password: 'safe-password',
        nickname: 'Learner',
      }),
    ).rejects.toBe(persistenceError)

    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('clears existing in-memory authentication when login cannot save the new session', async () => {
    const persistenceError = new Error('session persistence failed')
    mocks.login.mockResolvedValue(apiResponse(authResponse))
    mocks.saveSession.mockImplementationOnce(() => {
      throw persistenceError
    })
    const store = useAuthStore()
    store.$patch({ accessToken: 'existing-token', currentUser })

    await expect(
      store.login({ email: 'learner@example.com', password: 'safe-password' }),
    ).rejects.toBe(persistenceError)

    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('clears existing in-memory authentication when a refreshed user cannot be saved', async () => {
    const persistenceError = new Error('session persistence failed')
    mocks.getCurrentUser.mockResolvedValue(apiResponse(refreshedUser))
    mocks.saveSession.mockImplementationOnce(() => {
      throw persistenceError
    })
    const store = useAuthStore()
    store.$patch({ accessToken: 'existing-token', currentUser })

    await expect(store.loadCurrentUser()).rejects.toBe(persistenceError)

    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('clears existing in-memory authentication when a nickname update cannot be saved', async () => {
    const persistenceError = new Error('session persistence failed')
    mocks.updateNickname.mockResolvedValue(apiResponse(refreshedUser))
    mocks.saveSession.mockImplementationOnce(() => {
      throw persistenceError
    })
    const store = useAuthStore()
    store.$patch({ accessToken: 'existing-token', currentUser })

    await expect(store.updateNickname('Latest Learner')).rejects.toBe(persistenceError)

    expect(store.accessToken).toBeNull()
    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })
})
