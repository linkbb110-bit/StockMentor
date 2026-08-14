<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { listWrongQuestions, reviewWrongQuestion } from '../api/quizApi'
import QuestionOptions from '../components/QuestionOptions.vue'
import type {
  QuestionType,
  WrongQuestion,
  WrongQuestionReviewResult,
  WrongQuestionStatus,
} from '../types/quiz'

const activeStatus = ref<WrongQuestionStatus>('PENDING')
const questions = ref<WrongQuestion[]>([])
const loading = ref(true)
const loadError = ref('')
const selectedAnswers = ref<Record<string, number[]>>({})
const submittingQuestions = ref<Record<string, boolean>>({})
const reviewErrors = ref<Record<string, string>>({})
const reviewResults = ref<Record<string, WrongQuestionReviewResult>>({})
const latestReview = ref<WrongQuestionReviewResult | null>(null)
let loadGeneration = 0

const questionKey = (questionId: number): string => String(questionId)
const selectedFor = (questionId: number): number[] =>
  selectedAnswers.value[questionKey(questionId)] ?? []
const isSubmitting = (questionId: number): boolean =>
  submittingQuestions.value[questionKey(questionId)] ?? false
const reviewErrorFor = (questionId: number): string =>
  reviewErrors.value[questionKey(questionId)] ?? ''
const reviewResultFor = (questionId: number): WrongQuestionReviewResult | null =>
  reviewResults.value[questionKey(questionId)] ?? null

const typeLabel = (type: QuestionType): string => {
  if (type === 'MULTIPLE_CHOICE') {
    return '多选题'
  }
  if (type === 'TRUE_FALSE') {
    return '判断题'
  }
  return '单选题'
}

const readableTime = (value: string | null): string =>
  value ? value.replace('T', ' ') : '—'

const loadQuestions = async (
  status: WrongQuestionStatus,
  showLoading = true,
): Promise<void> => {
  const generation = ++loadGeneration
  if (showLoading) {
    loading.value = true
    loadError.value = ''
    questions.value = []
    selectedAnswers.value = {}
    reviewErrors.value = {}
    reviewResults.value = {}
    latestReview.value = null
  }

  try {
    const response = await listWrongQuestions(status)
    if (generation === loadGeneration && activeStatus.value === status) {
      questions.value = response.data.data
      loadError.value = ''
    }
  } catch {
    if (generation === loadGeneration && activeStatus.value === status) {
      loadError.value = showLoading
        ? '错题加载失败，请稍后重试。'
        : '复习结果已提交，但列表刷新失败，请重新加载。'
    }
  } finally {
    if (generation === loadGeneration && activeStatus.value === status) {
      loading.value = false
    }
  }
}

const switchStatus = (status: WrongQuestionStatus): void => {
  if (activeStatus.value === status) {
    return
  }
  activeStatus.value = status
  void loadQuestions(status)
}

const retryLoad = (): void => {
  void loadQuestions(activeStatus.value)
}

const updateSelection = (questionId: number, optionIds: number[]): void => {
  const key = questionKey(questionId)
  selectedAnswers.value = { ...selectedAnswers.value, [key]: [...optionIds] }
  reviewErrors.value = { ...reviewErrors.value, [key]: '' }
  const nextReviewResults = { ...reviewResults.value }
  delete nextReviewResults[key]
  reviewResults.value = nextReviewResults
}

const optionLabels = (question: WrongQuestion, optionIds: number[]): string =>
  question.options
    .filter((option) => optionIds.includes(option.optionId))
    .map((option) => `${option.optionKey}. ${option.content}`)
    .join('、')

