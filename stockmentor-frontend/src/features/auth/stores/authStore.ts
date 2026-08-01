import { defineStore } from 'pinia'

import { login as loginRequest, register as registerRequest } from '../api/authApi'
import { authSession } from '../session/authSession'
import type { CurrentUser, LoginPayload, RegisterPayload } from '../types/auth'
import {
  getCurrentUser as getCurrentUserRequest,
  updateNickname as updateNicknameRequest,
} from '../../profile/api/profileApi'

interface AuthenticationState {
  accessToken: string | null
  currentUser: CurrentUser | null
}

const saveAuthentication = (
  state: AuthenticationState,
  accessToken: string,
  currentUser: CurrentUser,
): void => {
  try {
    authSession.save(accessToken, currentUser)
  } catch (error: unknown) {
    state.accessToken = null
    state.currentUser = null
    throw error
  }

  state.accessToken = accessToken
  state.currentUser = currentUser
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: null as string | null,
    currentUser: null as CurrentUser | null,
    initialized: false,
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.currentUser),
  },

  actions: {
    async register(payload: RegisterPayload): Promise<void> {
      const response = await registerRequest(payload)
      const { accessToken, user } = response.data.data

      saveAuthentication(this, accessToken, user)
    },

    async login(payload: LoginPayload): Promise<void> {
      const response = await loginRequest(payload)
      const { accessToken, user } = response.data.data

      saveAuthentication(this, accessToken, user)
    },

    async restoreSession(): Promise<void> {
      try {
        const { accessToken } = authSession.load()
        if (!accessToken) {
          this.clearSession()
          return
        }

        this.accessToken = accessToken
        this.currentUser = null
        await this.loadCurrentUser()
      } catch {
        this.clearSession()
      } finally {
        this.initialized = true
      }
    },

    async loadCurrentUser(): Promise<CurrentUser> {
      const accessToken = this.accessToken
      if (!accessToken) {
        this.clearSession()
        throw new Error('An access Token is required to load the current user')
      }

      const response = await getCurrentUserRequest()
      const currentUser = response.data.data

      saveAuthentication(this, accessToken, currentUser)
      return currentUser
    },

    async updateNickname(nickname: string): Promise<CurrentUser> {
      const accessToken = this.accessToken
      if (!accessToken) {
        this.clearSession()
        throw new Error('An access Token is required to update the nickname')
      }

      const response = await updateNicknameRequest(nickname)
      const currentUser = response.data.data

      saveAuthentication(this, accessToken, currentUser)
      return currentUser
    },

    logout(): void {
      this.clearSession()
    },

    clearSession(): void {
      this.accessToken = null
      this.currentUser = null
      authSession.clear()
    },
  },
})
