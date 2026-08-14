import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import QuestionOptions from './QuestionOptions.vue'

const options = [
  { optionId: 11, optionKey: 'A', content: '第一项' },
  { optionId: 12, optionKey: 'B', content: '第二项' },
  { optionId: 13, optionKey: 'C', content: '第三项' },
]

describe('QuestionOptions', () => {
  it.each(['SINGLE_CHOICE', 'TRUE_FALSE'] as const)(
    'renders %s as one labelled radio group',
    async (type) => {
      const wrapper = mount(QuestionOptions, {
        props: {
          name: `question-${type}`,
          type,
          options,
          selectedOptionIds: [],
        },
      })

      const inputs = wrapper.findAll('input[type="radio"]')
      expect(inputs).toHaveLength(3)
      expect(inputs.map((input) => input.attributes('name'))).toEqual([
        `question-${type}`,
        `question-${type}`,
        `question-${type}`,
      ])
      expect(wrapper.text()).toContain('A. 第一项')
      expect(wrapper.text()).toContain('C. 第三项')

      await inputs[1]?.setValue(true)
      expect(wrapper.emitted('update:selectedOptionIds')?.at(-1)).toEqual([[12]])
    },
  )

  it('renders MULTIPLE_CHOICE as checkboxes and emits the backend option order', async () => {
    const wrapper = mount(QuestionOptions, {
      props: {
        name: 'question-multiple',
        type: 'MULTIPLE_CHOICE',
        options,
        selectedOptionIds: [13],
      },
    })

    const inputs = wrapper.findAll('input[type="checkbox"]')
    expect(inputs).toHaveLength(3)
    expect((inputs[2]?.element as HTMLInputElement).checked).toBe(true)

    await inputs[0]?.setValue(true)
    expect(wrapper.emitted('update:selectedOptionIds')?.at(-1)).toEqual([[11, 13]])
  })

  it('uses real disabled controls while a submission is pending', () => {
    const wrapper = mount(QuestionOptions, {
      props: {
        name: 'question-disabled',
        type: 'SINGLE_CHOICE',
        options,
        selectedOptionIds: [11],
        disabled: true,
      },
    })

    expect(wrapper.findAll('input').every((input) => input.attributes('disabled') !== undefined))
      .toBe(true)
  })
})
