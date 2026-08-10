import type { AxiosResponse } from 'axios'

import http from '../../../api/http'
import type { ApiResponse } from '../../../types/api'
import type { CourseDetail, CourseSummary, LessonDetail } from '../types/course'

export const listCourses = (): Promise<AxiosResponse<ApiResponse<CourseSummary[]>>> =>
  http.get('/courses')

export const getCourse = (
  courseId: number,
): Promise<AxiosResponse<ApiResponse<CourseDetail>>> => http.get(`/courses/${courseId}`)

export const getLesson = (
  lessonId: number,
): Promise<AxiosResponse<ApiResponse<LessonDetail>>> => http.get(`/lessons/${lessonId}`)
