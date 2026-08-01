import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const originalUser = {
  id: 42,
  email: 'learner@example.com',
  nickname: '学习者',
  role: 'USER' as const,
  createdAt: '2026-08-01T10:00:00',
}

const mocks = vi.hoisted(() => ({
  updateNickname: vi.fn(),
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

vi.mock('../../auth/stores/authStore', () => ({
  useAuthStore: () => ({
    currentUser: mocks.currentUser,
    updateNickname: mocks.updateNickname,
    logout: mocks.logout,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplace,
  }),
}))

import ProfileView from './ProfileView.vue'

describe('ProfileView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    Object.assign(mocks.currentUser, originalUser)
  })

  it('shows a read-only email and logs out through the store before navigating', async () => {
    const wrapper = mount(ProfileView)

    const email = wrapper.get('#profile-email')
    expect((email.element as HTMLInputElement).value).toBe('learner@example.com')
    expect(email.attributes('readonly')).toBeDefined()
    expect(wrapper.get('#profile-nickname').attributes('maxlength')).toBeUndefined()

    await wrapper.get('[data-testid="profile-logout"]').trigger('click')

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.routerReplace).toHaveBeenCalledWith('/login')
  })

  it('updates through the store, disables while pending, and gives immediate success feedback', async () => {
    let finishUpdate: ((value: typeof originalUser) => void) | undefined
    mocks.updateNickname.mockReturnValue(
      new Promise<typeof originalUser>((resolve) => {
        finishUpdate = resolve
      }),
    )
    const wrapper = mount(ProfileView)
    await wrapper.get('#profile-nickname').setValue('长期学习者')

    await wrapper.get('form').trigger('submit')

    expect(mocks.updateNickname).toHaveBeenCalledWith('长期学习者')
    expect(wrapper.get('[data-testid="profile-save"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="profile-save"]').text()).toContain('保存中')
    expect(wrapper.get('[data-testid="profile-logout"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-testid="profile-logout"]').trigger('click')
    expect(mocks.logout).not.toHaveBeenCalled()
    expect(mocks.routerReplace).not.toHaveBeenCalled()

    finishUpdate?.({ ...originalUser, nickname: '长期学习者' })
    await flushPromises()

    expect((wrapper.get('#profile-nickname').element as HTMLInputElement).value).toBe(
      '长期学习者',
    )
    expect(wrapper.get('[role="status"]').text()).toBe('昵称已更新')
    expect(wrapper.get('[data-testid="profile-save"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.get('[data-testid="profile-logout"]').attributes('disabled')).toBeUndefined()
  })

  it('renders safe nickname errors and never raw errors', async () => {
    const rawError = new Error('SQL update failure with private details')
    rawError.stack = 'PRIVATE_PROFILE_STACK'
    mocks.updateNickname.mockRejectedValue(rawError)
    const wrapper = mount(ProfileView)
    await wrapper.get('#profile-nickname').setValue('长期学习者')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('昵称更新失败，请稍后重试')
    expect(wrapper.text()).not.toContain(rawError.message)
    expect(wrapper.text()).not.toContain('PRIVATE_PROFILE_STACK')
  })

  it('maps an invalid nickname code to the approved public rule without raw details', async () => {
    const invalidNickname = Object.assign(new Error('private validation internals'), {
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          code: 'USER_NICKNAME_INVALID',
          message: 'raw invalid character detail',
        },
      },
    })
    mocks.updateNickname.mockRejectedValue(invalidNickname)
    const wrapper = mount(ProfileView)
    await wrapper.get('#profile-nickname').setValue('长期学习者')

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('昵称须为 2–20 个字符')
    expect(wrapper.text()).not.toContain(invalidNickname.message)
    expect(wrapper.text()).not.toContain('raw invalid character detail')
  })
})
