import {
  AxiosError,
  AxiosHeaders,
  type AxiosAdapter,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { ApiResponse } from '../types/api'

const mocks = vi.hoisted(() => ({
  loadSession: vi.fn(),
  clearSession: vi.fn(),
  clearStore: vi.fn(),
  replace: vi.fn(),
  currentPath: '/dashboard',
  initialized: true,
  currentAccessToken: null as string | null,
  authenticationGeneration: 0,
}))

vi.mock('../features/auth/session/authSession', () => ({
  authSession: {
    load: mocks.loadSession,
    clear: mocks.clearSession,
  },
}))

import http, {
  configureUnauthorizedHandler,
  createAuthenticationFailureHandler,
} from './http'

interface AuthenticationOwnership {
  accessToken: string | null
  generation: number
}

const configureWithOwnership = configureUnauthorizedHandler as unknown as (
  handler: (ownership: AuthenticationOwnership) => unknown,
  currentAuthentication: () => AuthenticationOwnership,
) => void

const okAdapter = (): AxiosAdapter => async (config) => ({
  config,
  data: {},
  headers: new AxiosHeaders(),
  status: 200,
  statusText: 'OK',
})

const unauthorizedError = (
  requestUrl: string,
  code: string,
): AxiosError<ApiResponse<null>> => {
  const config = {
    headers: new AxiosHeaders(),
    url: requestUrl,
  } as InternalAxiosRequestConfig
  const response: AxiosResponse<ApiResponse<null>> = {
    config,
    data: { code, message: 'authentication failed', data: null },
    headers: new AxiosHeaders(),
    status: 401,
    statusText: 'Unauthorized',
  }

  return new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, response)
}

const attachRequestConfig = (
  error: AxiosError<ApiResponse<null>>,
  config: InternalAxiosRequestConfig,
): void => {
  error.config = config
  if (error.response) {
    error.response.config = config
  }
}

const rejectFromAdapter = (error: AxiosError<ApiResponse<null>>): AxiosAdapter =>
  async (config) => {
    attachRequestConfig(error, config)
    return Promise.reject(error)
  }

const deferredRejectionAdapter = (error: AxiosError<ApiResponse<null>>) => {
  let rejectRequest: () => void = () => undefined
  let markStarted: () => void = () => undefined
  const started = new Promise<void>((resolve) => {
    markStarted = resolve
  })
  const adapter: AxiosAdapter = async (config) => {
    attachRequestConfig(error, config)
    markStarted()
    return new Promise((_, reject) => {
      rejectRequest = () => reject(error)
    })
  }

  return { adapter, started, reject: () => rejectRequest() }
}

const rejectionFrom = async (request: Promise<unknown>): Promise<unknown> =>
  request.then(
    () => new Error('expected request to reject'),
    (error: unknown) => error,
  )

