import axios from 'axios'

import { authSession } from '../features/auth/session/authSession'

type UnauthorizedHandler = () => unknown

interface AuthenticationFailureActions {
  clearAuthentication: () => void
  currentPath: () => string
  isInitialized: () => boolean
  redirectToLogin: () => unknown
}

let unauthorizedHandler: UnauthorizedHandler = () => authSession.clear()
let unauthorizedHandling: Promise<void> | null = null

export const configureUnauthorizedHandler = (handler: UnauthorizedHandler): void => {
  unauthorizedHandler = handler
}

export const createAuthenticationFailureHandler = (
  actions: AuthenticationFailureActions,
): UnauthorizedHandler =>
  () => {
    actions.clearAuthentication()

    if (!actions.isInitialized() || actions.currentPath() === '/login') {
      return
    }

    return actions.redirectToLogin()
  }

const requestPath = (url: unknown): string => {
  if (typeof url !== 'string') {
    return ''
  }

  return url.split(/[?#]/, 1)[0] ?? ''
}

const publicErrorCode = (data: unknown): unknown => {
  if (typeof data !== 'object' || data === null || !('code' in data)) {
    return undefined
  }

  return data.code
}

const isLoginInvalidCredentialsError = (error: unknown): boolean =>
  axios.isAxiosError(error) &&
  requestPath(error.config?.url) === '/auth/login' &&
  publicErrorCode(error.response?.data) === 'AUTH_INVALID_CREDENTIALS'

const startUnauthorizedHandling = (): void => {
  if (unauthorizedHandling) {
    return
  }

  unauthorizedHandling = Promise.resolve()

  let result: unknown
  try {
    result = unauthorizedHandler()
  } catch {
    result = undefined
  }

  const currentHandling = Promise.resolve(result).then(
    () => undefined,
    () => undefined,
  )
  unauthorizedHandling = currentHandling
  void currentHandling.finally(() => {
    if (unauthorizedHandling === currentHandling) {
      unauthorizedHandling = null
    }
  })
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10_000,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const { accessToken } = authSession.load()
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !isLoginInvalidCredentialsError(error)
    ) {
      startUnauthorizedHandling()
    }

    return Promise.reject(error)
  },
)

export default http
