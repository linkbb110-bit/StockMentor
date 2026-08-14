import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Quiz, QuizAttemptResult } from '../types/quiz'

const mocks = vi.hoisted(() => ({
  getQuiz: vi.fn(),
  submitQuiz: vi.fn(),
  isAuthenticated: false,
}))

vi.mock('../api/quizApi', () => ({
  getQuiz: mocks.getQuiz,
  submitQuiz: mocks.submitQuiz,
}))

vi.mock('../../auth/stores/authStore', () => ({
  useAuthStore: () => ({
    get isAuthenticated() {
      return mocks.isAuthenticated
    },
  }),
}))

import QuizView from './QuizView.vue'

const apiResponse = <T>(data: T) => ({
  data: { code: 'SUCCESS', message: '操作成功', data },
})

const quizOne: Quiz = {
  id: 501,
  lessonId: 101,
  lessonTitle: '股票是什么',
  courseId: 7,
  courseTitle: '股票投资基础',
  chapterId: 11,
  chapterTitle: '市场与资产',
  title: '股票基础课后测验',
  summary: '用三道题检查本课理解。',
  questions: [
    {
      questionId: 1001,
      type: 'SINGLE_CHOICE',
      stem: '股票通常代表什么？',
      options: [
        { optionId: 11, optionKey: 'A', content: '企业所有权的一部分' },
        { optionId: 12, optionKey: 'B', content: '固定利息存款' },
      ],
    },
    {
      questionId: 1002,
      type: 'MULTIPLE_CHOICE',
      stem: '哪些属于基础风险？',
      options: [
        { optionId: 21, optionKey: 'A', content: '价格波动' },
        { optionId: 22, optionKey: 'B', content: '经营变化' },
        { optionId: 23, optionKey: 'C', content: '保证收益' },
      ],
    },
    {
      questionId: 1003,
      type: 'TRUE_FALSE',
      stem: '股票收益始终有保证。',
      options: [
        { optionId: 31, optionKey: 'T', content: '正确' },
        { optionId: 32, optionKey: 'F', content: '错误' },
      ],
    },
  ],
}

const quizTwo: Quiz = {
  ...quizOne,
  id: 502,
  lessonId: 102,
  lessonTitle: '指数是什么',
  title: '指数课后测验',
  summary: '检查指数基础。',
  questions: [
    {
      questionId: 2001,
      type: 'SINGLE_CHOICE',
      stem: '指数的主要用途是什么？',
      options: [
        { optionId: 41, optionKey: 'A', content: '反映一组资产的变化' },
        { optionId: 42, optionKey: 'B', content: '保证单只股票上涨' },
      ],
    },
  ],
}

const incorrectResult: QuizAttemptResult = {
  attemptId: 9001,
  totalQuestions: 3,
  correctCount: 2,
  scorePercent: 17,
  results: [
    {
      questionId: 1001,
      correct: true,
      selectedOptionIds: [11],
      correctOptionIds: [11],
      explanation: '股票代表企业所有权的一部分。',
    },
    {
      questionId: 1002,
      correct: false,
      selectedOptionIds: [21],
      correctOptionIds: [21, 22],
      explanation: '价格和经营状况都可能变化。',
    },
    {
      questionId: 1003,
      correct: true,
      selectedOptionIds: [32],
      correctOptionIds: [32],
      explanation: '投资收益并不受保证。',
    },
  ],
}

const allCorrectResult: QuizAttemptResult = {
  ...incorrectResult,
  attemptId: 9002,
  correctCount: 3,
  scorePercent: 100,
  results: incorrectResult.results.map((result) => ({ ...result, correct: true })),
}

const routerHost = defineComponent({
  components: { RouterView },
  template: '<RouterView />',
})

const mountQuizAt = async (path = '/lessons/101/quiz') => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/lessons/:lessonId/quiz', component: QuizView },
      { path: '/lessons/:lessonId', component: { template: '<div>课时页</div>' } },
      { path: '/login', component: { template: '<div>登录页</div>' } },
      { path: '/wrong-questions', component: { template: '<div>错题本</div>' } },
    ],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(routerHost, { global: { plugins: [router] } })
  return { router, wrapper }
}

