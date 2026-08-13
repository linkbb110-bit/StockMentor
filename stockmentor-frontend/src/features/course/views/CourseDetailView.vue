<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '../../auth/stores/authStore'
import { getCourse } from '../api/courseApi'
import { getCourseProgress } from '../api/progressApi'
import type { CourseDetail, CourseProgress } from '../types/course'

const route = useRoute()
const authStore = useAuthStore()
const courseId = Number(route.params.courseId)

const course = ref<CourseDetail | null>(null)
const progress = ref<CourseProgress | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const progressError = ref('')

const isCompleted = (lessonId: number): boolean =>
  progress.value?.completedLessonIds.includes(lessonId) ?? false

const loadProgress = async (): Promise<void> => {
  try {
    const response = await getCourseProgress(courseId)
    progress.value = response.data.data
  } catch {
    progressError.value = '学习进度加载失败，请稍后重试。'
  }
}

const loadCourse = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getCourse(courseId)
    course.value = response.data.data
  } catch {
    errorMessage.value = '课程详情加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }

  if (course.value && authStore.isAuthenticated) {
    await loadProgress()
  }
}

onMounted(loadCourse)
</script>

<template>
  <main class="course-page">
    <section class="detail-shell">
      <p v-if="loading" class="state-card" role="status">正在加载课程目录…</p>
      <p v-else-if="errorMessage" class="state-card state-error" role="alert">
        {{ errorMessage }}
      </p>
      <template v-else-if="course">
        <nav class="breadcrumb" aria-label="课程导航">
          <RouterLink to="/courses">全部课程</RouterLink>
          <span aria-hidden="true">/</span>
          <span>{{ course.title }}</span>
        </nav>

        <header class="detail-header">
          <p class="eyebrow">课程目录</p>
          <h1>{{ course.title }}</h1>
          <p>{{ course.summary }}</p>
          <p v-if="progress" class="progress-summary" role="status">
            已完成 {{ progress.completedLessons }} / {{ progress.totalLessons }} ·
            {{ progress.progressPercent }}%
          </p>
          <p v-else-if="progressError" class="feedback feedback-error" role="alert">
            {{ progressError }}
          </p>
        </header>

        <div class="chapter-list">
          <section
            v-for="chapter in course.chapters"
            :key="chapter.id"
            data-testid="chapter"
            class="chapter-card"
          >
            <header>
              <h2>{{ chapter.title }}</h2>
              <p>{{ chapter.summary }}</p>
            </header>

            <p v-if="chapter.lessons.length === 0" class="chapter-empty">
              本章暂时没有已发布课时。
            </p>
            <ol v-else class="lesson-list">
              <li v-for="lesson in chapter.lessons" :key="lesson.id">
                <RouterLink
                  data-testid="lesson-link"
                  class="lesson-link"
                  :to="`/lessons/${lesson.id}`"
                >
                  <span class="lesson-copy">
                    <strong>{{ lesson.title }}</strong>
                    <small>{{ lesson.summary }}</small>
                  </span>
                  <span class="lesson-meta">
                    <span
                      v-if="isCompleted(lesson.id)"
                      :data-testid="`lesson-completed-${lesson.id}`"
                      class="completed-mark"
                    >
                      ✓ 已完成
                    </span>
                    <span>{{ lesson.estimatedMinutes }} 分钟</span>
                  </span>
                </RouterLink>
              </li>
            </ol>
          </section>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.course-page {
  min-height: 100vh;
  padding: clamp(1.5rem, 4vw, 3.5rem) 1rem;
  background: #f5f7fb;
}

.detail-shell {
  width: min(100%, 62rem);
  margin: 0 auto;
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  color: #607089;
  font-size: 0.9rem;
}

.detail-header,
.chapter-card,
.state-card {
  border: 1px solid #dce4ef;
  border-radius: 1rem;
  background: #ffffff;
}

.detail-header {
  padding: clamp(1.4rem, 4vw, 2.25rem);
}

.detail-header h1,
.detail-header p,
.chapter-card h2,
.chapter-card p {
  margin: 0;
}

.detail-header h1 {
  margin-top: 0.4rem;
  color: #10233f;
  font-size: clamp(1.8rem, 5vw, 2.6rem);
}

.detail-header h1 + p,
.chapter-card header p {
  margin-top: 0.65rem;
  color: #607089;
  line-height: 1.65;
}

.progress-summary {
  margin-top: 1.15rem !important;
  color: #166534 !important;
  font-weight: 700;
}

.chapter-list {
  display: grid;
  gap: 1rem;
  margin-top: 1.25rem;
}

.chapter-card {
  padding: 1.25rem;
}

.chapter-card h2 {
  color: #172b49;
  font-size: 1.2rem;
}

.lesson-list {
  display: grid;
  gap: 0.65rem;
  margin: 1rem 0 0;
  padding: 0;
  list-style: none;
}

.lesson-link {
  display: flex;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  padding: 0.9rem;
  border: 1px solid #e4eaf2;
  border-radius: 0.7rem;
  color: inherit;
  text-decoration: none;
}

.lesson-link:hover {
  border-color: #9bb7e7;
  background: #f8faff;
}

.lesson-copy,
.lesson-meta {
  display: grid;
  gap: 0.3rem;
}

.lesson-copy small,
.lesson-meta,
.chapter-empty {
  color: #607089;
}

.lesson-meta {
  flex: 0 0 auto;
  justify-items: end;
  font-size: 0.82rem;
}

.completed-mark {
  color: #166534;
  font-weight: 700;
}

.chapter-empty {
  margin-top: 1rem !important;
}

.state-card {
  padding: 1.25rem;
  color: #52627a;
}

.state-error {
  color: #991b1b;
}

@media (max-width: 580px) {
  .lesson-link {
    align-items: flex-start;
    flex-direction: column;
  }

  .lesson-meta {
    justify-items: start;
  }
}
</style>
