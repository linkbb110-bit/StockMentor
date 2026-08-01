import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  logout: vi.fn(),
  routerReplace: vi.fn(),
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

describe('AuthDashboardView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('shows only the approved V0.2 identity and placeholder content', () => {
    const wrapper = mount(AuthDashboardView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.text()).toContain('欢迎，学习者')
    expect(wrapper.text()).toContain('learner@example.com')
    expect(wrapper.text()).toContain('USER')
    expect(wrapper.text()).toContain('V0.2 用户与认证已完成')
    expect(wrapper.text()).toContain('课程系统将在 V0.3 开放')
    expect(wrapper.get('a[href="/profile"]').text()).toContain('个人中心')
    expect(wrapper.find('canvas').exists()).toBe(false)
    expect(wrapper.find('svg').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('学习进度')
    expect(wrapper.text()).not.toContain('组合表现')
    expect(wrapper.text()).not.toContain('ECharts')
    expect(wrapper.text()).not.toContain('must-not-be-rendered')
  })

  it('logs out only through the store and navigates to login', async () => {
    const wrapper = mount(AuthDashboardView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('[data-testid="dashboard-logout"]').trigger('click')

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.routerReplace).toHaveBeenCalledOnce()
    expect(mocks.routerReplace).toHaveBeenCalledWith('/login')
  })
})
