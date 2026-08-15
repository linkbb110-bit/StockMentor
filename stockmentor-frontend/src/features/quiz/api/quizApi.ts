import type { AxiosResponse } from 'axios'

import http from '../../../api/http'
import type { ApiResponse } from '../../../types/api'
import type {
  PublicQuiz,
  QuizAttemptPayload,
  QuizAttemptResult,
  WrongQuestion,
  WrongQuestionAnswerPayload,
  WrongQuestionReviewResult,
  WrongQuestionStatus,
} from '../types/quiz'

export const getQuiz = (
  lessonId: number,
): Promise<AxiosResponse<ApiResponse<PublicQuiz>>> => http.get(`/lessons/${lessonId}/quiz`)

export const submitQuiz = (
  quizId: number,
  payload: QuizAttemptPayload,
): Promise<AxiosResponse<ApiResponse<QuizAttemptResult>>> =>
  http.post(`/me/quizzes/${quizId}/attempts`, payload)

export const listWrongQuestions = (
  status: WrongQuestionStatus,
): Promise<AxiosResponse<ApiResponse<WrongQuestion[]>>> =>
  http.get('/me/wrong-questions', { params: { status } })

export const reviewWrongQuestion = (
  questionId: number,
  payload: WrongQuestionAnswerPayload,
): Promise<AxiosResponse<ApiResponse<WrongQuestionReviewResult>>> =>
  http.post(`/me/wrong-questions/${questionId}/answer`, payload)