const review = async (question: WrongQuestion): Promise<void> => {
  const key = questionKey(question.questionId)
  if (isSubmitting(question.questionId)) {
    return
  }

  const selectedOptionIds = selectedFor(question.questionId)
  if (selectedOptionIds.length === 0) {
    reviewErrors.value = { ...reviewErrors.value, [key]: '请先选择答案。' }
    return
  }

  const status = activeStatus.value
  submittingQuestions.value = { ...submittingQuestions.value, [key]: true }
  reviewErrors.value = { ...reviewErrors.value, [key]: '' }

  try {
    const response = await reviewWrongQuestion(question.questionId, {
      selectedOptionIds: [...selectedOptionIds],
    })
    if (activeStatus.value !== status) {
      return
    }

    const result = response.data.data
    reviewResults.value = { ...reviewResults.value, [key]: result }
    latestReview.value = result
    await loadQuestions(status, false)
  } catch {
    if (activeStatus.value === status) {
      reviewErrors.value = {
        ...reviewErrors.value,
        [key]: '复习提交失败，请稍后重试。',
      }
    }
  } finally {
    submittingQuestions.value = { ...submittingQuestions.value, [key]: false }
  }
}

onMounted(() => loadQuestions(activeStatus.value))
</script>

<template>
  <main class="wrong-page">
    <section class="wrong-shell">
      <header class="wrong-header">
        <div>
          <p class="eyebrow">StockMentor 复习中心</p>
          <h1>错题本</h1>
          <p>通过持续复习，把曾经的错误转化为真正掌握。</p>
        </div>
        <nav class="page-links" aria-label="错题本导航">
          <RouterLink to="/dashboard">学习中心</RouterLink>
          <RouterLink to="/courses">浏览课程</RouterLink>
        </nav>
      </header>

      <div class="status-tabs" role="group" aria-label="错题状态">
        <button
          data-testid="wrong-tab-pending"
          type="button"
          :class="{ active: activeStatus === 'PENDING' }"
          :aria-pressed="activeStatus === 'PENDING'"
          @click="switchStatus('PENDING')"
        >
          待复习
        </button>
        <button
          data-testid="wrong-tab-mastered"
          type="button"
          :class="{ active: activeStatus === 'MASTERED' }"
          :aria-pressed="activeStatus === 'MASTERED'"
          @click="switchStatus('MASTERED')"
        >
          已掌握
        </button>
      </div>

      <p v-if="loading" data-testid="wrong-loading" class="state-card" role="status">
        正在加载错题…
      </p>
      <div v-else-if="loadError" class="state-card state-error" role="alert">
        <p>{{ loadError }}</p>
        <button data-testid="wrong-retry" type="button" @click="retryLoad">重新加载</button>
      </div>

      <template v-else>
        <div v-if="latestReview" class="latest-review" role="status">
          <strong>{{ latestReview.correct ? '回答正确' : '回答错误' }}</strong>
          <p>{{ latestReview.explanation }}</p>
        </div>

        <p v-if="questions.length === 0" class="state-card empty-state">
          {{
            activeStatus === 'PENDING'
              ? '当前没有待复习错题。'
              : '当前还没有已掌握错题。'
          }}
        </p>

        <div v-else class="wrong-list">
          <article
            v-for="question in questions"
            :key="question.questionId"
            data-testid="wrong-question"
            :data-question-id="question.questionId"
            class="wrong-card"
          >
            <header class="question-header">
              <div>
                <RouterLink :to="`/lessons/${question.lessonId}`">
                  {{ question.lessonTitle }}
                </RouterLink>
                <span class="question-type">{{ typeLabel(question.type) }}</span>
              </div>
              <strong>错误 {{ question.errorCount }} 次</strong>
            </header>

            <h2>{{ question.stem }}</h2>
            <QuestionOptions
              :name="`wrong-question-${question.questionId}`"
              :type="question.type"
              :options="question.options"
              :selected-option-ids="selectedFor(question.questionId)"
              :disabled="isSubmitting(question.questionId)"
              @update:selected-option-ids="updateSelection(question.questionId, $event)"
            />

            <dl class="question-times">
              <div>
                <dt>最近答错</dt>
                <dd>{{ readableTime(question.lastWrongAt) }}</dd>
              </div>
              <div v-if="question.status === 'MASTERED'">
                <dt>掌握时间</dt>
                <dd>{{ readableTime(question.masteredAt) }}</dd>
              </div>
            </dl>

            <p v-if="reviewErrorFor(question.questionId)" class="feedback feedback-error" role="alert">
              {{ reviewErrorFor(question.questionId) }}
            </p>

            <div
              v-if="reviewResultFor(question.questionId)"
              class="review-feedback"
              :class="reviewResultFor(question.questionId)?.correct ? 'correct' : 'incorrect'"
              aria-live="polite"
            >
              <strong>
                {{ reviewResultFor(question.questionId)?.correct ? '回答正确' : '回答错误' }}
              </strong>
              <p>
                正确答案：{{
                  optionLabels(question, reviewResultFor(question.questionId)?.correctOptionIds ?? [])
                }}
              </p>
              <p>解析：{{ reviewResultFor(question.questionId)?.explanation }}</p>
            </div>

            <button
              data-testid="wrong-review-submit"
              class="review-button"
              type="button"
              :disabled="isSubmitting(question.questionId)"
              @click="review(question)"
            >
              {{ isSubmitting(question.questionId) ? '提交中…' : '提交复习答案' }}
            </button>
          </article>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped>
