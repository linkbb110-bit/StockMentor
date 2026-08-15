export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE'

export interface QuizOption {
  optionId: number
  optionKey: string
  content: string
}

export interface QuizQuestion {
  questionId: number
  type: QuestionType
  stem: string
  options: QuizOption[]
}

export interface Quiz {
  id: number
  lessonId: number
  lessonTitle: string
  courseId: number
  courseTitle: string
  chapterId: number
  chapterTitle: string
  title: string
  summary: string
  questions: QuizQuestion[]
}

export type PublicQuiz = Quiz

export interface QuizAnswerSubmission {
  questionId: number
  selectedOptionIds: number[]
}

export interface QuizAttemptPayload {
  answers: QuizAnswerSubmission[]
}

export interface QuizQuestionResult {
  questionId: number
  correct: boolean
  selectedOptionIds: number[]
  correctOptionIds: number[]
  explanation: string
}

export interface QuizAttemptResult {
  attemptId: number
  totalQuestions: number
  correctCount: number
  scorePercent: number
  results: QuizQuestionResult[]
}

export type WrongQuestionStatus = 'PENDING' | 'MASTERED'

export interface WrongQuestion {
  questionId: number
  lessonId: number
  lessonTitle: string
  type: QuestionType
  stem: string
  options: QuizOption[]
  status: WrongQuestionStatus
  errorCount: number
  lastWrongAt: string
  masteredAt: string | null
}

export interface WrongQuestionAnswerPayload {
  selectedOptionIds: number[]
}

export interface WrongQuestionReviewResult {
  questionId: number
  correct: boolean
  status: WrongQuestionStatus
  errorCount: number
  correctOptionIds: number[]
  explanation: string
}
