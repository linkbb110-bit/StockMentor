import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}))

vi.mock('../../../api/http', () => ({
  default: {
    get: mocks.get,
    post: mocks.post,
  },
}))

import {
  getQuiz,
  listWrongQuestions,
  reviewWrongQuestion,
  submitQuiz,
} from './quizApi'

describe('quizApi', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('uses the shared Axios base URL and the exact public Quiz path', () => {
    const response = Promise.resolve({ data: 'quiz' })
    mocks.get.mockReturnValue(response)

    expect(getQuiz(101)).toBe(response)
    expect(mocks.get).toHaveBeenCalledWith('/lessons/101/quiz')
  })

  it('submits a complete attempt to the current-user endpoint', () => {
    const response = Promise.resolve({ data: 'attempt' })
    const payload = {
      answers: [
        { questionId: 1001, selectedOptionIds: [11] },
        { questionId: 1002, selectedOptionIds: [21, 23] },
      ],
    }
    mocks.post.mockReturnValue(response)

    expect(submitQuiz(501, payload)).toBe(response)
    expect(mocks.post).toHaveBeenCalledWith('/me/quizzes/501/attempts', payload)
  })

  it('loads and reviews wrong questions through user-scoped relative paths', () => {
    const listResponse = Promise.resolve({ data: 'wrong-list' })
    const reviewResponse = Promise.resolve({ data: 'review' })
    mocks.get.mockReturnValue(listResponse)
    mocks.post.mockReturnValue(reviewResponse)

    expect(listWrongQuestions('MASTERED')).toBe(listResponse)
    expect(reviewWrongQuestion(1001, { selectedOptionIds: [12] })).toBe(reviewResponse)
    expect(mocks.get).toHaveBeenCalledWith('/me/wrong-questions', {
      params: { status: 'MASTERED' },
    })
    expect(mocks.post).toHaveBeenCalledWith('/me/wrong-questions/1001/answer', {
      selectedOptionIds: [12],
    })
  })
})
