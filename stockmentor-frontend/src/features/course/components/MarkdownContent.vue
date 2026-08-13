<script lang="ts">
import MarkdownIt from 'markdown-it'
import { computed, defineComponent } from 'vue'

const markdownRenderer = new MarkdownIt({ html: false })

export default defineComponent({
  name: 'MarkdownContent',
  props: {
    content: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const renderedContent = computed(() => markdownRenderer.render(props.content))

    return { renderedContent }
  },
})
</script>

<template>
  <article class="markdown-content" v-html="renderedContent"></article>
</template>

<style scoped>
.markdown-content {
  color: #27364b;
  font-size: 1rem;
  line-height: 1.8;
}

.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin: 2rem 0 0.75rem;
  color: #10233f;
  line-height: 1.35;
}

.markdown-content :deep(p),
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.8rem 0;
}

.markdown-content :deep(a) {
  overflow-wrap: anywhere;
}

.markdown-content :deep(code) {
  padding: 0.1rem 0.3rem;
  border-radius: 0.3rem;
  background: #eef2f7;
}
</style>
