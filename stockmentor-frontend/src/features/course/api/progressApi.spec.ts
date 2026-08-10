import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
}))

vi.mock('../../../api/http', () => ({
  default: {
    get: mocks.get,
    put: mocks.put,
  },
}))

import { completeLesson, getCourseProgress } from './progressApi'

describe('progressApi', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('uses current-user relative paths without sending a user ID or completion body', () => {
    const progressResponse = Promise.resolve({ data: 'progress' })
    const completionResponse = Promise.resolve({ data: 'completion' })
    mocks.get.mockReturnValue(progressResponse)
    mocks.put.mockReturnValue(completionResponse)

    expect(getCourseProgress(7)).toBe(progressResponse)
    expect(completeLesson(101)).toBe(completionResponse)
    expect(mocks.get).toHaveBeenCalledWith('/me/courses/7/progress')
    expect(mocks.put).toHaveBeenCalledWith('/me/lessons/101/completion')
  })
})
