<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { useAuthStore } from '../../auth/stores/authStore'
import { getLesson } from '../api/courseApi'
import { completeLesson, getCourseProgress } from '../api/progressApi'
import MarkdownContent from '../components/MarkdownContent.vue'
import type { CourseProgress, LessonDetail } from '../types/course'

const route = useRoute()
const authStore = useAuthStore()

const lesson = ref<LessonDetail | null>(null)
const progress = ref<CourseProgress | null>(null)
const loading = ref(true)
const progressReady = ref(false)
const completionPending = ref(false)
const errorMessage = ref('')
const progressError = ref('')
const completionError = ref('')
let loadGeneration = 0

const completed = computed(() =>
  lesson.value === null
    ? false
    : progress.value?.completedLessonIds.includes(lesson.value.id) ?? false,
)

const isCurrentGeneration = (generation: number): boolean => generation === loadGeneration

const resetLessonState = (): void => {
  lesson.value = null
  progress.value = null
  loading.value = true
  progressReady.value = false
  completionPending.value = false
  errorMessage.value = ''
  progressError.value = ''
  completionError.value = ''
}

const refreshProgress = async (courseId: number, generation: number): Promise<void> => {
  const response = await getCourseProgress(courseId)
  if (!isCurrentGeneration(generation)) {
    return
  }
  progress.value = response.data.data
  progressReady.value = true
  progressError.value = ''
}

const loadLesson = async (lessonId: number, generation: number): Promise<void> => {
  resetLessonState()
  let loadedLesson: LessonDetail | null = null

  try {
    const response = await getLesson(lessonId)
    if (!isCurrentGeneration(generation)) {
      return
    }
    loadedLesson = response.data.data
    lesson.value = loadedLesson
  } catch {
    if (isCurrentGeneration(generation)) {
      errorMessage.value = '课时加载失败，请稍后重试。'
    }
  } finally {
    if (isCurrentGeneration(generation)) {
      loading.value = false
    }
  }

  if (loadedLesson && authStore.isAuthenticated && isCurrentGeneration(generation)) {
    try {
      await refreshProgress(loadedLesson.courseId, generation)
    } catch {
      if (isCurrentGeneration(generation)) {
        progressReady.value = false
        progressError.value = '学习进度加载失败，请稍后重试。'
      }
    }
  }
}

const markComplete = async (): Promise<void> => {
  if (!lesson.value || completionPending.value || completed.value) {
    return
  }

  const currentLesson = lesson.value
  const generation = loadGeneration
  completionPending.value = true
  completionError.value = ''

  try {
    await completeLesson(currentLesson.id)
    if (!isCurrentGeneration(generation)) {
      return
    }
    await refreshProgress(currentLesson.courseId, generation)
  } catch {
    if (isCurrentGeneration(generation)) {
      completionError.value = '记录完成状态失败，请稍后重试。'
    }
  } finally {
    if (isCurrentGeneration(generation)) {
      completionPending.value = false
    }
  }
}

watch(
  () => route.params.lessonId,
  (routeLessonId) => {
    loadGeneration += 1
    void loadLesson(Number(routeLessonId), loadGeneration)
  },
  { immediate: true },
)
</script>

