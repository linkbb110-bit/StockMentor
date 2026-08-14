<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../../auth/stores/authStore'
import { getQuiz, submitQuiz } from '../api/quizApi'
import QuestionOptions from '../components/QuestionOptions.vue'
import type {
  PublicQuiz,
  QuizAttemptResult,
  QuizOption,
  QuizQuestion,
  QuizQuestionResult,
} from '../types/quiz'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const quiz = ref<PublicQuiz | null>(null)
const selectedAnswers = ref<Record<string, number[]>>({})
const attemptResult = ref<QuizAttemptResult | null>(null)
const loading = ref(true)
const loadError = ref('')
const validationError = ref('')
const submitPending = ref(false)
const submitError = ref('')
let requestGeneration = 0

const hasIncorrectAnswers = computed(
  () => attemptResult.value?.results.some((result) => !result.correct) ?? false,
)
const allAnswersCorrect = computed(
  () =>
    attemptResult.value !== null &&
    attemptResult.value.correctCount === attemptResult.value.totalQuestions,
)
const controlsDisabled = computed(() => submitPending.value || attemptResult.value !== null)

const isCurrentRequest = (generation: number): boolean => generation === requestGeneration

const routeLessonId = (): number => {
  const value = route.params.lessonId
  return Number(Array.isArray(value) ? value[0] : value)
}

const resetQuizState = (): void => {
  quiz.value = null
  selectedAnswers.value = {}
  attemptResult.value = null
  loading.value = true
  loadError.value = ''
  validationError.value = ''
  submitPending.value = false
  submitError.value = ''
}

const loadQuiz = async (lessonId: number, generation: number): Promise<void> => {
  resetQuizState()
  if (!Number.isSafeInteger(lessonId) || lessonId <= 0) {
    loadError.value = '无效的课时地址。'
    loading.value = false
    return
  }

  try {
    const response = await getQuiz(lessonId)
    if (isCurrentRequest(generation)) {
      quiz.value = response.data.data
    }
  } catch {
    if (isCurrentRequest(generation)) {
      loadError.value = '测验加载失败，请稍后重试。'
    }
  } finally {
    if (isCurrentRequest(generation)) {
      loading.value = false
    }
  }
}

const retryLoad = (): void => {
  requestGeneration += 1
  void loadQuiz(routeLessonId(), requestGeneration)
}

const selectedFor = (questionId: number): number[] =>
  selectedAnswers.value[String(questionId)] ?? []

const updateSelection = (questionId: number, optionIds: number[]): void => {
  selectedAnswers.value = {
    ...selectedAnswers.value,
    [String(questionId)]: [...optionIds],
  }
  validationError.value = ''
  submitError.value = ''
}

const questionResult = (questionId: number): QuizQuestionResult | null =>
  attemptResult.value?.results.find((result) => result.questionId === questionId) ?? null

const optionLabels = (question: QuizQuestion, optionIds: number[]): string =>
  question.options
    .filter((option) => optionIds.includes(option.optionId))
    .map((option: QuizOption) => `${option.optionKey}. ${option.content}`)
    .join('、')

const submit = async (): Promise<void> => {
  const currentQuiz = quiz.value
  if (!currentQuiz || submitPending.value || attemptResult.value) {
    return
  }

  if (currentQuiz.questions.some((question) => selectedFor(question.questionId).length === 0)) {
    validationError.value = '请完成所有题目后再提交。'
    return
  }

  if (!authStore.isAuthenticated) {
    await router.push('/login')
    return
  }

  const generation = requestGeneration
  const quizId = currentQuiz.id
  submitPending.value = true
  submitError.value = ''
  validationError.value = ''

  try {
    const response = await submitQuiz(quizId, {
      answers: currentQuiz.questions.map((question) => ({
        questionId: question.questionId,
        selectedOptionIds: [...selectedFor(question.questionId)],
      })),
    })
    if (isCurrentRequest(generation) && quiz.value?.id === quizId) {
      attemptResult.value = response.data.data
    }
  } catch {
    if (isCurrentRequest(generation) && quiz.value?.id === quizId) {
      submitError.value = '测验提交失败，请稍后重试。'
    }
  } finally {
    if (isCurrentRequest(generation) && quiz.value?.id === quizId) {
      submitPending.value = false
    }
  }
}

const retake = (): void => {
  selectedAnswers.value = {}
  attemptResult.value = null
  validationError.value = ''
  submitError.value = ''
}

watch(
  () => route.params.lessonId,
  () => {
    requestGeneration += 1
    void loadQuiz(routeLessonId(), requestGeneration)
  },
  { immediate: true },
)
</script>

