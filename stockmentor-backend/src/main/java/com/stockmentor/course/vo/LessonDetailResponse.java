package com.stockmentor.course.vo;

public record LessonDetailResponse(
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
