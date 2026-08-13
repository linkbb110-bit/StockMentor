import type { AxiosResponse } from 'axios'

import http from '../../../api/http'
import type { ApiResponse } from '../../../types/api'
import type { CourseProgress, LessonCompletion } from '../types/course'

export const getCourseProgress = (
  courseId: number,
): Promise<AxiosResponse<ApiResponse<CourseProgress>>> =>
  http.get(`/me/courses/${courseId}/progress`)

export const completeLesson = (
  lessonId: number,
): Promise<AxiosResponse<ApiResponse<LessonCompletion>>> =>
  http.put(`/me/lessons/${lessonId}/completion`)
