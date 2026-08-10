package com.stockmentor.course.repository;

public record NextLessonRow(
        long id,
        String title,
        String summary,
        int estimatedMinutes,
        long chapterId,
        String chapterTitle
) {
}
