import type { CurrentUser } from '../types/auth'

const ACCESS_TOKEN_KEY = 'stockmentor.accessToken'
const CURRENT_USER_KEY = 'stockmentor.currentUser'

const clearAuthValues = () => {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(CURRENT_USER_KEY)
}

const toStoredCurrentUser = (currentUser: CurrentUser): CurrentUser => ({
  id: currentUser.id,
  email: currentUser.email,
  nickname: currentUser.nickname,
  role: currentUser.role,
  createdAt: currentUser.createdAt,
})

export const authSession = {
  load(): { accessToken: string | null; currentUser: CurrentUser | null } {
    const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
    const storedCurrentUser = sessionStorage.getItem(CURRENT_USER_KEY)

    if (storedCurrentUser === null) {
      return { accessToken, currentUser: null }
    }

    try {
      return {
        accessToken,
        currentUser: JSON.parse(storedCurrentUser) as CurrentUser,
      }
    } catch {
      clearAuthValues()
      return { accessToken: null, currentUser: null }
    }
  },

  save(accessToken: string, currentUser: CurrentUser): void {
    const storedCurrentUser = JSON.stringify(toStoredCurrentUser(currentUser))

    try {
      sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
      sessionStorage.setItem(CURRENT_USER_KEY, storedCurrentUser)
    } catch (error: unknown) {
      clearAuthValues()
      throw error
    }
  },

  clear(): void {
    clearAuthValues()
  },
}
