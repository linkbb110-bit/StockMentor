import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  listCourses: vi.fn(),
}))

vi.mock('../api/courseApi', () => ({
  listCourses: mocks.listCourses,
}))

import CoursesView from './CoursesView.vue'

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
    coverUrl: 'https://example.test/course-cover.png',
  },
]

describe('CoursesView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows loading before rendering the returned course order and learning links', async () => {
    let finishLoading: ((value: ReturnType<typeof apiResponse<typeof courses>>) => void) | undefined
    mocks.listCourses.mockReturnValue(
      new Promise((resolve) => {
        finishLoading = resolve
      }),
    )
    const wrapper = mount(CoursesView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.get('[data-testid="courses-loading"]').text()).toContain('加载')

    finishLoading?.(apiResponse(courses))
    await flushPromises()

    const cards = wrapper.findAll('[data-testid="course-card"]')
    expect(cards.map((card) => card.get('h2').text())).toEqual([
      '股票投资基础',
      '企业分析入门',
    ])
    expect(cards[0]?.get('a').attributes('href')).toBe('/courses/7')
    expect(cards[1]?.get('img').attributes('src')).toBe(
      'https://example.test/course-cover.png',
    )
  })

  it('renders an educational empty state when no course is published', async () => {
    mocks.listCourses.mockResolvedValue(apiResponse([]))
    const wrapper = mount(CoursesView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await flushPromises()

    expect(wrapper.get('[data-testid="courses-empty"]').text()).toContain('尚未发布')
    expect(wrapper.find('[data-testid="course-card"]').exists()).toBe(false)
  })

  it('shows only a safe public error when course loading fails', async () => {
    const rawError = new Error('private upstream host and stack')
    mocks.listCourses.mockRejectedValue(rawError)
    const wrapper = mount(CoursesView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('课程加载失败')
    expect(wrapper.text()).not.toContain(rawError.message)
  })
})
