import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  routerPush: vi.fn(),
}))

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    login: mocks.login,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.routerPush,
  }),
}))

import LoginView from './LoginView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('submits a password snapshot, clears the field, disables while pending, and navigates', async () => {
    let finishLogin: ((applied: boolean) => void) | undefined
    mocks.login.mockReturnValue(
      new Promise<boolean>((resolve) => {
        finishLogin = resolve
      }),
    )
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('#login-email').setValue(' Learner@Example.com ')
    await wrapper.get('#login-password').setValue('study123')
    await wrapper.get('form').trigger('submit')

    expect(mocks.login).toHaveBeenCalledWith({
      email: ' Learner@Example.com ',
      password: 'study123',
    })
    expect(wrapper.get('#login-email').attributes('type')).toBe('text')
    expect(wrapper.get('#login-email').attributes('inputmode')).toBe('email')
    expect(wrapper.get('#login-email').attributes('autocomplete')).toBe('email')
    expect((wrapper.get('#login-password').element as HTMLInputElement).value).toBe('')
    expect(wrapper.get('[data-testid="login-submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="login-submit"]').text()).toContain('登录中')

    finishLogin?.(true)
    await flushPromises()

    expect(mocks.routerPush).toHaveBeenCalledWith('/dashboard')
    expect(wrapper.get('[data-testid="login-submit"]').attributes('disabled')).toBeUndefined()
  })

  it('does not navigate when its successful login response no longer owns authentication', async () => {
    mocks.login.mockResolvedValue(false)
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('#login-email').setValue('learner@example.com')
    await wrapper.get('#login-password').setValue('study123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.routerPush).not.toHaveBeenCalled()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('renders only a safe public error and never the raw error or stack', async () => {
    const rawError = new Error('database host and private stack details')
    rawError.stack = 'PRIVATE_STACK_TRACE'
    mocks.login.mockRejectedValue(rawError)
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('#login-email').setValue('learner@example.com')
    await wrapper.get('#login-password').setValue('study123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('登录失败，请稍后重试')
    expect(wrapper.text()).not.toContain(rawError.message)
    expect(wrapper.text()).not.toContain('PRIVATE_STACK_TRACE')
    expect(mocks.routerPush).not.toHaveBeenCalled()
  })

  it('maps invalid credentials to the single approved public message', async () => {
    const invalidCredentials = Object.assign(new Error('internal authentication detail'), {
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          code: 'AUTH_INVALID_CREDENTIALS',
          message: 'raw response detail',
        },
      },
    })
    mocks.login.mockRejectedValue(invalidCredentials)
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('#login-email').setValue('learner@example.com')
    await wrapper.get('#login-password').setValue('study123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('邮箱或密码错误')
    expect(wrapper.text()).not.toContain(invalidCredentials.message)
    expect(wrapper.text()).not.toContain('raw response detail')
  })

  it('validates required and length rules before dispatching', async () => {
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('form').trigger('submit')

    expect(mocks.login).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('邮箱')
  })

  it('submits a normalized-254-character email with surrounding spaces intact', async () => {
    const normalizedEmail = `${'a'.repeat(64)}@${'b'.repeat(63)}.${'c'.repeat(63)}.${'d'.repeat(61)}`
    const rawEmail = ` ${normalizedEmail} `
    expect(normalizedEmail).toHaveLength(254)
    mocks.login.mockResolvedValue(true)
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.get('#login-email').attributes('maxlength')).toBeUndefined()
    await wrapper.get('#login-email').setValue(rawEmail)
    await wrapper.get('#login-password').setValue('study123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.login).toHaveBeenCalledWith({ email: rawEmail, password: 'study123' })
  })

  it('accepts exactly 72 UTF-8 password bytes and rejects more than 72 bytes locally', async () => {
    const exactLimitPassword = `a1${'学'.repeat(23)}x`
    const aboveLimitPassword = `${exactLimitPassword}x`
    expect(new TextEncoder().encode(exactLimitPassword)).toHaveLength(72)
    expect(new TextEncoder().encode(aboveLimitPassword)).toHaveLength(73)
    mocks.login.mockResolvedValue(true)
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    await wrapper.get('#login-email').setValue('learner@example.com')
    await wrapper.get('#login-password').setValue(exactLimitPassword)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.login).toHaveBeenCalledWith({
      email: 'learner@example.com',
      password: exactLimitPassword,
    })

    mocks.login.mockClear()
    await wrapper.get('#login-password').setValue(aboveLimitPassword)
    await wrapper.get('form').trigger('submit')

    expect(mocks.login).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('72')
  })

  it('shows the registration link and investment-education-only notice', () => {
    const wrapper = mount(LoginView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.get('a[href="/register"]').text()).toContain('注册')
    expect(wrapper.text()).toContain('仅用于投资教育')
    expect(wrapper.text()).toContain('不构成投资建议')
  })
})
