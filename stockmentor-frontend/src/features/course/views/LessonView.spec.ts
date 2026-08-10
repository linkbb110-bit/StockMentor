import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  getLesson: vi.fn(),
  getCourseProgress: vi.fn(),
  completeLesson: vi.fn(),
  isAuthenticated: false,
  routeParams: { lessonId: '101' },
}))

vi.mock('../api/courseApi', () => ({
  getLesson: mocks.getLesson,
}))

vi.mock('../api/progressApi', () => ({
  getCourseProgress: mocks.getCourseProgress,
  completeLesson: mocks.completeLesson,
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

import LessonView from './LessonView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

const apiResponse = <T>(data: T) => ({
  data: { code: 'SUCCESS', message: '操作成功', data },
})

const lesson = {
  id: 101,
  title: '股票是什么',
  summary: '理解股票所代表的基本权利。',
  contentMd: '## 一份所有权凭证\n\n股票代表企业的一部分所有权。',
  estimatedMinutes: 8,
  courseId: 7,
  courseTitle: '股票投资基础',
  chapterId: 11,
  chapterTitle: '第一章 市场与资产',
}

const incompleteProgress = {
  completedLessons: 0,
  totalLessons: 3,
  progressPercent: 0,
  completedLessonIds: [],
  nextLesson: {
    id: 101,
    title: lesson.title,
    summary: lesson.summary,
    estimatedMinutes: lesson.estimatedMinutes,
    chapterId: lesson.chapterId,
    chapterTitle: lesson.chapterTitle,
  },
}

const completedProgress = {
  ...incompleteProgress,
  completedLessons: 1,
  progressPercent: 33,
  completedLessonIds: [101],
  nextLesson: {
    id: 102,
    title: '指数是什么',
    summary: '理解指数用途。',
    estimatedMinutes: 7,
    chapterId: 11,
    chapterTitle: lesson.chapterTitle,
  },
}

describe('LessonView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.isAuthenticated = false
    mocks.getLesson.mockResolvedValue(apiResponse(lesson))
  })

  it('lets anonymous users read Markdown and context without any private request', async () => {
    const wrapper = mount(LessonView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await flushPromises()

    expect(mocks.getLesson).toHaveBeenCalledWith(101)
    expect(mocks.getCourseProgress).not.toHaveBeenCalled()
    expect(mocks.completeLesson).not.toHaveBeenCalled()
    expect(wrapper.get('h1').text()).toBe('股票是什么')
    expect(wrapper.get('.markdown-content h2').text()).toBe('一份所有权凭证')
    expect(wrapper.text()).toContain('股票投资基础')
    expect(wrapper.text()).toContain('第一章 市场与资产')
    expect(wrapper.text()).toContain('8 分钟')
    expect(wrapper.get('a[href="/login"]').text()).toContain('登录')
    expect(wrapper.find('[data-testid="complete-lesson"]').exists()).toBe(false)
  })

  it('restores completed state from completedLessonIds on every mount', async () => {
    mocks.isAuthenticated = true
    mocks.getCourseProgress.mockResolvedValue(apiResponse(completedProgress))

    const firstMount = mount(LessonView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()
    expect(firstMount.get('[data-testid="lesson-completed"]').text()).toContain('已完成')
    firstMount.unmount()

    const refreshedMount = mount(LessonView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(mocks.getLesson).toHaveBeenCalledTimes(2)
    expect(mocks.getCourseProgress).toHaveBeenCalledTimes(2)
    expect(mocks.getCourseProgress).toHaveBeenLastCalledWith(7)
    expect(refreshedMount.get('[data-testid="lesson-completed"]').text()).toContain('已完成')
    expect(refreshedMount.find('[data-testid="complete-lesson"]').exists()).toBe(false)
  })

  it('disables duplicate completion while pending and trusts the refreshed progress', async () => {
    mocks.isAuthenticated = true
    mocks.getCourseProgress
      .mockResolvedValueOnce(apiResponse(incompleteProgress))
      .mockResolvedValueOnce(apiResponse(completedProgress))
    let finishCompletion: ((value: ReturnType<typeof apiResponse>) => void) | undefined
    mocks.completeLesson.mockReturnValue(
      new Promise((resolve) => {
        finishCompletion = resolve
      }),
    )
    const wrapper = mount(LessonView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    const button = wrapper.get('[data-testid="complete-lesson"]')
    await button.trigger('click')
    await button.trigger('click')

    expect(mocks.completeLesson).toHaveBeenCalledOnce()
    expect(mocks.completeLesson).toHaveBeenCalledWith(101)
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toContain('提交中')
    expect(wrapper.find('[data-testid="lesson-completed"]').exists()).toBe(false)

    finishCompletion?.(
      apiResponse({ lessonId: 101, completed: true, completedAt: '2026-08-10T12:00:00' }),
    )
    await flushPromises()

    expect(mocks.getCourseProgress).toHaveBeenCalledTimes(2)
    expect(wrapper.get('[data-testid="lesson-completed"]').text()).toContain('已完成')
    expect(wrapper.find('[data-testid="complete-lesson"]').exists()).toBe(false)
    expect(wrapper.get('a[href="/lessons/102"]').text()).toContain('继续学习')
  })

  it('recovers the button and never fabricates completion after a failed PUT', async () => {
    const rawError = new Error('private completion failure')
    mocks.isAuthenticated = true
    mocks.getCourseProgress.mockResolvedValue(apiResponse(incompleteProgress))
    mocks.completeLesson.mockRejectedValue(rawError)
    const wrapper = mount(LessonView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    await wrapper.get('[data-testid="complete-lesson"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('记录完成状态失败')
    expect(wrapper.text()).not.toContain(rawError.message)
    expect(wrapper.find('[data-testid="lesson-completed"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="complete-lesson"]').attributes('disabled')).toBeUndefined()
    expect(mocks.getCourseProgress).toHaveBeenCalledOnce()
  })
})
