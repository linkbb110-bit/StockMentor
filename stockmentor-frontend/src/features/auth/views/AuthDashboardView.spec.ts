import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  logout: vi.fn(),
  routerReplace: vi.fn(),
  listCourses: vi.fn(),
  getCourseProgress: vi.fn(),
  currentUser: {
    id: 42,
    email: 'learner@example.com',
    nickname: '学习者',
    role: 'USER' as const,
    createdAt: '2026-08-01T10:00:00',
  },
}))

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    accessToken: 'must-not-be-rendered',
    currentUser: mocks.currentUser,
    logout: mocks.logout,
  }),
}))

vi.mock('../../course/api/courseApi', () => ({
  listCourses: mocks.listCourses,
}))

vi.mock('../../course/api/progressApi', () => ({
  getCourseProgress: mocks.getCourseProgress,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplace,
  }),
}))

import AuthDashboardView from './AuthDashboardView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

const apiResponse = <T>(data: T) => ({
  data: { code: 'SUCCESS', message: '操作成功', data },
})

const courses = [
  {
    id: 7,
    title: '股票投资基础',
    summary: '从资产权利、风险和长期方法开始。',
    coverUrl: null,
  },
  {
    id: 9,
    title: '企业分析入门',
    summary: '理解企业如何创造价值。',
    coverUrl: null,
  },
]

const partialProgress = {
  completedLessons: 7,
  totalLessons: 20,
  progressPercent: 35,
  completedLessonIds: [101, 102, 103, 104, 105, 106, 107],
  nextLesson: {
    id: 108,
    title: '资产负债表怎么看',
    summary: '认识资产、负债与所有者权益。',
    estimatedMinutes: 12,
    chapterId: 14,
    chapterTitle: '读懂财务报表',
  },
}

const mountDashboard = () =>
  mount(AuthDashboardView, {
    global: { stubs: { RouterLink: routerLinkStub } },
  })

