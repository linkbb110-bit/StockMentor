import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}))

vi.mock('../../../api/http', () => ({
  default: {
    get: mocks.get,
  },
}))

import { getCourse, getLesson, listCourses } from './courseApi'

describe('courseApi', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('uses only the shared Axios base URL and relative public course paths', () => {
    const listResponse = Promise.resolve({ data: 'courses' })
    const detailResponse = Promise.resolve({ data: 'course' })
    const lessonResponse = Promise.resolve({ data: 'lesson' })
    mocks.get
      .mockReturnValueOnce(listResponse)
      .mockReturnValueOnce(detailResponse)
      .mockReturnValueOnce(lessonResponse)

    expect(listCourses()).toBe(listResponse)
    expect(getCourse(7)).toBe(detailResponse)
    expect(getLesson(101)).toBe(lessonResponse)
    expect(mocks.get.mock.calls).toEqual([
      ['/courses'],
      ['/courses/7'],
      ['/lessons/101'],
    ])
  })
})
