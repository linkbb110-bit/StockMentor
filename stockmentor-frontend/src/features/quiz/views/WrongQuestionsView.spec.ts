import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type {
  WrongQuestion,
  WrongQuestionReviewResult,
} from '../types/quiz'

const mocks = vi.hoisted(() => ({
  listWrongQuestions: vi.fn(),
  reviewWrongQuestion: vi.fn(),
}))

vi.mock('../api/quizApi', () => ({
  listWrongQuestions: mocks.listWrongQuestions,
  reviewWrongQuestion: mocks.reviewWrongQuestion,
}))

import WrongQuestionsView from './WrongQuestionsView.vue'

const apiResponse = <T>(data: T) => ({
  data: { code: 'SUCCESS', message: '操作成功', data },
})

const pendingQuestions: WrongQuestion[] = [
  {
    questionId: 1001,
    lessonId: 101,
    lessonTitle: '股票是什么',
    type: 'SINGLE_CHOICE',
    stem: '股票通常代表什么？',
    options: [
      { optionId: 11, optionKey: 'A', content: '企业所有权的一部分' },
      { optionId: 12, optionKey: 'B', content: '固定利息存款' },
    ],
    status: 'PENDING',
    errorCount: 2,
    lastWrongAt: '2026-08-14T09:30:00',
    masteredAt: null,
  },
  {
    questionId: 1002,
    lessonId: 101,
    lessonTitle: '股票是什么',
    type: 'MULTIPLE_CHOICE',
    stem: '哪些属于基础风险？',
    options: [
      { optionId: 21, optionKey: 'A', content: '价格波动' },
      { optionId: 22, optionKey: 'B', content: '经营变化' },
      { optionId: 23, optionKey: 'C', content: '保证收益' },
    ],
    status: 'PENDING',
    errorCount: 1,
    lastWrongAt: '2026-08-14T09:20:00',
    masteredAt: null,
  },
  {
    questionId: 1003,
    lessonId: 101,
    lessonTitle: '股票是什么',
    type: 'TRUE_FALSE',
    stem: '股票收益始终有保证。',
    options: [
      { optionId: 31, optionKey: 'T', content: '正确' },
      { optionId: 32, optionKey: 'F', content: '错误' },
    ],
    status: 'PENDING',
    errorCount: 3,
    lastWrongAt: '2026-08-14T09:10:00',
    masteredAt: null,
  },
]

const masteredQuestions: WrongQuestion[] = [
  {
    ...pendingQuestions[0]!,
    status: 'MASTERED',
    errorCount: 4,
    masteredAt: '2026-08-14T10:00:00',
  },
]

const result = (
  overrides: Partial<WrongQuestionReviewResult> = {},
): WrongQuestionReviewResult => ({
  questionId: 1001,
  correct: false,
  status: 'PENDING',
  errorCount: 3,
  correctOptionIds: [11],
  explanation: '股票通常代表企业所有权的一部分。',
  ...overrides,
})

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

const mountView = () =>
  mount(WrongQuestionsView, {
    global: { stubs: { RouterLink: routerLinkStub } },
  })

const card = (wrapper: ReturnType<typeof mountView>, questionId: number) =>
  wrapper.get(`[data-question-id="${questionId}"]`)

const answerQuestion = async (
  wrapper: ReturnType<typeof mountView>,
  questionId: number,
  optionIds: number[],
) => {
  const questionCard = card(wrapper, questionId)
  for (const optionId of optionIds) {
    await questionCard.get(`input[value="${optionId}"]`).setValue(true)
  }
}

