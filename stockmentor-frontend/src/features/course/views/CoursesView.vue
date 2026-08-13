<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { listCourses } from '../api/courseApi'
import type { CourseSummary } from '../types/course'

const courses = ref<CourseSummary[]>([])
const loading = ref(true)
const errorMessage = ref('')

const loadCourses = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await listCourses()
    courses.value = response.data.data
  } catch {
    errorMessage.value = '课程加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(loadCourses)
</script>

<template>
  <main class="course-page">
    <section class="course-shell" aria-labelledby="courses-title">
      <header class="course-hero">
        <p class="eyebrow">StockMentor 学习中心</p>
        <h1 id="courses-title">投资知识课程</h1>
        <p>用清晰、可复习的课时建立长期投资基础。</p>
      </header>

      <p v-if="loading" data-testid="courses-loading" class="state-card" role="status">
        正在加载课程…
      </p>
      <p v-else-if="errorMessage" class="state-card state-error" role="alert">
        {{ errorMessage }}
      </p>
      <div v-else-if="courses.length === 0" data-testid="courses-empty" class="state-card">
        教育内容尚未发布，请稍后再来。
      </div>
      <div v-else class="course-grid">
        <article v-for="course in courses" :key="course.id" data-testid="course-card" class="course-card">
          <img
            v-if="course.coverUrl"
            class="course-cover"
            :src="course.coverUrl"
            :alt="`${course.title}封面`"
          />
          <div v-else class="course-cover course-cover-placeholder" aria-hidden="true">SM</div>
          <div class="course-card-body">
            <h2>{{ course.title }}</h2>
            <p>{{ course.summary }}</p>
            <RouterLink class="button-link course-entry" :to="`/courses/${course.id}`">
              查看课程
            </RouterLink>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.course-page {
  min-height: 100vh;
  padding: clamp(1.5rem, 4vw, 3.5rem) 1rem;
  background: #f5f7fb;
}

.course-shell {
  width: min(100%, 68rem);
  margin: 0 auto;
}

.course-hero {
  margin-bottom: 2rem;
}

.course-hero h1,
.course-hero p {
  margin: 0;
}

.course-hero h1 {
  margin-top: 0.45rem;
  color: #10233f;
  font-size: clamp(2rem, 6vw, 3rem);
}

.course-hero h1 + p {
  margin-top: 0.7rem;
  color: #607089;
  line-height: 1.7;
}

.state-card {
  padding: 1.25rem;
  border: 1px solid #dce4ef;
  border-radius: 0.9rem;
  color: #52627a;
  background: #ffffff;
}

.state-error {
  color: #991b1b;
  background: #fff7f7;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 18rem), 1fr));
  gap: 1.25rem;
}

.course-card {
  overflow: hidden;
  border: 1px solid #dce4ef;
  border-radius: 1rem;
  background: #ffffff;
  box-shadow: 0 0.8rem 2rem rgb(29 45 72 / 7%);
}

.course-cover {
  width: 100%;
  height: 10rem;
  object-fit: cover;
}

.course-cover-placeholder {
  display: grid;
  place-items: center;
  color: #1d4ed8;
  background: linear-gradient(135deg, #eaf1ff, #f4f7fb);
  font-size: 2rem;
  font-weight: 750;
}

.course-card-body {
  padding: 1.25rem;
}

.course-card h2,
.course-card p {
  margin: 0;
}

.course-card h2 {
  color: #10233f;
  font-size: 1.25rem;
}

.course-card p {
  min-height: 3rem;
  margin-top: 0.65rem;
  color: #607089;
  line-height: 1.6;
}

.course-entry {
  margin-top: 1.15rem;
}
</style>