<template>
  <main class="lesson-page">
    <section class="lesson-shell">
      <p v-if="loading" class="state-card" role="status">正在加载课时…</p>
      <p v-else-if="errorMessage" class="state-card state-error" role="alert">
        {{ errorMessage }}
      </p>
      <template v-else-if="lesson">
        <nav class="breadcrumb" aria-label="课时导航">
          <RouterLink to="/courses">全部课程</RouterLink>
          <span aria-hidden="true">/</span>
          <RouterLink :to="`/courses/${lesson.courseId}`">{{ lesson.courseTitle }}</RouterLink>
          <span aria-hidden="true">/</span>
          <span>{{ lesson.chapterTitle }}</span>
        </nav>

        <article class="lesson-card">
          <header class="lesson-header">
            <p class="eyebrow">{{ lesson.chapterTitle }}</p>
            <h1>{{ lesson.title }}</h1>
            <p>{{ lesson.summary }}</p>
            <span class="reading-time">预计 {{ lesson.estimatedMinutes }} 分钟</span>
          </header>

          <MarkdownContent :content="lesson.contentMd" />

          <RouterLink class="quiz-entry" :to="`/lessons/${lesson.id}/quiz`">
            课后测验
          </RouterLink>
        </article>

        <section class="completion-card" aria-labelledby="completion-title">
          <h2 id="completion-title">学习进度</h2>

          <div v-if="!authStore.isAuthenticated" class="login-prompt">
            <p>登录后可记录学习进度，并在刷新后恢复完成状态。</p>
            <RouterLink class="button-link" to="/login">登录并记录进度</RouterLink>
          </div>

          <template v-else>
            <p v-if="progressError" class="feedback feedback-error" role="alert">
              {{ progressError }}
            </p>
            <p
              v-if="completed"
              data-testid="lesson-completed"
              class="completed-state"
              role="status"
            >
              ✓ 已完成
            </p>
            <button
              v-else-if="progressReady"
              data-testid="complete-lesson"
              type="button"
              :disabled="completionPending"
              @click="markComplete"
            >
              {{ completionPending ? '提交中…' : '标记完成' }}
            </button>
            <p v-if="completionError" class="feedback feedback-error" role="alert">
              {{ completionError }}
            </p>

            <RouterLink
              v-if="progressReady && progress?.nextLesson"
              class="next-lesson"
              :to="`/lessons/${progress.nextLesson.id}`"
            >
              继续学习：{{ progress.nextLesson.title }}
            </RouterLink>
            <p
              v-else-if="progressReady && progress?.totalLessons && progress.progressPercent === 100"
              class="course-complete"
            >
              本课程已全部完成。
            </p>
          </template>
        </section>
      </template>
    </section>
  </main>
</template>

<style scoped>
.lesson-page {
  min-height: 100vh;
  padding: clamp(1.5rem, 4vw, 3.5rem) 1rem;
  background: #f5f7fb;
}

.lesson-shell {
  width: min(100%, 52rem);
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

.lesson-card,
.completion-card,
.state-card {
  border: 1px solid #dce4ef;
  border-radius: 1rem;
  background: #ffffff;
  box-shadow: 0 0.8rem 2rem rgb(29 45 72 / 6%);
}

.lesson-card {
  padding: clamp(1.4rem, 4vw, 2.5rem);
}

.lesson-header {
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #e4eaf2;
}

.lesson-header h1,
.lesson-header p,
.completion-card h2,
.completion-card p {
  margin: 0;
}

.lesson-header h1 {
  margin-top: 0.4rem;
  color: #10233f;
  font-size: clamp(1.8rem, 5vw, 2.6rem);
}

.lesson-header h1 + p {
  margin-top: 0.75rem;
  color: #607089;
  line-height: 1.65;
}

.reading-time {
  display: inline-block;
  margin-top: 1rem;
  color: #1d4ed8;
  font-size: 0.9rem;
  font-weight: 700;
}

.markdown-content {
  margin-top: 1.5rem;
}

.quiz-entry {
  display: inline-flex;
  margin-top: 1.5rem;
  font-weight: 700;
}

.completion-card {
  margin-top: 1.25rem;
  padding: 1.25rem;
}

.completion-card h2 {
  color: #172b49;
  font-size: 1.15rem;
}

.login-prompt {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
  color: #607089;
}

.completed-state {
  margin-top: 1rem !important;
  color: #166534;
  font-weight: 750;
}

.completion-card button {
  margin-top: 1rem;
}

.next-lesson {
  display: block;
  margin-top: 1rem;
  font-weight: 650;
}

.course-complete {
  margin-top: 1rem !important;
  color: #166534;
}

.state-card {
  padding: 1.25rem;
  color: #52627a;
}

.state-error {
  color: #991b1b;
}
</style>
