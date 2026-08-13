export interface CourseSummary {
  id: number
  title: string
  summary: string
  coverUrl: string | null
}

export interface LessonSummary {
  id: number
  title: string
  summary: string
  estimatedMinutes: number
}

export interface ChapterSummary {
  id: number
  title: string
  summary: string
  lessons: LessonSummary[]
}

export interface CourseDetail extends CourseSummary {
  chapters: ChapterSummary[]
}

export interface LessonDetail {
  id: number
  title: string
  summary: string
  contentMd: string
  estimatedMinutes: number
  courseId: number
  courseTitle: string
  chapterId: number
  chapterTitle: string
}

export interface NextLesson {
  id: number
  title: string
  summary: string
  estimatedMinutes: number
  chapterId: number
  chapterTitle: string
}

export interface CourseProgress {
  completedLessons: number
  totalLessons: number
  progressPercent: number
  completedLessonIds: number[]
  nextLesson: NextLesson | null
}

export interface LessonCompletion {
  lessonId: number
  completed: boolean
  completedAt: string
}