.wrong-page {
  min-height: 100vh;
  padding: clamp(1.5rem, 4vw, 3.5rem) 1rem;
  background: #f5f7fb;
}

.wrong-shell {
  width: min(100%, 58rem);
  margin: 0 auto;
}

.wrong-header {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
  justify-content: space-between;
}

.wrong-header h1,
.wrong-header p,
.wrong-card h2,
.state-card p,
.latest-review p,
.review-feedback p {
  margin: 0;
}

.wrong-header h1 {
  margin-top: 0.4rem;
  color: #10233f;
  font-size: clamp(2rem, 6vw, 3rem);
}

.wrong-header h1 + p {
  margin-top: 0.7rem;
  color: #607089;
}

.page-links {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.status-tabs {
  display: flex;
  gap: 0.65rem;
  margin: 1.5rem 0 1rem;
}

.status-tabs button {
  color: #1d4ed8;
  background: #ffffff;
}

.status-tabs button.active {
  color: #ffffff;
  background: #1d4ed8;
}

.wrong-list {
  display: grid;
  gap: 1rem;
}

.wrong-card,
.state-card,
.latest-review {
  padding: clamp(1.25rem, 4vw, 2rem);
  border: 1px solid #dce4ef;
  border-radius: 1rem;
  background: #ffffff;
  box-shadow: 0 0.8rem 2rem rgb(29 45 72 / 6%);
}

.question-header {
  display: flex;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  color: #607089;
  font-size: 0.9rem;
}

.question-type {
  margin-left: 0.6rem;
  padding: 0.2rem 0.45rem;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
}

.wrong-card h2 {
  margin-top: 0.9rem;
  color: #172b49;
  font-size: 1.15rem;
  line-height: 1.55;
}

.question-times {
  display: flex;
  gap: 1.5rem;
  margin: 1rem 0 0;
  color: #607089;
  font-size: 0.82rem;
}

.question-times div {
  display: flex;
  gap: 0.35rem;
}

.question-times dd {
  margin: 0;
}

.review-feedback,
.latest-review {
  margin-top: 1rem;
}

.review-feedback {
  padding: 0.85rem;
  border-radius: 0.7rem;
}

.review-feedback p,
.latest-review p {
  margin-top: 0.35rem;
  line-height: 1.5;
}

.review-feedback.correct,
.latest-review {
  color: #166534;
  background: #f0fdf4;
}

.review-feedback.incorrect {
  color: #991b1b;
  background: #fef2f2;
}

.review-button {
  margin-top: 1rem;
}

.state-card {
  color: #52627a;
}

.state-error {
  display: grid;
  gap: 1rem;
  justify-items: start;
  color: #991b1b;
}

.empty-state {
  text-align: center;
}

@media (max-width: 42rem) {
  .wrong-header,
  .question-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
