package com.stockmentor.course.vo;

import java.util.List;

public record CourseProgressResponse(
        long completedLessons,
        long totalLessons,
        int progressPercent,
        List<Long> completedLessonIds,
        NextLessonResponse nextLesson
) {
}
