import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  register: vi.fn(),
  routerPush: vi.fn(),
}))

vi.mock('../stores/authStore', () => ({
  useAuthStore: () => ({
    register: mocks.register,
  }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.routerPush,
  }),
}))

import RegisterView from './RegisterView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

const fillValidForm = async (wrapper: ReturnType<typeof mount>) => {
  await wrapper.get('#register-email').setValue(' Learner@Example.com ')
  await wrapper.get('#register-nickname').setValue('学习者')
  await wrapper.get('#register-password').setValue('study123')
  await wrapper.get('#register-confirm-password').setValue('study123')
}

describe('RegisterView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('keeps confirmation frontend-only, clears secrets, disables while pending, and navigates', async () => {
    let finishRegistration: ((applied: boolean) => void) | undefined
    mocks.register.mockReturnValue(
      new Promise<boolean>((resolve) => {
        finishRegistration = resolve
      }),
    )
    const wrapper = mount(RegisterView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await fillValidForm(wrapper)

    await wrapper.get('form').trigger('submit')

    expect(mocks.register).toHaveBeenCalledWith({
      email: ' Learner@Example.com ',
      nickname: '学习者',
      password: 'study123',
    })
    expect(wrapper.get('#register-email').attributes('type')).toBe('text')
    expect(wrapper.get('#register-email').attributes('inputmode')).toBe('email')
    expect(wrapper.get('#register-email').attributes('autocomplete')).toBe('email')
    expect(mocks.register.mock.calls[0]?.[0]).not.toHaveProperty('confirmPassword')
    expect((wrapper.get('#register-password').element as HTMLInputElement).value).toBe('')
    expect((wrapper.get('#register-confirm-password').element as HTMLInputElement).value).toBe('')
    expect(wrapper.get('[data-testid="register-submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="register-submit"]').text()).toContain('注册中')

    finishRegistration?.(true)
    await flushPromises()

    expect(mocks.routerPush).toHaveBeenCalledWith('/dashboard')
  })

  it('does not navigate when its successful registration no longer owns authentication', async () => {
    mocks.register.mockResolvedValue(false)
    const wrapper = mount(RegisterView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await fillValidForm(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.routerPush).not.toHaveBeenCalled()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('shows the full password rule and rejects a mismatched confirmation locally', async () => {
    const wrapper = mount(RegisterView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.get('[data-testid="password-rule"]').text()).toContain('8–64')
    expect(wrapper.get('[data-testid="password-rule"]').text()).toContain('英文字母')
    expect(wrapper.get('[data-testid="password-rule"]').text()).toContain('数字')
    expect(wrapper.get('[data-testid="password-rule"]').text()).toContain('72')
    expect(wrapper.get('#register-nickname').attributes('maxlength')).toBeUndefined()

    await fillValidForm(wrapper)
    await wrapper.get('#register-confirm-password').setValue('different123')
    await wrapper.get('form').trigger('submit')

    expect(mocks.register).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toBe('两次输入的密码不一致')
  })

  it('accepts exactly 72 UTF-8 password bytes and rejects more than 72 bytes locally', async () => {
    const exactLimitPassword = `a1${'学'.repeat(23)}x`
    const aboveLimitPassword = `${exactLimitPassword}x`
    expect(new TextEncoder().encode(exactLimitPassword)).toHaveLength(72)
    expect(new TextEncoder().encode(aboveLimitPassword)).toHaveLength(73)
    mocks.register.mockResolvedValue(true)
    const wrapper = mount(RegisterView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await wrapper.get('#register-email').setValue('learner@example.com')
    await wrapper.get('#register-nickname').setValue('学习者')
    await wrapper.get('#register-password').setValue(exactLimitPassword)
    await wrapper.get('#register-confirm-password').setValue(exactLimitPassword)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.register).toHaveBeenCalledWith({
      email: 'learner@example.com',
      nickname: '学习者',
      password: exactLimitPassword,
    })

    mocks.register.mockClear()
    await wrapper.get('#register-password').setValue(aboveLimitPassword)
    await wrapper.get('#register-confirm-password').setValue(aboveLimitPassword)
    await wrapper.get('form').trigger('submit')

    expect(mocks.register).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('72')
  })

  it('maps an email conflict to a safe 409 message without rendering raw server details', async () => {
    const conflict = Object.assign(new Error('duplicate index uk_sys_user_email'), {
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          code: 'USER_EMAIL_ALREADY_EXISTS',
          message: 'raw database detail',
        },
      },
    })
    mocks.register.mockRejectedValue(conflict)
    const wrapper = mount(RegisterView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await fillValidForm(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('该邮箱已注册，请直接登录')
    expect(wrapper.text()).not.toContain(conflict.message)
    expect(wrapper.text()).not.toContain('raw database detail')
    expect(mocks.routerPush).not.toHaveBeenCalled()
  })

  it('submits a normalized-254-character email with surrounding spaces intact', async () => {
    const normalizedEmail = `${'a'.repeat(64)}@${'b'.repeat(63)}.${'c'.repeat(63)}.${'d'.repeat(61)}`
    const rawEmail = ` ${normalizedEmail} `
    expect(normalizedEmail).toHaveLength(254)
    mocks.register.mockResolvedValue(true)
    const wrapper = mount(RegisterView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.get('#register-email').attributes('maxlength')).toBeUndefined()
    await wrapper.get('#register-email').setValue(rawEmail)
    await wrapper.get('#register-nickname').setValue('学习者')
    await wrapper.get('#register-password').setValue('study123')
    await wrapper.get('#register-confirm-password').setValue('study123')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.register).toHaveBeenCalledWith({
      email: rawEmail,
      nickname: '学习者',
      password: 'study123',
    })
  })
})
