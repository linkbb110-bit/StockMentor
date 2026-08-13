import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import MarkdownContent from './MarkdownContent.vue'

describe('MarkdownContent', () => {
  it('renders the supported Markdown structure', () => {
    const wrapper = mount(MarkdownContent, {
      props: {
        content: '## 风险与收益\n\n**长期视角**有助于减少噪声。\n\n- 先理解风险\n- 再比较收益',
      },
    })

    expect(wrapper.get('h2').text()).toBe('风险与收益')
    expect(wrapper.get('strong').text()).toBe('长期视角')
    expect(wrapper.findAll('li').map((item) => item.text())).toEqual([
      '先理解风险',
      '再比较收益',
    ])
  })

  it('keeps raw HTML and dangerous links out of the executable DOM', () => {
    const wrapper = mount(MarkdownContent, {
      props: {
        content: [
          '<script>window.__stockmentorExecuted = true</script>',
          '<img src="x" onerror="window.__stockmentorExecuted = true">',
          '[危险链接](javascript:alert(1))',
          '[数据链接](data:text/html,<script>alert(1)</script>)',
        ].join('\n\n'),
      },
    })

    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('[onerror]').exists()).toBe(false)
    expect(wrapper.find('a[href^="javascript:"]').exists()).toBe(false)
    expect(wrapper.find('a[href^="data:"]').exists()).toBe(false)
    expect(wrapper.html()).toContain('&lt;script&gt;')
  })
})
