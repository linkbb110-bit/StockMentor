import axios from 'axios'

import { authSession } from '../features/auth/session/authSession'
import type { AuthenticationOwnership } from '../features/auth/types/auth'

type UnauthorizedHandler = (ownership: AuthenticationOwnership) => unknown
type CurrentAuthentication = () => AuthenticationOwnership

interface AuthenticationFailureActions {
  clearAuthentication: (ownership: AuthenticationOwnership) => boolean | void
  currentPath: () => string
  isInitialized: () => boolean
  redirectToLogin: () => unknown
}

interface AuthenticationRequestConfig {
  stockmentorAuthentication?: AuthenticationOwnership
}

interface UnauthorizedHandling {
  ownership: AuthenticationOwnership
  promise: Promise<void>
}

let currentAuthentication: CurrentAuthentication = () => ({
  accessToken: authSession.load().accessToken,
  generation: 0,
})
let unauthorizedHandler: UnauthorizedHandler = (ownership) => {
  if (currentAuthentication().accessToken === ownership.accessToken) {
    authSession.clear()
  }
}
const unauthorizedHandlings: UnauthorizedHandling[] = []

export const configureUnauthorizedHandler = (
  handler: UnauthorizedHandler,
  authentication?: CurrentAuthentication,
): void => {
  unauthorizedHandler = handler
  if (authentication) {
    currentAuthentication = authentication
  }
}

export const createAuthenticationFailureHandler = (
  actions: AuthenticationFailureActions,
): UnauthorizedHandler =>
  (ownership) => {
    const cleared = actions.clearAuthentication(ownership)

    if (cleared === false) {
      return
    }

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

const sameOwnership = (
  left: AuthenticationOwnership,
  right: AuthenticationOwnership,
): boolean =>
  left.generation === right.generation && left.accessToken === right.accessToken

const startUnauthorizedHandling = (ownership: AuthenticationOwnership): void => {
  if (unauthorizedHandlings.some((handling) => sameOwnership(handling.ownership, ownership))) {
    return
  }

  let result: unknown
  try {
    result = unauthorizedHandler(ownership)
  } catch {
    result = undefined
  }

  const currentHandling = Promise.resolve(result).then(
    () => undefined,
    () => undefined,
  )
  const handling = { ownership, promise: currentHandling }
  unauthorizedHandlings.push(handling)
  void currentHandling.finally(() => {
    const index = unauthorizedHandlings.indexOf(handling)
    if (index >= 0) {
      unauthorizedHandlings.splice(index, 1)
    }
  })
}

const bearerToken = (authorization: unknown): string | null => {
  if (typeof authorization !== 'string') {
    return null
  }

  const match = /^Bearer (.+)$/.exec(authorization)
  return match?.[1] ?? null
}

const requestAuthentication = (error: unknown): AuthenticationOwnership | null => {
  if (!axios.isAxiosError(error)) {
    return null
  }

  return (
    (error.config as (typeof error.config & AuthenticationRequestConfig) | undefined)
      ?.stockmentorAuthentication ?? null
  )
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
  const ownership = currentAuthentication()
  const { accessToken } = authSession.load()
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }

  ;(config as typeof config & AuthenticationRequestConfig).stockmentorAuthentication = {
    accessToken: bearerToken(config.headers.get('Authorization')),
    generation: ownership.generation,
  }

  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const failedAuthentication = requestAuthentication(error)
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !isLoginInvalidCredentialsError(error) &&
      failedAuthentication !== null &&
      sameOwnership(failedAuthentication, currentAuthentication())
    ) {
      startUnauthorizedHandling(failedAuthentication)
    }

    return Promise.reject(error)
  },
)

export default http
