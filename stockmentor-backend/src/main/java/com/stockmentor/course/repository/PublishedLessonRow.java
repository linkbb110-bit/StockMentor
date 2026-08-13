package com.stockmentor.course.repository;

public record PublishedLessonRow(
        long id,
        String title,
        String summary,
        String contentMd,
        int estimatedMinutes,
        long courseId,
        String courseTitle,
        long chapterId,
        String chapterTitle
) {
}
