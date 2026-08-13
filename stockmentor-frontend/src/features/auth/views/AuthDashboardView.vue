<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { listCourses } from '../../course/api/courseApi'
import { getCourseProgress } from '../../course/api/progressApi'
import type { CourseProgress, CourseSummary } from '../../course/types/course'
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
const router = useRouter()
const currentUser = computed(() => authStore.currentUser)
const currentCourse = ref<CourseSummary | null>(null)
const progress = ref<CourseProgress | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const isComplete = computed(
  () => progress.value?.progressPercent === 100 && progress.value.nextLesson === null,
)

const loadDashboard = async (): Promise<void> => {
  loading.value = true
  errorMessage.value = ''
  currentCourse.value = null
  progress.value = null

  try {
    const coursesResponse = await listCourses()
    const firstCourse = coursesResponse.data.data[0] ?? null
    currentCourse.value = firstCourse

    if (firstCourse) {
      const progressResponse = await getCourseProgress(firstCourse.id)
      progress.value = progressResponse.data.data
    }
  } catch {
    errorMessage.value = '学习进度加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const logout = (): void => {
  authStore.logout()
  void router.replace('/login')
}

onMounted(loadDashboard)
</script>

<template>
  <main class="dashboard-page">
    <section class="dashboard-shell" aria-labelledby="dashboard-title">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">StockMentor 学习中心</p>
          <h1 id="dashboard-title">欢迎，{{ currentUser?.nickname }}</h1>
          <p>从上次停下的地方继续，稳步建立投资知识。</p>
        </div>
        <nav class="account-actions" aria-label="用户操作">
          <RouterLink class="button-secondary button-link" to="/courses">浏览课程</RouterLink>
          <RouterLink class="button-secondary button-link" to="/profile">个人中心</RouterLink>
          <button data-testid="dashboard-logout" class="button-secondary" type="button" @click="logout">
            退出登录
          </button>
        </nav>
      </header>

      <div v-if="loading" data-testid="dashboard-loading" class="state-card" role="status">
        正在加载学习进度…
      </div>

      <div v-else-if="errorMessage" class="state-card state-error" role="alert">
        <p>{{ errorMessage }}</p>
        <button data-testid="dashboard-retry" type="button" @click="loadDashboard">重新加载</button>
      </div>

      <div v-else-if="!currentCourse" data-testid="dashboard-empty" class="state-card">
        <p>教育内容尚未发布，暂时没有可学习的课程。</p>
        <RouterLink class="button-link" to="/courses">浏览课程</RouterLink>
      </div>

      <article v-else-if="progress" class="progress-card">
        <div class="course-heading">
          <div>
            <p class="section-label">当前课程</p>
            <h2>
              <RouterLink
                data-testid="dashboard-course-title"
                :to="`/courses/${currentCourse.id}`"
              >
                {{ currentCourse.title }}
              </RouterLink>
            </h2>
            <p>{{ currentCourse.summary }}</p>
          </div>
          <strong data-testid="dashboard-progress-percent" class="progress-percent">
            {{ progress.progressPercent }}%
          </strong>
        </div>

        <div class="progress-overview">
          <progress
            :value="progress.progressPercent"
            max="100"
            :aria-label="`${currentCourse.title}学习进度 ${progress.progressPercent}%`"
          />
          <p data-testid="dashboard-progress-count">
            已完成 {{ progress.completedLessons }} / {{ progress.totalLessons }}
          </p>
        </div>

        <div v-if="isComplete" data-testid="dashboard-complete" class="completion-state" role="status">
          <span aria-hidden="true">✓</span>
          <div>
            <strong>已完成《{{ currentCourse.title }}》</strong>
            <p>做得很好。可以回到课程目录复习任意课时。</p>
          </div>
        </div>

        <div v-else-if="progress.nextLesson" class="next-lesson-card">
          <div data-testid="dashboard-next-lesson">
            <p class="section-label">下一课 · {{ progress.nextLesson.chapterTitle }}</p>
            <h3>{{ progress.nextLesson.title }}</h3>
            <p>{{ progress.nextLesson.summary }}</p>
            <small>预计 {{ progress.nextLesson.estimatedMinutes }} 分钟</small>
          </div>
          <RouterLink
            data-testid="continue-learning"
            class="button-link"
            :to="`/lessons/${progress.nextLesson.id}`"
          >
            继续学习
          </RouterLink>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  padding: clamp(1.5rem, 4vw, 3.5rem) 1rem;
  background: #f5f7fb;
}

.dashboard-shell {
  width: min(100%, 62rem);
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.dashboard-header h1,
.dashboard-header p,
.progress-card h2,
.progress-card h3,
.progress-card p,
.state-card p {
  margin: 0;
}

.dashboard-header h1 {
  margin-top: 0.4rem;
  color: #10233f;
  font-size: clamp(2rem, 6vw, 3rem);
}

.dashboard-header h1 + p {
  margin-top: 0.7rem;
  color: #607089;
  line-height: 1.65;
}

.account-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  justify-content: flex-end;
}

.progress-card,
.state-card {
  border: 1px solid #dce4ef;
  border-radius: 1rem;
  background: #ffffff;
  box-shadow: 0 0.8rem 2rem rgb(29 45 72 / 7%);
}

.progress-card {
  padding: clamp(1.4rem, 4vw, 2.25rem);
}

.state-card {
  display: grid;
  gap: 1rem;
  justify-items: start;
  padding: 1.25rem;
  color: #52627a;
}

.state-error {
  color: #991b1b;
  background: #fff7f7;
}

.course-heading,
.next-lesson-card {
  display: flex;
  gap: 1.25rem;
  align-items: center;
  justify-content: space-between;
}

.course-heading h2 {
  margin-top: 0.35rem;
  font-size: clamp(1.5rem, 4vw, 2rem);
}

.course-heading h2 a {
  color: #10233f;
  text-decoration: none;
}

.course-heading h2 a:hover {
  color: #1d4ed8;
}

.course-heading h2 + p,
.next-lesson-card h3 + p,
.completion-state p {
  margin-top: 0.45rem;
  color: #607089;
  line-height: 1.6;
}

.section-label {
  color: #1d4ed8;
  font-size: 0.76rem;
  font-weight: 750;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.progress-percent {
  flex: 0 0 auto;
  color: #1d4ed8;
  font-size: clamp(2rem, 7vw, 3.2rem);
  line-height: 1;
}

.progress-overview {
  margin-top: 1.75rem;
}

.progress-overview progress {
  width: 100%;
  height: 0.75rem;
  accent-color: #1d4ed8;
}

.progress-overview p {
  margin-top: 0.55rem;
  color: #52627a;
  font-weight: 650;
}

.next-lesson-card,
.completion-state {
  margin-top: 1.75rem;
  padding: 1.15rem;
  border-radius: 0.85rem;
  background: #f3f7ff;
}

.next-lesson-card h3 {
  margin-top: 0.35rem;
  color: #172b49;
}

.next-lesson-card small {
  display: block;
  margin-top: 0.55rem;
  color: #607089;
}

.completion-state {
  display: flex;
  gap: 0.9rem;
  align-items: flex-start;
  color: #166534;
  background: #f0fdf4;
}

.completion-state > span {
  display: grid;
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 999px;
  color: #ffffff;
  background: #16a34a;
  font-weight: 800;
}

@media (max-width: 44rem) {
  .dashboard-header,
  .course-heading,
  .next-lesson-card {
    align-items: stretch;
    flex-direction: column;
  }

  .account-actions {
    justify-content: flex-start;
  }

  .progress-percent {
    align-self: flex-start;
  }
}
</style>
