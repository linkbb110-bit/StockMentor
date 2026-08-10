import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  getCourse: vi.fn(),
  getCourseProgress: vi.fn(),
  isAuthenticated: false,
  routeParams: { courseId: '7' },
}))

vi.mock('../api/courseApi', () => ({
  getCourse: mocks.getCourse,
}))

vi.mock('../api/progressApi', () => ({
  getCourseProgress: mocks.getCourseProgress,
}))

vi.mock('../../auth/stores/authStore', () => ({
  useAuthStore: () => ({
    get isAuthenticated() {
      return mocks.isAuthenticated
    },
  }),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: mocks.routeParams }),
}))

import CourseDetailView from './CourseDetailView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

const apiResponse = <T>(data: T) => ({
  data: { code: 'SUCCESS', message: '操作成功', data },
})

const course = {
  id: 7,
  title: '股票投资基础',
  summary: '建立长期学习所需的基础框架。',
  coverUrl: null,
  chapters: [
    {
      id: 11,
      title: '第一章 市场与资产',
      summary: '理解常见资产。',
      lessons: [
        { id: 101, title: '第一课 股票是什么', summary: '理解股东权利。', estimatedMinutes: 8 },
        { id: 102, title: '第二课 指数是什么', summary: '理解指数用途。', estimatedMinutes: 7 },
      ],
    },
    {
      id: 12,
      title: '第二章 风险与收益',
      summary: '建立风险意识。',
      lessons: [
        { id: 103, title: '第三课 波动与损失', summary: '区分两个概念。', estimatedMinutes: 9 },
      ],
    },
  ],
}

const progress = {
  completedLessons: 2,
  totalLessons: 3,
  progressPercent: 66,
  completedLessonIds: [101, 103],
  nextLesson: {
    id: 102,
    title: '第二课 指数是什么',
    summary: '理解指数用途。',
    estimatedMinutes: 7,
    chapterId: 11,
    chapterTitle: '第一章 市场与资产',
  },
}

describe('CourseDetailView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.isAuthenticated = false
    mocks.getCourse.mockResolvedValue(apiResponse(course))
  })

  it('keeps backend chapter and lesson order for anonymous readers without private calls', async () => {
    const wrapper = mount(CourseDetailView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await flushPromises()

    expect(mocks.getCourse).toHaveBeenCalledWith(7)
    expect(mocks.getCourseProgress).not.toHaveBeenCalled()
    expect(wrapper.findAll('[data-testid="chapter"] h2').map((item) => item.text())).toEqual([
      '第一章 市场与资产',
      '第二章 风险与收益',
    ])
    expect(
      wrapper
        .findAll('[data-testid="lesson-link"]')
        .map((item) => item.get('strong').text()),
    ).toEqual(['第一课 股票是什么', '第二课 指数是什么', '第三课 波动与损失'])
    expect(wrapper.find('[data-testid^="lesson-completed-"]').exists()).toBe(false)
  })

  it('loads one course progress request and marks exactly its completed lesson IDs', async () => {
    mocks.isAuthenticated = true
    mocks.getCourseProgress.mockResolvedValue(apiResponse(progress))
    const wrapper = mount(CourseDetailView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await flushPromises()

    expect(mocks.getCourseProgress).toHaveBeenCalledOnce()
    expect(mocks.getCourseProgress).toHaveBeenCalledWith(7)
    expect(wrapper.find('[data-testid="lesson-completed-101"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="lesson-completed-102"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="lesson-completed-103"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('66%')
  })
})