describe('WrongQuestionsView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.listWrongQuestions.mockResolvedValue(apiResponse(pendingQuestions))
  })

  it('loads PENDING by default and renders persisted data without assuming answers', async () => {
    let finishLoad: ((value: ReturnType<typeof apiResponse<WrongQuestion[]>>) => void) | undefined
    mocks.listWrongQuestions.mockReturnValue(new Promise((resolve) => { finishLoad = resolve }))
    const wrapper = mountView()

    expect(wrapper.get('[data-testid="wrong-loading"]').text()).toContain('加载')
    finishLoad?.(apiResponse(pendingQuestions))
    await flushPromises()

    expect(mocks.listWrongQuestions).toHaveBeenCalledWith('PENDING')
    expect(wrapper.findAll('[data-testid="wrong-question"]')).toHaveLength(3)
    expect(wrapper.text()).toContain('股票通常代表什么？')
    expect(wrapper.text()).toContain('错误 2 次')
    expect(wrapper.get('a[href="/lessons/101"]').text()).toContain('股票是什么')
    expect(wrapper.text()).not.toContain('正确答案')
    expect(wrapper.text()).not.toContain('股票通常代表企业所有权的一部分。')
  })

  it('renders all three question controls from backend types', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(card(wrapper, 1001).findAll('input[type="radio"]')).toHaveLength(2)
    expect(card(wrapper, 1002).findAll('input[type="checkbox"]')).toHaveLength(3)
    expect(card(wrapper, 1003).findAll('input[type="radio"]')).toHaveLength(2)
  })

  it('fetches MASTERED on tab switch and shows status-specific empty states', async () => {
    mocks.listWrongQuestions
      .mockResolvedValueOnce(apiResponse([]))
      .mockResolvedValueOnce(apiResponse([]))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('当前没有待复习错题')
    await wrapper.get('[data-testid="wrong-tab-mastered"]').trigger('click')
    await flushPromises()

    expect(mocks.listWrongQuestions).toHaveBeenNthCalledWith(2, 'MASTERED')
    expect(wrapper.text()).toContain('当前还没有已掌握错题')
  })

  it('shows a load error and retries the active status', async () => {
    mocks.listWrongQuestions
      .mockRejectedValueOnce(new Error('network detail'))
      .mockResolvedValueOnce(apiResponse(pendingQuestions))
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('错题加载失败')
    expect(wrapper.text()).not.toContain('network detail')
    await wrapper.get('[data-testid="wrong-retry"]').trigger('click')
    await flushPromises()

    expect(mocks.listWrongQuestions).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('股票通常代表什么？')
  })

  it('submits only the target selection and disables only that question while pending', async () => {
    let finishReview: ((value: ReturnType<typeof apiResponse<WrongQuestionReviewResult>>) => void) | undefined
    mocks.reviewWrongQuestion.mockReturnValue(new Promise((resolve) => { finishReview = resolve }))
    const wrapper = mountView()
    await flushPromises()
    await answerQuestion(wrapper, 1002, [21, 22])

    await card(wrapper, 1002).get('[data-testid="wrong-review-submit"]').trigger('click')

    expect(mocks.reviewWrongQuestion).toHaveBeenCalledOnce()
    expect(mocks.reviewWrongQuestion).toHaveBeenCalledWith(1002, {
      selectedOptionIds: [21, 22],
    })
    expect(card(wrapper, 1002).get('button').attributes('disabled')).toBeDefined()
    expect(card(wrapper, 1002).findAll('input').every((input) => input.attributes('disabled') !== undefined))
      .toBe(true)
    expect(card(wrapper, 1001).get('button').attributes('disabled')).toBeUndefined()
    expect(card(wrapper, 1001).findAll('input').every((input) => input.attributes('disabled') === undefined))
      .toBe(true)

    finishReview?.(apiResponse(result({ questionId: 1002 })))
    await flushPromises()
  })

  it('keeps a PENDING wrong answer, refreshes errorCount, and shows backend explanation', async () => {
    const refreshed = pendingQuestions.map((question) =>
      question.questionId === 1001 ? { ...question, errorCount: 3 } : question,
    )
    mocks.listWrongQuestions
      .mockResolvedValueOnce(apiResponse(pendingQuestions))
      .mockResolvedValueOnce(apiResponse(refreshed))
    mocks.reviewWrongQuestion.mockResolvedValue(apiResponse(result()))
    const wrapper = mountView()
    await flushPromises()
    await answerQuestion(wrapper, 1001, [12])

    await card(wrapper, 1001).get('[data-testid="wrong-review-submit"]').trigger('click')
    await flushPromises()

    expect(card(wrapper, 1001).text()).toContain('错误 3 次')
    expect(card(wrapper, 1001).text()).toContain('回答错误')
    expect(card(wrapper, 1001).text()).toContain('股票通常代表企业所有权的一部分。')
    expect(mocks.listWrongQuestions).toHaveBeenCalledTimes(2)
  })

  it('removes a mastered transition from the PENDING list using refreshed backend state', async () => {
    mocks.listWrongQuestions
      .mockResolvedValueOnce(apiResponse(pendingQuestions))
      .mockResolvedValueOnce(apiResponse(pendingQuestions.slice(1)))
    mocks.reviewWrongQuestion.mockResolvedValue(
      apiResponse(result({ correct: true, status: 'MASTERED', errorCount: 2 })),
    )
    const wrapper = mountView()
    await flushPromises()
    await answerQuestion(wrapper, 1001, [11])

    await card(wrapper, 1001).get('[data-testid="wrong-review-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-question-id="1001"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('回答正确')
  })

  it.each([
    {
      name: 'keeps a correct MASTERED answer in MASTERED',
      review: result({ correct: true, status: 'MASTERED', errorCount: 4 }),
      refreshed: masteredQuestions,
      remains: true,
    },
    {
      name: 'removes a wrong MASTERED answer that returned to PENDING',
      review: result({ correct: false, status: 'PENDING', errorCount: 5 }),
      refreshed: [],
      remains: false,
    },
  ])('$name', async ({ review, refreshed, remains }) => {
    mocks.listWrongQuestions
      .mockResolvedValueOnce(apiResponse(pendingQuestions))
      .mockResolvedValueOnce(apiResponse(masteredQuestions))
      .mockResolvedValueOnce(apiResponse(refreshed))
    mocks.reviewWrongQuestion.mockResolvedValue(apiResponse(review))
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('[data-testid="wrong-tab-mastered"]').trigger('click')
    await flushPromises()
    await answerQuestion(wrapper, 1001, [11])

    await card(wrapper, 1001).get('[data-testid="wrong-review-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-question-id="1001"]').exists()).toBe(remains)
    expect(mocks.listWrongQuestions).toHaveBeenNthCalledWith(3, 'MASTERED')
  })

  it('preserves the item and selection after review failure', async () => {
    mocks.reviewWrongQuestion.mockRejectedValue(new Error('private detail'))
    const wrapper = mountView()
    await flushPromises()
    await answerQuestion(wrapper, 1001, [12])

    await card(wrapper, 1001).get('[data-testid="wrong-review-submit"]').trigger('click')
    await flushPromises()

    expect(card(wrapper, 1001).get('[role="alert"]').text()).toContain('复习提交失败')
    expect(wrapper.text()).not.toContain('private detail')
    expect((card(wrapper, 1001).get('input[value="12"]').element as HTMLInputElement).checked)
      .toBe(true)
    expect(card(wrapper, 1001).get('button').attributes('disabled')).toBeUndefined()
    expect(wrapper.findAll('[data-testid="wrong-question"]')).toHaveLength(3)
  })

  it('re-fetches each tab instead of keeping a long-lived local copy', async () => {
    mocks.listWrongQuestions
      .mockResolvedValueOnce(apiResponse(pendingQuestions))
      .mockResolvedValueOnce(apiResponse(masteredQuestions))
      .mockResolvedValueOnce(apiResponse([pendingQuestions[1]!]))
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('[data-testid="wrong-tab-mastered"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="wrong-tab-pending"]').trigger('click')
    await flushPromises()

    expect(mocks.listWrongQuestions).toHaveBeenCalledTimes(3)
    expect(mocks.listWrongQuestions).toHaveBeenNthCalledWith(1, 'PENDING')
    expect(mocks.listWrongQuestions).toHaveBeenNthCalledWith(2, 'MASTERED')
    expect(mocks.listWrongQuestions).toHaveBeenNthCalledWith(3, 'PENDING')
    expect(wrapper.findAll('[data-testid="wrong-question"]')).toHaveLength(1)
    expect(wrapper.text()).toContain('哪些属于基础风险？')
  })
})