describe('AuthDashboardView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows a loading state while the ordered course list is pending', () => {
    mocks.listCourses.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountDashboard()

    expect(wrapper.get('[data-testid="dashboard-loading"]').text()).toContain('加载')
    expect(mocks.getCourseProgress).not.toHaveBeenCalled()
  })

  it('offers authenticated learners a direct wrong-question entry', () => {
    const wrapper = mountDashboard()

    expect(wrapper.get('a[href="/wrong-questions"]').text()).toContain('错题本')
  })

  it('uses the first ordered course and reloads partial progress from the backend on mount', async () => {
    const refreshedProgress = {
      ...partialProgress,
      completedLessons: 8,
      progressPercent: 40,
      completedLessonIds: [...partialProgress.completedLessonIds, 108],
      nextLesson: { ...partialProgress.nextLesson, id: 109, title: '利润表怎么看' },
    }
    mocks.listCourses.mockResolvedValue(apiResponse(courses))
    mocks.getCourseProgress
      .mockResolvedValueOnce(apiResponse(partialProgress))
      .mockResolvedValueOnce(apiResponse(refreshedProgress))

    const firstMount = mountDashboard()
    await flushPromises()

    expect(mocks.getCourseProgress).toHaveBeenNthCalledWith(1, 7)
    expect(firstMount.get('[data-testid="dashboard-course-title"]').text()).toBe('股票投资基础')
    expect(firstMount.get('[data-testid="dashboard-progress-count"]').text()).toContain('7 / 20')
    expect(firstMount.get('[data-testid="dashboard-progress-percent"]').text()).toBe('35%')
    expect(firstMount.get('[data-testid="dashboard-next-lesson"]').text()).toContain(
      '资产负债表怎么看',
    )
    expect(firstMount.get('[data-testid="continue-learning"]').attributes('href')).toBe(
      '/lessons/108',
    )
    expect(firstMount.get('[data-testid="dashboard-course-title"]').attributes('href')).toBe(
      '/courses/7',
    )
    expect(firstMount.get('a[href="/profile"]').text()).toContain('个人中心')
    expect(firstMount.text()).not.toContain('must-not-be-rendered')
    firstMount.unmount()

    const refreshedMount = mountDashboard()
    await flushPromises()

    expect(mocks.getCourseProgress).toHaveBeenNthCalledWith(2, 7)
    expect(refreshedMount.get('[data-testid="dashboard-progress-count"]').text()).toContain(
      '8 / 20',
    )
    expect(refreshedMount.get('[data-testid="dashboard-progress-percent"]').text()).toBe('40%')
  })

  it('renders zero progress and links to the first unfinished lesson', async () => {
    mocks.listCourses.mockResolvedValue(apiResponse(courses.slice(0, 1)))
    mocks.getCourseProgress.mockResolvedValue(
      apiResponse({
        ...partialProgress,
        completedLessons: 0,
        progressPercent: 0,
        completedLessonIds: [],
        nextLesson: { ...partialProgress.nextLesson, id: 101, title: '股票代表什么' },
      }),
    )

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.get('[data-testid="dashboard-progress-count"]').text()).toContain('0 / 20')
    expect(wrapper.get('[data-testid="dashboard-progress-percent"]').text()).toBe('0%')
    expect(wrapper.get('[data-testid="continue-learning"]').attributes('href')).toBe(
      '/lessons/101',
    )
  })

  it('shows a completed state at 100 percent without a stale continue action', async () => {
    mocks.listCourses.mockResolvedValue(apiResponse(courses.slice(0, 1)))
    mocks.getCourseProgress.mockResolvedValue(
      apiResponse({
        completedLessons: 20,
        totalLessons: 20,
        progressPercent: 100,
        completedLessonIds: Array.from({ length: 20 }, (_, index) => index + 101),
        nextLesson: null,
      }),
    )

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.get('[data-testid="dashboard-complete"]').text()).toContain(
      '已完成《股票投资基础》',
    )
    expect(wrapper.get('[data-testid="dashboard-progress-count"]').text()).toContain('20 / 20')
    expect(wrapper.get('[data-testid="dashboard-progress-percent"]').text()).toBe('100%')
    expect(wrapper.find('[data-testid="continue-learning"]').exists()).toBe(false)
  })

  it('handles an empty published course list without requesting private progress', async () => {
    mocks.listCourses.mockResolvedValue(apiResponse([]))

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.get('[data-testid="dashboard-empty"]').text()).toContain('尚未发布')
    expect(mocks.getCourseProgress).not.toHaveBeenCalled()
    expect(wrapper.get('a[href="/courses"]').text()).toContain('浏览课程')
  })

  it('shows a safe API error and can retry the complete dashboard request', async () => {
    const rawError = new Error('private upstream host and stack')
    mocks.listCourses
      .mockRejectedValueOnce(rawError)
      .mockResolvedValueOnce(apiResponse(courses.slice(0, 1)))
    mocks.getCourseProgress.mockResolvedValue(apiResponse(partialProgress))

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('学习进度加载失败')
    expect(wrapper.text()).not.toContain(rawError.message)

    await wrapper.get('[data-testid="dashboard-retry"]').trigger('click')
    await flushPromises()

    expect(mocks.listCourses).toHaveBeenCalledTimes(2)
    expect(mocks.getCourseProgress).toHaveBeenCalledWith(7)
    expect(wrapper.get('[data-testid="dashboard-progress-percent"]').text()).toBe('35%')
  })

  it('preserves local logout and returns to login', async () => {
    mocks.listCourses.mockReturnValue(new Promise(() => undefined))
    const wrapper = mountDashboard()

    await wrapper.get('[data-testid="dashboard-logout"]').trigger('click')

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.routerReplace).toHaveBeenCalledOnce()
    expect(mocks.routerReplace).toHaveBeenCalledWith('/login')
  })
})
