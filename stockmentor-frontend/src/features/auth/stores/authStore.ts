import { defineStore } from 'pinia'

import { login as loginRequest, register as registerRequest } from '../api/authApi'
import { authSession } from '../session/authSession'
import type {
  AuthenticationOwnership,
  CurrentUser,
  LoginPayload,
  RegisterPayload,
} from '../types/auth'
import {
  getCurrentUser as getCurrentUserRequest,
  updateNickname as updateNicknameRequest,
} from '../../profile/api/profileApi'

interface AuthenticationState {
  accessToken: string | null
  currentUser: CurrentUser | null
  authenticationGeneration: number
}

const ownsAuthentication = (
  state: AuthenticationState,
  ownership: AuthenticationOwnership,
): boolean =>
  state.authenticationGeneration === ownership.generation &&
  state.accessToken === ownership.accessToken

const saveAuthentication = (
  state: AuthenticationState,
  accessToken: string,
  currentUser: CurrentUser,
): void => {
  try {
    authSession.save(accessToken, currentUser)
  } catch (error: unknown) {
    state.authenticationGeneration += 1
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
    authenticationGeneration: 0,
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.currentUser),
    authenticationOwnership: (state): AuthenticationOwnership => ({
      accessToken: state.accessToken,
      generation: state.authenticationGeneration,
    }),
  },

  actions: {
    async register(payload: RegisterPayload): Promise<boolean> {
      const generation = ++this.authenticationGeneration
      const response = await registerRequest(payload)
      const { accessToken, user } = response.data.data

      if (this.authenticationGeneration !== generation) {
        return false
      }

      saveAuthentication(this, accessToken, user)
      return true
    },

    async login(payload: LoginPayload): Promise<boolean> {
      const generation = ++this.authenticationGeneration
      const response = await loginRequest(payload)
      const { accessToken, user } = response.data.data

      if (this.authenticationGeneration !== generation) {
        return false
      }

      saveAuthentication(this, accessToken, user)
      return true
    },

    async restoreSession(): Promise<void> {
      const generation = this.authenticationGeneration
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
        if (this.authenticationGeneration === generation) {
          this.clearSession()
        }
      } finally {
        this.initialized = true
      }
    },

    async loadCurrentUser(): Promise<CurrentUser | null> {
      const accessToken = this.accessToken
      if (!accessToken) {
        this.clearSession()
        throw new Error('An access Token is required to load the current user')
      }
      const ownership = {
        accessToken,
        generation: this.authenticationGeneration,
      }

      const response = await getCurrentUserRequest()
      const currentUser = response.data.data

      if (!ownsAuthentication(this, ownership)) {
        return null
      }

      saveAuthentication(this, accessToken, currentUser)
      return currentUser
    },

    async updateNickname(nickname: string): Promise<CurrentUser | null> {
      const accessToken = this.accessToken
      if (!accessToken) {
        this.clearSession()
        throw new Error('An access Token is required to update the nickname')
      }
      const ownership = {
        accessToken,
        generation: this.authenticationGeneration,
      }

      const response = await updateNicknameRequest(nickname)
      const currentUser = response.data.data

      if (!ownsAuthentication(this, ownership)) {
        return null
      }

      saveAuthentication(this, accessToken, currentUser)
      return currentUser
    },

    logout(): void {
      this.clearSession()
    },

    clearSession(): void {
      this.authenticationGeneration += 1
      this.accessToken = null
      this.currentUser = null
      authSession.clear()
    },

    clearSessionIfOwned(ownership: AuthenticationOwnership): boolean {
      if (!ownsAuthentication(this, ownership)) {
        return false
      }

      this.clearSession()
      return true
    },
  },
})