<template>
  <main class="quiz-page">
    <section class="quiz-shell">
      <p v-if="loading" data-testid="quiz-loading" class="state-card" role="status">
        正在加载课后测验…
      </p>
      <div v-else-if="loadError" class="state-card state-error" role="alert">
        <p>{{ loadError }}</p>
        <button data-testid="quiz-retry" type="button" @click="retryLoad">重新加载</button>
      </div>

      <template v-else-if="quiz">
        <nav class="breadcrumb" aria-label="测验导航">
          <RouterLink :to="`/lessons/${quiz.lessonId}`">返回《{{ quiz.lessonTitle }}》</RouterLink>
          <span aria-hidden="true">/</span>
          <span>{{ quiz.chapterTitle }}</span>
        </nav>

        <header class="quiz-header">
          <p class="eyebrow">{{ quiz.courseTitle }}</p>
          <h1>{{ quiz.title }}</h1>
          <p>{{ quiz.summary }}</p>
        </header>

        <form class="quiz-form" novalidate :aria-busy="submitPending" @submit.prevent="submit">
          <article
            v-for="(question, index) in quiz.questions"
            :key="question.questionId"
            data-testid="quiz-question"
            class="question-card"
          >
            <fieldset>
              <legend><span>第 {{ index + 1 }} 题</span>{{ question.stem }}</legend>
              <QuestionOptions
                :name="`quiz-question-${question.questionId}`"
                :type="question.type"
                :options="question.options"
                :selected-option-ids="selectedFor(question.questionId)"
                :disabled="controlsDisabled"
                @update:selected-option-ids="updateSelection(question.questionId, $event)"
              />
            </fieldset>

            <div
              v-if="questionResult(question.questionId)"
              class="question-feedback"
              :class="questionResult(question.questionId)?.correct ? 'correct' : 'incorrect'"
            >
              <strong>
                {{ questionResult(question.questionId)?.correct ? '回答正确' : '回答错误' }}
              </strong>
              <p>
                你的选择：{{ optionLabels(question, questionResult(question.questionId)?.selectedOptionIds ?? []) }}
              </p>
              <p>
                正确答案：{{ optionLabels(question, questionResult(question.questionId)?.correctOptionIds ?? []) }}
              </p>
              <p>解析：{{ questionResult(question.questionId)?.explanation }}</p>
            </div>
          </article>

          <p v-if="validationError || submitError" class="feedback feedback-error" role="alert">
            {{ validationError || submitError }}
          </p>

          <button
            v-if="!attemptResult"
            data-testid="quiz-submit"
            class="submit-button"
            type="submit"
            :disabled="submitPending"
          >
            {{ submitPending ? '提交中…' : '提交答案' }}
          </button>
        </form>

        <section v-if="attemptResult" data-testid="quiz-result" class="result-card" aria-live="polite">
          <h2 data-testid="quiz-score">
            得分 {{ attemptResult.scorePercent }}% · 答对 {{ attemptResult.correctCount }} / {{ attemptResult.totalQuestions }}
          </h2>
          <p v-if="allAnswersCorrect" class="all-correct">✓ 全部答对，继续保持。</p>
          <RouterLink v-if="hasIncorrectAnswers" class="button-link" to="/wrong-questions">
            查看错题本
          </RouterLink>
          <button data-testid="quiz-retake" class="button-secondary" type="button" @click="retake">
            重新测验
          </button>
        </section>
      </template>
    </section>
  </main>
</template>

<style scoped>
.quiz-page {
  min-height: 100vh;
  padding: clamp(1.5rem, 4vw, 3.5rem) 1rem;
  background: #f5f7fb;
}

.quiz-shell {
  width: min(100%, 52rem);
  margin: 0 auto;
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  color: #607089;
  font-size: 0.9rem;
}

.quiz-header,
.question-card,
.result-card,
.state-card {
  border: 1px solid #dce4ef;
  border-radius: 1rem;
  background: #ffffff;
  box-shadow: 0 0.8rem 2rem rgb(29 45 72 / 6%);
}

.quiz-header,
.question-card,
.result-card,
.state-card {
  padding: clamp(1.25rem, 4vw, 2rem);
}

.quiz-header h1,
.quiz-header p,
.result-card h2,
.result-card p,
.state-card p {
  margin: 0;
}

.quiz-header h1 {
  margin-top: 0.4rem;
  color: #10233f;
  font-size: clamp(1.8rem, 5vw, 2.6rem);
}

.quiz-header h1 + p {
  margin-top: 0.7rem;
  color: #607089;
  line-height: 1.6;
}

.quiz-form {
  display: grid;
  gap: 1rem;
  margin-top: 1.25rem;
}

.question-card fieldset {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.question-card legend {
  color: #172b49;
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.55;
}

.question-card legend span {
  display: block;
  color: #1d4ed8;
  font-size: 0.75rem;
  letter-spacing: 0.08em;
}

.question-feedback {
  margin-top: 1rem;
  padding: 0.9rem;
  border-radius: 0.7rem;
}

.question-feedback p {
  margin: 0.35rem 0 0;
  line-height: 1.5;
}

.question-feedback.correct {
  color: #166534;
  background: #f0fdf4;
}

.question-feedback.incorrect {
  color: #991b1b;
  background: #fef2f2;
}

.submit-button {
  width: 100%;
}

.result-card {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  align-items: center;
  margin-top: 1.25rem;
}

.result-card h2 {
  width: 100%;
  color: #10233f;
}

.all-correct {
  width: 100%;
  color: #166534;
  font-weight: 700;
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
</style>
