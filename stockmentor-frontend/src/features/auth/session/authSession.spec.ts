import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authSession } from './authSession'
import type { CurrentUser } from '../types/auth'

const ACCESS_TOKEN_KEY = 'stockmentor.accessToken'
const CURRENT_USER_KEY = 'stockmentor.currentUser'

const currentUser: CurrentUser = {
  id: 42,
  email: 'learner@example.com',
  nickname: 'Learner',
  role: 'USER',
  createdAt: '2026-08-01T10:00:00',
}

describe('authSession', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    sessionStorage.clear()
    localStorage.clear()
  })

  it('saves exactly the access Token and current user session keys', () => {
    authSession.save('access-token', currentUser)

    expect(sessionStorage.length).toBe(2)
    expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('access-token')
    expect(sessionStorage.getItem(CURRENT_USER_KEY)).toBe(JSON.stringify(currentUser))
  })

  it('does not persist fields outside the CurrentUser contract', () => {
    const userWithUnexpectedFields = {
      ...currentUser,
      password: 'must-not-be-stored',
      status: 'ACTIVE',
    }

    authSession.save('access-token', userWithUnexpectedFields)

    expect(JSON.parse(sessionStorage.getItem(CURRENT_USER_KEY) ?? '{}')).toEqual(currentUser)
  })

  it('loads a valid saved session', () => {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, 'access-token')
    sessionStorage.setItem(CURRENT_USER_KEY, JSON.stringify(currentUser))

    expect(authSession.load()).toEqual({
      accessToken: 'access-token',
      currentUser,
    })
  })

  it('clears both session values when current user JSON is malformed', () => {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, 'access-token')
    sessionStorage.setItem(CURRENT_USER_KEY, '{malformed-json')

    expect(authSession.load()).toEqual({
      accessToken: null,
      currentUser: null,
    })
    expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(sessionStorage.getItem(CURRENT_USER_KEY)).toBeNull()
  })

  it('clears both auth keys without removing unrelated session data', () => {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, 'access-token')
    sessionStorage.setItem(CURRENT_USER_KEY, JSON.stringify(currentUser))
    sessionStorage.setItem('unrelated', 'preserved')

    authSession.clear()

    expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(sessionStorage.getItem(CURRENT_USER_KEY)).toBeNull()
    expect(sessionStorage.getItem('unrelated')).toBe('preserved')
  })

  it('never reads from or writes to localStorage', () => {
    const getItem = vi.spyOn(localStorage, 'getItem')
    const setItem = vi.spyOn(localStorage, 'setItem')
    const removeItem = vi.spyOn(localStorage, 'removeItem')
    const clear = vi.spyOn(localStorage, 'clear')

    authSession.save('access-token', currentUser)
    authSession.load()
    authSession.clear()

    expect(getItem).not.toHaveBeenCalled()
    expect(setItem).not.toHaveBeenCalled()
    expect(removeItem).not.toHaveBeenCalled()
    expect(clear).not.toHaveBeenCalled()
  })
})