const selectCompleteAnswers = async (wrapper: Awaited<ReturnType<typeof mountQuizAt>>['wrapper']) => {
  const questions = wrapper.findAll('[data-testid="quiz-question"]')
  await questions[0]?.find('input[value="11"]').setValue(true)
  await questions[1]?.find('input[value="21"]').setValue(true)
  await questions[1]?.find('input[value="22"]').setValue(true)
  await questions[2]?.find('input[value="32"]').setValue(true)
}

describe('QuizView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.isAuthenticated = false
    mocks.getQuiz.mockResolvedValue(apiResponse(quizOne))
  })

  it('shows loading, then renders public SINGLE, MULTIPLE and TRUE_FALSE questions', async () => {
    let finishLoad: ((value: ReturnType<typeof apiResponse<Quiz>>) => void) | undefined
    mocks.getQuiz.mockReturnValue(new Promise((resolve) => { finishLoad = resolve }))
    const { wrapper } = await mountQuizAt()

    expect(wrapper.get('[data-testid="quiz-loading"]').text()).toContain('加载')
    finishLoad?.(apiResponse(quizOne))
    await flushPromises()

    expect(mocks.getQuiz).toHaveBeenCalledWith(101)
    expect(wrapper.get('h1').text()).toBe(quizOne.title)
    expect(wrapper.findAll('[data-testid="quiz-question"]')).toHaveLength(3)
    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(4)
    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(3)
    expect(wrapper.text()).not.toContain(incorrectResult.results[0]?.explanation)
  })

  it('does not submit incomplete answers and displays a useful validation message', async () => {
    mocks.isAuthenticated = true
    const { wrapper } = await mountQuizAt()
    await flushPromises()

    await wrapper.get('form').trigger('submit')

    expect(mocks.submitQuiz).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('完成所有题目')
  })

  it('lets anonymous users answer but enters login without calling the private API', async () => {
    const { router, wrapper } = await mountQuizAt()
    await flushPromises()
    await selectCompleteAnswers(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.submitQuiz).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('submits exact selected IDs once, disables controls, and renders only backend results', async () => {
    mocks.isAuthenticated = true
    let finishSubmit: ((value: ReturnType<typeof apiResponse<QuizAttemptResult>>) => void) | undefined
    mocks.submitQuiz.mockReturnValue(new Promise((resolve) => { finishSubmit = resolve }))
    const { wrapper } = await mountQuizAt()
    await flushPromises()
    await selectCompleteAnswers(wrapper)

    const submit = wrapper.get('[data-testid="quiz-submit"]')
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(mocks.submitQuiz).toHaveBeenCalledOnce()
    expect(mocks.submitQuiz).toHaveBeenCalledWith(501, {
      answers: [
        { questionId: 1001, selectedOptionIds: [11] },
        { questionId: 1002, selectedOptionIds: [21, 22] },
        { questionId: 1003, selectedOptionIds: [32] },
      ],
    })
    expect(submit.attributes('disabled')).toBeDefined()
    expect(wrapper.findAll('input').every((input) => input.attributes('disabled') !== undefined))
      .toBe(true)
    expect(wrapper.find('[data-testid="quiz-result"]').exists()).toBe(false)

    finishSubmit?.(apiResponse(incorrectResult))
    await flushPromises()

    expect(wrapper.get('[data-testid="quiz-score"]').text()).toContain('17%')
    expect(wrapper.get('[data-testid="quiz-score"]').text()).toContain('2 / 3')
    expect(wrapper.text()).toContain('回答错误')
    expect(wrapper.text()).toContain('价格和经营状况都可能变化。')
    expect(wrapper.text()).toContain('A. 价格波动')
    expect(wrapper.text()).toContain('B. 经营变化')
    expect(wrapper.get('a[href="/wrong-questions"]').text()).toContain('查看错题本')
  })

  it('shows all-correct feedback and retakes locally without refetching', async () => {
    mocks.isAuthenticated = true
    mocks.submitQuiz.mockResolvedValue(apiResponse(allCorrectResult))
    const { wrapper } = await mountQuizAt()
    await flushPromises()
    await selectCompleteAnswers(wrapper)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('全部答对')
    expect(wrapper.find('a[href="/wrong-questions"]').exists()).toBe(false)

    await wrapper.get('[data-testid="quiz-retake"]').trigger('click')

    expect(wrapper.find('[data-testid="quiz-result"]').exists()).toBe(false)
    expect(wrapper.findAll('input:checked')).toHaveLength(0)
    expect(mocks.getQuiz).toHaveBeenCalledOnce()
  })

  it('preserves selections and restores controls after submit failure', async () => {
    mocks.isAuthenticated = true
    mocks.submitQuiz.mockRejectedValue(new Error('private details'))
    const { wrapper } = await mountQuizAt()
    await flushPromises()
    await selectCompleteAnswers(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('提交失败')
    expect(wrapper.text()).not.toContain('private details')
    expect(wrapper.findAll('input:checked')).toHaveLength(4)
    expect(wrapper.get('[data-testid="quiz-submit"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="quiz-result"]').exists()).toBe(false)
  })

  it('shows load failure and retries the current lesson', async () => {
    mocks.getQuiz
      .mockRejectedValueOnce(new Error('network detail'))
      .mockResolvedValueOnce(apiResponse(quizOne))
    const { wrapper } = await mountQuizAt()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('测验加载失败')
    expect(wrapper.text()).not.toContain('network detail')
    await wrapper.get('[data-testid="quiz-retry"]').trigger('click')
    await flushPromises()

    expect(mocks.getQuiz).toHaveBeenCalledTimes(2)
    expect(wrapper.get('h1').text()).toBe(quizOne.title)
  })

  it('reloads the reused component on a route-param change and resets old state', async () => {
    mocks.isAuthenticated = true
    mocks.getQuiz.mockImplementation((lessonId: number) =>
      Promise.resolve(apiResponse(lessonId === 101 ? quizOne : quizTwo)),
    )
    mocks.submitQuiz.mockResolvedValue(apiResponse(incorrectResult))
    const { router, wrapper } = await mountQuizAt()
    await flushPromises()
    await selectCompleteAnswers(wrapper)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-testid="quiz-result"]').exists()).toBe(true)

    await router.push('/lessons/102/quiz')
    await flushPromises()

    expect(mocks.getQuiz).toHaveBeenNthCalledWith(2, 102)
    expect(wrapper.get('h1').text()).toBe(quizTwo.title)
    expect(wrapper.text()).toContain('指数的主要用途是什么？')
    expect(wrapper.text()).not.toContain(quizOne.title)
    expect(wrapper.findAll('input:checked')).toHaveLength(0)
    expect(wrapper.find('[data-testid="quiz-result"]').exists()).toBe(false)
  })

  it('ignores a stale GET response after a rapid route change', async () => {
    let finishFirst: ((value: ReturnType<typeof apiResponse<Quiz>>) => void) | undefined
    mocks.getQuiz.mockImplementation((lessonId: number) =>
      lessonId === 101
        ? new Promise((resolve) => { finishFirst = resolve })
        : Promise.resolve(apiResponse(quizTwo)),
    )
    const { router, wrapper } = await mountQuizAt()

    await router.push('/lessons/102/quiz')
    await flushPromises()
    finishFirst?.(apiResponse(quizOne))
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe(quizTwo.title)
    expect(wrapper.text()).not.toContain(quizOne.title)
  })

  it('ignores a stale submit response after navigation to another Quiz', async () => {
    mocks.isAuthenticated = true
    let finishSubmit: ((value: ReturnType<typeof apiResponse<QuizAttemptResult>>) => void) | undefined
    mocks.getQuiz.mockImplementation((lessonId: number) =>
      Promise.resolve(apiResponse(lessonId === 101 ? quizOne : quizTwo)),
    )
    mocks.submitQuiz.mockReturnValue(new Promise((resolve) => { finishSubmit = resolve }))
    const { router, wrapper } = await mountQuizAt()
    await flushPromises()
    await selectCompleteAnswers(wrapper)
    await wrapper.get('form').trigger('submit')

    await router.push('/lessons/102/quiz')
    await flushPromises()
    finishSubmit?.(apiResponse(incorrectResult))
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe(quizTwo.title)
    expect(wrapper.find('[data-testid="quiz-result"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('17%')
  })
})