describe('authentication HTTP interceptors', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.currentPath = '/dashboard'
    mocks.initialized = true
    mocks.currentAccessToken = null
    mocks.authenticationGeneration = 0
    mocks.loadSession.mockReturnValue({ accessToken: null, currentUser: null })
    mocks.clearStore.mockImplementation(() => mocks.clearSession())
    mocks.replace.mockResolvedValue(undefined)
    configureWithOwnership(
      createAuthenticationFailureHandler({
        clearAuthentication: mocks.clearStore,
        currentPath: () => mocks.currentPath,
        isInitialized: () => mocks.initialized,
        redirectToLogin: () => mocks.replace('/login'),
      }),
      () => ({
        accessToken: mocks.currentAccessToken,
        generation: mocks.authenticationGeneration,
      }),
    )
  })

  it('adds the saved access Token as a Bearer request header', async () => {
    mocks.loadSession.mockReturnValue({ accessToken: 'access-token', currentUser: null })
    const adapter = vi.fn(okAdapter())

    await http.get('/users/me', { adapter })

    const config = adapter.mock.calls[0]?.[0]
    expect(config?.headers.get('Authorization')).toBe('Bearer access-token')
  })

  it('does not add an Authorization header when no Token is saved', async () => {
    const adapter = vi.fn(okAdapter())

    await http.get('/system/health', { adapter })

    const config = adapter.mock.calls[0]?.[0]
    expect(config?.headers.has('Authorization')).toBe(false)
  })

  it('lets a login invalid-credentials 401 reach the login page without clearing or redirecting', async () => {
    const originalError = unauthorizedError('/auth/login', 'AUTH_INVALID_CREDENTIALS')

    const rejection = await rejectionFrom(
      http.post('/auth/login', {}, { adapter: rejectFromAdapter(originalError) }),
    )

    expect(rejection).toBe(originalError)
    expect(mocks.clearStore).not.toHaveBeenCalled()
    expect(mocks.clearSession).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
  })

  it.each([
    ['/users/me', 'AUTH_INVALID_CREDENTIALS'],
    ['/users/auth/login', 'AUTH_INVALID_CREDENTIALS'],
    ['/auth/login', 'AUTH_INVALID_TOKEN'],
  ])(
    'requires both the login request URL and public invalid-credentials code for the 401 exception',
    async (requestUrl, code) => {
      const originalError = unauthorizedError(requestUrl, code)

      const rejection = await rejectionFrom(
        http.get(requestUrl, { adapter: rejectFromAdapter(originalError) }),
      )

      expect(rejection).toBe(originalError)
      expect(mocks.clearStore).toHaveBeenCalledOnce()
      expect(mocks.clearSession).toHaveBeenCalledOnce()
      expect(mocks.replace).toHaveBeenCalledOnce()
      expect(mocks.replace).toHaveBeenCalledWith('/login')
    },
  )

  it('clears the session and Pinia state then redirects for an expired authenticated request', async () => {
    const originalError = unauthorizedError('/users/me', 'AUTH_TOKEN_EXPIRED')

    const rejection = await rejectionFrom(
      http.get('/users/me', { adapter: rejectFromAdapter(originalError) }),
    )

    expect(rejection).toBe(originalError)
    expect(mocks.clearStore).toHaveBeenCalledOnce()
    expect(mocks.clearSession).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
  })

  it('clears initial invalid authentication synchronously without starting a competing navigation', async () => {
    mocks.initialized = false
    const originalError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')

    const rejection = await rejectionFrom(
      http.get('/users/me', { adapter: rejectFromAdapter(originalError) }),
    )

    expect(rejection).toBe(originalError)
    expect(mocks.clearStore).toHaveBeenCalledOnce()
    expect(mocks.clearSession).toHaveBeenCalledOnce()
    expect(mocks.replace).not.toHaveBeenCalled()
  })

  it('clears authentication but does not redirect repeatedly when already on the login page', async () => {
    mocks.currentPath = '/login'
    const originalError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')

    const rejection = await rejectionFrom(
      http.get('/users/me', { adapter: rejectFromAdapter(originalError) }),
    )

    expect(rejection).toBe(originalError)
    expect(mocks.clearStore).toHaveBeenCalledOnce()
    expect(mocks.clearSession).toHaveBeenCalledOnce()
    expect(mocks.replace).not.toHaveBeenCalled()
  })

  it('coalesces concurrent 401 handling into one clear and one redirect', async () => {
    mocks.currentAccessToken = 'current-token'
    mocks.authenticationGeneration = 4
    mocks.loadSession.mockReturnValue({ accessToken: 'current-token', currentUser: null })
    let finishRedirect: (() => void) | undefined
    mocks.replace.mockReturnValue(
      new Promise<void>((resolve) => {
        finishRedirect = resolve
      }),
    )
    const firstError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')
    const secondError = unauthorizedError('/users/me/nickname', 'AUTH_TOKEN_EXPIRED')

    const firstRequest = http.get('/users/me', { adapter: rejectFromAdapter(firstError) })
    const secondRequest = http.get('/users/me/nickname', {
      adapter: rejectFromAdapter(secondError),
    })
    const results = await Promise.allSettled([firstRequest, secondRequest])

    expect(results).toEqual([
      { status: 'rejected', reason: firstError },
      { status: 'rejected', reason: secondError },
    ])
    expect(mocks.clearStore).toHaveBeenCalledOnce()
    expect(mocks.clearSession).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledOnce()
    finishRedirect?.()
  })

  it('ignores a delayed 401 from an old Authorization Token after a newer session starts', async () => {
    mocks.currentAccessToken = 'old-token'
    mocks.authenticationGeneration = 7
    mocks.loadSession.mockReturnValue({ accessToken: 'old-token', currentUser: null })
    const originalError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')
    const pending = deferredRejectionAdapter(originalError)

    const request = http.get('/users/me', { adapter: pending.adapter })
    await pending.started
    mocks.currentAccessToken = 'new-token'
    mocks.authenticationGeneration = 8
    mocks.loadSession.mockReturnValue({ accessToken: 'new-token', currentUser: null })
    pending.reject()

    await expect(rejectionFrom(request)).resolves.toBe(originalError)
    expect(mocks.clearStore).not.toHaveBeenCalled()
    expect(mocks.clearSession).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
  })

  it('ignores a delayed 401 when the same Token belongs to a newer generation', async () => {
    mocks.currentAccessToken = 'reused-token'
    mocks.authenticationGeneration = 11
    mocks.loadSession.mockReturnValue({ accessToken: 'reused-token', currentUser: null })
    const originalError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')
    const pending = deferredRejectionAdapter(originalError)

    const request = http.get('/users/me', { adapter: pending.adapter })
    await pending.started
    mocks.authenticationGeneration = 12
    pending.reject()

    await expect(rejectionFrom(request)).resolves.toBe(originalError)
    expect(mocks.clearStore).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
  })

  it('does not let pending old-session handling suppress a newer-session 401', async () => {
    let finishOldRedirect: (() => void) | undefined
    mocks.currentAccessToken = 'old-token'
    mocks.authenticationGeneration = 20
    mocks.loadSession.mockReturnValue({ accessToken: 'old-token', currentUser: null })
    mocks.replace.mockReturnValueOnce(
      new Promise<void>((resolve) => {
        finishOldRedirect = resolve
      }),
    )
    const oldError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')

    await expect(
      rejectionFrom(http.get('/users/me', { adapter: rejectFromAdapter(oldError) })),
    ).resolves.toBe(oldError)
    expect(mocks.clearStore).toHaveBeenCalledOnce()

    mocks.currentAccessToken = 'new-token'
    mocks.authenticationGeneration = 21
    mocks.loadSession.mockReturnValue({ accessToken: 'new-token', currentUser: null })
    const newError = unauthorizedError('/users/me', 'AUTH_TOKEN_EXPIRED')

    await expect(
      rejectionFrom(http.get('/users/me', { adapter: rejectFromAdapter(newError) })),
    ).resolves.toBe(newError)
    finishOldRedirect?.()
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    expect(mocks.clearStore).toHaveBeenCalledTimes(2)
    expect(mocks.replace).toHaveBeenCalledTimes(2)
  })

  it('rejects the original Axios error when the injected side-effect handler throws', async () => {
    const handlerFailure = new Error('handler failed')
    configureUnauthorizedHandler(() => {
      throw handlerFailure
    })
    const originalError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')

    const rejection = await rejectionFrom(
      http.get('/users/me', { adapter: rejectFromAdapter(originalError) }),
    )

    expect(rejection).toBe(originalError)
    expect(rejection).not.toBe(handlerFailure)
  })

  it('always rejects the original Axios error even when redirect handling fails', async () => {
    const redirectFailure = new Error('navigation failed')
    mocks.replace.mockRejectedValue(redirectFailure)
    const originalError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')

    const rejection = await rejectionFrom(
      http.get('/users/me', { adapter: rejectFromAdapter(originalError) }),
    )

    expect(rejection).toBe(originalError)
    expect(rejection).not.toBe(redirectFailure)

    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    mocks.replace.mockResolvedValue(undefined)
    const laterError = unauthorizedError('/users/me', 'AUTH_INVALID_TOKEN')
    const laterRejection = await rejectionFrom(
      http.get('/users/me', { adapter: rejectFromAdapter(laterError) }),
    )

    expect(laterRejection).toBe(laterError)
    expect(mocks.clearStore).toHaveBeenCalledTimes(2)
    expect(mocks.replace).toHaveBeenCalledTimes(2)
  })
})
